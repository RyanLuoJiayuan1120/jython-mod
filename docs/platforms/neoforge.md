# NeoForge 平台

## 概述

NeoForge 支持完整的客户端 + 服务端环境、Cloth Config 配置 GUI、资源包/数据包生成，字节码转换通过 coremod 机制实现。

## 入口与加载时机

- 入口类：`net.luojiayuan.jython.mod.JythonModNeoForge`（`@Mod("jythonmod")`）
- **注册时机差异**：NeoForge 在模组构造期冻结注册表，Python 模组不能在构造器里注册物品/方块。因此 `main`（公共环境）延迟到 **`RegisterEvent`** 阶段执行（此时注册表可写），`client` / `server` 环境分别在 `FMLClientSetupEvent` / `FMLDedicatedServerSetupEvent` 执行（按 `FMLEnvironment.getDist()` 判断）。
- 各环境仅执行一次（`AtomicBoolean` 防重）。

## 类名与映射

NeoForge 使用 **官方（Mojang）映射**，类名与 Yarn 名在 1.21.11 一致，可直通使用，无需 intermediary 转换。

## 配置方式

- Cloth Config，配置文件 `config/jython-mod.json`，支持游戏内 GUI。
- 依赖声明：`META-INF/neoforge.mods.toml` 要求 `cloth_config >= 21.11.152`。

## 字节码转换

支持，通过 coremod 机制：`NeoForgeBytecodeProcessor` 实现了 `ClassProcessor` SPI（`META-INF/services/net.neoforged.neoforgespi.transformation.ClassProcessor`），在 Mixin 与 COMPUTING_FRAMES 之后处理类字节码。Python 侧仍用 `BytecodeHelper.registerTransformer()`（见 [libs.md](../usage/libs.md)）。

## 资源包 / 数据包

支持。与 Fabric 相同（见 [resources.md](../usage/resources.md)）。

## 构建与部署

```bash
./gradlew neoforgeJar
```

产物：`build/libs/jython-mod-<version>-neoforge.jar`（内含 GraalPy 运行时与 coremod 服务文件）。

部署：将 JAR 放入 `mods/` 文件夹，将 Python 模块 ZIP 放入 `jymods/` 文件夹，启动游戏。

> 依赖：NeoForge >= 21.11、Minecraft [1.21.11, 1.22)、Cloth Config。见 `neoforge.mods.toml`。
