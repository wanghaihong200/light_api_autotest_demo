# light_api_autotest_demo

[light-api-tester](../light_api_autotest) 接口自动化测试框架的**业务接入标准模板**：以一个独立工程只依赖框架三方包的形式，覆盖框架全部核心能力的验证用例。

新业务工程接入框架时，以本工程为模板起步：改 `groupId/artifactId`、替换被测服务配置、按需删减用例即可。

## 它验证什么

| 测试类 | 框架能力 | 说明 |
|---|---|---|
| `AuthFlowTest` | token 鉴权 | 配置驱动自动登录（`/auth/login`），token 提取后注入 `X-Token` 请求头 |
| `ProductApiTest` | starter 自动配置 | `ApiClient`/Service 分层注入、POJO 自动映射（含中文）、流式断言助手 |
| `MultiEnvClientTest` | 多环境多客户端 | `Api.client("other")` 按名字取第二套系统的独立客户端，互不干扰 |
| `FilterDemoTest` | SPI 过滤器 | 匿名类实现 `Filter`，为该客户端所有请求统一加 `X-Channel` 打标 |
| `RetryDemoTest` | 失败重试 | `retryAnalyzer = ApiRetryAnalyzer.class`，5xx 自动重试、断言失败不重试 |
| `OpenMeteoRealApiTest` | 真实公网 API | Open-Meteo 天气接口（无鉴权），按 GMT 口径校验 `current_weather.time` 含当天 |
| `LoginDataDrivenTest` | DB 数据驱动 | `@DataProvider` 从 MySQL 拉取登录场景数据，响应 `user` 字段与 `users` 表逐字段比对 |

被测服务由 `DemoInfraListener` 启动的**双 WireMock 实例**模拟（18090 主系统 / 18091 第二套系统），测试结束自动关闭。

## 快速开始

前置条件：

1. **JDK**：8+（IDEA 运行）或 17（Maven 运行，`maven.compiler.release=8`）均已验证
2. **框架 SNAPSHOT**：本地 Maven 仓库需有 `com.wang.light:light-api-*:1.0.0-SNAPSHOT`。没有时到框架工程执行 `mvn install -DskipTests`
3. **外部依赖**（按用例需要，缺哪个就跳过对应用例）：
   - 外网连通（`OpenMeteoRealApiTest` 访问 api.open-meteo.com）
   - MySQL `localhost:3307`（root/root123），含 `huxuebao.tester_datasource_demo` 与 `test_platform.users`（`LoginDataDrivenTest`）
   - 本地用户服务 `127.0.0.1:8000`（`LoginDataDrivenTest`）

运行：

```bash
mvn test          # 走 testng.xml 全量执行
```

IDEA 中直接右键任意测试类/方法运行（单类运行同样有效，原因见「关键配置」）。

## 项目结构

```
light_api_autotest_demo/
├── pom.xml                          # 唯一框架入口依赖 light-api-spring-boot-starter + test 依赖
└── src/test/
    ├── java/com/demo/lightapi/
    │   ├── DemoApp.java             # @SpringBootApplication 入口（@SpringBootTest 用）
    │   ├── DemoInfraListener.java   # ISuiteListener：拉起/关闭双 WireMock 并注册全部 stub
    │   ├── AuthFlowTest.java        # 鉴权流
    │   ├── ProductApiTest.java      # CRUD + POJO 映射（service/、model/ 分层示例）
    │   ├── MultiEnvClientTest.java  # 多环境客户端
    │   ├── FilterDemoTest.java      # 请求打标过滤器
    │   ├── RetryDemoTest.java       # 5xx 重试
    │   ├── OpenMeteoRealApiTest.java# 真实公网 API
    │   └── LoginDataDrivenTest.java # DB 数据驱动
    └── resources/
        ├── application.yml          # 框架配置：profiles / auth / data-sources / 日志开关
        ├── testng.xml               # suite 定义（parallel=classes, thread-count=3）
        └── META-INF/services/
            └── org.testng.ITestNGListener   # DemoInfraListener 的 ServiceLoader 注册
```

## 关键配置

### application.yml（框架唯一配置面）

```yaml
light-api:
  env: test                  # 激活哪个 profile
  profiles:
    test:                    # 主被测系统（含 token 鉴权）
      base-url: http://localhost:18090
      auth:
        type: token
        login-path: /auth/login
        username: qa
        password: qa123
        token-json-path: $.data.token   # 从登录响应提取 token
        token-header: X-Token           # 注入到后续请求的 header
        token-prefix: ""
      data-sources:          # DB 访问（Db.query()/Db.source() 使用）
        main:
          url: jdbc:mysql://localhost:3307/huxuebao?useSSL=false&characterEncoding=utf8&serverTimezone=Asia/Shanghai
          username: root
          password: root123
    other:                   # 第二套系统（无鉴权）
      base-url: http://localhost:18091
    openmeteo:               # 公网 API（无鉴权）
      base-url: https://api.open-meteo.com
    usercenter:              # 用户中心（无鉴权，登录接口本身即被测对象）
      base-url: http://127.0.0.1:8000
```

要点：

- **鉴权按 profile 隔离**：配了 `auth` 的 profile，其客户端每个请求自动注入 token（首次请求懒登录，401 可配置重登重发）；不配 `auth` 则完全无鉴权动作
- **日志**：`log-body`（默认 `true`）控制 `[light-api]`（HTTP 请求头/请求体/响应）与 `[light-api-db]`（SQL/参数/结果）日志；`max-log-body-length`（默认 4096）统一截断

### 监听器注册：为什么是 META-INF/services 而不是 testng.xml

`DemoInfraListener` 通过 `src/test/resources/META-INF/services/org.testng.ITestNGListener`（ServiceLoader）注册。这是**入口无关**的方式：Maven surefire（走 testng.xml）与 IDEA 单类运行（`IDEARemoteTestNG` 不加载 suite XML）都会自动加载它，保证任何运行方式下 WireMock 都已就绪。往 testng.xml 里写 `<listeners>` 的方式在单类运行时会失效，不要使用。

## 常见任务

**新增一个被测系统**：`application.yml` 加 profile（按需配 `auth`/`data-sources`）→ 代码里 `Api.client("名字")` 或（主系统）直接注入 `ApiClient`。

**新增用例**：仿照现有类，`@SpringBootTest(classes = DemoApp.class)` + 注入客户端，或无 Spring 依赖时直接用 `Api.client()`；记得加进 `testng.xml` 的 `<classes>`。

**模拟 IDEA 单类运行**（排查入口差异时）：

```bash
mvn -q test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp "target/test-classes;target/classes;$(cat target/cp.txt)" \
     org.testng.TestNG -testclass com.demo.lightapi.AuthFlowTest -d target/sim-reports
```

## 故障排查

| 现象 | 原因与处理 |
|---|---|
| IDEA 单类运行报 `Connection refused: localhost:18090` | 监听器未生效。确认 `META-INF/services/org.testng.ITestNGListener` 存在且内容为 `com.demo.lightapi.DemoInfraListener`；不要只注册在 testng.xml |
| Maven 编译报「不再支持源选项 5」 | `maven-compiler-plugin` 未固定版本时 Maven 3.6 默认解析到 3.1（不认识 `maven.compiler.release`）。本工程已固定 3.11.0，新模板工程照抄 |
| `LoginDataDrivenTest` 中 `smokeviewer` 失败(401) | **已知数据漂移**：`tester_datasource_demo` 中该行密码与实际账号不符，经约定保持失败作为可见提醒；修正该行数据后用例自动通过 |
| 报告中 Skipped=1 | TestNG 将 `ApiRetryAnalyzer` 重试前的失败尝试记为 skipped，属重试机制的正常表现 |
| `SQL 查询失败 / Connection refused: 3307` | MySQL 容器未启动（本机为 `cicd-mysql`，宿主端口 3307） |

## 相关仓库

- 框架工程 `light_api_autotest`：core（客户端/配置/断言/SPI）、http（Unirest 引擎）、db、report、spring-boot-starter / starter，以及 ADR 决策文档
