package com.example.abilitytoolkit.client;

import com.example.abilitytoolkit.AbilityToolkit;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * One key per feature plus one for the menu. They appear in Options > Controls > Key Binds under
 * "Ability Toolkit", so players can rebind them with the normal vanilla UI (saved in options.txt).
 */
public final class Keybinds {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(AbilityToolkit.id("main"));

	public static final KeyMapping OPEN_MENU = create("open_menu", InputConstants.KEY_RSHIFT);
	public static final KeyMapping TOGGLE_NO_KNOCKBACK = create("toggle_no_knockback", InputConstants.KEY_K);
	public static final KeyMapping TOGGLE_FLIGHT = create("toggle_flight", InputConstants.KEY_G);
	public static final KeyMapping TOGGLE_SPEED = create("toggle_speed", InputConstants.KEY_V);

	private Keybinds() {
	}

	private static KeyMapping create(String name, int defaultKey) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key." + AbilityToolkit.MOD_ID + "." + name,
				InputConstants.Type.KEYSYM,
				defaultKey,
				CATEGORY));
	}

	/** Forces the static fields above to initialize (and therefore register) at startup. */
	public static void init() {
	}

	/** consumeClick() returns true once per key press, so each press toggles exactly once. */
	public static void poll(Minecraft client) {
		while (TOGGLE_NO_KNOCKBACK.consumeClick()) {
			if (client.player != null) {
				AbilityController.toggle(Ability.NO_KNOCKBACK);
			}
		}
		while (TOGGLE_FLIGHT.consumeClick()) {
			if (client.player != null) {
				AbilityController.toggle(Ability.FLIGHT);
			}
		}
		while (TOGGLE_SPEED.consumeClick()) {
			if (client.player != null) {
				AbilityController.toggle(Ability.SPEED);
			}
		}
		while (OPEN_MENU.consumeClick()) {
			if (client.player != null && client.gui.screen() == null) {
				client.gui.setScreen(new AbilityScreen(null));
			}
		}
	}
}
