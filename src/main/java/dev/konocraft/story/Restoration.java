package dev.konocraft.story;

import dev.konocraft.entity.TownsfolkEntity;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.world.Blueprint;
import dev.konocraft.world.KonoWorld;
import dev.konocraft.world.Regions;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Axel rebuilds itself. Blocks destroyed by explosions, by the Destroyer or by adventurers breaking
 * the town's buildings are remembered; after a while builders come out of the nearest house and put
 * them back, and outside the walls grass and flowers grow back on their own.
 * Blocks players place themselves are never touched.
 */
public final class Restoration {
	/** Delay before repairs start: town buildings / nature. */
	public static final int TOWN_DELAY = 20 * 30, WILD_DELAY = 20 * 120;
	private static final int MAX_ENTRIES = 30000, MAX_BUILDERS = 3;
	private static int tick;

	private Restoration() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Restoration::tick);
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (!(world instanceof ServerWorld sw) || !KonoWorld.is(world) || blockEntity != null) return;
			BlockState planned = Blueprint.get().get(pos.getX(), pos.getY(), pos.getZ());
			// only the town's own buildings, roads and fences: not terrain, not what players built
			if (planned != null && planned.getBlock() == state.getBlock()) {
				record(sw, pos, state, Blocks.AIR.getDefaultState(), 0);
			}
		});
	}

	/** Remembers a destroyed block. {@code extraDelay} postpones the repair (e.g. the Destroyer's ramps). */
	public static void record(World world, BlockPos pos, BlockState original, BlockState damaged, int extraDelay) {
		if (!(world instanceof ServerWorld w) || !KonoWorld.is(w)) return;
		// air may be "restored" too: that is how the Destroyer's boarding ramps disappear later
		if (original.equals(damaged) || original.hasBlockEntity() || !original.getFluidState().isEmpty() || original.isOf(Blocks.FIRE)) return;
		State st = State.get(w);
		long key = pos.asLong();
		if (st.entries.containsKey(key) || st.entries.size() >= MAX_ENTRIES) return;
		boolean town = Regions.inTown(pos.getX(), pos.getZ());
		long ready = w.getTime() + (town ? TOWN_DELAY : WILD_DELAY) + extraDelay;
		st.entries.put(key, new Entry(original, damaged, ready));
		st.markDirty();
	}

	/** Repairs everything at once (command / tests). */
	public static void restoreAll(ServerWorld w) {
		State st = State.get(w);
		java.util.List<Long> keys = new java.util.ArrayList<>(st.entries.keySet());
		keys.sort(Comparator.comparingInt(BlockPos::unpackLongY));
		for (long k : keys) restore(w, st, k, st.entries.get(k), false);
	}

	public static int pending(ServerWorld w) {
		return State.get(w).entries.size();
	}

	private static void tick(MinecraftServer server) {
		if (++tick % 10 != 0) return;
		ServerWorld w = KonoWorld.get(server);
		if (w == null) return;
		State st = State.get(w);
		if (st.entries.isEmpty()) return;
		long now = w.getTime();
		List<TownsfolkEntity> builders = w.getEntitiesByClass(TownsfolkEntity.class,
			new Box(-Regions.TOWN_HALF - 20, 30, -Regions.TOWN_HALF - 20, Regions.TOWN_HALF + 20, 160, Regions.TOWN_HALF + 20),
			e -> e.getRole() == TownsfolkEntity.BUILDER);
		// Builders repair what is around them, lowest blocks first so nothing hangs in the air
		for (TownsfolkEntity b : builders) {
			if (b.getScheduleTarget() != null) continue;
			long best = 0;
			Entry bestE = null;
			for (var e : st.entries.long2ObjectEntrySet()) {
				if (e.getValue().ready > now) continue;
				BlockPos p = BlockPos.fromLong(e.getLongKey());
				if (b.getBlockPos().getSquaredDistance(p) > 36) continue;
				if (bestE == null || p.getY() < BlockPos.unpackLongY(best)) {
					best = e.getLongKey();
					bestE = e.getValue();
				}
			}
			if (bestE != null) {
				BlockPos p = BlockPos.fromLong(best);
				b.getLookControl().lookAt(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
				b.swingHand(Hand.MAIN_HAND);
				restore(w, st, best, bestE, true);
			} else {
				BlockPos next = nearestTown(st, now, b.getBlockPos(), 40);
				if (next != null) {
					b.walkTo(next, TownsfolkEntity.GO_WORK);
				} else {
					BlockPos door = TownLife.nearestDoor(b.getBlockPos(), 80);
					b.say("Готово! Как новенькое. До следующего взрыва...");
					if (door != null) b.walkTo(door, TownsfolkEntity.GO_HOME);
					else b.discard();
				}
			}
		}
		// Call builders to fresh damage in town, or fix it quietly when nobody is watching
		BlockPos site = nearestTown(st, now, BlockPos.ORIGIN, 400);
		if (site != null && w.isChunkLoaded(site.getX() >> 4, site.getZ() >> 4)) {
			boolean seen = w.getClosestPlayer(site.getX(), site.getY(), site.getZ(), 96, false) != null;
			if (!seen) {
				restoreAround(w, st, site, 20, now);
			} else if (builders.size() < MAX_BUILDERS && builders.stream().noneMatch(b -> b.squaredDistanceTo(site.getX(), site.getY(), site.getZ()) < 40 * 40)) {
				spawnBuilder(w, site);
			}
		}
		// Outside the walls nature grows back by itself, a few blocks at a time
		int grown = 0;
		List<Long> wild = new ArrayList<>();
		for (var e : st.entries.long2ObjectEntrySet()) {
			if (e.getValue().ready > now) continue;
			BlockPos p = BlockPos.fromLong(e.getLongKey());
			if (Regions.inTown(p.getX(), p.getZ()) || !w.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) continue;
			wild.add(e.getLongKey());
			if (wild.size() >= 64) break;
		}
		wild.sort(Comparator.comparingInt(BlockPos::unpackLongY));
		for (long key : wild) {
			if (grown++ >= 4) break;
			restore(w, st, key, st.entries.get(key), false);
		}
	}

	private static BlockPos nearestTown(State st, long now, BlockPos from, double range) {
		BlockPos best = null;
		double bestD = range * range;
		for (var e : st.entries.long2ObjectEntrySet()) {
			if (e.getValue().ready > now) continue;
			BlockPos p = BlockPos.fromLong(e.getLongKey());
			if (!Regions.inTown(p.getX(), p.getZ())) continue;
			double d = p.getSquaredDistance(from);
			if (d < bestD) {
				bestD = d;
				best = p;
			}
		}
		return best;
	}

	private static void restoreAround(ServerWorld w, State st, BlockPos site, int max, long now) {
		List<Long> keys = new ArrayList<>();
		for (var e : st.entries.long2ObjectEntrySet()) {
			if (e.getValue().ready <= now && BlockPos.fromLong(e.getLongKey()).getSquaredDistance(site) < 24 * 24) keys.add(e.getLongKey());
		}
		keys.sort(Comparator.comparingInt(BlockPos::unpackLongY));
		for (int i = 0; i < keys.size() && i < max; i++) restore(w, st, keys.get(i), st.entries.get(keys.get(i)), false);
	}

	/** Puts one block back if nobody has built something else there in the meantime. */
	private static void restore(ServerWorld w, State st, long key, Entry e, boolean effects) {
		st.entries.remove(key);
		st.markDirty();
		if (e == null) return;
		BlockPos p = BlockPos.fromLong(key);
		if (!w.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) return;
		BlockState now = w.getBlockState(p);
		boolean untouched = now.equals(e.damaged) || e.damaged.isAir() && (now.isAir() || now.isOf(Blocks.FIRE) || now.isReplaceable());
		if (!untouched) return;
		w.setBlockState(p, e.original, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
		if (effects) {
			w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, e.original), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
			w.playSound(null, p, e.original.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 0.8f, 0.9f);
		}
	}

	private static void spawnBuilder(ServerWorld w, BlockPos site) {
		BlockPos door = TownLife.nearestDoor(site, 60);
		BlockPos at = door != null ? TownLife.besideDoor(w, door) : null;
		if (at == null) {
			int y = w.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, site.getX() + 2, site.getZ() + 2);
			at = new BlockPos(site.getX() + 2, y, site.getZ() + 2);
		}
		TownsfolkEntity b = ModEntities.TOWNSFOLK.create(w);
		if (b == null) return;
		b.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		b.setVariant(w.random.nextInt(TownsfolkEntity.VARIANTS));
		b.setRole(TownsfolkEntity.BUILDER);
		b.setHome(at);
		b.equipStack(EquipmentSlot.MAINHAND, new ItemStack(w.random.nextBoolean() ? Items.IRON_SHOVEL : Items.IRON_AXE));
		b.setCustomName(Text.literal("Строитель"));
		b.setPersistent();
		w.spawnEntity(b);
		b.walkTo(site, TownsfolkEntity.GO_WORK);
		String[] lines = {"Опять? Ну кто на этот раз — Мегумин?", "Так, ребята, за работу! Город сам себя не починит.",
			"Счёт за ремонт пришлю в гильдию. На имя Казумы.", "Я уже двадцать лет чиню этот город. Взрывы — самое скучное."};
		b.say(lines[w.random.nextInt(lines.length)]);
	}

	// ---------------------------------------------------------------------------------------------

	record Entry(BlockState original, BlockState damaged, long ready) {
	}

	public static class State extends PersistentState {
		final Long2ObjectOpenHashMap<Entry> entries = new Long2ObjectOpenHashMap<>();

		public static State get(ServerWorld w) {
			return w.getPersistentStateManager().getOrCreate(State::fromNbt, State::new, "konocraft_restoration");
		}

		@Override
		public NbtCompound writeNbt(NbtCompound nbt) {
			NbtList list = new NbtList();
			for (var e : entries.long2ObjectEntrySet()) {
				NbtCompound c = new NbtCompound();
				c.putLong("p", e.getLongKey());
				c.put("o", NbtHelper.fromBlockState(e.getValue().original));
				c.put("d", NbtHelper.fromBlockState(e.getValue().damaged));
				c.putLong("r", e.getValue().ready);
				list.add(c);
			}
			nbt.put("entries", list);
			return nbt;
		}

		static State fromNbt(NbtCompound nbt) {
			State s = new State();
			var blocks = Registries.BLOCK.getReadOnlyWrapper();
			for (NbtElement el : nbt.getList("entries", NbtElement.COMPOUND_TYPE)) {
				NbtCompound c = (NbtCompound) el;
				s.entries.put(c.getLong("p"), new Entry(NbtHelper.toBlockState(blocks, c.getCompound("o")),
					NbtHelper.toBlockState(blocks, c.getCompound("d")), c.getLong("r")));
			}
			return s;
		}
	}
}
