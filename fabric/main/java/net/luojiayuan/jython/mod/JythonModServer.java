package net.luojiayuan.jython.mod;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.luojiayuan.jython.mod.platform.FabricPlatformHooks;
import net.luojiayuan.jython.mod.platform.PlatformHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JythonModServer implements DedicatedServerModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("jython-mod-server");

	@Override
	public void onInitializeServer() {
		LOGGER.info("Initializing Jython Mod Server...");
		PlatformHooks.set(new FabricPlatformHooks());
		ModBootstrap.start(LOGGER, "server");
	}
}
