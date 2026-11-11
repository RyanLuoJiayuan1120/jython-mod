# 资源包与数据包

模组会从每个 Python 模块 ZIP 中提取资源，按模块生成内置包。**当前只有 Fabric 实现并测试通过**；NeoForge / Paper 的 `PlatformHooks.supportsResourcePacks()` 返回 `false`，暂不生成。

## 每个模组一个包

一个模块 ZIP 可以同时包含资源与数据：

```
your_mod.zip
├── assets/
│   └── your_namespace/
│       ├── lang/
│       ├── models/
│       └── textures/
└── data/
    └── your_namespace/
        ├── function/
        ├── loot_table/
        └── ...
```

生成结果：

```
<gamedir>/jython-mod/packs/
├── resourcepacks/
│   └── your_mod/
│       ├── pack.mcmeta
│       └── assets/...
└── datapacks/
    └── your_mod/
        ├── pack.mcmeta
        └── data/...
```

- 只有 `assets/`（以及根目录的 `sounds.json`、`gpu_warnlist.json`、`regional_compliancies.json`、`pack.png`）时，只生成资源包。
- 只有 `data/` 时，只生成数据包。
- 两者都有时，生成同名的资源包和数据包。
- 模块文件名会转成小写、安全的 id；`config.json` 的 `name` 用作显示名。
- 每个包会写入自己的 `pack.mcmeta`，资源包使用当前资源格式（1.21.11 为 75.0），数据包使用当前数据格式（94.1）。

## 默认启用且对用户隐藏

包通过 Fabric 的内置包 API 注册，激活类型为 `ALWAYS_ENABLED`：

- 不依赖 `resourcepacks/` 目录或 `options.txt`，也不复制到存档的 `datapacks/`。
- 资源包在客户端启动时直接参与 `ResourceManager` 重载；数据包在服务端/存档中自动加入已启用列表。
- 客户端 `PackSelectionModel` 的混入会过滤掉 `jython-mod:` 前缀的条目，因此在“资源包”和“数据包”选择界面中看不到它们。
- 由于是 required 包，即使用户打开选择界面并点击“完成”，它们仍保持启用。

验证日志（客户端）：

```
Reloading ResourceManager: vanilla, ..., jython-mod, jython-mod:packdemo2, jython-mod:packdemo
```

验证日志（服务端）：

```
Found new data pack jython-mod:packdemo, loading it automatically
```

## 旧版合并包清理

启动时会删除旧版生成的：

- `<gamedir>/resourcepacks/JythonModAssets.zip`
- 各存档 `datapacks/JythonModData.zip`
- `jython_mod_temp_res/`、`jython_mod_temp_data/`
- `options.txt` 中对应的 `file/JythonModAssets.zip` 条目

## 相关文档

- 平台差异：[platforms](../platforms/)
