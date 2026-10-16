package dev.konocraft.world.structure;

import dev.konocraft.registry.ModBlocks;
import dev.konocraft.world.Build;
import dev.konocraft.world.Regions;
import dev.konocraft.world.Terrain;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/** Axel, the town where beginner adventurers start. */
public final class AxelTown {
	private AxelTown() {
	}

	static final int G = Regions.TOWN_Y;      // ground block
	static final int F = Regions.TOWN_Y + 1;  // first walkable layer
	static final int H = Regions.TOWN_HALF;   // outer wall coordinate

	/** Areas reserved for landmark buildings: x1, z1, x2, z2. */
	static final int[][] RESERVED = {
		{-46, -42, -10, -12},  // guild (+margin)
		{12, -34, 34, -12},    // Wiz's shop
		{14, 10, 42, 48},      // church of Eris (+tower)
		{-32, 28, -14, 46},    // Axis church
		{-68, 44, -44, 66},    // stable
		{34, -72, 70, -40},    // mansion
	};

	public static void build(Build b) {
		ground(b);
		walls(b);
		plaza(b);
		guild(b);
		wizShop(b);
		erisChurch(b);
		axisChurch(b);
		stable(b);
		mansion(b);
		houses(b);
		lamps(b);
	}

	// ------------------------------------------------------------------------------------------
	static BlockState cobbleStreet(int x, int z) {
		double r = Terrain.rand(x, z, 201);
		if (r < 0.45) return Blocks.STONE_BRICKS.getDefaultState();
		if (r < 0.65) return Blocks.ANDESITE.getDefaultState();
		if (r < 0.80) return Blocks.COBBLESTONE.getDefaultState();
		if (r < 0.92) return Blocks.POLISHED_ANDESITE.getDefaultState();
		return Blocks.MOSSY_STONE_BRICKS.getDefaultState();
	}

	static boolean street(int x, int z) {
		return Math.abs(x) <= 3 || Math.abs(z) <= 3;
	}

	private static void ground(Build b) {
		for (int x = -H + 3; x <= H - 3; x++) {
			for (int z = -H + 3; z <= H - 3; z++) {
				if (street(x, z) || x * x + z * z <= 15 * 15) {
					b.set(x, G, z, cobbleStreet(x, z));
				}
			}
		}
		// Streets continue through the gates and a little beyond
		for (int i = H - 3; i <= H + 8; i++) {
			for (int w = -3; w <= 3; w++) {
				b.set(w, G, i, cobbleStreet(w, i));
				b.set(w, G, -i, cobbleStreet(w, -i));
				b.set(i, G, w, cobbleStreet(i, w));
				b.set(-i, G, w, cobbleStreet(-i, w));
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	private static void walls(Build b) {
		BlockState[] mix = {Blocks.STONE_BRICKS.getDefaultState(), Blocks.MOSSY_STONE_BRICKS.getDefaultState(),
			Blocks.CRACKED_STONE_BRICKS.getDefaultState(), Blocks.STONE_BRICKS.getDefaultState()};
		int top = F + 9;
		for (int i = -H; i <= H; i++) {
			for (int t = 0; t < 3; t++) {
				int c = H - t;
				b.fillMix(i, G, c, i, top, c, mix);
				b.fillMix(i, G, -c, i, top, -c, mix);
				b.fillMix(c, G, i, c, top, i, mix);
				b.fillMix(-c, G, i, -c, top, i, mix);
			}
			if (Math.floorMod(i, 2) == 0) {
				b.set(i, top + 1, H, Blocks.STONE_BRICKS.getDefaultState());
				b.set(i, top + 1, -H, Blocks.STONE_BRICKS.getDefaultState());
				b.set(H, top + 1, i, Blocks.STONE_BRICKS.getDefaultState());
				b.set(-H, top + 1, i, Blocks.STONE_BRICKS.getDefaultState());
			}
			b.set(i, top + 1, H - 2, Blocks.STONE_BRICK_WALL.getDefaultState());
			b.set(i, top + 1, -(H - 2), Blocks.STONE_BRICK_WALL.getDefaultState());
			b.set(H - 2, top + 1, i, Blocks.STONE_BRICK_WALL.getDefaultState());
			b.set(-(H - 2), top + 1, i, Blocks.STONE_BRICK_WALL.getDefaultState());
		}
		// Gates
		gate(b, Direction.NORTH);
		gate(b, Direction.SOUTH);
		gate(b, Direction.EAST);
		gate(b, Direction.WEST);
		// Round corner towers
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				int cx = sx * (H - 1), cz = sz * (H - 1);
				b.cylinder(cx, G, top + 6, cz, 5.5, Blocks.STONE_BRICKS.getDefaultState(), false);
				b.cylinder(cx, F, top + 5, cz, 4.2, Build.AIR, false);
				b.disk(cx, top + 1, cz, 4.2, Blocks.SPRUCE_PLANKS.getDefaultState());
				for (int k = 0; k < 8; k++) {
					double a = k * Math.PI / 4;
					b.set(cx + (int) Math.round(Math.cos(a) * 5), top + 3, cz + (int) Math.round(Math.sin(a) * 5), Blocks.AIR.getDefaultState());
				}
				b.cone(cx, top + 7, cz, 6.5, 10, Blocks.DARK_PRISMARINE.getDefaultState());
				b.set(cx, top + 17, cz, Blocks.LIGHTNING_ROD.getDefaultState());
			}
		}
	}

	private static void gate(Build b, Direction side) {
		int top = F + 9;
		// Local frame: "along" axis is the wall axis, "out" is the outward direction.
		for (int along = -10; along <= 10; along++) {
			for (int out = -4; out <= 2; out++) {
				int depth = H - 1 + out;
				int[] p = toWorld(side, along, depth);
				boolean tower = Math.abs(along) >= 4 && Math.abs(along) <= 10;
				boolean passage = Math.abs(along) <= 3;
				if (tower) {
					for (int y = G; y <= top + 5; y++) {
						BlockState s = (Math.abs(along) == 4 || Math.abs(along) == 10 || out == -4 || out == 2)
							? Blocks.STONE_BRICKS.getDefaultState()
							: (y == top + 1 ? Blocks.SPRUCE_PLANKS.getDefaultState() : Build.AIR);
						b.set(p[0], y, p[1], s);
					}
					if ((Math.abs(along) + out) % 2 == 0 && (Math.abs(along) == 4 || Math.abs(along) == 10 || out == -4 || out == 2)) {
						b.set(p[0], top + 6, p[1], Blocks.STONE_BRICKS.getDefaultState());
					}
					if (out == 2 && Math.abs(along) == 7) {
						b.set(p[0], F + 4, p[1], Blocks.AIR.getDefaultState());
						b.set(p[0], F + 5, p[1], Blocks.AIR.getDefaultState());
					}
				} else if (passage) {
					for (int y = F; y <= F + 6; y++) b.set(p[0], y, p[1], Build.AIR);
					if (Math.abs(along) == 3) b.set(p[0], F + 6, p[1], Blocks.STONE_BRICKS.getDefaultState());
					if (out == 2 || out == -2) b.set(p[0], F + 7, p[1], Blocks.STONE_BRICKS.getDefaultState());
					if (out == 1 && Math.abs(along) <= 2) b.set(p[0], F + 6, p[1], Blocks.IRON_BARS.getDefaultState());
					b.set(p[0], G, p[1], cobbleStreet(p[0], p[1]));
				}
			}
		}
		// Lanterns flanking the gate, inside and outside
		for (int s = -1; s <= 1; s += 2) {
			int[] outside = toWorld(side, 4 * s, H + 2);
			int[] inside = toWorld(side, 4 * s, H - 6);
			b.set(outside[0], F + 3, outside[1], Build.lantern(false, false));
			b.set(inside[0], F, inside[1], Blocks.SPRUCE_FENCE.getDefaultState());
			b.set(inside[0], F + 1, inside[1], Blocks.SPRUCE_FENCE.getDefaultState());
			b.set(inside[0], F + 2, inside[1], Build.lantern(false, false));
		}
	}

	/** along: position along the wall, depth: distance from the town centre towards the outside. */
	static int[] toWorld(Direction side, int along, int depth) {
		return switch (side) {
			case NORTH -> new int[]{along, -depth};
			case SOUTH -> new int[]{along, depth};
			case EAST -> new int[]{depth, along};
			default -> new int[]{-depth, along};
		};
	}

	// ------------------------------------------------------------------------------------------
	private static void plaza(Build b) {
		// Fountain
		b.disk(0, G, 0, 6, Blocks.STONE_BRICKS.getDefaultState());
		b.ring(0, F, 0, 6, 1.2, Blocks.STONE_BRICK_WALL.getDefaultState());
		b.disk(0, F, 0, 4.9, Blocks.WATER.getDefaultState());
		b.fill(0, F, 0, 0, F + 2, 0, Blocks.CHISELED_STONE_BRICKS);
		b.ring(0, F + 3, 0, 1.6, 1.2, Blocks.STONE_BRICK_WALL.getDefaultState());
		b.set(0, F + 3, 0, Blocks.WATER.getDefaultState());
		for (Direction d : Direction.Type.HORIZONTAL) {
			b.set(d.getOffsetX(), F + 2, d.getOffsetZ(), Blocks.WATER.getDefaultState());
		}
		// Benches facing the fountain
		for (int k = 0; k < 8; k++) {
			double a = k * Math.PI / 4 + Math.PI / 8;
			int x = (int) Math.round(Math.cos(a) * 10), z = (int) Math.round(Math.sin(a) * 10);
			Direction face = Math.abs(x) > Math.abs(z) ? (x > 0 ? Direction.EAST : Direction.WEST) : (z > 0 ? Direction.SOUTH : Direction.NORTH);
			b.set(x, F, z, Build.stairs(Blocks.SPRUCE_STAIRS, face));
			int[] side = {face.rotateYClockwise().getOffsetX(), face.rotateYClockwise().getOffsetZ()};
			b.set(x + side[0], F, z + side[1], Build.stairs(Blocks.SPRUCE_STAIRS, face));
		}
		// Flower beds
		for (int k = 0; k < 4; k++) {
			double a = k * Math.PI / 2 + Math.PI / 4;
			int x = (int) Math.round(Math.cos(a) * 13), z = (int) Math.round(Math.sin(a) * 13);
			b.fill(x - 1, G, z - 1, x + 1, G, z + 1, Blocks.GRASS_BLOCK);
			b.walls(x - 2, F, z - 2, x + 2, F, z + 2, Blocks.SPRUCE_TRAPDOOR.getDefaultState());
			b.set(x, F, z, Blocks.OAK_LOG.getDefaultState());
			b.set(x, F + 1, z, Blocks.OAK_LOG.getDefaultState());
			b.sphere(x, F + 3, z, 2.2, Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState().with(Properties.PERSISTENT, true));
			b.set(x, F + 2, z, Blocks.OAK_LOG.getDefaultState());
			b.set(x - 1, F, z, Blocks.RED_TULIP.getDefaultState());
			b.set(x + 1, F, z, Blocks.ORANGE_TULIP.getDefaultState());
			b.set(x, F, z - 1, Blocks.CORNFLOWER.getDefaultState());
			b.set(x, F, z + 1, Blocks.ALLIUM.getDefaultState());
		}
		// Market stalls
		stall(b, 8, 9, Blocks.RED_WOOL, Blocks.CARVED_PUMPKIN, Blocks.MELON);
		stall(b, -9, 9, Blocks.YELLOW_WOOL, Blocks.HAY_BLOCK, Blocks.BARREL);
		stall(b, 9, -9, Blocks.BLUE_WOOL, Blocks.BOOKSHELF, Blocks.BARREL);
		stall(b, -9, -9, Blocks.GREEN_WOOL, Blocks.MELON, Blocks.PUMPKIN);
	}

	private static void stall(Build b, int x, int z, Block wool, Block goods1, Block goods2) {
		for (int dx = -1; dx <= 1; dx += 2) {
			for (int dz = -1; dz <= 1; dz += 2) {
				b.set(x + dx, F, z + dz, Blocks.OAK_FENCE.getDefaultState());
				b.set(x + dx, F + 1, z + dz, Blocks.OAK_FENCE.getDefaultState());
			}
		}
		b.fill(x - 1, F + 2, z - 1, x + 1, F + 2, z + 1, wool);
		b.set(x, F + 3, z, wool.getDefaultState());
		b.set(x - 1, F, z, Build.slab(Blocks.SPRUCE_SLAB, true));
		b.set(x + 1, F, z, Build.slab(Blocks.SPRUCE_SLAB, true));
		b.set(x, F, z, goods1.getDefaultState());
		b.set(x, F, z - 1, goods2.getDefaultState());
	}

	// ------------------------------------------------------------------------------------------
	/** Half-timbered wall segment helper used by the landmark buildings. */
	static void timberWalls(Build b, int x1, int y1, int z1, int x2, int y2, int z2, BlockState frame, BlockState fill) {
		b.walls(x1, y1, z1, x2, y2, z2, fill);
		for (int y = y1; y <= y2; y++) {
			b.set(x1, y, z1, frame);
			b.set(x2, y, z1, frame);
			b.set(x1, y, z2, frame);
			b.set(x2, y, z2, frame);
		}
		BlockState beamX = frame.contains(Properties.AXIS) ? frame.with(Properties.AXIS, Direction.Axis.X) : frame;
		BlockState beamZ = frame.contains(Properties.AXIS) ? frame.with(Properties.AXIS, Direction.Axis.Z) : frame;
		for (int x = x1 + 1; x < x2; x++) {
			b.set(x, y2, z1, beamX);
			b.set(x, y2, z2, beamX);
			b.set(x, y1, z1, beamX);
			b.set(x, y1, z2, beamX);
		}
		for (int z = z1 + 1; z < z2; z++) {
			b.set(x1, y2, z, beamZ);
			b.set(x2, y2, z, beamZ);
			b.set(x1, y1, z, beamZ);
			b.set(x2, y1, z, beamZ);
		}
		// vertical posts every 4 blocks
		for (int x = x1 + 4; x < x2; x += 4) {
			for (int y = y1; y <= y2; y++) {
				b.set(x, y, z1, frame);
				b.set(x, y, z2, frame);
			}
		}
		for (int z = z1 + 4; z < z2; z += 4) {
			for (int y = y1; y <= y2; y++) {
				b.set(x1, y, z, frame);
				b.set(x2, y, z, frame);
			}
		}
	}

	/** Windows on all four walls at the given height, every `step` blocks, skipping frame posts. */
	static void windows(Build b, int x1, int z1, int x2, int z2, int y, int height, BlockState pane) {
		for (int x = x1 + 2; x <= x2 - 2; x++) {
			if (Math.floorMod(x - x1, 4) == 0) continue;
			if (Math.floorMod(x - x1, 4) == 2) {
				for (int yy = y; yy < y + height; yy++) {
					b.set(x, yy, z1, pane);
					b.set(x, yy, z2, pane);
				}
			}
		}
		for (int z = z1 + 2; z <= z2 - 2; z++) {
			if (Math.floorMod(z - z1, 4) == 2) {
				for (int yy = y; yy < y + height; yy++) {
					b.set(x1, yy, z, pane);
					b.set(x2, yy, z, pane);
				}
			}
		}
	}

	private static void chandelier(Build b, int x, int yTop, int z, int drop) {
		for (int i = 0; i < drop; i++) b.set(x, yTop - i, z, Blocks.CHAIN.getDefaultState());
		b.set(x, yTop - drop, z, Build.lantern(false, true));
	}

	// ------------------------------------------------------------------------------------------
	/** The Adventurers' Guild: reception, quest board and the tavern where every story begins. */
	private static void guild(Build b) {
		int x1 = -44, z1 = -40, x2 = -12, z2 = -14;
		BlockState stone = Blocks.STONE_BRICKS.getDefaultState();
		BlockState frame = Blocks.DARK_OAK_LOG.getDefaultState();
		BlockState plaster = Blocks.CALCITE.getDefaultState();
		b.fill(x1, G, z1, x2, G, z2, stone);
		b.clear(x1 + 1, F, z1 + 1, x2 - 1, F + 12, z2 - 1);
		// Ground floor: stone walls, upper floor: half-timbered
		b.wallsMix(x1, F, z1, x2, F + 5, z2, stone, Blocks.CRACKED_STONE_BRICKS.getDefaultState(), Blocks.STONE_BRICKS.getDefaultState());
		timberWalls(b, x1, F + 6, z1, x2, F + 11, z2, frame, plaster);
		b.fill(x1 + 1, F + 6, z1 + 1, x2 - 1, F + 6, z2 - 1, Blocks.SPRUCE_PLANKS);
		windows(b, x1, z1, x2, z2, F + 2, 3, Blocks.GLASS_PANE.getDefaultState());
		windows(b, x1, z1, x2, z2, F + 8, 2, Blocks.GLASS_PANE.getDefaultState());
		b.gableRoof(x1, z1, x2, z2, F + 12, Blocks.DEEPSLATE_TILE_STAIRS, Build.slab(Blocks.DEEPSLATE_TILE_SLAB, false), plaster, true);
		// Floor
		for (int x = x1 + 1; x < x2; x++)
			for (int z = z1 + 1; z < z2; z++)
				b.set(x, G, z, Math.floorMod(x + z, 2) == 0 ? Blocks.SPRUCE_PLANKS.getDefaultState() : Blocks.DARK_OAK_PLANKS.getDefaultState());
		// Entrance: double door with a porch and the guild emblem above
		b.clear(-30, F, z2, -27, F + 3, z2);
		b.door(-29, F, z2, Blocks.SPRUCE_DOOR, Direction.NORTH, false);
		b.door(-28, F, z2, Blocks.SPRUCE_DOOR, Direction.NORTH, true);
		b.set(-30, F, z2, frame);
		b.set(-27, F, z2, frame);
		for (int x = -31; x <= -26; x++) b.set(x, G, z2 + 1, Build.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH));
		for (int x = -31; x <= -26; x++) b.set(x, F + 3, z2 + 1, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
		b.set(-31, F + 2, z2 + 1, Build.lantern(false, true));
		b.set(-26, F + 2, z2 + 1, Build.lantern(false, true));
		// Emblem: a shield with crossed swords
		int ey = F + 7;
		b.fill(-30, ey, z2, -27, ey + 3, z2, Blocks.RED_WOOL);
		b.set(-30, ey + 3, z2, Blocks.GOLD_BLOCK.getDefaultState());
		b.set(-27, ey + 3, z2, Blocks.GOLD_BLOCK.getDefaultState());
		b.set(-29, ey + 1, z2, Blocks.YELLOW_WOOL.getDefaultState());
		b.set(-28, ey + 2, z2, Blocks.YELLOW_WOOL.getDefaultState());
		b.set(-29, ey, z2, Blocks.WHITE_WOOL.getDefaultState());
		b.set(-28, ey, z2, Blocks.WHITE_WOOL.getDefaultState());

		// Reception counter along the north wall; Luna stands behind it
		for (int x = -34; x <= -22; x++) {
			b.set(x, F, -36, Blocks.BARREL.getDefaultState().with(Properties.FACING, Direction.UP));
			b.set(x, F + 1, -36, Build.slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
		}
		b.set(-28, F + 1, -36, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
		b.set(-35, F, -36, Blocks.SPRUCE_FENCE_GATE.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.EAST));
		for (int x = -42; x <= -14; x++) {
			if (Math.floorMod(x, 3) == 0) b.fill(x, F, z1 + 1, x, F + 3, z1 + 1, Blocks.BOOKSHELF);
			else b.set(x, F, z1 + 1, Blocks.BARREL.getDefaultState());
		}
		b.set(-31, F + 2, -36, Blocks.POTTED_RED_TULIP.getDefaultState());
		b.set(-25, F + 2, -36, Blocks.LECTERN.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		b.set(-28, F + 2, -38, Build.lantern(false, false));

		// Quest board on the west wall
		BlockState board = ModBlocks.QUEST_BOARD.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.EAST);
		for (int z = -29; z <= -25; z++) {
			b.set(x1 + 1, F + 1, z, board);
			b.set(x1 + 1, F + 2, z, board);
		}
		b.set(x1 + 1, F + 3, -30, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true));

		// Tavern tables (east half)
		int[][] tables = {{-21, -31}, {-16, -31}, {-21, -25}, {-16, -25}, {-21, -19}, {-16, -19}};
		for (int[] t : tables) {
			b.set(t[0], F, t[1], Blocks.DARK_OAK_FENCE.getDefaultState());
			b.set(t[0], F + 1, t[1], Blocks.SPRUCE_PRESSURE_PLATE.getDefaultState());
			b.set(t[0] - 1, F, t[1], Build.stairs(Blocks.SPRUCE_STAIRS, Direction.WEST));
			b.set(t[0] + 1, F, t[1], Build.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST));
			b.set(t[0], F, t[1] - 1, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
		}
		// Bar along the east wall
		for (int z = -36; z <= -28; z++) {
			b.set(-14, F, z, Blocks.SPRUCE_PLANKS.getDefaultState());
			b.set(-14, F + 1, z, Build.slab(Blocks.SPRUCE_SLAB, false));
			b.set(x2 - 1, F, z, Blocks.BARREL.getDefaultState());
			b.set(x2 - 1, F + 1, z, Blocks.BARREL.getDefaultState().with(Properties.FACING, Direction.WEST));
		}
		b.set(-14, F + 2, -33, Blocks.BREWING_STAND.getDefaultState());
		b.set(x2 - 1, F, -38, Blocks.SMOKER.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
		b.set(x2 - 1, F, -37, Blocks.CAULDRON.getDefaultState());
		b.set(x2 - 2, F, -38, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, false));

		// Staircase to the upper floor along the west wall
		for (int i = 0; i < 6; i++) {
			b.set(x1 + 1, F + i, z2 - 2 - i, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
			b.set(x1 + 2, F + i, z2 - 2 - i, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
			for (int k = 1; k <= 3; k++) {
				b.set(x1 + 1, F + i + k, z2 - 2 - i, Build.AIR);
				b.set(x1 + 2, F + i + k, z2 - 2 - i, Build.AIR);
			}
		}
		b.set(x1 + 1, F + 6, z2 - 8, Blocks.SPRUCE_PLANKS.getDefaultState());
		b.set(x1 + 2, F + 6, z2 - 8, Blocks.SPRUCE_PLANKS.getDefaultState());
		for (int z = z2 - 7; z <= z2 - 2; z++) {
			b.set(x1 + 3, F + 6, z, Build.AIR);
			b.set(x1 + 3, F + 7, z, Blocks.SPRUCE_FENCE.getDefaultState());
		}
		// Chandeliers
		for (int x = -38; x <= -18; x += 10) {
			chandelier(b, x, F + 5, -26, 2);
			chandelier(b, x, F + 5, -20, 1);
		}
		// Upper floor: guild master's office and a reading room
		b.fill(-30, F + 7, z1 + 1, -30, F + 10, z2 - 1, Blocks.SPRUCE_PLANKS);
		b.clear(-30, F + 7, -27, -30, F + 8, -27);
		b.door(-30, F + 7, -27, Blocks.SPRUCE_DOOR, Direction.EAST, false);
		for (int x = -28; x <= -14; x += 3) b.fill(x, F + 7, z1 + 1, x, F + 9, z1 + 1, Blocks.BOOKSHELF);
		b.set(-22, F + 7, -30, Blocks.DARK_OAK_FENCE.getDefaultState());
		b.set(-22, F + 8, -30, Blocks.DARK_OAK_PRESSURE_PLATE.getDefaultState());
		b.set(-22, F + 7, -31, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
		b.set(-40, F + 7, -36, Blocks.DARK_OAK_PLANKS.getDefaultState());
		b.set(-39, F + 7, -36, Blocks.DARK_OAK_PLANKS.getDefaultState());
		b.set(-40, F + 8, -36, Blocks.LANTERN.getDefaultState());
		b.set(-39, F + 7, -37, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH));
		b.set(-34, F + 7, -24, Blocks.RED_CARPET.getDefaultState());
		b.set(-35, F + 7, -24, Blocks.RED_CARPET.getDefaultState());
		chandelier(b, -36, F + 11, -26, 1);
		chandelier(b, -22, F + 11, -26, 1);
	}

	// ------------------------------------------------------------------------------------------
	/** Wiz's magic item shop: famous for selling only useless items. */
	private static void wizShop(Build b) {
		int x1 = 14, z1 = -32, x2 = 32, z2 = -14;
		BlockState frame = Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState();
		BlockState wall = Blocks.DARK_OAK_PLANKS.getDefaultState();
		b.fill(x1, G, z1, x2, G, z2, Blocks.POLISHED_DEEPSLATE);
		b.clear(x1 + 1, F, z1 + 1, x2 - 1, F + 10, z2 - 1);
		timberWalls(b, x1, F, z1, x2, F + 5, z2, frame, wall);
		windows(b, x1, z1, x2, z2, F + 1, 3, Blocks.PURPLE_STAINED_GLASS_PANE.getDefaultState());
		b.gableRoof(x1, z1, x2, z2, F + 6, Blocks.PURPUR_STAIRS, Build.slab(Blocks.PURPUR_SLAB, false), wall, false);
		for (int x = x1 + 1; x < x2; x++)
			for (int z = z1 + 1; z < z2; z++)
				b.set(x, G, z, Math.floorMod(x * 3 + z, 5) == 0 ? Blocks.PURPLE_TERRACOTTA.getDefaultState() : Blocks.DARK_OAK_PLANKS.getDefaultState());
		// Door and a little awning
		b.clear(23, F, z2, 23, F + 1, z2);
		b.door(23, F, z2, Blocks.DARK_OAK_DOOR, Direction.NORTH, false);
		for (int x = 20; x <= 26; x++) b.set(x, F + 3, z2 + 1, Build.slab(Blocks.PURPUR_SLAB, true));
		b.set(21, F + 2, z2 + 1, Build.lantern(true, true));
		b.set(25, F + 2, z2 + 1, Build.lantern(true, true));
		b.set(19, F, z2 + 1, Blocks.POTTED_ALLIUM.getDefaultState());
		b.set(27, F, z2 + 1, Blocks.POTTED_ALLIUM.getDefaultState());
		// Counter (Wiz stands behind it)
		for (int x = 19; x <= 27; x++) {
			b.set(x, F, -27, Blocks.DARK_OAK_PLANKS.getDefaultState());
			b.set(x, F + 1, -27, Build.slab(Blocks.DARK_OAK_SLAB, false));
		}
		b.set(20, F + 2, -27, Blocks.AMETHYST_CLUSTER.getDefaultState());
		b.set(26, F + 2, -27, Blocks.BREWING_STAND.getDefaultState());
		b.set(23, F + 2, -27, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true).with(CandleBlock.CANDLES, 2));
		// Shelves full of dubious goods
		for (int x = x1 + 1; x < x2; x++) {
			if (x == 23) continue;
			b.set(x, F, z1 + 1, Blocks.BOOKSHELF.getDefaultState());
			b.set(x, F + 1, z1 + 1, Math.floorMod(x, 2) == 0 ? Blocks.BARREL.getDefaultState() : Blocks.BOOKSHELF.getDefaultState());
			b.set(x, F + 2, z1 + 1, Build.slab(Blocks.DARK_OAK_SLAB, false));
		}
		for (int z = z1 + 3; z <= z2 - 3; z += 2) {
			b.set(x1 + 1, F, z, Blocks.BARREL.getDefaultState().with(Properties.FACING, Direction.EAST));
			b.set(x1 + 1, F + 1, z, Blocks.POTTED_CRIMSON_FUNGUS.getDefaultState());
			b.set(x2 - 1, F, z, Blocks.BOOKSHELF.getDefaultState());
			b.set(x2 - 1, F + 1, z, Blocks.AMETHYST_CLUSTER.getDefaultState());
		}
		b.set(x1 + 2, F, z2 - 2, Blocks.CAULDRON.getDefaultState());
		b.set(x2 - 2, F, z2 - 2, Blocks.ENCHANTING_TABLE.getDefaultState());
		chandelier(b, 23, F + 5, -21, 1);
		b.set(18, F + 4, -21, Build.lantern(true, true));
		b.set(28, F + 4, -21, Build.lantern(true, true));
		b.set(17, F, -17, Blocks.PURPLE_CARPET.getDefaultState());
		b.set(18, F, -17, Blocks.PURPLE_CARPET.getDefaultState());
	}

	// ------------------------------------------------------------------------------------------
	/** Church of goddess Eris. Respawned adventurers wake up here. */
	private static void erisChurch(Build b) {
		int x1 = 16, z1 = 16, x2 = 40, z2 = 46;
		BlockState wall = Blocks.CALCITE.getDefaultState();
		BlockState trim = Blocks.QUARTZ_PILLAR.getDefaultState();
		b.fill(x1, G, z1, x2, G, z2, Blocks.SMOOTH_QUARTZ);
		b.clear(x1 + 1, F, z1 + 1, x2 - 1, F + 14, z2 - 1);
		b.walls(x1, F, z1, x2, F + 9, z2, wall);
		for (int z = z1; z <= z2; z += 5) {
			b.fill(x1, F, z, x1, F + 9, z, trim);
			b.fill(x2, F, z, x2, F + 9, z, trim);
			b.fill(x1 - 1, F, z, x1 - 1, F + 4, z, Blocks.STONE_BRICKS);
			b.fill(x2 + 1, F, z, x2 + 1, F + 4, z, Blocks.STONE_BRICKS);
			b.set(x1 - 1, F + 5, z, Build.stairs(Blocks.STONE_BRICK_STAIRS, Direction.EAST));
			b.set(x2 + 1, F + 5, z, Build.stairs(Blocks.STONE_BRICK_STAIRS, Direction.WEST));
		}
		// Tall stained glass windows
		for (int z = z1 + 2; z <= z2 - 2; z++) {
			if (Math.floorMod(z - z1, 5) == 0) continue;
			if (Math.floorMod(z - z1, 5) == 2 || Math.floorMod(z - z1, 5) == 3) {
				for (int y = F + 2; y <= F + 7; y++) {
					BlockState g = (y % 3 == 0) ? Blocks.YELLOW_STAINED_GLASS_PANE.getDefaultState() : Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState();
					b.set(x1, y, z, g);
					b.set(x2, y, z, g);
				}
			}
		}
		b.gableRoof(x1, z1, x2, z2, F + 10, Blocks.PRISMARINE_BRICK_STAIRS, Blocks.DARK_PRISMARINE.getDefaultState(), wall, false);
		// Rose window at the back
		for (int i = -4; i <= 4; i++)
			for (int j = -4; j <= 4; j++) {
				double d = Math.sqrt(i * i + j * j);
				if (d < 4.4) b.set(28 + i, F + 9 + j, z2, d < 1.5 ? Blocks.YELLOW_STAINED_GLASS.getDefaultState()
					: (d < 3.4 ? Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState() : Blocks.WHITE_STAINED_GLASS.getDefaultState()));
			}
		// Floor: blue carpet aisle
		for (int z = z1 + 1; z < z2; z++)
			for (int x = x1 + 1; x < x2; x++)
				b.set(x, G, z, Math.floorMod(x + z, 2) == 0 ? Blocks.SMOOTH_QUARTZ.getDefaultState() : Blocks.WHITE_CONCRETE.getDefaultState());
		for (int z = z1 + 1; z < z2 - 4; z++) {
			b.set(27, F, z, Blocks.LIGHT_BLUE_CARPET.getDefaultState());
			b.set(28, F, z, Blocks.LIGHT_BLUE_CARPET.getDefaultState());
			b.set(29, F, z, Blocks.LIGHT_BLUE_CARPET.getDefaultState());
		}
		// Pews facing the altar (south)
		for (int z = z1 + 7; z <= z2 - 9; z += 3) {
			for (int x = x1 + 2; x <= 25; x++) b.set(x, F, z, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
			for (int x = 31; x <= x2 - 2; x++) b.set(x, F, z, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH));
		}
		// Altar and statue of Eris
		b.fill(x1 + 4, F, z2 - 5, x2 - 4, F, z2 - 1, Blocks.SMOOTH_QUARTZ);
		for (int x = x1 + 4; x <= x2 - 4; x++) b.set(x, F, z2 - 6, Build.stairs(Blocks.QUARTZ_STAIRS, Direction.SOUTH));
		b.fill(26, F + 1, z2 - 4, 30, F + 1, z2 - 4, Blocks.QUARTZ_BLOCK);
		b.set(26, F + 2, z2 - 4, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true).with(CandleBlock.CANDLES, 4));
		b.set(30, F + 2, z2 - 4, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true).with(CandleBlock.CANDLES, 4));
		b.set(28, F + 2, z2 - 4, Blocks.GOLD_BLOCK.getDefaultState());
		// statue: robe, arms, head with silver hair
		b.fill(28, F + 1, z2 - 2, 28, F + 4, z2 - 2, Blocks.QUARTZ_PILLAR);
		b.fill(27, F + 3, z2 - 2, 29, F + 4, z2 - 2, Blocks.QUARTZ_BLOCK);
		b.set(28, F + 5, z2 - 2, Blocks.WHITE_CONCRETE.getDefaultState());
		b.set(28, F + 6, z2 - 2, Blocks.WHITE_WOOL.getDefaultState());
		b.set(26, F + 4, z2 - 2, Blocks.QUARTZ_SLAB.getDefaultState());
		b.set(30, F + 4, z2 - 2, Blocks.QUARTZ_SLAB.getDefaultState());
		b.set(27, F + 5, z2 - 2, Blocks.LIGHT_BLUE_WOOL.getDefaultState());
		b.set(29, F + 5, z2 - 2, Blocks.LIGHT_BLUE_WOOL.getDefaultState());
		for (int x = 22; x <= 34; x += 12) {
			chandelier(b, x, F + 9, 26, 2);
			chandelier(b, x, F + 9, 36, 2);
		}
		// Bell tower over the entrance
		int tx1 = 24, tz1 = 12, tx2 = 32, tz2 = 18;
		b.fill(tx1, G, tz1, tx2, G, tz2, Blocks.STONE_BRICKS);
		b.walls(tx1, F, tz1, tx2, F + 18, tz2, wall);
		b.clear(tx1 + 1, F, tz1 + 1, tx2 - 1, F + 18, tz2 - 1);
		for (int y = F; y <= F + 18; y++) {
			b.set(tx1, y, tz1, trim);
			b.set(tx2, y, tz1, trim);
			b.set(tx1, y, tz2, trim);
			b.set(tx2, y, tz2, trim);
		}
		for (int y = F + 14; y <= F + 16; y++) {
			for (int i = 26; i <= 30; i++) {
				b.set(i, y, tz1, Build.AIR);
				b.set(i, y, tz2, Build.AIR);
			}
			for (int i = 14; i <= 16; i++) {
				b.set(tx1, y, i, Build.AIR);
				b.set(tx2, y, i, Build.AIR);
			}
		}
		b.fill(tx1 + 1, F + 13, tz1 + 1, tx2 - 1, F + 13, tz2 - 1, Blocks.SPRUCE_PLANKS);
		b.set(28, F + 16, 15, Blocks.GOLD_BLOCK.getDefaultState());
		b.set(28, F + 17, 15, Blocks.CHAIN.getDefaultState());
		for (int i = 0; i < 9; i++) {
			int y = F + 19 + i;
			int inset = i / 2;
			b.walls(tx1 + inset, y, tz1 + inset, tx2 - inset, y, tz2 - inset, Blocks.DARK_PRISMARINE.getDefaultState());
		}
		b.set(28, F + 28, 15, Blocks.GOLD_BLOCK.getDefaultState());
		b.set(28, F + 29, 15, Blocks.END_ROD.getDefaultState());
		// Entrance through the tower
		b.clear(27, F, tz1, 29, F + 3, tz1);
		b.door(27, F, tz1, Blocks.BIRCH_DOOR, Direction.SOUTH, false);
		b.door(29, F, tz1, Blocks.BIRCH_DOOR, Direction.SOUTH, true);
		b.clear(27, F, z1, 29, F + 3, z1);
		b.clear(26, F, tz2, 30, F + 4, tz2);
		for (int x = 26; x <= 30; x++) b.set(x, G, tz1 - 1, Build.stairs(Blocks.QUARTZ_STAIRS, Direction.SOUTH));
	}

	// ------------------------------------------------------------------------------------------
	/** Church of the Axis Cult. Very blue. Very enthusiastic. Aqua approves. */
	private static void axisChurch(Build b) {
		int x1 = -30, z1 = 30, x2 = -16, z2 = 44;
		BlockState wall = Blocks.LIGHT_BLUE_TERRACOTTA.getDefaultState();
		b.fill(x1, G, z1, x2, G, z2, Blocks.PRISMARINE_BRICKS);
		b.clear(x1 + 1, F, z1 + 1, x2 - 1, F + 10, z2 - 1);
		timberWalls(b, x1, F, z1, x2, F + 6, z2, Blocks.WHITE_CONCRETE.getDefaultState(), wall);
		windows(b, x1, z1, x2, z2, F + 2, 3, Blocks.BLUE_STAINED_GLASS_PANE.getDefaultState());
		b.gableRoof(x1, z1, x2, z2, F + 7, Blocks.PRISMARINE_STAIRS, Blocks.PRISMARINE.getDefaultState(), wall, false);
		b.clear(-24, F, z1, -22, F + 2, z1);
		b.door(-23, F, z1, Blocks.WARPED_DOOR, Direction.SOUTH, false);
		// Holy fountain with a statue of Aqua
		b.ring(-23, F, 39, 3.2, 1.2, Blocks.PRISMARINE_WALL.getDefaultState());
		b.disk(-23, F, 39, 2.2, Blocks.WATER.getDefaultState());
		b.fill(-23, F, 39, -23, F + 1, 39, Blocks.LIGHT_BLUE_CONCRETE);
		b.set(-23, F + 2, 39, Blocks.WHITE_CONCRETE.getDefaultState());
		b.set(-23, F + 3, 39, Blocks.LIGHT_BLUE_WOOL.getDefaultState());
		b.set(-24, F + 2, 39, Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
		b.set(-22, F + 2, 39, Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
		b.set(-23, F + 4, 39, Blocks.WATER.getDefaultState());
		// Membership forms stacked everywhere
		for (int x = x1 + 2; x <= x2 - 2; x += 3) {
			b.set(x, F, z1 + 2, Blocks.LECTERN.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		}
		b.set(x1 + 1, F, z2 - 1, Blocks.BARREL.getDefaultState());
		b.set(x2 - 1, F, z2 - 1, Blocks.BARREL.getDefaultState());
		chandelier(b, -23, F + 6, 35, 1);
	}

	// ------------------------------------------------------------------------------------------
	/** The stable where broke newbie adventurers sleep in the hay. */
	private static void stable(Build b) {
		int x1 = -66, z1 = 46, x2 = -46, z2 = 64;
		BlockState wall = Blocks.SPRUCE_PLANKS.getDefaultState();
		b.fill(x1, G, z1, x2, G, z2, Blocks.COARSE_DIRT);
		b.clear(x1 + 1, F, z1 + 1, x2 - 1, F + 9, z2 - 1);
		timberWalls(b, x1, F, z1, x2, F + 5, z2, Blocks.STRIPPED_SPRUCE_LOG.getDefaultState(), wall);
		b.gableRoof(x1, z1, x2, z2, F + 6, Blocks.DARK_OAK_STAIRS, Build.slab(Blocks.DARK_OAK_SLAB, false), wall, true);
		// Big open entrance on the east side
		b.clear(x2, F, 52, x2, F + 3, 58);
		// Stalls
		for (int x = x1 + 4; x <= x2 - 4; x += 4) {
			b.fill(x, F, z1 + 1, x, F + 1, z1 + 5, Blocks.SPRUCE_FENCE);
			b.fill(x, F, z2 - 5, x, F + 1, z2 - 1, Blocks.SPRUCE_FENCE);
		}
		for (int x = x1 + 1; x < x2; x++) {
			if (Math.floorMod(x, 4) == 0) continue;
			b.set(x, F, z1 + 1, Blocks.HAY_BLOCK.getDefaultState());
			b.set(x, F, z2 - 1, Blocks.HAY_BLOCK.getDefaultState());
			if (Math.floorMod(x, 3) == 0) b.set(x, F + 1, z1 + 1, Blocks.HAY_BLOCK.getDefaultState());
		}
		// The famous "beginner bed" of hay
		b.fill(-60, F, 54, -58, F, 56, Blocks.HAY_BLOCK);
		b.set(-59, F + 1, 55, Blocks.WHITE_CARPET.getDefaultState());
		b.set(-56, F, 55, Blocks.CAULDRON.getDefaultState());
		b.set(-62, F, 55, Blocks.LANTERN.getDefaultState());
		chandelier(b, -56, F + 5, 55, 1);
	}

	// ------------------------------------------------------------------------------------------
	/** A big haunted-looking mansion the party may eventually call home. */
	private static void mansion(Build b) {
		int x1 = 38, z1 = -68, x2 = 66, z2 = -50;
		BlockState wall = Blocks.BRICKS.getDefaultState();
		BlockState frame = Blocks.STONE_BRICKS.getDefaultState();
		// Garden with a fence
		for (int x = x1 - 2; x <= x2 + 2; x++) {
			b.set(x, F, -42, Blocks.DARK_OAK_FENCE.getDefaultState());
		}
		for (int z = -48; z <= -42; z++) {
			b.set(x1 - 2, F, z, Blocks.DARK_OAK_FENCE.getDefaultState());
			b.set(x2 + 2, F, z, Blocks.DARK_OAK_FENCE.getDefaultState());
		}
		b.clear(50, F, -42, 54, F, -42);
		b.fill(50, G, -49, 54, G, -40, Blocks.GRAVEL);
		for (int x = x1; x <= x2; x++) {
			if (x >= 49 && x <= 55) continue;
			if (Math.floorMod(x, 3) == 0) b.set(x, F, -45, Blocks.ROSE_BUSH.getDefaultState().with(Properties.DOUBLE_BLOCK_HALF, net.minecraft.block.enums.DoubleBlockHalf.LOWER));
			if (Math.floorMod(x, 3) == 0) b.set(x, F + 1, -45, Blocks.ROSE_BUSH.getDefaultState().with(Properties.DOUBLE_BLOCK_HALF, net.minecraft.block.enums.DoubleBlockHalf.UPPER));
		}
		b.disk(42, G, -46, 2.5, Blocks.WATER.getDefaultState());
		b.set(42, G, -46, Blocks.WATER.getDefaultState());
		// Building
		b.fill(x1, G, z1, x2, G, z2, Blocks.STONE_BRICKS);
		b.clear(x1 + 1, F, z1 + 1, x2 - 1, F + 14, z2 - 1);
		b.walls(x1, F, z1, x2, F + 11, z2, wall);
		for (int x = x1; x <= x2; x += 4) b.fill(x, F, z2, x, F + 11, z2, frame);
		for (int x = x1; x <= x2; x += 4) b.fill(x, F, z1, x, F + 11, z1, frame);
		b.fill(x1 + 1, F + 5, z1 + 1, x2 - 1, F + 5, z2 - 1, Blocks.DARK_OAK_PLANKS);
		b.fill(x1, F + 5, z1, x2, F + 5, z1, frame);
		b.fill(x1, F + 5, z2, x2, F + 5, z2, frame);
		windows(b, x1, z1, x2, z2, F + 1, 3, Blocks.GLASS_PANE.getDefaultState());
		windows(b, x1, z1, x2, z2, F + 7, 3, Blocks.GLASS_PANE.getDefaultState());
		b.gableRoof(x1, z1, x2, z2, F + 12, Blocks.DARK_OAK_STAIRS, Build.slab(Blocks.DARK_OAK_SLAB, false), wall, true);
		// Chimney
		b.fill(x2 - 3, F + 10, z1 + 3, x2 - 2, F + 20, z1 + 4, Blocks.BRICKS);
		b.set(x2 - 3, F + 21, z1 + 3, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		// Entrance
		b.clear(51, F, z2, 53, F + 3, z2);
		b.door(51, F, z2, Blocks.DARK_OAK_DOOR, Direction.NORTH, false);
		b.door(53, F, z2, Blocks.DARK_OAK_DOOR, Direction.NORTH, true);
		b.set(52, F + 3, z2, frame);
		for (int x = 50; x <= 54; x++) b.set(x, G, z2 + 1, Build.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH));
		// Interior: fireplace, sofas, dining table, stairs
		for (int x = x1 + 1; x < x2; x++)
			for (int z = z1 + 1; z < z2; z++) b.set(x, G, z, Blocks.DARK_OAK_PLANKS.getDefaultState());
		b.fill(x2 - 4, F, z1 + 1, x2 - 1, F + 4, z1 + 1, Blocks.BRICKS);
		b.set(x2 - 3, F, z1 + 2, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		b.set(x2 - 2, F, z1 + 2, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		b.set(x2 - 3, F + 1, z1 + 2, Blocks.BRICK_WALL.getDefaultState());
		b.set(x2 - 2, F + 1, z1 + 2, Blocks.BRICK_WALL.getDefaultState());
		for (int x = x2 - 6; x <= x2 - 2; x++) b.set(x, F, z1 + 6, Build.stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH));
		b.fill(x2 - 5, F, z1 + 8, x2 - 3, F, z1 + 9, Blocks.RED_CARPET);
		for (int x = x1 + 4; x <= x1 + 12; x++) {
			b.set(x, F, z1 + 8, Blocks.DARK_OAK_FENCE.getDefaultState());
			b.set(x, F + 1, z1 + 8, Blocks.WHITE_CARPET.getDefaultState());
			b.set(x, F, z1 + 7, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
			b.set(x, F, z1 + 9, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH));
		}
		b.set(x1 + 6, F + 2, z1 + 8, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true).with(CandleBlock.CANDLES, 3));
		b.set(x1 + 10, F + 2, z1 + 8, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true).with(CandleBlock.CANDLES, 3));
		for (int i = 0; i < 5; i++) {
			b.set(x1 + 1, F + i, z2 - 2 - i, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
			b.set(x1 + 1, F + 5, z2 - 2 - i, Build.AIR);
			b.set(x1 + 2, F + 5, z2 - 2 - i, Build.AIR);
		}
		chandelier(b, 52, F + 4, -60, 1);
		chandelier(b, 44, F + 10, -60, 1);
		chandelier(b, 60, F + 10, -60, 1);
		for (int x = x1 + 3; x <= x2 - 3; x += 4) b.fill(x, F + 6, z1 + 1, x, F + 8, z1 + 1, Blocks.BOOKSHELF);
	}

	// ------------------------------------------------------------------------------------------
	private record Style(BlockState frame, BlockState fill, Block roof, BlockState ridge, BlockState pane) {
	}

	private static final Style[] STYLES = {
		new Style(Blocks.DARK_OAK_LOG.getDefaultState(), Blocks.CALCITE.getDefaultState(), Blocks.ACACIA_STAIRS, Build.slab(Blocks.ACACIA_SLAB, false), Blocks.GLASS_PANE.getDefaultState()),
		new Style(Blocks.SPRUCE_LOG.getDefaultState(), Blocks.WHITE_TERRACOTTA.getDefaultState(), Blocks.MANGROVE_STAIRS, Build.slab(Blocks.MANGROVE_SLAB, false), Blocks.GLASS_PANE.getDefaultState()),
		new Style(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState(), Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.BRICK_STAIRS, Build.slab(Blocks.BRICK_SLAB, false), Blocks.GLASS_PANE.getDefaultState()),
		new Style(Blocks.DARK_OAK_LOG.getDefaultState(), Blocks.BIRCH_PLANKS.getDefaultState(), Blocks.DEEPSLATE_TILE_STAIRS, Build.slab(Blocks.DEEPSLATE_TILE_SLAB, false), Blocks.GLASS_PANE.getDefaultState()),
		new Style(Blocks.SPRUCE_LOG.getDefaultState(), Blocks.CALCITE.getDefaultState(), Blocks.PRISMARINE_BRICK_STAIRS, Blocks.DARK_PRISMARINE.getDefaultState(), Blocks.GLASS_PANE.getDefaultState()),
		new Style(Blocks.STRIPPED_SPRUCE_LOG.getDefaultState(), Blocks.WHITE_TERRACOTTA.getDefaultState(), Blocks.ACACIA_STAIRS, Build.slab(Blocks.ACACIA_SLAB, false), Blocks.GLASS_PANE.getDefaultState()),
	};

	static boolean reserved(int x1, int z1, int x2, int z2) {
		for (int[] r : RESERVED) {
			if (x1 <= r[2] && x2 >= r[0] && z1 <= r[3] && z2 >= r[1]) return true;
		}
		// streets, plaza and the wall walk
		if (x1 <= 5 && x2 >= -5) return true;
		if (z1 <= 5 && z2 >= -5) return true;
		for (int x = x1; x <= x2; x += Math.max(1, x2 - x1)) {
			for (int z = z1; z <= z2; z += Math.max(1, z2 - z1)) {
				if (x * x + z * z < 19 * 19) return true;
			}
		}
		int lim = H - 6;
		return x1 < -lim || x2 > lim || z1 < -lim || z2 > lim;
	}

	private static void houses(Build b) {
		List<int[]> lots = new ArrayList<>();
		for (int lx = -lotStart(); lx <= H; lx += 14) {
			for (int lz = -lotStart(); lz <= H; lz += 14) {
				int w = 8 + (int) (Terrain.rand(lx, lz, 301) * 4);
				int d = 7 + (int) (Terrain.rand(lx, lz, 302) * 4);
				int x1 = lx + (int) (Terrain.rand(lx, lz, 303) * (12 - w));
				int z1 = lz + (int) (Terrain.rand(lx, lz, 304) * (12 - d));
				int x2 = x1 + w, z2 = z1 + d;
				if (reserved(x1 - 1, z1 - 1, x2 + 1, z2 + 1)) continue;
				lots.add(new int[]{x1, z1, x2, z2, lx, lz});
			}
		}
		for (int[] lot : lots) house(b, lot[0], lot[1], lot[2], lot[3], lot[4], lot[5]);
	}

	private static int lotStart() {
		return 70;
	}

	private static void house(Build b, int x1, int z1, int x2, int z2, int seedX, int seedZ) {
		Style st = STYLES[(int) (Terrain.rand(seedX, seedZ, 310) * STYLES.length)];
		boolean twoFloors = Terrain.rand(seedX, seedZ, 311) < 0.45;
		int top = twoFloors ? F + 7 : F + 3;
		b.fill(x1, G, z1, x2, G, z2, Blocks.COBBLESTONE);
		b.clear(x1 + 1, F, z1 + 1, x2 - 1, top + 8, z2 - 1);
		b.wallsMix(x1, F, z1, x2, F, z2, Blocks.COBBLESTONE.getDefaultState(), Blocks.MOSSY_COBBLESTONE.getDefaultState());
		timberWalls(b, x1, F + 1, z1, x2, top, z2, st.frame(), st.fill());
		if (twoFloors) {
			b.fill(x1 + 1, F + 4, z1 + 1, x2 - 1, F + 4, z2 - 1, Blocks.SPRUCE_PLANKS);
			for (int x = x1; x <= x2; x++) {
				b.set(x, F + 4, z1, st.frame().contains(Properties.AXIS) ? st.frame().with(Properties.AXIS, Direction.Axis.X) : st.frame());
				b.set(x, F + 4, z2, st.frame().contains(Properties.AXIS) ? st.frame().with(Properties.AXIS, Direction.Axis.X) : st.frame());
			}
			windows(b, x1, z1, x2, z2, F + 5, 2, st.pane());
		}
		windows(b, x1, z1, x2, z2, F + 1, 2, st.pane());
		boolean alongX = (x2 - x1) >= (z2 - z1);
		b.gableRoof(x1, z1, x2, z2, top + 1, st.roof(), st.ridge(), st.fill(), alongX);
		for (int x = x1 + 1; x < x2; x++)
			for (int z = z1 + 1; z < z2; z++)
				b.set(x, G, z, Blocks.OAK_PLANKS.getDefaultState());
		// Door facing the closest main street
		int cx = (x1 + x2) / 2, cz = (z1 + z2) / 2;
		Direction face;
		if (Math.abs(cx) < Math.abs(cz)) face = cx > 0 ? Direction.WEST : Direction.EAST;
		else face = cz > 0 ? Direction.NORTH : Direction.SOUTH;
		int dx, dz;
		switch (face) {
			case WEST -> { dx = x1; dz = cz; }
			case EAST -> { dx = x2; dz = cz; }
			case NORTH -> { dx = cx; dz = z1; }
			default -> { dx = cx; dz = z2; }
		}
		b.clear(dx, F, dz, dx, F + 2, dz);
		b.door(dx, F, dz, Blocks.SPRUCE_DOOR, face.getOpposite(), false);
		b.set(dx + face.getOffsetX(), F + 2, dz + face.getOffsetZ(), Build.lantern(false, false));
		b.set(dx + face.getOffsetX(), F + 1, dz + face.getOffsetZ(), Blocks.SPRUCE_FENCE.getDefaultState());
		b.set(dx + face.getOffsetX(), F, dz + face.getOffsetZ(), Blocks.SPRUCE_FENCE.getDefaultState());
		int ox = face.rotateYClockwise().getOffsetX() * 2, oz = face.rotateYClockwise().getOffsetZ() * 2;
		b.set(dx + face.getOffsetX() + ox, F, dz + face.getOffsetZ() + oz, Blocks.FLOWER_POT.getDefaultState());
		b.set(dx + face.getOffsetX() - ox, F, dz + face.getOffsetZ() - oz, Blocks.BARREL.getDefaultState());
		// Path from the door towards the street
		for (int i = 1; i <= 4; i++) {
			int px = dx + face.getOffsetX() * i, pz = dz + face.getOffsetZ() * i;
			if (!b.has(px, G, pz)) b.set(px, G, pz, Blocks.DIRT_PATH.getDefaultState());
		}
		// Furniture
		double r = Terrain.rand(seedX, seedZ, 320);
		b.set(x1 + 1, F, z1 + 1, Blocks.CRAFTING_TABLE.getDefaultState());
		b.set(x2 - 1, F, z1 + 1, Blocks.FURNACE.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		b.set(x2 - 1, F, z2 - 1, r < 0.5 ? Blocks.BOOKSHELF.getDefaultState() : Blocks.BARREL.getDefaultState());
		b.set(x1 + 1, F, z2 - 1, Blocks.POTTED_POPPY.getDefaultState());
		int tx = cx, tz = cz;
		if (tx == dx && tz == dz) tx += 1;
		b.set(tx, F, tz, Blocks.OAK_FENCE.getDefaultState());
		b.set(tx, F + 1, tz, Blocks.WHITE_CARPET.getDefaultState());
		b.set(tx + 1, F, tz, Build.stairs(Blocks.OAK_STAIRS, Direction.EAST));
		b.set(tx - 1, F, tz, Build.stairs(Blocks.OAK_STAIRS, Direction.WEST));
		b.set(cx, top, cz, Build.lantern(false, true));
		if (twoFloors) {
			b.set(x1 + 1, F + 5, z1 + 1, Blocks.RED_WOOL.getDefaultState());
			b.set(x1 + 2, F + 5, z1 + 1, Blocks.RED_CARPET.getDefaultState());
			b.set(x2 - 1, F + 5, z2 - 1, Blocks.BOOKSHELF.getDefaultState());
			// ladder hole
			b.set(x2 - 1, F + 4, z1 + 2, Build.AIR);
			for (int y = F; y <= F + 4; y++)
				b.set(x2 - 1, y, z1 + 2, Blocks.LADDER.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
		}
		// Chimney on some houses
		if (r > 0.6) {
			int chx = x1 + 1, chz = z1 + 1;
			for (int y = F; y <= top + 7; y++) b.set(chx, y, chz, Blocks.BRICKS.getDefaultState());
			b.set(chx, F, chz, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, false));
		}
		// Small garden in front
		if (r < 0.4) {
			int gx = dx + face.getOffsetX() * 2 + face.rotateYCounterclockwise().getOffsetX() * 3;
			int gz = dz + face.getOffsetZ() * 2 + face.rotateYCounterclockwise().getOffsetZ() * 3;
			b.set(gx, G, gz, Blocks.FARMLAND.getDefaultState().with(Properties.MOISTURE, 7));
			b.set(gx, F, gz, Blocks.CARROTS.getDefaultState().with(CropBlock.AGE, 7));
			b.set(gx + 1, G, gz, Blocks.WATER.getDefaultState());
		}
	}

	// ------------------------------------------------------------------------------------------
	private static void lamps(Build b) {
		for (int i = -66; i <= 66; i += 11) {
			if (Math.abs(i) < 16) continue;
			post(b, 4, i);
			post(b, -4, i);
			post(b, i, 4);
			post(b, i, -4);
		}
		for (int k = 0; k < 12; k++) {
			double a = k * Math.PI / 6;
			post(b, (int) Math.round(Math.cos(a) * 16), (int) Math.round(Math.sin(a) * 16));
		}
	}

	private static void post(Build b, int x, int z) {
		BlockState cur = b.get(x, F, z);
		if (cur != null && !cur.isAir()) return;
		b.set(x, F, z, Blocks.DARK_OAK_FENCE.getDefaultState());
		b.set(x, F + 1, z, Blocks.DARK_OAK_FENCE.getDefaultState());
		b.set(x, F + 2, z, Blocks.DARK_OAK_FENCE.getDefaultState());
		b.set(x, F + 3, z, Build.lantern(false, false));
	}
}
