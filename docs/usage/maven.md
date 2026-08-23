# Maven 依赖仓库（对外 API 发布）

其它 Java 模组可以通过 Maven 依赖本模组的 **API-only jar**
（`jython-mod-api`），在代码里调用 `JythonModApi.register(...)`，
把自己的 API 暴露给 Python 脚本（见 [对外 API](api.md)）。

仓库由 **Cloudflare Pages** 托管（免费、国内可访问），CI 在打 tag 时自动发布。

## 仓库坐标

```groovy
// 仓库地址（Cloudflare Pages 自定义域名）
maven { url = 'https://maven.luojiayuan.de5.net' }

// 依赖（把版本换成最新 tag）
modCompileOnly "net.luojiayuan.jython.mod:jython-mod-api:v1.2.0-pre"
```

> `jython-mod-api` 是 **API-only** 构件（纯 JDK，约 10KB，只含
> `JythonModApi` / `ApiReflect`）。它只用于编译期调用注册 API；
> **运行时仍需把完整模组 jar 放进 mods/ / plugins/**（完整 fat jar
> 从 [Releases](https://github.com/RyanLuoJiayuan1120/jython-mod/releases) 下载）。

## 一、Cloudflare Pages 初始化（一次性）

1. 登录 [Cloudflare Dashboard](https://dash.cloudflare.com)；
2. **Workers & Pages → Create → Pages**，项目名填 `jython-mod-maven`
   （可改，但要与 CI 里的 `CLOUDFLARE_PAGES_PROJECT` 一致）；
   连接方式选 **Direct Upload**（不用连 Git，CI 直接传文件）；
3. 项目创建后：**Custom domains → Set up a custom domain**，
   填 `maven.yourdomain.com` 并完成 DNS 绑定；
4. 记录 **Account ID**（Dashboard 首页右下角 / Workers 页面）。

## 二、GitHub Actions Secrets（一次性）

仓库 `Settings → Secrets and variables → Actions` 添加：

| Secret | 值 |
|--------|-----|
| `CLOUDFLARE_API_TOKEN` | 在 Cloudflare 创建 **API Token**，权限选 **Pages:Edit**（`Account > Cloudflare Pages > Edit`），Token 记为 `CLOUDFLARE_API_TOKEN` |
| `CLOUDFLARE_ACCOUNT_ID` | 账户 ID（见上） |

可选 variable：`CLOUDFLARE_PAGES_PROJECT`（默认 `jython-mod-maven`）。

## 三、发布流程

打 tag（或手动触发 `Publish Maven API` workflow）：

```bash
git tag v1.2.0-pre && git push origin v1.2.0-pre
```

CI 自动执行：

```
./gradlew publishMavenApiPublicationToGithubPagesRepository  # 生成 build/maven-repo
npx wrangler pages deploy build/maven-repo                    # 推到 Cloudflare Pages
```

发布后验证（浏览器或 curl）：

```
https://maven.luojiayuan.de5.net/net/luojiayuan/jython/mod/jython-mod-api/maven-metadata.xml
```

## 四、本地手动发布（调试用，不需要 CI）

```bash
# 生成 maven 目录到 build/maven-repo
./gradlew publishMavenApiPublicationToGithubPagesRepository

# 本地起个静态服务器验证（任选其一）
python3 -m http.server 8080 -d build/maven-repo

# 用 wrangler 手动部署（需本机登录 Cloudflare：npx wrangler login）
npx wrangler pages deploy build/maven-repo --project-name=jython-mod-maven
```

## 五、模组作者接入示例

```groovy
repositories {
    maven { url = 'https://maven.luojiayuan.de5.net' }
    // Fabric 还需 Fabric maven：
    maven { url = 'https://maven.fabricmc.net/' }
}

dependencies {
    // Fabric：modCompileOnly；NeoForge/Paper 用 compileOnly
    modCompileOnly "net.luojiayuan.jython.mod:jython-mod-api:v1.2.0-pre"
}
```

```java
import net.luojiayuan.jython.mod.api.JythonModApi;

public class MyMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // 建议在各平台最早的初始化阶段注册（Fabric preLaunch / NeoForge
        // 构造 / Paper onLoad），保证早于 Python 脚本运行，详见 api.md 时序一节。
        JythonModApi.register("mymod", new MyApi());
        JythonModApi.register("mymod", MyApiClass.class);
    }
}
```

## 相关文档

- 对外 API 的完整用法与时序约束：[api.md](api.md)
