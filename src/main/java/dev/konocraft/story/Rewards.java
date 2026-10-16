package dev.konocraft.story;

import dev.konocraft.registry.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Experience, bounty money and quest progress for defeated monsters. */
public final class Rewards {
	private Rewards() {
	}

	public static void onKill(MinecraftServer server, LivingEntity dead, ServerPlayerEntity killer) {
		EntityType<?> type = dead.getType();
		StoryState s = StoryState.get(server);
		WorldEvents.onKill(server, dead);
		int exp = 0;
		long money = 0;
		if (type == ModEntities.GIANT_TOAD) {
			exp = 30;
			money = 1500;
			if (s.stage == Story.CH1_TOADS) Story.addProgress(server, 1, Story.TOAD_GOAL, Story.CH1_REPORT);
		} else if (type == ModEntities.TOAD_KING) {
			exp = 400;
			money = 80000;
		} else if (type == ModEntities.FLYING_CABBAGE) {
			exp = 45;
			money = 3000;
			if (s.stage == Story.CH3_CABBAGE) {
				int before = s.questCounter;
				Story.addProgress(server, 1, Story.CABBAGE_GOAL, Story.CH3_REPORT);
				if (before < Story.CABBAGE_GOAL && s.stage == Story.CH3_REPORT && killer != null) {
					CabbageEvent.stop(server);
					DialogueManager.start(killer, "ch3_cabbage_done");
				}
			}
			CabbageEvent.onCaught(server);
		} else if (type == ModEntities.GOBLIN) {
			exp = 20;
			money = 800;
		} else if (type == ModEntities.UNDEAD_KNIGHT) {
			exp = 40;
			money = 1500;
		} else if (type == ModEntities.DESTROYER_GOLEM) {
			exp = 60;
			money = 2500;
		} else if (dead instanceof HostileEntity) {
			exp = 10;
		}
		if (killer == null) return;
		if (exp > 0) Progression.addExp(killer, exp);
		if (money > 0) Progression.addMoney(killer, money);
		Bounties.onKill(killer, type);
	}
}
