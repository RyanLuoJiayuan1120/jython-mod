package net.luojiayuan.jython.mod;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.luojiayuan.jython.mod.config.ModConfig;
import net.luojiayuan.jython.mod.engine.graalpy.GpIniter;
import net.luojiayuan.jython.mod.loader.Loader;
import net.luojiayuan.jython.mod.mapping.MappingBridge;
import net.luojiayuan.jython.mod.mapping.MappingLoader;
import org.slf4j.Logger;

/**
 * Common mod startup flow used by Fabric and NeoForge entrypoints.
 */
public final class ModBootstrap {
    private static volatile boolean initialized;

    private ModBootstrap() {
    }

    public static synchronized void start(Logger logger, String envType) {
        if (logger == null) {
            throw new IllegalArgumentException("Logger must not be null");
        }

        if (ModRuntime.CONFIG == null) {
            AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
            ModRuntime.init(
                    AutoConfig.getConfigHolder(ModConfig.class).getConfig(),
                    logger
            );
        } else {
            ModRuntime.LOGGER = logger;
        }

        if (!initialized) {
            MappingLoader.Loader();
            MappingBridge.init();
            new GpIniter(ModRuntime.LOGGER, ModRuntime.CONFIG.debugMode);
            logConfiguration();
            initialized = true;
        }

        if (!ModRuntime.CONFIG.enabled) {
            ModRuntime.LOGGER.info("Jython Mod is disabled in config.");
            return;
        }

        try {
            new Loader(envType);
        } catch (Exception e) {
            ModRuntime.LOGGER.error("Runtime error in {} loader: {}", envType, e.getMessage(), e);
        }
    }

    private static void logConfiguration() {
        if (!ModRuntime.CONFIG.debugMode) {
            return;
        }

        ModRuntime.LOGGER.debug("Jython Mod Configuration:");
        ModRuntime.LOGGER.debug("  Enabled: {}", ModRuntime.CONFIG.enabled);
        ModRuntime.LOGGER.debug("  Debug Mode: {}", ModRuntime.CONFIG.debugMode);
        ModRuntime.LOGGER.debug("  Script Path: {}", ModRuntime.CONFIG.scriptPath);
    }
}
