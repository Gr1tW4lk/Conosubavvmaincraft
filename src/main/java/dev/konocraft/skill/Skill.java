package dev.konocraft.skill;

/** Learnable adventurer skills. Unlock flags come from meeting the character who can teach the skill. */
public enum Skill {
	STEAL("Кража", "Крадёт случайный предмет у цели. Шанс зависит от удачи. Говорят, на некоторых боссах работает особенно смешно.", 10, 60, 1, null, false, false),
	LURK("Затаиться", "Невидимость на 12 секунд. Враги теряют вас из виду.", 15, 400, 1, null, false, false),
	SNIPE("Снайпинг", "Выпускает самонаводящийся магический снаряд. Урон растёт с удачей.", 12, 30, 2, null, true, false),
	TINDER("Разжигание", "Поджигает цель. Классика бытовой магии.", 6, 30, 1, null, true, false),
	WIND_BREATH("Ветряное дыхание", "Порыв ветра отбрасывает врагов перед вами.", 8, 60, 1, null, true, false),
	CREATE_WATER("Создание воды", "Обливает врагов водой. Нежить и рыцари без головы это ОЧЕНЬ не любят.", 8, 40, 1, "met_aqua", true, false),
	FREEZE("Заморозка", "Замедляет и ранит врагов перед вами холодом.", 14, 80, 2, "met_wiz", true, false),
	HEAL("Исцеление", "Лечит вас и союзников рядом.", 20, 200, 3, "met_aqua", true, true),
	TURN_UNDEAD("Изгнание нежити", "Наносит огромный урон нежити вокруг.", 25, 160, 3, "met_aqua", true, true),
	DECOY("Провокация", "Все враги рядом атакуют вас. Даркнесс в восторге.", 10, 200, 2, "met_darkness", false, false),
	DRAIN_TOUCH("Касание истощения", "Высасывает здоровье и ману из цели вплотную.", 5, 60, 3, "met_wiz", true, false),
	EXPLOSION("Взрыв!", "Сильнейшая магия. Уничтожает всё в огромном радиусе... и вас заодно оставляет без сил.", 80, 1200, 10, "megumin_trust", true, false);

	public final String title;
	public final String description;
	public final int mana;
	public final int cooldown;
	public final int spCost;
	/** Story flag required before this skill can be learned, or null when always available. */
	public final String unlockFlag;
	public final boolean magic;
	public final boolean holy;

	Skill(String title, String description, int mana, int cooldown, int spCost, String unlockFlag, boolean magic, boolean holy) {
		this.title = title;
		this.description = description;
		this.mana = mana;
		this.cooldown = cooldown;
		this.spCost = spCost;
		this.unlockFlag = unlockFlag;
		this.magic = magic;
		this.holy = holy;
	}

	public static Skill byName(String name) {
		for (Skill s : values()) {
			if (s.name().equalsIgnoreCase(name)) return s;
		}
		return null;
	}
}
