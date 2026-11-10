package com.nebrion.easytp.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModSettingsTest {
	@Test
	void defaultsAreSane() {
		ModSettings settings = new ModSettings();

		assertTrue(settings.animations);
		assertTrue(settings.autoRefresh);
		assertTrue(settings.confirmBeforeTeleport);
		assertTrue(settings.showResults);
		assertTrue(settings.showPlayerHeads);
		assertEquals(ModSettings.DENSITY_NORMAL, settings.density);
		assertEquals(ModSettings.SORT_NAME, settings.sortMode);
		assertTrue(settings.backgroundOpacity >= 120 && settings.backgroundOpacity <= 255);
		assertTrue(settings.commandSpacingTicks >= 1);
		assertTrue(settings.rowHeight() >= 14);
		assertTrue(settings.smallRowHeight() > 0);
	}

	@Test
	void densityAndSortingCycle() {
		ModSettings settings = new ModSettings();

		settings.cycleDensity();
		assertEquals(ModSettings.DENSITY_COMFORTABLE, settings.density);
		settings.cycleDensity();
		assertEquals(ModSettings.DENSITY_COMPACT, settings.density);
		settings.cycleDensity();
		assertEquals(ModSettings.DENSITY_NORMAL, settings.density);

		settings.cycleSort();
		assertEquals(ModSettings.SORT_TEAM, settings.sortMode);
		settings.cycleSort();
		assertEquals(ModSettings.SORT_ONLINE, settings.sortMode);
		settings.cycleSort();
		assertEquals(ModSettings.SORT_NAME, settings.sortMode);
	}

	@Test
	void compactListShortensRows() {
		ModSettings normal = new ModSettings();
		ModSettings compact = new ModSettings();
		compact.compactPlayerList = true;

		assertTrue(compact.rowHeight() < normal.rowHeight());
	}

	@Test
	void configCreatesServerStoresOnDemand() {
		EasyTpConfig config = new EasyTpConfig();
		ServerStore first = config.server("multiplayer:example.org");
		ServerStore second = config.server("multiplayer:example.org");

		assertNotNull(first);
		assertTrue(first == second);
		assertTrue(config.servers.containsKey("multiplayer:example.org"));
	}

	@Test
	void colourDefaultsMatchTheDocumentedPalette() {
		ColourSettings colors = new ColourSettings();

		assertEquals(0xFFEF4444, colors.unselected);
		assertEquals(0xFF22C55E, colors.selected);
		assertEquals(0xFFF5A524, colors.destination);
	}
}
