package net.luojiayuan.jython.mod;

import java.io.InputStream;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.luojiayuan.jython.mod.config.ModConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class JythonModClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("jython-mod-client");

	// // 注册按键绑定 - 使用 Minecraft 内置的 MISC 分类
	// public static final KeyMapping OPEN_CONFIG_KEY = KeyBindingHelper.registerKeyBinding(
	// 	new KeyMapping(
	// 		"key.jython-mod.open_config",
	// 		InputConstants.Type.KEYSYM,
	// 		GLFW.GLFW_KEY_G,
	// 		KeyMapping.Category.MISC
	// 	)
	// );
	@Override
	public void onInitializeClient() {
		// 确保配置已初始化
		if (Jythonmod.CONFIG == null) {
			AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
			Jythonmod.CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
		}

		// 如果模组被禁用，则不继续初始化
		if (!Jythonmod.CONFIG.enabled) {
			LOGGER.info("Jython Mod Client is disabled in config.");
			return;
		}

		LOGGER.info("Initializing Jython Mod Client...");

		// Jython客户端初始化 - 执行 client.py
		try {
			// 创建 Python 解释器
			PythonInterpreter interpreter = new PythonInterpreter();

			// 设置环境变量，让Python知道这是客户端
			interpreter.set("ENV_TYPE", "client");
			interpreter.set("LOGGER", new PythonLogger(LOGGER));

			// 从 resources 目录读取 main.py 文件
			InputStream pythonScript = getClass().getResourceAsStream(Jythonmod.CONFIG.scriptPath);
			if (pythonScript != null) {
				LOGGER.info("Running main.py for client initialization");
				interpreter.execfile(pythonScript);

				// 运行所有已加载的client模块
				interpreter.exec(
					"if 'importer' in globals():\n" +
					"    LOGGER.info('Client modules loaded: ' + str(len(importer.libs_client)))\n" +
					"    if importer.libs_client:\n" +
					"        for mod in importer.libs_client:\n" +
					"            if mod is not None:\n" +
					"                try:\n" +
					"                    if hasattr(mod, 'client'):\n" +
					"                        mod.client()\n" +
					"                except Exception as e:\n" +
					"                    file_path = getattr(mod, '__file__', 'unknown')\n" +
					"                    LOGGER.warning('error at running client module in ' + str(file_path) + ': ' + str(e))\n" +
					"    else:\n" +
					"        LOGGER.info('No client modules to run')\n" +
					"else:\n" +
					"    LOGGER.warning('importer not found in globals')"
				);

				LOGGER.info("Succeed in running client modules");
				pythonScript.close();
			} else {
				LOGGER.error("Cannot find main.py for client initialization");
			}
			interpreter.close();

		} catch (Exception e) {
			LOGGER.error("Runtime Error in running client modules: " + e.getMessage(), e);
		}

		// 注册按键监听
		// ClientTickEvents.END_CLIENT_TICK.register(client -> {
		// 	while (OPEN_CONFIG_KEY.consumeClick()) {
		// 		// 打开配置屏幕
		// 		client.setScreen(new net.luojiayuan.jython.mod.config.ModConfigScreen(client.screen));
		// 	}
		// });
	}
}
