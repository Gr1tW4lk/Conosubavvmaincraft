package dev.konocraft.entity;

import dev.konocraft.story.Party;
import dev.konocraft.story.StoryState;
import dev.konocraft.world.KonoWorld;
import dev.konocraft.world.Regions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/** A party member: follows the leader, fights in her own special (useless) way, never truly dies. */
public abstract class CompanionEntity extends KonoNpcEntity {
	public static final int STATE_NORMAL = 0;
	public static final int STATE_COLLAPSED = 1;
	public static final int STATE_FAINTED = 2;
	public static final int STATE_CASTING = 3;
	public static final int STATE_CARRIED = 4;

	private static final TrackedData<Boolean> FOLLOWING = DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Integer> STATE = DataTracker.registerData(CompanionEntity.class, TrackedDataHandlerRegistry.INTEGER);

	protected int stateTicks;
	private UUID carrier;
	protected Vec3d castTarget;
	private int lineCooldown;
	private boolean swallowed;

	protected CompanionEntity(EntityType<? extends CompanionEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder createCompanionAttributes() {
		return MobEntity.createMobAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 40)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4)
			.add(EntityAttributes.GENERIC_ARMOR, 4);
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(FOLLOWING, false);
		dataTracker.startTracking(STATE, STATE_NORMAL);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new StayDownGoal(this));
		goalSelector.add(3, new FollowLeaderGoal(this));
		goalSelector.add(6, new ReturnHomeGoal(this));
		goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
		goalSelector.add(8, new LookAroundGoal(this));
		addCombatGoals();
	}

	protected abstract void addCombatGoals();

	public boolean isFollowing() {
		return dataTracker.get(FOLLOWING);
	}

	public void setFollowing(boolean following) {
		dataTracker.set(FOLLOWING, following);
	}

	public int getCompanionState() {
		return dataTracker.get(STATE);
	}

	protected void setCompanionState(int state, int ticks) {
		dataTracker.set(STATE, state);
		stateTicks = ticks;
	}

	public boolean isDown() {
		int s = getCompanionState();
		return s == STATE_COLLAPSED || s == STATE_FAINTED || s == STATE_CARRIED;
	}

	@Override
	public boolean staysHome() {
		return !isFollowing();
	}

	@Override
	public boolean isPushable() {
		return isFollowing() && !isDown();
	}

	public ServerPlayerEntity leader() {
		if (getServer() == null || !isFollowing()) return null;
		return Party.leader(getServer());
	}

	// ------------------------------------------------------------------------------------------
	// States

	public void collapse(int ticks) {
		setCompanionState(STATE_COLLAPSED, ticks);
		getNavigation().stop();
		setTarget(null);
	}

	public void faint() {
		setCompanionState(STATE_FAINTED, 400);
		getNavigation().stop();
		setTarget(null);
		setHealth(1);
		say(faintLine());
	}

	public void recover() {
		setCompanionState(STATE_NORMAL, 0);
		carrier = null;
		setNoGravity(false);
		noClip = false;
		if (getHealth() < getMaxHealth() * 0.5f) setHealth(getMaxHealth() * 0.5f);
	}

	public void castPose(int ticks, Vec3d target) {
		castTarget = target;
		setCompanionState(STATE_CASTING, ticks);
		getNavigation().stop();
	}

	public void carryBy(ServerPlayerEntity player) {
		carrier = player.getUuid();
		setCompanionState(STATE_CARRIED, 20 * 120);
		setNoGravity(true);
		noClip = true;
	}

	public boolean isSwallowed() {
		return swallowed;
	}

	public void setSwallowed(boolean swallowed) {
		this.swallowed = swallowed;
		setInvisible(swallowed);
		if (swallowed) {
			getNavigation().stop();
			say(swallowLine());
		}
	}

	protected abstract String faintLine();

	protected abstract String swallowLine();

	protected String[] idleLines() {
		return new String[0];
	}

	// ------------------------------------------------------------------------------------------

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (getWorld().isClient) return false;
		if (source.getAttacker() instanceof PlayerEntity || source.getAttacker() instanceof CompanionEntity) return false;
		if (getCompanionState() == STATE_FAINTED || getCompanionState() == STATE_CARRIED || swallowed) return false;
		if (amount >= getHealth()) {
			faint();
			return true;
		}
		boolean hurt = super.damage(source, amount);
		if (hurt && lineCooldown <= 0) {
			onHurtReaction(source);
			lineCooldown = 100;
		}
		return hurt;
	}

	@Override
	protected boolean isInvulnerableNpc() {
		return false;
	}

	protected void onHurtReaction(DamageSource source) {
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) return;
		if (lineCooldown > 0) lineCooldown--;
		int state = getCompanionState();
		if (state != STATE_NORMAL && stateTicks > 0 && --stateTicks == 0) {
			recover();
			if (state == STATE_FAINTED) say("Я... я в порядке! Почти.");
		}
		if (state == STATE_CARRIED) tickCarried();
		if (state == STATE_FAINTED && age % 10 == 0) {
			((ServerWorld) getWorld()).spawnParticles(ParticleTypes.CRIT, getX(), getY() + 0.6, getZ(), 2, 0.2, 0.1, 0.2, 0);
		}
		if (state == STATE_NORMAL && isFollowing() && age % 20 == 0) {
			ServerPlayerEntity leader = leader();
			if (leader != null && KonoWorld.is(leader.getWorld()) && leader.getWorld() == getWorld()
				&& !Regions.inAfterlife((int) leader.getX(), (int) leader.getZ()) && squaredDistanceTo(leader) > 40 * 40) {
				teleportNear(leader);
			}
			if (age % 600 == 0 && leader != null && random.nextInt(3) == 0) {
				String[] lines = idleLines();
				if (lines.length > 0) say(lines[random.nextInt(lines.length)]);
			}
		}
		if (state == STATE_NORMAL && !swallowed) companionTick();
	}

	protected void companionTick() {
	}

	private void tickCarried() {
		if (carrier == null || getServer() == null) {
			recover();
			return;
		}
		ServerPlayerEntity p = getServer().getPlayerManager().getPlayer(carrier);
		if (p == null || !p.isAlive() || p.getWorld() != getWorld()) {
			recover();
			return;
		}
		if (Regions.inTown((int) p.getX(), (int) p.getZ()) && stateTicks < 20 * 115) {
			recover();
			say("Мы дома! Спасибо, что донёс... то есть, так и должно быть. Я же великий маг.");
			requestTeleport(p.getX() + 1, p.getY(), p.getZ() + 1);
			return;
		}
		float yaw = p.getBodyYaw() * MathHelper.RADIANS_PER_DEGREE;
		double bx = p.getX() + MathHelper.sin(yaw) * 0.38;
		double bz = p.getZ() - MathHelper.cos(yaw) * 0.38;
		refreshPositionAndAngles(bx, p.getY() + 0.55, bz, p.getBodyYaw(), 0);
		setBodyYaw(p.getBodyYaw());
		setHeadYaw(p.getHeadYaw());
		setVelocity(Vec3d.ZERO);
	}

	public void teleportNear(LivingEntity target) {
		for (int i = 0; i < 10; i++) {
			double x = target.getX() + random.nextInt(5) - 2;
			double z = target.getZ() + random.nextInt(5) - 2;
			if (getWorld().isSpaceEmpty(this, getBoundingBox().offset(x - getX(), target.getY() - getY(), z - getZ()))) {
				requestTeleport(x, target.getY(), z);
				getNavigation().stop();
				return;
			}
		}
		requestTeleport(target.getX(), target.getY(), target.getZ());
	}

	/** Nearby monsters that threaten the party. */
	protected List<MobEntity> threats(double radius) {
		ServerPlayerEntity leader = leader();
		Entity center = leader != null ? leader : this;
		return getWorld().getEntitiesByClass(MobEntity.class, center.getBoundingBox().expand(radius), CompanionEntity::isEnemy);
	}

	public static boolean isEnemy(Entity e) {
		return e.isAlive() && (e instanceof Monster || e instanceof GiantToadEntity || e instanceof FlyingCabbageEntity
			|| e instanceof BeldiaEntity b && !b.isVisiting() || e instanceof DestroyerGolemEntity || e instanceof BarrierCrystalEntity);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Following", isFollowing());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		setFollowing(nbt.getBoolean("Following"));
		// never stay invisible inside a toad that no longer exists
		swallowed = false;
		setInvisible(false);
	}

	// ------------------------------------------------------------------------------------------

	/** Keeps the companion still while she is collapsed, fainted, casting or carried. */
	static class StayDownGoal extends Goal {
		private final CompanionEntity mob;

		StayDownGoal(CompanionEntity mob) {
			this.mob = mob;
			setControls(EnumSet.of(Control.MOVE, Control.JUMP, Control.LOOK));
		}

		@Override
		public boolean canStart() {
			return mob.getCompanionState() != STATE_NORMAL || mob.isSwallowed();
		}

		@Override
		public void start() {
			mob.getNavigation().stop();
		}

		@Override
		public void tick() {
			if (mob.getCompanionState() == STATE_CASTING && mob.castTarget != null) {
				mob.getLookControl().lookAt(mob.castTarget.x, mob.castTarget.y + 1, mob.castTarget.z);
			}
		}
	}

	/** Follows the party leader like a loyal (if questionable) adventurer. */
	static class FollowLeaderGoal extends Goal {
		private final CompanionEntity mob;
		private ServerPlayerEntity leader;
		private int recalc;

		FollowLeaderGoal(CompanionEntity mob) {
			this.mob = mob;
			setControls(EnumSet.of(Control.MOVE, Control.LOOK));
		}

		@Override
		public boolean canStart() {
			if (!mob.isFollowing() || mob.getTarget() != null) return false;
			leader = mob.leader();
			return leader != null && leader.getWorld() == mob.getWorld() && mob.squaredDistanceTo(leader) > 4.5 * 4.5;
		}

		@Override
		public boolean shouldContinue() {
			return leader != null && mob.isFollowing() && mob.getTarget() == null && !mob.getNavigation().isIdle()
				&& mob.squaredDistanceTo(leader) > 2.5 * 2.5;
		}

		@Override
		public void tick() {
			if (leader == null) return;
			mob.getLookControl().lookAt(leader, 10f, mob.getMaxLookPitchChange());
			if (--recalc <= 0) {
				recalc = 10;
				double speed = mob.squaredDistanceTo(leader) > 12 * 12 ? 1.35 : 1.0;
				mob.getNavigation().startMovingTo(leader, speed);
			}
		}

		@Override
		public void stop() {
			mob.getNavigation().stop();
			leader = null;
		}
	}

	/** Picks a monster that threatens the party as the target. */
	static class DefendPartyGoal extends Goal {
		private final CompanionEntity mob;
		private final double radius;

		DefendPartyGoal(CompanionEntity mob, double radius) {
			this.mob = mob;
			this.radius = radius;
			setControls(EnumSet.of(Control.TARGET));
		}

		@Override
		public boolean canStart() {
			if (!mob.isFollowing() || mob.getCompanionState() != STATE_NORMAL || mob.isSwallowed()) return false;
			if (mob.age % 10 != 0) return false;
			List<MobEntity> list = mob.threats(radius);
			MobEntity best = null;
			double bestD = Double.MAX_VALUE;
			for (MobEntity m : list) {
				double d = m.squaredDistanceTo(mob);
				if (d < bestD) {
					bestD = d;
					best = m;
				}
			}
			if (best == null) return false;
			mob.setTarget(best);
			return true;
		}

		@Override
		public boolean shouldContinue() {
			LivingEntity t = mob.getTarget();
			return t != null && t.isAlive() && mob.getCompanionState() == STATE_NORMAL && mob.squaredDistanceTo(t) < radius * radius * 4;
		}

		@Override
		public void stop() {
			mob.setTarget(null);
		}
	}
}
