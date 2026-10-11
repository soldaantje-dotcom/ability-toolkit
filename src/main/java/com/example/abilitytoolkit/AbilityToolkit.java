package com.example.abilitytoolkit;

import com.example.abilitytoolkit.payload.AbilityRequestPayload;
import com.example.abilitytoolkit.payload.PermissionPayload;
import com.example.abilitytoolkit.server.AbilityManager;
import com.example.abilitytoolkit.server.ServerPolicy;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint. Runs on dedicated servers, integrated (singleplayer/LAN) servers and clients.
 * All gameplay changes happen here, on the logical SERVER, so vanilla server-side validation stays consistent.
 */
public class AbilityToolkit implements ModInitializer {
	public static final String MOD_ID = "abilitytoolkit";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// 1. Register packets (must happen on both sides).
		PayloadTypeRegistry.clientboundPlay().register(PermissionPayload.TYPE, PermissionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AbilityRequestPayload.TYPE, AbilityRequestPayload.CODEC);

		// 2. Server policy (who may use what) is loaded when a server starts.
		ServerLifecycleEvents.SERVER_STARTING.register(ServerPolicy::load);
		// Make sure nothing we granted gets written into player save data.
		ServerLifecycleEvents.SERVER_STOPPING.register(AbilityManager::releaseAll);

		// 3. Tell each joining player what they are allowed to do; clean up on leave.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> AbilityManager.onJoin(handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> AbilityManager.onDisconnect(handler.getPlayer()));

		// 4. Requests from clients are validated against the policy.
		ServerPlayNetworking.registerGlobalReceiver(AbilityRequestPayload.TYPE,
				(payload, context) -> AbilityManager.handleRequest(context.player(), payload));

		// 5. Keep granted abilities applied (respawn, /gamemode changes, ...).
		ServerTickEvents.END_SERVER_TICK.register(AbilityManager::tick);

		LOGGER.info("Ability Toolkit loaded (abilities are server-authorized).");
	}
}
