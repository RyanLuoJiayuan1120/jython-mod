# Paper 只是实验性的！！！！！

# Jython Mod

[![Build and Release](https://github.com/RyanLuoJiayuan1120/jython-mod/actions/workflows/build.yml/badge.svg)](https://github.com/RyanLuoJiayuan1120/jython-mod/actions)
[![Discord](https://img.shields.io/badge/Discord-Join-5865F2?logo=discord&logoColor=white)](https://discord.gg/FfkFkdhb)
[![QQ群](https://img.shields.io/badge/QQ群-1094507536-blue)](https://qm.qq.com/q/1094507536)

一个基于 GraalPy 的 Minecraft 模组，允许使用 Python 3 编写模组功能。支持 **Fabric / NeoForge / Paper** 三个平台。

## 特性

- 🐍 **GraalPy 引擎**：运行 Python 3 脚本
- 🎮 **多平台**：Fabric、NeoForge、Paper（各平台差异见文档）
- ⚙️ **字节码转换器**：通过 `BytecodeHelper` 在 Mixin 之后注册自定义类转换逻辑（Fabric / NeoForge；Paper 不支持）
- 🧩 **模块化**：Python 模块以 ZIP 形式放入 `jymods/`，即插即用

## 系统要求

- Minecraft: 1.21.11
- Java: >= 21

平台额外依赖见各平台文档。

## 安装

1. 下载对应平台的 Jython Mod 文件（Fabric/NeoForge 为 mod JAR，Paper 为插件 JAR）
2. Fabric / NeoForge：将 JAR 放入 `mods/` 文件夹；Paper：放入 `plugins/` 文件夹
3. 将 Python 模块 ZIP 放入 `jymods/` 文件夹（Paper 为 `plugins/jymods/`）
4. 启动游戏 / 服务器

## 快速开始

创建 ZIP 文件：

```
your_mod.zip
├── main.py          # 公共环境入口
├── client.py        # 客户端代码（可选）
└── server.py        # 服务端代码（可选）
```

`main.py` 示例：

```python
def main():
    LOGGER.info("Hello from Python!")
```

## 文档

- **使用指南**
  - [配置](docs/usage/config.md) — 配置文件与字段说明
  - [模块结构](docs/usage/module.md) — ZIP 结构、入口约定、可用变量
  - [辅助库 API](docs/usage/libs.md) — item / block / MinecraftClasses / BytecodeHelper
  - [对外 API](docs/usage/api.md) — 其它 Java 模组向 Python 暴露 API（JythonModApi / jython_api）
  - [Maven 依赖仓库](docs/usage/maven.md) — 通过 Cloudflare Pages 发布 jython-mod-api 供模组依赖
  - [资源包与数据包](docs/usage/resources.md)
  - [第三方 Python 包](docs/usage/packages.md)
- **平台**
  - [Fabric](docs/platforms/fabric.md)
  - [NeoForge](docs/platforms/neoforge.md)
  - [Paper](docs/platforms/paper.md)
- [特殊语法与功能](docs/special-syntax.md) — Java 互操作、反射调用
- [故障排除](docs/TROUBLESHOOTING.md)

## 开发

```bash
git clone https://github.com/RyanLuoJiayuan1120/jython-mod.git
cd jython-mod
./gradlew build          # Fabric
./gradlew neoforgeJar    # NeoForge
./gradlew paperJar       # Paper
```

构建的 JAR 位于 `build/libs/`。

## 许可证

本项目采用 [MIT](LICENSE) 许可证。

**免责声明：本人不承担任何因为安装不正当的 jython 模组导致的任何后果。**

---

- [FabricMC](https://fabricmc.net/)
- [源代码](https://github.com/RyanLuoJiayuan1120/jython-mod)
- [Mc百科](https://www.mcmod.cn/class/25999.html)
- Discord: [https://discord.gg/FfkFkdhb](https://discord.gg/FfkFkdhb)
