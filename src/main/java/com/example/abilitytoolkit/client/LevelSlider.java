package com.example.abilitytoolkit.client;

import com.example.abilitytoolkit.AbilityLimits;
import java.util.Locale;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/**
 * Slider for the speed level (1-10). AbstractSliderButton keeps a value between 0.0 and 1.0 in {@code value};
 * we convert it to and from a whole level. If this class ever fails to compile after a Minecraft update,
 * it is self-contained: swap it for two +/- Buttons that call AbilityController.setSpeedLevel.
 */
final class LevelSlider extends AbstractSliderButton {
	LevelSlider(int x, int y, int width, int height) {
		super(x, y, width, height, Component.empty(), toSlider(ClientConfig.get().speedLevel));
		updateMessage();
	}

	private static double toSlider(int level) {
		return (level - AbilityLimits.MIN_SPEED_LEVEL)
				/ (double) (AbilityLimits.MAX_SPEED_LEVEL - AbilityLimits.MIN_SPEED_LEVEL);
	}

	private static int fromSlider(double value) {
		int span = AbilityLimits.MAX_SPEED_LEVEL - AbilityLimits.MIN_SPEED_LEVEL;
		return AbilityLimits.MIN_SPEED_LEVEL + (int) Math.round(value * span);
	}

	@Override
	protected void updateMessage() {
		int level = fromSlider(this.value);
		String multiplier = String.format(Locale.ROOT, "%.1f", AbilityLimits.speedMultiplier(level));
		setMessage(Component.translatable("abilitytoolkit.slider.speed", level, multiplier));
	}

	@Override
	protected void applyValue() {
		AbilityController.setSpeedLevel(fromSlider(this.value));
	}
}
