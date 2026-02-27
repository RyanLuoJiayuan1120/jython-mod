package net.luojiayuan.jython.mod;

import java.io.InputStream;

import net.fabricmc.api.ModInitializer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.*;
import org.python.util.PythonInterpreter;
import org.python.core.PyObject;
import net.luojiayuan.jython.mod.utils.path;
import net.luojiayuan.jython.mod.utils.GameDirHelper;
// import net.luojiayuan.jython.mod.utils.GameDirHelperExample;
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
	
	String path_ = path.get();

	@Override
	public void onInitialize() {
		// 初始化配置
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

		LOGGER.info(path_);
		LOGGER.info("Hello Fabric world!");
		if (CONFIG.debugMode) {
			LOGGER.info("Jython Mod Configuration:");
			LOGGER.info("  Enabled: " + CONFIG.enabled);
			LOGGER.info("  Debug Mode: " + CONFIG.debugMode);
			LOGGER.info("  Script Path: " + CONFIG.scriptPath);
			LOGGER.info("  Auto Reload: " + CONFIG.autoReload);
			LOGGER.info("  Script Timeout: " + CONFIG.scriptTimeout + "s");
		}

		// 如果模组被禁用，则不继续初始化
		if (!CONFIG.enabled) {
			LOGGER.info("Jython Mod is disabled in config.");
			return;
		}

		// Jython使用示例 - 导入并执行 main.py
		try {
			LOGGER.info("Initing Jython ...");

			// 创建 Python 解释器
			PythonInterpreter interpreter = new PythonInterpreter();

			// 设置logger到Python环境
			interpreter.set("LOGGER", new PythonLogger(LOGGER));
			interpreter.set("ENV_TYPE", "common");

			// 从 resources 目录读取 main.py 文件
			InputStream pythonScript = getClass().getResourceAsStream(CONFIG.scriptPath);
			LOGGER.info(pythonScript.toString());
			if (pythonScript != null) {
				LOGGER.info("Running main.py");
				interpreter.execfile(pythonScript);
				LOGGER.info("Succeed in running main.py");
				pythonScript.close();
			} else {
				LOGGER.error("Cannot init Jython, stop!");
			}
			interpreter.close();

		} catch (Exception e) {
			LOGGER.error("Runtime Error in running main.py, stop: " + e.getMessage(), e);
		}
	}
}