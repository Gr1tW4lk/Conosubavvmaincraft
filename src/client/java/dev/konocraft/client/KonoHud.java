package dev.konocraft.client;

import dev.konocraft.client.screen.DialogueScreen;
import dev.konocraft.skill.Skill;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Quest tracker, mana bar, active skill and screen effects (flash, letterbox). */
public final class KonoHud {
	private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

	private KonoHud() {
	}

	public static void render(DrawContext ctx, float tickDelta) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || mc.options.hudHidden) {
			effects(ctx, mc);
			return;
		}
		TextRenderer tr = mc.textRenderer;
		int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
		boolean dialogue = mc.currentScreen instanceof DialogueScreen;
		if (!dialogue) {
			objective(ctx, mc, tr);
			mana(ctx, tr, w, h);
		}
		effects(ctx, mc);
	}

	private static void objective(DrawContext ctx, MinecraftClient mc, TextRenderer tr) {
		if (ClientState.objective == null || ClientState.objective.isEmpty()) return;
		int x = 10, y = 10, width = 190;
		String[] parts = ClientState.objective.split("\n");
		int lines = 0;
		for (String p : parts) lines += tr.wrapLines(Text.literal(p), width).size();
		int boxH = 16 + lines * 10 + (ClientState.objectiveTarget != null ? 12 : 0);
		dev.konocraft.client.screen.KonoUi.panel(ctx, dev.konocraft.client.screen.KonoUi.PANEL_DARK, x - 7, y - 7, width + 16, boxH + 8);
		dev.konocraft.client.screen.KonoUi.icon(ctx, dev.konocraft.client.screen.KonoUi.GEM_LIT, 0, x + 1, y);
		ctx.drawText(tr, Text.literal("Задание"), x + 12, y, 0xFFFFC040, true);
		int ty = y + 12;
		for (int i = 0; i < parts.length; i++) {
			List<OrderedText> wrapped = tr.wrapLines(Text.literal(parts[i]), width);
			for (OrderedText line : wrapped) {
				ctx.drawText(tr, line, x + 2, ty, i == 0 ? 0xFFFFFFFF : 0xFFAADDFF, true);
				ty += 10;
			}
		}
		Vec3d target = ClientState.objectiveTarget;
		if (target != null && mc.player != null) {
			double dx = target.x - mc.player.getX(), dz = target.z - mc.player.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);
			float targetYaw = (float) (MathHelper.atan2(dz, dx) * MathHelper.DEGREES_PER_RADIAN) - 90f;
			float rel = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());
			int idx = Math.floorMod(Math.round(rel / 45f), 8);
			String dir = dist < 6 ? "● здесь" : ARROWS[idx] + " " + (int) dist + " м";
			ctx.drawText(tr, Text.literal(dir), x + 2, ty + 1, 0xFF80FF80, true);
		}
	}

	private static void mana(DrawContext ctx, TextRenderer tr, int w, int h) {
		if (ClientState.maxMana <= 0) return;
		int barW = 81;
		int x = w / 2 - 91 - barW - 8;
		int y = h - 22;
		if (x < 4) x = 4;
		ctx.fill(x - 1, y - 1, x + barW + 1, y + 6, 0xA0000000);
		int fill = (int) (barW * MathHelper.clamp(ClientState.mana / ClientState.maxMana, 0, 1));
		ctx.fill(x, y, x + fill, y + 5, 0xFF3A7BFF);
		ctx.fill(x, y, x + fill, y + 1, 0xFF8AB8FF);
		String m = "МП " + (int) ClientState.mana + "/" + ClientState.maxMana;
		ctx.drawText(tr, Text.literal(m), x, y - 10, 0xFF9EC8FF, true);
		if (!ClientState.skills.isEmpty()) {
			int sel = MathHelper.clamp(ClientState.selectedSkill, 0, ClientState.skills.size() - 1);
			Skill s = Skill.byName(ClientState.skills.get(sel));
			if (s != null) {
				String keyName = KonoKeys.CAST.getBoundKeyLocalizedText().getString();
				String line = "[" + keyName + "] " + s.title;
				int color = ClientState.mana >= s.mana ? 0xFFFFE070 : 0xFF888888;
				ctx.drawText(tr, Text.literal(line), x, y - 20, color, true);
			}
		}
		String lvl = "Ур. " + ClientState.level;
		ctx.drawText(tr, Text.literal(lvl), x + barW - tr.getWidth(lvl), y - 10, 0xFFFFFFFF, true);
	}

	private static void effects(DrawContext ctx, MinecraftClient mc) {
		int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
		if (ClientState.letterboxTicks > 0 && !(mc.currentScreen instanceof DialogueScreen)) {
			int bar = Math.min(h / 9, 40);
			ctx.fill(0, 0, w, bar, 0xFF000000);
			ctx.fill(0, h - bar, w, h, 0xFF000000);
		}
		if (ClientState.heatTicks > 0) {
			float a = (float) ClientState.heatTicks / ClientState.heatMax;
			ctx.fill(0, 0, w, h, ((int) (a * a * 110) << 24) | 0xFF5A1E);
		}
		if (ClientState.flashTicks > 0) {
			float a = (float) ClientState.flashTicks / ClientState.flashMax;
			int alpha = (int) (Math.min(1f, a * 1.4f) * 230);
			ctx.fill(0, 0, w, h, (alpha << 24) | 0xFFF4E0);
		}
		title(ctx, mc, w, h);
	}

	/** Centre-screen title: as big as the window allows, wrapped instead of running off the screen. */
	private static void title(DrawContext ctx, MinecraftClient mc, int w, int h) {
		if (ClientState.titleTicks <= 0 || ClientState.title == null) return;
		TextRenderer tr = mc.textRenderer;
		int t = ClientState.titleTicks, in = ClientState.titleFadeIn, out = ClientState.titleFadeOut;
		int total = in + ClientState.titleStay + out;
		int age = total - t;
		float alpha = age < in ? (float) age / Math.max(1, in) : t < out ? (float) t / Math.max(1, out) : 1f;
		int a = (int) (alpha * 255);
		if (a < 8) return;
		int color = (a << 24) | 0xFFFFFF;
		int avail = w - 24;
		// title
		int tw = Math.max(1, tr.getWidth(ClientState.title));
		float scale = Math.min(4f, (float) avail / tw);
		List<OrderedText> lines;
		if (scale < 1.8f) {
			scale = 1.8f;
			lines = tr.wrapLines(ClientState.title, (int) (avail / scale));
		} else {
			lines = List.of(ClientState.title.asOrderedText());
		}
		int blockH = (int) (lines.size() * 10 * scale);
		float y = h / 2f - blockH - 4;
		for (OrderedText line : lines) {
			ctx.getMatrices().push();
			ctx.getMatrices().translate(w / 2f, y, 0);
			ctx.getMatrices().scale(scale, scale, 1);
			ctx.drawTextWithShadow(tr, line, -tr.getWidth(line) / 2, 0, color);
			ctx.getMatrices().pop();
			y += 10 * scale;
		}
		// subtitle: twice as small, up to three lines
		if (ClientState.subtitle != null && !ClientState.subtitle.getString().isEmpty()) {
			float s = Math.min(2f, (float) avail / Math.max(1, tr.getWidth(ClientState.subtitle)));
			if (s < 1.25f) s = 1.25f;
			List<OrderedText> sub = tr.wrapLines(ClientState.subtitle, (int) (avail / s));
			if (sub.size() > 3) sub = sub.subList(0, 3);
			float sy = h / 2f + 6;
			for (OrderedText line : sub) {
				ctx.getMatrices().push();
				ctx.getMatrices().translate(w / 2f, sy, 0);
				ctx.getMatrices().scale(s, s, 1);
				ctx.drawTextWithShadow(tr, line, -tr.getWidth(line) / 2, 0, color);
				ctx.getMatrices().pop();
				sy += 10 * s;
			}
		}
	}

	public static void tick(MinecraftClient mc) {
		if (ClientState.flashTicks > 0) ClientState.flashTicks--;
		if (ClientState.heatTicks > 0) ClientState.heatTicks--;
		if (ClientState.titleTicks > 0) ClientState.titleTicks--;
		if (ClientState.letterboxTicks > 0) ClientState.letterboxTicks--;
		Shake.tick(mc);
	}

	/** Camera shake without mixins: a zero-sum jitter on the player's look direction. */
	static final class Shake {
		private static float lastYaw, lastPitch;

		static void tick(MinecraftClient mc) {
			if (mc.player == null) return;
			mc.player.setYaw(mc.player.getYaw() - lastYaw);
			mc.player.setPitch(mc.player.getPitch() - lastPitch);
			lastYaw = 0;
			lastPitch = 0;
			if (ClientState.shakeTicks > 0) {
				ClientState.shakeTicks--;
				float s = ClientState.shakeStrength * Math.min(1f, ClientState.shakeTicks / 15f);
				lastYaw = (mc.player.getRandom().nextFloat() - 0.5f) * 3f * s;
				lastPitch = (mc.player.getRandom().nextFloat() - 0.5f) * 3f * s;
				mc.player.setYaw(mc.player.getYaw() + lastYaw);
				mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + lastPitch, -90, 90));
			}
		}
	}
}
