package dev.konocraft.world;

import dev.konocraft.KonoCraft;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public final class KonoWorld {
	public static final RegistryKey<World> KEY = RegistryKey.of(RegistryKeys.WORLD, KonoCraft.id("konosuba"));

	private KonoWorld() {
	}

	public static void init() {
		Registry.register(Registries.CHUNK_GENERATOR, KonoCraft.id("konosuba"), KonoChunkGenerator.CODEC);
		Registry.register(Registries.BIOME_SOURCE, KonoCraft.id("konosuba"), KonoBiomeSource.CODEC);
		MobSpawner.init();
	}

	public static ServerWorld get(MinecraftServer server) {
		return server.getWorld(KEY);
	}

	public static boolean is(World world) {
		return world.getRegistryKey() == KEY;
	}
}
