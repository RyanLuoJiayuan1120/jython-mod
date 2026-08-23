package net.luojiayuan.jython.apidemo;

/**
 * 示例 API：一个普通的"第三方模组"服务类（实例形式）。
 *
 * Python 侧通过 JythonModApi 注册表访问：
 * <pre>{@code
 * api = API["apidemo"]["TradeApi"]          # 实例
 * api.greet("Python")
 * api.addTrade(5)
 * }</pre>
 */
public class TradeApi {
    private int tradeCount = 0;
    private final String trader;

    public TradeApi() {
        this("default");
    }

    public TradeApi(String trader) {
        this.trader = trader;
    }

    public String greet(String name) {
        return "Hello " + name + " from " + trader;
    }

    public int addTrade(int amount) {
        tradeCount += amount;
        return tradeCount;
    }

    public int getTradeCount() {
        return tradeCount;
    }

    public static String getVersion() {
        return "1.0.0";
    }
}
