package dev.konocraft.item;

import dev.konocraft.entity.BeldiaEntity;
import dev.konocraft.fx.Fx;
import dev.konocraft.net.Packets;
import dev.konocraft.net.ServerNet;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.story.DialogueManager;
import dev.konocraft.story.Locations;
import dev.konocraft.story.PlayerData;
import dev.konocraft.story.Progression;
import dev.konocraft.story.Story;
import dev.konocraft.story.StoryState;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/** All custom item behaviours. */
public final class KonoItems {
	private KonoItems() {
	}

	private static void tooltip(List<Text> tooltip, String key) {
		tooltip.add(Text.translatable(key).formatted(Formatting.GRAY, Formatting.ITALIC));
	}

	// ---------------------------------------------------------------------------------------------
	public enum Material implements ToolMaterial {
		KATANA(900, 7f, 3f, 2, 14, Items.IRON_INGOT),
		HOLY_STEEL(1600, 8f, 4f, 3, 12, Items.DIAMOND),
		STAFF(500, 2f, 1f, 1, 22, Items.AMETHYST_SHARD);

		private final int durability, miningLevel, enchantability;
		private final float speed, damage;
		private final Item repair;

		Material(int durability, float speed, float damage, int miningLevel, int enchantability, Item repair) {
			this.durability = durability;
			this.speed = speed;
			this.damage = damage;
			this.miningLevel = miningLevel;
			this.enchantability = enchantability;
			this.repair = repair;
		}

		@Override
		public int getDurability() {
			return durability;
		}

		@Override
		public float getMiningSpeedMultiplier() {
			return speed;
		}

		@Override
		public float getAttackDamage() {
			return damage;
		}

		@Override
		public int getMiningLevel() {
			return miningLevel;
		}

		@Override
		public int getEnchantability() {
			return enchantability;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.ofItems(repair);
		}
	}

	/** Magic staves: holding one boosts mana regeneration. */
	public static class Staff extends SwordItem {
		public final float manaBoost;

		public Staff(ToolMaterial material, int damage, float speed, Settings settings, float manaBoost) {
			super(material, damage, speed, settings);
			this.manaBoost = manaBoost;
		}

		@Override
		public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
			tooltip.add(Text.literal("Восстановление маны ×" + manaBoost).formatted(Formatting.BLUE));
			tooltip(tooltip, getTranslationKey() + ".desc");
		}
	}

	public static class AdventurerCard extends Item {
		public AdventurerCard(Settings settings) {
			super(settings);
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			if (!world.isClient && user instanceof ServerPlayerEntity sp) {
				Progression.sync(sp);
				ServerNet.openScreen(sp, Packets.SCREEN_CARD, PacketByteBufs.create());
			}
			return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
		}

		@Override
		public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
			tooltip(tooltip, "item.konocraft.adventurer_card.desc");
		}
	}

	public static class GuideBook extends Item {
		public GuideBook(Settings settings) {
			super(settings);
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			if (!world.isClient && user instanceof ServerPlayerEntity sp) DialogueManager.start(sp, "guide");
			return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
		}

		@Override
		public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
			tooltip(tooltip, "item.konocraft.guide_book.desc");
		}
	}

	public static class Diary extends Item {
		public Diary(Settings settings) {
			super(settings);
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			if (!world.isClient && user instanceof ServerPlayerEntity sp) DialogueManager.start(sp, "destroyer_diary");
			return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
		}
	}

	/** Food that also grants adventurer experience (cabbages are famously nutritious here). */
	public static class ExpFood extends Item {
		private final int exp;

		public ExpFood(Settings settings, int exp) {
			super(settings);
			this.exp = exp;
		}

		@Override
		public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
			if (!world.isClient && user instanceof ServerPlayerEntity sp) {
				Progression.addExp(sp, exp);
				sp.sendMessage(Text.literal("+" + exp + " опыта авантюриста. Капуста — источник силы!").formatted(Formatting.GREEN), true);
			}
			return super.finishUsing(stack, world, user);
		}

		@Override
		public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
			tooltip.add(Text.literal("+" + exp + " опыта авантюриста").formatted(Formatting.GREEN));
		}
	}

	public static class Soda extends Item {
		public Soda(Settings settings) {
			super(settings);
		}

		@Override
		public UseAction getUseAction(ItemStack stack) {
			return UseAction.DRINK;
		}

		@Override
		public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
			if (!world.isClient) {
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 60, 1));
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 20 * 60, 0));
			}
			return super.finishUsing(stack, world, user);
		}

		@Override
		public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
			tooltip(tooltip, "item.konocraft.crimson_soda.desc");
		}
	}

	/** Drinkable that restores mana. */
	public static class ManaPotion extends Item {
		private final int amount;

		public ManaPotion(Settings settings, int amount) {
			super(settings);
			this.amount = amount;
		}

		@Override
		public UseAction getUseAction(ItemStack stack) {
			return UseAction.DRINK;
		}

		@Override
		public int getMaxUseTime(ItemStack stack) {
			return 24;
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			user.setCurrentHand(hand);
			return TypedActionResult.consume(user.getStackInHand(hand));
		}

		@Override
		public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
			if (!world.isClient && user instanceof ServerPlayerEntity sp) {
				PlayerData d = Progression.data(sp);
				d.mana = Math.min(d.maxMana(), d.mana + amount);
				Progression.sync(sp);
				sp.sendMessage(Text.literal("+" + amount + " маны").formatted(Formatting.BLUE), true);
				if (!sp.isCreative()) stack.decrement(1);
			}
			return stack;
		}

		@Override
		public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
			tooltip.add(Text.literal("+" + amount + " маны").formatted(Formatting.BLUE));
		}
	}

	public static class Manatite extends Item {
		public Manatite(Settings settings) {
			super(settings);
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (!world.isClient && user instanceof ServerPlayerEntity sp) {
				PlayerData d = Progression.data(sp);
				d.mana = d.maxMana();
				Progression.sync(sp);
				Fx.sparkle((ServerWorld) world, sp.getPos());
				sp.sendMessage(Text.literal("Манатит рассыпается в пыль. Мана полностью восстановлена!").formatted(Formatting.AQUA), true);
				if (!sp.isCreative()) stack.decrement(1);
			}
			return TypedActionResult.success(stack, world.isClient);
		}

		@Override
		public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
			tooltip(tooltip, "item.konocraft.manatite.desc");
		}
	}

	/** Splash of holy water in front of you: undead hate it, Beldia gets soaked. */
	public static class HolyWater extends Item {
		public HolyWater(Settings settings) {
			super(settings);
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (!world.isClient && user instanceof ServerPlayerEntity sp) {
				ServerWorld w = (ServerWorld) world;
				Vec3d at = sp.getPos().add(sp.getRotationVec(1f).multiply(3));
				Fx.water(w, at);
				Fx.holy(w, at);
				for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, sp.getBoundingBox().expand(6), LivingEntity::isUndead)) {
					if (e instanceof BeldiaEntity b) b.soak(sp);
					else e.damage(w.getDamageSources().indirectMagic(sp, sp), 14);
				}
				if (!sp.isCreative()) stack.decrement(1);
				sp.getItemCooldownManager().set(this, 20);
			}
			return TypedActionResult.success(stack, world.isClient);
		}

		@Override
		public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
			tooltip(tooltip, "item.konocraft.holy_water.desc");
		}
	}

	public static class TeleportScroll extends Item {
		public TeleportScroll(Settings settings) {
			super(settings);
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (!world.isClient && user instanceof ServerPlayerEntity sp) {
				if (DialogueManager.inDialogue(sp)) return TypedActionResult.fail(stack);
				Fx.teleport((ServerWorld) world, sp.getPos());
				Story.teleport(sp, Locations.AXEL, true);
				Fx.teleport(sp.getServerWorld(), sp.getPos());
				if (!sp.isCreative()) stack.decrement(1);
			}
			return TypedActionResult.success(stack, world.isClient);
		}

		@Override
		public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
			tooltip(tooltip, "item.konocraft.teleport_scroll.desc");
		}
	}

	/** Wiz's best seller: a potion that explodes when opened. Why would anyone buy this? */
	public static class ExplosivePotion extends Item {
		public ExplosivePotion(Settings settings) {
			super(settings);
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (!world.isClient && user instanceof ServerPlayerEntity sp) {
				ServerWorld w = (ServerWorld) world;
				Fx.smallExplosion(w, sp.getPos());
				sp.damage(w.getDamageSources().magic(), 3);
				sp.addVelocity(0, 0.8, 0);
				sp.velocityModified = true;
				String[] lines = {"Зелье взорвалось. Как и было написано на этикетке.", "БУМ! Ну, вы хотя бы проверили, что товар работает.",
					"Виз была права: открывать его было плохой идеей."};
				sp.sendMessage(Text.literal(lines[sp.getRandom().nextInt(lines.length)]).formatted(Formatting.RED), false);
				// it does damage everything around, too
				for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, sp.getBoundingBox().expand(4), e -> e != sp && !(e instanceof PlayerEntity))) {
					e.damage(w.getDamageSources().indirectMagic(sp, sp), 12);
				}
				if (!sp.isCreative()) stack.decrement(1);
			}
			return TypedActionResult.success(stack, world.isClient);
		}

		@Override
		public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
			tooltip(tooltip, "item.konocraft.explosive_potion.desc");
		}
	}

	/** Attracts giant toads. Wiz insists it is a best seller. */
	public static class ToadRing extends Item {
		public ToadRing(Settings settings) {
			super(settings);
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (!world.isClient && user instanceof ServerPlayerEntity sp) {
				ServerWorld w = (ServerWorld) world;
				for (int i = 0; i < 2; i++) {
					var toad = ModEntities.GIANT_TOAD.create(w);
					if (toad == null) continue;
					double a = sp.getRandom().nextDouble() * Math.PI * 2;
					toad.refreshPositionAndAngles(sp.getX() + Math.cos(a) * 8, sp.getY() + 1, sp.getZ() + Math.sin(a) * 8, 0, 0);
					toad.initialize(w, w.getLocalDifficulty(toad.getBlockPos()), SpawnReason.EVENT, null, null);
					w.spawnEntity(toad);
				}
				w.playSound(null, sp.getBlockPos(), dev.konocraft.registry.ModSounds.TOAD_CROAK, SoundCategory.HOSTILE, 2f, 0.8f);
				sp.sendMessage(Text.literal("Где-то рядом радостно заквакали. Кольцо работает. К сожалению.").formatted(Formatting.GREEN), false);
				sp.getItemCooldownManager().set(this, 20 * 30);
			}
			return TypedActionResult.success(stack, world.isClient);
		}

		@Override
		public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
			tooltip(tooltip, "item.konocraft.toad_ring.desc");
		}
	}

	public static class AxisForm extends Item {
		public AxisForm(Settings settings) {
			super(settings);
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			ItemStack stack = user.getStackInHand(hand);
			if (!world.isClient && user instanceof ServerPlayerEntity sp) {
				String[] lines = {"Вы подписали анкету культа Аксис. Где-то вдалеке Аква радостно плачет.",
					"Пункт 1: Культ Аксис прав всегда. Пункт 2: Если культ Аксис не прав, см. пункт 1.",
					"«Делайте, что хотите, пока это никому не вредит!» — учение Аксис. Вы чувствуете себя свободнее.",
					"Вы попытались выбросить анкету, но она прилипла. Это мыло культа Аксис."};
				sp.sendMessage(Text.literal(lines[sp.getRandom().nextInt(lines.length)]).formatted(Formatting.AQUA), false);
				sp.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 20 * 120, 0));
				world.playSound(null, sp.getBlockPos(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1f);
				StoryState.get(sp.getServer()).addAffection("aqua", 1);
				if (!sp.isCreative()) stack.decrement(1);
			}
			return TypedActionResult.success(stack, world.isClient);
		}
	}

	public static class Trophy extends Item {
		private final String kind;

		public Trophy(Settings settings, String kind) {
			super(settings);
			this.kind = kind;
		}

		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			if (!world.isClient) {
				String msg = kind.equals("beldia")
					? "Голова Бельдии смотрит на вас с немым укором. «Мог бы хоть вернуть...»"
					: "Коронатит всё ещё тёплый. Лучше не держать его возле лица.";
				user.sendMessage(Text.literal(msg).formatted(Formatting.DARK_PURPLE), true);
			}
			return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
		}

		@Override
		public boolean hasGlint(ItemStack stack) {
			return true;
		}

		@Override
		public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
			tooltip(tooltip, getTranslationKey() + ".desc");
		}
	}
}
