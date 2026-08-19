package net.luojiayuan.jython.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.luojiayuan.jython.mod.config.ModConfig;
import net.luojiayuan.jython.mod.engine.graalpy.GpIniter;
import net.luojiayuan.jython.mod.loader.Loader;
import net.luojiayuan.jython.mod.mapping.MappingBridge;
import net.luojiayuan.jython.mod.mapping.MappingLoader;
import net.luojiayuan.jython.mod.platform.PlatformHooks;
import org.slf4j.Logger;

/**
 * Common mod startup flow shared by the Fabric, NeoForge and Paper entrypoints.
 */
public final class ModBootstrap {
    private static volatile boolean initialized;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ModBootstrap() {
    }

    public static synchronized void start(Logger logger, String envType) {
        if (logger == null) {
            throw new IllegalArgumentException("Logger must not be null");
        }

        if (ModRuntime.CONFIG == null) {
            if (PlatformHooks.get().supportsConfigGui()) {
                // Cloth Config (Fabric / NeoForge). Raw types are required
                // because ModConfig deliberately does not implement ConfigData
                // (that interface is absent on Paper), while AutoConfig's
                // register() declares T extends ConfigData.
                @SuppressWarnings({"rawtypes", "unchecked"})
                Class configClass = ModConfig.class;
                AutoConfig.register(configClass, GsonConfigSerializer::new);
                ModRuntime.init(
                        (ModConfig) AutoConfig.getConfigHolder(configClass).getConfig(),
                        logger
                );
            } else {
                // Paper: no Cloth Config, read a plain JSON file (or defaults).
                ModRuntime.init(loadPaperConfig(logger), logger);
            }
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

    /**
     * Loads the config from {@code {gamedir}/config/jython-mod.json} on
     * platforms without Cloth Config (Paper). Missing fields fall back to the
     * {@link ModConfig} defaults.
     */
    private static ModConfig loadPaperConfig(Logger logger) {
        ModConfig config = new ModConfig();
        Path configFile = PlatformHooks.get().getGameDir().resolve("config/jython-mod.json");
        if (!Files.isRegularFile(configFile)) {
            logger.info("Config not found at {}, using defaults.", configFile);
            return config;
        }
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
            if (loaded != null) {
                config = loaded;
            }
            logger.info("Loaded config from {}", configFile);
        } catch (IOException | com.google.gson.JsonParseException e) {
            logger.error("Failed to read config {}: {}", configFile, e.getMessage());
        }
        return config;
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
