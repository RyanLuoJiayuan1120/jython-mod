# 模块结构

Python 模块是一个 ZIP 文件，放入配置的 `modsPaths`（默认 `{gamedir}/jymods`）目录后由模组自动加载。

## ZIP 结构

```
your_mod.zip
├── config.json          # 模组元信息（可选）
├── main.py              # 公共环境入口（必需）
├── client.py            # 客户端入口（可选）
├── server.py            # 服务端入口（可选）
└── Lib/                 # 内置第三方纯 Python 包（可选）
    ├── requests/
    └── termcolor.py
```

> 也可用含 `__init__.py` 的文件夹替代 `.py` 文件。

## 入口约定

每个入口文件需定义 `main()` 函数，加载时自动调用：

```python
def main():
    LOGGER.info("Hello from Python!")
```

| 入口 | 环境 | 说明 |
|------|------|------|
| `main.py` | 公共环境 | 两端都会执行，必需 |
| `client.py` | 客户端环境 | 仅客户端执行（Fabric / NeoForge） |
| `server.py` | 服务端环境 | 仅服务端执行 |

> **Paper 平台没有客户端环境**：只执行 `main.py` 与 `server.py`，`client.py` 会被跳过。详见 [platforms/paper.md](../platforms/paper.md)。

## 可用变量

脚本运行时注入以下变量：

| 变量 | 类型 | 说明 |
|------|------|------|
| `LOGGER` | Logger | 日志记录器（SLF4J），支持 `info` / `warn` / `error` / `debug` |
| `ENV_TYPE` | str | 环境类型：`"common"` / `"client"` / `"server"` |
| `GAME_DIR` | str | 游戏/服务器根目录路径 |
| `Script` | str | 当前模块 ZIP 的绝对路径 |
| `API` | ApiView | 对外 API 注册表只读视图（其它 Java 模组注册的 API），详见 [对外 API](api.md) |

```python
def main():
    LOGGER.info("ENV=%s GAME_DIR=%s", ENV_TYPE, GAME_DIR)
    LOGGER.debug("module path: %s", Script)
    # 调用其它 Java 模组注册的 API（见 docs/usage/api.md）
    api = API["mymod"]["TradeApi"]
    api.greet("Python")
```

> 注意：`LOGGER` 等方法调用支持 SLF4J 风格占位符 `{}`，也支持 Python 的 `%` 格式化拼接。

## 模组元信息（config.json）

可选。用于日志展示模组名称与依赖：

```json
{
  "name": "My Mod",
  "version": "1.0.0",
  "dependencies": ["termcolor"]
}
```

- `name` / `version`：仅用于日志展示。
- `dependencies`：已内置在 `Lib/` 中的第三方包名列表，用于日志校验。

## 相关文档

- 辅助库 API：[libs.md](libs.md)
- 内置第三方包打包：[packages.md](packages.md)
