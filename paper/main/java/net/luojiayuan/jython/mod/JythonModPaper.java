package net.luojiayuan.jython.mod;

import net.luojiayuan.jython.mod.platform.PaperPlatformHooks;
import net.luojiayuan.jython.mod.platform.PlatformHooks;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Paper (Bukkit) server plugin entrypoint.
 *
 * <p>Paper is server-only: there is no client environment, so we run the shared
 * {@code main} environment followed by {@code server} (mirroring the Fabric
 * dedicated-server entrypoint split). Bytecode transformation is not supported
 * on Paper (server classes are already loaded before plugin enable and there is
 * no transformer SPI), and Python mods target the {@code org.bukkit.*} API —
 * {@code net.minecraft.*} classes are not visible to plugins.</p>
 */
public class JythonModPaper extends JavaPlugin {
	private static final Logger LOGGER = LoggerFactory.getLogger("jython-mod-paper");

	@Override
	public void onEnable() {
		PlatformHooks.set(new PaperPlatformHooks(this));
		ModBootstrap.start(LOGGER, "main");
		ModBootstrap.start(LOGGER, "server");
	}

	@Override
	public void onDisable() {
		if (ModRuntime.LOGGER != null) {
			ModRuntime.LOGGER.info("Jython Mod (Paper) disabled.");
		} else {
			LOGGER.info("Jython Mod (Paper) disabled.");
		}
	}
}
