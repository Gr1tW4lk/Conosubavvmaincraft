package dev.konocraft.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * Pixel-art button in the KonoCraft style. DARK: navy with a golden frame, PAPER: wood on the
 * parchment screens, CHOICE: a dialogue answer with a gem bullet, left-aligned and possibly two lines.
 */
public class KonoButton extends ButtonWidget {
	public enum Style {
		DARK, PAPER, CHOICE
	}

	private final Style style;
	private long appearAt;

	public KonoButton(int x, int y, int w, int h, Text message, PressAction onPress, Style style) {
		super(x, y, w, h, message, onPress, DEFAULT_NARRATION_SUPPLIER);
		this.style = style;
	}

	public static KonoButton of(Text message, int x, int y, int w, int h, PressAction onPress) {
		return new KonoButton(x, y, w, h, message, onPress, Style.DARK);
	}

	public KonoButton tip(Text text) {
		setTooltip(Tooltip.of(text));
		return this;
	}

	/** Slides in from the right, starting after the given delay. */
	public void appear(long delayMs) {
		appearAt = Util.getMeasuringTimeMs() + delayMs;
	}

	/** Height a choice button needs for its text (one or two lines). */
	public static int choiceHeight(TextRenderer tr, Text message, int width) {
		int lines = Math.min(2, tr.wrapLines(message, width - 22).size());
		return lines <= 1 ? 20 : 30;
	}

	@Override
	public void renderButton(DrawContext ctx, int mouseX, int mouseY, float delta) {
		MinecraftClient mc = MinecraftClient.getInstance();
		TextRenderer tr = mc.textRenderer;
		float slide = 0;
		if (appearAt > 0) {
			float t = MathHelper.clamp((Util.getMeasuringTimeMs() - appearAt) / 180f, 0, 1);
			if (t <= 0) return;
			slide = (1 - t) * (1 - t) * 24;
		}
		ctx.getMatrices().push();
		ctx.getMatrices().translate(slide, 0, 0);
		int state = !active ? 2 : isSelected() ? 1 : 0;
		KonoUi.nine(ctx, style == Style.PAPER ? KonoUi.BUTTON_PAPER : KonoUi.BUTTON, getX(), getY(), width, height, 0, state * 20, 64, 20, 5, 64, 60);
		int color;
		if (style == Style.PAPER) color = !active ? 0xFF6A5E50 : isSelected() ? 0xFF2A1404 : 0xFF3A2410;
		else color = !active ? 0xFF8A8A96 : isSelected() ? 0xFFFFE9A0 : 0xFFF2F2FF;
		boolean shadow = style != Style.PAPER;
		if (style == Style.CHOICE) {
			KonoUi.icon(ctx, isSelected() ? KonoUi.GEM_LIT : KonoUi.GEM, 0, getX() + 6, getY() + (height - 8) / 2);
			List<OrderedText> lines = tr.wrapLines(getMessage(), width - 22);
			int n = Math.min(2, lines.size());
			int ty = getY() + (height - n * 10) / 2 + 1;
			for (int i = 0; i < n; i++) ctx.drawText(tr, lines.get(i), getX() + 18 + (isSelected() ? 2 : 0), ty + i * 10, color, true);
		} else {
			int tw = tr.getWidth(getMessage());
			OrderedText text = tw > width - 8 ? net.minecraft.util.Language.getInstance().reorder(tr.trimToWidth(getMessage(), width - 12)) : getMessage().asOrderedText();
			ctx.drawText(tr, text, getX() + (width - Math.min(tw, width - 12)) / 2, getY() + (height - 8) / 2, color, shadow);
		}
		ctx.getMatrices().pop();
	}
}
