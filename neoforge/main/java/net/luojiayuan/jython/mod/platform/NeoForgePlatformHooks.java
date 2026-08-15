package net.luojiayuan.jython.mod.platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.IModFileInfo;

public final class NeoForgePlatformHooks implements PlatformHooks {
    private static final String MOD_ID = "jythonmod";

    @Override
    public Path getGameDir() {
        return FMLLoader.getCurrent().getGameDir();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public InputStream findModResource(String modId, String path) {
        IModFileInfo info = ModList.get().getModFileById(modId);
        if (info == null) {
            return null;
        }
        try {
            return info.getFile().getContents().openFile(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
