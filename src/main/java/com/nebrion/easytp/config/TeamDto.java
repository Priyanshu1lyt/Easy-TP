package com.nebrion.easytp.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serialised form of a team. UUIDs are stored as strings so that a corrupted single entry can be
 * skipped without losing the whole file.
 */
public final class TeamDto {
	public String id;
	public String name;
	public int color;
	public List<String> members = new ArrayList<>();
	public Map<String, String> names = new LinkedHashMap<>();

	public TeamDto() {
	}

	public TeamDto(String id, String name, int color) {
		this.id = id;
		this.name = name;
		this.color = color;
	}
}
