package com.example.abilitytoolkit.client;

import com.example.abilitytoolkit.payload.PermissionPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The settings menu: three toggles, a speed slider, a HUD toggle and the current server authorization. */
public class AbilityScreen extends Screen {
	private static final int WIDTH = 200;
	private static final int ROW = 24;

	private final Screen parent;
	private Button noKnockbackButton;
	private Button flightButton;
	private Button speedButton;
	private Button hudButton;

	public AbilityScreen(Screen parent) {
		super(Component.translatable("abilitytoolkit.screen.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = this.width / 2 - WIDTH / 2;
		int y = Math.max(34, this.height / 2 - 80);

		// Widgets are created in init() (not the constructor) because width/height are only valid here.
		noKnockbackButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggle(Ability.NO_KNOCKBACK))
				.bounds(x, y, WIDTH, 20).build());
		flightButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggle(Ability.FLIGHT))
				.bounds(x, y + ROW, WIDTH, 20).build());
		speedButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggle(Ability.SPEED))
				.bounds(x, y + ROW * 2, WIDTH, 20).build());
		addRenderableWidget(new LevelSlider(x, y + ROW * 3, WIDTH, 20));
		hudButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
			ClientConfig.get().showHud = !ClientConfig.get().showHud;
			ClientConfig.save();
			refresh();
		}).bounds(x, y + ROW * 4, WIDTH, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(x, y + ROW * 5 + 6, WIDTH, 20).build());

		refresh();
	}

	private void toggle(Ability ability) {
		AbilityController.toggle(ability);
		refresh();
	}

	private void refresh() {
		noKnockbackButton.setMessage(label(Ability.NO_KNOCKBACK));
		flightButton.setMessage(label(Ability.FLIGHT));
		speedButton.setMessage(label(Ability.SPEED));
		hudButton.setMessage(Component.translatable("abilitytoolkit.button.hud", Component.translatable(
				ClientConfig.get().showHud ? "abilitytoolkit.state.on" : "abilitytoolkit.state.off")));
	}

	private Component label(Ability ability) {
		String state;
		if (!AbilityController.isAllowed(ability)) {
			state = "abilitytoolkit.state.locked";
		} else {
			state = ClientConfig.get().get(ability) ? "abilitytoolkit.state.on" : "abilitytoolkit.state.off";
		}
		return Component.translatable("abilitytoolkit.button.ability", ability.displayName(), Component.translatable(state));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		// Calling super draws the background and all registered widgets.
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		int centerX = this.width / 2;
		int top = Math.max(34, this.height / 2 - 80);
		graphics.text(this.font, this.title, centerX - this.font.width(this.title) / 2, top - 20, 0xFFFFFFFF, true);

		Component status = statusLine();
		int statusY = top + ROW * 5 + 34;
		graphics.text(this.font, status, centerX - this.font.width(status) / 2, statusY, 0xFFAAAAAA, false);
	}

	private Component statusLine() {
		if (!ClientState.hasServerHandshake()) {
			return Component.translatable("abilitytoolkit.status.no_server_mod");
		}
		PermissionPayload p = ClientState.permissions();
		if (!p.any()) {
			return Component.translatable("abilitytoolkit.status.denied");
		}
		return Component.translatable("abilitytoolkit.status.granted",
				yesNo(p.noKnockback()), yesNo(p.flight()), yesNo(p.speed()), p.maxSpeedLevel());
	}

	private static Component yesNo(boolean value) {
		return Component.translatable(value ? "abilitytoolkit.yes" : "abilitytoolkit.no");
	}

	@Override
	public void onClose() {
		ClientConfig.save();
		this.minecraft.gui.setScreen(this.parent);
	}
}
