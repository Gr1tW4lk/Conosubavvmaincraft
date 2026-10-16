package dev.konocraft.skill;

import dev.konocraft.entity.BeldiaEntity;
import dev.konocraft.entity.BossPart;
import dev.konocraft.entity.CompanionEntity;
import dev.konocraft.entity.FlyingCabbageEntity;
import dev.konocraft.entity.GiantToadEntity;
import dev.konocraft.entity.KonoNpcEntity;
import dev.konocraft.entity.MagicBoltEntity;
import dev.konocraft.fx.Fx;
import dev.konocraft.registry.ModItems;
import dev.konocraft.registry.ModSounds;
import dev.konocraft.story.Cutscenes;
import dev.konocraft.story.PlayerData;
import dev.konocraft.story.Progression;
import dev.konocraft.story.StoryState;
import dev.konocraft.util.Scheduler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Casting adventurer skills. All skills are server-side; the client only sends "cast". */
public final class Skills {
	private static final Map<UUID, EnumMap<Skill, Long>> cooldowns = new HashMap<>();

	private Skills() {
	}

	public static void init() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cooldowns.remove(handler.player.getUuid()));
	}

	public static boolean unlocked(StoryState state, PlayerData d, Skill skill) {
		if (d.skills.contains(skill)) return false;
		return skill.unlockFlag == null || state.hasFlag(skill.unlockFlag);
	}

	public static void learn(ServerPlayerEntity player, Skill skill) {
		StoryState state = StoryState.get(player.getServer());
		PlayerData d = state.player(player.getUuid());
		if (!unlocked(state, d, skill)) return;
		int cost = d.playerClass.costOf(skill);
		if (d.skillPoints < cost) {
			player.sendMessage(Text.literal("Не хватает очков навыков (нужно " + cost + ")").formatted(Formatting.RED), true);
			return;
		}
		d.skillPoints -= cost;
		grant(player, skill);
	}

	public static void grant(ServerPlayerEntity player, Skill skill) {
		StoryState state = StoryState.get(player.getServer());
		PlayerData d = state.player(player.getUuid());
		if (d.skills.add(skill)) {
			player.sendMessage(Text.literal("★ Изучен навык: " + skill.title).formatted(Formatting.AQUA, Formatting.BOLD), false);
			player.getServerWorld().playSound(null, player.getBlockPos(), ModSounds.LEVEL_UP, SoundCategory.PLAYERS, 0.8f, 1.4f);
		}
		state.markDirty();
		Progression.sync(player);
	}

	public static void select(ServerPlayerEntity player, int value, boolean absolute) {
		PlayerData d = Progression.data(player);
		int n = d.skills.size();
		if (n == 0) return;
		d.selectedSkill = absolute ? Math.floorMod(value, n) : Math.floorMod(d.selectedSkill + value, n);
		Skill s = d.selected();
		if (s != null) player.sendMessage(Text.literal("Навык: " + s.title + " (" + s.mana + " маны)").formatted(Formatting.AQUA), true);
		Progression.sync(player);
	}

	public static void castSelected(ServerPlayerEntity player) {
		PlayerData d = Progression.data(player);
		Skill s = d.selected();
		if (s == null) {
			player.sendMessage(Text.literal("У вас нет навыков. Изучите их через карту авантюриста (K).").formatted(Formatting.GRAY), true);
			return;
		}
		cast(player, s);
	}

	public static void cast(ServerPlayerEntity player, Skill skill) {
		PlayerData d = Progression.data(player);
		if (!d.skills.contains(skill)) return;
		long now = player.getServerWorld().getTime();
		EnumMap<Skill, Long> cds = cooldowns.computeIfAbsent(player.getUuid(), u -> new EnumMap<>(Skill.class));
		Long ready = cds.get(skill);
		if (ready != null && ready > now) {
			player.sendMessage(Text.literal(skill.title + ": перезарядка " + ((ready - now) / 20 + 1) + " с").formatted(Formatting.GRAY), true);
			return;
		}
		if (d.mana < skill.mana) {
			player.sendMessage(Text.literal("Не хватает маны! (" + (int) d.mana + "/" + skill.mana + ")").formatted(Formatting.BLUE), true);
			return;
		}
		boolean ok = perform(player, skill, d);
		if (!ok) return;
		d.mana -= skill.mana;
		cds.put(skill, now + skill.cooldown);
		StoryState.get(player.getServer()).markDirty();
		Progression.sync(player);
	}

	// ---------------------------------------------------------------------------------------------

	private static LivingEntity lookTarget(ServerPlayerEntity player, double range) {
		Vec3d eye = player.getEyePos();
		Vec3d look = player.getRotationVec(1f);
		Vec3d end = eye.add(look.multiply(range));
		Box box = player.getBoundingBox().stretch(look.multiply(range)).expand(1.5);
		EntityHitResult hit = ProjectileUtil.raycast(player, eye, end, box, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator(), range * range);
		if (hit != null && hit.getEntity() instanceof LivingEntity le) return le;
		if (hit != null && hit.getEntity() instanceof BossPart part && part.owner() instanceof LivingEntity le) return le;
		return null;
	}

	private static List<LivingEntity> cone(ServerPlayerEntity player, double range, double cos) {
		Vec3d eye = player.getEyePos();
		Vec3d look = player.getRotationVec(1f);
		return player.getServerWorld().getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(range), e -> {
			if (e == player || !e.isAlive() || e instanceof PlayerEntity || e instanceof KonoNpcEntity) return false;
			Vec3d to = e.getPos().add(0, e.getHeight() / 2, 0).subtract(eye);
			double len = to.length();
			return len <= range && to.normalize().dotProduct(look) >= cos;
		});
	}

	private static boolean hostile(Entity e) {
		return e instanceof Monster || e instanceof GiantToadEntity || e instanceof FlyingCabbageEntity || e instanceof BeldiaEntity
			|| e instanceof dev.konocraft.entity.BossEntity || e instanceof dev.konocraft.entity.BarrierCrystalEntity;
	}

	private static boolean perform(ServerPlayerEntity player, Skill skill, PlayerData d) {
		ServerWorld w = player.getServerWorld();
		Vec3d pos = player.getPos();
		switch (skill) {
			case STEAL -> {
				LivingEntity t = lookTarget(player, 5);
				if (t == null) {
					player.sendMessage(Text.literal("Не на кого применить «Кражу».").formatted(Formatting.GRAY), true);
					return false;
				}
				steal(player, t, d);
			}
			case LURK -> {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 240, 0, false, false, true));
				for (MobEntity m : w.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(24), m -> m.getTarget() == player)) {
					m.setTarget(null);
				}
				w.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 1, pos.z, 30, 0.4, 0.8, 0.4, 0.02);
				w.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1f, 1.2f);
			}
			case SNIPE -> {
				MagicBoltEntity bolt = new MagicBoltEntity(w, player, 6 + d.luck() / 10f);
				bolt.setVelocity(player, player.getPitch(), player.getYaw(), 0, 2.2f, 0.2f);
				bolt.setHoming(true);
				w.spawnEntity(bolt);
				w.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1f, 1.5f);
			}
			case TINDER -> {
				LivingEntity t = lookTarget(player, 14);
				if (t == null) return false;
				t.setOnFireFor(5);
				t.damage(w.getDamageSources().indirectMagic(player, player), 3);
				w.spawnParticles(ParticleTypes.FLAME, t.getX(), t.getY() + 1, t.getZ(), 30, 0.3, 0.5, 0.3, 0.05);
				w.playSound(null, t.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1f, 1f);
			}
			case WIND_BREATH -> {
				Vec3d look = player.getRotationVec(1f);
				for (LivingEntity e : cone(player, 7, 0.5)) {
					e.damage(w.getDamageSources().indirectMagic(player, player), 2);
					e.takeKnockback(2.2, -look.x, -look.z);
				}
				for (int i = 1; i <= 7; i++) {
					Vec3d p = player.getEyePos().add(look.multiply(i));
					w.spawnParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 6, 0.3 * i / 3, 0.3, 0.3 * i / 3, 0.05);
				}
				w.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 1.5f, 1.4f);
			}
			case CREATE_WATER -> {
				Vec3d look = player.getRotationVec(1f);
				for (LivingEntity e : cone(player, 8, 0.6)) {
					e.extinguish();
					if (e instanceof BeldiaEntity b) b.soak(player);
					else if (e.isUndead()) e.damage(w.getDamageSources().indirectMagic(player, player), 7);
					else e.damage(w.getDamageSources().indirectMagic(player, player), 2);
				}
				player.extinguish();
				for (int i = 1; i <= 8; i++) {
					Vec3d p = player.getEyePos().add(look.multiply(i));
					w.spawnParticles(ParticleTypes.SPLASH, p.x, p.y - 0.3, p.z, 20, 0.3, 0.3, 0.3, 0.2);
					w.spawnParticles(Fx.BLUE, p.x, p.y - 0.3, p.z, 6, 0.3, 0.3, 0.3, 0);
				}
				w.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.PLAYERS, 1f, 1.1f);
			}
			case FREEZE -> {
				Vec3d look = player.getRotationVec(1f);
				for (LivingEntity e : cone(player, 8, 0.55)) {
					e.damage(w.getDamageSources().indirectMagic(player, player), 5);
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 3));
					e.setFrozenTicks(140);
				}
				for (int i = 1; i <= 8; i++) {
					Vec3d p = player.getEyePos().add(look.multiply(i));
					w.spawnParticles(ParticleTypes.SNOWFLAKE, p.x, p.y - 0.3, p.z, 15, 0.4, 0.4, 0.4, 0.02);
				}
				w.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_POWDER_SNOW_PLACE, SoundCategory.PLAYERS, 1.5f, 0.8f);
			}
			case HEAL -> {
				player.heal(8);
				Fx.heal(w, pos);
				for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(8), e -> e instanceof PlayerEntity || e instanceof CompanionEntity)) {
					e.heal(8);
					Fx.heal(w, e.getPos());
				}
			}
			case TURN_UNDEAD -> {
				Fx.holy(w, pos);
				for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(9), LivingEntity::isUndead)) {
					e.damage(w.getDamageSources().indirectMagic(player, player), e instanceof BeldiaEntity ? 40 : 26);
					Fx.holy(w, e.getPos());
				}
			}
			case DECOY -> {
				for (MobEntity m : w.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(14), Skills::hostile)) {
					m.setTarget(player);
				}
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 1));
				w.spawnParticles(ParticleTypes.ANGRY_VILLAGER, pos.x, pos.y + 2, pos.z, 8, 0.5, 0.3, 0.5, 0);
				w.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 1.5f, 1f);
			}
			case DRAIN_TOUCH -> {
				LivingEntity t = lookTarget(player, 3.5);
				if (t == null) return false;
				t.damage(w.getDamageSources().indirectMagic(player, player), 6);
				player.heal(6);
				d.mana = Math.min(d.maxMana(), d.mana + 12);
				w.spawnParticles(Fx.PURPLE, t.getX(), t.getY() + 1, t.getZ(), 30, 0.3, 0.5, 0.3, 0);
				w.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.5f, 1.2f);
			}
			case EXPLOSION -> {
				Vec3d target = explosionTarget(player);
				if (target == null) {
					player.sendMessage(Text.literal("Цель слишком далеко!").formatted(Formatting.RED), true);
					return false;
				}
				explosion(player, target);
			}
		}
		return true;
	}

	private static Vec3d explosionTarget(ServerPlayerEntity player) {
		Vec3d eye = player.getEyePos();
		Vec3d end = eye.add(player.getRotationVec(1f).multiply(64));
		BlockHitResult hit = player.getWorld().raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, player));
		if (hit.getType() == HitResult.Type.MISS) return null;
		Vec3d p = hit.getPos();
		if (p.distanceTo(eye) < 8) {
			player.sendMessage(Text.literal("Слишком близко! Взрыв задел бы вас самих.").formatted(Formatting.RED), true);
			return null;
		}
		return p;
	}

	private static void explosion(ServerPlayerEntity player, Vec3d target) {
		ServerWorld w = player.getServerWorld();
		player.sendMessage(Text.literal("«Пламя багровое, пробудись! ...»").formatted(Formatting.RED, Formatting.ITALIC), true);
		Fx.magicCircle(w, target, 6.5, 30, Fx.RED);
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 35, 3, false, false, false));
		Scheduler.after(30, () -> {
			player.networkHandler.sendPacket(new TitleFadeS2CPacket(2, 20, 10));
			player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("ВЗРЫВ!").formatted(Formatting.RED, Formatting.BOLD)));
			Cutscenes.explosionDamage(w, target, 9, 90f, player);
			Fx.explosion(w, target, 1.0f);
			// The price of the strongest magic: you cannot move afterwards
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 220, 5, false, true, true));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 300, 2, false, true, true));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 300, 2, false, true, true));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 220, 250, false, false, false));
			PlayerData d = Progression.data(player);
			d.mana = 0;
			Progression.sync(player);
			player.sendMessage(Text.literal("Вы потратили всю ману и рухнули без сил. Теперь вы понимаете Мегумин.").formatted(Formatting.GRAY), false);
		});
	}

	// ---------------------------------------------------------------------------------------------

	private static void steal(ServerPlayerEntity player, LivingEntity t, PlayerData d) {
		ServerWorld w = player.getServerWorld();
		w.spawnParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getHeight() / 2, t.getZ(), 20, 0.4, 0.4, 0.4, 0.2);
		w.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1f, 0.6f);
		if (t instanceof BeldiaEntity b) {
			b.tryStealHead(player, d.luck());
			return;
		}
		if (t instanceof CompanionEntity c) {
			String[] lines = switch (c.characterKey()) {
				case "megumin" -> new String[]{"Вы украли у Мегумин... её запасную повязку на глаз. «Верни немедленно, или я тебя взорву!»",
					"Вы украли у Мегумин горсть конфет. Она смотрит на вас так, будто готовит заклинание."};
				case "aqua" -> new String[]{"Вы украли у Аквы... бутылку дешёвого вина. «Это священный напиток! Отдай!!»",
					"Вы украли у Аквы стопку анкет культа Аксис. Она только рада."};
				case "darkness" -> new String[]{"Даркнесс краснеет: «Т-ты посмел обокрасть крестоносца... при всех?! ...Сделай так ещё раз.»",
					"Вы украли у Даркнесс точильный камень. Она всё равно ни во что не попадает."};
				default -> new String[]{"Вы ничего не украли. Это было бы невежливо."};
			};
			player.sendMessage(Text.literal(lines[player.getRandom().nextInt(lines.length)]).formatted(Formatting.LIGHT_PURPLE), false);
			return;
		}
		if (t instanceof KonoNpcEntity) {
			player.sendMessage(Text.literal("Вы украли... собственный кошелёк. Видимо, удача решила пошутить.").formatted(Formatting.GRAY), false);
			return;
		}
		if (t instanceof PlayerEntity) return;
		double chance = 0.35 + d.luck() / 160.0;
		if (player.getRandom().nextDouble() > chance) {
			player.sendMessage(Text.literal("Кража не удалась!").formatted(Formatting.GRAY), true);
			return;
		}
		Item item;
		if (t instanceof GiantToadEntity) item = ModItems.TOAD_MEAT;
		else if (t instanceof FlyingCabbageEntity) {
			item = ModItems.CABBAGE;
			t.discard();
		} else {
			ItemStack held = t.getMainHandStack();
			if (!held.isEmpty() && player.getRandom().nextBoolean()) {
				ItemStack copy = held.copy();
				t.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				giveStolen(player, copy);
				return;
			}
			Item[] loot = {Items.GOLD_NUGGET, Items.EMERALD, Items.BONE, Items.STRING, Items.IRON_NUGGET, Items.APPLE};
			item = loot[player.getRandom().nextInt(loot.length)];
		}
		giveStolen(player, new ItemStack(item));
	}

	private static void giveStolen(ServerPlayerEntity player, ItemStack stack) {
		player.sendMessage(Text.literal("Украдено: ").formatted(Formatting.GREEN).append(stack.toHoverableText()), true);
		if (!player.getInventory().insertStack(stack)) player.dropItem(stack, false);
	}
}
