package net.luojiayuan.jython.mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.luojiayuan.jython.mod.platform.NeoForgePlatformHooks;
import net.luojiayuan.jython.mod.platform.PlatformHooks;
import net.neoforged.fml.common.Mod;

@Mod("jythonmod")
public class JythonModNeoForge {
	private static final Logger LOGGER = LoggerFactory.getLogger(JythonModNeoForge.class);

	public JythonModNeoForge() {
		PlatformHooks.set(new NeoForgePlatformHooks());
		ModBootstrap.start(LOGGER, "main");
	}
}
