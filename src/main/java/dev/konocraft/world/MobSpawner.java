package dev.konocraft.world;

import dev.konocraft.entity.GiantToadEntity;
import dev.konocraft.entity.KonoMonsterEntity;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.story.PlayerData;
import dev.konocraft.story.Story;
import dev.konocraft.story.StoryState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;

/**
 * Natural spawning for the KonoSuba world. Biomes have no vanilla spawns; instead each region gets
 * its own wildlife: toads on the plains, goblins in the forest, undead around the castle.
 */
public final class MobSpawner {
	private static int tick;
	private static final int CAP = 7;

	private MobSpawner() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(MobSpawner::tick);
	}

	private static void tick(MinecraftServer server) {
		if (++tick % 60 != 0) return;
		ServerWorld world = KonoWorld.get(server);
		if (world == null || world.getPlayers().isEmpty()) return;
		if (!world.getGameRules().getBoolean(net.minecraft.world.GameRules.DO_MOB_SPAWNING)) return;
		StoryState state = StoryState.get(server);
		for (ServerPlayerEntity p : world.getPlayers()) {
			if (p.isSpectator()) continue;
			int px = MathHelper.floor(p.getX()), pz = MathHelper.floor(p.getZ());
			if (Regions.inTown(px, pz) || Regions.inAfterlife(px, pz)) continue;
			if (Regions.inDungeon(px, MathHelper.floor(p.getY()), pz)) {
				dungeonSpawn(world, p);
				continue;
			}
			int nearby = world.getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(56, 32, 56),
				e -> e instanceof HostileEntity && !(e instanceof dev.konocraft.entity.BossEntity)).size();
			if (nearby < CAP) trySpawn(world, p, state);
			ensureToadKing(world, p, state);
		}
	}

	private static void trySpawn(ServerWorld world, ServerPlayerEntity player, StoryState state) {
		var random = world.random;
		double a = random.nextDouble() * Math.PI * 2;
		double d = 26 + random.nextDouble() * 22;
		int x = MathHelper.floor(player.getX() + Math.cos(a) * d);
		int z = MathHelper.floor(player.getZ() + Math.sin(a) * d);
		if (!world.isChunkLoaded(x >> 4, z >> 4)) return;
		if (Regions.inTown(x, z) || Regions.inAfterlife(x, z)) return;
		int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos pos = new BlockPos(x, y, z);
		BlockState below = world.getBlockState(pos.down());
		if (!below.isSolid() || !world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) return;
		if (world.getBlockState(pos.down()).isOf(net.minecraft.block.Blocks.WATER)) return;
		boolean night = world.isNight();
		EntityType<? extends MobEntity> type = switch (Regions.biomeAt(x, z)) {
			case TOAD_PLAINS -> ModEntities.GIANT_TOAD;
			case LAKE -> random.nextInt(3) == 0 ? ModEntities.GIANT_TOAD : null;
			case FOREST -> ModEntities.GOBLIN;
			case CASTLE -> state.stage >= Story.CH4_GATE || night ? ModEntities.UNDEAD_KNIGHT : null;
			case BATTLEFIELD -> state.stage >= Story.ENDING && random.nextInt(4) == 0 ? ModEntities.DESTROYER_GOLEM : (night ? ModEntities.GOBLIN : null);
			case PLAINS -> night ? (random.nextBoolean() ? ModEntities.GOBLIN : ModEntities.GIANT_TOAD) : (random.nextInt(4) == 0 ? ModEntities.GIANT_TOAD : null);
			default -> null;
		};
		if (type == null) return;
		// Not during the cabbage/destroyer story moments near the action
		if (state.stage == Story.CH5_FIGHT && Regions.inBattlefield(x, z)) return;
		int group = type == ModEntities.GOBLIN ? 1 + random.nextInt(3) : 1;
		for (int i = 0; i < group; i++) {
			MobEntity mob = type.create(world);
			if (mob == null) return;
			mob.refreshPositionAndAngles(x + 0.5 + i, y, z + 0.5, random.nextFloat() * 360, 0);
			if (!world.isSpaceEmpty(mob)) continue;
			mob.initialize(world, world.getLocalDifficulty(pos), SpawnReason.NATURAL, null, null);
			world.spawnEntity(mob);
		}
	}

	/** The beginner dungeon keeps its undead coming: Aqua's holy aura attracts them, she says. */
	private static void dungeonSpawn(ServerWorld world, ServerPlayerEntity player) {
		int nearby = world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(40, 12, 40), e -> e instanceof HostileEntity).size();
		if (nearby >= 5) return;
		var random = world.random;
		int[] room = dev.konocraft.world.structure.KeeleDungeon.ROOMS[random.nextInt(dev.konocraft.world.structure.KeeleDungeon.ROOMS.length)];
		int x = room[0] + random.nextInt(room[2] - room[0] + 1), z = room[1] + random.nextInt(room[3] - room[1] + 1);
		int y = dev.konocraft.world.structure.KeeleDungeon.FLOOR;
		if (player.squaredDistanceTo(x, y, z) < 12 * 12 || !world.isChunkLoaded(x >> 4, z >> 4)) return;
		EntityType<? extends MobEntity> type = random.nextInt(10) < 6 ? ModEntities.UNDEAD_KNIGHT : EntityType.SKELETON;
		MobEntity mob = type.create(world);
		if (mob == null) return;
		mob.refreshPositionAndAngles(x + 0.5, y, z + 0.5, random.nextFloat() * 360, 0);
		if (!world.isSpaceEmpty(mob)) return;
		mob.initialize(world, world.getLocalDifficulty(mob.getBlockPos()), SpawnReason.NATURAL, null, null);
		world.spawnEntity(mob);
	}

	/** The Toad King only appears while somebody hunts it. */
	private static void ensureToadKing(ServerWorld world, ServerPlayerEntity player, StoryState state) {
		PlayerData d = state.player(player.getUuid());
		if (!"toadking".equals(d.bounty)) return;
		if (Regions.dist(player.getX(), player.getZ(), Regions.TOAD_X, Regions.TOAD_Z) > 140) return;
		boolean exists = !world.getEntitiesByClass(GiantToadEntity.class, player.getBoundingBox().expand(160, 64, 160), GiantToadEntity::isKing).isEmpty();
		if (exists) return;
		int[] pond = Regions.PONDS[(int) (world.getTime() / 1200 % Regions.PONDS.length)];
		int x = pond[0] + pond[2] + 3, z = pond[1];
		if (!world.isChunkLoaded(x >> 4, z >> 4)) return;
		int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
		GiantToadEntity king = ModEntities.TOAD_KING.create(world);
		if (king == null) return;
		king.refreshPositionAndAngles(x + 0.5, y, z + 0.5, 0, 0);
		king.initialize(world, world.getLocalDifficulty(king.getBlockPos()), SpawnReason.EVENT, null, null);
		king.setPersistent();
		world.spawnEntity(king);
		player.sendMessage(net.minecraft.text.Text.literal("Земля дрожит... У прудов квакает что-то ОЧЕНЬ большое.").formatted(net.minecraft.util.Formatting.DARK_GREEN), false);
	}

	public static boolean isKonoMonster(EntityType<?> type) {
		return KonoMonsterEntity.isMonster(type) || type == ModEntities.GIANT_TOAD;
	}
}
