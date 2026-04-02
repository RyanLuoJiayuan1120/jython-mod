# Java 代码转 Jython 代码教程

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

## 项目特定 API

本模组提供了简化的 API：

### 注册 API

```python
# 注册物品
from net.luojiayuan.jython.mod.libs import item
item.register(name, modid, factory, properties)

# 注册方块
from net.luojiayuan.jython.mod.libs import block
block.register(name, modid, factory, properties, has_item)
```

### MinecraftClasses - Minecraft 类辅助工具

`MinecraftClasses` 是一个便捷的辅助类，用于简化从 Jython 访问 Minecraft 类的过程。它提供了常用 Minecraft 类的引用和创建实例的静态方法。

**基础用法：**

```python
from net.luojiayuan.jython.mod.libs import MinecraftClasses as mc

# ===== 类引用 =====
# 访问方块和物品相关的类
Block = mc.Block                    # Block 类
Block_Properties = mc.Block_Properties  # BlockBehaviour.Properties 类
Item = mc.Item                      # Item 类
Item_Properties = mc.Item_Properties    # Item.Properties 类
BlockItem = mc.BlockItem            # BlockItem 类
```

**创建方块示例：**

```python
from net.luojiayuan.jython.mod.libs import MinecraftClasses as mc
from net.luojiayuan.jython.mod.libs import block

# 方法1: 使用静态方法创建方块属性
props = mc.createBlockProperties()

# 方法2: 直接创建方块属性
props = mc.Block_Properties.of()

# 注册方块
my_block = block.register(
    "my_block",
    "mymod",
    lambda p: mc.Block(p),
    props,
    True
)
```

**创建物品示例：**

```python
from net.luojiayuan.jython.mod.libs import MinecraftClasses as mc
from net.luojiayuan.jython.mod.libs import item

# 方法1: 使用静态方法创建物品属性
item_props = mc.createItemProperties()

# 方法2: 直接创建物品属性
item_props = mc.Item_Properties()

# 注册物品
my_item = item.register(
    "my_item",
    "mymod",
    lambda: mc.Item(item_props),
    item_props
)
```

**创建方块物品示例：**

```python
from net.luojiayuan.jython.mod.libs import MinecraftClasses as mc
from net.minecraft.core import Registry
from net.minecraft.core.registries import BuiltInRegistries
from net.minecraft.resources import ResourceKey, Identifier
from net.minecraft.core.registries import Registries

# 假设已经有了一个方块实例
my_block = ...  # 你的方块

# 创建方块物品
item_props = mc.createItemProperties()
item_key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("mymod", "my_block"))
block_item = mc.createBlockItem(my_block, item_props.setId(item_key))
Registry.register(BuiltInRegistries.ITEM, item_key, block_item)
```

**完整的物品注册示例（带食物属性）：**

```python
from net.luojiayuan.jython.mod.libs import MinecraftClasses as mc
from net.luojiayuan.jython.mod.libs import item
from net.minecraft.world.effect import MobEffects
from net.minecraft.world.effect import MobEffectInstance
from net.minecraft.world.food import FoodProperties
from net.minecraft.world.item.consumables import Consumables
from net.minecraft.world.item.consumponents import ApplyStatusEffectsConsumeEffect

# 创建毒效果消耗品组件
poison_effect = MobEffectInstance(MobEffects.POISON, 6 * 20, 1)
poison_consume_effect = ApplyStatusEffectsConsumeEffect(poison_effect, 1.0)
POISON_FOOD_CONSUMABLE = Consumables.defaultFood().onConsume(poison_consume_effect).build()

# 创建食物属性
POISON_FOOD_COMPONENT = FoodProperties.Builder().alwaysEdible().build()

# 创建物品属性并设置食物
item_props = mc.createItemProperties()
item_props.food(POISON_FOOD_COMPONENT)

# 注册毒食物
poison_food = item.register(
    "poison_food",
    "mymod",
    lambda: mc.Item(item_props),
    item_props
)
```

**可用方法列表：**

```python
# 方块相关
mc.createBlockProperties()              # 创建方块属性
mc.createBlock(props)                   # 创建方块实例
mc.createBlockFromProps(props)          # 从属性创建方块（用于 lambda）

# 物品相关
mc.createItemProperties()               # 创建物品属性
mc.createItemSettings()                 # 创建物品设置（别名）
mc.createItem(props)                    # 创建物品实例
mc.createItemFromProps(props)           # 从属性创建物品（用于 lambda）
mc.createBlockItem(block, props)        # 创建方块物品实例
```

### 日志 API

```python
# Logger 由 Java 端自动传入，直接使用
LOGGER.info(message)
LOGGER.warn(message)
LOGGER.warning(message)  # warn 的别名
LOGGER.error(message)
LOGGER.debug(message)
```

### GameDirHelper - 游戏目录工具

用于获取 Minecraft 运行目录及相关子目录：

```python
from net.luojiayuan.jython.mod.utils import GameDirHelper

# ===== 获取游戏根目录 =====
game_dir = GameDirHelper.getGameDirPath()        # 字符串形式（推荐）
game_dir_file = GameDirHelper.getGameDirFile()   # File 对象
game_dir_path = GameDirHelper.getGameDir()       # Path 对象

# ===== 获取常用子目录 =====
mods_dir = GameDirHelper.getModsDirPath()        # mods 目录
config_dir = GameDirHelper.getConfigDirPath()    # config 目录
saves_dir = GameDirHelper.getSavesDirPath()      # saves 目录（存档）
screenshots_dir = GameDirHelper.getScreenshotsDirPath()  # screenshots 目录（截图）
resourcepacks_dir = GameDirHelper.getResourcePacksDir()  # resourcepacks 目录

# ===== 获取其他目录 =====
shaderpacks_dir = GameDirHelper.getShaderPacksDir()      # shaderpacks 目录
logs_dir = GameDirHelper.getLogsDir()                   # logs 目录
crash_reports_dir = GameDirHelper.getCrashReportsDir()   # crash-reports 目录

# ===== 获取自定义子目录 =====
# 相对于游戏根目录的子路径
custom_dir = GameDirHelper.getSubDirPath("custom/folder")  # 返回游戏目录/custom/folder
my_mod_dir = GameDirHelper.getSubDirPath("mymod")          # 返回游戏目录/mymod

# ===== 创建目录 =====
# 如果目录不存在则创建（返回 True 表示成功或已存在）
success = GameDirHelper.createDirIfNotExists(game_dir_path)
success = GameDirHelper.createDirIfNotExists(game_dir_file)

# ===== 实用示例 =====
# 创建模组配置目录
config_path = GameDirHelper.getSubDirPath("mymod")
GameDirHelper.createDirIfNotExists(config_path)
LOGGER.info("Config directory: " + config_path)

# 读取配置文件
import os
config_file = os.path.join(GameDirHelper.getConfigDirPath(), "mymod.json")

# 列出所有存档
saves_path = GameDirHelper.getSavesDirPath()
for save_name in os.listdir(saves_path):
    LOGGER.info("Found save: " + save_name)

# 备份截图
backup_dir = GameDirHelper.getSubDirPath("screenshots_backup")
GameDirHelper.createDirIfNotExists(backup_dir)
```

### ModConfig - 模组配置

模组配置通过 `config/jython-mod.json` 文件管理，或在游戏中通过模组设置界面修改：

```python
# 在 Python 中访问配置（由 Java 端传入）
from net.luojiayuan.jython.mod import Jythonmod

# 可用的配置项：
CONFIG_ENABLED = Jythonmod.CONFIG.enabled          # 是否启用模组
CONFIG_DEBUG = Jythonmod.CONFIG.debugMode          # 调试模式
CONFIG_SCRIPT_PATH = Jythonmod.CONFIG.scriptPath   # 主脚本路径
CONFIG_AUTO_RELOAD = Jythonmod.CONFIG.autoReload   # 自动重载脚本
CONFIG_TIMEOUT = Jythonmod.CONFIG.scriptTimeout    # 脚本超时时间（秒）
CONFIG_SHOW_OUTPUT = Jythonmod.CONFIG.showPythonOutput  # 显示Python输出
CONFIG_PYTHON_PATH = Jythonmod.CONFIG.pythonPath   # Python系统路径
CONFIG_MODS_PATHS = Jythonmod.CONFIG.modsPaths     # Jython模组加载路径

# 示例：根据配置执行不同逻辑
if CONFIG_DEBUG:
    LOGGER.info("Debug mode is enabled")

# 示例：获取模组加载路径
# modsPaths 格式: "{gamedir}/jymods" 或 "path1;path2;path3"
mods_paths = CONFIG_MODS_PATHS.replace("{gamedir}", GAME_DIR).split(";")
for path in mods_paths:
    LOGGER.info("Mod path: " + path)
```

**配置文件位置：** `config/jython-mod.json`

**默认配置：**
```json
{
  "enabled": true,
  "debugMode": false,
  "scriptPath": "/assets/jython-mod/jython/main.py",
  "autoReload": true,
  "scriptTimeout": 30,
  "showPythonOutput": true,
  "pythonPath": "",
  "modsPaths": "{gamedir}/jymods"
}
```

---

## 总结

将 Java 代码转换为 Jython 的关键要点：

1. **利用 Python 的简洁性** - 无需类型声明、花括号和分号
2. **直接导入 Java 类** - Jython 可以无缝使用 Java API
3. **使用 Python 语法** - lambda、列表推导、字典等
4. **注意 Python 2.x 语法**
5. **善用项目辅助函数** - item.register()、block.register() 等
