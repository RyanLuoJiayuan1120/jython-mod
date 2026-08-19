package net.luojiayuan.jython.mod.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.luojiayuan.jython.mod.platform.PlatformHooks;

public class McReflect {

    /**
     * Returns the runtime class name for a given Yarn class name.
     * Used by GraalPy: java.type() requires a string class name.
     */
    public static String getClassName(String yarnClass) throws Exception {
        if (usesDirectNames()) {
            return yarnClass;
        }
        String className = resolveMappedClassName(yarnClass);
        if (className == null) {
            throw new RuntimeException("Mapping not found: " + yarnClass);
        }
        return className;
    }

    /**
     * Resolves the runtime (intermediary) class name for a Yarn class name,
     * handling inner classes written with '.' (Item.Properties -> Item$Properties).
     *
     * @return the intermediary class name, or {@code null} when unmapped
     */
    private static String resolveMappedClassName(String yarnClass) {
        String className = MappingBridge.getObfClass(yarnClass);
        if (className == null && yarnClass.contains(".")) {
            // Inner classes may be written with '.' in Python (Item.Properties)
            // while mappings use '$' (Item$Properties). Try right-to-left.
            String candidate = yarnClass;
            int idx = candidate.lastIndexOf('.');
            while (idx > 0) {
                candidate = candidate.substring(0, idx) + '$' + candidate.substring(idx + 1);
                className = MappingBridge.getObfClass(candidate);
                if (className != null) {
                    break;
                }
                idx = candidate.lastIndexOf('.');
            }
        }
        return className;
    }

    /**
     * Resolves a field by its Yarn name. In production Fabric the runtime
     * field name is intermediary (field_xxx), so the Yarn name is translated
     * via the mapping bridge; dev / NeoForge compare directly.
     */
    private static Field findField(String yarnClass, Class<?> clazz, String yarnFieldName)
            throws NoSuchFieldException {
        String runtimeFieldName;
        if (usesDirectNames()) {
            runtimeFieldName = yarnFieldName;
        } else {
            runtimeFieldName = MappingBridge.getObfField(yarnClass, yarnFieldName);
            if (runtimeFieldName == null) {
                runtimeFieldName = yarnFieldName;
            }
        }
        return clazz.getField(runtimeFieldName);
    }

    /**
     * Whether runtime class names can be used directly without mapping.
     * True in dev environments (Yarn names) and on NeoForge production
     * (official Mojang names, which match the Yarn names on 1.21.11).
     */
    private static boolean usesDirectNames() {
        return PlatformHooks.get().isDevelopmentEnvironment()
                || PlatformHooks.get().usesOfficialMappings();
    }

    /**
     * Returns the Java Class object for a given Yarn class name.
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

        boolean devEnv = usesDirectNames();

        String className;
        if (devEnv) {
            className = yarnClass;
        } else {
            className = resolveMappedClassName(yarnClass);
        }


        if (className == null) {
            throw new RuntimeException("找不到映射: " + yarnClass);
        }
        Class<?> clazz = Class.forName(className);

        // Handle constructors
        if ("<init>".equals(yarnMethod)) {
            Constructor<?> matched = null;
            for (Constructor<?> c : clazz.getConstructors()) {
                if (isArgsMatch(c.getParameterTypes(), args)) {
                    if (matched != null) {
                        if (isMoreSpecific(c.getParameterTypes(), matched.getParameterTypes())) {
                            matched = c;
                        } else if (!isMoreSpecific(matched.getParameterTypes(), c.getParameterTypes())) {
                            throw new RuntimeException("Constructor overload conflict: " + yarnClass);
                        }
                    } else {
                        matched = c;
                    }
                }
            }
            if (matched == null) {
                throw new RuntimeException("Constructor not found: " + yarnClass);
            }
            matched.setAccessible(true);
            Class<?>[] paramTypes = matched.getParameterTypes();
            Object[] unpackedArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                unpackedArgs[i] = narrowArg(unpackPolyglotValue(args[i], paramTypes[i]), paramTypes[i]);
            }
            return matched.newInstance(unpackedArgs);
        }

        // Try field access (static fields, no args)
        if (instance == null && (args == null || args.length == 0)) {
            try {
                Field field = findField(yarnClass, clazz, yarnMethod);
                if (field != null) {
                    return field.get(null);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // Not a field, continue to look for methods
            }
        }

        Method matched = null;

        // Runtime method names differ by platform:
        //  - dev / NeoForge: Yarn (named) names, compare directly
        //  - Fabric production: intermediary names, reverse-map to Yarn first
        boolean directNames = usesDirectNames();
        for (Method m : clazz.getMethods()) {
            String methodName = m.getName();
            if (!directNames) {
                String named = MappingBridge.getNamedMethod(className, methodName);
                if (named == null || !named.equals(yarnMethod)) {
                    continue;
                }
            } else if (!methodName.equals(yarnMethod)) {
                continue;
            }

            if (isArgsMatch(m.getParameterTypes(), args)) {
                if (matched != null) {
                    if (isMoreSpecific(m.getParameterTypes(), matched.getParameterTypes())) {
                        matched = m;
                    } else if (!isMoreSpecific(matched.getParameterTypes(), m.getParameterTypes())) {
                        throw new RuntimeException("Method overload conflict: " + yarnMethod);
                    }
                } else {
                    matched = m;
                }
            }
        }

        if (matched == null) {
            throw new RuntimeException("Method not found: " + yarnMethod);
        }

        matched.setAccessible(true);
        
        // Unpack GraalPy Value objects and narrow numeric types
        Object unpackedInstance = unpackPolyglotValue(instance, clazz);
        Class<?>[] paramTypes = matched.getParameterTypes();
        Object[] unpackedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            unpackedArgs[i] = narrowArg(unpackPolyglotValue(args[i], paramTypes[i]), paramTypes[i]);
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
                // 1. Try asHostObject (Java object proxy)
                try {
                    Method asHost = cls.getMethod("asHostObject");
                    Object host = asHost.invoke(obj);
                    if (host != null) return host;
                } catch (Exception ignored) {}
                
                // 2. Direct numeric / boolean / char conversion
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
                if (targetType == char.class || targetType == Character.class) {
                    try {
                        return (char) (int) cls.getMethod("asInt").invoke(obj);
                    } catch (Exception ignored) {}
                    try {
                        return cls.getMethod("asString").invoke(obj);
                    } catch (Exception ignored) {}
                }
                
                // 3. Generic as conversion (boxed types)
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
                // Unpack failed, keep original
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

    private static Object narrowArg(Object arg, Class<?> targetType) {
        if (arg == null) return null;
        if (targetType == char.class) {
            if (arg instanceof Number) return (char) ((Number) arg).intValue();
            if (arg instanceof Character) return ((Character) arg).charValue();
            if (arg instanceof String) {
                String s = (String) arg;
                if (s.length() != 1) {
                    throw new IllegalArgumentException("Expected single character but got string of length " + s.length());
                }
                return s.charAt(0);
            }
            return arg;
        }
        if (arg instanceof Number) {
            Number n = (Number) arg;
            if (targetType == float.class) return n.floatValue();
            if (targetType == double.class) return n.doubleValue();
            if (targetType == int.class) return n.intValue();
            if (targetType == long.class) return n.longValue();
            if (targetType == short.class) return n.shortValue();
            if (targetType == byte.class) return n.byteValue();
        }
        return arg;
    }

    private static Class<?> primitiveOf(Class<?> cls) {
        if (cls == Boolean.class) return boolean.class;
        if (cls == Byte.class) return byte.class;
        if (cls == Character.class) return char.class;
        if (cls == Short.class) return short.class;
        if (cls == Integer.class) return int.class;
        if (cls == Long.class) return long.class;
        if (cls == Float.class) return float.class;
        if (cls == Double.class) return double.class;
        return cls;
    }

    private static boolean isWideningPrimitive(Class<?> target, Class<?> source) {
        if (target == source) return true;
        if (target == short.class) return source == byte.class;
        if (target == int.class) return source == byte.class || source == short.class || source == char.class;
        if (target == long.class) return source == byte.class || source == short.class || source == char.class || source == int.class;
        if (target == float.class) return source == byte.class || source == short.class || source == char.class || source == int.class || source == long.class;
        if (target == double.class) return source == byte.class || source == short.class || source == char.class || source == int.class || source == long.class || source == float.class;
        return false;
    }

    private static boolean isMoreSpecific(Class<?>[] candidate, Class<?>[] reference) {
        if (candidate.length != reference.length) return false;
        boolean strictlyNarrower = false;
        for (int i = 0; i < candidate.length; i++) {
            if (candidate[i] == reference[i]) continue;
            Class<?> candPrim = primitiveOf(candidate[i]);
            Class<?> refPrim = primitiveOf(reference[i]);
            if (candPrim.isPrimitive() && refPrim.isPrimitive()) {
                if (candPrim == refPrim) continue;
                if (!isWideningPrimitive(refPrim, candPrim)) return false;
                strictlyNarrower = true;
            } else if (candPrim != refPrim) {
                if (!reference[i].isAssignableFrom(candidate[i])) return false;
                strictlyNarrower = true;
            }
        }
        return strictlyNarrower;
    }

    private static boolean isAssignable(Class<?> param, Class<?> arg) {
        String argName = arg.getName();
        if (argName.contains("polyglot") || argName.contains("truffle")) return true;
        if (Number.class.isAssignableFrom(arg)) {
            // Python ints/floats are Number subclasses; accept any numeric primitive target
            return param.isPrimitive() && param != boolean.class;
        }
        if (param.isPrimitive()) {
            if (param == boolean.class) return arg == Boolean.class;
            if (param == char.class) return arg == Character.class || arg == String.class;
            // Character can be widened to any numeric primitive
            return arg == Character.class;
        }
        return param.isAssignableFrom(arg);
    }
}
