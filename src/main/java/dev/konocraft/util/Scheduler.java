package dev.konocraft.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tiny server-thread task scheduler measured in server ticks. */
public final class Scheduler {
	private static final List<Task> tasks = new ArrayList<>();
	private static final List<Task> pending = new ArrayList<>();
	private static MinecraftServer server;

	private record Task(long due, Runnable action) {
	}

	private static long now;

	private Scheduler() {
	}

	public static void init() {
		ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			server = null;
			tasks.clear();
			pending.clear();
		});
		ServerTickEvents.END_SERVER_TICK.register(s -> tick());
	}

	public static MinecraftServer server() {
		return server;
	}

	public static void after(int ticks, Runnable action) {
		pending.add(new Task(now + Math.max(1, ticks), action));
	}

	private static void tick() {
		now++;
		if (!pending.isEmpty()) {
			tasks.addAll(pending);
			pending.clear();
		}
		Iterator<Task> it = tasks.iterator();
		List<Task> due = new ArrayList<>();
		while (it.hasNext()) {
			Task t = it.next();
			if (t.due() <= now) {
				due.add(t);
				it.remove();
			}
		}
		for (Task t : due) {
			try {
				t.action().run();
			} catch (Exception e) {
				dev.konocraft.KonoCraft.LOGGER.error("Scheduled task failed", e);
			}
		}
	}
}
