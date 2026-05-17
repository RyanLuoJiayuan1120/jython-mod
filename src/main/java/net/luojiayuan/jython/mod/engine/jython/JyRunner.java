package net.luojiayuan.jython.mod.engine.jython;

import java.io.InputStream;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import net.luojiayuan.jython.mod.Jythonmod;
import net.luojiayuan.jython.mod.PythonLogger;
import net.luojiayuan.jython.mod.utils.GameDirHelper;

import net.luojiayuan.jython.mod.engine.RunnerMain;
public class JyRunner extends RunnerMain {
    private PythonInterpreter interpreter;

    public JyRunner(String env_type, String Mod) {
        this.interpreter = new PythonInterpreter();
        String path_ = GameDirHelper.getGameDirPath();
        interpreter.set("LOGGER", new PythonLogger(Jythonmod.LOGGER));
        interpreter.set("ENV_TYPE", env_type);
        interpreter.set("GAME_DIR", path_);
        interpreter.set("Script", Mod);
    }

    public void runScript(InputStream script) {
        interpreter.execfile(script);
    }

    public void exec(String code) {
        interpreter.exec(code);
    }

    public void close() {
        interpreter.close();
    }
}
