package com.nebrion.easytp.gui;

import com.nebrion.easytp.EasyTP;
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

import java.util.List;
import java.util.UUID;

/**
 * Destination mode A: teleport the selected players/teams to another online player.
 */
public final class DestinationScreen extends EasyTpScreen {
	private int listX;
	private int listY;
	private int listW;
	private int listH;
	private int panelX;
	private int panelW;
	private int scroll;

	private UUID pending;
	private String pendingName;

	public DestinationScreen(TeleportScreen parent) {
		super(Text.literal("Player destination"), parent);

		Destination current = TeleportManager.get().destination();

		if (current != null && current.isPlayer()) {
			pending = current.playerId();
			pendingName = current.playerName();
		}
	}

	@Override
	protected void onFirstInit() {
		PlayerManager.get().refresh(client);
	}

	@Override
	protected void buildWidgets() {
		int leftW = Math.max(170, (int) ((contentW - GAP) * 0.58));
		listX = contentX;
		listY = contentY + 46;
		listW = leftW;
		listH = Math.max(40, contentY + contentH - listY - 26);
		panelX = contentX + leftW + GAP;
		panelW = Math.max(120, contentW - leftW - GAP);
	}

	@Override
	protected int navIndex() {
		return -1;
	}

	@Override
	protected void drawContent(DrawContext context) {
		List<PlayerData> online = PlayerManager.get().sortedByName();

		Theme.text(context, textRenderer, "PLAYER DESTINATION", contentX, contentY + 2, Theme.TEXT);
		Theme.fill(context, contentX, contentY + 14, 22, 2, Theme.GOLD);
		Theme.textRight(context, textRenderer, "Where should the selected players go?",
				contentX + contentW, contentY + 4, Theme.TEXT_DIM);

		Theme.panel(context, listX, listY - 22, listW, listH + 22, panelAlpha());
		cardHeading(context, listX, listY - 22, listW, "CHOOSE A PLAYER", online.size() + " online");

		int rowH = Math.max(18, rowHeight() + 2);
		int contentHeight = online.size() * rowH;
		scroll = clampScroll(scroll, contentHeight, listH);

		if (online.isEmpty()) {
			emptyState(context, listX, listY, listW, listH, "No players are online.");
		} else {
			context.enableScissor(listX + 1, listY, listX + listW - 1, listY + listH);

			for (int index = 0; index < online.size(); index++) {
				int rowY = listY + index * rowH - scroll;

				if (rowY + rowH < listY || rowY > listY + listH) {
					continue;
				}

				drawPlayerRow(context, online.get(index), listX + 1, rowY, listW - 2, rowH, index);
			}

			context.disableScissor();
			scrollbar(context, listX + listW - 4, listY, listH, contentHeight, scroll);
		}

		// --- destination card ------------------------------------------------------------------
		Theme.panel(context, panelX, listY - 22, panelW, listH + 22, panelAlpha());
		cardHeading(context, panelX, listY - 22, panelW, "DESTINATION", pending == null ? "none" : "gold");

		int boxY = listY + 2;
		Theme.fill(context, panelX + 8, boxY, panelW - 16, 54, ColourUtil.withAlpha(Theme.GOLD, 26));
		Theme.fill(context, panelX + 8, boxY, 3, 54, Theme.GOLD);

		if (pending == null) {
			Theme.text(context, textRenderer, "NO DESTINATION SELECTED", panelX + 16, boxY + 10, Theme.GOLD);
			Theme.text(context, textRenderer, "Pick a player from the list.", panelX + 16, boxY + 26, Theme.TEXT_MUTED);
		} else {
			PlayerData data = PlayerManager.get().get(pending);
			int headX = panelX + 16;

			if (data != null && data.skinTextures() != null) {
				PlayerSkinDrawer.draw(context, data.skinTextures(), headX, boxY + 10, 16);
			}

			int textX = headX + (data != null ? 22 : 0);
			String name = data != null ? data.name() : pendingName;
			Theme.text(context, textRenderer, trim(name == null ? "Unknown" : name, panelW - 60), textX, boxY + 10,
					ConfigManager.get().colors().destination);
			Theme.text(context, textRenderer, "DESTINATION SELECTED", textX, boxY + 24, Theme.GOLD);
			Theme.text(context, textRenderer, data != null ? data.gameMode() : "offline", textX, boxY + 36,
					Theme.TEXT_MUTED);
		}

		boolean isTarget = pending != null && (PlayerManager.get().isSelected(pending)
				|| TeleportManager.get().resolveTargets().contains(pending));

		if (isTarget) {
			Theme.text(context, textRenderer, trim("This player is also a target - they will be",
					panelW - 16), panelX + 8, boxY + 62, Theme.YELLOW);
			Theme.text(context, textRenderer, "excluded from their own teleport.", panelX + 8, boxY + 72, Theme.YELLOW);
		}

		int bottom = listY + listH;
		button(context, "CONFIRM DESTINATION", panelX + 8, bottom - 38, panelW - 16, 17, ButtonKind.PRIMARY, () -> {
			if (pending == null) {
				Toasts.warn("Select a destination player first.");
				return;
			}

			PlayerData data = PlayerManager.get().get(pending);

			if (data == null) {
				Toasts.error("That player is no longer online; choose another destination.");
				return;
			}

			TeleportManager.get().setDestination(Destination.player(data.uuid(), data.name()));
			Toasts.success("Destination set to " + data.name() + ".");
			close();
		});
		button(context, "COORDINATES INSTEAD", panelX + 8, bottom - 19, panelW - 16, 16, ButtonKind.GHOST, () -> {
			if (client != null) {
				net.minecraft.client.gui.screen.Screen back = parent != null ? parent : this;
				client.setScreen(new CoordinateScreen(back));
			}
		});

		Theme.text(context, textRenderer, trim("Operator: " + EasyTP.serverLabel(client), panelW - 16),
				panelX + 8, bottom + 3, Theme.TEXT_MUTED);
	}

	private void drawPlayerRow(DrawContext context, PlayerData data, int x, int y, int w, int h, int index) {
		boolean chosen = pending != null && pending.equals(data.uuid());
		boolean hovered = contains(mouseX, mouseY, x, y, w, h);
		TeamData team = TeamManager.get().primaryTeamOf(data.uuid());

		int background = index % 2 == 0 ? ColourUtil.withAlpha(0xFF16161B, 180) : ColourUtil.withAlpha(0xFF121216, 180);

		if (chosen) {
			background = ColourUtil.withAlpha(ConfigManager.get().colors().destination, 50);
		}

		if (hovered) {
			background = ColourUtil.withAlpha(Theme.PANEL_HOVER, 210);
		}

		Theme.fill(context, x, y, w, h, background);
		Theme.fill(context, x, y, 2, h, chosen ? ConfigManager.get().colors().destination
				: team != null ? team.color() : Theme.BORDER);

		int cursor = x + 8;

		if (ConfigManager.get().settings().showPlayerHeads && data.skinTextures() != null) {
			int size = Math.min(12, Math.max(6, h - 6));
			PlayerSkinDrawer.draw(context, data.skinTextures(), cursor, y + (h - size) / 2, size);
			cursor += size + 5;
		}

		Theme.text(context, textRenderer, trim(data.name(), Math.max(30, w - 120)), cursor, y + (h - 8) / 2,
				chosen ? ConfigManager.get().colors().destination : Theme.TEXT);

		if (ConfigManager.get().settings().showOnlineStatus) {
			Theme.dot(context, x + w - 54, y + (h - 4) / 2, 4, Theme.GREEN);
			Theme.text(context, textRenderer, data.latency() >= 0 ? data.latency() + "ms" : "—", x + w - 46,
					y + (h - 8) / 2, Theme.TEXT_MUTED);
		}

		if (chosen) {
			Theme.textRight(context, textRenderer, "GOLD", x + w - 6, y + (h - 8) / 2,
					ConfigManager.get().colors().destination);
		}

		region(x, y, w, h, () -> {
			pending = data.uuid();
			pendingName = data.name();
		});
	}

	@Override
	protected boolean handleScroll(int mouseX, int mouseY, double amount) {
		if (!contains(mouseX, mouseY, listX, listY, listW, listH)) {
			return false;
		}

		int rowH = Math.max(18, rowHeight() + 2);
		scroll = clampScroll(scroll - (int) (amount * rowH), PlayerManager.get().onlineCount() * rowH, listH);
		return true;
	}
}
