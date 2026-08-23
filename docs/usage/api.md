# 对外 API（JythonModApi + jython_api）

本模组提供一套**双向接入点**：其它 Java 模组把自己的 API 类 / 实例注册进来，
运行在本模组中的 Python 脚本即可通过两种方式调用它们。

```
┌────────────────────┐   register()   ┌──────────────────────┐
│  其它 Java 模组      │ ──────────────▶ │  JythonModApi 注册表   │
│  (Fabric/NeoForge/  │                │  modId → apiName →  │
│   Paper)            │                │  Object              │
└────────────────────┘                └──────────┬───────────┘
                                                 │ 活引用（只读视图）
                                                 ▼
┌────────────────────┐                ┌──────────────────────┐
│  Python 脚本        │ ◀────────────── │  API 全局 / jython_api │
│  (main.py / ...)   │   API[...] 或    │  import 钩子          │
└────────────────────┘   from ... import
```

## 一、Java 侧：注册 API

### 依赖声明

其它模组在编译期依赖本模组（`compileOnly` / `modCompileOnly`），运行时本模组
的 jar 与它同时存在于 `mods/`（Fabric/NeoForge）或 `plugins/`（Paper）。

- **Maven 坐标**：`net.luojiayuan.jython.mod:jython-mod-api:<version>`，
  仓库地址与接入示例见 [maven.md](maven.md)（`jython-mod-api` 为 API-only
  构件，仅编译期使用）；
- **Fabric**：`fabric.mod.json` 里声明 `"depends": { "jython-mod": "*" }`；
  代码里 `import net.luojiayuan.jython.mod.api.JythonModApi;`
- **NeoForge**：`neoforge.mods.toml` 里声明对 `jythonmod` 的依赖；
- **Paper**：`paper-plugin.yml` 里 `depend: [jython-mod]`。

### 注册

```java
import net.luojiayuan.jython.mod.api.JythonModApi;

// 1) 注册实例：key 自动取类简单名（"TradeApi"）
JythonModApi.register("mymod", new TradeApi());

// 2) 注册 Class 对象：key 自动取类简单名（"Calculator"），Python 侧可调静态方法/构造
JythonModApi.register("mymod", Calculator.class);

// 3) 自定义 key
JythonModApi.register("mymod", "trade", new TradeApi());
```

**调用时机**：建议在**各平台最早**的初始化阶段注册，保证早于本模组运行
Python 脚本：

| 平台 | 推荐注册点 | 说明 |
|------|-----------|------|
| Fabric | `preLaunch` entrypoint | 早于任何 `main` entrypoint，必然早于 Python |
| NeoForge | `@Mod` 构造函数 | Python 在 `RegisterEvent` 阶段运行，晚于构造 |
| Paper | `onLoad()` | Python 在 `onEnable()` 运行，`onLoad` 必然更早 |

> **时序约束（重要）**：本模组在初始化阶段同步执行 Python 脚本。若你的模组
> 通过 `depends` 声明依赖本模组（Fabric/NeoForge 的 `depends` 会使你的模组
> **晚于**本模组初始化），则注册发生在 Python 脚本运行**之后**——需要"后
> 注册" API 的 Python 脚本应自行延后读取时机（例如注册事件回调、服务端启动
> 后的事件里再读）。注册表是活引用：**任何时刻注册的数据，Python 侧之后
> 任何时候都能读到**。

### 完整方法面

| 方法 | 说明 |
|------|------|
| `register(String modId, Object api)` | 注册，key 自动取类简单名 |
| `register(String modId, String apiName, Object api)` | 注册，自定义 key；可覆盖 |
| `get(String modId, String apiName)` | 获取；不存在抛 `IllegalArgumentException` |
| `getAll(String modId)` | 该模组全部 API 的只读 Map；不存在返回空 Map |
| `has(String modId, String apiName)` | 是否存在 |
| `modIds()` | 已注册模组 ID 集合 |
| `unregister(String modId)` | 注销整个模组 |
| `unregister(String modId, String apiName)` | 注销单个 API |

## 二、Python 侧：调用 API

脚本运行时注入全局 `API`（dict 风格只读视图），另有 `jython_api` 导入钩子。

### 方式一：全局 `API` 直取

```python
# 实例：直接调用方法
api = API["mymod"]["TradeApi"]
api.greet("Python")

# Class 对象：静态方法 / 静态字段 / 构造
calc = API["mymod"]["Calculator"]
calc.add(1, 2)          # 静态方法
calc.PI                 # 静态字段

# Class 对象：构造实例（注册的是类）
cls = API["mymod"]["TradeApiClass"]
inst = cls("参数")

# 嵌套类 + 链式 Builder
b = calc.Builder()
b.set(21).build()
```

### 方式二：import 钩子

```python
# 直取式：import 的即对象
from jython_api.mymod import TradeApi
TradeApi.greet("import")

# 模块式：先拿模组命名空间，再取 API
from jython_api import mymod
mymod.Calculator.add(3, 4)
```

### 接口面

| 操作 | 说明 |
|------|------|
| `API["modId"]` | 取模组命名空间；不存在抛 `KeyError` |
| `API["modId"]["apiName"]` | 取 API；不存在抛 `KeyError` |
| `"modId" in API` / `"apiName" in API["modId"]` | 存在性判断 |
| `API.get("modId")` / `API["modId"].get("apiName", default)` | 温和取值 |
| `API.keys()` / `API["modId"].keys()` | 列出外层模组 ID / 内层 API 名 |
| `API.items()` / `values()` / `len(API)` | 标准 dict 风格 |

### 值包装规则

- 注册的是**实例**：直接返回宿主对象，方法调用即实例方法；
- 注册的是 **Class 对象**：包装为可调用引用——
  - 属性访问 → 先查嵌套类，再静态字段，最后静态方法；
  - `类(...)` → 构造实例；
  - 返回值若是 Class 继续包装（支持链式 Builder）。

### 错误处理

```python
try:
    API["不存在的模组"]
except KeyError as e:
    pass

try:
    from jython_api.不存在的模组 import X
except ImportError as e:
    pass
```

## 三、示例

- Java 侧完整示例模组：`examples/api-demo-mod/`（Fabric/NeoForge/Paper 三平台
  入口 + `ApiDemoCore.register()`），构建任务 `./gradlew demoJars`；
- Python 侧示例：`examples/api-demo-python/main.py`（演示直取、import 钩子、
  Class 包装、链式、错误处理），构建任务 `./gradlew demoPythonZip`。

冒烟测试：将 `build/libs/*-demo-*.jar` 放入对应平台 mods/plugins 目录，
将 `build/libs/api-demo-python.zip` 放入 `jymods/` 目录，启动后查看日志中
`JythonModApi demo` 输出。

## 相关文档

- 模块结构与可用变量：[module.md](module.md)
- Java 互操作（McReflect 等）：[special-syntax.md](../special-syntax.md)
