package dev.konocraft.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.HorizontalConnectingBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.enums.WallShape;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

/**
 * All hand-built structures of the world, stored as absolute block positions. The chunk generator
 * stamps the blocks of each chunk as the chunk is generated, so the whole map exists without any
 * lag spikes and looks identical in every world.
 */
public final class Blueprint {
	private final Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap<>(1 << 20);
	private Long2ObjectOpenHashMap<long[]> byChunk;

	private static volatile Blueprint instance;

	public static Blueprint get() {
		Blueprint b = instance;
		if (b == null) {
			synchronized (Blueprint.class) {
				b = instance;
				if (b == null) {
					long start = System.currentTimeMillis();
					b = new Blueprint();
					Structures.buildAll(new Build(b));
					b.finish();
					dev.konocraft.KonoCraft.LOGGER.info("KonoCraft world blueprint: {} blocks in {} chunks, built in {} ms",
						b.blocks.size(), b.byChunk.size(), System.currentTimeMillis() - start);
					instance = b;
				}
			}
		}
		return b;
	}

	public void set(int x, int y, int z, BlockState state) {
		blocks.put(BlockPos.asLong(x, y, z), state);
	}

	public BlockState get(int x, int y, int z) {
		return blocks.get(BlockPos.asLong(x, y, z));
	}

	private final Long2ObjectOpenHashMap<net.minecraft.nbt.NbtCompound> blockEntities = new Long2ObjectOpenHashMap<>();

	/** Extra block entity data (e.g. a loot table) for the block at this position. */
	public void setBlockEntity(int x, int y, int z, net.minecraft.nbt.NbtCompound nbt) {
		blockEntities.put(BlockPos.asLong(x, y, z), nbt);
	}

	public net.minecraft.nbt.NbtCompound blockEntity(int x, int y, int z) {
		return blockEntities.get(BlockPos.asLong(x, y, z));
	}

	/** Every blueprint position inside the box whose block matches. */
	public java.util.List<BlockPos> find(java.util.function.Predicate<BlockState> test, int x1, int z1, int x2, int z2) {
		java.util.List<BlockPos> out = new java.util.ArrayList<>();
		for (var e : blocks.long2ObjectEntrySet()) {
			long l = e.getLongKey();
			int x = BlockPos.unpackLongX(l), z = BlockPos.unpackLongZ(l);
			if (x >= x1 && x <= x2 && z >= z1 && z <= z2 && test.test(e.getValue())) out.add(BlockPos.fromLong(l));
		}
		return out;
	}

	public boolean has(int x, int y, int z) {
		return blocks.containsKey(BlockPos.asLong(x, y, z));
	}

	/** True when any structure block exists in this column (natural plants and trees skip it). */
	public boolean touchesColumn(int x, int z) {
		return columns.contains(ChunkPos.toLong(x, z));
	}

	private final LongOpenHashSet columns = new LongOpenHashSet();

	private void finish() {
		connectBlocks();
		Long2ObjectOpenHashMap<LongArrayList> lists = new Long2ObjectOpenHashMap<>();
		for (Long2ObjectMap.Entry<BlockState> e : blocks.long2ObjectEntrySet()) {
			long pos = e.getLongKey();
			int bx = BlockPos.unpackLongX(pos), bz = BlockPos.unpackLongZ(pos);
			long chunk = ChunkPos.toLong(bx >> 4, bz >> 4);
			lists.computeIfAbsent(chunk, k -> new LongArrayList()).add(pos);
			columns.add(ChunkPos.toLong(bx, bz));
		}
		byChunk = new Long2ObjectOpenHashMap<>(lists.size());
		for (Long2ObjectMap.Entry<LongArrayList> e : lists.long2ObjectEntrySet()) {
			byChunk.put(e.getLongKey(), e.getValue().toLongArray());
		}
	}

	/** Every block of every structure (used to paint the world map). */
	public void forEachBlock(Terrain.BlockSink sink) {
		for (Long2ObjectMap.Entry<BlockState> e : blocks.long2ObjectEntrySet()) {
			long l = e.getLongKey();
			sink.set(BlockPos.unpackLongX(l), BlockPos.unpackLongY(l), BlockPos.unpackLongZ(l), e.getValue());
		}
	}

	public boolean hasChunk(int chunkX, int chunkZ) {
		return byChunk.containsKey(ChunkPos.toLong(chunkX, chunkZ));
	}

	public void forEachInChunk(int chunkX, int chunkZ, Terrain.BlockSink sink) {
		long[] list = byChunk.get(ChunkPos.toLong(chunkX, chunkZ));
		if (list == null) return;
		for (long pos : list) {
			sink.set(BlockPos.unpackLongX(pos), BlockPos.unpackLongY(pos), BlockPos.unpackLongZ(pos), blocks.get(pos));
		}
	}

	/** Worldgen does not run neighbour updates, so fences, panes and walls get their connections here. */
	private void connectBlocks() {
		LongArrayList keys = new LongArrayList(blocks.keySet());
		for (int i = 0; i < keys.size(); i++) {
			long pos = keys.getLong(i);
			BlockState state = blocks.get(pos);
			boolean fence = state.getBlock() instanceof FenceBlock;
			boolean pane = state.getBlock() instanceof PaneBlock;
			boolean wall = state.getBlock() instanceof WallBlock;
			if (!fence && !pane && !wall) continue;
			int x = BlockPos.unpackLongX(pos), y = BlockPos.unpackLongY(pos), z = BlockPos.unpackLongZ(pos);
			for (Direction d : Direction.Type.HORIZONTAL) {
				BlockState n = blocks.get(BlockPos.asLong(x + d.getOffsetX(), y, z + d.getOffsetZ()));
				boolean connect = n != null && connects(state, n);
				if (wall) {
					EnumProperty<WallShape> prop = switch (d) {
						case NORTH -> WallBlock.NORTH_SHAPE;
						case SOUTH -> WallBlock.SOUTH_SHAPE;
						case EAST -> WallBlock.EAST_SHAPE;
						default -> WallBlock.WEST_SHAPE;
					};
					state = state.with(prop, connect ? WallShape.LOW : WallShape.NONE);
				} else {
					BooleanProperty prop = switch (d) {
						case NORTH -> HorizontalConnectingBlock.NORTH;
						case SOUTH -> HorizontalConnectingBlock.SOUTH;
						case EAST -> HorizontalConnectingBlock.EAST;
						default -> HorizontalConnectingBlock.WEST;
					};
					state = state.with(prop, connect);
				}
			}
			if (wall) {
				boolean ns = state.get(WallBlock.NORTH_SHAPE) != WallShape.NONE && state.get(WallBlock.SOUTH_SHAPE) != WallShape.NONE;
				boolean ew = state.get(WallBlock.EAST_SHAPE) != WallShape.NONE && state.get(WallBlock.WEST_SHAPE) != WallShape.NONE;
				boolean straight = (ns && state.get(WallBlock.EAST_SHAPE) == WallShape.NONE && state.get(WallBlock.WEST_SHAPE) == WallShape.NONE)
					|| (ew && state.get(WallBlock.NORTH_SHAPE) == WallShape.NONE && state.get(WallBlock.SOUTH_SHAPE) == WallShape.NONE);
				BlockState above = blocks.get(BlockPos.asLong(x, y + 1, z));
				state = state.with(WallBlock.UP, !straight || (above != null && !above.isAir()));
			}
			blocks.put(pos, state);
		}
	}

	private static boolean connects(BlockState self, BlockState other) {
		if (other.isAir()) return false;
		if (self.getBlock() instanceof FenceBlock) {
			return other.getBlock() instanceof FenceBlock || isFullSolid(other);
		}
		if (self.getBlock() instanceof PaneBlock) {
			return other.getBlock() instanceof PaneBlock || other.getBlock() instanceof WallBlock || isFullSolid(other);
		}
		return other.getBlock() instanceof WallBlock || other.getBlock() instanceof PaneBlock || isFullSolid(other);
	}

	private static boolean isFullSolid(BlockState s) {
		return s.isOpaque() && s.isSolid() && !s.isOf(Blocks.WATER);
	}
}
