package dev.konocraft.world.structure;

import dev.konocraft.world.Build;
import dev.konocraft.world.Regions;
import dev.konocraft.world.Terrain;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.CandleBlock;
import net.minecraft.util.math.Direction;

/**
 * The abandoned castle on the hill, home of the Dullahan Beldia. Megumin blows a piece of it up
 * every single day, which is visible in the craters all over its south-western side.
 */
public final class BeldiaCastle {
	private BeldiaCastle() {
	}

	static final int CX = Regions.CASTLE_X, CZ = Regions.CASTLE_Z;
	static final int G = Regions.CASTLE_Y, F = Regions.CASTLE_Y + 1;
	public static final int HALF = 32;

	// Keep / throne room bounds
	public static final int KX1 = CX - 16, KX2 = CX + 16, KZ1 = CZ - 26, KZ2 = CZ + 4;

	private static final BlockState[] STONE = {
		Blocks.DEEPSLATE_BRICKS.getDefaultState(), Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState(),
		Blocks.COBBLED_DEEPSLATE.getDefaultState(), Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(),
		Blocks.MOSSY_COBBLESTONE.getDefaultState()
	};

	public static void build(Build b) {
		// Courtyard ground
		for (int x = CX - HALF; x <= CX + HALF; x++) {
			for (int z = CZ - HALF; z <= CZ + HALF; z++) {
				double r = Terrain.rand(x, z, 401);
				BlockState s = r < 0.5 ? Blocks.COARSE_DIRT.getDefaultState() : r < 0.75 ? Blocks.GRAVEL.getDefaultState()
					: r < 0.9 ? Blocks.COBBLED_DEEPSLATE.getDefaultState() : Blocks.MOSS_BLOCK.getDefaultState();
				b.set(x, G, z, s);
			}
		}
		b.clearAbove(CX - HALF, CZ - HALF, CX + HALF, CZ + HALF, F);
		curtainWall(b);
		towers(b);
		gatehouse(b);
		keep(b);
		courtyard(b);
		explosionDamage(b);
	}

	private static void curtainWall(Build b) {
		int top = F + 12;
		for (int i = -HALF; i <= HALF; i++) {
			for (int t = 0; t < 3; t++) {
				int c = HALF - t;
				b.fillMix(CX + i, G - 3, CZ - c, CX + i, top, CZ - c, STONE);
				b.fillMix(CX + i, G - 3, CZ + c, CX + i, top, CZ + c, STONE);
				b.fillMix(CX - c, G - 3, CZ + i, CX - c, top, CZ + i, STONE);
				b.fillMix(CX + c, G - 3, CZ + i, CX + c, top, CZ + i, STONE);
			}
			if (Math.floorMod(i, 2) == 0) {
				b.set(CX + i, top + 1, CZ - HALF, STONE[0]);
				b.set(CX + i, top + 1, CZ + HALF, STONE[0]);
				b.set(CX - HALF, top + 1, CZ + i, STONE[0]);
				b.set(CX + HALF, top + 1, CZ + i, STONE[0]);
			}
		}
		b.foundation(CX - HALF - 1, CZ - HALF - 1, CX + HALF + 1, CZ + HALF + 1, G - 2, Blocks.COBBLED_DEEPSLATE.getDefaultState());
	}

	private static void towers(Build b) {
		int top = F + 20;
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				int tx = CX + sx * (HALF - 1), tz = CZ + sz * (HALF - 1);
				for (int y = G - 3; y <= top; y++) {
					int ir = 7;
					for (int dx = -ir; dx <= ir; dx++)
						for (int dz = -ir; dz <= ir; dz++) {
							double d = Math.sqrt(dx * dx + dz * dz);
							if (d <= 6.5 && d > 5.2) b.set(tx + dx, y, tz + dz, Build.pick(tx + dx + y * 3L, tz + dz, STONE));
							else if (d <= 5.2 && y >= F) b.set(tx + dx, y, tz + dz, Build.AIR);
						}
				}
				for (int k = 0; k < 16; k += 2) {
					double a = k * Math.PI / 8;
					b.set(tx + (int) Math.round(Math.cos(a) * 6), top + 1, tz + (int) Math.round(Math.sin(a) * 6), STONE[0]);
				}
				b.disk(tx, F + 8, tz, 5.2, Blocks.DARK_OAK_PLANKS.getDefaultState());
				b.set(tx, F + 8, tz, Build.AIR);
				b.set(tx, F + 7, tz, Build.lantern(true, true));
				// arrow slits
				for (int k = 0; k < 4; k++) {
					double a = k * Math.PI / 2 + Math.PI / 4;
					int ax = tx + (int) Math.round(Math.cos(a) * 6), az = tz + (int) Math.round(Math.sin(a) * 6);
					b.set(ax, F + 4, az, Build.AIR);
					b.set(ax, F + 5, az, Build.AIR);
					b.set(ax, F + 12, az, Build.AIR);
				}
				// Door from the courtyard
				int dx = tx - sx * 5, dz = tz - sz * 5;
				b.clear(dx - sx, F, dz, dx, F + 2, dz);
			}
		}
	}

	private static void gatehouse(Build b) {
		int gz = CZ + HALF;
		// Thick gate walls with an opening
		for (int x = CX - 7; x <= CX + 7; x++) {
			for (int z = gz - 4; z <= gz + 3; z++) {
				for (int y = G; y <= F + 16; y++) b.set(x, y, z, Build.pick(x * 3L + y, z, STONE));
			}
		}
		b.clear(CX - 3, F, gz - 4, CX + 3, F + 7, gz + 3);
		for (int x = CX - 3; x <= CX + 3; x++) {
			b.set(x, F + 7, gz + 1, Blocks.IRON_BARS.getDefaultState());
			b.set(x, F + 6, gz + 1, Math.floorMod(x, 2) == 0 ? Blocks.IRON_BARS.getDefaultState() : Build.AIR);
			b.set(x, G, gz - 4, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
			for (int z = gz - 3; z <= gz + 3; z++) b.set(x, G, z, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
		}
		for (int s = -1; s <= 1; s += 2) {
			b.set(CX + s * 5, F + 3, gz + 4, Blocks.SOUL_LANTERN.getDefaultState());
			b.set(CX + s * 5, F + 2, gz + 4, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
			b.set(CX + s * 5, F + 1, gz + 4, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
			b.set(CX + s * 5, F, gz + 4, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
		}
		for (int x = CX - 7; x <= CX + 7; x += 2) b.set(x, F + 17, gz + 3, STONE[0]);
		// Path from the gate down the hill
		for (int z = gz + 4; z <= gz + 22; z++) {
			for (int x = CX - 2; x <= CX + 2; x++) {
				int h = Terrain.heightAt(x, z);
				b.set(x, h, z, Terrain.rand(x, z, 402) < 0.7 ? Blocks.COBBLED_DEEPSLATE.getDefaultState() : Blocks.GRAVEL.getDefaultState());
			}
		}
	}

	private static void keep(Build b) {
		int top = F + 15;
		b.fill(KX1, G, KZ1, KX2, G, KZ2, Blocks.POLISHED_BLACKSTONE.getDefaultState());
		b.clear(KX1 + 1, F, KZ1 + 1, KX2 - 1, top + 6, KZ2 - 1);
		b.wallsMix(KX1, F, KZ1, KX2, top, KZ2, STONE);
		// buttresses
		for (int x = KX1; x <= KX2; x += 6) {
			b.fill(x, F, KZ2 + 1, x, F + 8, KZ2 + 1, STONE[0]);
			b.fill(x, F, KZ1 - 1, x, F + 8, KZ1 - 1, STONE[0]);
		}
		// Battlements on top
		for (int x = KX1; x <= KX2; x++) {
			if (Math.floorMod(x, 2) == 0) {
				b.set(x, top + 1, KZ1, STONE[0]);
				b.set(x, top + 1, KZ2, STONE[0]);
			}
		}
		for (int z = KZ1; z <= KZ2; z++) {
			if (Math.floorMod(z, 2) == 0) {
				b.set(KX1, top + 1, z, STONE[0]);
				b.set(KX2, top + 1, z, STONE[0]);
			}
		}
		// Roof (dark, heavy) - partially blown away later
		for (int x = KX1 + 1; x < KX2; x++)
			for (int z = KZ1 + 1; z < KZ2; z++)
				b.set(x, top, z, Blocks.DEEPSLATE_TILES.getDefaultState());
		// Gothic windows with red glass
		for (int z = KZ1 + 3; z <= KZ2 - 3; z += 5) {
			for (int y = F + 4; y <= F + 9; y++) {
				b.set(KX1, y, z, Blocks.RED_STAINED_GLASS_PANE.getDefaultState());
				b.set(KX2, y, z, Blocks.RED_STAINED_GLASS_PANE.getDefaultState());
			}
		}
		// Entrance
		b.clear(CX - 2, F, KZ2, CX + 2, F + 5, KZ2);
		b.set(CX - 2, F + 5, KZ2, Build.stairsTop(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST));
		b.set(CX + 2, F + 5, KZ2, Build.stairsTop(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST));
		// Pillars
		for (int z = KZ1 + 4; z <= KZ2 - 4; z += 6) {
			for (int s = -1; s <= 1; s += 2) {
				int px = CX + s * 8;
				b.fill(px, F, z, px, top - 1, z, Blocks.POLISHED_BASALT);
				b.set(px, F + 6, z + 1, Build.lantern(true, false));
				b.set(px, F + 5, z + 1, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
			}
		}
		// Red carpet to the throne
		for (int z = KZ1 + 6; z < KZ2; z++) {
			for (int x = CX - 1; x <= CX + 1; x++) b.set(x, F, z, Blocks.RED_CARPET.getDefaultState());
		}
		// Dais and throne
		for (int x = CX - 6; x <= CX + 6; x++) {
			b.set(x, F, KZ1 + 5, Build.stairs(Blocks.POLISHED_BLACKSTONE_STAIRS, Direction.NORTH));
			for (int z = KZ1 + 1; z <= KZ1 + 4; z++) b.set(x, F, z, Blocks.POLISHED_BLACKSTONE.getDefaultState());
		}
		b.set(CX, F + 1, KZ1 + 2, Build.stairs(Blocks.BLACKSTONE_STAIRS, Direction.NORTH));
		b.set(CX - 1, F + 1, KZ1 + 2, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
		b.set(CX + 1, F + 1, KZ1 + 2, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
		b.fill(CX - 1, F + 1, KZ1 + 1, CX + 1, F + 4, KZ1 + 1, Blocks.OBSIDIAN);
		b.set(CX, F + 5, KZ1 + 1, Blocks.CRYING_OBSIDIAN.getDefaultState());
		// Hanging red cloth behind the throne
		for (int x = CX - 4; x <= CX + 4; x++) {
			if (Math.abs(x - CX) <= 1) continue;
			for (int y = F + 3; y <= F + 10; y++) b.set(x, y, KZ1 + 1, Blocks.RED_WOOL.getDefaultState());
		}
		// Soul lights
		for (int x = CX - 12; x <= CX + 12; x += 8) {
			for (int z = KZ1 + 6; z <= KZ2 - 4; z += 10) {
				for (int i = 0; i < 4; i++) b.set(x, top - 1 - i, z, Blocks.CHAIN.getDefaultState());
				b.set(x, top - 5, z, Build.lantern(true, true));
			}
		}
		// Cobwebs in corners
		for (int[] c : new int[][]{{KX1 + 1, KZ1 + 1}, {KX2 - 1, KZ1 + 1}, {KX1 + 1, KZ2 - 1}, {KX2 - 1, KZ2 - 1}}) {
			b.set(c[0], top - 1, c[1], Blocks.COBWEB.getDefaultState());
			b.set(c[0], top - 2, c[1], Blocks.COBWEB.getDefaultState());
			b.set(c[0], F, c[1], Blocks.COBWEB.getDefaultState());
		}
		b.set(CX - 6, F + 1, KZ1 + 3, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true).with(CandleBlock.CANDLES, 4));
		b.set(CX + 6, F + 1, KZ1 + 3, Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true).with(CandleBlock.CANDLES, 4));
	}

	private static void courtyard(Build b) {
		// Graves
		for (int i = 0; i < 9; i++) {
			int x = CX - 26 + (i % 3) * 4, z = CZ + 10 + (i / 3) * 5;
			b.set(x, F, z, Blocks.MOSSY_COBBLESTONE_WALL.getDefaultState());
			b.set(x, F + 1, z, Blocks.MOSSY_COBBLESTONE_WALL.getDefaultState());
			b.set(x - 1, F + 1, z, Blocks.STONE_BUTTON.getDefaultState());
			b.set(x, F, z + 1, Blocks.COARSE_DIRT.getDefaultState());
			b.set(x, G, z + 1, Blocks.PODZOL.getDefaultState());
		}
		// Dead trees inside the walls
		Terrain.deadTree(CX + 22, F, CZ + 14, 6, 1, 2, b::set);
		Terrain.deadTree(CX - 22, F, CZ - 18, 7, 3, 4, b::set);
		Terrain.deadTree(CX + 24, F, CZ - 14, 5, 5, 6, b::set);
		// Broken carts and soul fires
		b.set(CX + 18, F, CZ + 22, Blocks.SOUL_CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		b.set(CX - 18, F, CZ + 24, Blocks.SOUL_CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		b.fill(CX + 10, F, CZ + 20, CX + 12, F, CZ + 21, Blocks.DARK_OAK_PLANKS);
		b.set(CX + 9, F, CZ + 20, Blocks.DARK_OAK_TRAPDOOR.getDefaultState());
		b.set(CX + 13, F, CZ + 21, Blocks.DARK_OAK_FENCE.getDefaultState());
		// Path from gate to keep
		for (int z = KZ2 + 1; z <= CZ + HALF - 4; z++)
			for (int x = CX - 2; x <= CX + 2; x++)
				b.set(x, G, z, Terrain.rand(x, z, 403) < 0.8 ? Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState() : Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState());
	}

	/** Megumin's daily explosions left craters on the side facing Axel. */
	private static void explosionDamage(Build b) {
		BlockState scorch = Blocks.BLACKSTONE.getDefaultState();
		b.blast(CX - HALF, F + 10, CZ + HALF, 7.5, scorch);
		b.blast(CX - HALF, F + 6, CZ + 8, 5.0, scorch);
		b.blast(CX - 10, F + 13, CZ + HALF, 4.5, scorch);
		b.blast(CX - HALF + 1, F + 18, CZ - HALF + 1, 5.5, scorch);
		b.blast(KX1 + 4, F + 16, KZ2 - 6, 5.0, scorch);
		b.blast(KX1 + 12, F + 16, KZ1 + 10, 4.0, scorch);
		b.blast(CX + HALF - 1, F + 20, CZ + HALF - 1, 4.0, scorch);
		// Scorched ground outside the walls
		for (int i = 0; i < 40; i++) {
			int x = CX - HALF - 10 + (int) (Terrain.rand(i, 7, 404) * 24);
			int z = CZ + HALF - 6 + (int) (Terrain.rand(i, 8, 404) * 20);
			int h = Terrain.heightAt(x, z);
			b.set(x, h, z, Terrain.rand(i, 9, 404) < 0.5 ? Blocks.BLACKSTONE.getDefaultState() : Blocks.COAL_BLOCK.getDefaultState());
		}
	}
}
