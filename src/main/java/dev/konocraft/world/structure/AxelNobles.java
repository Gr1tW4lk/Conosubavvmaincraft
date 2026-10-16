package dev.konocraft.world.structure;

import dev.konocraft.world.Build;
import dev.konocraft.world.Terrain;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/** The noble quarter north of the boulevard: Lord Alderp's mansion, the Dustiness estate and the courthouse. */
final class AxelNobles {
	private AxelNobles() {
	}

	static final int G = AxelTown.G, F = AxelTown.F;

	static void build(Build b) {
		alderp(b);
		dustiness(b);
		police(b);
	}

	// ------------------------------------------------------------------------------------------
	static void stairFlight(Build b, int xa, int xb, int y, int z, int dz, int steps, Block stairs) {
		Direction facing = dz < 0 ? Direction.NORTH : Direction.SOUTH;
		for (int i = 0; i < steps; i++) {
			int zz = z + dz * i, yy = y + i;
			for (int x = xa; x <= xb; x++) {
				b.set(x, yy, zz, Build.stairs(stairs, facing));
				for (int h = 1; h <= 3; h++) b.set(x, yy + h, zz, Build.AIR);
			}
		}
	}

	static void bed(Build b, int x, int y, int z, Block bed, Direction headTowards) {
		b.set(x, y, z, bed.getDefaultState().with(BedBlock.FACING, headTowards).with(BedBlock.PART, BedPart.FOOT));
		b.set(x + headTowards.getOffsetX(), y, z + headTowards.getOffsetZ(),
			bed.getDefaultState().with(BedBlock.FACING, headTowards).with(BedBlock.PART, BedPart.HEAD));
	}

	static BlockState banner(Block wallBanner, Direction facing) {
		return wallBanner.getDefaultState().with(WallBannerBlock.FACING, facing);
	}

	static void hedge(Build b, int x1, int z1, int x2, int z2, int height) {
		BlockState leaves = Blocks.AZALEA_LEAVES.getDefaultState().with(Properties.PERSISTENT, true);
		BlockState flowering = Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState().with(Properties.PERSISTENT, true);
		for (int x = x1; x <= x2; x++)
			for (int z = z1; z <= z2; z++)
				for (int y = F; y < F + height; y++)
					b.set(x, y, z, Terrain.rand(x, z * 3L + y, 1401) < 0.2 ? flowering : leaves);
	}

	/** Shell of a grand house: storeys of 5 blocks, trimmed corners, tall windows and a gable roof (ridge along X). */
	static int manor(Build b, int x1, int z1, int x2, int z2, int floors, BlockState wall, BlockState trim, Block roof,
					 BlockState ridge, BlockState pane, Block floor) {
		int top = F + 5 * floors - 1;
		b.fill(x1, G, z1, x2, G, z2, trim);
		for (int x = x1 + 1; x < x2; x++)
			for (int z = z1 + 1; z < z2; z++)
				b.set(x, G, z, Math.floorMod(x + z, 2) == 0 ? floor.getDefaultState() : Blocks.DARK_OAK_PLANKS.getDefaultState());
		b.walls(x1, F, z1, x2, top, z2, wall);
		for (int x = x1; x <= x2; x += 4) {
			b.fill(x, F, z1, x, top, z1, trim);
			b.fill(x, F, z2, x, top, z2, trim);
		}
		b.fill(x2, F, z1, x2, top, z1, trim);
		b.fill(x2, F, z2, x2, top, z2, trim);
		for (int z = z1; z <= z2; z += 4) {
			b.fill(x1, F, z, x1, top, z, trim);
			b.fill(x2, F, z, x2, top, z, trim);
		}
		for (int k = 0; k < floors; k++) {
			int y0 = F + 5 * k;
			b.clear(x1 + 1, y0, z1 + 1, x2 - 1, y0 + 3, z2 - 1);
			b.fill(x1 + 1, y0 + 4, z1 + 1, x2 - 1, y0 + 4, z2 - 1, floor);
			b.walls(x1, y0 + 4, z1, x2, y0 + 4, z2, trim);
			for (int x = x1 + 2; x <= x2 - 2; x += 4) {
				for (int y = y0 + 1; y <= y0 + 3; y++) {
					b.set(x, y, z1, pane);
					b.set(x, y, z2, pane);
				}
			}
			for (int z = z1 + 2; z <= z2 - 2; z += 4) {
				for (int y = y0 + 1; y <= y0 + 3; y++) {
					b.set(x1, y, z, pane);
					b.set(x2, y, z, pane);
				}
			}
		}
		b.gableRoof(x1, z1, x2, z2, top + 1, roof, ridge, wall, true);
		return top;
	}

	static void chandelier(Build b, int x, int y, int z) {
		b.set(x, y, z, Blocks.CHAIN.getDefaultState());
		b.set(x, y - 1, z, Build.lantern(false, true));
	}

	// ------------------------------------------------------------------------------------------
	/** Lord Alderp: greedy, gaudy and the owner of a basement nobody is supposed to know about. */
	private static void alderp(Build b) {
		int x1 = 84, z1 = -118, x2 = 118, z2 = -84;
		BlockState sand = Blocks.SMOOTH_SANDSTONE.getDefaultState();
		BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
		// Wall with golden caps
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				if (x != x1 && x != x2 && z != z1 && z != z2) continue;
				if (z == z2 && x >= 99 && x <= 103) continue;
				boolean post = Math.floorMod(x - x1, 5) == 0 && (z == z1 || z == z2) || Math.floorMod(z - z1, 5) == 0 && (x == x1 || x == x2);
				b.set(x, G, z, Blocks.SANDSTONE.getDefaultState());
				b.fill(x, F, z, x, F + (post ? 3 : 2), z, post ? Blocks.CUT_SANDSTONE.getDefaultState() : Blocks.SANDSTONE_WALL.getDefaultState());
				if (post) b.set(x, F + 4, z, gold);
			}
		}
		for (int s = -1; s <= 1; s += 2) {
			int gx = 101 + 3 * s;
			b.fill(gx, F, z2, gx, F + 5, z2, Blocks.CHISELED_SANDSTONE);
			b.set(gx, F + 6, z2, gold);
			b.set(gx, F + 7, z2, Build.lantern(false, false));
		}
		b.fill(99, F + 5, z2, 103, F + 5, z2, Blocks.CUT_SANDSTONE);
		b.fill(100, F + 6, z2, 102, F + 6, z2, gold);
		// Garden
		for (int x = x1 + 1; x < x2; x++)
			for (int z = z1 + 1; z < z2; z++) {
				boolean path = Math.abs(x - 101) <= 2 && z > -100 || Math.abs(z + 92) <= 1;
				b.set(x, G, z, path ? Blocks.SMOOTH_SANDSTONE.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
				if (!path && z > -99 && Terrain.rand(x, z, 1410) < 0.3) {
					double r = Terrain.rand(z, x, 1411);
					b.set(x, F, z, (r < 0.4 ? Blocks.ORANGE_TULIP : r < 0.7 ? Blocks.DANDELION : r < 0.85 ? Blocks.RED_TULIP : Blocks.TORCHFLOWER).getDefaultState());
				}
			}
		for (int z = -98; z <= -86; z += 4) {
			for (int s = -1; s <= 1; s += 2) {
				int x = 101 + 4 * s;
				b.set(x, F, z, Blocks.SUNFLOWER.getDefaultState().with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
				b.set(x, F + 1, z, Blocks.SUNFLOWER.getDefaultState().with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
			}
		}
		// Fountain with a golden statue of the lord himself (paid for with taxes)
		int fx = 92, fz = -92;
		b.disk(fx, G, fz, 4.5, Blocks.CUT_SANDSTONE.getDefaultState());
		b.ring(fx, F, fz, 4.5, 1.2, Blocks.SANDSTONE_WALL.getDefaultState());
		b.disk(fx, F, fz, 3.4, Blocks.WATER.getDefaultState());
		b.fill(fx, F, fz, fx, F + 1, fz, Blocks.CHISELED_SANDSTONE);
		b.set(fx - 1, F + 2, fz, gold);
		b.set(fx + 1, F + 2, fz, gold);
		b.fill(fx - 1, F + 3, fz, fx + 1, F + 5, fz, gold);
		b.set(fx, F + 4, fz - 1, gold);
		b.set(fx, F + 4, fz + 1, gold);
		b.set(fx, F + 6, fz, Blocks.RAW_GOLD_BLOCK.getDefaultState());
		b.set(fx, F + 7, fz, Blocks.YELLOW_CARPET.getDefaultState());
		b.set(fx + 2, F + 4, fz, Blocks.GOLD_BLOCK.getDefaultState());
		b.set(fx + 2, F + 5, fz, Blocks.LIGHTNING_ROD.getDefaultState());
		int ox = 110;
		b.disk(ox, G, fz, 3, Blocks.WATER.getDefaultState());
		b.disk(ox, F, fz, 3, Build.AIR);
		b.set(ox, G - 1, fz, Blocks.SAND.getDefaultState());
		b.set(ox + 1, F, fz + 1, Blocks.LILY_PAD.getDefaultState());

		// The mansion: three storeys, red roof, golden trim, towers on the front corners
		int mx1 = 88, mz1 = -114, mx2 = 114, mz2 = -100;
		int top = manor(b, mx1, mz1, mx2, mz2, 3, sand, Blocks.CUT_SANDSTONE.getDefaultState(), Blocks.RED_NETHER_BRICK_STAIRS,
			Build.slab(Blocks.RED_NETHER_BRICK_SLAB, false), Blocks.YELLOW_STAINED_GLASS_PANE.getDefaultState(), Blocks.BIRCH_PLANKS);
		for (int k = 0; k < 3; k++) {
			int y = F + 5 * k + 4;
			for (int x = mx1; x <= mx2; x += 2) {
				b.set(x, y, mz2, gold);
				b.set(x, y, mz1, gold);
			}
		}
		for (int s = 0; s < 2; s++) {
			int tx = s == 0 ? mx1 - 2 : mx2 - 2;
			int tz = mz2 - 2;
			b.fill(tx, G, tz, tx + 4, G, tz + 4, Blocks.CUT_SANDSTONE);
			b.walls(tx, F, tz, tx + 4, top + 5, tz + 4, Blocks.CUT_SANDSTONE.getDefaultState());
			b.clear(tx + 1, F, tz + 1, tx + 3, top + 5, tz + 3);
			for (int y = F + 2; y <= top + 3; y += 5) {
				b.set(tx + 2, y, tz + 4, Blocks.YELLOW_STAINED_GLASS_PANE.getDefaultState());
				b.set(tx + 2, y + 1, tz + 4, Blocks.YELLOW_STAINED_GLASS_PANE.getDefaultState());
			}
			b.cone(tx + 2, top + 6, tz + 2, 3.6, 7, Blocks.RED_NETHER_BRICKS.getDefaultState());
			b.set(tx + 2, top + 13, tz + 2, gold);
			b.set(tx + 2, top + 14, tz + 2, Blocks.LIGHTNING_ROD.getDefaultState());
			b.set(tx + 2, top + 4, tz + 2, Build.lantern(false, true));
		}
		// Portico and the main door
		b.clear(100, F, mz2, 102, F + 3, mz2);
		b.door(100, F, mz2, Blocks.DARK_OAK_DOOR, Direction.NORTH, false);
		b.door(102, F, mz2, Blocks.DARK_OAK_DOOR, Direction.NORTH, true);
		b.set(101, F, mz2, Build.AIR);
		b.set(101, F + 1, mz2, Build.AIR);
		for (int x = 97; x <= 105; x++) {
			b.set(x, G, mz2 + 1, Build.stairs(Blocks.SANDSTONE_STAIRS, Direction.NORTH));
			b.set(x, F + 5, mz2 + 1, Build.slab(Blocks.SMOOTH_SANDSTONE_SLAB, false));
		}
		for (int x = 97; x <= 105; x += 8) {
			b.fill(x, F, mz2 + 1, x, F + 4, mz2 + 1, Blocks.QUARTZ_PILLAR);
			b.set(x, F + 2, mz2 + 2, Build.lantern(false, false));
		}
		b.set(99, F + 3, mz2 + 1, banner(Blocks.YELLOW_WALL_BANNER, Direction.SOUTH));
		b.set(103, F + 3, mz2 + 1, banner(Blocks.YELLOW_WALL_BANNER, Direction.SOUTH));
		// Hall: red carpet, gold, a portrait-sized ego
		for (int z = mz1 + 2; z < mz2; z++) for (int x = 100; x <= 102; x++) b.set(x, F, z, Blocks.RED_CARPET.getDefaultState());
		b.fill(99, F, mz1 + 1, 103, F, mz1 + 2, Blocks.GOLD_BLOCK);
		b.set(101, F + 1, mz1 + 2, Build.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH));
		b.set(100, F + 1, mz1 + 2, Blocks.GOLD_BLOCK.getDefaultState());
		b.set(102, F + 1, mz1 + 2, Blocks.GOLD_BLOCK.getDefaultState());
		b.fill(99, F + 2, mz1 + 1, 103, F + 3, mz1 + 1, Blocks.YELLOW_WOOL);
		for (int x = 92; x <= 110; x += 9) chandelier(b, x, F + 3, -107);
		// dining table piled with food
		for (int x = 91; x <= 97; x++) {
			b.set(x, F, -104, Blocks.DARK_OAK_FENCE.getDefaultState());
			b.set(x, F + 1, -104, x % 2 == 0 ? Blocks.CAKE.getDefaultState() : Blocks.WHITE_CARPET.getDefaultState());
			b.set(x, F, -105, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
			b.set(x, F, -103, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH));
		}
		// vault with "tax revenue"
		b.fill(106, F, mz1 + 1, 113, F + 3, mz1 + 1, Blocks.IRON_BLOCK);
		for (int x = 106; x <= 113; x++) {
			b.set(x, F, mz1 + 2, Math.floorMod(x, 2) == 0 ? Blocks.GOLD_BLOCK.getDefaultState() : Blocks.RAW_GOLD_BLOCK.getDefaultState());
			if (Math.floorMod(x, 3) == 0) b.set(x, F + 1, mz1 + 2, Blocks.GOLD_BLOCK.getDefaultState());
		}
		b.fill(105, F, mz1 + 1, 105, F + 3, mz1 + 5, Blocks.IRON_BLOCK);
		b.fill(105, F, mz1 + 5, 113, F + 3, mz1 + 5, Blocks.IRON_BARS);
		b.clear(109, F, mz1 + 5, 109, F + 1, mz1 + 5);
		b.door(109, F, mz1 + 5, Blocks.IRON_DOOR, Direction.NORTH, false);
		// stairs to the upper floors
		stairFlight(b, mx1 + 1, mx1 + 2, F, mz2 - 2, -1, 5, Blocks.DARK_OAK_STAIRS);
		stairFlight(b, mx2 - 2, mx2 - 1, F + 5, mz2 - 2, -1, 5, Blocks.DARK_OAK_STAIRS);
		// bedrooms on the upper floors
		bed(b, 100, F + 5, -108, Blocks.YELLOW_BED, Direction.NORTH);
		bed(b, 101, F + 5, -108, Blocks.YELLOW_BED, Direction.NORTH);
		b.fill(98, F + 5, mz1 + 1, 104, F + 5, mz1 + 1, Blocks.GOLD_BLOCK);
		for (int x = 92; x <= 110; x += 9) chandelier(b, x, F + 8, -107);
		for (int x = 92; x <= 110; x += 9) chandelier(b, x, F + 13, -107);
		for (int x = mx1 + 4; x <= mx2 - 4; x += 3) b.fill(x, F + 10, mz1 + 1, x, F + 12, mz1 + 1, Blocks.BOOKSHELF);
		// The basement nobody is supposed to find: a hatch behind the throne
		int bx = 95, bz = -112;
		b.fill(bx - 6, G - 8, bz - 1, bx + 6, G - 1, bz + 9, Blocks.DEEPSLATE_BRICKS);
		b.clear(bx - 5, G - 7, bz + 1, bx + 5, G - 3, bz + 8);
		b.clear(bx, G - 7, bz, bx, G - 1, bz);
		b.set(bx, G, bz, Build.trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH, false, true));
		for (int y = G - 6; y < G; y++) b.set(bx, y, bz, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH));
		// summoning circle and chains: the "personal hobby" of the lord
		int cx = bx, cz = bz + 5;
		b.ring(cx, G - 8, cz, 3.2, 1.0, Blocks.CRYING_OBSIDIAN.getDefaultState());
		b.set(cx, G - 8, cz, Blocks.CRYING_OBSIDIAN.getDefaultState());
		for (int k = 0; k < 6; k++) {
			double a = k * Math.PI / 3;
			b.set(cx + (int) Math.round(Math.cos(a) * 3), G - 7, cz + (int) Math.round(Math.sin(a) * 3),
				Blocks.RED_CANDLE.getDefaultState().with(CandleBlock.LIT, true).with(CandleBlock.CANDLES, 2));
		}
		for (int x = bx - 4; x <= bx + 4; x += 4) {
			b.fill(x, G - 6, bz + 8, x, G - 3, bz + 8, Blocks.CHAIN);
			b.set(x, G - 5, bz + 7, Build.lantern(true, false));
		}
		b.set(bx + 4, G - 7, bz + 2, Blocks.CHEST.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		AxelTown.loot(b, bx + 4, G - 7, bz + 2, "konocraft:chests/alderp_basement");
		b.set(bx - 4, G - 7, bz + 2, Blocks.LECTERN.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
	}

	// ------------------------------------------------------------------------------------------
	/** The Dustiness estate: Darkness's family home. Elegant, quiet and slightly worried about its daughter. */
	private static void dustiness(Build b) {
		int x1 = 40, z1 = -118, x2 = 76, z2 = -84;
		BlockState stone = Blocks.STONE_BRICKS.getDefaultState();
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
				boolean path = Math.abs(x - 58) <= 1 && z > -100 || Math.abs(z + 92) <= 1 && x > x1 + 4 && x < x2 - 4;
				b.set(x, G, z, edge ? stone : path ? Blocks.GRAVEL.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
				if (!edge) continue;
				if (z == z2 && Math.abs(x - 58) <= 2) continue;
				boolean post = Math.floorMod(x - x1, 6) == 0 && (z == z1 || z == z2) || Math.floorMod(z - z1, 6) == 0 && (x == x1 || x == x2);
				if (post) {
					b.fill(x, F, z, x, F + 2, z, stone);
					b.set(x, F + 3, z, Build.lantern(false, false));
				} else {
					b.set(x, F, z, Blocks.STONE_BRICK_WALL.getDefaultState());
					b.set(x, F + 1, z, Blocks.IRON_BARS.getDefaultState());
				}
			}
		}
		for (int s = -1; s <= 1; s += 2) {
			int gx = 58 + 3 * s;
			b.fill(gx, F, z2, gx, F + 4, z2, Blocks.CHISELED_STONE_BRICKS);
			b.set(gx, F + 5, z2, Build.lantern(false, false));
			b.set(gx, F + 3, z2 + 1, banner(Blocks.BLUE_WALL_BANNER, Direction.SOUTH));
		}
		// hedged garden with roses and a fountain
		hedge(b, x1 + 2, -98, 55, -98, 2);
		hedge(b, 61, -98, x2 - 2, -98, 2);
		hedge(b, x1 + 2, -88, 55, -88, 2);
		hedge(b, 61, -88, x2 - 2, -88, 2);
		for (int x = x1 + 3; x <= x2 - 3; x++) {
			if (Math.abs(x - 58) <= 3) continue;
			for (int z = -96; z <= -90; z += 6) {
				if (Math.floorMod(x, 2) == 0) {
					Block fl = Math.floorMod(x, 4) == 0 ? Blocks.ROSE_BUSH : Blocks.PEONY;
					b.set(x, F, z, fl.getDefaultState().with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
					b.set(x, F + 1, z, fl.getDefaultState().with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
				}
			}
		}
		b.disk(58, G, -92, 3.5, Blocks.STONE_BRICKS.getDefaultState());
		b.ring(58, F, -92, 3.5, 1.2, Blocks.STONE_BRICK_WALL.getDefaultState());
		b.disk(58, F, -92, 2.4, Blocks.WATER.getDefaultState());
		b.fill(58, F, -92, 58, F + 2, -92, Blocks.QUARTZ_PILLAR);
		b.set(58, F + 3, -92, Blocks.WATER.getDefaultState());
		for (int[] t : new int[][]{{44, -94}, {72, -94}, {44, -88}, {72, -88}}) {
			b.fill(t[0], F, t[1], t[0], F + 3, t[1], Blocks.BIRCH_LOG);
			b.sphere(t[0], F + 5, t[1], 2.3, Blocks.BIRCH_LEAVES.getDefaultState().with(Properties.PERSISTENT, true));
			b.set(t[0], F + 4, t[1], Blocks.BIRCH_LOG.getDefaultState());
		}
		for (int s = -1; s <= 1; s += 2) {
			b.set(58 + 5 * s, F, -86, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
			b.set(58 + 5 * s + 1, F, -86, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
		}

		// The house
		int mx1 = 44, mz1 = -114, mx2 = 72, mz2 = -100;
		manor(b, mx1, mz1, mx2, mz2, 3, Blocks.CALCITE.getDefaultState(), Blocks.POLISHED_DEEPSLATE.getDefaultState(),
			Blocks.DEEPSLATE_TILE_STAIRS, Build.slab(Blocks.DEEPSLATE_TILE_SLAB, false), Blocks.GLASS_PANE.getDefaultState(), Blocks.SPRUCE_PLANKS);
		b.clear(57, F, mz2, 59, F + 3, mz2);
		b.door(57, F, mz2, Blocks.SPRUCE_DOOR, Direction.NORTH, false);
		b.door(59, F, mz2, Blocks.SPRUCE_DOOR, Direction.NORTH, true);
		for (int x = 55; x <= 61; x++) b.set(x, G, mz2 + 1, Build.stairs(Blocks.POLISHED_DEEPSLATE_STAIRS, Direction.NORTH));
		for (int x = 55; x <= 61; x += 6) {
			b.fill(x, F, mz2 + 1, x, F + 4, mz2 + 1, Blocks.QUARTZ_PILLAR);
		}
		b.fill(55, F + 5, mz2 + 1, 61, F + 5, mz2 + 1, Blocks.POLISHED_DEEPSLATE_SLAB);
		b.set(58, F + 4, mz2 + 1, banner(Blocks.BLUE_WALL_BANNER, Direction.SOUTH));
		// hall with the family crest and a long carpet
		for (int z = mz1 + 2; z < mz2; z++) b.set(58, F, z, Blocks.BLUE_CARPET.getDefaultState());
		b.fill(56, F + 1, mz1 + 1, 60, F + 3, mz1 + 1, Blocks.BLUE_WOOL);
		b.set(58, F + 2, mz1 + 1, Blocks.GOLD_BLOCK.getDefaultState());
		chandelier(b, 58, F + 3, -107);
		chandelier(b, 50, F + 3, -107);
		chandelier(b, 66, F + 3, -107);
		// dining room (west) and library (east)
		for (int z = -110; z <= -104; z++) {
			b.set(49, F, z, Blocks.DARK_OAK_FENCE.getDefaultState());
			b.set(49, F + 1, z, Blocks.WHITE_CARPET.getDefaultState());
			b.set(48, F, z, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST));
			b.set(50, F, z, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST));
		}
		b.set(49, F + 2, -107, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true).with(CandleBlock.CANDLES, 4));
		for (int x = 64; x <= 71; x++) b.fill(x, F, mz1 + 1, x, F + 3, mz1 + 1, Blocks.BOOKSHELF);
		for (int z = mz1 + 2; z <= mz1 + 9; z++) b.fill(71, F, z, 71, F + 3, z, Blocks.BOOKSHELF);
		b.set(66, F, -108, Blocks.LECTERN.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		b.set(68, F, -106, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
		// fireplace
		b.fill(mx2 - 1, F, -108, mx2 - 1, F + 3, -106, Blocks.BRICKS);
		b.set(mx2 - 1, F, -107, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		b.set(mx2 - 1, F + 1, -107, Build.AIR);
		b.fill(mx2, F + 15, -108, mx2, F + 22, -107, Blocks.BRICKS);
		stairFlight(b, mx1 + 1, mx1 + 2, F, mz2 - 2, -1, 5, Blocks.SPRUCE_STAIRS);
		stairFlight(b, mx2 - 2, mx2 - 1, F + 5, mz2 - 2, -1, 5, Blocks.SPRUCE_STAIRS);
		// Darkness's room: a bed, a suit of armour and an alarming collection of chains
		bed(b, 64, F + 5, -112, Blocks.WHITE_BED, Direction.NORTH);
		for (int x = 60; x <= 63; x++) b.fill(x, F + 6, mz1 + 1, x, F + 8, mz1 + 1, Blocks.CHAIN);
		b.set(66, F + 5, -112, Blocks.SMITHING_TABLE.getDefaultState());
		b.set(67, F + 5, -112, Blocks.ANVIL.getDefaultState());
		b.set(62, F + 5, -110, Blocks.PINK_CARPET.getDefaultState());
		b.set(63, F + 5, -110, Blocks.PINK_CARPET.getDefaultState());
		// the father's study and guest rooms
		bed(b, 50, F + 5, -112, Blocks.BLUE_BED, Direction.NORTH);
		b.set(52, F + 5, -110, Blocks.DARK_OAK_FENCE.getDefaultState());
		b.set(52, F + 6, -110, Blocks.DARK_OAK_PRESSURE_PLATE.getDefaultState());
		for (int x = 48; x <= 68; x += 10) chandelier(b, x, F + 8, -107);
		for (int x = 48; x <= 68; x += 10) chandelier(b, x, F + 13, -107);
		for (int x = 47; x <= 69; x += 4) bed(b, x, F + 10, -112, Blocks.LIGHT_BLUE_BED, Direction.NORTH);
	}

	// ------------------------------------------------------------------------------------------
	/** Police station with cells and the courthouse where Kazuma was once tried for treason. */
	private static void police(Build b) {
		int x1 = -48, z1 = -114, x2 = -10, z2 = -91;
		BlockState stone = Blocks.STONE_BRICKS.getDefaultState();
		BlockState trim = Blocks.POLISHED_ANDESITE.getDefaultState();
		// forecourt with flag poles
		for (int x = -50; x <= -8; x++)
			for (int z = -90; z <= -85; z++) b.set(x, G, z, AxelTown.cobbleStreet(x, z));
		for (int x : new int[]{-44, -24, -14}) {
			b.fill(x, F, -87, x, F + 7, -87, Blocks.IRON_BARS);
			b.set(x, F + 8, -87, Blocks.LIGHTNING_ROD.getDefaultState());
			b.set(x, F + 6, -86, banner(Blocks.BLUE_WALL_BANNER, Direction.SOUTH));
			b.set(x, F + 5, -86, Build.AIR);
		}
		// Police station (west wing)
		int px2 = -30;
		b.fill(x1, G, z1, px2, G, z2, Blocks.POLISHED_ANDESITE);
		b.clear(x1 + 1, F, z1 + 1, px2 - 1, F + 9, z2 - 1);
		b.wallsMix(x1, F, z1, px2, F + 9, z2, stone, Blocks.CRACKED_STONE_BRICKS.getDefaultState(), Blocks.MOSSY_STONE_BRICKS.getDefaultState());
		b.fill(x1 + 1, F + 4, z1 + 1, px2 - 1, F + 4, z2 - 1, Blocks.SPRUCE_PLANKS);
		b.walls(x1, F + 4, z1, px2, F + 4, z2, trim);
		AxelTown.windows(b, x1, z1, px2, z2, F + 1, 2, Blocks.IRON_BARS.getDefaultState());
		AxelTown.windows(b, x1, z1, px2, z2, F + 6, 2, Blocks.GLASS_PANE.getDefaultState());
		b.gableRoof(x1, z1, px2, z2, F + 10, Blocks.DEEPSLATE_TILE_STAIRS, Build.slab(Blocks.DEEPSLATE_TILE_SLAB, false), stone, false);
		b.clear(-40, F, z2, -39, F + 2, z2);
		b.door(-40, F, z2, Blocks.SPRUCE_DOOR, Direction.NORTH, false);
		b.door(-39, F, z2, Blocks.SPRUCE_DOOR, Direction.NORTH, true);
		b.set(-41, F + 2, z2 + 1, Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
		b.set(-38, F + 2, z2 + 1, Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
		b.set(-39, F + 3, z2 + 1, banner(Blocks.BLUE_WALL_BANNER, Direction.SOUTH));
		// front office
		for (int x = -46; x <= -34; x += 4) {
			b.set(x, F, -97, Blocks.SPRUCE_PLANKS.getDefaultState());
			b.set(x + 1, F, -97, Blocks.SPRUCE_PLANKS.getDefaultState());
			b.set(x, F + 1, -97, Blocks.LECTERN.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
			b.set(x, F, -98, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
		}
		b.set(-33, F, -93, Blocks.BARREL.getDefaultState());
		b.set(-32, F, -93, Blocks.BARREL.getDefaultState());
		b.fill(-47, F, -100, -31, F + 3, -100, stone);
		b.clear(-39, F, -100, -39, F + 1, -100);
		b.door(-39, F, -100, Blocks.IRON_DOOR, Direction.NORTH, false);
		b.set(-38, F + 1, -99, Blocks.STONE_BUTTON.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
		b.set(-40, F + 1, -101, Blocks.STONE_BUTTON.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
		// cells
		for (int i = 0; i < 4; i++) {
			int cx1 = -47 + i * 4;
			b.fill(cx1 + 3, F, z1 + 1, cx1 + 3, F + 3, -105, stone);
			b.fill(cx1, F, -105, cx1 + 2, F + 3, -105, Blocks.IRON_BARS);
			b.set(cx1 + 1, F, z1 + 2, Blocks.HAY_BLOCK.getDefaultState());
			b.set(cx1 + 1, F, z1 + 1, Blocks.CAULDRON.getDefaultState());
			b.set(cx1 + 2, F + 3, z1 + 2, Build.lantern(false, true));
		}
		b.fill(-31, F, z1 + 1, -31, F + 3, -105, stone);
		chandelier(b, -42, F + 3, -95);
		chandelier(b, -36, F + 3, -95);
		// Sena's office upstairs
		stairFlight(b, -33, -32, F, -94, -1, 5, Blocks.SPRUCE_STAIRS);
		for (int x = -46; x <= -36; x += 2) b.fill(x, F + 5, z1 + 1, x, F + 7, z1 + 1, Blocks.BOOKSHELF);
		b.set(-41, F + 5, -108, Blocks.DARK_OAK_FENCE.getDefaultState());
		b.set(-40, F + 5, -108, Blocks.DARK_OAK_FENCE.getDefaultState());
		b.set(-41, F + 6, -108, Blocks.LECTERN.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		b.set(-40, F + 6, -108, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true));
		b.set(-40, F + 5, -109, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH));
		chandelier(b, -40, F + 8, -102);

		// Courthouse (east wing): one tall hall with columns
		int cx1 = -28;
		b.fill(cx1, G, z1, x2, G, z2, Blocks.POLISHED_DIORITE);
		b.clear(cx1 + 1, F, z1 + 1, x2 - 1, F + 12, z2 - 1);
		b.walls(cx1, F, z1, x2, F + 11, z2, Blocks.SMOOTH_STONE.getDefaultState());
		for (int x = cx1; x <= x2; x += 3) {
			b.fill(x, F, z2, x, F + 11, z2, Blocks.QUARTZ_PILLAR);
			b.fill(x, F, z1, x, F + 11, z1, Blocks.QUARTZ_PILLAR);
		}
		for (int x = cx1 + 1; x < x2; x++) {
			if (Math.floorMod(x - cx1, 3) == 0) continue;
			for (int y = F + 6; y <= F + 9; y++) {
				b.set(x, y, z2, Blocks.WHITE_STAINED_GLASS_PANE.getDefaultState());
				b.set(x, y, z1, Blocks.WHITE_STAINED_GLASS_PANE.getDefaultState());
			}
		}
		b.gableRoof(cx1, z1, x2, z2, F + 12, Blocks.DEEPSLATE_TILE_STAIRS, Build.slab(Blocks.DEEPSLATE_TILE_SLAB, false),
			Blocks.SMOOTH_STONE.getDefaultState(), false);
		b.clear(-20, F, z2, -18, F + 4, z2);
		b.door(-20, F, z2, Blocks.DARK_OAK_DOOR, Direction.NORTH, false);
		b.door(-18, F, z2, Blocks.DARK_OAK_DOOR, Direction.NORTH, true);
		for (int x = -23; x <= -15; x++) b.set(x, G, z2 + 1, Build.stairs(Blocks.POLISHED_DIORITE_STAIRS, Direction.NORTH));
		for (int x = cx1 + 1; x < x2; x++) for (int z = z1 + 1; z < z2; z++) {
			if (Math.floorMod(x + z, 2) == 0) b.set(x, G, z, Blocks.POLISHED_ANDESITE.getDefaultState());
		}
		// judge's bench, the dock and the audience
		b.fill(-24, F, z1 + 1, -14, F, z1 + 4, Blocks.DARK_OAK_PLANKS);
		b.fill(-24, F + 1, z1 + 4, -14, F + 1, z1 + 4, Blocks.DARK_OAK_PLANKS);
		b.fill(-24, F + 2, z1 + 4, -14, F + 2, z1 + 4, Build.slab(Blocks.DARK_OAK_SLAB, false));
		b.set(-19, F + 1, z1 + 2, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
		b.fill(-21, F + 1, z1 + 1, -17, F + 4, z1 + 1, Blocks.RED_WOOL);
		b.set(-19, F + 3, z1 + 4, Blocks.BELL.getDefaultState());
		for (int x = -22; x <= -16; x += 6) {
			b.fill(x - 1, F, -105, x + 1, F, -105, Blocks.DARK_OAK_FENCE);
			b.set(x, F, -104, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH));
		}
		for (int z = -101; z <= -94; z += 2) {
			for (int x = cx1 + 2; x <= x2 - 2; x++) {
				if (Math.abs(x + 19) <= 1) continue;
				b.set(x, F, z, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
			}
		}
		for (int z = -111; z <= -95; z += 8) {
			chandelier(b, -24, F + 10, z);
			chandelier(b, -14, F + 10, z);
		}
	}
}
