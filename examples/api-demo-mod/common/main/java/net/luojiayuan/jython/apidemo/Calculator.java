package net.luojiayuan.jython.apidemo;

/**
 * 示例 API：纯静态工具类（类形式），并带一个嵌套类 Builder 演示链式调用。
 *
 * Python 侧（注册的 Class 对象会被包装成可调用引用）：
 * <pre>{@code
 * calc = API["apidemo"]["Calculator"]
 * calc.PI                 # 静态字段
 * calc.add(1, 2)          # 静态方法
 * calc.Builder().set(21).build()   # 嵌套类 + 链式
 * }</pre>
 */
public class Calculator {

    public static final double PI = 3.14159;

    public static int add(int a, int b) {
        return a + b;
    }

    public static String describe() {
        return "Calculator v1";
    }

    /** 嵌套类：演示 Builder 链式构造。 */
    public static class Builder {
        private int value = 1;

        public Builder set(int v) {
            this.value = v;
            return this;
        }

        public int build() {
            return value * 2;
        }
    }
}
