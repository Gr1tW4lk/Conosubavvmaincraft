package dev.konocraft.fx;

import dev.konocraft.net.Packets;
import dev.konocraft.net.ServerNet;
import dev.konocraft.registry.ModSounds;
import dev.konocraft.story.DialogueManager;
import dev.konocraft.util.Scheduler;
import net.minecraft.block.Blocks;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/** Particle + sound effects shared by skills, cutscenes and bosses. */
public final class Fx {
	public static final DustParticleEffect RED = new DustParticleEffect(new Vector3f(1f, 0.15f, 0.1f), 1.6f);
	public static final DustParticleEffect CRIMSON = new DustParticleEffect(new Vector3f(0.8f, 0f, 0.05f), 2.0f);
	public static final DustParticleEffect GOLD = new DustParticleEffect(new Vector3f(1f, 0.85f, 0.3f), 1.2f);
	public static final DustParticleEffect BLUE = new DustParticleEffect(new Vector3f(0.3f, 0.75f, 1f), 1.4f);
	public static final DustParticleEffect PURPLE = new DustParticleEffect(new Vector3f(0.65f, 0.3f, 1f), 1.6f);
	public static final DustParticleEffect WHITE = new DustParticleEffect(new Vector3f(1f, 1f, 1f), 1.2f);

	/** Players this far away still see the explosion effects. */
	private static final double FX_RANGE = 256;

	private Fx() {
	}

	public static void play(ServerPlayerEntity player, String type, Vec3d pos, DialogueManager.Session session) {
		ServerWorld w = player.getServerWorld();
		switch (type) {
			case "explosion" -> explosion(w, pos, 1.0f);
			case "explosion_double" -> {
				explosion(w, pos.add(-8, 0, 0), 0.9f);
				Scheduler.after(6, () -> explosion(w, pos.add(8, 0, 0), 0.9f));
			}
			case "explosion_small" -> smallExplosion(w, pos);
			case "magic_circle" -> magicCircle(w, pos, 5.5, 60, RED);
			case "sparkle" -> sparkle(w, pos);
			case "water" -> water(w, pos);
			case "heal" -> heal(w, pos);
			case "holy" -> holy(w, pos);
			case "curse" -> curse(w, pos);
			case "flash" -> flashAll(w, pos, 12);
			case "shake" -> shakeAll(w, pos, 30, 1.0f);
			case "smoke" -> w.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 1, pos.z, 40, 1.2, 1, 1.2, 0.02);
			case "barrier_break" -> barrierBreak(w, pos);
			case "teleport" -> teleport(w, pos);
			case "hearts" -> w.spawnParticles(ParticleTypes.HEART, pos.x, pos.y + 2, pos.z, 6, 0.4, 0.3, 0.4, 0.1);
			case "tears" -> w.spawnParticles(ParticleTypes.FALLING_WATER, pos.x, pos.y + 1.7, pos.z, 25, 0.25, 0.1, 0.25, 0);
			case "confetti" -> confetti(w, pos);
			default -> {
			}
		}
	}

	/** Megumin-grade explosion. The fireball, shock waves and mushroom cloud are drawn by the clients (ExplosionFx). */
	public static void explosion(ServerWorld w, Vec3d pos, float scale) {
		for (ServerPlayerEntity p : w.getPlayers()) {
			if (p.squaredDistanceTo(pos) < FX_RANGE * FX_RANGE) ServerNet.sendFxAt(p, Packets.FX_AT_BLAST, pos, 0, scale);
		}
		w.playSound(null, pos.x, pos.y, pos.z, ModSounds.EXPLOSION_BIG, SoundCategory.MASTER, 6f, 1f);
		w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 6f, 0.5f);
		w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 4f, 0.6f);
		flashAll(w, pos, 14);
		shakeAll(w, pos, 50, 1.6f * scale);
	}

	public static void smallExplosion(ServerWorld w, Vec3d pos) {
		w.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 1, pos.z, 4, 0.8, 0.5, 0.8, 0);
		w.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 1, pos.z, 30, 0.6, 0.6, 0.6, 0.05);
		w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 1.5f, 1.2f);
	}

	/** Megumin's magic circle gathering power for {@code ticks}; drawn by the clients (ExplosionFx). */
	public static void magicCircle(ServerWorld w, Vec3d pos, double radius, int ticks, ParticleEffect color) {
		w.playSound(null, pos.x, pos.y, pos.z, ModSounds.CHANT, SoundCategory.MASTER, 2f, 1f);
		for (ServerPlayerEntity p : w.getPlayers()) {
			if (p.squaredDistanceTo(pos) < FX_RANGE * FX_RANGE) ServerNet.sendFxAt(p, Packets.FX_AT_CHARGE, pos, ticks, (float) radius);
		}
	}

	public static void sparkle(ServerWorld w, Vec3d pos) {
		w.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1, pos.z, 30, 0.6, 0.8, 0.6, 0.05);
		w.spawnParticles(GOLD, pos.x, pos.y + 1, pos.z, 30, 0.6, 0.8, 0.6, 0);
		w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.MASTER, 1f, 1.4f);
	}

	public static void water(ServerWorld w, Vec3d pos) {
		w.spawnParticles(ParticleTypes.SPLASH, pos.x, pos.y + 1, pos.z, 120, 1.2, 1, 1.2, 0.3);
		w.spawnParticles(ParticleTypes.FALLING_WATER, pos.x, pos.y + 3, pos.z, 80, 1.5, 1, 1.5, 0);
		w.spawnParticles(BLUE, pos.x, pos.y + 1, pos.z, 50, 1, 1, 1, 0);
		w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.MASTER, 1.5f, 0.9f);
	}

	public static void heal(ServerWorld w, Vec3d pos) {
		w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 1, pos.z, 25, 0.5, 0.8, 0.5, 0);
		w.spawnParticles(ParticleTypes.HEART, pos.x, pos.y + 1.8, pos.z, 5, 0.4, 0.3, 0.4, 0);
		w.playSound(null, pos.x, pos.y, pos.z, ModSounds.HOLY, SoundCategory.MASTER, 1f, 1.3f);
	}

	public static void holy(ServerWorld w, Vec3d pos) {
		for (int i = 0; i < 12; i++) w.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y + i * 0.6, pos.z, 6, 0.5, 0.2, 0.5, 0.01);
		w.spawnParticles(WHITE, pos.x, pos.y + 1, pos.z, 120, 2.5, 1.5, 2.5, 0);
		w.spawnParticles(GOLD, pos.x, pos.y + 1, pos.z, 80, 2.5, 1.5, 2.5, 0);
		w.playSound(null, pos.x, pos.y, pos.z, ModSounds.HOLY, SoundCategory.MASTER, 2f, 1f);
	}

	public static void curse(ServerWorld w, Vec3d pos) {
		w.spawnParticles(PURPLE, pos.x, pos.y + 1, pos.z, 120, 0.8, 1.2, 0.8, 0);
		w.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y + 1, pos.z, 30, 0.6, 1, 0.6, 0.03);
		w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 0.6f, 1.6f);
	}

	public static void barrierBreak(ServerWorld w, Vec3d pos) {
		w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PURPLE_STAINED_GLASS.getDefaultState()), pos.x, pos.y + 6, pos.z, 600, 9, 6, 9, 0.6);
		w.spawnParticles(PURPLE, pos.x, pos.y + 6, pos.z, 400, 10, 6, 10, 0);
		w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.MASTER, 6f, 0.5f);
		w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.MASTER, 6f, 0.8f);
		flashAll(w, pos, 8);
	}

	public static void teleport(ServerWorld w, Vec3d pos) {
		w.spawnParticles(ParticleTypes.PORTAL, pos.x, pos.y + 1, pos.z, 200, 1.5, 2, 1.5, 0.8);
		w.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 1, pos.z, 100, 1, 2, 1, 0.1);
		w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.MASTER, 2f, 0.8f);
	}

	public static void confetti(ServerWorld w, Vec3d pos) {
		DustParticleEffect[] colors = {RED, GOLD, BLUE, PURPLE, WHITE};
		for (DustParticleEffect c : colors) w.spawnParticles(c, pos.x, pos.y + 3, pos.z, 40, 3, 1.5, 3, 0);
		w.spawnParticles(ParticleTypes.FIREWORK, pos.x, pos.y + 3, pos.z, 60, 2, 1, 2, 0.1);
		w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.MASTER, 2f, 1f);
	}

	public static void flashAll(ServerWorld w, Vec3d pos, int duration) {
		for (ServerPlayerEntity p : w.getPlayers()) {
			if (p.squaredDistanceTo(pos) < 160 * 160) ServerNet.sendFx(p, Packets.FX_FLASH, duration, 1f);
		}
	}

	public static void shakeAll(ServerWorld w, Vec3d pos, int duration, float strength) {
		for (ServerPlayerEntity p : w.getPlayers()) {
			double d = Math.sqrt(p.squaredDistanceTo(pos));
			if (d < 160) ServerNet.sendFx(p, Packets.FX_SHAKE, duration, strength * (float) Math.max(0.3, 1 - d / 160));
		}
	}
}
