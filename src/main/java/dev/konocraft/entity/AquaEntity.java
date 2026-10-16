package dev.konocraft.entity;

import dev.konocraft.fx.Fx;
import dev.konocraft.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;

/**
 * Aqua: goddess of water, beloved of the Axis Cult. Excellent healer, even better at crying.
 * Purifies undead, soaks Beldia, and very often gets eaten by toads.
 */
public class AquaEntity extends CompanionEntity {
	private int healCooldown;
	private int holyCooldown;
	private int waterCooldown;

	public AquaEntity(EntityType<? extends AquaEntity> type, World world) {
		super(type, world);
	}

	@Override
	protected void addCombatGoals() {
		goalSelector.add(2, new MeleeAttackGoal(this, 1.1, false));
		targetSelector.add(1, new DefendPartyGoal(this, 10));
	}

	@Override
	public boolean tryAttack(net.minecraft.entity.Entity target) {
		if (random.nextInt(4) == 0) say(random.nextBoolean() ? "Божественный удар!" : "Получай, мерзкая тварь! Божественное возмездие!");
		return super.tryAttack(target);
	}

	@Override
	protected void companionTick() {
		if (healCooldown > 0) healCooldown--;
		if (holyCooldown > 0) holyCooldown--;
		if (waterCooldown > 0) waterCooldown--;
		if (age % 10 != 0) return;
		ServerWorld w = (ServerWorld) getWorld();
		ServerPlayerEntity leader = leader();
		// Heal the party
		if (healCooldown <= 0 && leader != null && leader.getHealth() < leader.getMaxHealth() * 0.6f && squaredDistanceTo(leader) < 14 * 14) {
			leader.heal(10);
			Fx.heal(w, leader.getPos());
			for (CompanionEntity c : w.getEntitiesByClass(CompanionEntity.class, getBoundingBox().expand(12), c -> true)) c.heal(10);
			say(random.nextBoolean() ? "Хил! Можешь не благодарить. Нет, лучше поблагодари." : "Сакрэд Хайнесс Хил! Видишь, какая я полезная?");
			healCooldown = 30 * 20;
		}
		// Turn Undead
		if (holyCooldown <= 0) {
			LivingEntity undead = null;
			for (MobEntity m : threats(10)) if (m.isUndead()) undead = m;
			if (undead != null) {
				say("Нежить?! Фу! Изгнание нежити!");
				Fx.holy(w, getPos());
				for (MobEntity m : w.getEntitiesByClass(MobEntity.class, getBoundingBox().expand(9), MobEntity::isUndead)) {
					if (m instanceof BeldiaEntity) continue;
					m.damage(w.getDamageSources().indirectMagic(this, this), 20);
				}
				holyCooldown = 12 * 20;
			}
		}
		// Beldia hates water. Aqua loves water.
		if (waterCooldown <= 0) {
			for (MobEntity m : threats(18)) {
				if (m instanceof BeldiaEntity b && !b.isVisiting()) {
					say(random.nextBoolean() ? "Создание воды! Много-много воды!" : "Хлебни-ка водички, рыцарь без головы!");
					Fx.water(w, b.getPos());
					b.soak(leader);
					waterCooldown = 15 * 20;
					break;
				}
			}
		}
	}

	@Override
	protected void onHurtReaction(DamageSource source) {
		getWorld().playSound(null, getBlockPos(), ModSounds.AQUA_CRY, SoundCategory.NEUTRAL, 1f, 1f);
		((ServerWorld) getWorld()).spawnParticles(net.minecraft.particle.ParticleTypes.FALLING_WATER, getX(), getY() + 1.7, getZ(), 20, 0.25, 0.1, 0.25, 0);
		String[] lines = {"Уаааа! Мне больно!", "Почему всегда я?!", "Ты должен меня защищать! Я же богиня!", "Хииии! Спасите!"};
		say(lines[random.nextInt(lines.length)]);
	}

	@Override
	protected String faintLine() {
		return "Уаааа... Богиня... повержена... Кто-нибудь, воскресите меня... ой, это же я воскрешаю...";
	}

	@Override
	protected String swallowLine() {
		return "Уааааа! Она меня съела! Тут скользко и темно! Спасите богиню!!!";
	}

	@Override
	protected String hurtLine() {
		return "Ты посмел ударить богиню?! Я всё расскажу Эрис!";
	}

	@Override
	protected String[] idleLines() {
		return new String[]{
			"Если что, я богиня. Просто напоминаю.",
			"Давай потом зайдём в таверну? Ты угощаешь.",
			"Вступай в культ Аксис! У нас есть печеньки и бесконечная свобода!",
			"Мне кажется, за нами следит жаба. Нет, правда.",
			"Я могла бы вылечить твою удачу, но это даже мне не под силу."
		};
	}
}
