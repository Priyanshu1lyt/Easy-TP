package com.nebrion.easytp.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Teams belonging to one server/world identity.
 */
public final class ServerStore {
	public boolean seeded = false;
	public List<TeamDto> teams = new ArrayList<>();
}
