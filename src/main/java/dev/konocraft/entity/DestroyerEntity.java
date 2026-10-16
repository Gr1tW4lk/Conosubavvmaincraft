package dev.konocraft.entity;

import dev.konocraft.fx.Fx;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.story.Bosses;
import dev.konocraft.world.Regions;
import dev.konocraft.world.Terrain;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * The Mobile Fortress Destroyer: a city-sized mechanical spider that tramples everything in its way.
 * Phase 0: walks towards Axel behind a magic barrier fed by four crystals on its legs.
 * Phase 1: barrier broken, legs exploded - it collapses and its coronatite core is exposed on top.
 * Phase 2: core destroyed - the finale cutscene takes over.
 */
public class DestroyerEntity extends PathAwareEntity implements BossEntity {
	public static final int WALKING = 0, BREACHED = 1, COLLAPSED = 2, DYING = 3;
	public static final float WIDTH = 15f;
	/** Height of the hull's deck once the fortress has collapsed. */
	public static final float DECK_HEIGHT = 5.6f;
	private static final TrackedData<Integer> PHASE = DataTracker.registerData(DestroyerEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> CRYSTALS = DataTracker.registerData(DestroyerEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> SINK = DataTracker.registerData(DestroyerEntity.class, TrackedDataHandlerRegistry.FLOAT);

	private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Мобильная крепость Разрушитель").formatted(Formatting.GOLD),
		BossBar.Color.YELLOW, BossBar.Style.PROGRESS);
	private int boltTimer = 60;
	private int golemTimer = 200;
	private int stompTimer;
	private int lonelyTicks;
	private float groundY;
	private int barrierMessage;
	private int creak;

	public DestroyerEntity(EntityType<? extends DestroyerEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
		noClip = true;
		setPersistent();
		ignoreCameraFrustum = true;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 1000)
			.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64);
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(PHASE, WALKING);
		dataTracker.startTracking(CRYSTALS, 4);
		dataTracker.startTracking(SINK, 0f);
	}

	@Override
	protected void initGoals() {
	}

	public int getPhase() {
		return dataTracker.get(PHASE);
	}

	public int getCrystals() {
		return dataTracker.get(CRYSTALS);
	}

	/** 0 = standing on its legs, 1 = fully collapsed on its belly. */
	public float getSink() {
		return dataTracker.get(SINK);
	}

	@Override
	public EntityDimensions getDimensions(EntityPose pose) {
		return EntityDimensions.fixed(WIDTH, getPhase() >= COLLAPSED ? DECK_HEIGHT : 11f);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (PHASE.equals(data)) calculateDimensions();
	}

	@Override
	public boolean isCollidable() {
		return getPhase() >= COLLAPSED;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean cannotDespawn() {
		return true;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (getWorld().isClient) return false;
		if (source.isOf(net.minecraft.entity.damage.DamageTypes.OUT_OF_WORLD) || source.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC_KILL)) {
			return super.damage(source, amount);
		}
		if (source.getAttacker() instanceof PlayerEntity p && --barrierMessage <= 0) {
			barrierMessage = 40;
			String msg = getPhase() == WALKING
				? "Магический барьер отражает удар! Уничтожьте кристаллы на ногах крепости!"
				: "Броня слишком толстая. Бейте по ядру из коронатита на спине!";
			p.sendMessage(Text.literal(msg).formatted(Formatting.LIGHT_PURPLE), true);
		}
		return false;
	}

	public void spawnCrystals() {
		ServerWorld w = (ServerWorld) getWorld();
		for (int i = 0; i < 4; i++) {
			BarrierCrystalEntity c = ModEntities.BARRIER_CRYSTAL.create(w);
			if (c == null) continue;
			c.attach(this, i);
			Vec3d p = crystalPos(i);
			c.refreshPositionAndAngles(p.x, p.y, p.z, 0, 0);
			w.spawnEntity(c);
		}
		dataTracker.set(CRYSTALS, 4);
	}

	/** Crystal positions on the four outer legs. */
	public Vec3d crystalPos(int index) {
		double side = (index % 2 == 0) ? -1 : 1;
		double front = (index < 2) ? -1 : 1;
		Vec3d local = new Vec3d(side * (WIDTH / 2 + 1.2), 0.2, front * 4.5);
		float yaw = getYaw() * MathHelper.RADIANS_PER_DEGREE;
		double x = local.x * MathHelper.cos(yaw) - local.z * MathHelper.sin(yaw);
		double z = local.x * MathHelper.sin(yaw) + local.z * MathHelper.cos(yaw);
		int gy = Terrain.heightAt(MathHelper.floor(getX() + x), MathHelper.floor(getZ() + z)) + 1;
		return new Vec3d(getX() + x, Math.max(gy, Regions.SEA_LEVEL + 1), getZ() + z);
	}

	public void onCrystalDestroyed() {
		int left = Math.max(0, getCrystals() - 1);
		dataTracker.set(CRYSTALS, left);
		ServerWorld w = (ServerWorld) getWorld();
		Fx.barrierBreak(w, getPos());
		for (ServerPlayerEntity p : w.getPlayers()) {
			if (p.squaredDistanceTo(this) < 120 * 120) {
				p.sendMessage(Text.literal(left > 0 ? "Кристалл барьера разрушен! Осталось: " + left : "Все кристаллы разрушены! Барьер слабеет!")
					.formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
			}
		}
		if (left == 0 && getPhase() == WALKING) {
			dataTracker.set(PHASE, BREACHED);
			Bosses.onCrystalsBroken(getServer(), this);
		}
	}

	public void collapse() {
		if (getPhase() >= COLLAPSED) return;
		dataTracker.set(PHASE, COLLAPSED);
		calculateDimensions();
		ServerWorld w = (ServerWorld) getWorld();
		Fx.explosion(w, getPos(), 1.4f);
		buildRamps(w);
		DestroyerCoreEntity core = ModEntities.DESTROYER_CORE.create(w);
		if (core != null) {
			core.attach(this);
			core.refreshPositionAndAngles(getX(), getY() + DECK_HEIGHT, getZ() + 6, 0, 0);
			w.spawnEntity(core);
		}
		bossBar.setName(Text.literal("Ядро Разрушителя").formatted(Formatting.RED));
		bossBar.setColor(BossBar.Color.RED);
	}

	/** Rubble ramps on both sides so adventurers can climb onto the fallen fortress. */
	private void buildRamps(ServerWorld w) {
		int cx = MathHelper.floor(getX()), cz = MathHelper.floor(getZ());
		int gy = MathHelper.floor(groundY);
		for (int side = -1; side <= 1; side += 2) {
			for (int i = 0; i < 6; i++) {
				int x = cx + side * (13 - i);
				int top = gy + i;
				for (int dz = -1; dz <= 1; dz++) {
					for (int y = gy; y <= top; y++) {
						BlockPos p = new BlockPos(x, y, cz + dz);
						BlockState s = y == top ? Blocks.COBBLED_DEEPSLATE_STAIRS.getDefaultState()
							.with(net.minecraft.block.StairsBlock.FACING, side < 0 ? Direction.EAST : Direction.WEST) : Blocks.COBBLED_DEEPSLATE.getDefaultState();
						// the ramps crumble a while after the fight
						dev.konocraft.story.Restoration.record(w, p, w.getBlockState(p), s, 20 * 60 * 10);
						w.setBlockState(p, s);
					}
				}
			}
		}
	}

	public void onCoreDestroyed() {
		if (getPhase() >= DYING) return;
		dataTracker.set(PHASE, DYING);
		bossBar.setPercent(0);
		Bosses.onCoreDestroyed(getServer(), this);
	}

	public void die() {
		ServerWorld w = (ServerWorld) getWorld();
		Fx.explosion(w, getPos(), 2.0f);
		Fx.explosion(w, getPos().add(6, 0, 4), 1.2f);
		Fx.explosion(w, getPos().add(-6, 0, -4), 1.2f);
		bossBar.clearPlayers();
		for (MobEntity m : w.getEntitiesByClass(MobEntity.class, getBoundingBox().expand(40), e -> e instanceof DestroyerGolemEntity)) m.kill();
		discard();
	}

	@Override
	public void tick() {
		super.tick();
		setNoGravity(true);
		noClip = true;
		setVelocity(Vec3d.ZERO);
		if (getWorld().isClient) return;
		ServerWorld w = (ServerWorld) getWorld();
		int phase = getPhase();
		groundY = Terrain.heightAt(MathHelper.floor(getX()), MathHelper.floor(getZ())) + 1;
		if (groundY < Regions.SEA_LEVEL + 1) groundY = Regions.SEA_LEVEL + 1;
		float sink = getSink();
		if (phase >= COLLAPSED && sink < 1f) {
			sink = Math.min(1f, sink + 0.03f);
			dataTracker.set(SINK, sink);
		}
		double y = groundY;
		if (phase == WALKING) {
			setYaw(180);
			bodyYaw = 180;
			headYaw = 180;
			double nz = getZ() - 0.055;
			setPosition(getX(), y, nz);
			trample(w);
			if (getZ() <= Regions.DESTROYER_FAIL_Z) {
				Bosses.onDestroyerFail(getServer());
				return;
			}
			if (++creak % 30 == 0) {
				w.playSound(null, getBlockPos(), SoundEvents.ENTITY_IRON_GOLEM_STEP, SoundCategory.HOSTILE, 8f, 0.4f);
				Fx.shakeAll(w, getPos(), 8, 0.35f);
				w.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 11, getZ(), 3, 1, 0.2, 1, 0.03);
			}
			if (age % 6 == 0) {
				// visible barrier shimmer
				double r = WIDTH * 0.8;
				for (int k = 0; k < 10; k++) {
					double a = random.nextDouble() * Math.PI * 2, b = random.nextDouble() * Math.PI;
					w.spawnParticles(Fx.PURPLE, getX() + Math.cos(a) * Math.sin(b) * r, getY() + 4 + Math.cos(b) * r * 0.6, getZ() + Math.sin(a) * Math.sin(b) * r, 1, 0, 0, 0, 0);
				}
			}
		} else {
			setPosition(getX(), y, getZ());
		}
		// Bossbar
		if (phase == WALKING) {
			double total = Regions.DESTROYER_START_Z - Regions.DESTROYER_FAIL_Z;
			bossBar.setPercent((float) MathHelper.clamp((getZ() - Regions.DESTROYER_FAIL_Z) / total, 0, 1));
			bossBar.setName(Text.literal("Мобильная крепость Разрушитель — кристаллы барьера: " + getCrystals() + "/4").formatted(Formatting.GOLD));
		}
		for (ServerPlayerEntity p : w.getPlayers()) {
			if (p.squaredDistanceTo(this) < 160 * 160) bossBar.addPlayer(p);
			else bossBar.removePlayer(p);
		}
		if (phase == DYING) {
			if (age % 4 == 0) w.spawnParticles(ParticleTypes.LAVA, getX(), getY() + 6, getZ(), 10, 4, 1, 4, 0);
			return;
		}
		// Attacks
		List<ServerPlayerEntity> near = w.getPlayers(p -> p.squaredDistanceTo(this) < 60 * 60 && !p.isCreative() && !p.isSpectator());
		// The fight only resets when everyone has left the southern plain, not while the defenders
		// are still waiting at the barricades for the fortress to arrive.
		boolean watched = !w.getPlayers(p -> p.squaredDistanceTo(this) < 380 * 380 && !p.isSpectator()).isEmpty();
		if (!watched) {
			if (++lonelyTicks > 20 * 45) {
				Bosses.onDestroyerFail(getServer());
				return;
			}
		} else {
			lonelyTicks = 0;
		}
		if (phase == BREACHED) return;
		if (--boltTimer <= 0 && !near.isEmpty()) {
			boltTimer = phase == WALKING ? 50 : 80;
			int shots = phase == WALKING ? 3 : 2;
			for (int i = 0; i < shots; i++) {
				ServerPlayerEntity t = near.get(random.nextInt(near.size()));
				Vec3d from = getPos().add((random.nextDouble() - 0.5) * WIDTH * 0.7, phase == WALKING ? 10.5 : 6.5, (random.nextDouble() - 0.5) * WIDTH * 0.7);
				MagicBoltEntity bolt = new MagicBoltEntity(w, from.x, from.y, from.z, 5);
				bolt.setOwner(this);
				Vec3d dir = t.getEyePos().subtract(from).normalize();
				bolt.setVelocity(dir.x, dir.y, dir.z, 1.0f, 3f);
				w.spawnEntity(bolt);
			}
			w.playSound(null, getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 4f, 0.6f);
		}
		if (--golemTimer <= 0) {
			golemTimer = phase == WALKING ? 260 : 200;
			int golems = w.getEntitiesByClass(DestroyerGolemEntity.class, getBoundingBox().expand(40), e -> true).size();
			for (int i = golems; i < Math.min(golems + 2, 6); i++) {
				double a = random.nextDouble() * Math.PI * 2;
				double gx = getX() + Math.cos(a) * (WIDTH / 2 + 3), gz = getZ() + Math.sin(a) * (WIDTH / 2 + 3);
				int gy = Terrain.heightAt(MathHelper.floor(gx), MathHelper.floor(gz)) + 1;
				Bosses.spawnGolem(w, gx, gy, gz);
				w.spawnParticles(ParticleTypes.LARGE_SMOKE, gx, gy + 1, gz, 20, 0.5, 1, 0.5, 0.02);
			}
		}
		// Stomp anything caught under the fortress
		if (phase == WALKING && --stompTimer <= 0) {
			stompTimer = 10;
			for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, getBoundingBox(), e -> !(e instanceof BossPart) && e != this && e.isAlive())) {
				e.damage(getDamageSources().mobAttack(this), 7);
				Vec3d push = e.getPos().subtract(getPos()).multiply(1, 0, 1).normalize().multiply(1.5);
				e.addVelocity(push.x, 0.4, push.z);
			}
		}
	}

	/** Destroys blocks in front of the walking fortress. */
	private void trample(ServerWorld w) {
		if (age % 5 != 0) return;
		int x1 = MathHelper.floor(getX() - WIDTH / 2), x2 = MathHelper.floor(getX() + WIDTH / 2);
		int z = MathHelper.floor(getZ() - WIDTH / 2) - 1;
		int base = MathHelper.floor(groundY);
		for (int x = x1; x <= x2; x++) {
			for (int y = base; y <= base + 9; y++) {
				BlockPos p = new BlockPos(x, y, z);
				BlockState s = w.getBlockState(p);
				if (s.isAir() || s.isOf(Blocks.BEDROCK) || s.isOf(Blocks.WATER)) continue;
				dev.konocraft.story.Restoration.record(w, p, s, Blocks.AIR.getDefaultState(), 0);
				w.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
				if (random.nextInt(6) == 0) w.spawnParticles(ParticleTypes.POOF, x + 0.5, y + 0.5, z + 0.5, 2, 0.2, 0.2, 0.2, 0.02);
			}
			BlockPos ground = new BlockPos(x, base - 1, z + 2);
			if (w.getBlockState(ground).isOf(Blocks.GRASS_BLOCK) && random.nextInt(3) == 0) {
				dev.konocraft.story.Restoration.record(w, ground, w.getBlockState(ground), Blocks.COARSE_DIRT.getDefaultState(), 0);
				w.setBlockState(ground, Blocks.COARSE_DIRT.getDefaultState());
			}
		}
	}

	@Override
	public void onStoppedTrackingBy(ServerPlayerEntity player) {
		super.onStoppedTrackingBy(player);
		bossBar.removePlayer(player);
	}

	@Override
	public void remove(RemovalReason reason) {
		bossBar.clearPlayers();
		super.remove(reason);
	}

	public void setCoreHealth(float fraction) {
		bossBar.setPercent(fraction);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Phase", getPhase());
		nbt.putInt("Crystals", getCrystals());
		nbt.putFloat("Sink", getSink());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		dataTracker.set(PHASE, nbt.getInt("Phase"));
		dataTracker.set(CRYSTALS, nbt.getInt("Crystals"));
		dataTracker.set(SINK, nbt.getFloat("Sink"));
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 512 * 512;
	}
}
