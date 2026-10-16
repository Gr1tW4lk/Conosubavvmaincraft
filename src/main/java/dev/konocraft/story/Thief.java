package dev.konocraft.story;

import dev.konocraft.entity.KonoMonsterEntity;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.world.KonoWorld;
import dev.konocraft.world.Regions;
import dev.konocraft.world.structure.TownPlan;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Sena's errand: a goblin pickpocket hides somewhere in the back yards of Axel. The objective only
 * names the quarter and points at the neighbourhood, the rest is a chase through the alleys.
 */
public final class Thief {
	public static final String TAG = "konothief";
	private static final int TIMEOUT = 20 * 60 * 12;
	private static BlockPos hint;
	private static long startedAt;

	private Thief() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Thief::tick);
	}

	public static boolean active(ServerPlayerEntity p) {
		return StoryState.get(p.getServer()).player(p.getUuid()).flags.contains("thief_active");
	}

	public static void start(ServerPlayerEntity player) {
		ServerWorld w = KonoWorld.get(player.getServer());
		if (w == null) return;
		stop(player.getServer(), false);
		BlockPos spot = null;
		for (int i = 0; i < 400 && spot == null; i++) {
			int x = w.random.nextInt(2 * TownPlan.LIM) - TownPlan.LIM;
			int z = w.random.nextInt(2 * TownPlan.LIM) - TownPlan.LIM;
			if (TownPlan.cell(x, z) != TownPlan.FREE) continue;
			if (Regions.dist(x, z, player.getX(), player.getZ()) < 45) continue;
			BlockPos p = new BlockPos(x, Regions.TOWN_Y + 1, z);
			if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
			if (w.getBlockState(p.down()).isSolid() && w.getBlockState(p).isAir() && w.getBlockState(p.up()).isAir()) spot = p;
		}
		if (spot == null) return;
		KonoMonsterEntity.Goblin g = ModEntities.GOBLIN.create(w);
		if (g == null) return;
		g.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, w.random.nextFloat() * 360, 0);
		g.initialize(w, w.getLocalDifficulty(spot), SpawnReason.EVENT, null, null);
		g.setCustomName(Text.literal("Карманник").formatted(Formatting.GOLD));
		g.setCustomNameVisible(true);
		g.addCommandTag(TAG);
		g.setPersistent();
		w.spawnEntity(g);
		hint = spot.add(w.random.nextInt(17) - 8, 0, w.random.nextInt(17) - 8);
		startedAt = w.getTime();
		StoryState s = StoryState.get(player.getServer());
		s.player(player.getUuid()).flags.add("thief_active");
		s.markDirty();
		Story.syncObjective(player);
	}

	/** Where the objective marker points while the chase is on (roughly, not exactly). */
	public static Vec3d hintPos() {
		return hint == null ? null : Vec3d.ofBottomCenter(hint);
	}

	public static String describe() {
		if (hint == null) return "Найдите карманника где-то в переулках Акселя";
		return "Найдите карманника: его видели " + quarter(hint.getX(), hint.getZ());
	}

	/** "in the north-east quarter, near the market square" */
	public static String quarter(int x, int z) {
		String[] dirs = {"в восточном", "в юго-восточном", "в южном", "в юго-западном", "в западном", "в северо-западном", "в северном", "в северо-восточном"};
		double a = Math.atan2(z, x);
		int i = Math.floorMod((int) Math.round(a / (Math.PI / 4)), 8);
		String where = Math.max(Math.abs(x), Math.abs(z)) > 80 ? " квартале у городской стены" : " квартале старого города";
		Locations.Loc best = null;
		double bd = Double.MAX_VALUE;
		for (String key : new String[]{"market", "inn", "bath", "smithy", "police", "alderp", "dustiness", "park", "barracks",
			"warehouses", "guild", "church", "stable", "mansion", "wiz_shop", "axel"}) {
			Locations.Loc l = Locations.get(key);
			double d = Regions.dist(x, z, l.x(), l.z());
			if (d < bd) {
				bd = d;
				best = l;
			}
		}
		return dirs[i] + where + (best != null ? ", недалеко от места «" + best.title() + "»" : "");
	}

	public static void onKill(MinecraftServer server, LivingEntity dead, ServerPlayerEntity killer) {
		if (!dead.getCommandTags().contains(TAG)) return;
		StoryState s = StoryState.get(server);
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			PlayerData d = s.player(p.getUuid());
			if (d.flags.remove("thief_active")) {
				d.flags.add("thief_caught");
				p.sendMessage(Text.literal("Карманник пойман! Сена будет довольна. Наверное.").formatted(Formatting.GOLD), false);
				Story.syncObjective(p);
			}
		}
		if (killer != null) {
			Progression.addMoney(killer, 20000);
			Progression.addExp(killer, 160);
			killer.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8f, 1.3f);
		}
		s.markDirty();
		hint = null;
	}

	/** Removes the thief and forgets the errand (he got away). */
	public static void stop(MinecraftServer server, boolean escaped) {
		ServerWorld w = KonoWorld.get(server);
		if (w != null) {
			for (Entity e : w.iterateEntities()) {
				if (e.getCommandTags().contains(TAG)) e.discard();
			}
		}
		StoryState s = StoryState.get(server);
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			if (s.player(p.getUuid()).flags.remove("thief_active")) {
				if (escaped) p.sendMessage(Text.literal("Карманник скрылся. Сена что-то мрачно записывает в блокнот.").formatted(Formatting.GRAY), false);
				Story.syncObjective(p);
			}
		}
		s.markDirty();
		hint = null;
	}

	private static void tick(MinecraftServer server) {
		if (hint == null || server.getTicks() % 100 != 0) return;
		ServerWorld w = KonoWorld.get(server);
		if (w != null && w.getTime() - startedAt > TIMEOUT) stop(server, true);
	}
}
