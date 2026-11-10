package com.nebrion.easytp.teleport;

import java.util.UUID;

/**
 * Outcome of one teleport command, shown on the results screen.
 */
public final class TeleportResult {
	public enum Status {
		/** Queued, not sent yet. */
		PENDING("PENDING", 0xFF9A9AA5),
		/** Command was sent, the server has not confirmed it yet. */
		SENT("UNCONFIRMED", 0xFFF5A524),
		/** Position check confirmed that the player moved. */
		CONFIRMED("CONFIRMED", 0xFF22C55E),
		/** The server rejected the command or the player did not move. */
		FAILED("FAILED", 0xFFEF4444),
		/** Could not be verified (different dimension, entity not loaded, ...). */
		UNCONFIRMED("UNCONFIRMED", 0xFFF5A524),
		/** The player is no longer online. */
		DISCONNECTED("DISCONNECTED", 0xFFF97316);

		private final String label;
		private final int color;

		Status(String label, int color) {
			this.label = label;
			this.color = color;
		}

		public String label() {
			return label;
		}

		public int color() {
			return color;
		}

		public boolean isFailure() {
			return this == FAILED || this == DISCONNECTED;
		}

		public boolean isSuccess() {
			return this == CONFIRMED;
		}

		public boolean isPending() {
			return this == PENDING || this == SENT || this == UNCONFIRMED;
		}
	}

	private final UUID targetId;
	private final String targetName;
	private final String command;
	private Status status;
	private String message;

	public TeleportResult(UUID targetId, String targetName, String command, Status status, String message) {
		this.targetId = targetId;
		this.targetName = targetName;
		this.command = command;
		this.status = status;
		this.message = message;
	}

	public UUID targetId() {
		return targetId;
	}

	public String targetName() {
		return targetName;
	}

	public String command() {
		return command;
	}

	public Status status() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String message() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void update(Status status, String message) {
		this.status = status;
		this.message = message;
	}
}
