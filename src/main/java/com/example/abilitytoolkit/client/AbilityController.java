package com.example.abilitytoolkit.client;

import com.example.abilitytoolkit.AbilityLimits;
import com.example.abilitytoolkit.payload.AbilityRequestPayload;
import com.example.abilitytoolkit.payload.PermissionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Abilities;

/** Client-side logic: toggles, syncing preferences to the server, and the per-tick flight start. */
public final class AbilityController {
	private AbilityController() {
	}

	public static boolean isAllowed(Ability ability) {
		PermissionPayload permissions = ClientState.permissions();
		return switch (ability) {
			case NO_KNOCKBACK -> permissions.noKnockback();
			case FLIGHT -> permissions.flight();
			case SPEED -> permissions.speed();
		};
	}

	/** Enabled by the player AND authorized by the server. */
	public static boolean isActive(Ability ability) {
		return ClientConfig.get().get(ability) && isAllowed(ability);
	}

	/** The level the server will really use (the player's choice, capped by the server). */
	public static int effectiveSpeedLevel() {
		return Math.min(ClientConfig.get().speedLevel, ClientState.permissions().maxSpeedLevel());
	}

	public static void toggle(Ability ability) {
		ClientConfig config = ClientConfig.get();
		boolean enable = !config.get(ability);

		if (enable && !isAllowed(ability)) {
			ClientState.flash(Component.translatable("abilitytoolkit.flash.locked", ability.displayName()));
			return;
		}

		config.set(ability, enable);
		ClientConfig.save();
		if (ability == Ability.FLIGHT && enable) {
			ClientState.requestFlyStart();
		}
		sync();
		ClientState.flash(Component.translatable(
				enable ? "abilitytoolkit.flash.on" : "abilitytoolkit.flash.off", ability.displayName()));
	}

	public static void setSpeedLevel(int level) {
		int clamped = AbilityLimits.clampLevel(level);
		ClientConfig config = ClientConfig.get();
		if (clamped == config.speedLevel) {
			return;
		}
		config.speedLevel = clamped;
		sync();
	}

	/** Sends our preferences. The server answers by applying only what the policy allows. */
	public static void sync() {
		// canSend is false on servers that do not have this mod -> we simply stay vanilla.
		if (!ClientPlayNetworking.canSend(AbilityRequestPayload.TYPE)) {
			return;
		}
		ClientConfig config = ClientConfig.get();
		ClientPlayNetworking.send(new AbilityRequestPayload(
				config.noKnockback, config.flight, config.speed, config.speedLevel));
	}

	/** Runs every client tick. */
	public static void tick(Minecraft client) {
		Keybinds.poll(client);

		LocalPlayer player = client.player;
		if (player == null || ClientState.pendingFlyTicks() == 0) {
			return;
		}
		Abilities abilities = player.getAbilities();
		if (abilities.mayfly) {
			// The server has granted flight: start flying right away instead of needing a double-tap of jump.
			abilities.flying = true;
			player.onUpdateAbilities();
			ClientState.clearPendingFly();
		} else {
			ClientState.decrementPendingFly();
		}
	}
}
