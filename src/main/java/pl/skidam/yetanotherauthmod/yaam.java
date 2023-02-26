package pl.skidam.yetanotherauthmod;

import eu.pb4.polymer.api.x.EarlyPlayNetworkHandler;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.skidam.yetanotherauthmod.data.LoginDatabase;
import pl.skidam.yetanotherauthmod.data.SessionDatabase;

import java.nio.file.Files;
import java.nio.file.Path;

public class yaam implements ModInitializer {

	public static final String MOD_ID = "yetanotherauthmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static LoginDatabase database;
	public static SessionDatabase sessions;

	@Override
	public void onInitialize() {

		Path databasePath = Path.of("./yaam/database.json");
		if (!Files.exists(databasePath)) {
			try {
				Files.createDirectories(databasePath.getParent());
				Files.createFile(databasePath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Path sessionsPath = Path.of("./yaam/sessions.json");
		if (!Files.exists(sessionsPath)) {
			try {
				Files.createDirectories(sessionsPath.getParent());
				Files.createFile(sessionsPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		sessions = new SessionDatabase(sessionsPath);
		database = new LoginDatabase(databasePath);


		EarlyPlayNetworkHandler.register(LimboHandler::new);

		LOGGER.info("YetAnotherAuthMod initialized");
	}
}