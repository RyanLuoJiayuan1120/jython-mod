# Paper 平台

## 概述

Paper（Bukkit）是**仅服务端**的插件平台。与 Fabric / NeoForge 相比有显著差异：

- **无客户端环境**：只执行 `main` + `server` 环境，`client.py` 被跳过。
- **Python 面向 `org.bukkit.*` API**：`net.minecraft.*` 类对插件不可见，Python 模组应使用 Bukkit API（`org.bukkit.Bukkit`、`org.bukkit.World` 等）。
- **无字节码转换**：服务端类在插件启用前已加载，且无 transformer SPI，`BytecodeHelper` 不可用。
- **无配置 GUI**：Gson 直读配置文件。
- **跳过资源包/数据包生成**。

## 入口与加载时机

- 入口类：`net.luojiayuan.jython.mod.JythonModPaper`（`JavaPlugin`）
- 加载时机：`onEnable()` 时依次执行 `main`（公共环境）→ `server`（服务端环境）
- 插件元数据见 `paper-plugin.yml`（`api-version: '1.21'`）。

## 类名与映射

Paper 使用官方（Mojang）类名，且 Bukkit API 无需映射。`usesOfficialMappings()` 返回 `true`，类名直通。

## 配置方式

- 无 Cloth Config，插件用 Gson 直读 `{gamedir}/config/jython-mod.json`（`{gamedir}` = 服务端根目录）。
- 文件缺失时使用默认值，修改后需重启服务器生效。
- 字段与 Fabric / NeoForge 一致，见 [config.md](../usage/config.md)。

## 字节码转换

**不支持。** 服务端启动时核心类已加载，无法在插件启用后转换字节码。

## 资源包 / 数据包

**不支持。** 加载时跳过资源包与数据包生成。

## 构建与部署

```bash
./gradlew paperJar
```

产物：`build/libs/jython-mod-<version>-paper.jar`（内含 GraalPy 运行时 + Cloth Config 接口类 + 共享资源）。

部署：
1. 将 `-paper.jar` 放入服务端的 `plugins/` 文件夹。
2. 将 Python 模块 ZIP 放入 `plugins/jymods/` 文件夹（配置 `modsPaths` 默认 `{gamedir}/jymods`，`{gamedir}` 即服务端根目录）。
3. 启动服务器。

> 插件为自包含 fat jar，无需额外依赖。

## 示例：Bukkit API 用法

```python
from org.bukkit import Bukkit

def main():
    server = Bukkit.getServer()
    LOGGER.info("server=%s version=%s", server.getName(), server.getVersion())
    world = server.getWorlds().get(0)
    LOGGER.info("world=%s", world.getName())
```

> 反射调用 `McReflect.call()` 同样可用于 `org.bukkit.*` 类，例如
> `McReflect.call("org.bukkit.Bukkit", "getServer", None)`（见 [special-syntax.md](../special-syntax.md)）。
