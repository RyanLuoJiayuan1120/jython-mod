package net.luojiayuan.jython.mod.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JythonModApi 注册表纯逻辑测试（不依赖 Minecraft / GraalPy）。
 */
class JythonModApiTest {

    static class DummyApi {
    }

    static class OtherApi {
    }

    @BeforeEach
    @AfterEach
    void clean() {
        for (String modId : JythonModApi.modIds()) {
            JythonModApi.unregister(modId);
        }
    }

    @Test
    void registerWithAutoNameUsesSimpleClassName() {
        JythonModApi.register("mymod", new DummyApi());
        assertTrue(JythonModApi.has("mymod", "DummyApi"));
        assertTrue(JythonModApi.get("mymod", "DummyApi") instanceof DummyApi);
    }

    @Test
    void registerClassUsesSimpleClassName() {
        JythonModApi.register("mymod", DummyApi.class);
        assertTrue(JythonModApi.has("mymod", "DummyApi"));
        assertEquals(DummyApi.class, JythonModApi.get("mymod", "DummyApi"));
    }

    @Test
    void registerWithCustomName() {
        JythonModApi.register("mymod", "custom", new DummyApi());
        assertTrue(JythonModApi.has("mymod", "custom"));
        assertFalse(JythonModApi.has("mymod", "DummyApi"));
    }

    @Test
    void registerOverwritesExistingKey() {
        DummyApi first = new DummyApi();
        OtherApi second = new OtherApi();
        JythonModApi.register("mymod", "api", first);
        JythonModApi.register("mymod", "api", second);
        assertEquals(second, JythonModApi.get("mymod", "api"));
    }

    @Test
    void getThrowsWhenMissing() {
        assertThrows(IllegalArgumentException.class, () -> JythonModApi.get("nope", "nope"));
        JythonModApi.register("mymod", new DummyApi());
        assertThrows(IllegalArgumentException.class, () -> JythonModApi.get("mymod", "nope"));
    }

    @Test
    void getAllReturnsEmptyForMissingMod() {
        assertTrue(JythonModApi.getAll("nope").isEmpty());
    }

    @Test
    void getAllReturnsAllApisForMod() {
        JythonModApi.register("mymod", "a", new DummyApi());
        JythonModApi.register("mymod", "b", new OtherApi());
        Map<String, Object> all = JythonModApi.getAll("mymod");
        assertEquals(2, all.size());
        assertTrue(all.containsKey("a"));
        assertTrue(all.containsKey("b"));
    }

    @Test
    void modIdsListsRegisteredMods() {
        JythonModApi.register("modA", new DummyApi());
        JythonModApi.register("modB", new DummyApi());
        Set<String> ids = JythonModApi.modIds();
        assertTrue(ids.contains("modA"));
        assertTrue(ids.contains("modB"));
    }

    @Test
    void unregisterRemovesMod() {
        JythonModApi.register("mymod", new DummyApi());
        JythonModApi.unregister("mymod");
        assertFalse(JythonModApi.has("mymod", "DummyApi"));
        assertTrue(JythonModApi.modIds().isEmpty());
    }

    @Test
    void unregisterSingleApiKeepsOthers() {
        JythonModApi.register("mymod", "a", new DummyApi());
        JythonModApi.register("mymod", "b", new OtherApi());
        JythonModApi.unregister("mymod", "a");
        assertFalse(JythonModApi.has("mymod", "a"));
        assertTrue(JythonModApi.has("mymod", "b"));
    }

    @Test
    void registerRejectsInvalidArgs() {
        assertThrows(IllegalArgumentException.class, () -> JythonModApi.register("", new DummyApi()));
        assertThrows(IllegalArgumentException.class, () -> JythonModApi.register(null, new DummyApi()));
        assertThrows(IllegalArgumentException.class, () -> JythonModApi.register("mymod", "", new DummyApi()));
        assertThrows(IllegalArgumentException.class, () -> JythonModApi.register("mymod", null, new DummyApi()));
        assertThrows(IllegalArgumentException.class, () -> JythonModApi.register("mymod", "a", null));
    }

    @Test
    void readOnlyViewReflectsLiveRegistry() {
        JythonModApi.register("mymod", new DummyApi());
        Map<String, Map<String, Object>> view = JythonModApi.readOnlyView();

        // 活引用：注册后的数据立即可见
        assertTrue(view.containsKey("mymod"));
        assertTrue(view.get("mymod").containsKey("DummyApi"));

        // 只读：写入被拒绝
        assertThrows(UnsupportedOperationException.class, () -> view.put("x", Map.of()));
        assertThrows(UnsupportedOperationException.class, () -> view.get("mymod").put("y", new Object()));
        assertThrows(UnsupportedOperationException.class, view::clear);
        assertThrows(UnsupportedOperationException.class, () -> view.remove("mymod"));

        // 后续注册在视图中也可见（活引用）
        JythonModApi.register("mymod2", new OtherApi());
        assertTrue(view.containsKey("mymod2"));
    }
}
