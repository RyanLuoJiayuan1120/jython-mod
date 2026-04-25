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
        /*
         * 模组加载文件夹列表的扫描 
        */
        String modsPath = Jythonmod.CONFIG.modsPaths;
        String gameDir = GameDirHelper.getGameDirPath();
        String resolvedPath = modsPath.replace("{gamedir}", gameDir);
        String[] resolvedPaths = resolvedPath.split(";");// 注意这里记得，路径里面不要有;
        
        for (String path : resolvedPaths){
            File folder = new File(path);
            if (folder.exists()) {
                Jythonmod.LOGGER.debug("Make dir:"+path);
                try {
                    folder.mkdir();
                    Jythonmod.LOGGER.debug("Succeed in Making dir:"+path);
                } catch (Exception e){
                    Jythonmod.LOGGER.error("Error at making dir \""+path+"\":"+e.toString());
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
                        Jythonmod.LOGGER.error("Error at listing dir \""+folder.toString()+"\":"+e.toString());
                    }
                }
            } catch (Exception e) {
                Jythonmod.LOGGER.error("Error at getting dir list:"+e.toString());
            }
            
        }
        for (String Mod : JythonMods) {
            try {
                Jythonmod.LOGGER.info("Run mod \""+Mod+"\"");
                String path_ = GameDirHelper.getGameDirPath();
                PythonInterpreter interpreter = new PythonInterpreter();
                interpreter.set("LOGGER", new PythonLogger(Jythonmod.LOGGER));
                interpreter.set("ENV_TYPE", env_type);
                interpreter.set("GAME_DIR", path_);
                interpreter.set("Script", Mod);
                InputStream pythonScript = getClass().getResourceAsStream("/assets/jython-mod/jython/zipimporter.py");
                interpreter.execfile(pythonScript);
                pythonScript.close();
                interpreter.exec("importer = ModImporter(ENV_TYPE, Script) \n"+
                                 "importer.Load()"
                                );
                interpreter.close();
                Jythonmod.LOGGER.info("Succeed in running mod \""+Mod+"\"");
            } catch (Exception e) {
                Jythonmod.LOGGER.error("Error at running mod \""+Mod+"\":"+e.toString());
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
