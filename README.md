# Jython Mod

一个基于 Jython 的 Minecraft Fabric 模组，允许使用 Jython 语言编写模组功能。

## 简介

Jython Mod 是一个创新的 Minecraft Fabric 模组，它集成了 Jython 引擎，使开发者能够使用 Jython 语言来创建 Minecraft 模组。这为不熟悉 Java 的开发者提供了更友好的开发体验，同时也为 Java 开发者提供了快速原型开发的能力。

## 特性

- 🐍 **Jython 支持**: 使用 Jython 2.7.3 引擎，支持 Jython 语法
- 📦 **模块化加载**: 支持从外部 ZIP 文件加载 Jython 模块
- 🔧 **灵活配置**: 基于 AutoConfig 的强大配置系统
- 🎨 **自动资源包处理**: 自动从模块中提取并生成资源包
- 📊 **自动数据包处理**: 自动从模块中提取并生成数据包
- 🌐 **多语言支持**: 内置中英文语言支持
- 📝 **完整日志**: 详细的日志记录，方便调试

## 系统要求

- Minecraft: 1.21.11
- Fabric Loader: >= 0.18.4
- Fabric API: 最新版本
- Java: >= 21
- Jython: 2.7.3 (内置)

## 安装

1. 下载最新版本的 Jython Mod
2. 将 JAR 文件放入 Minecraft 的 `jymods` 文件夹
3. 启动游戏

## 配置

模组配置文件位于 `config/jython-mod.json`，默认配置如下：

```json
{
  "enabled": true,
  "debugMode": false,
  "scriptPath": "/assets/jython-mod/jython/main.py",
  "modsPaths": "{gamedir}/jython-mods",
  "autoReload": false,
  "scriptTimeout": 30
}
```

### 配置项说明

- `enabled`: 是否启用模组（默认：true）
- `debugMode`: 调试模式，输出详细日志（默认：false）
- `scriptPath`: 主脚本路径（默认：/assets/jython-mod/jython/main.py）
- `modsPaths`: Jython 模块搜索路径，支持 `;` 分隔多个路径，支持 `{gamedir}` 占位符（默认：{gamedir}/jymods）
- `autoReload`: 是否自动重载脚本（默认：false）
- `scriptTimeout`: 脚本执行超时时间，单位秒（默认：30）

## 使用方法

### 创建 Jython 模块

1. 在游戏目录下创建 `jython-mods` 文件夹
2. 创建一个 ZIP 文件，包含以下结构：
   ```
   your_mod.zip
   ├── main.py          # 主模块（公共环境）
   ├── client.py        # 客户端模块（可选）
   └── server.py        # 服务端模块（可选）
   ```

> 注意！这些文件可替换成含__init__.py的文件夹

### 模块示例

**main.py** (公共环境):
```Jython
# 这个函数会在模组加载时自动调用
def main():
    LOGGER.info("Hello from Jython!")
    
    # 你的代码...
```

**client.py** (客户端):
```Jython
def main():
    LOGGER.info("Client-side code running")
    # 客户端特定代码...
```

**server.py** (服务端):
```Jython
def main():
    LOGGER.info("Server-side code running")
    # 服务端特定代码...
```

### 可用变量

在 Jython 脚本中，以下变量可以直接使用：

- `LOGGER`: 日志记录器（Java Logger 的封装）
- `ENV_TYPE`: 环境类型（"common"、"client" 或 "server"）
- `GAME_DIR`: 游戏目录路径

### 日志记录

使用提供的 LOGGER 记录日志：

```Jython
LOGGER.info("Information message")
LOGGER.warn("Warning message")
LOGGER.error("Error message")
LOGGER.debug("Debug message")
```

## 资源包支持

Jython Mod 支持从 Jython 模块 ZIP 文件中自动提取资源并生成资源包。

### 支持的资源文件夹

- `assets` - 资源文件
- `atlases` - 纹理图集
- `blockstates` - 方块状态
- `equipment` - 装备
- `font` - 字体
- `items` - 物品
- `lang` - 语言文件
- `models` - 模型
- `particles` - 粒子效果
- `post_effect` - 后处理效果
- `sounds` - 声音
- `shaders` - 着色器
- `texts` - 文本
- `textures` - 纹理
- `waypoint_style` - 路点样式

### 根文件

支持以下根文件：
- `gpu_warnlist.json`
- `regional_compliancies.json`
- `sounds.json`

生成的资源包会自动保存为 `resourcepacks/JythonModAssets.zip`。

## 数据包支持

Jython Mod 支持从 Jython 模块 ZIP 文件中自动提取数据并生成数据包。

### 数据包结构

在 ZIP 文件中包含 `data/` 文件夹：
```
your_mod.zip
├── data/
│   ├── your_namespace/
│   │   ├── functions/
│   │   ├── loot_tables/
│   │   └── ...
```

生成的数据包会自动复制到所有存档的 `datapacks/` 文件夹中。

## 开发

### 构建项目

```bash
# 克隆仓库
git clone https://github.com/RyanLuoJiayuan1120/jython-mod.git
cd jython-mod

# 使用 Gradle 构建
./gradlew build

# 构建的 JAR 文件位于 build/libs/
```

### 开发环境

- JDK: 21+
- Minecraft: 1.21.11

## 调试

启用调试模式以获取详细的日志信息：

1. 打开 `config/jython-mod.json`
2. 将 `debugMode` 设置为 `true`
3. 重启游戏

## 故障排除

### 模块无法加载

1. 检查 ZIP 文件是否包含 `main.py`
2. 确认 ZIP 文件路径正确
3. 查看日志文件了解详细错误信息

### 脚本执行超时

如果脚本执行时间过长，可以在配置中增加 `scriptTimeout` 的值。

### Jython 语法错误

Jython 使用 Jython 2.7 语法，请注意与 Jython 3 的区别：
- 使用 `print "text"` 而不是 `print("text")`
- `str` 和 `unicode` 是不同的类型
- 没有 `range` 函数，使用 `xrange` 代替

### 关于Mixin

> Mixin 是 Fabric 生态系统中强大重要的工具，其主要用途是修改基本游戏中的已存在的代码，
> 可以是通过注入自定义的逻辑、移除机制或者修改值。注意 Mixin 只能使用 Java 语言编写，即便你的项目使用 Kotlin 或者其他语言。
>                                               ———— Fabric Wiki

所以我们不会支持Mixin

## 许可证

本项目采用 CC0-1.0 许可证 - 详见 [LICENSE](LICENSE) 文件

## 作者

- RyanLuo2011

## 链接

- [FabricMC](https://fabricmc.net/)
- [源代码](https://github.com/RyanLuoJiayuan1120/jython-mod)

## 贡献

欢迎提交 Issue 和 Pull Request！

## 致谢

- [FabricMC](https://fabricmc.net/) - 提供强大的 Fabric 模组框架
- [Jython](https://www.jython.org/) - 提供 Java 中的 Jython 实现
- [AutoConfig](https://github.com/shedaniel/AutoConfig) - 提供配置系统

---

**注意**: 本项目仍在开发中，可能会有一些不完善的地方。欢迎提供反馈和建议！