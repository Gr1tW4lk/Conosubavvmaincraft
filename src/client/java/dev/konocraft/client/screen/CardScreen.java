package dev.konocraft.client.screen;

import dev.konocraft.client.ClientNet;
import dev.konocraft.client.ClientState;
import dev.konocraft.skill.PlayerClass;
import dev.konocraft.skill.Skill;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** The adventurer card: stats, known skills (pick the active one) and skills you can learn. */
public class CardScreen extends Screen {
	private static final int W = 360, H = 244;

	public CardScreen() {
		super(Text.literal("Карта авантюриста"));
	}

	public void refresh() {
		clearChildren();
		init();
	}

	@Override
	protected void init() {
		int x0 = (width - W) / 2, y0 = (height - H) / 2;
		// Known skills: click to make active
		int y = y0 + 118;
		for (int i = 0; i < ClientState.skills.size(); i++) {
			Skill s = Skill.byName(ClientState.skills.get(i));
			if (s == null) continue;
			final int index = i;
			boolean active = i == ClientState.selectedSkill;
			ButtonWidget b = ButtonWidget.builder(Text.literal((active ? "▶ " : "") + s.title + " (" + s.mana + ")"), btn -> {
					ClientNet.selectSkill(index, true);
					ClientState.selectedSkill = index;
					refresh();
				})
				.dimensions(x0 + 10 + (i % 2) * 86, y + (i / 2) * 21, 84, 20)
				.tooltip(Tooltip.of(Text.literal(s.title + " (клик — сделать активным)\n" + s.description + "\nМана: " + s.mana + ", перезарядка: " + (s.cooldown / 20) + " с")))
				.build();
			addDrawableChild(b);
		}
		// Learnable skills
		for (int i = 0; i < ClientState.learnable.size(); i++) {
			Skill s = Skill.byName(ClientState.learnable.get(i));
			if (s == null) continue;
			int cost = ClientState.learnCost.get(i);
			ButtonWidget b = ButtonWidget.builder(Text.literal(s.title + " — " + cost + " ОН"), btn -> ClientNet.learnSkill(s.name()))
				.dimensions(x0 + W / 2 + 8, y0 + 118 + i * 21, W / 2 - 18, 20)
				.tooltip(Tooltip.of(Text.literal(s.description + "\nСтоимость: " + cost + " очк. навыков")))
				.build();
			b.active = ClientState.skillPoints >= cost;
			addDrawableChild(b);
		}
		addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), btn -> close()).dimensions(x0 + W - 70, y0 + H - 26, 60, 20).build());
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		renderBackground(ctx);
		int x0 = (width - W) / 2, y0 = (height - H) / 2;
		// card body
		ctx.fill(x0, y0, x0 + W, y0 + H, 0xF0E8DCC0);
		ctx.fill(x0, y0, x0 + W, y0 + 24, 0xFF7A4A22);
		ctx.fill(x0 + 2, y0 + 2, x0 + W - 2, y0 + 3, 0xFFC89A5A);
		ctx.drawText(textRenderer, Text.literal("★ КАРТА АВАНТЮРИСТА ★"), x0 + 10, y0 + 8, 0xFFFFE9B0, true);
		String name = client != null && client.player != null ? client.player.getName().getString() : "";
		ctx.drawText(textRenderer, Text.literal(name), x0 + W - 10 - textRenderer.getWidth(name), y0 + 8, 0xFFFFFFFF, true);

		PlayerClass cls = PlayerClass.byName(ClientState.playerClass);
		int tx = x0 + 12, ty = y0 + 32;
		int ink = 0xFF3A2A18;
		ctx.drawText(textRenderer, Text.literal("Класс: " + cls.title), tx, ty, ink, false);
		ctx.drawText(textRenderer, Text.literal("Уровень: " + ClientState.level), tx, ty + 12, ink, false);
		ctx.drawText(textRenderer, Text.literal("Удача: " + ClientState.luck), tx, ty + 24, ink, false);
		String luckNote = ClientState.luck >= 90 ? "(подозрительно высокая)" : ClientState.luck < 25 ? "(очень грустная)" : "";
		ctx.drawText(textRenderer, Text.literal(luckNote), tx + 8, ty + 35, 0xFF8A7A6A, false);
		ctx.drawText(textRenderer, Text.literal("Деньги: " + String.format("%,d", ClientState.money).replace(',', ' ') + " эрис"), tx, ty + 47, 0xFF8A5A00, false);
		ctx.drawText(textRenderer, Text.literal("Очки навыков: " + ClientState.skillPoints), tx, ty + 59, 0xFF1A5A9A, false);
		// exp bar
		int bx = x0 + W / 2 + 8, by = ty;
		ctx.drawText(textRenderer, Text.literal("Опыт: " + ClientState.exp + " / " + ClientState.expNext), bx, by, ink, false);
		ctx.fill(bx, by + 11, bx + W / 2 - 20, by + 17, 0xFF5A4A3A);
		ctx.fill(bx, by + 11, bx + (int) ((W / 2f - 20) * ClientState.exp / Math.max(1, ClientState.expNext)), by + 17, 0xFF6AD04A);
		ctx.drawText(textRenderer, Text.literal("Мана: " + (int) ClientState.mana + " / " + ClientState.maxMana), bx, by + 22, ink, false);
		ctx.fill(bx, by + 33, bx + W / 2 - 20, by + 39, 0xFF3A3A5A);
		ctx.fill(bx, by + 33, bx + (int) ((W / 2f - 20) * ClientState.mana / Math.max(1, ClientState.maxMana)), by + 39, 0xFF4A8AFF);
		if (!ClientState.registered) {
			ctx.drawText(textRenderer, Text.literal("Не зарегистрирован в гильдии"), bx, by + 46, 0xFFB03030, false);
		} else if (!ClientState.bounty.isEmpty()) {
			ctx.drawText(textRenderer, Text.literal(ClientState.bounty), bx, by + 46, 0xFF6A3A00, false);
		}
		ctx.fill(x0 + 8, y0 + 102, x0 + W - 8, y0 + 103, 0xFF9A7A50);
		ctx.drawText(textRenderer, Text.literal("Ваши навыки (R — применить):"), x0 + 10, y0 + 106, ink, false);
		ctx.drawText(textRenderer, Text.literal("Можно изучить:"), x0 + W / 2 + 8, y0 + 106, ink, false);
		if (ClientState.skills.isEmpty()) {
			ctx.drawText(textRenderer, Text.literal("Пока ни одного навыка."), x0 + 12, y0 + 122, 0xFF7A6A5A, false);
		}
		if (ClientState.learnable.isEmpty()) {
			ctx.drawText(textRenderer, Text.literal("Знакомьтесь с людьми,"), x0 + W / 2 + 10, y0 + 122, 0xFF7A6A5A, false);
			ctx.drawText(textRenderer, Text.literal("чтобы учиться новому."), x0 + W / 2 + 10, y0 + 133, 0xFF7A6A5A, false);
		}
		super.render(ctx, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
