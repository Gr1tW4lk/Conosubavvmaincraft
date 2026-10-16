package dev.konocraft.entity;

import dev.konocraft.fx.Fx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/** The glowing coronatite core on the Destroyer's back. It overheats in pulses - do not stand too close. */
public class DestroyerCoreEntity extends PathAwareEntity implements BossPart {
	private UUID ownerId;
	private DestroyerEntity ownerCache;
	private int pulse = 100;

	public DestroyerCoreEntity(EntityType<? extends DestroyerCoreEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
		setPersistent();
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 260)
			.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
			.add(EntityAttributes.GENERIC_ARMOR, 6);
	}

	public void attach(DestroyerEntity owner) {
		ownerId = owner.getUuid();
		ownerCache = owner;
	}

	@Override
	public Entity owner() {
		if (ownerCache != null && !ownerCache.isRemoved()) return ownerCache;
		if (ownerId != null && getWorld() instanceof ServerWorld w && w.getEntity(ownerId) instanceof DestroyerEntity d) ownerCache = d;
		return ownerCache;
	}

	@Override
	protected void initGoals() {
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
	public void tick() {
		super.tick();
		setVelocity(Vec3d.ZERO);
		if (getWorld().isClient) {
			getWorld().addParticle(ParticleTypes.FLAME, getX() + (random.nextDouble() - 0.5), getY() + 1 + random.nextDouble(), getZ() + (random.nextDouble() - 0.5), 0, 0.05, 0);
			return;
		}
		ServerWorld w = (ServerWorld) getWorld();
		if (!(owner() instanceof DestroyerEntity d) || d.isRemoved()) {
			if (age > 40) discard();
			return;
		}
		setPosition(d.getX(), d.getY() + DestroyerEntity.DECK_HEIGHT + 4.2 * (1 - d.getSink()), d.getZ() + 6);
		d.setCoreHealth(getHealth() / getMaxHealth());
		pulse--;
		if (pulse == 25) {
			w.playSound(null, getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 3f, 0.5f);
			for (int k = 0; k < 40; k++) {
				double a = k * Math.PI * 2 / 40;
				w.spawnParticles(Fx.CRIMSON, getX() + Math.cos(a) * 5, getY() + 0.2, getZ() + Math.sin(a) * 5, 1, 0, 0, 0, 0);
			}
		}
		if (pulse <= 0) {
			pulse = 100;
			w.spawnParticles(ParticleTypes.FLAME, getX(), getY() + 1, getZ(), 200, 2.5, 0.8, 2.5, 0.15);
			w.playSound(null, getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3f, 0.4f);
			for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(5, 2, 5), e -> e instanceof PlayerEntity || e instanceof CompanionEntity)) {
				e.damage(getDamageSources().onFire(), 6);
				e.setOnFireFor(3);
				Vec3d push = e.getPos().subtract(getPos()).multiply(1, 0, 1).normalize();
				e.addVelocity(push.x, 0.4, push.z);
			}
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source.getAttacker() instanceof DestroyerEntity || source.getAttacker() instanceof DestroyerGolemEntity) return false;
		if (source.isOf(net.minecraft.entity.damage.DamageTypes.ON_FIRE) || source.isOf(net.minecraft.entity.damage.DamageTypes.IN_FIRE)) return false;
		return super.damage(source, amount);
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (!getWorld().isClient && owner() instanceof DestroyerEntity d) d.onCoreDestroyed();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BLOCK_ANVIL_LAND;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_GENERIC_EXPLODE;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (ownerId != null) nbt.putUuid("Owner", ownerId);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.containsUuid("Owner")) ownerId = nbt.getUuid("Owner");
	}
}
