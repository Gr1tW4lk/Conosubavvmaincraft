package dev.konocraft.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Ancient guardian golems that pour out of the Mobile Fortress Destroyer. */
public class DestroyerGolemEntity extends KonoMonsterEntity {
	public DestroyerGolemEntity(EntityType<? extends DestroyerGolemEntity> type, World world) {
		super(type, world);
		experiencePoints = 12;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 60)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8)
			.add(EntityAttributes.GENERIC_ARMOR, 10)
			.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40);
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit && target instanceof LivingEntity le) {
			le.addVelocity(0, 0.45, 0);
		}
		return hit;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_IRON_GOLEM_REPAIR;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_IRON_GOLEM_DEATH;
	}

	@Override
	protected void playStepSound(net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
		playSound(SoundEvents.ENTITY_IRON_GOLEM_STEP, 0.8f, 1.2f);
	}

	@Override
	protected void dropLoot(DamageSource source, boolean causedByPlayer) {
		super.dropLoot(source, causedByPlayer);
		dropStack(new ItemStack(Items.IRON_INGOT, 1 + random.nextInt(2)));
		if (random.nextInt(3) == 0) dropStack(new ItemStack(Items.REDSTONE, 2 + random.nextInt(3)));
	}
}
