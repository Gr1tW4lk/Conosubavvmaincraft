package dev.konocraft.entity;

import dev.konocraft.fx.Fx;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.registry.ModItems;
import dev.konocraft.story.Bosses;
import dev.konocraft.story.Party;
import dev.konocraft.story.Speakers;
import dev.konocraft.story.StoryState;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Beldia the Dullahan, one of the Demon King's generals. An honourable knight who just wanted
 * someone to stop blowing up his castle every day. Weak to water. Very weak to losing his head.
 */
public class BeldiaEntity extends HostileEntity implements BossEntity {
	private static final TrackedData<Boolean> VISITING = DataTracker.registerData(BeldiaEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> HEAD_UP = DataTracker.registerData(BeldiaEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> HEADLESS = DataTracker.registerData(BeldiaEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> WET = DataTracker.registerData(BeldiaEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Бельдия, Безголовый рыцарь").formatted(Formatting.DARK_PURPLE),
		BossBar.Color.PURPLE, BossBar.Style.NOTCHED_10);
	private int sweepTimer = 120;
	private int summonTimer = 260;
	private int headTimer = 360;
	private int headUpTicks;
	private int headlessTicks;
	private int wetTicks;
	private int lonelyTicks;
	private boolean cursed;
	private boolean summonedHalf;
	private boolean headStolenOnce;

	public BeldiaEntity(EntityType<? extends BeldiaEntity> type, World world) {
		super(type, world);
		experiencePoints = 200;
		equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
		setEquipmentDropChance(EquipmentSlot.MAINHAND, 0);
		setPersistent();
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return HostileEntity.createHostileAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 420)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 11)
			.add(EntityAttributes.GENERIC_ARMOR, 12)
			.add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 4)
			.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.9)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48);
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(VISITING, false);
		dataTracker.startTracking(HEAD_UP, false);
		dataTracker.startTracking(HEADLESS, false);
		dataTracker.startTracking(WET, false);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(2, new MeleeAttackGoal(this, 1.15, true) {
			@Override
			public boolean canStart() {
				return !isVisiting() && !isHeadless() && super.canStart();
			}
		});
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 16f));
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this) {
			@Override
			public boolean canStart() {
				return !isVisiting() && super.canStart();
			}
		});
		targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true) {
			@Override
			public boolean canStart() {
				return !isVisiting() && super.canStart();
			}
		});
		targetSelector.add(3, new ActiveTargetGoal<>(this, CompanionEntity.class, true, e -> !((CompanionEntity) e).isDown()) {
			@Override
			public boolean canStart() {
				return !isVisiting() && super.canStart();
			}
		});
	}

	public boolean isVisiting() {
		return dataTracker.get(VISITING);
	}

	public void setVisiting(boolean v) {
		dataTracker.set(VISITING, v);
		if (v) setTarget(null);
	}

	public boolean isHeadUp() {
		return dataTracker.get(HEAD_UP);
	}

	public boolean isHeadless() {
		return dataTracker.get(HEADLESS);
	}

	public boolean isWet() {
		return dataTracker.get(WET);
	}

	@Override
	public EntityGroup getGroup() {
		return EntityGroup.UNDEAD;
	}

	@Override
	public boolean isDisallowedInPeaceful() {
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
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (isVisiting() && player instanceof ServerPlayerEntity sp && hand == Hand.MAIN_HAND) {
			dev.konocraft.story.DialogueManager.start(sp, "beldia_idle");
			return ActionResult.SUCCESS;
		}
		return super.interactMob(player, hand);
	}

	public void shout(String text) {
		if (getWorld().isClient) return;
		Speakers.Speaker sp = Speakers.get("beldia");
		Text msg = Text.literal("[" + sp.name() + "] ").styled(s -> s.withColor(sp.color())).append(Text.literal(text).formatted(Formatting.WHITE));
		for (PlayerEntity p : getWorld().getPlayers()) if (p.squaredDistanceTo(this) < 48 * 48) p.sendMessage(msg, false);
	}

	/** Water! His one weakness (well, one of several). */
	public void soak(ServerPlayerEntity by) {
		if (isVisiting()) return;
		dataTracker.set(WET, true);
		wetTicks = 200;
		addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 2));
		Fx.water((ServerWorld) getWorld(), getPos());
		if (random.nextBoolean()) shout(random.nextBoolean() ? "Вода?! Только не вода! Мои доспехи!" : "Ты!.. Откуда столько воды?!");
	}

	public void tryStealHead(ServerPlayerEntity player, int luck) {
		if (isVisiting()) return;
		if (isHeadless()) {
			player.sendMessage(Text.literal("Голова уже у вас. Больше красть нечего.").formatted(Formatting.GRAY), true);
			return;
		}
		boolean hurt = getHealth() < getMaxHealth() * 0.65f;
		double chance = hurt ? 0.45 + luck / 150.0 : 0.05 + luck / 400.0;
		if (random.nextDouble() > chance) {
			shout(hurt ? "Хм! Мою голову так просто не украсть!" : "Ты пытаешься обокрасть генерала армии Короля Демонов? Смешно!");
			player.sendMessage(Text.literal(hurt ? "Кража не удалась! Попробуйте ещё." : "Бельдия ещё слишком силён. Ослабьте его, прежде чем красть.").formatted(Formatting.GRAY), true);
			return;
		}
		dataTracker.set(HEADLESS, true);
		headlessTicks = 20 * 15;
		setTarget(null);
		getNavigation().stop();
		ItemStack head = new ItemStack(ModItems.BELDIA_HEAD);
		if (!player.getInventory().insertStack(head)) player.dropItem(head, false);
		player.sendMessage(Text.literal("Кража! Вы украли... ГОЛОВУ Бельдии!").formatted(Formatting.GOLD, Formatting.BOLD), false);
		shout("ВЕРНИ МОЮ ГОЛОВУ!!! Я ничего не вижу! Кто-нибудь!..");
		if (!headStolenOnce) {
			headStolenOnce = true;
			for (PlayerEntity p : getWorld().getPlayers()) {
				if (p.squaredDistanceTo(this) < 48 * 48) {
					p.sendMessage(Text.literal("[Авантюристы] Пас мне! Пас! Пинай голову!").formatted(Formatting.YELLOW), false);
				}
			}
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (isVisiting()) return false;
		if (source.getAttacker() instanceof GiantToadEntity || source.getAttacker() instanceof KonoMonsterEntity) return false;
		if (isWet()) amount *= 1.8f;
		if (isHeadless()) amount *= 2.5f;
		if (source.isOf(net.minecraft.entity.damage.DamageTypes.DROWN)) amount *= 2f;
		return super.damage(source, amount);
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (isVisiting()) {
			setTarget(null);
			return;
		}
		ServerWorld w = (ServerWorld) getWorld();
		bossBar.setPercent(getHealth() / getMaxHealth());
		for (ServerPlayerEntity p : w.getPlayers()) {
			if (p.squaredDistanceTo(this) < 50 * 50) bossBar.addPlayer(p);
			else bossBar.removePlayer(p);
		}
		// Reset when nobody is fighting
		PlayerEntity closest = w.getClosestPlayer(this, 40);
		if (getCommandTags().contains("konotest")) {
			lonelyTicks = 0;
		} else if (closest == null || closest.isSpectator()) {
			if (++lonelyTicks > 200) {
				heal(getMaxHealth());
				bossBar.clearPlayers();
				Bosses.onBeldiaReset(getServer());
				discard();
				return;
			}
		} else {
			lonelyTicks = 0;
		}
		if (isTouchingWater() || w.hasRain(getBlockPos())) {
			if (!isWet()) dataTracker.set(WET, true);
			wetTicks = Math.max(wetTicks, 40);
		}
		if (wetTicks > 0 && --wetTicks == 0) dataTracker.set(WET, false);
		if (isWet() && age % 5 == 0) w.spawnParticles(ParticleTypes.DRIPPING_WATER, getX(), getY() + 1.6, getZ(), 3, 0.4, 0.6, 0.4, 0);

		if (isHeadless()) {
			if (--headlessTicks <= 0) {
				dataTracker.set(HEADLESS, false);
				shout("Наконец-то! Голова на месте... То есть, в руке. Вы за это заплатите!");
			} else if (age % 20 == 0) {
				// stumbling around blindly
				getNavigation().startMovingTo(getX() + random.nextInt(9) - 4, getY(), getZ() + random.nextInt(9) - 4, 0.8);
				if (random.nextInt(4) == 0) shout(random.nextBoolean() ? "Где я?! Где стена?!" : "Перестаньте её пинать!!!");
			}
			return;
		}
		if (headUpTicks > 0 && --headUpTicks == 0) dataTracker.set(HEAD_UP, false);
		LivingEntity target = getTarget();
		if (target == null) return;

		if (--sweepTimer <= 0) {
			sweepTimer = 140 + random.nextInt(60);
			sweep(w);
		}
		if (--headTimer <= 0) {
			headTimer = 380 + random.nextInt(100);
			dataTracker.set(HEAD_UP, true);
			headUpTicks = 120;
			shout("Я подброшу свою голову, чтобы видеть вас всех сверху! Вам не скрыться!");
		}
		if (isHeadUp() && age % 25 == 0) {
			for (ServerPlayerEntity p : w.getPlayers()) {
				if (p.squaredDistanceTo(this) > 30 * 30 || p.isCreative()) continue;
				MagicBoltEntity bolt = new MagicBoltEntity(w, getX(), getY() + 3.2, getZ(), 6);
				bolt.setOwner(this);
				Vec3d dir = p.getEyePos().subtract(bolt.getPos()).normalize();
				bolt.setVelocity(dir.x, dir.y, dir.z, 1.1f, 1f);
				w.spawnEntity(bolt);
			}
		}
		if (--summonTimer <= 0) {
			summonTimer = 320;
			summonKnights(w, 2);
		}
		if (!summonedHalf && getHealth() < getMaxHealth() * 0.5f) {
			summonedHalf = true;
			shout("Восстаньте, мои верные рыцари! Покажите им ужас армии Короля Демонов!");
			summonKnights(w, 3);
		}
		if (!cursed && getHealth() < getMaxHealth() * 0.75f && target instanceof ServerPlayerEntity p) {
			cursed = true;
			shout("Смертельное проклятие! Через неделю ты умрёшь!");
			Fx.curse(w, p.getPos());
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1));
			StoryState s = StoryState.get(getServer());
			if (s.party.contains("aqua")) {
				dev.konocraft.util.Scheduler.after(40, () -> {
					p.removeStatusEffect(StatusEffects.WITHER);
					Fx.holy(w, p.getPos());
					net.minecraft.entity.Entity aqua = dev.konocraft.story.Npcs.find(getServer(), "aqua");
					if (aqua instanceof AquaEntity a) a.say("Брейк Спелл! Пф, детское проклятие. Опять.");
					shout("...Опять?! Да что не так с этой жрицей?!");
				});
			}
		}
	}

	private void sweep(ServerWorld w) {
		swingHand(Hand.MAIN_HAND);
		w.playSound(null, getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 2f, 0.6f);
		for (int k = 0; k < 36; k++) {
			double a = k * Math.PI * 2 / 36;
			w.spawnParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(a) * 3.5, getY() + 1, getZ() + Math.sin(a) * 3.5, 1, 0, 0, 0, 0);
		}
		List<LivingEntity> hit = w.getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(4.5, 1.5, 4.5),
			e -> e != this && (e instanceof PlayerEntity || e instanceof CompanionEntity));
		for (LivingEntity e : hit) {
			e.damage(getDamageSources().mobAttack(this), 10);
			Vec3d push = e.getPos().subtract(getPos()).normalize().multiply(1.4);
			e.addVelocity(push.x, 0.5, push.z);
		}
		if (random.nextBoolean()) shout(random.nextBoolean() ? "Получайте! Круговой удар!" : "Ха! Это всё, на что способны авантюристы Акселя?");
	}

	private void summonKnights(ServerWorld w, int count) {
		int existing = w.getEntitiesByClass(KonoMonsterEntity.UndeadKnight.class, getBoundingBox().expand(24), e -> true).size();
		for (int i = 0; i < count && existing + i < 5; i++) {
			KonoMonsterEntity.UndeadKnight k = ModEntities.UNDEAD_KNIGHT.create(w);
			if (k == null) continue;
			double a = random.nextDouble() * Math.PI * 2;
			k.refreshPositionAndAngles(getX() + Math.cos(a) * 4, getY(), getZ() + Math.sin(a) * 4, random.nextFloat() * 360, 0);
			k.initialize(w, w.getLocalDifficulty(k.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
			w.spawnEntity(k);
			w.spawnParticles(ParticleTypes.SOUL, k.getX(), k.getY() + 1, k.getZ(), 20, 0.4, 0.8, 0.4, 0.05);
		}
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		bossBar.clearPlayers();
		if (getWorld().isClient) return;
		ServerWorld w = (ServerWorld) getWorld();
		Fx.holy(w, getPos());
		ServerPlayerEntity killer = source.getAttacker() instanceof ServerPlayerEntity p ? p : Party.leader(getServer());
		Bosses.onBeldiaDefeated(getServer(), killer);
	}

	@Override
	public void onStoppedTrackingBy(ServerPlayerEntity player) {
		super.onStoppedTrackingBy(player);
		bossBar.removePlayer(player);
	}

	@Override
	public void remove(RemovalReason reason) {
		bossBar.clearPlayers();
		super.remove(reason);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_WITHER_DEATH;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Visiting", isVisiting());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		setVisiting(nbt.getBoolean("Visiting"));
	}
}
