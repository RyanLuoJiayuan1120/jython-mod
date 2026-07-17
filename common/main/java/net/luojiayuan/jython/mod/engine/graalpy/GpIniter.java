package net.luojiayuan.jython.mod.engine.graalpy;

import org.slf4j.Logger;

public class GpIniter {
    public GpIniter(Logger logger, boolean debugMode) {
        logger.info("Using GraalPy...");
        logger.info("Initializing GraalPy environment...");
        
        String pythonHome = "/";
        System.setProperty("python.home", pythonHome);
        System.setProperty("python.path", "/Lib");
        logger.debug("Set python.home={}", pythonHome);
        logger.debug("Set python.path=/Lib");
        
        if (debugMode) {
            logger.debug("GraalPy environment initialized.");
        }
    }
}
