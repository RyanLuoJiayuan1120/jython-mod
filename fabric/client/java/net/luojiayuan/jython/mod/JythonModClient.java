package net.luojiayuan.jython.mod;

import net.fabricmc.api.ClientModInitializer;
import net.luojiayuan.jython.mod.platform.FabricPlatformHooks;
import net.luojiayuan.jython.mod.platform.PlatformHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		LOGGER.info("Initializing Jython Mod Client...");
		PlatformHooks.set(new FabricPlatformHooks());
		ModBootstrap.start(LOGGER, "client");
	}
}
