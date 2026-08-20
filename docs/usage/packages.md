# 第三方 Python 包

每个 Python 模组可以通过 `config.json` 声明依赖，并将包文件内置到 ZIP 的 `Lib/` 目录中。游戏加载模组时会自动把 `Lib/` 解压到 `{gamedir}/graalpy/Lib` 并加入 Python 搜索路径。

## 模组 ZIP 结构

```
your_mod.zip
├── config.json          # 模组配置文件
├── main.py              # 公共环境入口
├── client.py            # 客户端入口（可选）
├── server.py            # 服务端入口（可选）
└── Lib/                 # 第三方纯 Python 包
    ├── requests/
    └── termcolor.py
```

## config.json 格式

```json
{
  "name": "My Mod",
  "version": "1.0.0",
  "dependencies": ["requests", "termcolor"]
}
```

- `name` / `version`：模组名称与版本，仅用于日志展示。
- `dependencies`：已内置在 `Lib/` 中的第三方包名列表，用于日志校验。

## 打包第三方包

在本地使用任意 Python 运行：

```bash
pip install --target ./Lib requests termcolor
```

然后将 `Lib/` 目录与 `config.json` 一起打包进 ZIP。只要包是纯 Python 代码，就能跨平台运行，且玩家端无需联网。

## 配置项

可在配置文件中修改第三方包部署目录（默认 `{gamedir}/graalpy/Lib`）：

```json
{
  "pythonPackagesPath": "{gamedir}/graalpy/Lib"
}
```

修改后需重启游戏生效。字段说明见 [config.md](config.md)。

## 注意事项

- 目前仅支持随模组 ZIP 内置包，不支持运行时从 PyPI 下载。
- 含 C 扩展的包需要为 GraalPy 重新编译，不建议通过此机制分发。
- 多个模组携带同一依赖的不同版本时，以先加载的版本为准。
