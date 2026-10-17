package dev.konocraft.registry;

import dev.konocraft.KonoCraft;
import dev.konocraft.item.KonoItems;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
	private static final List<Item> TAB = new ArrayList<>();

	public static final Item ADVENTURER_CARD = register("adventurer_card", new KonoItems.AdventurerCard(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));
	public static final Item GUIDE_BOOK = register("guide_book", new KonoItems.GuideBook(new Item.Settings().maxCount(1)));

	// Weapons: Eris' "cheats"
	public static final Item CHUNCHUNMARU = register("chunchunmaru", new SwordItem(KonoItems.Material.KATANA, 4, -2.2f, new Item.Settings().rarity(Rarity.RARE)));
	public static final Item MANATITE_STAFF = register("manatite_staff", new KonoItems.Staff(KonoItems.Material.STAFF, 2, -2.6f, new Item.Settings().rarity(Rarity.RARE), 2.0f));
	public static final Item CRUSADER_SWORD = register("crusader_sword", new SwordItem(KonoItems.Material.HOLY_STEEL, 5, -2.8f, new Item.Settings().rarity(Rarity.RARE)));
	public static final Item GODDESS_SCEPTER = register("goddess_scepter", new KonoItems.Staff(KonoItems.Material.STAFF, 3, -2.4f, new Item.Settings().rarity(Rarity.RARE), 1.6f));
	public static final Item MEGUMIN_STAFF = register("megumin_staff", new KonoItems.Staff(KonoItems.Material.STAFF, 2, -2.6f, new Item.Settings().rarity(Rarity.EPIC), 2.5f));
	public static final Item STAFF_REPLICA = register("staff_replica", new KonoItems.Staff(KonoItems.Material.STAFF, 1, -2.6f, new Item.Settings().rarity(Rarity.UNCOMMON), 1.5f));

	// Food
	public static final Item TOAD_MEAT = register("toad_meat", new Item(new Item.Settings().food(new FoodComponent.Builder().hunger(3).saturationModifier(0.3f).meat().build())));
	public static final Item FRIED_TOAD_LEGS = register("fried_toad_legs", new Item(new Item.Settings().food(new FoodComponent.Builder().hunger(8).saturationModifier(0.8f).meat().build())));
	public static final Item CABBAGE = register("cabbage", new KonoItems.ExpFood(new Item.Settings().food(new FoodComponent.Builder().hunger(3).saturationModifier(0.4f).build()), 40));
	public static final Item CABBAGE_SALAD = register("cabbage_salad", new KonoItems.ExpFood(new Item.Settings().maxCount(16).food(new FoodComponent.Builder().hunger(7).saturationModifier(0.7f).build()), 70));
	public static final Item CRIMSON_SODA = register("crimson_soda", new KonoItems.Soda(new Item.Settings().maxCount(16).food(new FoodComponent.Builder().hunger(1).saturationModifier(0.1f).alwaysEdible().build())));

	// Magic items (Wiz's shop)
	public static final Item MANA_POTION = register("mana_potion", new KonoItems.ManaPotion(new Item.Settings().maxCount(16), 70));
	public static final Item MANATITE = register("manatite", new KonoItems.Manatite(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)));
	public static final Item HOLY_WATER = register("holy_water", new KonoItems.HolyWater(new Item.Settings().maxCount(16)));
	public static final Item TELEPORT_SCROLL = register("teleport_scroll", new KonoItems.TeleportScroll(new Item.Settings().maxCount(16)));
	public static final Item EXPLOSIVE_POTION = register("explosive_potion", new KonoItems.ExplosivePotion(new Item.Settings().maxCount(16)));
	public static final Item TOAD_RING = register("toad_ring", new KonoItems.ToadRing(new Item.Settings().maxCount(1)));
	public static final Item AXIS_FORM = register("axis_form", new KonoItems.AxisForm(new Item.Settings().maxCount(64)));
	public static final Item LUCKY_COIN = register("lucky_coin", new KonoItems.LuckyCoin(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));

	// Trophies
	public static final Item BELDIA_HEAD = register("beldia_head", new KonoItems.Trophy(new Item.Settings().maxCount(1).rarity(Rarity.EPIC), "beldia"));
	public static final Item CORONATITE = register("coronatite", new KonoItems.Trophy(new Item.Settings().maxCount(1).rarity(Rarity.EPIC), "coronatite"));
	public static final Item DESTROYER_DIARY = register("destroyer_diary", new KonoItems.Diary(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));

	// Projectile visuals (not in the tab)
	public static final Item MAGIC_BOLT = Registry.register(Registries.ITEM, KonoCraft.id("magic_bolt"), new Item(new Item.Settings()));
	public static final Item DARK_BOLT = Registry.register(Registries.ITEM, KonoCraft.id("dark_bolt"), new Item(new Item.Settings()));

	public static final Item QUEST_BOARD = register("quest_board", new BlockItem(ModBlocks.QUEST_BOARD, new Item.Settings()));

	// Spawn eggs
	public static final Item GIANT_TOAD_SPAWN_EGG = egg("giant_toad", ModEntities.GIANT_TOAD, 0x6C9A3C, 0xE8D27A);
	public static final Item TOAD_KING_SPAWN_EGG = egg("toad_king", ModEntities.TOAD_KING, 0x4E7A2A, 0xFFD700);
	public static final Item FLYING_CABBAGE_SPAWN_EGG = egg("flying_cabbage", ModEntities.FLYING_CABBAGE, 0x9BD46A, 0xE6F7C8);
	public static final Item GOBLIN_SPAWN_EGG = egg("goblin", ModEntities.GOBLIN, 0x5E8C31, 0x6B4423);
	public static final Item UNDEAD_KNIGHT_SPAWN_EGG = egg("undead_knight", ModEntities.UNDEAD_KNIGHT, 0x3A3A44, 0x7A2BA0);
	public static final Item DESTROYER_GOLEM_SPAWN_EGG = egg("destroyer_golem", ModEntities.DESTROYER_GOLEM, 0x8C6B3E, 0x3BE0FF);
	public static final Item BELDIA_SPAWN_EGG = egg("beldia", ModEntities.BELDIA, 0x222230, 0xC02020);
	public static final Item TOWNSFOLK_SPAWN_EGG = egg("townsfolk", ModEntities.TOWNSFOLK, 0xC8A27A, 0x3B6EA5);

	public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, KonoCraft.id("main"), FabricItemGroup.builder()
		.icon(() -> new ItemStack(MEGUMIN_STAFF))
		.displayName(Text.translatable("itemGroup.konocraft.main"))
		.entries((context, entries) -> {
			for (Item item : TAB) entries.add(item);
		})
		.build());

	private ModItems() {
	}

	private static <T extends Item> T register(String name, T item) {
		T registered = Registry.register(Registries.ITEM, KonoCraft.id(name), item);
		TAB.add(registered);
		return registered;
	}

	private static Item egg(String name, EntityType<? extends MobEntity> type, int primary, int secondary) {
		return register(name + "_spawn_egg", new SpawnEggItem(type, primary, secondary, new Item.Settings()));
	}

	public static void init() {
	}
}
