package net.luojiayuan.jython.mod.engine.graalpy;

import net.luojiayuan.jython.mod.Jythonmod;
import net.luojiayuan.jython.mod.config.ModConfig;
import net.luojiayuan.jython.mod.engine.RunnerMain;

public class GpIniter {
    public GpIniter() {
        Jythonmod.LOGGER.info("Using GraalPy...");
        Jythonmod.LOGGER.info("Initializing GraalPy environment...");
        
        String pythonHome = "/";
        System.setProperty("python.home", pythonHome);
        System.setProperty("python.path", "/Lib");
        Jythonmod.LOGGER.info("Set python.home=" + pythonHome);
        Jythonmod.LOGGER.info("Set python.path=/Lib");
        
        if (Jythonmod.CONFIG.debugMode) {
            Jythonmod.LOGGER.info("GraalPy environment initialized.");
        }
    }
}
