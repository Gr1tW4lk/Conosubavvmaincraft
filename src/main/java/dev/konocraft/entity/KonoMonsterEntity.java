package dev.konocraft.entity;

import dev.konocraft.registry.ModEntities;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

/** Shared base for simple melee monsters: goblins, undead knights and the Destroyer's golems. */
public abstract class KonoMonsterEntity extends HostileEntity {
	protected KonoMonsterEntity(EntityType<? extends KonoMonsterEntity> type, World world) {
		super(type, world);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(2, new MeleeAttackGoal(this, 1.1, false));
		goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 10f));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		targetSelector.add(3, new ActiveTargetGoal<>(this, CompanionEntity.class, true, e -> !((CompanionEntity) e).isDown()));
	}

	@Override
	public boolean isDisallowedInPeaceful() {
		return false;
	}

	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, NbtCompound entityNbt) {
		EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
		equip();
		return data;
	}

	protected void equip() {
	}

	// ---------------------------------------------------------------------------------------------

	public static class Goblin extends KonoMonsterEntity {
		public Goblin(EntityType<? extends Goblin> type, World world) {
			super(type, world);
			experiencePoints = 4;
		}

		public static DefaultAttributeContainer.Builder createAttributes() {
			return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 16)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.33)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24);
		}

		@Override
		protected void equip() {
			equipStack(EquipmentSlot.MAINHAND, new ItemStack(random.nextBoolean() ? Items.WOODEN_SWORD : Items.STONE_AXE));
		}

		@Override
		protected SoundEvent getAmbientSound() {
			return SoundEvents.ENTITY_PIGLIN_AMBIENT;
		}

		@Override
		protected SoundEvent getHurtSound(DamageSource source) {
			return SoundEvents.ENTITY_PIGLIN_HURT;
		}

		@Override
		protected SoundEvent getDeathSound() {
			return SoundEvents.ENTITY_PIGLIN_DEATH;
		}

		@Override
		public float getSoundPitch() {
			return 1.6f;
		}

		@Override
		protected void dropLoot(DamageSource source, boolean causedByPlayer) {
			super.dropLoot(source, causedByPlayer);
			if (random.nextInt(3) == 0) dropStack(new ItemStack(Items.GOLD_NUGGET, 1 + random.nextInt(3)));
		}
	}

	public static class UndeadKnight extends KonoMonsterEntity {
		public UndeadKnight(EntityType<? extends UndeadKnight> type, World world) {
			super(type, world);
			experiencePoints = 8;
		}

		public static DefaultAttributeContainer.Builder createAttributes() {
			return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 32)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6)
				.add(EntityAttributes.GENERIC_ARMOR, 8)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
		}

		@Override
		public EntityGroup getGroup() {
			return EntityGroup.UNDEAD;
		}

		@Override
		protected void equip() {
			equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		}

		@Override
		public boolean damage(DamageSource source, float amount) {
			// Holy water and purification hurt them a lot more
			if (source.getAttacker() instanceof AquaEntity) amount *= 1.5f;
			return super.damage(source, amount);
		}

		@Override
		protected SoundEvent getAmbientSound() {
			return SoundEvents.ENTITY_SKELETON_AMBIENT;
		}

		@Override
		protected SoundEvent getHurtSound(DamageSource source) {
			return SoundEvents.ENTITY_SKELETON_HURT;
		}

		@Override
		protected SoundEvent getDeathSound() {
			return SoundEvents.ENTITY_SKELETON_DEATH;
		}

		@Override
		protected void dropLoot(DamageSource source, boolean causedByPlayer) {
			super.dropLoot(source, causedByPlayer);
			dropStack(new ItemStack(Items.BONE, 1 + random.nextInt(2)));
			if (random.nextInt(4) == 0) dropStack(new ItemStack(Items.IRON_NUGGET, 2 + random.nextInt(4)));
		}
	}

	public static boolean isMonster(EntityType<?> type) {
		return type == ModEntities.GOBLIN || type == ModEntities.UNDEAD_KNIGHT || type == ModEntities.DESTROYER_GOLEM;
	}
}
