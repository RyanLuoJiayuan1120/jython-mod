package net.luojiayuan.jython.mod.platform;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loader-specific operations used by common code.
 *
 * <p>A concrete implementation must be registered by the active mod loader
 * entrypoint before any common bootstrap code runs.</p>
 */
public interface PlatformHooks {
    Path getGameDir();

    boolean isDevelopmentEnvironment();

    /**
     * Returns the mod id used by the active mod loader for this mod.
     *
     * <p>Fabric and NeoForge use different id rules: Fabric accepts {@code jython-mod}
     * while NeoForge requires ids without hyphens, so the id is loader-specific.</p>
     */
    String getModId();

    /**
     * Opens a resource inside the given mod's file.
     *
     * @param modId the mod id
     * @param path  resource path relative to the mod file root
     * @return the resource stream, or {@code null} if it is not present
     */
    InputStream findModResource(String modId, String path);

    final class Holder {
        private Holder() {
        }

        static volatile PlatformHooks INSTANCE;
    }

    static void set(PlatformHooks hooks) {
        if (hooks == null) {
            throw new IllegalArgumentException("PlatformHooks must not be null");
        }
        Holder.INSTANCE = hooks;
    }

    static PlatformHooks get() {
        if (Holder.INSTANCE == null) {
            throw new IllegalStateException("PlatformHooks has not been initialized");
        }
        return Holder.INSTANCE;
    }
}
