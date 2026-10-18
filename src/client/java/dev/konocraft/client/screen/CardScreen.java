package dev.konocraft.client.screen;

import dev.konocraft.client.ClientNet;
import dev.konocraft.client.ClientState;
import dev.konocraft.skill.PlayerClass;
import dev.konocraft.skill.Skill;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** The adventurer card: stats, known skills (pick the active one) and skills you can learn. */
public class CardScreen extends Screen {
	private static final int ROW = 18, LIST_TOP = 118;
	private int W = 360, H = 244;
	private final java.util.List<KonoButton> known = new java.util.ArrayList<>();
	private final java.util.List<KonoButton> learn = new java.util.ArrayList<>();
	private int knownScroll, learnScroll;

	public CardScreen() {
		super(Text.literal("Карта авантюриста"));
	}

	public void refresh() {
		clearChildren();
		init();
	}

	private int listBottom() {
		return (height - H) / 2 + H - 34;
	}

	private int visibleRows() {
		return Math.max(1, (listBottom() - ((height - H) / 2 + LIST_TOP)) / ROW);
	}

	@Override
	protected void init() {
		// the card fits any window and GUI scale: long skill lists scroll instead of spilling out
		W = Math.min(380, width - 16);
		int rows = Math.max(ClientState.skills.size(), ClientState.learnable.size());
		H = Math.max(200, Math.min(height - 12, LIST_TOP + 34 + Math.max(3, rows) * ROW + 4));
		int x0 = (width - W) / 2;
		int colW = W / 2 - 16;
		known.clear();
		learn.clear();
		for (int i = 0; i < ClientState.skills.size(); i++) {
			Skill s = Skill.byName(ClientState.skills.get(i));
			if (s == null) continue;
			final int index = i;
			boolean active = i == ClientState.selectedSkill;
			KonoButton b = new KonoButton(x0 + 10, 0, colW, ROW - 2, Text.literal((active ? "▶ " : "") + s.title + " · " + s.mana), btn -> {
					ClientNet.selectSkill(index, true);
					ClientState.selectedSkill = index;
					refresh();
				}, KonoButton.Style.PAPER)
				.tip(Text.literal(s.title + " (клик — сделать активным)\n" + s.description + "\nМана: " + s.mana + ", перезарядка: " + (s.cooldown / 20) + " с"));
			known.add(b);
			addDrawableChild(b);
		}
		for (int i = 0; i < ClientState.learnable.size(); i++) {
			Skill s = Skill.byName(ClientState.learnable.get(i));
			if (s == null) continue;
			int cost = ClientState.learnCost.get(i);
			KonoButton b = new KonoButton(x0 + W / 2 + 6, 0, colW, ROW - 2, Text.literal(s.title + " — " + cost + " ОН"),
				btn -> ClientNet.learnSkill(s.name()), KonoButton.Style.PAPER)
				.tip(Text.literal(s.description + "\nСтоимость: " + cost + " очк. навыков"));
			b.active = ClientState.skillPoints >= cost;
			learn.add(b);
			addDrawableChild(b);
		}
		int y0 = (height - H) / 2;
		addDrawableChild(new KonoButton(x0 + W - 72, y0 + H - 26, 60, 20, Text.literal("Закрыть"), btn -> close(), KonoButton.Style.PAPER));
		layoutLists();
	}

	/** Places the visible slice of both lists and hides the rest. */
	private void layoutLists() {
		int top = (height - H) / 2 + LIST_TOP, rows = visibleRows();
		knownScroll = Math.max(0, Math.min(knownScroll, known.size() - rows));
		learnScroll = Math.max(0, Math.min(learnScroll, learn.size() - rows));
		for (int i = 0; i < known.size(); i++) {
			int r = i - knownScroll;
			known.get(i).visible = r >= 0 && r < rows;
			known.get(i).setY(top + r * ROW);
		}
		for (int i = 0; i < learn.size(); i++) {
			int r = i - learnScroll;
			learn.get(i).visible = r >= 0 && r < rows;
			learn.get(i).setY(top + r * ROW);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		int step = amount > 0 ? -1 : 1;
		if (mouseX < width / 2.0) knownScroll += step;
		else learnScroll += step;
		layoutLists();
		return true;
	}

	private void scrollHint(DrawContext ctx, int x, int w, int scroll, int size) {
		int rows = visibleRows();
		if (size <= rows) return;
		int y = listBottom() + 1;
		String t = (scroll > 0 ? "▲ " : "") + (scroll + 1) + "–" + Math.min(size, scroll + rows) + " из " + size + (scroll + rows < size ? " ▼" : "");
		ctx.drawText(textRenderer, Text.literal(t), x, y, 0xFF7A5A3A, false);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		renderBackground(ctx);
		int x0 = (width - W) / 2, y0 = (height - H) / 2;
		layoutLists();
		// card body
		KonoUi.panel(ctx, KonoUi.PANEL_PAPER, x0, y0, W, H);
		ctx.fill(x0 + 5, y0 + 5, x0 + W - 5, y0 + 24, 0xFF7A4A22);
		ctx.fill(x0 + 5, y0 + 23, x0 + W - 5, y0 + 24, 0xFFC89A5A);
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
		scrollHint(ctx, x0 + 12, W / 2, knownScroll, known.size());
		scrollHint(ctx, x0 + W / 2 + 8, W / 2, learnScroll, learn.size());
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
