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
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The townhouses of Axel: narrow half-timbered terraces with stone ground floors, overhanging upper
 * storeys, steep roofs, chimneys and shops with awnings along the busy streets.
 */
final class TownHouses {
	private TownHouses() {
	}

	static final int G = AxelTown.G, F = AxelTown.F;

	enum Shop {
		BAKERY(Blocks.YELLOW_WOOL), GROCER(Blocks.LIME_WOOL), BUTCHER(Blocks.RED_WOOL), POTIONS(Blocks.PURPLE_WOOL),
		TAILOR(Blocks.MAGENTA_WOOL), ARMORER(Blocks.GRAY_WOOL), BOOKS(Blocks.BLUE_WOOL), FLOWERS(Blocks.PINK_WOOL),
		PUB(Blocks.ORANGE_WOOL), GENERAL(Blocks.LIGHT_BLUE_WOOL), CAFE(Blocks.BROWN_WOOL);

		final Block awning;

		Shop(Block awning) {
			this.awning = awning;
		}
	}

	private static final BlockState[] FILLS = {
		Blocks.CALCITE.getDefaultState(), Blocks.CALCITE.getDefaultState(), Blocks.WHITE_TERRACOTTA.getDefaultState(),
		Blocks.WHITE_TERRACOTTA.getDefaultState(), Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.MUD_BRICKS.getDefaultState(),
		Blocks.BIRCH_PLANKS.getDefaultState(), Blocks.POLISHED_DIORITE.getDefaultState(), Blocks.YELLOW_TERRACOTTA.getDefaultState(),
		Blocks.LIGHT_GRAY_TERRACOTTA.getDefaultState(), Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.CALCITE.getDefaultState(),
	};
	private static final Block[] FRAMES = {Blocks.DARK_OAK_LOG, Blocks.SPRUCE_LOG, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.DARK_OAK_LOG};
	private static final Block[] FRAME_STAIRS = {Blocks.DARK_OAK_STAIRS, Blocks.SPRUCE_STAIRS, Blocks.DARK_OAK_STAIRS, Blocks.SPRUCE_STAIRS, Blocks.DARK_OAK_STAIRS};
	private static final Block[] ROOFS = {
		Blocks.BRICK_STAIRS, Blocks.BRICK_STAIRS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.MANGROVE_STAIRS,
		Blocks.MANGROVE_STAIRS, Blocks.SPRUCE_STAIRS, Blocks.ACACIA_STAIRS, Blocks.DARK_OAK_STAIRS, Blocks.PRISMARINE_BRICK_STAIRS,
		Blocks.MUD_BRICK_STAIRS, Blocks.BRICK_STAIRS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.NETHER_BRICK_STAIRS,
	};
	private static final Block[] ROOF_SLABS = {
		Blocks.BRICK_SLAB, Blocks.BRICK_SLAB, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILE_SLAB, Blocks.MANGROVE_SLAB,
		Blocks.MANGROVE_SLAB, Blocks.SPRUCE_SLAB, Blocks.ACACIA_SLAB, Blocks.DARK_OAK_SLAB, Blocks.PRISMARINE_BRICK_SLAB,
		Blocks.MUD_BRICK_SLAB, Blocks.BRICK_SLAB, Blocks.DEEPSLATE_TILE_SLAB, Blocks.NETHER_BRICK_SLAB,
	};
	private static final Block[] DOORS = {Blocks.SPRUCE_DOOR, Blocks.OAK_DOOR, Blocks.DARK_OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR};
	private static final Block[] FLOORS = {Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.BIRCH_PLANKS};
	private static final Block[] BEDS = {Blocks.RED_BED, Blocks.BLUE_BED, Blocks.GREEN_BED, Blocks.YELLOW_BED, Blocks.WHITE_BED, Blocks.BROWN_BED, Blocks.CYAN_BED};
	private static final Block[] FLOWERS = {Blocks.POTTED_RED_TULIP, Blocks.POTTED_POPPY, Blocks.POTTED_CORNFLOWER, Blocks.POTTED_ALLIUM,
		Blocks.POTTED_AZURE_BLUET, Blocks.POTTED_ORANGE_TULIP, Blocks.POTTED_OXEYE_DAISY, Blocks.POTTED_PINK_TULIP};

	/** Local frame of a lot: u runs along the street front, v goes from the front wall (0) inwards. */
	private static final class Frame {
		final Build b;
		final TownPlan.Lot lot;
		final Direction out, in, right;
		final int w, d;

		Frame(Build b, TownPlan.Lot lot) {
			this.b = b;
			this.lot = lot;
			out = lot.face();
			in = out.getOpposite();
			right = out.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
			w = lot.width();
			d = lot.depth();
		}

		int x(int u, int v) {
			return switch (out) {
				case NORTH, SOUTH -> lot.x1() + u;
				case WEST -> lot.x1() + v;
				default -> lot.x2() - v;
			};
		}

		int z(int u, int v) {
			return switch (out) {
				case NORTH -> lot.z1() + v;
				case SOUTH -> lot.z2() - v;
				default -> lot.z1() + u;
			};
		}

		void set(int u, int y, int v, BlockState s) {
			b.set(x(u, v), y, z(u, v), s);
		}

		void set(int u, int y, int v, Block s) {
			b.set(x(u, v), y, z(u, v), s.getDefaultState());
		}

		void setIfEmpty(int u, int y, int v, BlockState s) {
			b.setIfEmpty(x(u, v), y, z(u, v), s);
		}

		byte outside(int u, int v) {
			return TownPlan.cell(x(u, v), z(u, v));
		}
	}

	static void buildAll(Build b) {
		List<TownPlan.Lot> lots = new ArrayList<>(TownPlan.lots());
		// lower houses first: the walls of taller neighbours then cut cleanly through overhanging eaves
		lots.sort(Comparator.comparingInt(TownPlan.Lot::floors));
		for (TownPlan.Lot lot : lots) house(b, lot);
	}

	private static double rand(TownPlan.Lot lot, int salt) {
		return Terrain.rand(lot.x1() * 31L + lot.z2(), lot.z1() * 17L + lot.x2(), salt);
	}

	private static <T> T pick(T[] arr, double r) {
		return arr[Math.min(arr.length - 1, (int) (r * arr.length))];
	}

	static Shop shopOf(TownPlan.Lot lot) {
		double chance = switch (lot.street()) {
			case 'A' -> 0.55;
			case 'R' -> 0.4;
			case 'I' -> 0.22;
			case 'C' -> 0.2;
			default -> 0;
		};
		if (rand(lot, 1301) >= chance || lot.width() < 5 || lot.depth() < 6) return null;
		return pick(Shop.values(), rand(lot, 1302));
	}

	private static void house(Build build, TownPlan.Lot lot) {
		Frame f = new Frame(build, lot);
		int w = f.w, d = f.d, n = lot.floors();
		BlockState fill = pick(FILLS, rand(lot, 1201));
		int fi = (int) (rand(lot, 1202) * FRAMES.length);
		Block frameLog = FRAMES[fi];
		Block frameStairs = FRAME_STAIRS[fi];
		int ri = (int) (rand(lot, 1203) * ROOFS.length);
		Block roof = ROOFS[ri];
		BlockState ridge = Build.slab(ROOF_SLABS[ri], false);
		Block floor = pick(FLOORS, rand(lot, 1204));
		Shop shop = shopOf(lot);
		boolean stoneBase = rand(lot, 1205) < 0.55 || shop != null;
		boolean jetty = n >= 2 && shop == null && rand(lot, 1206) < 0.45;
		int stoneKind = (int) (rand(lot, 1207) * 3);
		int doorU = w / 2;
		if (isPostU(doorU, w)) doorU--;
		int top = F + 4 * n - 1;

		// Foundation and ground floor
		for (int u = 0; u < w; u++) {
			for (int v = 0; v < d; v++) {
				boolean wall = u == 0 || u == w - 1 || v == 0 || v == d - 1;
				f.set(u, G, v, wall ? stone(f.x(u, v), f.z(u, v), stoneKind) : floor.getDefaultState());
			}
		}
		for (int k = 0; k < n; k++) {
			int y0 = F + 4 * k, y3 = y0 + 3;
			int vf = jetty && k >= 1 ? -1 : 0;
			// shell
			for (int y = y0; y <= y3; y++) {
				for (int u = 0; u < w; u++) {
					f.set(u, y, vf, wallBlock(f, u, vf, y, y0, y3, k, stoneBase, stoneKind, frameLog, fill, w, d, vf, true));
					f.set(u, y, d - 1, wallBlock(f, u, d - 1, y, y0, y3, k, stoneBase, stoneKind, frameLog, fill, w, d, vf, true));
				}
				for (int v = vf; v < d; v++) {
					f.set(0, y, v, wallBlock(f, 0, v, y, y0, y3, k, stoneBase, stoneKind, frameLog, fill, w, d, vf, false));
					f.set(w - 1, y, v, wallBlock(f, w - 1, v, y, y0, y3, k, stoneBase, stoneKind, frameLog, fill, w, d, vf, false));
				}
			}
			// rooms
			for (int u = 1; u < w - 1; u++) {
				for (int v = vf + 1; v < d - 1; v++) {
					for (int y = y0; y < y3; y++) f.set(u, y, v, Build.AIR);
					f.set(u, y3, v, floor.getDefaultState());
				}
			}
			if (vf == -1) {
				// the jettied storey rests on a beam and carved brackets over the street
				for (int u = 0; u < w; u++) {
					f.set(u, y0 - 1, -1, Build.log(frameLog, f.right.getAxis()));
					if (isPostU(u, w)) f.set(u, y0 - 2, -1, Build.stairsTop(frameStairs, f.in));
				}
			}
			windows(f, k, y0, vf, w, d, doorU, shop != null);
			if (k < n - 1) {
				for (int y = y0; y <= y3; y++) f.set(w - 2, y, d - 2, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, f.out));
			}
			if (k == 0) {
				if (shop != null) shopInterior(f, lot, shop, doorU);
				else livingRoom(f, lot, doorU);
			} else {
				bedroom(f, lot, k, y0, vf);
			}
			f.set(w / 2, y3 - 1, (vf + d) / 2, Build.lantern(false, true));
		}

		// Door, lamp and the front of the house
		Block door = pick(DOORS, rand(lot, 1208));
		f.set(doorU, F, 0, Build.AIR);
		f.set(doorU, F + 1, 0, Build.AIR);
		f.b.door(f.x(doorU, 0), F, f.z(doorU, 0), door, f.in, false);
		int tu = doorU + 1 < w - 1 ? doorU + 1 : doorU - 1;
		f.set(tu, F + 2, -1, Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, f.out));
		if (shop != null) shopFront(f, lot, shop, doorU, tu);

		// Roof
		int rv = jetty ? -1 : 0;
		boolean ridgeInwards = w <= 7 ? rand(lot, 1209) < 0.75 : rand(lot, 1209) < 0.3;
		boolean alongX = ridgeInwards ? f.out.getAxis() == Direction.Axis.X : f.out.getAxis() == Direction.Axis.Z;
		int rx1 = Math.min(f.x(0, rv), f.x(w - 1, d - 1)), rx2 = Math.max(f.x(0, rv), f.x(w - 1, d - 1));
		int rz1 = Math.min(f.z(0, rv), f.z(w - 1, d - 1)), rz2 = Math.max(f.z(0, rv), f.z(w - 1, d - 1));
		f.b.gableRoof(rx1, rz1, rx2, rz2, top + 1, roof, ridge, fill, alongX);
		int span = alongX ? rz2 - rz1 + 3 : rx2 - rx1 + 3;
		int peak = top + 1 + (span - 1) / 2;
		// round attic window in the gable that faces the street
		if (ridgeInwards && n >= 2 && w >= 6) f.set(w / 2, top + 2, rv, Blocks.GLASS_PANE.getDefaultState());

		// Chimney with a wisp of smoke
		if (rand(lot, 1210) < 0.5) {
			for (int y = F; y <= peak + 1; y++) f.set(w - 2, y, d - 1, Blocks.BRICKS.getDefaultState());
			f.set(w - 2, peak + 2, d - 1, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true).with(CampfireBlock.SIGNAL_FIRE, false));
		}
	}

	private static boolean isPostU(int u, int w) {
		int c = w / 2;
		return u == 0 || u == w - 1 || (w >= 8 && (u == c - 2 || u == c + 2));
	}

	private static boolean isPostV(int v, int vf, int d) {
		return v == vf || v == d - 1 || Math.floorMod(v - vf, 3) == 0;
	}

	private static BlockState stone(int x, int z, int kind) {
		double r = Terrain.rand(x, z, 1220);
		return switch (kind) {
			case 0 -> r < 0.6 ? Blocks.COBBLESTONE.getDefaultState() : r < 0.85 ? Blocks.MOSSY_COBBLESTONE.getDefaultState() : Blocks.STONE.getDefaultState();
			case 1 -> r < 0.7 ? Blocks.STONE_BRICKS.getDefaultState() : r < 0.88 ? Blocks.CRACKED_STONE_BRICKS.getDefaultState() : Blocks.MOSSY_STONE_BRICKS.getDefaultState();
			default -> r < 0.55 ? Blocks.ANDESITE.getDefaultState() : r < 0.8 ? Blocks.POLISHED_ANDESITE.getDefaultState() : Blocks.COBBLESTONE.getDefaultState();
		};
	}

	private static BlockState wallBlock(Frame f, int u, int v, int y, int y0, int y3, int k, boolean stoneBase, int stoneKind,
										Block frameLog, BlockState fill, int w, int d, int vf, boolean frontOrBack) {
		if (k == 0 && stoneBase) return stone(f.x(u, v), f.z(u, v) + y, stoneKind);
		boolean post = frontOrBack ? isPostU(u, w) : isPostV(v, vf, d);
		if (post) return Build.log(frameLog, Direction.Axis.Y);
		if (y == y0 || y == y3) {
			Direction.Axis axis = frontOrBack ? f.right.getAxis() : f.out.getAxis();
			return Build.log(frameLog, axis);
		}
		return fill;
	}

	private static void windows(Frame f, int k, int y0, int vf, int w, int d, int doorU, boolean shop) {
		BlockState pane = Blocks.GLASS_PANE.getDefaultState();
		boolean box = k >= 1 && Terrain.rand(f.lot.x1(), f.lot.z1() + k, 1230) < 0.45;
		for (int u = 1; u < w - 1; u++) {
			if (isPostU(u, w)) continue;
			boolean front;
			if (k == 0) front = Math.abs(u - doorU) > 1 && (shop || u % 2 == 1);
			else front = u % 2 == 1 || (w <= 6 && u == w / 2);
			if (front) {
				f.set(u, y0 + 1, vf, pane);
				f.set(u, y0 + 2, vf, pane);
				if (box) {
					f.set(u, y0, vf - 1, Build.trapdoor(Blocks.SPRUCE_TRAPDOOR, f.in, false, true));
					f.set(u, y0 + 1, vf - 1, pick(FLOWERS, Terrain.rand(f.x(u, 0), f.z(u, 0), 1231 + k)).getDefaultState());
				}
			}
			if (u % 2 == 1 && u != w - 2) {
				f.set(u, y0 + 1, d - 1, pane);
				f.set(u, y0 + 2, d - 1, pane);
			}
		}
		// side windows only where no neighbour leans against the wall
		for (int side = 0; side < 2; side++) {
			int u = side == 0 ? 0 : w - 1, ou = side == 0 ? -1 : w;
			for (int v = vf + 1; v < d - 1; v++) {
				if (isPostV(v, vf, d) || Math.floorMod(v - vf, 3) != 1) continue;
				byte o = f.outside(ou, Math.max(v, 0));
				if (o == TownPlan.HOUSE || o == TownPlan.LANDMARK) continue;
				f.set(u, y0 + 1, v, pane);
				f.set(u, y0 + 2, v, pane);
			}
		}
	}

	private static void livingRoom(Frame f, TownPlan.Lot lot, int doorU) {
		int w = f.w, d = f.d;
		double r = rand(lot, 1240);
		f.set(1, F, 1, Blocks.CRAFTING_TABLE);
		if (w - 2 != doorU) f.set(w - 2, F, 1, (r < 0.5 ? Blocks.FURNACE : Blocks.SMOKER).getDefaultState().with(HorizontalFacingBlock.FACING, f.in));
		f.set(1, F, d - 2, r < 0.3 ? Blocks.BOOKSHELF.getDefaultState() : Blocks.BARREL.getDefaultState());
		f.set(1, F + 1, d - 2, pick(FLOWERS, rand(lot, 1241)).getDefaultState());
		int tu = w / 2, tv = Math.max(2, d / 2);
		if (tv >= d - 2) tv = d - 3;
		if (tu == w - 2 && tv == d - 2) tv--;
		f.set(tu, F, tv, Blocks.OAK_FENCE);
		f.set(tu, F + 1, tv, r < 0.5 ? Blocks.WHITE_CARPET.getDefaultState() : Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true));
		if (tu - 1 >= 1) f.set(tu - 1, F, tv, Build.stairs(Blocks.OAK_STAIRS, f.right.getOpposite()));
		if (tu + 1 <= w - 2 && !(tu + 1 == w - 2 && tv == d - 2)) f.set(tu + 1, F, tv, Build.stairs(Blocks.OAK_STAIRS, f.right));
		if (d >= 8) f.set(w / 2, F, d - 2, Blocks.RED_CARPET);
	}

	private static void bedroom(Frame f, TownPlan.Lot lot, int k, int y0, int vf) {
		int w = f.w, d = f.d;
		Block bed = pick(BEDS, Terrain.rand(lot.x1() + k, lot.z1(), 1250));
		f.set(1, y0, d - 3, bed.getDefaultState().with(BedBlock.FACING, f.in).with(BedBlock.PART, BedPart.FOOT));
		f.set(1, y0, d - 2, bed.getDefaultState().with(BedBlock.FACING, f.in).with(BedBlock.PART, BedPart.HEAD));
		double r = Terrain.rand(lot.x1(), lot.z1() + k, 1251);
		f.set(1, y0, vf + 1, r < 0.5 ? Blocks.BOOKSHELF.getDefaultState() : Blocks.BARREL.getDefaultState());
		if (w >= 6) f.set(w - 2, y0, vf + 1, Blocks.CHISELED_BOOKSHELF.getDefaultState().with(HorizontalFacingBlock.FACING, f.in));
		f.set(w / 2, y0, (vf + d) / 2, r < 0.5 ? Blocks.BROWN_CARPET.getDefaultState() : Blocks.LIGHT_GRAY_CARPET.getDefaultState());
		if (d >= 8) f.set(2, y0, d - 2, pick(FLOWERS, r).getDefaultState());
	}

	// ------------------------------------------------------------------------------------------
	private static void shopInterior(Frame f, TownPlan.Lot lot, Shop shop, int doorU) {
		int w = f.w, d = f.d;
		int cv = d - 3;
		for (int u = 1; u <= w - 3; u++) {
			f.set(u, F, cv, Blocks.SPRUCE_PLANKS);
			f.set(u, F + 1, cv, counterItem(shop, u, lot));
		}
		for (int u = 1; u <= w - 3; u++) {
			f.set(u, F, d - 2, shelf(shop, u, 0));
			f.set(u, F + 1, d - 2, shelf(shop, u, 1));
		}
		if (doorU != 1) f.set(1, F, 1, shelf(shop, 3, 2));
		if (doorU != w - 2) f.set(w - 2, F, 1, shelf(shop, 4, 2));
	}

	private static BlockState counterItem(Shop shop, int u, TownPlan.Lot lot) {
		boolean odd = u % 2 == 1;
		return switch (shop) {
			case BAKERY -> odd ? Blocks.CAKE.getDefaultState() : Blocks.AIR.getDefaultState();
			case POTIONS -> odd ? Blocks.BREWING_STAND.getDefaultState() : Blocks.POTTED_CRIMSON_FUNGUS.getDefaultState();
			case FLOWERS -> pick(FLOWERS, Terrain.rand(u, lot.x1(), 1260)).getDefaultState();
			case BOOKS -> odd ? Blocks.LECTERN.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.NORTH) : Blocks.AIR.getDefaultState();
			case PUB, CAFE -> odd ? Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true).with(CandleBlock.CANDLES, 2) : Blocks.AIR.getDefaultState();
			case ARMORER -> odd ? Blocks.ANVIL.getDefaultState() : Blocks.AIR.getDefaultState();
			default -> odd ? Blocks.LANTERN.getDefaultState() : Blocks.AIR.getDefaultState();
		};
	}

	private static BlockState shelf(Shop shop, int u, int layer) {
		Block[] set = switch (shop) {
			case BAKERY -> new Block[]{Blocks.HAY_BLOCK, Blocks.BARREL, Blocks.SMOKER};
			case GROCER -> new Block[]{Blocks.MELON, Blocks.PUMPKIN, Blocks.COMPOSTER};
			case BUTCHER -> new Block[]{Blocks.SMOKER, Blocks.BARREL, Blocks.HAY_BLOCK};
			case POTIONS -> new Block[]{Blocks.BOOKSHELF, Blocks.CAULDRON, Blocks.AMETHYST_BLOCK};
			case TAILOR -> new Block[]{Blocks.RED_WOOL, Blocks.BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.LOOM, Blocks.WHITE_WOOL};
			case ARMORER -> new Block[]{Blocks.SMITHING_TABLE, Blocks.GRINDSTONE, Blocks.BLAST_FURNACE};
			case BOOKS -> new Block[]{Blocks.BOOKSHELF, Blocks.BOOKSHELF, Blocks.CHISELED_BOOKSHELF};
			case FLOWERS -> new Block[]{Blocks.FLOWERING_AZALEA, Blocks.AZALEA, Blocks.MOSS_BLOCK};
			case PUB -> new Block[]{Blocks.BARREL, Blocks.BARREL, Blocks.BREWING_STAND};
			case GENERAL -> new Block[]{Blocks.BARREL, Blocks.CRAFTING_TABLE, Blocks.CARTOGRAPHY_TABLE, Blocks.FLETCHING_TABLE};
			case CAFE -> new Block[]{Blocks.BARREL, Blocks.SMOKER, Blocks.BOOKSHELF};
		};
		Block blk = set[Math.floorMod(u * 3 + layer, set.length)];
		BlockState s = blk.getDefaultState();
		if (s.contains(Properties.HORIZONTAL_FACING)) s = s.with(Properties.HORIZONTAL_FACING, Direction.NORTH);
		if (s.contains(net.minecraft.block.WallMountedBlock.FACE)) s = s.cycle(net.minecraft.block.WallMountedBlock.FACE).cycle(net.minecraft.block.WallMountedBlock.FACE);
		return s;
	}

	private static void shopFront(Frame f, TownPlan.Lot lot, Shop shop, int doorU, int torchU) {
		int w = f.w;
		BlockState stripe = Blocks.WHITE_WOOL.getDefaultState();
		for (int u = 0; u < w; u++) {
			f.set(u, F + 3, -1, u % 2 == 0 ? shop.awning.getDefaultState() : stripe);
		}
		// goods out on the street beside the door
		for (int s = -1; s <= 1; s += 2) {
			int u = doorU + 2 * s;
			if (u < 0 || u >= w || u == torchU) continue;
			BlockState goods = switch (shop) {
				case BAKERY, BUTCHER -> Blocks.HAY_BLOCK.getDefaultState();
				case GROCER -> s < 0 ? Blocks.MELON.getDefaultState() : Blocks.PUMPKIN.getDefaultState();
				case FLOWERS -> pick(FLOWERS, Terrain.rand(lot.x1(), u, 1270)).getDefaultState();
				case POTIONS -> Blocks.WATER_CAULDRON.getDefaultState().with(Properties.LEVEL_3, 3);
				case ARMORER -> Blocks.GRINDSTONE.getDefaultState().with(Properties.HORIZONTAL_FACING, f.right).cycle(net.minecraft.block.WallMountedBlock.FACE).cycle(net.minecraft.block.WallMountedBlock.FACE);
				case PUB, CAFE -> Blocks.BARREL.getDefaultState();
				case BOOKS -> Blocks.BOOKSHELF.getDefaultState();
				default -> Blocks.BARREL.getDefaultState().with(Properties.FACING, Direction.UP);
			};
			f.setIfEmpty(u, F, -1, goods);
		}
	}
}
