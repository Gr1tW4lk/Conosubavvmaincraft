package dev.konocraft.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

/**
 * Darkness: a crusader with unmatched defence and a sword that has never hit anything.
 * Taunts every monster around and genuinely enjoys it. A little too much.
 */
public class DarknessEntity extends CompanionEntity {
	private int tauntCooldown;

	public DarknessEntity(EntityType<? extends DarknessEntity> type, World world) {
		super(type, world);
		equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		setEquipmentDropChance(EquipmentSlot.MAINHAND, 0);
	}

	public static DefaultAttributeContainer.Builder createDarknessAttributes() {
		return createCompanionAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 90)
			.add(EntityAttributes.GENERIC_ARMOR, 16)
			.add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 6)
			.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 9);
	}

	@Override
	protected void addCombatGoals() {
		goalSelector.add(2, new MeleeAttackGoal(this, 1.05, true));
		targetSelector.add(1, new DefendPartyGoal(this, 12));
	}

	@Override
	public boolean tryAttack(Entity target) {
		if (random.nextFloat() < 0.8f) {
			swingHand(net.minecraft.util.Hand.MAIN_HAND);
			((ServerWorld) getWorld()).spawnParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(), 1, 0.5, 0.2, 0.5, 0);
			getWorld().playSound(null, getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, SoundCategory.NEUTRAL, 1f, 1f);
			if (random.nextInt(5) == 0) {
				String[] miss = {"Промах! ...Снова.", "Я специально! Это тактика!", "Хм. Меч сегодня капризный.", "Не смотри на меня так!"};
				say(miss[random.nextInt(miss.length)]);
			}
			return false;
		}
		say("Попала?! Я попала!!");
		return super.tryAttack(target);
	}

	@Override
	protected void companionTick() {
		if (tauntCooldown > 0) tauntCooldown--;
		if (age % 10 != 0 || tauntCooldown > 0) return;
		var threats = threats(12);
		if (threats.isEmpty()) return;
		for (MobEntity m : threats) {
			if (m instanceof BarrierCrystalEntity) continue;
			m.setTarget(this);
		}
		String[] lines = {
			"Ко мне, мерзкие твари! Цельтесь в меня! Сильнее!",
			"Провокация! ...Ах, все смотрят только на меня...",
			"Я — щит этой группы! Бейте меня, а не их!",
			"Не жалейте меня! Ни в коем случае не жалейте!"
		};
		say(lines[random.nextInt(lines.length)]);
		((ServerWorld) getWorld()).spawnParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 2.2, getZ(), 5, 0.4, 0.2, 0.4, 0);
		tauntCooldown = 10 * 20;
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient && age % 40 == 0 && getHealth() < getMaxHealth() && getCompanionState() == STATE_NORMAL) heal(2);
	}

	@Override
	protected void onHurtReaction(DamageSource source) {
		String[] lines = {"Ах... какой удар... Ещё!", "Неплохо... для монстра.", "Нет, я не наслаждаюсь! Вовсе нет!", "Хм! Я выдержу что угодно!"};
		say(lines[random.nextInt(lines.length)]);
	}

	@Override
	protected String faintLine() {
		return "Повержена... на глазах у всех... какое унижение... великолепно...";
	}

	@Override
	protected String swallowLine() {
		return "Меня... проглотили... Внутри так склизко и унизительно... Н-не спасай меня пока!";
	}

	@Override
	protected String hurtLine() {
		return "Т-ты ударил меня?.. Без предупреждения?.. Сделай так ещё раз... То есть, не смей!";
	}

	@Override
	protected String[] idleLines() {
		return new String[]{
			"Если встретим кого-то сильного, пусти меня вперёд. Обязательно.",
			"Я из благородной семьи. Не говори никому. Особенно отцу.",
			"Интересно, у генералов армии Короля Демонов тяжёлая рука?..",
			"Мой меч остр. Просто он стесняется попадать.",
			"Ты сегодня какой-то слишком добрый. Мне это не нравится."
		};
	}

	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, NbtCompound entityNbt) {
		equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
	}
}
