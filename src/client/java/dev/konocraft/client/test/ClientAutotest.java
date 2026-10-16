package dev.konocraft.client.test;

import dev.konocraft.KonoCraft;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Screenshot tour used by CI under Xvfb. Only active with -Dkonocraft.autotest.client=true.
 * Each step runs after a delay; commands execute on the integrated server as the player.
 */
public final class ClientAutotest {
	private static final boolean ENABLED = Boolean.getBoolean("konocraft.autotest.client");

	private record Step(int delay, Consumer<MinecraftClient> action) {
	}

	private static final List<Step> steps = new ArrayList<>();
	private static int stepIndex;
	private static int wait;
	private static boolean started;

	private ClientAutotest() {
	}

	public static void register() {
		if (!ENABLED) return;
		KonoCraft.LOGGER.info("[AUTOTEST] client autotest enabled");
		buildTour();
		ClientTickEvents.END_CLIENT_TICK.register(ClientAutotest::tick);
	}

	public static void command(MinecraftClient client, String command) {
		IntegratedServer server = client.getServer();
		if (server == null || client.player == null) return;
		String playerName = client.player.getGameProfile().getName();
		server.execute(() -> {
			var player = server.getPlayerManager().getPlayer(playerName);
			if (player != null) {
				server.getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(4), command);
			}
		});
	}

	public static void screenshot(MinecraftClient client, String name) {
		ScreenshotRecorder.saveScreenshot(client.runDirectory, name + ".png", client.getFramebuffer(),
			(Text message) -> KonoCraft.LOGGER.info("[AUTOTEST] screenshot {} -> {}", name, message.getString()));
	}

	private static void add(int delay, Consumer<MinecraftClient> action) {
		steps.add(new Step(delay, action));
	}

	private static void shot(String name, String tp) {
		add(10, c -> command(c, tp));
		add(120, c -> screenshot(c, name));
	}

	private static void buildTour() {
		add(100, c -> {
			c.options.hudHidden = false;
			command(c, "gamemode creative");
			command(c, "time set noon");
			command(c, "gamerule doDaylightCycle false");
			command(c, "weather clear");
		});
		shot("00_spawn", "tp @s ~ ~ ~ 0 20");
		add(20, c -> {
			KonoCraft.LOGGER.info("[AUTOTEST] client tour finished");
			c.scheduleStop();
		});
	}

	private static void tick(MinecraftClient client) {
		if (client.world == null || client.player == null) return;
		if (!started) {
			started = true;
			KonoCraft.LOGGER.info("[AUTOTEST] joined world, starting tour of {} steps", steps.size());
			wait = steps.isEmpty() ? 0 : steps.get(0).delay();
		}
		if (stepIndex >= steps.size()) return;
		if (wait-- > 0) return;
		Step step = steps.get(stepIndex++);
		try {
			step.action().accept(client);
		} catch (Exception e) {
			KonoCraft.LOGGER.error("[AUTOTEST] client step {} failed", stepIndex, e);
		}
		if (stepIndex < steps.size()) {
			wait = steps.get(stepIndex).delay();
		}
	}
}
