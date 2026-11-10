package com.nebrion.easytp.gui;

import com.nebrion.easytp.util.ColourUtil;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Dark administration-dashboard theme: black and dark grey panels with a red accent.
 */
public final class Theme {
	public static final int BG_TOP = 0xFF141419;
	public static final int BG_BOTTOM = 0xFF08080A;
	public static final int BG = 0xFF0A0A0C;

	public static final int PANEL = 0xFF121216;
	public static final int PANEL_ALT = 0xFF17171D;
	public static final int PANEL_RAISED = 0xFF1E1E26;
	public static final int PANEL_HOVER = 0xFF262630;

	public static final int BORDER = 0xFF26262F;
	public static final int BORDER_LIGHT = 0xFF3A3A47;

	public static final int TEXT = 0xFFF4F4F7;
	public static final int TEXT_DIM = 0xFFA2A2AE;
	public static final int TEXT_MUTED = 0xFF6C6C78;

	public static final int ACCENT = 0xFFE11D2E;
	public static final int ACCENT_DARK = 0xFF8C0F1B;
	public static final int ACCENT_SOFT = 0xFF2A1013;

	public static final int GREEN = 0xFF22C55E;
	public static final int RED = 0xFFEF4444;
	public static final int GOLD = 0xFFF5A524;
	public static final int BLUE = 0xFF3B82F6;
	public static final int PURPLE = 0xFFA855F7;
	public static final int YELLOW = 0xFFFACC15;
	public static final int ORANGE = 0xFFF97316;
	public static final int CYAN = 0xFF06B6D4;
	public static final int GREY = 0xFF8B8B97;

	public static final int CHECK_ON = 0xFF22C55E;
	public static final int CHECK_OFF = 0xFF3A3A47;

	private Theme() {
	}

	// --- primitives ----------------------------------------------------------------------------

	public static void fill(DrawContext context, int x, int y, int width, int height, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}

		context.fill(x, y, x + width, y + height, color);
	}

	public static void border(DrawContext context, int x, int y, int width, int height, int color) {
		fill(context, x, y, width, 1, color);
		fill(context, x, y + height - 1, width, 1, color);
		fill(context, x, y, 1, height, color);
		fill(context, x + width - 1, y, 1, height, color);
	}

	public static void panel(DrawContext context, int x, int y, int width, int height, float alpha) {
		if (width <= 0 || height <= 0) {
			return;
		}

		int bottom = height > 2 ? height - 1 : height;
		context.fill(x, y, x + width, y + height, ColourUtil.withAlpha(PANEL, alpha));
		context.fill(x + 1, y + 1, x + width - 1, y + bottom, ColourUtil.withAlpha(PANEL_ALT, alpha));
		fill(context, x + 1, y + 1, width - 2, 1, ColourUtil.withAlpha(PANEL_RAISED, alpha));
		border(context, x, y, width, height, ColourUtil.withAlpha(BORDER, alpha));
	}

	public static void raised(DrawContext context, int x, int y, int width, int height, int color) {
		fill(context, x, y, width, height, color);
		fill(context, x, y, width, 1, ColourUtil.brighten(color, 0.10F));
	}

	public static void divider(DrawContext context, int x, int y, int width) {
		fill(context, x, y, width, 1, BORDER);
	}

	public static void accentBar(DrawContext context, int x, int y, int height, int color) {
		fill(context, x, y, 2, height, color);
	}

	// --- text ----------------------------------------------------------------------------------

	public static void text(DrawContext context, TextRenderer renderer, String value, int x, int y, int color) {
		context.drawText(renderer, value, x, y, color, false);
	}

	public static void textShadow(DrawContext context, TextRenderer renderer, String value, int x, int y, int color) {
		context.drawText(renderer, value, x, y, color, true);
	}

	public static void textCentered(DrawContext context, TextRenderer renderer, String value, int centerX, int y, int color) {
		context.drawText(renderer, value, centerX - renderer.getWidth(value) / 2, y, color, false);
	}

	public static void textRight(DrawContext context, TextRenderer renderer, String value, int rightX, int y, int color) {
		context.drawText(renderer, value, rightX - renderer.getWidth(value), y, color, false);
	}

	public static void label(DrawContext context, TextRenderer renderer, Text text, int x, int y, int color) {
		context.drawText(renderer, text, x, y, color, false);
	}

	public static int width(TextRenderer renderer, String value) {
		return renderer.getWidth(value);
	}

	public static String trim(TextRenderer renderer, String value, int maxWidth) {
		if (maxWidth <= 0) {
			return "";
		}

		if (renderer.getWidth(value) <= maxWidth) {
			return value;
		}

		String trimmed = renderer.trimToWidth(value, Math.max(0, maxWidth - renderer.getWidth("...")));
		return trimmed + "...";
	}

	// --- small components ----------------------------------------------------------------------

	public static void dot(DrawContext context, int x, int y, int size, int color) {
		fill(context, x, y, size, size, color);
		fill(context, x + 1, y + 1, Math.max(0, size - 2), Math.max(0, size - 2), ColourUtil.brighten(color, 0.25F));
	}

	public static void checkbox(DrawContext context, int x, int y, int size, boolean checked, int color) {
		fill(context, x, y, size, size, checked ? color : CHECK_OFF);
		fill(context, x + 1, y + 1, size - 2, size - 2, checked
				? ColourUtil.darken(color, 0.55F)
				: ColourUtil.darken(CHECK_OFF, 0.55F));

		if (checked) {
			fill(context, x + 2, y + size - 5, 3, 1, 0xFFFFFFFF);
			fill(context, x + 3, y + size - 4, 3, 1, 0xFFFFFFFF);
			fill(context, x + 4, y + size - 5, 1, 1, 0xFFFFFFFF);
			fill(context, x + size - 4, y + 3, 1, 3, 0xFFFFFFFF);
			fill(context, x + size - 3, y + 2, 1, 2, 0xFFFFFFFF);
		}
	}

	public static void switchTrack(DrawContext context, int x, int y, int width, int height, boolean on) {
		int fillColor = on ? ColourUtil.withAlpha(ACCENT, 220) : 0xFF2A2A34;
		fill(context, x, y, width, height, fillColor);
		border(context, x, y, width, height, on ? ColourUtil.brighten(ACCENT, 0.2F) : BORDER);

		int knob = Math.max(4, height - 4);
		int knobX = on ? x + width - knob - 2 : x + 2;
		fill(context, knobX, y + 2, knob, knob, on ? 0xFFFFFFFF : 0xFF7A7A88);
	}

	public static void chip(DrawContext context, TextRenderer renderer, String label, int x, int y, int color) {
		int width = renderer.getWidth(label) + 8;
		fill(context, x, y, width, 11, ColourUtil.withAlpha(color, 0x33));
		fill(context, x, y, 2, 11, color);
		text(context, renderer, label, x + 5, y + 2, colourText(color));
	}

	public static int chipWidth(TextRenderer renderer, String label) {
		return renderer.getWidth(label) + 8;
	}

	public static int colourText(int color) {
		return ColourUtil.brighten(color, 0.35F) | 0xFF000000;
	}
}
