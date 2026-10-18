package dev.konocraft.net;

import dev.konocraft.skill.Skill;
import dev.konocraft.skill.Skills;
import dev.konocraft.story.Bounties;
import dev.konocraft.story.DialogueManager;
import dev.konocraft.story.PlayerData;
import dev.konocraft.story.Progression;
import dev.konocraft.story.Shops;
import dev.konocraft.story.Story;
import dev.konocraft.story.StoryState;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class ServerNet {
	private ServerNet() {
	}

	public static void init() {
		ServerPlayNetworking.registerGlobalReceiver(Packets.DIALOGUE_ADVANCE, (server, player, handler, buf, sender) -> {
			int choice = buf.readVarInt();
			server.execute(() -> DialogueManager.advance(player, choice));
		});
		ServerPlayNetworking.registerGlobalReceiver(Packets.CAST_SKILL, (server, player, handler, buf, sender) ->
			server.execute(() -> Skills.castSelected(player)));
		ServerPlayNetworking.registerGlobalReceiver(Packets.SELECT_SKILL, (server, player, handler, buf, sender) -> {
			int delta = buf.readVarInt();
			boolean absolute = buf.readBoolean();
			server.execute(() -> Skills.select(player, delta, absolute));
		});
		ServerPlayNetworking.registerGlobalReceiver(Packets.LEARN_SKILL, (server, player, handler, buf, sender) -> {
			String name = buf.readString();
			server.execute(() -> {
				Skill skill = Skill.byName(name);
				if (skill != null) Skills.learn(player, skill);
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(Packets.SHOP_BUY, (server, player, handler, buf, sender) -> {
			String shop = buf.readString();
			int index = buf.readVarInt();
			server.execute(() -> Shops.buy(player, shop, index));
		});
		ServerPlayNetworking.registerGlobalReceiver(Packets.BOARD_ACCEPT, (server, player, handler, buf, sender) -> {
			int index = buf.readVarInt();
			server.execute(() -> Bounties.accept(player, index));
		});
		ServerPlayNetworking.registerGlobalReceiver(Packets.REQUEST_SYNC, (server, player, handler, buf, sender) ->
			server.execute(() -> {
				Progression.sync(player);
				Story.syncObjective(player);
			}));
	}

	public static void sendLine(ServerPlayerEntity player, String speakerKey, String name, int color, String text,
								List<String> choices, int auto, int portraitId) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(speakerKey);
		buf.writeString(name);
		buf.writeInt(color);
		buf.writeString(text, 32767);
		buf.writeVarInt(auto);
		buf.writeVarInt(portraitId);
		buf.writeVarInt(choices.size());
		for (String c : choices) buf.writeString(c);
		ServerPlayNetworking.send(player, Packets.DIALOGUE_LINE, buf);
	}

	public static void sendClose(ServerPlayerEntity player) {
		ServerPlayNetworking.send(player, Packets.DIALOGUE_CLOSE, PacketByteBufs.create());
	}

	public static void sendPlayerData(ServerPlayerEntity player, PlayerData d) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(d.playerClass.name());
		buf.writeVarInt(d.level);
		buf.writeVarInt(d.exp);
		buf.writeVarInt(PlayerData.expForNext(d.level));
		buf.writeVarInt(d.skillPoints);
		buf.writeFloat(d.mana);
		buf.writeVarInt(d.maxMana());
		buf.writeVarLong(d.money);
		buf.writeVarInt(d.luck());
		List<Skill> skills = d.skillList();
		buf.writeVarInt(skills.size());
		for (Skill s : skills) buf.writeString(s.name());
		buf.writeVarInt(d.selectedSkill);
		buf.writeBoolean(d.registered);
		// Skills that can be learned right now (unlocked by story flags)
		StoryState state = StoryState.get(player.getServer());
		int count = 0;
		for (Skill s : Skill.values()) if (Skills.unlocked(player, state, d, s)) count++;
		buf.writeVarInt(count);
		for (Skill s : Skill.values()) {
			if (Skills.unlocked(player, state, d, s)) {
				buf.writeString(s.name());
				buf.writeVarInt(d.playerClass.costOf(s));
			}
		}
		buf.writeString(d.bounty == null ? "" : Bounties.describe(d));
		ServerPlayNetworking.send(player, Packets.PLAYER_SYNC, buf);
	}

	public static void sendMana(ServerPlayerEntity player, PlayerData d) {
		sendPlayerData(player, d);
	}

	public static void sendObjective(ServerPlayerEntity player, String text, Vec3d target) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(text == null ? "" : text);
		buf.writeBoolean(target != null);
		if (target != null) {
			buf.writeDouble(target.x);
			buf.writeDouble(target.y);
			buf.writeDouble(target.z);
		}
		ServerPlayNetworking.send(player, Packets.OBJECTIVE, buf);
	}

	public static void sendFx(ServerPlayerEntity player, int kind, int duration, float strength) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(kind);
		buf.writeVarInt(duration);
		buf.writeFloat(strength);
		ServerPlayNetworking.send(player, Packets.FX, buf);
	}

	public static void sendFxAt(ServerPlayerEntity player, int kind, net.minecraft.util.math.Vec3d pos, int duration, float scale) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(kind);
		buf.writeDouble(pos.x);
		buf.writeDouble(pos.y);
		buf.writeDouble(pos.z);
		buf.writeVarInt(duration);
		buf.writeFloat(scale);
		ServerPlayNetworking.send(player, Packets.FX_AT, buf);
	}

	public static void sendTitle(ServerPlayerEntity player, net.minecraft.text.Text title, net.minecraft.text.Text subtitle, int fadeIn, int stay, int fadeOut) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeText(title);
		buf.writeText(subtitle == null ? net.minecraft.text.Text.empty() : subtitle);
		buf.writeVarInt(fadeIn);
		buf.writeVarInt(stay);
		buf.writeVarInt(fadeOut);
		ServerPlayNetworking.send(player, Packets.TITLE, buf);
	}

	public static void sendMusic(ServerPlayerEntity player, String music) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(music == null ? "" : music);
		ServerPlayNetworking.send(player, Packets.MUSIC, buf);
	}

	public static void sendToast(ServerPlayerEntity player, String title, String text) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(title);
		buf.writeString(text);
		ServerPlayNetworking.send(player, Packets.TOAST, buf);
	}

	public static void openScreen(ServerPlayerEntity player, int screen, PacketByteBuf payload) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(screen);
		buf.writeBytes(payload);
		ServerPlayNetworking.send(player, Packets.OPEN_SCREEN, buf);
	}
}
