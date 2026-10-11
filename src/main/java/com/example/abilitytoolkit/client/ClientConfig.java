package com.example.abilitytoolkit.client;

import com.example.abilitytoolkit.AbilityLimits;
import com.example.abilitytoolkit.AbilityToolkit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Client preferences, saved to config/abilitytoolkit-client.json.
 * These are only what the player WANTS; the server decides what is actually granted.
 * (Key bindings are stored by vanilla in options.txt, no extra work needed.)
 */
public final class ClientConfig {
	public boolean noKnockback = false;
	public boolean flight = false;
	public boolean speed = false;
	public int speedLevel = 2;
	public boolean showHud = true;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("abilitytoolkit-client.json");
	private static ClientConfig instance = new ClientConfig();

	public static ClientConfig get() {
		return instance;
	}

	public boolean get(Ability ability) {
		return switch (ability) {
			case NO_KNOCKBACK -> noKnockback;
			case FLIGHT -> flight;
			case SPEED -> speed;
		};
	}

	public void set(Ability ability, boolean value) {
		switch (ability) {
			case NO_KNOCKBACK -> noKnockback = value;
			case FLIGHT -> flight = value;
			case SPEED -> speed = value;
		}
	}

	public static void load() {
		if (!Files.exists(PATH)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(PATH)) {
			ClientConfig loaded = GSON.fromJson(reader, ClientConfig.class);
			if (loaded != null) {
				loaded.speedLevel = AbilityLimits.clampLevel(loaded.speedLevel);
				instance = loaded;
			}
		} catch (IOException | RuntimeException e) {
			AbilityToolkit.LOGGER.warn("Could not read {}, using defaults", PATH, e);
			instance = new ClientConfig();
		}
	}

	public static void save() {
		try {
			Files.writeString(PATH, GSON.toJson(instance));
		} catch (IOException e) {
			AbilityToolkit.LOGGER.warn("Could not save {}", PATH, e);
		}
	}
}
