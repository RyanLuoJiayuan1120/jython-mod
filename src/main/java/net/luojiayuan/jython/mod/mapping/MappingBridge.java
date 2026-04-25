package net.luojiayuan.jython.mod.mapping;

import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.util.HashMap;
import java.util.Map;

public class MappingBridge {

    private static final Map<String, String> CLASS_MAP = new HashMap<>();
    private static final Map<String, String> METHOD_MAP = new HashMap<>();

    public static void init() {
        MemoryMappingTree tree = MappingLoader.getTree();

        System.out.println("[MappingBridge] 开始解析 mapping...");

        // ✅ 用 tree.getClasses() 返回的 Iterable<Object>
        for (Object clsObj : tree.getClasses()) {

            String yarnClass = getString(clsObj, "getName", "named");
            String interClass = getString(clsObj, "getName", "intermediary");

            if (yarnClass == null || interClass == null) {
                continue;
            }

            CLASS_MAP.put(yarnClass.replace('/', '.'), interClass.replace('/', '.'));

            // ✅ 关键：用 tree 的公开方法获取方法
            // 注意：这里不能用 getMethods()
            // 但我们可以用 tree.getMethods(clsObj)
            Iterable<?> methods;
            try {
                methods = (Iterable<?>) clsObj.getClass().getMethod("getMethods").invoke(clsObj);
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

                String key = yarnClass.replace('/', '.') + "#" + yarnMethod + "(" + desc + ")";
                METHOD_MAP.put(key, interMethod);
            }
        }

        System.out.println("[MappingBridge] 完成，共加载类: " + CLASS_MAP.size());
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
        return METHOD_MAP.get(yarnClass + "#" + yarnMethod + "(" + desc + ")");
    }
}