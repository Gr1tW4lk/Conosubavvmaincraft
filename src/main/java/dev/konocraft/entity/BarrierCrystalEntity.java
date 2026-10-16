package dev.konocraft.entity;

import dev.konocraft.fx.Fx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/** Magic crystal on one of the Destroyer's legs. Breaking all four brings the barrier down. */
public class BarrierCrystalEntity extends PathAwareEntity implements BossPart {
	private UUID ownerId;
	private int index;
	private DestroyerEntity ownerCache;

	public BarrierCrystalEntity(EntityType<? extends BarrierCrystalEntity> type, World world) {
		super(type, world);
		setNoGravity(true);
		setPersistent();
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 70)
			.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
			.add(EntityAttributes.GENERIC_ARMOR, 4);
	}

	public void attach(DestroyerEntity owner, int index) {
		this.ownerId = owner.getUuid();
		this.ownerCache = owner;
		this.index = index;
	}

	@Override
	public Entity owner() {
		if (ownerCache != null && !ownerCache.isRemoved()) return ownerCache;
		if (ownerId != null && getWorld() instanceof ServerWorld w) {
			Entity e = w.getEntity(ownerId);
			if (e instanceof DestroyerEntity d) ownerCache = d;
		}
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
			if (random.nextInt(3) == 0) getWorld().addParticle(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 0, 0.2, 0);
			return;
		}
		Entity o = owner();
		if (!(o instanceof DestroyerEntity d) || d.isRemoved()) {
			if (age > 40) discard();
			return;
		}
		Vec3d p = d.crystalPos(index);
		setPosition(p.x, p.y, p.z);
		if (age % 4 == 0) {
			// beam towards the body
			ServerWorld w = (ServerWorld) getWorld();
			Vec3d to = d.getPos().add(0, 6, 0).subtract(getPos().add(0, 1.5, 0));
			for (int i = 1; i < 6; i++) {
				Vec3d q = getPos().add(0, 1.5, 0).add(to.multiply(i / 6.0));
				w.spawnParticles(Fx.PURPLE, q.x, q.y, q.z, 1, 0, 0, 0, 0);
			}
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source.getAttacker() instanceof DestroyerEntity || source.getAttacker() instanceof DestroyerGolemEntity) return false;
		return super.damage(source, amount);
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (!getWorld().isClient && owner() instanceof DestroyerEntity d) d.onCrystalDestroyed();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.BLOCK_AMETHYST_BLOCK_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (ownerId != null) nbt.putUuid("Owner", ownerId);
		nbt.putInt("Index", index);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.containsUuid("Owner")) ownerId = nbt.getUuid("Owner");
		index = nbt.getInt("Index");
	}

	@Override
	public void playAmbientSound() {
		getWorld().playSound(null, getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 1f, 0.6f);
	}
}
