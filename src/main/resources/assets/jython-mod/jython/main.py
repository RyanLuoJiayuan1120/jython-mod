# -*- coding: utf-8 -*-
import sys, os, zipimport, zipfile, shutil
from net.luojiayuan.jython.mod.utils import path, GameDirHelper
from net.luojiayuan.jython.mod import Jythonmod

# 设置默认编码为UTF-8（Python 2兼容）
if sys.version_info[0] == 2:
    reload(sys)
    sys.setdefaultencoding('utf-8')

# 获取logger（由Java传入）
try:
	LOGGER = LOGGER
except NameError:
	# 如果没有传入logger，使用默认的print
	class DefaultLogger:
		def _safe_print(self, prefix, msg):
			"""安全地打印消息，处理编码问题"""
			try:
				print(prefix + " " + str(msg))
			except UnicodeEncodeError:
				# 如果编码失败，使用ASCII安全输出
				safe_msg = str(msg).encode('ascii', errors='replace').decode('ascii')
				print(prefix + " " + safe_msg)

		def info(self, msg): self._safe_print("[INFO]", msg)
		def warn(self, msg): self._safe_print("[WARN]", msg)
		def warning(self, msg): self._safe_print("[WARN]", msg)
		def error(self, msg): self._safe_print("[ERROR]", msg)
		def debug(self, msg): self._safe_print("[DEBUG]", msg)
	LOGGER = DefaultLogger()

MODPATH = Jythonmod.CONFIG.modsPaths
game_dir = GameDirHelper.getGameDirPath()
# 确保路径使用UTF-8编码
if isinstance(MODPATH, unicode):
    modpath_str = MODPATH.encode('utf-8')
else:
    modpath_str = str(MODPATH)
REMODPATH = modpath_str.replace("{gamedir}", game_dir).replace("//", "/").replace("\\\\", "\\").split(";")

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
                # 确保文件名使用UTF-8编码
                if isinstance(i, unicode):
                    i_str = i.encode('utf-8')
                else:
                    i_str = str(i)
                full_path = os.path.join(path, i_str)
                LOGGER.info("Loading: " + full_path)
                importer.import_(full_path)
            except Exception as e:
                # 安全地输出错误信息，避免编码错误
                try:
                    LOGGER.error("Error loading " + str(i) + ": " + str(e))
                except UnicodeEncodeError:
                    LOGGER.error("Error loading file (encoding error in filename): " + repr(i))
    except Exception as e:
        LOGGER.error("Error scanning path " + path + ": " + str(e))

LOGGER.info("Loaded main modules: " + str(importer.libs))
LOGGER.info("Loaded client modules: " + str(importer.libs_client))
LOGGER.info("Loaded server modules: " + str(importer.libs_server))
LOGGER.info("Building resource pack from mod resources...")
gamedir = GameDirHelper.getGameDirPath()
temp_res_dir = gamedir + "/jython_mod_temp_res"
final_res_pack = gamedir + "/resourcepacks/JythonModAssets.zip"
resource_folders = ['assets', 'atlases', 'blockstates', 'equipment', 'font',
                   'items', 'lang', 'models', 'particles', 'post_effect',
                   'sounds', 'shaders', 'texts', 'textures', 'waypoint_style']
resource_files = ['gpu_warnlist.json', 'regional_compliancies.json', 'sounds.json']
if os.path.exists(temp_res_dir):
    shutil.rmtree(temp_res_dir)
try:
    os.makedirs(temp_res_dir)
except OSError:
    if not os.path.isdir(temp_res_dir):
        raise
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
                            try:
                                zf.extract(name, temp_res_dir)
                            except UnicodeDecodeError:
                                import io
                                data = zf.read(name)
                                if isinstance(name, unicode):
                                    arcname_utf8 = name.encode('utf-8')
                                else:
                                    arcname_utf8 = name.decode('utf-8', errors='ignore').encode('utf-8')
                                target_path = os.path.join(temp_res_dir, arcname_utf8)
                                target_dir = os.path.dirname(target_path)
                                if not os.path.exists(target_dir):
                                    os.makedirs(target_dir)
                                with io.open(target_path, 'wb') as f:
                                    f.write(data)
                            extracted = True

                    if extracted:
                        try:
                            LOGGER.info("Extracted resources from: " + filename)
                        except UnicodeEncodeError:
                            LOGGER.info("Extracted resources from file")
                        resources_collected = True
            except Exception as e:
                try:
                    LOGGER.warning("Failed to extract resources from " + filename + ": " + str(e))
                except UnicodeEncodeError:
                    LOGGER.warning("Failed to extract resources (encoding error)")
    except Exception as e:
        LOGGER.warning("Failed to scan path " + base_path + ": " + str(e))

if resources_collected:
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
    LOGGER.info("Creating resource pack: " + final_res_pack)
    with zipfile.ZipFile(final_res_pack, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(temp_res_dir):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, temp_res_dir)
                zf.write(file_path, arcname)
    shutil.rmtree(temp_res_dir)
    LOGGER.info("Resource pack created successfully!")
    ResourcePackHelper.enableResourcePack("JythonModAssets.zip")
else:
    if os.path.exists(temp_res_dir):
        shutil.rmtree(temp_res_dir)
    LOGGER.info("No resources found in mods, skipping resource pack creation")

LOGGER.info("Building datapack from mod data...")
temp_data_dir = gamedir + "/jython_mod_temp_data"
final_data_pack = gamedir + "/resourcepacks/JythonModData.zip"
if os.path.exists(temp_data_dir):
    shutil.rmtree(temp_data_dir)
try:
    os.makedirs(temp_data_dir)
except OSError:
    if not os.path.isdir(temp_data_dir):
        raise
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
                    extracted = False
                    for name in zf.namelist():
                        if name.startswith('data/') and not name.endswith('/'):
                            try:
                                zf.extract(name, temp_data_dir)
                            except UnicodeDecodeError:
                                import io
                                data = zf.read(name)
                                if isinstance(name, unicode):
                                    arcname_utf8 = name.encode('utf-8')
                                else:
                                    arcname_utf8 = name.decode('utf-8', errors='ignore').encode('utf-8')
                                target_path = os.path.join(temp_data_dir, arcname_utf8)
                                target_dir = os.path.dirname(target_path)
                                if not os.path.exists(target_dir):
                                    os.makedirs(target_dir)
                                with io.open(target_path, 'wb') as f:
                                    f.write(data)
                            extracted = True

                    if extracted:
                        try:
                            LOGGER.info("Extracted data from: " + filename)
                        except UnicodeEncodeError:
                            LOGGER.info("Extracted data from file")
                        data_collected = True
            except Exception as e:
                try:
                    LOGGER.warning("Failed to extract data from " + filename + ": " + str(e))
                except UnicodeEncodeError:
                    LOGGER.warning("Failed to extract data (encoding error)")
    except Exception as e:
        LOGGER.warning("Failed to scan path " + base_path + ": " + str(e))

if data_collected:
    import json
    pack_meta = {
        "pack": {
            "pack_format": 48,
            "description": "Jython Mod Data - Generated from mod data"
        }
    }
    with open(temp_data_dir + "/pack.mcmeta", 'w') as f:
        json.dump(pack_meta, f)
    LOGGER.info("Creating datapack...")
    import StringIO
    zip_buffer = StringIO.StringIO()
    with zipfile.ZipFile(zip_buffer, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(temp_data_dir):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, temp_data_dir)
                zf.write(file_path, arcname)
    saves_dir = gamedir + "/saves"
    if os.path.exists(saves_dir):
        saves_copied = 0
        for save_name in os.listdir(saves_dir):
            save_path = os.path.join(saves_dir, save_name)
            if os.path.isdir(save_path):
                datapacks_dir = os.path.join(save_path, "datapacks")
                if not os.path.exists(datapacks_dir):
                    try:
                        os.makedirs(datapacks_dir)
                    except OSError:
                        if not os.path.isdir(datapacks_dir):
                            continue
                datapack_path = os.path.join(datapacks_dir, "JythonModData.zip")
                try:
                    with open(datapack_path, 'wb') as f:
                        f.write(zip_buffer.getvalue())
                    saves_copied += 1
                    try:
                        LOGGER.info("Copied datapack to save: " + save_name)
                    except UnicodeEncodeError:
                        LOGGER.info("Copied datapack to save")
                except Exception as e:
                    try:
                        LOGGER.warning("Failed to copy datapack to " + save_name + ": " + str(e))
                    except UnicodeEncodeError:
                        LOGGER.warning("Failed to copy datapack (encoding error)")

        LOGGER.info("Datapack created and copied to " + str(saves_copied) + " save(s)!")
    else:
        LOGGER.info("No saves directory found, skipping datapack installation")
    shutil.rmtree(temp_data_dir)
else:
    if os.path.exists(temp_data_dir):
        shutil.rmtree(temp_data_dir)
    LOGGER.info("No data found in mods, skipping datapack creation")
for mod in importer.libs:
    try:
        if hasattr(mod, 'main'):
            mod.main()
    except Exception as e:
        LOGGER.error("error at running main in " + str(mod.__file__) + ": " + str(e))
