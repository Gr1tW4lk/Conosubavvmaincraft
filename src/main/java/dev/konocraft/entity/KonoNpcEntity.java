package dev.konocraft.entity;

import dev.konocraft.story.Speakers;
import dev.konocraft.story.Story;
import dev.konocraft.story.StoryState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.UUID;

/** A named story character (Luna, Wiz, Eris...). Talks on right click and stays near its spot. */
public class KonoNpcEntity extends PathAwareEntity {
	private static final TrackedData<Boolean> GHOST = DataTracker.registerData(KonoNpcEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	protected Vec3d home;
	protected float homeYaw;
	private int validateTimer;

	public KonoNpcEntity(EntityType<? extends KonoNpcEntity> type, World world) {
		super(type, world);
		setPersistent();
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 40)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3);
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(GHOST, false);
	}

	/** Kazuma spends most of the story as a ghost in the afterlife (again). */
	public boolean isGhost() {
		return dataTracker.get(GHOST);
	}

	public String characterKey() {
		return Registries.ENTITY_TYPE.getId(getType()).getPath();
	}

	/** Story characters are unique and tracked by the story; townsfolk are not. */
	protected boolean isUnique() {
		return true;
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(6, new ReturnHomeGoal(this));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
		goalSelector.add(8, new LookAroundGoal(this));
	}

	public void setHome(double x, double y, double z, float yaw) {
		home = new Vec3d(x, y, z);
		homeYaw = yaw;
	}

	public Vec3d getHome() {
		return home;
	}

	/** Whether the character should walk back to its spot. Companions in the party override this. */
	public boolean staysHome() {
		return true;
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
		if (!getWorld().isClient && player instanceof ServerPlayerEntity sp) {
			getLookControl().lookAt(player, 30f, 30f);
			Story.talk(sp, this);
		}
		return ActionResult.success(getWorld().isClient);
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source.getAttacker() instanceof PlayerEntity) {
			if (!getWorld().isClient && random.nextInt(3) == 0) {
				say(hurtLine());
			}
			return false;
		}
		if (isInvulnerableNpc()) return false;
		return super.damage(source, amount);
	}

	protected boolean isInvulnerableNpc() {
		return true;
	}

	protected String hurtLine() {
		return "Эй! Не надо так!";
	}

	/** Shows a short chat line from this character to players nearby. */
	public void say(String text) {
		if (getWorld().isClient) return;
		Speakers.Speaker sp = Speakers.get(characterKey());
		Text msg = Text.literal("[" + sp.name() + "] ").styled(s -> s.withColor(sp.color())).append(Text.literal(text).styled(s -> s.withColor(0xFFFFFF)));
		for (PlayerEntity p : getWorld().getPlayers()) {
			if (p.squaredDistanceTo(this) < 28 * 28) p.sendMessage(msg, false);
		}
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public boolean cannotDespawn() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return !staysHome();
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) return;
		if (!hasCustomName()) {
			Speakers.Speaker sp = Speakers.get(characterKey());
			if (!sp.name().isEmpty() && isUnique()) {
				setCustomName(Text.literal(sp.name()).styled(s -> s.withColor(sp.color())));
				setCustomNameVisible(true);
			}
		}
		if (isUnique() && ++validateTimer % 40 == 0 && getServer() != null && !getCommandTags().contains("konotest")) {
			StoryState state = StoryState.get(getServer());
			if (characterKey().equals("kazuma")) dataTracker.set(GHOST, !state.hasFlag("kazuma_alive"));
			UUID tracked = state.npcs.get(characterKey());
			if (tracked == null) {
				state.npcs.put(characterKey(), getUuid());
				state.markDirty();
			} else if (!tracked.equals(getUuid())) {
				discard();
			}
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (home != null) {
			nbt.putDouble("HomeX", home.x);
			nbt.putDouble("HomeY", home.y);
			nbt.putDouble("HomeZ", home.z);
			nbt.putFloat("HomeYaw", homeYaw);
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("HomeX")) {
			home = new Vec3d(nbt.getDouble("HomeX"), nbt.getDouble("HomeY"), nbt.getDouble("HomeZ"));
			homeYaw = nbt.getFloat("HomeYaw");
		}
	}

	/** Walks back to the home spot and turns to face its default direction. */
	public static class ReturnHomeGoal extends Goal {
		private final KonoNpcEntity npc;
		private final double maxDistance;

		public ReturnHomeGoal(KonoNpcEntity npc) {
			this(npc, 2.5);
		}

		public ReturnHomeGoal(KonoNpcEntity npc, double maxDistance) {
			this.npc = npc;
			this.maxDistance = maxDistance;
			setControls(EnumSet.of(Control.MOVE));
		}

		@Override
		public boolean canStart() {
			return npc.home != null && npc.staysHome() && npc.squaredDistanceTo(npc.home) > maxDistance * maxDistance;
		}

		@Override
		public void start() {
			npc.getNavigation().startMovingTo(npc.home.x, npc.home.y, npc.home.z, 0.8);
		}

		@Override
		public boolean shouldContinue() {
			return npc.staysHome() && !npc.getNavigation().isIdle() && npc.squaredDistanceTo(npc.home) > 1.0;
		}

		@Override
		public void stop() {
			npc.getNavigation().stop();
			if (npc.squaredDistanceTo(npc.home) > 30 * 30) {
				npc.requestTeleport(npc.home.x, npc.home.y, npc.home.z);
			}
			if (npc.squaredDistanceTo(npc.home) < 4) {
				npc.setYaw(npc.homeYaw);
				npc.setBodyYaw(npc.homeYaw);
				npc.setHeadYaw(npc.homeYaw);
			}
		}
	}
}
