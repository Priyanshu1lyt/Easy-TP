package com.nebrion.easytp.config;

/**
 * All persisted user preferences. Plain fields on purpose: the whole object is serialised to
 * {@code config/easy_tp.json} with Gson, and it is read directly by the GUI.
 */
public final class ModSettings {
	public static final String DENSITY_COMPACT = "COMPACT";
	public static final String DENSITY_NORMAL = "NORMAL";
	public static final String DENSITY_COMFORTABLE = "COMFORTABLE";

	public static final String SORT_NAME = "NAME";
	public static final String SORT_TEAM = "TEAM";
	public static final String SORT_ONLINE = "ONLINE";

	// --- GUI settings -------------------------------------------------------------------------
	public boolean animations = true;
	/** Panel background opacity, 120 (very translucent) to 255 (solid). */
	public int backgroundOpacity = 240;
	public String density = DENSITY_NORMAL;
	public boolean compactPlayerList = false;
	public boolean showPlayerHeads = true;
	public boolean showOnlineStatus = true;

	// --- Player settings ----------------------------------------------------------------------
	public boolean autoRefresh = true;
	public String sortMode = SORT_NAME;
	public boolean searchIncludesTeams = true;

	// --- General settings ---------------------------------------------------------------------
	public boolean confirmBeforeTeleport = true;
	public boolean showResults = true;
	public boolean autoSaveTeams = true;
	public boolean multiTeamMembership = false;
	/** Delay in ticks between two consecutive /tp commands (20 ticks = 1 second). */
	public int commandSpacingTicks = 2;

	public int rowHeight() {
		int base = switch (density) {
			case DENSITY_COMPACT -> 16;
			case DENSITY_COMFORTABLE -> 24;
			default -> 20;
		};

		if (compactPlayerList) {
			base = Math.max(14, base - 2);
		}

		return base;
	}

	public int smallRowHeight() {
		return Math.max(13, rowHeight() - 4);
	}

	public void cycleDensity() {
		density = switch (density) {
			case DENSITY_COMPACT -> DENSITY_NORMAL;
			case DENSITY_NORMAL -> DENSITY_COMFORTABLE;
			default -> DENSITY_COMPACT;
		};
	}

	public void cycleSort() {
		sortMode = switch (sortMode) {
			case SORT_NAME -> SORT_TEAM;
			case SORT_TEAM -> SORT_ONLINE;
			default -> SORT_NAME;
		};
	}

	public String densityLabel() {
		return switch (density) {
			case DENSITY_COMPACT -> "Compact";
			case DENSITY_COMFORTABLE -> "Comfortable";
			default -> "Normal";
		};
	}

	public String sortLabel() {
		return switch (sortMode) {
			case SORT_TEAM -> "Team, then name";
			case SORT_ONLINE -> "Team status first";
			default -> "Username (A-Z)";
		};
	}

	public boolean onOff(boolean value) {
		return value;
	}
}
