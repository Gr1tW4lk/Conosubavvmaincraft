package dev.konocraft.mixin.client;

import dev.konocraft.client.ClientState;
import dev.konocraft.registry.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.MusicSound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Boss fights and the ending override the biome music. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Inject(method = "getMusicType", at = @At("HEAD"), cancellable = true)
	private void konocraft$bossMusic(CallbackInfoReturnable<MusicSound> cir) {
		MusicSound music = switch (ClientState.music) {
			case "boss" -> ModSounds.BOSS_MUSIC;
			case "destroyer" -> ModSounds.DESTROYER_MUSIC;
			case "battle" -> ModSounds.BATTLE_MUSIC;
			case "ending" -> ModSounds.ENDING_MUSIC;
			default -> null;
		};
		if (music != null) cir.setReturnValue(music);
	}
}
