package dev.konocraft.story;

import dev.konocraft.skill.PlayerClass;
import dev.konocraft.skill.Skill;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Per-player adventurer progress: class, level, mana, money, skills, bounties and dailies. */
public class PlayerData {
	public static final int MAX_LEVEL = 50;

	public PlayerClass playerClass = PlayerClass.NONE;
	public int level = 1;
	public int exp;
	public int skillPoints;
	public float mana;
	public long money;
	public final EnumSet<Skill> skills = EnumSet.noneOf(Skill.class);
	public int selectedSkill;
	public boolean prologueDone;
	public boolean registered;
	public final Set<String> flags = new HashSet<>();
	public final Map<String, Long> dailies = new HashMap<>();

	// Bounty board
	public String bounty = "";
	public int bountyProgress;
	public long boardDay = -1;
	public final List<String> boardOffers = new ArrayList<>();

	public int maxMana() {
		return playerClass.baseMana + playerClass.manaPerLevel * (level - 1);
	}

	public static int expForNext(int level) {
		return 60 + level * level * 12;
	}

	public int luck() {
		return Math.min(100, playerClass.luck + level / 5);
	}

	public List<Skill> skillList() {
		return new ArrayList<>(skills);
	}

	public Skill selected() {
		List<Skill> list = skillList();
		if (list.isEmpty()) return null;
		if (selectedSkill < 0 || selectedSkill >= list.size()) selectedSkill = 0;
		return list.get(selectedSkill);
	}

	public boolean dailyDone(String key, long day) {
		Long d = dailies.get(key);
		return d != null && d == day;
	}

	public NbtCompound toNbt() {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("class", playerClass.name());
		nbt.putInt("level", level);
		nbt.putInt("exp", exp);
		nbt.putInt("sp", skillPoints);
		nbt.putFloat("mana", mana);
		nbt.putLong("money", money);
		NbtList skillList = new NbtList();
		for (Skill s : skills) skillList.add(NbtString.of(s.name()));
		nbt.put("skills", skillList);
		nbt.putInt("selected", selectedSkill);
		nbt.putBoolean("prologue", prologueDone);
		nbt.putBoolean("registered", registered);
		NbtList flagList = new NbtList();
		for (String f : flags) flagList.add(NbtString.of(f));
		nbt.put("flags", flagList);
		NbtCompound daily = new NbtCompound();
		dailies.forEach(daily::putLong);
		nbt.put("dailies", daily);
		nbt.putString("bounty", bounty);
		nbt.putInt("bountyProgress", bountyProgress);
		nbt.putLong("boardDay", boardDay);
		NbtList offers = new NbtList();
		for (String o : boardOffers) offers.add(NbtString.of(o));
		nbt.put("offers", offers);
		return nbt;
	}

	public static PlayerData fromNbt(NbtCompound nbt) {
		PlayerData d = new PlayerData();
		d.playerClass = PlayerClass.byName(nbt.getString("class"));
		d.level = Math.max(1, nbt.getInt("level"));
		d.exp = nbt.getInt("exp");
		d.skillPoints = nbt.getInt("sp");
		d.mana = nbt.getFloat("mana");
		d.money = nbt.getLong("money");
		for (NbtElement e : nbt.getList("skills", NbtElement.STRING_TYPE)) {
			Skill s = Skill.byName(e.asString());
			if (s != null) d.skills.add(s);
		}
		d.selectedSkill = nbt.getInt("selected");
		d.prologueDone = nbt.getBoolean("prologue");
		d.registered = nbt.getBoolean("registered");
		for (NbtElement e : nbt.getList("flags", NbtElement.STRING_TYPE)) d.flags.add(e.asString());
		NbtCompound daily = nbt.getCompound("dailies");
		for (String k : daily.getKeys()) d.dailies.put(k, daily.getLong(k));
		d.bounty = nbt.getString("bounty");
		d.bountyProgress = nbt.getInt("bountyProgress");
		d.boardDay = nbt.contains("boardDay") ? nbt.getLong("boardDay") : -1;
		for (NbtElement e : nbt.getList("offers", NbtElement.STRING_TYPE)) d.boardOffers.add(e.asString());
		return d;
	}
}
