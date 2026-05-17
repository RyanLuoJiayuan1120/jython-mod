# -*- coding: utf-8 -*-
import zipimport
import sys

# ========== GraalPy Java 互操作兼容层 ==========
try:
    import java
    import types
    
    class JavaPackage(types.ModuleType):
        """模拟 Jython 的 Java 包，支持 from x.y import Zzz 语法"""
        def __getattr__(self, name):
            full_name = self.__name__ + '.' + name
            try:
                cls = java.type(full_name)
                setattr(self, name, cls)
                return cls
            except Exception:
                # 不是类，创建子包
                sub = JavaPackage(full_name)
                setattr(self, name, sub)
                sys.modules[full_name] = sub
                return sub
    
    class JythonCompatFinder:
        def find_module(self, fullname, path=None):
            prefixes = ('java.', 'javax.', 'net.', 'org.', 'com.')
            top_packages = ('java', 'javax', 'net', 'org', 'com')
            if fullname.startswith(prefixes) or fullname in top_packages:
                return self
            return None
        
        def load_module(self, fullname):
            if fullname in sys.modules:
                return sys.modules[fullname]
            pkg = JavaPackage(fullname)
            sys.modules[fullname] = pkg
            return pkg
    
    # 避免重复注册
    if not any(type(f).__name__ == 'JythonCompatFinder' for f in sys.meta_path):
        sys.meta_path.insert(0, JythonCompatFinder())
except ImportError:
    pass  # Jython 环境不需要兼容层
# ============================================

# 兼容 Jython 和 GraalPy
try:
    from net.luojiayuan.jython.mod import Jythonmod
    from org.python.core import codecs
    codecs.setDefaultEncoding('utf-8')
    LOGGER = Jythonmod.LOGGER
except ImportError:
    # GraalPy 环境: LOGGER 由 Java bindings 注入
    pass

class ModImporter:
    def __init__(self, env, path):
        self.env = env
        self.path = path

    def Load(self):
        try:
            importer = zipimport.zipimporter(self.path)
            Mod = importer.load_module(self.env)
            Mod.main()
        except zipimport.ZipImportError as e:
            LOGGER.warn("Skip...Is \"" + self.path + "\" a folder?" + str(e))
        except AttributeError as e:
            LOGGER.warn("Skip...The Mod \"" + self.path + "\" don't have method " + self.env)
