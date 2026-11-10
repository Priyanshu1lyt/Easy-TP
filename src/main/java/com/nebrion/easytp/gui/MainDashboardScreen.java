package com.nebrion.easytp.gui;

import com.nebrion.easytp.EasyTP;
import com.nebrion.easytp.config.ConfigManager;
import com.nebrion.easytp.keybind.KeybindManager;
import com.nebrion.easytp.player.PlayerManager;
import com.nebrion.easytp.team.TeamManager;
import com.nebrion.easytp.teleport.CoordinateManager;
import com.nebrion.easytp.teleport.Destination;
import com.nebrion.easytp.teleport.TeleportManager;
import com.nebrion.easytp.teleport.TeleportResult;
import com.nebrion.easytp.util.ColourUtil;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Landing screen of the dashboard: live statistics, quick actions and a session summary.
 */
public final class MainDashboardScreen extends EasyTpScreen {
	public MainDashboardScreen() {
		super(Text.literal("Easy TP"), null);
	}

	@Override
	protected void onFirstInit() {
		PlayerManager.get().refresh(client);
		TeamManager.get().ensureServer(client);
	}

	@Override
	protected int navIndex() {
		return 0;
	}

	@Override
	protected void drawContent(DrawContext context) {
		PlayerManager players = PlayerManager.get();
		TeamManager teams = TeamManager.get();
		TeleportManager teleports = TeleportManager.get();

		drawTitle(context);

		int top = contentY + 26;
		int cardWidth = Math.max(70, (contentW - GAP * 3) / 4);
		int cardHeight = 50;

		drawStatCard(context, contentX, top, cardWidth, cardHeight, "ONLINE PLAYERS",
				String.valueOf(players.onlineCount()), Theme.BLUE, "in this server");
		drawStatCard(context, contentX + (cardWidth + GAP), top, cardWidth, cardHeight, "SELECTED",
				String.valueOf(players.selectedCount() + teamSelectedMembers()), Theme.GREEN, "ready to teleport");
		drawStatCard(context, contentX + (cardWidth + GAP) * 2, top, cardWidth, cardHeight, "TEAMS",
				String.valueOf(teams.count()), Theme.PURPLE, teams.selectedCount() + " selected");
		drawStatCard(context, contentX + (cardWidth + GAP) * 3, top, cardWidth, cardHeight, "DESTINATION",
				teleports.destination() == null ? "—" : (teleports.destination().isPlayer() ? "PLAYER" : "COORDS"),
				Theme.GOLD, teleports.destination() == null ? "not chosen yet" : teleports.destination().describe());

		int belowTop = top + cardHeight + GAP;
		int available = contentY + contentH - belowTop - GAP;
		int leftWidth = Math.max(150, (contentW - GAP) * 3 / 5);
		int rightWidth = Math.max(120, contentW - GAP - leftWidth);
		int actionsHeight = Math.max(90, Math.min(available, 26 + 4 * 24));

		drawActions(context, contentX, belowTop, leftWidth, actionsHeight);
		drawSession(context, contentX + leftWidth + GAP, belowTop, rightWidth, Math.min(available, actionsHeight));

		int resultsTop = belowTop + Math.min(available, actionsHeight) + GAP;
		int resultsHeight = contentY + contentH - resultsTop;

		if (resultsHeight >= 34) {
			drawLastTeleport(context, contentX, resultsTop, contentW, resultsHeight, teleports);
		}
	}

	private void drawTitle(DrawContext context) {
		Theme.text(context, textRenderer, "DASHBOARD", contentX, contentY + 2, Theme.TEXT);
		Theme.fill(context, contentX, contentY + 14, 22, 2, Theme.ACCENT);

		String user = client != null && client.getSession() != null ? client.getSession().getUsername() : "operator";
		Theme.text(context, textRenderer, "Welcome back, " + user + " · press "
						+ KeybindManager.describeKey() + " to toggle this dashboard",
				contentX + 30, contentY + 4, Theme.TEXT_MUTED);

		String server = EasyTP.serverLabel(client) + "  ·  "
				+ CoordinateManager.currentDimensionLabel(client);
		Theme.textRight(context, textRenderer, trim(server, Math.max(80, contentW / 3)), contentX + contentW,
				contentY + 4, Theme.TEXT_DIM);
	}

	private int teamSelectedMembers() {
		int total = 0;

		for (String id : TeamManager.get().selectedIds()) {
			total += TeamManager.get().byId(id).map(team -> team.onlineMemberCount()).orElse(0);
		}

		return total;
	}

	private void drawStatCard(DrawContext context, int x, int y, int w, int h, String label, String value, int color, String hint) {
		Theme.panel(context, x, y, w, h, panelAlpha());
		Theme.fill(context, x, y, 2, h, color);

		Theme.text(context, textRenderer, label, x + 8, y + 7, Theme.TEXT_MUTED);
		Theme.text(context, textRenderer, trim(value, w - 14), x + 8, y + 19, color);
		Theme.text(context, textRenderer, trim(hint, w - 14), x + 8, y + h - 12, Theme.TEXT_MUTED);
	}

	private void drawActions(DrawContext context, int x, int y, int w, int h) {
		Theme.panel(context, x, y, w, h, panelAlpha());
		cardHeading(context, x, y, w, "QUICK ACTIONS", null);

		int buttonY = y + 26;
		int half = (w - 16 - GAP) / 2;

		button(context, "PLAYER MANAGER", x + 8, buttonY, half, 18, ButtonKind.SECONDARY, () -> navigate(1));
		button(context, "TEAM MANAGER", x + 8 + half + GAP, buttonY, half, 18, ButtonKind.SECONDARY, () -> navigate(2));
		button(context, "TELEPORT CONSOLE", x + 8, buttonY + 22, half, 18, ButtonKind.PRIMARY, () -> navigate(3));
		button(context, "SETTINGS", x + 8 + half + GAP, buttonY + 22, half, 18, ButtonKind.SECONDARY, () -> navigate(4));

		if (h >= 96) {
			button(context, "SELECT ALL ONLINE PLAYERS", x + 8, buttonY + 44, w - 16, 18, ButtonKind.GHOST, () -> {
				PlayerManager.get().selectAll();
				com.nebrion.easytp.util.Toasts.info("Selected " + PlayerManager.get().onlineCount() + " players.");
			});
			button(context, "CLEAR EVERY SELECTION", x + 8, buttonY + 66, w - 16, 18, ButtonKind.GHOST, () -> {
				PlayerManager.get().clearSelection();
				TeamManager.get().clearSelection();
				com.nebrion.easytp.util.Toasts.info("Selection cleared.");
			});
		}
	}

	private void drawSession(DrawContext context, int x, int y, int w, int h) {
		Theme.panel(context, x, y, w, h, panelAlpha());
		cardHeading(context, x, y, w, "CURRENT SESSION", null);

		int line = y + 26;
		row(context, x, w, line, "SERVER", EasyTP.serverLabel(client));
		line += 14;
		row(context, x, w, line, "DIMENSION", CoordinateManager.currentDimensionLabel(client));
		line += 14;
		row(context, x, w, line, "YOU", client != null && client.getSession() != null
				? client.getSession().getUsername()
				: "operator");
		line += 14;
		row(context, x, w, line, "KEYBIND", KeybindManager.describeKey());
		line += 14;

		if (line + 10 < y + h) {
			row(context, x, w, line, "TEAM STORAGE",
					ConfigManager.get().settings().autoSaveTeams ? "auto saving" : "manual saving");
		}
	}

	private void row(DrawContext context, int x, int w, int y, String label, String value) {
		Theme.text(context, textRenderer, label, x + 8, y, Theme.TEXT_MUTED);
		Theme.textRight(context, textRenderer, trim(value, Math.max(40, w - 90)), x + w - 8, y, Theme.TEXT_DIM);
	}

	private void drawLastTeleport(DrawContext context, int x, int y, int w, int h, TeleportManager teleports) {
		Theme.panel(context, x, y, w, h, panelAlpha());
		cardHeading(context, x, y, w, "LAST TELEPORT", teleports.lastDestination() == null
				? "no teleport yet this session"
				: teleports.lastDestination().describe());

		if (!teleports.hasResults()) {
			emptyState(context, x, y + 18, w, h - 18, "Select players or a team, pick a destination, then review and teleport.");
			return;
		}

		int confirmed = teleports.confirmedCount();
		int failed = teleports.failedCount();
		int unconfirmed = teleports.unconfirmedCount();
		int total = teleports.totalCount();

		int lineY = y + 26;
		String summary = total + " requested   ·   " + confirmed + " confirmed   ·   " + failed
				+ " failed   ·   " + unconfirmed + " unconfirmed";
		Theme.text(context, textRenderer, trim(summary, w - 130), x + 8, lineY, Theme.TEXT_DIM);

		progressBar(context, x + 8, lineY + 12, Math.max(40, w - 130), 3,
				total == 0 ? 0 : confirmed / (float) total, Theme.GREEN);

		button(context, "VIEW RESULTS", x + w - 112, lineY - 4, 104, 18, ButtonKind.SECONDARY,
				() -> {
					if (client != null) {
						client.setScreen(new ResultsScreen(this));
					}
				});

		if (h >= 58) {
			String destination = teleports.lastDestination() == null ? "unknown" : teleports.lastDestination().describe();
			Theme.text(context, textRenderer, trim("Destination: " + destination, w - 20), x + 8, lineY + 20, Theme.TEXT_MUTED);
		}
	}

	@SuppressWarnings("unused")
	private int statusColour(TeleportResult.Status status) {
		return status.color();
	}

	@SuppressWarnings("unused")
	private int destinationColour(Destination destination) {
		return destination.isPlayer() ? Theme.GOLD : ColourUtil.brighten(Theme.GOLD, 0.2F);
	}
}
