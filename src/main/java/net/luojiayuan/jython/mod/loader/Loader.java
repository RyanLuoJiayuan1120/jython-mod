package net.luojiayuan.jython.mod.loader;

import java.io.File;
import java.io.InputStream;
import java.util.Vector;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import net.luojiayuan.jython.mod.Jythonmod;
import net.luojiayuan.jython.mod.PythonLogger;
import net.luojiayuan.jython.mod.utils.GameDirHelper;
import net.luojiayuan.jython.mod.builder.*;
import net.luojiayuan.jython.mod.engine.jython.JyRunner;
import net.luojiayuan.jython.mod.engine.graalpy.GpRunner;
import net.luojiayuan.jython.mod.engine.RunnerMain;
public class Loader {
    public Vector<File> dirs = new Vector<File>();
    public Vector<String> JythonMods = new Vector<String>();

    public Loader(String env_type){
        Scanner(); ModLoader(env_type);
        try {
            PacksPacker(env_type);
        } catch (Exception e) {
            Jythonmod.LOGGER.error(e.toString());
        }
    }

    private void Scanner(){
        String modsPath = Jythonmod.CONFIG.modsPaths;
        String gameDir = GameDirHelper.getGameDirPath();
        String resolvedPath = modsPath.replace("{gamedir}", gameDir);
        String[] resolvedPaths = resolvedPath.split(";"); // NOTE: paths must not contain semicolons
        
        for (String path : resolvedPaths){
            File folder = new File(path);
            if (folder.exists()) {
                Jythonmod.LOGGER.debug("Creating dir: {}", path);
                try {
                    folder.mkdir();
                    Jythonmod.LOGGER.debug("Created dir: {}", path);
                } catch (Exception e){
                    Jythonmod.LOGGER.error("Failed to create dir \"{}\": {}", path, e.toString());
                }
            }
            dirs.add(new File(path));
        }
    }

    private void ModLoader(String env_type){
        for (File folder : dirs) {
            try {
                File[] files = folder.listFiles();
                for (File file : files) {
                    try {
                        JythonMods.add(file.getAbsolutePath());
                    } catch (Exception e){
                        Jythonmod.LOGGER.error("Failed to list dir \"{}\": {}", folder, e.toString());
                    }
                }
            } catch (Exception e) {
                Jythonmod.LOGGER.error("Failed to get dir list: {}", e.toString());
            }
            
        }
        for (String Mod : JythonMods) {
            try {
                Jythonmod.LOGGER.info("Running mod \"{}\"", Mod);
                RunnerMain runner = null;
                if (Jythonmod.CONFIG.engineVersion == 1) {
                    runner = new JyRunner(env_type, Mod);
                    
                } else if (Jythonmod.CONFIG.engineVersion == 2) {
                    runner = new GpRunner(env_type, Mod);
                }

                InputStream pythonScript = getClass().getResourceAsStream("/assets/jython-mod/jython/zipimporter.py");
                runner.runScript(pythonScript);
                runner.exec("importer = ModImporter(ENV_TYPE, Script) \n"+
                            "importer.Load()"
                );
                runner.close();
                Jythonmod.LOGGER.info("Successfully ran mod \"{}\"", Mod);
            } catch (Exception e) {
                Jythonmod.LOGGER.error("Failed to run mod \"{}\": {}", Mod, e.toString(), e);
            }
        }
        
    }

    private void PacksPacker(String env) {
        String[] modPaths = Jythonmod.CONFIG.modsPaths
        .replace("{gamedir}", GameDirHelper.getGameDirPath())
        .split(";");
        ModResourcePackBuilder.build(env, modPaths);
        ModDataPackBuilder.build(modPaths);
    }
}
