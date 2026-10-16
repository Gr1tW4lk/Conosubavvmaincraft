package dev.konocraft.entity;

import dev.konocraft.fx.Fx;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** A glowing magic projectile: the Snipe skill, Beldia's dark bolts and the Destroyer's turrets. */
public class MagicBoltEntity extends ThrownItemEntity {
	private float damage = 6;
	private boolean homing;
	private boolean hostile;

	public MagicBoltEntity(EntityType<? extends MagicBoltEntity> type, World world) {
		super(type, world);
	}

	public MagicBoltEntity(World world, LivingEntity owner, float damage) {
		super(ModEntities.MAGIC_BOLT, owner, world);
		this.damage = damage;
		this.hostile = !(owner instanceof PlayerEntity) && !(owner instanceof CompanionEntity);
		setItem(new ItemStack(hostile ? ModItems.DARK_BOLT : ModItems.MAGIC_BOLT));
	}

	public MagicBoltEntity(World world, double x, double y, double z, float damage) {
		super(ModEntities.MAGIC_BOLT, x, y, z, world);
		this.damage = damage;
		this.hostile = true;
		setItem(new ItemStack(ModItems.DARK_BOLT));
	}

	public void setHoming(boolean homing) {
		this.homing = homing;
	}

	public boolean isHostile() {
		return hostile;
	}

	@Override
	protected Item getDefaultItem() {
		return hostile ? ModItems.DARK_BOLT : ModItems.MAGIC_BOLT;
	}

	@Override
	protected float getGravity() {
		return 0f;
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			getWorld().addParticle(getItem().isOf(ModItems.DARK_BOLT) ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
			return;
		}
		if (age > 120) {
			discard();
			return;
		}
		if (homing && age > 3) {
			LivingEntity target = null;
			double best = 20 * 20;
			for (LivingEntity e : getWorld().getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(16), CompanionEntity::isEnemy)) {
				double d = e.squaredDistanceTo(this);
				if (d < best) {
					best = d;
					target = e;
				}
			}
			if (target != null) {
				Vec3d to = target.getPos().add(0, target.getHeight() / 2, 0).subtract(getPos()).normalize();
				Vec3d v = getVelocity();
				double speed = v.length();
				setVelocity(v.normalize().multiply(0.7).add(to.multiply(0.3)).normalize().multiply(speed));
			}
		}
	}

	@Override
	protected boolean canHit(Entity entity) {
		if (!super.canHit(entity)) return false;
		if (hostile) return !(entity instanceof GiantToadEntity) && !(entity instanceof KonoMonsterEntity) && !(entity instanceof BossEntity) && !(entity instanceof BossPart);
		return !(entity instanceof PlayerEntity) && !(entity instanceof KonoNpcEntity);
	}

	@Override
	protected void onEntityHit(EntityHitResult hit) {
		super.onEntityHit(hit);
		Entity owner = getOwner();
		hit.getEntity().damage(getDamageSources().indirectMagic(this, owner), damage);
	}

	@Override
	protected void onCollision(HitResult hit) {
		super.onCollision(hit);
		if (!getWorld().isClient) {
			ServerWorld w = (ServerWorld) getWorld();
			w.spawnParticles(hostile ? Fx.PURPLE : Fx.GOLD, getX(), getY(), getZ(), 20, 0.3, 0.3, 0.3, 0);
			w.spawnParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.2);
			discard();
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putFloat("BoltDamage", damage);
		nbt.putBoolean("Homing", homing);
		nbt.putBoolean("Hostile", hostile);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		damage = nbt.getFloat("BoltDamage");
		homing = nbt.getBoolean("Homing");
		hostile = nbt.getBoolean("Hostile");
	}
}
