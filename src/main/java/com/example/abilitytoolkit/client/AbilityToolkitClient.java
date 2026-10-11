package com.example.abilitytoolkit.client;

import com.example.abilitytoolkit.AbilityToolkit;
import com.example.abilitytoolkit.payload.PermissionPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

/** Client-only entrypoint: config, keybinds, networking receiver, tick loop and HUD. */
public class AbilityToolkitClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		AbilityToolkit.LOGGER.info("Ability Toolkit CLIENT loaded");
		ClientConfig.load();
		Keybinds.init();

		// Server -> client handshake: remember what we are allowed to do, then send our saved preferences.
		ClientPlayNetworking.registerGlobalReceiver(PermissionPayload.TYPE, (payload, context) -> {
			ClientState.setPermissions(payload);
			AbilityController.sync();
		});

		// Until a server tells us otherwise, every ability is locked.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientState.reset());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.reset());

		ClientTickEvents.END_CLIENT_TICK.register(AbilityController::tick);

		// Draw just before the chat layer so we sit on top of the normal HUD.
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, AbilityToolkit.id("ability_hud"), AbilityHud::extract);
	}
}
