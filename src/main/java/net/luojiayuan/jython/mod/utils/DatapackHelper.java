package net.luojiayuan.jython.mod.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据包工具类
 * 用于自动启用生成的数据包
 */
public class DatapackHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger("jython-mod");

	/**
	 * 在所有已存在的世界中启用指定的数据包
	 * @param packName 数据包文件名（例如：JythonModData.zip）
	 */
	public static void enableDatapackInAllWorlds(String packName) {
		List<Path> worldDirs = new ArrayList<>();
		Path gameDir = GameDirHelper.getGameDir();

		// 1. 检查 gamedir/saves/ 目录（客户端世界）
		Path savesDir = gameDir.resolve("saves");
		if (Files.exists(savesDir)) {
			try {
				for (Path worldDir : Files.newDirectoryStream(savesDir)) {
					if (Files.isDirectory(worldDir)) {
						worldDirs.add(worldDir);
					}
				}
			} catch (IOException e) {
				LOGGER.debug("Failed to scan saves directory: " + e.getMessage());
			}
		}

		// 2. 检查 gamedir/ 下的直接子目录（服务器世界）
		try {
			for (Path item : Files.newDirectoryStream(gameDir)) {
				if (Files.isDirectory(item)) {
					// 检查是否是世界目录（包含 level.dat）
					Path levelDat = item.resolve("level.dat");
					if (Files.exists(levelDat)) {
						// 确保不是已经在 saves 目录中的
						if (!item.startsWith(savesDir)) {
							worldDirs.add(item);
						}
					}
				}
			}
		} catch (IOException e) {
			LOGGER.debug("Failed to scan game directory for worlds: " + e.getMessage());
		}

		// 启用所有世界的数据包
		for (Path worldDir : worldDirs) {
			enableDatapackInWorld(worldDir, packName);
		}
	}

	/**
	 * 在指定世界中启用数据包
	 * @param worldDir 世界文件夹路径
	 * @param packName 数据包文件名
	 */
	private static void enableDatapackInWorld(Path worldDir, String packName) {
		try {
			// 检查数据包是否存在
			Path datapacksDir = worldDir.resolve("datapacks");
			Path datapackPath = datapacksDir.resolve(packName);

			if (!Files.exists(datapackPath)) {
				LOGGER.debug("Datapack not found in " + worldDir + ": " + packName);
				return;
			}

			// 创建或更新 enabled_packs.json 文件
			Path enabledPacksPath = worldDir.resolve("datapacks").resolve("enabled_packs.json");

			List<String> enabledPacks = new ArrayList<>();
			// 默认包含 vanilla
			enabledPacks.add("vanilla");

			// 如果文件已存在，读取它
			if (Files.exists(enabledPacksPath)) {
				try {
					String content = new String(Files.readAllBytes(enabledPacksPath));
					// 简单的JSON解析（手动处理）
					if (content.contains("\"")) {
						// 提取所有引用的字符串
						int start = content.indexOf("[");
						int end = content.lastIndexOf("]");
						if (start != -1 && end != -1) {
							String arrayContent = content.substring(start + 1, end);
							// 分割并清理包名
							for (String pack : arrayContent.split(",")) {
								pack = pack.trim().replaceAll("^\"|\"$", "").replace("\\\"", "\"");
								if (!pack.isEmpty() && !pack.equals("vanilla")) {
									enabledPacks.add(pack);
								}
							}
						}
					}
				} catch (Exception e) {
					LOGGER.debug("Failed to read enabled_packs.json, creating new one");
				}
			}

			// 检查是否已启用
			String packId = "file/" + packName;
			if (!enabledPacks.contains(packId)) {
				enabledPacks.add(packId);

				// 构建JSON
				StringBuilder json = new StringBuilder("{\n  \"packs\": [\n");
				for (int i = 0; i < enabledPacks.size(); i++) {
					if (i > 0) json.append(",\n");
					json.append("    \"").append(enabledPacks.get(i)).append("\"");
				}
				json.append("\n  ]\n}");

				// 确保目录存在
				Files.createDirectories(datapacksDir);

				// 写入文件
				Files.write(enabledPacksPath, json.toString().getBytes());

				LOGGER.info("Datapack '{}' enabled in world: {}",
					packName, worldDir.getFileName());
			} else {
				LOGGER.debug("Datapack '{}' already enabled in world: {}",
					packName, worldDir.getFileName());
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to enable datapack in " + worldDir + ": " + e.getMessage());
		}
	}

	/**
	 * 启用数据包（供Python调用的简化接口）
	 * @param packName 数据包文件名
	 */
	public static void enableDatapack(String packName) {
		enableDatapackInAllWorlds(packName);
	}
}
