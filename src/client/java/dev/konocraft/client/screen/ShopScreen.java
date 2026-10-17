package dev.konocraft.client.screen;

import dev.konocraft.client.ClientNet;
import dev.konocraft.client.ClientState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
			KonoButton b = KonoButton.of(Text.literal("Купить"), x0 + W - 66, y0 + 52 + i * 24, 56, 20, btn -> ClientNet.buy(id, index))
				.tip(Text.literal(e.note()));
			b.active = ClientState.money >= e.price();
			addDrawableChild(b);
		}
		addDrawableChild(KonoButton.of(Text.literal("Уйти"), x0 + W - 66, y0 + height0() - 28, 56, 20, btn -> close()));
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		renderBackground(ctx);
		int x0 = (width - W) / 2, y0 = (height - height0()) / 2;
		KonoUi.panel(ctx, KonoUi.PANEL_DARK, x0, y0, W, height0());
		KonoUi.nine(ctx, KonoUi.NAMEPLATE, x0 + 6, y0 + 4, W - 12, 18, 0, 0, 32, 16, 6, 32, 16);
		KonoUi.icon(ctx, KonoUi.GEM_LIT, 0, x0 + 12, y0 + 9);
		ctx.drawText(textRenderer, title, x0 + 24, y0 + 9, 0xFFFFE0A0, true);
		KonoUi.icon(ctx, KonoUi.COIN, 0, x0 + W - 22, y0 + 9);
		String money = "Кошелёк: " + String.format("%,d", ClientState.money).replace(',', ' ') + " эрис";
		ctx.drawText(textRenderer, Text.literal(money), x0 + W - 26 - textRenderer.getWidth(money), y0 + 9, 0xFFFFD050, true);
		ctx.drawText(textRenderer, Text.literal("«" + greeting + "»"), x0 + 10, y0 + 30, 0xFFCCCCCC, false);
		for (int i = 0; i < entries.size(); i++) {
			Entry e = entries.get(i);
			int y = y0 + 52 + i * 24;
			ctx.fill(x0 + 8, y - 2, x0 + W - 8, y + 22, i % 2 == 0 ? 0x22A0B0FF : 0x10A0B0FF);
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
