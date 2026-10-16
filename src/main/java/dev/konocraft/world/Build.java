package dev.konocraft.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/** Small building DSL writing into the {@link Blueprint}. Coordinates are inclusive. */
public final class Build {
	public static final BlockState AIR = Blocks.AIR.getDefaultState();
	private final Blueprint bp;

	public Build(Blueprint bp) {
		this.bp = bp;
	}

	public void set(int x, int y, int z, BlockState s) {
		bp.set(x, y, z, s);
	}

	public void set(int x, int y, int z, Block b) {
		bp.set(x, y, z, b.getDefaultState());
	}

	public BlockState get(int x, int y, int z) {
		return bp.get(x, y, z);
	}

	public boolean has(int x, int y, int z) {
		return bp.has(x, y, z);
	}

	/** Places only where nothing has been placed yet. */
	public void setIfEmpty(int x, int y, int z, BlockState s) {
		if (!bp.has(x, y, z)) bp.set(x, y, z, s);
	}

	public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
			for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
				for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
					bp.set(x, y, z, s);
	}

	public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
		fill(x1, y1, z1, x2, y2, z2, b.getDefaultState());
	}

	public void clear(int x1, int y1, int z1, int x2, int y2, int z2) {
		fill(x1, y1, z1, x2, y2, z2, AIR);
	}

	/** Four vertical walls of a box (no floor or ceiling). */
	public void walls(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
		fill(x1, y1, z1, x2, y2, z1, s);
		fill(x1, y1, z2, x2, y2, z2, s);
		fill(x1, y1, z1, x1, y2, z2, s);
		fill(x2, y1, z1, x2, y2, z2, s);
	}

	/** Random mix of block states, chosen by position (deterministic). */
	public void fillMix(int x1, int y1, int z1, int x2, int y2, int z2, BlockState... states) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
			for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
				for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
					bp.set(x, y, z, pick(x * 7L + y, z, states));
	}

	public void wallsMix(int x1, int y1, int z1, int x2, int y2, int z2, BlockState... states) {
		fillMix(x1, y1, z1, x2, y2, z1, states);
		fillMix(x1, y1, z2, x2, y2, z2, states);
		fillMix(x1, y1, z1, x1, y2, z2, states);
		fillMix(x2, y1, z1, x2, y2, z2, states);
	}

	public static BlockState pick(long x, long z, BlockState... states) {
		double r = Terrain.rand(x, z, 99);
		// first state is the most common
		if (r < 0.6 || states.length == 1) return states[0];
		return states[1 + (int) ((r - 0.6) / 0.4 * (states.length - 1))];
	}

	public void disk(int cx, int y, int cz, double r, BlockState s) {
		int ir = (int) Math.ceil(r);
		for (int dx = -ir; dx <= ir; dx++)
			for (int dz = -ir; dz <= ir; dz++)
				if (dx * dx + dz * dz <= r * r) bp.set(cx + dx, y, cz + dz, s);
	}

	public void ring(int cx, int y, int cz, double r, double thickness, BlockState s) {
		int ir = (int) Math.ceil(r);
		for (int dx = -ir; dx <= ir; dx++)
			for (int dz = -ir; dz <= ir; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d <= r && d > r - thickness) bp.set(cx + dx, y, cz + dz, s);
			}
	}

	public void cylinder(int cx, int y1, int y2, int cz, double r, BlockState s, boolean hollow) {
		for (int y = y1; y <= y2; y++) {
			if (hollow) ring(cx, y, cz, r, 1.2, s);
			else disk(cx, y, cz, r, s);
		}
	}

	public void sphere(int cx, int cy, int cz, double r, BlockState s) {
		int ir = (int) Math.ceil(r);
		for (int dx = -ir; dx <= ir; dx++)
			for (int dy = -ir; dy <= ir; dy++)
				for (int dz = -ir; dz <= ir; dz++)
					if (dx * dx + dy * dy + dz * dz <= r * r) bp.set(cx + dx, cy + dy, cz + dz, s);
	}

	/** Removes blocks in a sphere, but only blocks that were part of the blueprint (explosion damage). */
	public void blast(int cx, int cy, int cz, double r, BlockState scorch) {
		int ir = (int) Math.ceil(r) + 1;
		for (int dx = -ir; dx <= ir; dx++)
			for (int dy = -ir; dy <= ir; dy++)
				for (int dz = -ir; dz <= ir; dz++) {
					double d2 = dx * dx + dy * dy + dz * dz;
					int x = cx + dx, y = cy + dy, z = cz + dz;
					BlockState cur = bp.get(x, y, z);
					if (cur == null || cur.isAir()) continue;
					double jitter = Terrain.rand(x * 13L + y, z, 77) * 1.5;
					if (d2 <= (r - jitter) * (r - jitter)) bp.set(x, y, z, AIR);
					else if (scorch != null && d2 <= (r + 1) * (r + 1) && Terrain.rand(x, z * 5L + y, 78) < 0.45) bp.set(x, y, z, scorch);
				}
	}

	/** Cone roof made of stacked disks. */
	public void cone(int cx, int y, int cz, double r, int height, BlockState s) {
		for (int i = 0; i < height; i++) {
			double rr = r * (1 - (double) i / height);
			if (rr < 0.5) {
				bp.set(cx, y + i, cz, s);
			} else {
				ring(cx, y + i, cz, rr, 1.5, s);
			}
		}
	}

	public static BlockState stairs(Block b, Direction facing) {
		return b.getDefaultState().with(StairsBlock.FACING, facing);
	}

	public static BlockState stairsTop(Block b, Direction facing) {
		return b.getDefaultState().with(StairsBlock.FACING, facing).with(StairsBlock.HALF, BlockHalf.TOP);
	}

	public static BlockState slab(Block b, boolean top) {
		return b.getDefaultState().with(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}

	public static BlockState log(Block b, Direction.Axis axis) {
		return b.getDefaultState().with(Properties.AXIS, axis);
	}

	public static BlockState lantern(boolean soul, boolean hanging) {
		return (soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN).getDefaultState().with(LanternBlock.HANGING, hanging);
	}

	public static BlockState trapdoor(Block b, Direction facing, boolean open, boolean top) {
		return b.getDefaultState().with(TrapdoorBlock.FACING, facing).with(TrapdoorBlock.OPEN, open)
			.with(TrapdoorBlock.HALF, top ? BlockHalf.TOP : BlockHalf.BOTTOM);
	}

	public void door(int x, int y, int z, Block door, Direction facing, boolean right) {
		BlockState base = door.getDefaultState().with(DoorBlock.FACING, facing).with(DoorBlock.HINGE, right ? DoorHinge.RIGHT : DoorHinge.LEFT);
		bp.set(x, y, z, base.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		bp.set(x, y + 1, z, base.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/**
	 * Gable roof over [x1,x2] x [z1,z2] starting at height y (first roof layer). The ridge runs along
	 * the X axis when alongX is true. Gable ends are filled with {@code gable}.
	 */
	public void gableRoof(int x1, int z1, int x2, int z2, int y, Block stairs, BlockState ridge, BlockState gable, boolean alongX) {
		if (alongX) {
			int lo = z1 - 1, hi = z2 + 1;
			int layer = 0;
			while (lo <= hi) {
				int yy = y + layer;
				for (int x = x1 - 1; x <= x2 + 1; x++) {
					if (lo == hi) {
						bp.set(x, yy, lo, ridge);
					} else {
						bp.set(x, yy, lo, stairs(stairs, Direction.SOUTH));
						bp.set(x, yy, hi, stairs(stairs, Direction.NORTH));
					}
				}
				// gable infill
				if (gable != null && layer > 0) {
					for (int z = lo + 1; z <= hi - 1; z++) {
						bp.set(x1, yy, z, gable);
						bp.set(x2, yy, z, gable);
					}
				}
				if (gable != null && layer == 0) {
					for (int z = lo + 1; z <= hi - 1; z++) {
						bp.set(x1, yy, z, gable);
						bp.set(x2, yy, z, gable);
					}
				}
				// clear the space under the roof interior
				for (int x = x1 + 1; x <= x2 - 1; x++)
					for (int z = lo + 1; z <= hi - 1; z++)
						bp.set(x, yy, z, AIR);
				if (lo + 1 == hi) {
					// two-wide top: finish with slabs
					for (int x = x1 - 1; x <= x2 + 1; x++) {
						bp.set(x, yy + 1, lo, ridge);
						bp.set(x, yy + 1, hi, ridge);
					}
					break;
				}
				lo++;
				hi--;
				layer++;
			}
		} else {
			int lo = x1 - 1, hi = x2 + 1;
			int layer = 0;
			while (lo <= hi) {
				int yy = y + layer;
				for (int z = z1 - 1; z <= z2 + 1; z++) {
					if (lo == hi) {
						bp.set(lo, yy, z, ridge);
					} else {
						bp.set(lo, yy, z, stairs(stairs, Direction.EAST));
						bp.set(hi, yy, z, stairs(stairs, Direction.WEST));
					}
				}
				if (gable != null) {
					for (int x = lo + 1; x <= hi - 1; x++) {
						bp.set(x, yy, z1, gable);
						bp.set(x, yy, z2, gable);
					}
				}
				for (int z = z1 + 1; z <= z2 - 1; z++)
					for (int x = lo + 1; x <= hi - 1; x++)
						bp.set(x, yy, z, AIR);
				if (lo + 1 == hi) {
					for (int z = z1 - 1; z <= z2 + 1; z++) {
						bp.set(lo, yy + 1, z, ridge);
						bp.set(hi, yy + 1, z, ridge);
					}
					break;
				}
				lo++;
				hi--;
				layer++;
			}
		}
	}

	/** Fills from the terrain surface up to y (exclusive) under a footprint, so buildings never float. */
	public void foundation(int x1, int z1, int x2, int z2, int y, BlockState s) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
			for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
				int h = Terrain.heightAt(x, z);
				for (int yy = h - 2; yy < y; yy++) bp.set(x, yy, z, s);
			}
	}

	/** Clears natural terrain above the floor of a footprint (useful on slopes). */
	public void clearAbove(int x1, int z1, int x2, int z2, int y) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
			for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
				int h = Terrain.heightAt(x, z);
				for (int yy = y; yy <= Math.max(h + 2, y); yy++) if (!bp.has(x, yy, z)) bp.set(x, yy, z, AIR);
			}
	}

	/** Simple table: fence post with a pressure plate. */
	public void table(int x, int y, int z, Block fence, Block plate) {
		bp.set(x, y, z, fence.getDefaultState());
		bp.set(x, y + 1, z, plate.getDefaultState());
	}

	public void chair(int x, int y, int z, Block stairs, Direction facing) {
		bp.set(x, y, z, stairs(stairs, facing));
	}
}
