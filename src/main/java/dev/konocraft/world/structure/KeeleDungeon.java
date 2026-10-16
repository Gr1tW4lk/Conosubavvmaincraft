package dev.konocraft.world.structure;

import dev.konocraft.world.Build;
import dev.konocraft.world.Regions;
import dev.konocraft.world.Terrain;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.util.math.Direction;

/**
 * Keele's Dungeon, the "beginner dungeon" in the goblin forest: a ruined arch, a long stair down,
 * a pillared hall, corridors and the hidden chamber where the arch-wizard Keele still waits.
 */
public final class KeeleDungeon {
	public static final int FLOOR = Regions.DUNGEON_Y;
	public static final int EX = Regions.DUNGEON_X, EZ = Regions.DUNGEON_Z;
	/** The stair runs north from the entrance and ends here, where the pillared hall begins. */
	public static final int STAIR_END = EZ - 2 - (Terrain.heightAt(EX, EZ) - FLOOR);
	private static final int HALL_Z1 = STAIR_END - 16;
	private static final int C1_Z1 = HALL_Z1 - 29;
	/** Rooms where the dungeon's undead get up: x1, z1, x2, z2 (hall, north corridor, west corridor). */
	public static final int[][] ROOMS = {
		{EX - 8, HALL_Z1, EX + 8, STAIR_END - 1},
		{EX - 1, C1_Z1, EX + 1, HALL_Z1 - 1},
		{EX - 30, C1_Z1, EX - 2, C1_Z1 + 2},
	};
	public static final int KX2 = EX - 31, KX1 = KX2 - 16, KZ1 = C1_Z1 + 1 - 10, KZ2 = C1_Z1 + 1 + 10;
	/** Where Keele sits. */
	public static final int KEELE_X = KX1 + 2, KEELE_Z = (KZ1 + KZ2) / 2;

	private KeeleDungeon() {
	}

	public static int surface() {
		return Terrain.heightAt(EX, EZ);
	}

	public static void build(Build b) {
		int top = surface();
		entrance(b, top);
		// Stairs down to the north, three wide
		int steps = top - FLOOR;
		for (int i = 0; i <= steps; i++) {
			int y = top - i, z = EZ - 2 - i;
			for (int x = EX - 2; x <= EX + 2; x++) {
				for (int yy = y - 1; yy <= y + 5; yy++) {
					boolean wall = x == EX - 2 || x == EX + 2 || yy == y + 5 || yy == y - 1;
					b.set(x, yy, z, wall ? stone(x, yy, z) : Build.AIR);
				}
			}
			for (int x = EX - 1; x <= EX + 1; x++) b.set(x, y - 1, z, Build.stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH));
			if (i % 6 == 3) b.set(EX - 1, y + 3, z, Build.lantern(true, true));
		}
		// Hall of pillars
		int[] hall = ROOMS[0];
		room(b, hall[0], hall[1], hall[2], hall[3], 6);
		for (int px = hall[0] + 3; px <= hall[2] - 3; px += 5) {
			for (int pz = hall[1] + 3; pz <= hall[3] - 3; pz += 5) {
				b.fill(px, FLOOR, pz, px, FLOOR + 5, pz, Blocks.POLISHED_DEEPSLATE.getDefaultState());
			}
		}
		b.set(hall[0] + 1, FLOOR + 4, hall[1] + 1, Blocks.COBWEB.getDefaultState());
		b.set(hall[2] - 1, FLOOR + 4, hall[3] - 1, Blocks.COBWEB.getDefaultState());
		b.set(EX, FLOOR + 5, (hall[1] + hall[3]) / 2, Build.lantern(true, true));
		// Doorways between the hall and the corridor, and the corridor and the west passage
		b.clear(EX - 1, FLOOR, HALL_Z1 - 1, EX + 1, FLOOR + 3, HALL_Z1);
		b.clear(EX - 2, FLOOR, C1_Z1, EX - 1, FLOOR + 3, C1_Z1 + 2);
		// Corridor north, then west to the chamber
		int[] c1 = ROOMS[1];
		room(b, c1[0], c1[1], c1[2], c1[3], 4);
		int[] c2 = ROOMS[2];
		room(b, c2[0], c2[1], c2[2], c2[3], 4);
		for (int z = c1[1]; z <= c1[3]; z += 7) b.set(c1[0], FLOOR + 3, z, Blocks.SOUL_TORCH.getDefaultState());
		for (int x = c2[0]; x <= c2[2]; x += 7) b.set(x, FLOOR + 3, c2[1], Blocks.SOUL_TORCH.getDefaultState());
		chamber(b);
	}

	private static BlockState stone(int x, int y, int z) {
		double r = Terrain.rand(x * 7L + y, z, 421);
		return r < 0.25 ? Blocks.MOSSY_STONE_BRICKS.getDefaultState() : r < 0.45 ? Blocks.CRACKED_STONE_BRICKS.getDefaultState() : Blocks.STONE_BRICKS.getDefaultState();
	}

	/** A room or corridor: brick shell with air inside. */
	private static void room(Build b, int x1, int z1, int x2, int z2, int height) {
		for (int x = x1 - 1; x <= x2 + 1; x++) {
			for (int z = z1 - 1; z <= z2 + 1; z++) {
				for (int y = FLOOR - 1; y <= FLOOR + height; y++) {
					boolean shell = x < x1 || x > x2 || z < z1 || z > z2 || y == FLOOR - 1 || y == FLOOR + height;
					if (shell) {
						if (!b.has(x, y, z) || !b.get(x, y, z).isAir()) b.set(x, y, z, stone(x, y, z));
					} else {
						b.set(x, y, z, Build.AIR);
					}
				}
			}
		}
	}

	private static void entrance(Build b, int top) {
		b.clearAbove(EX - 4, EZ - 3, EX + 4, EZ + 3, top + 1);
		// Ruined arch over the stairwell
		for (int y = top; y <= top + 5; y++) {
			b.set(EX - 3, y, EZ - 1, stone(EX - 3, y, EZ - 1));
			if (y < top + 4) b.set(EX + 3, y, EZ - 1, stone(EX + 3, y, EZ - 1));
		}
		for (int x = EX - 3; x <= EX + 1; x++) b.set(x, top + 5, EZ - 1, Blocks.MOSSY_STONE_BRICKS.getDefaultState());
		b.set(EX + 2, top + 5, EZ - 1, Build.slab(Blocks.MOSSY_STONE_BRICK_SLAB, false));
		b.set(EX - 2, top + 4, EZ - 1, Blocks.COBWEB.getDefaultState());
		b.set(EX + 3, top + 4, EZ - 1, Blocks.VINE.getDefaultState());
		// Warning sign from the guild, half-buried
		b.set(EX + 4, top + 1, EZ + 1, Blocks.OAK_FENCE.getDefaultState());
		b.set(EX + 4, top + 2, EZ + 1, Blocks.JACK_O_LANTERN.getDefaultState());
		for (int x = EX - 2; x <= EX + 2; x++) b.set(x, top, EZ, Blocks.GRAVEL.getDefaultState());
	}

	private static void chamber(Build b) {
		int h = 8;
		room(b, KX1, KZ1, KX2, KZ2, h);
		int cx = (KX1 + KX2) / 2;
		int cz = KEELE_Z;
		// Opening from the west corridor
		b.clear(KX2, FLOOR, ROOMS[2][1], KX2 + 2, FLOOR + 3, ROOMS[2][3]);
		b.fill(KX1, FLOOR - 1, KZ1, KX2, FLOOR - 1, KZ2, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
		// Carpet to the throne
		for (int x = cx - 1; x <= KX2; x++) b.set(x, FLOOR, cz, Blocks.PURPLE_CARPET.getDefaultState());
		// Throne of the arch-wizard
		b.set(KX1 + 2, FLOOR, cz, Build.stairs(Blocks.POLISHED_BLACKSTONE_STAIRS, Direction.EAST));
		b.set(KX1 + 1, FLOOR, cz, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
		b.fill(KX1 + 1, FLOOR + 1, cz, KX1 + 1, FLOOR + 2, cz, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
		b.set(KX1 + 2, FLOOR, cz - 1, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
		b.set(KX1 + 2, FLOOR, cz + 1, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
		// Bookshelves of forbidden research and soul fire braziers
		for (int z = KZ1; z <= KZ2; z++) {
			if (Math.abs(z - cz) > 2) {
				b.fill(KX1, FLOOR, z, KX1, FLOOR + 2, z, Blocks.BOOKSHELF.getDefaultState());
			}
		}
		for (int[] p : new int[][]{{KX1 + 4, KZ1 + 2}, {KX1 + 4, KZ2 - 2}, {KX2 - 3, KZ1 + 2}, {KX2 - 3, KZ2 - 2}}) {
			b.set(p[0], FLOOR, p[1], Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState());
			b.set(p[0], FLOOR + 1, p[1], Blocks.SOUL_CAMPFIRE.getDefaultState());
		}
		// The princess's resting place, kept with care for three hundred years
		b.fill(KX2 - 6, FLOOR, KZ1 + 1, KX2 - 4, FLOOR, KZ1 + 2, Blocks.QUARTZ_BRICKS.getDefaultState());
		b.set(KX2 - 5, FLOOR + 1, KZ1 + 1, Blocks.POTTED_LILY_OF_THE_VALLEY.getDefaultState());
		b.set(KX2 - 4, FLOOR + 1, KZ1 + 1, Blocks.CANDLE.getDefaultState().with(CandleBlock.CANDLES, 4).with(CandleBlock.LIT, true));
		// Cobwebs and an amethyst cluster growing from forgotten mana
		b.set(KX1 + 1, FLOOR + h - 1, KZ1 + 1, Blocks.COBWEB.getDefaultState());
		b.set(KX2 - 1, FLOOR + h - 1, KZ2 - 1, Blocks.COBWEB.getDefaultState());
		b.set(cx, FLOOR + h - 1, cz, Build.lantern(true, true));
		b.set(KX2 - 1, FLOOR, KZ2 - 1, Blocks.AMETHYST_CLUSTER.getDefaultState());
	}
}
