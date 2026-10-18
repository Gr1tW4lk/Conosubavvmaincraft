package dev.konocraft.story;

import dev.konocraft.entity.CompanionEntity;
import dev.konocraft.entity.GiantToadEntity;
import dev.konocraft.fx.Fx;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.registry.ModSounds;
import dev.konocraft.util.Scheduler;
import dev.konocraft.world.KonoWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.Monster;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.List;

/** Scripted moments used by dialogues: explosions, toads swallowing goddesses, alarms. */
public final class Cutscenes {
	private Cutscenes() {
	}

	public static void alarm(MinecraftServer server, String text) {
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), ModSounds.ALARM, SoundCategory.MASTER, 1.5f, 1f);
			dev.konocraft.net.ServerNet.sendTitle(p, Text.literal("ЭКСТРЕННЫЙ КВЕСТ!").formatted(Formatting.RED, Formatting.BOLD),
				Text.literal(text).formatted(Formatting.YELLOW), 10, 60, 20);
			p.sendMessage(Text.literal("[Объявление гильдии] " + text).formatted(Formatting.RED), false);
		}
	}

	public static void swallow(ServerPlayerEntity player, String key) {
		Entity e = Npcs.find(player.getServer(), key);
		if (!(e instanceof CompanionEntity companion)) return;
		ServerWorld world = (ServerWorld) e.getWorld();
		List<GiantToadEntity> toads = world.getEntitiesByClass(GiantToadEntity.class, e.getBoundingBox().expand(30), t -> t.isAlive() && !t.hasVictim());
		GiantToadEntity toad;
		if (toads.isEmpty()) {
			toad = ModEntities.GIANT_TOAD.create(world);
			if (toad == null) return;
			Vec3d p = groundAt(world, e.getX() + 4, e.getZ() + 3);
			toad.refreshPositionAndAngles(p.x, p.y, p.z, 0, 0);
			toad.initialize(world, world.getLocalDifficulty(toad.getBlockPos()), SpawnReason.EVENT, null, null);
			world.spawnEntity(toad);
			Fx.play(player, "smoke", p, null);
		} else {
			toad = toads.get(0);
			for (GiantToadEntity t : toads) if (t.squaredDistanceTo(e) < toad.squaredDistanceTo(e)) toad = t;
			toad.requestTeleport(e.getX() + 2, e.getY(), e.getZ() + 2);
		}
		toad.swallow(companion);
	}

	public static Vec3d groundAt(ServerWorld world, double x, double z) {
		int bx = MathHelper.floor(x), bz = MathHelper.floor(z);
		int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, bx, bz);
		return new Vec3d(bx + 0.5, y, bz + 0.5);
	}

	public static void spawnToads(ServerPlayerEntity player, int count, double distance, DialogueManager.Session session) {
		ServerWorld world = KonoWorld.get(player.getServer());
		if (world == null) return;
		Vec3d center = session != null && session.target != null ? session.target : Actions.resolve(player, "ahead " + distance, session);
		for (int i = 0; i < count; i++) {
			GiantToadEntity toad = ModEntities.GIANT_TOAD.create(world);
			if (toad == null) continue;
			double a = i * Math.PI * 2 / count;
			Vec3d p = groundAt(world, center.x + Math.cos(a) * 4, center.z + Math.sin(a) * 4);
			toad.refreshPositionAndAngles(p.x, p.y, p.z, world.random.nextFloat() * 360, 0);
			toad.initialize(world, world.getLocalDifficulty(toad.getBlockPos()), SpawnReason.EVENT, null, null);
			world.spawnEntity(toad);
		}
	}

	/** "megumin_explosion <target spec> [delay]" - Megumin chants and blows the target up. */
	public static void meguminExplosion(ServerPlayerEntity player, DialogueManager.Session session, String arg) {
		String[] p = arg.split(" ");
		int delay = 60;
		String spec = arg;
		if (p.length >= 2) {
			try {
				delay = Integer.parseInt(p[p.length - 1]);
				spec = arg.substring(0, arg.lastIndexOf(' '));
			} catch (NumberFormatException ignored) {
			}
		}
		Vec3d target = spec.isEmpty() ? (session != null ? session.target : null) : Actions.resolve(player, spec, session);
		if (target == null) target = Actions.resolve(player, "ahead 28", session);
		final Vec3d t = target;
		ServerWorld world = player.getServerWorld();
		Entity m = Npcs.find(player.getServer(), "megumin");
		if (m instanceof CompanionEntity megumin) {
			megumin.castPose(delay + 10, t);
		}
		Fx.magicCircle(world, t, 6.5, delay, Fx.RED);
		Scheduler.after(delay, () -> {
			explosionDamage(world, t, 10, 120f, player);
			Fx.explosion(world, t, 1.2f);
			Entity m2 = Npcs.find(player.getServer(), "megumin");
			if (m2 instanceof CompanionEntity megumin) megumin.collapse(1600);
		});
	}

	/** Damages everything hostile in the blast radius. Friends and players are never harmed. */
	public static void explosionDamage(ServerWorld world, Vec3d center, double radius, float damage, ServerPlayerEntity source) {
		Box box = new Box(center.subtract(radius, radius, radius), center.add(radius, radius + 4, radius));
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, le -> le.isAlive())) {
			if (e instanceof ServerPlayerEntity || e instanceof dev.konocraft.entity.KonoNpcEntity) continue;
			double d = Math.sqrt(e.squaredDistanceTo(center));
			if (d > radius) continue;
			float amount = (float) (damage * (1 - d / (radius * 1.5)));
			e.damage(source != null ? world.getDamageSources().indirectMagic(source, source) : world.getDamageSources().magic(), amount);
			Vec3d push = e.getPos().subtract(center).normalize().multiply(1.6);
			e.addVelocity(push.x, 0.8, push.z);
		}
	}

	public static void killHostilesNear(ServerPlayerEntity player, double radius) {
		ServerWorld world = player.getServerWorld();
		for (Entity e : world.getOtherEntities(player, player.getBoundingBox().expand(radius), x -> x instanceof Monster || x instanceof GiantToadEntity)) {
			if (e instanceof dev.konocraft.entity.BossEntity) continue;
			e.discard();
		}
	}
}
