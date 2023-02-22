package pl.skidam.yetanotherauthmod;

import eu.pb4.polymer.networking.api.EarlyPlayNetworkHandler;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

public class yaam implements ModInitializer {

	public static final String MOD_ID = "yetanotherauthmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static LoginDatabase database;
	public static SessionDatabase sessions;

	/**
	 * HashSet of player names that have Mojang accounts.
	 * If player is saved in here, they will be treated as online-mode ones.
	 */
	public static final HashSet<String> mojangAccountNamesCache = new HashSet<>();

	@Override
	public void onInitialize() {
		Path databasePath = Path.of("./yaam/database.json");
		if (!Files.exists(databasePath)) {
			try {
				Files.createDirectories(databasePath.getParent());
				Files.createFile(databasePath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Path sessionsPath = Path.of("./yaam/sessions.json");
		if (!Files.exists(sessionsPath)) {
			try {
				Files.createDirectories(sessionsPath.getParent());
				Files.createFile(sessionsPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		sessions = new SessionDatabase(sessionsPath);
		database = new LoginDatabase(databasePath);

		EarlyPlayNetworkHandler.register(LimboHandler::new);

		LOGGER.info("YetAnotherAuthMod initialized");
	}
}