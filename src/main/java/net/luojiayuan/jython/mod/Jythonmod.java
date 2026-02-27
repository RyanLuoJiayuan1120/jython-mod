package net.luojiayuan.jython.mod;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import net.fabricmc.api.ModInitializer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.*;
import org.python.util.PythonInterpreter;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.core.Py;
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
			LOGGER.info("Initializing Jython environment...");

			// 关键：设置系统属性，让 Jython 能找到内置的 Lib 目录
			// 在打包的 JAR 中，Lib 目录位于根目录
			String pythonHome = "/";
			System.setProperty("python.home", pythonHome);
			System.setProperty("python.path", "/Lib");

			LOGGER.info("Set python.home=" + pythonHome);
			LOGGER.info("Set python.path=/Lib");

			// 初始化 Python 系统状态
			PySystemState.initialize();

			// 获取并配置 PySystemState
			PySystemState sys = new PySystemState();

			// 清空默认路径，重新设置
			sys.path.clear();

			// 添加内置 Lib 目录（标准库）- 使用相对路径
			sys.path.append(Py.newString("Lib"));

			// 添加 mod 的 Python 脚本目录
			sys.path.append(Py.newString("/assets/jython-mod/jython"));

			// 设置可写缓存目录（用于 .pyc 文件）
			try {
				Path cacheDir = Path.of(path_, "jython_cache");
				Files.createDirectories(cacheDir);
				sys.path.append(Py.newString(cacheDir.toString()));
				LOGGER.info("Jython cache directory: " + cacheDir);
			} catch (Exception e) {
				LOGGER.warn("Could not create cache directory: " + e.getMessage());
			}

			if (CONFIG.debugMode) {
				LOGGER.info("Python path configured: " + sys.path);
			}

			// 创建带配置的 Python 解释器
			PythonInterpreter interpreter = new PythonInterpreter();

			// 设置logger到Python环境
			interpreter.set("LOGGER", new PythonLogger(LOGGER));
			interpreter.set("ENV_TYPE", "common");
			interpreter.set("GAME_DIR", path_);

			// 从 resources 目录读取 main.py 文件
			InputStream pythonScript = getClass().getResourceAsStream(CONFIG.scriptPath);
			if (pythonScript != null) {
				LOGGER.info("Running main.py");
				interpreter.execfile(pythonScript);
				LOGGER.info("Succeed in running main.py");
				pythonScript.close();
			} else {
				LOGGER.error("Cannot find main.py at: " + CONFIG.scriptPath);
			}
			interpreter.close();

		} catch (Exception e) {
			LOGGER.error("Runtime Error in running main.py: " + e.getMessage(), e);
		}
	}
}