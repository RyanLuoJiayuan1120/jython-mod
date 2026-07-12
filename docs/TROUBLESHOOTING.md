# 故障排除

## 调试模式

启用调试模式以获取详细日志：

1. 打开 `config/jython-mod.json`
2. 将 `debugMode` 设为 `true`
3. 重启游戏

## 常见问题

### 模块无法加载

1. 检查 ZIP 文件是否包含 `main.py`（或含 `__init__.py` 的 `main/` 文件夹）
2. 确认 ZIP 文件已放入 `jymods/` 目录
3. 查看日志（`logs/latest.log`）了解详细错误信息

### 脚本执行超时

在配置中增加 `scriptTimeout` 的值（单位：秒）。

### 找不到方法 / 构造函数

若使用 `McReflect.call()` 出现 `找不到方法：<init>` 或类似错误：

- 构造函数需使用 `"<init>"` 作为方法名
- 确保参数数量和类型与目标方法匹配
- 查看日志中的 `DEBUG: yarn=... -> className=...` 确认类映射正确

### `from net.minecraft.xxx import Yyy` 报错

如果直接导入 Minecraft 类失败：

1. 确认类名使用 Yarn 命名（开发环境）或正确拼写
2. 检查日志中是否有 `McReflect import hook registered`
3. 生产环境下类映射由 `McReflect` 自动处理，若仍失败可改用 `McReflect.call()` 或 `McReflect.getClassName()` 排查

## 关于 Mixin

> Mixin 是 Fabric 生态中修改游戏代码的重要工具，传统上只能使用 Java 编写。

本项目**不支持**在 Jython 中直接编写 Mixin 类，但提供了 `BytecodeHelper` 字节码转换机制：可以在 Jython / GraalPy 中注册自定义转换器，在 Mixin 转换完成后继续修改类的字节码，从而实现类似的代码注入效果。详见 [USAGE.md](USAGE.md) 中的 `BytecodeHelper` 章节。
