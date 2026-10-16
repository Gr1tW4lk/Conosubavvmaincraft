package dev.konocraft.story;

import dev.konocraft.KonoCraft;
import dev.konocraft.net.ServerNet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Runs dialogue scripts for players on the server; the client only renders lines and returns choices. */
public final class DialogueManager {
	public static final class Session {
		final ServerPlayerEntity player;
		Dialogues.Dialogue dialogue;
		int index;
		Dialogues.Line current;
		int autoLeft;
		List<Integer> visibleChoices = new ArrayList<>();
		String chainNext;
		boolean ended;
		/** Free-form data actions can share (e.g. the explosion target). */
		public Vec3d target;
		public final List<Runnable> onEnd = new ArrayList<>();

		Session(ServerPlayerEntity player, Dialogues.Dialogue dialogue) {
			this.player = player;
			this.dialogue = dialogue;
		}

		public ServerPlayerEntity player() {
			return player;
		}

		public void chain(String id) {
			chainNext = id;
		}

		public void forceEnd() {
			index = Integer.MAX_VALUE / 2;
		}
	}

	private static final Map<UUID, Session> sessions = new HashMap<>();

	private DialogueManager() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> sessions.remove(handler.player.getUuid()));
	}

	public static boolean inDialogue(ServerPlayerEntity player) {
		Session s = sessions.get(player.getUuid());
		return s != null && !s.ended;
	}

	public static Session session(ServerPlayerEntity player) {
		return sessions.get(player.getUuid());
	}

	public static boolean start(ServerPlayerEntity player, String id) {
		return start(player, id, null);
	}

	public static boolean start(ServerPlayerEntity player, String id, Runnable onEnd) {
		Dialogues.Dialogue d = Dialogues.get(id);
		if (d == null) {
			KonoCraft.LOGGER.warn("Unknown dialogue {}", id);
			return false;
		}
		Session old = sessions.get(player.getUuid());
		Session s = new Session(player, d);
		if (old != null && !old.ended) {
			s.target = old.target;
		}
		if (onEnd != null) s.onEnd.add(onEnd);
		sessions.put(player.getUuid(), s);
		show(s);
		return true;
	}

	private static void show(Session s) {
		int guard = 0;
		while (!s.ended && s.index < s.dialogue.lines().size()) {
			if (++guard > 500) {
				KonoCraft.LOGGER.error("Dialogue {} looped", s.dialogue.id());
				break;
			}
			Dialogues.Line line = s.dialogue.lines().get(s.index);
			if (line.cond() != null && !Actions.check(s.player, line.cond())) {
				s.index++;
				continue;
			}
			for (String action : line.actions()) {
				try {
					Actions.run(s.player, action, s);
				} catch (Exception e) {
					KonoCraft.LOGGER.error("Dialogue action '{}' failed", action, e);
				}
				if (s.ended || sessions.get(s.player.getUuid()) != s) return;
			}
			if (s.index >= Integer.MAX_VALUE / 4) break;
			boolean shows = line.hasText() || !line.choices().isEmpty() || line.auto() > 0;
			if (shows) {
				s.current = line;
				s.autoLeft = line.auto();
				send(s, line);
				return;
			}
			if (line.end()) break;
			jumpAfter(s, line);
		}
		finish(s);
	}

	private static void jumpAfter(Session s, Dialogues.Line line) {
		if (line.gotoLabel() != null) {
			Integer target = s.dialogue.labels().get(line.gotoLabel());
			s.index = target != null ? target : s.dialogue.lines().size();
		} else {
			s.index++;
		}
	}

	private static void send(Session s, Dialogues.Line line) {
		String speakerKey = line.speaker() == null ? "narrator" : line.speaker();
		String name;
		int color;
		if ("player".equals(speakerKey)) {
			name = s.player.getGameProfile().getName();
			color = 0xFFFFFF;
		} else {
			Speakers.Speaker sp = Speakers.get(speakerKey);
			name = line.name() != null ? line.name() : sp.name();
			color = sp.color();
		}
		String text = line.hasText() ? Actions.format(s.player, line.text()) : "";
		s.visibleChoices.clear();
		List<String> choiceTexts = new ArrayList<>();
		for (int i = 0; i < line.choices().size(); i++) {
			Dialogues.Choice c = line.choices().get(i);
			if (c.cond() == null || Actions.check(s.player, c.cond())) {
				s.visibleChoices.add(i);
				choiceTexts.add(Actions.format(s.player, c.text()));
			}
		}
		int portrait = findPortrait(s.player, speakerKey);
		ServerNet.sendLine(s.player, speakerKey, name, color, text, choiceTexts, line.auto(), portrait);
	}

	/** The entity id of the nearest entity matching the speaker, for the client-side portrait. */
	private static int findPortrait(ServerPlayerEntity player, String key) {
		if ("player".equals(key)) return player.getId();
		Entity best = null;
		double bestDist = 40 * 40;
		for (Entity e : player.getServerWorld().getOtherEntities(player, player.getBoundingBox().expand(40), e -> e instanceof LivingEntity)) {
			String path = Registries.ENTITY_TYPE.getId(e.getType()).getPath();
			if (!path.equals(key)) continue;
			double d = e.squaredDistanceTo(player);
			if (d < bestDist) {
				bestDist = d;
				best = e;
			}
		}
		return best == null ? -1 : best.getId();
	}

	public static void advance(ServerPlayerEntity player, int choice) {
		Session s = sessions.get(player.getUuid());
		if (s == null || s.ended || s.current == null) return;
		Dialogues.Line line = s.current;
		if (!line.choices().isEmpty()) {
			if (choice < 0 || choice >= s.visibleChoices.size()) {
				// re-send so the client shows the choices again
				send(s, line);
				return;
			}
			Dialogues.Choice c = line.choices().get(s.visibleChoices.get(choice));
			s.current = null;
			if (c.label() == null) {
				s.index++;
			} else if ("end".equals(c.label())) {
				finish(s);
				return;
			} else {
				Integer target = s.dialogue.labels().get(c.label());
				s.index = target != null ? target : s.dialogue.lines().size();
			}
			show(s);
			return;
		}
		s.current = null;
		if (line.end()) {
			finish(s);
			return;
		}
		jumpAfter(s, line);
		show(s);
	}

	private static void finish(Session s) {
		if (s.ended) return;
		s.ended = true;
		if (sessions.get(s.player.getUuid()) == s) sessions.remove(s.player.getUuid());
		String next = s.chainNext;
		if (next == null) ServerNet.sendClose(s.player);
		for (Runnable r : s.onEnd) {
			try {
				r.run();
			} catch (Exception e) {
				KonoCraft.LOGGER.error("Dialogue end hook failed", e);
			}
		}
		if (next != null) {
			Dialogues.Dialogue d = Dialogues.get(next);
			if (d != null) {
				Session ns = new Session(s.player, d);
				ns.target = s.target;
				sessions.put(s.player.getUuid(), ns);
				show(ns);
			} else {
				ServerNet.sendClose(s.player);
			}
		}
	}

	public static void close(ServerPlayerEntity player) {
		Session s = sessions.remove(player.getUuid());
		if (s != null) {
			s.ended = true;
			ServerNet.sendClose(player);
		}
	}

	private static void tick() {
		if (sessions.isEmpty()) return;
		for (Session s : new ArrayList<>(sessions.values())) {
			if (s.ended || s.current == null) continue;
			if (s.player.isRemoved()) {
				sessions.remove(s.player.getUuid());
				continue;
			}
			if (s.autoLeft > 0 && --s.autoLeft == 0) {
				advance(s.player, -1);
			}
		}
	}
}
