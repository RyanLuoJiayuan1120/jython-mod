package net.luojiayuan.jython.mod.platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper (Bukkit) implementation of {@link PlatformHooks}.
 *
 * <p>Paper is a server-only plugin platform. The game directory is the server
 * root, mod resources are read from the plugin's own classloader (the fat jar),
 * and the runtime uses official Mojang class names — but only {@code org.bukkit.*}
 * is visible to plugins, so Python mods target the Bukkit API.</p>
 */
public final class PaperPlatformHooks implements PlatformHooks {
    private final JavaPlugin plugin;

    public PaperPlatformHooks(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Path getGameDir() {
        // Server root directory (where plugins/, world/, config/ live).
        // Fall back to the plugin data folder's parent chain when needed.
        try {
            return plugin.getServer().getWorldContainer().toPath();
        } catch (Exception e) {
            return plugin.getDataFolder().getParentFile().getParentFile().toPath();
        }
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return false;
    }

    @Override
    public boolean usesOfficialMappings() {
        // Paper runs official (Mojang) class names; Bukkit API needs no mapping.
        return true;
    }

    @Override
    public String getModId() {
        return "jython-mod";
    }

    @Override
    public boolean supportsConfigGui() {
        return false;
    }

    @Override
    public boolean supportsResourcePacks() {
        return false;
    }

    @Override
    public InputStream findModResource(String modId, String path) {
        InputStream in = plugin.getClass().getClassLoader().getResourceAsStream(path);
        if (in != null) {
            return in;
        }
        return null;
    }
}
