package com.nebrion.easytp.teleport;

import org.junit.jupiter.api.Test;

import net.minecraft.util.Identifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandBuilderTest {
	@Test
	void formatsWholeNumbersWithoutDecimals() {
		assertEquals("150", CommandBuilder.formatCoordinate(150.0));
		assertEquals("-340", CommandBuilder.formatCoordinate(-340.0));
		assertEquals("0", CommandBuilder.formatCoordinate(0.0));
	}

	@Test
	void keepsUpToThreeDecimals() {
		assertEquals("12.5", CommandBuilder.formatCoordinate(12.5));
		assertEquals("12.501", CommandBuilder.formatCoordinate(12.5009));
		assertEquals("-0.25", CommandBuilder.formatCoordinate(-0.25));
	}

	@Test
	void buildsPlainPlayerCommands() {
		assertEquals("tp Steve Alex", CommandBuilder.playerTarget("Steve", "Alex"));
	}

	@Test
	void buildsCoordinateCommandsInTheCurrentDimension() {
		Identifier overworld = Identifier.of("minecraft", "overworld");
		Destination destination = Destination.coordinates(150, 72, -340, overworld);

		assertEquals("tp Steve 150 72 -340", CommandBuilder.coordinateTarget("Steve", destination, overworld));
	}

	@Test
	void wrapsCrossDimensionCommandsInExecute() {
		Identifier overworld = Identifier.of("minecraft", "overworld");
		Identifier nether = Identifier.of("minecraft", "the_nether");
		Destination destination = Destination.coordinates(10, 64, -10, nether);
		Destination player = Destination.player(UUID.randomUUID(), "Alex");

		assertEquals("execute in minecraft:the_nether run tp Steve 10 64 -10",
				CommandBuilder.coordinateTarget("Steve", destination, overworld));
		assertEquals("tp Steve Alex", CommandBuilder.playerTarget("Steve", player.playerName()));
	}
}
