package com.example.abilitytoolkit.server;

import com.example.abilitytoolkit.AbilityLimits;
import com.example.abilitytoolkit.AbilityToolkit;
import com.example.abilitytoolkit.payload.AbilityRequestPayload;
import com.example.abilitytoolkit.payload.PermissionPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;

/**
 * Applies abilities to players using ONLY vanilla mechanisms:
 *
 * <ul>
 *   <li>No Knockback -> attribute modifiers on knockback_resistance (+1.0) and explosion_knockback_resistance (+1.0)</li>
 *   <li>Speed Boost  -> a movement_speed attribute modifier (ADD_MULTIPLIED_TOTAL) and a scaled flying speed</li>
 *   <li>Flight       -> the same Abilities.mayfly flag that creative mode uses</li>
 * </ul>
 *
 * Because the server itself applies these, its movement checks, fall-damage rules and "flying is not enabled"
 * checks all see consistent state. Nothing here spoofs packets or fights the server.
 */
public final class AbilityManager {
	private static final Identifier KNOCKBACK_ID = AbilityToolkit.id("no_knockback");
	private static final Identifier EXPLOSION_KNOCKBACK_ID = AbilityToolkit.id("no_explosion_knockback");
	private static final Identifier SPEED_ID = AbilityToolkit.id("speed_boost");

	/**
	 * Entity tag stored in the player's save data while WE granted flight. If the game crashes, the tag survives,
	 * and on next login we know the leftover mayfly flag came from us and can remove it.
	 */
	private static final String FLIGHT_TAG = "abilitytoolkit_flight";

	/** What the server actually granted (request AND permission), per player. Memory only. */
	private record Granted(boolean noKnockback, boolean flight, boolean speed, int speedLevel) {
		static final Granted NONE = new Granted(false, false, false, AbilityLimits.MIN_SPEED_LEVEL);

		boolean isNone() {
			return !noKnockback && !flight && !speed;
		}
	}

	private static final Map<UUID, Granted> ACTIVE = new HashMap<>();

	private AbilityManager() {
	}

	// ---------------------------------------------------------------- lifecycle

	public static void onJoin(ServerPlayer player) {
		// Crash recovery: flight we granted last session must not survive in save data.
		if (player.removeTag(FLIGHT_TAG)) {
			restoreVanillaFlight(player);
		}
		// Handshake: tell the client what it may do. The client answers with its saved preferences.
		ServerPlayNetworking.send(player, ServerPolicy.permissionsFor(player));
	}

	public static void onDisconnect(ServerPlayer player) {
		// Called before the player is saved, so granted flight is removed from the save data.
		ACTIVE.remove(player.getUUID());
		apply(player, Granted.NONE);
	}

	public static void releaseAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			onDisconnect(player);
		}
		ACTIVE.clear();
	}

	// ---------------------------------------------------------------- requests

	public static void handleRequest(ServerPlayer player, AbilityRequestPayload request) {
		// NEVER trust the client: intersect the request with the server policy.
		PermissionPayload permissions = ServerPolicy.permissionsFor(player);
		int level = Math.min(AbilityLimits.clampLevel(request.speedLevel()), permissions.maxSpeedLevel());
		Granted granted = new Granted(
				request.noKnockback() && permissions.noKnockback(),
				request.flight() && permissions.flight(),
				request.speed() && permissions.speed(),
				level);

		if (granted.isNone()) {
			ACTIVE.remove(player.getUUID());
		} else {
			ACTIVE.put(player.getUUID(), granted);
		}
		apply(player, granted);
	}

	/** Re-applies grants about once per second (respawns create a new player object, /gamemode resets abilities). */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0 || ACTIVE.isEmpty()) {
			return;
		}
		for (Map.Entry<UUID, Granted> entry : ACTIVE.entrySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null && !isApplied(player, entry.getValue())) {
				apply(player, entry.getValue());
			}
		}
	}

	// ---------------------------------------------------------------- applying

	private static void apply(ServerPlayer player, Granted granted) {
		setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID,
				1.0, AttributeModifier.Operation.ADD_VALUE, granted.noKnockback());
		setModifier(player, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, EXPLOSION_KNOCKBACK_ID,
				1.0, AttributeModifier.Operation.ADD_VALUE, granted.noKnockback());

		// ADD_MULTIPLIED_TOTAL: final = value * (1 + amount). amount = multiplier - 1.
		setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID,
				AbilityLimits.speedMultiplier(granted.speedLevel()) - 1.0,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, granted.speed());

		applyFlight(player, granted);
	}

	private static void setModifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id,
									double amount, AttributeModifier.Operation operation, boolean enabled) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		if (enabled) {
			// "Transient" = not written to the save file, so it can never outlive the session.
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
		} else if (instance.hasModifier(id)) {
			instance.removeModifier(id);
		}
	}

	private static void applyFlight(ServerPlayer player, Granted granted) {
		if (granted.flight()) {
			float flyingSpeed = AbilityLimits.VANILLA_FLYING_SPEED
					* (float) (granted.speed() ? AbilityLimits.speedMultiplier(granted.speedLevel()) : 1.0);
			Abilities abilities = player.getAbilities();
			player.addTag(FLIGHT_TAG);
			abilities.mayfly = true;
			abilities.setFlyingSpeed(flyingSpeed);
			player.onUpdateAbilities(); // sends the abilities packet to the client
		} else if (player.removeTag(FLIGHT_TAG)) {
			restoreVanillaFlight(player);
		}
	}

	/** Puts flight back to what the current game mode would normally give. */
	private static void restoreVanillaFlight(ServerPlayer player) {
		Abilities abilities = player.getAbilities();
		boolean vanillaMayFly = player.isCreative() || player.isSpectator();
		abilities.mayfly = vanillaMayFly;
		if (!vanillaMayFly) {
			abilities.flying = false;
		}
		abilities.setFlyingSpeed(AbilityLimits.VANILLA_FLYING_SPEED);
		player.onUpdateAbilities();
	}

	private static boolean isApplied(ServerPlayer player, Granted granted) {
		return hasModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID) == granted.noKnockback()
				&& hasModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID) == granted.speed()
				&& (!granted.flight() || player.getAbilities().mayfly);
	}

	private static boolean hasModifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id) {
		AttributeInstance instance = player.getAttribute(attribute);
		return instance != null && instance.hasModifier(id);
	}
}
