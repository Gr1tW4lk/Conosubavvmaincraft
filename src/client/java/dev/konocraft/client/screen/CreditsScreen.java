package dev.konocraft.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** End credits. Roll slowly, like the ending of a good episode. */
public class CreditsScreen extends Screen {
	private static final String[] LINES = {
		"§6§lДА БЛАГОСЛОВЯТ БОГИ ЭТОТ КУБИЧЕСКИЙ МИР!",
		"",
		"§7Фан-мод по мотивам «KonoSuba»",
		"",
		"§eВ ролях",
		"Мегумин — величайший маг клана Алых Демонов",
		"Аква — богиня (по её словам)",
		"Даркнесс — крестоносец, щит и загадка",
		"Казума — призрак в зале ожидания",
		"Эрис — очень терпеливая богиня",
		"Луна — сотрудница гильдии, заслуживает отпуска",
		"Виз — лич и худший бизнесмен Акселя",
		"Ванир — всевидящий продавец",
		"Юнъюн — подруга Мегумин (по её словам)",
		"Бельдия — рыцарь, у которого отобрали всё. И голову",
		"Мобильная крепость Разрушитель — в роли себя",
		"",
		"§eГигантские жабы",
		"Жаба №1, Жаба №2, Жаба №3, Жаба №4, Жаба №5",
		"Король жаб (камео)",
		"",
		"§eЛетающая капуста",
		"Вся капуста урожая этого года",
		"",
		"§eОсобая благодарность",
		"Гильдии авантюристов Акселя",
		"Культу Аксис (они очень настаивали)",
		"Лорду Альдерпу за предоставленный особняк",
		"",
		"§eСюжет, код, музыка, модели и текстуры",
		"Claude — для Gr1tW4lk",
		"",
		"§7Персонажи «KonoSuba» принадлежат Нацумэ Акацуки,",
		"§7Курон Мисиме и их издателям. Это некоммерческий фан-проект.",
		"",
		"",
		"§cВЗРЫВ!",
		"",
		"",
		"§7...Конец? Наверное.",
		"§7Свободная игра продолжается: доска заданий,",
		"§7ежедневные взрывы, капустный сезон и Король жаб.",
	};

	private float scroll;

	public CreditsScreen() {
		super(Text.literal("Титры"));
	}

	@Override
	public void tick() {
		scroll += 0.6f;
		if (scroll > LINES.length * 14 + height + 40) close();
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		ctx.fill(0, 0, width, height, 0xFF050308);
		float y = height - (scroll + delta * 0.6f);
		for (String line : LINES) {
			if (y > -12 && y < height) ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(line), width / 2, (int) y, 0xFFFFFFFF);
			y += 14;
		}
		ctx.drawText(textRenderer, Text.literal("Esc — пропустить"), 6, height - 12, 0x60FFFFFF, false);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
