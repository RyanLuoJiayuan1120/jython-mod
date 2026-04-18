package net.luojiayuan.jython.mod;

import java.io.InputStream;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.luojiayuan.jython.mod.config.ModConfig;
import net.luojiayuan.jython.mod.loader.Loader;
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
		try {
			Loader loader =  new Loader("client");
		} catch (Exception e) {
			LOGGER.error("Runtime Error in running client modules: " + e.getMessage(), e);
		}
	}
}
