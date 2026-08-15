package net.luojiayuan.jython.mod.bytecode;

/**
 * 动态字节码转换器接口。
 * 供 Java / Python 端注册自定义类转换逻辑。
 */
@FunctionalInterface
public interface BytecodeTransformer {
    /**
     * 转换类字节码。
     *
     * @param className  类的全限定名（点号分隔）
     * @param classBytes 当前字节码（可能已被 Mixin 处理过）
     * @return 转换后的字节码；如无需修改可直接返回 classBytes
     */
    byte[] transform(String className, byte[] classBytes);
}
