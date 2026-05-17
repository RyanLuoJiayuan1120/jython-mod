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
import net.luojiayuan.jython.mod.engine.jython.JyIniter;

public class Jythonmod implements ModInitializer {
	public static final String MOD_ID = "jython-mod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static ModConfig CONFIG;
	
	String path_ = path.get();

	@Override
	public void onInitialize() {
		// 初始化配置
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
		MappingLoader.Loader();
		if (CONFIG.engineVersion == 1) {
			JyIniter initer = new JyIniter();
		} else if (CONFIG.engineVersion == 2) {
			GpIniter initer = new GpIniter();
		}
		MappingBridge.init();
		LOGGER.info(path_);
		LOGGER.info("Hello Fabric world!");
		if (CONFIG.debugMode) {
			LOGGER.info("Jython Mod Configuration:");
			LOGGER.info("  Enabled: " + CONFIG.enabled);
			LOGGER.info("  Debug Mode: " + CONFIG.debugMode);
			LOGGER.info("  Script Path: " + CONFIG.scriptPath);
		}
		if (!CONFIG.enabled) {
			LOGGER.info("Jython Mod is disabled in config.");
			return;
		}
		try {
			
			Loader loader =  new Loader("main");

		} catch (Exception e) {
			LOGGER.error("Runtime Error in running: " + e.getMessage(), e);
		}
	}
}