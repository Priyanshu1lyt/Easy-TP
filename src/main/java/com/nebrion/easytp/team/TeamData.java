package com.nebrion.easytp.team;

import com.nebrion.easytp.config.TeamDto;
import com.nebrion.easytp.player.PlayerData;
import com.nebrion.easytp.player.PlayerManager;
import com.nebrion.easytp.util.ColourUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A colour coded, named group of players. Teams are unlimited in number and can hold players that
 * are currently offline (membership is stored by UUID).
 */
public final class TeamData {
	private final String id;
	private String name;
	private int color;
	private final LinkedHashSet<UUID> members = new LinkedHashSet<>();
	private final Map<UUID, String> names = new LinkedHashMap<>();

	public TeamData(String id, String name, int color) {
		this.id = id;
		this.name = name;
		this.color = color;
	}

	public String id() {
		return id;
	}

	public String name() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int color() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public LinkedHashSet<UUID> members() {
		return members;
	}

	public int memberCount() {
		return members.size();
	}

	public boolean hasMember(UUID uuid) {
		return uuid != null && members.contains(uuid);
	}

	public void addMember(UUID uuid, String name) {
		if (uuid == null) {
			return;
		}

		members.add(uuid);

		if (name != null) {
			names.put(uuid, name);
		}
	}

	public void removeMember(UUID uuid) {
		if (uuid == null) {
			return;
		}

		members.remove(uuid);
	}

	public String storedName(UUID uuid) {
		String stored = names.get(uuid);

		if (stored != null) {
			return stored;
		}

		return uuid == null ? "Unknown" : uuid.toString().substring(0, 8);
	}

	/** Name shown in the UI: prefers the live player list over the stored snapshot. */
	public String displayName(UUID uuid) {
		PlayerData live = PlayerManager.get().get(uuid);

		if (live != null) {
			names.put(uuid, live.name());
			return live.name();
		}

		return storedName(uuid);
	}

	public List<String> displayNames() {
		List<String> list = new ArrayList<>();

		for (UUID member : members) {
			list.add(displayName(member));
		}

		list.sort(String.CASE_INSENSITIVE_ORDER);
		return list;
	}

	public int onlineMemberCount() {
		int count = 0;

		for (UUID member : members) {
			if (PlayerManager.get().get(member) != null) {
				count++;
			}
		}

		return count;
	}

	public TeamDto toDto() {
		TeamDto dto = new TeamDto(id, name, color);

		for (UUID member : members) {
			dto.members.add(member.toString());
			dto.names.put(member.toString(), storedName(member));
		}

		return dto;
	}

	public static TeamData fromDto(TeamDto dto) {
		if (dto == null || dto.name == null) {
			return null;
		}

		String id = dto.id;

		if (id == null || id.isBlank()) {
			id = UUID.randomUUID().toString();
		}

		TeamData team = new TeamData(id, dto.name, dto.color);

		if (dto.members != null) {
			for (String raw : dto.members) {
				try {
					UUID uuid = UUID.fromString(raw);
					String name = dto.names != null ? dto.names.get(raw) : null;
					team.addMember(uuid, name);
				} catch (IllegalArgumentException exception) {
					// Skip a single malformed member instead of losing the whole team.
				}
			}
		}

		return team;
	}

	/** The four example teams created the first time a server is used. */
	public static List<TeamDto> defaultTeams() {
		List<TeamDto> list = new ArrayList<>();
		list.add(new TeamDto(UUID.randomUUID().toString(), "GREEN TEAM", ColourUtil.paletteColor(0)));
		list.add(new TeamDto(UUID.randomUUID().toString(), "BLUE TEAM", ColourUtil.paletteColor(1)));
		list.add(new TeamDto(UUID.randomUUID().toString(), "RED TEAM", ColourUtil.paletteColor(2)));
		list.add(new TeamDto(UUID.randomUUID().toString(), "YELLOW TEAM", ColourUtil.paletteColor(3)));
		return list;
	}
}
