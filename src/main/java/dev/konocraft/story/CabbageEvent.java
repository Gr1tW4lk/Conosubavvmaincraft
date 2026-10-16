package dev.konocraft.story;

import dev.konocraft.entity.FlyingCabbageEntity;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.world.KonoWorld;
import dev.konocraft.world.Regions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

/**
 * Cabbage harvest season: the cabbages of this world grow wings and fly off rather than be eaten.
 * Every few days (after chapter 3) adventurers are called to catch them for a juicy reward.
 */
public final class CabbageEvent {
	private static int tick;
	private static int caughtThisEvent;
	private static int eventTicks;

	private CabbageEvent() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(CabbageEvent::tick);
	}

	public static void start(MinecraftServer server, boolean story) {
		StoryState s = StoryState.get(server);
		s.cabbageEventActive = true;
		s.markDirty();
		caughtThisEvent = 0;
		eventTicks = 0;
		if (!story) {
			Cutscenes.alarm(server, "Капустный сезон! Все авантюристы — на западные поля!");
		}
	}

	public static void stop(MinecraftServer server) {
		StoryState s = StoryState.get(server);
		s.cabbageEventActive = false;
		s.markDirty();
	}

	public static void onCaught(MinecraftServer server) {
		caughtThisEvent++;
	}

	private static void tick(MinecraftServer server) {
		if (++tick % 40 != 0) return;
		StoryState s = StoryState.get(server);
		ServerWorld world = KonoWorld.get(server);
		if (world == null) return;
		long day = world.getTimeOfDay() / 24000L;
		long time = world.getTimeOfDay() % 24000L;
		// Seasonal event after the story chapter
		if (!s.cabbageEventActive && s.stage > Story.CH3_REPORT && s.stage != Story.CH5_FIGHT && day >= s.nextCabbageDay && time > 1000 && time < 5000) {
			s.nextCabbageDay = day + 3;
			s.markDirty();
			start(server, false);
		}
		if (!s.cabbageEventActive) return;
		eventTicks += 40;
		boolean story = s.stage == Story.CH3_CABBAGE;
		if (!story && (eventTicks > 20 * 60 * 5 || caughtThisEvent >= 30)) {
			stop(server);
			for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
				p.sendMessage(Text.literal("Капустный сезон окончен. Капуста улетела зимовать.").formatted(Formatting.GREEN), false);
			}
			return;
		}
		if (story && !s.hasFlag("cabbage_arrived")) return;
		// Keep a swarm of cabbages over the fields while someone is around
		boolean playerNear = false;
		for (ServerPlayerEntity p : world.getPlayers()) {
			if (Regions.inFarms((int) p.getX(), (int) p.getZ()) || Regions.dist(p.getX(), p.getZ(), Locations.CABBAGE_FIELDS.x(), Locations.CABBAGE_FIELDS.z()) < 90) {
				playerNear = true;
				break;
			}
		}
		if (!playerNear) return;
		Box box = new Box(Regions.FARM_X1 - 20, 40, Regions.FARM_Z1 - 20, Regions.FARM_X2 + 20, 120, Regions.FARM_Z2 + 20);
		int count = world.getEntitiesByClass(FlyingCabbageEntity.class, box, e -> true).size();
		int target = story ? 12 : 10;
		for (int i = count; i < target; i++) {
			FlyingCabbageEntity c = ModEntities.FLYING_CABBAGE.create(world);
			if (c == null) break;
			double x = Locations.CABBAGE_FIELDS.x() - 40 + world.random.nextDouble() * 70;
			double z = Locations.CABBAGE_FIELDS.z() - 35 + world.random.nextDouble() * 70;
			c.refreshPositionAndAngles(x, Regions.TOWN_Y + 2 + world.random.nextInt(4), z, world.random.nextFloat() * 360, 0);
			c.initialize(world, world.getLocalDifficulty(c.getBlockPos()), SpawnReason.EVENT, null, null);
			world.spawnEntity(c);
		}
	}
}
