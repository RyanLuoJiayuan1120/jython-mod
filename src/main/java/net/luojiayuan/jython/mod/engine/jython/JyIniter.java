package net.luojiayuan.jython.mod.engine.jython;

import org.python.core.PySystemState;
import org.python.core.Py;
import net.luojiayuan.jython.mod.Jythonmod;
import net.luojiayuan.jython.mod.config.ModConfig;
public class JyIniter {
    public static final PySystemState sys = new PySystemState();
    public JyIniter() {
        Jythonmod.LOGGER.info("Using Jython...");
        Jythonmod.LOGGER.info("Initializing Jython environment...");
		String pythonHome = "/";
		System.setProperty("python.home", pythonHome);
		System.setProperty("python.path", "/Lib");
		Jythonmod.LOGGER.info("Set python.home=" + pythonHome);
		Jythonmod.LOGGER.info("Set python.path=/Lib");
		PySystemState.initialize();
			
		sys.path.append(Py.newString("Lib"));
		sys.path.append(Py.newString("/assets/jython-mod/jython"));
		if (Jythonmod.CONFIG.debugMode) {
			Jythonmod.LOGGER.info("Python path configured: " + sys.path);
		}
    }
}
