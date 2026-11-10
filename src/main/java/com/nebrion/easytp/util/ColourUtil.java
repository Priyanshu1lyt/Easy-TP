package com.nebrion.easytp.util;

import java.util.List;

/**
 * Colour helpers: hex parsing, alpha maths and a shared team palette.
 */
public final class ColourUtil {
	/** Palette offered by the team colour picker (ARGB, fully opaque). */
	public static final List<Integer> PALETTE = List.of(
			0xFF22C55E, // green
			0xFF3B82F6, // blue
			0xFFEF4444, // red
			0xFFFACC15, // yellow
			0xFFA855F7, // purple
			0xFF06B6D4, // cyan
			0xFFF97316, // orange
			0xFFEC4899, // pink
			0xFF14B8A6, // teal
			0xFF84CC16, // lime
			0xFFEAB308, // amber
			0xFF8B5CF6, // violet
			0xFFF43F5E, // rose
			0xFF64748B  // slate
	);

	private ColourUtil() {
	}

	public static int withAlpha(int color, int alpha) {
		return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
	}

	public static int withAlpha(int color, float multiplier) {
		int alpha = (int) (((color >>> 24) & 0xFF) * Math.max(0.0F, Math.min(1.0F, multiplier)));
		return withAlpha(color, alpha);
	}

	public static int rgb(int color) {
		return color & 0x00FFFFFF;
	}

	public static int mix(int first, int second, float ratio) {
		float clamped = Math.max(0.0F, Math.min(1.0F, ratio));
		int a = (int) (((first >>> 24) & 0xFF) * (1.0F - clamped) + ((second >>> 24) & 0xFF) * clamped);
		int r = (int) (((first >> 16) & 0xFF) * (1.0F - clamped) + ((second >> 16) & 0xFF) * clamped);
		int g = (int) (((first >> 8) & 0xFF) * (1.0F - clamped) + ((second >> 8) & 0xFF) * clamped);
		int b = (int) ((first & 0xFF) * (1.0F - clamped) + (second & 0xFF) * clamped);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/** Darkens a colour towards black; keeps the alpha channel. */
	public static int darken(int color, float amount) {
		return mix(color, withAlpha(0xFF000000, (color >>> 24) & 0xFF), amount);
	}

	/** Brightens a colour towards white; keeps the alpha channel. */
	public static int brighten(int color, float amount) {
		return mix(color, withAlpha(0xFFFFFFFF, (color >>> 24) & 0xFF), amount);
	}

	/** Picks black or white depending on the perceived luminance of the colour. */
	public static int readableText(int background) {
		int r = (background >> 16) & 0xFF;
		int g = (background >> 8) & 0xFF;
		int b = background & 0xFF;
		double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
		return luminance > 0.62 ? 0xFF101014 : 0xFFF7F7FA;
	}

	/**
	 * Parses a user supplied hex string. Accepts {@code #RRGGBB}, {@code RRGGBB}, {@code #RGB} and the
	 * same values prefixed with {@code 0x}. Returns {@code null} when the input is not a colour.
	 */
	public static Integer parseHex(String input) {
		if (input == null) {
			return null;
		}

		String value = input.trim().toUpperCase();

		if (value.startsWith("#")) {
			value = value.substring(1);
		} else if (value.startsWith("0X")) {
			value = value.substring(2);
		}

		if (value.length() == 3) {
			StringBuilder builder = new StringBuilder();

			for (int i = 0; i < 3; i++) {
				char c = value.charAt(i);
				builder.append(c).append(c);
			}

			value = builder.toString();
		}

		if (value.length() != 6) {
			return null;
		}

		try {
			int rgb = Integer.parseInt(value, 16);
			return 0xFF000000 | rgb;
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	public static String toHex(int color) {
		return String.format("#%06X", color & 0xFFFFFF);
	}

	/** Returns the closest palette entry, used as a fallback when a colour is created randomly. */
	public static int paletteColor(int index) {
		int size = PALETTE.size();
		int wrapped = ((index % size) + size) % size;
		return PALETTE.get(wrapped);
	}
}
