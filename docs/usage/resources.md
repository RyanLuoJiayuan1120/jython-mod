# 资源包与数据包

模组会自动从模块 ZIP 中提取资源，生成资源包与数据包。**本功能仅 Fabric / NeoForge 支持；Paper 端会跳过资源包与数据包生成**（见 [platforms/paper.md](../platforms/paper.md)）。

## 资源包

模组自动提取 ZIP 中的资源文件，生成 `resourcepacks/JythonModAssets.zip`。

支持的资源文件夹：

```
assets
atlases
blockstates
equipment
font
items
lang
models
particles
post_effect
sounds
shaders
texts
textures
waypoint_style
```

支持的根文件：

```
gpu_warnlist.json
regional_compliancies.json
sounds.json
```

## 数据包

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

## 相关文档

- 平台差异：[platforms](../platforms/)
