package com.nebrion.easytp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColourUtilTest {
	@Test
	void parsesSixDigitHex() {
		assertEquals(0xFF22C55E, ColourUtil.parseHex("#22C55E"));
		assertEquals(0xFF22C55E, ColourUtil.parseHex("22c55e"));
		assertEquals(0xFF22C55E, ColourUtil.parseHex("  #22c55e "));
		assertEquals(0xFF22C55E, ColourUtil.parseHex("0x22C55E"));
	}

	@Test
	void expandsThreeDigitHex() {
		assertEquals(0xFFFFFFFF, ColourUtil.parseHex("#fff"));
		assertEquals(0xFF880000, ColourUtil.parseHex("800"));
	}

	@Test
	void rejectsInvalidHex() {
		assertNull(ColourUtil.parseHex(null));
		assertNull(ColourUtil.parseHex(""));
		assertNull(ColourUtil.parseHex("#12345"));
		assertNull(ColourUtil.parseHex("#GGGGGG"));
	}

	@Test
	void roundTripsHex() {
		assertEquals("#22C55E", ColourUtil.toHex(0xFF22C55E));
		assertEquals("#000000", ColourUtil.toHex(0xFF000000));
	}

	@Test
	void adjustsAlphaAndLuminance() {
		assertEquals(0x80112233, ColourUtil.withAlpha(0xFF112233, 128));
		assertEquals(0xFF112233, ColourUtil.withAlpha(0xFF112233, 255));
		assertEquals(0x00112233, ColourUtil.withAlpha(0xFF112233, 0));

		int lightText = ColourUtil.readableText(0xFFFFFFFF);
		int darkText = ColourUtil.readableText(0xFF101010);

		assertTrue((lightText & 0xFFFFFF) < 0x202020);
		assertTrue((darkText & 0xFFFFFF) > 0xE0E0E0);
	}

	@Test
	void paletteIsUsable() {
		assertNotNull(ColourUtil.PALETTE);
		assertTrue(ColourUtil.PALETTE.size() >= 4);

		for (int color : ColourUtil.PALETTE) {
			assertEquals(0xFF, (color >>> 24) & 0xFF);
		}

		assertEquals(ColourUtil.paletteColor(0), ColourUtil.paletteColor(ColourUtil.PALETTE.size()));
	}
}
