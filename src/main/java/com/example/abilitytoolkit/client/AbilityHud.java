package com.example.abilitytoolkit.client;

import com.example.abilitytoolkit.AbilityLimits;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Draws active abilities in the top-left corner. Registered with Fabric's HudElementRegistry. */
public final class AbilityHud {
	private static final int GREEN = 0xFF55FF55;
	private static final int RED = 0xFFFF5555;
	private static final int YELLOW = 0xFFFFFF55;

	private record Line(Component text, int color) {
	}

	private AbilityHud() {
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		ClientConfig config = ClientConfig.get();
		List<Line> lines = new ArrayList<>();

		if (config.showHud) {
			for (Ability ability : Ability.values()) {
				if (!config.get(ability)) {
					continue; // only abilities the player switched on are listed
				}
				if (!AbilityController.isAllowed(ability)) {
					lines.add(new Line(Component.translatable("abilitytoolkit.hud.locked", ability.displayName()), RED));
				} else if (ability == Ability.SPEED) {
					int level = AbilityController.effectiveSpeedLevel();
					String multiplier = String.format(Locale.ROOT, "%.1f", AbilityLimits.speedMultiplier(level));
					lines.add(new Line(Component.translatable(
							"abilitytoolkit.hud.speed", ability.displayName(), multiplier, level), GREEN));
				} else {
					lines.add(new Line(ability.displayName(), GREEN));
				}
			}
		}

		Component flash = ClientState.activeMessage();
		if (flash != null) {
			lines.add(new Line(flash, YELLOW));
		}
		if (lines.isEmpty()) {
			return;
		}

		int x = 6;
		int y = 6;
		int lineHeight = client.font.lineHeight + 2;
		int width = 0;
		for (Line line : lines) {
			width = Math.max(width, client.font.width(line.text()));
		}

		// Semi-transparent backdrop, then one line of text per ability.
		graphics.fill(x - 3, y - 3, x + width + 3, y + lines.size() * lineHeight + 1, 0x90000000);
		for (Line line : lines) {
			graphics.text(client.font, line.text(), x, y, line.color(), true);
			y += lineHeight;
		}
	}
}
