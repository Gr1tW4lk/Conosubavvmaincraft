package dev.konocraft.client.screen;

import dev.konocraft.client.ClientNet;
import dev.konocraft.registry.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual-novel style dialogue box: speaker portrait, name tag, typewriter text and choices.
 * The world keeps running behind it, so cutscenes play out while characters talk.
 */
public class DialogueScreen extends Screen {
	private static final int BOX_HEIGHT = 92;

	private String speakerKey;
	private String speakerName;
	private int color;
	private String fullText;
	private List<String> choices;
	private int auto;
	private int portraitId;
	private float revealed;
	private int age;
	private final List<ButtonWidget> choiceButtons = new ArrayList<>();
	private boolean waitingServer;

	private DialogueScreen() {
		super(Text.literal("Диалог"));
	}

	public static void show(MinecraftClient client, String key, String name, int color, String text, List<String> choices, int auto, int portrait) {
		DialogueScreen screen = client.currentScreen instanceof DialogueScreen d ? d : new DialogueScreen();
		screen.speakerKey = key;
		screen.speakerName = name;
		screen.color = color;
		screen.fullText = text;
		screen.choices = choices;
		screen.auto = auto;
		screen.portraitId = portrait;
		screen.revealed = 0;
		screen.age = 0;
		screen.waitingServer = false;
		if (client.currentScreen != screen) {
			client.setScreen(screen);
		} else {
			screen.rebuild();
		}
	}

	@Override
	protected void init() {
		rebuild();
	}

	private void rebuild() {
		for (ButtonWidget b : choiceButtons) remove(b);
		choiceButtons.clear();
		if (choices == null || choices.isEmpty()) return;
		int w = Math.min(320, width - 40);
		int x = width - w - 20;
		int y = height - BOX_HEIGHT - 16 - choices.size() * 22;
		for (int i = 0; i < choices.size(); i++) {
			final int index = i;
			ButtonWidget b = ButtonWidget.builder(Text.literal((i + 1) + ". " + choices.get(i)), btn -> choose(index))
				.dimensions(x, y + i * 22, w, 20).build();
			b.visible = false;
			choiceButtons.add(b);
			addDrawableChild(b);
		}
	}

	private boolean fullyShown() {
		return fullText == null || revealed >= fullText.length();
	}

	private void choose(int index) {
		if (waitingServer) return;
		waitingServer = true;
		ClientNet.advance(index);
	}

	private void advance() {
		if (auto > 0 || waitingServer) return;
		if (!fullyShown()) {
			revealed = fullText.length();
			return;
		}
		if (choices != null && !choices.isEmpty()) return;
		waitingServer = true;
		ClientNet.advance(-1);
	}

	/** One "click" of the CI story playthrough: reveal the line, then pick a choice or move on. */
	public void autotestStep(int choice) {
		if (waitingServer || auto > 0) return;
		if (!fullyShown()) {
			revealed = fullText.length();
			return;
		}
		if (choices != null && !choices.isEmpty()) {
			choose(Math.floorMod(choice, choices.size()));
			return;
		}
		advance();
	}

	public String autotestLine() {
		return (speakerName == null || speakerName.isEmpty() ? "*" : speakerName) + ": " + fullText
			+ (choices == null || choices.isEmpty() ? "" : " " + choices);
	}

	@Override
	public void tick() {
		age++;
		if (fullText != null && revealed < fullText.length()) {
			float speed = auto > 0 ? 1.6f : 1.25f;
			int before = (int) revealed;
			revealed = Math.min(fullText.length(), revealed + speed);
			if ((int) revealed != before && before % 3 == 0 && client != null && auto == 0) {
				client.getSoundManager().play(PositionedSoundInstance.master(ModSounds.BLIP, 1.0f + (color % 7) * 0.04f, 0.25f));
			}
		}
		boolean showChoices = fullyShown();
		for (ButtonWidget b : choiceButtons) b.visible = showChoices;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		advance();
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_E) {
			advance();
			return true;
		}
		if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9 && choices != null && fullyShown()) {
			int i = keyCode - GLFW.GLFW_KEY_1;
			if (i < choices.size()) {
				choose(i);
				return true;
			}
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			advance();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void renderBackground(DrawContext context) {
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		boolean cinematic = auto > 0;
		if (cinematic) {
			int bar = Math.min(height / 9, 40);
			ctx.fill(0, 0, width, bar, 0xFF000000);
			ctx.fill(0, height - bar, width, height, 0xFF000000);
		}
		boolean hasText = fullText != null && !fullText.isEmpty();
		if (hasText) {
			int x1 = 12, x2 = width - 12;
			int y1 = height - BOX_HEIGHT - 8, y2 = height - 8;
			ctx.fill(x1, y1, x2, y2, 0xD0101018);
			int border = 0xFF000000 | color;
			ctx.fill(x1, y1, x2, y1 + 2, border);
			ctx.fill(x1, y2 - 1, x2, y2, 0x80FFFFFF & border);
			int textX = x1 + 12;
			// Portrait
			Entity e = portraitId >= 0 && client != null && client.world != null ? client.world.getEntityById(portraitId) : null;
			if (e instanceof LivingEntity le && !le.isInvisible()) {
				int px = x1 + 44;
				ctx.enableScissor(x1 + 4, y1 + 4, x1 + 86, y2 - 4);
				float h = le.getHeight();
				int size = (int) (52 / Math.max(1.0f, h / 1.8f));
				// hide the floating name tag, the dialogue box already shows the speaker's name
				Text customName = le.getCustomName();
				le.setCustomName(null);
				InventoryScreen.drawEntity(ctx, px, y2 + (int) (size * h * 0.55f), size, (float) (px - mouseX) * 0.1f, -10f, le);
				le.setCustomName(customName);
				ctx.disableScissor();
				textX = x1 + 94;
			}
			// Name tag
			if (speakerName != null && !speakerName.isEmpty()) {
				int nw = textRenderer.getWidth(speakerName) + 16;
				ctx.fill(textX - 4, y1 - 14, textX - 4 + nw, y1, 0xE0101018);
				ctx.fill(textX - 4, y1 - 14, textX - 4 + nw, y1 - 13, border);
				ctx.drawText(textRenderer, Text.literal(speakerName), textX + 4, y1 - 10, border, true);
			}
			// Text with typewriter effect
			String shown = fullText.substring(0, Math.min(fullText.length(), (int) revealed));
			List<OrderedText> lines = textRenderer.wrapLines(Text.literal(shown), x2 - textX - 14);
			int ty = y1 + 12;
			boolean narrator = speakerName == null || speakerName.isEmpty();
			for (OrderedText line : lines) {
				ctx.drawText(textRenderer, line, textX, ty, narrator ? 0xFFCCCCCC : 0xFFFFFFFF, true);
				ty += 11;
				if (ty > y2 - 12) break;
			}
			if (fullyShown() && auto == 0 && (choices == null || choices.isEmpty()) && (age / 10) % 2 == 0) {
				ctx.drawText(textRenderer, Text.literal("▼"), x2 - 18, y2 - 14, border, false);
			}
			if (auto == 0 && (choices == null || choices.isEmpty())) {
				String hint = "ЛКМ / Пробел — далее";
				ctx.drawText(textRenderer, Text.literal(hint), x2 - textRenderer.getWidth(hint) - 28, y2 - 12, 0x60FFFFFF, false);
			}
		}
		super.render(ctx, mouseX, mouseY, delta);
	}
}
