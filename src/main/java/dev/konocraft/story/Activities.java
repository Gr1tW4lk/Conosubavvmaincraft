package dev.konocraft.story;

import dev.konocraft.entity.CompanionEntity;
import dev.konocraft.fx.Fx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/** Repeatable side activities: daily explosions, Yunyun's duels, Aqua's party tricks. */
public final class Activities {
	private Activities() {
	}

	/** Picks a nice open spot ahead of the player for Megumin's daily explosion. */
	public static void dailyExplosion(ServerPlayerEntity player, DialogueManager.Session session) {
		Vec3d t = Actions.resolve(player, "ahead 30", session);
		session.target = t;
		Story.lookAt(player, t);
	}

	public static void rateExplosion(ServerPlayerEntity player, int score) {
		StoryState s = StoryState.get(player.getServer());
		s.addAffection("megumin", score);
		if (s.affection("megumin") >= 10 && !s.hasFlag("megumin_trust")) {
			s.setFlag("megumin_trust", true);
			player.sendMessage(Text.literal("★ Мегумин доверяет вам. Навык «Взрыв!» можно изучить через карту авантюриста.").formatted(Formatting.RED, Formatting.BOLD), false);
			Progression.sync(player);
		}
		Entity m = Npcs.find(player.getServer(), "megumin");
		if (m != null && score >= 3) Fx.play(player, "hearts", m.getPos(), null);
	}

	/** Magic duel with Yunyun: fire beats wind, wind beats water, water beats fire. */
	public static void yunyunDuel(ServerPlayerEntity player, DialogueManager.Session session, String pick) {
		PlayerData d = Progression.data(player);
		d.flags.remove("duel_win");
		d.flags.remove("duel_lose");
		d.flags.remove("duel_draw");
		d.flags.remove("yy_fire");
		d.flags.remove("yy_water");
		d.flags.remove("yy_wind");
		String[] options = {"fire", "water", "wind"};
		String yy = options[player.getRandom().nextInt(3)];
		d.flags.add("yy_" + yy);
		String result;
		if (yy.equals(pick)) result = "duel_draw";
		else if ((pick.equals("fire") && yy.equals("wind")) || (pick.equals("wind") && yy.equals("water")) || (pick.equals("water") && yy.equals("fire"))) result = "duel_win";
		else result = "duel_lose";
		d.flags.add(result);
		if (result.equals("duel_win")) {
			StoryState.get(player.getServer()).addAffection("yunyun", 1);
			Progression.addExp(player, 40);
		}
		StoryState.get(player.getServer()).markDirty();
	}

	public static void partyTrick(ServerPlayerEntity player) {
		Entity aqua = Npcs.find(player.getServer(), "aqua");
		Vec3d pos = aqua != null ? aqua.getPos() : player.getPos();
		Fx.play(player, "water", pos, null);
		Fx.play(player, "sparkle", pos.add(0, 1, 0), null);
		Fx.play(player, "confetti", pos, null);
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 60, 1));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 20 * 300, 0));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 20 * 300, 0));
	}

	public static void stealGag(ServerPlayerEntity player) {
		Entity m = Npcs.find(player.getServer(), "megumin");
		if (m instanceof CompanionEntity c) Fx.play(player, "sparkle", c.getPos(), null);
	}
}
