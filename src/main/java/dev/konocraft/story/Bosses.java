package dev.konocraft.story;

import dev.konocraft.entity.BarrierCrystalEntity;
import dev.konocraft.entity.BeldiaEntity;
import dev.konocraft.entity.DestroyerCoreEntity;
import dev.konocraft.entity.DestroyerEntity;
import dev.konocraft.entity.DestroyerGolemEntity;
import dev.konocraft.fx.Fx;
import dev.konocraft.net.ServerNet;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.world.KonoWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Spawning, resetting and finishing the two big fights: Beldia and the Mobile Fortress Destroyer. */
public final class Bosses {
	private static UUID beldia;
	private static UUID destroyer;

	private Bosses() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Bosses::tick);
	}

	// ---------------------------------------------------------------------------------------------
	// The Destroyer marches from far outside the simulation distance, so the chunks around it are
	// kept loaded while it lives; otherwise it would freeze until a player walked up to it.

	private static final Set<Long> forced = new HashSet<>();
	private static int ticks;
	private static boolean staleChecked;

	private static void tick(MinecraftServer server) {
		if (++ticks % 20 != 0) return;
		ServerWorld world = KonoWorld.get(server);
		if (world == null) return;
		// the wide search for a lost Destroyer only makes sense during its chapter
		int stage = StoryState.get(server).stage;
		boolean chapter = stage >= Story.CH5_DEFENSE && stage <= Story.ENDING;
		Entity d = destroyer != null || chapter && ticks % 200 == 0 ? find(server, "destroyer") : null;
		if (d != null && d.isAlive()) {
			keepLoaded(world, d.getChunkPos());
		} else if (!forced.isEmpty() || !staleChecked && !Boolean.getBoolean("konocraft.autotest.server")) {
			staleChecked = true;
			releaseChunks(world);
		}
	}

	private static void keepLoaded(ServerWorld world, ChunkPos center) {
		Set<Long> want = new HashSet<>();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) want.add(ChunkPos.toLong(center.x + dx, center.z + dz));
		}
		for (long l : forced) {
			if (!want.contains(l)) world.setChunkForced(ChunkPos.getPackedX(l), ChunkPos.getPackedZ(l), false);
		}
		for (long l : want) {
			if (!forced.contains(l)) world.setChunkForced(ChunkPos.getPackedX(l), ChunkPos.getPackedZ(l), true);
		}
		forced.clear();
		forced.addAll(want);
	}

	/** Once per server start this also clears chunks left forced by a fight interrupted by a restart. */
	private static void releaseChunks(ServerWorld world) {
		long[] chunks = forced.isEmpty() ? world.getForcedChunks().toLongArray() : forced.stream().mapToLong(Long::longValue).toArray();
		for (long l : chunks) {
			int x = ChunkPos.getPackedX(l), z = ChunkPos.getPackedZ(l);
			if (x >= -20 && x <= 20 && z >= -8 && z <= 50) world.setChunkForced(x, z, false);
		}
		forced.clear();
	}

	public static Entity find(MinecraftServer server, String key) {
		ServerWorld world = KonoWorld.get(server);
		if (world == null) return null;
		UUID id = switch (key) {
			case "beldia" -> beldia;
			case "destroyer" -> destroyer;
			default -> null;
		};
		Entity e = id == null ? null : world.getEntity(id);
		if (e == null && key.equals("destroyer")) {
			// after a reload the static id is gone; look around the battlefield
			var list = world.getEntitiesByClass(DestroyerEntity.class, new Box(-200, -64, -100, 200, 320, 700), d -> true);
			if (!list.isEmpty()) {
				e = list.get(0);
				destroyer = e.getUuid();
			}
		}
		if (e == null && key.equals("beldia")) {
			var list = world.getEntitiesByClass(BeldiaEntity.class, new Box(-300, -64, -400, 400, 320, 100), d -> true);
			if (!list.isEmpty()) {
				e = list.get(0);
				beldia = e.getUuid();
			}
		}
		return e;
	}

	// ---------------------------------------------------------------------------------------------
	// Beldia

	public static void beldiaVisit(ServerPlayerEntity player) {
		ServerWorld world = KonoWorld.get(player.getServer());
		if (world == null) return;
		Entity old = find(player.getServer(), "beldia");
		if (old != null) old.discard();
		BeldiaEntity b = ModEntities.BELDIA.create(world);
		if (b == null) return;
		Locations.Loc loc = Locations.BELDIA_GATE;
		b.refreshPositionAndAngles(loc.x(), loc.y(), loc.z(), loc.yaw(), 0);
		b.setHeadYaw(loc.yaw());
		b.setBodyYaw(loc.yaw());
		b.setVisiting(true);
		world.spawnEntity(b);
		beldia = b.getUuid();
		Fx.play(player, "curse", b.getPos(), null);
	}

	public static void beldiaLeave(MinecraftServer server) {
		Entity e = find(server, "beldia");
		if (e instanceof BeldiaEntity b && b.isVisiting()) {
			ServerWorld w = (ServerWorld) e.getWorld();
			Fx.teleport(w, e.getPos());
			e.discard();
			beldia = null;
		}
	}

	public static boolean beldiaActive(MinecraftServer server) {
		Entity e = find(server, "beldia");
		return e instanceof BeldiaEntity b && b.isAlive() && !b.isVisiting();
	}

	public static void startBeldia(ServerPlayerEntity player) {
		ServerWorld world = KonoWorld.get(player.getServer());
		if (world == null) return;
		Entity old = find(player.getServer(), "beldia");
		if (old != null) old.discard();
		BeldiaEntity b = ModEntities.BELDIA.create(world);
		if (b == null) return;
		Locations.Loc loc = Locations.BELDIA_THRONE;
		b.refreshPositionAndAngles(loc.x(), loc.y(), loc.z(), loc.yaw(), 0);
		b.setVisiting(false);
		b.setPersistent();
		world.spawnEntity(b);
		b.setTarget(player);
		beldia = b.getUuid();
		StoryState.get(player.getServer()).setFlag("beldia_met_castle", true);
		for (ServerPlayerEntity p : world.getPlayers()) {
			if (p.squaredDistanceTo(b) < 80 * 80) ServerNet.sendMusic(p, "boss");
		}
	}

	public static void onBeldiaDefeated(MinecraftServer server, ServerPlayerEntity killer) {
		beldia = null;
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) ServerNet.sendMusic(p, "");
		ServerPlayerEntity target = killer != null ? killer : Party.leader(server);
		if (target != null) DialogueManager.start(target, "ch4_beldia_defeat");
		else Story.setStage(server, Story.CH4_REPORT);
	}

	public static void onBeldiaReset(MinecraftServer server) {
		beldia = null;
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) ServerNet.sendMusic(p, "");
	}

	// ---------------------------------------------------------------------------------------------
	// Destroyer

	public static void startDestroyer(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		ServerWorld world = KonoWorld.get(server);
		if (world == null) return;
		cleanupDestroyer(server);
		DestroyerEntity d = ModEntities.DESTROYER.create(world);
		if (d == null) return;
		Locations.Loc loc = Locations.DESTROYER_START;
		keepLoaded(world, new ChunkPos(net.minecraft.util.math.BlockPos.ofFloored(loc.x(), loc.y(), loc.z())));
		d.refreshPositionAndAngles(loc.x(), loc.y(), loc.z(), 180, 0);
		d.setPersistent();
		world.spawnEntity(d);
		d.spawnCrystals();
		destroyer = d.getUuid();
		Story.setStage(server, Story.CH5_FIGHT);
		for (ServerPlayerEntity p : world.getPlayers()) ServerNet.sendMusic(p, "destroyer");
	}

	/** Removes the Destroyer and everything it spawned. */
	public static void cleanupDestroyer(MinecraftServer server) {
		ServerWorld world = KonoWorld.get(server);
		if (world == null) return;
		Box box = new Box(-300, -64, -100, 300, 320, 800);
		for (Entity e : world.getEntitiesByClass(Entity.class, box, x -> x instanceof DestroyerEntity || x instanceof BarrierCrystalEntity
			|| x instanceof DestroyerCoreEntity || x instanceof DestroyerGolemEntity)) {
			e.discard();
		}
		destroyer = null;
	}

	public static void onDestroyerFail(MinecraftServer server) {
		cleanupDestroyer(server);
		StoryState s = StoryState.get(server);
		s.destroyerAttempts++;
		s.markDirty();
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			ServerNet.sendMusic(p, "");
		}
		Story.setStage(server, Story.CH5_DEFENSE);
		ServerPlayerEntity leader = Party.leader(server);
		if (leader != null) DialogueManager.start(leader, "ch5_fail");
	}

	public static void onCrystalsBroken(MinecraftServer server, DestroyerEntity d) {
		for (ServerPlayerEntity p : ((ServerWorld) d.getWorld()).getPlayers()) {
			if (p.squaredDistanceTo(d) < 120 * 120) {
				DialogueManager.start(p, "ch5_breach");
				return;
			}
		}
		d.collapse();
	}

	public static void onCoreDestroyed(MinecraftServer server, DestroyerEntity d) {
		for (ServerPlayerEntity p : ((ServerWorld) d.getWorld()).getPlayers()) {
			if (p.squaredDistanceTo(d) < 150 * 150) {
				DialogueManager.start(p, "ch5_finale");
				return;
			}
		}
		d.die();
	}

	/** Called by dialogue action "boss <name>". */
	public static void action(MinecraftServer server, String name) {
		Entity e = find(server, "destroyer");
		if (!(e instanceof DestroyerEntity d)) return;
		switch (name) {
			case "collapse" -> d.collapse();
			case "die" -> d.die();
			default -> {
			}
		}
	}

	public static void spawnGolem(ServerWorld world, double x, double y, double z) {
		DestroyerGolemEntity g = ModEntities.DESTROYER_GOLEM.create(world);
		if (g == null) return;
		g.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360, 0);
		g.initialize(world, world.getLocalDifficulty(g.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
		world.spawnEntity(g);
	}
}
