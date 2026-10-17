package dev.konocraft.world.structure;

import dev.konocraft.world.Build;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Casino "Eris's Luck" on the eastern avenue: roulette, dice and card tables, a row of slot machines,
 * a bar, a VIP gallery upstairs and the manager's office with a secret he would rather keep.
 */
public final class AxelCasino {
	private AxelCasino() {
	}

	static final int G = AxelTown.G, F = AxelTown.F;
	public static final int X1 = 46, Z1 = -31, X2 = 75, Z2 = -10;
	/** Upper floor: the slab is at UPPER - 1. */
	public static final int UPPER = F + 7;
	public static final int ROULETTE_X = 58, ROULETTE_Z = -21;
	/** Buttons of the slot machines along the west wall. */
	public static final List<BlockPos> SLOT_BUTTONS = new ArrayList<>();
	/** The box with the enchanted dice in the manager's office. */
	public static final BlockPos EVIDENCE = new BlockPos(72, UPPER, -29);

	static {
		for (int z = -19; z <= Z2 - 1; z += 2) SLOT_BUTTONS.add(new BlockPos(X1 + 2, F + 1, z));
	}

	public static boolean isSlotButton(BlockPos p) {
		return p.getY() == F + 1 && p.getX() == X1 + 2 && SLOT_BUTTONS.contains(p);
	}

	static void build(Build b) {
		BlockState wall = Blocks.RED_NETHER_BRICKS.getDefaultState();
		BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
		BlockState trim = Blocks.QUARTZ_PILLAR.getDefaultState();
		int top = F + 10;
		// forecourt with a red carpet
		for (int x = 43; x <= 77; x++)
			for (int z = Z2 + 1; z <= -4; z++) b.set(x, G, z, AxelTown.cobbleStreet(x, z));
		for (int z = Z2 + 1; z <= -4; z++) {
			for (int x = 59; x <= 62; x++) b.set(x, F, z, Blocks.RED_CARPET.getDefaultState());
		}
		for (int x : new int[]{55, 66}) {
			for (int z : new int[]{-8, -5}) {
				b.fill(x, F, z, x, F + 2, z, trim);
				b.set(x, F + 3, z, gold);
				b.set(x, F + 4, z, Build.lantern(false, false));
			}
		}
		// shell
		b.fill(X1, G, Z1, X2, G, Z2, Blocks.POLISHED_BLACKSTONE);
		for (int x = X1 + 1; x < X2; x++)
			for (int z = Z1 + 1; z < Z2; z++)
				b.set(x, G, z, Math.floorMod(x + z, 2) == 0 ? Blocks.DARK_OAK_PLANKS.getDefaultState() : Blocks.SPRUCE_PLANKS.getDefaultState());
		b.clear(X1 + 1, F, Z1 + 1, X2 - 1, top, Z2 - 1);
		b.walls(X1, F, Z1, X2, top, Z2, wall);
		for (int x = X1; x <= X2; x += 5) {
			b.fill(x, F, Z1, x, top, Z1, trim);
			b.fill(x, F, Z2, x, top, Z2, trim);
			b.set(x, top + 1, Z2, gold);
		}
		for (int z = Z1; z <= Z2; z += 5) {
			b.fill(X1, F, z, X1, top, z, trim);
			b.fill(X2, F, z, X2, top, z, trim);
		}
		b.walls(X1, F + 6, Z1, X2, F + 6, Z2, gold);
		b.walls(X1, top, Z1, X2, top, Z2, gold);
		for (int x = X1 + 2; x <= X2 - 2; x += 5) {
			for (int k = 0; k < 2; k++) {
				int y = k == 0 ? F + 2 : F + 8;
				for (int dx = 0; dx <= 1; dx++) {
					b.set(x + dx, y, Z1, Blocks.ORANGE_STAINED_GLASS_PANE.getDefaultState());
					b.set(x + dx, y + 1, Z1, Blocks.ORANGE_STAINED_GLASS_PANE.getDefaultState());
					if (x + dx < 57 || x + dx > 64) {
						b.set(x + dx, y, Z2, Blocks.ORANGE_STAINED_GLASS_PANE.getDefaultState());
						b.set(x + dx, y + 1, Z2, Blocks.ORANGE_STAINED_GLASS_PANE.getDefaultState());
					}
				}
			}
		}
		b.gableRoof(X1, Z1, X2, Z2, top + 1, Blocks.NETHER_BRICK_STAIRS, gold, wall, true);
		// facade: the doors, a giant golden coin and banners
		b.clear(60, F, Z2, 61, F + 2, Z2);
		b.door(60, F, Z2, Blocks.DARK_OAK_DOOR, Direction.NORTH, false);
		b.door(61, F, Z2, Blocks.DARK_OAK_DOOR, Direction.NORTH, true);
		for (int x = 58; x <= 63; x++) b.set(x, F + 3, Z2 + 1, Build.slab(Blocks.SMOOTH_QUARTZ_SLAB, true));
		for (int dx = -3; dx <= 3; dx++)
			for (int dy = -2; dy <= 2; dy++) {
				double d = Math.sqrt((dx + 0.5) * (dx + 0.5) + dy * dy);
				if (d > 2.7) continue;
				b.set(60 + dx, F + 8 + dy, Z2 + 1, d < 1.3 ? Blocks.YELLOW_GLAZED_TERRACOTTA.getDefaultState() : gold);
			}
		for (int x : new int[]{56, 65}) {
			b.set(x, F + 5, Z2 + 1, AxelNobles.banner(Blocks.RED_WALL_BANNER, Direction.SOUTH));
			b.set(x, F + 4, Z2 + 1, Build.AIR);
			b.set(x + 1, F + 5, Z2 + 1, AxelNobles.banner(Blocks.YELLOW_WALL_BANNER, Direction.SOUTH));
		}
		for (int x = X1 + 1; x < X2; x += 3) b.set(x, F + 5, Z2 + 1, Blocks.OCHRE_FROGLIGHT.getDefaultState());

		// --- ground floor: the gaming hall
		for (int z = Z1 + 2; z < Z2; z++) {
			b.set(60, F, z, Blocks.RED_CARPET.getDefaultState());
			b.set(61, F, z, Blocks.RED_CARPET.getDefaultState());
		}
		// roulette wheel in the middle of the hall
		int rx = ROULETTE_X, rz = ROULETTE_Z;
		for (int dx = -3; dx <= 3; dx++)
			for (int dz = -3; dz <= 3; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > 3.2) continue;
				BlockState s;
				if (d < 0.8) s = gold;
				else if (d < 2.4) {
					int sector = Math.floorMod((int) Math.floor((Math.atan2(dz, dx) + Math.PI) / (Math.PI / 6)), 12);
					s = sector == 0 ? Blocks.LIME_CONCRETE.getDefaultState() : sector % 2 == 0 ? Blocks.RED_CONCRETE.getDefaultState() : Blocks.BLACK_CONCRETE.getDefaultState();
				} else s = Blocks.DARK_OAK_PLANKS.getDefaultState();
				b.set(rx + dx, F, rz + dz, s);
			}
		b.set(rx, F + 1, rz, Blocks.LIGHTNING_ROD.getDefaultState());
		b.set(rx + 2, F + 1, rz, Blocks.WHITE_CARPET.getDefaultState());
		// dice and card tables with green felt
		for (int[] t : new int[][]{{50, -25}, {69, -25}, {50, -16}, {69, -16}}) {
			b.fill(t[0], F, t[1], t[0] + 2, F, t[1] + 1, Blocks.GREEN_WOOL);
			for (int x = t[0]; x <= t[0] + 2; x++) {
				b.set(x, F, t[1] - 1, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
				b.set(x, F, t[1] + 2, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH));
			}
			b.set(t[0] + 1, F + 1, t[1], Blocks.WHITE_CARPET.getDefaultState());
			b.set(t[0], F + 1, t[1] + 1, Blocks.RED_CARPET.getDefaultState());
			b.set(t[0] + 2, F + 1, t[1], Blocks.CANDLE.getDefaultState().with(Properties.LIT, true).with(Properties.CANDLES, 2));
		}
		// slot machines along the west wall
		Block[] lights = {Blocks.OCHRE_FROGLIGHT, Blocks.VERDANT_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT};
		int i = 0;
		for (BlockPos p : SLOT_BUTTONS) {
			int x = X1 + 1, z = p.getZ();
			b.set(x, F, z, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
			b.set(x, F + 1, z, lights[i++ % lights.length].getDefaultState());
			b.set(x, F + 2, z, gold);
			b.set(x, F + 3, z, Blocks.YELLOW_CANDLE.getDefaultState().with(Properties.LIT, true));
			b.set(p.getX(), p.getY(), p.getZ(), Blocks.POLISHED_BLACKSTONE_BUTTON.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.EAST));
			b.set(x + 1, F, z, Blocks.RED_CARPET.getDefaultState());
		}
		// the bar along the east wall
		for (int z = Z1 + 3; z <= Z2 - 6; z++) {
			b.set(X2 - 3, F, z, Blocks.DARK_OAK_PLANKS.getDefaultState());
			b.set(X2 - 3, F + 1, z, Build.slab(Blocks.SMOOTH_QUARTZ_SLAB, false));
			b.set(X2 - 1, F, z, Blocks.BARREL.getDefaultState().with(Properties.FACING, Direction.WEST));
			b.set(X2 - 1, F + 1, z, Math.floorMod(z, 3) == 0 ? Blocks.BREWING_STAND.getDefaultState() : Blocks.BARREL.getDefaultState().with(Properties.FACING, Direction.WEST));
			if (Math.floorMod(z, 2) == 0) {
				b.set(X2 - 4, F, z, Blocks.DARK_OAK_FENCE.getDefaultState());
				b.set(X2 - 4, F + 1, z, Blocks.RED_CARPET.getDefaultState());
			}
		}
		// open gallery over the roulette, lit by froglights in the ceiling
		b.fill(X1 + 1, F + 6, Z1 + 1, X2 - 1, F + 6, Z2 - 1, Blocks.DARK_OAK_PLANKS);
		for (int x = X1 + 3; x < X2; x += 4)
			for (int z = Z1 + 3; z < Z2; z += 4) b.set(x, F + 6, z, Blocks.PEARLESCENT_FROGLIGHT.getDefaultState());
		int hx1 = 53, hx2 = 63, hz1 = -27, hz2 = -15;
		b.clear(hx1, F + 6, hz1, hx2, F + 6, hz2);
		for (int x = hx1 - 1; x <= hx2 + 1; x++) {
			b.set(x, UPPER, hz1 - 1, Blocks.DARK_OAK_FENCE.getDefaultState());
			b.set(x, UPPER, hz2 + 1, Blocks.DARK_OAK_FENCE.getDefaultState());
		}
		for (int z = hz1 - 1; z <= hz2 + 1; z++) {
			b.set(hx1 - 1, UPPER, z, Blocks.DARK_OAK_FENCE.getDefaultState());
			b.set(hx2 + 1, UPPER, z, Blocks.DARK_OAK_FENCE.getDefaultState());
		}
		for (int[] c : new int[][]{{rx, rz}, {rx - 3, -24}, {rx + 3, -18}}) {
			for (int y = F + 5; y <= top - 1; y++) b.set(c[0], y, c[1], Blocks.CHAIN.getDefaultState());
			b.set(c[0], F + 4, c[1], Build.lantern(false, true));
		}
		// stairs up (behind the slot machines)
		AxelNobles.stairFlight(b, X1 + 1, X1 + 2, F, -21, -1, 7, Blocks.DARK_OAK_STAIRS);

		// --- upper floor: VIP lounge (west) and the manager's office (east)
		for (int x = X1 + 2; x <= 50; x++) {
			b.set(x, UPPER, Z2 - 2, Build.stairs(Blocks.RED_NETHER_BRICK_STAIRS, Direction.SOUTH));
		}
		b.fill(X1 + 2, UPPER, Z2 - 5, 49, UPPER, Z2 - 4, Blocks.RED_WOOL);
		b.set(48, UPPER + 1, Z2 - 5, Blocks.CAKE.getDefaultState());
		b.set(52, UPPER, Z1 + 2, Blocks.GREEN_WOOL.getDefaultState());
		b.set(53, UPPER, Z1 + 2, Blocks.GREEN_WOOL.getDefaultState());
		b.set(52, UPPER, Z1 + 3, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH));
		b.set(53, UPPER, Z1 + 3, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH));
		int ox = 64;
		b.fill(ox, UPPER, Z1 + 1, ox, F + 9, -20, Blocks.DARK_OAK_PLANKS);
		b.fill(ox, UPPER, -20, X2 - 1, F + 9, -20, Blocks.DARK_OAK_PLANKS);
		b.door(ox, UPPER, -25, Blocks.SPRUCE_DOOR, Direction.EAST, false);
		for (int x = ox + 1; x < X2; x++)
			for (int z = Z1 + 1; z < -20; z++) b.set(x, UPPER, z, Blocks.RED_CARPET.getDefaultState());
		// desk, safe and the box with the enchanted dice
		for (int x = 69; x <= 72; x++) b.set(x, UPPER, -27, Blocks.DARK_OAK_PLANKS.getDefaultState());
		b.set(70, UPPER + 1, -27, Blocks.LECTERN.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		b.set(71, UPPER, -28, Build.stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH));
		b.fill(X2 - 1, UPPER, Z1 + 1, X2 - 1, UPPER + 2, Z1 + 3, Blocks.IRON_BLOCK);
		b.set(EVIDENCE.getX(), EVIDENCE.getY(), EVIDENCE.getZ(), Blocks.CHEST.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
		AxelTown.loot(b, EVIDENCE.getX(), EVIDENCE.getY(), EVIDENCE.getZ(), "konocraft:chests/casino_office");
		b.set(ox + 2, UPPER + 2, -25, Build.lantern(false, true));
		b.set(68, UPPER, -22, Blocks.POTTED_WITHER_ROSE.getDefaultState());
		b.set(73, UPPER, -22, Blocks.GOLD_BLOCK.getDefaultState());
		b.set(73, UPPER + 1, -22, Blocks.CANDLE.getDefaultState().with(Properties.LIT, true).with(Properties.CANDLES, 3));
		for (int x = 50; x <= 70; x += 10) b.set(x, F + 9, -13, Build.lantern(false, true));
		b.set(56, F + 9, -29, Build.lantern(false, true));
		// a little smoke from the kitchen chimney
		b.fill(X2 - 2, top + 1, Z1 + 2, X2 - 2, top + 9, Z1 + 2, Blocks.BRICKS);
		b.set(X2 - 2, top + 10, Z1 + 2, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
	}
}
