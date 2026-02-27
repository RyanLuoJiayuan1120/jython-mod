# -*- coding: utf-8 -*-
import sys, os, zipimport, zipfile, shutil
from net.luojiayuan.jython.mod.utils import path, GameDirHelper
from net.luojiayuan.jython.mod import Jythonmod

# 获取logger（由Java传入）
try:
	LOGGER = LOGGER
except NameError:
	# 如果没有传入logger，使用默认的print
	class DefaultLogger:
		def info(self, msg): print("[INFO] " + str(msg))
		def warn(self, msg): print("[WARN] " + str(msg))
		def warning(self, msg): print("[WARN] " + str(msg))
		def error(self, msg): print("[ERROR] " + str(msg))
		def debug(self, msg): print("[DEBUG] " + str(msg))
	LOGGER = DefaultLogger()

MODPATH = Jythonmod.CONFIG.modsPaths
REMODPATH=str(MODPATH).replace("{gamedir}", GameDirHelper.getGameDirPath()).replace("//", "/").replace("\\\\", "\\").split(";")

class ModImporter:
    def __init__(self):
        self.libs = []
        self.libs_client = []
        self.libs_server = []

    def import_(self, path):
        LOGGER.info("Loading module from: " + str(path))
        importer = zipimport.zipimporter(path)
        self.libs.append(importer.load_module("main"))
        try:
            self.libs_client.append(importer.load_module("client"))
            LOGGER.info("Loaded client module from: " + str(path))
        except Exception as e:
            LOGGER.debug("No client module in " + str(path) + ": " + str(e))
        try:
            self.libs_server.append(importer.load_module("server"))
            LOGGER.info("Loaded server module from: " + str(path))
        except Exception as e:
            LOGGER.debug("No server module in " + str(path) + ": " + str(e))

importer = ModImporter()

for path in REMODPATH:
    # 创建目录（如果不存在）
    if not os.path.isdir(path):
        try:
            os.makedirs(path)
            LOGGER.info("Created directory: " + path)
        except Exception as e:
            LOGGER.error("Error at mkdir:" + path + " - " + str(e))
            continue  # 跳过无法创建的目录

    LOGGER.info("Scanning path: " + path)

    # 安全地遍历目录
    try:
        walk_result = list(os.walk(path))
        if not walk_result:
            LOGGER.info("No files found in: " + path)
            continue

        # walk_result[0] 是 (dirpath, dirnames, filenames)
        # 我们需要 filenames (索引2)
        file_list = walk_result[0][2] if len(walk_result[0]) > 2 else []
        LOGGER.info("Found files: " + str(file_list))

        for i in file_list:
            try:
                full_path = os.path.join(path, str(i))
                LOGGER.info("Loading: " + full_path)
                importer.import_(full_path)
            except Exception as e:
                LOGGER.error("Error loading " + str(i) + ": " + str(e))
    except Exception as e:
        LOGGER.error("Error scanning path " + path + ": " + str(e))

LOGGER.info("Loaded main modules: " + str(importer.libs))
LOGGER.info("Loaded client modules: " + str(importer.libs_client))
LOGGER.info("Loaded server modules: " + str(importer.libs_server))

# ============ 资源包处理 ============
LOGGER.info("Building resource pack from mod resources...")

# 创建临时目录存放所有资源
gamedir = GameDirHelper.getGameDirPath()
temp_res_dir = gamedir + "/jython_mod_temp_res"
final_res_pack = gamedir + "/resourcepacks/JythonModAssets.zip"

# 需要打包的资源文件夹列表
resource_folders = ['assets', 'atlases', 'blockstates', 'equipment', 'font',
                   'items', 'lang', 'models', 'particles', 'post_effect',
                   'sounds', 'shaders', 'texts', 'textures', 'waypoint_style']

# 需要打包的文件列表（根目录文件）
resource_files = ['gpu_warnlist.json', 'regional_compliancies.json', 'sounds.json']

# 清理旧的临时目录
if os.path.exists(temp_res_dir):
    shutil.rmtree(temp_res_dir)
# 兼容Python 2的方式创建目录
try:
    os.makedirs(temp_res_dir)
except OSError:
    if not os.path.isdir(temp_res_dir):
        raise

# 遍历所有zip文件，提取所有资源
resources_collected = False
for base_path in REMODPATH:
    if not os.path.isdir(base_path):
        continue
    try:
        for filename in os.listdir(base_path):
            if not filename.endswith('.zip'):
                continue
            zip_path = os.path.join(base_path, filename)
            try:
                with zipfile.ZipFile(zip_path, 'r') as zf:
                    # 提取所有资源文件夹和文件
                    extracted = False
                    for name in zf.namelist():
                        # 检查是否是资源文件夹或文件
                        is_resource = False
                        for folder in resource_folders:
                            if name.startswith(folder + '/') or name == folder:
                                is_resource = True
                                break
                        for file in resource_files:
                            if name == file:
                                is_resource = True
                                break

                        if is_resource and not name.endswith('/'):
                            # 解压到临时目录
                            zf.extract(name, temp_res_dir)
                            extracted = True

                    if extracted:
                        LOGGER.info("Extracted resources from: " + filename)
                        resources_collected = True
            except Exception as e:
                LOGGER.warning("Failed to extract resources from " + filename + ": " + str(e))
    except Exception as e:
        LOGGER.warning("Failed to scan path " + base_path + ": " + str(e))

if resources_collected:
    # 创建pack.mcmeta
    import json
    from net.luojiayuan.jython.mod.utils import ResourcePackHelper
    pack_meta = {
        "pack": {
            "pack_format": 48,
            "description": "Jython Mod Resources - Generated from mod resources"
        }
    }
    with open(temp_res_dir + "/pack.mcmeta", 'w') as f:
        json.dump(pack_meta, f)

    # 打包成zip
    LOGGER.info("Creating resource pack: " + final_res_pack)
    with zipfile.ZipFile(final_res_pack, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(temp_res_dir):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, temp_res_dir)
                zf.write(file_path, arcname)

    # 清理临时目录
    shutil.rmtree(temp_res_dir)
    LOGGER.info("Resource pack created successfully!")

    # 自动启用资源包
    ResourcePackHelper.enableResourcePack("JythonModAssets.zip")
else:
    # 清理空的临时目录
    if os.path.exists(temp_res_dir):
        shutil.rmtree(temp_res_dir)
    LOGGER.info("No resources found in mods, skipping resource pack creation")

# ============ 数据包处理 ============
LOGGER.info("Building datapack from mod data...")

# 创建临时目录存放所有数据
temp_data_dir = gamedir + "/jython_mod_temp_data"
final_data_pack = gamedir + "/resourcepacks/JythonModData.zip"

# 清理旧的临时目录
if os.path.exists(temp_data_dir):
    shutil.rmtree(temp_data_dir)
try:
    os.makedirs(temp_data_dir)
except OSError:
    if not os.path.isdir(temp_data_dir):
        raise

# 遍历所有zip文件，提取data文件夹
data_collected = False
for base_path in REMODPATH:
    if not os.path.isdir(base_path):
        continue
    try:
        for filename in os.listdir(base_path):
            if not filename.endswith('.zip'):
                continue
            zip_path = os.path.join(base_path, filename)
            try:
                with zipfile.ZipFile(zip_path, 'r') as zf:
                    # 提取data文件夹
                    extracted = False
                    for name in zf.namelist():
                        if name.startswith('data/') and not name.endswith('/'):
                            zf.extract(name, temp_data_dir)
                            extracted = True

                    if extracted:
                        LOGGER.info("Extracted data from: " + filename)
                        data_collected = True
            except Exception as e:
                LOGGER.warning("Failed to extract data from " + filename + ": " + str(e))
    except Exception as e:
        LOGGER.warning("Failed to scan path " + base_path + ": " + str(e))

if data_collected:
    # 创建pack.mcmeta（数据包格式）
    import json
    pack_meta = {
        "pack": {
            "pack_format": 48,
            "description": "Jython Mod Data - Generated from mod data"
        }
    }
    with open(temp_data_dir + "/pack.mcmeta", 'w') as f:
        json.dump(pack_meta, f)

    # 打包成zip到内存
    LOGGER.info("Creating datapack...")
    import StringIO
    zip_buffer = StringIO.StringIO()
    with zipfile.ZipFile(zip_buffer, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(temp_data_dir):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, temp_data_dir)
                zf.write(file_path, arcname)

    # 遍历所有存档，复制数据包
    saves_dir = gamedir + "/saves"
    if os.path.exists(saves_dir):
        saves_copied = 0
        for save_name in os.listdir(saves_dir):
            save_path = os.path.join(saves_dir, save_name)
            if os.path.isdir(save_path):
                datapacks_dir = os.path.join(save_path, "datapacks")
                # 确保datapacks目录存在
                if not os.path.exists(datapacks_dir):
                    try:
                        os.makedirs(datapacks_dir)
                    except OSError:
                        if not os.path.isdir(datapacks_dir):
                            continue

                # 写入数据包文件
                datapack_path = os.path.join(datapacks_dir, "JythonModData.zip")
                try:
                    with open(datapack_path, 'wb') as f:
                        f.write(zip_buffer.getvalue())
                    saves_copied += 1
                    LOGGER.info("Copied datapack to save: " + save_name)
                except Exception as e:
                    LOGGER.warning("Failed to copy datapack to " + save_name + ": " + str(e))

        LOGGER.info("Datapack created and copied to " + str(saves_copied) + " save(s)!")
    else:
        LOGGER.info("No saves directory found, skipping datapack installation")

    # 清理临时目录
    shutil.rmtree(temp_data_dir)
else:
    # 清理空的临时目录
    if os.path.exists(temp_data_dir):
        shutil.rmtree(temp_data_dir)
    LOGGER.info("No data found in mods, skipping datapack creation")

# ============ 运行模块 ============
# main的 - 在common/main中运行
for mod in importer.libs:
    try:
        if hasattr(mod, 'main'):
            mod.main()
    except Exception as e:
        LOGGER.error("error at running main in " + str(mod.__file__) + ": " + str(e))
