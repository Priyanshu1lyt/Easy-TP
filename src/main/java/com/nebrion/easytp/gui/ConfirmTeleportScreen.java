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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Final review before any command is sent. Everything is revalidated here: online state of the
 * targets, the destination, and the "destination is never its own target" rule.
 */
public final class ConfirmTeleportScreen extends EasyTpScreen {
	private final List<UUID> targets;
	private final Destination destination;

	private int listX;
	private int listY;
	private int listW;
	private int listH;
	private int sideX;
	private int sideW;
	private int scroll;

	public ConfirmTeleportScreen(Screen parent, List<UUID> targets, Destination destination) {
		super(Text.literal("Confirm teleport"), parent);
		this.targets = new ArrayList<>(targets);
		this.destination = destination;
	}

	@Override
	protected void onFirstInit() {
		PlayerManager.get().refresh(client);
	}

	@Override
	protected void buildWidgets() {
		int leftW = Math.max(160, (int) ((contentW - GAP) * 0.6));
		listX = contentX;
		listY = contentY + 48;
		listW = leftW;
		listH = Math.max(40, contentY + contentH - listY - 30);
		sideX = contentX + leftW + GAP;
		sideW = Math.max(120, contentW - leftW - GAP);
	}

	@Override
	protected int navIndex() {
		return -1;
	}

	@Override
	protected void drawContent(DrawContext context) {
		PlayerManager players = PlayerManager.get();

		Theme.text(context, textRenderer, "CONFIRM TELEPORT", contentX, contentY + 2, Theme.TEXT);
		Theme.fill(context, contentX, contentY + 14, 22, 2, Theme.ACCENT);
		Theme.textRight(context, textRenderer, targets.size() + " target" + (targets.size() == 1 ? "" : "s"),
				contentX + contentW, contentY + 4, Theme.TEXT_DIM);

		Theme.text(context, textRenderer, "These commands will be sent as you, through the normal command channel.",
				contentX, contentY + 24, Theme.TEXT_MUTED);

		// --- target list -----------------------------------------------------------------------
		Theme.panel(context, listX, listY - 22, listW, listH + 22, panelAlpha());
		cardHeading(context, listX, listY - 22, listW, "SELECTED PLAYERS", players.selectedCount() + " selected");

		int rowH = rowHeight();
		int contentHeight = targets.size() * rowH;
		scroll = clampScroll(scroll, contentHeight, listH);

		if (targets.isEmpty()) {
			emptyState(context, listX, listY, listW, listH, "No targets.");
		} else {
			context.enableScissor(listX + 1, listY, listX + listW - 1, listY + listH);

			for (int index = 0; index < targets.size(); index++) {
				int rowY = listY + index * rowH - scroll;

				if (rowY + rowH < listY || rowY > listY + listH) {
					continue;
				}

				drawTargetRow(context, targets.get(index), listX + 1, rowY, listW - 2, rowH, index);
			}

			context.disableScissor();
			scrollbar(context, listX + listW - 4, listY, listH, contentHeight, scroll);
		}

		// --- summary ---------------------------------------------------------------------------
		Theme.panel(context, sideX, listY - 22, sideW, listH + 22, panelAlpha());
		cardHeading(context, sideX, listY - 22, sideW, "DESTINATION", destination.isPlayer() ? "player" : "coordinates");

		int boxY = listY - 14;
		int boxH = destination.isPlayer() ? 46 : 58;
		boolean destinationOnline = !destination.isPlayer() || PlayerManager.get().get(destination.playerId()) != null;

		Theme.fill(context, sideX + 8, boxY, sideW - 16, boxH,
				ColourUtil.withAlpha(destinationOnline ? Theme.GOLD : Theme.RED, 26));
		Theme.fill(context, sideX + 8, boxY, 3, boxH, destinationOnline ? Theme.GOLD : Theme.RED);

		if (destination.isPlayer()) {
			PlayerData data = PlayerManager.get().get(destination.playerId());
			int textX = sideX + 16;

			if (data != null && data.skinTextures() != null) {
				PlayerSkinDrawer.draw(context, data.skinTextures(), textX, boxY + 8, 16);
				textX += 22;
			}

			Theme.text(context, textRenderer, trim(destination.playerName(), sideW - (textX - sideX) - 10),
					textX, boxY + 8, ConfigManager.get().colors().destination);
			Theme.text(context, textRenderer, destinationOnline ? "ONLINE" : "OFFLINE - CHANGE DESTINATION",
					textX, boxY + 21, destinationOnline ? Theme.GREEN : Theme.RED);
			Theme.text(context, textRenderer, "destination player", textX, boxY + 32, Theme.TEXT_MUTED);
		} else {
			Theme.text(context, textRenderer, "X  " + com.nebrion.easytp.teleport.CommandBuilder.formatCoordinate(destination.x()),
					sideX + 16, boxY + 8, Theme.TEXT);
			Theme.text(context, textRenderer, "Y  " + com.nebrion.easytp.teleport.CommandBuilder.formatCoordinate(destination.y()),
					sideX + 16, boxY + 19, Theme.TEXT);
			Theme.text(context, textRenderer, "Z  " + com.nebrion.easytp.teleport.CommandBuilder.formatCoordinate(destination.z()),
					sideX + 16, boxY + 30, Theme.TEXT);
			Theme.text(context, textRenderer, "in " + destination.dimensionLabel(), sideX + 16, boxY + 43, Theme.TEXT_MUTED);
		}

		int infoY = boxY + boxH + 8;
		int onlineTargets = 0;

		for (UUID id : targets) {
			if (PlayerManager.get().get(id) != null
					&& !(destination.isPlayer() && destination.playerId().equals(id))) {
				onlineTargets++;
			}
		}

		Theme.text(context, textRenderer, "COMMANDS", sideX + 8, infoY, Theme.TEXT_MUTED);
		Theme.textRight(context, textRenderer, String.valueOf(onlineTargets), sideX + sideW - 8, infoY, Theme.TEXT);

		boolean destinationIsTarget = destination.isPlayer() && targets.contains(destination.playerId());
		Theme.text(context, textRenderer, "INVALID TARGETS", sideX + 8, infoY + 12, Theme.TEXT_MUTED);
		Theme.textRight(context, textRenderer, String.valueOf(targets.size() - onlineTargets), sideX + sideW - 8,
				infoY + 12, targets.size() - onlineTargets > 0 ? Theme.ORANGE : Theme.TEXT);

		if (destinationIsTarget) {
			Theme.text(context, textRenderer, trim("Destination is in the target list; it is",
					sideW - 16), sideX + 8, infoY + 28, Theme.YELLOW);
			Theme.text(context, textRenderer, "automatically excluded.", sideX + 8, infoY + 38, Theme.YELLOW);
		}

		int footerY = contentY + contentH - 18;
		button(context, "CONFIRM TELEPORT", contentX, footerY, 150, 18, ButtonKind.PRIMARY, this::confirm);
		button(context, "BACK", contentX + 156, footerY, 70, 18, ButtonKind.SECONDARY, this::close);
		button(context, "CANCEL", contentX + 232, footerY, 70, 18, ButtonKind.GHOST, () -> {
			if (client != null) {
				client.setScreen(new TeleportScreen(parent));
			}
		});
	}

	private void drawTargetRow(DrawContext context, UUID uuid, int x, int y, int w, int h, int index) {
		PlayerData data = PlayerManager.get().get(uuid);
		boolean online = data != null;
		boolean isDestination = destination.isPlayer() && destination.playerId().equals(uuid);
		TeamData team = online ? TeamManager.get().primaryTeamOf(uuid) : null;

		int background = index % 2 == 0 ? ColourUtil.withAlpha(0xFF16161B, 180) : ColourUtil.withAlpha(0xFF121216, 180);

		if (isDestination) {
			background = ColourUtil.withAlpha(Theme.RED, 34);
		}

		Theme.fill(context, x, y, w, h, background);
		Theme.fill(context, x, y, 2, h, isDestination ? Theme.RED : team != null ? team.color() : Theme.BORDER);

		int cursor = x + 8;

		if (online && ConfigManager.get().settings().showPlayerHeads && data.skinTextures() != null) {
			int size = Math.min(10, Math.max(6, h - 6));
			PlayerSkinDrawer.draw(context, data.skinTextures(), cursor, y + (h - size) / 2, size);
			cursor += size + 5;
		}

		String name = online ? data.name() : uuid.toString().substring(0, 8);
		Theme.text(context, textRenderer, trim(name, Math.max(30, w - 92)), cursor, y + (h - 8) / 2,
				online ? (isDestination ? Theme.RED : Theme.TEXT) : Theme.TEXT_MUTED);

		String status = isDestination ? "EXCLUDED" : online ? "READY" : "OFFLINE";
		Theme.textRight(context, textRenderer, status, x + w - 6, y + (h - 8) / 2,
				isDestination ? Theme.RED : online ? Theme.GREEN : Theme.ORANGE);
	}

	private void confirm() {
		if (client == null) {
			return;
		}

		PlayerManager.get().refresh(client);

		if (destination.isPlayer() && PlayerManager.get().get(destination.playerId()) == null) {
			Toasts.error("The destination player left the server. Pick another destination.");
			return;
		}

		List<UUID> valid = new ArrayList<>();

		for (UUID id : targets) {
			if (PlayerManager.get().get(id) == null) {
				continue;
			}

			if (destination.isPlayer() && destination.playerId().equals(id)) {
				continue;
			}

			valid.add(id);
		}

		if (valid.isEmpty()) {
			Toasts.error("None of the selected players are still online.");
			return;
		}

		TeleportManager.get().start(client, valid, destination);

		if (ConfigManager.get().settings().showResults) {
			client.setScreen(new ResultsScreen(parent));
		} else {
			Toasts.info("Sent " + valid.size() + " teleport command" + (valid.size() == 1 ? "" : "s") + ".");
			client.setScreen(new TeleportScreen(parent));
		}
	}

	@Override
	protected boolean handleScroll(int mouseX, int mouseY, double amount) {
		if (!contains(mouseX, mouseY, listX, listY, listW, listH)) {
			return false;
		}

		scroll = clampScroll(scroll - (int) (amount * rowHeight()), targets.size() * rowHeight(), listH);
		return true;
	}
}
