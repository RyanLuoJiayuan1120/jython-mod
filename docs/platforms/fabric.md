# Fabric 平台

## 概述

Fabric 是模组的主要目标平台，支持完整的客户端 + 服务端环境、Cloth Config 配置 GUI、资源包/数据包生成与字节码转换。

## 入口与加载时机

- 入口类：`net.luojiayuan.jython.mod.Jythonmod`（`ModInitializer`）
- 加载时机：
  - `main`（公共环境）：`onInitialize()` 时执行
  - `client`（客户端环境）：`JythonModClient`（`ClientModInitializer`）执行
  - `server`（服务端环境）：`JythonModServer`（`DedicatedServerModInitializer`）执行
- 注册条目见 `fabric.mod.json` 的 `entrypoints`。

## 类名与映射

生产环境中 Minecraft 类名是 **intermediary**（如 `class_1792`）。模组内置 McReflect import hook，`from net.minecraft.xxx import Yyy` 会自动映射为运行时类名，无需手动处理。

- 开发环境（`runClient` / `runServer`）使用 Yarn 名，直通。
- 生产环境通过 `mappings.tiny`（mojmap→intermediary）自动转换。

## 配置方式

- Cloth Config，配置文件 `config/jython-mod.json`，支持游戏内 GUI。
- 依赖声明：`fabric.mod.json` 的 `depends` 要求 `cloth-config >= 21.11.153`。

## 字节码转换

支持。`BytecodeHelper.registerTransformer()` 注册的转换器在 Mixin 转换完成后执行（见 [libs.md](../usage/libs.md) 的 BytecodeHelper 小节）。

## 资源包 / 数据包

支持。模组自动从模块 ZIP 提取资源生成资源包与数据包（见 [resources.md](../usage/resources.md)）。

## 构建与部署

```bash
./gradlew build
```

产物：`build/libs/jython-mod-<version>.jar`（内含 GraalPy 运行时，约 56MB）。

部署：将 JAR 放入 `mods/` 文件夹，将 Python 模块 ZIP 放入 `jymods/` 文件夹，启动游戏。

> 依赖：Fabric Loader >= 0.18.4、Fabric API、Cloth Config。见 `fabric.mod.json` 的 `depends`。
