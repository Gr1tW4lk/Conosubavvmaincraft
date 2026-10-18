package dev.konocraft.client;

import dev.konocraft.client.screen.CardScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class KonoKeys {
	public static final KeyBinding CAST = new KeyBinding("key.konocraft.cast", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.konocraft");
	public static final KeyBinding NEXT_SKILL = new KeyBinding("key.konocraft.next_skill", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.konocraft");
	public static final KeyBinding CARD = new KeyBinding("key.konocraft.card", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.konocraft");
	public static final KeyBinding MAP = new KeyBinding("key.konocraft.map", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.konocraft");

	private KonoKeys() {
	}

	public static void register() {
		KeyBindingHelper.registerKeyBinding(CAST);
		KeyBindingHelper.registerKeyBinding(NEXT_SKILL);
		KeyBindingHelper.registerKeyBinding(CARD);
		KeyBindingHelper.registerKeyBinding(MAP);
	}

	private static boolean conflictsChecked;

	/**
	 * Iris binds R (reload shaders) and K (toggle shaders) by default. A key drives only one binding, so
	 * unless the player changed them, those two are unbound to keep casting and the card working.
	 */
	private static void freeConflicts(MinecraftClient mc) {
		conflictsChecked = true;
		KeyBinding[] ours = {CAST, NEXT_SKILL, CARD, MAP};
		boolean changed = false;
		for (KeyBinding other : mc.options.allKeys) {
			if (!other.getTranslationKey().startsWith("iris.") || !other.isDefault() || other.isUnbound()) continue;
			for (KeyBinding k : ours) {
				if (other.equals(k)) {
					other.setBoundKey(InputUtil.UNKNOWN_KEY);
					changed = true;
					break;
				}
			}
		}
		if (changed) {
			KeyBinding.updateKeysByCode();
			mc.options.write();
		}
	}

	public static void tick(MinecraftClient mc) {
		if (!conflictsChecked && mc.options != null) freeConflicts(mc);
		if (mc.player == null) return;
		while (CAST.wasPressed()) ClientNet.castSkill();
		while (NEXT_SKILL.wasPressed()) ClientNet.selectSkill(1, false);
		while (CARD.wasPressed()) {
			if (mc.currentScreen == null) mc.setScreen(new CardScreen());
		}
		while (MAP.wasPressed()) {
			if (mc.currentScreen == null) mc.setScreen(new dev.konocraft.client.screen.MapScreen());
		}
	}
}
