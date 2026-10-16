package dev.konocraft.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Any datapack dimension marks a world as "experimental", which makes the game nag with a
 * backup prompt every time the world is opened. The KonoSuba dimension is a normal part of the mod.
 */
@Mixin(LevelProperties.class)
public abstract class LevelPropertiesMixin {
	@Inject(method = "getLifecycle", at = @At("HEAD"), cancellable = true)
	private void konocraft$stable(CallbackInfoReturnable<Lifecycle> cir) {
		cir.setReturnValue(Lifecycle.stable());
	}
}
