package com.nebrion.easytp;

import com.nebrion.easytp.config.ConfigManager;
import com.nebrion.easytp.keybind.KeybindManager;
import com.nebrion.easytp.player.PlayerManager;
import com.nebrion.easytp.teleport.ChatWatcher;
import com.nebrion.easytp.teleport.TeleportManager;
import com.nebrion.easytp.util.Toasts;
import com.nebrion.easytp.util.Ticker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Client entrypoint of Easy TP.
 */
public final class EasyTPClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ConfigManager.get().load();
		KeybindManager.register();
		ChatWatcher.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Ticker.tick();
			Toasts.tick();
			TeleportManager.get().tick();
			KeybindManager.tick(client);

			// Keep the online player list warm while no GUI is open so that the dashboard is instant.
			PlayerManager.get().autoRefresh(client);
		});

		EasyTP.LOGGER.info("Easy TP {} initialised (client side only).", EasyTP.VERSION);
	}
}
