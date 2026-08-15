package net.luojiayuan.jython.mod.bytecode;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * PreLaunch entrypoint: after Mixin initialization, replace
 * {@code KnotClassDelegate.mixinTransformer} with a dynamic proxy
 * so custom bytecode transformers run after all Mixin transformations.
 */
public class BytecodeHook implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LoggerFactory.getLogger("jython-mod");
    public static volatile boolean hooked = false;

    @Override
    public void onPreLaunch() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();

            Class<?> knotClassLoaderClass = Class.forName("net.fabricmc.loader.impl.launch.knot.KnotClassLoader", true, cl);

            if (!knotClassLoaderClass.isInstance(cl)) {
                LOGGER.error("[BytecodeHook] Current ClassLoader is not KnotClassLoader, hook aborted.");
                return;
            }

            Field delegateField = knotClassLoaderClass.getDeclaredField("delegate");
            delegateField.setAccessible(true);
            Object delegate = delegateField.get(cl);

            Class<?> knotClassDelegateClass = Class.forName("net.fabricmc.loader.impl.launch.knot.KnotClassDelegate", true, cl);

            // Preload proxy-related classes to avoid recursive class loading inside transformClassBytes
            Class.forName("net.luojiayuan.jython.mod.bytecode.BytecodeRegistry", true, cl);
            Class.forName("net.luojiayuan.jython.mod.bytecode.BytecodeTransformer", true, cl);
            Class.forName("net.luojiayuan.jython.mod.bytecode.DynamicMixinProxy", true, cl);

            Field transformerField = knotClassDelegateClass.getDeclaredField("mixinTransformer");
            transformerField.setAccessible(true);
            Object originalTransformer = transformerField.get(delegate);

            if (originalTransformer == null) {
                LOGGER.error("[BytecodeHook] Mixin transformer is null, hook aborted.");
                return;
            }

            Class<?> mixinTransformerIface = Class.forName("org.spongepowered.asm.mixin.transformer.IMixinTransformer", true, cl);
            InvocationHandler handler = new DynamicMixinProxy(originalTransformer);
            Object proxy = Proxy.newProxyInstance(
                    cl,
                    new Class<?>[]{mixinTransformerIface},
                    handler
            );

            transformerField.set(delegate, proxy);
            hooked = true;
            LOGGER.info("[BytecodeHook] Mixin transformer hooked. Registered transformers: {}", BytecodeRegistry.transformerCount());

        } catch (Exception e) {
            LOGGER.error("[BytecodeHook] Failed to hook Mixin transformer: {}", e.getMessage(), e);
        }
    }
}
