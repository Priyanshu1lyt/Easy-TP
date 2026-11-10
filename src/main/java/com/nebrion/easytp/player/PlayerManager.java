package com.nebrion.easytp.player;

import com.nebrion.easytp.EasyTP;
import com.nebrion.easytp.config.ConfigManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks the online players and the operator's current selection.
 *
 * <p>The list is rebuilt from the vanilla client player list, so joins and quits are picked up
 * automatically and duplicates cannot happen. Disconnected players are dropped from active
 * selections, while team membership (owned by {@link com.nebrion.easytp.team.TeamManager}) is kept.</p>
 */
public final class PlayerManager {
	private static final PlayerManager INSTANCE = new PlayerManager();
	private static final int AUTO_REFRESH_INTERVAL = 40;

	private final Map<UUID, PlayerData> online = new LinkedHashMap<>();
	private final Set<UUID> selected = new LinkedHashSet<>();
	private int autoRefreshCooldown;
	private long lastRefresh;

	private PlayerManager() {
	}

	public static PlayerManager get() {
		return INSTANCE;
	}

	/** Rebuilds the online list from the client player list. Safe to call at any time. */
	public void refresh(MinecraftClient client) {
		lastRefresh = System.currentTimeMillis();
		online.clear();

		if (client == null || client.getNetworkHandler() == null) {
			selected.clear();
			return;
		}

		for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
			if (entry == null || entry.getProfile() == null) {
				continue;
			}

			UUID uuid = entry.getProfile().getId();

			if (uuid == null) {
				continue;
			}

			String name = entry.getProfile().getName();

			if (name == null || name.isBlank()) {
				name = "Unknown";
			}

			String mode = entry.getGameMode() != null
					? entry.getGameMode().getTranslatableName().getString()
					: "Unknown";

			online.put(uuid, new PlayerData(uuid, name, true, entry.getLatency(), mode, entry.getSkinTextures()));
			ConfigManager.get().rememberName(uuid.toString(), name);
		}

		// A player who left the server must never stay in the active selection.
		selected.removeIf(uuid -> !online.containsKey(uuid));
	}

	public void autoRefresh(MinecraftClient client) {
		if (!ConfigManager.get().settings().autoRefresh) {
			return;
		}

		if (--autoRefreshCooldown > 0) {
			return;
		}

		autoRefreshCooldown = AUTO_REFRESH_INTERVAL;
		refresh(client);
	}

	public Collection<PlayerData> all() {
		return online.values();
	}

	public PlayerData get(UUID uuid) {
		return uuid == null ? null : online.get(uuid);
	}

	public PlayerData byName(String name) {
		if (name == null) {
			return null;
		}

		for (PlayerData data : online.values()) {
			if (data.name().equalsIgnoreCase(name)) {
				return data;
			}
		}

		return null;
	}

	public List<PlayerData> sortedByName() {
		List<PlayerData> list = new ArrayList<>(online.values());
		list.sort((first, second) -> String.CASE_INSENSITIVE_ORDER.compare(first.name(), second.name()));
		return list;
	}

	public Optional<PlayerData> self(MinecraftClient client) {
		if (client == null || client.player == null) {
			return Optional.empty();
		}

		return Optional.ofNullable(online.get(client.player.getUuid()));
	}

	public long lastRefresh() {
		return lastRefresh;
	}

	// --- selection -----------------------------------------------------------------------------

	public boolean isSelected(UUID uuid) {
		return uuid != null && selected.contains(uuid);
	}

	public void toggleSelection(UUID uuid) {
		if (uuid == null) {
			return;
		}

		if (!selected.remove(uuid)) {
			if (online.containsKey(uuid)) {
				selected.add(uuid);
			}
		}
	}

	public void select(UUID uuid) {
		if (uuid != null && online.containsKey(uuid)) {
			selected.add(uuid);
		}
	}

	public void deselect(UUID uuid) {
		if (uuid != null) {
			selected.remove(uuid);
		}
	}

	public void selectAll() {
		selected.addAll(online.keySet());
	}

	public void clearSelection() {
		selected.clear();
	}

	public Set<UUID> selectedIds() {
		return new LinkedHashSet<>(selected);
	}

	public int selectedCount() {
		return selected.size();
	}

	public List<PlayerData> selectedPlayers() {
		List<PlayerData> list = new ArrayList<>();

		for (UUID uuid : selected) {
			PlayerData data = online.get(uuid);

			if (data != null) {
				list.add(data);
			}
		}

		list.sort((first, second) -> String.CASE_INSENSITIVE_ORDER.compare(first.name(), second.name()));
		return list;
	}

	public int onlineCount() {
		return online.size();
	}
}
