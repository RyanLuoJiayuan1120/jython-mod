package net.luojiayuan.jython.mod;

import net.fabricmc.api.ClientModInitializer;
import net.luojiayuan.jython.mod.config.ModConfig;
import net.luojiayuan.jython.mod.loader.Loader;
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
		// Ensure config is initialized
		if (Jythonmod.CONFIG == null) {
			AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
			Jythonmod.CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
		}

		// Skip initialization if mod is disabled
		if (!Jythonmod.CONFIG.enabled) {
			LOGGER.info("Jython Mod Client is disabled in config.");
			return;
		}

		LOGGER.info("Initializing Jython Mod Client...");
		try {
			Loader loader =  new Loader("client");
		} catch (Exception e) {
			LOGGER.error("Runtime error in client loader: {}", e.getMessage(), e);
		}
	}
}
