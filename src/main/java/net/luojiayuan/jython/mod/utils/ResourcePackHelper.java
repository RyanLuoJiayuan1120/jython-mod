package net.luojiayuan.jython.mod.utils;

import java.io.*;
import java.nio.file.*;
import java.util.regex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 资源包工具类
 * 用于自动启用生成的资源包
 */
public class ResourcePackHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger("jython-mod");

	/**
	 * 自动启用指定的资源包
	 * @param packName 资源包文件名（例如：JythonModAssets.zip）
	 */
	public static void enableResourcePack(String packName) {
		Path optionsPath = GameDirHelper.getGameDir().resolve("options.txt");
		String packId = "file/" + packName;

		try {
			if (!Files.exists(optionsPath)) {
				LOGGER.warn("options.txt not found, cannot auto-enable resource pack");
				return;
			}

			String content = new String(Files.readAllBytes(optionsPath));

			// 解析resourcePacks
			Pattern pattern = Pattern.compile("resourcePacks:\\[(.*?)\\]");
			Matcher matcher = pattern.matcher(content);

			if (matcher.find()) {
				String packsStr = matcher.group(1);
				java.util.List<String> packs = new java.util.ArrayList<>();

				// 解析已有的资源包列表
				if (packsStr != null && !packsStr.trim().isEmpty()) {
					for (String p : packsStr.split(",")) {
						p = p.trim().replace("\"", "");
						if (!p.isEmpty()) {
							packs.add(p);
						}
					}
				}

				// 添加我们的资源包（如果不存在）
				if (!packs.contains(packId)) {
					packs.add(1, packId); // 插入到vanilla之后

					// 重建resourcePacks字符串
					StringBuilder newPacksStr = new StringBuilder("resourcePacks:[");
					for (int i = 0; i < packs.size(); i++) {
						if (i > 0) newPacksStr.append(",");
						newPacksStr.append("\"").append(packs.get(i)).append("\"");
					}
					newPacksStr.append("]");

					// 替换原文件中的resourcePacks
					String newContent = content.replaceAll("resourcePacks:\\[.*?\\]", newPacksStr.toString());

					Files.write(optionsPath, newContent.getBytes());
					LOGGER.info("Resource pack '{}' enabled automatically!", packName);
				} else {
					LOGGER.info("Resource pack '{}' already enabled", packName);
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to auto-enable resource pack: " + e.getMessage());
		}
	}
}
