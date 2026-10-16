package dev.konocraft;

import dev.konocraft.command.KonoCommand;
import dev.konocraft.net.ServerNet;
import dev.konocraft.registry.ModBlocks;
import dev.konocraft.registry.ModEntities;
import dev.konocraft.registry.ModItems;
import dev.konocraft.registry.ModSounds;
import dev.konocraft.story.Story;
import dev.konocraft.test.ServerAutotest;
import dev.konocraft.util.Scheduler;
import dev.konocraft.world.KonoWorld;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KonoCraft implements ModInitializer {
	public static final String MOD_ID = "konocraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		LOGGER.info("KonoCraft: Да благословят боги этот кубический мир!");
		ModSounds.init();
		ModBlocks.init();
		ModEntities.init();
		ModItems.init();
		KonoWorld.init();
		Scheduler.init();
		ServerNet.init();
		Story.init();
		KonoCommand.init();
		ServerAutotest.register();
	}
}
