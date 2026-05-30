package net.luojiayuan.jython.mod.bytecode;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * {@link org.spongepowered.asm.mixin.transformer.IMixinTransformer} 的动态代理。
 * 拦截 {@code transformClassBytes} 方法，在 Mixin 完成转换后继续执行自定义转换器链。
 */
public class DynamicMixinProxy implements InvocationHandler {
    private final Object delegate;
    private static final ThreadLocal<Boolean> TRANSFORMING = ThreadLocal.withInitial(() -> false);

    public DynamicMixinProxy(Object delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = method.invoke(delegate, args);

        // 拦截 transformClassBytes(String, String, byte[])
        if ("transformClassBytes".equals(method.getName())
                && result instanceof byte[]
                && args != null
                && args.length >= 3) {
            if (TRANSFORMING.get()) return result;

            TRANSFORMING.set(true);
            try {
                String className = (String) args[0];
                byte[] bytes = (byte[]) result;
                bytes = BytecodeRegistry.transform(className, bytes);
                return bytes;
            } finally {
                TRANSFORMING.set(false);
            }
        }

        return result;
    }
}
