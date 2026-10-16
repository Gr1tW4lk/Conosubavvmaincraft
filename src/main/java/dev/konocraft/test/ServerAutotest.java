package dev.konocraft.test;

import dev.konocraft.KonoCraft;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless self-test used by CI: generates the world, runs logic checks and stops the server.
 * Only active with -Dkonocraft.autotest.server=true.
 */
public final class ServerAutotest {
	private static final boolean ENABLED = Boolean.getBoolean("konocraft.autotest.server");
	private static final List<String> failures = new ArrayList<>();
	private static int ticks;

	private ServerAutotest() {
	}

	public static void register() {
		if (!ENABLED) return;
		KonoCraft.LOGGER.info("[AUTOTEST] server autotest enabled");
		ServerLifecycleEvents.SERVER_STARTED.register(ServerAutotest::onStarted);
		ServerTickEvents.END_SERVER_TICK.register(ServerAutotest::onTick);
	}

	public static void check(boolean condition, String message) {
		if (condition) {
			KonoCraft.LOGGER.info("[AUTOTEST] ok   {}", message);
		} else {
			KonoCraft.LOGGER.error("[AUTOTEST] FAIL {}", message);
			failures.add(message);
		}
	}

	private static void onStarted(MinecraftServer server) {
		KonoCraft.LOGGER.info("[AUTOTEST] server started");
	}

	private static void onTick(MinecraftServer server) {
		ticks++;
		if (ticks == 40) {
			if (failures.isEmpty()) {
				KonoCraft.LOGGER.info("[AUTOTEST] RESULT PASS");
			} else {
				KonoCraft.LOGGER.error("[AUTOTEST] RESULT FAIL ({} failures)", failures.size());
			}
			server.stop(false);
		}
	}
}
