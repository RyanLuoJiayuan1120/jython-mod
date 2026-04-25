# Jython Mod

[![Build and Release](https://github.com/RyanLuoJiayuan1120/jython-mod/actions/workflows/build.yml/badge.svg)](https://github.com/RyanLuoJiayuan1120/jython-mod/actions)
[![Discord](https://img.shields.io/badge/Discord-Join-5865F2?logo=discord&logoColor=white)](https://discord.gg/FfkFkdhb)
[![QQ群](https://img.shields.io/badge/QQ群-1094507536-blue)](https://qm.qq.com/q/1094507536)

> ⚠️ **注意：Jython ≠ Python！** Jython 是 Python 2.7 的 Java 实现，语法与 Python 2.x 一致。

一个基于 Jython 的 Minecraft Fabric 模组，允许使用 Jython 编写模组功能。

## 特性

- 🐍 **Jython 支持**：使用 Jython 2.7.3 引擎
- 📦 **模块化加载**：从 ZIP 文件加载 Jython 模块
- 🔧 **灵活配置**：基于 AutoConfig 的配置系统
- 🎨 **自动资源包 / 数据包处理**
- 📝 **完整日志**：方便调试

## 系统要求

- Minecraft: 1.21.11
- Fabric Loader: >= 0.18.4
- Java: >= 21

## 安装

1. 下载最新版本的 Jython Mod
2. 将 JAR 放入 `mods/` 文件夹
3. 将 Jython 模块 ZIP 放入 `jymods/` 文件夹
4. 启动游戏

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
    LOGGER.info("Hello from Jython!")
```

## 文档

- [使用指南](docs/USAGE.md) — 配置、API、资源包 / 数据包
- [Java → Jython 教程](docs/JAVA_TO_JTHON_TUTORIAL.md) — 语法转换、类型对照
- [故障排除](docs/TROUBLESHOOTING.md) — 常见问题与调试

## 开发

```bash
git clone https://github.com/RyanLuoJiayuan1120/jython-mod.git
cd jython-mod
./gradlew build
```

构建的 JAR 位于 `build/libs/`。

## 许可证

本项目采用 [CC0-1.0](LICENSE) 许可证。

**免责声明：本人不承担任何因为安装不正当的 jython 模组导致的任何后果。**

---

- [FabricMC](https://fabricmc.net/)
- [源代码](https://github.com/RyanLuoJiayuan1120/jython-mod)
- [Mc百科](https://www.mcmod.cn/class/25999.html)
- Discord: [https://discord.gg/FfkFkdhb](https://discord.gg/FfkFkdhb)
