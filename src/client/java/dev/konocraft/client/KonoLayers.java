package dev.konocraft.client;

import dev.konocraft.KonoCraft;
import dev.konocraft.client.model.GeneratedModels;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

public final class KonoLayers {
	public static final EntityModelLayer CHARACTER_SLIM = layer("character_slim");
	public static final EntityModelLayer CHARACTER_WIDE = layer("character_wide");
	public static final EntityModelLayer MEGUMIN_HAT = layer("megumin_hat");
	public static final EntityModelLayer GIANT_TOAD = layer("giant_toad");
	public static final EntityModelLayer FLYING_CABBAGE = layer("flying_cabbage");
	public static final EntityModelLayer DESTROYER_GOLEM = layer("destroyer_golem");
	public static final EntityModelLayer DESTROYER = layer("destroyer");
	public static final EntityModelLayer BARRIER_CRYSTAL = layer("barrier_crystal");
	public static final EntityModelLayer DESTROYER_CORE = layer("destroyer_core");

	private KonoLayers() {
	}

	private static EntityModelLayer layer(String name) {
		return new EntityModelLayer(KonoCraft.id(name), "main");
	}

	public static void register() {
		EntityModelLayerRegistry.registerModelLayer(CHARACTER_SLIM, () -> TexturedModelData.of(PlayerEntityModel.getTexturedModelData(Dilation.NONE, true), 64, 64));
		EntityModelLayerRegistry.registerModelLayer(CHARACTER_WIDE, () -> TexturedModelData.of(PlayerEntityModel.getTexturedModelData(Dilation.NONE, false), 64, 64));
		EntityModelLayerRegistry.registerModelLayer(MEGUMIN_HAT, GeneratedModels::meguminHat);
		EntityModelLayerRegistry.registerModelLayer(GIANT_TOAD, GeneratedModels::giant_toad);
		EntityModelLayerRegistry.registerModelLayer(FLYING_CABBAGE, GeneratedModels::flying_cabbage);
		EntityModelLayerRegistry.registerModelLayer(DESTROYER_GOLEM, GeneratedModels::destroyer_golem);
		EntityModelLayerRegistry.registerModelLayer(DESTROYER, GeneratedModels::destroyer);
		EntityModelLayerRegistry.registerModelLayer(BARRIER_CRYSTAL, GeneratedModels::barrier_crystal);
		EntityModelLayerRegistry.registerModelLayer(DESTROYER_CORE, GeneratedModels::destroyer_core);
	}
}
