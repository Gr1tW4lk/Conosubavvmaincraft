package dev.konocraft.world;

/**
 * The hand-designed map of the KonoSuba world. All coordinates are absolute block coordinates
 * in the konocraft:konosuba dimension. Axel sits at the origin.
 */
public final class Regions {
	private Regions() {
	}

	public static final int TOWN_Y = 64;
	public static final int TOWN_HALF = 76;
	public static final int SEA_LEVEL = 62;

	public static final int CASTLE_X = 250, CASTLE_Z = -250, CASTLE_Y = 82, CASTLE_R = 46;
	public static final int VIEW_X = 168, VIEW_Z = -168, VIEW_Y = 74, VIEW_R = 10;
	public static final int LAKE_X = -40, LAKE_Z = -235, LAKE_R = 52;
	public static final int TOAD_X = 210, TOAD_Z = 20;
	public static final int FARM_X1 = -250, FARM_X2 = -130, FARM_Z1 = -40, FARM_Z2 = 60;
	public static final int FIELD_X = 50, FIELD_Z1 = 84, FIELD_Z2 = 560;
	public static final int DEFENSE_Z = 150;
	public static final int DESTROYER_START_Z = 470;
	public static final int DESTROYER_FAIL_Z = 92;
	public static final int AFTERLIFE_X = 3000, AFTERLIFE_Y = 200, AFTERLIFE_Z = 3000;
	public static final int FOREST_X = -180, FOREST_Z = -120;

	/** Toad ponds: x, z, radius. */
	public static final int[][] PONDS = {
		{185, 40, 9}, {240, -15, 7}, {255, 65, 10}, {150, -30, 6}, {300, 20, 8}
	};

	public enum Biome {
		TOWN, PLAINS, TOAD_PLAINS, FARMS, LAKE, FOREST, CASTLE, BATTLEFIELD, AFTERLIFE
	}

	public static boolean inTown(int x, int z) {
		return Math.abs(x) <= TOWN_HALF + 4 && Math.abs(z) <= TOWN_HALF + 4;
	}

	public static double dist(double x1, double z1, double x2, double z2) {
		double dx = x1 - x2, dz = z1 - z2;
		return Math.sqrt(dx * dx + dz * dz);
	}

	public static boolean inCastle(int x, int z) {
		return dist(x, z, CASTLE_X, CASTLE_Z) < CASTLE_R + 30;
	}

	public static boolean inLake(int x, int z) {
		return dist(x, z, LAKE_X, LAKE_Z) < LAKE_R + 20;
	}

	public static boolean inFarms(int x, int z) {
		return x >= FARM_X1 && x <= FARM_X2 && z >= FARM_Z1 && z <= FARM_Z2;
	}

	public static boolean inBattlefield(int x, int z) {
		return Math.abs(x) <= FIELD_X + 20 && z >= FIELD_Z1 && z <= FIELD_Z2;
	}

	public static boolean inToadPlains(int x, int z) {
		return dist(x, z, TOAD_X, TOAD_Z) < 130 && !inTown(x, z) && !inCastle(x, z);
	}

	public static boolean inForest(int x, int z) {
		return x < FOREST_X && z < FOREST_Z && !inLake(x, z);
	}

	public static boolean inAfterlife(int x, int z) {
		return Math.abs(x - AFTERLIFE_X) < 64 && Math.abs(z - AFTERLIFE_Z) < 64;
	}

	public static Biome biomeAt(int x, int z) {
		if (inAfterlife(x, z)) return Biome.AFTERLIFE;
		if (inTown(x, z)) return Biome.TOWN;
		if (inCastle(x, z)) return Biome.CASTLE;
		if (inLake(x, z)) return Biome.LAKE;
		if (inFarms(x, z)) return Biome.FARMS;
		if (inBattlefield(x, z)) return Biome.BATTLEFIELD;
		if (inToadPlains(x, z)) return Biome.TOAD_PLAINS;
		if (inForest(x, z)) return Biome.FOREST;
		return Biome.PLAINS;
	}
}
