# Jython 方块实体实现说明

本目录包含了将 FabricMC 方块实体 Java 代码翻译为 Jython 的完整实现。

## 文件说明

### 1. `block_entity.py` - 基础方块实体
包含简单的方块实体实现，适合初学者理解基本概念。

**主要组件：**
- `MyBlockEntity`: 基础方块实体类
- `MyEntityBlock`: 带有实体属性的方块类
- `BlockEntityTypes`: 方块实体类型注册器

### 2. `advanced_block_entity.py` - 高级方块实体
包含完整的 NBT 数据持久化功能，适合实际项目使用。

**主要组件：**
- `AdvancedBlockEntity`: 支持数据保存/加载的方块实体
- `AdvancedEntityBlock`: 高级实体方块
- `BlockEntityTypes`: 方块实体类型注册器

**功能特性：**
- 自动保存和加载数据到 NBT
- 计数器功能
- 自定义消息存储
- 激活状态管理

## 快速开始

### 基础示例

在 `main.py` 中添加以下代码：

```python
from lib.block import Blocks
from lib.block_entity import MyEntityBlock, MyBlockEntity, BlockEntityTypes
from net.minecraft.world.level.block import Block, SoundType
from net.minecraft.world.level.block.state import BlockBehaviour

# 创建普通方块
properties = BlockBehaviour.Properties.of().sound(SoundType.GRASS)
my_block = Blocks.register(
    "my_block",
    lambda props: Block(props),
    properties,
    True
)

# 创建带实体属性的方块
entity_block_properties = BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(2.0)
my_entity_block = Blocks.register(
    "my_entity_block",
    lambda props: MyEntityBlock(props),
    entity_block_properties,
    True
)

# 注册方块实体类型
BlockEntityTypes.register(
    "my_block_entity",
    lambda pos, state: MyBlockEntity(pos, state),
    my_entity_block
)
```

### 高级示例（带 NBT 数据持久化）

```python
from lib.block import Blocks
from lib.advanced_block_entity import AdvancedEntityBlock, AdvancedBlockEntity, BlockEntityTypes
from net.minecraft.world.level.block import Block, SoundType
from net.minecraft.world.level.block.state import BlockBehaviour

# 创建高级实体方块
advanced_block_properties = BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(3.0)
advanced_block = Blocks.register(
    "advanced_block",
    lambda props: AdvancedEntityBlock(props),
    advanced_block_properties,
    True
)

# 注册方块实体类型
BlockEntityTypes.register(
    "advanced_block_entity",
    lambda pos, state: AdvancedBlockEntity(pos, state),
    advanced_block
)
```

## 方块实体生命周期

方块实体会在以下情况自动保存/加载数据：

1. **方块被放置时** - 创建新的方块实体
2. **区块卸载时** - 保存数据到磁盘
3. **区块加载时** - 从磁盘加载数据
4. **方块被破坏时** - 保存数据
5. **游戏关闭时** - 保存所有未保存的数据

## 访问和操作方块实体

### 在代码中获取方块实体

```python
# 假设你有一个 Level 对象和 BlockPos
block_entity = level.getBlockEntity(pos)

if isinstance(block_entity, AdvancedBlockEntity):
    # 操作方块实体
    block_entity.incrementCounter()
    block_entity.setMessage("新的消息")
    
    print(f"计数器: {block_entity.getCounter()}")
    print(f"消息: {block_entity.getMessage()}")
    print(f"激活状态: {block_entity.isActive()}")
```

### 重要提示

1. **使用 `setChanged()`**：每次修改方块实体的数据后，必须调用 `setChanged()` 方法标记实体已更改，否则数据可能不会被保存。

2. **类型检查**：在访问方块实体之前，使用 `isinstance()` 检查类型，确保你操作的是正确的方块实体类型。

3. **NBT 标签名**：在 `saveAdditional()` 和 `load()` 方法中使用的字符串标签名必须一致，否则数据无法正确保存和加载。

## 核心概念

### 1. 方块实体（BlockEntity）

方块实体是绑定到特定方块位置的特殊对象，可以存储数据和执行逻辑。适合需要持久化数据的方块，如箱子、熔炉、告示牌等。

### 2. 实体方块（EntityBlock）

继承自 `Block`，并实现 `newBlockEntity()` 方法来创建方块实体实例。

### 3. 方块实体类型（BlockEntityType）

用于标识和管理方块实体类型，需要在游戏注册表中注册。

### 4. NBT 数据持久化

通过实现 `saveAdditional()` 和 `load()` 方法，可以自动保存和加载数据到 NBT 格式。

## 自定义方块实体

### 创建自己的方块实体

```python
class MyCustomBlockEntity(BlockEntity):
    def __init__(self, pos, state):
        super(MyCustomBlockEntity, self).__init__(BLOCK_ENTITY_TYPE, pos, state)
        # 初始化自定义属性
        self.custom_property = "默认值"
    
    def saveAdditional(self, tag):
        super(MyCustomBlockEntity, self).saveAdditional(tag)
        # 保存自定义属性
        tag.putString("CustomProperty", self.custom_property)
    
    def load(self, tag):
        super(MyCustomBlockEntity, self).load(tag)
        # 加载自定义属性
        self.custom_property = tag.getString("CustomProperty")
    
    def setCustomProperty(self, value):
        self.custom_property = value
        self.setChanged()  # 重要！标记已更改
```

### 注册自定义方块实体

```python
# 注册方块
my_custom_block = Blocks.register(
    "my_custom_block",
    lambda props: MyCustomEntityBlock(props),
    BlockBehaviour.Properties.of(),
    True
)

# 注册方块实体类型
BlockEntityTypes.register(
    "my_custom_block_entity",
    lambda pos, state: MyCustomBlockEntity(pos, state),
    my_custom_block
)
```

## 常见问题

### Q: 方块实体的数据没有被保存？

A: 确保在修改数据后调用了 `setChanged()` 方法。

### Q: 如何在玩家交互时访问方块实体？

A: 需要实现方块的 `use()` 方法或使用事件监听器。这需要更多的 Fabric API 知识。

### Q: 方块实体可以存储多少数据？

A: 方块实体的数据存储在 NBT 格式中，理论上可以存储大量数据，但建议保持数据量合理，以免影响性能。

## 参考资源

- [FabricMC 方块实体文档](https://docs.fabricmc.net/zh_cn/develop/blocks/block-entities)
- Minecraft Wiki - NBT 格式
- Fabric API 文档

## 许可证

本代码遵循项目的主许可证。