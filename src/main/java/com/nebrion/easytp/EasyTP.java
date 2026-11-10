package com.nebrion.easytp;

import com.nebrion.easytp.config.ConfigManager;
import com.nebrion.easytp.player.PlayerManager;
import com.nebrion.easytp.team.TeamManager;
import com.nebrion.easytp.teleport.TeleportManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central access point for Easy TP. Holds the mod-wide constants and the lazy singletons used by
 * the GUI, the managers and the configuration layer.
 *
 * <p>Easy TP is a <strong>client side only</strong> mod: it never touches the server side of the game,
 * it only sends ordinary chat commands that the server is free to accept or reject.</p>
 */
public final class EasyTP {
	public static final String MOD_ID = "easy_tp";
	public static final String MOD_NAME = "Easy TP";
	public static final String VERSION = "1.0.0";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	private EasyTP() {
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	public static MinecraftClient client() {
		return MinecraftClient.getInstance();
	}

	public static ConfigManager config() {
		return ConfigManager.get();
	}

	public static PlayerManager players() {
		return PlayerManager.get();
	}

	public static TeamManager teams() {
		return TeamManager.get();
	}

	public static TeleportManager teleports() {
		return TeleportManager.get();
	}

	/**
	 * Identity of the server/world the operator is currently connected to. Team storage is separated
	 * by this key so that two different servers never share team membership.
	 */
	public static String serverKey(MinecraftClient client) {
		if (client == null) {
			return "unknown";
		}

		if (client.isInSingleplayer()) {
			return "singleplayer:" + singleplayerWorldName(client);
		}

		ServerInfo info = client.getCurrentServerEntry();

		if (info != null && info.address != null && !info.address.isBlank()) {
			return "multiplayer:" + info.address;
		}

		return "multiplayer:unknown";
	}

	/**
	 * Name of the integrated server's save, so that team storage stays stable while travelling between
	 * dimensions in singleplayer. Falls back to a shared profile when it cannot be resolved.
	 */
	private static String singleplayerWorldName(MinecraftClient client) {
		try {
			if (client.getServer() != null) {
				String level = client.getServer().getSaveProperties().getLevelName();

				if (level != null && !level.isBlank()) {
					return level;
				}
			}
		} catch (Throwable throwable) {
			LOGGER.debug("Could not resolve the singleplayer world name", throwable);
		}

		return "shared";
	}

	/** Human readable label of the current server/world, used by the dashboard chrome. */
	public static String serverLabel(MinecraftClient client) {
		if (client == null) {
			return "Not connected";
		}

		if (client.isInSingleplayer()) {
			String world = singleplayerWorldName(client);
			return "shared".equals(world) ? "Singleplayer" : "Singleplayer · " + world;
		}

		ServerInfo info = client.getCurrentServerEntry();

		if (info != null && info.address != null && !info.address.isBlank()) {
			return info.address;
		}

		return "Multiplayer";
	}
}
