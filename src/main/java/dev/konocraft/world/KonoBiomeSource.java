package dev.konocraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.stream.Stream;

/** Biomes follow the hand-made regions of the map (town, toad plains, haunted castle...). */
public class KonoBiomeSource extends BiomeSource {
	public static final Codec<KonoBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Biome.REGISTRY_CODEC.fieldOf("town").forGetter(s -> s.town),
		Biome.REGISTRY_CODEC.fieldOf("plains").forGetter(s -> s.plains),
		Biome.REGISTRY_CODEC.fieldOf("toad_plains").forGetter(s -> s.toadPlains),
		Biome.REGISTRY_CODEC.fieldOf("farms").forGetter(s -> s.farms),
		Biome.REGISTRY_CODEC.fieldOf("lake").forGetter(s -> s.lake),
		Biome.REGISTRY_CODEC.fieldOf("forest").forGetter(s -> s.forest),
		Biome.REGISTRY_CODEC.fieldOf("castle").forGetter(s -> s.castle),
		Biome.REGISTRY_CODEC.fieldOf("battlefield").forGetter(s -> s.battlefield),
		Biome.REGISTRY_CODEC.fieldOf("afterlife").forGetter(s -> s.afterlife)
	).apply(instance, instance.stable(KonoBiomeSource::new)));

	private final RegistryEntry<Biome> town, plains, toadPlains, farms, lake, forest, castle, battlefield, afterlife;

	public KonoBiomeSource(RegistryEntry<Biome> town, RegistryEntry<Biome> plains, RegistryEntry<Biome> toadPlains,
						   RegistryEntry<Biome> farms, RegistryEntry<Biome> lake, RegistryEntry<Biome> forest,
						   RegistryEntry<Biome> castle, RegistryEntry<Biome> battlefield, RegistryEntry<Biome> afterlife) {
		this.town = town;
		this.plains = plains;
		this.toadPlains = toadPlains;
		this.farms = farms;
		this.lake = lake;
		this.forest = forest;
		this.castle = castle;
		this.battlefield = battlefield;
		this.afterlife = afterlife;
	}

	@Override
	protected Codec<? extends BiomeSource> getCodec() {
		return CODEC;
	}

	@Override
	protected Stream<RegistryEntry<Biome>> biomeStream() {
		return Stream.of(town, plains, toadPlains, farms, lake, forest, castle, battlefield, afterlife);
	}

	@Override
	public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
		return switch (Regions.biomeAt(x << 2, z << 2)) {
			case TOWN -> town;
			case TOAD_PLAINS -> toadPlains;
			case FARMS -> farms;
			case LAKE -> lake;
			case FOREST -> forest;
			case CASTLE -> castle;
			case BATTLEFIELD -> battlefield;
			case AFTERLIFE -> afterlife;
			default -> plains;
		};
	}
}
