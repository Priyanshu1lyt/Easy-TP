package com.nebrion.easytp.teleport;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

/**
 * Listens to server system messages so that rejected teleport commands can be reported as FAILED
 * instead of being silently marked as unconfirmed.
 */
public final class ChatWatcher {
	private ChatWatcher() {
	}

	public static void register() {
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
			if (!overlay && message != null) {
				try {
					TeleportManager.get().onServerMessage(message.getString());
				} catch (Throwable throwable) {
					com.nebrion.easytp.EasyTP.LOGGER.warn("Easy TP could not inspect a server message", throwable);
				}
			}

			return true;
		});
	}
}
