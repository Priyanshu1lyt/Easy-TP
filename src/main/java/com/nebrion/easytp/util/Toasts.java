package com.nebrion.easytp.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Small in-GUI notification stack (bottom right of the dashboard).
 */
public final class Toasts {
	public static final int INFO = 0xFFE5E7EB;
	public static final int SUCCESS = 0xFF22C55E;
	public static final int WARN = 0xFFF5A524;
	public static final int ERROR = 0xFFEF4444;

	private static final List<Entry> ENTRIES = new ArrayList<>();
	private static final int LIFETIME = 90;
	private static final int MAX_VISIBLE = 4;

	private Toasts() {
	}

	public static void info(String message) {
		add(message, INFO);
	}

	public static void success(String message) {
		add(message, SUCCESS);
	}

	public static void warn(String message) {
		add(message, WARN);
	}

	public static void error(String message) {
		add(message, ERROR);
	}

	private static void add(String message, int color) {
		if (message == null || message.isBlank()) {
			return;
		}

		ENTRIES.add(new Entry(message, color));

		while (ENTRIES.size() > 12) {
			ENTRIES.remove(0);
		}
	}

	public static void tick() {
		if (ENTRIES.isEmpty()) {
			return;
		}

		for (Entry entry : ENTRIES) {
			entry.ticksLeft--;
		}

		ENTRIES.removeIf(entry -> entry.ticksLeft <= 0);
	}

	public static List<Entry> visible() {
		if (ENTRIES.size() <= MAX_VISIBLE) {
			return new ArrayList<>(ENTRIES);
		}

		return new ArrayList<>(ENTRIES.subList(ENTRIES.size() - MAX_VISIBLE, ENTRIES.size()));
	}

	public static void clear() {
		ENTRIES.clear();
	}

	public static final class Entry {
		private final String text;
		private final int color;
		private int ticksLeft = LIFETIME;

		private Entry(String text, int color) {
			this.text = text;
			this.color = color;
		}

		public String text() {
			return text;
		}

		public int color() {
			return color;
		}

		public int life() {
			return ticksLeft;
		}
	}
}
