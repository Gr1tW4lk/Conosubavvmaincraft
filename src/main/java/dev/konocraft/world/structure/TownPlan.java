package dev.konocraft.world.structure;

import dev.konocraft.world.Regions;
import dev.konocraft.world.Terrain;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The street plan of Axel: avenues, the boulevard on the line of the old wall, side streets, lanes
 * along the wall, the canal, the landmark plots and the row houses that fill every street front.
 * Everything is deterministic, so the town is the same in every world (and on the map screen).
 */
public final class TownPlan {
	private TownPlan() {
	}

	public static final int H = Regions.TOWN_HALF;
	/** Houses stay inside this square (the lane along the wall is outside it). */
	public static final int LIM = H - 9;

	public static final byte FREE = 0, STREET = 1, LANDMARK = 2, HOUSE = 3, PLAZA = 4, WALL = 5, QUAY = 6;

	/** kind: A avenue, R boulevard, I side street, L lane, C canal quay. */
	public record Rect(int x1, int z1, int x2, int z2, char kind) {
		public boolean contains(int x, int z) {
			return x >= x1 && x <= x2 && z >= z1 && z <= z2;
		}

		public boolean alongX() {
			return x2 - x1 >= z2 - z1;
		}
	}

	public record Landmark(String name, int x1, int z1, int x2, int z2) {
		public boolean contains(int x, int z) {
			return x >= x1 && x <= x2 && z >= z1 && z <= z2;
		}
	}

	/** A house plot. face: the side of the house that looks at the street. */
	public record Lot(int x1, int z1, int x2, int z2, Direction face, char street, int floors, int id) {
		public int width() {
			return face.getAxis() == Direction.Axis.Z ? x2 - x1 + 1 : z2 - z1 + 1;
		}

		public int depth() {
			return face.getAxis() == Direction.Axis.Z ? z2 - z1 + 1 : x2 - x1 + 1;
		}
	}

	public static final Rect[] STREETS = {
		new Rect(-3, -125, 3, 125, 'A'), new Rect(-125, -3, 125, 3, 'A'),
		new Rect(-82, -119, -78, 119, 'R'), new Rect(78, -119, 82, 119, 'R'),
		new Rect(-119, -82, 119, -78, 'R'), new Rect(-119, 78, 119, 82, 'R'),
		new Rect(-52, -77, -48, -4, 'I'), new Rect(-77, -54, -4, -50, 'I'),
		new Rect(38, -38, 42, -4, 'I'), new Rect(38, -38, 77, -34, 'I'),
		new Rect(46, 4, 50, 77, 'I'), new Rect(4, 52, 77, 56, 'I'),
		new Rect(-42, 4, -38, 77, 'I'), new Rect(-77, 22, -4, 26, 'I'),
		new Rect(-122, -122, 122, -120, 'L'), new Rect(-122, 120, 122, 122, 'L'),
		new Rect(-122, -122, -120, 122, 'L'), new Rect(120, -122, 122, 122, 'L'),
		new Rect(-97, -104, -83, -100, 'I'), new Rect(-119, -104, -107, -100, 'I'),
		new Rect(4, -104, 36, -100, 'I'), new Rect(-77, -104, -52, -100, 'I'),
		new Rect(83, -64, 119, -60, 'I'), new Rect(83, 58, 119, 62, 'I'),
		new Rect(-77, 100, -36, 104, 'I'), new Rect(-119, 100, -83, 104, 'I'),
	};

	/** The canal runs from the water gate in the north wall down into the pond of the park. */
	public static final int CANAL_X1 = -104, CANAL_X2 = -100, CANAL_Z1 = -128, CANAL_Z2 = 18;
	public static final Rect CANAL = new Rect(CANAL_X1 - 2, -122, CANAL_X2 + 2, 7, 'C');

	public static final Landmark[] LANDMARKS = {
		new Landmark("guild", -46, -42, -10, -4), new Landmark("wiz", 12, -34, 34, -4),
		new Landmark("eris", 14, 4, 42, 48), new Landmark("axis", -32, 27, -14, 46),
		new Landmark("stable", -68, 44, -43, 66), new Landmark("mansion", 34, -72, 70, -39),
		new Landmark("alderp", 84, -118, 118, -84), new Landmark("dustiness", 40, -118, 76, -84),
		new Landmark("police", -50, -118, -8, -85), new Landmark("warehouses", 86, -46, 118, -5),
		new Landmark("inn", 86, 5, 118, 40), new Landmark("market", -34, 84, 34, 118),
		new Landmark("barracks", 38, 86, 76, 118), new Landmark("bath", 84, 86, 118, 118),
		new Landmark("smithy", -97, -28, -84, -5), new Landmark("park", -119, 8, -84, 76),
		new Landmark("casino", 43, -33, 77, -4),
	};

	private static byte[] grid;
	private static List<Lot> lots;

	private static int idx(int x, int z) {
		return (x + H) * (2 * H + 1) + (z + H);
	}

	public static byte cell(int x, int z) {
		if (Math.abs(x) > H || Math.abs(z) > H) return WALL;
		ensure();
		return grid[idx(x, z)];
	}

	public static boolean isStreet(int x, int z) {
		byte c = cell(x, z);
		return c == STREET || c == PLAZA;
	}

	public static Rect streetAt(int x, int z) {
		for (Rect r : STREETS) if (r.contains(x, z)) return r;
		return CANAL.contains(x, z) ? CANAL : null;
	}

	public static Landmark landmark(String name) {
		for (Landmark l : LANDMARKS) if (l.name.equals(name)) return l;
		throw new IllegalArgumentException(name);
	}

	public static Landmark landmarkAt(int x, int z) {
		for (Landmark l : LANDMARKS) if (l.contains(x, z)) return l;
		return null;
	}

	public static List<Lot> lots() {
		ensure();
		return lots;
	}

	private static synchronized void ensure() {
		if (grid != null) return;
		int n = 2 * H + 1;
		byte[] g = new byte[n * n];
		for (int x = -H; x <= H; x++) {
			for (int z = -H; z <= H; z++) {
				byte c = FREE;
				if (Math.max(Math.abs(x), Math.abs(z)) > H - 3) c = WALL;
				else if (x * x + z * z <= 19 * 19) c = PLAZA;
				g[(x + H) * n + (z + H)] = c;
			}
		}
		for (Landmark l : LANDMARKS) mark(g, l.x1, l.z1, l.x2, l.z2, LANDMARK);
		mark(g, CANAL.x1, CANAL.z1, CANAL.x2, CANAL.z2, QUAY);
		for (Rect r : STREETS) mark(g, r.x1, r.z1, r.x2, r.z2, STREET);
		grid = g;
		List<Lot> out = new ArrayList<>();
		for (Rect r : STREETS) {
			if (r.alongX()) {
				placeSide(out, r, Direction.NORTH);
				placeSide(out, r, Direction.SOUTH);
			} else {
				placeSide(out, r, Direction.WEST);
				placeSide(out, r, Direction.EAST);
			}
		}
		placeSide(out, CANAL, Direction.WEST);
		placeSide(out, CANAL, Direction.EAST);
		lots = Collections.unmodifiableList(out);
	}

	private static void mark(byte[] g, int x1, int z1, int x2, int z2, byte c) {
		for (int x = Math.max(x1, -H); x <= Math.min(x2, H); x++)
			for (int z = Math.max(z1, -H); z <= Math.min(z2, H); z++) {
				int i = idx(x, z);
				if (g[i] != WALL || c == STREET) g[i] = c;
			}
	}

	private static boolean free(int x1, int z1, int x2, int z2) {
		if (x1 < -LIM || x2 > LIM || z1 < -LIM || z2 > LIM) return false;
		for (int x = x1; x <= x2; x++)
			for (int z = z1; z <= z2; z++)
				if (grid[idx(x, z)] != FREE) return false;
		return true;
	}

	/**
	 * Walks along one side of a street and fills it with a terrace of houses, each touching the
	 * street with its front wall. side: where the houses stand relative to the street.
	 */
	private static void placeSide(List<Lot> out, Rect r, Direction side) {
		boolean alongX = side.getAxis() == Direction.Axis.Z;
		int a0 = alongX ? r.x1 : r.z1, a1 = alongX ? r.x2 : r.z2;
		int front = switch (side) {
			case NORTH -> r.z1 - 1;
			case SOUTH -> r.z2 + 1;
			case WEST -> r.x1 - 1;
			default -> r.x2 + 1;
		};
		Direction face = side.getOpposite();
		int p = a0;
		while (p <= a1) {
			int w0 = 5 + (int) (Terrain.rand(p, front, 1101 + side.ordinal()) * 5);
			int d0 = 7 + (int) (Terrain.rand(front, p, 1111 + side.ordinal()) * 5);
			boolean placed = false;
			int ww = 0;
			outer:
			for (ww = w0; ww >= 5; ww--) {
				if (p + ww - 1 > a1) continue;
				for (int d = d0; d >= 6; d--) {
					int hx1, hz1, hx2, hz2;
					switch (side) {
						case NORTH -> { hx1 = p; hx2 = p + ww - 1; hz2 = front; hz1 = front - d + 1; }
						case SOUTH -> { hx1 = p; hx2 = p + ww - 1; hz1 = front; hz2 = front + d - 1; }
						case WEST -> { hz1 = p; hz2 = p + ww - 1; hx2 = front; hx1 = front - d + 1; }
						default -> { hz1 = p; hz2 = p + ww - 1; hx1 = front; hx2 = front + d - 1; }
					}
					if (free(hx1, hz1, hx2, hz2)) {
						mark(grid, hx1, hz1, hx2, hz2, HOUSE);
						int floors = floors(r.kind, hx1, hz1);
						out.add(new Lot(hx1, hz1, hx2, hz2, face, r.kind, floors, out.size()));
						placed = true;
						break outer;
					}
				}
			}
			if (placed) {
				p += ww + (Terrain.rand(p, front, 1121) < 0.22 ? 1 + (int) (Terrain.rand(front, p, 1122) * 2) : 0);
			} else {
				p++;
			}
		}
	}

	private static int floors(char street, int x, int z) {
		double r = Terrain.rand(x, z, 1131);
		double central = 1 - Math.min(1, Math.sqrt(x * x + z * z) / 150.0);
		if (street == 'L') return r < 0.6 ? 1 : 2;
		if (street == 'A' || street == 'R') return r < 0.15 ? 1 : r < 0.55 + 0.35 * central ? 2 : 3;
		return r < 0.3 ? 1 : r < 0.8 + 0.15 * central ? 2 : 3;
	}
}
