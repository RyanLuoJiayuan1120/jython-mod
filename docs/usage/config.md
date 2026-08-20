# 配置

## 配置文件位置

| 平台 | 路径 | 读取方式 |
|------|------|----------|
| Fabric | `config/jython-mod.json` | Cloth Config（含游戏内 GUI） |
| NeoForge | `config/jython-mod.json` | Cloth Config（含游戏内 GUI） |
| Paper | `{gamedir}/config/jython-mod.json` | Gson 直读（无 GUI） |

- Fabric / NeoForge 使用 Cloth Config 管理，首次启动自动生成默认配置，可在游戏内 Mod 设置界面修改。
- Paper 端无 Cloth Config，由插件用 Gson 直接读取同名字段；文件缺失时使用默认值，修改后需重启服务器生效。
- 三端字段名与默认值完全一致，可跨平台复用同一份配置。

## 字段说明

```json
{
  "enabled": true,
  "debugMode": false,
  "scriptPath": "/assets/jython-mod/jython/main.py",
  "showPythonOutput": true,
  "pythonPath": "",
  "modsPaths": "{gamedir}/jymods",
  "pythonPackagesPath": "{gamedir}/graalpy/Lib"
}
```

| 字段 | 说明 | 默认值 |
|------|------|--------|
| `enabled` | 是否启用模组 | `true` |
| `debugMode` | 调试模式，输出详细日志 | `false` |
| `scriptPath` | 主脚本路径（当前实现未使用，保留兼容） | `/assets/jython-mod/jython/main.py` |
| `showPythonOutput` | 是否在控制台显示 Python 输出 | `true` |
| `pythonPath` | 附加 Python 系统路径（逗号分隔） | `""` |
| `modsPaths` | 模块搜索路径，支持 `;` 分隔多个路径 | `{gamedir}/jymods` |
| `pythonPackagesPath` | 第三方 Python 包部署目录 | `{gamedir}/graalpy/Lib` |

> `{gamedir}` 占位符会替换为当前平台的游戏/服务器根目录。
>
> 历史版本中的 `autoReload`、`scriptTimeout` 配置项已移除，请勿再使用。

## 相关文档

- 平台差异（GUI 支持、路径解析）：[platforms](../platforms/)
- 第三方包部署目录说明：[packages.md](packages.md)
