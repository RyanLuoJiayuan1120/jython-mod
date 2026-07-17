package net.luojiayuan.jython.mod;
import net.luojiayuan.jython.mod.loader.Loader;
import net.fabricmc.api.ModInitializer;
import net.luojiayuan.jython.mod.utils.path;
import net.luojiayuan.jython.mod.mapping.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.luojiayuan.jython.mod.config.ModConfig;
import net.luojiayuan.jython.mod.engine.graalpy.GpIniter;

public class Jythonmod implements ModInitializer {
	public static final String MOD_ID = "jython-mod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static ModConfig CONFIG;
	
	String path_ = path.get();

	@Override
	public void onInitialize() {
		// Initialize config
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
		MappingLoader.Loader();
		GpIniter initer = new GpIniter();
		MappingBridge.init();
		LOGGER.debug("Script path: {}", path_);
		LOGGER.info("Jython Mod initialized");
		if (CONFIG.debugMode) {
			LOGGER.debug("Jython Mod Configuration:");
			LOGGER.debug("  Enabled: {}", CONFIG.enabled);
			LOGGER.debug("  Debug Mode: {}", CONFIG.debugMode);
			LOGGER.debug("  Script Path: {}", CONFIG.scriptPath);
		}
		if (!CONFIG.enabled) {
			LOGGER.info("Jython Mod is disabled in config.");
			return;
		}
		try {
			
			Loader loader =  new Loader("main");

		} catch (Exception e) {
			LOGGER.error("Runtime error in main loader: {}", e.getMessage(), e);
		}
	}
}