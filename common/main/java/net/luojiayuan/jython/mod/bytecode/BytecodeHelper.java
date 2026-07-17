package net.luojiayuan.jython.mod.bytecode;

import org.graalvm.polyglot.Value;

/**
 * 字节码转换辅助类，支持 GraalPy 引擎。
 *
 * <p>使用示例：
 * <pre>
 *   from net.luojiayuan.jython.mod.bytecode import BytecodeHelper
 *   def my_transform(className, classBytes):
 *       return classBytes
 *   BytecodeHelper.registerTransformer(my_transform)
 * </pre>
 */
public class BytecodeHelper {

    /**
     * 注册 Java 实现的转换器。
     */
    public static void registerTransformer(BytecodeTransformer transformer) {
        BytecodeRegistry.register(transformer);
    }

    /**
     * 注册 GraalPy Python 回调作为转换器。
     * 回调签名: def transform(className: str, classBytes: bytes) -> bytes
     */
    public static void registerTransformer(Value callback) {
        BytecodeRegistry.register((className, bytes) -> {
            Value result = callback.execute(className, bytes);
            return result.as(byte[].class);
        });
    }

    /**
     * 注销转换器。
     */
    public static void unregisterTransformer(BytecodeTransformer transformer) {
        BytecodeRegistry.unregister(transformer);
    }

    /**
     * 获取当前已注册的转换器数量。
     */
    public static int getTransformerCount() {
        return BytecodeRegistry.transformerCount();
    }
}
