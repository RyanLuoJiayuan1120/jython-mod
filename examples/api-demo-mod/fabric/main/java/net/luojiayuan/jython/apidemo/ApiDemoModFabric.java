package net.luojiayuan.jython.apidemo;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Fabric 入口：在 preLaunch 阶段注册（早于任何 mod 的 main entrypoint，
 * 因此必然早于 jython-mod 在 onInitialize 里运行 Python 脚本）。
 */
public class ApiDemoModFabric implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        ApiDemoCore.register();
    }
}
