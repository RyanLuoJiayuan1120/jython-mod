package net.luojiayuan.jython.mod.utils;

import net.luojiayuan.jython.mod.Jythonmod;

/**
 * GameDirHelper 使用示例
 */
public class GameDirHelperExample {

	/**
	 * 打印所有重要的游戏目录信息
	 */
	public static void printAllPaths() {
		Jythonmod.LOGGER.info("=== Minecraft 游戏目录信息 ===");

		Jythonmod.LOGGER.info("游戏根目录: " + GameDirHelper.getGameDirPath());
		Jythonmod.LOGGER.info("Mods 目录: " + GameDirHelper.getModsDirPath());
		Jythonmod.LOGGER.info("Config 目录: " + GameDirHelper.getConfigDirPath());
		Jythonmod.LOGGER.info("Saves 目录: " + GameDirHelper.getSavesDirPath());
		Jythonmod.LOGGER.info("Screenshots 目录: " + GameDirHelper.getScreenshotsDirPath());
		Jythonmod.LOGGER.info("Resource Packs 目录: " + GameDirHelper.getResourcePacksDir().toAbsolutePath());
		Jythonmod.LOGGER.info("Shader Packs 目录: " + GameDirHelper.getShaderPacksDir().toAbsolutePath());
		Jythonmod.LOGGER.info("Logs 目录: " + GameDirHelper.getLogsDir().toAbsolutePath());
		Jythonmod.LOGGER.info("Crash Reports 目录: " + GameDirHelper.getCrashReportsDir().toAbsolutePath());

		Jythonmod.LOGGER.info("=== 目录信息结束 ===");
	}

	/**
	 * 检查目录是否存在，不存在则创建
	 */
	public static void ensureDirectoriesExist() {
		// 确保 mods 目录存在
		boolean modsExists = GameDirHelper.createDirIfNotExists(GameDirHelper.getModsDir());
		Jythonmod.LOGGER.info("Mods 目录检查: " + (modsExists ? "已存在或创建成功" : "创建失败"));

		// 确保 config 目录存在
		boolean configExists = GameDirHelper.createDirIfNotExists(GameDirHelper.getConfigDir());
		Jythonmod.LOGGER.info("Config 目录检查: " + (configExists ? "已存在或创建成功" : "创建失败"));

		// 创建自定义目录
		boolean customDirExists = GameDirHelper.createDirIfNotExists(
			GameDirHelper.getSubDir("jython-mod/custom")
		);
		Jythonmod.LOGGER.info("自定义目录检查: " + (customDirExists ? "已存在或创建成功" : "创建失败"));
	}

	/**
	 * 示例：列出 mods 目录中的所有文件
	 */
	public static void listMods() {
		java.io.File[] mods = GameDirHelper.getModsDirFile().listFiles();
		if (mods != null) {
			Jythonmod.LOGGER.info("=== Mods 目录中的文件 (" + mods.length + " 个) ===");
			for (java.io.File mod : mods) {
				if (mod.isFile()) {
					Jythonmod.LOGGER.info("  - " + mod.getName() + " (" + mod.length() / 1024 + " KB)");
				}
			}
		} else {
			Jythonmod.LOGGER.info("Mods 目录不存在或为空");
		}
	}

	/**
	 * 示例：列出 config 目录中的所有文件
	 */
	public static void listConfigs() {
		java.io.File[] configs = GameDirHelper.getConfigDirFile().listFiles();
		if (configs != null) {
			Jythonmod.LOGGER.info("=== Config 目录中的文件 (" + configs.length + " 个) ===");
			for (java.io.File config : configs) {
				if (config.isFile()) {
					Jythonmod.LOGGER.info("  - " + config.getName());
				}
			}
		} else {
			Jythonmod.LOGGER.info("Config 目录不存在或为空");
		}
	}
}
