package net.luojiayuan.jython.mod;

import net.fabricmc.api.DedicatedServerModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.luojiayuan.jython.mod.config.ModConfig;
import net.luojiayuan.jython.mod.loader.Loader;

public class JythonModServer implements DedicatedServerModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("jython-mod-server");

	@Override
	public void onInitializeServer() {
		// Ensure config is initialized
		if (Jythonmod.CONFIG == null) {
			AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
			Jythonmod.CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
		}

		// Skip initialization if mod is disabled
		if (!Jythonmod.CONFIG.enabled) {
			LOGGER.info("Jython Mod Server is disabled in config.");
			return;
		}

		LOGGER.info("Initializing Jython Mod Server...");

		// Server initialization - runs server modules only
		try {
			Loader loader =  new Loader("server");

		} catch (Exception e) {
			LOGGER.error("Runtime error in server loader: {}", e.getMessage(), e);
		}
	}
}
