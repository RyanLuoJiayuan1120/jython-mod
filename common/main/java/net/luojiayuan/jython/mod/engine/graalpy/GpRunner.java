package net.luojiayuan.jython.mod.engine.graalpy;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.*;

import net.luojiayuan.jython.mod.PythonLogger;
import net.luojiayuan.jython.mod.engine.RunnerMain;
import org.slf4j.Logger;

public class GpRunner extends RunnerMain {
    private Context context;
    private Value pythonBindings;
    private final Logger logger;

    public GpRunner(String env_type, String Mod, Logger logger, String gameDir, String pythonPackagesPath) {
        this.logger = logger;
        this.context = Context.newBuilder("python")
                .allowAllAccess(true)
                .build();

        // 设置 Python 路径（与 GpIniter 保持一致）
        Value sysModule = context.eval("python", "import sys; sys");
        Value syspath = sysModule.getMember("path");
        syspath.getMember("append").executeVoid("Lib");
        syspath.getMember("append").executeVoid("/assets/jython-mod/jython");

        // 确保第三方包目录存在并加入 Python 路径
        if (pythonPackagesPath != null && !pythonPackagesPath.isEmpty()) {
            File packagesDir = new File(pythonPackagesPath);
            if (!packagesDir.exists()) {
                if (packagesDir.mkdirs()) {
                    logger.debug("Created python packages directory: {}", pythonPackagesPath);
                } else {
                    logger.warn("Failed to create python packages directory: {}", pythonPackagesPath);
                }
            }
            syspath.getMember("append").executeVoid(pythonPackagesPath);
        }

        this.pythonBindings = context.getBindings("python");
        
        pythonBindings.putMember("LOGGER", new PythonLogger(logger));
        pythonBindings.putMember("ENV_TYPE", env_type);
        pythonBindings.putMember("GAME_DIR", gameDir);
        pythonBindings.putMember("Script", Mod);
    }

    public void runScript(InputStream script) {
        try {
            String code = new String(script.readAllBytes(), StandardCharsets.UTF_8);
            context.eval("python", code);
        } catch (Exception e) {
            logger.error("Error running script", e);
        }
    }

    public void exec(String code) {
        context.eval("python", code);
    }

    public void close() {
        context.close();
    }
}
