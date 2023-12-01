package me.modmuss50.optifabric.mod;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class Optifabric implements ModInitializer {

	private static final OptiFabricLogger LOGGER = new OptiFabricLogger();

	public static void checkForErrors() {
		if (OptifabricError.hasError()) {
			LOGGER.error("An OptiFabric error has occurred");
			LOGGER.error(OptifabricError.getError());
		}
	}

	@Override
	public void onInitialize() {

	}

	public static OptiFabricLogger getLogger() {
		return LOGGER;
	}

	public static class OptiFabricLogger {
		private final Logger LOGGER = LogManager.getLogger("OptiFabric");

		public void debug(String category, String message, Object... args) {
			LOGGER.debug("[" + category + "] " + message, args);
		}

		public void info(String category, String message, Object... args) {
			LOGGER.info("[" + category + "] " + message, args);
		}

		public void error(String message, Object... args) {
			LOGGER.error(message, args);
		}

		public void info(String message, Object... args) {
			LOGGER.info(message, args);
		}

		public Logger get() {
			return LOGGER;
		}
	}
}
