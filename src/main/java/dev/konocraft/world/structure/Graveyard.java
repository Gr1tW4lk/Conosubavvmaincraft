package dev.konocraft.world.structure;

import dev.konocraft.world.Build;
import dev.konocraft.world.Regions;
import dev.konocraft.world.Terrain;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.util.math.Direction;

/**
 * Axel's public cemetery north-west of the walls: rows of graves, an iron fence, a small crypt.
 * At night the undead like to get up here, and a certain lich shopkeeper comes to send lost souls on.
 */
public final class Graveyard {
	public static final int X1 = Regions.GRAVE_X - 17, X2 = Regions.GRAVE_X + 17;
	public static final int Z1 = Regions.GRAVE_Z - 14, Z2 = Regions.GRAVE_Z + 14;

	private Graveyard() {
	}

	public static int floor() {
		return Terrain.heightAt(Regions.GRAVE_X, Regions.GRAVE_Z);
	}

	public static void build(Build b) {
		int y = floor();
		BlockState soil = Blocks.COARSE_DIRT.getDefaultState();
		b.foundation(X1 - 1, Z1 - 1, X2 + 1, Z2 + 1, y + 1, Blocks.DIRT.getDefaultState());
		b.clearAbove(X1 - 1, Z1 - 1, X2 + 1, Z2 + 1, y + 1);
		for (int x = X1; x <= X2; x++) {
			for (int z = Z1; z <= Z2; z++) {
				double r = Terrain.rand(x, z, 311);
				b.set(x, y, z, r < 0.55 ? Blocks.GRASS_BLOCK.getDefaultState() : r < 0.8 ? Blocks.PODZOL.getDefaultState() : soil);
			}
		}
		// Fence with stone posts and soul lanterns; the gate faces the road on the east side
		for (int x = X1; x <= X2; x++) {
			fence(b, x, y, Z1, (x - X1) % 4 == 0);
			fence(b, x, y, Z2, (x - X1) % 4 == 0);
		}
		for (int z = Z1; z <= Z2; z++) {
			fence(b, X1, y, z, (z - Z1) % 4 == 0);
			if (Math.abs(z - Regions.GRAVE_Z) > 1) fence(b, X2, y, z, (z - Z1) % 4 == 0);
		}
		for (int z = Regions.GRAVE_Z - 1; z <= Regions.GRAVE_Z + 1; z++) {
			b.set(X2, y, z, Blocks.GRAVEL.getDefaultState());
			b.set(X2, y + 1, z, Build.AIR);
		}
		b.set(X2, y + 3, Regions.GRAVE_Z - 2, Build.lantern(true, false));
		b.set(X2, y + 3, Regions.GRAVE_Z + 2, Build.lantern(true, false));
		// Central path
		for (int x = X1 + 10; x <= X2; x++) {
			for (int z = Regions.GRAVE_Z - 1; z <= Regions.GRAVE_Z + 1; z++) {
				b.set(x, y, z, Terrain.rand(x, z, 312) < 0.7 ? Blocks.GRAVEL.getDefaultState() : Blocks.COBBLESTONE.getDefaultState());
			}
		}
		// Rows of graves
		for (int row = 0; row < 4; row++) {
			int gz = row < 2 ? Z1 + 3 + row * 5 : Z2 - 3 - (row - 2) * 5;
			Direction facing = row < 2 ? Direction.SOUTH : Direction.NORTH;
			for (int gx = X1 + 12; gx <= X2 - 3; gx += 3) {
				grave(b, gx, y, gz, facing);
			}
		}
		crypt(b, y);
		// Old dead tree, cobwebs and a bench for night visitors
		Terrain.deadTree(X1 + 13, y + 1, Regions.GRAVE_Z, 6, X1 + 13, Regions.GRAVE_Z, (x, yy, z, s) -> b.set(x, yy, z, s));
		b.set(X1 + 14, y + 4, Regions.GRAVE_Z + 1, Blocks.COBWEB.getDefaultState());
		b.chair(X2 - 4, y + 1, Regions.GRAVE_Z + 3, Blocks.DARK_OAK_STAIRS, Direction.NORTH);
		b.chair(X2 - 5, y + 1, Regions.GRAVE_Z + 3, Blocks.DARK_OAK_STAIRS, Direction.NORTH);
	}

	private static void fence(Build b, int x, int y, int z, boolean post) {
		if (post) {
			b.set(x, y + 1, z, Blocks.STONE_BRICKS.getDefaultState());
			b.set(x, y + 2, z, Blocks.STONE_BRICK_WALL.getDefaultState());
		} else {
			b.set(x, y + 1, z, Blocks.IRON_BARS.getDefaultState());
			b.set(x, y + 2, z, Blocks.IRON_BARS.getDefaultState());
		}
	}

	private static void grave(Build b, int x, int y, int z, Direction facing) {
		double r = Terrain.rand(x, z, 313);
		BlockState stone = r < 0.3 ? Blocks.MOSSY_COBBLESTONE_WALL.getDefaultState()
			: r < 0.6 ? Blocks.ANDESITE_WALL.getDefaultState()
			: r < 0.85 ? Blocks.STONE_BRICK_WALL.getDefaultState() : Blocks.CHISELED_STONE_BRICKS.getDefaultState();
		int back = facing == Direction.SOUTH ? -1 : 1;
		b.set(x, y + 1, z + back, stone);
		b.set(x, y, z, Blocks.PODZOL.getDefaultState());
		b.set(x, y, z - back, Blocks.COARSE_DIRT.getDefaultState());
		double d = Terrain.rand(x, z, 314);
		if (d < 0.25) {
			b.set(x, y + 1, z, Blocks.CANDLE.getDefaultState().with(CandleBlock.CANDLES, 2));
		} else if (d < 0.45) {
			b.set(x, y + 1, z, Blocks.POTTED_WITHER_ROSE.getDefaultState());
		} else if (d < 0.6) {
			b.set(x, y + 1, z, Blocks.DEAD_BUSH.getDefaultState());
		}
	}

	private static void crypt(Build b, int y) {
		int x1 = X1 + 2, x2 = X1 + 9, z1 = Regions.GRAVE_Z - 4, z2 = Regions.GRAVE_Z + 4;
		b.fill(x1, y, z1, x2, y, z2, Blocks.POLISHED_DEEPSLATE.getDefaultState());
		b.walls(x1, y + 1, z1, x2, y + 4, z2, Blocks.STONE_BRICKS.getDefaultState());
		b.wallsMix(x1, y + 1, z1, x2, y + 2, z2, Blocks.MOSSY_STONE_BRICKS.getDefaultState(), Blocks.CRACKED_STONE_BRICKS.getDefaultState(), Blocks.STONE_BRICKS.getDefaultState());
		b.clear(x1 + 1, y + 1, z1 + 1, x2 - 1, y + 4, z2 - 1);
		b.gableRoof(x1 - 1, z1 - 1, x2 + 1, z2 + 1, y + 5, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES.getDefaultState(), Blocks.STONE_BRICKS.getDefaultState(), true);
		// Entrance with an iron door facing the graves
		b.door(x2, y + 1, Regions.GRAVE_Z, Blocks.IRON_DOOR, Direction.WEST, false);
		b.set(x2 + 1, y + 3, Regions.GRAVE_Z, Build.lantern(true, false));
		// Sarcophagus and candles inside
		b.fill(x1 + 2, y + 1, Regions.GRAVE_Z - 1, x1 + 4, y + 1, Regions.GRAVE_Z, Blocks.CHISELED_DEEPSLATE.getDefaultState());
		b.set(x1 + 1, y + 1, z1 + 1, Blocks.CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true));
		b.set(x1 + 1, y + 1, z2 - 1, Blocks.CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true));
		b.set(x1 + 3, y + 4, Regions.GRAVE_Z, Build.lantern(true, true));
		b.set(x1 + 1, y + 3, Regions.GRAVE_Z + 2, Blocks.COBWEB.getDefaultState());
	}
}
