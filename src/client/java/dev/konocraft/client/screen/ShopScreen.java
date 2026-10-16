package dev.konocraft.client.screen;

import dev.konocraft.client.ClientNet;
import dev.konocraft.client.ClientState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

/** A simple shop window: item, price, a joke of a description, and a buy button. */
public class ShopScreen extends Screen {
	public record Entry(ItemStack stack, long price, String note) {
	}

	private final String id;
	private final String greeting;
	private final List<Entry> entries;
	private static final int W = 340;

	public ShopScreen(String id, String title, String greeting, List<Entry> entries) {
		super(Text.literal(title));
		this.id = id;
		this.greeting = greeting;
		this.entries = entries;
	}

	private int height0() {
		return 60 + entries.size() * 24 + 30;
	}

	public void refresh() {
		clearChildren();
		init();
	}

	@Override
	protected void init() {
		int x0 = (width - W) / 2, y0 = (height - height0()) / 2;
		for (int i = 0; i < entries.size(); i++) {
			final int index = i;
			Entry e = entries.get(i);
			ButtonWidget b = ButtonWidget.builder(Text.literal("Купить"), btn -> ClientNet.buy(id, index))
				.dimensions(x0 + W - 64, y0 + 52 + i * 24, 56, 20)
				.tooltip(Tooltip.of(Text.literal(e.note())))
				.build();
			b.active = ClientState.money >= e.price();
			addDrawableChild(b);
		}
		addDrawableChild(ButtonWidget.builder(Text.literal("Уйти"), btn -> close()).dimensions(x0 + W - 64, y0 + height0() - 26, 56, 20).build());
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		renderBackground(ctx);
		int x0 = (width - W) / 2, y0 = (height - height0()) / 2;
		ctx.fill(x0, y0, x0 + W, y0 + height0(), 0xF0201A28);
		ctx.fill(x0, y0, x0 + W, y0 + 22, 0xFF5A3A7A);
		ctx.drawText(textRenderer, title, x0 + 10, y0 + 7, 0xFFFFE0FF, true);
		String money = "Кошелёк: " + String.format("%,d", ClientState.money).replace(',', ' ') + " эрис";
		ctx.drawText(textRenderer, Text.literal(money), x0 + W - 10 - textRenderer.getWidth(money), y0 + 7, 0xFFFFD050, true);
		ctx.drawText(textRenderer, Text.literal("«" + greeting + "»"), x0 + 10, y0 + 30, 0xFFCCCCCC, false);
		for (int i = 0; i < entries.size(); i++) {
			Entry e = entries.get(i);
			int y = y0 + 52 + i * 24;
			ctx.fill(x0 + 6, y - 2, x0 + W - 6, y + 22, i % 2 == 0 ? 0x30FFFFFF : 0x18FFFFFF);
			ctx.drawItem(e.stack(), x0 + 10, y + 2);
			ctx.drawItemInSlot(textRenderer, e.stack(), x0 + 10, y + 2);
			ctx.drawText(textRenderer, e.stack().getName(), x0 + 32, y + 2, 0xFFFFFFFF, false);
			String price = String.format("%,d", e.price()).replace(',', ' ') + " эрис";
			ctx.drawText(textRenderer, Text.literal(price), x0 + 32, y + 12, ClientState.money >= e.price() ? 0xFFFFD050 : 0xFFAA5555, false);
		}
		super.render(ctx, mouseX, mouseY, delta);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
