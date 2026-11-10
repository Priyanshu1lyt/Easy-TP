package com.nebrion.easytp.gui;

import com.nebrion.easytp.config.ConfigManager;
import com.nebrion.easytp.player.PlayerData;
import com.nebrion.easytp.player.PlayerManager;
import com.nebrion.easytp.team.TeamData;
import com.nebrion.easytp.team.TeamManager;
import com.nebrion.easytp.teleport.Destination;
import com.nebrion.easytp.teleport.TeleportManager;
import com.nebrion.easytp.util.ColourUtil;
import com.nebrion.easytp.util.Toasts;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Teleport console: pick targets (players and/or teams) on the left, choose and review the
 * destination on the right.
 */
public final class TeleportScreen extends EasyTpScreen {
	private int leftX;
	private int leftY;
	private int leftW;
	private int leftH;
	private int rightX;
	private int rightY;
	private int rightW;
	private int rightH;

	private int listY;
	private int listH;
	private int scroll;

	public TeleportScreen() {
		super(Text.literal("Teleport"), null);
	}

	public TeleportScreen(net.minecraft.client.gui.screen.Screen parent) {
		super(Text.literal("Teleport"), parent);
	}

	@Override
	protected void onFirstInit() {
		PlayerManager.get().refresh(client);
		TeamManager.get().ensureServer(client);
	}

	@Override
	protected void buildWidgets() {
		leftX = contentX;
		leftY = contentY + 24;
		leftW = Math.max(170, (int) ((contentW - GAP) * 0.56));
		leftH = Math.max(80, contentH - 24);
		rightX = leftX + leftW + GAP;
		rightY = leftY;
		rightW = Math.max(120, contentW - leftW - GAP);
		rightH = leftH;

		listY = leftY + 24 + teamRowsHeight() + 4;
		listH = Math.max(24, leftY + leftH - listY - 24);
	}

	private int teamRowsHeight() {
		int teams = TeamManager.get().count();

		if (teams == 0) {
			return 0;
		}

		return Math.min(2, (teams * 60) / Math.max(1, leftW) + 1) * 16;
	}

	@Override
	protected int navIndex() {
		return 3;
	}

	@Override
	protected void drawContent(DrawContext context) {
		PlayerManager players = PlayerManager.get();
		TeamManager teams = TeamManager.get();
		TeleportManager teleports = TeleportManager.get();

		Theme.text(context, textRenderer, "TELEPORT CONSOLE", contentX, contentY + 2, Theme.TEXT);
		Theme.fill(context, contentX, contentY + 14, 22, 2, Theme.ACCENT);
		Theme.textRight(context, textRenderer,
				teleports.resolveTargets().size() + " targets  ·  " + teams.selectedCount() + " teams selected",
				contentX + contentW, contentY + 4, Theme.TEXT_DIM);

		// --- targets ---------------------------------------------------------------------------
		Theme.panel(context, leftX, leftY, leftW, leftH, panelAlpha());
		cardHeading(context, leftX, leftY, leftW, "TARGETS", players.selectedCount() + " players selected");

		int chipY = leftY + 23;
		drawTeamChips(context, chipY);

		int playerListTop = listY;
		int playerListBottom = leftY + leftH - 24;

		if (playerListTop < playerListBottom) {
			List<PlayerData> online = players.sortedByName();
			int rowH = rowHeight();
			int contentHeight = online.size() * rowH;
			scroll = clampScroll(scroll, contentHeight, playerListBottom - playerListTop);

			if (online.isEmpty()) {
				emptyState(context, leftX, playerListTop, leftW, playerListBottom - playerListTop,
						"No players online on this server.");
			} else {
				context.enableScissor(leftX + 1, playerListTop, leftX + leftW - 1, playerListBottom);

				for (int index = 0; index < online.size(); index++) {
					int rowY = playerListTop + index * rowH - scroll;

					if (rowY + rowH < playerListTop || rowY > playerListBottom) {
						continue;
					}

					drawTargetRow(context, online.get(index), leftX + 1, rowY, leftW - 2, rowH, index);
				}

				context.disableScissor();
				scrollbar(context, leftX + leftW - 4, playerListTop, playerListBottom - playerListTop, contentHeight, scroll);
			}
		}

		button(context, "SELECT ALL", leftX + 8, leftY + leftH - 20, (leftW - 22) / 2, 14, ButtonKind.GHOST,
				() -> players.selectAll());
		button(context, "CLEAR", leftX + 8 + (leftW - 22) / 2 + GAP, leftY + leftH - 20, (leftW - 22) / 2, 14,
				ButtonKind.GHOST, () -> {
					players.clearSelection();
					teams.clearSelection();
				});

		// --- destination and actions -----------------------------------------------------------
		Theme.panel(context, rightX, rightY, rightW, rightH, panelAlpha());
		cardHeading(context, rightX, rightY, rightW, "DESTINATION", null);

		Destination destination = teleports.destination();
		int boxY = rightY + 24;
		int boxH = 42;
		Theme.fill(context, rightX + 8, boxY, rightW - 16, boxH, ColourUtil.withAlpha(Theme.GOLD, 24));
		Theme.fill(context, rightX + 8, boxY, 3, boxH, Theme.GOLD);

		if (destination == null) {
			Theme.text(context, textRenderer, "NO DESTINATION", rightX + 16, boxY + 6, Theme.GOLD);
			Theme.text(context, textRenderer, "Choose a player or coordinates", rightX + 16, boxY + 18, Theme.TEXT_MUTED);
			Theme.text(context, textRenderer, "before teleporting.", rightX + 16, boxY + 28, Theme.TEXT_MUTED);
		} else {
			Theme.text(context, textRenderer, destination.isPlayer() ? "PLAYER DESTINATION" : "COORDINATE DESTINATION",
					rightX + 16, boxY + 6, Theme.GOLD);
			Theme.text(context, textRenderer, trim(destination.describe(), rightW - 30), rightX + 16, boxY + 18, Theme.TEXT);
			Theme.text(context, textRenderer, destination.isPlayer() ? "ready to receive players" : destination.dimensionLabel(),
					rightX + 16, boxY + 29, Theme.TEXT_MUTED);
		}

		int tabY = boxY + boxH + 6;
		int tabW = (rightW - 16 - 4) / 2;
		boolean playerMode = destination == null || destination.isPlayer();
		tab(context, "PLAYER", rightX + 8, tabY, tabW, 16, playerMode, () -> {
			if (client != null) {
				client.setScreen(new DestinationScreen(this));
			}
		});
		tab(context, "COORDINATES", rightX + 8 + tabW + 4, tabY, tabW, 16, !playerMode, () -> {
			if (client != null) {
				client.setScreen(new CoordinateScreen(this));
			}
		});

		int summaryY = tabY + 22;
		Theme.text(context, textRenderer, "TARGETS", rightX + 8, summaryY, Theme.TEXT_MUTED);
		Theme.textRight(context, textRenderer, String.valueOf(teleports.resolveTargets().size()),
				rightX + rightW - 8, summaryY, Theme.TEXT);

		String teamLine = teams.selectedCount() == 0
				? "none"
				: String.join(", ", teleports.selectedTeamNames());
		Theme.text(context, textRenderer, "TEAMS", rightX + 8, summaryY + 12, Theme.TEXT_MUTED);
		Theme.textRight(context, textRenderer, trim(teamLine, rightW - 70), rightX + rightW - 8, summaryY + 12, Theme.TEXT_DIM);

		// Action buttons pinned to the bottom of the card.
		int actionBottom = rightY + rightH - 8;
		int actionHeight = 17;
		int gap = 3;
		int primaryW = rightW - 16;
		int halfW = (rightW - 16 - gap) / 2;

		int y = actionBottom - actionHeight;
		button(context, "REVIEW & TELEPORT", rightX + 8, y, primaryW, actionHeight, ButtonKind.PRIMARY,
				() -> review(teleports.resolveTargets()));

		y -= actionHeight + gap;
		button(context, "TP ALL PLAYERS", rightX + 8, y, halfW, actionHeight, ButtonKind.SECONDARY,
				() -> review(allOnline()));
		button(context, "TP SELECTED PLAYERS", rightX + 8 + halfW + gap, y, halfW, actionHeight, ButtonKind.SECONDARY,
				() -> review(players.selectedIds()));

		y -= actionHeight + gap;
		button(context, "TP SELECTED TEAMS", rightX + 8, y, halfW, actionHeight, ButtonKind.SECONDARY,
				() -> review(teamMembers()));
		button(context, "CHANGE DESTINATION", rightX + 8 + halfW + gap, y, halfW, actionHeight, ButtonKind.GHOST,
				() -> {
					if (client != null) {
						client.setScreen(destination != null && destination.isCoordinates()
								? new CoordinateScreen(this)
								: new DestinationScreen(this));
					}
				});

		y -= actionHeight + gap;
		button(context, "CLEAR SELECTION", rightX + 8, y, primaryW, actionHeight, ButtonKind.GHOST, () -> {
			players.clearSelection();
			teams.clearSelection();
			Toasts.info("Selection cleared.");
		});
	}

	private void drawTeamChips(DrawContext context, int y) {
		TeamManager teams = TeamManager.get();

		if (teams.count() == 0) {
			Theme.text(context, textRenderer, "No teams yet - create one in TEAMS.", leftX + 8, y + 3, Theme.TEXT_MUTED);
			return;
		}

		int x = leftX + 8;
		int row = 0;
		int maxRows = 2;

		for (TeamData team : teams.all()) {
			String label = trim(team.name(), 70);
			int width = chipWidth(label) + 12;

			if (x + width > leftX + leftW - 8) {
				row++;
				x = leftX + 8;

				if (row >= maxRows) {
					Theme.text(context, textRenderer, "...", leftX + leftW - 14, y + 2, Theme.TEXT_MUTED);
					return;
				}
			}

			int chipY = y + row * 16;
			boolean selected = teams.isSelected(team.id());
			boolean hovered = contains(mouseX, mouseY, x, chipY, width, 13);

			Theme.fill(context, x, chipY, width, 13,
					selected ? ColourUtil.withAlpha(team.color(), 90) : hovered ? Theme.PANEL_HOVER : Theme.PANEL_ALT);
			Theme.border(context, x, chipY, width, 13, selected ? team.color() : Theme.BORDER);
			Theme.text(context, textRenderer, label, x + 5, chipY + 3, selected
					? ColourUtil.brighten(team.color(), 0.4F)
					: Theme.TEXT_DIM);
			Theme.text(context, textRenderer, String.valueOf(team.onlineMemberCount()),
					x + width - 12, chipY + 3, Theme.TEXT_MUTED);

			region(x, chipY, width, 13, () -> toggleTeam(team));
			x += width + 4;
		}
	}

	private void toggleTeam(TeamData team) {
		TeamManager teams = TeamManager.get();
		teams.toggle(team.id());

		if (teams.isSelected(team.id())) {
			for (PlayerData member : teams.onlineMembers(team)) {
				PlayerManager.get().select(member.uuid());
			}
		} else {
			for (UUID member : team.members()) {
				PlayerManager.get().deselect(member);
			}
		}
	}

	private void drawTargetRow(DrawContext context, PlayerData data, int x, int y, int w, int h, int index) {
		PlayerManager players = PlayerManager.get();
		TeamManager teams = TeamManager.get();
		boolean selected = players.isSelected(data.uuid());
		boolean hovered = contains(mouseX, mouseY, x, y, w, h);
		TeamData team = teams.primaryTeamOf(data.uuid());
		boolean destination = TeleportManager.get().destination() != null
				&& TeleportManager.get().destination().isPlayer()
				&& data.uuid().equals(TeleportManager.get().destination().playerId());

		int background = index % 2 == 0 ? ColourUtil.withAlpha(0xFF16161B, 180) : ColourUtil.withAlpha(0xFF121216, 180);

		if (selected) {
			background = ColourUtil.withAlpha(ConfigManager.get().colors().selected, 34);
		}

		if (hovered) {
			background = ColourUtil.withAlpha(Theme.PANEL_HOVER, 210);
		}

		Theme.fill(context, x, y, w, h, background);
		Theme.fill(context, x, y, 2, h, destination
				? ConfigManager.get().colors().destination
				: team != null ? team.color() : ConfigManager.get().colors().unselected);

		Theme.checkbox(context, x + 6, y + (h - 9) / 2, 9, selected, ConfigManager.get().colors().selected);

		int cursor = x + 19;

		if (ConfigManager.get().settings().showPlayerHeads && data.skinTextures() != null) {
			int size = Math.min(10, Math.max(6, h - 6));
			PlayerSkinDrawer.draw(context, data.skinTextures(), cursor, y + (h - size) / 2, size);
			cursor += size + 5;
		}

		int nameColor = destination
				? ConfigManager.get().colors().destination
				: selected ? ColourUtil.brighten(ConfigManager.get().colors().selected, 0.25F) : Theme.TEXT;
		Theme.text(context, textRenderer, trim(data.name(), Math.max(30, w / 2)), cursor, y + (h - 8) / 2, nameColor);

		if (destination) {
			Theme.textRight(context, textRenderer, "DEST", x + w - 6, y + (h - 8) / 2,
					ConfigManager.get().colors().destination);
		} else if (team != null && w > 140) {
			Theme.textRight(context, textRenderer, trim(team.name(), 56), x + w - 6, y + (h - 8) / 2,
					ColourUtil.brighten(team.color(), 0.2F));
		}

		region(x, y, w, h, () -> players.toggleSelection(data.uuid()));
	}

	private Collection<UUID> allOnline() {
		List<UUID> ids = new ArrayList<>();

		for (PlayerData data : PlayerManager.get().all()) {
			ids.add(data.uuid());
		}

		return ids;
	}

	private Collection<UUID> teamMembers() {
		LinkedHashSet<UUID> ids = new LinkedHashSet<>();

		for (TeamData team : TeamManager.get().selectedTeams()) {
			for (PlayerData member : TeamManager.get().onlineMembers(team)) {
				ids.add(member.uuid());
			}
		}

		if (ids.isEmpty()) {
			Toasts.warn("No team selected yet.");
		}

		return ids;
	}

	private void review(Collection<UUID> targets) {
		TeleportManager teleports = TeleportManager.get();
		List<UUID> ids = new ArrayList<>(targets);

		if (ids.isEmpty()) {
			Toasts.warn("Select at least one player or team first.");
			return;
		}

		if (ids.size() > 200) {
			Toasts.warn("Too many targets at once (" + ids.size() + "); teleport in smaller groups.");
			return;
		}

		if (teleports.destination() == null) {
			Toasts.warn("Choose a destination player or coordinates first.");
			return;
		}

		if (ConfigManager.get().settings().confirmBeforeTeleport && client != null) {
			client.setScreen(new ConfirmTeleportScreen(this, ids, teleports.destination()));
			return;
		}

		teleports.start(client, ids, teleports.destination());

		if (ConfigManager.get().settings().showResults && client != null) {
			client.setScreen(new ResultsScreen(this));
		}
	}

	@Override
	protected boolean handleScroll(int mouseX, int mouseY, double amount) {
		if (!contains(mouseX, mouseY, leftX, listY, leftW, listH)) {
			return false;
		}

		int contentHeight = PlayerManager.get().onlineCount() * rowHeight();
		scroll = clampScroll(scroll - (int) (amount * rowHeight()), contentHeight, listH);
		return true;
	}
}
