package dev.konocraft.story;

import dev.konocraft.registry.ModItems;
import dev.konocraft.world.KonoWorld;
import dev.konocraft.world.structure.AxelCasino;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Casino "Eris's Luck": dice and roulette at the croupier's table (through dialogue choices), slot
 * machines you play by pressing their buttons, and the side quest about who is really cheating.
 * The house always has an edge, unless the casino has been made honest and you are very lucky.
 */
public final class Casino {
	public static final long SLOT_PRICE = 200;
	private static final String[] SYMBOLS = {"Жаба", "Капуста", "Взрыв", "Аква", "Эрис"};
	private static final int[] WEIGHTS = {30, 25, 20, 17, 8};
	private static final Formatting[] COLORS = {Formatting.GREEN, Formatting.DARK_GREEN, Formatting.RED, Formatting.AQUA, Formatting.GOLD};
	private static final int[] TRIPLE = {4, 6, 12, 0, 40};

	private static final Map<UUID, Long> stakes = new HashMap<>();
	private static final Map<UUID, String> results = new HashMap<>();
	private static final Map<UUID, Spin> spins = new HashMap<>();

	private record Spin(int[] reels, int[] frame) {
	}

	private Casino() {
	}

	public static void init() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!world.isClient && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity sp && KonoWorld.is(world)
				&& AxelCasino.isSlotButton(hit.getBlockPos())) {
				pullLever(sp);
			}
			return ActionResult.PASS;
		});
		ServerTickEvents.END_SERVER_TICK.register(Casino::tick);
	}

	public static String lastResult(ServerPlayerEntity p) {
		return results.getOrDefault(p.getUuid(), "");
	}

	private static boolean fair(MinecraftServer server) {
		return StoryState.get(server).hasFlag("casino_fair");
	}

	/** Luck decides a little: the stat of the class and level, plus Chris's coin in the pocket. */
	private static int luck(ServerPlayerEntity p) {
		int l = StoryState.get(p.getServer()).player(p.getUuid()).luck();
		if (p.getInventory().count(ModItems.LUCKY_COIN) > 0) l += 15;
		return l;
	}

	private static boolean pay(ServerPlayerEntity p, long amount) {
		PlayerData d = StoryState.get(p.getServer()).player(p.getUuid());
		if (d.money < amount) {
			results.put(p.getUuid(), "Не хватает денег на ставку.");
			return false;
		}
		Progression.addMoney(p, -amount);
		return true;
	}

	// ------------------------------------------------------------------------------------------
	// Table games, run from the croupier's dialogue

	public static void stake(ServerPlayerEntity p, long amount) {
		stakes.put(p.getUuid(), amount);
	}

	public static void dice(ServerPlayerEntity p, String bet) {
		long stake = stakes.getOrDefault(p.getUuid(), 500L);
		if (!pay(p, stake)) return;
		Random r = p.getRandom();
		boolean fair = fair(p.getServer());
		int a = 1 + r.nextInt(6), b = 1 + r.nextInt(6);
		boolean win = wins(bet, a + b);
		String note = "";
		if (win && !fair && r.nextFloat() < 0.2f) {
			// the enchanted dice of the house
			a = 1 + r.nextInt(6);
			b = 1 + r.nextInt(6);
			win = wins(bet, a + b);
			note = " Кости как-то странно подпрыгнули...";
		} else if (!win && fair && r.nextFloat() < luck(p) / 400f) {
			a = 1 + r.nextInt(6);
			b = 1 + r.nextInt(6);
			win = wins(bet, a + b);
			if (win) note = " Кость скатилась с края стола и... перевернулась. Вот это удача!";
		}
		long prize = !win ? 0 : bet.equals("seven") ? stake * 11 / 2 : stake * 11 / 5;
		String head = "Выпало " + a + " + " + b + " = " + (a + b) + ". ";
		finish(p, prize, stake, head + (win ? "Вы выиграли " + Progression.formatMoney(prize) + "!" : "Ставка проиграна.") + note);
	}

	private static boolean wins(String bet, int sum) {
		return switch (bet) {
			case "high" -> sum >= 8;
			case "low" -> sum <= 6;
			default -> sum == 7;
		};
	}

	public static void roulette(ServerPlayerEntity p, String bet) {
		long stake = stakes.getOrDefault(p.getUuid(), 500L);
		if (!pay(p, stake)) return;
		Random r = p.getRandom();
		int n = r.nextInt(37);
		boolean win = rouletteWins(bet, n);
		String note = "";
		if (win && !fair(p.getServer()) && r.nextFloat() < 0.15f) {
			n = r.nextInt(37);
			win = rouletteWins(bet, n);
			note = " Шарик подпрыгнул, будто его кто-то подтолкнул.";
		} else if (!win && fair(p.getServer()) && r.nextFloat() < luck(p) / 500f) {
			n = r.nextInt(37);
			win = rouletteWins(bet, n);
			if (win) note = " Шарик долго не мог выбрать... и выбрал вас.";
		}
		String color = n == 0 ? "зеро «Эрис»" : n % 2 == 1 ? "красное" : "чёрное";
		long prize = !win ? 0 : bet.equals("eris") ? stake * 33 : stake * 2;
		finish(p, prize, stake, "Шарик остановился: " + n + ", " + color + ". "
			+ (win ? "Выигрыш " + Progression.formatMoney(prize) + "!" : "Ставка проиграна.") + note);
		if (win && n == 0) firework(p);
	}

	private static boolean rouletteWins(String bet, int n) {
		return switch (bet) {
			case "red" -> n != 0 && n % 2 == 1;
			case "black" -> n != 0 && n % 2 == 0;
			default -> n == 0;
		};
	}

	private static void finish(ServerPlayerEntity p, long prize, long stake, String text) {
		results.put(p.getUuid(), text);
		if (prize > 0) {
			Progression.addMoney(p, prize);
			p.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.7f, 1.6f);
		} else {
			p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 0.8f, 0.6f);
		}
		StoryState s = StoryState.get(p.getServer());
		PlayerData d = s.player(p.getUuid());
		d.casinoBalance += prize - stake;
		s.markDirty();
	}

	// ------------------------------------------------------------------------------------------
	// Slot machines

	private static void pullLever(ServerPlayerEntity p) {
		if (spins.containsKey(p.getUuid())) return;
		if (!pay(p, SLOT_PRICE)) {
			p.sendMessage(Text.literal("Автомат: «Вставьте " + SLOT_PRICE + " эрис». У вас их нет.").formatted(Formatting.GRAY), true);
			return;
		}
		Random r = p.getRandom();
		int[] reels = {roll(r), roll(r), roll(r)};
		spins.put(p.getUuid(), new Spin(reels, new int[]{0}));
		p.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 30, 10));
		p.playSound(SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 1f, 0.8f);
	}

	private static int roll(Random r) {
		int x = r.nextInt(100);
		for (int i = 0; i < WEIGHTS.length; i++) {
			x -= WEIGHTS[i];
			if (x < 0) return i;
		}
		return 0;
	}

	private static Text reelText(int a, int b, int c) {
		return Text.literal("[").formatted(Formatting.GRAY)
			.append(Text.literal(SYMBOLS[a]).formatted(COLORS[a])).append(Text.literal("] [").formatted(Formatting.GRAY))
			.append(Text.literal(SYMBOLS[b]).formatted(COLORS[b])).append(Text.literal("] [").formatted(Formatting.GRAY))
			.append(Text.literal(SYMBOLS[c]).formatted(COLORS[c])).append(Text.literal("]").formatted(Formatting.GRAY));
	}

	private static void spinTick(MinecraftServer server) {
		if (spins.isEmpty()) return;
		for (var it = spins.entrySet().iterator(); it.hasNext(); ) {
			var e = it.next();
			ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
			Spin s = e.getValue();
			int f = ++s.frame()[0];
			if (p == null) {
				it.remove();
				continue;
			}
			Random r = p.getRandom();
			if (f % 3 == 0 && f < 36) {
				// reels stop one after another
				int a = f >= 18 ? s.reels()[0] : r.nextInt(5);
				int b = f >= 27 ? s.reels()[1] : r.nextInt(5);
				int c = r.nextInt(5);
				p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(" ")));
				p.networkHandler.sendPacket(new SubtitleS2CPacket(reelText(a, b, c)));
				p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.BLOCKS, 0.5f, 1.2f + f / 40f);
			}
			if (f == 36) {
				it.remove();
				int[] v = s.reels();
				long prize = 0;
				String msg;
				if (v[0] == v[1] && v[1] == v[2]) {
					prize = SLOT_PRICE * TRIPLE[v[0]];
					msg = switch (v[0]) {
						case 4 -> "ДЖЕКПОТ! Три Эрис! Богиня удачи улыбается вам!";
						case 3 -> "Три Аквы... Аква радостно потратила ваш выигрыш на выпивку.";
						case 2 -> "Три Взрыва! ВЗРЫВ! Мегумин гордилась бы.";
						default -> "Три в ряд!";
					};
				} else if (v[0] == v[1] || v[1] == v[2] || v[0] == v[2]) {
					int pair = v[0] == v[1] || v[0] == v[2] ? v[0] : v[1];
					if (pair == 3) msg = "Пара Акв. Ничего не выиграли, зато настроение плохое.";
					else {
						prize = SLOT_PRICE * 3 / 2;
						msg = "Пара! Маленький выигрыш.";
					}
				} else {
					msg = "Ничего. Автомат довольно урчит.";
				}
				p.networkHandler.sendPacket(new TitleS2CPacket(prize >= SLOT_PRICE * 10 ? Text.literal("ДЖЕКПОТ!").formatted(Formatting.GOLD, Formatting.BOLD) : Text.literal(" ")));
				p.networkHandler.sendPacket(new SubtitleS2CPacket(reelText(v[0], v[1], v[2])));
				p.sendMessage(Text.literal(msg + (prize > 0 ? " (+" + Progression.formatMoney(prize) + ")" : "")).formatted(prize > 0 ? Formatting.GOLD : Formatting.GRAY), false);
				finish(p, prize, SLOT_PRICE, msg);
				if (prize >= SLOT_PRICE * 10) firework(p);
			}
		}
	}

	private static void firework(ServerPlayerEntity p) {
		ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
		NbtCompound fw = new NbtCompound();
		NbtList explosions = new NbtList();
		NbtCompound ex = new NbtCompound();
		ex.putByte("Type", (byte) 1);
		ex.putIntArray("Colors", new int[]{0xFFD700, 0xFF4040});
		explosions.add(ex);
		fw.put("Explosions", explosions);
		fw.putByte("Flight", (byte) 0);
		rocket.getOrCreateNbt().put("Fireworks", fw);
		Vec3d pos = p.getPos().add(0, 2.5, 0);
		p.getWorld().spawnEntity(new FireworkRocketEntity(p.getWorld(), pos.x, pos.y, pos.z, rocket));
	}

	// ------------------------------------------------------------------------------------------
	// Side quest: who is cheating at "Eris's Luck"?

	private static int tick;

	private static void tick(MinecraftServer server) {
		spinTick(server);
		if (++tick % 10 != 0) return;
		StoryState s = StoryState.get(server);
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			PlayerData d = s.player(p.getUuid());
			if (!d.flags.contains("casino_help") || d.flags.contains("casino_evidence") || DialogueManager.inDialogue(p)) continue;
			if (!KonoWorld.is(p.getWorld())) continue;
			BlockPos e = AxelCasino.EVIDENCE;
			if (p.squaredDistanceTo(e.getX() + 0.5, e.getY(), e.getZ() + 0.5) < 3.5 * 3.5) {
				DialogueManager.start(p, "casino_office");
			}
		}
	}

	/** Objective line and marker for the casino errand, or null. */
	public static Story.Objective objective(ServerPlayerEntity p) {
		PlayerData d = StoryState.get(p.getServer()).player(p.getUuid());
		if (d.flags.contains("casino_evidence")) return new Story.Objective("Казино: отнесите заколдованные кости Сене в полицию", Locations.SENA);
		if (d.flags.contains("casino_help"))
			return new Story.Objective("Казино: обыщите кабинет управляющего на втором этаже, пока Крис его отвлекает", Locations.CASINO_OFFICE);
		if (d.flags.contains("casino_quest")) return new Story.Objective("Казино: присмотритесь к везучей посетительнице за карточным столом", Locations.CHRIS);
		return null;
	}
}
