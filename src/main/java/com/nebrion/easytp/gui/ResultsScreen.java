package com.nebrion.easytp.gui;

import com.nebrion.easytp.EasyTP;
import com.nebrion.easytp.config.ConfigManager;
import com.nebrion.easytp.teleport.TeleportManager;
import com.nebrion.easytp.teleport.TeleportResult;
import com.nebrion.easytp.util.ColourUtil;
import com.nebrion.easytp.util.Toasts;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Execution report: what was requested, what the client could confirm, what failed and what could
 * not be verified. Only failed/unconfirmed operations can be retried.
 */
public final class ResultsScreen extends EasyTpScreen {
	private static final int RESULT_ROW_HEIGHT = 30;

	private int listX;
	private int listY;
	private int listW;
	private int listH;
	private int scroll;

	public ResultsScreen(Screen parent) {
		super(Text.literal("Teleport results"), parent);
	}

	@Override
	protected void buildWidgets() {
		listY = contentY + 84;
		listX = contentX;
		listW = contentW;
		listH = Math.max(40, contentY + contentH - listY - 26);
	}

	@Override
	protected int navIndex() {
		return -1;
	}

	@Override
	protected void drawContent(DrawContext context) {
		TeleportManager teleports = TeleportManager.get();
		List<TeleportResult> results = teleports.results();

		Theme.text(context, textRenderer, "TELEPORT RESULTS", contentX, contentY + 2, Theme.TEXT);
		Theme.fill(context, contentX, contentY + 14, 22, 2, Theme.ACCENT);
		Theme.textRight(context, textRenderer,
				teleports.lastDestination() == null ? "no destination" : teleports.lastDestination().describe(),
				contentX + contentW, contentY + 4, Theme.TEXT_DIM);

		int confirmed = teleports.confirmedCount();
		int failed = teleports.failedCount();
		int unconfirmed = teleports.unconfirmedCount();
		int total = teleports.totalCount();

		int cardWidth = Math.max(60, (contentW - GAP * 3) / 4);
		drawStat(context, contentX, contentY + 24, cardWidth, "REQUESTED", total, Theme.BLUE);
		drawStat(context, contentX + cardWidth + GAP, contentY + 24, cardWidth, "CONFIRMED", confirmed, Theme.GREEN);
		drawStat(context, contentX + (cardWidth + GAP) * 2, contentY + 24, cardWidth, "FAILED", failed, Theme.RED);
		drawStat(context, contentX + (cardWidth + GAP) * 3, contentY + 24, cardWidth, "UNCONFIRMED",
				unconfirmed, Theme.GOLD);

		Theme.panel(context, listX, listY, listW, listH, panelAlpha());
		cardHeading(context, listX, listY, listW, "OPERATIONS",
				teleports.isBusy() ? "sending..." : "see status legend below");

		int headerHeight = 24;
		int footerHeight = 13;
		int viewportY = listY + headerHeight;
		int viewportH = Math.max(10, listH - headerHeight - footerHeight);
		int contentHeight = results.size() * RESULT_ROW_HEIGHT;
		scroll = clampScroll(scroll, contentHeight, viewportH);

		if (results.isEmpty()) {
			emptyState(context, listX, viewportY, listW, viewportH,
					"No teleport has been executed in this session yet.");
		} else {
			context.enableScissor(listX + 1, viewportY, listX + listW - 1, viewportY + viewportH);

			for (int index = 0; index < results.size(); index++) {
				int rowY = viewportY + index * RESULT_ROW_HEIGHT - scroll;

				if (rowY + RESULT_ROW_HEIGHT < viewportY || rowY > viewportY + viewportH) {
					continue;
				}

				drawResultRow(context, results.get(index), listX + 1, rowY, listW - 2, index);
			}

			context.disableScissor();
			scrollbar(context, listX + listW - 4, viewportY, viewportH, contentHeight, scroll);
		}

		Theme.text(context, textRenderer,
				"GREEN confirmed  ·  RED failed  ·  GOLD unconfirmed or pending  ·  success is verified from your client's view",
				listX + 8, listY + listH - 11, Theme.TEXT_MUTED);

		int footerY = contentY + contentH - 18;
		int footerW = Math.max(52, (contentW - GAP * 3) / 4);
		button(context, "RETRY FAILED", contentX, footerY, footerW, 18, ButtonKind.SECONDARY,
				failed + unconfirmed > 0, () -> {
					TeleportManager.get().retryFailed(client);
					scroll = 0;

					if (!ConfigManager.get().settings().showResults && client != null) {
						client.setScreen(new TeleportScreen(this));
					}
				});
		button(context, "NEW TELEPORT", contentX + footerW + GAP, footerY, footerW, 18, ButtonKind.PRIMARY, () -> {
			if (client != null) {
				client.setScreen(new TeleportScreen(this));
			}
		});
		button(context, "DASHBOARD", contentX + (footerW + GAP) * 2, footerY, footerW, 18, ButtonKind.GHOST, () -> {
			if (client != null) {
				client.setScreen(new MainDashboardScreen());
			}
		});
		button(context, "CLOSE", contentX + (footerW + GAP) * 3, footerY, footerW, 18, ButtonKind.GHOST, () -> {
			if (client != null) {
				client.setScreen(null);
			}
		});

		if (contentW > 470) {
			Theme.textRight(context, textRenderer, "Easy TP · " + EasyTP.serverLabel(client), contentX + contentW,
					footerY + 5, Theme.TEXT_MUTED);
		}
	}

	private void drawStat(DrawContext context, int x, int y, int w, String label, int value, int color) {
		Theme.panel(context, x, y, w, 32, panelAlpha());
		Theme.fill(context, x, y, 2, 32, color);
		Theme.text(context, textRenderer, label, x + 8, y + 5, Theme.TEXT_MUTED);
		Theme.text(context, textRenderer, String.valueOf(value), x + 8, y + 16, color);
	}

	private void drawResultRow(DrawContext context, TeleportResult result, int x, int y, int w, int index) {
		int color = result.status().color();
		int background = index % 2 == 0 ? ColourUtil.withAlpha(0xFF16161B, 180) : ColourUtil.withAlpha(0xFF121216, 180);

		Theme.fill(context, x, y, w, RESULT_ROW_HEIGHT, background);
		Theme.fill(context, x, y, 2, RESULT_ROW_HEIGHT, color);

		Theme.dot(context, x + 8, y + 7, 4, color);
		Theme.text(context, textRenderer, trim(result.targetName(), Math.max(30, w / 3)), x + 17, y + 5, Theme.TEXT);

		String status = result.status().label();
		int chipWidth = chipWidth(status);
		Theme.fill(context, x + w - chipWidth - 6, y + 4, chipWidth, 11, ColourUtil.withAlpha(color, 0x33));
		Theme.fill(context, x + w - chipWidth - 6, y + 4, 2, 11, color);
		Theme.text(context, textRenderer, status, x + w - chipWidth - 1, y + 6, ColourUtil.brighten(color, 0.35F));

		String message = result.message() == null ? "" : result.message();
		Theme.text(context, textRenderer, trim(message, w - 20), x + 17, y + 17, Theme.TEXT_MUTED);

		if (contains(mouseX, mouseY, x, y, w, RESULT_ROW_HEIGHT)) {
			Theme.fill(context, x, y, w, RESULT_ROW_HEIGHT, ColourUtil.withAlpha(Theme.PANEL_HOVER, 90));
			String command = "/" + result.command();
			Theme.text(context, textRenderer, trim(command, w - 20), x + 17, y + RESULT_ROW_HEIGHT - 10, Theme.TEXT_DIM);
		}
	}

	@Override
	protected boolean handleScroll(int mouseX, int mouseY, double amount) {
		if (!contains(mouseX, mouseY, listX, listY, listW, listH)) {
			return false;
		}

		scroll = clampScroll(scroll - (int) (amount * RESULT_ROW_HEIGHT),
				TeleportManager.get().totalCount() * RESULT_ROW_HEIGHT, Math.max(10, listH - 37));
		return true;
	}
}
