package net.luojiayuan.jython.mod.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.LoggerFactory;

import net.luojiayuan.jython.mod.PythonLogger;

/**
 * GraalPy 包装逻辑集成测试：模拟 GpRunner 的初始化（注入全局变量、加载
 * zipimporter.py），验证 API 直取、import 钩子、Class 包装（静态方法/字段/
 * 构造/嵌套类链式）以及错误处理。不依赖 Minecraft。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JythonApiPythonTest {

    /** 实例 API。 */
    public static class GreetApi {
        public String greet(String name) {
            return "Hello " + name;
        }

        public int addTrade(int n) {
            return n + 1;
        }
    }

    /** Class API：静态方法 + 静态字段 + 嵌套 Builder。 */
    public static class Calc {
        public static final double PI = 3.14;

        public static int add(int a, int b) {
            return a + b;
        }

        public static class Builder {
            private int value = 1;

            public Builder set(int v) {
                this.value = v;
                return this;
            }

            public int build() {
                return value * 2;
            }
        }
    }

    /** 带构造参数、可被 Python 构造的类。 */
    public static class Named {
        private final String label;

        public Named(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private Context context;
    private Value bindings;

    @BeforeAll
    void setUp() throws Exception {
        JythonModApi.register("testmod", new GreetApi());          // 实例，key=GreetApi
        JythonModApi.register("testmod", Calc.class);              // 类，key=Calc
        JythonModApi.register("testmod", "NamedClass", Named.class); // 类，自定义 key
        JythonModApi.register("testmod", "custom", new GreetApi());  // 实例，自定义 key

        context = Context.newBuilder("python")
                .allowAllAccess(true)
                .hostClassLoader(getClass().getClassLoader())
                .build();

        Value sysModule = context.eval("python", "import sys; sys");
        sysModule.getMember("path").getMember("append").executeVoid("Lib");

        bindings = context.getBindings("python");
        bindings.putMember("LOGGER", new PythonLogger(LoggerFactory.getLogger("jython-api-test")));
        bindings.putMember("ENV_TYPE", "common");
        bindings.putMember("GAME_DIR", ".");
        bindings.putMember("Script", "test.zip");
        bindings.putMember("API", JythonModApi.readOnlyView());

        // 加载 zipimporter.py（注册 McReflect/JythonCompat/JythonApi finder 与 ApiView）
        try (InputStream is = getClass().getResourceAsStream("/assets/jython-mod/jython/zipimporter.py")) {
            assertTrue(is != null, "zipimporter.py not on test classpath");
            context.eval("python", new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @AfterAll
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @BeforeEach
    void cleanRegistry() {
        // 每个测试独立注册表，避免相互影响
        for (String modId : JythonModApi.modIds()) {
            JythonModApi.unregister(modId);
        }
        JythonModApi.register("testmod", new GreetApi());
        JythonModApi.register("testmod", Calc.class);
        JythonModApi.register("testmod", "NamedClass", Named.class);
        JythonModApi.register("testmod", "custom", new GreetApi());
    }

    /** 运行一段断言脚本，脚本把失败信息追加到全局 failures 列表。 */
    private List<String> runChecks(String pythonCode) {
        context.eval("python", "failures = []\n"
                + "def check(name, cond):\n"
                + "    if not cond: failures.append(name)\n"
                + pythonCode);
        Value failures = bindings.getMember("failures");
        List<String> result = new ArrayList<>();
        for (long i = 0; i < failures.getArraySize(); i++) {
            result.add(failures.getArrayElement(i).asString());
        }
        return result;
    }

    @Test
    void apiDictDirectAccess() {
        List<String> failures = runChecks("""
                api = API["testmod"]["GreetApi"]
                check("instance method", api.greet("Py") == "Hello Py")
                check("instance method 2", api.addTrade(41) == 42)
                check("custom key", API["testmod"]["custom"].greet("c") == "Hello c")
                """);
        assertTrue(failures.isEmpty(), "failed: " + failures);
    }

    @Test
    void apiDictKeysAndContains() {
        List<String> failures = runChecks("""
                check("in mod", "testmod" in API)
                check("in api", "Calc" in API["testmod"])
                check("not in", "nope" not in API)
                check("keys", sorted(API.keys()) == ["testmod"])
                check("len", len(API) == 1)
                """);
        assertTrue(failures.isEmpty(), "failed: " + failures);
    }

    @Test
    void classStaticMethodFieldAndCtor() {
        List<String> failures = runChecks("""
                calc = API["testmod"]["Calc"]
                check("static method", calc.add(20, 22) == 42)
                check("static field", calc.PI == 3.14)
                named = API["testmod"]["NamedClass"]("from-python")
                check("ctor", named.label() == "from-python")
                """);
        assertTrue(failures.isEmpty(), "failed: " + failures);
    }

    @Test
    void nestedBuilderChaining() {
        List<String> failures = runChecks("""
                calc = API["testmod"]["Calc"]
                b = calc.Builder()
                check("builder set->build", b.set(21).build() == 42)
                """);
        assertTrue(failures.isEmpty(), "failed: " + failures);
    }

    @Test
    void importHookDirectAndModuleStyle() {
        List<String> failures = runChecks("""
                from jython_api.testmod import Calc as DirectCalc
                check("import direct static", DirectCalc.add(40, 2) == 42)
                from jython_api import testmod
                check("module style", testmod.GreetApi.greet("m") == "Hello m")
                check("module style static", testmod.Calc.add(1, 1) == 2)
                """);
        assertTrue(failures.isEmpty(), "failed: " + failures);
    }

    @Test
    void keyErrorAndImportError() {
        List<String> failures = runChecks("""
                try:
                    API["nope"]
                    check("KeyError mod", False)
                except KeyError:
                    check("KeyError mod", True)
                try:
                    API["testmod"]["nope"]
                    check("KeyError api", False)
                except KeyError:
                    check("KeyError api", True)
                try:
                    from jython_api.nope import X
                    check("ImportError", False)
                except ImportError:
                    check("ImportError", True)
                """);
        assertTrue(failures.isEmpty(), "failed: " + failures);
    }

    @Test
    void liveReferenceSeesLateRegistration() {
        // Python 侧持有视图后，Java 侧再注册 —— 活引用立即可见
        JythonModApi.register("later", Calc.class);
        List<String> failures = runChecks("""
                check("live late mod", "later" in API)
                check("live late api", API["later"]["Calc"].add(1, 2) == 3)
                """);
        assertTrue(failures.isEmpty(), "failed: " + failures);
    }
}
