package net.luojiayuan.jython.apidemo;

import net.neoforged.fml.common.Mod;

@Mod("jython_api_demo")
public class ApiDemoModNeoForge {
    public ApiDemoModNeoForge() {
        ApiDemoCore.register();
    }
}
