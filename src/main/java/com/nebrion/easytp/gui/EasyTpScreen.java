package com.nebrion.easytp.gui;

import com.nebrion.easytp.EasyTP;
import com.nebrion.easytp.config.ConfigManager;
import com.nebrion.easytp.player.PlayerManager;
import com.nebrion.easytp.team.TeamManager;
import com.nebrion.easytp.util.ColourUtil;
import com.nebrion.easytp.util.Toasts;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Shared chrome and drawing toolkit for every Easy TP screen: dark backdrop, branded header,
 * navigation rail, cards, buttons, checkboxes, scroll bars and toasts.
 *
 * <p>Interactions are registered as click regions while drawing, which keeps the layout code and
 * the hit testing in sync even when the window is resized.</p>
 */
public abstract class EasyTpScreen extends Screen {
	protected static final int OUTER = 8;
	protected static final int HEADER_HEIGHT = 32;
	protected static final int GAP = 6;

	private static final String[] NAV_LABELS = {"DASHBOARD", "PLAYERS", "TEAMS", "TELEPORT", "SETTINGS"};

	protected final Screen parent;
	protected int mouseX;
	protected int mouseY;

	protected int headerX;
	protected int headerY;
	protected int headerW;
	protected int headerH;
	protected int sidebarX;
	protected int sidebarY;
	protected int sidebarW;
	protected int sidebarH;
	protected int contentX;
	protected int contentY;
	protected int contentW;
	protected int contentH;

	private final List<Region> regions = new ArrayList<>();
	private int ticksOpen;
	private int refreshCooldown = 20;
	private boolean started;

	protected EasyTpScreen(Text title, Screen parent) {
		super(title);
		this.parent = parent;
	}

	// --- lifecycle -----------------------------------------------------------------------------

	@Override
	protected void init() {
		computeLayout();
		buildWidgets();

		if (!started) {
			started = true;
			onFirstInit();
		}
	}

	protected void computeLayout() {
		int margin = OUTER;
		headerX = margin;
		headerY = margin;
		headerW = Math.max(120, width - margin * 2);
		headerH = HEADER_HEIGHT;

		int top = headerY + headerH + GAP;
		sidebarX = margin;
		sidebarY = top;
		sidebarW = Math.max(84, Math.min(116, width / 7));
		sidebarH = Math.max(60, height - top - margin);

		contentX = sidebarX + sidebarW + GAP;
		contentY = top;
		contentW = Math.max(80, width - contentX - margin);
		contentH = sidebarH;
	}

	/** Called once, after the first layout pass. */
	protected void onFirstInit() {
	}

	/** Create the text fields (and any vanilla widgets) of this screen. */
	protected void buildWidgets() {
	}

	/** Navigation index highlighted in the rail, or -1 for sub screens. */
	protected abstract int navIndex();

	protected abstract void drawContent(DrawContext context);

	protected void drawOverlay(DrawContext context) {
	}

	/** Returns true when the overlay consumed the click (used to close popups). */
	protected boolean handleOverlayClick(int mouseX, int mouseY) {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		ticksOpen++;

		if (--refreshCooldown <= 0) {
			refreshCooldown = 20;

			if (ConfigManager.get().settings().autoRefresh) {
				PlayerManager.get().refresh(client);
				TeamManager.get().ensureServer(client);
				afterRefresh();
			}
		}
	}

	protected void afterRefresh() {
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public void close() {
		if (parent != null) {
			if (client != null) {
				client.setScreen(parent);
			}

			return;
		}

		super.close();
	}

	// --- rendering -----------------------------------------------------------------------------

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		// Easy TP paints its own backdrop; the vanilla blur/darkening would only add noise.
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.regions.clear();

		drawBackdrop(context);
		drawHeader(context);
		drawSidebar(context);
		drawContent(context);
		super.render(context, mouseX, mouseY, delta);
		drawToasts(context);
		drawOverlay(context);
	}

	private void drawBackdrop(DrawContext context) {
		context.fill(0, 0, width, height, Theme.BG);
		context.fillGradient(0, 0, width, height, Theme.BG_TOP, Theme.BG_BOTTOM);
		context.fillGradient(0, 0, width, 2, ColourUtil.withAlpha(Theme.ACCENT, 200), ColourUtil.withAlpha(Theme.ACCENT, 0));
	}

	private void drawHeader(DrawContext context) {
		Theme.panel(context, headerX, headerY, headerW, headerH, 1.0F);

		// Logo block
		Theme.fill(context, headerX + 7, headerY + 8, 16, 16, Theme.ACCENT);
		Theme.fill(context, headerX + 9, headerY + 10, 12, 12, ColourUtil.darken(Theme.ACCENT, 0.45F));
		Theme.fill(context, headerX + 11, headerY + 12, 8, 2, 0xFFFFFFFF);
		Theme.fill(context, headerX + 11, headerY + 16, 5, 2, 0xFFFFFFFF);

		Theme.text(context, textRenderer, "EASY TP", headerX + 29, headerY + 7, Theme.TEXT);
		Theme.text(context, textRenderer, "TELEPORT & PLAYER CONTROL", headerX + 29, headerY + 18, Theme.TEXT_MUTED);

		PlayerManager players = PlayerManager.get();
		TeamManager teams = TeamManager.get();
		String stats = "ONLINE " + players.onlineCount()
				+ "    SELECTED " + players.selectedCount()
				+ "    TEAMS " + teams.count();
		Theme.textRight(context, textRenderer, stats, headerX + headerW - 10, headerY + 7, Theme.TEXT_DIM);

		String right = EasyTP.serverLabel(client) + "  ·  " + (client != null && client.getSession() != null
				? client.getSession().getUsername()
				: "operator");
		Theme.textRight(context, textRenderer, Theme.trim(textRenderer, right, Math.max(60, headerW / 2)),
				headerX + headerW - 10, headerY + 18, Theme.TEXT_MUTED);
	}

	private void drawSidebar(DrawContext context) {
		Theme.panel(context, sidebarX, sidebarY, sidebarW, sidebarH, panelAlpha());

		int itemHeight = 22;
		int y = sidebarY + 8;

		for (int index = 0; index < NAV_LABELS.length; index++) {
			drawNavItem(context, index, NAV_LABELS[index], sidebarX + 6, y, sidebarW - 12, itemHeight);
			y += itemHeight + 3;
		}

		Theme.divider(context, sidebarX + 8, sidebarY + sidebarH - 30, sidebarW - 16);
		Theme.text(context, textRenderer, "v" + EasyTP.VERSION, sidebarX + 8, sidebarY + sidebarH - 23, Theme.TEXT_MUTED);
		Theme.text(context, textRenderer, "ESC to close", sidebarX + 8, sidebarY + sidebarH - 13, Theme.TEXT_MUTED);
	}

	private void drawNavItem(DrawContext context, int index, String label, int x, int y, int w, int h) {
		boolean active = navIndex() == index;
		boolean hovered = contains(mouseX, mouseY, x, y, w, h);

		int background = active
				? ColourUtil.withAlpha(Theme.ACCENT, 40)
				: hovered ? Theme.PANEL_HOVER : 0x00000000;

		if ((background >>> 24) != 0) {
			Theme.fill(context, x, y, w, h, background);
		}

		if (active) {
			Theme.fill(context, x, y, 2, h, Theme.ACCENT);
		}

		int textColor = active ? Theme.TEXT : hovered ? Theme.TEXT_DIM : Theme.TEXT_MUTED;
		Theme.text(context, textRenderer, label, x + 8, y + (h - 8) / 2, textColor);

		region(x, y, w, h, () -> navigate(index));
	}

	protected void navigate(int index) {
		if (client == null) {
			return;
		}

		switch (index) {
			case 0 -> client.setScreen(new MainDashboardScreen());
			case 1 -> client.setScreen(new PlayersScreen());
			case 2 -> client.setScreen(new TeamsScreen());
			case 3 -> client.setScreen(new TeleportScreen());
			case 4 -> client.setScreen(new SettingsScreen());
			default -> {
			}
		}
	}

	private void drawToasts(DrawContext context) {
		List<Toasts.Entry> entries = Toasts.visible();

		if (entries.isEmpty()) {
			return;
		}

		int y = height - OUTER - 4;

		for (int index = entries.size() - 1; index >= 0; index--) {
			Toasts.Entry entry = entries.get(index);
			String text = Theme.trim(textRenderer, entry.text(), Math.min(320, width / 2));
			int textWidth = textRenderer.getWidth(text);
			int boxWidth = textWidth + 18;
			int boxHeight = 16;
			y -= boxHeight + 3;
			int x = width - OUTER - boxWidth;

			int alpha = entry.life() < 20 ? (int) (255 * (entry.life() / 20.0F)) : 255;
			Theme.fill(context, x, y, boxWidth, boxHeight, ColourUtil.withAlpha(0xFF101015, alpha));
			Theme.border(context, x, y, boxWidth, boxHeight, ColourUtil.withAlpha(Theme.BORDER, alpha));
			Theme.fill(context, x, y, 2, boxHeight, ColourUtil.withAlpha(entry.color(), alpha));
			Theme.text(context, textRenderer, text, x + 8, y + 4, ColourUtil.withAlpha(entry.color(), alpha));
		}
	}

	// --- interaction ---------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == 0) {
			if (handleOverlayClick(mouseX, mouseY)) {
				return true;
			}

			for (int index = regions.size() - 1; index >= 0; index--) {
				Region region = regions.get(index);

				if (region.contains(mouseX, mouseY)) {
					if (region.action != null) {
						region.action.run();
					}

					return true;
				}
			}
		}

		return super.mouseClicked(click, doubled);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		this.mouseX = (int) mouseX;
		this.mouseY = (int) mouseY;
		super.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (handleScroll(this.mouseX, this.mouseY, vertical)) {
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	/** Screen-specific wheel handling. */
	protected boolean handleScroll(int mouseX, int mouseY, double amount) {
		return false;
	}

	// --- chrome helpers ------------------------------------------------------------------------

	protected float panelAlpha() {
		int opacity = ConfigManager.get().settings().backgroundOpacity;
		float target = Math.max(0.25F, Math.min(1.0F, opacity / 255.0F));

		if (!ConfigManager.get().settings().animations) {
			return target;
		}

		// Short, smooth reveal so screens do not pop in.
		float reveal = Math.min(1.0F, ticksOpen / 5.0F);
		return Math.max(0.15F, target * (0.55F + 0.45F * reveal));
	}

	protected int rowHeight() {
		return ConfigManager.get().settings().rowHeight();
	}

	protected int fontHeight() {
		return textRenderer.fontHeight;
	}

	protected boolean contains(int x, int y, int boxX, int boxY, int boxWidth, int boxHeight) {
		return x >= boxX && x < boxX + boxWidth && y >= boxY && y < boxY + boxHeight;
	}

	protected void region(int x, int y, int w, int h, Runnable action) {
		if (w <= 0 || h <= 0) {
			return;
		}

		regions.add(new Region(x, y, w, h, action));
	}

	protected TextFieldWidget textField(int x, int y, int w, int h, String initial, int maxLength, Consumer<String> changed) {
		TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, w, h, Text.literal(""));
		field.setMaxLength(maxLength);
		field.setDrawsBackground(false);
		field.setEditableColor(Theme.TEXT);
		field.setUneditableColor(Theme.TEXT_MUTED);
		field.setText(initial == null ? "" : initial);
		field.setChangedListener(value -> {
			if (changed != null) {
				changed.accept(value);
			}
		});
		addDrawableChild(field);
		return field;
	}

	protected void drawFieldBox(DrawContext context, int x, int y, int w, int h, boolean focused) {
		Theme.fill(context, x, y, w, h, Theme.PANEL_ALT);
		Theme.border(context, x, y, w, h, focused ? Theme.ACCENT : Theme.BORDER);
	}

	protected void text(DrawContext context, String value, int x, int y, int color) {
		Theme.text(context, textRenderer, value, x, y, color);
	}

	protected void textRight(DrawContext context, String value, int rightX, int y, int color) {
		Theme.textRight(context, textRenderer, value, rightX, y, color);
	}

	protected void textCentered(DrawContext context, String value, int centerX, int y, int color) {
		Theme.textCentered(context, textRenderer, value, centerX, y, color);
	}

	protected String trim(String value, int maxWidth) {
		return Theme.trim(textRenderer, value, maxWidth);
	}

	protected void chip(DrawContext context, String label, int x, int y, int color) {
		Theme.chip(context, textRenderer, label, x, y, color);
	}

	protected int chipWidth(String label) {
		return Theme.chipWidth(textRenderer, label);
	}

	protected void cardHeading(DrawContext context, int x, int y, int w, String heading, String hint) {
		Theme.text(context, textRenderer, heading, x + 8, y + 6, Theme.TEXT);
		Theme.fill(context, x + 8, y + 17, 14, 2, Theme.ACCENT);

		if (hint != null && !hint.isBlank()) {
			Theme.textRight(context, textRenderer, trim(hint, Math.max(40, w - 100)), x + w - 8, y + 6, Theme.TEXT_MUTED);
		}
	}

	protected void emptyState(DrawContext context, int x, int y, int w, int h, String message) {
		Theme.textCentered(context, textRenderer, trim(message, w - 20), x + w / 2, y + h / 2 - 4, Theme.TEXT_MUTED);
	}

	public enum ButtonKind {
		PRIMARY,
		SECONDARY,
		GHOST,
		DANGER,
		SUCCESS,
		SELECTED
	}

	protected void button(DrawContext context, String label, int x, int y, int w, int h, ButtonKind kind, Runnable action) {
		button(context, label, x, y, w, h, kind, true, action);
	}

	protected void button(DrawContext context, String label, int x, int y, int w, int h, ButtonKind kind, boolean enabled, Runnable action) {
		boolean hovered = enabled && contains(mouseX, mouseY, x, y, w, h);

		int fill;
		int borderColor;
		int textColor;

		switch (kind) {
			case PRIMARY -> {
				fill = hovered ? ColourUtil.brighten(Theme.ACCENT, 0.12F) : Theme.ACCENT;
				borderColor = ColourUtil.brighten(Theme.ACCENT, 0.25F);
				textColor = 0xFFFFFFFF;
			}
			case SUCCESS -> {
				fill = hovered ? ColourUtil.brighten(Theme.GREEN, 0.12F) : ColourUtil.darken(Theme.GREEN, 0.55F);
				borderColor = Theme.GREEN;
				textColor = 0xFFFFFFFF;
			}
			case DANGER -> {
				fill = hovered ? ColourUtil.withAlpha(Theme.RED, 90) : ColourUtil.withAlpha(Theme.RED, 45);
				borderColor = ColourUtil.withAlpha(Theme.RED, 160);
				textColor = ColourUtil.brighten(Theme.RED, 0.4F);
			}
			case GHOST -> {
				fill = hovered ? ColourUtil.withAlpha(Theme.PANEL_HOVER, 200) : 0x00000000;
				borderColor = hovered ? Theme.BORDER_LIGHT : Theme.BORDER;
				textColor = hovered ? Theme.TEXT : Theme.TEXT_DIM;
			}
			case SELECTED -> {
				fill = hovered ? ColourUtil.withAlpha(Theme.ACCENT, 70) : ColourUtil.withAlpha(Theme.ACCENT, 45);
				borderColor = Theme.ACCENT;
				textColor = Theme.TEXT;
			}
			default -> {
				fill = hovered ? Theme.PANEL_HOVER : Theme.PANEL_RAISED;
				borderColor = hovered ? Theme.BORDER_LIGHT : Theme.BORDER;
				textColor = hovered ? Theme.TEXT : Theme.TEXT_DIM;
			}
		}

		if (!enabled) {
			fill = ColourUtil.withAlpha(Theme.PANEL, 160);
			borderColor = Theme.BORDER;
			textColor = Theme.TEXT_MUTED;
		}

		if ((fill >>> 24) != 0) {
			Theme.fill(context, x, y, w, h, fill);
		}

		Theme.border(context, x, y, w, h, borderColor);
		String text = trim(label, w - 10);
		Theme.textCentered(context, textRenderer, text, x + w / 2, y + (h - 8) / 2, textColor);

		if (enabled) {
			region(x, y, w, h, action);
		}
	}

	protected void tab(DrawContext context, String label, int x, int y, int w, int h, boolean selected, Runnable action) {
		boolean hovered = contains(mouseX, mouseY, x, y, w, h);
		int fill = selected ? ColourUtil.withAlpha(Theme.ACCENT, 55) : hovered ? Theme.PANEL_HOVER : Theme.PANEL_ALT;
		Theme.fill(context, x, y, w, h, fill);
		Theme.border(context, x, y, w, h, selected ? Theme.ACCENT : Theme.BORDER);
		Theme.textCentered(context, textRenderer, trim(label, w - 8), x + w / 2, y + (h - 8) / 2,
				selected ? Theme.TEXT : Theme.TEXT_DIM);
		region(x, y, w, h, action);
	}

	protected void checkboxRow(DrawContext context, int x, int y, int w, int h, boolean checked, int color, boolean hovered) {
		if (hovered) {
			Theme.fill(context, x, y, w, h, ColourUtil.withAlpha(Theme.PANEL_HOVER, 150));
		}

		Theme.checkbox(context, x + 4, y + (h - 10) / 2, 10, checked, color);
	}

	protected void scrollbar(DrawContext context, int x, int y, int h, int contentHeight, int offset) {
		if (contentHeight <= h || h <= 0) {
			return;
		}

		Theme.fill(context, x, y, 2, h, Theme.BORDER);
		int thumbHeight = Math.max(14, (int) ((h / (double) contentHeight) * h));
		int maxOffset = Math.max(1, contentHeight - h);
		int thumbY = y + (int) ((offset / (double) maxOffset) * (h - thumbHeight));
		Theme.fill(context, x, thumbY, 2, thumbHeight, Theme.BORDER_LIGHT);
	}

	protected int clampScroll(int offset, int contentHeight, int viewportHeight) {
		int max = Math.max(0, contentHeight - viewportHeight);
		return Math.max(0, Math.min(max, offset));
	}

	protected void sectionLabel(DrawContext context, String label, int x, int y) {
		Theme.text(context, textRenderer, label.toUpperCase(), x, y, Theme.TEXT_MUTED);
	}

	protected void progressBar(DrawContext context, int x, int y, int w, int h, float ratio, int color) {
		Theme.fill(context, x, y, w, h, Theme.BORDER);
		Theme.fill(context, x, y, Math.max(0, Math.min(w, (int) (w * ratio))), h, color);
	}

	private static final class Region {
		private final int x;
		private final int y;
		private final int w;
		private final int h;
		private final Runnable action;

		private Region(int x, int y, int w, int h, Runnable action) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.action = action;
		}

		private boolean contains(int mouseX, int mouseY) {
			return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		}
	}
}
