package dev.konocraft.mixin;

import dev.konocraft.story.Restoration;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Remembers what creepers and TNT blow up in the KonoSuba world, so Axel's builders can fix it. */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
	@Shadow
	@Final
	private World world;

	@Shadow
	public abstract List<BlockPos> getAffectedBlocks();

	@Inject(method = "affectWorld", at = @At("HEAD"))
	private void konocraft$remember(boolean particles, CallbackInfo ci) {
		if (world.isClient) return;
		for (BlockPos pos : getAffectedBlocks()) {
			Restoration.record(world, pos, world.getBlockState(pos), Blocks.AIR.getDefaultState(), 0);
		}
	}
}
