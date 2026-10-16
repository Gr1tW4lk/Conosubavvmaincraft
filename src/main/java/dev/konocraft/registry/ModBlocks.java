package dev.konocraft.registry;

import dev.konocraft.KonoCraft;
import dev.konocraft.block.QuestBoardBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {
	public static final Block QUEST_BOARD = Registry.register(Registries.BLOCK, KonoCraft.id("quest_board"),
		new QuestBoardBlock(FabricBlockSettings.create().mapColor(MapColor.BROWN).strength(2.0f).sounds(BlockSoundGroup.WOOD).nonOpaque().burnable()));

	private ModBlocks() {
	}

	public static void init() {
	}
}
