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
			String world = "world";

			if (client.world != null) {
				world = client.world.getRegistryKey().getValue().getPath();
			}

			return "singleplayer:" + world;
		}

		ServerInfo info = client.getCurrentServerEntry();

		if (info != null && info.address != null && !info.address.isBlank()) {
			return "multiplayer:" + info.address;
		}

		return "multiplayer:unknown";
	}

	/** Human readable label of the current server/world, used by the dashboard chrome. */
	public static String serverLabel(MinecraftClient client) {
		if (client == null) {
			return "Not connected";
		}

		if (client.isInSingleplayer()) {
			return "Singleplayer";
		}

		ServerInfo info = client.getCurrentServerEntry();

		if (info != null && info.address != null && !info.address.isBlank()) {
			return info.address;
		}

		return "Multiplayer";
	}
}
