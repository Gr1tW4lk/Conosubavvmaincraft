package dev.konocraft;

import dev.konocraft.test.ServerAutotest;
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
		ServerAutotest.register();
	}
}
