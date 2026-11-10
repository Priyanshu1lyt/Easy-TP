package com.nebrion.easytp.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Root of {@code config/easy_tp.json}.
 */
public final class EasyTpConfig {
	public int version = 1;
	public ModSettings settings = new ModSettings();
	public ColourSettings colors = new ColourSettings();
	/** Team storage, separated per server/world identity. */
	public Map<String, ServerStore> servers = new LinkedHashMap<>();
	/** Last known username for a player UUID (used when a team member is offline). */
	public Map<String, String> knownPlayers = new LinkedHashMap<>();
	/** Remembered last used coordinate destination, purely a convenience. */
	public List<Double> lastCoordinates = new ArrayList<>();
	public String lastDimension = "minecraft:overworld";

	public ServerStore server(String key) {
		return servers.computeIfAbsent(key, ignored -> new ServerStore());
	}
}
