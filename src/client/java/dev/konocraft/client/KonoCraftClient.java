package dev.konocraft.client;

import dev.konocraft.client.render.KonoRenderers;
import dev.konocraft.client.test.ClientAutotest;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.render.RenderLayer;

public class KonoCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		KonoLayers.register();
		KonoRenderers.register();
		KonoKeys.register();
		ClientNet.init();
		BlockRenderLayerMap.INSTANCE.putBlock(dev.konocraft.registry.ModBlocks.QUEST_BOARD, RenderLayer.getCutout());
		HudRenderCallback.EVENT.register(KonoHud::render);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			KonoKeys.tick(client);
			KonoHud.tick(client);
		});
		ClientAutotest.register();
	}
}
