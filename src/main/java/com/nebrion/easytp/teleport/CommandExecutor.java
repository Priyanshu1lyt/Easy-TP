package com.nebrion.easytp.teleport;

import com.nebrion.easytp.EasyTP;

import net.minecraft.client.MinecraftClient;

/**
 * Sends ordinary chat commands through the vanilla command channel. The server decides whether the
 * operator is allowed to run them; Easy TP never bypasses permissions.
 */
public final class CommandExecutor {
	private CommandExecutor() {
	}

	public static boolean canSend(MinecraftClient client) {
		return client != null && client.player != null && client.getNetworkHandler() != null;
	}

	public static boolean send(MinecraftClient client, String command) {
		if (!canSend(client)) {
			EasyTP.LOGGER.warn("Cannot send /{}: no active connection", command);
			return false;
		}

		try {
			client.getNetworkHandler().sendChatCommand(command);
			return true;
		} catch (Throwable throwable) {
			EasyTP.LOGGER.error("Failed to send command /{}", command, throwable);
			return false;
		}
	}
}
