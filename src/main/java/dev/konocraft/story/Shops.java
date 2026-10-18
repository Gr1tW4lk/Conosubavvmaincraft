package dev.konocraft.story;

import dev.konocraft.net.Packets;
import dev.konocraft.net.ServerNet;
import dev.konocraft.registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Shops: the guild tavern, Wiz's magic shop (useless items included) and the blacksmith. */
public final class Shops {
	public record Entry(Supplier<Item> item, int count, long price, String note) {
	}

	public record Shop(String title, String greeting, List<Entry> entries) {
	}

	private static final Map<String, Shop> SHOPS = Map.of(
		"tavern", new Shop("Таверна гильдии", "Что будете заказывать? Жабьи лапки сегодня особенно хрустящие!", List.of(
			new Entry(() -> ModItems.FRIED_TOAD_LEGS, 1, 800, "Жареные лапки гигантской жабы. Вкуснее, чем звучит."),
			new Entry(() -> ModItems.CABBAGE_SALAD, 1, 1200, "Салат из пойманной капусты. Даёт немного опыта."),
			new Entry(() -> ModItems.CRIMSON_SODA, 1, 1500, "Шипучка «Алый демон». Скорость и прыгучесть."),
			new Entry(() -> Items.BREAD, 3, 300, "Хлеб. Просто хлеб."),
			new Entry(() -> ModItems.AXIS_FORM, 1, 1, "Анкета вступления в культ Аксис. Почему она в меню?")
		)),
		"wiz", new Shop("Лавка магических предметов Виз", "Д-добро пожаловать! Всё очень полезное... наверное.", List.of(
			new Entry(() -> ModItems.MANA_POTION, 1, 5000, "Восстанавливает ману. Одно из немногих работающих зелий!"),
			new Entry(() -> ModItems.HOLY_WATER, 1, 3000, "Святая вода. Нежить её ненавидит."),
			new Entry(() -> ModItems.TELEPORT_SCROLL, 1, 10000, "Свиток телепорта в Аксель."),
			new Entry(() -> ModItems.MANATITE, 1, 60000, "Манатит. Мгновенно восстанавливает всю ману."),
			new Entry(() -> ModItems.EXPLOSIVE_POTION, 1, 20000, "Зелье, которое взрывается, если его открыть. Зачем? Не спрашивайте."),
			new Entry(() -> ModItems.TOAD_RING, 1, 45000, "Кольцо, привлекающее жаб. Виз уверена, что это хит продаж."),
			new Entry(() -> ModItems.STAFF_REPLICA, 1, 250000, "Реплика посоха Мегумин. ПКМ — Взрыв! (вся мана). Мегумин одобряет.")
		)),
		"smith", new Shop("Оружейная лавка", "Мечи, щиты, броня. Всё для начинающих героев.", List.of(
			new Entry(() -> Items.IRON_SWORD, 1, 8000, "Надёжный железный меч."),
			new Entry(() -> Items.SHIELD, 1, 6000, "Щит. Даркнесс бы им не пользовалась."),
			new Entry(() -> Items.BOW, 1, 7000, "Лук для снайперов."),
			new Entry(() -> Items.ARROW, 16, 1500, "Стрелы."),
			new Entry(() -> Items.IRON_CHESTPLATE, 1, 20000, "Железный нагрудник."),
			new Entry(() -> Items.IRON_HELMET, 1, 10000, "Железный шлем."),
			new Entry(() -> Items.IRON_LEGGINGS, 1, 15000, "Железные поножи."),
			new Entry(() -> Items.IRON_BOOTS, 1, 8000, "Железные сапоги.")
		)),
		"market", new Shop("Лавка Хенрика", "Свежее! Всё свежее! Ну, кроме капусты. Капуста сама убежала.", List.of(
			new Entry(() -> Items.BREAD, 4, 400, "Свежий хлеб из пекарни у фонтана."),
			new Entry(() -> Items.APPLE, 4, 300, "Яблоки с ферм за западными воротами."),
			new Entry(() -> Items.COOKED_BEEF, 3, 900, "Жаркое. Не жаба. Честно."),
			new Entry(() -> Items.BAKED_POTATO, 4, 350, "Печёная картошка. Еда настоящих авантюристов."),
			new Entry(() -> Items.PUMPKIN_PIE, 2, 700, "Тыквенный пирог. Мегумин съела три."),
			new Entry(() -> Items.CAKE, 1, 2500, "Торт. На случай, если у вас праздник. Или плохой день."),
			new Entry(() -> Items.TORCH, 16, 500, "Факелы для подземелий."),
			new Entry(() -> Items.POPPY, 1, 100, "Цветок. Подарите кому-нибудь. Только не Аксис-культистам.")
		))
	);

	private Shops() {
	}

	public static void open(ServerPlayerEntity player, String id) {
		Shop shop = SHOPS.get(id);
		if (shop == null) return;
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(id);
		buf.writeString(shop.title());
		buf.writeString(shop.greeting());
		buf.writeVarInt(shop.entries().size());
		for (Entry e : shop.entries()) {
			buf.writeItemStack(new ItemStack(e.item().get(), e.count()));
			buf.writeVarLong(e.price());
			buf.writeString(e.note());
		}
		ServerNet.openScreen(player, Packets.SCREEN_SHOP, buf);
	}

	public static void buy(ServerPlayerEntity player, String id, int index) {
		Shop shop = SHOPS.get(id);
		if (shop == null || index < 0 || index >= shop.entries().size()) return;
		Entry e = shop.entries().get(index);
		PlayerData d = Progression.data(player);
		if (d.money < e.price()) {
			player.sendMessage(Text.literal("Не хватает денег! Нужно " + Progression.formatMoney(e.price())).formatted(Formatting.RED), true);
			return;
		}
		Progression.addMoney(player, -e.price());
		ItemStack stack = new ItemStack(e.item().get(), e.count());
		if (!player.getInventory().insertStack(stack)) player.dropItem(stack, false);
		if (id.equals("wiz")) {
			StoryState s = StoryState.get(player.getServer());
			s.setFlag("wiz_customer", true);
		}
	}
}
