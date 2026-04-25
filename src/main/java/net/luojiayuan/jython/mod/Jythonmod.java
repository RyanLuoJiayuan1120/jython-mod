package net.luojiayuan.jython.mod;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.luojiayuan.jython.mod.loader.Loader;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.*;
import org.python.util.PythonInterpreter;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.core.Py;
import net.luojiayuan.jython.mod.utils.path;
import net.luojiayuan.jython.mod.utils.GameDirHelper;
import net.luojiayuan.jython.mod.mapping.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.luojiayuan.jython.mod.config.ModConfig;

public class Jythonmod implements ModInitializer {
	public static final String MOD_ID = "jython-mod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static ModConfig CONFIG;
	public static final PySystemState sys = new PySystemState();
	String path_ = path.get();

	@Override
	public void onInitialize() {
		// 初始化配置
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
		MappingLoader.Loader();

		MappingBridge.init();
		LOGGER.info(path_);
		LOGGER.info("Hello Fabric world!");
		if (CONFIG.debugMode) {
			LOGGER.info("Jython Mod Configuration:");
			LOGGER.info("  Enabled: " + CONFIG.enabled);
			LOGGER.info("  Debug Mode: " + CONFIG.debugMode);
			LOGGER.info("  Script Path: " + CONFIG.scriptPath);
			LOGGER.info("  Auto Reload: " + CONFIG.autoReload);
		}
		if (!CONFIG.enabled) {
			LOGGER.info("Jython Mod is disabled in config.");
			return;
		}
		try {
			LOGGER.info("Initializing Jython environment...");
			String pythonHome = "/";
			System.setProperty("python.home", pythonHome);
			System.setProperty("python.path", "/Lib");
			LOGGER.info("Set python.home=" + pythonHome);
			LOGGER.info("Set python.path=/Lib");
			PySystemState.initialize();
			
			sys.path.append(Py.newString("Lib"));
			sys.path.append(Py.newString("/assets/jython-mod/jython"));
			if (CONFIG.debugMode) {
				LOGGER.info("Python path configured: " + sys.path);
			}
			Loader loader =  new Loader("main");

		} catch (Exception e) {
			LOGGER.error("Runtime Error in running: " + e.getMessage(), e);
		}
	}
}