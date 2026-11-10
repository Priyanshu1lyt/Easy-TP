package com.nebrion.easytp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.nebrion.easytp.EasyTP;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Loads and saves {@code config/easy_tp.json}. Corrupted files are backed up (never deleted) and
 * replaced by a fresh, valid configuration.
 */
public final class ConfigManager {
	private static final ConfigManager INSTANCE = new ConfigManager();

	private final Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private EasyTpConfig root = new EasyTpConfig();
	private Path path;
	private boolean loaded;
	private boolean saveQueued;

	private ConfigManager() {
	}

	public static ConfigManager get() {
		return INSTANCE;
	}

	public EasyTpConfig root() {
		return root;
	}

	public ModSettings settings() {
		return root.settings;
	}

	public ColourSettings colors() {
		return root.colors;
	}

	public Path file() {
		if (path == null) {
			path = FabricLoader.getInstance().getConfigDir().resolve("easy_tp.json");
		}

		return path;
	}

	public synchronized void load() {
		Path file = file();
		loaded = true;

		if (!Files.exists(file)) {
			EasyTP.LOGGER.info("No Easy TP configuration found, creating defaults at {}", file);
			saveNow();
			return;
		}

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			EasyTpConfig parsed = gson.fromJson(reader, EasyTpConfig.class);

			if (parsed == null) {
				throw new JsonSyntaxException("Configuration file is empty");
			}

			root = migrate(parsed);
			EasyTP.LOGGER.info("Loaded Easy TP configuration ({} server profile(s))", root.servers.size());
		} catch (IOException | RuntimeException exception) {
			EasyTP.LOGGER.error("Easy TP configuration is unreadable, starting from defaults", exception);
			backupCorrupted(file);
			root = new EasyTpConfig();
			saveNow();
		}
	}

	private EasyTpConfig migrate(EasyTpConfig parsed) {
		if (parsed.settings == null) {
			parsed.settings = new ModSettings();
		}

		if (parsed.colors == null) {
			parsed.colors = new ColourSettings();
		}

		if (parsed.servers == null) {
			parsed.servers = new java.util.LinkedHashMap<>();
		}

		if (parsed.knownPlayers == null) {
			parsed.knownPlayers = new java.util.LinkedHashMap<>();
		}

		if (parsed.lastCoordinates == null) {
			parsed.lastCoordinates = new java.util.ArrayList<>();
		}

		if (parsed.lastDimension == null || parsed.lastDimension.isBlank()) {
			parsed.lastDimension = "minecraft:overworld";
		}

		for (ServerStore store : parsed.servers.values()) {
			if (store.teams == null) {
				store.teams = new java.util.ArrayList<>();
			}
		}

		return parsed;
	}

	private void backupCorrupted(Path file) {
		try {
			Path backup = file.resolveSibling("easy_tp.json.corrupt");
			Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
			EasyTP.LOGGER.warn("Backed up the unreadable configuration to {}", backup);
		} catch (IOException exception) {
			EasyTP.LOGGER.warn("Could not back up the unreadable configuration", exception);
		}
	}

	public synchronized void save() {
		if (!loaded) {
			loaded = true;
		}

		saveNow();
	}

	/** Saves at the end of the current tick at the earliest, so bursts of edits write only once. */
	public void requestSave() {
		if (!root.settings.autoSaveTeams) {
			return;
		}

		if (saveQueued) {
			return;
		}

		saveQueued = true;
		com.nebrion.easytp.util.Ticker.schedule(1, () -> {
			saveQueued = false;
			save();
		});
	}

	public synchronized void saveNow() {
		Path file = file();

		try {
			Path parent = file.getParent();

			if (parent != null) {
				Files.createDirectories(parent);
			}

			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				gson.toJson(root, writer);
			}
		} catch (IOException exception) {
			EasyTP.LOGGER.error("Failed to save Easy TP configuration to {}", file, exception);
		}
	}

	/** Teams of one server profile, seeding the four default teams the first time a server is seen. */
	public List<TeamDto> teamsFor(String serverKey) {
		ServerStore store = root.server(serverKey);

		if (!store.seeded) {
			store.seeded = true;

			if (store.teams.isEmpty()) {
				store.teams.addAll(com.nebrion.easytp.team.TeamData.defaultTeams());
			}

			requestSave();
		}

		return store.teams;
	}

	public void setTeamsFor(String serverKey, List<TeamDto> teams) {
		ServerStore store = root.server(serverKey);
		store.teams = teams;
	}

	public void rememberName(String uuid, String name) {
		if (uuid == null || name == null) {
			return;
		}

		String previous = root.knownPlayers.put(uuid, name);

		if (!name.equals(previous)) {
			requestSave();
		}
	}

	public String knownName(String uuid) {
		return root.knownPlayers.get(uuid);
	}
}
