package dev.konocraft.client.render;

import dev.konocraft.KonoCraft;
import dev.konocraft.client.KonoLayers;
import dev.konocraft.client.model.KonoModels;
import dev.konocraft.entity.BarrierCrystalEntity;
import dev.konocraft.entity.BeldiaEntity;
import dev.konocraft.entity.DestroyerCoreEntity;
import dev.konocraft.entity.DestroyerEntity;
import dev.konocraft.entity.DestroyerGolemEntity;
import dev.konocraft.entity.FlyingCabbageEntity;
import dev.konocraft.entity.GiantToadEntity;
import dev.konocraft.entity.KonoMonsterEntity;
import dev.konocraft.registry.ModEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Registers every entity renderer of the mod. */
public final class KonoRenderers {
	private KonoRenderers() {
	}

	public static void register() {
		for (String key : new String[]{"eris", "kazuma", "luna", "wiz", "yunyun"}) {
			EntityRendererRegistry.register(ModEntities.byCharacter(key), ctx -> new CharacterRenderer<>(ctx, key, 1.0f));
		}
		EntityRendererRegistry.register(ModEntities.VANIR, ctx -> new CharacterRenderer<>(ctx, "vanir", 1.1f));
		EntityRendererRegistry.register(ModEntities.MEGUMIN, ctx -> new CharacterRenderer<>(ctx, "megumin", 0.92f));
		EntityRendererRegistry.register(ModEntities.AQUA, ctx -> new CharacterRenderer<>(ctx, "aqua", 1.0f));
		EntityRendererRegistry.register(ModEntities.DARKNESS, ctx -> new CharacterRenderer<>(ctx, "darkness", 1.03f));
		EntityRendererRegistry.register(ModEntities.TOWNSFOLK, ctx -> new CharacterRenderer<>(ctx, "townsfolk", 1.0f));
		EntityRendererRegistry.register(ModEntities.GOBLIN, ctx -> new HumanoidMonsterRenderer<>(ctx, "goblin", 0.7f));
		EntityRendererRegistry.register(ModEntities.UNDEAD_KNIGHT, ctx -> new HumanoidMonsterRenderer<>(ctx, "undead_knight", 1.05f));
		EntityRendererRegistry.register(ModEntities.BELDIA, BeldiaRenderer::new);
		EntityRendererRegistry.register(ModEntities.GIANT_TOAD, ctx -> new ToadRenderer(ctx, 1.0f, "giant_toad"));
		EntityRendererRegistry.register(ModEntities.TOAD_KING, ctx -> new ToadRenderer(ctx, 2.2f, "toad_king"));
		EntityRendererRegistry.register(ModEntities.FLYING_CABBAGE, CabbageRenderer::new);
		EntityRendererRegistry.register(ModEntities.DESTROYER_GOLEM, GolemRenderer::new);
		EntityRendererRegistry.register(ModEntities.DESTROYER, DestroyerRenderer::new);
		EntityRendererRegistry.register(ModEntities.BARRIER_CRYSTAL, CrystalRenderer::new);
		EntityRendererRegistry.register(ModEntities.DESTROYER_CORE, CoreRenderer::new);
		EntityRendererRegistry.register(ModEntities.MAGIC_BOLT, ctx -> new FlyingItemEntityRenderer<>(ctx, 1.4f, true));
	}

	// ---------------------------------------------------------------------------------------------

	public static class HumanoidMonsterRenderer<T extends KonoMonsterEntity> extends BipedEntityRenderer<T, CharacterModel<T>> {
		private final Identifier texture;
		private final float scale;

		public HumanoidMonsterRenderer(EntityRendererFactory.Context ctx, String name, float scale) {
			super(ctx, new CharacterModel<>(ctx.getPart(KonoLayers.CHARACTER_WIDE), false), 0.5f * scale);
			this.texture = KonoCraft.id("textures/entity/character/" + name + ".png");
			this.scale = scale;
		}

		@Override
		public Identifier getTexture(T entity) {
			return texture;
		}

		@Override
		protected void scale(T entity, MatrixStack matrices, float amount) {
			matrices.scale(0.9375f * scale, 0.9375f * scale, 0.9375f * scale);
		}
	}

	/** Beldia, carrying his own head under his arm (or tossing it into the air). */
	public static class BeldiaRenderer extends BipedEntityRenderer<BeldiaEntity, CharacterModel<BeldiaEntity>> {
		private static final Identifier TEXTURE = KonoCraft.id("textures/entity/character/beldia.png");

		public BeldiaRenderer(EntityRendererFactory.Context ctx) {
			super(ctx, new CharacterModel<>(ctx.getPart(KonoLayers.CHARACTER_WIDE), false), 0.7f);
		}

		@Override
		public Identifier getTexture(BeldiaEntity entity) {
			return TEXTURE;
		}

		@Override
		protected void scale(BeldiaEntity entity, MatrixStack matrices, float amount) {
			matrices.scale(1.3f, 1.3f, 1.3f);
		}

		@Override
		public void render(BeldiaEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertices, int light) {
			model.head.visible = false;
			model.hat.visible = false;
			super.render(entity, yaw, tickDelta, matrices, vertices, light);
			if (entity.isHeadless()) return;
			// draw the head separately
			matrices.push();
			float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - bodyYaw));
			matrices.scale(-1.3f, -1.3f, 1.3f);
			if (entity.isHeadUp()) {
				float t = entity.age + tickDelta;
				matrices.translate(0, -3.4 - Math.sin(t * 0.15) * 0.15, 0);
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 6));
			} else {
				// resting in the crook of the left arm
				matrices.translate(0.42, -0.86, -0.36);
			}
			model.head.visible = true;
			model.hat.visible = true;
			model.head.setPivot(0, 0, 0);
			model.head.pitch = 0;
			model.head.yaw = 0;
			model.head.roll = 0;
			model.hat.copyTransform(model.head);
			VertexConsumer vc = vertices.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
			model.head.render(matrices, vc, light, OverlayTexture.DEFAULT_UV);
			model.hat.render(matrices, vc, light, OverlayTexture.DEFAULT_UV);
			matrices.pop();
		}
	}

	public static class ToadRenderer extends MobEntityRenderer<GiantToadEntity, KonoModels.Toad> {
		private final float scale;
		private final Identifier texture;

		public ToadRenderer(EntityRendererFactory.Context ctx, float scale, String texture) {
			super(ctx, new KonoModels.Toad(ctx.getPart(KonoLayers.GIANT_TOAD)), 1.1f * scale);
			this.scale = scale;
			this.texture = KonoCraft.id("textures/entity/" + texture + ".png");
		}

		@Override
		public Identifier getTexture(GiantToadEntity entity) {
			return texture;
		}

		@Override
		protected void scale(GiantToadEntity entity, MatrixStack matrices, float amount) {
			matrices.scale(scale, scale, scale);
		}
	}

	public static class CabbageRenderer extends MobEntityRenderer<FlyingCabbageEntity, KonoModels.Cabbage> {
		private static final Identifier TEXTURE = KonoCraft.id("textures/entity/flying_cabbage.png");

		public CabbageRenderer(EntityRendererFactory.Context ctx) {
			super(ctx, new KonoModels.Cabbage(ctx.getPart(KonoLayers.FLYING_CABBAGE)), 0.3f);
		}

		@Override
		public Identifier getTexture(FlyingCabbageEntity entity) {
			return TEXTURE;
		}
	}

	public static class GolemRenderer extends MobEntityRenderer<DestroyerGolemEntity, KonoModels.Golem> {
		private static final Identifier TEXTURE = KonoCraft.id("textures/entity/destroyer_golem.png");

		public GolemRenderer(EntityRendererFactory.Context ctx) {
			super(ctx, new KonoModels.Golem(ctx.getPart(KonoLayers.DESTROYER_GOLEM)), 0.8f);
		}

		@Override
		public Identifier getTexture(DestroyerGolemEntity entity) {
			return TEXTURE;
		}
	}

	public static class CrystalRenderer extends MobEntityRenderer<BarrierCrystalEntity, KonoModels.Crystal> {
		private static final Identifier TEXTURE = KonoCraft.id("textures/entity/barrier_crystal.png");

		public CrystalRenderer(EntityRendererFactory.Context ctx) {
			super(ctx, new KonoModels.Crystal(ctx.getPart(KonoLayers.BARRIER_CRYSTAL)), 0.6f);
		}

		@Override
		public Identifier getTexture(BarrierCrystalEntity entity) {
			return TEXTURE;
		}

		@Override
		protected RenderLayer getRenderLayer(BarrierCrystalEntity entity, boolean showBody, boolean translucent, boolean showOutline) {
			return RenderLayer.getEntityTranslucentEmissive(TEXTURE);
		}

		@Override
		protected int getBlockLight(BarrierCrystalEntity entity, net.minecraft.util.math.BlockPos pos) {
			return 15;
		}
	}

	public static class CoreRenderer extends MobEntityRenderer<DestroyerCoreEntity, KonoModels.Core> {
		private static final Identifier TEXTURE = KonoCraft.id("textures/entity/destroyer_core.png");

		public CoreRenderer(EntityRendererFactory.Context ctx) {
			super(ctx, new KonoModels.Core(ctx.getPart(KonoLayers.DESTROYER_CORE)), 0.8f);
		}

		@Override
		public Identifier getTexture(DestroyerCoreEntity entity) {
			return TEXTURE;
		}

		@Override
		protected int getBlockLight(DestroyerCoreEntity entity, net.minecraft.util.math.BlockPos pos) {
			return 15;
		}
	}

	/** The Mobile Fortress Destroyer: a 6x scaled model plus its magic barrier dome. */
	public static class DestroyerRenderer extends MobEntityRenderer<DestroyerEntity, KonoModels.Destroyer> {
		private static final Identifier TEXTURE = KonoCraft.id("textures/entity/destroyer.png");
		private static final Identifier BARRIER = KonoCraft.id("textures/entity/barrier.png");
		public static final float SCALE = 6f;

		public DestroyerRenderer(EntityRendererFactory.Context ctx) {
			super(ctx, new KonoModels.Destroyer(ctx.getPart(KonoLayers.DESTROYER)), 7f);
		}

		@Override
		public Identifier getTexture(DestroyerEntity entity) {
			return TEXTURE;
		}

		@Override
		protected void scale(DestroyerEntity entity, MatrixStack matrices, float amount) {
			matrices.scale(SCALE, SCALE, SCALE);
		}

		@Override
		protected void setupTransforms(DestroyerEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta) {
			super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta);
			// sink onto its belly after the legs are blown off (11 model pixels)
			matrices.translate(0, -entity.getSink() * 11f / 16f * SCALE, 0);
			if (entity.getPhase() == DestroyerEntity.WALKING) {
				matrices.translate(0, Math.abs(Math.sin((entity.age + tickDelta) * 0.18)) * 0.25, 0);
			}
		}

		@Override
		protected float getLyingAngle(DestroyerEntity entity) {
			return 0;
		}

		@Override
		public void render(DestroyerEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertices, int light) {
			super.render(entity, yaw, tickDelta, matrices, vertices, light);
			if (entity.getPhase() == DestroyerEntity.WALKING && entity.getCrystals() > 0) {
				renderBarrier(entity, tickDelta, matrices, vertices);
			}
		}

		private void renderBarrier(DestroyerEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertices) {
			matrices.push();
			matrices.translate(0, 5.5, 0);
			float t = entity.age + tickDelta;
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 0.5f));
			float r = 13f;
			VertexConsumer vc = vertices.getBuffer(RenderLayer.getEntityTranslucentEmissive(BARRIER));
			Matrix4f pos = matrices.peek().getPositionMatrix();
			Matrix3f normal = matrices.peek().getNormalMatrix();
			float alpha = 0.18f + 0.06f * MathHelper.sin(t * 0.1f) + 0.05f * (4 - entity.getCrystals());
			int lat = 12, lon = 24;
			for (int i = 0; i < lat; i++) {
				double t0 = Math.PI * i / lat, t1 = Math.PI * (i + 1) / lat;
				for (int j = 0; j < lon; j++) {
					double p0 = 2 * Math.PI * j / lon, p1 = 2 * Math.PI * (j + 1) / lon;
					vertex(vc, pos, normal, r, t0, p0, alpha, j / (float) lon, i / (float) lat);
					vertex(vc, pos, normal, r, t1, p0, alpha, j / (float) lon, (i + 1) / (float) lat);
					vertex(vc, pos, normal, r, t1, p1, alpha, (j + 1) / (float) lon, (i + 1) / (float) lat);
					vertex(vc, pos, normal, r, t0, p1, alpha, (j + 1) / (float) lon, i / (float) lat);
				}
			}
			matrices.pop();
		}

		private static void vertex(VertexConsumer vc, Matrix4f pos, Matrix3f normal, float r, double theta, double phi, float alpha, float u, float v) {
			float x = (float) (Math.sin(theta) * Math.cos(phi));
			float y = (float) Math.cos(theta);
			float z = (float) (Math.sin(theta) * Math.sin(phi));
			vc.vertex(pos, x * r, y * r * 0.75f, z * r).color(0.75f, 0.45f, 1f, alpha).texture(u * 4, v * 2)
				.overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(normal, x, y, z).next();
		}
	}
}
