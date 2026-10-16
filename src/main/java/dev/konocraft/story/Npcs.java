package dev.konocraft.story;

import dev.konocraft.entity.CompanionEntity;
import dev.konocraft.entity.KonoNpcEntity;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.world.KonoWorld;
import dev.konocraft.world.Regions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps exactly one of every named character in the world, at the right place for the story. */
public final class Npcs {
	public static final String[] CHARACTERS = {"eris", "kazuma", "luna", "aqua", "megumin", "darkness", "wiz", "vanir", "yunyun"};

	/** Temporary positions set by cutscenes; cleared whenever the stage changes. */
	private static final Map<String, Locations.Loc> overrides = new HashMap<>();
	private static int tick;

	private Npcs() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (++tick % 40 == 0) refresh(server);
		});
	}

	public static Locations.Loc homeFor(StoryState s, String key) {
		Locations.Loc o = overrides.get(key);
		if (o != null) return o;
		int st = s.stage;
		boolean battle = st == Story.CH5_DEFENSE || st == Story.CH5_FIGHT;
		return switch (key) {
			case "eris" -> Locations.ERIS;
			case "kazuma" -> s.hasFlag("kazuma_alive") ? Locations.GUILD_MEGUMIN : Locations.KAZUMA_GHOST;
			case "luna" -> battle ? Locations.DEFENSE_LUNA : Locations.LUNA;
			case "aqua" -> st >= Story.CH1_GUILD ? Locations.GUILD_AQUA : null;
			case "megumin" -> st >= Story.CH2_MEET ? Locations.GUILD_MEGUMIN : null;
			case "darkness" -> st >= Story.CH3_MEET ? Locations.GUILD_DARKNESS : null;
			case "wiz" -> battle ? Locations.DEFENSE_WIZ : Locations.WIZ;
			case "vanir" -> st >= Story.CH5_WIZ ? Locations.VANIR : null;
			case "yunyun" -> st >= Story.CH2_REPORT ? Locations.YUNYUN : null;
			default -> null;
		};
	}

	public static void setOverride(String key, Locations.Loc loc) {
		if (loc == null) overrides.remove(key);
		else overrides.put(key, loc);
	}

	public static void clearOverrides() {
		overrides.clear();
	}

	public static Entity find(MinecraftServer server, String key) {
		ServerWorld world = KonoWorld.get(server);
		if (world == null) return null;
		UUID uuid = StoryState.get(server).npcs.get(key);
		if (uuid == null) return null;
		return world.getEntity(uuid);
	}

	public static void refresh(MinecraftServer server) {
		ServerWorld world = KonoWorld.get(server);
		if (world == null) return;
		StoryState s = StoryState.get(server);
		ServerPlayerEntity leader = Party.leader(server);
		for (String key : CHARACTERS) {
			boolean inParty = s.party.contains(key);
			Entity e = find(server, key);
			if (inParty && overrides.get(key) == null) {
				if (leader == null || !KonoWorld.is(leader.getWorld()) || Regions.inAfterlife((int) leader.getX(), (int) leader.getZ())) continue;
				if (e == null) {
					spawn(world, key, leader.getX() + world.random.nextInt(5) - 2, leader.getY(), leader.getZ() + world.random.nextInt(5) - 2, leader.getYaw());
				} else if (e instanceof CompanionEntity c) {
					c.setFollowing(true);
				}
				continue;
			}
			Locations.Loc home = homeFor(s, key);
			if (home == null) {
				if (e != null) {
					e.discard();
					s.npcs.remove(key);
					s.markDirty();
				}
				continue;
			}
			boolean homeLoaded = world.isChunkLoaded(MathHelper.floor(home.x()) >> 4, MathHelper.floor(home.z()) >> 4);
			if (e == null) {
				if (homeLoaded) spawn(world, key, home.x(), home.y(), home.z(), home.yaw());
				continue;
			}
			if (e instanceof CompanionEntity c) c.setFollowing(false);
			if (e instanceof KonoNpcEntity n) n.setHome(home.x(), home.y(), home.z(), home.yaw());
			if (e.squaredDistanceTo(home.x(), home.y(), home.z()) > 12 * 12) {
				e.requestTeleport(home.x(), home.y(), home.z());
				e.setYaw(home.yaw());
			}
		}
	}

	public static Entity spawn(ServerWorld world, String key, double x, double y, double z, float yaw) {
		EntityType<? extends KonoNpcEntity> type = ModEntities.byCharacter(key);
		if (type == null) return null;
		KonoNpcEntity e = type.create(world);
		if (e == null) return null;
		e.refreshPositionAndAngles(x, y, z, yaw, 0);
		e.setHeadYaw(yaw);
		e.setBodyYaw(yaw);
		e.setHome(x, y, z, yaw);
		StoryState s = StoryState.get(world.getServer());
		Entity old = find(world.getServer(), key);
		if (old != null) old.discard();
		s.npcs.put(key, e.getUuid());
		s.markDirty();
		world.spawnEntity(e);
		return e;
	}

	/** Moves a character for a cutscene: teleports it if loaded, otherwise spawns it there. */
	public static void moveTo(MinecraftServer server, String key, Locations.Loc loc) {
		if (loc == null) return;
		ServerWorld world = KonoWorld.get(server);
		if (world == null) return;
		setOverride(key, loc);
		Entity e = find(server, key);
		if (e != null) {
			e.requestTeleport(loc.x(), loc.y(), loc.z());
			e.setYaw(loc.yaw());
			if (e instanceof KonoNpcEntity n) n.setHome(loc.x(), loc.y(), loc.z(), loc.yaw());
		} else {
			dev.konocraft.util.Scheduler.after(10, () -> spawn(world, key, loc.x(), loc.y(), loc.z(), loc.yaw()));
		}
	}
}
