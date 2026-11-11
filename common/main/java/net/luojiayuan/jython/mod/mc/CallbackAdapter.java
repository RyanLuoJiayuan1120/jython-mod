package net.luojiayuan.jython.mod.mc;

import org.graalvm.polyglot.Value;

import java.lang.reflect.Proxy;

/**
 * Adapts a Python callable (a GraalPy {@link Value}) to a strongly-typed Java
 * interface. Fabric events, Brigadier commands and similar APIs store listeners
 * in typed arrays; passing a raw Python function causes
 * {@code ArrayStoreException: PolyglotMapAndFunction}. This adapter wraps the
 * callable in a real JDK proxy implementing the requested interface.
 *
 * <p>Python usage:</p>
 * <pre>
 * ServerStarted = java.type("...ServerLifecycleEvents$ServerStarted")
 * ServerLifecycleEvents.SERVER_STARTED.register(CallbackAdapter.adapt(on_start, ServerStarted))
 * </pre>
 */
public final class CallbackAdapter {
    private CallbackAdapter() {
    }

    public static <T> T adapt(Value function, Class<T> iface) {
        if (function == null || !function.canExecute()) {
            throw new IllegalArgumentException("callback is not executable: " + function);
        }
        if (iface == null || !iface.isInterface()) {
            throw new IllegalArgumentException("target must be an interface: " + iface);
        }
        ClassLoader loader = iface.getClassLoader();
        if (loader == null) {
            loader = CallbackAdapter.class.getClassLoader();
        }
        Object proxy = Proxy.newProxyInstance(loader, new Class<?>[]{iface}, (proxyObject, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                switch (method.getName()) {
                    case "toString":
                        return "PythonCallback(" + iface.getSimpleName() + ")";
                    case "hashCode":
                        return System.identityHashCode(proxyObject);
                    case "equals":
                        return proxyObject == args[0];
                    default:
                        return null;
                }
            }
            Value result = function.execute(args == null ? new Object[0] : args);
            Class<?> returnType = method.getReturnType();
            if (returnType == void.class) {
                return null;
            }
            if (result == null || result.isNull()) {
                return defaultOf(returnType);
            }
            if (returnType == int.class || returnType == Integer.class) {
                return result.asInt();
            }
            if (returnType == long.class || returnType == Long.class) {
                return result.asLong();
            }
            if (returnType == boolean.class || returnType == Boolean.class) {
                return result.asBoolean();
            }
            if (returnType == double.class || returnType == Double.class) {
                return result.asDouble();
            }
            if (returnType == float.class || returnType == Float.class) {
                return result.asFloat();
            }
            if (returnType == byte.class || returnType == Byte.class) {
                return (byte) result.asInt();
            }
            if (returnType == short.class || returnType == Short.class) {
                return (short) result.asInt();
            }
            if (returnType == char.class || returnType == Character.class) {
                return result.asString().isEmpty() ? '\0' : result.asString().charAt(0);
            }
            return result.as(returnType);
        });
        return iface.cast(proxy);
    }

    private static Object defaultOf(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null;
    }
}
