package net.luojiayuan.jython.mod;

import java.io.InputStream;

import net.fabricmc.api.DedicatedServerModInitializer;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.luojiayuan.jython.mod.config.ModConfig;

public class JythonModServer implements DedicatedServerModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("jython-mod-server");

	@Override
	public void onInitializeServer() {
		// 确保配置已初始化
		if (Jythonmod.CONFIG == null) {
			AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
			Jythonmod.CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
		}

		// 如果模组被禁用，则不继续初始化
		if (!Jythonmod.CONFIG.enabled) {
			LOGGER.info("Jython Mod Server is disabled in config.");
			return;
		}

		LOGGER.info("Initializing Jython Mod Server...");

		// Jython服务器初始化 - 执行 server.py
		try {
			// 创建 Python 解释器
			PythonInterpreter interpreter = new PythonInterpreter();

			// 设置环境变量，让Python知道这是服务器端
			interpreter.set("ENV_TYPE", "server");
			interpreter.set("LOGGER", new PythonLogger(LOGGER));

			// 从 resources 目录读取 main.py 文件
			InputStream pythonScript = getClass().getResourceAsStream(Jythonmod.CONFIG.scriptPath);
			if (pythonScript != null) {
				LOGGER.info("Running main.py for server initialization");
				interpreter.execfile(pythonScript);

				// 运行所有已加载的server模块
				interpreter.exec(
					"if 'importer' in globals():\n" +
					"    LOGGER.info('Server modules loaded: ' + str(len(importer.libs_server)))\n" +
					"    if importer.libs_server:\n" +
					"        for mod in importer.libs_server:\n" +
					"            if mod is not None:\n" +
					"                try:\n" +
					"                    if hasattr(mod, 'server'):\n" +
					"                        mod.server()\n" +
					"                except Exception as e:\n" +
					"                    file_path = getattr(mod, '__file__', 'unknown')\n" +
					"                    LOGGER.warning('error at running server module in ' + str(file_path) + ': ' + str(e))\n" +
					"    else:\n" +
					"        LOGGER.info('No server modules to run')\n" +
					"else:\n" +
					"    LOGGER.warning('importer not found in globals')"
				);

				LOGGER.info("Succeed in running server modules");
				pythonScript.close();
			} else {
				LOGGER.error("Cannot find main.py for server initialization");
			}
			interpreter.close();

		} catch (Exception e) {
			LOGGER.error("Runtime Error in running server modules: " + e.getMessage(), e);
		}
	}
}
