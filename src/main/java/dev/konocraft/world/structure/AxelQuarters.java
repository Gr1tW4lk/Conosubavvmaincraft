package dev.konocraft.world.structure;

import dev.konocraft.world.Build;
import dev.konocraft.world.Terrain;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/** The working quarters of Axel: smithy, inn, warehouses, market square, barracks, bathhouse, park and canal. */
final class AxelQuarters {
	private AxelQuarters() {
	}

	static final int G = AxelTown.G, F = AxelTown.F;
	/** Water surface of the canal and the park pond. */
	static final int WATER = G - 1;

	static void build(Build b) {
		smithy(b);
		inn(b);
		warehouses(b);
		market(b);
		barracks(b);
		bath(b);
	}

	private static void table(Build b, int x, int z, Block fence, BlockState top) {
		b.set(x, F, z, fence.getDefaultState());
		b.set(x, F + 1, z, top);
	}

	private static void chairs(Build b, int x, int z, Block stairs) {
		b.set(x - 1, F, z, Build.stairs(stairs, Direction.WEST));
		b.set(x + 1, F, z, Build.stairs(stairs, Direction.EAST));
	}

	private static void pave(Build b, int x1, int z1, int x2, int z2) {
		for (int x = x1; x <= x2; x++)
			for (int z = z1; z <= z2; z++) b.set(x, G, z, AxelTown.cobbleStreet(x, z));
	}

	// ------------------------------------------------------------------------------------------
	/** The smithy by the canal: weapons and armour for adventurers who can afford them. */
	private static void smithy(Build b) {
		int x1 = -96, z1 = -26, x2 = -85, z2 = -12;
		for (int x = -97; x <= -84; x++)
			for (int z = -11; z <= -5; z++) {
				double r = Terrain.rand(x, z, 1501);
				b.set(x, G, z, r < 0.4 ? Blocks.GRAVEL.getDefaultState() : r < 0.75 ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.COBBLESTONE.getDefaultState());
			}
		b.fill(x1, G, z1, x2, G, z2, Blocks.COBBLESTONE);
		b.fill(x1 + 1, G, z1 + 1, x2 - 1, G, z2 - 1, Blocks.STONE);
		b.clear(x1 + 1, F, z1 + 1, x2 - 1, F + 9, z2 - 1);
		b.wallsMix(x1, F, z1, x2, F + 4, z2, Blocks.COBBLESTONE.getDefaultState(), Blocks.STONE_BRICKS.getDefaultState(),
			Blocks.MOSSY_COBBLESTONE.getDefaultState(), Blocks.ANDESITE.getDefaultState());
		AxelTown.timberWalls(b, x1, F + 5, z1, x2, F + 8, z2, Blocks.DARK_OAK_LOG.getDefaultState(), Blocks.MUD_BRICKS.getDefaultState());
		b.fill(x1 + 1, F + 5, z1 + 1, x2 - 1, F + 5, z2 - 1, Blocks.SPRUCE_PLANKS);
		AxelTown.windows(b, x1, z1, x2, z2, F + 1, 3, Blocks.IRON_BARS.getDefaultState());
		AxelTown.windows(b, x1, z1, x2, z2, F + 6, 2, Blocks.GLASS_PANE.getDefaultState());
		b.gableRoof(x1, z1, x2, z2, F + 9, Blocks.DEEPSLATE_TILE_STAIRS, Build.slab(Blocks.DEEPSLATE_TILE_SLAB, false), Blocks.MUD_BRICKS.getDefaultState(), false);
		// wide doors
		b.clear(-91, F, z2, -90, F + 2, z2);
		b.door(-91, F, z2, Blocks.SPRUCE_DOOR, Direction.NORTH, false);
		b.door(-90, F, z2, Blocks.SPRUCE_DOOR, Direction.NORTH, true);
		b.set(-92, F + 2, z2 + 1, Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
		b.set(-89, F + 2, z2 + 1, Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
		b.set(-91, F + 3, z2 + 1, AxelNobles.banner(Blocks.BLACK_WALL_BANNER, Direction.SOUTH));
		b.set(-90, F + 3, z2 + 1, AxelNobles.banner(Blocks.GRAY_WALL_BANNER, Direction.SOUTH));
		// the forge and its chimney
		b.fill(-95, F, -25, -92, F + 1, -25, Blocks.BRICKS);
		b.set(-94, F, -24, Blocks.LAVA_CAULDRON.getDefaultState());
		b.set(-93, F, -24, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		b.fill(-95, F + 2, -25, -92, F + 3, -24, Blocks.BRICKS);
		b.fill(-95, F, -23, -95, F + 1, -23, Blocks.BRICKS);
		b.fill(-92, F, -23, -92, F + 1, -23, Blocks.BRICKS);
		b.fill(-94, F + 4, -25, -93, F + 18, -25, Blocks.BRICKS);
		b.fill(-94, F + 4, -26, -93, F + 18, -26, Blocks.BRICKS);
		b.set(-94, F + 19, -26, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		b.set(-93, F + 19, -25, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		b.set(-91, F, -25, Blocks.BLAST_FURNACE.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		b.set(-90, F, -25, Blocks.FURNACE.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		b.set(-90, F, -21, Blocks.ANVIL.getDefaultState());
		b.set(-88, F, -21, Blocks.CHIPPED_ANVIL.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.EAST));
		b.set(-87, F, -25, Blocks.SMITHING_TABLE.getDefaultState());
		b.set(-86, F, -16, Blocks.GRINDSTONE.getDefaultState().cycle(net.minecraft.block.WallMountedBlock.FACE).cycle(net.minecraft.block.WallMountedBlock.FACE));
		b.set(-95, F, -14, Blocks.RAW_IRON_BLOCK.getDefaultState());
		b.set(-95, F, -15, Blocks.BARREL.getDefaultState());
		b.set(-95, F + 1, -15, Blocks.IRON_BLOCK.getDefaultState());
		b.set(-95, F, -16, Blocks.COAL_BLOCK.getDefaultState());
		for (int z = -22; z <= -17; z++) {
			b.set(x2 - 1, F + 1, z, Math.floorMod(z, 2) == 0 ? Blocks.IRON_BARS.getDefaultState() : Blocks.LIGHTNING_ROD.getDefaultState());
		}
		for (int x = -94; x <= -87; x += 3) {
			b.set(x, F + 4, -19, Blocks.CHAIN.getDefaultState());
			b.set(x, F + 3, -19, Build.lantern(false, true));
		}
		for (int y = F; y <= F + 5; y++) b.set(-86, y, -25, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH));
		// living room above the workshop
		AxelNobles.bed(b, -88, F + 6, -15, Blocks.GRAY_BED, Direction.SOUTH);
		b.set(-94, F + 6, -14, Blocks.BARREL.getDefaultState());
		table(b, -91, -18, Blocks.SPRUCE_FENCE, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true));
		b.set(-91, F, -17, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
		// outdoor work place
		b.set(-94, F, -9, Blocks.ANVIL.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.EAST));
		b.set(-94, F, -7, Blocks.WATER_CAULDRON.getDefaultState().with(Properties.LEVEL_3, 3));
		b.set(-87, F, -9, Blocks.STRIPPED_OAK_LOG.getDefaultState());
		b.set(-87, F, -7, Blocks.HAY_BLOCK.getDefaultState());
		b.set(-86, F, -7, Blocks.BARREL.getDefaultState().with(Properties.FACING, Direction.UP));
		for (int z = -10; z <= -6; z += 4) {
			b.fill(-97, F, z, -97, F + 2, z, Blocks.DARK_OAK_FENCE);
			b.set(-97, F + 3, z, Build.lantern(false, false));
		}
	}

	// ------------------------------------------------------------------------------------------
	/** "The Adventurer's Rest": a three-storey inn by the east gate with a beer garden. */
	private static void inn(Build b) {
		int x1 = 88, z1 = 12, x2 = 116, z2 = 34;
		BlockState frame = Blocks.DARK_OAK_LOG.getDefaultState();
		BlockState plaster = Blocks.WHITE_TERRACOTTA.getDefaultState();
		pave(b, 86, 5, 118, 11);
		b.fill(x1, G, z1, x2, G, z2, Blocks.STONE_BRICKS);
		for (int x = x1 + 1; x < x2; x++)
			for (int z = z1 + 1; z < z2; z++)
				b.set(x, G, z, Math.floorMod(x, 2) == 0 ? Blocks.SPRUCE_PLANKS.getDefaultState() : Blocks.DARK_OAK_PLANKS.getDefaultState());
		b.clear(x1 + 1, F, z1 + 1, x2 - 1, F + 12, z2 - 1);
		b.wallsMix(x1, F, z1, x2, F + 4, z2, Blocks.STONE_BRICKS.getDefaultState(), Blocks.CRACKED_STONE_BRICKS.getDefaultState(), Blocks.MOSSY_STONE_BRICKS.getDefaultState());
		AxelTown.timberWalls(b, x1, F + 5, z1, x2, F + 8, z2, frame, plaster);
		AxelTown.timberWalls(b, x1, F + 9, z1, x2, F + 12, z2, frame, plaster);
		b.fill(x1 + 1, F + 4, z1 + 1, x2 - 1, F + 4, z2 - 1, Blocks.SPRUCE_PLANKS);
		b.fill(x1 + 1, F + 8, z1 + 1, x2 - 1, F + 8, z2 - 1, Blocks.SPRUCE_PLANKS);
		b.fill(x1 + 1, F + 12, z1 + 1, x2 - 1, F + 12, z2 - 1, Blocks.SPRUCE_PLANKS);
		AxelTown.windows(b, x1, z1, x2, z2, F + 1, 2, Blocks.GLASS_PANE.getDefaultState());
		AxelTown.windows(b, x1, z1, x2, z2, F + 6, 2, Blocks.GLASS_PANE.getDefaultState());
		AxelTown.windows(b, x1, z1, x2, z2, F + 10, 2, Blocks.GLASS_PANE.getDefaultState());
		b.gableRoof(x1, z1, x2, z2, F + 13, Blocks.BRICK_STAIRS, Build.slab(Blocks.BRICK_SLAB, false), plaster, true);
		// entrance, sign and lamps
		b.clear(101, F, z1, 102, F + 2, z1);
		b.door(101, F, z1, Blocks.OAK_DOOR, Direction.SOUTH, false);
		b.door(102, F, z1, Blocks.OAK_DOOR, Direction.SOUTH, true);
		b.set(100, F + 2, z1 - 1, Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
		b.set(103, F + 2, z1 - 1, Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
		for (int x = 99; x <= 104; x++) b.set(x, F + 3, z1 - 1, x % 2 == 0 ? Blocks.ORANGE_WOOL.getDefaultState() : Blocks.WHITE_WOOL.getDefaultState());
		b.set(101, F + 4, z1 - 1, AxelNobles.banner(Blocks.ORANGE_WALL_BANNER, Direction.NORTH));
		b.set(102, F + 4, z1 - 1, AxelNobles.banner(Blocks.ORANGE_WALL_BANNER, Direction.NORTH));
		// beer garden
		for (int x = 90; x <= 114; x += 6) {
			if (Math.abs(x - 101) < 3) continue;
			table(b, x, 8, Blocks.SPRUCE_FENCE, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true));
			chairs(b, x, 8, Blocks.SPRUCE_STAIRS);
		}
		for (int x = 87; x <= 117; x += 10) {
			b.fill(x, F, 6, x, F + 2, 6, Blocks.DARK_OAK_FENCE);
			b.set(x, F + 3, 6, Build.lantern(false, false));
		}
		b.set(88, F, 10, Blocks.BARREL.getDefaultState());
		b.set(89, F, 10, Blocks.BARREL.getDefaultState());
		b.set(88, F + 1, 10, Blocks.BARREL.getDefaultState().with(Properties.FACING, Direction.UP));
		// ground floor: reception, dining hall, kitchen, fireplace
		for (int x = x1 + 1; x <= 95; x++) {
			b.set(x, F, 17, Blocks.SPRUCE_PLANKS.getDefaultState());
			b.set(x, F + 1, 17, Build.slab(Blocks.SPRUCE_SLAB, false));
		}
		b.set(92, F + 2, 17, Blocks.BELL.getDefaultState());
		b.set(94, F + 2, 17, Blocks.POTTED_RED_TULIP.getDefaultState());
		for (int x = x1 + 1; x <= 95; x++) b.fill(x, F, z1 + 1, x, F + 2, z1 + 1, Blocks.BOOKSHELF);
		for (int x = 99; x <= 111; x += 4) {
			for (int z = 16; z <= 28; z += 4) {
				table(b, x, z, Blocks.DARK_OAK_FENCE, Blocks.WHITE_CARPET.getDefaultState());
				chairs(b, x, z, Blocks.SPRUCE_STAIRS);
			}
		}
		b.fill(100, F, 33, 100, F + 3, 33, Blocks.BRICKS);
		b.fill(102, F, 33, 102, F + 3, 33, Blocks.BRICKS);
		b.fill(101, F + 2, 33, 101, F + 3, 33, Blocks.BRICKS);
		b.set(101, F, 33, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		b.fill(100, F + 4, 33, 102, F + 17, 34, Blocks.BRICKS);
		b.set(101, F + 18, 34, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		b.set(101, F + 18, 33, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		b.set(x1 + 1, F, 32, Blocks.SMOKER.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.EAST));
		b.set(x1 + 1, F, 31, Blocks.FURNACE.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.EAST));
		b.set(x1 + 1, F, 30, Blocks.WATER_CAULDRON.getDefaultState().with(Properties.LEVEL_3, 3));
		b.set(x1 + 1, F, 29, Blocks.BARREL.getDefaultState());
		b.set(x1 + 2, F, 33, Blocks.BARREL.getDefaultState());
		b.set(x1 + 3, F, 33, Blocks.BARREL.getDefaultState());
		b.set(x1 + 2, F + 1, 33, Blocks.BARREL.getDefaultState().with(Properties.FACING, Direction.UP));
		for (int x = 93; x <= 109; x += 8) {
			AxelNobles.chandelier(b, x, F + 3, 18);
			AxelNobles.chandelier(b, x, F + 3, 27);
		}
		// staircases in the east end
		AxelNobles.stairFlight(b, 114, 115, F, 31, -1, 5, Blocks.SPRUCE_STAIRS);
		AxelNobles.stairFlight(b, 114, 115, F + 5, 18, 1, 4, Blocks.SPRUCE_STAIRS);
		// guest rooms on both upper floors
		for (int k = 0; k < 2; k++) {
			int y = F + 5 + 4 * k;
			b.fill(x1 + 1, y, 21, 112, y + 2, 21, Blocks.SPRUCE_PLANKS);
			b.fill(x1 + 1, y, 25, 112, y + 2, 25, Blocks.SPRUCE_PLANKS);
			for (int px = 94; px <= 112; px += 6) {
				b.fill(px, y, z1 + 1, px, y + 2, 21, Blocks.SPRUCE_PLANKS);
				b.fill(px, y, 25, px, y + 2, z2 - 1, Blocks.SPRUCE_PLANKS);
			}
			for (int rx = 89; rx <= 107; rx += 6) {
				int dx = rx + 2;
				b.door(dx, y, 21, Blocks.SPRUCE_DOOR, Direction.NORTH, false);
				b.door(dx, y, 25, Blocks.SPRUCE_DOOR, Direction.SOUTH, false);
				AxelNobles.bed(b, rx, y, 14, Blocks.RED_BED, Direction.NORTH);
				AxelNobles.bed(b, rx, y, 32, Blocks.RED_BED, Direction.SOUTH);
				b.set(rx + 4, y, z1 + 1, Blocks.BARREL.getDefaultState());
				b.set(rx + 4, y, z2 - 1, Blocks.BARREL.getDefaultState());
				b.set(rx + 4, y + 1, z1 + 1, Blocks.POTTED_OXEYE_DAISY.getDefaultState());
				b.set(rx + 4, y + 1, z2 - 1, Blocks.POTTED_CORNFLOWER.getDefaultState());
				b.set(rx + 2, y + 2, 17, Build.lantern(false, true));
				b.set(rx + 2, y + 2, 29, Build.lantern(false, true));
			}
			for (int x = 92; x <= 110; x += 9) b.set(x, y + 2, 23, Build.lantern(false, true));
			for (int x = x1 + 1; x <= 112; x++) b.set(x, y, 23, Blocks.RED_CARPET.getDefaultState());
		}
	}

	// ------------------------------------------------------------------------------------------
	/** Warehouses and the loading yard behind the east gate. */
	private static void warehouses(Build b) {
		for (int x = 86; x <= 118; x++)
			for (int z = -16; z <= -5; z++) {
				double r = Terrain.rand(x, z, 1520);
				b.set(x, G, z, r < 0.55 ? Blocks.COBBLESTONE.getDefaultState() : r < 0.8 ? Blocks.GRAVEL.getDefaultState() : Blocks.ANDESITE.getDefaultState());
			}
		for (int s = 0; s < 2; s++) {
			int x1 = s == 0 ? 88 : 104, x2 = x1 + 13, z1 = -44, z2 = -18;
			b.fill(x1, G, z1, x2, G, z2, Blocks.COBBLESTONE);
			b.fill(x1 + 1, G, z1 + 1, x2 - 1, G, z2 - 1, Blocks.SPRUCE_PLANKS);
			b.clear(x1 + 1, F, z1 + 1, x2 - 1, F + 8, z2 - 1);
			b.wallsMix(x1, F, z1, x2, F + 1, z2, Blocks.COBBLESTONE.getDefaultState(), Blocks.MOSSY_COBBLESTONE.getDefaultState());
			AxelTown.timberWalls(b, x1, F + 2, z1, x2, F + 7, z2, Blocks.SPRUCE_LOG.getDefaultState(), Blocks.SPRUCE_PLANKS.getDefaultState());
			AxelTown.windows(b, x1, z1, x2, z2, F + 5, 1, Blocks.GLASS_PANE.getDefaultState());
			b.gableRoof(x1, z1, x2, z2, F + 8, Blocks.DARK_OAK_STAIRS, Build.slab(Blocks.DARK_OAK_SLAB, false), Blocks.SPRUCE_PLANKS.getDefaultState(), false);
			int c = (x1 + x2) / 2;
			b.clear(c - 1, F, z2, c + 1, F + 3, z2);
			b.set(c - 2, F + 2, z2 + 1, Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
			b.set(c + 2, F + 2, z2 + 1, Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH));
			// stacks of barrels, hay and crates
			for (int x = x1 + 1; x < x2; x++) {
				if (Math.abs(x - c) <= 1) continue;
				for (int z = z1 + 1; z <= z2 - 3; z++) {
					if (Math.floorMod(z - z1, 4) == 0) continue;
					double r = Terrain.rand(x, z, 1521);
					int h = 1 + (int) (r * 3);
					for (int y = F; y < F + h; y++) {
						b.set(x, y, z, r < 0.5 ? Blocks.BARREL.getDefaultState() : r < 0.75 ? Blocks.HAY_BLOCK.getDefaultState() : Blocks.SPRUCE_PLANKS.getDefaultState());
					}
				}
			}
			// loft with a ladder
			b.fill(x1 + 1, F + 4, z1 + 1, x2 - 1, F + 4, z1 + 8, Blocks.SPRUCE_PLANKS);
			b.clear(x1 + 1, F + 5, z1 + 1, x2 - 1, F + 7, z1 + 8);
			b.fill(c - 1, F, z1 + 9, c + 1, F + 4, z1 + 9, Blocks.SPRUCE_PLANKS);
			for (int y = F; y <= F + 4; y++) b.set(c, y, z1 + 10, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH));
			b.set(x1 + 3, F + 5, z1 + 3, Blocks.BARREL.getDefaultState());
			b.set(x1 + 4, F + 5, z1 + 3, Blocks.HAY_BLOCK.getDefaultState());
			for (int z = z1 + 6; z <= z2 - 4; z += 8) {
				b.set(c, F + 7, z, Blocks.CHAIN.getDefaultState());
				b.set(c, F + 6, z, Build.lantern(false, true));
			}
		}
		// crane and carts in the yard
		b.fill(102, F, -17, 102, F + 8, -17, Blocks.STRIPPED_SPRUCE_LOG);
		for (int x = 98; x <= 101; x++) b.set(x, F + 8, -17, Blocks.SPRUCE_FENCE.getDefaultState());
		for (int y = F + 4; y <= F + 7; y++) b.set(98, y, -17, Blocks.CHAIN.getDefaultState());
		b.set(98, F + 3, -17, Blocks.BARREL.getDefaultState());
		cart(b, 92, -10, Blocks.HAY_BLOCK);
		cart(b, 110, -11, Blocks.BARREL);
		for (int[] p : new int[][]{{88, -14}, {89, -14}, {88, -13}, {116, -15}, {117, -15}, {116, -14}, {100, -7}}) {
			b.set(p[0], F, p[1], Blocks.BARREL.getDefaultState());
			if (Terrain.rand(p[0], p[1], 1522) < 0.5) b.set(p[0], F + 1, p[1], Blocks.BARREL.getDefaultState().with(Properties.FACING, Direction.UP));
		}
	}

	static void cart(Build b, int x, int z, Block cargo) {
		for (int dz = 0; dz <= 2; dz++)
			for (int dx = 0; dx <= 1; dx++) {
				b.set(x + dx, F, z + dz, Build.slab(Blocks.SPRUCE_SLAB, true));
				b.set(x + dx, F + 1, z + dz, cargo.getDefaultState());
			}
		b.set(x - 1, F, z + 1, Build.trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.WEST, true, false));
		b.set(x + 2, F, z + 1, Build.trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.EAST, true, false));
		b.set(x, F, z + 3, Blocks.SPRUCE_FENCE.getDefaultState());
		b.set(x + 1, F, z + 3, Blocks.SPRUCE_FENCE.getDefaultState());
	}

	// ------------------------------------------------------------------------------------------
	/** The market square inside the south gate. */
	private static void market(Build b) {
		int x1 = -34, z1 = 84, x2 = 34, z2 = 118;
		for (int x = x1; x <= x2; x++)
			for (int z = z1; z <= z2; z++) {
				boolean line = Math.floorMod(x, 8) == 0 || Math.floorMod(z, 8) == 0;
				double r = Terrain.rand(x, z, 1530);
				b.set(x, G, z, line ? Blocks.POLISHED_ANDESITE.getDefaultState()
					: r < 0.6 ? Blocks.STONE_BRICKS.getDefaultState() : r < 0.85 ? Blocks.ANDESITE.getDefaultState() : Blocks.MOSSY_STONE_BRICKS.getDefaultState());
			}
		// fountain with a statue of a brave (and broke) adventurer
		int fz = 101;
		b.disk(0, G, fz, 5.5, Blocks.CHISELED_STONE_BRICKS.getDefaultState());
		b.ring(0, F, fz, 5.5, 1.2, Blocks.STONE_BRICK_WALL.getDefaultState());
		b.disk(0, F, fz, 4.4, Blocks.WATER.getDefaultState());
		b.fill(0, F, fz, 0, F + 1, fz, Blocks.STONE_BRICKS);
		b.set(0, F + 2, fz, Blocks.POLISHED_ANDESITE.getDefaultState());
		b.set(0, F + 3, fz, Blocks.IRON_BLOCK.getDefaultState());
		b.set(0, F + 4, fz, Blocks.LIGHT_GRAY_CONCRETE.getDefaultState());
		b.set(1, F + 4, fz, Blocks.LIGHTNING_ROD.getDefaultState());
		b.set(0, F + 5, fz, Blocks.WATER.getDefaultState());
		// stalls in rows
		Block[] wool = {Blocks.RED_WOOL, Blocks.YELLOW_WOOL, Blocks.BLUE_WOOL, Blocks.GREEN_WOOL, Blocks.ORANGE_WOOL, Blocks.PURPLE_WOOL, Blocks.WHITE_WOOL, Blocks.LIME_WOOL};
		Block[][] goods = {{Blocks.MELON, Blocks.PUMPKIN}, {Blocks.HAY_BLOCK, Blocks.BARREL}, {Blocks.BOOKSHELF, Blocks.BARREL},
			{Blocks.PUMPKIN, Blocks.MELON}, {Blocks.CAKE, Blocks.BARREL}, {Blocks.AMETHYST_BLOCK, Blocks.BREWING_STAND},
			{Blocks.WHITE_WOOL, Blocks.LOOM}, {Blocks.MOSS_BLOCK, Blocks.FLOWERING_AZALEA}};
		int i = 0;
		for (int z : new int[]{89, 96, 106, 113}) {
			for (int x : new int[]{-29, -21, -13, 13, 21, 29}) {
				int k = (int) (Terrain.rand(x, z, 1531) * wool.length);
				AxelTown.stall(b, x, z, wool[k], goods[(k + i) % goods.length][0], goods[(k + i) % goods.length][1]);
				i++;
			}
		}
		// benches around the fountain and lamps on the corners
		for (int s = -1; s <= 1; s += 2) {
			for (int dz = -1; dz <= 1; dz++) {
				b.set(8 * s, F, fz + dz, Build.stairs(Blocks.SPRUCE_STAIRS, s > 0 ? Direction.EAST : Direction.WEST));
			}
		}
		for (int[] p : new int[][]{{x1 + 1, z1 + 1}, {x2 - 1, z1 + 1}, {x1 + 1, z2 - 1}, {x2 - 1, z2 - 1}, {-8, 92}, {8, 92}, {-8, 110}, {8, 110}}) {
			b.set(p[0], F, p[1], Blocks.STONE_BRICK_WALL.getDefaultState());
			b.set(p[0], F + 1, p[1], Blocks.STONE_BRICK_WALL.getDefaultState());
			b.set(p[0], F + 2, p[1], Blocks.DARK_OAK_FENCE.getDefaultState());
			b.set(p[0], F + 3, p[1], Build.lantern(false, false));
		}
		cart(b, -33, 100, Blocks.MELON);
		cart(b, 31, 99, Blocks.PUMPKIN);
		for (int[] p : new int[][]{{-17, 88}, {-16, 88}, {17, 116}, {16, 116}, {-25, 117}, {25, 85}}) {
			b.set(p[0], F, p[1], Blocks.BARREL.getDefaultState());
		}
	}

	// ------------------------------------------------------------------------------------------
	/** Barracks of the town guard, with a training yard and a watchtower. */
	private static void barracks(Build b) {
		int x1 = 38, z1 = 86, x2 = 76, z2 = 118;
		for (int x = x1; x <= x2; x++)
			for (int z = z1; z <= 99; z++) {
				double r = Terrain.rand(x, z, 1540);
				b.set(x, G, z, r < 0.5 ? Blocks.SAND.getDefaultState() : r < 0.8 ? Blocks.GRAVEL.getDefaultState() : Blocks.COARSE_DIRT.getDefaultState());
			}
		for (int x = x1; x <= x2; x++) {
			if (Math.abs(x - 57) <= 2) continue;
			b.set(x, F, z1, Blocks.SPRUCE_FENCE.getDefaultState());
		}
		for (int z = z1; z <= 99; z++) {
			b.set(x1, F, z, Blocks.SPRUCE_FENCE.getDefaultState());
			b.set(x2, F, z, Blocks.SPRUCE_FENCE.getDefaultState());
		}
		// training dummies and archery targets
		for (int x = 42; x <= 54; x += 6) {
			b.set(x, F, 92, Blocks.SPRUCE_FENCE.getDefaultState());
			b.set(x, F + 1, 92, Blocks.HAY_BLOCK.getDefaultState());
			b.set(x, F + 2, 92, Blocks.CARVED_PUMPKIN.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
			b.set(x - 1, F + 1, 92, Blocks.SPRUCE_FENCE.getDefaultState());
			b.set(x + 1, F + 1, 92, Blocks.SPRUCE_FENCE.getDefaultState());
		}
		for (int x = 62; x <= 66; x += 2) {
			b.set(x, F, 89, Blocks.SPRUCE_FENCE.getDefaultState());
			b.set(x, F + 1, 89, Blocks.TARGET.getDefaultState());
		}
		b.set(68, F, 96, Blocks.HAY_BLOCK.getDefaultState());
		b.set(69, F, 96, Blocks.HAY_BLOCK.getDefaultState());
		b.set(68, F + 1, 96, Blocks.HAY_BLOCK.getDefaultState());
		// the barracks
		int top = AxelNobles.manor(b, 40, 100, 74, 116, 2, Blocks.STONE_BRICKS.getDefaultState(), Blocks.POLISHED_ANDESITE.getDefaultState(),
			Blocks.DEEPSLATE_TILE_STAIRS, Build.slab(Blocks.DEEPSLATE_TILE_SLAB, false), Blocks.GLASS_PANE.getDefaultState(), Blocks.SPRUCE_PLANKS);
		b.clear(56, F, 100, 58, F + 3, 100);
		b.door(56, F, 100, Blocks.SPRUCE_DOOR, Direction.SOUTH, false);
		b.door(58, F, 100, Blocks.SPRUCE_DOOR, Direction.SOUTH, true);
		b.set(57, F + 4, 99, AxelNobles.banner(Blocks.BLUE_WALL_BANNER, Direction.NORTH));
		b.set(55, F + 2, 99, Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
		b.set(59, F + 2, 99, Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
		// mess hall and armoury on the ground floor
		for (int x = 44; x <= 52; x++) {
			b.set(x, F, 106, Blocks.SPRUCE_FENCE.getDefaultState());
			b.set(x, F + 1, 106, Blocks.WHITE_CARPET.getDefaultState());
			b.set(x, F, 105, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
			b.set(x, F, 107, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
		}
		for (int x = 62; x <= 72; x++) {
			b.set(x, F, 115, Math.floorMod(x, 3) == 0 ? Blocks.ANVIL.getDefaultState() : Blocks.BARREL.getDefaultState());
			b.set(x, F + 1, 115, Math.floorMod(x, 2) == 0 ? Blocks.IRON_BARS.getDefaultState() : Blocks.LIGHTNING_ROD.getDefaultState());
		}
		b.set(73, F, 110, Blocks.GRINDSTONE.getDefaultState().cycle(net.minecraft.block.WallMountedBlock.FACE).cycle(net.minecraft.block.WallMountedBlock.FACE));
		for (int x = 46; x <= 68; x += 11) AxelNobles.chandelier(b, x, F + 3, 108);
		AxelNobles.stairFlight(b, 41, 42, F, 114, -1, 5, Blocks.SPRUCE_STAIRS);
		// dormitory upstairs
		for (int x = 45; x <= 72; x += 3) {
			AxelNobles.bed(b, x, F + 5, 102, Blocks.BLUE_BED, Direction.NORTH);
			AxelNobles.bed(b, x, F + 5, 114, Blocks.BLUE_BED, Direction.SOUTH);
		}
		for (int x = 46; x <= 68; x += 11) AxelNobles.chandelier(b, x, F + 8, 108);
		// watchtower
		int tx = 70, tz = 87;
		b.fill(tx, G, tz, tx + 4, G, tz + 4, Blocks.STONE_BRICKS);
		b.walls(tx, F, tz, tx + 4, F + 13, tz + 4, Blocks.STONE_BRICKS.getDefaultState());
		b.clear(tx + 1, F, tz + 1, tx + 3, F + 13, tz + 3);
		b.clear(tx, F, tz + 2, tx, F + 1, tz + 2);
		b.door(tx, F, tz + 2, Blocks.SPRUCE_DOOR, Direction.EAST, false);
		for (int y = F; y <= F + 13; y++) b.set(tx + 2, y, tz + 3, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH));
		b.fill(tx - 1, F + 13, tz - 1, tx + 5, F + 13, tz + 5, Blocks.STONE_BRICKS);
		b.set(tx + 2, F + 13, tz + 3, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.NORTH));
		b.walls(tx - 1, F + 14, tz - 1, tx + 5, F + 14, tz + 5, Blocks.STONE_BRICK_WALL.getDefaultState());
		for (int[] c : new int[][]{{tx - 1, tz - 1}, {tx + 5, tz - 1}, {tx - 1, tz + 5}, {tx + 5, tz + 5}}) {
			b.fill(c[0], F + 15, c[1], c[0], F + 16, c[1], Blocks.SPRUCE_FENCE);
		}
		b.fill(tx - 1, F + 17, tz - 1, tx + 5, F + 17, tz + 5, Build.slab(Blocks.DEEPSLATE_TILE_SLAB, false));
		b.set(tx + 2, F + 16, tz + 2, Build.lantern(false, true));
		b.set(tx + 2, F + 17, tz + 2, Blocks.DEEPSLATE_TILES.getDefaultState());
	}

	// ------------------------------------------------------------------------------------------
	/** The public bath. Two halls, one wall between them and absolutely no peeking. */
	private static void bath(Build b) {
		int x1 = 86, z1 = 90, x2 = 116, z2 = 116;
		pave(b, 84, 86, 118, 89);
		BlockState frame = Blocks.DARK_OAK_LOG.getDefaultState();
		BlockState plaster = Blocks.CALCITE.getDefaultState();
		b.fill(x1, G, z1, x2, G, z2, Blocks.STONE_BRICKS);
		for (int x = x1 + 1; x < x2; x++)
			for (int z = z1 + 1; z < z2; z++)
				b.set(x, G, z, Math.floorMod(x + z, 2) == 0 ? Blocks.SMOOTH_STONE.getDefaultState() : Blocks.POLISHED_DIORITE.getDefaultState());
		b.clear(x1 + 1, F, z1 + 1, x2 - 1, F + 8, z2 - 1);
		b.wallsMix(x1, F, z1, x2, F + 1, z2, Blocks.STONE_BRICKS.getDefaultState(), Blocks.MOSSY_STONE_BRICKS.getDefaultState());
		AxelTown.timberWalls(b, x1, F + 2, z1, x2, F + 7, z2, frame, plaster);
		AxelTown.windows(b, x1, z1, x2, z2, F + 5, 2, Blocks.GLASS_PANE.getDefaultState());
		b.fill(x1 + 1, F + 8, z1 + 1, x2 - 1, F + 8, z2 - 1, Blocks.DARK_OAK_PLANKS);
		b.gableRoof(x1, z1, x2, 103, F + 8, Blocks.DEEPSLATE_TILE_STAIRS, Build.slab(Blocks.DEEPSLATE_TILE_SLAB, false), plaster, true);
		b.gableRoof(x1, 103, x2, z2, F + 8, Blocks.DEEPSLATE_TILE_STAIRS, Build.slab(Blocks.DEEPSLATE_TILE_SLAB, false), plaster, true);
		// entrance with curtains
		b.clear(100, F, z1, 102, F + 2, z1);
		b.set(100, F + 3, z1 - 1, AxelNobles.banner(Blocks.BLUE_WALL_BANNER, Direction.NORTH));
		b.set(101, F + 3, z1 - 1, AxelNobles.banner(Blocks.WHITE_WALL_BANNER, Direction.NORTH));
		b.set(102, F + 3, z1 - 1, AxelNobles.banner(Blocks.RED_WALL_BANNER, Direction.NORTH));
		for (int x = 98; x <= 104; x++) b.set(x, F + 4, z1 - 1, Build.slab(Blocks.DARK_OAK_SLAB, true));
		b.set(98, F + 2, z1 - 1, Build.lantern(false, true));
		b.set(104, F + 2, z1 - 1, Build.lantern(false, true));
		b.set(98, F + 3, z1 - 1, Blocks.DARK_OAK_FENCE.getDefaultState());
		b.set(104, F + 3, z1 - 1, Blocks.DARK_OAK_FENCE.getDefaultState());
		for (int x : new int[]{88, 92, 110, 114}) {
			b.set(x, F, 87, Blocks.POTTED_BAMBOO.getDefaultState());
			b.set(x + 1, F, 87, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
		}
		// lobby: the counter and a cold drink after the bath
		for (int x = 96; x <= 106; x++) {
			if (Math.abs(x - 101) <= 1) continue;
			b.set(x, F, 94, Blocks.DARK_OAK_PLANKS.getDefaultState());
			b.set(x, F + 1, 94, Build.slab(Blocks.DARK_OAK_SLAB, false));
		}
		b.set(97, F + 2, 94, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true).with(CandleBlock.CANDLES, 3));
		b.set(x1 + 1, F, 92, Blocks.POWDER_SNOW_CAULDRON.getDefaultState().with(Properties.LEVEL_3, 3));
		b.set(x2 - 1, F, 92, Blocks.POWDER_SNOW_CAULDRON.getDefaultState().with(Properties.LEVEL_3, 3));
		for (int x = x1 + 2; x <= x2 - 2; x += 6) b.set(x, F, 96, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
		AxelNobles.chandelier(b, 101, F + 7, 93);
		// halls
		b.fill(x1 + 1, F, 97, x2 - 1, F + 7, 97, Blocks.DARK_OAK_PLANKS);
		b.clear(93, F, 97, 93, F + 1, 97);
		b.door(93, F, 97, Blocks.DARK_OAK_DOOR, Direction.SOUTH, false);
		b.clear(109, F, 97, 109, F + 1, 97);
		b.door(109, F, 97, Blocks.DARK_OAK_DOOR, Direction.SOUTH, false);
		b.set(93, F + 3, 96, AxelNobles.banner(Blocks.BLUE_WALL_BANNER, Direction.NORTH));
		b.set(109, F + 3, 96, AxelNobles.banner(Blocks.RED_WALL_BANNER, Direction.NORTH));
		// the famous dividing wall: tall enough to be proper, low enough to be a plot device
		b.fill(101, F, 98, 101, F + 4, z2 - 1, Blocks.DARK_OAK_PLANKS);
		b.fill(101, F + 5, 98, 101, F + 5, z2 - 1, Blocks.DARK_OAK_FENCE);
		for (int s = 0; s < 2; s++) {
			int px1 = s == 0 ? 88 : 103, px2 = s == 0 ? 99 : 114;
			int pz1 = 101, pz2 = 114;
			b.fill(px1 - 1, G, pz1 - 1, px2 + 1, G, pz2 + 1, Blocks.SMOOTH_QUARTZ);
			for (int x = px1; x <= px2; x++)
				for (int z = pz1; z <= pz2; z++) {
					b.set(x, G, z, Blocks.WATER.getDefaultState());
					b.set(x, G - 1, z, Blocks.WATER.getDefaultState());
					boolean glow = Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0;
					boolean bubbles = !glow && Terrain.rand(x, z, 1550) < 0.18;
					b.set(x, G - 2, z, glow ? Blocks.SEA_LANTERN.getDefaultState() : bubbles ? Blocks.SOUL_SAND.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState());
					if (bubbles) {
						BlockState column = Blocks.BUBBLE_COLUMN.getDefaultState().with(net.minecraft.block.BubbleColumnBlock.DRAG, false);
						b.set(x, G - 1, z, column);
						b.set(x, G, z, column);
					}
					b.set(x, G - 3, z, Blocks.STONE.getDefaultState());
				}
			for (int x = px1 - 2; x <= px2 + 2; x++) {
				b.set(x, G - 1, pz1 - 2, Blocks.STONE.getDefaultState());
				b.set(x, G - 1, pz2 + 2, Blocks.STONE.getDefaultState());
			}
			for (int x = px1; x <= px2; x += 3) {
				b.set(x, F, pz2 + 1, Build.stairs(Blocks.SMOOTH_QUARTZ_STAIRS, Direction.SOUTH));
			}
			for (int z = pz1; z <= pz2; z += 3) {
				b.set(s == 0 ? x1 + 1 : x2 - 1, F, z, Blocks.BARREL.getDefaultState().with(Properties.FACING, Direction.UP));
			}
			int cx = (px1 + px2) / 2;
			AxelNobles.chandelier(b, cx, F + 7, 104);
			AxelNobles.chandelier(b, cx, F + 7, 111);
		}
		// boiler and its chimney
		b.fill(100, F, z2 - 1, 102, F + 3, z2 - 1, Blocks.BRICKS);
		b.fill(100, F, z2, 102, F + 20, z2, Blocks.BRICKS);
		b.set(101, F + 21, z2, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		b.set(100, F + 21, z2, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
	}

	// ------------------------------------------------------------------------------------------
	/** Carves the canal (water one block below the street) from the north water gate down into the park pond. */
	static void canal(Build b) {
		int x1 = TownPlan.CANAL_X1, x2 = TownPlan.CANAL_X2;
		for (int z = TownPlan.CANAL_Z1; z <= TownPlan.CANAL_Z2; z++) {
			for (int x = x1 - 1; x <= x2 + 1; x++) {
				if (pond(x, z) <= 1.25) continue;
				boolean wall = x == x1 - 1 || x == x2 + 1;
				b.set(x, WATER - 4, z, Blocks.STONE_BRICKS.getDefaultState());
				for (int y = WATER - 3; y <= WATER; y++) {
					b.set(x, y, z, wall ? Build.pick(x * 3L + y, z, Blocks.STONE_BRICKS.getDefaultState(), Blocks.MOSSY_STONE_BRICKS.getDefaultState(),
						Blocks.CRACKED_STONE_BRICKS.getDefaultState()) : Blocks.WATER.getDefaultState());
				}
				if (!wall && z > -126) b.set(x, G, z, Build.AIR);
			}
			// ladders out of the water every so often
			if (Math.floorMod(z, 24) == 0 && z > -118) {
				for (int y = WATER - 2; y <= WATER; y++) {
					b.set(x1, y, z, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.EAST).with(Properties.WATERLOGGED, true));
				}
			}
		}
		// water gate under the north wall
		for (int x = x1; x <= x2; x++) {
			for (int y = WATER - 3; y <= WATER; y++) {
				b.set(x, y, -127, Blocks.IRON_BARS.getDefaultState().with(Properties.WATERLOGGED, true));
			}
		}
		// railings where streets bridge the canal
		for (TownPlan.Rect r : TownPlan.STREETS) {
			if (r.x1() > x1 || r.x2() < x2 || r.z1() > TownPlan.CANAL_Z2 || r.z2() < TownPlan.CANAL_Z1) continue;
			for (int z : new int[]{r.z1() - 1, r.z2() + 1}) {
				if (TownPlan.cell(x1, z) != TownPlan.QUAY) continue;
				for (int x = x1; x <= x2; x++) {
					b.set(x, G, z, Blocks.STONE_BRICKS.getDefaultState());
					b.set(x, F, z, Blocks.STONE_BRICK_WALL.getDefaultState());
				}
			}
		}
		// quays
		for (int z = TownPlan.CANAL.z1(); z <= TownPlan.CANAL.z2(); z++) {
			for (int x : new int[]{x1 - 2, x1 - 1, x2 + 1, x2 + 2}) {
				if (TownPlan.cell(x, z) == TownPlan.QUAY) b.set(x, G, z, AxelTown.cobbleStreet(x, z));
			}
			if (Math.floorMod(z, 16) == 8 && TownPlan.cell(x1 - 1, z) == TownPlan.QUAY) {
				for (int x : new int[]{x1 - 1, x2 + 1}) {
					b.set(x, F, z, Blocks.STONE_BRICK_WALL.getDefaultState());
					b.set(x, F + 1, z, Blocks.DARK_OAK_FENCE.getDefaultState());
					b.set(x, F + 2, z, Blocks.DARK_OAK_FENCE.getDefaultState());
					b.set(x, F + 3, z, Build.lantern(false, false));
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	static final int PCX = -102, PCZ = 27;
	static final double PRX = 11, PRZ = 10;

	/** Elliptic distance from the centre of the park pond (1 = water's edge). */
	static double pond(int x, int z) {
		return Math.pow((x - PCX) / PRX, 2) + Math.pow((z - PCZ) / PRZ, 2);
	}

	/** The town park: a pond fed by the canal, cherry trees, a gazebo and quiet benches. */
	static void park(Build b) {
		int x1 = -119, z1 = 8, x2 = -84, z2 = 76;
		int pcx = PCX, pcz = PCZ;
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				double e = pond(x, z);
				if (e > 1.25 && x >= TownPlan.CANAL_X1 - 1 && x <= TownPlan.CANAL_X2 + 1 && z <= TownPlan.CANAL_Z2) continue;
				double ring = Math.pow((x - pcx) / (PRX + 3), 2) + Math.pow((z - pcz) / (PRZ + 3), 2);
				boolean path = Math.abs(x + 90) <= 1 || Math.abs(z - 56) <= 1 || ring <= 1 && ring > 0.78;
				if (e <= 1) {
					double depth = 1 - e;
					b.set(x, G, z, Build.AIR);
					b.set(x, WATER, z, Blocks.WATER.getDefaultState());
					if (depth > 0.25) b.set(x, WATER - 1, z, Blocks.WATER.getDefaultState());
					if (depth > 0.6) b.set(x, WATER - 2, z, Blocks.WATER.getDefaultState());
					int bottom = depth > 0.6 ? WATER - 3 : depth > 0.25 ? WATER - 2 : WATER - 1;
					b.set(x, bottom, z, Terrain.rand(x, z, 1560) < 0.6 ? Blocks.SAND.getDefaultState() : Blocks.GRAVEL.getDefaultState());
					if (depth < 0.5 && Terrain.rand(x, z, 1561) < 0.06) b.set(x, G, z, Blocks.LILY_PAD.getDefaultState());
				} else if (e <= 1.25) {
					b.set(x, G, z, Blocks.SAND.getDefaultState());
				} else if (path) {
					b.set(x, G, z, Terrain.rand(x, z, 1562) < 0.7 ? Blocks.DIRT_PATH.getDefaultState() : Blocks.GRAVEL.getDefaultState());
				} else {
					b.set(x, G, z, Blocks.GRASS_BLOCK.getDefaultState());
					double r = Terrain.rand(x, z, 1563);
					if (r < 0.10) b.set(x, F, z, Blocks.GRASS.getDefaultState());
					else if (r < 0.16) b.set(x, F, z, (r < 0.12 ? Blocks.DANDELION : r < 0.14 ? Blocks.POPPY : Blocks.CORNFLOWER).getDefaultState());
				}
			}
		}
		// footbridge over the canal where it enters the park
		for (int x = TownPlan.CANAL_X1 - 1; x <= TownPlan.CANAL_X2 + 1; x++) {
			b.set(x, G, 12, Blocks.SPRUCE_PLANKS.getDefaultState());
			b.set(x, G, 13, Blocks.SPRUCE_PLANKS.getDefaultState());
			b.set(x, F, 11, Blocks.SPRUCE_FENCE.getDefaultState());
			b.set(x, F, 14, Blocks.SPRUCE_FENCE.getDefaultState());
		}
		for (int x = TownPlan.CANAL_X2 + 2; x <= -91; x++) {
			b.set(x, G, 12, Blocks.DIRT_PATH.getDefaultState());
			b.set(x, G, 13, Blocks.DIRT_PATH.getDefaultState());
		}
		// pier
		for (int z = pcz - 1; z <= pcz + 1; z++)
			for (int x = pcx + 6; x <= pcx + 12; x++) b.set(x, G, z, Blocks.SPRUCE_PLANKS.getDefaultState());
		b.set(pcx + 6, F, pcz - 1, Blocks.SPRUCE_FENCE.getDefaultState());
		b.set(pcx + 6, F, pcz + 1, Blocks.SPRUCE_FENCE.getDefaultState());
		b.set(pcx + 6, F + 1, pcz - 1, Build.lantern(false, false));
		// gazebo
		int gx = -106, gz = 66;
		for (int x = gx - 3; x <= gx + 3; x++)
			for (int z = gz - 3; z <= gz + 3; z++) b.set(x, G, z, Blocks.SPRUCE_PLANKS.getDefaultState());
		for (int[] c : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
			b.fill(gx + c[0], F, gz + c[1], gx + c[0], F + 3, gz + c[1], Blocks.STRIPPED_SPRUCE_LOG);
		}
		for (int i = 0; i < 4; i++) {
			int r = 4 - i;
			b.walls(gx - r, F + 4 + i, gz - r, gx + r, F + 4 + i, gz + r, Blocks.DARK_OAK_PLANKS.getDefaultState());
		}
		b.set(gx, F + 8, gz, Blocks.DARK_OAK_SLAB.getDefaultState());
		b.set(gx, F + 6, gz, Blocks.CHAIN.getDefaultState());
		b.set(gx, F + 5, gz, Blocks.CHAIN.getDefaultState());
		b.set(gx, F + 4, gz, Build.lantern(false, true));
		b.set(gx, F + 7, gz, Blocks.DARK_OAK_PLANKS.getDefaultState());
		for (int d = -1; d <= 1; d++) {
			b.set(gx + d, F, gz - 2, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
			b.set(gx + d, F, gz + 2, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
		}
		// trees: pink cherries and a few big oaks
		int placed = 0;
		for (int i = 0; i < 160 && placed < 26; i++) {
			int tx = x1 + 2 + (int) (Terrain.rand(i, 7, 1564) * (x2 - x1 - 4));
			int tz = z1 + 2 + (int) (Terrain.rand(i, 9, 1564) * (z2 - z1 - 4));
			if (!treeSpot(b, tx, tz)) continue;
			boolean cherry = Terrain.rand(i, 11, 1564) < 0.6;
			int h = 4 + (int) (Terrain.rand(i, 13, 1564) * 2);
			Block log = cherry ? Blocks.CHERRY_LOG : Blocks.OAK_LOG;
			BlockState leaves = (cherry ? Blocks.CHERRY_LEAVES : Blocks.OAK_LEAVES).getDefaultState().with(Properties.PERSISTENT, true);
			for (int y = F; y < F + h; y++) b.set(tx, y, tz, log.getDefaultState());
			for (int dx = -3; dx <= 3; dx++)
				for (int dy = -1; dy <= 2; dy++)
					for (int dz = -3; dz <= 3; dz++) {
						double dd = dx * dx + dz * dz + dy * dy * 2.2;
						if (dd <= 9.5 && Terrain.rand(tx + dx, tz + dz + dy * 31L, 1565) < 0.9)
							b.setIfEmpty(tx + dx, F + h + dy, tz + dz, leaves);
					}
			placed++;
		}
		// benches and lamps along the paths
		for (int z = 16; z <= 72; z += 14) {
			b.set(-88, F, z, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST));
			b.set(-88, F, z + 1, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST));
			b.fill(-92, F, z + 4, -92, F + 2, z + 4, Blocks.DARK_OAK_FENCE);
			b.set(-92, F + 3, z + 4, Build.lantern(false, false));
		}
	}

	private static boolean treeSpot(Build b, int x, int z) {
		for (int dx = -2; dx <= 2; dx++)
			for (int dz = -2; dz <= 2; dz++) {
				BlockState g = b.get(x + dx, G, z + dz);
				if (g == null || !g.isOf(Blocks.GRASS_BLOCK)) return false;
				BlockState f = b.get(x + dx, F, z + dz);
				if (f != null && !f.isReplaceable()) return false;
			}
		return true;
	}
}
