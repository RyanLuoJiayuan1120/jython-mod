# Java 代码转 Jython 代码教程

> 本项目 API 文档（item / block / McReflect / MinecraftClasses 等）请查看 [USAGE.md](USAGE.md)。
> 本教程专注于 Java → Jython 的语法转换。

本教程介绍如何将 Java 代码转换为 Jython (Python for Java) 代码，特别针对本 Minecraft Fabric 模组开发项目。

## 目录

1. [环境准备](#环境准备)
2. [基础转换规则](#基础转换规则)
3. [类型转换](#类型转换)
4. [导入 Java 类](#导入-java-类)
5. [实际示例](#实际示例)
6. [常见问题](#常见问题)

---

## 环境准备

Jython 是 Python 语言的 Java 实现，可以：
- 直接导入和使用 Java 类
- 继承 Java 类
- 实现 Java 接口
- 调用 Java 方法

在本项目中，Jython 环境已配置好，Python 脚本位于 `src/main/resources/assets/jython-mod/jython/` 目录。

---

## 基础转换规则

### 1. 变量声明

**Java:**
```java
String name = "Steve";
int count = 42;
boolean isActive = true;
```

**Jython:**
```python
name = "Steve"
count = 42
isActive = True
```

### 2. 方法定义

**Java:**
```java
public void greet(String name) {
    System.out.println("Hello, " + name);
}

public int add(int a, int b) {
    return a + b;
}
```

**Jython:**
```python
def greet(name):
    print("Hello, " + name)

def add(a, b):
    return a + b
```

### 3. 条件语句

**Java:**
```java
if (value > 10) {
    System.out.println("Large");
} else if (value > 5) {
    System.out.println("Medium");
} else {
    System.out.println("Small");
}
```

**Jython:**
```python
if value > 10:
    print("Large")
elif value > 5:
    print("Medium")
else:
    print("Small")
```

### 4. 循环

**Java (传统 for 循环):**
```java
for (int i = 0; i < 10; i++) {
    System.out.println(i);
}
```

**Jython:**
```python
for i in range(10):
    print(i)
```

**Java (增强 for 循环):**
```java
List<String> items = Arrays.asList("A", "B", "C");
for (String item : items) {
    System.out.println(item);
}
```

**Jython:**
```python
items = ["A", "B", "C"]
for item in items:
    print(item)
```

---

## 类型转换

### 基本类型

| Java | Jython | 说明 |
|------|--------|------|
| `null` | `None` | 空值 |
| `true/false` | `True/False` | 布尔值（注意大小写）|
| `int`, `long` | `int`, `long` | 整数 |
| `float`, `double` | `float`, `double` | 浮点数 |
| `String` | `str` / `unicode` | 字符串 |

### 集合类型

**Java ArrayList → Python List:**
```java
// Java
List<String> list = new ArrayList<>();
list.add("item1");
list.add("item2");
```

```python
# Jython
list = ["item1", "item2"]
# 或使用 Java 类型
from java.util import ArrayList
list = ArrayList()
list.add("item1")
list.add("item2")
```

**Java HashMap → Python Dict:**
```java
// Java
Map<String, Integer> map = new HashMap<>();
map.put("key1", 100);
```

```python
# Jython
map = {"key1": 100}
# 或使用 Java 类型
from java.util import HashMap
map = HashMap()
map.put("key1", 100)
```

---

## 导入 Java 类

Jython 可以直接导入和使用 Java 类：

```python
# 导入单个类
from net.minecraft.world.item import Item
from net.minecraft.world.level.block import Block

# 导入整个包
from net.luojiayuan.jython.mod.libs import item, block

# 导入嵌套类
from net.minecraft.world.item import Item.Properties

# 使用 import 语句
import java.util.ArrayList
```

---

## 实际示例

### 示例 1: 注册物品

**Java 代码:**
```java
import net.minecraft.world.item.Item;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

public class MyMod {
    public static final Item MY_ITEM = new Item(new Item.Properties());

    public static void registerItems() {
        ResourceKey<Item> itemKey = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("mymod", "my_item")
        );
        Registry.register(BuiltInRegistries.ITEM, itemKey, MY_ITEM);
    }
}
```

**Jython 代码:**
```python
# 使用项目提供的辅助函数
from net.minecraft.world.item import Item
from net.luojiayuan.jython.mod.libs import item

# 方法1: 使用辅助函数（推荐）
my_item = item.register(
    "my_item",           # 物品名称
    "mymod",             # 模组ID
    lambda: Item(),      # 物品工厂函数
    Item.Properties()    # 物品属性
)

# 方法2: 直接使用 Java API
from net.minecraft.core import Registry
from net.minecraft.core.registries import BuiltInRegistries
from net.minecraft.resources import ResourceKey, Identifier
from net.minecraft.core.registries import Registries

my_item = Item()
item_key = ResourceKey.create(
    Registries.ITEM,
    Identifier.fromNamespaceAndPath("mymod", "my_item")
)
Registry.register(BuiltInRegistries.ITEM, item_key, my_item)
```

### 示例 2: 注册方块

**Java 代码:**
```java
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class MyMod {
    public static final Block MY_BLOCK = new Block(
        BlockBehaviour.Properties.of()
    );

    public static void registerBlocks() {
        // 注册方块
        ResourceKey<Block> blockKey = ResourceKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath("mymod", "my_block")
        );
        Registry.register(BuiltInRegistries.BLOCK, blockKey, MY_BLOCK);

        // 注册方块物品
        ResourceKey<Item> itemKey = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("mymod", "my_block")
        );
        BlockItem blockItem = new BlockItem(MY_BLOCK,
            new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
    }
}
```

**Jython 代码:**
```python
# 使用项目提供的辅助函数（推荐）
from net.minecraft.world.level.block import Block
from net.minecraft.world.level.block.state import BlockBehaviour
from net.luojiayuan.jython.mod.libs import block

# 创建方块属性
props = BlockBehaviour.Properties.of()

# 注册方块（自动注册物品）
my_block = block.register(
    "my_block",              # 方块名称
    "mymod",                 # 模组ID
    lambda props: Block(props),  # 方块工厂
    props,                   # 方块属性
    True                     # 是否注册物品
)

# 仅注册方块（不注册物品）
tech_block = block.register(
    "tech_block",
    "mymod",
    lambda props: Block(props),
    props,
    False  # 不注册物品
)
```

### 示例 3: 创建食物属性

**Java 代码:**
```java
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.consumables.Consumable;
import net.minecraft.world.item.consumables.Consumables;
import net.minecraft.world.item.consumponents.ApplyStatusEffectsConsumeEffect;

// 创建毒效果消耗品组件
public static final Consumable POISON_FOOD_CONSUMABLE = Consumables.defaultFood()
    .onConsume(new ApplyStatusEffectsConsumeEffect(
        new MobEffectInstance(MobEffects.POISON, 6 * 20, 1), 1.0f))
    .build();

// 创建食物属性
public static final FoodProperties POISON_FOOD = new FoodProperties.Builder()
    .alwaysEdible()
    .nutrition(4)
    .saturationMod(0.5f)
    .build();
```

**Jython 代码:**
```python
from net.minecraft.world.effect import MobEffects
from net.minecraft.world.effect import MobEffectInstance
from net.minecraft.world.food import FoodProperties
from net.minecraft.world.item.consumables import Consumable, Consumables
from net.minecraft.world.item.consumponents import ApplyStatusEffectsConsumeEffect

# 创建毒效果（持续6秒，中毒I级）
poison_effect = MobEffectInstance(MobEffects.POISON, 6 * 20, 1)
poison_consume_effect = ApplyStatusEffectsConsumeEffect(poison_effect, 1.0)

# 创建消耗品组件
POISON_FOOD_CONSUMABLE = Consumables.defaultFood() \
    .onConsume(poison_consume_effect) \
    .build()

# 创建食物属性
POISON_FOOD = FoodProperties.Builder() \
    .alwaysEdible() \
    .nutrition(4) \
    .saturationMod(0.5) \
    .build()
```

### 示例 4: 使用 Logger

**Java 代码:**
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyMod {
    public static final Logger LOGGER = LoggerFactory.getLogger("mymod");

    public void doSomething() {
        LOGGER.info("Starting operation");
        LOGGER.debug("Debug info: {}", someValue);
        LOGGER.warn("This is a warning");
        LOGGER.error("An error occurred", exception);
    }
}
```

**Jython 代码:**
```python
# Logger 由 Java 端传入，直接使用
LOGGER.info("Starting operation")
LOGGER.debug("Debug info: " + str(some_value))
LOGGER.warn("This is a warning")
LOGGER.error("An error occurred: " + str(exception))
```

### 示例 5: Lambda 表达式转换

**Java 代码:**
```java
// Lambda 表达式
Function<Item.Properties, Item> factory = props -> new Item(props);

// 方法引用
items.forEach(System.out::println);
```

**Jython 代码:**
```python
# 使用 Python lambda
factory = lambda props: Item(props)

# 使用 Python 循环
for item in items:
    print(item)
```

### 示例 6: 异常处理

**Java 代码:**
```java
try {
    doSomething();
} catch (IOException e) {
    LOGGER.error("IO Error", e);
} catch (Exception e) {
    LOGGER.error("General error", e);
} finally {
    cleanup();
}
```

**Jython 代码:**
```python
try:
    do_something()
except IOException, e:
    LOGGER.error("IO Error: " + str(e))
except Exception, e:
    LOGGER.error("General error: " + str(e))
finally:
    cleanup()
```

---

## 常见问题

### Q1: 如何处理 Java 的重载方法？

Jython 会自动选择最匹配的方法：

```python
# Java 有多个 println 方法
# println(String), println(int), println(Object)
System.out.println("text")   # 调用 println(String)
System.out.println(42)       # 调用 println(int)
```

### Q2: 如何创建 Java 数组？

```python
# 方法1: 使用 Jython 数组（会自动转换）
arr = [1, 2, 3, 4, 5]

# 方法2: 创建 Java 数组
from jarray import array
java_array = array([1, 2, 3], 'i')  # 'i' 表示 int

# 方法3: 使用 Java 类
from java.lang import String
string_array = String(["A", "B", "C"])
```

### Q3: 如何处理接口和回调？

```python
from java.lang import Runnable

# 方法1: 使用 Python 函数（推荐）
def my_run():
    print("Running...")

# Jython 会自动适配
runnable = my_run

# 方法2: 显式实现接口
class MyRunnable(Runnable):
    def run(self):
        print("Running...")

runnable = MyRunnable()
```

### Q4: Python 2 vs Python 3 语法？

本项目使用 Jython 2.x，需要注意：

```python
# print 函数形式（推荐）
print("Hello")

# 字符串类型
text = "Hello"          # str 类型（字节串）
unicode_text = u"Hello" # unicode 类型

# 异常语法
except Exception, e:    # 正确
# except Exception as e:  # Python 3 语法，不支持
```

### Q5: 如何访问 Java 静态成员？

```python
# 访问静态字段
from net.minecraft.core.registries import BuiltInRegistries
registry = BuiltInRegistries.ITEM

# 调用静态方法
from net.minecraft.resources import Identifier
id = Identifier.of("mod", "item")
```

---

## 进阶技巧

### 1. 使用 Java 泛型

Jython 不需要声明泛型类型：

```python
# Java: Map<String, Integer> map = new HashMap<>();
from java.util import HashMap
map = HashMap()
map.put("key", 123)  # 类型自动推断
```

### 2. 继承 Java 类

```python
from net.minecraft.world.item import Item

class CustomItem(Item):
    def __init__(self, props):
        Item.__init__(self, props)

    def useOn(self, context):
        # 自定义逻辑
        LOGGER.info("Item used!")
        return Item.useOn(self, context)
```

### 3. 实现多个接口

```python
from java.lang import Runnable, Comparable

class MyClass(Runnable, Comparable):
    def run(self):
        print("Running")

    def compareTo(self, other):
        return 0
```

---

## 快速参考

### Java → Jython 对照表

| Java | Jython |
|------|--------|
| `public/private/protected` | 无关键字，全部 public |
| `;` 分号 | 换行 |
| `{}` 花括号 | 缩进 |
| `==` (对象比较) | `is` 或 `==` |
| `equals()` | `==` (字符串/值比较) |
| `new Class()` | `Class()` |
| `instanceof` | `isinstance(obj, Class)` |
| `null` | `None` |
| `&&` \|\| `!` | `and`, `or`, `not` |
| `i++` | `i += 1` |

---


---

## 下一步

了解本项目的具体 API（注册物品/方块、反射调用、配置读取等），请查看 [USAGE.md](USAGE.md)。
