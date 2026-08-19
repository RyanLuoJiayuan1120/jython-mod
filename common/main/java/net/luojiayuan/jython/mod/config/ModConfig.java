package net.luojiayuan.jython.mod.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * Jython Mod configuration.
 *
 * <p>Implements {@link ConfigData} so AutoConfig's serializers (Fabric /
 * NeoForge) can validate and cast the config. The interface lives in Cloth
 * Config, which is not a server dependency on Paper; the Paper fat jar bundles
 * Cloth Config (see {@code paperRuntime} in build.gradle) so this class loads
 * there too. Paper never runs the AutoConfig path ({@code supportsConfigGui()}
 * is false) and reads the same fields via plain Gson instead.</p>
 */
@Config(name = "jython-mod")
public class ModConfig implements ConfigData {

	// 是否启用模组
	public boolean enabled = true;

	// 是否开启调试模式
	public boolean debugMode = false;

	// Python脚本路径（需要重启游戏才能生效）
	@ConfigEntry.Gui.RequiresRestart
	public String scriptPath = "/assets/jython-mod/jython/main.py";

	// 是否自动重载脚本
	// public boolean autoReload = true;

	// 脚本执行超时时间（秒）
	// @ConfigEntry.BoundedDiscrete(min = 1, max = 300)
	// public int scriptTimeout = 30;

	// 是否在控制台显示Python输出
	public boolean showPythonOutput = true;

	// Python系统路径（可选，用逗号分隔）
	public String pythonPath = "";

	@ConfigEntry.Gui.RequiresRestart
	public String modsPaths = "{gamedir}/jymods";

	// 第三方 Python 包本地部署目录
	@ConfigEntry.Gui.RequiresRestart
	public String pythonPackagesPath = "{gamedir}/graalpy/Lib";
}
