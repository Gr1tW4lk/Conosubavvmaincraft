package dev.konocraft.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.konocraft.KonoCraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Shared look of the KonoCraft screens: nine-sliced pixel-art panels, frames and icons. */
public final class KonoUi {
	private KonoUi() {
	}

	public static final Identifier PANEL_DARK = KonoCraft.id("textures/gui/panel_dark.png");
	public static final Identifier PANEL_PAPER = KonoCraft.id("textures/gui/panel_paper.png");
	public static final Identifier NOTE = KonoCraft.id("textures/gui/note.png");
	public static final Identifier BUTTON = KonoCraft.id("textures/gui/button.png");
	public static final Identifier BUTTON_PAPER = KonoCraft.id("textures/gui/button_paper.png");
	public static final Identifier NAMEPLATE = KonoCraft.id("textures/gui/nameplate.png");
	public static final Identifier PORTRAIT = KonoCraft.id("textures/gui/portrait.png");
	public static final Identifier ICONS = KonoCraft.id("textures/gui/icons.png");

	public static final int GEM = 0, GEM_LIT = 8, ARROW = 16, COIN = 24;

	/** A 48x48 panel texture (corner 10) stretched over the box without stretching its frame. */
	public static void panel(DrawContext ctx, Identifier tex, int x, int y, int w, int h) {
		nine(ctx, tex, x, y, w, h, 0, 0, 48, 48, 10, 48, 48);
	}

	/**
	 * Nine-slice: the corners keep their size, edges stretch along one axis, the middle in both.
	 * (u, v, sw, sh) is the source rectangle inside a texture of texW x texH pixels.
	 */
	public static void nine(DrawContext ctx, Identifier tex, int x, int y, int w, int h, int u, int v, int sw, int sh, int c, int texW, int texH) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		int cw = Math.min(c, w / 2), ch = Math.min(c, h / 2);
		int mw = w - 2 * cw, mh = h - 2 * ch, smw = sw - 2 * c, smh = sh - 2 * c;
		// corners
		ctx.drawTexture(tex, x, y, cw, ch, u, v, cw, ch, texW, texH);
		ctx.drawTexture(tex, x + w - cw, y, cw, ch, u + sw - cw, v, cw, ch, texW, texH);
		ctx.drawTexture(tex, x, y + h - ch, cw, ch, u, v + sh - ch, cw, ch, texW, texH);
		ctx.drawTexture(tex, x + w - cw, y + h - ch, cw, ch, u + sw - cw, v + sh - ch, cw, ch, texW, texH);
		// edges
		if (mw > 0) {
			ctx.drawTexture(tex, x + cw, y, mw, ch, u + c, v, smw, ch, texW, texH);
			ctx.drawTexture(tex, x + cw, y + h - ch, mw, ch, u + c, v + sh - ch, smw, ch, texW, texH);
		}
		if (mh > 0) {
			ctx.drawTexture(tex, x, y + ch, cw, mh, u, v + c, cw, smh, texW, texH);
			ctx.drawTexture(tex, x + w - cw, y + ch, cw, mh, u + sw - cw, v + c, cw, smh, texW, texH);
		}
		// middle
		if (mw > 0 && mh > 0) ctx.drawTexture(tex, x + cw, y + ch, mw, mh, u + c, v + c, smw, smh, texW, texH);
		RenderSystem.disableBlend();
	}

	/** An 8x8 icon from icons.png (u is one of GEM, GEM_LIT, ARROW, COIN). */
	public static void icon(DrawContext ctx, int u, int v, int x, int y) {
		RenderSystem.enableBlend();
		ctx.drawTexture(ICONS, x, y, u, v, 8, 8, 32, 16);
		RenderSystem.disableBlend();
	}
}
