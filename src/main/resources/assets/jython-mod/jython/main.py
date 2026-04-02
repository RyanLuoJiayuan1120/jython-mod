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

# 去重路径列表，避免重复扫描
seen_paths = {}
unique_paths = []
for path in REMODPATH:
    normalized = os.path.normpath(path)
    if normalized and not seen_paths.has_key(normalized):
        seen_paths[normalized] = True
        unique_paths.append(normalized)
REMODPATH = unique_paths

LOGGER.debug("Mod paths after deduplication: " + str(REMODPATH))

class ModImporter:
    def __init__(self):
        # 使用字典存储，key为路径，value为模块对象
        self.libs = {}
        self.libs_client = {}
        self.libs_server = {}
        # 记录已执行main()的模块，避免重复执行
        self.executed_mains = set()

    def import_(self, path):
        # 标准化路径，避免因路径格式不同导致的重复加载
        normalized_path = os.path.normpath(path)

        # 检查是否已加载过此路径
        if self.libs.has_key(normalized_path):
            LOGGER.debug("Skipping already loaded module: " + str(path))
            return

        LOGGER.info("Loading module from: " + str(path))
        LOGGER.info("loaded:"+str([self.libs, self.libs_client, self.libs_server]))
        importer = zipimport.zipimporter(path)
        self.libs[normalized_path] = importer.load_module("main")
        try:
            client_mod = importer.load_module("client")
            if client_mod:
                self.libs_client[normalized_path] = client_mod
            LOGGER.info("Loaded client module from: " + str(path))
        except Exception as e:
            LOGGER.debug("No client module in " + str(path) + ": " + str(e))
        try:
            server_mod = importer.load_module("server")
            self.libs_server[normalized_path] = server_mod
            LOGGER.info("Loaded server module from: " + str(path))
        except Exception as e:
            LOGGER.debug("No server module in " + str(path) + ": " + str(e))

importer = ModImporter()

# 只在common环境下加载模块，client/server环境不需要重新加载
if ENV_TYPE == "common":
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
            # 直接列出顶层目录的文件，避免子目录重复
            file_list = os.listdir(path)
            # 只处理.zip文件
            zip_files = [f for f in file_list if f.endswith('.zip')]
            if not zip_files:
                LOGGER.info("No zip files found in: " + path)
                continue

            LOGGER.info("Found zip files: " + str(zip_files))

            for i in zip_files:
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

    LOGGER.info("Loaded main modules: " + str(importer.libs.keys()))
    LOGGER.info("Loaded client modules: " + str(importer.libs_client.keys()))
    LOGGER.info("Loaded server modules: " + str(importer.libs_server.keys()))

# ======== res ========
gamedir = GameDirHelper.getGameDirPath()
try:
    ENV_TYPE = ENV_TYPE
    if ENV_TYPE == "server":
        LOGGER.info("Building resource pack from mod resources...")
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

except:LOGGER.warn("Don't pack resources pack, because there are some errors,or there's server")


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

    # 收集所有可能的世界目录
    world_dirs = []

    # 1. 检查 gamedir/saves/ 目录（客户端世界）
    saves_dir = gamedir + "/saves"
    if os.path.exists(saves_dir):
        for save_name in os.listdir(saves_dir):
            save_path = os.path.join(saves_dir, save_name)
            if os.path.isdir(save_path):
                world_dirs.append(save_path)

    # 2. 检查 gamedir/ 下的直接子目录（服务器世界）
    # 在服务器模式下，world 文件夹通常直接在游戏目录下
    try:
        for item in os.listdir(gamedir):
            item_path = os.path.join(gamedir, item)
            # 检查是否是世界目录（包含 level.dat 和 datapacks 文件夹）
            if os.path.isdir(item_path):
                level_dat = os.path.join(item_path, "level.dat")
                if os.path.exists(level_dat):
                    # 确保不是已经在 saves 目录中的
                    if not item_path.startswith(saves_dir + os.sep):
                        world_dirs.append(item_path)
    except Exception as e:
        LOGGER.debug("Error scanning gamedir for worlds: " + str(e))

    # 复制数据包到所有世界
    if world_dirs:
        saves_copied = 0
        for save_path in world_dirs:
            save_name = os.path.basename(save_path)
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

        # 自动启用数据包
        try:
            from net.luojiayuan.jython.mod.utils import DatapackHelper
            DatapackHelper.enableDatapack("JythonModData.zip")
            LOGGER.info("Datapack enabled successfully!")
        except Exception as e:
            LOGGER.warning("Failed to auto-enable datapack: " + str(e))
    else:
        LOGGER.info("No world directories found, skipping datapack installation")
    shutil.rmtree(temp_data_dir)
else:
    if os.path.exists(temp_data_dir):
        shutil.rmtree(temp_data_dir)
    LOGGER.info("No data found in mods, skipping datapack creation")
# Only run mod.main() in common environment (not in client/server)
if ENV_TYPE == "common":
    LOGGER.info("Preparing to execute main() for " + str(len(importer.libs)) + " modules")
    for mod_path, mod in importer.libs.items():
        # 跳过已经执行过main()的模块
        if mod_path in importer.executed_mains:
            LOGGER.debug("Skipping already executed main() for: " + str(mod_path))
            continue

        try:
            if hasattr(mod, 'main'):
                LOGGER.info("Executing main() for: " + str(mod_path))
                mod.main()
                importer.executed_mains.add(mod_path)
                LOGGER.info("Successfully executed main() for: " + str(mod_path))
        except Exception as e:
            error_msg = str(e)
            # 忽略重复注册错误
            if "duplicate" in error_msg.lower() or "duplicate key" in error_msg.lower() or "Adding duplicate" in error_msg:
                LOGGER.info("Skipping duplicate registration from: " + str(mod_path))
                importer.executed_mains.add(mod_path)
            else:
                LOGGER.error("error at running main in " + str(mod_path) + ": " + str(e))
