package net.luojiayuan.jython.mod.utils;
import java.io.File;
import java.net.URI;
// import net.minecraft.world.level.block.state.properties;
public class path {
    public static String get() {
        try {
            // 直接获取JAR文件对象
            File jarFile = new File(path.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            return jarFile.getAbsolutePath().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}