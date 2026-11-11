package net.luojiayuan.jython.mod.platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.fabric.impl.resource.ResourceLoaderImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.luojiayuan.jython.mod.builder.GeneratedPack;
import net.luojiayuan.jython.mod.mc.FabricMcApi;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FabricPlatformHooks implements PlatformHooks {
    private static final Logger LOGGER = LoggerFactory.getLogger("jython-mod");
    private static final Set<String> REGISTERED_PACK_IDS = ConcurrentHashMap.newKeySet();
    private static final String PACK_ROOT = "jython-mod/packs";

    @Override
    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean supportsResourcePacks() {
        return true;
    }

    @Override
    public void registerGeneratedPacks(List<GeneratedPack> packs) {
        if (packs == null || packs.isEmpty()) {
            return;
        }

        ModContainer baseContainer = FabricLoader.getInstance()
                .getModContainer("jython-mod")
                .orElseThrow(() -> new IllegalStateException("jython-mod container is missing"));
        Path packRoot = getGameDir().resolve(PACK_ROOT).toAbsolutePath().normalize();
        ModContainer packContainer = new GeneratedPackContainer(baseContainer, packRoot);

        for (GeneratedPack pack : packs) {
            if (pack.hasResourcePack()) {
                registerPack(packContainer, pack.id(), "resourcepacks/" + pack.id(),
                        pack.displayName(), pack.resourceDir());
            }
            if (pack.hasDataPack()) {
                registerPack(packContainer, pack.id(), "datapacks/" + pack.id(),
                        pack.displayName() + " Data", pack.dataDir());
            }
        }
    }

    private static void registerPack(ModContainer container, String id, String subPath,
                                     String displayName, Path packDir) {
        String registrationKey = subPath + ":" + id;
        if (!REGISTERED_PACK_IDS.add(registrationKey)) {
            LOGGER.debug("Built-in pack '{}' was already registered", id);
            return;
        }

        try {
            boolean registered = ResourceLoaderImpl.registerBuiltinPack(
                    Identifier.fromNamespaceAndPath("jython-mod", id),
                    subPath,
                    container,
                    Component.literal(displayName),
                    PackActivationType.ALWAYS_ENABLED
            );

            if (registered) {
                LOGGER.info("Registered hidden built-in pack '{}' ({}) at {}, enabled by default",
                        id, subPath, packDir);
            } else {
                REGISTERED_PACK_IDS.remove(registrationKey);
                LOGGER.warn("Built-in pack '{}' could not be registered (no matching namespace in {})",
                        id, packDir);
            }
        } catch (Throwable t) {
            REGISTERED_PACK_IDS.remove(registrationKey);
            LOGGER.error("Failed to register built-in pack '{}': {}", id, t.toString(), t);
        }
    }

    @Override
    public Object createMcApi() {
        return new FabricMcApi();
    }

    @Override
    public String getModId() {
        return "jython-mod";
    }

    @Override
    public InputStream findModResource(String modId, String path) {
        Path resource = FabricLoader.getInstance()
                .getModContainer(modId)
                .orElseThrow(() -> new IllegalStateException("Unknown mod: " + modId))
                .findPath(path)
                .orElse(null);
        if (resource == null) {
            return null;
        }
        try {
            return Files.newInputStream(resource);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
