package dev.konocraft.client.render;

import dev.konocraft.KonoCraft;
import dev.konocraft.client.KonoLayers;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

/** Megumin's iconic witch hat, attached to the head. */
public class HatFeatureRenderer<T extends LivingEntity> extends FeatureRenderer<T, CharacterModel<T>> {
	private static final Identifier TEXTURE = KonoCraft.id("textures/entity/megumin_hat.png");
	private final ModelPart hat;

	public HatFeatureRenderer(FeatureRendererContext<T, CharacterModel<T>> context, EntityModelLoader loader) {
		super(context);
		this.hat = loader.getModelPart(KonoLayers.MEGUMIN_HAT);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance,
					   float tickDelta, float animationProgress, float headYaw, float headPitch) {
		if (entity.isInvisible()) return;
		matrices.push();
		getContextModel().head.rotate(matrices);
		hat.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, OverlayTexture.DEFAULT_UV);
		matrices.pop();
	}
}
