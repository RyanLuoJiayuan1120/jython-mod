package net.luojiayuan.jython.mod.mapping;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.luojiayuan.jython.mod.Jythonmod;



public class MappingLoader {
    private static MemoryMappingTree tree;

    public static void Loader() {
        tree = new MemoryMappingTree();
        try {
            Path mappingPath = FabricLoader.getInstance()
                .getModContainer("jython-mod") // ← 你的 modid
                .orElseThrow()
                .getPath("mappings.tiny");    // ✅ 不要引号
            System.out.println(mappingPath.toString());
            MappingReader.read(mappingPath, tree);
        } catch (IOException e) {
            Jythonmod.LOGGER.error("Error at loading mapping: "+e.toString());
        }
    }

    public static MemoryMappingTree getTree() {
        return tree;
    }
}
