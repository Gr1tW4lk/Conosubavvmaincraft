package dev.konocraft.client.test;

import dev.konocraft.KonoCraft;
import dev.konocraft.client.screen.CardScreen;
import dev.konocraft.world.Regions;
import dev.konocraft.world.Terrain;
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
	private static final int LOAD = 260;

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

	private static String tp(double x, double y, double z, float yaw, float pitch) {
		return String.format(java.util.Locale.ROOT, "execute in konocraft:konosuba run tp @s %.1f %.1f %.1f %.1f %.1f", x, y, z, yaw, pitch);
	}

	private static double ground(double x, double z) {
		return Math.max(Terrain.heightAt((int) Math.floor(x), (int) Math.floor(z)), Regions.SEA_LEVEL) + 1;
	}

	private static void shot(String name, String command, int delay) {
		add(10, c -> {
			if (c.currentScreen != null) c.setScreen(null);
			command(c, command);
		});
		// chat feedback of the commands would cover the picture; the frame is drawn before the shot
		add(delay - 5, c -> c.inGameHud.getChatHud().clear(false));
		add(5, c -> screenshot(c, name));
	}

	private static void summon(String type, double x, double y, double z, float yaw) {
		add(2, c -> command(c, String.format(java.util.Locale.ROOT,
			"execute in konocraft:konosuba run summon konocraft:%s %.1f %.1f %.1f {NoAI:1b,Rotation:[%.1ff,0f],Tags:[\"konotest\"],PersistenceRequired:1b}",
			type, x, y, z, yaw)));
	}

	private static void buildTour() {
		add(100, c -> {
			c.options.hudHidden = false;
			command(c, "gamemode spectator");
			command(c, "time set 6000");
			command(c, "gamerule doDaylightCycle false");
			command(c, "gamerule doMobSpawning false");
			command(c, "weather clear 100000");
		});
		int ax = Regions.AFTERLIFE_X, ay = Regions.AFTERLIFE_Y, az = Regions.AFTERLIFE_Z;
		shot("01_afterlife", tp(ax + 0.5, ay + 2.6, az + 11.5, 180, 8), LOAD);
		add(10, c -> command(c, "gamemode creative"));
		add(10, c -> command(c, "konocraft dialogue prologue"));
		add(160, c -> screenshot(c, "02_dialogue"));
		add(10, c -> command(c, "konocraft unstuck"));
		add(5, c -> command(c, "gamemode spectator"));
		shot("03_axel_plaza", tp(0.5, 68.5, 30.5, 180, 8), LOAD);
		shot("04_axel_aerial", tp(40, 175, 215, 170, 38), LOAD + 160);
		shot("05_guild", tp(-28.5, 67, 4.5, 180, 4), LOAD);
		shot("06_guild_inside", tp(-15.5, 67.6, -17.5, 125, 12), LOAD);
		shot("07_wiz_shop", tp(23.5, 67.2, -16.5, 180, 14), LOAD);
		shot("08_church", tp(28.5, 67.2, 19.5, 0, 6), LOAD);
		shot("09_stable_houses", tp(-30, 78, 75, 200, 25), LOAD);
		shot("10_castle", tp(196, 104, -190, -135, 18), LOAD + 100);
		shot("11_throne_room", tp(250.5, Regions.CASTLE_Y + 4, -248.5, 180, 8), LOAD);
		shot("12_battlefield", tp(-30, ground(-30, 180) + 12, 180, -40, 15), LOAD);
		shot("13_lake", tp(-40, 82, -205, 180, 20), LOAD);
		shot("14_farms", tp(-200, 80, 75, 140, 20), LOAD);
		// Characters lineup on the plaza
		add(10, c -> command(c, tp(0.5, 65, 22.5, 180, 0)));
		String[] chars = {"megumin", "aqua", "darkness", "kazuma", "eris", "luna", "wiz", "vanir", "yunyun"};
		for (int i = 0; i < chars.length; i++) {
			summon(chars[i], -8 + i * 2 + 0.5, 65, 17.5, 0);
		}
		shot("15_characters", tp(0.5, 66.4, 23.5, 180, 4), 120);
		// Monsters at the toad plains
		double tx = 251, tz = 18;
		add(10, c -> command(c, tp(tx, ground(tx, tz), tz, -90, 0)));
		add(120, c -> {
		});
		summon("giant_toad", tx + 9, ground(tx + 9, tz - 4), tz - 4, 90);
		summon("toad_king", tx + 16, ground(tx + 16, tz + 5), tz + 5, 90);
		summon("goblin", tx + 7, ground(tx + 7, tz + 3), tz + 3, 90);
		summon("undead_knight", tx + 7, ground(tx + 7, tz + 5), tz + 5, 90);
		summon("destroyer_golem", tx + 9, ground(tx + 9, tz + 1), tz + 1, 90);
		summon("flying_cabbage", tx + 6, ground(tx + 6, tz - 1) + 1.5, tz - 1, 90);
		summon("beldia", tx + 8, ground(tx + 8, tz - 8), tz - 8, 90);
		shot("16_monsters", tp(tx - 1, ground(tx, tz) + 1.6, tz, -90, 6), 140);
		// The Destroyer on the southern plain
		double dz = 385;
		summon("destroyer", 0.5, ground(0.5, dz), dz, 180);
		summon("barrier_crystal", 9.5, ground(9.5, dz - 4), dz - 4, 0);
		summon("destroyer_core", -4.5, ground(-4.5, dz - 18), dz - 18, 0);
		shot("17_destroyer", tp(-30, ground(-30, dz - 48) + 8, dz - 48, -32, 4), LOAD);
		// Explosion!
		add(10, c -> command(c, tp(0.5, ground(0.5, 295) + 2, 295, 0, 8)));
		add(LOAD, c -> command(c, "konocraft fx magic_circle"));
		add(50, c -> command(c, "konocraft fx explosion"));
		add(8, c -> screenshot(c, "18_explosion"));
		add(25, c -> screenshot(c, "19_explosion_after"));
		// The new Axel: market, noble quarter, canal, park, bath, inn, streets and lanes, the map
		shot("28_market", tp(0.5, 72, 86.5, 0, 15), LOAD);
		shot("29_noble_quarter", tp(75, 85, -70, 180, 20), LOAD);
		shot("30_canal", tp(-96, 70, -20, 170, 12), LOAD);
		shot("31_park", tp(-88, 75, 60, 135, 20), LOAD);
		shot("32_bath", tp(94.5, 68, 99.5, 0, 25), LOAD);
		shot("33_inn", tp(105.5, 67.5, 14.5, 0, 15), LOAD);
		shot("34_street", tp(-30.5, 66.6, 0.5, 90, 0), LOAD);
		shot("35_lane", tp(-121, 66.6, -60.5, 0, 0), LOAD);
		add(10, c -> command(c, tp(0.5, 66, 30.5, 180, 0)));
		add(LOAD, c -> c.setScreen(new dev.konocraft.client.screen.MapScreen()));
		add(100, c -> screenshot(c, "36_map"));
		add(10, c -> c.setScreen(null));
		// New places: the cemetery by day and on a night of the undead, Axel at night, Keele's dungeon
		int gy = dev.konocraft.world.structure.Graveyard.floor();
		shot("23_graveyard", tp(Regions.GRAVE_X + 26, gy + 7, Regions.GRAVE_Z + 0.5, 90, 18), LOAD);
		add(10, c -> {
			command(c, "time set 18000");
			command(c, "konocraft event graveyard");
		});
		shot("24_graveyard_night", tp(Regions.GRAVE_X + 26, gy + 7, Regions.GRAVE_Z + 0.5, 90, 18), 220);
		shot("25_town_night", tp(0.5, 70, 40.5, 180, 8), LOAD);
		add(5, c -> {
			command(c, "konocraft event stop");
			command(c, "time set 6000");
		});
		int[] hall = dev.konocraft.world.structure.KeeleDungeon.ROOMS[0];
		shot("26_dungeon", tp(Regions.DUNGEON_X + 0.5, Regions.DUNGEON_Y + 2.5, hall[3] - 1.5, 180, 12), LOAD);
		shot("27_keele", tp(dev.konocraft.world.structure.KeeleDungeon.KEELE_X + 6.5, Regions.DUNGEON_Y + 1.2,
			dev.konocraft.world.structure.KeeleDungeon.KEELE_Z + 0.5, 90, 4), LOAD);
		// UI
		add(40, c -> {
			command(c, "konocraft class adventurer");
			command(c, "konocraft money 123456");
			command(c, "konocraft stage 21");
		});
		add(60, c -> screenshot(c, "20_hud"));
		add(10, c -> c.setScreen(new CardScreen()));
		add(30, c -> screenshot(c, "21_card"));
		add(10, c -> c.setScreen(null));
		add(10, c -> command(c, "konocraft shop wiz"));
		add(40, c -> screenshot(c, "22_shop"));
		add(20, c -> {
			KonoCraft.LOGGER.info("[AUTOTEST] client tour finished");
			c.scheduleStop();
		});
	}

	private static int idle;

	private static void tick(MinecraftClient client) {
		if (client.world == null || client.player == null) {
			// report what is blocking the world from opening (title screen, a prompt, loading...)
			if (++idle % 200 == 0) {
				String screen = client.currentScreen == null ? "none" : client.currentScreen.getClass().getName()
					+ " \"" + client.currentScreen.getTitle().getString() + "\"";
				KonoCraft.LOGGER.info("[AUTOTEST] waiting for world ({} ticks), screen: {}", idle, screen);
				if (idle <= 1200) screenshot(client, "00_waiting_" + idle);
			}
			if (idle == 4000) {
				KonoCraft.LOGGER.error("[AUTOTEST] world never opened, giving up");
				client.scheduleStop();
			}
			return;
		}
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
