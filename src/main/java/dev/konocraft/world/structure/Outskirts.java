package dev.konocraft.world.structure;

import dev.konocraft.world.Build;
import dev.konocraft.world.Regions;
import dev.konocraft.world.Terrain;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/** Everything outside the walls: farms, lake, viewpoint, the battlefield and the road lights. */
public final class Outskirts {
	private Outskirts() {
	}

	public static void build(Build b) {
		battlefield(b);
		farms(b);
		lakeDock(b);
		viewpoint(b);
		toadCamp(b);
		roadLights(b);
	}

	private static int h(int x, int z) {
		return Terrain.heightAt(x, z);
	}

	// ------------------------------------------------------------------------------------------
	/** The southern plain where Axel makes its last stand against the Mobile Fortress Destroyer. */
	private static void battlefield(Build b) {
		int z0 = Regions.DEFENSE_Z;
		// Barricade line
		for (int x = -48; x <= 48; x++) {
			int y = h(x, z0) + 1;
			int seg = Math.floorMod(x, 6);
			if (Math.abs(x) <= 2) continue; // the road stays open (the Destroyer would flatten it anyway)
			if (seg == 0) {
				for (int i = 0; i < 3; i++) b.set(x, y + i, z0, Build.log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
				b.set(x, y + 3, z0, Blocks.SPRUCE_FENCE.getDefaultState());
			} else if (seg == 3) {
				b.set(x, y, z0, Blocks.COBBLESTONE_WALL.getDefaultState());
				b.set(x, y + 1, z0, Blocks.COBBLESTONE_WALL.getDefaultState());
			} else {
				b.set(x, y, z0, Build.log(Blocks.OAK_LOG, Direction.Axis.X));
				b.set(x, y + 1, z0, Blocks.OAK_FENCE.getDefaultState());
			}
			// sharpened stakes in front
			if (Math.floorMod(x, 3) == 0) {
				b.set(x, y, z0 + 2, Blocks.OAK_FENCE.getDefaultState());
				b.set(x, y + 1, z0 + 2, Blocks.POINTED_DRIPSTONE.getDefaultState());
			}
		}
		// Tents behind the line
		tent(b, -30, z0 - 14, Blocks.WHITE_WOOL);
		tent(b, -16, z0 - 16, Blocks.RED_WOOL);
		tent(b, 14, z0 - 15, Blocks.WHITE_WOOL);
		tent(b, 28, z0 - 13, Blocks.BLUE_WOOL);
		// War table
		int ty = h(8, z0 - 8) + 1;
		b.fill(7, ty, z0 - 9, 10, ty, z0 - 8, Blocks.SPRUCE_PLANKS);
		b.set(8, ty + 1, z0 - 9, Blocks.WHITE_CARPET.getDefaultState());
		b.set(9, ty + 1, z0 - 8, Blocks.CANDLE.getDefaultState());
		// Campfires
		for (int[] c : new int[][]{{-22, z0 - 8}, {20, z0 - 6}, {-6, z0 - 20}}) {
			b.set(c[0], h(c[0], c[1]) + 1, c[1], Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		}
		// Decorative catapults
		catapult(b, -38, z0 - 6);
		catapult(b, 38, z0 - 6);
		// The trampled path the Destroyer left behind on earlier rampages
		for (int z = 355; z <= Regions.FIELD_Z2; z++) {
			for (int x = -12; x <= 12; x++) {
				if (Math.abs(x) > 9 && Terrain.rand(x, z, 501) < 0.6) continue;
				int y = h(x, z);
				b.set(x, y, z, Terrain.rand(x, z, 502) < 0.7 ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.DIRT.getDefaultState());
			}
		}
	}

	private static void tent(Build b, int x, int z, Block wool) {
		int y = h(x, z) + 1;
		for (int dz = -2; dz <= 2; dz++) {
			b.set(x - 2, y, z + dz, wool.getDefaultState());
			b.set(x + 2, y, z + dz, wool.getDefaultState());
			b.set(x - 1, y + 1, z + dz, wool.getDefaultState());
			b.set(x + 1, y + 1, z + dz, wool.getDefaultState());
			b.set(x, y + 2, z + dz, wool.getDefaultState());
			b.set(x, y, z + dz, Build.AIR);
			b.set(x - 1, y, z + dz, Build.AIR);
			b.set(x + 1, y, z + dz, Build.AIR);
			b.set(x, y + 1, z + dz, Build.AIR);
		}
		b.set(x, y, z - 2, Blocks.WHITE_CARPET.getDefaultState());
		b.set(x - 1, y, z - 1, Blocks.BARREL.getDefaultState());
	}

	private static void catapult(Build b, int x, int z) {
		int y = h(x, z) + 1;
		b.fill(x - 1, y, z - 2, x + 1, y, z + 2, Blocks.SPRUCE_PLANKS);
		b.set(x - 1, y + 1, z, Blocks.SPRUCE_FENCE.getDefaultState());
		b.set(x + 1, y + 1, z, Blocks.SPRUCE_FENCE.getDefaultState());
		b.set(x, y + 2, z, Build.log(Blocks.SPRUCE_LOG, Direction.Axis.X));
		b.set(x, y + 1, z + 1, Build.log(Blocks.SPRUCE_LOG, Direction.Axis.Z));
		b.set(x, y + 2, z + 2, Build.log(Blocks.SPRUCE_LOG, Direction.Axis.Z));
		b.set(x, y + 3, z + 3, Blocks.CAULDRON.getDefaultState());
		b.set(x, y + 1, z - 3, Blocks.STONE.getDefaultState());
		b.set(x + 1, y + 1, z - 3, Blocks.COBBLESTONE.getDefaultState());
	}

	// ------------------------------------------------------------------------------------------
	/** Cabbage fields west of Axel. The cabbages here fly away every harvest season. */
	private static void farms(Build b) {
		int y = Regions.TOWN_Y;
		for (int px = Regions.FARM_X1 + 4; px <= Regions.FARM_X2 - 14; px += 14) {
			for (int pz = Regions.FARM_Z1 + 4; pz <= Regions.FARM_Z2 - 14; pz += 14) {
				if (px <= -260 && px >= -274 && pz <= -20) continue; // farmhouse plot
				double r = Terrain.rand(px, pz, 601);
				Block crop = r < 0.45 ? Blocks.POTATOES : r < 0.75 ? Blocks.BEETROOTS : Blocks.CARROTS;
				int maxAge = crop == Blocks.BEETROOTS ? 3 : 7;
				for (int x = px; x < px + 11; x++) {
					for (int z = pz; z < pz + 11; z++) {
						boolean edge = x == px || x == px + 10 || z == pz || z == pz + 10;
						if (edge) {
							b.set(x, y, z, Blocks.DIRT_PATH.getDefaultState());
						} else if (z == pz + 5) {
							b.set(x, y, z, Blocks.WATER.getDefaultState());
						} else {
							b.set(x, y, z, Blocks.FARMLAND.getDefaultState().with(Properties.MOISTURE, 7));
							BlockState c = crop == Blocks.BEETROOTS
								? crop.getDefaultState().with(Properties.AGE_3, maxAge)
								: crop.getDefaultState().with(CropBlock.AGE, maxAge);
							b.set(x, y + 1, z, c);
						}
					}
				}
				// scarecrow
				if (r > 0.5) {
					int sx = px + 2, sz = pz + 2;
					b.set(sx, y + 1, sz, Blocks.OAK_FENCE.getDefaultState());
					b.set(sx, y + 2, sz, Blocks.HAY_BLOCK.getDefaultState());
					b.set(sx, y + 3, sz, Blocks.CARVED_PUMPKIN.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
					b.set(sx - 1, y + 2, sz, Blocks.OAK_FENCE.getDefaultState());
					b.set(sx + 1, y + 2, sz, Blocks.OAK_FENCE.getDefaultState());
				}
			}
		}
		// Farmhouse
		int x1 = -274, z1 = -36, x2 = -262, z2 = -26;
		b.fill(x1, y, z1, x2, y, z2, Blocks.COBBLESTONE);
		b.clear(x1 + 1, y + 1, z1 + 1, x2 - 1, y + 9, z2 - 1);
		AxelTown.timberWalls(b, x1, y + 1, z1, x2, y + 4, z2, Blocks.SPRUCE_LOG.getDefaultState(), Blocks.SPRUCE_PLANKS.getDefaultState());
		AxelTown.windows(b, x1, z1, x2, z2, y + 2, 2, Blocks.GLASS_PANE.getDefaultState());
		b.gableRoof(x1, z1, x2, z2, y + 5, Blocks.ACACIA_STAIRS, Build.slab(Blocks.ACACIA_SLAB, false), Blocks.SPRUCE_PLANKS.getDefaultState(), true);
		b.clear(-268, y + 1, z2, -268, y + 2, z2);
		b.door(-268, y + 1, z2, Blocks.SPRUCE_DOOR, Direction.NORTH, false);
		b.set(-272, y + 1, -34, Blocks.CRAFTING_TABLE.getDefaultState());
		b.set(-264, y + 1, -34, Blocks.COMPOSTER.getDefaultState());
		b.set(-270, y + 1, -28, Blocks.HAY_BLOCK.getDefaultState());
		// Windmill
		int wx = -220, wz = 48, wy = h(wx, wz);
		b.cylinder(wx, wy - 2, wy + 12, wz, 3.6, Blocks.STONE_BRICKS.getDefaultState(), true);
		b.cylinder(wx, wy + 1, wy + 11, wz, 2.4, Build.AIR, false);
		b.cone(wx, wy + 13, wz, 4.5, 5, Blocks.SPRUCE_PLANKS.getDefaultState());
		b.clear(wx, wy + 1, wz + 3, wx, wy + 2, wz + 3);
		int hubZ = wz + 4, hubY = wy + 10;
		b.set(wx, hubY, hubZ, Blocks.SPRUCE_LOG.getDefaultState().with(Properties.AXIS, Direction.Axis.Z));
		for (int i = 1; i <= 7; i++) {
			b.set(wx + i, hubY + i, hubZ, Blocks.OAK_FENCE.getDefaultState());
			b.set(wx - i, hubY - i, hubZ, Blocks.OAK_FENCE.getDefaultState());
			b.set(wx + i, hubY - i, hubZ, Blocks.OAK_FENCE.getDefaultState());
			b.set(wx - i, hubY + i, hubZ, Blocks.OAK_FENCE.getDefaultState());
			if (i > 2) {
				b.set(wx + i + 1, hubY + i, hubZ, Blocks.WHITE_WOOL.getDefaultState());
				b.set(wx - i - 1, hubY - i, hubZ, Blocks.WHITE_WOOL.getDefaultState());
				b.set(wx + i, hubY - i - 1, hubZ, Blocks.WHITE_WOOL.getDefaultState());
				b.set(wx - i, hubY + i + 1, hubZ, Blocks.WHITE_WOOL.getDefaultState());
			}
		}
		// Fence around the fields
		for (int x = Regions.FARM_X1; x <= Regions.FARM_X2; x++) {
			if (Math.abs(x + 220) < 3) continue;
			b.set(x, y + 1, Regions.FARM_Z2 + 2, Blocks.OAK_FENCE.getDefaultState());
		}
	}

	// ------------------------------------------------------------------------------------------
	private static void lakeDock(Build b) {
		int x = Regions.LAKE_X, zStart = Regions.LAKE_Z + Regions.LAKE_R - 20;
		int y = Regions.SEA_LEVEL + 1;
		for (int z = zStart; z <= zStart + 22; z++) {
			if (h(x, z) > y + 1) break;
			for (int dx = -1; dx <= 1; dx++) b.set(x + dx, y, z, Blocks.SPRUCE_PLANKS.getDefaultState());
			if (Math.floorMod(z, 4) == 0) {
				b.set(x - 2, y, z, Blocks.SPRUCE_FENCE.getDefaultState());
				b.set(x + 2, y, z, Blocks.SPRUCE_FENCE.getDefaultState());
				for (int yy = y - 1; yy >= Terrain.heightAt(x - 2, z) + 1; yy--) {
					b.set(x - 2, yy, z, Build.log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
					b.set(x + 2, yy, z, Build.log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
				}
			}
		}
		b.set(x - 2, y + 1, zStart, Build.lantern(false, false));
		b.set(x + 2, y + 1, zStart, Build.lantern(false, false));
		b.set(x - 1, y + 1, zStart + 1, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
		b.set(x - 1, y + 1, zStart + 2, Blocks.BARREL.getDefaultState());
	}

	// ------------------------------------------------------------------------------------------
	/** Megumin's favourite spot to blow up the castle from. */
	private static void viewpoint(Build b) {
		int x = Regions.VIEW_X, z = Regions.VIEW_Z, y = Regions.VIEW_Y + 1;
		b.set(x - 3, y, z + 2, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
		b.set(x - 2, y, z + 2, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
		Terrain.oak(x + 4, y, z + 3, 5, Blocks.OAK_LOG.getDefaultState(), Blocks.OAK_LEAVES.getDefaultState(), b::set);
		for (int i = 0; i < 18; i++) {
			int sx = x - 4 + (int) (Terrain.rand(i, 1, 701) * 9), sz = z - 4 + (int) (Terrain.rand(i, 2, 701) * 9);
			b.set(sx, Regions.VIEW_Y, sz, Blocks.COARSE_DIRT.getDefaultState());
		}
		b.set(x, y, z - 2, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, false));
	}

	// ------------------------------------------------------------------------------------------
	/** Hunters' camp on the toad plains. */
	private static void toadCamp(Build b) {
		int x = 205, z = 12;
		tent(b, x, z - 6, Blocks.GREEN_WOOL);
		int y = h(x + 5, z) + 1;
		b.set(x + 5, y, z, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		b.set(x + 4, y, z, Build.stairs(Blocks.OAK_STAIRS, Direction.WEST));
		b.set(x + 6, h(x + 6, z + 1) + 1, z + 1, Blocks.BARREL.getDefaultState());
	}

	// ------------------------------------------------------------------------------------------
	private static void roadLights(Build b) {
		for (int[] r : Terrain.ROADS) {
			double len = Regions.dist(r[0], r[1], r[2], r[3]);
			for (double t = 12; t < len; t += 26) {
				double f = t / len;
				int x = (int) Math.round(r[0] + (r[2] - r[0]) * f);
				int z = (int) Math.round(r[1] + (r[3] - r[1]) * f);
				double nx = -(r[3] - r[1]) / len, nz = (r[2] - r[0]) / len;
				int px = (int) Math.round(x + nx * 3), pz = (int) Math.round(z + nz * 3);
				if (Regions.inTown(px, pz) || Regions.inCastle(px, pz)) continue;
				int y = h(px, pz);
				if (y < Regions.SEA_LEVEL) continue;
				b.set(px, y + 1, pz, Blocks.SPRUCE_FENCE.getDefaultState());
				b.set(px, y + 2, pz, Blocks.SPRUCE_FENCE.getDefaultState());
				b.set(px, y + 3, pz, Build.lantern(false, false));
			}
		}
	}
}
