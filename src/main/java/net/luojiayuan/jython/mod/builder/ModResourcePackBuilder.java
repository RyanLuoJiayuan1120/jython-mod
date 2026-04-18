package net.luojiayuan.jython.mod.builder;

/*
* 本文件是使用元宝生成的（懒得重写了）
*             ↑ 豪用！！
*/

import net.luojiayuan.jython.mod.utils.GameDirHelper;
import net.luojiayuan.jython.mod.utils.ResourcePackHelper;

import java.io.*;
import java.nio.file.Files;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ModResourcePackBuilder {

    private static final String[] RESOURCE_FOLDERS = {
            "assets", "atlases", "blockstates", "equipment", "font",
            "items", "lang", "models", "particles", "post_effect",
            "sounds", "shaders", "texts", "textures", "waypoint_style"
    };

    private static final String[] RESOURCE_FILES = {
            "gpu_warnlist.json", "regional_compliancies.json", "sounds.json"
    };

    public static void build(String envType, String[] modPaths) {
        if (!"server".equals(envType)) {
            // 服务器环境不需要生成资源包
            return;
        }

        String gameDir = GameDirHelper.getGameDirPath();
        String tempResDir = gameDir + "/jython_mod_temp_res";
        String finalResPack = gameDir + "/resourcepacks/JythonModAssets.zip";

        File tempDir = new File(tempResDir);
        deleteDirectory(tempDir);
        tempDir.mkdirs();

        boolean resourcesCollected = false;

        Set<String> paths = PathUtil.deduplicatePaths(modPaths);

        for (String basePath : paths) {
            File dir = new File(basePath);
            if (!dir.isDirectory()) continue;

            File[] files = dir.listFiles();
            if (files == null) continue;

            for (File file : files) {
                if (!PathUtil.isZipFile(file.getName())) continue;

                try (ZipFile zip = new ZipFile(file)) {
                    boolean extracted = false;

                    for (ZipEntry entry : zip.stream().toArray(ZipEntry[]::new)) {
                        if (entry.isDirectory()) continue;

                        String name = entry.getName();

                        if (isResourceEntry(name)) {
                            extractEntry(zip, entry, tempResDir);
                            extracted = true;
                        }
                    }

                    if (extracted) {
                        System.out.println("Extracted resources from: " + file.getName());
                        resourcesCollected = true;
                    }

                } catch (Exception e) {
                    System.err.println("Failed to extract resources from " + file.getName() + ": " + e.getMessage());
                }
            }
        }

        if (resourcesCollected) {
            try {
                writePackMeta(tempResDir, "Jython Mod Resources - Generated from mod resources");
            } catch (IOException e) {
                System.err.println("Failed to write pack.mcmeta: " + e.getMessage());
            }
            createZip(tempResDir, finalResPack);
            ResourcePackHelper.enableResourcePack("JythonModAssets.zip");
            System.out.println("Resource pack created successfully!");
        } else {
            System.out.println("No resources found, skipping resource pack creation");
        }

        deleteDirectory(tempDir);
    }

    private static boolean isResourceEntry(String name) {
        for (String folder : RESOURCE_FOLDERS) {
            if (name.startsWith(folder + "/") || name.equals(folder)) {
                return true;
            }
        }
        for (String file : RESOURCE_FILES) {
            if (name.equals(file)) {
                return true;
            }
        }
        return false;
    }

    private static void extractEntry(ZipFile zip, ZipEntry entry, String destDir) throws IOException {
        File outFile = new File(destDir, entry.getName());
        outFile.getParentFile().mkdirs();
        try (InputStream in = zip.getInputStream(entry);
             OutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }

    private static void writePackMeta(String dir, String description) throws IOException {
        File meta = new File(dir, "pack.mcmeta");
        try (PrintWriter pw = new PrintWriter(meta)) {
            pw.println("{");
            pw.println("  \"pack\": {");
            pw.println("    \"pack_format\": 48,");
            pw.println("    \"description\": \"" + description + "\"");
            pw.println("  }");
            pw.println("}");
        }
    }

    private static void createZip(String sourceDir, String zipPath) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath))) {
            File base = new File(sourceDir);
            addFolderToZip(base, base, zos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addFolderToZip(File root, File folder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            String relative = root.toURI().relativize(file.toURI()).getPath();
            if (file.isDirectory()) {
                addFolderToZip(root, file, zos);
            } else {
                ZipEntry entry = new ZipEntry(relative);
                zos.putNextEntry(entry);
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) deleteDirectory(f);
                    else f.delete();
                }
            }
            dir.delete();
        }
    }
}