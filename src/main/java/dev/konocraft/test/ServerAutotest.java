package dev.konocraft.test;

import dev.konocraft.KonoCraft;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.story.Dialogues;
import dev.konocraft.story.Locations;
import dev.konocraft.world.Blueprint;
import dev.konocraft.world.KonoWorld;
import dev.konocraft.world.Regions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless self-test used by CI: generates the key areas of the world, checks structures,
 * dialogues and that every entity can be spawned and ticked, then stops the server.
 * Only active with -Dkonocraft.autotest.server=true.
 */
public final class ServerAutotest {
	private static final boolean ENABLED = Boolean.getBoolean("konocraft.autotest.server");
	private static final List<String> failures = new ArrayList<>();
	private static final List<Entity> spawned = new ArrayList<>();
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
		try {
			ServerWorld w = KonoWorld.get(server);
			check(w != null, "KonoSuba dimension exists");
			if (w == null) return;
			long t0 = System.currentTimeMillis();
			Blueprint.get();
			KonoCraft.LOGGER.info("[AUTOTEST] blueprint ready in {} ms", System.currentTimeMillis() - t0);
			t0 = System.currentTimeMillis();
			int chunks = 0;
			for (Locations.Loc loc : Locations.all().values()) {
				int cx = MathHelper.floor(loc.x()) >> 4, cz = MathHelper.floor(loc.z()) >> 4;
				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -2; dz <= 2; dz++) {
						w.getChunk(cx + dx, cz + dz, ChunkStatus.FULL, true);
						chunks++;
					}
				}
			}
			KonoCraft.LOGGER.info("[AUTOTEST] generated {} chunks in {} ms", chunks, System.currentTimeMillis() - t0);
			check(w.getBlockState(new BlockPos(0, Regions.TOWN_Y + 1, 0)).isOf(Blocks.CHISELED_STONE_BRICKS), "fountain pillar at the plaza");
			check(w.getBlockState(new BlockPos(-29, Regions.TOWN_Y + 1, -14)).isOf(Blocks.SPRUCE_DOOR), "guild door");
			check(w.getBlockState(new BlockPos(-43, Regions.TOWN_Y + 2, -27)).isOf(dev.konocraft.registry.ModBlocks.QUEST_BOARD), "quest board in the guild");
			check(w.getBlockState(new BlockPos(Regions.AFTERLIFE_X, Regions.AFTERLIFE_Y, Regions.AFTERLIFE_Z)).isOf(Blocks.SEA_LANTERN), "afterlife hall floor");
			check(!w.getBlockState(new BlockPos(Regions.CASTLE_X - 16, Regions.CASTLE_Y + 5, Regions.CASTLE_Z - 10)).isAir(), "castle keep wall");
			check(w.getBlockState(new BlockPos(0, Regions.TOWN_Y + 1, Regions.TOWN_HALF)).isAir(), "south gate is open");
			// New areas: cemetery, Keele's dungeon and its owner
			int gy = dev.konocraft.world.structure.Graveyard.floor();
			check(w.getBlockState(new BlockPos(dev.konocraft.world.structure.Graveyard.X1 + 3, gy + 1, Regions.GRAVE_Z)).isOf(Blocks.CHISELED_DEEPSLATE)
				|| w.getBlockState(new BlockPos(dev.konocraft.world.structure.Graveyard.X1 + 4, gy + 1, Regions.GRAVE_Z)).isOf(Blocks.CHISELED_DEEPSLATE), "graveyard crypt");
			int kx = dev.konocraft.world.structure.KeeleDungeon.KX1, kz = dev.konocraft.world.structure.KeeleDungeon.KZ1;
			check(w.getBlockState(new BlockPos(kx + 4, Regions.DUNGEON_Y + 1, kz + 2)).isOf(Blocks.SOUL_CAMPFIRE), "keele's chamber");
			check(!w.getEntitiesByClass(dev.konocraft.entity.TownsfolkEntity.class, new net.minecraft.util.math.Box(BlockPos.ofFloored(Locations.KEELE.vec())).expand(6),
				e -> e.getRole() == dev.konocraft.entity.TownsfolkEntity.KEELE).isEmpty(), "keele waits on his throne");
			check(dev.konocraft.story.TownLife.doors().size() > 20, "house doors found: " + dev.konocraft.story.TownLife.doors().size());
			// Restoration: blow a hole in a house and let the builders' bookkeeping put it back
			BlockPos door = dev.konocraft.story.TownLife.doors().get(0);
			java.util.Map<BlockPos, net.minecraft.block.BlockState> before = new java.util.HashMap<>();
			for (BlockPos p : BlockPos.iterate(door.add(-3, -1, -3), door.add(3, 3, 3))) before.put(p.toImmutable(), w.getBlockState(p));
			w.createExplosion(null, door.getX() + 0.5, door.getY() + 1, door.getZ() + 0.5, 2.5f, net.minecraft.world.World.ExplosionSourceType.TNT);
			int broken = 0;
			for (var e : before.entrySet()) if (!w.getBlockState(e.getKey()).equals(e.getValue())) broken++;
			int remembered = dev.konocraft.story.Restoration.pending(w);
			dev.konocraft.story.Restoration.restoreAll(w);
			int still = 0;
			for (var e : before.entrySet()) {
				if (!e.getValue().hasBlockEntity() && !w.getBlockState(e.getKey()).equals(e.getValue())) still++;
			}
			check(broken > 0 && remembered > 0 && still <= 2, "restoration: broken " + broken + ", remembered " + remembered + ", left " + still);
			// Town life: a citizen goes home and comes back out in the morning
			dev.konocraft.entity.TownsfolkEntity citizen = ModEntities.TOWNSFOLK.create(w);
			if (citizen != null) {
				BlockPos d2 = dev.konocraft.story.TownLife.doors().get(dev.konocraft.story.TownLife.doors().size() / 2);
				citizen.refreshPositionAndAngles(d2.getX() + 0.5, d2.getY(), d2.getZ() + 2.5, 0, 0);
				w.spawnEntity(citizen);
				int asleep = dev.konocraft.story.TownLife.sleeping(w);
				citizen.walkTo(d2, dev.konocraft.entity.TownsfolkEntity.GO_HOME);
				dev.konocraft.story.TownLife.arrive(citizen);
				boolean home = citizen.isRemoved() && dev.konocraft.story.TownLife.sleeping(w) == asleep + 1;
				dev.konocraft.story.TownLife.wakeUp(w, 50);
				check(home && dev.konocraft.story.TownLife.sleeping(w) == 0, "citizens go home at night and come out in the morning");
			}
			int dialogues = Dialogues.all().size();
			check(dialogues > 60, "dialogues loaded: " + dialogues);
			// Every entity type can be created and ticked
			double x = 6.5, z = 20.5;
			// no player is online, so keep the test chunks loaded or every entity is unloaded with them
			for (int cx = 0; cx <= 5; cx++) w.setChunkForced(cx, 1, true);
			w.setChunkForced(0, 18, true);
			for (EntityType<?> type : Registries.ENTITY_TYPE) {
				if (!Registries.ENTITY_TYPE.getId(type).getNamespace().equals(KonoCraft.MOD_ID)) continue;
				if (type == ModEntities.DESTROYER || type == ModEntities.MAGIC_BOLT) continue;
				Entity e = type.create(w);
				check(e != null, "create " + Registries.ENTITY_TYPE.getId(type));
				if (e == null) continue;
				e.refreshPositionAndAngles(x, Regions.TOWN_Y + 1, z, 0, 0);
				e.addCommandTag("konotest");
				w.spawnEntity(e);
				spawned.add(e);
				x += 3;
			}
			Entity d = ModEntities.DESTROYER.create(w);
			if (d != null) {
				d.refreshPositionAndAngles(0.5, Regions.TOWN_Y + 1, 300.5, 180, 0);
				w.spawnEntity(d);
				((dev.konocraft.entity.DestroyerEntity) d).spawnCrystals();
				spawned.add(d);
			}
		} catch (Exception e) {
			KonoCraft.LOGGER.error("[AUTOTEST] exception", e);
			failures.add("exception: " + e);
		}
	}

	private static void onTick(MinecraftServer server) {
		ticks++;
		if (ticks == 200) {
			int alive = 0;
			for (Entity e : spawned) {
				if (!e.isRemoved()) alive++;
				else KonoCraft.LOGGER.warn("[AUTOTEST] entity removed: {} ({})", Registries.ENTITY_TYPE.getId(e.getType()), e.getRemovalReason());
			}
			check(alive >= spawned.size() - 2, "spawned entities still alive: " + alive + "/" + spawned.size());
			for (Entity e : spawned) e.discard();
			if (failures.isEmpty()) {
				KonoCraft.LOGGER.info("[AUTOTEST] RESULT PASS");
			} else {
				KonoCraft.LOGGER.error("[AUTOTEST] RESULT FAIL ({} failures): {}", failures.size(), failures);
			}
			server.stop(false);
		}
	}
}
