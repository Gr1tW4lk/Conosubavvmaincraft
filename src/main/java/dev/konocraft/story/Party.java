package dev.konocraft.story;

import dev.konocraft.entity.CompanionEntity;
import dev.konocraft.world.KonoWorld;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/** Aqua, Megumin and Darkness join the party and follow its leader around. */
public final class Party {
	private static UUID leaderUuid;

	private Party() {
	}

	public static void init() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (handler.player.getUuid().equals(leaderUuid)) leaderUuid = null;
		});
	}

	public static void setLeader(ServerPlayerEntity player) {
		leaderUuid = player.getUuid();
	}

	public static ServerPlayerEntity leader(MinecraftServer server) {
		if (leaderUuid != null) {
			ServerPlayerEntity p = server.getPlayerManager().getPlayer(leaderUuid);
			if (p != null && KonoWorld.is(p.getWorld())) return p;
		}
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			if (KonoWorld.is(p.getWorld()) && !p.isSpectator()) {
				leaderUuid = p.getUuid();
				return p;
			}
		}
		return null;
	}

	public static void join(ServerPlayerEntity player, String key) {
		StoryState s = StoryState.get(player.getServer());
		if (s.party.add(key)) {
			s.markDirty();
			s.setFlag("met_" + key, true);
			Speakers.Speaker sp = Speakers.get(key);
			for (ServerPlayerEntity p : player.getServer().getPlayerManager().getPlayerList()) {
				p.sendMessage(Text.literal("★ " + sp.name() + " присоединяется к группе!").formatted(Formatting.GOLD), false);
			}
		}
		setLeader(player);
		Entity e = Npcs.find(player.getServer(), key);
		if (e instanceof CompanionEntity c) c.setFollowing(true);
		Npcs.setOverride(key, null);
	}

	public static void leave(MinecraftServer server, String key) {
		StoryState s = StoryState.get(server);
		if (s.party.remove(key)) s.markDirty();
		Entity e = Npcs.find(server, key);
		if (e instanceof CompanionEntity c) c.setFollowing(false);
	}

	/** Brings the whole party along to a location (used by story teleports). */
	public static void teleportAll(ServerPlayerEntity player, Locations.Loc loc) {
		if (loc == null) return;
		MinecraftServer server = player.getServer();
		StoryState s = StoryState.get(server);
		ServerWorld world = KonoWorld.get(server);
		if (world == null) return;
		int i = 0;
		for (String key : s.party) {
			i++;
			double ox = (i % 2 == 0 ? 1.5 : -1.5) * ((i + 1) / 2), oz = 1.5;
			Entity e = Npcs.find(server, key);
			if (e != null) {
				e.requestTeleport(loc.x() + ox, loc.y(), loc.z() + oz);
				continue;
			}
			final double fx = loc.x() + ox, fz = loc.z() + oz;
			dev.konocraft.util.Scheduler.after(20, () -> {
				if (Npcs.find(server, key) == null) Npcs.spawn(world, key, fx, loc.y(), fz, loc.yaw());
			});
		}
	}
}
