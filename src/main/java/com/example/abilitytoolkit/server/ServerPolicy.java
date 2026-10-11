package com.example.abilitytoolkit.server;

import com.example.abilitytoolkit.AbilityLimits;
import com.example.abilitytoolkit.AbilityToolkit;
import com.example.abilitytoolkit.payload.PermissionPayload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Decides who may use which ability. This is the permission gate that keeps the mod from bypassing server rules.
 *
 * <ul>
 *   <li>Dedicated server: DEFAULT DENY. Only UUIDs listed in {@code config/abilitytoolkit-server.json} get access.</li>
 *   <li>Integrated server (singleplayer / open-to-LAN): allowed by default, because the host owns the world.</li>
 * </ul>
 */
public final class ServerPolicy {
	/** Plain data class that Gson reads/writes. Field names are the JSON keys. */
	static final class Config {
		/** On an integrated server, allow everyone on it (the host). Set false to require the list below. */
		boolean integratedServerAllowAll = true;
		/** UUIDs (with dashes) of players authorized on a dedicated server. */
		List<String> authorizedPlayers = new ArrayList<>();
		boolean allowNoKnockback = true;
		boolean allowFlight = true;
		boolean allowSpeed = true;
		/** Highest speed level a player may use on a dedicated server (1-10, level 4 = x3.0). */
		int maxSpeedLevel = 4;
		/** Highest speed level on an integrated server. */
		int integratedMaxSpeedLevel = 10;
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Config config = new Config();
	private static boolean integrated;

	private ServerPolicy() {
	}

	public static void load(MinecraftServer server) {
		integrated = !server.isDedicatedServer();
		Path path = FabricLoader.getInstance().getConfigDir().resolve("abilitytoolkit-server.json");
		try {
			if (Files.exists(path)) {
				try (Reader reader = Files.newBufferedReader(path)) {
					Config loaded = GSON.fromJson(reader, Config.class);
					config = loaded != null ? loaded : new Config();
				}
			} else {
				config = new Config();
				Files.writeString(path, GSON.toJson(config));
			}
		} catch (IOException | RuntimeException e) {
			AbilityToolkit.LOGGER.error("Could not read {}, using safe defaults", path, e);
			config = new Config();
		}
		if (config.authorizedPlayers == null) {
			config.authorizedPlayers = new ArrayList<>();
		}
		AbilityToolkit.LOGGER.info("Server policy loaded ({} server, {} authorized players listed)",
				integrated ? "integrated" : "dedicated", config.authorizedPlayers.size());
	}

	public static PermissionPayload permissionsFor(ServerPlayer player) {
		String uuid = player.getUUID().toString();
		boolean listed = config.authorizedPlayers.stream().anyMatch(entry -> entry.equalsIgnoreCase(uuid));
		boolean authorized = listed || (integrated && config.integratedServerAllowAll);
		if (!authorized) {
			return PermissionPayload.NONE;
		}
		int maxLevel = integrated ? config.integratedMaxSpeedLevel : config.maxSpeedLevel;
		return new PermissionPayload(
				config.allowNoKnockback,
				config.allowFlight,
				config.allowSpeed,
				AbilityLimits.clampLevel(maxLevel));
	}
}
