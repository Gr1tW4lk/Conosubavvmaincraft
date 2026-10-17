package dev.konocraft.story;

import dev.konocraft.entity.TownsfolkEntity;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.world.Blueprint;
import dev.konocraft.world.KonoWorld;
import dev.konocraft.world.Regions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;

/**
 * The daily rhythm of Axel. Citizens wander the streets by day, a third of them spend the evening in
 * the guild tavern, at night everybody goes home through their front door (and comes out of it
 * again in the morning) while guards with lanterns watch the four gates.
 */
public final class TownLife {
	private static final int HALF = Regions.TOWN_HALF;
	public static final BlockPos GUILD_DOOR = new BlockPos(-29, Regions.TOWN_Y + 1, -14);
	private static final double[][] TAVERN_SPOTS = {
		{-22.5, -21.5}, {-24.5, -26.5}, {-31.5, -24.5}, {-34.5, -28.5}, {-21.5, -29.5}, {-27.5, -22.5}, {-33.5, -21.5}
	};
	/** The inn by the east gate: its dining hall is the second tavern of the town. */
	public static final BlockPos INN_DOOR = new BlockPos(101, Regions.TOWN_Y + 1, 11);
	private static final double[][] INN_SPOTS = {
		{101.5, 18.5}, {105.5, 22.5}, {109.5, 26.5}, {101.5, 26.5}, {105.5, 18.5}, {109.5, 22.5}
	};
	private static final int[][] GATES = {{0, -(HALF - 4)}, {0, HALF - 4}, {HALF - 4, 0}, {-(HALF - 4), 0}};
	private static List<BlockPos> doors;
	private static int tick;

	private TownLife() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(TownLife::tick);
	}

	/** Lower halves of the wooden house doors inside the walls (from the world blueprint). */
	public static List<BlockPos> doors() {
		if (doors == null) {
			doors = Blueprint.get().find(s -> s.getBlock() instanceof DoorBlock && !s.isOf(Blocks.IRON_DOOR)
				&& s.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER, -HALF + 2, -HALF + 2, HALF - 2, HALF - 2);
			// front doors only: rooms upstairs in the inn are not anybody's home
			doors.removeIf(p -> p.getY() != Regions.TOWN_Y + 1);
		}
		return doors;
	}

	public static BlockPos nearestDoor(BlockPos from, double maxDist) {
		BlockPos best = null;
		double bestD = maxDist * maxDist;
		for (BlockPos d : doors()) {
			double dd = d.getSquaredDistance(from);
			if (dd < bestD) {
				bestD = dd;
				best = d;
			}
		}
		return best;
	}

	public static boolean isNight(long t) {
		return t >= 13000 && t < 23000;
	}

	private static void tick(MinecraftServer server) {
		if (++tick % 40 != 0) return;
		ServerWorld w = KonoWorld.get(server);
		if (w == null) return;
		// The town only lives while somebody is near enough to see it
		boolean watched = false;
		for (var p : w.getPlayers()) {
			if (Math.abs(p.getX()) < HALF + 140 && Math.abs(p.getZ()) < HALF + 140 && !p.isSpectator()) watched = true;
		}
		if (!watched) return;
		long t = w.getTimeOfDay() % 24000L;
		boolean night = isNight(t), evening = t >= 11000 && t < 13000, late = t >= 18000 && t < 23000;
		List<TownsfolkEntity> folk = w.getEntitiesByClass(TownsfolkEntity.class,
			new Box(-HALF - 8, 40, -HALF - 8, HALF + 8, 140, HALF + 8), e -> e.isAlive());
		int assigned = 0;
		for (TownsfolkEntity f : folk) {
			if (f.getRole() != TownsfolkEntity.CITIZEN || f.getScheduleTarget() != null || assigned >= 6) continue;
			boolean tavernGoer = Math.floorMod(f.getUuid().hashCode(), 100) < 35;
			boolean inTavern = inTavern(f);
			if (night && (!tavernGoer || late)) {
				BlockPos door = inTavern ? tavernDoor(f) : nearestDoor(f.getBlockPos(), 48);
				if (door != null) {
					f.walkTo(door, TownsfolkEntity.GO_HOME);
					assigned++;
				}
			} else if ((evening || night) && tavernGoer && !inTavern) {
				f.walkTo(tavernDoor(f), TownsfolkEntity.GO_TAVERN);
				assigned++;
			}
		}
		workers(w, folk);
		if (night) {
			guards(w, folk);
		} else {
			for (TownsfolkEntity f : folk) {
				if (f.getRole() == TownsfolkEntity.GUARD && f.getScheduleTarget() == null) {
					BlockPos door = nearestDoor(f.getBlockPos(), 80);
					if (door != null) f.walkTo(door, TownsfolkEntity.GO_HOME);
					else f.discard();
				}
			}
			if (!evening) wakeUp(w, 2);
		}
	}

	private static boolean inTavern(TownsfolkEntity f) {
		return f.getX() > -46 && f.getX() < -12 && f.getZ() > -42 && f.getZ() < -14.5
			|| f.getX() > 88 && f.getX() < 116 && f.getZ() > 12 && f.getZ() < 34 && f.getY() < Regions.TOWN_Y + 4;
	}

	/** East-siders drink at the inn, everybody else at the guild. */
	private static BlockPos tavernDoor(TownsfolkEntity f) {
		BlockPos h = f.getOriginalHome();
		return h.getSquaredDistance(INN_DOOR) < h.getSquaredDistance(GUILD_DOOR) ? INN_DOOR : GUILD_DOOR;
	}

	/** Townspeople with a job: they always stand at their counter (and come back if they were removed). */
	public record Worker(int role, String loc, String name, int variant, net.minecraft.item.Item item) {
	}

	public static final Worker[] WORKERS = {
		new Worker(TownsfolkEntity.SMITH, "smith", "Кузнец Брок", 7, Items.IRON_AXE),
		new Worker(TownsfolkEntity.INNKEEPER, "innkeeper", "Хозяйка гостиницы Марта", 8, Items.AIR),
		new Worker(TownsfolkEntity.BATHKEEPER, "bath_keeper", "Банщица Юки", 9, Items.AIR),
		new Worker(TownsfolkEntity.POLICE, "sena", "Сена", 10, Items.BOOK),
		new Worker(TownsfolkEntity.LORD, "alderp_lord", "Лорд Алдерп", 11, Items.GOLD_INGOT),
		new Worker(TownsfolkEntity.NOBLE, "ignis", "Лорд Игнис Дастинесс", 12, Items.AIR),
		new Worker(TownsfolkEntity.MERCHANT, "merchant", "Торговец Хенрик", 13, Items.APPLE),
		new Worker(TownsfolkEntity.CASINO_OWNER, "casino_owner", "Управляющий Вальтер", 14, Items.GOLD_NUGGET),
		new Worker(TownsfolkEntity.CROUPIER, "croupier", "Крупье Лили", 15, Items.AIR),
		new Worker(TownsfolkEntity.CHRIS, "chris", "Крис", 16, Items.AIR),
	};

	private static void workers(ServerWorld w, List<TownsfolkEntity> folk) {
		for (Worker wk : WORKERS) {
			Locations.Loc l = Locations.get(wk.loc());
			if (!w.isChunkLoaded((int) Math.floor(l.x()) >> 4, (int) Math.floor(l.z()) >> 4)) continue;
			boolean present = false;
			// Chris leaves the casino for good once its secret is out
			boolean gone = wk.role() == TownsfolkEntity.CHRIS && StoryState.get(w.getServer()).hasFlag("casino_done");
			for (TownsfolkEntity f : folk) {
				if (f.getRole() == wk.role()) {
					if (present || gone) f.discard();
					present = true;
				}
			}
			if (present || gone) continue;
			TownsfolkEntity e = ModEntities.TOWNSFOLK.create(w);
			if (e == null) continue;
			e.refreshPositionAndAngles(l.x(), l.y(), l.z(), l.yaw(), 0);
			e.setVariant(wk.variant());
			e.setRole(wk.role());
			e.setHome(l.x(), l.y(), l.z(), l.yaw());
			if (wk.item() != Items.AIR) e.equipStack(EquipmentSlot.MAINHAND, new ItemStack(wk.item()));
			e.setCustomName(Text.literal(wk.name()));
			e.setCustomNameVisible(true);
			e.setPersistent();
			w.spawnEntity(e);
		}
	}

	private static void guards(ServerWorld w, List<TownsfolkEntity> folk) {
		for (int[] g : GATES) {
			boolean present = false;
			for (TownsfolkEntity f : folk) {
				if (f.getRole() == TownsfolkEntity.GUARD && f.squaredDistanceTo(g[0] + 0.5, f.getY(), g[1] + 0.5) < 100) present = true;
			}
			if (present || !w.isChunkLoaded(g[0] >> 4, g[1] >> 4)) continue;
			TownsfolkEntity e = ModEntities.TOWNSFOLK.create(w);
			if (e == null) continue;
			int y = w.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, g[0] + 2, g[1]);
			e.refreshPositionAndAngles(g[0] + 2.5, y, g[1] + 0.5, 0, 0);
			e.setVariant(1);
			e.setRole(TownsfolkEntity.GUARD);
			e.setHome(new BlockPos(g[0] + 2, y, g[1]));
			e.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.LANTERN));
			e.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_SWORD));
			e.setCustomName(Text.literal("Стражник"));
			e.setPersistent();
			w.spawnEntity(e);
		}
	}

	/** Mornings: citizens step out of the doors they went in through. */
	public static void wakeUp(ServerWorld w, int max) {
		State st = State.get(w);
		int done = 0;
		for (int i = 0; i < st.sleeping.size() && done < max; ) {
			Sleeper s = st.sleeping.get(i);
			if (!w.isChunkLoaded(s.door.getX() >> 4, s.door.getZ() >> 4)) {
				i++;
				continue;
			}
			st.sleeping.remove(i);
			st.markDirty();
			BlockPos spot = besideDoor(w, s.door);
			TownsfolkEntity e = ModEntities.TOWNSFOLK.create(w);
			if (e == null || spot == null) continue;
			e.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, w.random.nextFloat() * 360, 0);
			e.setVariant(s.variant);
			e.setHome(s.home);
			e.setPersistent();
			w.spawnEntity(e);
			w.playSound(null, s.door, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.NEUTRAL, 0.6f, 1f);
			done++;
		}
	}

	public static BlockPos besideDoor(ServerWorld w, BlockPos door) {
		for (BlockPos p : new BlockPos[]{door.north(), door.south(), door.east(), door.west(), door.north(2), door.south(2), door.east(2), door.west(2)}) {
			if (w.getBlockState(p.down()).isSolid() && w.getBlockState(p).isAir() && w.getBlockState(p.up()).isAir()) return p;
		}
		return null;
	}

	/** Called by a townsfolk that reached the spot it was walking to. */
	public static void arrive(TownsfolkEntity f) {
		if (!(f.getWorld() instanceof ServerWorld w)) return;
		BlockPos target = f.getScheduleTarget();
		int mode = f.getScheduleMode();
		f.clearSchedule();
		switch (mode) {
			case TownsfolkEntity.GO_HOME -> {
				if (f.getRole() == TownsfolkEntity.CITIZEN && target != null) {
					BlockPos home = f.getOriginalHome();
					BlockPos door = target.equals(GUILD_DOOR) ? nearestDoor(home, 64) : target;
					State st = State.get(w);
					st.sleeping.add(new Sleeper(f.getVariant(), home, door != null ? door : target));
					st.markDirty();
				}
				if (target != null) w.playSound(null, target, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, SoundCategory.NEUTRAL, 0.6f, 1f);
				f.discard();
			}
			case TownsfolkEntity.GO_TAVERN -> {
				f.getOriginalHome();
				double[][] spots = INN_DOOR.equals(target) ? INN_SPOTS : TAVERN_SPOTS;
				double[] spot = spots[w.random.nextInt(spots.length)];
				f.requestTeleport(spot[0], Regions.TOWN_Y + 1, spot[1]);
				f.setHome(spot[0], Regions.TOWN_Y + 1, spot[1], f.getYaw());
			}
			case TownsfolkEntity.GO_WORK -> f.setHome(f.getX(), f.getY(), f.getZ(), f.getYaw());
			default -> {
			}
		}
	}

	// ---------------------------------------------------------------------------------------------

	public static int sleeping(ServerWorld w) {
		return State.get(w).sleeping.size();
	}

	record Sleeper(int variant, BlockPos home, BlockPos door) {
	}

	/** Who is asleep behind which door; saved with the KonoSuba dimension. */
	public static class State extends PersistentState {
		final List<Sleeper> sleeping = new ArrayList<>();

		public static State get(ServerWorld w) {
			return w.getPersistentStateManager().getOrCreate(State::fromNbt, State::new, "konocraft_townlife");
		}

		@Override
		public NbtCompound writeNbt(NbtCompound nbt) {
			NbtList list = new NbtList();
			for (Sleeper s : sleeping) {
				NbtCompound c = new NbtCompound();
				c.putInt("v", s.variant());
				c.putLong("h", s.home().asLong());
				c.putLong("d", s.door().asLong());
				list.add(c);
			}
			nbt.put("sleeping", list);
			return nbt;
		}

		static State fromNbt(NbtCompound nbt) {
			State s = new State();
			for (NbtElement e : nbt.getList("sleeping", NbtElement.COMPOUND_TYPE)) {
				NbtCompound c = (NbtCompound) e;
				s.sleeping.add(new Sleeper(c.getInt("v"), BlockPos.fromLong(c.getLong("h")), BlockPos.fromLong(c.getLong("d"))));
			}
			return s;
		}
	}
}
