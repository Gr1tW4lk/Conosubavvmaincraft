package dev.konocraft.story;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** World-wide story progress. The main story is shared by everyone on the server. */
public class StoryState extends PersistentState {
	private static final String KEY = "konocraft_story";

	public int stage;
	public int questCounter;
	public final Set<String> flags = new HashSet<>();
	/** Companions that joined the party, in join order. */
	public final Set<String> party = new LinkedHashSet<>();
	/** Character key -> UUID of the one true entity of that character. */
	public final Map<String, UUID> npcs = new HashMap<>();
	public final Map<String, Integer> affection = new HashMap<>();
	public final Map<UUID, PlayerData> players = new HashMap<>();
	public long nextCabbageDay = 2;
	public boolean cabbageEventActive;
	public int destroyerAttempts;

	public static StoryState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(StoryState::fromNbt, StoryState::new, KEY);
	}

	public PlayerData player(UUID uuid) {
		return players.computeIfAbsent(uuid, u -> {
			markDirty();
			return new PlayerData();
		});
	}

	public boolean hasFlag(String flag) {
		return flags.contains(flag);
	}

	public void setFlag(String flag, boolean value) {
		if (value ? flags.add(flag) : flags.remove(flag)) markDirty();
	}

	public int affection(String who) {
		return affection.getOrDefault(who, 0);
	}

	public void addAffection(String who, int amount) {
		affection.merge(who, amount, Integer::sum);
		markDirty();
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		nbt.putInt("stage", stage);
		nbt.putInt("questCounter", questCounter);
		NbtList flagList = new NbtList();
		for (String f : flags) flagList.add(NbtString.of(f));
		nbt.put("flags", flagList);
		NbtList partyList = new NbtList();
		for (String p : party) partyList.add(NbtString.of(p));
		nbt.put("party", partyList);
		NbtCompound npcTag = new NbtCompound();
		npcs.forEach(npcTag::putUuid);
		nbt.put("npcs", npcTag);
		NbtCompound aff = new NbtCompound();
		affection.forEach(aff::putInt);
		nbt.put("affection", aff);
		NbtCompound pl = new NbtCompound();
		players.forEach((uuid, data) -> pl.put(uuid.toString(), data.toNbt()));
		nbt.put("players", pl);
		nbt.putLong("nextCabbageDay", nextCabbageDay);
		nbt.putBoolean("cabbageEvent", cabbageEventActive);
		nbt.putInt("destroyerAttempts", destroyerAttempts);
		return nbt;
	}

	public static StoryState fromNbt(NbtCompound nbt) {
		StoryState s = new StoryState();
		s.stage = nbt.getInt("stage");
		s.questCounter = nbt.getInt("questCounter");
		for (NbtElement e : nbt.getList("flags", NbtElement.STRING_TYPE)) s.flags.add(e.asString());
		for (NbtElement e : nbt.getList("party", NbtElement.STRING_TYPE)) s.party.add(e.asString());
		NbtCompound npcTag = nbt.getCompound("npcs");
		for (String k : npcTag.getKeys()) {
			if (npcTag.containsUuid(k)) s.npcs.put(k, npcTag.getUuid(k));
		}
		NbtCompound aff = nbt.getCompound("affection");
		for (String k : aff.getKeys()) s.affection.put(k, aff.getInt(k));
		NbtCompound pl = nbt.getCompound("players");
		for (String k : pl.getKeys()) {
			try {
				s.players.put(UUID.fromString(k), PlayerData.fromNbt(pl.getCompound(k)));
			} catch (IllegalArgumentException ignored) {
			}
		}
		if (nbt.contains("nextCabbageDay")) s.nextCabbageDay = nbt.getLong("nextCabbageDay");
		s.cabbageEventActive = nbt.getBoolean("cabbageEvent");
		s.destroyerAttempts = nbt.getInt("destroyerAttempts");
		return s;
	}
}
