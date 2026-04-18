package net.luojiayuan.jython.mod.builder;

/*
* 本文件是使用元宝生成的（懒得重写了）
*             ↑ 豪用！！
*/

import net.luojiayuan.jython.mod.utils.DatapackHelper;
import net.luojiayuan.jython.mod.utils.GameDirHelper;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ModDataPackBuilder {

    public static void build(String[] modPaths) {
        String gameDir = GameDirHelper.getGameDirPath();
        String tempDataDir = gameDir + "/jython_mod_temp_data";
        // String finalDataPack = gameDir + "/resourcepacks/JythonModData.zip";

        File tempDir = new File(tempDataDir);
        deleteDirectory(tempDir);
        tempDir.mkdirs();

        boolean dataCollected = false;

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
                        if (name.startsWith("data/")) {
                            extractEntry(zip, entry, tempDataDir);
                            extracted = true;
                        }
                    }

                    if (extracted) {
                        System.out.println("Extracted data from: " + file.getName());
                        dataCollected = true;
                    }

                } catch (Exception e) {
                    System.err.println("Failed to extract data from " + file.getName() + ": " + e.getMessage());
                }
            }
        }

        if (!dataCollected) {
            deleteDirectory(tempDir);
            System.out.println("No data found, skipping datapack creation");
            return;
        }

        try {
            writePackMeta(tempDataDir, "Jython Mod Data - Generated from mod data");
        } catch (IOException e) {
            System.err.println("Failed to write datapack pack.mcmeta: " + e.getMessage());
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            addFolderToZip(tempDir, tempDir, zos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<File> worldDirs = findWorldDirectories(gameDir);

        int copied = 0;
        for (File world : worldDirs) {
            File datapacks = new File(world, "datapacks");
            datapacks.mkdirs();

            File datapack = new File(datapacks, "JythonModData.zip");
            try (FileOutputStream fos = new FileOutputStream(datapack)) {
                fos.write(baos.toByteArray());
                copied++;
                System.out.println("Copied datapack to: " + world.getName());
            } catch (IOException e) {
                System.err.println("Failed to copy datapack to " + world.getName());
            }
        }

        if (copied > 0) {
            DatapackHelper.enableDatapack("JythonModData.zip");
            System.out.println("Datapack created and copied to " + copied + " save(s)");
        }

        deleteDirectory(tempDir);
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

    private static List<File> findWorldDirectories(String gameDir) {
        List<File> worlds = new ArrayList<>();

        // saves 目录
        File saves = new File(gameDir, "saves");
        if (saves.exists()) {
            File[] savesList = saves.listFiles();
            if (savesList != null) {
                for (File f : savesList) {
                    if (f.isDirectory() && new File(f, "level.dat").exists()) {
                        worlds.add(f);
                    }
                }
            }
        }

        // 服务器世界（直接在 gameDir 下）
        File[] rootFiles = new File(gameDir).listFiles();
        if (rootFiles != null) {
            for (File f : rootFiles) {
                if (f.isDirectory() && new File(f, "level.dat").exists()) {
                    worlds.add(f);
                }
            }
        }

        return worlds;
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