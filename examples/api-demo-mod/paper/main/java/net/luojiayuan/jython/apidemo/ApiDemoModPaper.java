package net.luojiayuan.jython.apidemo;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Paper 入口：在 onLoad 阶段注册（早于 jython-mod 的 onEnable，即早于
 * Python 脚本运行）。
 *
 * <p>Paper 的插件类加载器是隔离的：第三方插件在编译期/类加载期看不到
 * jython-mod 的类（即使声明了 depend），因此这里通过 jython-mod 插件的
 * ClassLoader 反射调用 {@code JythonModApi.register}——这是 Paper 上
 * 跨插件调用的标准做法（对应文档 Q17 的"反射逃生舱"）。
 */
public class ApiDemoModPaper extends JavaPlugin {

    @Override
    public void onLoad() {
        try {
            Plugin jythonMod = Bukkit.getPluginManager().getPlugin("jython-mod");
            if (jythonMod == null) {
                getLogger().warning("jython-mod plugin not found; demo APIs not registered");
                return;
            }
            ClassLoader loader = jythonMod.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("net.luojiayuan.jython.mod.api.JythonModApi", true, loader);
            Method register = apiClass.getMethod("register", String.class, String.class, Object.class);

            register.invoke(null, "apidemo", "TradeApi", new TradeApi());
            register.invoke(null, "apidemo", "TradeApiClass", TradeApi.class);
            register.invoke(null, "apidemo", "Calculator", Calculator.class);
            register.invoke(null, "apidemo", "trade", new TradeApi("custom"));
            getLogger().info("Registered JythonModApi demo APIs (4)");
        } catch (Exception e) {
            getLogger().severe("Failed to register JythonModApi demo APIs: " + e);
        }
    }
}
