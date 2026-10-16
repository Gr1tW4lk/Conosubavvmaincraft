package dev.konocraft.registry;

import dev.konocraft.KonoCraft;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
	public static final RegistryEntry.Reference<SoundEvent> MUSIC_AXEL = registerReference("music.axel");
	public static final RegistryEntry.Reference<SoundEvent> MUSIC_FIELDS = registerReference("music.fields");
	public static final RegistryEntry.Reference<SoundEvent> MUSIC_CASTLE = registerReference("music.castle");
	public static final RegistryEntry.Reference<SoundEvent> MUSIC_AFTERLIFE = registerReference("music.afterlife");
	public static final RegistryEntry.Reference<SoundEvent> MUSIC_BATTLE = registerReference("music.battle");
	public static final RegistryEntry.Reference<SoundEvent> MUSIC_BOSS = registerReference("music.boss");
	public static final RegistryEntry.Reference<SoundEvent> MUSIC_DESTROYER = registerReference("music.destroyer");
	public static final RegistryEntry.Reference<SoundEvent> MUSIC_ENDING = registerReference("music.ending");

	public static final SoundEvent EXPLOSION_BIG = register("fx.explosion_big");
	public static final SoundEvent CHANT = register("fx.chant");
	public static final SoundEvent ALARM = register("fx.alarm");
	public static final SoundEvent BLIP = register("fx.blip");
	public static final SoundEvent LEVEL_UP = register("fx.level_up");
	public static final SoundEvent HOLY = register("fx.holy");
	public static final SoundEvent TOAD_CROAK = register("entity.toad.croak");
	public static final SoundEvent TOAD_GULP = register("entity.toad.gulp");
	public static final SoundEvent CABBAGE_WHOOSH = register("entity.cabbage.whoosh");
	public static final SoundEvent AQUA_CRY = register("voice.aqua_cry");
	public static final SoundEvent COIN = register("fx.coin");

	public static final MusicSound BOSS_MUSIC = new MusicSound(MUSIC_BOSS, 0, 0, true);
	public static final MusicSound DESTROYER_MUSIC = new MusicSound(MUSIC_DESTROYER, 0, 0, true);
	public static final MusicSound BATTLE_MUSIC = new MusicSound(MUSIC_BATTLE, 0, 0, true);
	public static final MusicSound ENDING_MUSIC = new MusicSound(MUSIC_ENDING, 0, 0, true);

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = KonoCraft.id(name);
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	private static RegistryEntry.Reference<SoundEvent> registerReference(String name) {
		Identifier id = KonoCraft.id(name);
		return Registry.registerReference(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	public static void init() {
	}
}
