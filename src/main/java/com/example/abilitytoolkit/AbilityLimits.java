package com.example.abilitytoolkit;

/** Constants and helpers shared by the client and the server. */
public final class AbilityLimits {
	public static final int MIN_SPEED_LEVEL = 1;
	public static final int MAX_SPEED_LEVEL = 10;

	/** Vanilla creative flight speed (Abilities default). */
	public static final float VANILLA_FLYING_SPEED = 0.05F;

	private AbilityLimits() {
	}

	public static int clampLevel(int level) {
		return Math.max(MIN_SPEED_LEVEL, Math.min(MAX_SPEED_LEVEL, level));
	}

	/** Level 1 = x1.5, level 2 = x2.0 ... level 10 = x6.0. */
	public static double speedMultiplier(int level) {
		return 1.0 + 0.5 * clampLevel(level);
	}
}
