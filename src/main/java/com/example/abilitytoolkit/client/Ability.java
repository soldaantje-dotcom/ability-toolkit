package com.example.abilitytoolkit.client;

import net.minecraft.network.chat.Component;

/** The three toggleable abilities. */
public enum Ability {
	NO_KNOCKBACK("no_knockback"),
	FLIGHT("flight"),
	SPEED("speed");

	private final String key;

	Ability(String key) {
		this.key = key;
	}

	public Component displayName() {
		return Component.translatable("abilitytoolkit.ability." + key);
	}
}
