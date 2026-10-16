package dev.konocraft.client;

import dev.konocraft.client.screen.BoardScreen;
import dev.konocraft.client.screen.CardScreen;
import dev.konocraft.client.screen.CreditsScreen;
import dev.konocraft.client.screen.DialogueScreen;
import dev.konocraft.client.screen.ShopScreen;
import dev.konocraft.net.Packets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class ClientNet {
	private ClientNet() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(Packets.DIALOGUE_LINE, (client, handler, buf, sender) -> {
			String key = buf.readString();
			String name = buf.readString();
			int color = buf.readInt();
			String text = buf.readString(32767);
			int auto = buf.readVarInt();
			int portrait = buf.readVarInt();
			int n = buf.readVarInt();
			List<String> choices = new ArrayList<>();
			for (int i = 0; i < n; i++) choices.add(buf.readString());
			client.execute(() -> DialogueScreen.show(client, key, name, color, text, choices, auto, portrait));
		});
		ClientPlayNetworking.registerGlobalReceiver(Packets.DIALOGUE_CLOSE, (client, handler, buf, sender) ->
			client.execute(() -> {
				if (client.currentScreen instanceof DialogueScreen) client.setScreen(null);
			}));
		ClientPlayNetworking.registerGlobalReceiver(Packets.PLAYER_SYNC, (client, handler, buf, sender) -> {
			String cls = buf.readString();
			int level = buf.readVarInt();
			int exp = buf.readVarInt();
			int expNext = buf.readVarInt();
			int sp = buf.readVarInt();
			float mana = buf.readFloat();
			int maxMana = buf.readVarInt();
			long money = buf.readVarLong();
			int luck = buf.readVarInt();
			int n = buf.readVarInt();
			List<String> skills = new ArrayList<>();
			for (int i = 0; i < n; i++) skills.add(buf.readString());
			int selected = buf.readVarInt();
			boolean registered = buf.readBoolean();
			int ln = buf.readVarInt();
			List<String> learn = new ArrayList<>();
			List<Integer> cost = new ArrayList<>();
			for (int i = 0; i < ln; i++) {
				learn.add(buf.readString());
				cost.add(buf.readVarInt());
			}
			String bounty = buf.readString();
			client.execute(() -> {
				ClientState.playerClass = cls;
				ClientState.level = level;
				ClientState.exp = exp;
				ClientState.expNext = expNext;
				ClientState.skillPoints = sp;
				ClientState.mana = mana;
				ClientState.maxMana = maxMana;
				ClientState.money = money;
				ClientState.luck = luck;
				ClientState.skills.clear();
				ClientState.skills.addAll(skills);
				ClientState.selectedSkill = selected;
				ClientState.registered = registered;
				ClientState.learnable.clear();
				ClientState.learnable.addAll(learn);
				ClientState.learnCost.clear();
				ClientState.learnCost.addAll(cost);
				ClientState.bounty = bounty;
				if (client.currentScreen instanceof CardScreen card) card.refresh();
				if (client.currentScreen instanceof ShopScreen shop) shop.refresh();
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(Packets.OBJECTIVE, (client, handler, buf, sender) -> {
			String text = buf.readString();
			Vec3d target = buf.readBoolean() ? new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
			client.execute(() -> {
				ClientState.objective = text;
				ClientState.objectiveTarget = target;
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(Packets.FX, (client, handler, buf, sender) -> {
			int kind = buf.readVarInt();
			int duration = buf.readVarInt();
			float strength = buf.readFloat();
			client.execute(() -> {
				switch (kind) {
					case Packets.FX_FLASH -> {
						ClientState.flashTicks = duration;
						ClientState.flashMax = Math.max(1, duration);
					}
					case Packets.FX_SHAKE -> {
						ClientState.shakeTicks = Math.max(ClientState.shakeTicks, duration);
						ClientState.shakeStrength = Math.max(strength, ClientState.shakeTicks > 0 ? ClientState.shakeStrength * 0.5f : 0);
					}
					case Packets.FX_LETTERBOX -> ClientState.letterboxTicks = duration;
					default -> {
					}
				}
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(Packets.MUSIC, (client, handler, buf, sender) -> {
			String music = buf.readString();
			client.execute(() -> {
				if (!music.equals(ClientState.music)) {
					ClientState.music = music;
					client.getMusicTracker().stop();
				}
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(Packets.TOAST, (client, handler, buf, sender) -> {
			String title = buf.readString();
			String text = buf.readString();
			client.execute(() -> SystemToast.add(client.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION, Text.literal(title), Text.literal(text)));
		});
		ClientPlayNetworking.registerGlobalReceiver(Packets.OPEN_SCREEN, (client, handler, buf, sender) -> {
			int screen = buf.readVarInt();
			switch (screen) {
				case Packets.SCREEN_SHOP -> {
					String id = buf.readString();
					String title = buf.readString();
					String greeting = buf.readString();
					int n = buf.readVarInt();
					List<ShopScreen.Entry> entries = new ArrayList<>();
					for (int i = 0; i < n; i++) {
						ItemStack stack = buf.readItemStack();
						long price = buf.readVarLong();
						String note = buf.readString();
						entries.add(new ShopScreen.Entry(stack, price, note));
					}
					client.execute(() -> client.setScreen(new ShopScreen(id, title, greeting, entries)));
				}
				case Packets.SCREEN_BOARD -> {
					String current = buf.readString();
					int progress = buf.readVarInt();
					int n = buf.readVarInt();
					List<BoardScreen.Bounty> list = new ArrayList<>();
					for (int i = 0; i < n; i++) {
						list.add(new BoardScreen.Bounty(buf.readString(), buf.readString(), buf.readString(), buf.readVarInt(), buf.readVarLong(), buf.readVarInt()));
					}
					client.execute(() -> client.setScreen(new BoardScreen(current, progress, list)));
				}
				case Packets.SCREEN_CARD -> client.execute(() -> client.setScreen(new CardScreen()));
				case Packets.SCREEN_CREDITS -> client.execute(() -> client.setScreen(new CreditsScreen()));
				default -> {
				}
			}
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			ClientState.reset();
			ClientPlayNetworking.send(Packets.REQUEST_SYNC, PacketByteBufs.create());
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.reset());
	}

	public static void advance(int choice) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(choice);
		ClientPlayNetworking.send(Packets.DIALOGUE_ADVANCE, buf);
	}

	public static void castSkill() {
		ClientPlayNetworking.send(Packets.CAST_SKILL, PacketByteBufs.create());
	}

	public static void selectSkill(int value, boolean absolute) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(value);
		buf.writeBoolean(absolute);
		ClientPlayNetworking.send(Packets.SELECT_SKILL, buf);
	}

	public static void learnSkill(String skill) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(skill);
		ClientPlayNetworking.send(Packets.LEARN_SKILL, buf);
	}

	public static void buy(String shop, int index) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(shop);
		buf.writeVarInt(index);
		ClientPlayNetworking.send(Packets.SHOP_BUY, buf);
	}

	public static void acceptBounty(int index) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(index);
		ClientPlayNetworking.send(Packets.BOARD_ACCEPT, buf);
	}

	public static MinecraftClient mc() {
		return MinecraftClient.getInstance();
	}
}
