package net.luojiayuan.jython.mod.api;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 反射辅助：供 Python 侧 {@code jython_api} 包装器调用已注册的 {@code Class} 对象。
 *
 * <p>与 {@link net.luojiayuan.jython.mod.mapping.McReflect} 的区别：这里直接持有
 * {@code Class} 对象引用，不按类名解析——因此不受 Paper 插件类加载器隔离影响，
 * 也无需 Minecraft 类名映射（第三方模组的类与方法名不会被 remap）。
 *
 * <p>支撑的能力：静态字段读取、静态方法调用、构造、嵌套类查找。
 */
public final class ApiReflect {

    private ApiReflect() {
    }

    /** 判断对象是否为 Java Class 对象（Python 侧包装用）。 */
    public static boolean isClass(Object obj) {
        return obj instanceof Class<?>;
    }

    /** 按简单名查找嵌套类（含私有嵌套类），找不到返回 null。 */
    public static Class<?> nestedClass(Class<?> clazz, String simpleName) {
        if (clazz == null || simpleName == null) {
            return null;
        }
        for (Class<?> c : clazz.getClasses()) {
            if (c.getSimpleName().equals(simpleName)) {
                return c;
            }
        }
        for (Class<?> c : clazz.getDeclaredClasses()) {
            if (c.getSimpleName().equals(simpleName)) {
                return c;
            }
        }
        return null;
    }

    /**
     * 读取静态字段；字段不存在（或不可访问）时返回 null。
     *
     * <p>注意：不抛受检异常——GraalPy 边界上受检异常不会被 Python
     * {@code except Exception} 捕获，包装器靠"null 表示无此字段"做分支。
     */
    public static Object getStaticFieldOrNull(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** 调用静态方法（按方法名 + 参数数量 + 可赋值性匹配重载）。 */
    public static Object callStatic(Class<?> clazz, String methodName, Object... args) throws Exception {
        Method matched = findMethod(clazz, methodName, args);
        if (matched == null) {
            throw new RuntimeException("Static method not found: " + clazz.getName() + "." + methodName);
        }
        return invokeMethod(matched, null, args);
    }

    /** 调用实例方法（实例对象由 Python 侧直接持有）。 */
    public static Object callInstance(Object instance, String methodName, Object... args) throws Exception {
        Method matched = findMethod(instance.getClass(), methodName, args);
        if (matched == null) {
            throw new RuntimeException(
                    "Method not found: " + instance.getClass().getName() + "." + methodName);
        }
        return invokeMethod(matched, instance, args);
    }

    /** 构造实例。 */
    public static Object construct(Class<?> clazz, Object... args) throws Exception {
        Constructor<?> matched = findConstructor(clazz, args);
        if (matched == null) {
            throw new RuntimeException("Constructor not found: " + clazz.getName());
        }
        matched.setAccessible(true);
        Class<?>[] paramTypes = matched.getParameterTypes();
        Object[] unpacked = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            unpacked[i] = narrowArg(unpackPolyglotValue(args[i], paramTypes[i]), paramTypes[i]);
        }
        return matched.newInstance(unpacked);
    }

    // ------------------------------------------------------------------
    // 内部：方法 / 构造匹配
    // ------------------------------------------------------------------

    private static Method findMethod(Class<?> clazz, String name, Object[] args) {
        Method matched = null;
        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(name)) {
                continue;
            }
            if (!isArgsMatch(m.getParameterTypes(), args)) {
                continue;
            }
            if (matched == null) {
                matched = m;
            } else if (isMoreSpecific(m.getParameterTypes(), matched.getParameterTypes())) {
                matched = m;
            }
        }
        return matched;
    }

    private static Constructor<?> findConstructor(Class<?> clazz, Object[] args) {
        Constructor<?> matched = null;
        for (Constructor<?> c : clazz.getConstructors()) {
            if (!isArgsMatch(c.getParameterTypes(), args)) {
                continue;
            }
            if (matched == null) {
                matched = c;
            } else if (isMoreSpecific(c.getParameterTypes(), matched.getParameterTypes())) {
                matched = c;
            }
        }
        return matched;
    }

    private static Object invokeMethod(Method m, Object instance, Object[] args) throws Exception {
        m.setAccessible(true);
        Class<?>[] paramTypes = m.getParameterTypes();
        Object[] unpacked = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            unpacked[i] = narrowArg(unpackPolyglotValue(args[i], paramTypes[i]), paramTypes[i]);
        }
        try {
            return m.invoke(instance, unpacked);
        } catch (Exception e) {
            throw new RuntimeException(describeInvoke(m, unpacked), e);
        }
    }

    private static String describeInvoke(Method m, Object[] args) {
        StringBuilder sb = new StringBuilder("invoke failed: ");
        sb.append(m.getDeclaringClass().getName()).append(".").append(m.getName()).append("(");
        for (int i = 0; i < m.getParameterTypes().length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(m.getParameterTypes()[i].getSimpleName());
        }
        sb.append(") with args [");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(args[i] == null ? "null" : args[i].getClass().getName());
        }
        return sb.append("]").toString();
    }

    /** 解开 GraalPy 传递的 Value 包装，并按目标类型收窄。 */
    private static Object unpackPolyglotValue(Object obj, Class<?> targetType) {
        if (obj == null) {
            return null;
        }
        Class<?> cls = obj.getClass();
        String name = cls.getName();
        if (!name.contains("polyglot") && !name.contains("truffle")) {
            return obj;
        }
        try {
            try {
                Method asHost = cls.getMethod("asHostObject");
                Object host = asHost.invoke(obj);
                if (host != null) {
                    return host;
                }
            } catch (Exception ignored) {
                // fall through
            }
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
            if (targetType == String.class) {
                return cls.getMethod("asString").invoke(obj);
            }
            Class<?> boxed = targetType;
            if (targetType == float.class) {
                boxed = Float.class;
            } else if (targetType == double.class) {
                boxed = Double.class;
            } else if (targetType == int.class) {
                boxed = Integer.class;
            } else if (targetType == long.class) {
                boxed = Long.class;
            } else if (targetType == boolean.class) {
                boxed = Boolean.class;
            }
            return cls.getMethod("as", Class.class).invoke(obj, boxed);
        } catch (Exception e) {
            return obj;
        }
    }

    private static Object narrowArg(Object arg, Class<?> targetType) {
        if (arg == null) {
            return null;
        }
        if (targetType == char.class) {
            if (arg instanceof Number) {
                return (char) ((Number) arg).intValue();
            }
            if (arg instanceof Character) {
                return arg;
            }
            if (arg instanceof String) {
                String s = (String) arg;
                if (s.length() != 1) {
                    throw new IllegalArgumentException(
                            "Expected single character but got string of length " + s.length());
                }
                return s.charAt(0);
            }
            return arg;
        }
        if (arg instanceof Number) {
            Number n = (Number) arg;
            if (targetType == float.class) {
                return n.floatValue();
            }
            if (targetType == double.class) {
                return n.doubleValue();
            }
            if (targetType == int.class) {
                return n.intValue();
            }
            if (targetType == long.class) {
                return n.longValue();
            }
            if (targetType == short.class) {
                return n.shortValue();
            }
            if (targetType == byte.class) {
                return n.byteValue();
            }
        }
        return arg;
    }

    private static boolean isArgsMatch(Class<?>[] paramTypes, Object[] args) {
        if (args == null) {
            return paramTypes.length == 0;
        }
        if (paramTypes.length != args.length) {
            return false;
        }
        for (int i = 0; i < paramTypes.length; i++) {
            if (args[i] == null) {
                continue;
            }
            if (!isAssignable(paramTypes[i], args[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAssignable(Class<?> param, Class<?> arg) {
        String argName = arg.getName();
        if (argName.contains("polyglot") || argName.contains("truffle")) {
            return true;
        }
        if (Number.class.isAssignableFrom(arg)) {
            return param.isPrimitive() && param != boolean.class || param.isAssignableFrom(arg);
        }
        if (param.isPrimitive()) {
            if (param == boolean.class) {
                return arg == Boolean.class;
            }
            if (param == char.class) {
                return arg == Character.class || arg == String.class;
            }
            return arg == Character.class;
        }
        return param.isAssignableFrom(arg);
    }

    private static boolean isMoreSpecific(Class<?>[] candidate, Class<?>[] reference) {
        if (candidate.length != reference.length) {
            return false;
        }
        boolean strictlyNarrower = false;
        for (int i = 0; i < candidate.length; i++) {
            if (candidate[i] == reference[i]) {
                continue;
            }
            if (!reference[i].isAssignableFrom(candidate[i])) {
                return false;
            }
            strictlyNarrower = true;
        }
        return strictlyNarrower;
    }
}
