package com.nebrion.easytp.team;

import com.nebrion.easytp.EasyTP;
import com.nebrion.easytp.config.ConfigManager;
import com.nebrion.easytp.config.TeamDto;
import com.nebrion.easytp.player.PlayerData;
import com.nebrion.easytp.player.PlayerManager;
import com.nebrion.easytp.util.ColourUtil;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Owns every team of the current server profile, team selection and team membership.
 */
public final class TeamManager {
	private static final TeamManager INSTANCE = new TeamManager();

	private final List<TeamData> teams = new ArrayList<>();
	private final Set<String> selectedTeams = new LinkedHashSet<>();
	private String loadedServerKey = "";
	private int colorCursor;

	private TeamManager() {
	}

	public static TeamManager get() {
		return INSTANCE;
	}

	/** Loads the team storage of the server the operator is connected to (no-op when unchanged). */
	public void ensureServer(MinecraftClient client) {
		String key = EasyTP.serverKey(client);

		if (key.equals(loadedServerKey)) {
			return;
		}

		loadedServerKey = key;
		teams.clear();
		selectedTeams.clear();

		for (TeamDto dto : ConfigManager.get().teamsFor(key)) {
			TeamData team = TeamData.fromDto(dto);

			if (team != null) {
				teams.add(team);
			}
		}

		colorCursor = teams.size();
	}

	public List<TeamData> all() {
		return teams;
	}

	public Optional<TeamData> byId(String id) {
		if (id == null) {
			return Optional.empty();
		}

		for (TeamData team : teams) {
			if (team.id().equals(id)) {
				return Optional.of(team);
			}
		}

		return Optional.empty();
	}

	public Optional<TeamData> byName(String name) {
		if (name == null) {
			return Optional.empty();
		}

		for (TeamData team : teams) {
			if (team.name().equalsIgnoreCase(name)) {
				return Optional.of(team);
			}
		}

		return Optional.empty();
	}

	public int count() {
		return teams.size();
	}

	public boolean nameTaken(String name, String ignoreId) {
		for (TeamData team : teams) {
			if (team.name().equalsIgnoreCase(name) && !team.id().equals(ignoreId)) {
				return true;
			}
		}

		return false;
	}

	public TeamData create(String name, int color) {
		TeamData team = new TeamData(UUID.randomUUID().toString(), name, color);
		teams.add(team);
		persist();
		return team;
	}

	public void delete(String id) {
		teams.removeIf(team -> team.id().equals(id));
		selectedTeams.remove(id);
		persist();
	}

	public void rename(String id, String name) {
		byId(id).ifPresent(team -> {
			team.setName(name);
			persist();
		});
	}

	public void setColor(String id, int color) {
		byId(id).ifPresent(team -> {
			team.setColor(color);
			persist();
		});
	}

	public void addMember(String teamId, UUID player, String name) {
		Optional<TeamData> target = byId(teamId);

		if (target.isEmpty() || player == null) {
			return;
		}

		if (!ConfigManager.get().settings().multiTeamMembership) {
			for (TeamData team : teams) {
				if (!team.id().equals(teamId)) {
					team.removeMember(player);
				}
			}
		}

		target.get().addMember(player, name);
		persist();
	}

	public void removeMember(String teamId, UUID player) {
		byId(teamId).ifPresent(team -> {
			team.removeMember(player);
			persist();
		});
	}

	public void removeFromAllTeams(UUID player) {
		if (player == null) {
			return;
		}

		boolean changed = false;

		for (TeamData team : teams) {
			if (team.hasMember(player)) {
				team.removeMember(player);
				changed = true;
			}
		}

		if (changed) {
			persist();
		}
	}

	public List<TeamData> teamsOf(UUID player) {
		List<TeamData> list = new ArrayList<>();

		for (TeamData team : teams) {
			if (team.hasMember(player)) {
				list.add(team);
			}
		}

		return list;
	}

	public TeamData primaryTeamOf(UUID player) {
		List<TeamData> list = teamsOf(player);
		return list.isEmpty() ? null : list.get(0);
	}

	public List<PlayerData> onlineMembers(TeamData team) {
		List<PlayerData> list = new ArrayList<>();

		for (UUID member : team.members()) {
			PlayerData data = PlayerManager.get().get(member);

			if (data != null) {
				list.add(data);
			}
		}

		list.sort((first, second) -> String.CASE_INSENSITIVE_ORDER.compare(first.name(), second.name()));
		return list;
	}

	// --- team selection ------------------------------------------------------------------------

	public boolean isSelected(String teamId) {
		return selectedTeams.contains(teamId);
	}

	public void toggle(String teamId) {
		if (!selectedTeams.remove(teamId)) {
			selectedTeams.add(teamId);
		}
	}

	public void select(String teamId) {
		selectedTeams.add(teamId);
	}

	public void deselect(String teamId) {
		selectedTeams.remove(teamId);
	}

	public void selectAll() {
		for (TeamData team : teams) {
			selectedTeams.add(team.id());
		}
	}

	public void clearSelection() {
		selectedTeams.clear();
	}

	public Set<String> selectedIds() {
		return new LinkedHashSet<>(selectedTeams);
	}

	public List<TeamData> selectedTeams() {
		List<TeamData> list = new ArrayList<>();

		for (String id : selectedTeams) {
			byId(id).ifPresent(list::add);
		}

		return list;
	}

	public int selectedCount() {
		return selectedTeams.size();
	}

	/** Union of explicitly selected players and every online member of the selected teams. */
	public LinkedHashSet<UUID> resolveTargets() {
		LinkedHashSet<UUID> targets = new LinkedHashSet<>(PlayerManager.get().selectedIds());

		for (TeamData team : selectedTeams()) {
			for (UUID member : team.members()) {
				if (PlayerManager.get().get(member) != null) {
					targets.add(member);
				}
			}
		}

		return targets;
	}

	public int nextPaletteColor() {
		int color = ColourUtil.paletteColor(colorCursor++);
		return color;
	}

	public void persist() {
		if (loadedServerKey.isEmpty()) {
			return;
		}

		List<TeamDto> dtos = new ArrayList<>();

		for (TeamData team : teams) {
			dtos.add(team.toDto());
		}

		ConfigManager.get().setTeamsFor(loadedServerKey, dtos);

		if (ConfigManager.get().settings().autoSaveTeams) {
			ConfigManager.get().requestSave();
		}
	}

	public String loadedServerKey() {
		return loadedServerKey;
	}
}
