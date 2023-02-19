package pl.yetanotherauthmod;

import eu.pb4.polymer.networking.api.EarlyPlayNetworkHandler;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class yaam implements ModInitializer {

	public static final String MOD_ID = "yetanotherauthmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static LoginDatabase database;

	@Override
	public void onInitialize() {

		LOGGER.info("Hello Fabric world!");

		Path databasePath = Path.of("./yaam/database.json");
		if (!Files.exists(databasePath)) {
			try {
				Files.createDirectories(databasePath.getParent());
				Files.createFile(databasePath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		database = new LoginDatabase(databasePath);



		EarlyPlayNetworkHandler.register(LimboHandler::new);

	}
}