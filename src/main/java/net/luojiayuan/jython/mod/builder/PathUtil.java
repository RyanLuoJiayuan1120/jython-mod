package net.luojiayuan.jython.mod.builder;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class PathUtil {

    public static Set<String> deduplicatePaths(String[] paths) {
        Set<String> result = new HashSet<>();
        for (String p : paths) {
            if (p != null && !p.isEmpty()) {
                result.add(new File(p).getAbsolutePath());
            }
        }
        return result;
    }

    public static boolean isZipFile(String name) {
        return name != null && name.toLowerCase().endsWith(".zip");
    }
}