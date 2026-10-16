package dev.konocraft.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Ordinary citizens of Axel. They wander their street and have opinions about adventurers. */
public class TownsfolkEntity extends KonoNpcEntity {
	/** Skins for ordinary citizens; variant 6 is Keele's. */
	public static final int VARIANTS = 6;
	public static final int KEELE_VARIANT = 6;
	/** All skins: random citizens, Keele, then one per townsperson with a job (7..13). */
	public static final int SKINS = 14;
	public static final int CITIZEN = 0, GUARD = 1, BUILDER = 2, KEELE = 3;
	/** Townspeople with a job and a fixed place: they stay at their counter all day. */
	public static final int SMITH = 4, INNKEEPER = 5, BATHKEEPER = 6, POLICE = 7, LORD = 8, NOBLE = 9, MERCHANT = 10;
	/** What the townsfolk is walking to: their front door at night, the guild in the evening... */
	public static final int GO_NONE = 0, GO_HOME = 1, GO_TAVERN = 2, GO_WORK = 3;
	private static final TrackedData<Integer> VARIANT = DataTracker.registerData(TownsfolkEntity.class, TrackedDataHandlerRegistry.INTEGER);

	private int role = CITIZEN;
	private BlockPos originalHome;
	private BlockPos scheduleTarget;
	private int scheduleMode = GO_NONE;
	private int scheduleTicks;

	public TownsfolkEntity(EntityType<? extends TownsfolkEntity> type, World world) {
		super(type, world);
	}

	public int getRole() {
		return role;
	}

	public void setRole(int role) {
		this.role = role;
	}

	/** The street spot this citizen lives by, kept while they visit the tavern. */
	public BlockPos getOriginalHome() {
		if (originalHome == null) originalHome = home != null ? BlockPos.ofFloored(home) : getBlockPos();
		return originalHome;
	}

	public void walkTo(BlockPos target, int mode) {
		far = false;
		scheduleTarget = target;
		scheduleMode = mode;
		scheduleTicks = 0;
		getNavigation().stop();
	}

	public BlockPos getScheduleTarget() {
		return scheduleTarget;
	}

	public int getScheduleMode() {
		return scheduleMode;
	}

	public void clearSchedule() {
		scheduleTarget = null;
		scheduleMode = GO_NONE;
	}

	@Override
	public boolean staysHome() {
		return scheduleTarget == null;
	}

	public boolean isWorker() {
		return role >= SMITH;
	}

	private int farTimer;
	private boolean far;

	/**
	 * Citizens nobody is looking at think four times slower. Nobody within 48 blocks can tell the
	 * difference, but a town of two hundred people costs a quarter of the server time.
	 */
	@Override
	public void tickMovement() {
		if (!getWorld().isClient) {
			if (++farTimer % 20 == 0) far = scheduleTarget == null && getWorld().getClosestPlayer(this, 48) == null;
			if (far && farTimer % 4 != 0) return;
		}
		super.tickMovement();
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(VARIANT, 0);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new ScheduleGoal(this));
		goalSelector.add(3, new ReturnHomeGoal(this, 1.5) {
			@Override
			public boolean canStart() {
				return isWorker() && super.canStart();
			}
		});
		goalSelector.add(4, new ReturnHomeGoal(this, 9));
		goalSelector.add(5, new WanderAroundGoal(this, 0.55, 80) {
			@Override
			public boolean canStart() {
				return !isWorker() && super.canStart();
			}
		});
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 7f));
		goalSelector.add(7, new LookAroundGoal(this));
	}

	public int getVariant() {
		return dataTracker.get(VARIANT);
	}

	public void setVariant(int v) {
		dataTracker.set(VARIANT, Math.floorMod(v, SKINS));
	}

	public void setHome(BlockPos pos) {
		setHome(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, getYaw());
	}

	@Override
	public String characterKey() {
		return "townsfolk";
	}

	@Override
	protected boolean isUnique() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return true;
	}

	@Override
	protected String hurtLine() {
		if (role == KEELE) return "Хе-хе... Юноша, я мёртв уже триста лет. Это не больно, но обидно.";
		if (role == GUARD) return "Нападение на стражу! Я запомнил твоё лицо, авантюрист!";
		if (role == BUILDER) return "Эй! Я тут вообще-то ваш бардак разгребаю!";
		if (role == SMITH) return "Осторожнее! Я держу раскалённое железо, а ты держишь мою вежливость.";
		if (role == INNKEEPER) return "Драки — во дворе! Мебель у меня одна, а авантюристов много.";
		if (role == BATHKEEPER) return "В бане не дерутся! Только моются. И иногда поют.";
		if (role == POLICE) return "Нападение на должностное лицо. Записываю. Опять.";
		if (role == LORD) return "Стража! СТРАЖА! Этот простолюдин коснулся моего камзола!";
		if (role == NOBLE) return "Хм. Лалатина говорила, что её друзья... своеобразные.";
		if (role == MERCHANT) return "Эй-эй! Сначала платят, потом бьют! То есть... вообще не бьют!";
		String[] lines = {"Стража! Тут авантюрист дерётся!", "Ай! Я плачу налоги, между прочим!", "Опять эти авантюристы..."};
		return lines[random.nextInt(lines.length)];
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Variant", getVariant());
		nbt.putInt("Role", role);
		if (originalHome != null) nbt.putLong("OriginalHome", originalHome.asLong());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		setVariant(nbt.getInt("Variant"));
		role = nbt.getInt("Role");
		if (nbt.contains("OriginalHome")) originalHome = BlockPos.fromLong(nbt.getLong("OriginalHome"));
	}

	/** Walks to the scheduled spot; dev.konocraft.story.TownLife decides what happens on arrival. */
	static class ScheduleGoal extends Goal {
		private final TownsfolkEntity mob;

		ScheduleGoal(TownsfolkEntity mob) {
			this.mob = mob;
			setControls(EnumSet.of(Control.MOVE, Control.LOOK));
		}

		@Override
		public boolean canStart() {
			return mob.scheduleTarget != null;
		}

		@Override
		public boolean shouldContinue() {
			return mob.scheduleTarget != null;
		}

		@Override
		public void tick() {
			BlockPos t = mob.scheduleTarget;
			if (t == null) return;
			mob.scheduleTicks++;
			double d2 = mob.squaredDistanceTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5);
			boolean close = d2 < (mob.scheduleMode == GO_WORK ? 16 : 5.5);
			// people who cannot find a way for half a minute slip in somewhere out of sight
			if (close || mob.scheduleTicks > 600) {
				dev.konocraft.story.TownLife.arrive(mob);
				return;
			}
			if (mob.scheduleTicks % 20 == 1 || mob.getNavigation().isIdle()) {
				mob.getNavigation().startMovingTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, mob.scheduleMode == GO_WORK ? 0.75 : 0.6);
			}
		}
	}
}
