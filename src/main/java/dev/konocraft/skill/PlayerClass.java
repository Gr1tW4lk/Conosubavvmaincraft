package dev.konocraft.skill;

import java.util.List;

/** Adventurer classes chosen in the prologue (the "cheat" from goddess Eris). */
public enum PlayerClass {
	NONE("Без класса", 0, 40, 3, 0.8f, 50, List.of()),
	ADVENTURER("Авантюрист", 0, 60, 5, 1.0f, 95, List.of(Skill.STEAL)),
	ARCH_WIZARD("Архимаг", -2, 130, 9, 1.8f, 45, List.of(Skill.TINDER, Skill.FREEZE)),
	CRUSADER("Крестоносец", 10, 35, 2, 0.6f, 35, List.of(Skill.DECOY)),
	ARCH_PRIEST("Архижрец", 2, 100, 7, 1.5f, 15, List.of(Skill.HEAL, Skill.TURN_UNDEAD));

	public final String title;
	/** Extra max health (half-hearts) at level 1. */
	public final int healthBonus;
	public final int baseMana;
	public final int manaPerLevel;
	/** Mana regenerated per second. */
	public final float manaRegen;
	public final int luck;
	public final List<Skill> startSkills;

	PlayerClass(String title, int healthBonus, int baseMana, int manaPerLevel, float manaRegen, int luck, List<Skill> startSkills) {
		this.title = title;
		this.healthBonus = healthBonus;
		this.baseMana = baseMana;
		this.manaPerLevel = manaPerLevel;
		this.manaRegen = manaRegen;
		this.luck = luck;
		this.startSkills = startSkills;
	}

	/** Skill point cost of a skill for this class. */
	public int costOf(Skill skill) {
		float mult = switch (this) {
			case ARCH_WIZARD -> skill.magic ? 0.6f : 1.5f;
			case CRUSADER -> skill.magic ? 1.5f : 0.8f;
			case ARCH_PRIEST -> skill.holy ? 0.5f : 1.2f;
			default -> 1.0f;
		};
		return Math.max(1, Math.round(skill.spCost * mult));
	}

	public static PlayerClass byName(String name) {
		for (PlayerClass c : values()) {
			if (c.name().equalsIgnoreCase(name)) return c;
		}
		return NONE;
	}
}
