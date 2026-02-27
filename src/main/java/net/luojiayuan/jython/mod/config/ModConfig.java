package net.luojiayuan.jython.mod.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "jython-mod")
public class ModConfig implements ConfigData {

	// 是否启用Jython模组
	public boolean enabled = true;

	// 是否开启调试模式
	public boolean debugMode = false;

	// Python脚本路径（需要重启游戏才能生效）
	@ConfigEntry.Gui.RequiresRestart
	public String scriptPath = "/assets/jython-mod/jython/main.py";

	// 是否自动重载脚本
	public boolean autoReload = true;

	// 脚本执行超时时间（秒）
	@ConfigEntry.BoundedDiscrete(min = 1, max = 300)
	public int scriptTimeout = 30;

	// 是否在控制台显示Python输出
	public boolean showPythonOutput = true;

	// Python系统路径（可选，用逗号分隔）
	public String pythonPath = "";

	// 版本号（隐藏字段，不显示在GUI中）
	// @ConfigEntry.Gui.Excluded
	// public String version = "";
	@ConfigEntry.Gui.RequiresRestart
	public String modsPaths = "{gamedir}/jymods";
}
