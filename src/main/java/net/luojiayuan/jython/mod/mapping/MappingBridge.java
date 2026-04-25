package net.luojiayuan.jython.mod.mapping;

import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.util.HashMap;
import java.util.Map;

public class MappingBridge {

    private static final Map<String, String> CLASS_MAP = new HashMap<>();
    private static final Map<String, String> METHOD_MAP = new HashMap<>();
    private static final Map<String, String> METHOD_MAP_REVERSE = new HashMap<>();

    public static void init() {
        MemoryMappingTree tree = MappingLoader.getTree();
        if (tree == null) {
            System.out.println("[MappingBridge] tree is null, mapping not loaded!");
            return;
        }

        System.out.println("[MappingBridge] 开始解析 mapping...");

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
                System.out.println("[MappingBridge] getMethods 失败: " + e.getMessage());
                continue;
            }

            if (methods == null) {
                continue;
            }

            for (Object methodObj : methods) {
                String yarnMethod = getString(methodObj, "getName", "named");
                String interMethod = getString(methodObj, "getName", "intermediary");
                String desc = getString(methodObj, "getDescriptor", "intermediary");

                if (yarnMethod == null || interMethod == null || desc == null) {
                    continue;
                }

                String classKey = yarnClass.replace('/', '.');
                String interClassKey = interClass.replace('/', '.');

                String key = classKey + "#" + yarnMethod + desc;
                METHOD_MAP.put(key, interMethod);

                String reverseKey = interClassKey + "#" + interMethod + desc;
                METHOD_MAP_REVERSE.put(reverseKey, yarnMethod);
            }
        }

        System.out.println("[MappingBridge] 完成，共加载类: " + CLASS_MAP.size() + ", 方法: " + METHOD_MAP.size());
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

    public static String getObfMethod(String yarnClass, String yarnMethod, String desc) {
        return METHOD_MAP.get(yarnClass + "#" + yarnMethod + desc);
    }

    public static String getNamedMethod(String interClass, String interMethod, String desc) {
        return METHOD_MAP_REVERSE.get(interClass + "#" + interMethod + desc);
    }
}
