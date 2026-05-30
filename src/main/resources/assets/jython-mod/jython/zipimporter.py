# -*- coding: utf-8 -*-
import zipimport
import sys

# ========== McReflect Import Hook (Jython & GraalPy) ==========
# Intercepts Minecraft-related package imports via sys.meta_path hooks,
# using McReflect's yarn->obf mapping to resolve class names automatically.
# This allows Python code like:
#   from net.minecraft.world.item import Item
# to work even in production (obfuscated) environments.

import types

class McReflectPackage(types.ModuleType):
    """Proxy package that maps Minecraft class names via McReflect"""
    def __getattr__(self, name):
        if name == '__path__':
            return []
        if name.startswith('__') and name.endswith('__'):
            raise AttributeError(name)
        full_name = self.__name__ + '.' + name
        try:
            from net.luojiayuan.jython.mod.mapping import McReflect
            cls = McReflect.getClass(full_name)
            setattr(self, name, cls)
            return cls
        except Exception:
            # Not a class, create sub-package
            sub = McReflectPackage(full_name)
            setattr(self, name, sub)
            sys.modules[full_name] = sub
            return sub

class McReflectFinder:
    _mc_prefixes = ('net.minecraft.', 'com.mojang.', 'net.fabricmc.')

    def find_module(self, fullname, path=None):
        if any(fullname.startswith(p) for p in self._mc_prefixes) or \
           any(fullname == p.rstrip('.') for p in self._mc_prefixes):
            return self
        return None

    def load_module(self, fullname):
        if fullname in sys.modules:
            return sys.modules[fullname]
        pkg = McReflectPackage(fullname)
        sys.modules[fullname] = pkg
        return pkg

# Avoid duplicate registration
if not any(type(f).__name__ == 'McReflectFinder' for f in sys.meta_path):
    sys.meta_path.insert(0, McReflectFinder())
    try:
        LOGGER.info("McReflect import hook registered")
    except NameError:
        pass

# ============================================

# ========== GraalPy Java Interop Compatibility Layer ==========
try:
    import java
    import types

    class JavaPackage(types.ModuleType):
        """Simulates Jython's Java package, supporting from x.y import Zzz syntax"""
        def __getattr__(self, name):
            if name == '__path__':
                return []
            if name.startswith('__') and name.endswith('__'):
                raise AttributeError(name)
            full_name = self.__name__ + '.' + name
            try:
                # 对 Minecraft 相关包使用 McReflect 做混淆映射
                if any(full_name.startswith(p) for p in ('net.minecraft.', 'com.mojang.', 'net.fabricmc.')):
                    from net.luojiayuan.jython.mod.mapping import McReflect
                    cls = java.type(McReflect.getClassName(full_name))
                else:
                    cls = java.type(full_name)
                setattr(self, name, cls)
                return cls
            except Exception:
                # Not a class, create sub-package
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

    # Avoid duplicate registration
    if not any(type(f).__name__ == 'JythonCompatFinder' for f in sys.meta_path):
        sys.meta_path.insert(0, JythonCompatFinder())
except ImportError:
    pass  # Jython environment does not need compatibility layer
# ============================================

# Jython and GraalPy compatibility
try:
    from net.luojiayuan.jython.mod import Jythonmod
    from org.python.core import codecs
    codecs.setDefaultEncoding('utf-8')
    LOGGER = Jythonmod.LOGGER
except ImportError:
    # GraalPy environment: LOGGER injected via Java bindings
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
            LOGGER.warn('Skip...Is "{}" a folder? {}', self.path, str(e))
        except AttributeError as e:
            LOGGER.warn('Skip...The mod "{}" does not have method "{}"', self.path, self.env)
