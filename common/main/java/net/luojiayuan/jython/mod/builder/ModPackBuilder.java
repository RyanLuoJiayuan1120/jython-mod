package net.luojiayuan.jython.mod.builder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.luojiayuan.jython.mod.platform.PlatformHooks;
import net.luojiayuan.jython.mod.utils.GameDirHelper;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds one pack pair per Python module.
 *
 * <p>For each module ZIP:</p>
 * <ul>
 *     <li>{@code assets/} (and root resource metadata) goes into
 *     {@code <gamedir>/jython-mod/packs/resourcepacks/<id>/};</li>
 *     <li>{@code data/} goes into
 *     {@code <gamedir>/jython-mod/packs/datapacks/<id>/}.</li>
 * </ul>
 *
 * <p>Each directory gets its own {@code pack.mcmeta} with the pack format for
 * its type. The two packs are registered as required built-in packs, so they
 * are active in every world without user interaction. The client pack-list
 * mixin hides them from the selection screens.</p>
 *
 * <p>No ZIPs are written into {@code resourcepacks/} or world
 * {@code datapacks/}; legacy combined packs from older versions are removed.</p>
 */
public final class ModPackBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger("jython-mod");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PACKS_DIR = "jython-mod/packs";
    private static final String LEGACY_RESOURCE_PACK = "JythonModAssets.zip";
    private static final String LEGACY_DATA_PACK = "JythonModData.zip";
    private static final Set<String> ROOT_RESOURCE_FILES = Set.of(
            "pack.png",
            "sounds.json",
            "gpu_warnlist.json",
            "regional_compliancies.json"
    );
    private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PACK_LIST = Pattern.compile("(?m)^([A-Za-z]+):\\[(.*?)]");

    private static boolean built;

    private ModPackBuilder() {
    }

    /**
     * Builds and registers all module packs. Safe to call more than once: the
     * Fabric loader can invoke the common and client/server entrypoints in the
     * same JVM, but pack directories and built-in profiles must only be created
     * once.
     *
     * @param modPaths semicolon-separated module directories (already expanded)
     */
    public static synchronized void build(String[] modPaths) {
        if (built) {
            LOGGER.debug("Per-module packs already built; skipping duplicate request");
            return;
        }

        Path packsRoot = GameDirHelper.getGameDir().resolve(PACKS_DIR);
        Path tempRoot = null;
        try {
            List<Path> moduleZips = findModuleZips(modPaths);
            if (moduleZips.isEmpty()) {
                LOGGER.info("No Python module ZIPs found; skipping pack generation");
                deleteRecursively(packsRoot);
                cleanupLegacy();
                built = true;
                return;
            }

            Path parent = packsRoot.getParent();
            Files.createDirectories(parent);
            tempRoot = Files.createTempDirectory(parent, ".packs-tmp-");
            Path tempResources = tempRoot.resolve("resourcepacks");
            Path tempDatas = tempRoot.resolve("datapacks");
            Files.createDirectories(tempResources);
            Files.createDirectories(tempDatas);

            List<GeneratedPack> packs = new ArrayList<>();
            Set<String> usedIds = new HashSet<>();

            for (Path zip : moduleZips) {
                String baseName = stripZipSuffix(zip.getFileName().toString());
                String id = uniqueId(baseName, usedIds);
                String displayName = readDisplayName(zip, baseName);
                Path resourceDir = tempResources.resolve(id);
                Path dataDir = tempDatas.resolve(id);

                boolean[] sections = extractModule(zip, displayName, resourceDir, dataDir);
                if (!sections[0] && !sections[1]) {
                    LOGGER.debug("Module '{}' has no valid assets/ or data/ namespace; skipping pack", zip.getFileName());
                    continue;
                }

                packs.add(new GeneratedPack(
                        id,
                        displayName,
                        sections[0] ? resourceDir : null,
                        sections[1] ? dataDir : null
                ));
                LOGGER.info("Prepared per-module pack '{}' (resource={}, data={})",
                        id, sections[0], sections[1]);
            }

            // Swap the freshly generated tree into place. Old packs are removed
            // so deleted modules do not leave stale profiles behind.
            deleteRecursively(packsRoot);
            try {
                Files.move(tempRoot, packsRoot, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempRoot, packsRoot);
            }
            tempRoot = null;

            List<GeneratedPack> finalPacks = new ArrayList<>(packs.size());
            for (GeneratedPack pack : packs) {
                finalPacks.add(new GeneratedPack(
                        pack.id(),
                        pack.displayName(),
                        pack.hasResourcePack()
                                ? packsRoot.resolve("resourcepacks").resolve(pack.id())
                                : null,
                        pack.hasDataPack()
                                ? packsRoot.resolve("datapacks").resolve(pack.id())
                                : null
                ));
            }

            cleanupLegacy();

            if (!finalPacks.isEmpty()) {
                PlatformHooks.get().registerGeneratedPacks(finalPacks);
                LOGGER.info("Generated {} per-module pack(s) under {} and registered them as required built-in packs",
                        finalPacks.size(), packsRoot);
            } else {
                LOGGER.info("No module contained a usable assets/ or data/ namespace; no packs registered");
            }
            built = true;
        } catch (Exception e) {
            LOGGER.error("Failed to build per-module packs: {}", e.toString(), e);
        } finally {
            if (tempRoot != null) {
                try {
                    deleteRecursively(tempRoot);
                } catch (IOException ignored) {
                    // Best effort cleanup of a failed build.
                }
            }
        }
    }

    private static List<Path> findModuleZips(String[] modPaths) throws IOException {
        Set<Path> uniqueDirs = new LinkedHashSet<>();
        if (modPaths != null) {
            for (String value : modPaths) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                uniqueDirs.add(Path.of(value).toAbsolutePath().normalize());
            }
        }

        List<Path> zips = new ArrayList<>();
        for (Path dir : uniqueDirs) {
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> entries = Files.list(dir)) {
                entries.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .forEach(zips::add);
            }
        }
        return zips;
    }

    private static boolean[] extractModule(Path zipFile, String displayName, Path resourceDir, Path dataDir)
            throws IOException {
        boolean sawAssets = false;
        boolean sawData = false;

        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName().replace('\\', '/');
                if (name.isEmpty() || name.startsWith("/") || name.contains("../")) {
                    continue;
                }
                if (name.equals("pack.mcmeta") || name.startsWith("META-INF/")) {
                    continue;
                }

                boolean isAsset = name.startsWith("assets/") || ROOT_RESOURCE_FILES.contains(name);
                boolean isData = name.startsWith("data/");
                if (!isAsset && !isData) {
                    continue;
                }

                Path baseDir = isAsset ? resourceDir : dataDir;
                Path target = baseDir.resolve(name).normalize();
                if (!target.startsWith(baseDir)) {
                    continue;
                }
                Files.createDirectories(target.getParent());
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }

                if (name.startsWith("assets/")) {
                    sawAssets = true;
                }
                if (name.startsWith("data/")) {
                    sawData = true;
                }
            }
        }

        boolean hasAssets = sawAssets && hasNamespace(resourceDir.resolve("assets"));
        boolean hasData = sawData && hasNamespace(dataDir.resolve("data"));

        if (hasAssets) {
            writePackMeta(resourceDir, displayName, false);
        } else {
            deleteRecursively(resourceDir);
        }
        if (hasData) {
            writePackMeta(dataDir, displayName, true);
        } else {
            deleteRecursively(dataDir);
        }
        return new boolean[]{hasAssets, hasData};
    }

    private static void writePackMeta(Path packDir, String description, boolean dataPack) throws IOException {
        PackFormat format = SharedConstants.getCurrentVersion()
                .packVersion(dataPack ? PackType.SERVER_DATA : PackType.CLIENT_RESOURCES);
        int major = format.major();
        int minor = format.minor();

        JsonObject pack = new JsonObject();
        pack.addProperty("description", description);
        JsonArray minFormat = new JsonArray();
        minFormat.add(major);
        minFormat.add(minor);
        pack.add("min_format", minFormat);
        pack.addProperty("max_format", major);

        JsonObject root = new JsonObject();
        root.add("pack", pack);
        Files.writeString(packDir.resolve("pack.mcmeta"), GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static boolean hasNamespace(Path typeDir) throws IOException {
        if (!Files.isDirectory(typeDir)) {
            return false;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(typeDir)) {
            for (Path child : stream) {
                if (Files.isDirectory(child)
                        && VALID_NAMESPACE.matcher(child.getFileName().toString()).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String readDisplayName(Path zipFile, String fallback) {
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            ZipEntry entry = zip.getEntry("config.json");
            if (entry == null) {
                return fallback;
            }
            String json = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString(json);
            if (root.isJsonObject()) {
                JsonObject object = root.getAsJsonObject();
                JsonElement name = object.get("name");
                if (name != null && name.isJsonPrimitive() && !name.getAsString().isBlank()) {
                    return name.getAsString();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not read display name from '{}': {}", zipFile.getFileName(), e.toString());
        }
        return fallback;
    }

    private static String stripZipSuffix(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".zip")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }

    private static String uniqueId(String baseName, Set<String> usedIds) {
        String sanitized = baseName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.-]", "_")
                .replaceAll("^[_.-]+|[_.-]+$", "");
        if (sanitized.isEmpty()) {
            sanitized = "module";
        }
        if (sanitized.length() > 40) {
            sanitized = sanitized.substring(0, 40);
        }

        String candidate = sanitized;
        int suffix = 2;
        while (!usedIds.add(candidate)) {
            candidate = sanitized + "-" + suffix++;
        }
        return candidate;
    }

    private static void cleanupLegacy() {
        Path gameDir = GameDirHelper.getGameDir();
        try {
            Path resourcePacks = gameDir.resolve("resourcepacks");
            Files.deleteIfExists(resourcePacks.resolve(LEGACY_RESOURCE_PACK));
            deleteRecursively(resourcePacks.resolve("JythonModAssets"));
            Files.deleteIfExists(gameDir.resolve("JythonModAssets.zip"));
            deleteRecursively(gameDir.resolve("jython_mod_temp_res"));
            deleteRecursively(gameDir.resolve("jython_mod_temp_data"));

            removePackFromOptions(gameDir.resolve("options.txt"), LEGACY_RESOURCE_PACK);
            cleanupWorldDataPacks(gameDir);
        } catch (Exception e) {
            LOGGER.debug("Legacy pack cleanup failed: {}", e.toString());
        }
    }

    private static void cleanupWorldDataPacks(Path gameDir) throws IOException {
        List<Path> worlds = new ArrayList<>();

        Path saves = gameDir.resolve("saves");
        if (Files.isDirectory(saves)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(saves)) {
                for (Path child : stream) {
                    if (Files.isDirectory(child) && Files.isRegularFile(child.resolve("level.dat"))) {
                        worlds.add(child);
                    }
                }
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(gameDir)) {
            for (Path child : stream) {
                if (!Files.isDirectory(child)
                        || child.getFileName().toString().equals("saves")
                        || child.getFileName().toString().equals("mods")
                        || child.getFileName().toString().equals("config")
                        || child.getFileName().toString().equals("logs")) {
                    continue;
                }
                if (Files.isRegularFile(child.resolve("level.dat"))) {
                    worlds.add(child);
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Could not scan game directory for worlds: {}", e.toString());
        }

        for (Path world : worlds) {
            Path datapacks = world.resolve("datapacks");
            Files.deleteIfExists(datapacks.resolve(LEGACY_DATA_PACK));
            Path oldEnabled = datapacks.resolve("enabled_packs.json");
            if (Files.isRegularFile(oldEnabled)) {
                String content = Files.readString(oldEnabled, StandardCharsets.UTF_8);
                if (content.contains(LEGACY_DATA_PACK)) {
                    Files.deleteIfExists(oldEnabled);
                }
            }
        }
    }

    private static void removePackFromOptions(Path options, String packFile) {
        if (!Files.isRegularFile(options)) {
            return;
        }
        try {
            String content = Files.readString(options, StandardCharsets.UTF_8);
            String updated = content;
            updated = removeFromPackList(updated, "resourcePacks", Set.of("file/" + packFile));
            updated = removeFromPackList(updated, "incompatibleResourcePacks", Set.of("file/" + packFile));
            if (!updated.equals(content)) {
                Files.writeString(options, updated, StandardCharsets.UTF_8);
                LOGGER.info("Removed legacy generated resource pack from options.txt");
            }
        } catch (Exception e) {
            LOGGER.debug("Could not clean options.txt: {}", e.toString());
        }
    }

    private static String removeFromPackList(String content, String key, Set<String> removedIds) {
        Matcher matcher = PACK_LIST.matcher(content);
        boolean changed = false;
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            if (!matcher.group(1).equals(key)) {
                continue;
            }

            List<String> kept = new ArrayList<>();
            boolean listChanged = false;
            for (String raw : matcher.group(2).split(",")) {
                String value = raw.trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                if (value.isEmpty()) {
                    continue;
                }
                if (removedIds.contains(value)) {
                    listChanged = true;
                } else {
                    kept.add(value);
                }
            }

            if (!listChanged) {
                continue;
            }

            result.append(content, lastEnd, matcher.start());
            result.append(key).append(":[");
            for (int i = 0; i < kept.size(); i++) {
                if (i > 0) {
                    result.append(',');
                }
                result.append('"').append(kept.get(i)).append('"');
            }
            result.append(']');
            lastEnd = matcher.end();
            changed = true;
        }

        if (!changed) {
            return content;
        }
        result.append(content, lastEnd, content.length());
        return result.toString();
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(child -> {
                try {
                    Files.deleteIfExists(child);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        } catch (java.io.UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
