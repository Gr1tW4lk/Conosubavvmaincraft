package dev.konocraft.client.render;

import dev.konocraft.entity.BeldiaEntity;
import dev.konocraft.entity.CompanionEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

/** Player-shaped model for all humanoid characters, with a few story poses. */
public class CharacterModel<T extends LivingEntity> extends PlayerEntityModel<T> {
	public float alpha = 1f;

	public CharacterModel(ModelPart root, boolean slim) {
		super(root, slim);
	}

	@Override
	public void setAngles(T entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		super.setAngles(entity, limbAngle, limbDistance, age, headYaw, headPitch);
		if (entity instanceof CompanionEntity c) {
			switch (c.getCompanionState()) {
				case CompanionEntity.STATE_CASTING -> {
					rightArm.pitch = -2.7f + MathHelper.sin(age * 0.5f) * 0.08f;
					rightArm.yaw = -0.2f;
					leftArm.pitch = -2.3f + MathHelper.cos(age * 0.5f) * 0.08f;
					leftArm.yaw = 0.3f;
				}
				case CompanionEntity.STATE_CARRIED -> {
					rightArm.pitch = -1.4f;
					leftArm.pitch = -1.4f;
					rightArm.yaw = 0.4f;
					leftArm.yaw = -0.4f;
					rightLeg.pitch = -1.2f;
					leftLeg.pitch = -1.2f;
					rightLeg.yaw = 0.3f;
					leftLeg.yaw = -0.3f;
					head.pitch = 0.3f;
				}
				case CompanionEntity.STATE_COLLAPSED, CompanionEntity.STATE_FAINTED -> {
					rightArm.pitch = -2.9f;
					leftArm.pitch = -2.9f;
					rightArm.roll = 0.2f;
					leftArm.roll = -0.2f;
					rightLeg.pitch = 0;
					leftLeg.pitch = 0;
				}
				default -> {
				}
			}
		}
		if (entity instanceof BeldiaEntity b) {
			// Beldia carries his head in his left arm
			leftArm.pitch = -1.1f + MathHelper.sin(age * 0.08f) * 0.05f;
			leftArm.yaw = 0.25f;
			if (b.isHeadless()) {
				rightArm.pitch = -1.6f + MathHelper.sin(age * 0.6f) * 0.6f;
				leftArm.pitch = -1.6f + MathHelper.cos(age * 0.6f) * 0.6f;
			}
		}
		copyOverlays();
	}

	private void copyOverlays() {
		hat.copyTransform(head);
		jacket.copyTransform(body);
		rightSleeve.copyTransform(rightArm);
		leftSleeve.copyTransform(leftArm);
		rightPants.copyTransform(rightLeg);
		leftPants.copyTransform(leftLeg);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float a) {
		super.render(matrices, vertices, light, overlay, red, green, blue, a * alpha);
	}
}
