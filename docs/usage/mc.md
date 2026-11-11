# 内置 MC API（不再需要 bridge 模组）

jython-mod 现在自带一层轻量的 MC 适配，Python 模块可以直接调用，不需要额外安装
“bridge” 模组。它解决三件 mapping 本身不负责的事：

1. **MC 对象方法调用**：正式服方法名是 intermediary，普通属性访问找不到。用
   `McReflect.invoke(obj, yarnName, ...)`，它会沿父类/接口自动找声明类。
2. **Python 回调 → Java 接口**：Fabric 事件是强类型监听器数组，直接传 Python
   函数会 `ArrayStoreException`。用 `CallbackAdapter.adapt(fn, Interface)`。
3. **上下文生命周期**：Loader 保留 GraalPy context，直到服务器停止才统一释放。

> 适用于 Fabric 1.21.11。`McApi` 目前由 `FabricPlatformHooks` 注入；NeoForge /
> Paper 暂未提供平台实现，脚本可退回 `McReflect.invoke`。

## 1. `McApi` 全局

Fabric 运行时在 Python 模块命名空间中注入 `McApi`：

```python
def main():
    def on_start(server):
        LOGGER.info("seed={}", McApi.seedOf(server))

    from net.fabricmc.fabric.api.event.lifecycle.v1 import ServerLifecycleEvents
    from net.luojiayuan.jython.mod.mc import CallbackAdapter
    ServerStarted = java.type("net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents$ServerStarted")
    ServerLifecycleEvents.SERVER_STARTED.register(
        CallbackAdapter.adapt(on_start, ServerStarted))
```

方法：

| 方法 | 说明 |
|---|---|
| `McApi.playerOf(source)` | 命令源 → `ServerPlayer`，控制台返回 `None` |
| `McApi.uuidOf(player)` / `nameOf(player)` | 玩家 UUID / 名字 |
| `McApi.seedOf(server)` | 主世界地图种子 |
| `McApi.isOp(source)` | 是否有 moderator 权限 |
| `McApi.heldItemId(player)` | 主手物品 ID，空手返回 `""` |
| `McApi.itemId(stack)` | `ItemStack` → 物品 ID |
| `McApi.countItem(player, id)` | 背包内该物品总数 |
| `McApi.removeItem(player, id, n)` | 最多移除 n 个，返回实际移除数 |
| `McApi.giveItem(player, id, n)` | 给予物品，返回实际放入数 |
| `McApi.send(source, text)` / `sendFailure(...)` | 聊天栏反馈 |

这些方法都是普通 Java 方法，Python 直接调用即可，不需要 mapping 参数。

## 2. `CallbackAdapter`

```python
import java
from net.luojiayuan.jython.mod.mc import CallbackAdapter

Command = java.type("com.mojang.brigadier.Command")

def run(ctx):
    McApi.send(ctx.getSource(), "hello")
    return 1

node.executes(CallbackAdapter.adapt(run, Command))
```

事件监听器同理。适配器使用 JDK `Proxy`，Python 函数只在被调用时进入对应
GraalPy context，因此不会出现 `PolyglotMapAndFunction` 的类型错误。

## 3. `McReflect.invoke`

当需要调用 `McApi` 没有封装的 MC 方法时：

```python
from net.luojiayuan.jython.mod.mapping import McReflect

world = McReflect.invoke(server, "overworld")
seed = McReflect.invoke(world, "getSeed")      # 自动从 WorldGenLevel 找到
inventory = McReflect.invoke(player, "getInventory")
```

`invoke` 会：
- 遍历目标类的父类与接口；
- 把 runtime 方法名反向映射成 Yarn 名；
- 选择参数匹配的重载并调用。

因此不需要知道 `getSeed` 声明在 `WorldGenLevel` 而不是 `ServerLevel`，也不需要
在脚本里硬编码 `method_xxx`。
