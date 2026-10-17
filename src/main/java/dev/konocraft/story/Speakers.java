package dev.konocraft.story;

import java.util.Map;

/** Display names and name colours of everyone who can speak in a dialogue. */
public final class Speakers {
	public record Speaker(String name, int color) {
	}

	private static final Map<String, Speaker> ALL = Map.ofEntries(
		Map.entry("eris", new Speaker("Эрис", 0xC9B6FF)),
		Map.entry("kazuma", new Speaker("Казума", 0x6FCF5A)),
		Map.entry("aqua", new Speaker("Аква", 0x55D6FF)),
		Map.entry("megumin", new Speaker("Мегумин", 0xFF4A4A)),
		Map.entry("darkness", new Speaker("Даркнесс", 0xFFD15C)),
		Map.entry("luna", new Speaker("Луна", 0xF5A35C)),
		Map.entry("wiz", new Speaker("Виз", 0xB07CFF)),
		Map.entry("vanir", new Speaker("Ванир", 0xD04040)),
		Map.entry("yunyun", new Speaker("Юнъюн", 0xFF8FC8)),
		Map.entry("beldia", new Speaker("Бельдия", 0x9A9AB0)),
		Map.entry("chris", new Speaker("Крис", 0x9AD0FF)),
		Map.entry("narrator", new Speaker("", 0xAAAAAA)),
		Map.entry("system", new Speaker("★", 0xFFE070)),
		Map.entry("alarm", new Speaker("Объявление гильдии", 0xFF5555)),
		Map.entry("town", new Speaker("Горожанин", 0xC8C8C8)),
		Map.entry("townsfolk", new Speaker("Горожанин", 0xC8C8C8)),
		Map.entry("adventurer", new Speaker("Авантюрист", 0xC8B48C)),
		Map.entry("diary", new Speaker("Дневник", 0xE0D0A0)),
		Map.entry("toad", new Speaker("Гигантская жаба", 0x7DB24A)),
		Map.entry("destroyer", new Speaker("Разрушитель", 0xD08040)),
		Map.entry("keele", new Speaker("Киль", 0xA070E0)),
		Map.entry("guard", new Speaker("Стражник", 0xB0C4DE)),
		Map.entry("builder", new Speaker("Строитель", 0xD2A060)),
		Map.entry("smith", new Speaker("Кузнец Брок", 0xC07040)),
		Map.entry("innkeeper", new Speaker("Марта", 0xE0A060)),
		Map.entry("bathkeeper", new Speaker("Юки", 0x80C8E0)),
		Map.entry("sena", new Speaker("Сена", 0x7090D0)),
		Map.entry("alderp", new Speaker("Лорд Алдерп", 0xE0C040)),
		Map.entry("ignis", new Speaker("Игнис Дастинесс", 0xC0C8E8)),
		Map.entry("merchant", new Speaker("Хенрик", 0x90C060)),
		Map.entry("thief", new Speaker("Карманник", 0x80A040)),
		Map.entry("casino_owner", new Speaker("Управляющий Вальтер", 0xD4A030)),
		Map.entry("croupier", new Speaker("Крупье Лили", 0xE070A0))
	);

	private Speakers() {
	}

	public static Speaker get(String key) {
		Speaker s = ALL.get(key);
		return s != null ? s : new Speaker(key, 0xFFFFFF);
	}
}
