package dev.konocraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.konocraft.entity.TownsfolkEntity;
import dev.konocraft.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnReason;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Generates the hand-designed KonoSuba map: terrain from {@link Terrain}, buildings from {@link Blueprint}. */
public class KonoChunkGenerator extends ChunkGenerator {
	public static final Codec<KonoChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
	).apply(instance, instance.stable(KonoChunkGenerator::new)));

	private static final int MIN_Y = -64;
	private static final int HEIGHT = 384;

	public KonoChunkGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	@Override
	protected Codec<? extends ChunkGenerator> getCodec() {
		return CODEC;
	}

	@Override
	public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
		ChunkPos cp = chunk.getPos();
		int x0 = cp.getStartX(), z0 = cp.getStartZ();
		int bottom = chunk.getBottomY(), top = chunk.getTopY();
		BlockPos.Mutable m = new BlockPos.Mutable();
		Terrain.BlockSink sink = (x, y, z, s) -> {
			if (x < x0 || x > x0 + 15 || z < z0 || z > z0 + 15 || y < bottom || y >= top) return;
			chunk.setBlockState(m.set(x, y, z), s, false);
		};
		Blueprint bp = Blueprint.get();
		for (int x = x0; x < x0 + 16; x++) {
			for (int z = z0; z < z0 + 16; z++) {
				Terrain.fillColumn(x, z, bottom, sink);
				if (!bp.touchesColumn(x, z)) Terrain.decorateColumn(x, z, sink);
			}
		}
		Terrain.trees(x0, z0, (x, y, z, s) -> {
			if (bp.touchesColumn(x, z)) return;
			if (x < x0 || x > x0 + 15 || z < z0 || z > z0 + 15) return;
			// do not replace solid terrain with leaves
			BlockState cur = chunk.getBlockState(m.set(x, y, z));
			if (!cur.isAir() && !cur.isReplaceable()) return;
			sink.set(x, y, z, s);
		});
		bp.forEachInChunk(cp.x, cp.z, sink);
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public void populateEntities(ChunkRegion region) {
		ChunkPos cp = region.getCenterPos();
		if (cp.x == dev.konocraft.world.structure.KeeleDungeon.KEELE_X >> 4 && cp.z == dev.konocraft.world.structure.KeeleDungeon.KEELE_Z >> 4) {
			dev.konocraft.story.WorldEvents.spawnKeele(region);
		}
		int x = cp.getStartX() + 8, z = cp.getStartZ() + 8;
		if (!Regions.inTown(x, z) || Math.abs(x) > Regions.TOWN_HALF - 8 || Math.abs(z) > Regions.TOWN_HALF - 8) return;
		// One or two townsfolk wander the streets of most town chunks
		double r = Terrain.rand(cp.x, cp.z, 900);
		int count = r < 0.35 ? 0 : r < 0.85 ? 1 : 2;
		for (int i = 0; i < count; i++) {
			int sx = cp.getStartX() + 2 + (int) (Terrain.rand(cp.x, cp.z, 901 + i) * 12);
			int sz = cp.getStartZ() + 2 + (int) (Terrain.rand(cp.x, cp.z, 903 + i) * 12);
			int y = findFloor(region, sx, sz);
			if (y < 0) continue;
			TownsfolkEntity e = ModEntities.TOWNSFOLK.create(region.toServerWorld());
			if (e == null) continue;
			e.refreshPositionAndAngles(sx + 0.5, y, sz + 0.5, (float) (Terrain.rand(sx, sz, 905) * 360), 0);
			e.setVariant((int) (Terrain.rand(sx, sz, 906) * TownsfolkEntity.VARIANTS));
			e.setHome(new BlockPos(sx, y, sz));
			e.initialize(region, region.getLocalDifficulty(e.getBlockPos()), SpawnReason.STRUCTURE, null, null);
			region.spawnEntityAndPassengers(e);
		}
	}

	/** Finds a walkable spot (two air blocks over a solid one) near ground level in the town. */
	private static int findFloor(ChunkRegion region, int x, int z) {
		BlockPos.Mutable m = new BlockPos.Mutable();
		for (int y = Regions.TOWN_Y + 1; y <= Regions.TOWN_Y + 3; y++) {
			BlockState below = region.getBlockState(m.set(x, y - 1, z));
			BlockState at = region.getBlockState(m.set(x, y, z));
			BlockState above = region.getBlockState(m.set(x, y + 1, z));
			if (below.isSolid() && at.isAir() && above.isAir()) return y;
		}
		return -1;
	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
	}

	@Override
	public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
	}

	@Override
	public int getWorldHeight() {
		return HEIGHT;
	}

	@Override
	public int getSeaLevel() {
		return Regions.SEA_LEVEL;
	}

	@Override
	public int getMinimumY() {
		return MIN_Y;
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		int h = Terrain.heightAt(x, z);
		if (heightmap == Heightmap.Type.OCEAN_FLOOR || heightmap == Heightmap.Type.OCEAN_FLOOR_WG) return h + 1;
		return Math.max(h, Regions.SEA_LEVEL) + 1;
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		int bottom = world.getBottomY();
		BlockState[] states = new BlockState[world.getHeight()];
		java.util.Arrays.fill(states, Blocks.AIR.getDefaultState());
		Terrain.fillColumn(x, z, bottom, (bx, by, bz, s) -> {
			int i = by - bottom;
			if (i >= 0 && i < states.length) states[i] = s;
		});
		return new VerticalBlockSample(bottom, states);
	}

	@Override
	public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
		text.add("KonoCraft region: " + Regions.biomeAt(pos.getX(), pos.getZ()));
	}
}
