package net.luojiayuan.jython.mod.mapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;

public class McReflect {

    public static Object call(
            String yarnClass,
            String yarnMethod,
            Object instance,
            Object... args
    ) throws Exception {

        boolean devEnv = FabricLoader.getInstance().isDevelopmentEnvironment();
        
        String className;
        if (devEnv) {
            className = yarnClass;
        } else {
            className = MappingBridge.getObfClass(yarnClass);
        }
        
        System.out.println("DEBUG: yarn=" + yarnClass + " -> className=" + className);
        if (className == null) {
            throw new RuntimeException("找不到映射: " + yarnClass);
        }
        Class<?> clazz = Class.forName(className);
        
        // 尝试字段访问（静态字段，无参数）
        if (instance == null && (args == null || args.length == 0)) {
            try {
                Field field = clazz.getField(yarnMethod);
                return field.get(null);
            } catch (NoSuchFieldException e) {
                // 不是字段，继续找方法
            }
        }
        
        Method matched = null;

        for (Method m : clazz.getMethods()) {
            String methodName;
            if (devEnv) {
                methodName = yarnMethod;
            } else {
                methodName = MappingBridge.getObfMethod(
                        yarnClass,
                        yarnMethod,
                        m.getParameterCount() == 0 ? "()V" : "(...)"
                );
            }
            
            if (methodName != null && m.getName().equals(methodName)) {
                if (m.getParameterCount() == args.length) {
                    if (matched != null) {
                        throw new RuntimeException("方法重载冲突：" + yarnMethod);
                    }
                    matched = m;
                }
            }
        }

        if (matched == null) {
            throw new RuntimeException("找不到方法：" + yarnMethod);
        }

        matched.setAccessible(true);
        return matched.invoke(instance, args);
    }
}