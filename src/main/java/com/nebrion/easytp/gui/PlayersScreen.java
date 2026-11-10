package com.nebrion.easytp.gui;

import com.nebrion.easytp.config.ConfigManager;
import com.nebrion.easytp.player.PlayerData;
import com.nebrion.easytp.player.PlayerManager;
import com.nebrion.easytp.team.TeamData;
import com.nebrion.easytp.team.TeamManager;
import com.nebrion.easytp.teleport.TeleportManager;
import com.nebrion.easytp.util.ColourUtil;
import com.nebrion.easytp.util.Toasts;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Online player list with search, multi selection and team assignment.
 */
public final class PlayersScreen extends EasyTpScreen {
	private String search = "";
	private TextFieldWidget searchField;

	private int searchX;
	private int searchY;
	private int searchW;
	private int searchH;
	private int listX;
	private int listY;
	private int listW;
	private int listH;
	private int scroll;

	private boolean popupOpen;
	private UUID singleTarget;
	private int popupX;
	private int popupY;
	private int popupW;
	private int popupH;

	public PlayersScreen() {
		super(Text.literal("Players"), null);
	}

	public PlayersScreen(net.minecraft.client.gui.screen.Screen parent) {
		super(Text.literal("Players"), parent);
	}

	@Override
	protected void onFirstInit() {
		PlayerManager.get().refresh(client);
		TeamManager.get().ensureServer(client);
	}

	@Override
	protected void buildWidgets() {
		searchX = contentX + 8;
		searchY = contentY + 25;
		searchW = Math.max(60, contentW - 16);
		searchH = 16;

		searchField = textField(searchX + 5, searchY + 4, Math.max(40, searchW - 10), 10, search, 32, value -> {
			search = value;
			scroll = 0;
		});

		listX = contentX;
		listY = searchY + searchH + 46;
		listW = contentW;
		listH = Math.max(24, contentY + contentH - listY);
	}

	@Override
	protected int navIndex() {
		return 1;
	}

	@Override
	protected void drawContent(DrawContext context) {
		PlayerManager players = PlayerManager.get();
		TeamManager teams = TeamManager.get();
		List<PlayerData> filtered = filteredPlayers();

		Theme.text(context, textRenderer, "PLAYERS", contentX, contentY + 2, Theme.TEXT);
		Theme.fill(context, contentX, contentY + 14, 22, 2, Theme.ACCENT);
		Theme.textRight(context, textRenderer,
				players.onlineCount() + " online  ·  " + players.selectedCount() + " selected",
				contentX + contentW, contentY + 4, Theme.TEXT_DIM);

		drawFieldBox(context, searchX, searchY, searchW, searchH, searchField != null && searchField.isFocused());

		if (search.isBlank()) {
			Theme.text(context, textRenderer, "Search by username or team...", searchX + 5, searchY + 4, Theme.TEXT_MUTED);
		}

		int toolbarA = searchY + searchH + 4;
		int thirdW = (contentW - 16 - GAP * 2) / 3;
		button(context, "SELECT ALL", contentX + 8, toolbarA, thirdW, 16, ButtonKind.SECONDARY, () -> {
			players.selectAll();
			Toasts.info("Selected " + players.onlineCount() + " players.");
		});
		button(context, "DESELECT ALL", contentX + 8 + thirdW + GAP, toolbarA, thirdW, 16, ButtonKind.SECONDARY, () -> {
			players.clearSelection();
			teams.clearSelection();
			Toasts.info("Selection cleared.");
		});
		button(context, "REFRESH", contentX + 8 + (thirdW + GAP) * 2, toolbarA, thirdW, 16, ButtonKind.GHOST, () -> {
			players.refresh(client);
			Toasts.info("Player list refreshed.");
		});

		int toolbarB = toolbarA + 20;
		int halfW = (contentW - 16 - GAP) / 2;
		button(context, "ASSIGN TO TEAM", contentX + 8, toolbarB, halfW, 16, ButtonKind.SELECTED,
				() -> openAssignPopup(null));
		button(context, "REMOVE FROM TEAM", contentX + 8 + halfW + GAP, toolbarB, halfW, 16, ButtonKind.DANGER, () -> {
			if (players.selectedCount() == 0) {
				Toasts.warn("Select at least one player first.");
				return;
			}

			for (UUID uuid : players.selectedIds()) {
				teams.removeFromAllTeams(uuid);
			}

			Toasts.success("Removed " + players.selectedCount() + " players from their teams.");
		});

		Theme.panel(context, listX, listY, listW, listH, panelAlpha());
		cardHeading(context, listX, listY, listW, "ONLINE PLAYERS",
				filtered.size() + " shown  ·  " + players.selectedCount() + " selected");

		int headerHeight = 24;
		int footerHeight = 14;
		int viewportY = listY + headerHeight;
		int viewportH = Math.max(10, listH - headerHeight - footerHeight);
		int rowH = rowHeight();
		int contentHeight = filtered.size() * rowH;
		scroll = clampScroll(scroll, contentHeight, viewportH);

		if (filtered.isEmpty()) {
			emptyState(context, listX, viewportY, listW, viewportH, search.isBlank()
					? "No players are online."
					: "No player matches \"" + search + "\".");
		} else {
			context.enableScissor(listX + 1, viewportY, listX + listW - 1, viewportY + viewportH);

			for (int index = 0; index < filtered.size(); index++) {
				int rowY = viewportY + index * rowH - scroll;

				if (rowY + rowH < viewportY || rowY > viewportY + viewportH) {
					continue;
				}

				drawPlayerRow(context, filtered.get(index), listX + 1, rowY, listW - 2, rowH, index);
			}

			context.disableScissor();
			scrollbar(context, listX + listW - 4, viewportY, viewportH, contentHeight, scroll);
		}

		String footer = filtered.size() + " player" + (filtered.size() == 1 ? "" : "s")
				+ "  ·  click a row to select  ·  "
				+ (ConfigManager.get().settings().multiTeamMembership ? "multi-team mode" : "one team per player");
		Theme.text(context, textRenderer, trim(footer, listW - 16), listX + 8, listY + listH - 11, Theme.TEXT_MUTED);
	}

	private void drawPlayerRow(DrawContext context, PlayerData data, int x, int y, int w, int h, int index) {
		PlayerManager players = PlayerManager.get();
		TeamManager teams = TeamManager.get();
		boolean selected = players.isSelected(data.uuid());
		boolean hovered = contains(mouseX, mouseY, x, y, w, h);
		TeamData team = teams.primaryTeamOf(data.uuid());
		boolean destination = TeleportManager.get().destination() != null
				&& TeleportManager.get().destination().isPlayer()
				&& data.uuid().equals(TeleportManager.get().destination().playerId());

		int background = (index % 2 == 0) ? ColourUtil.withAlpha(0xFF16161B, 190) : ColourUtil.withAlpha(0xFF121216, 190);

		if (selected) {
			background = ColourUtil.withAlpha(ConfigManager.get().colors().selected, 40);
		}

		if (hovered) {
			background = ColourUtil.withAlpha(Theme.PANEL_HOVER, 220);
		}

		Theme.fill(context, x, y, w, h, background);

		int accent = team != null
				? team.color()
				: ConfigManager.get().colors().unselected;

		if (destination) {
			accent = ConfigManager.get().colors().destination;
		}

		Theme.fill(context, x, y, 2, h, accent);
		Theme.fill(context, x, y + h - 1, w, 1, ColourUtil.withAlpha(Theme.BORDER, 120));

		Theme.checkbox(context, x + 7, y + (h - 9) / 2, 9, selected, ConfigManager.get().colors().selected);

		int cursor = x + 21;

		if (ConfigManager.get().settings().showPlayerHeads && data.skinTextures() != null) {
			int size = Math.min(10, Math.max(6, h - 6));
			PlayerSkinDrawer.draw(context, data.skinTextures(), cursor, y + (h - size) / 2, size);
			cursor += size + 5;
		}

		int nameColor = destination
				? ConfigManager.get().colors().destination
				: selected ? ColourUtil.brighten(ConfigManager.get().colors().selected, 0.25F) : Theme.TEXT;

		String name = data.name();
		int nameMax = Math.max(30, w / 3);
		Theme.text(context, textRenderer, trim(name, nameMax), cursor, y + (h - 8) / 2, nameColor);
		cursor += Math.min(textRenderer.getWidth(name), nameMax) + 8;

		if (team != null && cursor + chipWidth(team.name()) < x + w - 110) {
			chip(context, team.name(), cursor, y + (h - 11) / 2, team.color());
			cursor += chipWidth(team.name()) + 6;
		}

		if (cursor + 60 < x + w) {
			String status = data.latency() >= 0 ? data.latency() + "ms" : "—";
			Theme.text(context, textRenderer, status, x + w - 118, y + (h - 8) / 2, latencyColour(data.latency()));

			if (ConfigManager.get().settings().showOnlineStatus) {
				Theme.dot(context, x + w - 128, y + (h - 4) / 2, 4, Theme.GREEN);
			}

			Theme.textRight(context, textRenderer, trim(data.gameMode(), 62), x + w - 8, y + (h - 8) / 2, Theme.TEXT_MUTED);
		}

		if (destination) {
			Theme.text(context, textRenderer, "DEST", x + w - 152, y + (h - 8) / 2, ConfigManager.get().colors().destination);
		}

		region(x, y, w, h, () -> PlayerManager.get().toggleSelection(data.uuid()));
	}

	private int latencyColour(int latency) {
		if (latency < 0) {
			return Theme.TEXT_MUTED;
		}

		if (latency <= 80) {
			return Theme.GREEN;
		}

		if (latency <= 160) {
			return Theme.YELLOW;
		}

		return Theme.RED;
	}

	private List<PlayerData> filteredPlayers() {
		List<PlayerData> list = new ArrayList<>();
		String needle = search.trim().toLowerCase();

		for (PlayerData data : PlayerManager.get().all()) {
			if (needle.isEmpty()) {
				list.add(data);
				continue;
			}

			if (data.name().toLowerCase().contains(needle)) {
				list.add(data);
				continue;
			}

			if (ConfigManager.get().settings().searchIncludesTeams) {
				for (TeamData team : TeamManager.get().teamsOf(data.uuid())) {
					if (team.name().toLowerCase().contains(needle)) {
						list.add(data);
						break;
					}
				}
			}
		}

		String mode = ConfigManager.get().settings().sortMode;

		if (ConfigManager.get().settings().sortMode.equals(com.nebrion.easytp.config.ModSettings.SORT_TEAM)) {
			list.sort(Comparator
					.comparing((PlayerData data) -> {
						TeamData team = TeamManager.get().primaryTeamOf(data.uuid());
						return team == null ? "~" : team.name().toLowerCase();
					})
					.thenComparing(PlayerData::name, String.CASE_INSENSITIVE_ORDER));
		} else if (mode.equals(com.nebrion.easytp.config.ModSettings.SORT_ONLINE)) {
			list.sort(Comparator
					.comparing((PlayerData data) -> !PlayerManager.get().isSelected(data.uuid()))
					.thenComparing(PlayerData::name, String.CASE_INSENSITIVE_ORDER));
		} else {
			list.sort(Comparator.comparing(PlayerData::name, String.CASE_INSENSITIVE_ORDER));
		}

		return list;
	}

	@Override
	protected boolean handleScroll(int mouseX, int mouseY, double amount) {
		if (!contains(mouseX, mouseY, listX, listY, listW, listH)) {
			return false;
		}

		int rowH = rowHeight();
		int contentHeight = filteredPlayers().size() * rowH;
		int viewportH = Math.max(10, listH - 38);
		scroll = clampScroll(scroll - (int) (amount * rowH * 2), contentHeight, viewportH);
		return true;
	}

	// --- team assignment popup -----------------------------------------------------------------

	private void openAssignPopup(UUID single) {
		boolean hasSelection = PlayerManager.get().selectedCount() > 0;

		if (single == null && !hasSelection) {
			Toasts.warn("Select at least one player first, then assign a team.");
			return;
		}

		singleTarget = single;
		popupOpen = true;

		int teams = TeamManager.get().count();
		popupW = Math.min(220, Math.max(150, contentW - 40));
		popupH = 30 + Math.max(1, teams) * 18 + 26;
		popupX = contentX + (contentW - popupW) / 2;
		popupY = contentY + Math.max(6, (contentH - popupH) / 2);
	}

	@Override
	protected void drawOverlay(DrawContext context) {
		if (!popupOpen) {
			return;
		}

		PlayerManager players = PlayerManager.get();
		TeamManager teams = TeamManager.get();

		context.fill(0, 0, width, height, 0x90000000);
		Theme.panel(context, popupX, popupY, popupW, popupH, 1.0F);
		Theme.fill(context, popupX, popupY, popupW, 2, Theme.ACCENT);

		String target = singleTarget == null
				? players.selectedCount() + " selected player" + (players.selectedCount() == 1 ? "" : "s")
				: players.get(singleTarget) != null ? players.get(singleTarget).name() : "player";
		Theme.text(context, textRenderer, "ASSIGN TO TEAM", popupX + 8, popupY + 8, Theme.TEXT);
		Theme.text(context, textRenderer, trim(target, popupW - 16), popupX + 8, popupY + 19, Theme.TEXT_MUTED);

		int y = popupY + 32;

		if (teams.count() == 0) {
			Theme.text(context, textRenderer, "No teams yet - create one first.", popupX + 8, y, Theme.TEXT_MUTED);
		}

		for (TeamData team : teams.all()) {
			boolean hovered = contains(mouseX, mouseY, popupX + 6, y, popupW - 12, 16);

			if (hovered) {
				Theme.fill(context, popupX + 6, y, popupW - 12, 16, Theme.PANEL_HOVER);
			}

			Theme.fill(context, popupX + 6, y, 2, 16, team.color());
			Theme.text(context, textRenderer, trim(team.name(), popupW - 80), popupX + 13, y + 4, Theme.TEXT);
			Theme.textRight(context, textRenderer, team.memberCount() + " members", popupX + popupW - 10, y + 4, Theme.TEXT_MUTED);

			region(popupX + 6, y, popupW - 12, 16, () -> {
				UUID id = singleTarget != null ? singleTarget : null;

				if (id != null) {
					assign(team.id(), id);
				} else {
					for (UUID selected : players.selectedIds()) {
						assign(team.id(), selected);
					}
				}

				popupOpen = false;
			});

			y += 18;
		}

		int footerY = popupY + popupH - 20;
		button(context, "REMOVE FROM ALL TEAMS", popupX + 6, footerY, (popupW - 18) / 2, 14, ButtonKind.DANGER, () -> {
			UUID id = singleTarget;

			if (id != null) {
				teams.removeFromAllTeams(id);
			} else {
				for (UUID selected : players.selectedIds()) {
					teams.removeFromAllTeams(selected);
				}
			}

			popupOpen = false;
			Toasts.success("Team assignment removed.");
		});
		button(context, "CLOSE", popupX + 12 + (popupW - 18) / 2, footerY, (popupW - 18) / 2, 14, ButtonKind.GHOST,
				() -> popupOpen = false);
	}

	private void assign(String teamId, UUID player) {
		PlayerData data = PlayerManager.get().get(player);
		TeamManager.get().addMember(teamId, player, data == null ? null : data.name());
	}

	@Override
	protected boolean handleOverlayClick(int mouseX, int mouseY) {
		if (!popupOpen) {
			return false;
		}

		if (contains(mouseX, mouseY, popupX, popupY, popupW, popupH)) {
			return false;
		}

		popupOpen = false;
		return true;
	}
}
