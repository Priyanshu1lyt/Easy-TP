package com.nebrion.easytp.gui;

import com.nebrion.easytp.config.ConfigManager;
import com.nebrion.easytp.player.PlayerData;
import com.nebrion.easytp.player.PlayerManager;
import com.nebrion.easytp.team.TeamData;
import com.nebrion.easytp.team.TeamManager;
import com.nebrion.easytp.util.ColourUtil;
import com.nebrion.easytp.util.Toasts;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

/**
 * Team dashboard: every team with its colour, member list, selection state and edit/delete actions.
 */
public final class TeamsScreen extends EasyTpScreen {
	private static final int TEAM_ROW_HEIGHT = 38;

	private int listX;
	private int listY;
	private int listW;
	private int listH;
	private int scroll;

	private String pendingDelete;
	private boolean deletePopupOpen;

	public TeamsScreen() {
		super(Text.literal("Teams"), null);
	}

	@Override
	protected void onFirstInit() {
		TeamManager.get().ensureServer(client);
		PlayerManager.get().refresh(client);
	}

	@Override
	protected void buildWidgets() {
		listY = contentY + 52;
		listX = contentX;
		listW = contentW;
		listH = Math.max(30, contentY + contentH - listY);
	}

	@Override
	protected int navIndex() {
		return 2;
	}

	@Override
	protected void drawContent(DrawContext context) {
		TeamManager teams = TeamManager.get();

		Theme.text(context, textRenderer, "TEAMS", contentX, contentY + 2, Theme.TEXT);
		Theme.fill(context, contentX, contentY + 14, 22, 2, Theme.ACCENT);
		Theme.textRight(context, textRenderer, teams.count() + " teams  ·  " + teams.selectedCount() + " selected",
				contentX + contentW, contentY + 4, Theme.TEXT_DIM);

		int createW = 96;
		button(context, "CREATE TEAM", contentX + contentW - createW, contentY + 22, createW, 16, ButtonKind.PRIMARY,
				() -> {
					if (client != null) {
						client.setScreen(new TeamEditorScreen(this, null));
					}
				});

		int toolbarWidth = contentW - createW - GAP - 16;
		int half = Math.max(50, toolbarWidth / 2);
		button(context, "SELECT ALL TEAMS", contentX + 8, contentY + 22, half - 2, 16, ButtonKind.SECONDARY, () -> {
			teams.selectAll();

			for (TeamData team : teams.selectedTeams()) {
				for (PlayerData member : teams.onlineMembers(team)) {
					PlayerManager.get().select(member.uuid());
				}
			}

			Toasts.info("Selected " + teams.selectedCount() + " teams.");
		});
		button(context, "CLEAR TEAM SELECTION", contentX + 8 + half + 2, contentY + 22, half - 2, 16,
				ButtonKind.GHOST, () -> {
					teams.clearSelection();
					Toasts.info("Team selection cleared.");
				});

		Theme.panel(context, listX, listY, listW, listH, panelAlpha());
		cardHeading(context, listX, listY, listW, "TEAM ROSTER",
				ConfigManager.get().settings().multiTeamMembership ? "multi-team membership" : "one team per player");

		int headerHeight = 24;
		int footerHeight = 13;
		int viewportY = listY + headerHeight;
		int viewportH = Math.max(10, listH - headerHeight - footerHeight);
		int contentHeight = teams.count() * TEAM_ROW_HEIGHT;
		scroll = clampScroll(scroll, contentHeight, viewportH);

		if (teams.count() == 0) {
			emptyState(context, listX, viewportY, listW, viewportH,
					"No teams yet. Create one to group players and teleport them together.");
		} else {
			context.enableScissor(listX + 1, viewportY, listX + listW - 1, viewportY + viewportH);

			for (int index = 0; index < teams.count(); index++) {
				int rowY = viewportY + index * TEAM_ROW_HEIGHT - scroll;

				if (rowY + TEAM_ROW_HEIGHT < viewportY || rowY > viewportY + viewportH) {
					continue;
				}

				drawTeamRow(context, teams.all().get(index), listX + 1, rowY, listW - 2, index);
			}

			context.disableScissor();
			scrollbar(context, listX + listW - 4, viewportY, viewportH, contentHeight, scroll);
		}

		Theme.text(context, textRenderer, "Click a team to select its online members  ·  footer buttons edit and delete",
				listX + 8, listY + listH - 11, Theme.TEXT_MUTED);
	}

	private void drawTeamRow(DrawContext context, TeamData team, int x, int y, int w, int index) {
		TeamManager teams = TeamManager.get();
		boolean selected = teams.isSelected(team.id());
		boolean hovered = contains(mouseX, mouseY, x, y, w, TEAM_ROW_HEIGHT);

		int background = index % 2 == 0 ? ColourUtil.withAlpha(0xFF16161B, 190) : ColourUtil.withAlpha(0xFF121216, 190);

		if (selected) {
			background = ColourUtil.withAlpha(team.color(), 38);
		}

		if (hovered) {
			background = ColourUtil.withAlpha(Theme.PANEL_HOVER, 190);
		}

		Theme.fill(context, x, y, w, TEAM_ROW_HEIGHT, background);
		Theme.fill(context, x, y, 3, TEAM_ROW_HEIGHT, team.color());
		Theme.fill(context, x, y + TEAM_ROW_HEIGHT - 1, w, 1, ColourUtil.withAlpha(Theme.BORDER, 120));

		int textX = x + 10;
		int nameColor = ColourUtil.brighten(team.color(), 0.35F);
		Theme.text(context, textRenderer, trim(team.name(), Math.max(40, w - 240)), textX, y + 6, nameColor);

		int chipX = textX + textRenderer.getWidth(trim(team.name(), Math.max(40, w - 240))) + 8;
		chip(context, team.memberCount() + " MEMBERS", chipX, y + 4, team.color());

		if (selected) {
			chip(context, "SELECTED", chipX + chipWidth(team.memberCount() + " MEMBERS") + 4, y + 4, Theme.ACCENT);
		}

		List<String> names = team.displayNames();
		String memberLine = names.isEmpty() ? "No members yet" : String.join(", ", names);
		Theme.text(context, textRenderer, trim(memberLine, Math.max(30, w - 260)), textX, y + 22,
				names.isEmpty() ? Theme.TEXT_MUTED : Theme.TEXT_DIM);

		int buttonY = y + (TEAM_ROW_HEIGHT - 16) / 2;
		int selectW = 52;
		int editW = 40;
		int deleteW = 52;
		int right = x + w - 6;

		button(context, "DELETE", right - deleteW, buttonY, deleteW, 16, ButtonKind.DANGER, () -> {
			pendingDelete = team.id();
			deletePopupOpen = true;
		});
		button(context, "EDIT", right - deleteW - 4 - editW, buttonY, editW, 16, ButtonKind.SECONDARY, () -> {
			if (client != null) {
				client.setScreen(new TeamEditorScreen(TeamsScreen.this, team));
			}
		});
		button(context, selected ? "UNSELECT" : "SELECT", right - deleteW - 4 - editW - 4 - selectW, buttonY, selectW, 16,
				selected ? ButtonKind.SELECTED : ButtonKind.GHOST, () -> toggleTeam(team));

		region(x, y, w - (selectW + editW + deleteW + 18), TEAM_ROW_HEIGHT, () -> toggleTeam(team));
	}

	private void toggleTeam(TeamData team) {
		TeamManager teams = TeamManager.get();
		teams.toggle(team.id());

		if (teams.isSelected(team.id())) {
			for (PlayerData member : teams.onlineMembers(team)) {
				PlayerManager.get().select(member.uuid());
			}

			Toasts.info(team.name() + " selected (" + teams.onlineMembers(team).size() + " online).");
		} else {
			for (UUID member : team.members()) {
				PlayerManager.get().deselect(member);
			}

			Toasts.info(team.name() + " unselected.");
		}
	}

	@Override
	protected boolean handleScroll(int mouseX, int mouseY, double amount) {
		if (!contains(mouseX, mouseY, listX, listY, listW, listH)) {
			return false;
		}

		int viewportH = Math.max(10, listH - 37);
		scroll = clampScroll(scroll - (int) (amount * TEAM_ROW_HEIGHT), TeamManager.get().count() * TEAM_ROW_HEIGHT, viewportH);
		return true;
	}

	@Override
	protected void drawOverlay(DrawContext context) {
		if (!deletePopupOpen || pendingDelete == null) {
			return;
		}

		TeamData team = TeamManager.get().byId(pendingDelete).orElse(null);

		if (team == null) {
			deletePopupOpen = false;
			return;
		}

		int popupW = Math.min(230, Math.max(170, contentW - 40));
		int popupH = 78;
		int popupX = contentX + (contentW - popupW) / 2;
		int popupY = contentY + (contentH - popupH) / 2;

		context.fill(0, 0, width, height, 0x90000000);
		Theme.panel(context, popupX, popupY, popupW, popupH, 1.0F);
		Theme.fill(context, popupX, popupY, popupW, 2, Theme.RED);

		Theme.text(context, textRenderer, "DELETE TEAM", popupX + 8, popupY + 8, Theme.TEXT);
		Theme.text(context, textRenderer, trim("\"" + team.name() + "\" and its membership data?",
				popupW - 16), popupX + 8, popupY + 22, Theme.TEXT_DIM);
		Theme.text(context, textRenderer, "The players themselves are never affected.", popupX + 8, popupY + 34, Theme.TEXT_MUTED);

		button(context, "CONFIRM DELETE", popupX + 8, popupY + 52, (popupW - 20) / 2, 16, ButtonKind.DANGER, () -> {
			for (UUID member : team.members()) {
				PlayerManager.get().deselect(member);
			}

			TeamManager.get().delete(team.id());
			deletePopupOpen = false;
			pendingDelete = null;
			Toasts.success("Team deleted.");
		});
		button(context, "CANCEL", popupX + 12 + (popupW - 20) / 2, popupY + 52, (popupW - 20) / 2, 16, ButtonKind.GHOST,
				() -> {
					deletePopupOpen = false;
					pendingDelete = null;
				});
	}

	@Override
	protected boolean handleOverlayClick(int mouseX, int mouseY) {
		if (!deletePopupOpen) {
			return false;
		}

		int popupW = Math.min(230, Math.max(170, contentW - 40));
		int popupH = 78;
		int popupX = contentX + (contentW - popupW) / 2;
		int popupY = contentY + (contentH - popupH) / 2;

		if (contains(mouseX, mouseY, popupX, popupY, popupW, popupH)) {
			return false;
		}

		deletePopupOpen = false;
		pendingDelete = null;
		return true;
	}
}
