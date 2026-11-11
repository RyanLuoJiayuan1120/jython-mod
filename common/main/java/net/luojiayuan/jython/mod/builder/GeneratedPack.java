package net.luojiayuan.jython.mod.builder;

import java.nio.file.Path;

/**
 * One pack generated from one Python module ZIP.
 *
 * <p>A module can contribute a resource pack, a data pack, or both. The two
 * pack types have different pack formats, so they live in separate directories
 * and get their own {@code pack.mcmeta}.</p>
 *
 * @param id          sanitized module id, also used as the pack id suffix
 * @param displayName human readable module name
 * @param resourceDir resource-pack directory, or {@code null} if none
 * @param dataDir     data-pack directory, or {@code null} if none
 */
public record GeneratedPack(String id, String displayName, Path resourceDir, Path dataDir) {
    public boolean hasResourcePack() {
        return resourceDir != null;
    }

    public boolean hasDataPack() {
        return dataDir != null;
    }
}
