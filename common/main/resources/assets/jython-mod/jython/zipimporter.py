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
# LOGGER 由 GpRunner 注入到 __main__ 全局（zipimporter 的 eval 作用域），
# 这里不再用 Jythonmod.LOGGER 覆盖 —— Paper 上 Jythonmod（Fabric 入口类）
# 会被 JythonCompatFinder 解析成 JavaPackage 子包（不抛异常），若执行
# `LOGGER = Jythonmod.LOGGER` 会把 LOGGER 污染成 JavaPackage，导致模组内
# LOGGER.info(...) 报 "'JavaPackage' object is not callable"。

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
    # GpRunner 注入到 __main__ 作用域的全局变量。zipimport 加载的模组拥有
    # 独立命名空间，Load() 加载后会把这些变量 setattr 到模组上，使模组内
    # 可以直接使用 LOGGER / ENV_TYPE / GAME_DIR / Script / API。
    _INJECTED_GLOBALS = ('LOGGER', 'ENV_TYPE', 'GAME_DIR', 'Script', 'API')

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
            # 注入运行时全局（模组命名空间独立，需显式拷贝）
            for _name in self._INJECTED_GLOBALS:
                if not hasattr(Mod, _name):
                    try:
                        setattr(Mod, _name, globals()[_name])
                    except KeyError:
                        # 防御：LOGGER 缺失时回退到 Java 侧 SLF4J。
                        if _name == 'LOGGER':
                            try:
                                import java
                                _lf = java.type('org.slf4j.LoggerFactory')
                                setattr(Mod, 'LOGGER', _lf.getLogger('jython-mod-python'))
                            except BaseException:
                                pass
            Mod.main()
        except zipimport.ZipImportError as e:
            _warn('Skip running "%s"...%s' % (self.path, str(e)))
        except AttributeError as e:
            _warn('Skip...The mod "%s" does not have method "%s"' % (self.path, self.env))

# ---------------------------------------------------------------------------
# 对外 API 注册表（JythonModApi）的 Python 侧视图
#
# Java 模组通过 JythonModApi.register(modId, apiName, obj) 注册 API，Python 侧
# 通过两种方式读取同一份活引用（只读）数据：
#   1. 全局 API 直取：API["modId"]["apiName"]
#   2. import 钩子：   from jython_api.modId import apiName
#                     from jython_api import modId  # 再 modId.apiName
#
# 值包装规则：
#   - Class 对象 -> JythonApiClass（可调静态方法 / 构造 / 访问嵌套类）
#   - 其它对象   -> 原样返回（GraalPy 宿主对象，实例方法可直接调用）
# ---------------------------------------------------------------------------


def _api_value_class(value):
    """判断 Java 对象是否为 Class（java.lang.Class 实例）。"""
    try:
        from net.luojiayuan.jython.mod.api import ApiReflect
        return bool(ApiReflect.isClass(value))
    except Exception:
        return False


def _api_wrap(value):
    """包装注册表中的值：Class -> JythonApiClass，其余原样。"""
    if _api_value_class(value):
        return JythonApiClass(value)
    return value


class JythonApiClass:
    """已注册 Class 对象的可调用引用。

    属性访问 -> 嵌套类 / 静态字段 / 静态方法（返回可调用）；调用 -> 构造。
    返回值若又是 Class 则继续包装（支持链式 Builder）。
    """

    def __init__(self, java_class):
        object.__setattr__(self, '_java_class', java_class)

    def __getattr__(self, name):
        if name.startswith('__') and name.endswith('__'):
            raise AttributeError(name)
        cls = object.__getattribute__(self, '_java_class')
        from net.luojiayuan.jython.mod.api import ApiReflect

        # 1. 嵌套类
        nested = ApiReflect.nestedClass(cls, name)
        if nested is not None:
            ref = JythonApiClass(nested)
            object.__setattr__(self, name, ref)
            return ref

        # 2. 静态字段（Java 侧不抛受检异常，null 表示无此字段）
        value = ApiReflect.getStaticFieldOrNull(cls, name)
        if value is not None:
            wrapped = _api_wrap(value)
            object.__setattr__(self, name, wrapped)
            return wrapped

        # 3. 静态方法（返回可调用，懒解析）
        def _static(*args):
            return _api_wrap(ApiReflect.callStatic(cls, name, *(_to_java_args(args))))
        object.__setattr__(self, name, _static)
        return _static

    def __call__(self, *args):
        from net.luojiayuan.jython.mod.api import ApiReflect
        return _api_wrap(ApiReflect.construct(
            object.__getattribute__(self, '_java_class'), *(_to_java_args(args))))

    def __repr__(self):
        try:
            return object.__getattribute__(self, '_java_class').getName()
        except Exception:
            return '<JythonApiClass>'


def _to_java_args(args):
    """把 Python 参数尽量转成 Java 侧好处理的形态（list -> ArrayList）。"""
    try:
        import java
        out = []
        for a in args:
            if isinstance(a, list):
                al = java.type('java.util.ArrayList')()
                for item in a:
                    al.add(item)
                out.append(al)
            else:
                out.append(a)
        return out
    except Exception:
        return args


class _ApiInnerView:
    """内层视图：API["modId"] 的返回，dict 风格只读。"""

    def __init__(self, java_map):
        self._map = java_map

    def __getitem__(self, api_name):
        if not self._map.containsKey(api_name):
            raise KeyError(api_name)
        return _api_wrap(self._map.get(api_name))

    def get(self, api_name, default=None):
        if not self._map.containsKey(api_name):
            return default
        return _api_wrap(self._map.get(api_name))

    def __contains__(self, api_name):
        return self._map.containsKey(api_name)

    def __len__(self):
        return self._map.size()

    def keys(self):
        return list(self._map.keySet())

    def values(self):
        return [_api_wrap(self._map.get(k)) for k in self.keys()]

    def items(self):
        return [(k, _api_wrap(self._map.get(k))) for k in self.keys()]

    def __repr__(self):
        return 'ApiView(mod={%s})' % ', '.join(repr(k) for k in self.keys())


class ApiView:
    """对外 API 注册表的 Python 侧只读视图（活引用，与 Java 注册表共享数据）。

    用法：API["modId"]["apiName"]，支持 in / get / keys / items / values / len。
    """

    def __init__(self, java_registry):
        self._map = java_registry

    def __getitem__(self, mod_id):
        inner = self._map.get(mod_id)
        if inner is None:
            raise KeyError(mod_id)
        return _ApiInnerView(inner)

    def get(self, mod_id, default=None):
        inner = self._map.get(mod_id)
        if inner is None:
            return default
        return _ApiInnerView(inner)

    def __contains__(self, mod_id):
        return self._map.containsKey(mod_id)

    def __len__(self):
        return self._map.size()

    def keys(self):
        return list(self._map.keySet())

    def values(self):
        return [self._ApiInnerViewOf(k) for k in self.keys()]

    def items(self):
        return [(k, self._ApiInnerViewOf(k)) for k in self.keys()]

    def _ApiInnerViewOf(self, mod_id):
        inner = self._map.get(mod_id)
        return None if inner is None else _ApiInnerView(inner)

    def __repr__(self):
        return 'ApiView({%s})' % ', '.join(repr(k) for k in self.keys())


class JythonApiPackage(types.ModuleType):
    """jython_api 包的代理模块：jython_api.<modId>[.<apiName>]。"""

    def __getattr__(self, name):
        if name == '__path__':
            return []
        if name.startswith('__') and name.endswith('__'):
            raise AttributeError(name)

        # 计算相对路径：'jython_api' -> ['modId', 'apiName', ...]
        parts = self.__name__.split('.')
        if parts[0] != 'jython_api':
            raise AttributeError(name)

        if len(parts) == 1:
            # jython_api.<modId> —— 返回该模组的子模块
            if name not in API:
                raise AttributeError(
                    "mod id %r is not registered in JythonModApi; "
                    "available: %s" % (name, list(API.keys())))
            sub = JythonApiPackage('jython_api.' + name)
            object.__setattr__(self, name, sub)
            sys.modules[sub.__name__] = sub
            return sub

        # jython_api.<modId>.<apiName> —— 返回包装后的 API 对象
        mod_id = parts[1]
        try:
            inner = API[mod_id]
        except KeyError:
            raise AttributeError("mod id %r is not registered" % mod_id)
        if name not in inner:
            raise AttributeError(
                "api %r is not registered for mod %r; available: %s"
                % (name, mod_id, list(inner.keys())))
        value = inner[name]
        object.__setattr__(self, name, value)
        return value


class JythonApiFinder:
    """把 jython_api 及其子模块交给 JythonApiPackage 代理的 meta_path finder。

    只认合法路径：
      - jython_api                      顶层包
      - jython_api.<modId>              已注册模组（未注册 -> None，import 抛 ImportError）
    更深路径（jython_api.<modId>.<apiName>）不 claim —— 由包的 __getattr__
    解析，避免把不存在的 apiName 误当成子模块。
    """

    def find_spec(self, fullname, path=None, target=None):
        if fullname == 'jython_api':
            return importlib.machinery.ModuleSpec(fullname, self, is_package=True)
        if fullname.startswith('jython_api.'):
            parts = fullname.split('.')
            if len(parts) == 2:
                mod_id = parts[1]
                if mod_id in API:
                    return importlib.machinery.ModuleSpec(fullname, self, is_package=True)
            return None
        return None

    def create_module(self, spec):
        return JythonApiPackage(spec.name)

    def exec_module(self, module):
        module.__path__ = []
        sys.modules[module.__name__] = module


def _install_jython_api():
    """把注入的 Java 只读 map 包装为 ApiView，并注册 jython_api 导入钩子。"""
    global API
    try:
        API = ApiView(API)
    except NameError:
        _warn('API global not injected by GpRunner; jython_api hook disabled')
        return
    if not any(type(f).__name__ == 'JythonApiFinder' for f in sys.meta_path):
        sys.meta_path.insert(0, JythonApiFinder())
    try:
        LOGGER.info("JythonApi view + import hook installed")
    except Exception:
        pass


_install_jython_api()
