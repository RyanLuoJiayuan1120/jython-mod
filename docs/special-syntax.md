# 特殊语法与功能

本模组使用 **GraalPy（Python 3）** 作为脚本引擎。本文只收录**本模组特有**的语法与功能——即与 Java 互操作相关的部分。通用 Python 语法（变量、循环、条件、集合等）请参考 Python 官方文档。

## 导入 Java 类

Python 可以直接导入 Java 类：

```python
from net.minecraft.world.item import Item
from net.minecraft.world.level.block import Block
from net.minecraft.resources import Identifier

item = Item()
id = Identifier.fromNamespaceAndPath("mymod", "my_item")
```

**Minecraft 类自动映射**：模组为 `net.minecraft.*`、`com.mojang.*`、`net.fabricmc.*` 注册了 import hook（McReflectFinder）。生产环境（类名被混淆）下，上述 `from net.minecraft.xxx import Yyy` 语句会自动通过 McReflect 解析为对应的运行时类名，无需手动处理映射。

> 平台差异：Fabric 生产环境映射为 intermediary 名，NeoForge 为官方名（直通），Paper 端面向 `org.bukkit.*`。详见 [platforms](../platforms/)。

其他 Java 类可用 `java.type()` 获取：

```python
import java
ArrayList = java.type('java.util.ArrayList')
```

## 嵌套类访问

嵌套类支持 `.` 与 `$` 两种写法，自动解析：

```python
from net.minecraft.world.item import Item

# 两种写法等价
props = Item.Properties()
props2 = Item$Properties()  # 或直接写 $ 形式
```

在 GraalPy 中直接访问宿主类的嵌套成员可能失败（尤其生产环境被混淆为 `class_1792$class_1793` 时），模组通过 `JavaClassRef` 包装自动尝试 `.` 与 `$` 两种形式，并回退到 `McReflect.call`。

> 若直接导入嵌套类失败，可使用 `MinecraftClasses` 快捷引用（`mc.Item_Properties`）或 `McReflect`（见下）。

## McReflect — 反射调用

`McReflect` 提供按类名字符串的反射调用，适合动态调用、构造函数、字段访问，以及 `net.minecraft.*` / `com.mojang.*` / `org.bukkit.*` 之外的类。

```python
from net.luojiayuan.jython.mod.mapping import McReflect

# 调用静态方法
id = McReflect.call(
    "net.minecraft.resources.Identifier",
    "fromNamespaceAndPath",
    None,          # 实例；静态方法传 None
    "mymod",
    "my_block"
)

# 调用构造函数（方法名使用 "<init>"）
settings = McReflect.call(
    "net.minecraft.world.level.block.state.BlockBehaviour$Properties",
    "<init>",
    None
)

# 调用实例方法
server = McReflect.call("org.bukkit.Bukkit", "getServer", None)
world = McReflect.call("org.bukkit.World", "getName", server)
```

### 方法签名

```python
McReflect.call(className, methodName, instance, *args)
```

- `className`：类全限定名（嵌套类可用 `.` 或 `$`）。
- `methodName`：方法名；构造函数用 `"<init>"`。
- `instance`：实例对象；静态方法/构造传 `None`。
- `args`：变长参数，自动匹配重载并做类型收窄。

### 其他方法

| 方法 | 说明 |
|------|------|
| `McReflect.getClassName(yarnClass)` | 返回运行时类名（字符串），未映射时抛异常 |
| `McReflect.getClass(yarnClass)` | 返回 Java `Class` 对象 |

> 找不到方法 / 构造函数时，检查参数数量与类型、嵌套类名写法（`$`），并确认日志中的类映射结果。

## 静态成员访问

```python
# 静态字段
from net.minecraft.core.registries import BuiltInRegistries
registry = BuiltInRegistries.ITEM

# 静态方法
from net.minecraft.resources import Identifier
id = Identifier.of("mod", "item")
```

## 继承 Java 类 / 实现 Java 接口

GraalPy 支持 Python 类继承 Java 类、实现 Java 接口：

```python
from net.minecraft.world.item import Item

class CustomItem(Item):
    def __init__(self, props):
        Item.__init__(self, props)

    def useOn(self, context):
        LOGGER.info("CustomItem used!")
        return Item.useOn(self, context)
```

实现接口：

```python
import java
Runnable = java.type('java.lang.Runnable')

# 方式1：Python 函数自动适配
def my_run():
    print("run!")
runnable = my_run

# 方式2：显式实现接口
class MyRunnable(Runnable):
    def run(self):
        print("run!")

runnable = MyRunnable()
```

## Lambda 与回调

GraalPy 自动将 Python 函数适配为 Java 函数式接口：

```python
# Java: Function<Item.Properties, Item> factory = props -> new Item(props);
factory = lambda props: Item(props)

# Java: items.forEach(System.out::println);
for item in items:
    print(item)
```

## 其他互操作提示

- **重载方法**：引擎自动选择最匹配的方法（如 `System.out.println` 传 str / int / Object）。
- **Java 数组**：Python 列表会自动转换；也可用 `java.type('java.lang.String[]')`。
- **泛型**：Python 无需声明泛型，`map = HashMap(); map.put("key", 123)` 类型自动推断。
- **异常处理**：Java 异常在 Python 中可直接 `except Exception as e` 捕获，`str(e)` 获取信息。

## 相关文档

- 辅助库 API（item / block / MinecraftClasses / BytecodeHelper）：[usage/libs.md](usage/libs.md)
- 模块结构（入口、可用变量）：[usage/module.md](usage/module.md)
