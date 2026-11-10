package com.nebrion.easytp.teleport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordinateManagerTest {
	@Test
	void acceptsPositiveIntegerCoordinates() {
		CoordinateManager.Validation validation = CoordinateManager.parse("150", "72", "-340");

		assertTrue(validation.valid());
		assertEquals(150.0, validation.x());
		assertEquals(72.0, validation.y());
		assertEquals(-340.0, validation.z());
		assertFalse(validation.hasWarning());
	}

	@Test
	void acceptsDecimalCoordinatesAndCommaDecimalSeparator() {
		CoordinateManager.Validation validation = CoordinateManager.parse("12,5", "-64.25", "0");

		assertTrue(validation.valid());
		assertEquals(12.5, validation.x(), 0.0001);
		assertEquals(-64.25, validation.y(), 0.0001);
	}

	@Test
	void rejectsNonNumericValues() {
		assertFalse(CoordinateManager.parse("abc", "64", "0").valid());
		assertFalse(CoordinateManager.parse("1", "", "2").valid());
		assertFalse(CoordinateManager.parse("1", "NaN", "2").valid());
		assertFalse(CoordinateManager.parse("1", "Infinity", "2").valid());
	}

	@Test
	void rejectsOutOfRangeCoordinates() {
		assertFalse(CoordinateManager.parse("30000001", "64", "0").valid());
		assertFalse(CoordinateManager.parse("0", "99999", "0").valid());
		assertFalse(CoordinateManager.parse("0", "64", "-30000001").valid());
	}

	@Test
	void warnsForUnusualHeightButStaysValid() {
		CoordinateManager.Validation validation = CoordinateManager.parse("0", "1000", "0");

		assertTrue(validation.valid());
		assertTrue(validation.hasWarning());
	}

	@Test
	void labelsKnownDimensions() {
		assertEquals("Overworld", CoordinateManager.dimensionLabel(CoordinateManager.OVERWORLD));
		assertEquals("Nether", CoordinateManager.dimensionLabel(CoordinateManager.NETHER));
		assertEquals("The End", CoordinateManager.dimensionLabel(CoordinateManager.END));
		assertEquals("Current dimension", CoordinateManager.dimensionLabel(null));
	}
}
