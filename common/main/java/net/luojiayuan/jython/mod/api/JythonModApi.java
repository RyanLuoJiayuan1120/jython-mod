package net.luojiayuan.jython.mod.api;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对外 API 注册表：允许其它 Java 模组把自己的 API 类 / 实例暴露给
 * 运行在本模组中的 Python 脚本。
 *
 * <p>结构为两层 map：{@code modId -> apiName -> Object}。
 *
 * <p>使用示例（第三方 Java 模组，建议在各平台 onInitialize / 插件启动时调用）：
 * <pre>{@code
 * // 注册一个实例（自动以类简单名 "TradeApi" 作为 key）
 * JythonModApi.register("mymod", new TradeApi());
 *
 * // 注册一个类（自动以类简单名 "Calculator" 作为 key，Python 侧可调用静态方法 / 构造）
 * JythonModApi.register("mymod", Calculator.class);
 *
 * // 自定义 key
 * JythonModApi.register("mymod", "trade", new TradeApi());
 * }</pre>
 *
 * <p>Python 侧用法见 {@code docs/usage/api.md}：
 * <ul>
 *   <li>全局 {@code API["mymod"]["TradeApi"]} 直取；</li>
 *   <li>{@code from jython_api.mymod import TradeApi} 导入钩子。</li>
 * </ul>
 *
 * <p>时序约束：本模组在各平台 onInitialize 阶段同步执行 Python 脚本。
 * 依赖本模组（depends）的 Java 模组初始化必然更晚，其注册在 Python 脚本
 * 首次运行期间可能不可见——需要"后注册" API 的 Python 模组应自行延后
 * 读取时机（如注册事件回调、server 启动后的事件里再读）。
 */
public final class JythonModApi {

    private static final Map<String, Map<String, Object>> REGISTRY = new ConcurrentHashMap<>();

    private JythonModApi() {
    }

    // ------------------------------------------------------------------
    // 注册
    // ------------------------------------------------------------------

    /**
     * 注册一个 API，key 自动取类简单名（类对象取 {@code Class.getSimpleName()}，
     * 实例取 {@code getClass().getSimpleName()}）。
     */
    public static void register(String modId, Object api) {
        register(modId, simpleName(api), api);
    }

    /**
     * 注册一个 API，使用自定义 key。已存在的 key 会被覆盖（后注册者赢）。
     *
     * @throws IllegalArgumentException modId / apiName 为空，或 api 为 null
     */
    public static void register(String modId, String apiName, Object api) {
        if (modId == null || modId.isEmpty()) {
            throw new IllegalArgumentException("modId must not be null or empty");
        }
        if (apiName == null || apiName.isEmpty()) {
            throw new IllegalArgumentException("apiName must not be null or empty");
        }
        if (api == null) {
            throw new IllegalArgumentException("api must not be null");
        }
        REGISTRY.computeIfAbsent(modId, k -> new ConcurrentHashMap<>()).put(apiName, api);
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    /**
     * 获取已注册的 API 对象。
     *
     * @throws IllegalArgumentException 该 modId / apiName 未注册
     */
    public static Object get(String modId, String apiName) {
        Map<String, Object> inner = REGISTRY.get(modId);
        if (inner == null || !inner.containsKey(apiName)) {
            throw new IllegalArgumentException(
                    "No API registered for modId='" + modId + "' apiName='" + apiName + "'");
        }
        return inner.get(apiName);
    }

    /**
     * 获取某个模组注册的全部 API（只读视图）。模组不存在时返回空 map。
     */
    public static Map<String, Object> getAll(String modId) {
        Map<String, Object> inner = REGISTRY.get(modId);
        return inner == null ? Collections.emptyMap() : Collections.unmodifiableMap(inner);
    }

    /**
     * 判断某个 API 是否已注册。
     */
    public static boolean has(String modId, String apiName) {
        Map<String, Object> inner = REGISTRY.get(modId);
        return inner != null && inner.containsKey(apiName);
    }

    /**
     * 已注册的全部模组 ID（只读集合）。
     */
    public static Set<String> modIds() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }

    // ------------------------------------------------------------------
    // 注销
    // ------------------------------------------------------------------

    /**
     * 注销某个模组注册的全部 API。
     */
    public static void unregister(String modId) {
        REGISTRY.remove(modId);
    }

    /**
     * 注销某个模组下的单个 API。
     */
    public static void unregister(String modId, String apiName) {
        Map<String, Object> inner = REGISTRY.get(modId);
        if (inner != null) {
            inner.remove(apiName);
        }
    }

    // ------------------------------------------------------------------
    // Python 侧只读视图
    // ------------------------------------------------------------------

    /**
     * 供 Python 侧使用的只读活视图：外层与内层均不可写，但底层引用同一份
     * 注册表——Java 侧后续注册立即可见（活引用）。
     */
    public static Map<String, Map<String, Object>> readOnlyView() {
        return new AbstractMap<>() {
            @Override
            public Map<String, Object> get(Object key) {
                Map<String, Object> inner = REGISTRY.get(key);
                return inner == null ? null : Collections.unmodifiableMap(inner);
            }

            @Override
            public boolean containsKey(Object key) {
                return REGISTRY.containsKey(key);
            }

            @Override
            public Set<String> keySet() {
                return Collections.unmodifiableSet(REGISTRY.keySet());
            }

            @Override
            public int size() {
                return REGISTRY.size();
            }

            @Override
            public Set<Entry<String, Map<String, Object>>> entrySet() {
                Set<Entry<String, Map<String, Object>>> entries = new HashSet<>();
                for (Entry<String, Map<String, Object>> e : REGISTRY.entrySet()) {
                    entries.add(new SimpleEntry<>(e.getKey(), Collections.unmodifiableMap(e.getValue())));
                }
                return Collections.unmodifiableSet(entries);
            }

            @Override
            public Map<String, Object> put(String key, Map<String, Object> value) {
                throw new UnsupportedOperationException("read-only view");
            }

            @Override
            public void putAll(Map<? extends String, ? extends Map<String, Object>> m) {
                throw new UnsupportedOperationException("read-only view");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("read-only view");
            }

            @Override
            public Map<String, Object> remove(Object key) {
                throw new UnsupportedOperationException("read-only view");
            }
        };
    }

    private static String simpleName(Object api) {
        if (api instanceof Class<?> cls) {
            return cls.getSimpleName();
        }
        return api.getClass().getSimpleName();
    }
}
