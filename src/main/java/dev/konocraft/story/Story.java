package dev.konocraft.story;

import dev.konocraft.KonoCraft;
import dev.konocraft.entity.KonoNpcEntity;
import dev.konocraft.net.ServerNet;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.registry.ModItems;
import dev.konocraft.skill.Skills;
import dev.konocraft.world.KonoWorld;
import dev.konocraft.world.Regions;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;

import java.util.Set;

/** The main story: chapters, triggers, objectives and who says what. */
public final class Story {
	// Stages
	public static final int NONE = 0;
	public static final int PROLOGUE = 10;
	public static final int CH1_GUILD = 20;
	public static final int CH1_TOADS = 21;
	public static final int CH1_REPORT = 22;
	public static final int CH2_MEET = 30;
	public static final int CH2_TEST = 31;
	public static final int CH2_REPORT = 32;
	public static final int CH3_MEET = 40;
	public static final int CH3_CABBAGE = 41;
	public static final int CH3_REPORT = 42;
	public static final int CH4_VIEWPOINT = 50;
	public static final int CH4_GATE = 51;
	public static final int CH4_CASTLE = 52;
	public static final int CH4_REPORT = 53;
	public static final int CH5_WIZ = 60;
	public static final int CH5_ALARM = 61;
	public static final int CH5_DEFENSE = 62;
	public static final int CH5_FIGHT = 63;
	public static final int ENDING = 70;
	public static final int POSTGAME = 100;

	public static final int TOAD_GOAL = 5;
	public static final int CABBAGE_GOAL = 15;

	private static int tickCounter;

	private Story() {
	}

	public static void init() {
		DialogueManager.init();
		Npcs.init();
		Party.init();
		CabbageEvent.init();
		Bosses.init();
		Skills.init();
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			Dialogues.load();
			StoryState state = StoryState.get(server);
			if (!state.hasFlag("rules_set")) {
				server.getGameRules().get(GameRules.KEEP_INVENTORY).set(true, server);
				state.setFlag("rules_set", true);
			}
		});
		// JOIN fires while vanilla is still adding the player to the world, and server.execute() would
		// run right away on the server thread; teleporting to another dimension at that point leaves
		// the player registered in the wrong world's chunk tracker (crash on disconnect).
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> dev.konocraft.util.Scheduler.after(5, () -> {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(handler.player.getUuid());
			if (player != null && !player.isRemoved()) onJoin(player);
		}));
		ServerPlayerEvents.AFTER_RESPAWN.register(Story::onRespawn);
		ServerLivingEntityEvents.AFTER_DEATH.register(Story::onDeath);
		ServerTickEvents.END_SERVER_TICK.register(Story::tick);
	}

	public static boolean autostart() {
		return !Boolean.getBoolean("konocraft.noautostart");
	}

	// ---------------------------------------------------------------------------------------------

	private static void onJoin(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		StoryState state = StoryState.get(server);
		PlayerData d = state.player(player.getUuid());
		Progression.applyAttributes(player);
		Progression.sync(player);
		if (!d.prologueDone && autostart()) {
			startPrologue(player);
		} else {
			syncObjective(player);
		}
	}

	public static void startPrologue(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		StoryState state = StoryState.get(server);
		if (state.stage < PROLOGUE) setStage(server, PROLOGUE);
		teleport(player, Locations.AFTERLIFE, false);
		if (player.getInventory().count(ModItems.ADVENTURER_CARD) == 0) {
			player.getInventory().insertStack(new ItemStack(ModItems.GUIDE_BOOK));
		}
		dev.konocraft.util.Scheduler.after(60, () -> {
			if (!player.isRemoved()) DialogueManager.start(player, "prologue");
		});
	}

	private static void onRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity player, boolean alive) {
		if (alive) return;
		StoryState state = StoryState.get(player.getServer());
		PlayerData d = state.player(player.getUuid());
		Progression.applyAttributes(player);
		Progression.sync(player);
		if (!d.prologueDone) return;
		if (!KonoWorld.is(player.getWorld())) {
			teleport(player, Locations.CHURCH, false);
		}
		String[] lines = {
			"«Вы снова умерли. Пожалуйста, будьте осторожнее... ради меня.» — Эрис",
			"«Аква уже воскресила вас. Она просит передать, что вы ей должны.» — Эрис",
			"«Знаете, у Казумы-сана был рекорд. Не пытайтесь его побить.» — Эрис",
			"«Ваша душа так часто здесь бывает, что я поставила для неё отдельный стул.» — Эрис",
			"«Я подправила документы. Никто не узнает. Только никому не говорите!» — Эрис"
		};
		player.sendMessage(Text.literal(lines[player.getRandom().nextInt(lines.length)]).formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC), false);
		Party.teleportAll(player, Locations.CHURCH);
	}

	private static final String[] DEATHS = {
		"поскользнулись на банановой кожуре, которую сами же и бросили",
		"испугались трактора... хотя это был просто очень громкий газонокосильщик",
		"уснули за компьютером и упали со стула прямо на клавиатуру",
		"пытались погладить крипера. Он не оценил",
		"подавились рисовым шариком, пока смотрели аниме",
		"героически спасли кота, который, как выяснилось, ни в чём не нуждался"
	};

	public static String deathReason(ServerPlayerEntity player) {
		int i = Math.floorMod(player.getUuid().hashCode(), DEATHS.length);
		return DEATHS[i];
	}

	public static void setRespawn(ServerPlayerEntity player) {
		Locations.Loc c = Locations.CHURCH;
		player.setSpawnPoint(KonoWorld.KEY, BlockPos.ofFloored(c.x(), c.y(), c.z()), c.yaw(), true, false);
	}

	// ---------------------------------------------------------------------------------------------

	public static void setStage(MinecraftServer server, int stage) {
		StoryState state = StoryState.get(server);
		int old = state.stage;
		state.stage = stage;
		state.questCounter = 0;
		state.markDirty();
		KonoCraft.LOGGER.info("Story stage {} -> {}", old, stage);
		Npcs.clearOverrides();
		onStageEnter(server, stage);
		Npcs.refresh(server);
		syncObjectiveAll(server);
	}

	private static void onStageEnter(MinecraftServer server, int stage) {
		switch (stage) {
			case CH3_CABBAGE -> CabbageEvent.start(server, true);
			case CH5_ALARM -> dev.konocraft.util.Scheduler.after(160, () -> {
				for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
					if (KonoWorld.is(p.getWorld())) {
						DialogueManager.start(p, "ch5_alarm");
						return;
					}
				}
			});
			case ENDING -> dev.konocraft.util.Scheduler.after(100, () -> {
				if (StoryState.get(server).stage != ENDING) return;
				for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
					if (DialogueManager.inDialogue(p)) return;
				}
				for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
					DialogueManager.start(p, "ending");
					return;
				}
			});
			default -> {
			}
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Teleports

	public static void teleport(ServerPlayerEntity player, Locations.Loc loc, boolean withParty) {
		if (loc == null) return;
		ServerWorld world = KonoWorld.get(player.getServer());
		if (world == null) {
			KonoCraft.LOGGER.error("KonoSuba dimension is missing!");
			return;
		}
		player.teleport(world, loc.x(), loc.y(), loc.z(), loc.yaw(), 0);
		player.fallDistance = 0;
		if (withParty) Party.teleportAll(player, loc);
	}

	public static void lookAt(ServerPlayerEntity player, Vec3d target) {
		if (target == null) return;
		Vec3d eye = player.getEyePos();
		double dx = target.x - eye.x, dy = target.y + 1 - eye.y, dz = target.z - eye.z;
		double h = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (MathHelper.atan2(dz, dx) * MathHelper.DEGREES_PER_RADIAN) - 90f;
		float pitch = (float) -(MathHelper.atan2(dy, h) * MathHelper.DEGREES_PER_RADIAN);
		player.networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), yaw, pitch);
	}

	// ---------------------------------------------------------------------------------------------
	// Talking to characters

	public static void talk(ServerPlayerEntity player, KonoNpcEntity npc) {
		if (DialogueManager.inDialogue(player)) return;
		String id = null;
		if (npc instanceof dev.konocraft.entity.CompanionEntity c) {
			int st = c.getCompanionState();
			if (st == dev.konocraft.entity.CompanionEntity.STATE_CARRIED) id = c.characterKey() + "_carried";
			else if (st == dev.konocraft.entity.CompanionEntity.STATE_COLLAPSED || st == dev.konocraft.entity.CompanionEntity.STATE_FAINTED) id = c.characterKey() + "_down";
			if (id != null && Dialogues.get(id) == null) id = null;
			if (c.isFollowing()) Party.setLeader(player);
		}
		if (id == null) id = Talk.dialogueFor(player, npc.characterKey());
		if (id != null) DialogueManager.start(player, id);
	}

	// ---------------------------------------------------------------------------------------------
	// Objectives

	public record Objective(String text, Locations.Loc loc) {
	}

	public static Objective objective(StoryState s) {
		return switch (s.stage) {
			case PROLOGUE -> new Objective("Поговорите с богиней Эрис", Locations.ERIS);
			case CH1_GUILD -> new Objective("Зарегистрируйтесь в Гильдии авантюристов", Locations.LUNA);
			case CH1_TOADS -> new Objective("Убейте гигантских жаб на восточных равнинах: " + s.questCounter + "/" + TOAD_GOAL, Locations.TOAD_PLAINS);
			case CH1_REPORT -> new Objective("Доложите Луне о выполненном задании", Locations.LUNA);
			case CH2_MEET -> new Objective("Кто-то откликнулся на объявление о наборе в группу. Загляните в таверну гильдии", Locations.GUILD_MEGUMIN);
			case CH2_TEST -> new Objective("Испытайте новую волшебницу на жабьих равнинах", Locations.TOAD_PLAINS);
			case CH2_REPORT -> new Objective("Вернитесь в гильдию за наградой", Locations.LUNA);
			case CH3_MEET -> new Objective("Вас ищет подозрительно воодушевлённый крестоносец", Locations.GUILD_DARKNESS);
			case CH3_CABBAGE -> new Objective("Экстренный квест! Поймайте летающую капусту: " + s.questCounter + "/" + CABBAGE_GOAL, Locations.CABBAGE_FIELDS);
			case CH3_REPORT -> new Objective("Получите награду за капусту у Луны", Locations.LUNA);
			case CH4_VIEWPOINT -> new Objective("Ежедневный взрыв: проводите Мегумин на холм к северо-востоку", Locations.VIEWPOINT);
			case CH4_GATE -> new Objective("У северных ворот какой-то шум!", Locations.NORTH_GATE);
			case CH4_CASTLE -> new Objective("Ворвитесь в замок и победите Бельдию в тронном зале", Locations.CASTLE_KEEP);
			case CH4_REPORT -> new Objective("Доложите о победе над генералом армии Короля Демонов", Locations.LUNA);
			case CH5_WIZ -> new Objective("Загляните в магазин магических предметов на площади", Locations.WIZ);
			case CH5_ALARM -> new Objective("Город как-то подозрительно затих...", null);
			case CH5_DEFENSE -> new Objective("Экстренный квест! Все на линию обороны у южных ворот!", Locations.DEFENSE_LINE);
			case CH5_FIGHT -> new Objective("Остановите Мобильную крепость Разрушитель!", null);
			case ENDING -> new Objective("Победа! Возвращайтесь в Аксель", Locations.GUILD_INSIDE);
			case POSTGAME -> new Objective("Свободная игра: доска заданий, взрывы с Мегумин, капустный сезон", null);
			default -> new Objective("", null);
		};
	}

	public static void syncObjective(ServerPlayerEntity player) {
		StoryState s = StoryState.get(player.getServer());
		PlayerData d = s.player(player.getUuid());
		Objective o = d.prologueDone || s.stage == PROLOGUE ? objective(s) : new Objective("Поговорите с богиней Эрис", Locations.ERIS);
		String text = o.text();
		if (!d.bounty.isEmpty()) text = text + "\n" + Bounties.describe(d);
		ServerNet.sendObjective(player, text, o.loc() == null ? null : o.loc().vec());
	}

	public static void syncObjectiveAll(MinecraftServer server) {
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) syncObjective(p);
	}

	public static void addProgress(MinecraftServer server, int amount, int goal, int nextStage) {
		StoryState s = StoryState.get(server);
		s.questCounter += amount;
		s.markDirty();
		if (s.questCounter >= goal) {
			s.questCounter = goal;
			setStage(server, nextStage);
		} else {
			syncObjectiveAll(server);
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Kills

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (entity.getWorld().isClient) return;
		MinecraftServer server = entity.getServer();
		if (server == null) return;
		ServerPlayerEntity killer = null;
		Entity attacker = source.getAttacker();
		if (attacker instanceof ServerPlayerEntity p) killer = p;
		else if (attacker instanceof dev.konocraft.entity.CompanionEntity c) killer = c.leader();
		Rewards.onKill(server, entity, killer);
	}

	// ---------------------------------------------------------------------------------------------
	// Tick: location triggers, mana and NPCs

	private static final Set<Integer> TRIGGER_STAGES = Set.of(CH1_TOADS, CH2_TEST, CH3_CABBAGE, CH4_VIEWPOINT, CH4_GATE, CH4_CASTLE, CH5_DEFENSE, ENDING);

	private static void tick(MinecraftServer server) {
		tickCounter++;
		if (tickCounter % 10 == 0) {
			for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
				Progression.regenMana(p);
				if (tickCounter % 40 == 0) ServerNet.sendMana(p, Progression.data(p));
			}
		}
		if (tickCounter % 10 != 5) return;
		StoryState state = StoryState.get(server);
		if (!TRIGGER_STAGES.contains(state.stage)) return;
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			if (!KonoWorld.is(p.getWorld()) || DialogueManager.inDialogue(p) || p.isSpectator()) continue;
			checkTriggers(p, state);
		}
	}

	private static boolean near(ServerPlayerEntity p, Locations.Loc loc, double r) {
		return p.squaredDistanceTo(loc.x(), p.getY(), loc.z()) < r * r;
	}

	private static void checkTriggers(ServerPlayerEntity p, StoryState s) {
		switch (s.stage) {
			case CH1_TOADS -> {
				if (!s.hasFlag("toads_arrived") && near(p, Locations.TOAD_PLAINS, 45)) {
					s.setFlag("toads_arrived", true);
					DialogueManager.start(p, "ch1_toads_arrive");
				}
			}
			case CH2_TEST -> {
				if (!s.hasFlag("explosion_test") && near(p, Locations.TOAD_PLAINS, 45)) {
					s.setFlag("explosion_test", true);
					DialogueManager.start(p, "ch2_explosion_test");
				}
			}
			case CH3_CABBAGE -> {
				if (!s.hasFlag("cabbage_arrived") && near(p, Locations.CABBAGE_FIELDS, 50)) {
					s.setFlag("cabbage_arrived", true);
					DialogueManager.start(p, "ch3_cabbage_arrive");
				}
			}
			case CH4_VIEWPOINT -> {
				if (near(p, Locations.VIEWPOINT, 10) && Math.abs(p.getY() - Locations.VIEWPOINT.y()) < 6) {
					DialogueManager.start(p, "ch4_castle_explosion");
				}
			}
			case CH4_GATE -> {
				if (near(p, Locations.NORTH_GATE, 14)) {
					DialogueManager.start(p, "ch4_beldia_gate");
				}
			}
			case CH4_CASTLE -> {
				if (!Bosses.beldiaActive(p.getServer()) && insideKeep(p)) {
					DialogueManager.start(p, s.hasFlag("beldia_met_castle") ? "ch4_beldia_retry" : "ch4_beldia_castle");
				}
			}
			case CH5_DEFENSE -> {
				if (!s.hasFlag("defense_prompted") && near(p, Locations.DEFENSE_LINE, 14)) {
					DialogueManager.start(p, s.destroyerAttempts > 0 ? "ch5_retry" : "ch5_war_council");
				}
			}
			case ENDING -> {
			}
			default -> {
			}
		}
	}

	public static boolean insideKeep(Entity p) {
		int x = MathHelper.floor(p.getX()), z = MathHelper.floor(p.getZ());
		return x > dev.konocraft.world.structure.BeldiaCastle.KX1 && x < dev.konocraft.world.structure.BeldiaCastle.KX2
			&& z > dev.konocraft.world.structure.BeldiaCastle.KZ1 && z < dev.konocraft.world.structure.BeldiaCastle.KZ2 - 2
			&& p.getY() > Regions.CASTLE_Y - 1 && p.getY() < Regions.CASTLE_Y + 16;
	}

	public static boolean isCharacter(Entity e, String key) {
		return e != null && e.getType() == ModEntities.byCharacter(key);
	}
}
