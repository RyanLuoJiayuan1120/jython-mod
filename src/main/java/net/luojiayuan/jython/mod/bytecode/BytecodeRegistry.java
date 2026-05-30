package net.luojiayuan.jython.mod.bytecode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bytecode transformer registry.
 * All registered transformers run after Mixin but before class definition.
 */
public class BytecodeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("jython-mod");
    private static final List<BytecodeTransformer> TRANSFORMERS = new CopyOnWriteArrayList<>();
    private static final ThreadLocal<Boolean> TRANSFORMING = ThreadLocal.withInitial(() -> false);

    public static void register(BytecodeTransformer transformer) {
        TRANSFORMERS.add(transformer);
    }

    public static void unregister(BytecodeTransformer transformer) {
        TRANSFORMERS.remove(transformer);
    }

    /**
     * Run all registered transformers with reentrance guard.
     */
    public static byte[] transform(String className, byte[] bytes) {
        if (bytes == null || bytes.length == 0) return bytes;
        if (TRANSFORMING.get()) return bytes;

        TRANSFORMING.set(true);
        try {
            for (BytecodeTransformer t : TRANSFORMERS) {
                try {
                    bytes = t.transform(className, bytes);
                } catch (Exception e) {
                    // Silently skip when GraalPy Context is already closed during shutdown
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("Context is already closed")) {
                        return bytes;
                    }
                    LOGGER.error("[BytecodeRegistry] Transformer failed for {}: {}", className, e.getMessage(), e);
                }
            }
        } finally {
            TRANSFORMING.set(false);
        }
        return bytes;
    }

    public static int transformerCount() {
        return TRANSFORMERS.size();
    }
}
