# 故障排除

## 调试模式

启用调试模式以获取详细日志：

1. 打开 `config/jython-mod.json`
2. 将 `debugMode` 设为 `true`
3. 重启游戏 / 服务器

## 常见问题

### 模块无法加载

1. 检查 ZIP 文件是否包含 `main.py`（或含 `__init__.py` 的 `main/` 文件夹）
2. 确认 ZIP 文件已放入 `jymods/` 目录（Paper 为 `plugins/jymods/`）
3. 查看日志（`logs/latest.log`）了解详细错误信息

### 找不到方法 / 构造函数

若使用 `McReflect.call()` 出现 `找不到方法：<init>` 或类似错误：

- 构造函数需使用 `"<init>"` 作为方法名
- 确保参数数量和类型与目标方法匹配
- 嵌套类名使用 `$` 写法（如 `BlockBehaviour$Properties`）
- 查看日志中的映射信息确认类名解析正确

### `from net.minecraft.xxx import Yyy` 报错

如果直接导入 Minecraft 类失败：

1. 确认类名拼写正确（嵌套类可用 `.` 或 `$`）
2. 检查日志中是否有 `McReflect import hook registered`
3. 生产环境下类映射由 `McReflect` 自动处理，若仍失败可改用 `McReflect.call()` 或 `McReflect.getClassName()` 排查
4. **Paper 平台**：`net.minecraft.*` 对插件不可见，应使用 `org.bukkit.*` API（见 [platforms/paper.md](platforms/paper.md)）

### 插件类报 `'JavaPackage' object is not callable`（Paper）

GraalPy 的宿主类加载器看不到插件自身类时会返回 JavaPackage 而非宿主类。确保使用最新版插件 jar（已修复 hostClassLoader 问题）；若仍出现，检查 `java.type()` / 导入语句的类名是否完整。

## 关于字节码转换（Mixin / Coremod）

> 传统上只能使用 Java 编写 Mixin / coremod 修改游戏代码。

本项目**不支持**在 Python 中直接编写 Mixin 类，但提供了 `BytecodeHelper` 字节码转换机制：可以在 GraalPy 中注册自定义转换器，在 Mixin 转换完成后继续修改类的字节码，从而实现类似的代码注入效果。

平台差异：
- **Fabric**：通过 Mixin 后的转换钩子生效（`BytecodeHook`，preLaunch 注册）
- **NeoForge**：通过 coremod 机制生效（`NeoForgeBytecodeProcessor`，`ClassProcessor` SPI）
- **Paper**：**不支持**字节码转换（服务端类在插件启用前已加载，无 transformer SPI）

详见 [usage/libs.md](usage/libs.md) 的 BytecodeHelper 小节与 [platforms](../platforms/) 各平台文档。
