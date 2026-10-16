package dev.konocraft.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;

/**
 * Deterministic terrain of the KonoSuba world. The map is authored, so every world looks the same:
 * gentle meadows around Axel, a lake to the north, the haunted castle hill to the north-east and the
 * long southern plain the Destroyer walks across.
 */
public final class Terrain {
	private static final long SEED = 0x6B6F6E6F73756261L; // "konosuba"
	private static final SimplexNoiseSampler N1 = new SimplexNoiseSampler(Random.create(SEED));
	private static final SimplexNoiseSampler N2 = new SimplexNoiseSampler(Random.create(SEED + 1));
	private static final SimplexNoiseSampler N3 = new SimplexNoiseSampler(Random.create(SEED + 2));
	private static final SimplexNoiseSampler N4 = new SimplexNoiseSampler(Random.create(SEED + 3));

	/** Road polylines: {x1, z1, x2, z2}. */
	public static final int[][] ROADS = {
		{0, -128, 0, -185}, {0, -185, -30, -238},
		{0, -185, 90, -200}, {90, -200, 160, -205}, {160, -205, 250, -212},
		{128, 0, 200, 10}, {200, 10, 265, 22},
		{-128, 0, -185, 8}, {-185, 8, -255, 10},
		{0, 128, 0, 620},
		{0, -178, -40, -178},
		{-30, -238, -140, -228}, {-140, -228, -260, -226}
	};

	private Terrain() {
	}

	private static double smooth(double t) {
		t = MathHelper.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	/** Weight 1 inside radius r, falling to 0 at r + blend. */
	private static double radial(double x, double z, double cx, double cz, double r, double blend) {
		double d = Regions.dist(x, z, cx, cz);
		return 1 - smooth((d - r) / blend);
	}

	/** Weight 1 inside the box, falling to 0 at distance blend outside it. */
	private static double box(double x, double z, double x1, double z1, double x2, double z2, double blend) {
		double dx = Math.max(Math.max(x1 - x, x - x2), 0);
		double dz = Math.max(Math.max(z1 - z, z - z2), 0);
		double d = Math.sqrt(dx * dx + dz * dz);
		return 1 - smooth(d / blend);
	}

	public static double baseHeight(double x, double z) {
		double h = 66
			+ 5.0 * N1.sample(x / 110.0, z / 110.0)
			+ 2.5 * N2.sample(x / 45.0, z / 45.0)
			+ 0.8 * N3.sample(x / 14.0, z / 14.0);
		double r = Math.sqrt(x * x + z * z);
		if (r > 520) {
			double m = Math.min(1, (r - 520) / 160.0);
			h += m * (18 + 22 * (0.5 + 0.5 * N4.sample(x / 60.0, z / 60.0)));
		}
		if (Regions.inForest((int) x, (int) z)) {
			h += 3 * (0.5 + 0.5 * N4.sample(x / 30.0, z / 30.0));
		}
		return h;
	}

	/** Y of the top solid block of the column. */
	public static int heightAt(int x, int z) {
		double h = baseHeight(x, z);
		// Toad ponds
		for (int[] p : Regions.PONDS) {
			double w = radial(x, z, p[0], p[1], p[2] * 0.6, p[2] * 0.6 + 3);
			if (w > 0) h = MathHelper.lerp(w, h, 58);
		}
		// Lake
		double lake = radial(x, z, Regions.LAKE_X, Regions.LAKE_Z, Regions.LAKE_R - 14, 26);
		if (lake > 0) h = MathHelper.lerp(lake, Math.min(h, 66), 52 + 2 * N3.sample(x / 12.0, z / 12.0));
		// Battlefield: a long gentle plain
		double field = box(x, z, -Regions.FIELD_X, Regions.FIELD_Z1, Regions.FIELD_X, Regions.FIELD_Z2, 26);
		if (field > 0) h = MathHelper.lerp(field, h, 64.6 + 0.8 * N2.sample(x / 30.0, z / 30.0));
		// Cabbage farms
		double farm = box(x, z, Regions.FARM_X1, Regions.FARM_Z1, Regions.FARM_X2, Regions.FARM_Z2, 18);
		if (farm > 0) h = MathHelper.lerp(farm, h, 64.5);
		// Castle plateau
		double castle = radial(x, z, Regions.CASTLE_X, Regions.CASTLE_Z, Regions.CASTLE_R, 36);
		if (castle > 0) h = MathHelper.lerp(castle, h, Regions.CASTLE_Y + 0.5);
		// Megumin's viewpoint hill
		double view = radial(x, z, Regions.VIEW_X, Regions.VIEW_Z, Regions.VIEW_R, 20);
		if (view > 0) h = MathHelper.lerp(view, h, Regions.VIEW_Y + 0.5);
		// Axel
		int t = Regions.TOWN_HALF + 6;
		double town = box(x, z, -t, -t, t, t, 22);
		if (town > 0) h = MathHelper.lerp(town, h, Regions.TOWN_Y + 0.5);
		// Afterlife: keep the ground far below calm
		return MathHelper.floor(h);
	}

	public static double roadDistance(int x, int z) {
		double best = Double.MAX_VALUE;
		for (int[] r : ROADS) {
			best = Math.min(best, segmentDistance(x, z, r[0], r[1], r[2], r[3]));
		}
		return best;
	}

	private static double segmentDistance(double px, double pz, double x1, double z1, double x2, double z2) {
		double dx = x2 - x1, dz = z2 - z1;
		double len = dx * dx + dz * dz;
		double t = len == 0 ? 0 : MathHelper.clamp(((px - x1) * dx + (pz - z1) * dz) / len, 0, 1);
		return Regions.dist(px, pz, x1 + t * dx, z1 + t * dz);
	}

	public static boolean onRoad(int x, int z) {
		if (Regions.inTown(x, z)) return false;
		return roadDistance(x, z) <= 1.6;
	}

	public static long hash(long x, long z, long salt) {
		long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L ^ SEED;
		h ^= h >>> 33;
		h *= 0xff51afd7ed558ccdL;
		h ^= h >>> 33;
		h *= 0xc4ceb9fe1a85ec53L;
		h ^= h >>> 33;
		return h;
	}

	public static double rand(long x, long z, long salt) {
		return (hash(x, z, salt) >>> 11) * 0x1.0p-53;
	}

	// ---------------------------------------------------------------------------------------------
	// Column contents

	public interface BlockSink {
		void set(int x, int y, int z, BlockState state);
	}

	public static BlockState topBlock(int x, int z, int h) {
		Regions.Biome biome = Regions.biomeAt(x, z);
		if (h < Regions.SEA_LEVEL + 1) {
			return h < Regions.SEA_LEVEL - 2 ? Blocks.GRAVEL.getDefaultState() : Blocks.SAND.getDefaultState();
		}
		if (onRoad(x, z)) return Blocks.DIRT_PATH.getDefaultState();
		double r = rand(x, z, 7);
		return switch (biome) {
			case CASTLE -> r < 0.35 ? Blocks.COARSE_DIRT.getDefaultState() : r < 0.5 ? Blocks.PODZOL.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState();
			case BATTLEFIELD -> r < 0.08 ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState();
			case FOREST -> r < 0.15 ? Blocks.PODZOL.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState();
			case LAKE -> h <= Regions.SEA_LEVEL + 1 ? Blocks.SAND.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState();
			default -> Blocks.GRASS_BLOCK.getDefaultState();
		};
	}

	private static BlockState stoneAt(int x, int y, int z) {
		if (y <= -63) return Blocks.BEDROCK.getDefaultState();
		boolean deep = y < 0;
		double r = rand(x * 31L + y, z, 11);
		if (r < 0.012 && y < 60) return deep ? Blocks.DEEPSLATE_COAL_ORE.getDefaultState() : Blocks.COAL_ORE.getDefaultState();
		if (r < 0.018 && y < 50) return deep ? Blocks.DEEPSLATE_IRON_ORE.getDefaultState() : Blocks.IRON_ORE.getDefaultState();
		if (r < 0.022 && y < 40) return deep ? Blocks.DEEPSLATE_COPPER_ORE.getDefaultState() : Blocks.COPPER_ORE.getDefaultState();
		if (r < 0.0235 && y < 20) return deep ? Blocks.DEEPSLATE_GOLD_ORE.getDefaultState() : Blocks.GOLD_ORE.getDefaultState();
		if (r < 0.0242 && y < 0) return Blocks.DEEPSLATE_DIAMOND_ORE.getDefaultState();
		if (r < 0.0250 && y < 10) return deep ? Blocks.DEEPSLATE_REDSTONE_ORE.getDefaultState() : Blocks.REDSTONE_ORE.getDefaultState();
		if (r < 0.0256 && y < 25) return deep ? Blocks.DEEPSLATE_LAPIS_ORE.getDefaultState() : Blocks.LAPIS_ORE.getDefaultState();
		if (deep) return Blocks.DEEPSLATE.getDefaultState();
		if (r > 0.97) return Blocks.ANDESITE.getDefaultState();
		if (r > 0.95) return Blocks.GRANITE.getDefaultState();
		return Blocks.STONE.getDefaultState();
	}

	/** Fills one column of terrain (excluding plants/trees). */
	public static void fillColumn(int x, int z, int minY, BlockSink sink) {
		int h = heightAt(x, z);
		if (Regions.inAfterlife(x, z)) {
			// Below the floating afterlife hall there is only a calm meadow.
			h = Math.min(h, 70);
		}
		BlockState top = topBlock(x, z, h);
		boolean sandy = top.isOf(Blocks.SAND) || top.isOf(Blocks.GRAVEL);
		for (int y = minY; y <= h; y++) {
			BlockState s;
			if (y == h) {
				s = top;
			} else if (y >= h - 3) {
				s = sandy ? Blocks.SAND.getDefaultState() : Blocks.DIRT.getDefaultState();
			} else {
				s = stoneAt(x, y, z);
			}
			sink.set(x, y, z, s);
		}
		for (int y = h + 1; y <= Regions.SEA_LEVEL; y++) {
			sink.set(x, y, z, Blocks.WATER.getDefaultState());
		}
	}

	/** Small plants on top of the column. */
	public static void decorateColumn(int x, int z, BlockSink sink) {
		int h = heightAt(x, z);
		if (h < Regions.SEA_LEVEL + 1) {
			if (Regions.inToadPlains(x, z) && h == Regions.SEA_LEVEL && rand(x, z, 21) < 0.15) {
				sink.set(x, Regions.SEA_LEVEL + 1, z, Blocks.LILY_PAD.getDefaultState());
			}
			return;
		}
		if (onRoad(x, z) || Regions.inTown(x, z) || Regions.inAfterlife(x, z)) return;
		Regions.Biome biome = Regions.biomeAt(x, z);
		if (biome == Regions.Biome.FARMS) return;
		if (biome == Regions.Biome.CASTLE && Regions.dist(x, z, Regions.CASTLE_X, Regions.CASTLE_Z) < Regions.CASTLE_R - 8) return;
		if (biome == Regions.Biome.BATTLEFIELD && Math.abs(x) < 6) return;
		double r = rand(x, z, 13);
		int y = h + 1;
		// Sugar cane next to water
		if (biome == Regions.Biome.TOAD_PLAINS || biome == Regions.Biome.LAKE) {
			if (r < 0.05 && nearWater(x, z)) {
				int len = 1 + (int) (rand(x, z, 14) * 3);
				for (int i = 0; i < len; i++) sink.set(x, y + i, z, Blocks.SUGAR_CANE.getDefaultState());
				return;
			}
		}
		double grass = switch (biome) {
			case TOAD_PLAINS -> 0.38;
			case CASTLE -> 0.10;
			case FOREST -> 0.25;
			case BATTLEFIELD -> 0.18;
			default -> 0.26;
		};
		if (r < grass) {
			double r2 = rand(x, z, 15);
			if (biome == Regions.Biome.CASTLE) {
				sink.set(x, y, z, r2 < 0.5 ? Blocks.DEAD_BUSH.getDefaultState() : Blocks.GRASS.getDefaultState());
			} else if (r2 < 0.12) {
				sink.set(x, y, z, Blocks.TALL_GRASS.getDefaultState().with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
				sink.set(x, y + 1, z, Blocks.TALL_GRASS.getDefaultState().with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
			} else if (r2 < 0.22 && biome != Regions.Biome.BATTLEFIELD) {
				BlockState[] flowers = {Blocks.DANDELION.getDefaultState(), Blocks.POPPY.getDefaultState(), Blocks.CORNFLOWER.getDefaultState(),
					Blocks.OXEYE_DAISY.getDefaultState(), Blocks.AZURE_BLUET.getDefaultState(), Blocks.ALLIUM.getDefaultState()};
				sink.set(x, y, z, flowers[(int) (rand(x, z, 16) * flowers.length)]);
			} else if (biome == Regions.Biome.FOREST && r2 < 0.3) {
				sink.set(x, y, z, Blocks.FERN.getDefaultState());
			} else {
				sink.set(x, y, z, Blocks.GRASS.getDefaultState());
			}
		}
	}

	private static boolean nearWater(int x, int z) {
		for (Direction d : Direction.Type.HORIZONTAL) {
			if (heightAt(x + d.getOffsetX(), z + d.getOffsetZ()) < Regions.SEA_LEVEL) return true;
		}
		return false;
	}

	// ---------------------------------------------------------------------------------------------
	// Trees

	public static final int TREE_CELL = 7;

	private static double treeChance(Regions.Biome biome) {
		return switch (biome) {
			case FOREST -> 0.75;
			case PLAINS -> 0.07;
			case TOAD_PLAINS -> 0.05;
			case LAKE -> 0.10;
			case CASTLE -> 0.22;
			case BATTLEFIELD -> 0.015;
			default -> 0;
		};
	}

	/** Emits every block of trees rooted in cells around [x0, x0+15] x [z0, z0+15]. */
	public static void trees(int x0, int z0, BlockSink sink) {
		int c0x = Math.floorDiv(x0 - 4, TREE_CELL), c1x = Math.floorDiv(x0 + 19, TREE_CELL);
		int c0z = Math.floorDiv(z0 - 4, TREE_CELL), c1z = Math.floorDiv(z0 + 19, TREE_CELL);
		for (int cx = c0x; cx <= c1x; cx++) {
			for (int cz = c0z; cz <= c1z; cz++) {
				int tx = cx * TREE_CELL + 1 + (int) (rand(cx, cz, 31) * (TREE_CELL - 2));
				int tz = cz * TREE_CELL + 1 + (int) (rand(cx, cz, 32) * (TREE_CELL - 2));
				Regions.Biome biome = Regions.biomeAt(tx, tz);
				if (rand(cx, cz, 33) >= treeChance(biome)) continue;
				if (roadDistance(tx, tz) < 4) continue;
				if (biome == Regions.Biome.CASTLE && Regions.dist(tx, tz, Regions.CASTLE_X, Regions.CASTLE_Z) < Regions.CASTLE_R - 2) continue;
				if (biome == Regions.Biome.BATTLEFIELD && Math.abs(tx) < 30) continue;
				if (Regions.dist(tx, tz, Regions.VIEW_X, Regions.VIEW_Z) < Regions.VIEW_R + 3) continue;
				int h = heightAt(tx, tz);
				if (h < Regions.SEA_LEVEL + 1) continue;
				double kind = rand(cx, cz, 34);
				int height = 4 + (int) (rand(cx, cz, 35) * 3);
				switch (biome) {
					case CASTLE -> deadTree(tx, h + 1, tz, height + 1, cx, cz, sink);
					case FOREST -> {
						if (kind < 0.4) spruce(tx, h + 1, tz, height + 3, sink);
						else if (kind < 0.7) oak(tx, h + 1, tz, height + 1, Blocks.DARK_OAK_LOG.getDefaultState(), Blocks.DARK_OAK_LEAVES.getDefaultState(), sink);
						else oak(tx, h + 1, tz, height, Blocks.OAK_LOG.getDefaultState(), Blocks.OAK_LEAVES.getDefaultState(), sink);
					}
					case TOAD_PLAINS -> bush(tx, h + 1, tz, sink);
					default -> {
						if (kind < 0.3) oak(tx, h + 1, tz, height + 1, Blocks.BIRCH_LOG.getDefaultState(), Blocks.BIRCH_LEAVES.getDefaultState(), sink);
						else if (kind < 0.85) oak(tx, h + 1, tz, height, Blocks.OAK_LOG.getDefaultState(), Blocks.OAK_LEAVES.getDefaultState(), sink);
						else bush(tx, h + 1, tz, sink);
					}
				}
			}
		}
	}

	private static BlockState leaves(BlockState s) {
		return s.with(LeavesBlock.PERSISTENT, true);
	}

	public static void oak(int x, int y, int z, int height, BlockState log, BlockState leaf, BlockSink sink) {
		leaf = leaves(leaf);
		for (int i = 0; i < height; i++) sink.set(x, y + i, z, log);
		int top = y + height;
		for (int dy = -3; dy <= 1; dy++) {
			int r = dy >= 0 ? 1 : 2;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.abs(dx) == r && Math.abs(dz) == r && (dy == 1 || rand(x + dx, z + dz, 40 + dy) < 0.5)) continue;
					if (dx == 0 && dz == 0 && dy < 0) continue;
					sink.set(x + dx, top + dy, z + dz, leaf);
				}
			}
		}
	}

	public static void spruce(int x, int y, int z, int height, BlockSink sink) {
		BlockState log = Blocks.SPRUCE_LOG.getDefaultState();
		BlockState leaf = leaves(Blocks.SPRUCE_LEAVES.getDefaultState());
		for (int i = 0; i < height; i++) sink.set(x, y + i, z, log);
		int top = y + height;
		sink.set(x, top, z, leaf);
		sink.set(x, top + 1, z, leaf);
		for (int i = 0; i < height - 2; i++) {
			int yy = top - 1 - i;
			int r = (i % 2 == 0) ? 1 + i / 3 : i / 3;
			if (r <= 0) r = 1;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx == 0 && dz == 0) continue;
					if (Math.abs(dx) + Math.abs(dz) > r + (r > 1 ? 1 : 0)) continue;
					sink.set(x + dx, yy, z + dz, leaf);
				}
			}
		}
	}

	public static void bush(int x, int y, int z, BlockSink sink) {
		BlockState leaf = leaves(Blocks.OAK_LEAVES.getDefaultState());
		sink.set(x, y, z, Blocks.OAK_LOG.getDefaultState());
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int dy = 0; dy <= 1; dy++) {
					if (dx == 0 && dz == 0 && dy == 0) continue;
					if (dy == 1 && Math.abs(dx) + Math.abs(dz) > 1) continue;
					sink.set(x + dx, y + dy, z + dz, leaf);
				}
			}
		}
	}

	public static void deadTree(int x, int y, int z, int height, int cx, int cz, BlockSink sink) {
		BlockState log = Blocks.DARK_OAK_LOG.getDefaultState();
		for (int i = 0; i < height; i++) sink.set(x, y + i, z, log);
		for (Direction d : Direction.Type.HORIZONTAL) {
			if (rand(cx * 4L + d.ordinal(), cz, 50) < 0.5) {
				int by = y + height - 2 - (int) (rand(cx, cz * 3L + d.ordinal(), 51) * 2);
				BlockState branch = log.with(Properties.AXIS, d.getAxis());
				sink.set(x + d.getOffsetX(), by, z + d.getOffsetZ(), branch);
				sink.set(x + d.getOffsetX() * 2, by + 1, z + d.getOffsetZ() * 2, branch);
			}
		}
	}
}
