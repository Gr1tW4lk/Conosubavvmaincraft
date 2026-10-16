package dev.konocraft.client.model;

import dev.konocraft.entity.BarrierCrystalEntity;
import dev.konocraft.entity.DestroyerCoreEntity;
import dev.konocraft.entity.DestroyerEntity;
import dev.konocraft.entity.DestroyerGolemEntity;
import dev.konocraft.entity.FlyingCabbageEntity;
import dev.konocraft.entity.GiantToadEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** Animated entity models backed by {@link GeneratedModels}. */
public final class KonoModels {
	private KonoModels() {
	}

	/** Base class: the whole model tree hangs off one root part. */
	public abstract static class Root<T extends Entity> extends SinglePartEntityModel<T> {
		protected final ModelPart root;

		protected Root(ModelPart root) {
			this.root = root;
		}

		@Override
		public ModelPart getPart() {
			return root;
		}
	}

	public static class Toad extends Root<GiantToadEntity> {
		private final ModelPart body, head, jaw, eyeL, eyeR, legFL, legFR, legBL, legBR;

		public Toad(ModelPart root) {
			super(root);
			body = root.getChild("body");
			head = root.getChild("head");
			jaw = head.getChild("jaw");
			eyeL = head.getChild("eye_left");
			eyeR = head.getChild("eye_right");
			legFL = root.getChild("leg_front_left");
			legFR = root.getChild("leg_front_right");
			legBL = root.getChild("leg_back_left");
			legBR = root.getChild("leg_back_right");
		}

		@Override
		public void setAngles(GiantToadEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
			head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.4f;
			head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE * 0.3f;
			float breathe = MathHelper.sin(age * 0.1f) * 0.04f;
			body.pitch = -0.18f + breathe;
			float swing = handSwingProgress;
			jaw.pitch = Math.max(0, MathHelper.sin(swing * MathHelper.PI)) * 0.9f + Math.max(0, MathHelper.sin(age * 0.05f) - 0.97f) * 8f;
			float blink = (age % 90) > 86 ? 0.2f : 1f;
			eyeL.yScale = blink;
			eyeR.yScale = blink;
			float hop = MathHelper.cos(limbAngle * 0.6f) * 1.2f * limbDistance;
			legFL.pitch = hop;
			legFR.pitch = hop;
			legBL.pitch = -hop * 0.8f;
			legBR.pitch = -hop * 0.8f;
		}
	}

	public static class Cabbage extends Root<FlyingCabbageEntity> {
		private final ModelPart head, wingL, wingR;

		public Cabbage(ModelPart root) {
			super(root);
			head = root.getChild("head");
			wingL = head.getChild("wing_left");
			wingR = head.getChild("wing_right");
		}

		@Override
		public void setAngles(FlyingCabbageEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
			float flap = MathHelper.sin(age * 1.6f) * 0.7f;
			wingL.roll = 0.3f + flap;
			wingR.roll = -0.3f - flap;
			head.pivotY = 18 + MathHelper.sin(age * 0.2f) * 1.5f;
			head.roll = MathHelper.sin(age * 0.15f) * 0.15f;
		}
	}

	public static class Golem extends Root<DestroyerGolemEntity> {
		private final ModelPart head, rightArm, leftArm, rightLeg, leftLeg;

		public Golem(ModelPart root) {
			super(root);
			head = root.getChild("head");
			rightArm = root.getChild("right_arm");
			leftArm = root.getChild("left_arm");
			rightLeg = root.getChild("right_leg");
			leftLeg = root.getChild("left_leg");
		}

		@Override
		public void setAngles(DestroyerGolemEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
			head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
			head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
			rightLeg.pitch = -1.5f * MathHelper.wrap(limbAngle, 13f) * limbDistance;
			leftLeg.pitch = 1.5f * MathHelper.wrap(limbAngle, 13f) * limbDistance;
			float attack = handSwingProgress;
			if (attack > 0) {
				float a = MathHelper.sin(attack * MathHelper.PI);
				rightArm.pitch = -2.0f * a;
				leftArm.pitch = -2.0f * a;
			} else {
				rightArm.pitch = (-0.2f + 1.5f * MathHelper.wrap(limbAngle, 13f)) * limbDistance;
				leftArm.pitch = (-0.2f - 1.5f * MathHelper.wrap(limbAngle, 13f)) * limbDistance;
			}
		}
	}

	public static class Destroyer extends Root<DestroyerEntity> {
		private final ModelPart[] femurs = new ModelPart[8];
		private final ModelPart[] shins = new ModelPart[8];

		public Destroyer(ModelPart root) {
			super(root);
			for (int i = 0; i < 8; i++) {
				String name = "leg_" + (i < 4 ? "l" : "r") + (i % 4);
				femurs[i] = root.getChild(name);
				shins[i] = femurs[i].getChild(name + "_lower");
			}
		}

		@Override
		public void setAngles(DestroyerEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
			float sink = e.getSink();
			boolean walking = e.getPhase() == DestroyerEntity.WALKING;
			for (int i = 0; i < 8; i++) {
				float side = i < 4 ? -1 : 1;
				float phase = (i % 2 == 0 ? 0 : MathHelper.PI) + (i < 4 ? 0 : MathHelper.PI);
				float step = walking ? MathHelper.sin(age * 0.18f + phase) : 0;
				float lift = walking ? Math.max(0, MathHelper.cos(age * 0.18f + phase)) : 0;
				femurs[i].yaw = femurs[i].getDefaultTransform().yaw + step * 0.22f * side;
				femurs[i].roll = side * (-0.75f - lift * 0.25f) * (1 - sink) + side * 0.15f * sink;
				shins[i].roll = side * (0.55f + lift * 0.15f) * (1 - sink) + side * -1.1f * sink;
				femurs[i].pivotY = 8;
			}
		}
	}

	public static class Crystal extends Root<BarrierCrystalEntity> {
		private final ModelPart gem;

		public Crystal(ModelPart root) {
			super(root);
			gem = root.getChild("gem");
		}

		@Override
		public void setAngles(BarrierCrystalEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
			gem.yaw = 0.785f + age * 0.05f;
			gem.pivotY = 10 + MathHelper.sin(age * 0.12f) * 1.2f;
		}
	}

	public static class Core extends Root<DestroyerCoreEntity> {
		private final ModelPart outer, inner, ring;

		public Core(ModelPart root) {
			super(root);
			outer = root.getChild("outer");
			inner = outer.getChild("inner");
			ring = root.getChild("ring");
		}

		@Override
		public void setAngles(DestroyerCoreEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
			outer.yaw = age * 0.03f;
			inner.yaw = 0.785f - age * 0.09f;
			inner.pitch = 0.785f + age * 0.05f;
			float pulse = 1 + MathHelper.sin(age * 0.3f) * 0.04f;
			outer.xScale = pulse;
			outer.yScale = pulse;
			outer.zScale = pulse;
			ring.yaw = -age * 0.02f;
		}
	}
}
