package dev.konocraft.entity;

import dev.konocraft.registry.ModEntities;
import dev.konocraft.registry.ModSounds;
import dev.konocraft.story.DialogueManager;
import dev.konocraft.story.Party;
import dev.konocraft.story.Story;
import dev.konocraft.story.StoryState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Giant toad: the classic first quest of every Axel adventurer. Slow, sturdy, immune to blunt
 * weapons in spirit, and fond of swallowing goddesses and arch wizards whole.
 */
public class GiantToadEntity extends HostileEntity {
	private LivingEntity victim;
	private int victimTicks;
	private int holdTicks;

	public GiantToadEntity(EntityType<? extends GiantToadEntity> type, World world) {
		super(type, world);
		experiencePoints = 8;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 34)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4)
			.add(EntityAttributes.GENERIC_ARMOR, 2)
			.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24);
	}

	public static DefaultAttributeContainer.Builder createKingAttributes() {
		return createAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 320)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10)
			.add(EntityAttributes.GENERIC_ARMOR, 8)
			.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
	}

	public boolean isKing() {
		return getType() == ModEntities.TOAD_KING;
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.add(5, new WanderAroundFarGoal(this, 0.7));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10f));
		goalSelector.add(7, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this));
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		targetSelector.add(3, new ActiveTargetGoal<>(this, CompanionEntity.class, true, e -> !((CompanionEntity) e).isDown()));
	}

	@Override
	public boolean isDisallowedInPeaceful() {
		return false;
	}

	public boolean hasVictim() {
		return victim != null;
	}

	@Override
	public boolean tryAttack(Entity target) {
		if (victim == null && target instanceof LivingEntity le && (le instanceof PlayerEntity || le instanceof CompanionEntity)
			&& !(le instanceof CompanionEntity c && (c.isDown() || c.isSwallowed()))
			&& random.nextFloat() < (isKing() ? 0.45f : 0.3f)) {
			swallow(le);
			return true;
		}
		return super.tryAttack(target);
	}

	public void swallow(LivingEntity v) {
		if (getWorld().isClient || victim != null) return;
		victim = v;
		victimTicks = 0;
		boolean story = false;
		if (v instanceof CompanionEntity c) {
			c.setSwallowed(true);
			StoryState s = getServer() == null ? null : StoryState.get(getServer());
			story = s != null && (s.stage == Story.CH1_TOADS || s.stage == Story.CH2_TEST);
		}
		holdTicks = v instanceof PlayerEntity ? 100 : (story ? 20 * 90 : 20 * 12);
		setTarget(null);
		getNavigation().stop();
		getWorld().playSound(null, getBlockPos(), ModSounds.TOAD_GULP, SoundCategory.HOSTILE, 1.4f, isKing() ? 0.6f : 1f);
	}

	private Vec3d mouth() {
		float yaw = bodyYaw * MathHelper.RADIANS_PER_DEGREE;
		double f = getWidth() * 0.15;
		return new Vec3d(getX() - MathHelper.sin(yaw) * f, getY() + getHeight() * 0.25, getZ() + MathHelper.cos(yaw) * f);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient || victim == null) return;
		victimTicks++;
		if (!victim.isAlive() || victim.isRemoved() || victim.getWorld() != getWorld() || victimTicks > holdTicks) {
			release();
			return;
		}
		getNavigation().stop();
		setTarget(null);
		Vec3d m = mouth();
		if (victim instanceof ServerPlayerEntity p) {
			if (victimTicks % 4 == 1) p.networkHandler.requestTeleport(m.x, m.y, m.z, p.getYaw(), p.getPitch());
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, false, false));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 5, false, false, false));
			if (victimTicks % 40 == 0) p.damage(getDamageSources().mobAttack(this), isKing() ? 4 : 1.5f);
			if (victimTicks == 2) p.sendMessage(net.minecraft.text.Text.literal("Вас проглотила гигантская жаба! Бейте её изнутри!").formatted(net.minecraft.util.Formatting.GREEN), true);
		} else {
			victim.refreshPositionAndAngles(m.x, m.y, m.z, victim.getYaw(), 0);
			victim.setVelocity(Vec3d.ZERO);
		}
		if (victimTicks % 20 == 0) {
			((ServerWorld) getWorld()).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.SLIME_BALL)),
				getX(), getY() + getHeight() * 0.7, getZ(), 6, 0.4, 0.2, 0.4, 0.05);
		}
	}

	public void release() {
		LivingEntity v = victim;
		victim = null;
		if (v == null || getWorld().isClient) return;
		float yaw = bodyYaw * MathHelper.RADIANS_PER_DEGREE;
		double fx = getX() - MathHelper.sin(yaw) * (getWidth() * 0.7 + 0.8);
		double fz = getZ() + MathHelper.cos(yaw) * (getWidth() * 0.7 + 0.8);
		if (v instanceof ServerPlayerEntity p) {
			p.networkHandler.requestTeleport(fx, getY() + 0.2, fz, p.getYaw(), p.getPitch());
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
		} else {
			v.refreshPositionAndAngles(fx, getY() + 0.2, fz, v.getYaw(), 0);
		}
		((ServerWorld) getWorld()).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.SLIME_BALL)),
			v.getX(), v.getY() + 1, v.getZ(), 40, 0.4, 0.6, 0.4, 0.1);
		getWorld().playSound(null, v.getBlockPos(), SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 1.5f, 0.7f);
		if (v instanceof CompanionEntity c) {
			c.setSwallowed(false);
			onCompanionFreed(c);
		}
	}

	private void onCompanionFreed(CompanionEntity c) {
		if (getServer() == null) return;
		StoryState s = StoryState.get(getServer());
		ServerPlayerEntity leader = Party.leader(getServer());
		String key = c.characterKey();
		if (key.equals("aqua") && s.stage == Story.CH1_TOADS && !s.hasFlag("aqua_freed") && leader != null) {
			s.setFlag("aqua_freed", true);
			DialogueManager.start(leader, "ch1_aqua_freed");
		} else if (key.equals("megumin") && s.stage == Story.CH2_TEST && !s.hasFlag("megumin_freed")) {
			s.setFlag("megumin_freed", true);
			ServerPlayerEntity p = leader != null ? leader : getServer().getPlayerManager().getPlayerList().stream().findFirst().orElse(null);
			if (p != null) DialogueManager.start(p, "ch2_megumin_freed");
		} else {
			String[] lines = switch (key) {
				case "aqua" -> new String[]{"Уааааа! Я вся в слизи! Это унижение для богини!!", "Я теперь пахну жабой... Никому не говори!"};
				case "megumin" -> new String[]{"Фу... Я липкая. Взорву всех жаб. Завтра.", "Спасибо... Только не смотри на меня так."};
				default -> new String[]{"Это было... восхитительно мерзко. Ещё раз?", "Слизь... на доспехах... какой позор..."};
			};
			c.say(lines[random.nextInt(lines.length)]);
		}
	}

	@Override
	public void onDeath(DamageSource source) {
		release();
		super.onDeath(source);
	}

	@Override
	public void remove(RemovalReason reason) {
		if (victim != null) release();
		super.remove(reason);
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		// Hitting a toad that is busy chewing is easier
		if (victim != null) amount *= 1.5f;
		return super.damage(source, amount);
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (!getWorld().isClient && isOnGround() && victim == null && !getNavigation().isIdle() && random.nextFloat() < 0.06f) {
			jump();
			Vec3d look = getRotationVector();
			addVelocity(look.x * 0.35, 0.1, look.z * 0.35);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.TOAD_CROAK;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_SLIME_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_SLIME_DEATH;
	}

	@Override
	protected float getSoundPitch() {
		return isKing() ? 0.55f : super.getSoundPitch();
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 160;
	}

	@Override
	protected void dropLoot(DamageSource source, boolean causedByPlayer) {
		super.dropLoot(source, causedByPlayer);
		dropStack(new ItemStack(dev.konocraft.registry.ModItems.TOAD_MEAT, isKing() ? 6 : 1 + random.nextInt(2)));
	}
}
