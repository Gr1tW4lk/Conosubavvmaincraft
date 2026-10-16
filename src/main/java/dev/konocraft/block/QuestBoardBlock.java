package dev.konocraft.block;

import dev.konocraft.story.Bounties;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** The guild's quest board. Right click to pick today's bounties. */
public class QuestBoardBlock extends HorizontalFacingBlock {
	private static final VoxelShape NORTH = Block.createCuboidShape(0, 0, 13, 16, 16, 16);
	private static final VoxelShape SOUTH = Block.createCuboidShape(0, 0, 0, 16, 16, 3);
	private static final VoxelShape EAST = Block.createCuboidShape(0, 0, 0, 3, 16, 16);
	private static final VoxelShape WEST = Block.createCuboidShape(13, 0, 0, 16, 16, 16);

	public QuestBoardBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return switch (state.get(FACING)) {
			case SOUTH -> SOUTH;
			case EAST -> EAST;
			case WEST -> WEST;
			default -> NORTH;
		};
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!world.isClient && player instanceof ServerPlayerEntity sp) {
			Bounties.openBoard(sp);
		}
		return ActionResult.success(world.isClient);
	}
}
