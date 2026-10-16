package dev.konocraft.story;

import dev.konocraft.net.Packets;
import dev.konocraft.net.ServerNet;
import dev.konocraft.registry.ModEntities;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.EntityType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** The guild quest board: daily bounties on the local wildlife. */
public final class Bounties {
	public record Bounty(String id, String title, String description, Supplier<EntityType<?>> target, int count, long money, int exp, int minStage) {
	}

	public static final List<Bounty> ALL = List.of(
		new Bounty("toads5", "Жабья угроза", "Гигантские жабы снова едят фермерских коз. Убейте 5 жаб на восточных равнинах.",
			() -> ModEntities.GIANT_TOAD, 5, 25000, 120, Story.CH1_REPORT),
		new Bounty("toads12", "Большая жабья охота", "Сезон размножения! Убейте 12 гигантских жаб. Совет гильдии: не стойте с открытым ртом.",
			() -> ModEntities.GIANT_TOAD, 12, 70000, 300, Story.CH2_REPORT),
		new Bounty("goblins", "Гоблины в лесу", "Гоблины грабят торговцев на северо-западной дороге. Уничтожьте 6 гоблинов.",
			() -> ModEntities.GOBLIN, 6, 40000, 160, Story.CH1_REPORT),
		new Bounty("knights", "Беспокойные мертвецы", "Рыцари-нежить бродят у старого замка. Упокойте 6 из них.",
			() -> ModEntities.UNDEAD_KNIGHT, 6, 60000, 260, Story.CH4_GATE),
		new Bounty("toadking", "Король жаб", "Огромная жаба размером с дом поселилась у прудов. Награда за голову — щедрая. Шансы — не очень.",
			() -> ModEntities.TOAD_KING, 1, 300000, 800, Story.CH3_REPORT),
		new Bounty("golems", "Остатки Разрушителя", "После битвы на южной равнине бродят уцелевшие големы. Уничтожьте 5.",
			() -> ModEntities.DESTROYER_GOLEM, 5, 120000, 500, Story.ENDING)
	);

	private Bounties() {
	}

	public static Bounty byId(String id) {
		for (Bounty b : ALL) if (b.id().equals(id)) return b;
		return null;
	}

	public static String describe(PlayerData d) {
		Bounty b = byId(d.bounty);
		if (b == null) return "";
		return "Задание: " + b.title() + " " + d.bountyProgress + "/" + b.count();
	}

	private static List<Bounty> offers(ServerPlayerEntity player, PlayerData d) {
		StoryState s = StoryState.get(player.getServer());
		long day = player.getServerWorld().getTimeOfDay() / 24000L;
		if (d.boardDay != day || d.boardOffers.isEmpty()) {
			d.boardDay = day;
			d.boardOffers.clear();
			List<Bounty> pool = new ArrayList<>();
			for (Bounty b : ALL) if (s.stage >= b.minStage()) pool.add(b);
			java.util.Collections.shuffle(pool, new java.util.Random(day * 31 + player.getUuid().hashCode()));
			for (int i = 0; i < Math.min(3, pool.size()); i++) d.boardOffers.add(pool.get(i).id());
			s.markDirty();
		}
		List<Bounty> list = new ArrayList<>();
		for (String id : d.boardOffers) {
			Bounty b = byId(id);
			if (b != null) list.add(b);
		}
		return list;
	}

	public static void openBoard(ServerPlayerEntity player) {
		PlayerData d = Progression.data(player);
		if (!d.registered) {
			player.sendMessage(Text.literal("Сначала зарегистрируйтесь в гильдии у Луны.").formatted(Formatting.RED), true);
			return;
		}
		List<Bounty> list = offers(player, d);
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(d.bounty);
		buf.writeVarInt(d.bountyProgress);
		buf.writeVarInt(list.size());
		for (Bounty b : list) {
			buf.writeString(b.id());
			buf.writeString(b.title());
			buf.writeString(b.description());
			buf.writeVarInt(b.count());
			buf.writeVarLong(b.money());
			buf.writeVarInt(b.exp());
		}
		ServerNet.openScreen(player, Packets.SCREEN_BOARD, buf);
	}

	public static void accept(ServerPlayerEntity player, int index) {
		PlayerData d = Progression.data(player);
		List<Bounty> list = offers(player, d);
		if (index == -1) {
			d.bounty = "";
			d.bountyProgress = 0;
			player.sendMessage(Text.literal("Вы отказались от задания. Луна вздыхает.").formatted(Formatting.GRAY), false);
		} else {
			if (index < 0 || index >= list.size()) return;
			Bounty b = list.get(index);
			d.bounty = b.id();
			d.bountyProgress = 0;
			player.sendMessage(Text.literal("Принято задание: " + b.title()).formatted(Formatting.GOLD), false);
		}
		StoryState.get(player.getServer()).markDirty();
		Progression.sync(player);
		Story.syncObjective(player);
	}

	public static void onKill(ServerPlayerEntity player, EntityType<?> type) {
		PlayerData d = Progression.data(player);
		Bounty b = byId(d.bounty);
		if (b == null || b.target().get() != type) return;
		d.bountyProgress++;
		if (d.bountyProgress >= b.count()) {
			d.bounty = "";
			d.bountyProgress = 0;
			d.boardOffers.remove(b.id());
			player.sendMessage(Text.literal("★ Задание «" + b.title() + "» выполнено! Награда зачислена на карту авантюриста.").formatted(Formatting.GOLD), false);
			Progression.addMoney(player, b.money());
			Progression.addExp(player, b.exp());
		}
		StoryState.get(player.getServer()).markDirty();
		Story.syncObjective(player);
	}
}
