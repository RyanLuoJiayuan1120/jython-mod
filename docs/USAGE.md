# 使用指南

本模组支持 **GraalPy（Python 3）** 与 **Jython（Python 2.7）** 两种脚本引擎。除非有旧代码兼容需求，否则建议新模块优先使用 **GraalPy**，以获得现代 Python 语法和更活跃的生态支持。本文档中的示例默认使用两种引擎都支持的语法。

## 配置

配置文件位于 `config/jython-mod.json`：

```json
{
  "enabled": true,
  "debugMode": false,
  "scriptPath": "/assets/jython-mod/jython/main.py",
  "modsPaths": "{gamedir}/jymods",
  "autoReload": false,
  "scriptTimeout": 30
}
```

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `enabled` | 是否启用模组 | `true` |
| `debugMode` | 调试模式，输出详细日志 | `false` |
| `scriptPath` | 主脚本路径 | `/assets/jython-mod/jython/main.py` |
| `modsPaths` | 模块搜索路径，支持 `;` 分隔多个路径 | `{gamedir}/jymods` |
| `autoReload` | 是否自动重载脚本 | `false` |
| `scriptTimeout` | 脚本执行超时时间（秒） | `30` |

## 创建 Python 模块

Python 模块是一个 ZIP 文件，结构如下：

```
your_mod.zip
├── main.py          # 主模块（公共环境）
├── client.py        # 客户端模块（可选）
└── server.py        # 服务端模块（可选）
```

> 也可用含 `__init__.py` 的文件夹替代 `.py` 文件。

每个入口文件需定义 `main()` 函数，加载时自动调用：

```python
def main():
    LOGGER.info("Hello from Python!")
```

### 可用变量

- `LOGGER` — 日志记录器
- `ENV_TYPE` — 环境类型（`"common"` / `"client"` / `"server"`）
- `GAME_DIR` — 游戏目录路径

## 内置辅助库

### item — 物品注册

```python
from net.minecraft.world.item import Item
from net.luojiayuan.jython.mod.libs import item

my_item = item.register("my_item", "mymod", lambda: Item(), Item.Properties())
```

### block — 方块注册

```python
from net.minecraft.world.level.block import Block
from net.minecraft.world.level.block.state import BlockBehaviour
from net.luojiayuan.jython.mod.libs import block

props = BlockBehaviour.Properties.of()
my_block = block.register("my_block", "mymod", lambda p: Block(p), props, True)
```

### MinecraftClasses — 类快捷引用

```python
from net.luojiayuan.jython.mod.libs import MinecraftClasses as mc

props = mc.Block_Properties.of()
block = mc.Block(props)
```

### 直接导入 Minecraft 类

项目已内置 McReflect import hook，对 `net.minecraft.*`、`com.mojang.*`、`net.fabricmc.*` 下的类会自动做 yarn → obf 映射。因此可以直接使用常规 import 语法：

```python
from net.minecraft.world.item import Item
from net.minecraft.world.level.block import Block
from net.minecraft.resources import Identifier

item = Item()
id = Identifier.fromNamespaceAndPath("mymod", "my_item")
```

> 该 hook 同时支持 Jython 与 GraalPy 环境。

### McReflect — 反射调用（无需 import Java 类）

如果目标类不在上述命名空间，或需要动态调用方法 / 构造函数 / 访问字段，可以使用 `McReflect.call()`：

```python
from net.luojiayuan.jython.mod.mapping import McReflect
from java.lang import String, Float

# 调用静态方法
id = McReflect.call(
    "net.minecraft.resources.Identifier",
    "fromNamespaceAndPath",
    None,
    String("mymod"),
    String("my_block")
)

# 调用构造函数（使用 "<init>"）
settings = McReflect.call(
    "net.minecraft.world.level.block.state.BlockBehaviour$Properties",
    "<init>",
    None
)
```

### GameDirHelper — 游戏目录工具

```python
from net.luojiayuan.jython.mod.utils import GameDirHelper

game_dir = GameDirHelper.getGameDirPath()
mods_dir = GameDirHelper.getModsDirPath()
config_dir = GameDirHelper.getConfigDirPath()
```

### ModConfig — 访问配置

```python
from net.luojiayuan.jython.mod import Jythonmod

if Jythonmod.CONFIG.debugMode:
    LOGGER.info("Debug mode enabled")
```

### BytecodeHelper — 注册字节码转换器

模组在 `preLaunch` 阶段通过 `BytecodeHook` 钩住了 Mixin Transformer，允许在 Mixin 转换完成后继续修改类的字节码。

GraalPy 用法（推荐）：

```python
from net.luojiayuan.jython.mod.bytecode import BytecodeHelper

def my_transform(className, classBytes):
    # className: 类的全限定名，如 "net.minecraft.class_1234"
    # classBytes: 字节数组
    # 返回修改后的字节数组，无需修改则直接返回 classBytes
    LOGGER.info("Transforming: " + className)
    return classBytes

BytecodeHelper.registerTransformer(my_transform)
```

Jython 用法：

```python
from net.luojiayuan.jython.mod.bytecode import BytecodeHelper, BytecodeTransformer

class MyTransformer(BytecodeTransformer):
    def transform(self, className, classBytes):
        LOGGER.info("Transforming: " + className)
        return classBytes

BytecodeHelper.registerTransformer(MyTransformer())
```

> 转换器在类加载时同步执行，应避免耗时操作；错误会被捕获并记录到日志。

## 资源包支持

模组会自动从 ZIP 中提取资源文件，生成 `resourcepacks/JythonModAssets.zip`。

支持的资源文件夹：`assets`、`atlases`、`blockstates`、`equipment`、`font`、`items`、`lang`、`models`、`particles`、`post_effect`、`sounds`、`shaders`、`texts`、`textures`、`waypoint_style`

支持的根文件：`gpu_warnlist.json`、`regional_compliancies.json`、`sounds.json`

## 数据包支持

在 ZIP 中包含 `data/` 文件夹：

```
your_mod.zip
└── data/
    └── your_namespace/
        ├── functions/
        ├── loot_tables/
        └── ...
```

生成的数据包会自动复制到所有存档的 `datapacks/` 文件夹中。
