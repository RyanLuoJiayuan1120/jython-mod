package net.luojiayuan.jython.mod.platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricPlatformHooks implements PlatformHooks {
    @Override
    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
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
