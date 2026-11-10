package com.nebrion.easytp.keybind;

import com.nebrion.easytp.EasyTP;
import com.nebrion.easytp.gui.EasyTpScreen;
import com.nebrion.easytp.gui.MainDashboardScreen;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

/**
 * Registers the "Open Easy TP Dashboard" keybind (default P, rebindable in vanilla Controls) and
 * opens the dashboard exactly once per press.
 */
public final class KeybindManager {
	public static final String CATEGORY_ID = "general";

	private static KeyBinding openDashboard;
	private static boolean warnedAboutCategory;

	private KeybindManager() {
	}

	public static void register() {
		openDashboard = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.easy_tp.open",
				GLFW.GLFW_KEY_P,
				KeyBinding.Category.create(EasyTP.id(CATEGORY_ID))
		));
	}

	public static KeyBinding openKey() {
		return openDashboard;
	}

	public static void tick(MinecraftClient client) {
		if (openDashboard == null) {
			return;
		}

		while (openDashboard.wasPressed()) {
			openDashboard(client);
		}
	}

	private static void openDashboard(MinecraftClient client) {
		if (client.player == null) {
			return;
		}

		// Never stack two Easy TP screens on top of each other.
		if (client.currentScreen instanceof EasyTpScreen) {
			return;
		}

		client.setScreen(new MainDashboardScreen());
	}

	/** Used by the settings screen to describe the currently bound key. */
	public static String describeKey() {
		if (openDashboard == null) {
			return "P";
		}

		return openDashboard.getBoundKeyLocalizedText().getString();
	}
}
