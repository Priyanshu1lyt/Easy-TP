package com.nebrion.easytp.util;

import com.nebrion.easytp.EasyTP;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A tiny client-tick scheduler. Used for delayed teleport command execution and for deferred
 * verification runs. Everything runs on the client thread.
 */
public final class Ticker {
	private static final List<Task> TASKS = new ArrayList<>();

	private Ticker() {
	}

	public static void schedule(int delayTicks, Runnable action) {
		if (action == null) {
			return;
		}

		TASKS.add(new Task(Math.max(0, delayTicks), action));
	}

	public static void tick() {
		if (TASKS.isEmpty()) {
			return;
		}

		List<Task> due = null;
		Iterator<Task> iterator = TASKS.iterator();

		while (iterator.hasNext()) {
			Task task = iterator.next();

			if (--task.ticksLeft <= 0) {
				iterator.remove();

				if (due == null) {
					due = new ArrayList<>();
				}

				due.add(task);
			}
		}

		if (due == null) {
			return;
		}

		for (Task task : due) {
			try {
				task.action.run();
			} catch (Throwable throwable) {
				EasyTP.LOGGER.error("Easy TP scheduled task failed", throwable);
			}
		}
	}

	public static void clear() {
		TASKS.clear();
	}

	private static final class Task {
		private int ticksLeft;
		private final Runnable action;

		private Task(int ticksLeft, Runnable action) {
			this.ticksLeft = ticksLeft;
			this.action = action;
		}
	}
}
