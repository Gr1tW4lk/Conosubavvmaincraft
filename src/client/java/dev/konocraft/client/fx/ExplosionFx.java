package dev.konocraft.client.fx;

import dev.konocraft.KonoCraft;
import dev.konocraft.client.ClientState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client side of Megumin's Explosion: stacked rotating magic circles and a beam while the spell charges,
 * then a flash, fireball, shock rings, fire pillar, mushroom cloud and ember rain. The geometry is drawn
 * additively, particles are spawned as "important" so they show from far away.
 */
public final class ExplosionFx {
	private ExplosionFx() {
	}

	private static final Identifier CIRCLE = KonoCraft.id("textures/fx/magic_circle.png");
	private static final Identifier RING = KonoCraft.id("textures/fx/shockwave.png");
	private static final Identifier GLOW = KonoCraft.id("textures/fx/glow.png");
	private static final Identifier SMOKE = KonoCraft.id("textures/fx/smoke.png");
	private static final Identifier BEAM = new Identifier("textures/entity/beacon_beam.png");

	private static final DustParticleEffect CRIMSON = new DustParticleEffect(new Vector3f(1f, 0.1f, 0.08f), 2.2f);
	private static final DustParticleEffect EMBER = new DustParticleEffect(new Vector3f(1f, 0.55f, 0.1f), 1.6f);
	private static final DustParticleEffect SOOT = new DustParticleEffect(new Vector3f(0.12f, 0.08f, 0.08f), 3.5f);
	private static final BlockState[] DEBRIS = {Blocks.DIRT.getDefaultState(), Blocks.STONE.getDefaultState(), Blocks.COARSE_DIRT.getDefaultState(), Blocks.MAGMA_BLOCK.getDefaultState(), Blocks.GRAVEL.getDefaultState()};

	private static final VertexConsumerProvider.Immediate BUFFERS = VertexConsumerProvider.immediate(new BufferBuilder(8192));
	private static final Random RAND = Random.create();

	private static final class Charge {
		final Vec3d pos;
		final float radius;
		final int duration;
		final long start;

		Charge(Vec3d pos, float radius, int duration) {
			this.pos = pos;
			this.radius = radius;
			this.duration = Math.max(10, duration);
			this.start = ticks;
		}
	}

	private static final class Blast {
		final Vec3d pos;
		final float scale;
		final long start;
		final Puff[] puffs;

		Blast(Vec3d pos, float scale) {
			this.pos = pos;
			this.scale = scale;
			this.start = ticks;
			this.puffs = Puff.cloud(new java.util.Random(Double.doubleToLongBits(pos.x * 31 + pos.z) ^ ticks));
		}
	}

	/** One puff of the mushroom cloud; positions are relative to the stem, the cap or the dust skirt. */
	private record Puff(int kind, float angle, float dist, float height, float size, float spin, int tile) {
		static final int STEM = 0, CAP = 1, TOP = 2, SKIRT = 3;

		static Puff[] cloud(java.util.Random r) {
			List<Puff> list = new ArrayList<>();
			for (int i = 0; i < 12; i++) list.add(new Puff(STEM, r.nextFloat() * 6.283f, r.nextFloat() * 1.6f, (i + r.nextFloat()) / 12f, 2.6f + r.nextFloat() * 1.6f, spin(r), r.nextInt(4)));
			for (int i = 0; i < 20; i++) list.add(new Puff(CAP, (i + r.nextFloat() * 0.6f) * 6.283f / 20, 0.55f + r.nextFloat() * 0.4f, (r.nextFloat() - 0.5f) * 3f, 0.38f + r.nextFloat() * 0.14f, spin(r), r.nextInt(4)));
			for (int i = 0; i < 7; i++) list.add(new Puff(TOP, r.nextFloat() * 6.283f, r.nextFloat() * 0.45f, 1.5f + r.nextFloat() * 3f, 0.45f + r.nextFloat() * 0.15f, spin(r), r.nextInt(4)));
			for (int i = 0; i < 18; i++) list.add(new Puff(SKIRT, (i + r.nextFloat() * 0.7f) * 6.283f / 18, 0.75f + r.nextFloat() * 0.3f, 0.6f + r.nextFloat() * 1.6f, 3f + r.nextFloat() * 2.5f, spin(r), r.nextInt(4)));
			return list.toArray(new Puff[0]);
		}

		private static float spin(java.util.Random r) {
			return (r.nextFloat() - 0.5f) * 0.04f;
		}
	}

	private static final List<Charge> charges = new ArrayList<>();
	private static final List<Blast> blasts = new ArrayList<>();
	private static long ticks;

	private static final int BLAST_LIFE = 140;

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(ExplosionFx::tick);
		WorldRenderEvents.AFTER_TRANSLUCENT.register(ExplosionFx::render);
	}

	/** A magic circle that keeps gathering power for {@code duration} ticks. */
	public static void charge(Vec3d pos, int duration, float radius) {
		charges.add(new Charge(pos, radius, duration));
		sound(pos, SoundEvents.BLOCK_BEACON_ACTIVATE, 3f, 0.6f);
		sound(pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 3f, 0.5f);
	}

	/** The detonation itself. */
	public static void blast(Vec3d pos, float scale) {
		charges.removeIf(c -> c.pos.squaredDistanceTo(pos) < 16 * 16);
		blasts.add(new Blast(pos, scale));
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player != null) {
			double d = mc.player.getPos().distanceTo(pos);
			if (d < 90) {
				ClientState.heatMax = 70;
				ClientState.heatTicks = (int) (70 * MathHelper.clamp(1.15 - d / 90, 0.25, 1));
			}
		}
		global(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 1f);
		global(SoundEvents.ENTITY_GENERIC_EXPLODE, 0.4f, 1f);
	}

	private static void tick(MinecraftClient mc) {
		ClientWorld w = mc.world;
		if (w == null) {
			charges.clear();
			blasts.clear();
			return;
		}
		if (mc.isPaused()) return;
		ticks++;
		for (Iterator<Charge> it = charges.iterator(); it.hasNext(); ) {
			Charge c = it.next();
			int age = (int) (ticks - c.start);
			if (age > c.duration + 4) {
				it.remove();
				continue;
			}
			tickCharge(w, c, age);
		}
		for (Iterator<Blast> it = blasts.iterator(); it.hasNext(); ) {
			Blast b = it.next();
			int age = (int) (ticks - b.start);
			if (age > BLAST_LIFE) {
				it.remove();
				continue;
			}
			tickBlast(w, b, age);
		}
	}

	// ---------------------------------------------------------------- particles

	private static void tickCharge(ClientWorld w, Charge c, int age) {
		double f = (double) age / c.duration;
		double r = c.radius * Math.min(1, f * 3.5);
		Vec3d p = c.pos;
		// runes stream into the centre of the circle
		for (int i = 0; i < 6 + (int) (f * 14); i++) {
			double a = RAND.nextDouble() * Math.PI * 2, d = r * (0.4 + RAND.nextDouble() * 0.8);
			add(w, ParticleTypes.ENCHANT, p.x, p.y + 2.2, p.z, Math.cos(a) * d, RAND.nextDouble() * 3 - 0.5, Math.sin(a) * d);
		}
		// crimson motes rising from the rim
		for (int i = 0; i < 10; i++) {
			double a = RAND.nextDouble() * Math.PI * 2;
			add(w, CRIMSON, p.x + Math.cos(a) * r, p.y + 0.2 + RAND.nextDouble() * 0.6, p.z + Math.sin(a) * r, 0, 0.05, 0);
		}
		// wind pulled into the spell
		if (f > 0.25) {
			for (int i = 0; i < 4; i++) {
				double a = RAND.nextDouble() * Math.PI * 2, d = c.radius * (1.4 + RAND.nextDouble());
				double x = Math.cos(a), z = Math.sin(a);
				add(w, ParticleTypes.END_ROD, p.x + x * d, p.y + 0.5 + RAND.nextDouble() * 6, p.z + z * d, -x * 0.35, 0.02, -z * 0.35);
			}
		}
		// fire climbing the beam
		if (f > 0.55) {
			for (int i = 0; i < 4; i++) {
				double a = (ticks * 0.6 + i * Math.PI / 2), rr = 0.6 + RAND.nextDouble() * 0.6;
				add(w, ParticleTypes.FLAME, p.x + Math.cos(a) * rr, p.y + 0.3, p.z + Math.sin(a) * rr, 0, 0.5 + RAND.nextDouble() * 0.6, 0);
			}
			add(w, ParticleTypes.ELECTRIC_SPARK, p.x + RAND.nextGaussian() * r * 0.4, p.y + 0.4, p.z + RAND.nextGaussian() * r * 0.4, 0, 0.2, 0);
		}
		if (f > 0.8 && age % 2 == 0) {
			add(w, ParticleTypes.LAVA, p.x + RAND.nextGaussian() * r * 0.3, p.y + 0.3, p.z + RAND.nextGaussian() * r * 0.3, 0, 0, 0);
		}
		if (age == c.duration / 2) sound(p, SoundEvents.BLOCK_BEACON_POWER_SELECT, 3f, 0.5f);
		if (age == Math.max(1, c.duration - 34)) sound(p, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 4f, 0.8f);
	}

	private static void tickBlast(ClientWorld w, Blast b, int age) {
		Vec3d p = b.pos;
		double s = b.scale;
		if (age == 0) {
			for (int i = 0; i < 8; i++) add(w, ParticleTypes.FLASH, p.x + RAND.nextGaussian() * 3, p.y + 2 + RAND.nextDouble() * 6, p.z + RAND.nextGaussian() * 3, 0, 0, 0);
			for (int i = 0; i < 14; i++) add(w, ParticleTypes.EXPLOSION_EMITTER, p.x + RAND.nextGaussian() * 4 * s, p.y + 1 + RAND.nextDouble() * 6 * s, p.z + RAND.nextGaussian() * 4 * s, 0, 0, 0);
			// debris thrown outwards
			for (int i = 0; i < 260; i++) {
				double a = RAND.nextDouble() * Math.PI * 2, v = 0.6 + RAND.nextDouble() * 1.4;
				ParticleEffect e = new BlockStateParticleEffect(ParticleTypes.BLOCK, DEBRIS[RAND.nextInt(DEBRIS.length)]);
				add(w, e, p.x, p.y + 0.6, p.z, Math.cos(a) * v, 0.5 + RAND.nextDouble() * 1.2, Math.sin(a) * v);
			}
			// dust wave on the ground
			for (int k = 0; k < 160; k++) {
				double a = k * Math.PI * 2 / 160;
				add(w, ParticleTypes.CLOUD, p.x + Math.cos(a) * 2, p.y + 0.4, p.z + Math.sin(a) * 2, Math.cos(a) * 1.6, 0.03, Math.sin(a) * 1.6);
				add(w, ParticleTypes.POOF, p.x + Math.cos(a) * 3, p.y + 0.8, p.z + Math.sin(a) * 3, Math.cos(a) * 1.1, 0.12, Math.sin(a) * 1.1);
			}
		}
		// fireball and pillar of fire
		if (age < 22) {
			double top = Math.min(48 * s, 6 + age * 3.2 * s);
			for (int i = 0; i < 26; i++) {
				double y = RAND.nextDouble() * top, wdt = 1.2 + y * 0.09;
				add(w, ParticleTypes.FLAME, p.x + RAND.nextGaussian() * wdt, p.y + y, p.z + RAND.nextGaussian() * wdt, RAND.nextGaussian() * 0.05, 0.25, RAND.nextGaussian() * 0.05);
			}
			for (int i = 0; i < 20; i++) {
				double a = RAND.nextDouble() * Math.PI * 2, d = RAND.nextDouble() * 7 * s;
				add(w, ParticleTypes.FLAME, p.x + Math.cos(a) * d, p.y + 1 + RAND.nextDouble() * 6 * s, p.z + Math.sin(a) * d, Math.cos(a) * 0.3, 0.15 + RAND.nextDouble() * 0.2, Math.sin(a) * 0.3);
			}
			for (int i = 0; i < 6; i++) {
				add(w, ParticleTypes.END_ROD, p.x + RAND.nextGaussian(), p.y + 2, p.z + RAND.nextGaussian(), RAND.nextGaussian() * 0.35, 1.2 + RAND.nextDouble() * 1.6, RAND.nextGaussian() * 0.35);
				add(w, ParticleTypes.LAVA, p.x + RAND.nextGaussian() * 3, p.y + 1, p.z + RAND.nextGaussian() * 3, 0, 0, 0);
			}
			if (age % 3 == 0) add(w, ParticleTypes.EXPLOSION_EMITTER, p.x + RAND.nextGaussian() * 2, p.y + top * RAND.nextDouble(), p.z + RAND.nextGaussian() * 2, 0, 0, 0);
		}
		// mushroom cloud: a smoking stem and a rolling cap
		if (age >= 3 && age < 70) {
			double capY = Math.min(34 * s, 10 + age * 1.4 * s);
			double capR = Math.min(15 * s, 3 + age * 0.45 * s);
			for (int i = 0; i < 5; i++) {
				add(w, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, p.x + RAND.nextGaussian() * 1.6, p.y + RAND.nextDouble() * capY, p.z + RAND.nextGaussian() * 1.6, 0, 0.06, 0);
			}
			for (int i = 0; i < 9; i++) {
				double a = RAND.nextDouble() * Math.PI * 2, d = capR * (0.3 + RAND.nextDouble() * 0.8);
				double x = Math.cos(a), z = Math.sin(a);
				add(w, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, p.x + x * d, p.y + capY + RAND.nextGaussian() * 2, p.z + z * d, x * 0.06, 0.02, z * 0.06);
				add(w, ParticleTypes.LARGE_SMOKE, p.x + x * d, p.y + capY - 2 + RAND.nextGaussian() * 2, p.z + z * d, x * 0.15, -0.02, z * 0.15);
			}
			if (age < 40) {
				for (int i = 0; i < 8; i++) {
					double a = RAND.nextDouble() * Math.PI * 2, d = capR * RAND.nextDouble() * 0.8;
					add(w, ParticleTypes.FLAME, p.x + Math.cos(a) * d, p.y + capY - 1 + RAND.nextGaussian(), p.z + Math.sin(a) * d, 0, 0.02, 0);
					add(w, CRIMSON, p.x + Math.cos(a) * d, p.y + capY * RAND.nextDouble(), p.z + Math.sin(a) * d, 0, 0, 0);
				}
			}
			if (age % 2 == 0) add(w, SOOT, p.x + RAND.nextGaussian() * 6 * s, p.y + 1 + RAND.nextDouble() * 4, p.z + RAND.nextGaussian() * 6 * s, 0, 0, 0);
		}
		// burning ground and ember rain
		if (age >= 8 && age < 120) {
			double rr = 22 * s;
			int n = age < 60 ? 6 : 2;
			for (int i = 0; i < n; i++) {
				double a = RAND.nextDouble() * Math.PI * 2, d = Math.sqrt(RAND.nextDouble()) * rr;
				add(w, ParticleTypes.FALLING_LAVA, p.x + Math.cos(a) * d, p.y + 14 + RAND.nextDouble() * 16, p.z + Math.sin(a) * d, 0, 0, 0);
				add(w, EMBER, p.x + Math.cos(a) * d * 0.7, p.y + 6 + RAND.nextDouble() * 14, p.z + Math.sin(a) * d * 0.7, 0, 0, 0);
			}
			double a = RAND.nextDouble() * Math.PI * 2, d = RAND.nextDouble() * 8 * s;
			add(w, ParticleTypes.FLAME, p.x + Math.cos(a) * d, p.y + 0.3, p.z + Math.sin(a) * d, 0, 0.04, 0);
			if (age % 3 == 0) add(w, ParticleTypes.LAVA, p.x + Math.cos(a) * d, p.y + 0.3, p.z + Math.sin(a) * d, 0, 0, 0);
		}
		switch (age) {
			case 5 -> global(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 0.9f);
			case 12 -> sound(p, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, 8f, 0.5f);
			case 24 -> global(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.35f, 0.6f);
			case 30, 50, 70, 90 -> sound(p, SoundEvents.BLOCK_FIRE_AMBIENT, 4f, 0.6f);
			default -> {
			}
		}
	}

	private static void add(ClientWorld w, ParticleEffect e, double x, double y, double z, double vx, double vy, double vz) {
		w.addImportantParticle(e, true, x, y, z, vx, vy, vz);
	}

	private static void sound(Vec3d p, SoundEvent e, float volume, float pitch) {
		ClientWorld w = MinecraftClient.getInstance().world;
		if (w != null) w.playSound(p.x, p.y, p.z, e, SoundCategory.MASTER, volume, pitch, false);
	}

	/** Heard everywhere, like the boom of a really big explosion. */
	private static void global(SoundEvent e, float pitch, float volume) {
		MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(e, pitch, volume));
	}

	// ---------------------------------------------------------------- geometry

	/** Alpha-blended, unlit: keeps deep colours even against a bright sky. */
	private static RenderLayer solid(Identifier tex) {
		return RenderLayer.getEntityTranslucentEmissive(tex);
	}

	/** Additive light. */
	private static RenderLayer light(Identifier tex) {
		return RenderLayer.getEyes(tex);
	}

	private static float capY(float age, float s) {
		return Math.min(34 * s, 10 + age * 1.4f * s);
	}

	private static float capR(float age, float s) {
		return Math.min(15 * s, 3 + age * 0.45f * s);
	}

	private static void render(WorldRenderContext ctx) {
		if (charges.isEmpty() && blasts.isEmpty()) return;
		MatrixStack m = ctx.matrixStack();
		Camera cam = ctx.camera();
		Vec3d cp = cam.getPos();
		double now = ticks + ctx.tickDelta();

		// --- alpha-blended layers: the crimson body of circles and beams, the smoke cloud, the orange haze
		VertexConsumer vc = BUFFERS.getBuffer(solid(CIRCLE));
		for (Charge c : charges) circles(m, cp, vc, c, now, false);
		vc = BUFFERS.getBuffer(solid(BEAM));
		for (Charge c : charges) {
			float f = (float) ((now - c.start) / c.duration);
			if (f < 0.5f) continue;
			float k = MathHelper.clamp((f - 0.5f) * 2.5f, 0, 1);
			at(m, cp, c.pos, 0);
			beam(m, vc, 0.3f + 0.4f * k, 90, (float) now, 0.85f, 0.04f, 0.04f, k * 0.9f, false);
			beam(m, vc, 0.7f + 1.0f * k, 90, (float) now * 0.5f, 0.9f, 0.1f, 0.05f, k * 0.35f, false);
			m.pop();
		}
		vc = BUFFERS.getBuffer(solid(SMOKE));
		for (Blast b : blasts) cloud(m, cam, cp, vc, b, (float) (now - b.start));
		vc = BUFFERS.getBuffer(solid(GLOW));
		for (Blast b : blasts) {
			float age = (float) (now - b.start);
			if (age >= 70) continue;
			float s = b.scale;
			float fade = age < 10 ? 1 : 1 - (age - 10) / 60f;
			at(m, cp, b.pos, 4 + age * 0.25f);
			billboard(m, cam, vc, (24 + age * 0.8f) * s, 1f, 0.42f, 0.08f, fade * 0.45f, false);
			m.pop();
			at(m, cp, b.pos, capY(age, s) - 2);
			billboard(m, cam, vc, capR(age, s) * 1.8f, 1f, 0.35f, 0.05f, fade * 0.4f, false);
			m.pop();
		}

		// --- additive light on top
		vc = BUFFERS.getBuffer(light(CIRCLE));
		for (Charge c : charges) circles(m, cp, vc, c, now, true);
		for (Blast b : blasts) {
			// the spent circle burns out after the blast
			float age = (float) (now - b.start);
			if (age > 20) continue;
			at(m, cp, b.pos, 0);
			flat(m, vc, 6.5f * b.scale * (1 + age * 0.05f), 0.06f, age * 0.1f, 1f, 0.6f, 0.3f, 1 - age / 20f, true);
			m.pop();
		}
		vc = BUFFERS.getBuffer(light(BEAM));
		for (Charge c : charges) {
			float f = (float) ((now - c.start) / c.duration);
			if (f < 0.5f) continue;
			float k = MathHelper.clamp((f - 0.5f) * 2.5f, 0, 1);
			at(m, cp, c.pos, 0);
			beam(m, vc, 0.12f + 0.12f * k, 90, (float) now * 1.5f, 1f, 0.45f, 0.3f, k * 0.8f, true);
			m.pop();
		}
		for (Blast b : blasts) {
			float age = (float) (now - b.start);
			if (age > 24) continue;
			float a = 1 - age / 24f;
			at(m, cp, b.pos, 0);
			beam(m, vc, (3.2f - age * 0.1f) * b.scale, 160, (float) now * 2, 1f, 0.95f, 0.8f, a, true);
			beam(m, vc, (6f + age * 0.2f) * b.scale, 160, (float) now, 1f, 0.4f, 0.1f, a * 0.5f, true);
			m.pop();
		}
		vc = BUFFERS.getBuffer(light(RING));
		for (Blast b : blasts) {
			float age = (float) (now - b.start);
			at(m, cp, b.pos, 0);
			ring(m, vc, age, 0, 0.3f, 4.2f, 30, b.scale, 1f, 0.85f, 0.6f);
			ring(m, vc, age, 4, 0.6f, 3f, 34, b.scale, 1f, 0.45f, 0.15f);
			ring(m, vc, age, 8, 24f, 1.1f, 40, b.scale, 0.9f, 0.5f, 0.3f);
			m.pop();
		}
		vc = BUFFERS.getBuffer(light(GLOW));
		for (Charge c : charges) {
			float f = (float) ((now - c.start) / c.duration);
			at(m, cp, c.pos, 1.5);
			billboard(m, cam, vc, 1.5f + f * f * 5f, 1f, 0.25f, 0.12f, Math.min(1, f * 2), true);
			m.pop();
		}
		for (Blast b : blasts) {
			float age = (float) (now - b.start);
			float s = b.scale;
			if (age < 60) {
				// white-hot core cooling down to deep red
				float grow = Math.min(1, age / 5f);
				float fade = age < 10 ? 1 : 1 - (age - 10) / 50f;
				float t = Math.min(1, age / 30f);
				at(m, cp, b.pos, 4 + age * 0.25f);
				billboard(m, cam, vc, (10 + age * 0.35f) * s * grow, 1f, 1f - 0.55f * t, 0.8f - 0.75f * t, fade, true);
				if (age < 8) billboard(m, cam, vc, 60 * s, 1f, 0.9f, 0.75f, 1 - age / 8f, true);
				m.pop();
			}
			float crater = age < 6 ? age / 6f : 1 - (age - 6) / (BLAST_LIFE - 6f);
			at(m, cp, b.pos, 0);
			flat(m, vc, 11 * s, 0.1f, 0, 1f, 0.35f, 0.05f, crater * (0.75f + 0.25f * MathHelper.sin(age * 0.5f)), true);
			m.pop();
		}
		BUFFERS.draw();
	}

	/** Pushes a matrix placed at {@code pos} (plus {@code dy}) relative to the camera; pop it afterwards. */
	private static void at(MatrixStack m, Vec3d cp, Vec3d pos, double dy) {
		m.push();
		m.translate(pos.x - cp.x, pos.y + dy - cp.y, pos.z - cp.z);
	}

	/** Three stacked magic circles turning against each other; they light up one after another. */
	private static void circles(MatrixStack m, Vec3d cp, VertexConsumer vc, Charge c, double now, boolean additive) {
		float f = (float) ((now - c.start) / c.duration);
		float grow = Math.min(1, f * 3.5f);
		float pulse = 0.85f + 0.15f * MathHelper.sin((float) now * (0.4f + f));
		float flicker = f > 0.85f ? 0.7f + 0.3f * RAND.nextFloat() : 1;
		float a = Math.min(1, f * 4) * pulse * flicker;
		float rot = (float) now * (0.02f + f * 0.06f);
		float mid = MathHelper.clamp((f - 0.2f) * 4, 0, 1);
		float top = MathHelper.clamp((f - 0.45f) * 4, 0, 1);
		at(m, cp, c.pos, 0);
		if (additive) {
			flat(m, vc, c.radius * grow, 0.07f, rot, 1f, 0.35f, 0.15f, a * 0.6f, true);
			flat(m, vc, c.radius * grow * 1.06f, 0.09f, -rot * 0.5f, 1f, 0.45f, 0.1f, a * 0.3f, true);
			if (mid > 0) flat(m, vc, c.radius * 0.7f * mid, 4.5f, -rot * 1.6f, 1f, 0.35f, 0.15f, a * mid * 0.5f, true);
			if (top > 0) flat(m, vc, c.radius * 0.45f * top, 9f, rot * 2.4f, 1f, 0.5f, 0.2f, a * top * 0.5f, true);
		} else {
			flat(m, vc, c.radius * grow, 0.06f, rot, 0.9f, 0.05f, 0.04f, a, false);
			if (mid > 0) flat(m, vc, c.radius * 0.7f * mid, 4.5f, -rot * 1.6f, 0.9f, 0.06f, 0.05f, a * mid, false);
			if (top > 0) flat(m, vc, c.radius * 0.45f * top, 9f, rot * 2.4f, 0.95f, 0.15f, 0.05f, a * top, false);
		}
		m.pop();
	}

	/** The mushroom cloud: a stem, a rolling cap and a dust skirt of soft puffs, fire-lit at first, then soot. */
	private static void cloud(MatrixStack m, Camera cam, Vec3d cp, VertexConsumer vc, Blast b, float age) {
		if (age < 4) return;
		float s = b.scale;
		float capY = capY(age, s), capR = capR(age, s);
		float skirtR = (3 + 18 * (1 - (float) Math.exp(-age / 16f))) * s;
		float heat = MathHelper.clamp((age - 6) / 45f, 0, 1);
		float in = MathHelper.clamp((age - 4) / 6f, 0, 1);
		float out = MathHelper.clamp((BLAST_LIFE - age) / 45f, 0, 1);
		float grow = 1 + age * 0.006f;
		at(m, cp, b.pos, 0);
		for (Puff p : b.puffs) {
			float cs = MathHelper.cos(p.angle), sn = MathHelper.sin(p.angle);
			float x, y, z, size, alpha = 0.92f * in * out, fire = 1 - heat;
			switch (p.kind) {
				case Puff.STEM -> {
					float d = p.dist * s * (1 + 0.4f * p.height);
					x = cs * d;
					z = sn * d;
					y = p.height * capY;
					size = p.size * s * (0.8f + 0.5f * p.height) * grow;
					fire = 1 - heat * (0.6f + 0.3f * p.height);
				}
				case Puff.CAP, Puff.TOP -> {
					x = cs * p.dist * capR;
					z = sn * p.dist * capR;
					y = capY + p.height * s;
					size = Math.max(2.5f, p.size * capR) * grow;
					// the underside of the cap keeps glowing longer
					if (p.kind == Puff.CAP && p.height < 0) fire = 1 - heat * 0.7f;
				}
				default -> {
					x = cs * p.dist * skirtR;
					z = sn * p.dist * skirtR;
					y = p.height * s;
					size = p.size * s * grow;
					alpha *= 0.7f * MathHelper.clamp((100 - age) / 40f, 0, 1);
				}
			}
			float r = MathHelper.lerp(fire, 0.27f, 1f), g = MathHelper.lerp(fire, 0.24f, 0.55f), bl = MathHelper.lerp(fire, 0.23f, 0.16f);
			puff(m, cam, vc, x, y, z, size, p.angle + p.spin * age, p.tile, r, g, bl, alpha);
		}
		m.pop();
	}

	private static void ring(MatrixStack m, VertexConsumer vc, float age, float delay, float y, float speed, float life, float scale, float r, float g, float b) {
		float t = age - delay;
		if (t < 0 || t > life) return;
		float radius = (2 + t * speed) * scale;
		float a = (1 - t / life);
		flat(m, vc, radius, y, 0, r, g, b, a * a, true);
	}

	/**
	 * A horizontal textured square of half-size {@code r}, turned by {@code angle}. Additive quads get the alpha
	 * folded into their colour and both windings (the eyes layer culls back faces); blended ones keep it as alpha.
	 */
	private static void flat(MatrixStack m, VertexConsumer vc, float r, float y, float angle, float red, float green, float blue, float alpha, boolean additive) {
		if (alpha <= 0.01f || r <= 0.01f) return;
		Matrix4f pos = m.peek().getPositionMatrix();
		Matrix3f nrm = m.peek().getNormalMatrix();
		float cs = MathHelper.cos(angle) * r, sn = MathHelper.sin(angle) * r;
		float[] xs = {-cs + sn, cs + sn, cs - sn, -cs - sn};
		float[] zs = {-sn - cs, sn - cs, sn + cs, -sn + cs};
		float[] us = {0, 1, 1, 0}, vs = {0, 0, 1, 1};
		float k = MathHelper.clamp(alpha, 0, 1);
		float cr = additive ? red * k : red, cg = additive ? green * k : green, cb = additive ? blue * k : blue, ca = additive ? 1 : k;
		for (int i = 0; i < 4; i++) vert(vc, pos, nrm, xs[i], y, zs[i], us[i], vs[i], cr, cg, cb, ca);
		if (additive) for (int i = 3; i >= 0; i--) vert(vc, pos, nrm, xs[i], y, zs[i], us[i], vs[i], cr, cg, cb, ca);
	}

	/** A square of half-size {@code r} always facing the camera. */
	private static void billboard(MatrixStack m, Camera cam, VertexConsumer vc, float r, float red, float green, float blue, float alpha, boolean additive) {
		if (alpha <= 0.01f || r <= 0.01f) return;
		m.push();
		m.multiply(cam.getRotation());
		Matrix4f pos = m.peek().getPositionMatrix();
		Matrix3f nrm = m.peek().getNormalMatrix();
		float k = MathHelper.clamp(alpha, 0, 1);
		float cr = additive ? red * k : red, cg = additive ? green * k : green, cb = additive ? blue * k : blue, ca = additive ? 1 : k;
		vert(vc, pos, nrm, -r, -r, 0, 0, 1, cr, cg, cb, ca);
		vert(vc, pos, nrm, r, -r, 0, 1, 1, cr, cg, cb, ca);
		vert(vc, pos, nrm, r, r, 0, 1, 0, cr, cg, cb, ca);
		vert(vc, pos, nrm, -r, r, 0, 0, 0, cr, cg, cb, ca);
		if (additive) {
			vert(vc, pos, nrm, -r, r, 0, 0, 0, cr, cg, cb, ca);
			vert(vc, pos, nrm, r, r, 0, 1, 0, cr, cg, cb, ca);
			vert(vc, pos, nrm, r, -r, 0, 1, 1, cr, cg, cb, ca);
			vert(vc, pos, nrm, -r, -r, 0, 0, 1, cr, cg, cb, ca);
		}
		m.pop();
	}

	/** One smoke puff from the 2x2 atlas, facing the camera and slowly rolling. */
	private static void puff(MatrixStack m, Camera cam, VertexConsumer vc, float x, float y, float z, float r, float roll, int tile, float red, float green, float blue, float alpha) {
		if (alpha <= 0.01f || r <= 0.01f) return;
		m.push();
		m.translate(x, y, z);
		m.multiply(cam.getRotation());
		m.multiply(RotationAxis.POSITIVE_Z.rotation(roll));
		Matrix4f pos = m.peek().getPositionMatrix();
		Matrix3f nrm = m.peek().getNormalMatrix();
		float u0 = (tile & 1) * 0.5f, v0 = (tile >> 1) * 0.5f, u1 = u0 + 0.5f, v1 = v0 + 0.5f;
		float a = MathHelper.clamp(alpha, 0, 1);
		vert(vc, pos, nrm, -r, -r, 0, u0, v1, red, green, blue, a);
		vert(vc, pos, nrm, r, -r, 0, u1, v1, red, green, blue, a);
		vert(vc, pos, nrm, r, r, 0, u1, v0, red, green, blue, a);
		vert(vc, pos, nrm, -r, r, 0, u0, v0, red, green, blue, a);
		m.pop();
	}

	/** A vertical column of light with a scrolling beacon texture, fading towards the top. */
	private static void beam(MatrixStack m, VertexConsumer vc, float width, float height, float time, float red, float green, float blue, float alpha, boolean additive) {
		if (alpha <= 0.01f || width <= 0.01f) return;
		Matrix4f pos = m.peek().getPositionMatrix();
		Matrix3f nrm = m.peek().getNormalMatrix();
		float k = MathHelper.clamp(alpha, 0, 1);
		float cr = additive ? red * k : red, cg = additive ? green * k : green, cb = additive ? blue * k : blue;
		float ca = additive ? 1 : k, topA = additive ? 1 : k * 0.2f, top = additive ? 0.2f : 1;
		float v0 = -time * 0.08f, v1 = v0 + height / 6f;
		int sides = 8;
		for (int i = 0; i < sides; i++) {
			float a0 = (float) (i * Math.PI * 2 / sides), a1 = (float) ((i + 1) * Math.PI * 2 / sides);
			float x0 = MathHelper.cos(a0) * width, z0 = MathHelper.sin(a0) * width;
			float x1 = MathHelper.cos(a1) * width, z1 = MathHelper.sin(a1) * width;
			float u0 = (float) i / sides, u1 = (float) (i + 1) / sides;
			vert(vc, pos, nrm, x0, 0, z0, u0, v1, cr, cg, cb, ca);
			vert(vc, pos, nrm, x1, 0, z1, u1, v1, cr, cg, cb, ca);
			vert(vc, pos, nrm, x1, height, z1, u1, v0, cr * top, cg * top, cb * top, topA);
			vert(vc, pos, nrm, x0, height, z0, u0, v0, cr * top, cg * top, cb * top, topA);
			if (additive) {
				vert(vc, pos, nrm, x0, height, z0, u0, v0, cr * top, cg * top, cb * top, topA);
				vert(vc, pos, nrm, x1, height, z1, u1, v0, cr * top, cg * top, cb * top, topA);
				vert(vc, pos, nrm, x1, 0, z1, u1, v1, cr, cg, cb, ca);
				vert(vc, pos, nrm, x0, 0, z0, u0, v1, cr, cg, cb, ca);
			}
		}
	}

	private static void vert(VertexConsumer vc, Matrix4f pos, Matrix3f nrm, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
		vc.vertex(pos, x, y, z).color(r, g, b, a).texture(u, v).overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(nrm, 0, 1, 0).next();
	}
}
