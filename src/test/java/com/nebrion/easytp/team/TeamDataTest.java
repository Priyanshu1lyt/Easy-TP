package com.nebrion.easytp.team;

import com.nebrion.easytp.config.TeamDto;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamDataTest {
	@Test
	void defaultsContainTheFourExampleTeams() {
		var defaults = TeamData.defaultTeams();

		assertEquals(4, defaults.size());
		assertEquals("GREEN TEAM", defaults.get(0).name);
		assertEquals("BLUE TEAM", defaults.get(1).name);
		assertEquals("RED TEAM", defaults.get(2).name);
		assertEquals("YELLOW TEAM", defaults.get(3).name);
	}

	@Test
	void roundTripsThroughDto() {
		TeamData team = new TeamData(UUID.randomUUID().toString(), "BLUE TEAM", 0xFF3B82F6);
		UUID member = UUID.randomUUID();
		team.addMember(member, "Steve");
		team.addMember(UUID.randomUUID(), "Alex");

		TeamDto dto = team.toDto();
		TeamData restored = TeamData.fromDto(dto);

		assertNotNull(restored);
		assertEquals(team.id(), restored.id());
		assertEquals("BLUE TEAM", restored.name());
		assertEquals(0xFF3B82F6, restored.color());
		assertEquals(2, restored.memberCount());
		assertTrue(restored.hasMember(member));
		assertEquals("Steve", restored.storedName(member));
	}

	@Test
	void skipsMalformedMembersInsteadOfFailing() {
		TeamDto dto = new TeamDto("id", "BROKEN", 0xFF000000);
		dto.members.add("not-a-uuid");
		dto.members.add(UUID.randomUUID().toString());

		TeamData team = TeamData.fromDto(dto);

		assertNotNull(team);
		assertEquals(1, team.memberCount());
	}

	@Test
	void removeMemberWorks() {
		TeamData team = new TeamData("id", "TEAM", 0xFF00FF00);
		UUID member = UUID.randomUUID();
		team.addMember(member, "Steve");

		assertTrue(team.hasMember(member));

		team.removeMember(member);

		assertFalse(team.hasMember(member));
		assertEquals(0, team.memberCount());
	}
}
