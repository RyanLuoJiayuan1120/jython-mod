package net.luojiayuan.jython.mod.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.luojiayuan.jython.mod.ModRuntime;
import net.luojiayuan.jython.mod.platform.PlatformHooks;



public class MappingLoader {
    private static MemoryMappingTree tree;

    public static void Loader() {
        tree = new MemoryMappingTree();
        try (InputStream mapping = PlatformHooks.get().findModResource(
                PlatformHooks.get().getModId(), "mappings.tiny")) {
            if (mapping == null) {
                ModRuntime.LOGGER.error("Mapping resource 'mappings.tiny' was not found");
                return;
            }
            MappingReader.read(
                    new InputStreamReader(mapping, StandardCharsets.UTF_8),
                    tree
            );
        } catch (IOException e) {
            ModRuntime.LOGGER.error("Error loading mappings: {}", e.toString());
        }
    }

    public static MemoryMappingTree getTree() {
        return tree;
    }
}
