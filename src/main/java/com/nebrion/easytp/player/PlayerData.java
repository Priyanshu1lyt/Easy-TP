package com.nebrion.easytp.player;

import net.minecraft.entity.player.SkinTextures;

import java.util.UUID;

/**
 * Immutable snapshot of one entry of the client player list.
 */
public final class PlayerData {
	private final UUID uuid;
	private final String name;
	private final boolean online;
	private final int latency;
	private final String gameMode;
	private final SkinTextures skinTextures;

	public PlayerData(UUID uuid, String name, boolean online, int latency, String gameMode, SkinTextures skinTextures) {
		this.uuid = uuid;
		this.name = name;
		this.online = online;
		this.latency = latency;
		this.gameMode = gameMode == null ? "Unknown" : gameMode;
		this.skinTextures = skinTextures;
	}

	public UUID uuid() {
		return uuid;
	}

	public String name() {
		return name;
	}

	public boolean online() {
		return online;
	}

	public int latency() {
		return latency;
	}

	public String gameMode() {
		return gameMode;
	}

	public SkinTextures skinTextures() {
		return skinTextures;
	}

	public String uuidString() {
		return uuid.toString();
	}
}
