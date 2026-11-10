package com.nebrion.easytp.teleport;

import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * A teleport destination: either another online player, or a set of coordinates in an explicit
 * dimension.
 */
public final class Destination {
	public enum Type {
		PLAYER,
		COORDINATES
	}

	private final Type type;
	private final UUID playerId;
	private final String playerName;
	private final double x;
	private final double y;
	private final double z;
	private final Identifier dimension;

	private Destination(Type type, UUID playerId, String playerName, double x, double y, double z, Identifier dimension) {
		this.type = type;
		this.playerId = playerId;
		this.playerName = playerName;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
	}

	public static Destination player(UUID playerId, String playerName) {
		return new Destination(Type.PLAYER, playerId, playerName, 0, 0, 0, null);
	}

	public static Destination coordinates(double x, double y, double z, Identifier dimension) {
		return new Destination(Type.COORDINATES, null, null, x, y, z, dimension);
	}

	public Type type() {
		return type;
	}

	public boolean isPlayer() {
		return type == Type.PLAYER;
	}

	public boolean isCoordinates() {
		return type == Type.COORDINATES;
	}

	public UUID playerId() {
		return playerId;
	}

	public String playerName() {
		return playerName;
	}

	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public double z() {
		return z;
	}

	/** {@code null} means "the dimension the operator is currently in". */
	public Identifier dimension() {
		return dimension;
	}

	public String dimensionLabel() {
		return CoordinateManager.dimensionLabel(dimension);
	}

	public String coordinateLabel() {
		return CommandBuilder.formatCoordinate(x) + ", "
				+ CommandBuilder.formatCoordinate(y) + ", "
				+ CommandBuilder.formatCoordinate(z);
	}

	public String describe() {
		if (isPlayer()) {
			return "Player " + playerName;
		}

		return coordinateLabel() + "  (" + dimensionLabel() + ")";
	}
}
