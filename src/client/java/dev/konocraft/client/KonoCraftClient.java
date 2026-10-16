package dev.konocraft.client;

import dev.konocraft.client.test.ClientAutotest;
import net.fabricmc.api.ClientModInitializer;

public class KonoCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientAutotest.register();
	}
}
