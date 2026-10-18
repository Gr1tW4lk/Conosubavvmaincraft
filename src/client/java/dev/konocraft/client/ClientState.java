package dev.konocraft.client;

import net.minecraft.util.math.Vec3d;

import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Everything the server told this client about the player and the story. */
public final class ClientState {
	public static String playerClass = "NONE";
	public static int level = 1;
	public static int exp;
	public static int expNext = 72;
	public static int skillPoints;
	public static float mana;
	public static int maxMana = 40;
	public static long money;
	public static int luck;
	public static final List<String> skills = new ArrayList<>();
	public static int selectedSkill;
	public static boolean registered;
	public static final List<String> learnable = new ArrayList<>();
	public static final List<Integer> learnCost = new ArrayList<>();
	public static String bounty = "";

	public static String objective = "";
	public static Vec3d objectiveTarget;

	public static String music = "";

	public static Text title, subtitle;
	public static int titleTicks, titleFadeIn, titleStay, titleFadeOut;
	/** Orange glow of a nearby explosion fading out over the screen. */
	public static int heatTicks, heatMax = 1;
	public static int flashTicks;
	public static int flashMax = 1;
	public static int shakeTicks;
	public static float shakeStrength;
	public static int letterboxTicks;

	private ClientState() {
	}

	public static void reset() {
		objective = "";
		objectiveTarget = null;
		music = "";
		flashTicks = 0;
		shakeTicks = 0;
		letterboxTicks = 0;
	}
}
