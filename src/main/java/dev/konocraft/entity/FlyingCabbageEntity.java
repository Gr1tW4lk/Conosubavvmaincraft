package dev.konocraft.entity;

import dev.konocraft.registry.ModItems;
import dev.konocraft.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/**
 * In this world cabbages fly. At harvest time they take off rather than be eaten, ram anyone who
 * tries to catch them, and give an unreasonable amount of experience when caught.
 */
public class FlyingCabbageEntity extends PathAwareEntity {
	private Vec3d goal;
	private int dashTicks;
	private int retarget;

	public FlyingCabbageEntity(EntityType<? extends FlyingCabbageEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
		experiencePoints = 5;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 6)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
			.add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2);
	}

	@Override
	protected void initGoals() {
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		setNoGravity(true);
		if (getWorld().isClient) {
			if (random.nextInt(4) == 0) getWorld().addParticle(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.3, getZ(), 0, 0, 0);
			return;
		}
		if (age > 20 * 60 * 6 && !hasCustomName()) {
			discard();
			return;
		}
		PlayerEntity near = getWorld().getClosestPlayer(this, 10);
		if (dashTicks > 0) {
			dashTicks--;
			if (near != null && getBoundingBox().expand(0.3).intersects(near.getBoundingBox())) {
				near.damage(getDamageSources().mobAttack(this), 2);
				Vec3d away = getPos().subtract(near.getPos()).normalize();
				setVelocity(away.x * 0.8, 0.4, away.z * 0.8);
				dashTicks = 0;
				goal = getPos().add(away.multiply(8)).add(0, 3, 0);
				getWorld().playSound(null, getBlockPos(), SoundEvents.BLOCK_GRASS_BREAK, SoundCategory.NEUTRAL, 1f, 1.5f);
			}
		} else if (near != null && random.nextInt(60) == 0 && !near.isCreative()) {
			// Charge!
			Vec3d dir = near.getEyePos().subtract(getPos()).normalize();
			setVelocity(dir.multiply(0.7));
			dashTicks = 20;
			getWorld().playSound(null, getBlockPos(), ModSounds.CABBAGE_WHOOSH, SoundCategory.NEUTRAL, 1f, 1.2f);
			return;
		}
		if (dashTicks > 0) return;
		if (goal == null || --retarget <= 0 || squaredDistanceTo(goal) < 1.5) {
			retarget = 40 + random.nextInt(60);
			int x = MathHelper.floor(getX() + random.nextInt(17) - 8);
			int z = MathHelper.floor(getZ() + random.nextInt(17) - 8);
			int ground = getWorld().getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
			goal = new Vec3d(x + 0.5, ground + 1.5 + random.nextDouble() * 5, z + 0.5);
			// flee from players that come too close
			if (near != null && near.squaredDistanceTo(this) < 16) {
				Vec3d away = getPos().subtract(near.getPos()).normalize().multiply(9);
				goal = getPos().add(away.x, 2, away.z);
			}
		}
		Vec3d to = goal.subtract(getPos());
		Vec3d v = getVelocity().multiply(0.85).add(to.normalize().multiply(0.045));
		// little bobbing wobble
		v = v.add(0, Math.sin(age * 0.3) * 0.01, 0);
		setVelocity(v);
		float yaw = (float) (MathHelper.atan2(v.z, v.x) * MathHelper.DEGREES_PER_RADIAN) - 90f;
		setYaw(yaw);
		bodyYaw = yaw;
		if (age % 50 == 0 && random.nextBoolean()) {
			getWorld().playSound(null, getBlockPos(), ModSounds.CABBAGE_WHOOSH, SoundCategory.NEUTRAL, 0.6f, 1.3f + random.nextFloat() * 0.3f);
		}
	}

	@Override
	public void travel(Vec3d movementInput) {
		move(net.minecraft.entity.MovementType.SELF, getVelocity());
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return distanceSquared > 96 * 96;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BLOCK_GRASS_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLOCK_GRASS_BREAK;
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (!getWorld().isClient) {
			((ServerWorld) getWorld()).spawnParticles(ParticleTypes.COMPOSTER, getX(), getY() + 0.3, getZ(), 15, 0.3, 0.3, 0.3, 0.1);
		}
	}

	@Override
	protected void dropLoot(DamageSource source, boolean causedByPlayer) {
		dropStack(new ItemStack(ModItems.CABBAGE));
	}
}
