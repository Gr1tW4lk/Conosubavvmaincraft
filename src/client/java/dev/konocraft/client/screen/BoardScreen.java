package dev.konocraft.client.screen;

import dev.konocraft.client.ClientNet;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

/** The guild quest board with today's bounties pinned on it. */
public class BoardScreen extends Screen {
	public record Bounty(String id, String title, String description, int count, long money, int exp) {
	}

	private final String current;
	private final int progress;
	private final List<Bounty> bounties;
	private static final int CARD_W = 120, CARD_H = 150;

	public BoardScreen(String current, int progress, List<Bounty> bounties) {
		super(Text.literal("Доска заданий"));
		this.current = current;
		this.progress = progress;
		this.bounties = bounties;
	}

	private int x0() {
		int n = Math.max(1, bounties.size());
		return (width - (n * (CARD_W + 10) - 10)) / 2;
	}

	@Override
	protected void init() {
		int y0 = (height - CARD_H) / 2;
		for (int i = 0; i < bounties.size(); i++) {
			final int index = i;
			Bounty b = bounties.get(i);
			boolean taken = b.id().equals(current);
			ButtonWidget btn = ButtonWidget.builder(Text.literal(taken ? "Взято" : "Взять"), w -> {
				ClientNet.acceptBounty(index);
				close();
			}).dimensions(x0() + i * (CARD_W + 10) + 10, y0 + CARD_H - 26, CARD_W - 20, 20).build();
			btn.active = !taken;
			addDrawableChild(btn);
		}
		if (!current.isEmpty()) {
			addDrawableChild(ButtonWidget.builder(Text.literal("Отказаться от текущего"), w -> {
				ClientNet.acceptBounty(-1);
				close();
			}).dimensions(width / 2 - 80, y0 + CARD_H + 12, 160, 20).build());
		}
		addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), w -> close()).dimensions(width / 2 - 40, y0 + CARD_H + 36, 80, 20).build());
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		renderBackground(ctx);
		int y0 = (height - CARD_H) / 2;
		ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("ДОСКА ЗАДАНИЙ ГИЛЬДИИ АКСЕЛЯ"), width / 2, y0 - 28, 0xFFFFD27A);
		ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(bounties.isEmpty() ? "Сегодня заданий нет. Приходите завтра!" : "Выберите задание. Одновременно можно выполнять только одно."), width / 2, y0 - 16, 0xFFCCCCCC);
		for (int i = 0; i < bounties.size(); i++) {
			Bounty b = bounties.get(i);
			int x = x0() + i * (CARD_W + 10);
			int tilt = (i - 1) * 2;
			ctx.fill(x, y0 + tilt, x + CARD_W, y0 + CARD_H + tilt, 0xFFEFE2C0);
			ctx.fill(x, y0 + tilt, x + CARD_W, y0 + 3 + tilt, 0xFFB08A50);
			ctx.fill(x + CARD_W / 2 - 3, y0 - 3 + tilt, x + CARD_W / 2 + 3, y0 + 3 + tilt, 0xFFC03030);
			int ty = y0 + 8 + tilt;
			for (OrderedText line : textRenderer.wrapLines(Text.literal(b.title()), CARD_W - 12)) {
				ctx.drawText(textRenderer, line, x + 6, ty, 0xFF5A1A0A, false);
				ty += 10;
			}
			ty += 3;
			for (OrderedText line : textRenderer.wrapLines(Text.literal(b.description()), CARD_W - 12)) {
				if (ty > y0 + CARD_H - 54 + tilt) break;
				ctx.drawText(textRenderer, line, x + 6, ty, 0xFF3A2A1A, false);
				ty += 9;
			}
			String reward = String.format("%,d", b.money()).replace(',', ' ') + " эрис";
			ctx.drawText(textRenderer, Text.literal("Награда:"), x + 6, y0 + CARD_H - 50 + tilt, 0xFF5A4A3A, false);
			ctx.drawText(textRenderer, Text.literal(reward), x + 6, y0 + CARD_H - 40 + tilt, 0xFF8A5A00, false);
			ctx.drawText(textRenderer, Text.literal("+" + b.exp() + " опыта"), x + 70, y0 + CARD_H - 40 + tilt, 0xFF2A7A2A, false);
			if (b.id().equals(current)) {
				ctx.drawText(textRenderer, Text.literal("Прогресс: " + progress + "/" + b.count()), x + 6, y0 + CARD_H - 62 + tilt, 0xFF1A5A9A, false);
			}
		}
		super.render(ctx, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
