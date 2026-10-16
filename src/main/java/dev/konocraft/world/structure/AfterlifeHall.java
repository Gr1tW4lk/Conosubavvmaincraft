package dev.konocraft.world.structure;

import dev.konocraft.world.Build;
import dev.konocraft.world.Regions;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.util.math.Direction;

/** The white hall between worlds where goddess Eris greets the recently deceased. */
public final class AfterlifeHall {
	private AfterlifeHall() {
	}

	public static void build(Build b) {
		int cx = Regions.AFTERLIFE_X, y = Regions.AFTERLIFE_Y, cz = Regions.AFTERLIFE_Z;
		int r = 15;
		BlockState white = Blocks.WHITE_CONCRETE.getDefaultState();
		BlockState quartz = Blocks.QUARTZ_BLOCK.getDefaultState();
		BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState();
		BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState();
		BlockState lamp = Blocks.SEA_LANTERN.getDefaultState();

		// Floor with a golden circle and star-like rays
		b.fill(cx - r, y - 1, cz - r, cx + r, y - 1, cz + r, Blocks.SMOOTH_QUARTZ);
		b.fill(cx - r, y, cz - r, cx + r, y, cz + r, white);
		b.ring(cx, y, cz, 9, 1, Blocks.GOLD_BLOCK.getDefaultState());
		b.ring(cx, y, cz, 5, 1, Blocks.SMOOTH_QUARTZ.getDefaultState());
		b.disk(cx, y, cz, 2, lamp);
		for (int i = -r + 1; i <= r - 1; i++) {
			if (Math.abs(i) > 9) {
				b.set(cx + i, y, cz, Blocks.SMOOTH_QUARTZ.getDefaultState());
				b.set(cx, y, cz + i, Blocks.SMOOTH_QUARTZ.getDefaultState());
			}
		}
		// Walls: quartz pillars with tall sky windows in between
		for (int yy = y + 1; yy <= y + 13; yy++) {
			for (int i = -r; i <= r; i++) {
				boolean post = Math.floorMod(i, 5) == 0 || Math.abs(i) == r;
				BlockState s = post ? pillar : (yy <= y + 1 || yy >= y + 12 ? quartz : glass);
				b.set(cx + i, yy, cz - r, s);
				b.set(cx + i, yy, cz + r, s);
				b.set(cx - r, yy, cz + i, s);
				b.set(cx + r, yy, cz + i, s);
			}
		}
		// Ceiling with glowing grid
		for (int x = cx - r; x <= cx + r; x++) {
			for (int z = cz - r; z <= cz + r; z++) {
				boolean light = Math.floorMod(x - cx, 4) == 0 && Math.floorMod(z - cz, 4) == 0;
				b.set(x, y + 14, z, light ? lamp : white);
			}
		}
		b.clear(cx - r + 1, y + 1, cz - r + 1, cx + r - 1, y + 13, cz + r - 1);

		// Eris' dais at the north end
		int dz = cz - r + 1;
		b.fill(cx - 6, y + 1, dz, cx + 6, y + 1, dz + 5, quartz);
		for (int x = cx - 6; x <= cx + 6; x++) b.set(x, y + 1, dz + 6, Build.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH));
		b.fill(cx - 3, y + 2, dz, cx + 3, y + 2, dz + 3, Blocks.SMOOTH_QUARTZ);
		for (int x = cx - 3; x <= cx + 3; x++) b.set(x, y + 2, dz + 4, Build.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH));
		// Throne
		b.set(cx, y + 3, dz + 1, Build.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH));
		b.set(cx - 1, y + 3, dz + 1, Blocks.QUARTZ_PILLAR.getDefaultState());
		b.set(cx + 1, y + 3, dz + 1, Blocks.QUARTZ_PILLAR.getDefaultState());
		b.fill(cx - 1, y + 3, dz, cx + 1, y + 6, dz, Blocks.QUARTZ_BLOCK);
		b.set(cx, y + 7, dz, Blocks.GOLD_BLOCK.getDefaultState());
		b.set(cx - 1, y + 4, dz + 1, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
		b.set(cx + 1, y + 4, dz + 1, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
		// Round "window" behind the throne
		for (int i = -4; i <= 4; i++) {
			for (int j = 0; j <= 8; j++) {
				double d = Math.sqrt(i * i + (j - 4) * (j - 4));
				if (d <= 4.3 && d >= 3.3) b.set(cx + i, y + 5 + j, cz - r, Blocks.GOLD_BLOCK.getDefaultState());
				else if (d < 3.3) b.set(cx + i, y + 5 + j, cz - r, Blocks.WHITE_STAINED_GLASS.getDefaultState());
			}
		}
		// Light columns and candles
		for (int[] p : new int[][]{{-10, -10}, {10, -10}, {-10, 10}, {10, 10}}) {
			b.set(cx + p[0], y + 1, cz + p[1], Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
			for (int yy = y + 2; yy <= y + 4; yy++) b.set(cx + p[0], yy, cz + p[1], Blocks.END_ROD.getDefaultState());
		}
		BlockState candles = Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true);
		b.set(cx - 5, y + 2, dz, candles);
		b.set(cx + 5, y + 2, dz, candles);
		b.set(cx - 5, y + 2, dz + 4, Blocks.POTTED_WHITE_TULIP.getDefaultState());
		b.set(cx + 5, y + 2, dz + 4, Blocks.POTTED_LILY_OF_THE_VALLEY.getDefaultState());
		// Waiting bench for souls (Kazuma sits here, again)
		for (int z = cz - 6; z <= cz - 2; z++) b.set(cx - 12, y + 1, z, Build.stairs(Blocks.QUARTZ_STAIRS, Direction.WEST));
		b.set(cx - 12, y + 1, cz - 7, Blocks.QUARTZ_SLAB.getDefaultState());
		b.set(cx - 12, y + 1, cz - 1, Blocks.QUARTZ_SLAB.getDefaultState());
		b.set(cx - 13, y + 1, cz + 2, Blocks.POTTED_BLUE_ORCHID.getDefaultState());
		// A paperwork desk with an absurd stack of files ("reincarnation queue")
		b.fill(cx + 11, y + 1, cz - 6, cx + 13, y + 1, cz - 4, Blocks.BIRCH_PLANKS);
		b.set(cx + 12, y + 2, cz - 5, Blocks.BOOKSHELF.getDefaultState());
		b.set(cx + 12, y + 3, cz - 5, Blocks.WHITE_WOOL.getDefaultState());
		b.set(cx + 12, y + 4, cz - 5, Blocks.WHITE_CARPET.getDefaultState());
		b.set(cx + 11, y + 2, cz - 6, Blocks.WHITE_CARPET.getDefaultState());
		b.set(cx + 13, y + 2, cz - 4, Blocks.WHITE_CARPET.getDefaultState());
	}
}
