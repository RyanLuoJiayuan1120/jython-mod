package net.luojiayuan.jython.mod;
import net.fabricmc.api.ModInitializer;
import net.luojiayuan.jython.mod.platform.FabricPlatformHooks;
import net.luojiayuan.jython.mod.platform.PlatformHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jythonmod implements ModInitializer {
	public static final String MOD_ID = "jython-mod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PlatformHooks.set(new FabricPlatformHooks());
		ModBootstrap.start(LOGGER, "main");
	}
}
