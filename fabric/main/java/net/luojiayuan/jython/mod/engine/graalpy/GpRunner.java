package net.luojiayuan.jython.mod.engine.graalpy;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.*;
import org.graalvm.python.embedding.GraalPyResources;

import net.luojiayuan.jython.mod.Jythonmod;
import net.luojiayuan.jython.mod.PythonLogger;
import net.luojiayuan.jython.mod.utils.GameDirHelper;

import net.luojiayuan.jython.mod.engine.RunnerMain;
public class GpRunner extends RunnerMain {
    private Context context;
    private Value pythonBindings;

    public GpRunner(String env_type, String Mod) {
        this.context = Context.newBuilder("python")
                .allowAllAccess(true)
                .build();

        // 设置 Python 路径（与 GpIniter 保持一致）
        Value sysModule = context.eval("python", "import sys; sys");
        Value syspath = sysModule.getMember("path");
        syspath.getMember("append").executeVoid("Lib");
        syspath.getMember("append").executeVoid("/assets/jython-mod/jython");

        this.pythonBindings = context.getBindings("python");
        
        String path_ = GameDirHelper.getGameDirPath();
        pythonBindings.putMember("LOGGER", new PythonLogger(Jythonmod.LOGGER));
        pythonBindings.putMember("ENV_TYPE", env_type);
        pythonBindings.putMember("GAME_DIR", path_);
        pythonBindings.putMember("Script", Mod);
    }

    public void runScript(InputStream script) {
        try {
            String code = new String(script.readAllBytes(), StandardCharsets.UTF_8);
            context.eval("python", code);
        } catch (Exception e) {
            Jythonmod.LOGGER.error("Error running script", e);
        }
    }

    public void exec(String code) {
        context.eval("python", code);
    }

    public void close() {
        context.close();
    }
}
