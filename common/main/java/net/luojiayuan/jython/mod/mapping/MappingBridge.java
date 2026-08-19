package net.luojiayuan.jython.mod.mapping;

import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.luojiayuan.jython.mod.ModRuntime;

import java.util.HashMap;
import java.util.Map;

public class MappingBridge {

    private static final Map<String, String> CLASS_MAP = new HashMap<>();
    // yarnClass#yarnMethod -> intermediary method name (ignores descriptor;
    // descriptor namespaces in tiny differ from runtime descriptors)
    private static final Map<String, String> METHOD_MAP = new HashMap<>();
    private static final Map<String, String> METHOD_MAP_REVERSE = new HashMap<>();
    // yarnClass#yarnField -> intermediary field name
    private static final Map<String, String> FIELD_MAP = new HashMap<>();
    private static final Map<String, String> FIELD_MAP_REVERSE = new HashMap<>();

    public static void init() {
        MemoryMappingTree tree = MappingLoader.getTree();
        if (tree == null) {
            ModRuntime.LOGGER.warn("[MappingBridge] Mapping tree is null, mappings not loaded!");
            return;
        }

        ModRuntime.LOGGER.debug("[MappingBridge] Parsing mappings...");

        for (Object clsObj : tree.getClasses()) {
            String yarnClass = getString(clsObj, "getName", "named");
            String interClass = getString(clsObj, "getName", "intermediary");

            if (yarnClass == null || interClass == null) {
                continue;
            }

            CLASS_MAP.put(yarnClass.replace('/', '.'), interClass.replace('/', '.'));

            Iterable<?> methods;
            try {
                java.lang.reflect.Method getMethods = clsObj.getClass().getDeclaredMethod("getMethods");
                getMethods.setAccessible(true);
                methods = (Iterable<?>) getMethods.invoke(clsObj);
            } catch (Exception e) {
                ModRuntime.LOGGER.debug("[MappingBridge] getMethods failed: {}", e.getMessage());
                continue;
            }

            if (methods == null) {
                continue;
            }

            for (Object methodObj : methods) {
                String yarnMethod = getString(methodObj, "getName", "named");
                String interMethod = getString(methodObj, "getName", "intermediary");

                if (yarnMethod == null || interMethod == null) {
                    continue;
                }

                String classKey = yarnClass.replace('/', '.');
                String interClassKey = interClass.replace('/', '.');

                String key = classKey + "#" + yarnMethod;
                METHOD_MAP.putIfAbsent(key, interMethod);

                String reverseKey = interClassKey + "#" + interMethod;
                METHOD_MAP_REVERSE.putIfAbsent(reverseKey, yarnMethod);
            }

            Iterable<?> fields;
            try {
                java.lang.reflect.Method getFields = clsObj.getClass().getDeclaredMethod("getFields");
                getFields.setAccessible(true);
                fields = (Iterable<?>) getFields.invoke(clsObj);
            } catch (Exception e) {
                ModRuntime.LOGGER.debug("[MappingBridge] getFields failed: {}", e.getMessage());
                continue;
            }

            if (fields == null) {
                continue;
            }

            for (Object fieldObj : fields) {
                String yarnField = getString(fieldObj, "getName", "named");
                String interField = getString(fieldObj, "getName", "intermediary");

                if (yarnField == null || interField == null) {
                    continue;
                }

                String classKey = yarnClass.replace('/', '.');
                String interClassKey = interClass.replace('/', '.');

                String key = classKey + "#" + yarnField;
                FIELD_MAP.putIfAbsent(key, interField);

                String reverseKey = interClassKey + "#" + interField;
                FIELD_MAP_REVERSE.putIfAbsent(reverseKey, yarnField);
            }
        }

        ModRuntime.LOGGER.debug("[MappingBridge] Loaded {} classes, {} methods, {} fields",
                CLASS_MAP.size(), METHOD_MAP.size(), FIELD_MAP.size());
    }

    private static String getString(Object obj, String methodName, String arg) {
        try {
            return (String) obj.getClass()
                    .getMethod(methodName, String.class)
                    .invoke(obj, arg);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getObfClass(String yarnClass) {
        return CLASS_MAP.get(yarnClass);
    }

    /**
     * Maps a Yarn method name to its intermediary (runtime) name for the
     * given Yarn class name. Descriptors are ignored: the intermediary name
     * is used together with runtime argument matching in {@link McReflect}.
     */
    public static String getObfMethod(String yarnClass, String yarnMethod) {
        return METHOD_MAP.get(yarnClass + "#" + yarnMethod);
    }

    /**
     * Maps an intermediary (runtime) method name back to its Yarn name.
     */
    public static String getNamedMethod(String interClass, String interMethod) {
        return METHOD_MAP_REVERSE.get(interClass + "#" + interMethod);
    }

    /**
     * Maps a Yarn field name to its intermediary (runtime) name.
     */
    public static String getObfField(String yarnClass, String yarnField) {
        return FIELD_MAP.get(yarnClass + "#" + yarnField);
    }

    /**
     * Maps an intermediary (runtime) field name back to its Yarn name.
     */
    public static String getNamedField(String interClass, String interField) {
        return FIELD_MAP_REVERSE.get(interClass + "#" + interField);
    }
}
