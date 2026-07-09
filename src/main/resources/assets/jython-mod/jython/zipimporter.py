# -*- coding: utf-8 -*-
import zipimport
import sys
import types

class McReflectPackage(types.ModuleType):
    """Proxy package that maps Minecraft class names via McReflect"""
    def __getattr__(self, name):
        if name == '__path__':
            return []
        if name.startswith('__') and name.endswith('__'):
            raise AttributeError(name)
        full_name = self.__name__ + '.' + name
        if self.__name__.endswith('.' + name):
            full_name = self.__name__
        try:
            try:
                import java
                try:
                    cls = java.type(full_name)
                    setattr(self, name, cls)
                    return cls
                except Exception:
                    pass
            except ImportError:
                pass
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

if not any(type(f).__name__ == 'McReflectFinder' for f in sys.meta_path):
    sys.meta_path.insert(0, McReflectFinder())
    try:
        LOGGER.info("McReflect import hook registered")
    except NameError:
        pass

try:
    import java
    import types

    class JavaPackage(types.ModuleType):
        def _resolve_class(self, name):
            full_name = self.__name__ + '.' + name
            if self.__name__.endswith('.' + name):
                full_name = self.__name__
            try:
                if any(full_name.startswith(p) for p in ('net.minecraft.', 'com.mojang.', 'net.fabricmc.')):
                    from net.luojiayuan.jython.mod.mapping import McReflect
                    return java.type(McReflect.getClassName(full_name))
                else:
                    return java.type(full_name)
            except Exception:
                return None

        def __getattribute__(self, name):
            if name and name[0].isupper() and not (name.startswith('__') and name.endswith('__')):
                try:
                    return object.__getattribute__(self, name)
                except AttributeError:
                    pass
                try:
                    self_name = object.__getattribute__(self, '__name__')
                except Exception:
                    self_name = None
                if self_name:
                    pkg = object.__getattribute__(self, '_resolve_class')
                    cls = pkg(name)
                    if cls is not None:
                        setattr(self, name, cls)
                        return cls
            return object.__getattribute__(self, name)

        def __getattr__(self, name):
            if name == '__path__':
                return []
            if name.startswith('__') and name.endswith('__'):
                raise AttributeError(name)
            cls = self._resolve_class(name)
            if cls is not None:
                setattr(self, name, cls)
                return cls
            full_name = self.__name__ + '.' + name
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

    if not any(type(f).__name__ == 'JythonCompatFinder' for f in sys.meta_path):
        sys.meta_path.insert(0, JythonCompatFinder())
except ImportError:
    pass 
try:
    from net.luojiayuan.jython.mod import Jythonmod
    from org.python.core import codecs
    codecs.setDefaultEncoding('utf-8')
    LOGGER = Jythonmod.LOGGER
except ImportError:
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
            LOGGER.warn('Skip running "{}"...{}', self.path, str(e))
        except AttributeError as e:
            LOGGER.warn('Skip...The mod "{}" does not have method "{}"', self.path, self.env)
