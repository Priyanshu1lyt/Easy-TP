package com.nebrion.easytp.teleport;

import com.nebrion.easytp.EasyTP;
import com.nebrion.easytp.player.PlayerData;
import com.nebrion.easytp.player.PlayerManager;
import com.nebrion.easytp.team.TeamData;
import com.nebrion.easytp.team.TeamManager;
import com.nebrion.easytp.util.Toasts;
import com.nebrion.easytp.util.Ticker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The teleport engine: resolves targets, builds vanilla commands, sends them with a configurable
 * spacing, then verifies the outcome from the client's own view of the world.
 *
 * <p>A client mod can never force the server to move a player. Success is only reported as
 * CONFIRMED when the operator's client can see the player at the destination; anything else stays
 * UNCONFIRMED, and server error messages are surfaced as FAILED.</p>
 */
public final class TeleportManager {
	private static final TeleportManager INSTANCE = new TeleportManager();

	private static final String[] ERROR_HINTS = {
			"unknown or incomplete command",
			"unknown command",
			"does not have permission",
			"don't have permission",
			"do not have permission",
			"no permission",
			"permission denied",
			"not allowed",
			"player not found",
			"no player was found",
			"that player does not exist",
			"unable to",
			"invalid",
			"incorrect argument",
			"expected",
			"cannot",
			"failed",
			"error"
	};

	private final List<TeleportResult> results = new ArrayList<>();
	private final List<String> feedbackErrors = new ArrayList<>();

	private Destination destination;
	private Destination lastDestination;
	private List<UUID> lastTargets = new ArrayList<>();
	private boolean awaitingFeedback;
	private int pendingCommands;
	private int verifyDelay;

	private TeleportManager() {
	}

	public static TeleportManager get() {
		return INSTANCE;
	}

	public void tick() {
		// Verification and sending are driven by the Ticker; nothing to poll here yet.
	}

	// --- destination ---------------------------------------------------------------------------

	public Destination destination() {
		return destination;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	public void clearDestination() {
		this.destination = null;
	}

	public Destination lastDestination() {
		return lastDestination;
	}

	public List<UUID> lastTargets() {
		return new ArrayList<>(lastTargets);
	}

	// --- target resolution ---------------------------------------------------------------------

	/** Every currently eligible target: explicit selection plus online members of selected teams. */
	public List<UUID> resolveTargets() {
		return new ArrayList<>(TeamManager.get().resolveTargets());
	}

	public List<PlayerData> targetsAsPlayers() {
		List<PlayerData> list = new ArrayList<>();

		for (UUID uuid : resolveTargets()) {
			PlayerData data = PlayerManager.get().get(uuid);

			if (data != null) {
				list.add(data);
			}
		}

		list.sort((first, second) -> String.CASE_INSENSITIVE_ORDER.compare(first.name(), second.name()));
		return list;
	}

	public List<String> selectedTeamNames() {
		List<String> names = new ArrayList<>();

		for (TeamData team : TeamManager.get().selectedTeams()) {
			names.add(team.name());
		}

		return names;
	}

	// --- execution -----------------------------------------------------------------------------

	public List<TeleportResult> results() {
		return new ArrayList<>(results);
	}

	public boolean hasResults() {
		return !results.isEmpty();
	}

	public boolean isBusy() {
		return pendingCommands > 0;
	}

	public int confirmedCount() {
		return count(TeleportResult.Status.CONFIRMED);
	}

	public int failedCount() {
		return count(TeleportResult.Status.FAILED) + count(TeleportResult.Status.DISCONNECTED);
	}

	public int unconfirmedCount() {
		return count(TeleportResult.Status.UNCONFIRMED) + count(TeleportResult.Status.SENT) + count(TeleportResult.Status.PENDING);
	}

	public int count(TeleportResult.Status status) {
		int total = 0;

		for (TeleportResult result : results) {
			if (result.status() == status) {
				total++;
			}
		}

		return total;
	}

	public int totalCount() {
		return results.size();
	}

	/**
	 * Starts a teleport for the given targets. Returns the created result entries; they are updated
	 * in place while the batch is running.
	 */
	public List<TeleportResult> start(MinecraftClient client, Collection<UUID> targetIds, Destination target) {
		results.clear();
		feedbackErrors.clear();
		awaitingFeedback = false;
		pendingCommands = 0;

		if (client == null || target == null) {
			return results;
		}

		PlayerManager.get().refresh(client);
		lastDestination = target;
		lastTargets = new ArrayList<>(targetIds == null ? List.of() : targetIds);

		if (targetIds == null || targetIds.isEmpty()) {
			Toasts.warn("No players selected for teleport.");
			return results;
		}

		if (target.isPlayer() && PlayerManager.get().get(target.playerId()) == null) {
			for (UUID id : targetIds) {
				PlayerData data = PlayerManager.get().get(id);
				results.add(new TeleportResult(id, data == null ? id.toString().substring(0, 8) : data.name(),
						"tp -", TeleportResult.Status.FAILED, "Destination player is offline."));
			}

			return results;
		}

		Identifier currentDimension = CoordinateManager.currentDimension(client);
		List<TeleportResult> queued = new ArrayList<>();

		for (UUID id : targetIds) {
			PlayerData data = PlayerManager.get().get(id);

			if (data == null) {
				results.add(new TeleportResult(id, id.toString().substring(0, 8), "tp -",
						TeleportResult.Status.DISCONNECTED, "Player is no longer online."));
				continue;
			}

			// The destination player is never part of its own teleport target list.
			if (target.isPlayer() && target.playerId().equals(id)) {
				continue;
			}

			String command = target.isPlayer()
					? CommandBuilder.playerTarget(data.name(), target.playerName())
					: CommandBuilder.coordinateTarget(data.name(), target, currentDimension);

			TeleportResult result = new TeleportResult(id, data.name(), command,
					TeleportResult.Status.PENDING, "Queued.");
			results.add(result);
			queued.add(result);
		}

		if (queued.isEmpty()) {
			Toasts.error("Nothing to teleport: check your selection.");
			return results;
		}

		int spacing = Math.max(1, com.nebrion.easytp.config.ConfigManager.get().settings().commandSpacingTicks);
		int delay = 2;
		awaitingFeedback = true;
		pendingCommands = queued.size();

		for (TeleportResult result : queued) {
			final TeleportResult entry = result;
			Ticker.schedule(delay, () -> send(client, entry));
			delay += spacing;
		}

		verifyDelay = delay + 30;
		Ticker.schedule(verifyDelay, () -> verify(client, target, queued));

		Toasts.info("Queued " + queued.size() + " teleport command" + (queued.size() == 1 ? "" : "s") + ".");
		return results;
	}

	private void send(MinecraftClient client, TeleportResult result) {
		if (!CommandExecutor.canSend(client)) {
			result.update(TeleportResult.Status.FAILED, "Disconnected before the command could be sent.");
			pendingCommands = Math.max(0, pendingCommands - 1);
			return;
		}

		// A player who left in the meantime is skipped, never sent a stale command.
		if (PlayerManager.get().get(result.targetId()) == null) {
			result.update(TeleportResult.Status.DISCONNECTED, "Player left the server before the command was sent.");
			pendingCommands = Math.max(0, pendingCommands - 1);
			return;
		}

		boolean sent = CommandExecutor.send(client, result.command());

		if (sent) {
			result.update(TeleportResult.Status.SENT, "Sent - awaiting confirmation.");
		} else {
			result.update(TeleportResult.Status.FAILED, "The client could not send the command.");
		}

		pendingCommands = Math.max(0, pendingCommands - 1);
	}

	private void verify(MinecraftClient client, Destination destination, List<TeleportResult> batch) {
		awaitingFeedback = false;

		if (client == null || client.world == null) {
			return;
		}

		PlayerManager.get().refresh(client);

		double destX = destination.x();
		double destY = destination.y();
		double destZ = destination.z();
		boolean haveDestination = destination.isCoordinates();

		if (destination.isPlayer()) {
			PlayerEntity destinationEntity = client.world.getPlayerByUuid(destination.playerId());

			if (destinationEntity != null) {
				destX = destinationEntity.getX();
				destY = destinationEntity.getY();
				destZ = destinationEntity.getZ();
				haveDestination = true;
			}
		}

		String firstError = feedbackErrors.isEmpty() ? null : feedbackErrors.get(0);

		for (TeleportResult result : batch) {
			if (result.status() != TeleportResult.Status.SENT) {
				continue;
			}

			PlayerData data = PlayerManager.get().get(result.targetId());

			if (data == null) {
				result.update(TeleportResult.Status.DISCONNECTED, "Player is no longer online.");
				continue;
			}

			PlayerEntity entity = client.world.getPlayerByUuid(result.targetId());

			if (entity == null) {
				if (firstError != null) {
					result.update(TeleportResult.Status.FAILED, "Server rejected the command: " + firstError);
				} else {
					result.update(TeleportResult.Status.UNCONFIRMED,
							"Player is not visible in your dimension, so success could not be verified.");
				}

				continue;
			}

			if (!haveDestination) {
				if (firstError != null) {
					result.update(TeleportResult.Status.FAILED, "Server rejected the command: " + firstError);
				} else {
					result.update(TeleportResult.Status.UNCONFIRMED,
							"Destination player is not visible, so success could not be verified.");
				}

				continue;
			}

			double dx = entity.getX() - destX;
			double dy = entity.getY() - destY;
			double dz = entity.getZ() - destZ;
			double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

			if (distance <= 8.0) {
				result.update(TeleportResult.Status.CONFIRMED,
						"Player is at the destination (within " + String.format(Locale.ROOT, "%.1f", distance) + " blocks).");
			} else if (firstError != null) {
				result.update(TeleportResult.Status.FAILED, "Server rejected the command: " + firstError);
			} else {
				// No server error was seen, so the command may still have been rejected silently.
				// Report UNCONFIRMED instead of pretending the teleport either worked or failed.
				result.update(TeleportResult.Status.UNCONFIRMED,
						"Could not verify the teleport: the player is still "
								+ String.format(Locale.ROOT, "%.0f", distance)
								+ " blocks from the destination.");
			}
		}
	}

	/** Called by {@link ChatWatcher} for every server system message. */
	public void onServerMessage(String message) {
		if (!awaitingFeedback || message == null) {
			return;
		}

		String normalized = message.trim();

		if (normalized.isEmpty()) {
			return;
		}

		String lower = normalized.toLowerCase(Locale.ROOT);

		for (String hint : ERROR_HINTS) {
			if (lower.contains(hint)) {
				if (feedbackErrors.size() < 6) {
					feedbackErrors.add(normalized);
				}

				EasyTP.LOGGER.warn("Server rejected a teleport command: {}", normalized);
				return;
			}
		}
	}

	/** Retries only the operations that failed; confirmed operations are never resent. */
	public List<TeleportResult> retryFailed(MinecraftClient client) {
		List<UUID> failed = new ArrayList<>();

		for (TeleportResult result : results) {
			if (result.status().isFailure() || result.status() == TeleportResult.Status.UNCONFIRMED) {
				failed.add(result.targetId());
			}
		}

		Destination target = lastDestination != null ? lastDestination : destination;

		if (failed.isEmpty() || target == null) {
			Toasts.info("There is nothing to retry.");
			return results();
		}

		Toasts.info("Retrying " + failed.size() + " operation" + (failed.size() == 1 ? "" : "s") + ".");
		return start(client, failed, target);
	}

	public void clearResults() {
		results.clear();
		feedbackErrors.clear();
	}
}
