package dev.konocraft.story;

import dev.konocraft.net.ServerNet;
import dev.konocraft.registry.ModSounds;
import dev.konocraft.skill.PlayerClass;
import dev.konocraft.skill.Skill;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/** Adventurer levels, money (eris), class bonuses and syncing them to the client. */
public final class Progression {
	private static final UUID HEALTH_UUID = UUID.fromString("6b6f6e6f-7375-6261-0001-000000000001");

	private Progression() {
	}

	public static PlayerData data(ServerPlayerEntity player) {
		return StoryState.get(player.getServer()).player(player.getUuid());
	}

	public static String formatMoney(long money) {
		String s = String.format("%,d", money).replace(',', ' ');
		return s + " эрис";
	}

	public static void addMoney(ServerPlayerEntity player, long amount) {
		PlayerData d = data(player);
		d.money = Math.max(0, d.money + amount);
		StoryState.get(player.getServer()).markDirty();
		if (amount != 0) {
			player.sendMessage(Text.literal((amount > 0 ? "+" : "") + formatMoney(amount)).formatted(amount > 0 ? Formatting.GOLD : Formatting.RED), true);
			if (amount > 0) player.getServerWorld().playSound(null, player.getBlockPos(), ModSounds.COIN, SoundCategory.PLAYERS, 0.8f, 1.0f);
		}
		sync(player);
	}

	public static void addExp(ServerPlayerEntity player, int amount) {
		PlayerData d = data(player);
		if (d.level >= PlayerData.MAX_LEVEL) return;
		d.exp += amount;
		boolean leveled = false;
		while (d.level < PlayerData.MAX_LEVEL && d.exp >= PlayerData.expForNext(d.level)) {
			d.exp -= PlayerData.expForNext(d.level);
			d.level++;
			int sp = d.level % 5 == 0 ? 3 : 1;
			d.skillPoints += sp;
			leveled = true;
			player.sendMessage(Text.literal("★ Уровень повышен! Теперь вы " + d.level + " уровня. +" + sp + " очк. навыков")
				.formatted(Formatting.YELLOW, Formatting.BOLD), false);
		}
		if (leveled) {
			d.mana = d.maxMana();
			applyAttributes(player);
			player.setHealth(player.getMaxHealth());
			player.getServerWorld().playSound(null, player.getBlockPos(), ModSounds.LEVEL_UP, SoundCategory.PLAYERS, 1f, 1f);
			player.getServerWorld().spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 1, 0.5, 0.3);
		}
		StoryState.get(player.getServer()).markDirty();
		sync(player);
	}

	public static void setClass(ServerPlayerEntity player, PlayerClass cls) {
		PlayerData d = data(player);
		d.playerClass = cls;
		for (Skill s : cls.startSkills) d.skills.add(s);
		d.mana = d.maxMana();
		StoryState.get(player.getServer()).markDirty();
		applyAttributes(player);
		player.setHealth(player.getMaxHealth());
		sync(player);
	}

	/** Extra max health from class and level. */
	public static void applyAttributes(ServerPlayerEntity player) {
		PlayerData d = data(player);
		EntityAttributeInstance inst = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
		if (inst == null) return;
		inst.removeModifier(HEALTH_UUID);
		double bonus = d.playerClass.healthBonus + (d.level - 1) / 2;
		if (bonus != 0) {
			inst.addPersistentModifier(new EntityAttributeModifier(HEALTH_UUID, "konocraft adventurer level", bonus, EntityAttributeModifier.Operation.ADDITION));
		}
		if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
	}

	public static void sync(ServerPlayerEntity player) {
		ServerNet.sendPlayerData(player, data(player));
	}

	/** Mana regeneration, called every 10 ticks. */
	public static void regenMana(ServerPlayerEntity player) {
		PlayerData d = data(player);
		int max = d.maxMana();
		if (d.mana < max) {
			float mult = 1f;
			if (player.getMainHandStack().getItem() instanceof dev.konocraft.item.KonoItems.Staff staff) mult = staff.manaBoost;
			d.mana = Math.min(max, d.mana + d.playerClass.manaRegen * 0.5f * mult);
		}
	}
}
