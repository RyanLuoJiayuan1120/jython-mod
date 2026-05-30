package net.luojiayuan.jython.mod.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;

public class McReflect {

    /**
     * 通过 yarn 类名获取对应的 obf 类名字符串（自动处理混淆映射）。
     * 供 GraalPy 使用：GraalPy 的 java.type() 需要字符串类名。
     */
    public static String getClassName(String yarnClass) throws Exception {
        boolean devEnv = FabricLoader.getInstance().isDevelopmentEnvironment();
        String className;
        if (devEnv) {
            className = yarnClass;
        } else {
            className = MappingBridge.getObfClass(yarnClass);
        }
        if (className == null) {
            throw new RuntimeException("找不到映射: " + yarnClass);
        }
        return className;
    }

    /**
     * 通过 yarn 类名获取对应的 Java Class 对象（自动处理混淆映射）。
     * 供 Jython import 钩子使用，Jython 可以直接使用 Class 对象。
     */
    public static Class<?> getClass(String yarnClass) throws Exception {
        return Class.forName(getClassName(yarnClass));
    }

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

        // System.out.println("DEBUG: yarn=" + yarnClass + " -> className=" + className);
        if (className == null) {
            throw new RuntimeException("找不到映射: " + yarnClass);
        }
        Class<?> clazz = Class.forName(className);

        // 处理构造函数
        if ("<init>".equals(yarnMethod)) {
            Constructor<?> matched = null;
            for (Constructor<?> c : clazz.getConstructors()) {
                if (isArgsMatch(c.getParameterTypes(), args)) {
                    if (matched != null) {
                        throw new RuntimeException("构造函数重载冲突：" + yarnClass);
                    }
                    matched = c;
                }
            }
            if (matched == null) {
                throw new RuntimeException("找不到构造函数：" + yarnClass);
            }
            matched.setAccessible(true);
            Class<?>[] paramTypes = matched.getParameterTypes();
            Object[] unpackedArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                unpackedArgs[i] = unpackPolyglotValue(args[i], paramTypes[i]);
            }
            for (int i = 0; i < unpackedArgs.length; i++) {
                if (unpackedArgs[i] instanceof Number) {
                    Number n = (Number) unpackedArgs[i];
                    if (paramTypes[i] == float.class) unpackedArgs[i] = n.floatValue();
                    else if (paramTypes[i] == double.class) unpackedArgs[i] = n.doubleValue();
                    else if (paramTypes[i] == int.class) unpackedArgs[i] = n.intValue();
                    else if (paramTypes[i] == long.class) unpackedArgs[i] = n.longValue();
                    else if (paramTypes[i] == short.class) unpackedArgs[i] = n.shortValue();
                    else if (paramTypes[i] == byte.class) unpackedArgs[i] = n.byteValue();
                }
            }
            return matched.newInstance(unpackedArgs);
        }

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
                String desc = getDescriptor(m.getParameterTypes(), m.getReturnType());
                String named = MappingBridge.getNamedMethod(className, m.getName(), desc);
                methodName = named != null && named.equals(yarnMethod) ? m.getName() : null;
            }

            if (methodName != null && m.getName().equals(methodName)) {
                if (isArgsMatch(m.getParameterTypes(), args)) {
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
        
        // 解包 GraalPy Value 对象
        Object unpackedInstance = unpackPolyglotValue(instance, clazz);
        Class<?>[] paramTypes = matched.getParameterTypes();
        Object[] unpackedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            unpackedArgs[i] = unpackPolyglotValue(args[i], paramTypes[i]);
        }
        
        // GraalPy 可能传入 Double 但方法需要 float，进行数值窄化转换
        for (int i = 0; i < unpackedArgs.length; i++) {
            if (unpackedArgs[i] instanceof Number) {
                Number n = (Number) unpackedArgs[i];
                if (paramTypes[i] == float.class) unpackedArgs[i] = n.floatValue();
                else if (paramTypes[i] == double.class) unpackedArgs[i] = n.doubleValue();
                else if (paramTypes[i] == int.class) unpackedArgs[i] = n.intValue();
                else if (paramTypes[i] == long.class) unpackedArgs[i] = n.longValue();
                else if (paramTypes[i] == short.class) unpackedArgs[i] = n.shortValue();
                else if (paramTypes[i] == byte.class) unpackedArgs[i] = n.byteValue();
            }
        }
        
        try {
            return matched.invoke(unpackedInstance, unpackedArgs);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("invoke failed: ").append(clazz.getName()).append(".").append(matched.getName()).append("(");
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(paramTypes[i].getSimpleName());
            }
            sb.append(") with args [");
            for (int i = 0; i < unpackedArgs.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(unpackedArgs[i] == null ? "null" : unpackedArgs[i].getClass().getName());
            }
            sb.append("]");
            throw new RuntimeException(sb.toString(), e);
        }
    }
    
    private static Object unpackPolyglotValue(Object obj, Class<?> targetType) {
        if (obj == null) return null;
        Class<?> cls = obj.getClass();
        String name = cls.getName();
        if (name.contains("polyglot") || name.contains("truffle")) {
            try {
                // 1. 尝试 asHostObject（Java 对象代理）
                try {
                    Method asHost = cls.getMethod("asHostObject");
                    Object host = asHost.invoke(obj);
                    if (host != null) return host;
                } catch (Exception ignored) {}
                
                // 2. 数值直接转换
                if (targetType == float.class || targetType == Float.class) {
                    return cls.getMethod("asFloat").invoke(obj);
                }
                if (targetType == double.class || targetType == Double.class) {
                    return cls.getMethod("asDouble").invoke(obj);
                }
                if (targetType == int.class || targetType == Integer.class) {
                    return cls.getMethod("asInt").invoke(obj);
                }
                if (targetType == long.class || targetType == Long.class) {
                    return cls.getMethod("asLong").invoke(obj);
                }
                if (targetType == boolean.class || targetType == Boolean.class) {
                    return cls.getMethod("asBoolean").invoke(obj);
                }
                
                // 3. 通用 as 转换（包装类）
                Class<?> boxed = targetType;
                if (targetType == float.class) boxed = Float.class;
                else if (targetType == double.class) boxed = Double.class;
                else if (targetType == int.class) boxed = Integer.class;
                else if (targetType == long.class) boxed = Long.class;
                else if (targetType == boolean.class) boxed = Boolean.class;
                else if (targetType == byte.class) boxed = Byte.class;
                else if (targetType == short.class) boxed = Short.class;
                else if (targetType == char.class) boxed = Character.class;
                return cls.getMethod("as", Class.class).invoke(obj, boxed);
            } catch (Exception e) {
                // 解包失败，保持原样
            }
        }
        return obj;
    }

    private static String getDescriptor(Class<?>[] params, Class<?> ret) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (Class<?> p : params) {
            sb.append(getTypeDesc(p));
        }
        sb.append(')');
        sb.append(getTypeDesc(ret));
        return sb.toString();
    }

    private static String getTypeDesc(Class<?> cls) {
        if (cls == void.class) return "V";
        if (cls == boolean.class) return "Z";
        if (cls == byte.class) return "B";
        if (cls == char.class) return "C";
        if (cls == short.class) return "S";
        if (cls == int.class) return "I";
        if (cls == long.class) return "J";
        if (cls == float.class) return "F";
        if (cls == double.class) return "D";
        if (cls.isArray()) return "[" + getTypeDesc(cls.getComponentType());
        return "L" + cls.getName().replace('.', '/') + ";";
    }

    private static boolean isArgsMatch(Class<?>[] paramTypes, Object[] args) {
        if (args == null) return paramTypes.length == 0;
        if (paramTypes.length != args.length) return false;
        for (int i = 0; i < paramTypes.length; i++) {
            if (args[i] == null) continue;
            if (!isAssignable(paramTypes[i], args[i].getClass())) return false;
        }
        return true;
    }

    private static boolean isAssignable(Class<?> param, Class<?> arg) {
        // GraalPy 的 Value / Number 对象由 Polyglot 在调用时自动解包
        String argName = arg.getName();
        if (argName.contains("polyglot") || argName.contains("truffle")) return true;
        if (Number.class.isAssignableFrom(arg)) {
            if (param == float.class || param == double.class || param == int.class || param == long.class) return true;
        }
        if (param.isPrimitive()) {
            if (param == boolean.class && arg == Boolean.class) return true;
            if (param == byte.class && arg == Byte.class) return true;
            if (param == char.class && arg == Character.class) return true;
            if (param == short.class && arg == Short.class) return true;
            if (param == int.class && arg == Integer.class) return true;
            if (param == long.class && arg == Long.class) return true;
            if (param == float.class && arg == Float.class) return true;
            if (param == double.class && arg == Double.class) return true;
            return false;
        }
        return param.isAssignableFrom(arg);
    }
}
