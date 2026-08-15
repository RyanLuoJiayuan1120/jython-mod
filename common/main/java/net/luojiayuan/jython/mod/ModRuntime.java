package net.luojiayuan.jython.mod;

import net.luojiayuan.jython.mod.config.ModConfig;
import org.slf4j.Logger;

/**
 * Loader-neutral runtime state shared by common code.
 */
public final class ModRuntime {
    public static ModConfig CONFIG;
    public static Logger LOGGER;

    private ModRuntime() {
    }

    public static void init(ModConfig config, Logger logger) {
        if (config == null) {
            throw new IllegalArgumentException("ModConfig must not be null");
        }
        if (logger == null) {
            throw new IllegalArgumentException("Logger must not be null");
        }
        CONFIG = config;
        LOGGER = logger;
    }
}
