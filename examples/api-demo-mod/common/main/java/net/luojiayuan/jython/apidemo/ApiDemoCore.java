package net.luojiayuan.jython.apidemo;

import net.luojiayuan.jython.mod.api.JythonModApi;

/**
 * 示例模组核心：把演示 API 注册进 JythonModApi。
 *
 * 各平台入口（Fabric / NeoForge / Paper）在初始化时调用 {@link #register()}。
 */
public final class ApiDemoCore {

    private ApiDemoCore() {
    }

    /**
     * 注册演示 API。覆盖四种注册形式：
     * <ul>
     *   <li>实例 + 自动类名 key：{@code TradeApi}</li>
     *   <li>类 + 自定义 key：{@code TradeApiClass}（Python 侧可构造实例）</li>
     *   <li>类 + 自动类名 key：{@code Calculator}（静态方法 / 字段 / 嵌套 Builder）</li>
     *   <li>实例 + 自定义 key：{@code trade}（自定义命名示例）</li>
     * </ul>
     */
    public static void register() {
        // 1) 实例，key 自动取类简单名 "TradeApi"
        JythonModApi.register("apidemo", new TradeApi());

        // 2) 类对象，自定义 key —— Python 侧可 new 出实例
        JythonModApi.register("apidemo", "TradeApiClass", TradeApi.class);

        // 3) 类对象，key 自动取类简单名 "Calculator"
        JythonModApi.register("apidemo", Calculator.class);

        // 4) 实例，自定义 key
        JythonModApi.register("apidemo", "trade", new TradeApi("custom"));
    }
}
