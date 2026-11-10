package com.nebrion.easytp.gui;

import com.nebrion.easytp.config.ColourSettings;
import com.nebrion.easytp.config.ConfigManager;
import com.nebrion.easytp.config.ModSettings;
import com.nebrion.easytp.keybind.KeybindManager;
import com.nebrion.easytp.player.PlayerManager;
import com.nebrion.easytp.team.TeamManager;
import com.nebrion.easytp.util.ColourUtil;
import com.nebrion.easytp.util.Toasts;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Settings: interface, colours, player list behaviour and teleport safety, all stored locally in
 * {@code config/easy_tp.json}.
 */
public final class SettingsScreen extends EasyTpScreen {
	private static final int ROW = 20;
	private static final int SECTION = 18;

	private int scroll;
	private int measuredHeight;

	public SettingsScreen() {
		super(Text.literal("Settings"), null);
	}

	@Override
	protected int navIndex() {
		return 4;
	}

	@Override
	protected void drawContent(DrawContext context) {
		ModSettings settings = ConfigManager.get().settings();
		ColourSettings colors = ConfigManager.get().colors();

		Theme.text(context, textRenderer, "SETTINGS", contentX, contentY + 2, Theme.TEXT);
		Theme.fill(context, contentX, contentY + 14, 22, 2, Theme.ACCENT);
		Theme.textRight(context, textRenderer, "saved locally · " + ConfigManager.get().file().getFileName(),
				contentX + contentW, contentY + 4, Theme.TEXT_MUTED);

		int top = contentY + 24;
		int viewportH = Math.max(40, contentH - 24 - 22);
		int w = contentW - 12;

		Theme.panel(context, contentX, top, contentW, viewportH, panelAlpha());

		int maxScroll = Math.max(0, measuredHeight - (viewportH - 12));
		scroll = Math.max(0, Math.min(maxScroll, scroll));

		context.enableScissor(contentX + 1, top + 1, contentX + contentW - 1, top + viewportH - 1);
		int y = top + 8 - scroll;
		int x = contentX + 8;

		y = section(context, x, y, "INTERFACE");
		y = toggleRow(context, x, y, w, "Animations", "smooth panel fade-in", settings.animations, () -> {
			settings.animations = !settings.animations;
			ConfigManager.get().requestSave();
		});
		y = stepperRow(context, x, y, w, "Background opacity", settings.backgroundOpacity + " / 255",
				() -> {
					settings.backgroundOpacity = Math.max(120, settings.backgroundOpacity - 15);
					ConfigManager.get().requestSave();
				}, () -> {
					settings.backgroundOpacity = Math.min(255, settings.backgroundOpacity + 15);
					ConfigManager.get().requestSave();
				});
		y = cycleRow(context, x, y, w, "GUI density / scale", settings.densityLabel(), () -> {
			settings.cycleDensity();
			ConfigManager.get().requestSave();
		});
		y = toggleRow(context, x, y, w, "Compact player list", "tighter rows", settings.compactPlayerList, () -> {
			settings.compactPlayerList = !settings.compactPlayerList;
			ConfigManager.get().requestSave();
		});
		y = toggleRow(context, x, y, w, "Show player heads", "avatars in lists", settings.showPlayerHeads, () -> {
			settings.showPlayerHeads = !settings.showPlayerHeads;
			ConfigManager.get().requestSave();
		});
		y = toggleRow(context, x, y, w, "Show online status", "ping dot and latency", settings.showOnlineStatus, () -> {
			settings.showOnlineStatus = !settings.showOnlineStatus;
			ConfigManager.get().requestSave();
		});

		y = section(context, x, y, "COLOURS");
		y = colourRow(context, x, y, w, "Unselected player colour", colors.unselected, () -> {
			colors.unselected = nextColour(colors.unselected);
			ConfigManager.get().requestSave();
		});
		y = colourRow(context, x, y, w, "Selected player colour", colors.selected, () -> {
			colors.selected = nextColour(colors.selected);
			ConfigManager.get().requestSave();
		});
		y = colourRow(context, x, y, w, "Destination colour", colors.destination, () -> {
			colors.destination = nextColour(colors.destination);
			ConfigManager.get().requestSave();
		});
		y = actionRow(context, x, y, w, "Reset status colours", "unselected, selected, destination", "RESET", () -> {
			ColourSettings defaults = new ColourSettings();
			colors.unselected = defaults.unselected;
			colors.selected = defaults.selected;
			colors.destination = defaults.destination;
			ConfigManager.get().requestSave();
			Toasts.success("Status colours reset.");
		});

		y = section(context, x, y, "PLAYERS");
		y = toggleRow(context, x, y, w, "Automatic player refresh", "every 2 seconds", settings.autoRefresh, () -> {
			settings.autoRefresh = !settings.autoRefresh;
			ConfigManager.get().requestSave();
		});
		y = cycleRow(context, x, y, w, "Player sorting", settings.sortLabel(), () -> {
			settings.cycleSort();
			ConfigManager.get().requestSave();
		});
		y = toggleRow(context, x, y, w, "Search includes team names", "match teams while searching",
				settings.searchIncludesTeams, () -> {
					settings.searchIncludesTeams = !settings.searchIncludesTeams;
					ConfigManager.get().requestSave();
				});
		y = toggleRow(context, x, y, w, "Allow multiple teams per player", "off = one team per player",
				settings.multiTeamMembership, () -> {
					settings.multiTeamMembership = !settings.multiTeamMembership;
					ConfigManager.get().requestSave();
					Toasts.info(settings.multiTeamMembership
							? "Players can now belong to several teams."
							: "Players belong to one team; assigning moves them.");
				});

		y = section(context, x, y, "TELEPORT & STORAGE");
		y = toggleRow(context, x, y, w, "Confirm before teleporting", "always ask before sending commands",
				settings.confirmBeforeTeleport, () -> {
					settings.confirmBeforeTeleport = !settings.confirmBeforeTeleport;
					ConfigManager.get().requestSave();
				});
		y = toggleRow(context, x, y, w, "Display execution results", "open the report after a teleport",
				settings.showResults, () -> {
					settings.showResults = !settings.showResults;
					ConfigManager.get().requestSave();
				});
		y = toggleRow(context, x, y, w, "Save teams automatically", "write changes to disk immediately",
				settings.autoSaveTeams, () -> {
					settings.autoSaveTeams = !settings.autoSaveTeams;
					ConfigManager.get().requestSave();
				});
		y = stepperRow(context, x, y, w, "Command spacing", settings.commandSpacingTicks + " ticks",
				() -> {
					settings.commandSpacingTicks = Math.max(1, settings.commandSpacingTicks - 1);
					ConfigManager.get().requestSave();
				}, () -> {
					settings.commandSpacingTicks = Math.min(20, settings.commandSpacingTicks + 1);
					ConfigManager.get().requestSave();
				});
		y = actionRow(context, x, y, w, "Save configuration now", "teams, colours and settings", "SAVE", () -> {
			TeamManager.get().persist();
			ConfigManager.get().saveNow();
			Toasts.success("Configuration saved.");
		});
		y = actionRow(context, x, y, w, "Reset all settings", "keeps your teams", "RESET", () -> {
			ConfigManager.get().root().settings = new ModSettings();
			ConfigManager.get().requestSave();
			Toasts.info("Settings restored to defaults.");
		});

		y = section(context, x, y, "PERMISSIONS");
		Theme.text(context, textRenderer, "Easy TP sends ordinary /tp commands. You need OP or command",
				x, y, Theme.TEXT_MUTED);
		Theme.text(context, textRenderer, "permissions on the server, and a server that allows those commands.",
				x, y + 10, Theme.TEXT_MUTED);
		Theme.text(context, textRenderer, "No server install is required, and permissions are never bypassed.",
				x, y + 20, Theme.TEXT_MUTED);
		y += 34;

		Theme.text(context, textRenderer, "Keybind: " + KeybindManager.describeKey()
				+ " (change it in Options → Controls → Easy TP)", x, y, Theme.TEXT_MUTED);
		y += ROW;

		context.disableScissor();
		scrollbar(context, contentX + contentW - 4, top + 1, viewportH - 2,
				measuredHeight + scroll, scroll);
		measuredHeight = y + scroll - (top + 8);

		Theme.text(context, textRenderer,
				PlayerManager.get().onlineCount() + " players online · " + TeamManager.get().count() + " teams · "
						+ "config/easy_tp.json",
				contentX, contentY + contentH - 10, Theme.TEXT_MUTED);
	}

	private int section(DrawContext context, int x, int y, String label) {
		Theme.text(context, textRenderer, label, x, y + 4, Theme.ACCENT);
		Theme.fill(context, x, y + 14, 16, 1, ColourUtil.withAlpha(Theme.ACCENT, 140));
		return y + SECTION;
	}

	private int toggleRow(DrawContext context, int x, int y, int w, String label, String hint, boolean value, Runnable onToggle) {
		boolean hovered = contains(mouseX, mouseY, x, y, w, ROW - 2);

		if (hovered) {
			Theme.fill(context, x, y, w, ROW - 2, ColourUtil.withAlpha(Theme.PANEL_HOVER, 140));
		}

		Theme.text(context, textRenderer, label, x + 4, y + 3, Theme.TEXT);
		Theme.text(context, textRenderer, hint, x + 4, y + 12, Theme.TEXT_MUTED);

		int switchW = 28;
		Theme.switchTrack(context, x + w - switchW - 6, y + 5, switchW, 10, value);
		region(x, y, w, ROW - 2, onToggle);
		return y + ROW;
	}

	private int cycleRow(DrawContext context, int x, int y, int w, String label, String value, Runnable onCycle) {
		boolean hovered = contains(mouseX, mouseY, x, y, w, ROW - 2);

		if (hovered) {
			Theme.fill(context, x, y, w, ROW - 2, ColourUtil.withAlpha(Theme.PANEL_HOVER, 140));
		}

		Theme.text(context, textRenderer, label, x + 4, y + 6, Theme.TEXT);
		int valueW = Math.min(w / 2, textRenderer.getWidth(value) + 24);
		Theme.fill(context, x + w - valueW - 6, y + 3, valueW, ROW - 8, Theme.PANEL_RAISED);
		Theme.border(context, x + w - valueW - 6, y + 3, valueW, ROW - 8, hovered ? Theme.ACCENT : Theme.BORDER);
		Theme.textCentered(context, textRenderer, trim(value, valueW - 8), x + w - valueW / 2 - 6, y + 6, Theme.TEXT_DIM);
		region(x, y, w, ROW - 2, onCycle);
		return y + ROW;
	}

	private int stepperRow(DrawContext context, int x, int y, int w, String label, String value, Runnable minus, Runnable plus) {
		Theme.text(context, textRenderer, label, x + 4, y + 6, Theme.TEXT);

		int buttonW = 16;
		int right = x + w - 6;
		button(context, "-", right - buttonW * 2 - 44, y + 3, buttonW, ROW - 8, ButtonKind.SECONDARY, minus);
		Theme.textCentered(context, textRenderer, trim(value, 44), right - 40, y + 6, Theme.TEXT_DIM);
		button(context, "+", right - buttonW, y + 3, buttonW, ROW - 8, ButtonKind.SECONDARY, plus);
		return y + ROW;
	}

	private int colourRow(DrawContext context, int x, int y, int w, String label, int color, Runnable cycle) {
		boolean hovered = contains(mouseX, mouseY, x, y, w, ROW - 2);

		if (hovered) {
			Theme.fill(context, x, y, w, ROW - 2, ColourUtil.withAlpha(Theme.PANEL_HOVER, 140));
		}

		Theme.text(context, textRenderer, label, x + 4, y + 6, Theme.TEXT);

		int swatch = ROW - 8;
		int right = x + w - 6;
		Theme.textRight(context, textRenderer, ColourUtil.toHex(color), right - swatch - 10, y + 6, Theme.TEXT_DIM);
		Theme.fill(context, right - swatch, y + 3, swatch, swatch, color);
		Theme.border(context, right - swatch, y + 3, swatch, swatch, hovered ? 0xFFFFFFFF : Theme.BORDER);
		region(x, y, w, ROW - 2, cycle);
		return y + ROW;
	}

	private int actionRow(DrawContext context, int x, int y, int w, String label, String hint, String buttonLabel, Runnable action) {
		Theme.text(context, textRenderer, label, x + 4, y + 3, Theme.TEXT);
		Theme.text(context, textRenderer, hint, x + 4, y + 12, Theme.TEXT_MUTED);
		int buttonW = 58;
		button(context, buttonLabel, x + w - buttonW - 6, y + 3, buttonW, ROW - 8, ButtonKind.SECONDARY, action);
		return y + ROW;
	}

	private int nextColour(int current) {
		int index = ColourUtil.PALETTE.indexOf(current);

		if (index < 0) {
			return ColourUtil.PALETTE.get(0);
		}

		return ColourUtil.PALETTE.get((index + 1) % ColourUtil.PALETTE.size());
	}

	@Override
	protected boolean handleScroll(int mouseX, int mouseY, double amount) {
		if (mouseX < contentX || mouseX > contentX + contentW) {
			return false;
		}

		int viewportH = Math.max(40, contentH - 24 - 22);
		int maxScroll = Math.max(0, measuredHeight - (viewportH - 12));
		scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (amount * ROW)));
		return true;
	}
}
