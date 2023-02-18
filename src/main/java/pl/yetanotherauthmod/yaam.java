package pl.yetanotherauthmod;

import eu.pb4.polymer.networking.api.EarlyPlayNetworkHandler;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class yaam implements ModInitializer {

	public static final String MOD_ID = "yetanotherauthmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info("Hello Fabric world!");

		EarlyPlayNetworkHandler.register(LimboHandler::new);
	}
}