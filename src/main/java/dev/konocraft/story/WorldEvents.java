package dev.konocraft.story;

import dev.konocraft.entity.TownsfolkEntity;
import dev.konocraft.fx.Fx;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.world.KonoWorld;
import dev.konocraft.world.Regions;
import dev.konocraft.world.structure.Graveyard;
import dev.konocraft.world.structure.KeeleDungeon;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;

import java.util.Set;

/**
 * Things that happen at certain times of day: the dead rise in the cemetery on some nights (and Wiz
 * comes to send their souls on), giant toads raid the east gate on some mornings, and the beginner
 * dungeon greets whoever climbs down into it.
 */
public final class WorldEvents {
	public static final String TAG = "konoevent";
	public static final int NONE = 0, GRAVEYARD = 1, TOAD_RAID = 2;
	private static final Set<Integer> QUIET_STAGES = Set.of(Story.CH4_CASTLE, Story.CH5_ALARM, Story.CH5_DEFENSE, Story.CH5_FIGHT, Story.ENDING);
	private static int active = NONE;
	private static int kills, goal, spawned, ticksActive;
	private static int tick;

	private WorldEvents() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(WorldEvents::tick);
	}

	public static int active() {
		return active;
	}

	/** Extra line under the quest objective while an event is on. */
	public static String describe() {
		return switch (active) {
			case GRAVEYARD -> "Ночной квест: упокойте нежить на кладбище — " + kills + "/" + goal;
			case TOAD_RAID -> "Жабы у восточных ворот! Прогнано: " + kills + "/" + goal;
			default -> "";
		};
	}

	private static void tick(MinecraftServer server) {
		if (++tick % 40 != 0) return;
		ServerWorld w = KonoWorld.get(server);
		if (w == null || w.getPlayers().isEmpty()) return;
		StoryState s = StoryState.get(server);
		long day = w.getTimeOfDay() / 24000L, t = w.getTimeOfDay() % 24000L;
		dungeonVisitors(w, s);
		if (active != NONE) {
			ticksActive += 40;
			if (active == GRAVEYARD) graveyardTick(w, t);
			else if (active == TOAD_RAID && ticksActive > 20 * 60 * 6) finish(server, false);
			return;
		}
		boolean ready = s.stage >= Story.CH1_REPORT && !QUIET_STAGES.contains(s.stage)
			&& w.getPlayers().stream().anyMatch(p -> !p.isSpectator() && s.player(p.getUuid()).prologueDone && !DialogueManager.inDialogue(p));
		if (!ready || !w.getGameRules().getBoolean(net.minecraft.world.GameRules.DO_MOB_SPAWNING)) return;
		if (t >= 13500 && t < 19000 && day >= s.nextGraveDay) {
			s.nextGraveDay = day + 2;
			s.markDirty();
			startGraveyard(w, s);
		} else if (t >= 2000 && t < 8000 && day >= s.nextRaidDay) {
			s.nextRaidDay = day + 3;
			s.markDirty();
			startToadRaid(w);
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Night of the undead

	public static void startGraveyard(ServerWorld w, StoryState s) {
		active = GRAVEYARD;
		kills = 0;
		spawned = 0;
		ticksActive = 0;
		goal = 10;
		Cutscenes.alarm(w.getServer(), "На кладбище к северо-западу от города поднялась нежить! Награда: 30 000 эрис.");
		if (s.hasFlag("met_wiz") && s.stage < Story.CH5_WIZ || s.stage >= Story.POSTGAME) {
			Npcs.setOverride("wiz", Locations.GRAVEYARD_WIZ);
			Npcs.refresh(w.getServer());
		}
		Story.syncObjectiveAll(w.getServer());
	}

	private static void graveyardTick(ServerWorld w, long t) {
		if (!TownLife.isNight(t) || ticksActive > 20 * 60 * 12) {
			// sunrise: whatever is left crawls back into the ground
			for (Entity e : tagged(w, new Box(Graveyard.X1 - 40, 0, Graveyard.Z1 - 40, Graveyard.X2 + 40, 200, Graveyard.Z2 + 40))) e.discard();
			finish(w.getServer(), false);
			return;
		}
		int alive = tagged(w, new Box(Graveyard.X1 - 40, 0, Graveyard.Z1 - 40, Graveyard.X2 + 40, 200, Graveyard.Z2 + 40)).size();
		if (alive < 4 && spawned < goal + 4 && w.isChunkLoaded(Regions.GRAVE_X >> 4, Regions.GRAVE_Z >> 4)) {
			int x = Graveyard.X1 + 11 + w.random.nextInt(Graveyard.X2 - Graveyard.X1 - 13);
			int z = Graveyard.Z1 + 2 + w.random.nextInt(Graveyard.Z2 - Graveyard.Z1 - 3);
			EntityType<? extends MobEntity> type = w.random.nextInt(3) == 0 ? ModEntities.UNDEAD_KNIGHT
				: w.random.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON;
			int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
			if (spawn(w, type, x, y, z) != null) {
				spawned++;
				w.spawnParticles(net.minecraft.particle.ParticleTypes.LARGE_SMOKE, x + 0.5, y + 0.5, z + 0.5, 20, 0.4, 0.6, 0.4, 0.02);
			}
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Toad raid on the east gate

	public static void startToadRaid(ServerWorld w) {
		active = TOAD_RAID;
		kills = 0;
		ticksActive = 0;
		goal = 4 + w.random.nextInt(3);
		spawned = 0;
		for (int i = 0; i < goal; i++) {
			int x = Regions.TOWN_HALF + 18 + w.random.nextInt(25);
			int z = -14 + w.random.nextInt(29);
			if (w.isChunkLoaded(x >> 4, z >> 4)) {
				int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
				if (spawn(w, ModEntities.GIANT_TOAD, x, y, z) != null) spawned++;
			}
		}
		goal = Math.max(1, spawned);
		Cutscenes.alarm(w.getServer(), "Стая гигантских жаб у восточных ворот! Отгоните их, пока они не съели стражу!");
		Story.syncObjectiveAll(w.getServer());
	}

	// ---------------------------------------------------------------------------------------------

	private static MobEntity spawn(ServerWorld w, EntityType<? extends MobEntity> type, int x, int y, int z) {
		MobEntity m = type.create(w);
		if (m == null) return null;
		m.refreshPositionAndAngles(x + 0.5, y, z + 0.5, w.random.nextFloat() * 360, 0);
		if (!w.isSpaceEmpty(m)) return null;
		m.initialize(w, w.getLocalDifficulty(m.getBlockPos()), SpawnReason.EVENT, null, null);
		m.addCommandTag(TAG);
		m.setPersistent();
		w.spawnEntity(m);
		return m;
	}

	private static java.util.List<Entity> tagged(ServerWorld w, Box box) {
		return w.getEntitiesByClass(Entity.class, box, e -> e.isAlive() && e.getCommandTags().contains(TAG));
	}

	/** Called for every kill of an event monster. */
	public static void onKill(MinecraftServer server, LivingEntity dead) {
		if (active == NONE || !dead.getCommandTags().contains(TAG)) return;
		kills++;
		if (kills >= goal) finish(server, true);
		else Story.syncObjectiveAll(server);
	}

	public static void stop(MinecraftServer server) {
		finish(server, false);
	}

	private static void finish(MinecraftServer server, boolean won) {
		int was = active;
		active = NONE;
		Npcs.setOverride("wiz", null);
		Npcs.refresh(server);
		ServerWorld w = KonoWorld.get(server);
		if (won && w != null) {
			BlockPos center = was == GRAVEYARD ? new BlockPos(Regions.GRAVE_X, 70, Regions.GRAVE_Z) : new BlockPos(Regions.TOWN_HALF + 25, 70, 0);
			long money = was == GRAVEYARD ? 30000 : 25000;
			int exp = was == GRAVEYARD ? 250 : 180;
			for (ServerPlayerEntity p : w.getPlayers()) {
				if (p.squaredDistanceTo(center.getX(), p.getY(), center.getZ()) > 90 * 90) continue;
				Progression.addMoney(p, money);
				Progression.addExp(p, exp);
				p.sendMessage(Text.literal(was == GRAVEYARD
					? "Экстренный квест выполнен! Нежить упокоена. Награда: 30 000 эрис."
					: "Жабы отогнаны! Стража угощает вас жабьими лапками. Награда: 25 000 эрис.").formatted(Formatting.GOLD), false);
			}
		}
		Story.syncObjectiveAll(server);
	}

	// ---------------------------------------------------------------------------------------------
	// Keele's Dungeon

	private static void dungeonVisitors(ServerWorld w, StoryState s) {
		for (ServerPlayerEntity p : w.getPlayers()) {
			int x = MathHelper.floor(p.getX()), y = MathHelper.floor(p.getY()), z = MathHelper.floor(p.getZ());
			if (!Regions.inDungeon(x, y, z) || p.isSpectator() || DialogueManager.inDialogue(p)) continue;
			PlayerData d = s.player(p.getUuid());
			if (!d.flags.contains("dungeon_seen")) {
				d.flags.add("dungeon_seen");
				s.markDirty();
				DialogueManager.start(p, "dungeon_enter");
			}
		}
	}

	/** Dialogue action "keele_purify": Aqua sends the old lich on to Eris. */
	public static void purifyKeele(ServerPlayerEntity player) {
		ServerWorld w = player.getServerWorld();
		for (TownsfolkEntity k : w.getEntitiesByClass(TownsfolkEntity.class, player.getBoundingBox().expand(24), e -> e.getRole() == TownsfolkEntity.KEELE)) {
			Fx.play(player, "holy", k.getPos(), null);
			k.discard();
		}
		StoryState s = StoryState.get(player.getServer());
		s.setFlag("keele_done", true);
		s.markDirty();
	}

	public static boolean graveyardActive() {
		return active == GRAVEYARD;
	}

	/** Keele waits on his throne; created with the dungeon's chunk. */
	public static void spawnKeele(net.minecraft.world.ChunkRegion region) {
		TownsfolkEntity k = ModEntities.TOWNSFOLK.create(region.toServerWorld());
		if (k == null) return;
		k.refreshPositionAndAngles(KeeleDungeon.KEELE_X + 0.5, KeeleDungeon.FLOOR + 0.5, KeeleDungeon.KEELE_Z + 0.5, -90, 0);
		k.setVariant(TownsfolkEntity.KEELE_VARIANT);
		k.setRole(TownsfolkEntity.KEELE);
		k.setHome(new BlockPos(KeeleDungeon.KEELE_X, KeeleDungeon.FLOOR, KeeleDungeon.KEELE_Z));
		k.setCustomName(Text.literal("Киль").formatted(Formatting.DARK_PURPLE));
		k.setCustomNameVisible(true);
		k.setAiDisabled(true);
		k.setPersistent();
		region.spawnEntityAndPassengers(k);
	}
}
