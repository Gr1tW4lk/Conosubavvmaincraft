package dev.konocraft.entity;

import dev.konocraft.fx.Fx;
import dev.konocraft.registry.ModItems;
import dev.konocraft.story.Cutscenes;
import dev.konocraft.util.Scheduler;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

import java.util.List;

/**
 * Megumin: the greatest arch wizard of the Crimson Demon clan. Knows exactly one spell.
 * Casts it once a day on anything worthy, then collapses and has to be carried home.
 */
public class MeguminEntity extends CompanionEntity {
	private long lastExplosionDay = -1;

	public MeguminEntity(EntityType<? extends MeguminEntity> type, World world) {
		super(type, world);
		equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.MEGUMIN_STAFF));
		setEquipmentDropChance(EquipmentSlot.MAINHAND, 0);
	}

	@Override
	protected void addCombatGoals() {
		// Megumin does not fight. Megumin explodes.
	}

	@Override
	protected void companionTick() {
		if (!isFollowing() || age % 20 != 0 || getServer() == null) return;
		long day = getWorld().getTimeOfDay() / 24000L;
		if (day == lastExplosionDay) return;
		ServerPlayerEntity lead = leader();
		if (lead != null && dev.konocraft.story.DialogueManager.inDialogue(lead)) return;
		List<MobEntity> threats = threats(26);
		LivingEntity boss = null;
		for (MobEntity m : threats) if (m instanceof BeldiaEntity || m instanceof DestroyerGolemEntity && threats.size() > 5) boss = m;
		boolean worthy = boss != null || threats.size() >= 4;
		if (!worthy) return;
		// target: the boss, or the centre of the crowd
		Vec3d target;
		if (boss != null) {
			target = boss.getPos();
		} else {
			double x = 0, y = 0, z = 0;
			for (MobEntity m : threats) {
				x += m.getX();
				y += m.getY();
				z += m.getZ();
			}
			target = new Vec3d(x / threats.size(), y / threats.size(), z / threats.size());
		}
		ServerPlayerEntity leader = leader();
		if (target.distanceTo(getPos()) < 9 || (leader != null && target.distanceTo(leader.getPos()) < 7)) {
			if (random.nextInt(6) == 0) say("Слишком близко! Отойди, я хочу взорвать их всех!");
			return;
		}
		lastExplosionDay = day;
		castExplosion(target, leader);
	}

	public void castExplosion(Vec3d target, ServerPlayerEntity credit) {
		ServerWorld w = (ServerWorld) getWorld();
		String[] chants = {
			"О пламя, что дремлет в алой крови моего клана... пробудись и прочерти багровую черту через само мироздание!",
			"Владыка багрового грома, внемли зову той, что избрала единственный путь! Я не прошу силы — я требую её!",
			"Пусть звёзды падут и горы обратятся в пыль! Узрите истинное искусство!"
		};
		say(chants[random.nextInt(chants.length)]);
		castPose(70, target);
		Fx.magicCircle(w, target, 6.5, 60, Fx.RED);
		Scheduler.after(60, () -> {
			if (!isAlive()) return;
			say("ВЗРЫВ!!!");
			Cutscenes.explosionDamage(w, target, 10, 110f, credit);
			Fx.explosion(w, target, 1.1f);
			Scheduler.after(15, () -> {
				collapse(20 * 75);
				say("Хе-хе... какой прекрасный взрыв... Я не могу пошевелиться. Понеси меня.");
			});
		});
	}

	@Override
	protected String faintLine() {
		return "Великий маг клана Алых Демонов... временно... отдыхает...";
	}

	@Override
	protected String swallowLine() {
		return "Ааа! Меня проглотила жаба! Вытащи меня отсюда!!!";
	}

	@Override
	protected String hurtLine() {
		return "Как ты смеешь поднимать руку на величайшего мага Алых Демонов?!";
	}

	@Override
	protected String[] idleLines() {
		return new String[]{
			"Мне скучно. Давай что-нибудь взорвём.",
			"Тебе не кажется, что вон та скала выглядит... взрывоопасно?",
			"Моё имя всё ещё лучшее в клане. Не спорь.",
			"Чомусуке опять сбежал. Это кошка. Наверное.",
			"Знаешь, сколько маны нужно на Взрыв? Всё. Вся мана. Это прекрасно."
		};
	}

	@Override
	protected void onHurtReaction(DamageSource source) {
		say(random.nextBoolean() ? "Ай! Это было больно!" : "Защищай своего мага, а?!");
	}

	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, NbtCompound entityNbt) {
		equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.MEGUMIN_STAFF));
		return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putLong("LastExplosionDay", lastExplosionDay);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("LastExplosionDay")) lastExplosionDay = nbt.getLong("LastExplosionDay");
	}
}
