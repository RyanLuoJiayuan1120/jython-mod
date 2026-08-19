# -*- coding: utf-8 -*-
import importlib.machinery
import importlib.util
import sys
import types
import zipimport

class McReflectPackage(types.ModuleType):
    """Proxy package that maps Minecraft class names via McReflect"""
    def __str__(self):
        try:
            import java
            if any(self.__name__.startswith(p) for p in ('net.minecraft.', 'com.mojang.', 'net.fabricmc.')):
                from net.luojiayuan.jython.mod.mapping import McReflect
                return McReflect.getClassName(self.__name__)
        except BaseException:
            pass
        return types.ModuleType.__str__(self)

    def __repr__(self):
        return self.__str__()

    def __getattr__(self, name):
        if name == '__path__':
            return []
        if name.startswith('__') and name.endswith('__'):
            raise AttributeError(name)
        full_name = self.__name__ + '.' + name
        if self.__name__.endswith('.' + name):
            full_name = self.__name__
        try:
            import java
            if any(full_name.startswith(p) for p in ('net.minecraft.', 'com.mojang.', 'net.fabricmc.')):
                cls = JavaClassRef(full_name)
                setattr(self, name, cls)
                return cls
            try:
                cls = java.type(full_name)
                setattr(self, name, cls)
                return cls
            except Exception:
                pass
        except ImportError:
            pass
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

    def find_spec(self, fullname, path=None, target=None):
        if any(fullname.startswith(p) for p in self._mc_prefixes) or \
           any(fullname == p.rstrip('.') for p in self._mc_prefixes):
            return importlib.machinery.ModuleSpec(fullname, self, is_package=True)
        return None

    def create_module(self, spec):
        return McReflectPackage(spec.name)

    def exec_module(self, module):
        module.__path__ = []
        sys.modules[module.__name__] = module

    # Legacy fallback (harmless on modern Pythons, used by older ones)
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
else:
    try:
        LOGGER.info("McReflect import hook already present")
    except NameError:
        pass

try:
    import java
    import types

    def _mc_mapped_name(full_name):
        """Map a Yarn/Mojang class name (possibly nested with '.') to its
        runtime class name, trying '$' for inner classes right-to-left."""
        from net.luojiayuan.jython.mod.mapping import McReflect
        return McReflect.getClassName(full_name)

    class JavaClassRef:
        """Lazy wrapper around a Java class that resolves nested classes,
        static methods/fields and constructors through McReflect, because
        GraalPy host-class attribute lookup cannot see intermediary-named
        nested classes (Item.Properties -> class_1792$class_1793)."""
        def __init__(self, full_name):
            object.__setattr__(self, '_full_name', full_name)

        @property
        def __name__(self):
            return self._full_name

        def _resolve(self, name):
            nested = self._full_name + '.' + name
            # Inner classes: try '.' (direct/yarn) and '$' (mojmap/intermediary).
            candidates = [nested]
            idx = nested.rfind('.')
            if idx > 0:
                candidates.append(nested[:idx] + '$' + nested[idx + 1:])
            for cand in candidates:
                try:
                    cls_name = _mc_mapped_name(cand)
                    cls = java.type(cls_name)
                    return JavaClassRef(cand)
                except BaseException:
                    continue
            return None

        def __getattr__(self, name):
            if name.startswith('__') and name.endswith('__'):
                raise AttributeError(name)
            ref = self._resolve(name)
            if ref is not None:
                object.__setattr__(self, name, ref)
                return ref
            # Static method: return a callable backed by McReflect.call
            from net.luojiayuan.jython.mod.mapping import McReflect

            def _static(*args):
                return McReflect.call(self._full_name, name, None, *args)
            return _static

        def __call__(self, *args):
            from net.luojiayuan.jython.mod.mapping import McReflect
            return McReflect.call(self._full_name, '<init>', None, *args)

        def __repr__(self):
            try:
                return _mc_mapped_name(self._full_name)
            except BaseException:
                return self._full_name

        def __eq__(self, other):
            if isinstance(other, JavaClassRef):
                return self._full_name == other._full_name
            return NotImplemented

        def __hash__(self):
            return hash(self._full_name)

    class JavaPackage(types.ModuleType):
        def _resolve_class(self, name):
            full_name = self.__name__ + '.' + name
            if self.__name__.endswith('.' + name):
                full_name = self.__name__
            try:
                if any(full_name.startswith(p) for p in ('net.minecraft.', 'com.mojang.', 'net.fabricmc.')):
                    return JavaClassRef(full_name)
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
        _mc_prefixes = ('net.minecraft.', 'com.mojang.', 'net.fabricmc.')

        def find_spec(self, fullname, path=None, target=None):
            # Never claim Minecraft-side packages: they belong to McReflectFinder.
            if any(fullname.startswith(p) for p in self._mc_prefixes):
                return None
            prefixes = ('java.', 'javax.', 'net.', 'org.', 'com.')
            top_packages = ('java', 'javax', 'net', 'org', 'com')
            if fullname.startswith(prefixes) or fullname in top_packages:
                return importlib.machinery.ModuleSpec(fullname, self, is_package=True)
            return None

        def create_module(self, spec):
            return JavaPackage(spec.name)

        def exec_module(self, module):
            module.__path__ = []
            sys.modules[module.__name__] = module

        # Legacy fallback
        def find_module(self, fullname, path=None):
            if any(fullname.startswith(p) for p in self._mc_prefixes):
                return None
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
        try:
            LOGGER.info("JythonCompat finder registered. meta_path=%s", [type(f).__name__ for f in sys.meta_path])
        except Exception:
            pass
except Exception:
    pass 
try:
    from net.luojiayuan.jython.mod import Jythonmod
    LOGGER = Jythonmod.LOGGER
except ImportError:
    pass

try:
    from org.python.core import codecs
    codecs.setDefaultEncoding('utf-8')
except Exception:
    # GraalPy does not expose Jython's codecs module; default encoding is already UTF-8
    pass

def _warn(msg):
    try:
        LOGGER.warn(msg)
    except Exception:
        pass

class ModImporter:
    def __init__(self, env, path):
        self.env = env
        self.path = path

    def Load(self):
        try:
            importer = zipimport.zipimporter(self.path)
            try:
                Mod = importer.load_module(self.env)
            except Exception as e:
                _warn('Skip running "%s"...%s' % (self.path, str(e)))
                return
            if Mod is None:
                _warn('Skip running "%s"...module is None' % self.path)
                return
            if not callable(getattr(Mod, 'main', None)):
                _warn('Skip...The mod "%s" does not have method "%s"' % (self.path, self.env))
                return
            Mod.main()
        except zipimport.ZipImportError as e:
            _warn('Skip running "%s"...%s' % (self.path, str(e)))
        except AttributeError as e:
            _warn('Skip...The mod "%s" does not have method "%s"' % (self.path, self.env))
