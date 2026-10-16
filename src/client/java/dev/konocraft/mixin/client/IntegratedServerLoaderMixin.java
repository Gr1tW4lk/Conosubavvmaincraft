package dev.konocraft.mixin.client;

import com.mojang.serialization.Lifecycle;
import net.minecraft.server.integrated.IntegratedServerLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Same as LevelPropertiesMixin, for the "experimental settings" question when creating a world. */
@Mixin(IntegratedServerLoader.class)
public abstract class IntegratedServerLoaderMixin {
	@ModifyVariable(method = "tryLoad", at = @At("HEAD"), argsOnly = true)
	private static Lifecycle konocraft$stable(Lifecycle lifecycle) {
		return Lifecycle.stable();
	}
}
