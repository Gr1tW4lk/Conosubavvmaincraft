package dev.konocraft.story;

import dev.konocraft.KonoCraft;
import dev.konocraft.entity.CompanionEntity;
import dev.konocraft.entity.KonoNpcEntity;
import dev.konocraft.fx.Fx;
import dev.konocraft.net.Packets;
import dev.konocraft.net.ServerNet;
import dev.konocraft.skill.PlayerClass;
import dev.konocraft.skill.Skill;
import dev.konocraft.skill.Skills;
import dev.konocraft.world.KonoWorld;
import dev.konocraft.world.Regions;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;


/** Interpreter for the small action and condition language used in dialogue scripts. */
public final class Actions {
	private Actions() {
	}

	// ---------------------------------------------------------------------------------------------
	// Text placeholders

	public static String format(ServerPlayerEntity player, String text) {
		if (text == null) return "";
		StoryState state = StoryState.get(player.getServer());
		PlayerData d = state.player(player.getUuid());
		return text.replace("%player%", player.getGameProfile().getName())
			.replace("%class%", d.playerClass.title)
			.replace("%money%", Progression.formatMoney(d.money))
			.replace("%level%", String.valueOf(d.level))
			.replace("%luck%", String.valueOf(d.luck()))
			.replace("%count%", String.valueOf(state.questCounter))
			.replace("%death%", Story.deathReason(player))
			.replace("%casino%", Casino.lastResult(player))
			.replace("%casino_total%", Progression.formatMoney(d.casinoBalance));
	}

	// ---------------------------------------------------------------------------------------------
	// Conditions

	public static boolean check(ServerPlayerEntity player, String cond) {
		for (String part : cond.split("&")) {
			if (!checkOne(player, part.trim())) return false;
		}
		return true;
	}

	private static boolean checkOne(ServerPlayerEntity player, String c) {
		if (c.isEmpty()) return true;
		boolean negate = c.startsWith("!");
		if (negate) c = c.substring(1);
		boolean result = evaluate(player, c);
		return negate != result;
	}

	private static boolean evaluate(ServerPlayerEntity player, String c) {
		StoryState state = StoryState.get(player.getServer());
		PlayerData d = state.player(player.getUuid());
		long day = player.getServerWorld().getTimeOfDay() / 24000L;
		if (c.startsWith("flag:")) return state.hasFlag(c.substring(5));
		if (c.startsWith("pflag:")) return d.flags.contains(c.substring(6));
		if (c.startsWith("party:")) return state.party.contains(c.substring(6));
		if (c.startsWith("class:")) return d.playerClass.name().equalsIgnoreCase(c.substring(6));
		if (c.startsWith("skill:")) {
			Skill s = Skill.byName(c.substring(6));
			return s != null && d.skills.contains(s);
		}
		if (c.startsWith("daily:")) return d.dailyDone(c.substring(6), day);
		if (c.startsWith("item:")) {
			Item item = Registries.ITEM.get(itemId(c.substring(5)));
			return player.getInventory().count(item) > 0;
		}
		if (c.equals("night")) {
			long t = player.getServerWorld().getTimeOfDay() % 24000L;
			return t > 13000 && t < 23000;
		}
		if (c.startsWith("random<")) return player.getRandom().nextDouble() < Double.parseDouble(c.substring(7));
		if (c.startsWith("aff:")) {
			String[] p = c.substring(4).split(">=");
			return state.affection(p[0]) >= Integer.parseInt(p[1]);
		}
		if (c.equals("registered")) return d.registered;
		if (c.equals("intown")) return Regions.inTown((int) player.getX(), (int) player.getZ());
		if (c.startsWith("down:")) {
			Entity e = Npcs.find(player.getServer(), c.substring(5));
			return e instanceof CompanionEntity comp && comp.isDown();
		}
		if (c.startsWith("bounty")) return !d.bounty.isEmpty();
		// numeric comparisons: money>=N, stage>=N, level>=N, stage==N, stage<N
		String[] ops = {">=", "<=", "==", "<", ">"};
		for (String op : ops) {
			int i = c.indexOf(op);
			if (i <= 0) continue;
			String key = c.substring(0, i);
			long value = Long.parseLong(c.substring(i + op.length()));
			long actual = switch (key) {
				case "money" -> d.money;
				case "stage" -> state.stage;
				case "level" -> d.level;
				case "sp" -> d.skillPoints;
				case "count" -> state.questCounter;
				default -> 0;
			};
			return switch (op) {
				case ">=" -> actual >= value;
				case "<=" -> actual <= value;
				case "==" -> actual == value;
				case "<" -> actual < value;
				default -> actual > value;
			};
		}
		KonoCraft.LOGGER.warn("Unknown condition {}", c);
		return false;
	}

	public static Identifier itemId(String name) {
		return name.contains(":") ? new Identifier(name) : KonoCraft.id(name);
	}

	// ---------------------------------------------------------------------------------------------
	// Actions

	public static void run(ServerPlayerEntity player, String action, DialogueManager.Session session) {
		String[] a = action.trim().split(" ", 2);
		String cmd = a[0];
		String arg = a.length > 1 ? a[1].trim() : "";
		String[] args = arg.isEmpty() ? new String[0] : arg.split(" ");
		StoryState state = StoryState.get(player.getServer());
		PlayerData d = state.player(player.getUuid());
		ServerWorld world = player.getServerWorld();
		switch (cmd) {
			case "stage" -> Story.setStage(player.getServer(), Integer.parseInt(arg));
			case "flag" -> state.setFlag(arg, true);
			case "unflag" -> state.setFlag(arg, false);
			case "pflag" -> {
				d.flags.add(arg);
				state.markDirty();
			}
			case "unpflag" -> {
				d.flags.remove(arg);
				state.markDirty();
			}
			case "registered" -> {
				d.registered = true;
				state.markDirty();
				Progression.sync(player);
			}
			case "prologue_done" -> {
				d.prologueDone = true;
				state.markDirty();
				Story.setRespawn(player);
				Story.syncObjective(player);
			}
			case "join" -> Party.join(player, arg);
			case "leave" -> Party.leave(player.getServer(), arg);
			case "money" -> Progression.addMoney(player, Long.parseLong(arg));
			case "give" -> {
				Item item = Registries.ITEM.get(itemId(args[0]));
				int count = args.length > 1 ? Integer.parseInt(args[1]) : 1;
				ItemStack stack = new ItemStack(item, count);
				if (!player.getInventory().insertStack(stack)) player.dropItem(stack, false);
				player.sendMessage(Text.literal("+ ").append(new ItemStack(item, count).toHoverableText())
					.append(count > 1 ? " ×" + count : ""), false);
			}
			case "take" -> {
				Item item = Registries.ITEM.get(itemId(args[0]));
				int count = args.length > 1 ? Integer.parseInt(args[1]) : 1;
				for (int i = 0; i < player.getInventory().size() && count > 0; i++) {
					ItemStack s = player.getInventory().getStack(i);
					if (s.isOf(item)) {
						int take = Math.min(count, s.getCount());
						s.decrement(take);
						count -= take;
					}
				}
			}
			case "xp" -> Progression.addExp(player, Integer.parseInt(arg));
			case "sp" -> {
				d.skillPoints += Integer.parseInt(arg);
				state.markDirty();
				Progression.sync(player);
			}
			case "learn" -> {
				Skill s = Skill.byName(arg);
				if (s != null) Skills.grant(player, s);
			}
			case "class" -> Progression.setClass(player, PlayerClass.byName(arg));
			case "tp" -> Story.teleport(player, Locations.get(arg), true);
			case "tp_alone" -> Story.teleport(player, Locations.get(arg), false);
			case "party_tp" -> Party.teleportAll(player, Locations.get(arg));
			case "npc" -> Npcs.moveTo(player.getServer(), args[0], Locations.get(args[1]));
			case "sound" -> {
				SoundEvent ev = Registries.SOUND_EVENT.get(new Identifier(args[0].contains(":") ? args[0] : "konocraft:" + args[0]));
				if (ev != null) {
					float pitch = args.length > 1 ? Float.parseFloat(args[1]) : 1f;
					float vol = args.length > 2 ? Float.parseFloat(args[2]) : 1f;
					world.playSound(null, player.getX(), player.getY(), player.getZ(), ev, SoundCategory.MASTER, vol, pitch);
				}
			}
			case "vsound" -> {
				// vanilla sound
				SoundEvent ev = Registries.SOUND_EVENT.get(new Identifier("minecraft", args[0]));
				if (ev != null) {
					float pitch = args.length > 1 ? Float.parseFloat(args[1]) : 1f;
					world.playSound(null, player.getX(), player.getY(), player.getZ(), ev, SoundCategory.MASTER, 1f, pitch);
				}
			}
			case "title" -> {
				String[] parts = arg.split("\\|", 2);
				player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 50, 20));
				player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(parts.length > 1 ? format(player, parts[1]) : "")));
				player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(format(player, parts[0]))));
			}
			case "msg" -> player.sendMessage(Text.literal(format(player, arg)), false);
			case "fx" -> Fx.play(player, args[0], resolve(player, arg.contains(" ") ? arg.substring(arg.indexOf(' ') + 1) : "@player", session), session);
			case "target" -> session.target = resolve(player, arg, session);
			case "look" -> Story.lookAt(player, resolve(player, arg, session));
			case "time" -> {
				long t = switch (arg) {
					case "night" -> 14000;
					case "sunset" -> 12000;
					case "noon" -> 6000;
					case "morning" -> 0;
					default -> 1000;
				};
				long day = world.getTimeOfDay() / 24000L;
				long target = day * 24000L + t;
				if (target < world.getTimeOfDay()) target += 24000L;
				for (ServerWorld w : player.getServer().getWorlds()) w.setTimeOfDay(target);
			}
			case "nextday" -> {
				long day = world.getTimeOfDay() / 24000L + 1;
				for (ServerWorld w : player.getServer().getWorlds()) w.setTimeOfDay(day * 24000L + 1000);
			}
			case "weather" -> {
				if (arg.equals("rain")) world.setWeather(0, 6000, true, false);
				else world.setWeather(12000, 0, false, false);
			}
			case "affection" -> state.addAffection(args[0], Integer.parseInt(args[1]));
			case "daily" -> {
				d.dailies.put(arg, world.getTimeOfDay() / 24000L);
				state.markDirty();
			}
			case "heal" -> {
				player.setHealth(player.getMaxHealth());
				player.getHungerManager().add(20, 1f);
			}
			case "mana" -> {
				d.mana = d.maxMana();
				Progression.sync(player);
			}
			case "effect" -> {
				StatusEffect effect = Registries.STATUS_EFFECT.get(new Identifier(args[0]));
				if (effect != null) {
					int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 30;
					int amp = args.length > 2 ? Integer.parseInt(args[2]) : 0;
					player.addStatusEffect(new StatusEffectInstance(effect, seconds * 20, amp));
				}
			}
			case "dialogue" -> session.chain(arg);
			case "end" -> session.forceEnd();
			case "shop" -> session.onEnd.add(() -> Shops.open(player, arg));
			case "board" -> session.onEnd.add(() -> Bounties.openBoard(player));
			case "card" -> session.onEnd.add(() -> ServerNet.openScreen(player, Packets.SCREEN_CARD, PacketByteBufs.create()));
			case "credits" -> session.onEnd.add(() -> ServerNet.openScreen(player, Packets.SCREEN_CREDITS, PacketByteBufs.create()));
			case "music" -> {
				for (ServerPlayerEntity p : player.getServer().getPlayerManager().getPlayerList()) ServerNet.sendMusic(p, arg.equals("stop") ? "" : arg);
			}
			case "swallow" -> Cutscenes.swallow(player, arg);
			case "spawn_toads" -> Cutscenes.spawnToads(player, Integer.parseInt(args[0]), args.length > 1 ? Double.parseDouble(args[1]) : 20, session);
			case "megumin_explosion" -> Cutscenes.meguminExplosion(player, session, arg);
			case "collapse" -> {
				Entity e = Npcs.find(player.getServer(), arg);
				if (e instanceof CompanionEntity c) c.collapse(1200);
			}
			case "carry" -> {
				Entity e = Npcs.find(player.getServer(), arg);
				if (e instanceof CompanionEntity c) c.carryBy(player);
			}
			case "recover" -> {
				Entity e = Npcs.find(player.getServer(), arg);
				if (e instanceof CompanionEntity c) c.recover();
			}
			case "alarm" -> Cutscenes.alarm(player.getServer(), arg);
			case "cabbage_event" -> CabbageEvent.start(player.getServer(), arg.equals("story"));
			case "beldia_visit" -> Bosses.beldiaVisit(player);
			case "beldia_leave" -> Bosses.beldiaLeave(player.getServer());
			case "beldia_fight" -> Bosses.startBeldia(player);
			case "destroyer_start" -> Bosses.startDestroyer(player);
			case "boss" -> Bosses.action(player.getServer(), arg);
			case "curse" -> player.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.WITHER, 60, 0));
			case "kazuma_alive" -> state.setFlag("kazuma_alive", true);
			case "reward" -> {
				// reward <money> <exp>
				Progression.addMoney(player, Long.parseLong(args[0]));
				if (args.length > 1) Progression.addExp(player, Integer.parseInt(args[1]));
			}
			case "party_reward" -> {
				for (ServerPlayerEntity p : player.getServer().getPlayerManager().getPlayerList()) {
					Progression.addMoney(p, Long.parseLong(args[0]));
					if (args.length > 1) Progression.addExp(p, Integer.parseInt(args[1]));
				}
			}
			case "objective" -> Story.syncObjectiveAll(player.getServer());
			case "save_spawn" -> Story.setRespawn(player);
			case "counter" -> {
				state.questCounter = Integer.parseInt(arg);
				state.markDirty();
			}
			case "letterbox" -> ServerNet.sendFx(player, Packets.FX_LETTERBOX, Integer.parseInt(arg), 1f);
			case "kill_mobs" -> Cutscenes.killHostilesNear(player, Double.parseDouble(arg));
			case "keele_purify" -> WorldEvents.purifyKeele(player);
			case "thief_start" -> Thief.start(player);
			case "casino_stake" -> Casino.stake(player, Long.parseLong(arg));
			case "casino_dice" -> Casino.dice(player, arg);
			case "casino_roulette" -> Casino.roulette(player, arg);
			case "yunyun_duel" -> Activities.yunyunDuel(player, session, arg);
			case "party_trick" -> Activities.partyTrick(player);
			case "rate_explosion" -> Activities.rateExplosion(player, Integer.parseInt(arg));
			case "daily_explosion" -> Activities.dailyExplosion(player, session);
			case "steal_gag" -> Activities.stealGag(player);
			case "in_konosuba" -> {
				if (!KonoWorld.is(player.getWorld())) Story.teleport(player, Locations.AXEL, true);
			}
			default -> KonoCraft.LOGGER.warn("Unknown dialogue action '{}'", action);
		}
	}

	/** Resolves a target spec: location name, @char, @player, @target, "ahead N", or "x y z". */
	public static Vec3d resolve(ServerPlayerEntity player, String spec, DialogueManager.Session session) {
		if (spec == null || spec.isEmpty() || spec.equals("@player")) return player.getPos();
		if (spec.equals("@target")) return session != null && session.target != null ? session.target : player.getPos();
		if (spec.startsWith("@")) {
			Entity e = Npcs.find(player.getServer(), spec.substring(1));
			if (e == null) e = Bosses.find(player.getServer(), spec.substring(1));
			return e != null ? e.getPos() : player.getPos();
		}
		if (spec.startsWith("ahead")) {
			double dist = spec.length() > 5 ? Double.parseDouble(spec.substring(5).trim()) : 25;
			float yaw = player.getYaw();
			double dx = -MathHelper.sin(yaw * MathHelper.RADIANS_PER_DEGREE) * dist;
			double dz = MathHelper.cos(yaw * MathHelper.RADIANS_PER_DEGREE) * dist;
			int x = MathHelper.floor(player.getX() + dx), z = MathHelper.floor(player.getZ() + dz);
			int y = player.getServerWorld().getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
			return new Vec3d(x + 0.5, y, z + 0.5);
		}
		Locations.Loc loc = Locations.get(spec);
		if (loc != null) return loc.vec();
		String[] p = spec.split(" ");
		if (p.length == 3) {
			try {
				return new Vec3d(Double.parseDouble(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]));
			} catch (NumberFormatException ignored) {
			}
		}
		return player.getPos();
	}

	static boolean isNpc(Entity e, String key) {
		return e instanceof KonoNpcEntity n && n.characterKey().equals(key);
	}
}
