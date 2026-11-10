package com.nebrion.easytp.config;

/**
 * Player status colours. Team colours live on {@link com.nebrion.easytp.team.TeamData}.
 */
public final class ColourSettings {
	/** Unselected players are red by design. */
	public int unselected = 0xFFEF4444;
	/** Selected players are green by design. */
	public int selected = 0xFF22C55E;
	/** The teleport destination is gold by design. */
	public int destination = 0xFFF5A524;
}
