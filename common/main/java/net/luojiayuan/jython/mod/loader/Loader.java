package net.luojiayuan.jython.mod.loader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.Gson;

import net.luojiayuan.jython.mod.ModRuntime;
import net.luojiayuan.jython.mod.platform.PlatformHooks;
import net.luojiayuan.jython.mod.utils.GameDirHelper;
import net.luojiayuan.jython.mod.builder.*;
import net.luojiayuan.jython.mod.engine.graalpy.GpRunner;
import net.luojiayuan.jython.mod.engine.RunnerMain;

public class Loader {
    public Vector<File> dirs = new Vector<File>();
    public Vector<String> mods = new Vector<String>();
    /**
     * Keep GraalPy contexts alive for the server lifetime. The upstream loader
     * closes each context immediately after main() — that makes any Python
     * callback registered with a Fabric event invalid ("Context is already
     * closed"). The economy module needs long-lived command/tick listeners, so
     * contexts are retained until process exit.
     */
    private static final Vector<RunnerMain> ACTIVE_RUNNERS = new Vector<RunnerMain>();

    /** Called on server stop so persistent GraalPy contexts do not leak. */
    public static synchronized void closeAll() {
        for (RunnerMain runner : ACTIVE_RUNNERS) {
            try {
                runner.close();
            } catch (Throwable t) {
                ModRuntime.LOGGER.warn("Failed to close Python context: {}", t.toString());
            }
        }
        ACTIVE_RUNNERS.clear();
    }
    private final String gameDir;
    private final String pythonPackagesPath;

    private static class ModConfigJson {
        String name;
        String version;
        List<String> dependencies;
    }

    public Loader(String env_type){
        this.gameDir = GameDirHelper.getGameDirPath();
        this.pythonPackagesPath = ModRuntime.CONFIG.pythonPackagesPath.replace("{gamedir}", gameDir);
        Scanner();
        ModLoader(env_type);
        if (PlatformHooks.get().supportsResourcePacks()) {
            try {
                PacksPacker();
            } catch (Exception e) {
                ModRuntime.LOGGER.error(e.toString());
            }
        }
    }

    private void Scanner(){
        String modsPath = ModRuntime.CONFIG.modsPaths;
        String resolvedPath = modsPath.replace("{gamedir}", gameDir);
        String[] resolvedPaths = resolvedPath.split(";"); // NOTE: paths must not contain semicolons
        
        for (String path : resolvedPaths){
            File folder = new File(path);
            if (!folder.exists()) {
                ModRuntime.LOGGER.debug("Creating dir: {}", path);
                try {
                    folder.mkdirs();
                    ModRuntime.LOGGER.debug("Created dir: {}", path);
                } catch (Exception e){
                    ModRuntime.LOGGER.error("Failed to create dir \"{}\": {}", path, e.toString());
                }
            }
            dirs.add(new File(path));
        }
    }

    private void ModLoader(String env_type){
        ensurePythonPackagesDir();

        for (File folder : dirs) {
            try {
                File[] files = folder.listFiles();
                if (files == null) continue;
                for (File file : files) {
                    try {
                        mods.add(file.getAbsolutePath());
                        // 对 zip 模组提前解压内置的第三方包
                        if (file.isFile() && file.getName().toLowerCase().endsWith(".zip")) {
                            ModConfigJson cfg = readModConfig(file);
                            if (cfg != null) {
                                ModRuntime.LOGGER.info("Mod config: {} v{} dependencies: {}",
                                        cfg.name != null ? cfg.name : file.getName(),
                                        cfg.version != null ? cfg.version : "?",
                                        cfg.dependencies != null ? cfg.dependencies : "[]");
                            }
                            installEmbeddedPackages(file);
                        }
                    } catch (Exception e){
                        ModRuntime.LOGGER.error("Failed to list dir \"{}\": {}", folder, e.toString());
                    }
                }
            } catch (Exception e) {
                ModRuntime.LOGGER.error("Failed to get dir list: {}", e.toString());
            }
        }
        for (String Mod : mods) {
            try {
                ModRuntime.LOGGER.info("Running mod \"{}\"", Mod);
                RunnerMain runner = new GpRunner(env_type, Mod, ModRuntime.LOGGER, gameDir, pythonPackagesPath);
                ACTIVE_RUNNERS.add(runner);

                InputStream pythonScript = getClass().getResourceAsStream("/assets/jython-mod/jython/zipimporter.py");
                runner.runScript(pythonScript);
                runner.exec("importer = ModImporter(ENV_TYPE, Script) \n"+
                            "importer.Load()"
                );
                // Intentionally not closed: Python callbacks stay live.
                ModRuntime.LOGGER.info("Successfully ran mod \"{}\"", Mod);
            } catch (Exception e) {
                ModRuntime.LOGGER.error("Failed to run mod \"{}\": {}", Mod, e.toString(), e);
            }
        }
    }

    private void ensurePythonPackagesDir() {
        File dir = new File(pythonPackagesPath);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                ModRuntime.LOGGER.debug("Created python packages directory: {}", pythonPackagesPath);
            } else {
                ModRuntime.LOGGER.warn("Failed to create python packages directory: {}", pythonPackagesPath);
            }
        }
    }

    private void installEmbeddedPackages(File zipFile) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            boolean installed = false;
            for (ZipEntry entry : zip.stream().toArray(ZipEntry[]::new)) {
                String name = entry.getName();
                if (name.startsWith("Lib/") && !entry.isDirectory()) {
                    String relative = name.substring(4); // 去掉 "Lib/"
                    Path target = Paths.get(pythonPackagesPath, relative);
                    Files.createDirectories(target.getParent());
                    try (InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    installed = true;
                }
            }
            if (installed) {
                ModRuntime.LOGGER.info("Installed embedded packages from \"{}\" to \"{}\"", zipFile.getName(), pythonPackagesPath);
            }
        } catch (Exception e) {
            ModRuntime.LOGGER.error("Failed to install embedded packages from \"{}\": {}", zipFile.getName(), e.toString());
        }
    }

    private ModConfigJson readModConfig(File zipFile) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry entry = zip.getEntry("config.json");
            if (entry == null) return null;
            try (InputStream in = zip.getInputStream(entry)) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return new Gson().fromJson(json, ModConfigJson.class);
            }
        } catch (Exception e) {
            ModRuntime.LOGGER.warn("Failed to read config.json from \"{}\": {}", zipFile.getName(), e.toString());
            return null;
        }
    }

    private void PacksPacker() {
        String[] modPaths = ModRuntime.CONFIG.modsPaths
        .replace("{gamedir}", gameDir)
        .split(";");
        ModPackBuilder.build(modPaths);
    }
}
