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

	public static void explosion(ServerWorld w, Vec3d pos, float scale) {
		double r = 6 * scale;
		w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1, pos.z, (int) (6 * scale), r * 0.4, r * 0.3, r * 0.4, 0);
		w.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + 2, pos.z, (int) (400 * scale), r * 0.5, r * 0.5, r * 0.5, 0.35);
		w.spawnParticles(ParticleTypes.LAVA, pos.x, pos.y + 1, pos.z, (int) (80 * scale), r * 0.4, 1, r * 0.4, 0);
		w.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 4, pos.z, (int) (200 * scale), r * 0.6, r * 0.7, r * 0.6, 0.08);
		w.spawnParticles(CRIMSON, pos.x, pos.y + 3, pos.z, (int) (300 * scale), r * 0.6, r * 0.8, r * 0.6, 0);
		// rising column of fire
		for (int i = 0; i < 24; i++) {
			w.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + i, pos.z, 12, 0.8 + i * 0.05, 0.3, 0.8 + i * 0.05, 0.02);
		}
		// shock ring on the ground
		for (int k = 0; k < 64; k++) {
			double a = k * Math.PI * 2 / 64;
			w.spawnParticles(ParticleTypes.CLOUD, pos.x + Math.cos(a) * r, pos.y + 0.3, pos.z + Math.sin(a) * r, 1, 0, 0, 0, 0.25);
		}
		w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DIRT.getDefaultState()), pos.x, pos.y + 1, pos.z, 150, r * 0.4, 1, r * 0.4, 0.4);
		w.playSound(null, pos.x, pos.y, pos.z, ModSounds.EXPLOSION_BIG, SoundCategory.MASTER, 6f, 1f);
		w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 6f, 0.5f);
		w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 4f, 0.6f);
		flashAll(w, pos, 14);
		shakeAll(w, pos, 40, 1.4f * scale);
		Scheduler.after(10, () -> w.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 8, pos.z, 150, r * 0.5, 3, r * 0.5, 0.05));
		Scheduler.after(25, () -> w.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y + 2, pos.z, 60, r * 0.4, 1, r * 0.4, 0.04));
	}

	public static void smallExplosion(ServerWorld w, Vec3d pos) {
		w.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 1, pos.z, 4, 0.8, 0.5, 0.8, 0);
		w.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 1, pos.z, 30, 0.6, 0.6, 0.6, 0.05);
		w.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 1.5f, 1.2f);
	}

	/** Expanding red magic circle drawn over several ticks. */
	public static void magicCircle(ServerWorld w, Vec3d pos, double radius, int ticks, ParticleEffect color) {
		w.playSound(null, pos.x, pos.y, pos.z, ModSounds.CHANT, SoundCategory.MASTER, 2f, 1f);
		for (int t = 0; t < ticks; t += 4) {
			final int tt = t;
			Scheduler.after(t + 1, () -> {
				double r = radius * Math.min(1, (tt + 8) / 30.0);
				double rot = tt * 0.05;
				for (int k = 0; k < 48; k++) {
					double a = k * Math.PI * 2 / 48 + rot;
					w.spawnParticles(color, pos.x + Math.cos(a) * r, pos.y + 0.2, pos.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
					w.spawnParticles(color, pos.x + Math.cos(a) * r * 0.6, pos.y + 0.2, pos.z + Math.sin(a) * r * 0.6, 1, 0, 0, 0, 0);
				}
				// star inside the circle
				for (int k = 0; k < 5; k++) {
					double a1 = k * Math.PI * 2 / 5 + rot, a2 = (k + 2) * Math.PI * 2 / 5 + rot;
					for (int s = 0; s <= 8; s++) {
						double f = s / 8.0;
						double x = Math.cos(a1) * r * (1 - f) + Math.cos(a2) * r * f;
						double z = Math.sin(a1) * r * (1 - f) + Math.sin(a2) * r * f;
						w.spawnParticles(color, pos.x + x, pos.y + 0.25, pos.z + z, 1, 0, 0, 0, 0);
					}
				}
				w.spawnParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 1.5, pos.z, 30, r * 0.5, 0.5, r * 0.5, 0.5);
			});
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
