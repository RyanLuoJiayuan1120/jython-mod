package net.luojiayuan.jython.mod.utils;

import net.luojiayuan.jython.mod.platform.PlatformHooks;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Minecraft 游戏目录工具类
 * 用于获取 Minecraft 运行目录及相关子目录
 */
public class GameDirHelper {

	/**
	 * 获取 Minecraft 游戏根目录
	 * 也就是包含 mods、config、saves 等文件夹的目录
	 *
	 * @return 游戏根目录的 Path 对象
	 */
	public static Path getGameDir() {
		return PlatformHooks.get().getGameDir();
	}

	/**
	 * 获取 Minecraft 游戏根目录（File 对象）
	 *
	 * @return 游戏根目录的 File 对象
	 */
	public static File getGameDirFile() {
		return getGameDir().toFile();
	}

	/**
	 * 获取 Minecraft 游戏根目录（字符串）
	 *
	 * @return 游戏根目录的绝对路径字符串
	 */
	public static String getGameDirPath() {
		return getGameDir().toAbsolutePath().toString();
	}

	/**
	 * 获取 mods 目录
	 *
	 * @return mods 目录的 Path 对象
	 */
	public static Path getModsDir() {
		return getGameDir().resolve("mods");
	}

	/**
	 * 获取 mods 目录（File 对象）
	 *
	 * @return mods 目录的 File 对象
	 */
	public static File getModsDirFile() {
		return getModsDir().toFile();
	}

	/**
	 * 获取 mods 目录（字符串）
	 *
	 * @return mods 目录的绝对路径字符串
	 */
	public static String getModsDirPath() {
		return getModsDir().toAbsolutePath().toString();
	}

	/**
	 * 获取 config 目录
	 *
	 * @return config 目录的 Path 对象
	 */
	public static Path getConfigDir() {
		return getGameDir().resolve("config");
	}

	/**
	 * 获取 config 目录（File 对象）
	 *
	 * @return config 目录的 File 对象
	 */
	public static File getConfigDirFile() {
		return getConfigDir().toFile();
	}

	/**
	 * 获取 config 目录（字符串）
	 *
	 * @return config 目录的绝对路径字符串
	 */
	public static String getConfigDirPath() {
		return getConfigDir().toAbsolutePath().toString();
	}

	/**
	 * 获取 saves 目录（存档目录）
	 *
	 * @return saves 目录的 Path 对象
	 */
	public static Path getSavesDir() {
		return getGameDir().resolve("saves");
	}

	/**
	 * 获取 saves 目录（File 对象）
	 *
	 * @return saves 目录的 File 对象
	 */
	public static File getSavesDirFile() {
		return getSavesDir().toFile();
	}

	/**
	 * 获取 saves 目录（字符串）
	 *
	 * @return saves 目录的绝对路径字符串
	 */
	public static String getSavesDirPath() {
		return getSavesDir().toAbsolutePath().toString();
	}

	/**
	 * 获取 screenshots 目录（截图目录）
	 *
	 * @return screenshots 目录的 Path 对象
	 */
	public static Path getScreenshotsDir() {
		return getGameDir().resolve("screenshots");
	}

	/**
	 * 获取 screenshots 目录（File 对象）
	 *
	 * @return screenshots 目录的 File 对象
	 */
	public static File getScreenshotsDirFile() {
		return getScreenshotsDir().toFile();
	}

	/**
	 * 获取 screenshots 目录（字符串）
	 *
	 * @return screenshots 目录的绝对路径字符串
	 */
	public static String getScreenshotsDirPath() {
		return getScreenshotsDir().toAbsolutePath().toString();
	}

	/**
	 * 获取 resourcepacks 目录（资源包目录）
	 *
	 * @return resourcepacks 目录的 Path 对象
	 */
	public static Path getResourcePacksDir() {
		return getGameDir().resolve("resourcepacks");
	}

	/**
	 * 获取 shaderpacks 目录（着色器包目录）
	 * 注意：这个目录可能不存在，取决于是否安装了 Iris/OptiFine
	 *
	 * @return shaderpacks 目录的 Path 对象
	 */
	public static Path getShaderPacksDir() {
		return getGameDir().resolve("shaderpacks");
	}

	/**
	 * 获取 logs 目录（日志目录）
	 *
	 * @return logs 目录的 Path 对象
	 */
	public static Path getLogsDir() {
		return getGameDir().resolve("logs");
	}

	/**
	 * 获取 crash-reports 目录（崩溃报告目录）
	 *
	 * @return crash-reports 目录的 Path 对象
	 */
	public static Path getCrashReportsDir() {
		return getGameDir().resolve("crash-reports");
	}

	/**
	 * 创建目录（如果不存在）
	 *
	 * @param path 要创建的目录路径
	 * @return 是否创建成功或目录已存在
	 */
	public static boolean createDirIfNotExists(Path path) {
		if (!path.toFile().exists()) {
			return path.toFile().mkdirs();
		}
		return true;
	}

	/**
	 * 创建目录（如果不存在）
	 *
	 * @param file 要创建的目录文件对象
	 * @return 是否创建成功或目录已存在
	 */
	public static boolean createDirIfNotExists(File file) {
		if (!file.exists()) {
			return file.mkdirs();
		}
		return true;
	}

	/**
	 * 获取自定义子目录
	 * 例如：getSubDir("custom/mods") 会返回游戏目录下的 custom/mods 文件夹
	 *
	 * @param subPath 子路径（相对于游戏根目录）
	 * @return 子目录的 Path 对象
	 */
	public static Path getSubDir(String subPath) {
		return getGameDir().resolve(subPath);
	}

	/**
	 * 获取自定义子目录（File 对象）
	 *
	 * @param subPath 子路径（相对于游戏根目录）
	 * @return 子目录的 File 对象
	 */
	public static File getSubDirFile(String subPath) {
		return getSubDir(subPath).toFile();
	}

	/**
	 * 获取自定义子目录（字符串）
	 *
	 * @param subPath 子路径（相对于游戏根目录）
	 * @return 子目录的绝对路径字符串
	 */
	public static String getSubDirPath(String subPath) {
		return getSubDir(subPath).toAbsolutePath().toString();
	}
}
