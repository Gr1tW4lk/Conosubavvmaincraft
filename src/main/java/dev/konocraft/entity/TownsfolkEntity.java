package dev.konocraft.entity;

import net.minecraft.entity.EntityType;
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
	public static final int VARIANTS = 6;
	private static final TrackedData<Integer> VARIANT = DataTracker.registerData(TownsfolkEntity.class, TrackedDataHandlerRegistry.INTEGER);

	public TownsfolkEntity(EntityType<? extends TownsfolkEntity> type, World world) {
		super(type, world);
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(VARIANT, 0);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(4, new ReturnHomeGoal(this, 9));
		goalSelector.add(5, new WanderAroundGoal(this, 0.55, 80));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 7f));
		goalSelector.add(7, new LookAroundGoal(this));
	}

	public int getVariant() {
		return dataTracker.get(VARIANT);
	}

	public void setVariant(int v) {
		dataTracker.set(VARIANT, Math.floorMod(v, VARIANTS));
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
		String[] lines = {"Стража! Тут авантюрист дерётся!", "Ай! Я плачу налоги, между прочим!", "Опять эти авантюристы..."};
		return lines[random.nextInt(lines.length)];
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Variant", getVariant());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		setVariant(nbt.getInt("Variant"));
	}
}
