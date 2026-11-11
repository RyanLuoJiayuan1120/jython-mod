package net.luojiayuan.jython.mod.platform;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import net.luojiayuan.jython.mod.builder.GeneratedPack;

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
     * Whether the production runtime uses official (Mojang) class names directly.
     *
     * <p>Fabric remaps production classes to intermediary names, so the shared
     * code must translate Yarn names via the mapping bridge. NeoForge runs on
     * official (Mojang) names at runtime, which on modern versions (1.21.11)
     * already match the Yarn names used by the Python API, so no translation is
     * needed there.</p>
     *
     * @return {@code true} when runtime class names are official Mojang names
     */
    default boolean usesOfficialMappings() {
        return false;
    }

    /**
     * Whether this platform can load the Cloth Config configuration GUI
     * (AutoConfig). Fabric and NeoForge ship Cloth Config; Paper does not,
     * so the shared bootstrap falls back to a plain JSON config file.
     *
     * @return {@code true} when AutoConfig is available
     */
    default boolean supportsConfigGui() {
        return true;
    }

    /**
     * Whether this platform supports Minecraft resource-pack / datapack
     * generation. The generated packs are registered as Fabric built-in packs;
     * Paper does not have a client resource pack system and NeoForge support is
     * still pending, so the default is {@code false}.
     *
     * @return {@code true} when per-module pack generation is supported
     */
    default boolean supportsResourcePacks() {
        return false;
    }

    /**
     * Registers the generated per-module packs with the platform, marking them
     * as required and enabled by default.
     *
     * <p>Fabric implements this with its built-in pack API. A platform without
     * support can leave the default no-op; {@link #supportsResourcePacks()}
     * should then return {@code false}.</p>
     *
     * @param packs generated packs, one per Python module
     */
    default void registerGeneratedPacks(List<GeneratedPack> packs) {
    }

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

    /**
     * Platform-native MC helpers exposed to Python as the global {@code McApi}
     * object. Returns {@code null} when the platform has no implementation yet;
     * scripts can then fall back to {@code McReflect}.
     */
    default Object createMcApi() {
        return null;
    }

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
