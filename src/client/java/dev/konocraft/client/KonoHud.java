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
		int x = 6, y = 6, width = 190;
		String[] parts = ClientState.objective.split("\n");
		int lines = 0;
		for (String p : parts) lines += tr.wrapLines(Text.literal(p), width).size();
		int boxH = 16 + lines * 10 + (ClientState.objectiveTarget != null ? 12 : 0);
		ctx.fill(x - 3, y - 3, x + width + 4, y + boxH - 2, 0x90000000);
		ctx.fill(x - 3, y - 3, x - 1, y + boxH - 2, 0xFFFFC040);
		ctx.drawText(tr, Text.literal("★ Задание"), x + 2, y, 0xFFFFC040, true);
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
		if (ClientState.flashTicks > 0) {
			float a = (float) ClientState.flashTicks / ClientState.flashMax;
			int alpha = (int) (Math.min(1f, a * 1.4f) * 230);
			ctx.fill(0, 0, w, h, (alpha << 24) | 0xFFF4E0);
		}
	}

	public static void tick(MinecraftClient mc) {
		if (ClientState.flashTicks > 0) ClientState.flashTicks--;
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
