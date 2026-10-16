package dev.konocraft.client.test;

import dev.konocraft.KonoCraft;
import dev.konocraft.client.ClientState;
import dev.konocraft.client.screen.DialogueScreen;
import dev.konocraft.entity.KonoNpcEntity;
import dev.konocraft.registry.ModEntities;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * CI story playthrough (-Dkonocraft.autotest.story=true): a stubborn player who clicks through every
 * dialogue, walks (teleports) to each objective, talks to whoever stands there and defeats whatever
 * the quest asks for. It logs every line and objective, so a broken quest shows up as a stuck run.
 */
public final class StoryBot {
	private static final boolean ENABLED = Boolean.getBoolean("konocraft.autotest.story");
	private static final int STUCK_TICKS = 20 * 240;
	private static final int TIME_LIMIT = 20 * 60 * 30;

	private static int ticks;
	private static int sinceProgress;
	private static int shots;
	private static boolean setUp;
	private static boolean finished;
	private static boolean complete;
	private static int idleAfterEnd;
	private static int endingLines;
	private static int stopAt;
	private static String lastObjective = "";
	private static String lastLine = "";
	private static int sameLineCount;
	private static int talkCooldown;
	private static int actionCooldown;
	private static final Map<String, Integer> seenLines = new HashMap<>();

	private StoryBot() {
	}

	public static void register() {
		if (!ENABLED) return;
		KonoCraft.LOGGER.info("[AUTOTEST] story playthrough enabled");
		ClientTickEvents.END_CLIENT_TICK.register(StoryBot::tick);
	}

	private static void log(String msg, Object... args) {
		KonoCraft.LOGGER.info("[STORY] " + msg, args);
	}

	private static void tick(MinecraftClient c) {
		if (c.world == null || c.player == null) return;
		ticks++;
		if (finished) {
			if (ticks == stopAt) c.scheduleStop();
			return;
		}
		sinceProgress++;
		if (ticks < 100) return;
		if (!setUp) {
			setUp = true;
			ClientAutotest.command(c, "gamemode creative");
			ClientAutotest.command(c, "gamerule doDaylightCycle false");
			ClientAutotest.command(c, "time set 6000");
			ClientAutotest.command(c, "weather clear 1000000");
			ClientAutotest.command(c, "gamerule sendCommandFeedback false");
		}
		String objective = ClientState.objective == null ? "" : ClientState.objective;
		if (!objective.equals(lastObjective)) {
			lastObjective = objective;
			sinceProgress = 0;
			log("objective: {}", objective);
			shot(c, "objective");
			if (objective.contains("Свободная игра") && !complete) {
				KonoCraft.LOGGER.info("[AUTOTEST] STORY COMPLETE in {} s", ticks / 20);
				complete = true;
			}
		}
		if (sinceProgress > STUCK_TICKS || ticks > TIME_LIMIT) {
			KonoCraft.LOGGER.error("[AUTOTEST] STORY STUCK at \"{}\" (screen: {})", objective,
				c.currentScreen == null ? "none" : c.currentScreen.getClass().getSimpleName());
			shot(c, "stuck");
			finish(c);
			return;
		}
		if (talkCooldown > 0) talkCooldown--;
		if (actionCooldown > 0) actionCooldown--;
		if (ticks % 5 != 0) return;

		// After the last quest, watch the ending to its last line, then leave the world
		if (complete) {
			if (c.currentScreen instanceof DialogueScreen) {
				idleAfterEnd = 0;
			} else if (++idleAfterEnd > 40) {
				shot(c, "after_ending");
				finish(c);
				return;
			}
		}
		// 1. Dialogues: read everything, choose a different answer each time the same question repeats
		if (c.currentScreen instanceof DialogueScreen d) {
			String line = d.autotestLine();
			if (!line.equals(lastLine)) {
				lastLine = line;
				sameLineCount = 0;
				sinceProgress = Math.min(sinceProgress, STUCK_TICKS / 2);
				log("{}", line);
			} else if (++sameLineCount > 400) {
				log("dialogue line never advanced, closing: {}", line);
				c.setScreen(null);
				ClientAutotest.command(c, "konocraft unstuck");
				return;
			}
			if (complete && ++endingLines % 6 == 0) shot(c, "ending");
			int visits = seenLines.merge(line, 1, Integer::sum);
			d.autotestStep(visits - 1);
			return;
		}
		if (c.currentScreen != null && !(c.currentScreen instanceof ChatScreen)) {
			log("closing screen {}", c.currentScreen.getClass().getSimpleName());
			c.setScreen(null);
			return;
		}

		// 2. Fights and quest targets
		if (!complete && actionCooldown == 0 && fight(c, objective)) {
			actionCooldown = 20;
			return;
		}

		// 3. Go to the objective and talk to whoever is there
		Vec3d target = ClientState.objectiveTarget;
		if (target == null) return;
		Vec3d pos = c.player.getPos();
		double dist = Math.sqrt(Math.pow(pos.x - target.x, 2) + Math.pow(pos.z - target.z, 2));
		if (dist > 6) {
			if (actionCooldown == 0) {
				tp(c, target.x + 0.5, target.y, target.z + 0.5);
				actionCooldown = 40;
			}
			return;
		}
		if (talkCooldown == 0) {
			KonoNpcEntity npc = nearestNpc(c, target, 10);
			if (npc != null) {
				if (c.player.squaredDistanceTo(npc) > 9) {
					tp(c, npc.getX() + 1.2, npc.getY(), npc.getZ() + 1.2);
				} else {
					log("talk to {}", npc.characterKey());
					c.interactionManager.interactEntity(c.player, npc, Hand.MAIN_HAND);
				}
				talkCooldown = 60;
			}
		}
	}

	/** Quest fights: returns true when it did something this round. */
	private static boolean fight(MinecraftClient c, String objective) {
		String o = objective.toLowerCase(Locale.ROOT);
		if (o.contains("жаб") && o.contains("убейте")) {
			return attackNearest(c, ModEntities.GIANT_TOAD, 90);
		}
		if (o.contains("капуст") && o.contains("поймайте")) {
			return attackNearest(c, ModEntities.FLYING_CABBAGE, 90);
		}
		if (o.contains("бельди") || o.contains("тронном зале")) {
			return attackNearest(c, ModEntities.BELDIA, 40);
		}
		if (o.contains("разрушитель")) {
			if (attackNearest(c, ModEntities.BARRIER_CRYSTAL, 200) || attackNearest(c, ModEntities.DESTROYER_CORE, 200)) return true;
			Entity d = nearest(c, ModEntities.DESTROYER, 400);
			if (d == null) {
				ClientAutotest.command(c, "execute at @e[type=konocraft:destroyer,limit=1] run tp @s ~ ~25 ~-45 0 30");
			} else if (c.player.squaredDistanceTo(d) > 60 * 60) {
				tp(c, d.getX(), d.getY() + 25, d.getZ() - 45);
			}
			return true;
		}
		return false;
	}

	private static boolean attackNearest(MinecraftClient c, EntityType<?> type, double range) {
		Entity e = nearest(c, type, range);
		if (e == null) return false;
		if (c.player.squaredDistanceTo(e) > 24 * 24) {
			tp(c, e.getX() + 3, e.getY() + 1, e.getZ() + 3);
		}
		ClientAutotest.command(c, "damage " + e.getUuidAsString() + " 60 minecraft:player_attack by @s");
		return true;
	}

	private static Entity nearest(MinecraftClient c, EntityType<?> type, double range) {
		Entity best = null;
		double bestD = range * range;
		for (Entity e : c.world.getEntities()) {
			if (e.getType() != type || !e.isAlive()) continue;
			double d = e.squaredDistanceTo(c.player);
			if (d < bestD) {
				bestD = d;
				best = e;
			}
		}
		return best;
	}

	private static KonoNpcEntity nearestNpc(MinecraftClient c, Vec3d near, double range) {
		KonoNpcEntity best = null;
		double bestD = range * range;
		for (Entity e : c.world.getEntities()) {
			if (!(e instanceof KonoNpcEntity npc)) continue;
			double d = e.squaredDistanceTo(near);
			if (d < bestD) {
				bestD = d;
				best = npc;
			}
		}
		return best;
	}

	private static void tp(MinecraftClient c, double x, double y, double z) {
		ClientAutotest.command(c, String.format(Locale.ROOT, "execute in konocraft:konosuba run tp @s %.1f %.1f %.1f", x, y, z));
	}

	private static void shot(MinecraftClient c, String what) {
		ClientAutotest.screenshot(c, String.format(Locale.ROOT, "story_%02d_%s", ++shots, what));
	}

	private static void finish(MinecraftClient c) {
		finished = true;
		stopAt = ticks + 60;
		KonoCraft.LOGGER.info("[AUTOTEST] story playthrough finished");
	}
}
