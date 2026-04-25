package net.luojiayuan.jython.mod;

import java.io.InputStream;

import net.fabricmc.api.DedicatedServerModInitializer;
import org.python.util.PythonInterpreter;
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

		// Jython服务器初始化 - 不执行 main.py，只运行 server 模块
		try {
			Loader loader =  new Loader("server");

		} catch (Exception e) {
			LOGGER.error("Runtime Error in running server modules: " + e.getMessage(), e);
		}
	}
}
