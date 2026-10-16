package dev.konocraft.story;

import net.minecraft.server.network.ServerPlayerEntity;

/** Chooses which dialogue a character starts, depending on the story stage. */
public final class Talk {
	private Talk() {
	}

	public static String dialogueFor(ServerPlayerEntity player, String key) {
		StoryState s = StoryState.get(player.getServer());
		PlayerData d = s.player(player.getUuid());
		int st = s.stage;
		boolean party = s.party.contains(key);
		return switch (key) {
			case "eris" -> !d.prologueDone ? "prologue" : "eris_idle";
			case "kazuma" -> s.hasFlag("kazuma_alive") ? "kazuma_idle" : "kazuma_ghost";
			case "luna" -> switch (st) {
				case Story.CH1_GUILD -> "ch1_luna_register";
				case Story.CH1_TOADS -> "luna_toads_wait";
				case Story.CH1_REPORT -> "ch1_luna_report";
				case Story.CH2_MEET -> "luna_ch2_hint";
				case Story.CH2_REPORT -> "ch2_luna_report";
				case Story.CH3_CABBAGE -> "luna_cabbage_wait";
				case Story.CH3_REPORT -> "ch3_luna_report";
				case Story.CH4_REPORT -> "ch4_luna_report";
				case Story.CH5_DEFENSE, Story.CH5_FIGHT -> "luna_defense";
				default -> d.registered ? "luna_menu" : "luna_unregistered";
			};
			case "aqua" -> {
				if (party) yield "aqua_party";
				yield st <= Story.CH1_GUILD ? "ch1_aqua_before" : "aqua_wait";
			}
			case "megumin" -> {
				if (st == Story.CH2_MEET) yield "ch2_megumin_meet";
				if (party) yield "megumin_party";
				yield "megumin_wait";
			}
			case "darkness" -> {
				if (st == Story.CH3_MEET) yield "ch3_darkness_meet";
				if (party) yield "darkness_party";
				yield "darkness_wait";
			}
			case "wiz" -> {
				if (st == Story.CH5_WIZ) yield "ch5_wiz";
				if (st == Story.CH5_DEFENSE || st == Story.CH5_FIGHT) yield "wiz_defense";
				yield s.hasFlag("met_wiz") ? "wiz_menu" : "wiz_first";
			}
			case "vanir" -> s.hasFlag("met_vanir") ? "vanir_menu" : "vanir_first";
			case "yunyun" -> s.hasFlag("met_yunyun") ? "yunyun_menu" : "yunyun_first";
			case "beldia" -> "beldia_idle";
			case "townsfolk" -> "town_" + (1 + player.getRandom().nextInt(14));
			default -> null;
		};
	}
}
