package dev.konocraft.client.render;

import dev.konocraft.KonoCraft;
import dev.konocraft.client.KonoLayers;
import dev.konocraft.entity.CompanionEntity;
import dev.konocraft.entity.KonoNpcEntity;
import dev.konocraft.entity.TownsfolkEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.Set;

/** Renders story characters and townsfolk with their own skins and story poses. */
public class CharacterRenderer<T extends KonoNpcEntity> extends BipedEntityRenderer<T, CharacterModel<T>> {
	public static final Set<String> SLIM = Set.of("aqua", "darkness", "luna", "wiz");
	private final String key;
	private final float scale;

	public CharacterRenderer(EntityRendererFactory.Context ctx, String key, float scale) {
		super(ctx, new CharacterModel<>(ctx.getPart(SLIM.contains(key) ? KonoLayers.CHARACTER_SLIM : KonoLayers.CHARACTER_WIDE), SLIM.contains(key)), 0.5f);
		this.key = key;
		this.scale = scale;
		if (key.equals("megumin")) addFeature(new HatFeatureRenderer<>(this, ctx.getModelLoader()));
	}

	@Override
	public Identifier getTexture(T entity) {
		if (entity instanceof TownsfolkEntity t) return KonoCraft.id("textures/entity/character/townsfolk_" + t.getVariant() + ".png");
		return KonoCraft.id("textures/entity/character/" + key + ".png");
	}

	private boolean ghost(T entity) {
		return entity.isGhost();
	}

	@Override
	public void render(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertices, int light) {
		model.alpha = ghost(entity) ? 0.55f : 1f;
		super.render(entity, yaw, tickDelta, matrices, vertices, light);
	}

	@Override
	protected RenderLayer getRenderLayer(T entity, boolean showBody, boolean translucent, boolean showOutline) {
		if (ghost(entity)) return RenderLayer.getEntityTranslucent(getTexture(entity));
		return super.getRenderLayer(entity, showBody, translucent, showOutline);
	}

	@Override
	protected void scale(T entity, MatrixStack matrices, float amount) {
		matrices.scale(0.9375f * scale, 0.9375f * scale, 0.9375f * scale);
	}

	@Override
	protected void setupTransforms(T entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta) {
		super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta);
		if (entity instanceof CompanionEntity c) {
			int s = c.getCompanionState();
			if (s == CompanionEntity.STATE_COLLAPSED || s == CompanionEntity.STATE_FAINTED) {
				// face-down on the ground, exactly how Megumin likes to spend her afternoons
				matrices.translate(0, 0.25, 0);
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
				matrices.translate(0, -0.9, 0);
			} else if (s == CompanionEntity.STATE_CARRIED) {
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15));
			}
		}
		if (ghost(entity)) {
			matrices.translate(0, 0.15 + Math.sin((entity.age + tickDelta) * 0.08) * 0.08, 0);
		}
	}
}
