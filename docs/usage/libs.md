# 辅助库 API

辅助库位于 `net.luojiayuan.jython.mod.libs` 包，提供物品/方块注册与类快捷引用。

> 其他常用 API 的归属：
> - `McReflect`（反射调用）→ [special-syntax.md](../special-syntax.md)
> - `GameDirHelper`（目录工具）→ [utils 小节](#其他工具类)
> - `BytecodeHelper`（字节码转换）→ [BytecodeHelper 小节](#bytecodehelper--字节码转换)

## item — 物品注册

```python
from net.minecraft.world.item import Item
from net.luojiayuan.jython.mod.libs import item

my_item = item.register("my_item", "mymod", lambda: Item(), Item.Properties())
```

签名：

```python
item.register(name, modid, itemFactory, settings)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `name` | str | 物品名称（不含模组 ID 前缀） |
| `modid` | str | 模组 ID |
| `itemFactory` | callable | 物品工厂函数，接收 `Item.Properties` 返回物品实例 |
| `settings` | `Item.Properties` | 物品属性 |

返回注册后的物品实例。若物品已注册则直接返回已有实例（不会重复注册）。

## block — 方块注册

```python
from net.minecraft.world.level.block import Block
from net.minecraft.world.level.block.state import BlockBehaviour
from net.luojiayuan.jython.mod.libs import block

props = BlockBehaviour.Properties.of()

# 注册方块 + 对应物品
my_block = block.register("my_block", "mymod", lambda p: Block(p), props, True)

# 仅注册方块（技术性方块，无物品）
tech_block = block.register("tech_block", "mymod", lambda p: Block(p), props, False)
```

签名：

```python
block.register(name, modid, blockFactory, settings, shouldRegisterItem)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `name` | str | 方块名称（不含模组 ID 前缀） |
| `modid` | str | 模组 ID |
| `blockFactory` | callable | 方块工厂函数，接收 `BlockBehaviour.Properties` 返回方块实例 |
| `settings` | `BlockBehaviour.Properties` | 方块属性 |
| `shouldRegisterItem` | bool | 是否同时注册对应的方块物品 |

## MinecraftClasses — 类快捷引用

用于绕过 GraalPy 对嵌套类名解析的限制，以静态字段形式提供常用 Minecraft 类：

```python
from net.luojiayuan.jython.mod.libs import MinecraftClasses as mc

props = mc.Block_Properties.of()
block = mc.Block(props)

item_props = mc.Item_Properties()
item = mc.Item(item_props)
```

| 字段 | 对应类 |
|------|--------|
| `mc.Block` | `net.minecraft.world.level.block.Block` |
| `mc.Block_Properties` | `BlockBehaviour.Properties` |
| `mc.Item` | `net.minecraft.world.item.Item` |
| `mc.Item_Properties` | `Item.Properties` |
| `mc.BlockItem` | `net.minecraft.world.item.BlockItem` |

另提供静态工厂方法：`mc.createBlockProperties()`、`mc.createBlock(props)`、`mc.createItemProperties()`、`mc.createItem(props)`。

> 嵌套类 `.` / `$` 两种写法见 [special-syntax.md](../special-syntax.md) 的嵌套类访问一节；现代写法直接 `from net.minecraft.world.item import Item.Properties` 通常也可用。

## BytecodeHelper — 字节码转换

在类加载阶段注册自定义字节码转换器（在 Mixin 转换之后继续修改类的字节码）。

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

> **平台差异**：字节码转换的生效机制因平台而异——Fabric 通过 Mixin 后的转换钩子，NeoForge 通过 coremod（`NeoForgeBytecodeProcessor`），**Paper 端不支持字节码转换**。详见 [platforms](../platforms/)。

## 其他工具类

- `GameDirHelper`（`net.luojiayuan.jython.mod.utils`）— 游戏目录工具：

```python
from net.luojiayuan.jython.mod.utils import GameDirHelper

game_dir = GameDirHelper.getGameDirPath()
mods_dir = GameDirHelper.getModsDirPath()
config_dir = GameDirHelper.getConfigDirPath()
```

  常用方法：`getGameDirPath()` / `getModsDirPath()` / `getConfigDirPath()` / `getSavesDirPath()` / `getSubDirPath(sub)` 等。

## 相关文档

- 反射调用（`McReflect`）：[special-syntax.md](../special-syntax.md)
- 模块结构：[module.md](module.md)
