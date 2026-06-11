# 技术栈现代化升级计划

本文档记录 yu-bi 技术栈现代化迁移的目标终态、阶段顺序、验收门槛和风险控制。它是后续升级工作的执行基线，避免把多个高风险迁移混在同一个提交里。

## 当前基线

- Java 运行时：JDK 21。
- 后端主框架：Spring Boot 3.5.12，Spring Cloud 2025.0.1。
- 构建工具：Maven，`maven-compiler-plugin` 3.14.1，`maven-assembly-plugin` 3.8.0，`exec-maven-plugin` 3.6.3。
- 前端运行时：默认工程化基线收口到 Node 24 LTS，并保留 Node 26 当前线兼容验证；开发和生产构建已切换到 Vite 6 稳定线。
- 前端主框架：React 18、React Router 6.30.1、Ant Design 5.26、TypeScript 5.9；CRA/CRACO 已退出前端主工作流。
- 已验证基线：
  - `npm run checkTs` 通过。
  - `npm run build:task` 通过。
  - `npm run build` 通过，默认使用 Vite 输出 `frontend/build`。
  - `mvn -DskipTests -Dexec.skip=false package -pl server -am` 通过，Maven `package` 阶段可复制 Vite 静态资源并生成 parser.js。
  - `mvn test -pl data-providers/jdbc-data-provider -am` 通过。
  - `GET /api/v1/sys/info` 返回 200。
  - `http://127.0.0.1:3001` 返回 200，`/api/v1/plugins/custom/charts` 返回成功。
  - Vite production preview 可渲染登录页。

## 目标终态

- 后端保持 Java 21+ 与 Spring Boot 稳定主线，优先使用 Boot BOM 管理版本，减少手写版本漂移。
- 前端迁出已退场的 Create React App，使用 Vite 作为现代构建工具。
- React 升级到 18+，后续再评估 React 19。
- UI 栈升级到 Ant Design 5/6，其中 Ant Design 6 需要先完成 React 18+。
- 路由升级到 React Router 6/7。
- 状态管理升级到 Redux Toolkit 2 与 React Redux 9。
- 浏览器自动化迁出 PhantomJS，使用 Selenium 4 + Chrome/Edge WebDriver，或 Playwright。
- JSON/JWT/HTTP/工具库统一到仍维护的现代库，删除重复和过时依赖。
- 构建和 CI 明确 Node、npm、Maven 最低版本，避免依赖开发者本机偶然状态。

## 最终现代化技术栈清单

这一节定义“项目最终完成现代化替代”时应达到的目标清单。后续每个阶段都要朝这个清单收敛，而不是只做局部兼容。

### 终态判定原则

- “现代化完成”不是把所有依赖都追到最新，而是满足四个条件：
  1. 进入仍活跃维护、与 JDK 21 / React 18 / Node 24 LTS 兼容，并经过 Node 当前线兼容验证的主线。
  2. 项目主运行时不再依赖已进入维护模式、已退场或高度历史化的基础栈。
  3. 本地、CI、Docker、发布包使用同一套可复现构建链。
  4. 关键业务路径有明确的自动或半自动验收证据，而不是只看能否编译。
- 因此本项目的“最终现代化替代方案”不要求在本轮直接把所有中长期架构专题一次做完，但要求：
  - 明确哪些项是本轮硬收口目标；
  - 明确哪些项是下一阶段专项；
  - 明确每项的目标栈、迁移前提、验收门槛和是否允许暂时保留。
- 只要某个栈仍被列入“长期暂存”，就不能宣称整个项目技术栈已经完全现代化；文档必须明确其剩余范围和退出路径。

### 后端目标清单

- 运行时与框架
  - JDK 21 LTS
  - Spring Boot 3 稳定主线
  - Spring Framework 6 / Jakarta EE 10 兼容链
- 数据与服务基础设施
  - MyBatis Spring Boot 3 主线
  - MySQL 驱动使用当前坐标 `com.mysql:mysql-connector-j`
  - Quartz 保持 Spring Boot 当前稳定主线
  - Redis、LDAP、Mail、Thymeleaf 维持 Boot BOM 管理
- 安全与认证
  - JWT 收敛到单一现代实现
  - 长期评估从 Shiro 迁到 Spring Security 原生安全体系
- JSON 与 HTTP
  - Web 层统一使用 Jackson
  - 自定义 JSON 解析尽量统一到 Jackson tree/model API
  - HTTP 客户端收敛到单一现代实现：JDK HttpClient / HttpClient 5 / OkHttp 三选一
- 浏览器自动化与导出
  - 不再依赖 PhantomJS
  - 使用 Playwright 或 Selenium 4 + Chromium Headless
- 旧基础库治理
  - 淘汰 `commons-lang 2`
  - 淘汰 `commons-io 1.x`
  - 收敛或升级 Guava、POI、Commons CSV、Calcite、MyBatis Generator、Nashorn

### 前端目标清单

- 构建与运行
  - Node 24 LTS 作为默认开发、构建、测试基线，并补 Node 当前线兼容验证
  - Vite 作为唯一主构建链
  - CRA / CRACO 完全退出主工作流
- 核心框架
  - React 18 稳定运行，后续再评估 React 19
  - React Router 升级到 6/7
  - Ant Design 升级到 5，之后再评估 6
- 状态与数据
  - Redux Toolkit 2
  - React Redux 9
  - Axios 1
- 样式与主题
  - 继续使用 `styled-components`，但升级到当前主线时需补齐 SSR、类型和 macro 兼容策略
  - AntD 主题能力统一到 token / CSS variables，不保留浏览器端 Less 动态编译链
- 测试与工具链
  - Testing Library 作为主测试方案
  - Enzyme 完全退出
  - Lint / Format / Type Check 与 Node 24 LTS 兼容，并补 Node 当前线兼容验证
- 兼容与运行策略
  - 明确是否放弃 IE 11；若放弃，则移除 `react-app-polyfill` 和相关历史兼容代码

### 构建、发布与工程化目标清单

- Maven / npm / Node / Java 版本约束全部显式化
- 本地、CI、Docker、发布包使用同一套产物生成链
- 发布包内静态资源、parser.js、配置目录和启动参数都可复现
- 服务启动、前端代理、后端 API、截图导出、分享页、多入口 HTML 都有自动或半自动验收路径

## 目标版本矩阵（2026-06）

这一节把“现代化替代方案”落到可执行版本线上。不是所有项都要求立刻追到最新大版本，但必须进入仍活跃维护、与当前架构相容、且后续可持续升级的主线。

### 已完成并锁定的基线

- JDK：`21 LTS`
- Spring Boot：`3.5.x` 稳定线
- Spring Cloud：`2025.0.x` 稳定线
- MyBatis Spring Boot Starter：`3.0.x`
- MySQL JDBC 坐标：`com.mysql:mysql-connector-j`
- Selenium：`4.31.0`
- Node：`24.x LTS` 默认基线，`26.x` 兼容验证
- Vite：`6.4.x`
- React：`18.3.x`
- React Router：`6.30.x`
- Redux Toolkit：`2.x`
- React Redux：`9.x`
- Axios：`1.x`
- TypeScript：`5.9.x`

### 已确定目标线，分批迁移中

- Ant Design：已进入 `5.26.x` 稳定线，6.x 暂不作为本轮硬目标
- `@ant-design/icons`：随 Ant Design 5 同步进入当前主线
- Jest：`27.5.x -> 30.x`，若验证成本更低可改为 Vitest，但二选一后必须收敛为单栈
- `styled-components`：`5.3.x -> 6.x`
- `jjwt`：`0.7.0 -> 0.12+`，已完成到 `0.12.7`
- Apache HttpClient：`4.5.14 -> 5.x`，已完成到 `5.5`
- `okhttp`：已确认生产代码使用面清零，退出主运行时依赖
- Apache POI：`5.0.0 -> 较新 5.x 稳定线`，已完成到 `5.5.1`
- Guava：`21.0 -> 较新稳定线`，项目自有生产代码使用面已清零
- Commons CSV：`1.8 -> 较新稳定线`，已完成到 `1.14.1`
- Commons Text：`1.9 -> 较新稳定线`，已移除直接依赖
- AspectJ Weaver：`1.9.8.M1 -> 正式 GA 版本`，已完成到 `1.9.25.1`
- H2：`1.4.200 -> 2.x`，已完成到 `2.4.240`

### 中长期演进项

- 安全框架：`Shiro 2 -> Spring Security`，本轮只做评估与前置拆障，不承诺一次完成
- 连接池：`Druid -> HikariCP`，需要在多数据源行为、监控诉求和 JDBC provider 实现稳定后推进
- 脚本引擎：`Nashorn -> GraalJS`，属于单独专项
- 代码生成链：`mybatis-generator-core` 独立治理，避免继续污染主运行时依赖面

## 项目级技术栈总表

这一节是后续执行时最常用的总览表。目标是让任何人都能快速回答 5 个问题：

1. 当前到底在用什么
2. 终态准备收敛到什么
3. 现在处于什么状态
4. 结论的证据是什么
5. 下一步应该做什么

### 后端总表

| 栈 | 当前值 | 终态目标 | 当前状态 | 当前证据 | 下一动作 |
| --- | --- | --- | --- | --- | --- |
| JDK | `21` | `21 LTS` | 已完成 | 根 POM、Enforcer、编译链通过 | 持续回归 |
| Spring Boot | `3.5.12` | `3.5.x` 稳定线 | 已完成 | 根 POM、server package 通过 | 持续回归 |
| Spring Cloud | `2025.0.1` | `2025.0.x` 稳定线 | 已完成 | 根 POM | 持续回归 |
| MyBatis Spring Boot | `3.0.4` | `3.0.x` | 已完成 | `core/pom.xml` | 持续回归 |
| MySQL JDBC | `com.mysql:mysql-connector-j` | 当前官方坐标 | 已完成 | `core/pom.xml` | 持续回归 |
| JSON 栈 | `Jackson` 单栈 | `Jackson` 单栈 | 已完成 | 生产代码 `fastjson` 检索清零 | 持续回归 |
| JWT | `jjwt 0.12.7` | 单一现代 JWT 栈 | 已完成主链升级 | `core/pom.xml`、`JwtUtils`、security/server compile 通过 | 后续评估是否继续收口到 Security JOSE |
| HTTP 客户端 | `HttpClient 5.5` | 单一现代 HTTP 客户端 | 已完成主链收口 | `core/pom.xml`、http/security 模块 compile 通过 | 补 TLS/行为回归 |
| 浏览器自动化 | `Selenium 4.31.0` | `Selenium 4` 或 `Playwright` | 已完成主链退出 PhantomJS | `core/pom.xml`、`server/pom.xml`、`WebUtils` 使用面 | 评估是否继续走 Playwright |
| 连接池 | `HikariCP` 主链 | `HikariCP` | 已完成主链切换 | server package、demo 健康检查 | 做多数据源/监控专项回归 |
| H2/demo | `2.4.240` | `2.x` 稳定线或更长期容器化测试库 | 已完成当前升级 | 依赖树、demo DB 重建、相关测试通过 | 固化 demo 数据维护策略 |
| POI | `5.5.1` | 较新 5.x 稳定线 | 已完成 | `core/pom.xml`、POIUtilsTest | 持续回归导出链 |
| Commons CSV | `1.14.1` | 当前稳定线 | 已完成 | `core/pom.xml` | 持续回归 |
| Guava | 项目自有生产代码已退出 | 不依赖自有 Guava API | 已完成 | 检索与依赖树 | 仅关注上游传递依赖 |
| Commons Lang 2 / IO 1.x | 主代码已退出 | 不再使用旧版本 | 已完成 | 检索与依赖树 | 持续回归 |
| 安全框架 | `Shiro 2.0.5` | `Spring Security` | 前置拆障进行中 | security 模块前置收口提交链 | 进入迁移设计与运行时接管 |
| 脚本引擎 | `GraalJS 25.0.1` + JSR-223 发现链 | `GraalJS` | 已完成主链替代 | 根 POM、`core/pom.xml`、`JavascriptUtilsTest`、整仓 `mvn -DskipTests package` | 补 JVMCI/性能专项评估 |
| SQL 解析内核 | `Calcite 1.26.0` | 较新稳定线 | 待专项预研 | data-provider-base 使用面与 warning | 做 parser/JDBC provider 专项设计 |
| 代码生成链 | `mybatis-generator-core 1.4.0` | 独立工具链/独立 profile | 已退出主运行时 | `core/pom.xml` profile 化 | 决定是否继续升级生成器 |

### 前端总表

| 栈 | 当前值 | 终态目标 | 当前状态 | 当前证据 | 下一动作 |
| --- | --- | --- | --- | --- | --- |
| Node | `24.x LTS` 默认基线，`26.x` 兼容验证 | `24.x LTS` 默认基线，按需保留 Node 当前线兼容验证 | 已完成本轮基线收口 | `package.json engines`、CI 双版本矩阵、`.nvmrc` | 持续回归 |
| Vite | `6.4.x` | 作为唯一主构建链 | 已完成本轮稳定线对齐 | `package.json`、`vite.config.mts`、`build` / `build:task` / `test:ci` 通过 | 持续优化分包 |
| task 打包链 | `Vite library mode` | 与 Vite 主链统一 | 已完成 | `vite.task.config.mts`、`build:task` 通过 | 清理残留历史依赖声明 |
| React | `18.3.1` | `18+`，后续再评估 `19` | 已完成 | `package.json`、build 通过 | 持续回归 |
| React Router | `6.30.x` | `6/7` | 当前主链已在 6 | 兼容层与主依赖 | 继续做路由回归，不急追 7 |
| Redux Toolkit | `2.12.0` | `2.x` | 已完成 | `package.json`、TS/build 通过 | 持续回归 |
| React Redux | `9.3.0` | `9.x` | 已完成 | `package.json`、typed hooks 收口 | 持续回归 |
| Ant Design | `5.26.2` | `5.x` 稳定线，后续再评估 6 | 已完成主升级 | `package.json`、页面构建与回归记录 | 清理 compat 壳与页面回归 |
| 时间体系 | `dayjs` 主链，局部值链待收尾 | `dayjs` 单栈 | 进行中 | 生产代码 `moment` 清零、task 产物已切换 | 收尾控件值链和页面回归 |
| 富文本 | `react-quill 2.0.0` + 本地适配层 | Quill 2 路线或更现代 React 封装 | 进行中 | `quillCompat`、`RichTextEditor`、本地插件模块 | 继续压缩对旧 `@types/quill` 内嵌类型路径的耦合 |
| 视频播放 | 原生 `<video>` | 原生能力 | 已完成 | VideoWidget 改造 | 持续回归 |
| 故事播放 | `reveal.js 6.0.1` | 暂保留当前主线 | 已完成主线升级 | `package.json` | 结合富文本专题复核 |
| 样式系统 | `styled-components 6.1.19` | `6.x` | 已完成主升级 | `package.json`、TS/build 通过 | 做稳定化复核 |
| 测试栈 | `Vitest 4` + `jsdom 29` | `Vitest` 单栈 | 已完成主链收口 | `package.json`、`vitest.config.mts`、`vitest.setup.ts`、全量 `test:ci` 通过 | 后续只做 warning 治理与覆盖率专题 |
| 代码规范链 | `ESLint 9` + `stylelint 17` + `Prettier 3` | 当前稳定主线 | 已完成主链升级，进入 warning 治理阶段 | `package.json`、`eslint.config.mjs`、`lint-staged 17`、`husky 9`、`commitlint 21`、`lint` / `lint:style` / `test:ci` 通过 | 继续按触达文件渐进收口 Prettier 3 历史格式差异 |
| 国际化 | `i18next 26.0.2` + `react-i18next 17.0.8` | 当前稳定主线 | 已完成主链升级 | `package.json`、hooks 用法与专项测试通过 | 后续评估 key / namespace 类型化 |
| IE11 残留 | 主运行时已退出 polyfill 主链 | 不再为 IE11 保留历史兼容壳 | 已完成主链退出 | `react-app-polyfill` 已移除 | 持续清理残留命名与文档 |

### 工程化总表

| 栈/链路 | 当前值 | 终态目标 | 当前状态 | 当前证据 | 下一动作 |
| --- | --- | --- | --- | --- | --- |
| Maven | `3.9+` 受 Enforcer 约束 | 显式最低版本 | 已完成 | 根 POM Enforcer | 持续回归 |
| npm | `11+` | 显式最低版本 | 已完成 | `package.json engines` | 持续回归 |
| Git hooks / 提交流程 | `husky 9` + `lint-staged 17` + `commitlint 21` | 进入当前维护主线并与 Node 24 LTS 兼容，同时补当前线验证 | 已完成本轮主升级 | `frontend/package.json`、`.husky`、`commitlint.config.mjs`、本轮 lint/test 校验 | 后续再评估是否把 root/子项目脚本继续收敛 |
| CI | Node 24 LTS + 26 / JDK 21 / package + health check | 与本地一致，并补 Node 当前线兼容矩阵 | 已完成主链对齐 | GitHub workflow | 持续补专项验证 |
| Docker | 直接消费安装包 | 与 package 链一致 | 已完成静态收口，镜像级实测不足 | Dockerfile、安装包结构 | 后续补 docker build/run 实测 |
| 发布包 | `mvn package` 统一产出 | 本地/CI/Docker 同产物 | 已完成主链收口 | server package、health check | 持续回归 |
| 健康检查 | `/api/v1/sys/info` | 最小稳定运行验收 | 已完成 | `scripts/check-demo-health.sh` | 后续可补 share 入口 smoke |

### 使用这张总表的规则

- `当前状态 = 已完成`：表示主链已经达到终态目标，后续只做稳定化回归
- `当前状态 = 进行中`：表示方向已定、主链已部分落地，但还不能宣称专题完成
- `当前状态 = 待评估`：表示当前仍在主链，但需要单独决定迁移路线
- `当前状态 = 待专项预研`：表示不能直接升级，必须先做设计和验证策略

## 遗留栈退出清单

这一节只管“哪些栈还没有完全退出终态视角”。它比审计表更严格，因为这里列出来的项都不能在项目收官时继续模糊带过。

| 栈 | 当前保留原因 | 退出条件 | 退出触发点 | 退出后的验收要求 |
| --- | --- | --- | --- | --- |
| `Shiro 2` | 当前生产鉴权链仍依赖它，且改动面跨登录、权限、分享页、OAuth2 | Spring Security 完整接管认证、鉴权、remember-me、OAuth2、分享认证 | JWT/OAuth/脚本链稳定后启动 Wave 5 | 登录、分享页、权限校验、remember-me、OAuth2 全链路通过 |
| `Calcite 1.26.0` | SQL 解析与 JDBC provider 强耦合，不能直接升版本 | 完成专项预研并通过 parser / function / JDBC provider 回归 | GraalJS 专题后或独立窗口 | JDBC provider 测试、SQL 渲染、函数校验通过 |
| `react-quill 2.0.0` / Quill 旧类型耦合 | 富文本功能面深，且自定义 blot/插件较多 | 完成 Quill 2 路线或明确的现代封装替换 | 时间体系收尾后启动富文本专题 | 编辑、只读、邮件、仪表板富文本回归通过 |
| `ESLint 9` | 已完成 flat config 迁移，当前剩余工作主要是历史 warning 治理 | 继续压缩 Prettier 历史差异和无效 eslint-disable 注释 | 已完成 | lint/type check 链稳定，Node 24 LTS 下配置与 hooks 一致，并补 Node 当前线验证 |

### 遗留栈关闭规则

只有同时满足以下条件，某项遗留栈才可以从这张清单移除：

1. 仓库中的主依赖声明已切到目标方案
2. 生产代码不再直接依赖旧栈
3. 文档中的审计表与总表已同步更新
4. 相关专项验收已经补证据

如果只满足其中 1-2 项，而验收和文档没跟上，只能算“已进入迁移中”，不能从遗留栈清单删除。

### 项目收官前必须清空或给出保留结论的项

在项目最终收官前，下列项必须逐一处理，不能悬空：

- `Shiro 2`
- `Calcite 1.26.0`
- `react-quill 1.3.5`
- `Jest 29`

其中前 3 项属于后端架构级保留项；如果最终没有彻底替换，也必须写出“为何保留、保留边界、退出前提、未来触发条件”的正式结论。

## 项目总控蓝图

这一节不是历史记录，而是后续整个大型迁移项目的执行主表。目标是把“看起来要做很多事”的升级工程，收敛成可以持续推进、持续验收、持续回退的项目计划。

### 项目边界

- 本项目的最终目标不是“把版本号改新”，而是让仓库的主运行时、主构建链、主测试链和主发布链都落在仍活跃维护的现代技术栈上。
- 本项目允许保留少量短期兼容层，但不允许把兼容层当长期终态；每个兼容层都必须有退出条件。
- 本项目允许中长期专题分阶段完成，但不能在文档或对外结论里把“仍在专项中”的栈误报成“已经全部现代化”。

### 执行原则

1. 一次只推进一个主题，不把前端主栈、后端基础设施和工程化改造混在同一个 checkpoint。
2. 先收口边界，再替换内核；先缩小耦合面，再做真正的大替换。
3. 先做低风险、高收益、能减少后续改动面的动作，再做架构级硬替换。
4. 每个主题都必须同时具备：
   - 目标替代方案
   - 迁移前提
   - 退出条件
   - 验收证据
   - 回退路径
5. 所有“已完成”结论都必须以当前仓库和当前验证结果为依据，不使用记忆性判断。

### 工作流拆分

整个项目按 6 条长期工作流推进：

| 工作流 | 目标 | 典型主题 | 是否允许并行 |
| --- | --- | --- | --- |
| W1 前端运行时主栈 | 保持 React/Vite/AntD/Router 主链现代化且可运行 | AntD 稳定化、时间体系、富文本 | 可与 W5 并行 |
| W2 前端工具链 | 收口测试、lint、格式化、task 打包链 | Jest/Vitest、ESLint、Stylelint、Prettier | 可与 W1 并行，但不得阻塞主业务回归 |
| W3 后端运行时主栈 | 收口 HTTP、JSON、连接池、脚本、SQL 解析等运行时基础设施 | HttpClient 5、HikariCP、GraalJS、Calcite | 仅允许一个高风险专题同时 in progress |
| W4 后端安全体系 | 统一认证与授权体系 | Shiro -> Spring Security | 不与 W3 的其它高风险专题并行 |
| W5 工程化与交付 | 让本地/CI/Docker/发布包使用同一产物链 | package、Docker、健康检查、版本约束 | 可持续并行 |
| W6 数据与生成链 | 收口 demo/H2、代码生成链、样例数据 | H2 2.x、mybatis-generator | 可与 W5 并行 |

### 推荐执行顺序

后续不建议再按“看到什么旧就升什么”的方式推进，而应固定为以下顺序：

| 波次 | 主题 | 目标 | 进入条件 | 退出条件 |
| --- | --- | --- | --- | --- |
| Wave 0 | 基线锁定 | 固化当前 JDK 21 / Boot 3 / Node 24 LTS 默认基线、Node 26 兼容验证、Vite 6 稳定线 / React 18 基线 | 已完成 | 基线文档、CI、构建链一致 |
| Wave 1 | 前端时间体系收尾 | 把 `moment` 专题从“主链已迁”推进到“专题完成” | AntD 5 主升级已稳定 | 生产代码、task 产物、时间控件值链和分享页回归全部闭环 |
| Wave 2 | 富文本与页面稳定化 | 收口 `react-quill 1.x` 旧生态，完成 Quill 升级路线决策 | 时间体系主链稳定 | `RichTextEditor` 包装层稳定，升级路线定稿并完成至少一批落地 |
| Wave 3 | 前端工具链现代化 | 收敛 Vitest 单栈，清理 lint/format 历史壳 | 富文本与时间链路不再频繁波动 | 测试链、lint 链、task 打包链全部收口到明确单栈 |
| Wave 4 | 脚本与运行时专题 | 已完成 `Nashorn -> GraalJS`，继续 Calcite / 代码生成链 | 前端主栈波动降低 | SQL 解析专题有完整专项验证 |
| Wave 5 | 安全体系专题 | `Shiro -> Spring Security` | JWT/OAuth/脚本链稳定，前置拆障完成 | 登录、分享页、权限校验、remember-me、OAuth2 全链路迁移完成 |
| Wave 6 | 总体验收与残项清零 | 收口剩余 compat 壳、长期保留结论和发布验收 | 前 5 个波次均有阶段证据 | 全部终态项有证据，剩余保留项有明确结论 |

### 当前建议的未来 12 个 checkpoint 队列

这 12 个 checkpoint 是“接下来按什么顺序做最合理”的具体落地版本。它们不是强制一次做完，但每完成一个都应更新本文档状态。

1. `moment -> dayjs` 值对象链收尾
2. 时间筛选器 / 变量页 / 分享页时间回归
3. 富文本 compat 层继续收口
4. `react-quill` 升级路线实验与定稿
5. 继续迁移 Jest 存量测试到 Vitest 兼容写法
6. ESLint 9 warning 治理与 stylelint / Prettier 持续收口
7. Calcite / parser / JDBC provider 升级预研
8. Spring Security 迁移设计稿与映射表
9. `Shiro -> Spring Security` 第一批运行时接管
10. 前端 chunk 拆分与重依赖隔离
11. Docker / 发布包 / smoke 验证增强
12. 最终集成验收与保留项结论固化

### 不允许并行推进的高风险组合

以下主题不得在同一个 checkpoint 或同一时间窗口内并行推进：

- `Shiro -> Spring Security` 与 `Nashorn -> GraalJS`
- `Shiro -> Spring Security` 与 `Calcite` 大版本升级
- `react-quill` 内核升级 与 `Jest/Vitest` 主测试链切换
- `AntD 6` 追版本尝试 与 时间体系 / 富文本主专题
- `Docker/发布包` 构建链改造 与 `Spring Boot` 主框架切换

原因很简单：这些组合一旦同时改，会失去定位回归根因的能力。

### 允许并行推进的低耦合组合

以下组合可以并行，但仍然要求各自单独提交和单独验收：

- `moment` 收尾 与 `Jest warning` 清理
- 富文本 compat 收口 与 `ESLint/stylelint` 规则收敛
- `mybatis-generator` 工具链治理 与 `Docker/发布包` 验收增强
- H2/demo 数据治理 与前端局部页面回归

### 分支与版本管理策略

- 当前现代化改造默认采用 `codex/<topic-name>` 专题分支推进。
- `main` 分支只接收 merge，不直接在 `main` 上串行提交专题改造。
- 默认策略：
  - 每个 checkpoint 先在独立专题分支完成实现与验证
  - 若同一专题需要多轮里程碑，可继续在该专题分支上串行推进
  - 专题验收通过后，再通过 merge 方式回到 `main`
- commit 策略：
  - 每个 checkpoint 一个中文 commit
  - commit message 必须直接描述本轮专题，不使用“update”这类空泛词
  - 每个 commit 都必须能单独解释“改了什么、为什么、怎么验”
- 禁止事项：
  - 不把 `package-lock`、临时目录、构建产物噪音混入 checkpoint
  - 不把多个高风险专题堆进一个 commit
  - 不在未补文档和验收证据的情况下宣称阶段完成

### 阶段门禁

每个专题在进入下一阶段前，都要过 4 个门禁：

| 门禁 | 问题 | 最低证据 |
| --- | --- | --- |
| G1 设计门 | 目标替代方案和退出条件是否清楚 | 文档有“目标 / 前提 / 风险 / 验收 / 回退” |
| G2 编译门 | 当前修改是否破坏主链构建 | `npm` / `mvn` 对应命令通过 |
| G3 运行门 | 关键运行时路径是否还可用 | `/api/v1/sys/info`、首页、share 入口、核心专题 smoke 通过 |
| G4 集成门 | 是否足够安全进入下一主题 | 当前 checkpoint 已提交，文档已更新，工作树干净 |

### 验收矩阵

后续所有主题都必须落到这个统一验收矩阵，避免每次临时发明标准。

| 主题类型 | 必跑命令 | 补充验收 |
| --- | --- | --- |
| 前端页面/组件 | `npm run checkTs`、`npm run build` | 涉及 task 链时加 `npm run build:task`；涉及测试链时跑定向 Jest/Vitest |
| 前端工具链 | `npm run checkTs`、`npm run build`、对应测试命令 | 验证 `package.json`、配置文件、CI 行为一致 |
| 后端普通专题 | `mvn -pl server -am -DskipTests compile` | 相关模块 compile/test |
| JDBC / SQL / Calcite | `mvn -pl data-providers/jdbc-data-provider -am test` 或定向测试 | 验证 parser、函数、SQL 渲染 |
| 安全专题 | `mvn -pl security -am -DskipTests compile`、`mvn -pl server -am -DskipTests compile` | 登录、分享页、权限校验、OAuth2 |
| 集成专题 | `mvn -DskipTests -Dexec.skip=false package -pl server -am` | `/api/v1/sys/info`、首页、至少一个 share 入口 |

### 风险登记与止损规则

- 若某专题连续 2 个 checkpoint 都只能“继续兼容、不能减少耦合”，应暂停并重新审视路线。
- 若某专题的回归范围已经超出单模块边界，应升级为独立专项，不再按低风险收口推进。
- 若某主题没有自动化证据，只能依赖人工验证，则必须把人工检查表补进文档。
- 若某栈短期无法替换，则必须明确写成：
  - 为什么暂时保留
  - 保留范围
  - 退出前提
  - 退出触发条件

### 最终总体验收拆解

“整个技术栈清单都是现代化替代方案”要拆成 8 个最终问题逐项证明：

1. 后端主运行时是否全部处于现代主线
2. 前端主运行时是否全部处于现代主线
3. 旧构建链是否全部退出主工作流
4. 旧测试链是否全部退出主工作流
5. 历史架构专题是否完成迁移，或至少有被验证的保留结论
6. 本地、CI、Docker、发布包是否使用同一产物链
7. 关键业务链路是否有稳定验收证据
8. 文档中的“已完成”是否都能被当前仓库和当前验证结果证明

如果这 8 个问题里任意一个仍是“部分成立”，项目就只能表述为“现代化迁移进行中”，不能表述为“全部完成”。

### Wave 执行板

这一节把总控波次落成可跟踪的执行板。后续推进时，只更新状态，不改波次定义。

| Wave | 主题 | 状态 | 负责人动作 | 阻塞项 | 进入下一 Wave 的条件 |
| --- | --- | --- | --- | --- | --- |
| Wave 0 | 基线锁定 | 已完成 | 持续验证基线不回退 | 无 | 基线仍稳定 |
| Wave 1 | 时间体系收尾 | 已完成 | 转入后续专题，保留当前回归基线 | 无 | 时间专题证据已闭环 |
| Wave 2 | 富文本路线定稿与落地 | 进行中 | 继续压缩 `react-quill` 直接耦合面，定稿 Quill 路线并做第一批升级 | 仍需补富文本页面级验收与路线决策 checkpoint | 富文本适配层稳定、路线已定 |
| Wave 3 | 前端工具链现代化 | 待执行 | 定 Jest/Vitest、升级规范链 | 依赖 Wave 2 稳定 | 测试链与规范链路线明确 |
| Wave 4 | GraalJS / Calcite / 生成链 | 待执行 | 先做 GraalJS，再做 Calcite 预研 | 属于高风险运行时专题 | 脚本与 SQL 专题均有专项验证 |
| Wave 5 | Spring Security 专题 | 待执行 | 启动 Shiro 接管迁移 | 依赖 JWT/OAuth/脚本链稳定 | Spring Security 主链接管完成 |
| Wave 6 | 总体验收与残项清零 | 待执行 | 清 compat 壳、补总体验收 | 依赖前面专题都形成证据 | 所有终态项可逐条证明 |

### 业务验收矩阵

技术栈现代化最终要落回业务能力，这一节定义后续每个专题都应该关心哪些业务面。

| 业务域 | 核心能力 | 受影响的主要专题 | 最低验收方式 |
| --- | --- | --- | --- |
| 登录与鉴权 | 登录、登出、remember-me、权限判断 | Spring Security、JWT、Shiro 拆障 | 登录成功、登出成功、受限资源访问控制正确 |
| 分享页 | 分享登录、口令访问、分享图表/看板/故事板 | Router、AntD、Spring Security、时间体系 | 至少一个 share 入口可访问并完成认证 |
| 可视化主流程 | 图表预览、看板编辑、故事板播放 | AntD、Router、时间体系、富文本 | 首页、图表、看板、故事板基本操作可运行 |
| 富文本 | 编辑、只读渲染、邮件模板、看板富文本 | `react-quill` / Quill 路线 | 编辑、保存、回显、只读展示一致 |
| 数据执行 | SQL 渲染、函数校验、变量替换、HTTP 数据源 | Calcite、GraalJS、HttpClient 5 | JDBC provider 定向测试与关键链路 smoke |
| 导出与截图 | Excel/PDF 导出、缩略图/截图 | POI、PDFBox、Selenium/Playwright | 导出成功、截图链可执行 |
| 发布与交付 | 安装包、Docker、健康检查 | Maven、Vite、Docker、CI | package 通过、健康检查通过、Docker 待实测 |
| 配置与 demo | demo profile、H2、样例数据 | H2、生成链、脚本链 | demo 启动成功、`/api/v1/sys/info` 返回 200 |

### 业务验收分层

后续所有专题都按 3 层验收：

1. `L1 编译层`
   - 对应 `npm run checkTs`、`npm run build`、`mvn compile`
2. `L2 运行层`
   - 对应 `/api/v1/sys/info`、首页、share 入口、定向 smoke
3. `L3 业务层`
   - 对应登录、可视化、富文本、导出、数据执行等真实工作流

如果某专题只通过了 L1/L2，没有 L3 证据，就不能宣称“功能和之前一致”。

### 2026-06-11 运行态补证据

- 本轮新增的真实阻塞修复：
  - `server/src/main/java/datart/server/config/CustomPropertiesValidate.java`
    - 调整 demo profile 与 `datartConfig` 属性源顺序，避免 `application-demo.yml` 压过命令行 `--server.port` 和系统环境变量。
    - 实际结果：安装包 demo 已可稳定监听 `8081`。
  - `server/src/main/java/datart/server/controller/CustomPluginController.java`
  - `server/src/main/java/datart/server/controller/DataProviderController.java`
  - `server/src/main/java/datart/server/controller/DownloadController.java`
  - `server/src/main/java/datart/server/controller/FileController.java`
  - `server/src/main/java/datart/server/controller/OrgController.java`
    - 补齐 `@RestController`，恢复 `/api/v1/orgs`、`/api/v1/data-provider/providers`、`/api/v1/download/tasks`、`/api/v1/plugins/custom/charts` 等主应用初始化接口注册。
  - `core/src/main/java/datart/core/mappers/ext/DownloadMapperExt.java`
  - `server/src/main/java/datart/server/service/impl/DownloadServiceImpl.java`
    - 把下载任务最近 7 天筛选从 H2 2.x 不兼容的 `NOW() - INTERVAL 7 DAY` 改成 Java 侧计算 `createdAfter` 参数，消除 demo 登录后的 H2 SQL 语法错误。
  - `core/src/main/java/datart/core/mappers/UserMapper.java`
  - `core/src/main/java/datart/core/mappers/UserSqlProvider.java`
    - 将 H2 2.x 下保留字表名 `user` 改为反引号引用 `` `user` ``，恢复 share 链路在 `UserMapper` 上的查询与更新。
  - `frontend/src/app/pages/MainPage/index.tsx`
  - `frontend/src/app/pages/MainPage/pages/SchedulePage/hooks.ts`
  - `frontend/src/app/pages/MainPage/pages/SchedulePage/index.tsx`
  - `frontend/src/app/pages/MainPage/pages/SchedulePage/Sidebar/index.tsx`
  - `frontend/src/app/pages/MainPage/pages/SchedulePage/EditorPage/index.tsx`
    - 修复调度页兼容路由参数链，让 `/schedules/add` 与 `/schedules/:scheduleId` 可以在当前 Router 6 兼容层下正确解析 `scheduleId`，恢复编辑页和时间范围控件渲染。
  - `frontend/src/app/pages/SharePage/hooks/useShareRouteParams.ts`
  - `frontend/src/app/pages/SharePage/Dashboard/ShareDashboardPage.tsx`
  - `frontend/src/app/pages/SharePage/Chart/ShareChartPage.tsx`
  - `frontend/src/app/pages/SharePage/StoryPlayer/ShareStoryPlayerPage.tsx`
    - share 独立入口不再依赖当前兼容层下拿不到参数的 `useParams()`，改为显式 `matchPath` 提取 `shareToken`，恢复 `/shareDashboard/:token`、`/shareChart/:token`、`/shareStoryPlayer/:token` 的真实参数解析。

- 本轮新增运行态证据：
  - 构建链：
    - `npm run checkTs`
    - `npm run build`
    - `mvn -o -pl server -am -DskipTests compile`
    - `mvn -o -pl server -am -DskipTests package`
  - 安装包 demo：
    - `java --add-opens=java.base/java.lang=ALL-UNNAMED -Dfile.encoding=UTF-8 -cp "lib/*" datart.DatartServerApplication --spring.profiles.active=demo --server.port=8081 --datart.server.address=http://127.0.0.1:8081`
    - 启动日志确认 `http-nio-0.0.0.0-8081`
  - 页面回归：
    - `http://127.0.0.1:8081/login`
    - `http://127.0.0.1:8081/organizations/f8435e0a3323459aaef679ab63fbd01a/vizs`
    - `http://127.0.0.1:8081/organizations/f8435e0a3323459aaef679ab63fbd01a/variables`
    - `http://127.0.0.1:8081/organizations/f8435e0a3323459aaef679ab63fbd01a/schedules/add`
    - `http://127.0.0.1:8081/shareDashboard/b5a9fafba0294a2788168a7b87648d01?type=NONE`
  - 实际观察结果：
    - 主应用初始化完成后，本次刷新窗口下不再产生新的 `download/tasks`、`custom charts` 或 H2 `INTERVAL 7 DAY` 错误。
    - 变量页“新建公共变量”弹窗切到“日期”值类型后，日期输入框和日期格式下拉真实渲染。
    - 调度页“新建定时任务”编辑页真实渲染，`有效时间范围` 的开始/结束日期输入框可见。
    - share API `POST /api/v1/shares/b5a9fafba0294a2788168a7b87648d01/viz` 在当前 demo 数据下返回 200，并可拿到 dashboard 明细与 `authorizedToken`。
    - share dashboard 冷启动页可真实渲染空看板态，不再是白屏；应用内浏览器 DOM 片段包含 `DashboardForShare` 容器、`more` 菜单按钮，以及 Ant Design Empty 的“暂无数据”内容。

  - 当前结论：
  - Wave 1 已完成。时间体系相关的主应用页与 share 入口均已补齐真实 L2 运行态证据。
  - 当前 share 验收样例使用的是空 dashboard，因此页面呈现“暂无数据”是符合预期的业务状态，不代表渲染失败。

### 2026-06-11 Wave 2 继续推进：富文本入口继续收口

- 新增结论：
  - `react-quill` 官方 2025 README 已明确提示“在 React 中使用 Quill 时，你大概率不需要这个库”。
  - `react-quill 2.0.0` 的 `package.json` 仍依赖 `quill ^1.3.7`，因此“升级到 `react-quill 2`”只能算 React 包装层升级，不能当作“已经进入 Quill 2 主线”。
  - 基于这一事实，Wave 2 的短期目标应继续放在“压缩仓库对 `react-quill` / Quill 1 的直接耦合面”，而不是草率把 `react-quill 2` 误报成终态替代方案。

- 本轮继续收口的代码边界：
  - `frontend/src/app/components/ChartGraph/BasicRichText/RichTextBootstrap.ts`
  - `frontend/src/app/components/ChartGraph/BasicRichText/RichTextEditor.tsx`
  - `frontend/src/app/components/ChartGraph/BasicRichText/ChartRichTextAdapter.tsx`
  - `frontend/src/app/pages/MainPage/pages/SchedulePage/EditorPage/EmailSettingForm/CommonRichText.tsx`
  - `frontend/src/app/pages/DashBoardPage/components/Widgets/RichTextWidget/RichTextWidgetCore.tsx`
  - 调整内容：
    - 把 `react-quill` 的样式加载和 `RichTextPluginLoader` 注册入口收敛到 `RichTextEditor` 单点 bootstrap。
    - 业务页和业务组件不再各自重复引入 Quill 样式或手动触发插件注册。

- 本轮收益：
  - 富文本相关页面进一步从“到处显式依赖 Quill 运行时副作用”收敛到“统一依赖仓库内 editor 包装层”。
  - 后续无论选择 `react-quill 2.x` 作为中间态，还是直接切到真正的 Quill 2 React 封装，都可以优先只调整 `quillCompat.ts`、`RichTextEditor.tsx` 与 `RichTextBootstrap.ts` 这一小块边界。

- 当前仍未完成项：
  - 还没有完成基于 Quill 2 目标的组件级兼容核验，尤其是 `MarkdownModule`、`TagBlot`、`CalcFieldBlot` 和 `selection-change` 相关行为。
  - 还需要补富文本页面级验收证据，覆盖图表富文本、仪表板富文本和调度邮件富文本三条主要业务链。

### 2026-06-11 Wave 3 预热：页面级懒加载与低风险依赖升级

- 本轮实际落地：
  - `frontend/src/app/pages/MainPage/index.tsx` 改为继续沿用仓库现有 `defaultLazyLoad` 模式，把 `ChartEditor`、主工作台页、故事播放/编辑页收敛到路由级懒加载。
  - 新增：
    - `frontend/src/app/pages/MainPage/PageLoadables.tsx`
    - `frontend/src/app/pages/DashBoardPage/pages/Board/Loadable.tsx`
    - `frontend/src/app/pages/SharePage/StoryPlayer/StoryPlayerForShare/Loadable.tsx`
  - `StoryBoardPage/components/StoryPageItem.tsx` 与分享故事页 `StoryPageItem.tsx` 不再同步直连重型 `Board` 渲染链，改为按需加载。
  - 升级并验证低风险依赖：
    - `dayjs 1.11.21`
    - `@antv/s2 2.7.2`
    - `echarts-wordcloud 2.1.0`
  - 新增 `frontend/src/app/components/DndProviderCompat.tsx`，把当前 `react-dnd 14` 与 TypeScript 5.9 / React 18 组合下的 `children` 类型噪音收口到单点兼容层。
  - `frontend/src/app/utils/time.ts` 改为统一兼容历史 `W / Q` 时间单位写法，避免 `dayjs` 新类型收紧后继续在业务调用点散落适配代码。

- 本轮验证结果：
  - `npm run checkTs` 通过。
  - `npm run build` 通过。
  - 故事页链路已稳定拆出独立小 chunk，例如 `BoardPageItem.*.js`、`StoryPageItem.*.js`。
  - `ChartEditor` 已从主路由入口中拆出独立 chunk。
  - 主入口仍然偏大，最新构建下最大 `index.*.js` 约 `4.18 MB`，说明下一步重点仍应放在业务级页面/工作台继续拆分，而不是只做 vendor 手工分包。

- 本轮调研结论：
  - `react-dnd 14` 仍可运行，但仓库内还保留 `DragLayer` / `DragSource` / `DropTarget` 等旧 HOC 时代 API。直接升到 `react-dnd 16` 会触发构建和类型断裂，因此这条线应单列专题。
  - 更现代拖拽替代路线可指向 `dnd-kit`。根据其官方文档与 README，当前主线是基于 hooks 的现代 React 拖拽工具包。
  - `i18next 19` + `react-i18next 11` 当前可运行，但已明显落后于活跃主线；官方 README 也明确现代 React 用法以 hooks 为主。该项适合放入前端工具链现代化专题，而不是和富文本/安全专题并行硬推。
  - `Spring Security` 仍是当前 Spring 官方安全主线，因此 `Shiro -> Spring Security` 继续保持为高优先级长期目标，但必须单列 Wave 5 安全专题推进。

- 下一步建议：
  - 继续拆 `MainPage` 内部高耦合页面与编辑链，优先查 `VizPage`、`ViewPage`、`SourcePage` 等是否还有工作台级同步重依赖。
  - 单列 `react-dnd -> dnd-kit` 迁移设计，先统计 HOC API 使用面，再决定是先迁 `react-dnd 16` hooks 主线，还是直接切到 `dnd-kit`。
  - 在 Wave 3 中补 `i18next / react-i18next` 升级评估与第一批验证。

### 2026-06-11 Wave 3 继续推进：国际化链路升级到较新稳定主线

- 本轮实际落地：
  - 升级：
    - `i18next 26.0.2`
    - `react-i18next 17.0.8`
    - `i18next-browser-languagedetector 8.2.1`
  - `frontend/src/app/hooks/useI18NPrefix.ts` 去掉了对旧版 `t.call(...)` 绑定方式的依赖，改为直接使用现代 `t(...)` 返回值并显式收口到字符串。

- 本轮验证结果：
  - `npm run checkTs` 通过。
  - `npm run build` 通过。
  - `npm run test:ci -- src/__tests__/task.test.ts src/styles/theme/__tests__/ThemeProvider.test.tsx` 通过。

- 本轮调研结论：
  - `react-i18next` 当前主线仍以 `useTranslation` hooks 形态作为现代 React 用法主路径。
  - 当前仓库国际化使用面总体健康，主要是 hooks `useTranslation`、少量 `i18next.t(...)` 直接调用，以及少量旧测试习惯；因此这条升级链路的风险显著低于 `react-dnd` 或 `Shiro` 这类跨架构专题。
  - 当前仍可继续关注的不是“是否还能升级”，而是是否要在后续专题中补类型化 key / namespace 收口，以及减少业务层直接调用全局 `i18next.t(...)` 的散点出口。

### 2026-06-11 Wave 3 继续推进：拖拽链升级到 react-dnd 16 主线

- 本轮实际落地：
  - 升级：
    - `react-dnd 16.0.1`
    - `react-dnd-html5-backend 16.0.1`
  - 把历史 HOC 时代的两个关键入口改成 hooks 主线：
    - `ChartDraggableElement.tsx`：`DragSource / DropTarget` -> `useDrag / useDrop`
    - `ChartDragLayer.tsx`：`DragLayer(...)` -> `useDragLayer(...)`
  - `DraggableItem.tsx` 与 `ThumbnailItem.tsx` 补齐了新的泛型类型参数，适配 `react-dnd 16` 的更严格 hooks 类型签名。

- 本轮验证结果：
  - `npm run checkTs` 通过。
  - `npm run build` 通过。
  - `npm run test:ci -- src/__tests__/task.test.ts src/styles/theme/__tests__/ThemeProvider.test.tsx src/app/models/__tests__/ChartSelectionManager.test.ts` 通过。

- 本轮调研结论：
  - 当前仓库拖拽链的大部分使用面原本就已经是 hooks 版，真正卡在旧生态上的只剩少量 HOC 入口；因此本轮直接升到 `react-dnd 16` 的收益大于继续停留在 `14`。
  - `dnd-kit` 依然是更现代的长期候选方案，但在当前仓库里它不再是“必须立刻切”的前置条件；现阶段先把 `react-dnd` 收到维护主线，更符合低风险持续推进策略。

### 2026-06-11 Wave 5 第一批运行时接管：JWT 请求恢复链进入 Spring Security 过滤器

- 本轮实际落地：
  - 新增 `server/src/main/java/datart/server/config/filter/JwtRequestAuthenticationFilter.java`
  - 当前请求头 `Authorization` 对应的 JWT 登录态恢复，开始通过 Spring `OncePerRequestFilter` 进入 `SecurityFilterChain`
  - `WebSecurityConfig` 已把该过滤器接入现有 Spring Security 过滤链
  - `LoginInterceptor` 不再自己负责“读取请求头 token -> 调用 `securityManager.login(token)` -> 回写响应头”这条逻辑，改为只保留：
    - `@SkipLogin` 匿名放行判断
    - 已认证状态判断
    - 请求收尾时的 `logoutCurrent()` 与 `RequestContext.clean()`

- 本轮收益：

### 2026-06-11 Wave 3 继续推进：提交流程工具链升级到当前稳定主线

- 本轮实际落地：
  - 升级：
    - `@commitlint/cli 21.0.2`
    - `@commitlint/config-conventional 21.0.2`
    - `husky 9.1.7`
    - `lint-staged 17.0.7`
  - `frontend/commitlint.config.js` 切换为 `frontend/commitlint.config.mjs`，与当前 `commitlint` ESM 主线保持一致。
  - 补齐 `frontend/.husky/commit-msg`，把提交信息校验正式接回本地提交流程。
  - `frontend/.husky/pre-commit` 改为通过 `lint-staged` 执行增量检查，不再全量跑样式命令。
  - `frontend/.husky/pre-push` 去掉旧 Husky 启动壳，避免继续依赖将在 Husky 10 失效的历史写法。

- 本轮验证结果：
  - `printf 'chore: 校验 commitlint' | npx --prefix frontend commitlint --config frontend/commitlint.config.mjs` 通过。
  - `cd frontend && npx lint-staged --debug` 通过，已确认能正确识别当前 monorepo 路径与 `package.json` 配置。
  - `cd frontend && npm run lint:css` 通过。
  - `cd frontend && npm run lint:style` 通过。
  - `cd frontend && npm run test:ci` 通过，结果为 `87 passed`, `665 passed | 4 skipped`。

- 本轮调研结论：
  - `husky 9` 已明确标记旧 `husky.sh` 启动壳为废弃写法，继续保留会在 `v10` 直接失效，因此本轮先完成 hook 文件格式收口是必要动作。
  - 当前仓库的 `core.hooksPath` 已稳定指向 `frontend/.husky`，因此受管环境下 `npm run prepare` 虽然因为 `.git/config` 写锁限制未能重写配置，但不影响现有 hooks 生效，也不影响本轮升级结果。
  - `lint-staged 17` 在当前仓库结构下可正常从 `frontend` 解析到上层 Git 根目录，因此继续保留配置在 `frontend/package.json` 是可行的。
  - “每个请求都通过 MVC 拦截器触发 Shiro 登录恢复”的模式开始松动，请求级登录态恢复首次进入 Spring Security 过滤链。
  - 这一步不改变现有权限判定语义，也不直接替换 `DatartRealm` / `ShiroSecurityManager`，因此风险显著低于直接硬切主认证体系。
  - 后续若继续推进 `Spring Security` 主链接管，可以优先沿过滤器、认证上下文和异常处理链继续扩展，而不是继续把更多逻辑堆在 `LoginInterceptor` 上。

- 本轮验证结果：
  - `mvn -pl security -am -DskipTests compile` 通过。
  - `mvn -pl server -am -DskipTests compile` 通过。

- 当前仍未完成项：
  - 权限判定仍然由 `DatartSecurityManager -> ShiroSecurityManager -> SecuritySubjectFacade` 主链承担。
  - `runAs`、角色检查、细粒度权限缓存、share 代理身份等核心语义，当前仍在 Shiro 运行时边界内。
  - 这说明本轮属于“请求入口第一批接管”，还不是“安全体系迁移完成”。

### 风险台账

这一节用于记录当前仍需重点关注的项目级风险，而不是单个 commit 的局部 warning。

| 风险编号 | 风险描述 | 影响范围 | 当前等级 | 缓解策略 | 解除条件 |
| --- | --- | --- | --- | --- | --- |
| R1 | `Shiro -> Spring Security` 改动面跨登录、权限、OAuth2、分享页 | 后端安全主链 | 高 | 继续通过 facade/assembler/cache 等中立层削耦 | Spring Security 设计稿完成并有第一批接管验证 |
| R2 | `Nashorn -> GraalJS` 可能影响脚本解析与表达式执行 | 脚本/数据执行链 | 高 | 先保持 JSR-223 发现链，再用专项测试切换 | GraalJS 第一批功能通过 |
| R3 | `Calcite` 升级可能带来 SQL 解析行为变化 | JDBC provider / SQL 解析 | 高 | 先做专题预研和回归清单 | parser / function / JDBC provider 回归通过 |
| R4 | 富文本升级可能影响编辑、只读、邮件模板多链路 | 前端富文本主链 | 高 | 继续把改动面收口到 `RichTextEditor` 和 compat 层 | Quill 路线定稿并完成第一批落地 |
| R5 | 时间体系收尾若处理不当会破坏控件值链与分享页过滤 | 前端时间控件与可视化筛选 | 中高 | 先做值链收尾，再做页面级专项回归 | DatePicker / RangePicker / 分享页回归通过 |
| R6 | 测试栈切换可能扩大现有 warning 与 mock 差异 | 前端测试链 | 中 | 先定路线，再做增量迁移，不与富文本并行 | 单栈测试路线落地 |
| R7 | Docker 尚未镜像级实测 | 发布与交付 | 中 | 保持安装包链稳定，后续补 docker build/run | 完成容器级 build/run 验证 |

### 风险台账使用规则

- `高`：不能与其它高风险专题并行
- `中高`：可以和低风险主题并行，但必须单独提交
- `中`：可以和专项文档、回归收尾并行
- 风险一旦解除，必须同步更新：
  - 风险台账
  - 技术栈总表
  - 遗留栈退出清单

## 全量升级路线图

这一节把“整个技术栈都走向现代替代方案”拆成可执行的阶段序列。顺序的核心原则是：先收口运行时基线，再收口前端 UI 与时间体系，再处理插件型依赖，最后进入后端架构专项。

### Phase A：工程化基线收口

目标：让当前已完成的大版本升级具备统一、可复现、可验收的工程化证明。

- 收口内容
  - CI Node 版本统一到 26。
  - CI 纳入 `npm run checkTs`、`npm run build:task`、`npm run build`、`mvn -pl server -am -DskipTests compile`、关键模块测试。
  - Docker / 发布包 / Maven `package` 链闭环到同一产物生成路径。
- 完成定义
  - 本地、CI、Docker 使用相同的 Node / Java / Maven 最低版本约束。
  - 发布包和 Docker 都不依赖手工准备的静态目录。
  - `/api/v1/sys/info`、登录页、至少一个 share 入口具备稳定验收路径。

当前状态补充：

- 已完成
  - `.github/workflows/dev-ut-stage.js.yml` 已统一到 Node `26.x`。
  - CI 已纳入前端 `npm ci`、`checkTs`、`build:task`、`build`、`test:ci`、`lint:css`、`lint:style`。
  - CI 已纳入后端 `mvn -pl server -am -DskipTests package` 与 JDBC provider 定向测试：
    `mvn -pl data-providers/jdbc-data-provider -am -Dtest=datart.data.provider.sql.SqlScriptRenderTest,datart.data.provider.function.SqlFunctionValidateTest -Dsurefire.failIfNoSpecifiedTests=false test`。
  - 仓库已新增统一健康检查脚本 `scripts/check-demo-health.sh`，直接消费 `mvn package` 产出的安装包，使用 demo profile 启动 `datart.DatartServerApplication` 并轮询 `GET /api/v1/sys/info`。
  - CI 已接入 `bash ./scripts/check-demo-health.sh`，把“编译通过”扩展到“服务可启动且系统信息接口可返回成功”。
  - Workflow 已显式声明 Temurin JDK `21`，并启用 npm / Maven 缓存。
  - `Dockerfile` 已改为直接消费 `mvn package` 产出的 `datart-server-*-install.zip`，不再手工复制 `bin/`、`config/`、`lib/`、`static/` 目录。
  - `Dockerfile` 启动参数已与当前 JDK 21 运行事实对齐，补入 `--add-opens=java.base/java.lang=ALL-UNNAMED`。
- 仍未完成
  - `mvn test -pl data-providers/jdbc-data-provider -am` 这种全量上游联动测试当前还不适合作为 CI 默认命令；本机复核显示它会先触发 `core` 的 `POIUtilsTest`，并在当前 JDK 21 环境下出现 surefire fork crash，需要后续单独收口该测试稳定性。
  - 当前环境未提供 Docker CLI，尚未做镜像级构建实测；现阶段证据来自安装包结构、Dockerfile 静态复核和安装包健康检查链路。

### Phase B：Ant Design 5 主升级

目标：把前端主 UI 栈从 Ant Design 4 彻底迁到当前主线，为后续 `moment` 退出和主题系统现代化扫清阻塞。

- 收口内容
  - 继续清理 `visible -> open`、`Menu children -> items`、`Dropdown overlay -> menu`、`Tabs.TabPane`、`Collapse.Panel`、`TreeSelect.TreeNode` 等旧 API。
  - 完成 `antd` 与 `@ant-design/icons` 升级。
  - 将主题链从历史 `ConfigProvider.config({ theme })` 过渡逻辑收敛到组件级 token / theme 配置。
- 完成定义
  - 主应用、分享页、仪表板编辑器、故事板、视图页、组织管理页在 AntD 5 下可运行。
  - 不再新增基于 AntD 4 legacy API 的兼容壳。
  - 文档中的 AntD backlog 从“升级前清障”转为“剩余人工回归项”。

当前状态补充：

- 已完成受控试升并通过回归：
  - `antd 4.24.x -> 5.26.2`
  - `@ant-design/icons -> 5.6.1`
  - `@ant-design/pro-table -> @ant-design/pro-components 2.8.10`
  - `@antv/s2 -> 2.7.1`
  - `@antv/s2-react -> 2.3.1`
  - AntD 样式入口已从 `antd.variable.min.css` 切到 `antd/dist/reset.css`
  - `npm run checkTs`、`npm run build`、`npm run lint:style` 已再次通过
- 本轮试升中额外完成的真实兼容专题：
  - `EditableProTable` 只读数组签名适配
  - `Button type="ghost"` 等已废弃属性收口
  - `message.warn -> message.warning`
  - `Transfer` / `Checkbox.Group` / `Slider` / `DatePicker` 类型收紧适配
  - `@antv/s2` 2.x tooltip、totals、interaction、palette 与事件 prop 映射
- 因此本阶段状态应从“升级前清障”切换为“主升级已进入稳定化阶段”，后续重点不再是 AntD 4 旧 API 搜索清零，而是：
  1. 继续做定向页面回归；
  2. 清理受控试升期间留下的 compat 壳；
  3. 推进时间体系和富文本等与 AntD 5 联动的后续专题。

- 已继续收口并通过回归验证的前置清障项包括：
  - `visible -> open`、`onVisibleChange/onDropdownVisibleChange -> onOpenChange`
  - `Collapse.Panel -> items`
  - `Modal destroyOnClose -> destroyOnHidden`
  - `Dropdown dropdownRender -> popupRender`
  - `Dropdown destroyPopupOnHide -> destroyOnHidden`
  - `Select dropdownMatchSelectWidth -> popupMatchSelectWidth`
- 以上改动最初通过局部 compat props 壳承接；当前既然 `antd 5.26.2` 已落地，这些壳的角色也从“升级前开路”转成“升级后稳定化缓冲层”。
- 当前仍值得继续清理的残留主要集中在：
  - 少量 `TreeSelect` 旧 popup props 与样式挂载点；
  - `SetFieldType` 里 submenu 级 `popupClassName` 这类菜单细节；
  - `Popup` / `Popover` / `Dropdown` 上为了过渡保留的 compat props 壳。
- 因此下一阶段最合理的动作不再是继续为 AntD 5 开路，而是：
  1. 以当前 `antd 5` 基线为前提，推进 `moment -> dayjs`；
  2. 对富文本、控制器、分享页和图表时间筛选器做 AntD 5 下的定向回归；
  3. 按批次删除已经只为过渡存在的 compat props 壳。

### Phase C：时间体系现代化

目标：退出维护模式的 `moment`，统一到 AntD 5 兼容、按需引入、后续可持续升级的时间栈。

当前阶段补充：

- 仓库已新增统一时间适配入口 `frontend/src/app/utils/date.ts`，开始收口 `dayjs` 插件与 locale 初始化。
- `frontend/src/locales/i18n.ts` 的时间 locale 初始化已从 `moment.locale(...)` 切到 `dayjs` 适配层。
- `frontend/src/app/utils/time.ts`、`frontend/src/app/utils/chartHelper.ts` 以及部分只依赖“当前时间格式化”的调用点，已开始从 `moment` 迁到 `dayjs`。
- `frontend/src/app/pages/MainPage/pages/VizPage/ChartPreview/components/ControllerPanel/components/TimeFilter.tsx` 已不再把 Dayjs 值直接 `toString()` 回写到过滤条件，而是统一按 `TIME_FORMATTER` 序列化，避免不同运行环境下字符串语义漂移。
- `frontend/src/app/pages/DashBoardPage/pages/BoardEditor/components/ControllerWidgetPanel/utils.ts` 中仍带 `Moment` 命名的控制器预处理函数已改为 `formatControlDateToDayjs`，并同步收口相关局部命名，避免把历史语义继续扩散到新的时间链路中。
- `frontend/src/app/pages/MainPage/pages/VariablePage/DefaultValue.tsx` 的日期默认值录入控件已跟随变量自身 `dateFormat` 切换格式与 `showTime` 行为，避免“展示格式是日期、录入控件却强制日期时间”的边界不一致。
- `frontend/src/app/pages/MainPage/pages/VariablePage` 相关类型已开始把默认值与权限值从宽泛 `any[]` 收紧到显式的 `VariableDefaultValueItem` 联合类型，降低后续时间值链回归时再次混入历史 `Moment` 语义的概率。
- 2026-06-11 最新推进：`CurrentRangeTime`、`ExactTimeSelector`、`RangeTimePickerFilter`、`TimeFilter` 与分享链接过期时间选择器已统一按 Dayjs 值对象回填，并显式对齐 `TIME_FORMATTER` / `showTime`；`ManualRangeTimeSelector` 与图表预览 `RangeTimeFilter` 同时修正了 range 状态原地修改，避免控件回显与条件回写不同步。
- 同日补证据：本轮已通过 `npm run checkTs`、`npm run build`、`npm test -- --runTestsByPath src/app/pages/DashBoardPage/utils/__tests__/index.test.tsx --runInBand --silent`，说明时间值链收口没有打断前端类型检查、生产构建和控制器相关定向测试。
- 2026-06-11 运行态补证据继续推进后，已确认：
  - 安装包 demo 以 `http://127.0.0.1:8081` 启动并可登录 `demo / 123456`。
  - 主应用初始化链路可稳定进入 `/organizations/f8435e0a3323459aaef679ab63fbd01a/vizs`，不再出现此前的 `download/tasks`、`custom charts` 静态资源兜底错误。
  - 变量页新增弹窗已能把“值类型”切到“日期”，并真实渲染“请选择日期”和日期格式选择控件。
  - 调度页 `/schedules/add` 已恢复编辑页渲染，`有效时间范围` 的开始/结束日期输入框与 `showTime` 范围控件已能真实显示。
- 同日还顺手收口了一处被运行态回归带出的前端兼容路由残留：`SchedulePage` 现在通过局部 `useScheduleRouteParams` 从当前 URL 提取 `orgId/scheduleId`，避免 `/schedules/add` 只切 URL 不渲染编辑表单。
- 这一步仍然刻意不碰 DatePicker / RangePicker 值类型和表单状态，以便把时间工具层与控件层的风险拆开治理。

迁移边界与批次策略：

- 第一批：优先迁移纯格式化、比较、序列化出口，以及只剩 `moment` 类型 import 的轻残留。这一批允许直接收口到自有 `dayjs` 适配层或项目内显式类型。
- 第二批：时间筛选器、控制器配置和请求参数构造这类“值会落库或进接口”的链路，先迁字符串出口与内部比较逻辑，再保留 AntD 4 仍需要的 `Moment` 值对象回显能力。
- 第三批：DatePicker / RangePicker 的值对象链、表单默认值、`Moment` 类型签名和 locale 联动，在当前 AntD 5 基线上统一切到 `dayjs` / 字符串边界。
- 批次约束：每一批都必须附带专项回归，重点覆盖分享页、变量页、调度页、仪表板控制器、图表时间筛选器与请求参数构造，避免“看似清了 import，实际破坏控件值链”的假收口。

- 目标替代方案
  - 默认目标：`moment -> dayjs`
  - 仅在出现复杂不可兼容场景时，再评估局部保留或改用 `date-fns`
- 收口内容
  - 时间格式化工具、筛选器默认值、日期控件、locale 和相对时间计算统一到自有时间适配层。
  - 清理 `moment` 直连调用，避免把时间计算散落在组件中。
- 当前证据
  - `frontend/src` 生产代码中的直接 `moment` import / `moment(...)` 调用已检索清零。
  - `frontend/package.json` 中的直接 `moment` 依赖已移除，`npm ls moment` 当前为空。
  - 当前剩余问题主要集中在两类：
    1. `rc-picker` 在 lockfile 中仍保留可选 peer 声明；
    2. `task` 独立打包链已完成复核：`rollup-plugin-cleanup` 在 Node 26 下会让 `build:task` 长时间挂起并留下旧产物；移除后 `frontend/public/task/index.js` 已重新生成并切到 `dayjs`。
    3. 控件值链仍需继续做页面级回归，尤其是分享页、变量页和控制器配置中 DatePicker / RangePicker 的默认值与回显边界。
- 完成定义
  - 前端生产代码检索 `moment` 结果清零，且项目自身依赖声明退出。
  - `task` 独立打包链重新生成后的产物与当前源码一致，不再携带旧 `moment` bundle。
  - lockfile 中若仍存在第三方可选 peer 声明，需要在文档中明确标注为上游约束，而不是项目自身直连依赖。
  - 时间筛选、控制器、分享页、故事板、图表请求参数构造具备专项回归验证。
  - 当前仍缺的主要证据收敛为：至少一个 share 入口的真实 L2/L3 运行态回归。

### Phase D：富文本与媒体插件链现代化

目标：替换仍停留在较老生态的前端插件型依赖，降低 React 18+ 长期兼容风险。

当前阶段补充：

- `video-react 0.15.0` 已从运行时代码与前端依赖清单中移除。
- `frontend/src/app/pages/DashBoardPage/components/Widgets/VideoWidget/VideoWidgetCore.tsx` 已改为原生 `<video>` + 轻量样式适配，继续保留现有视频组件的输入配置结构与仪表板嵌入行为。
- 这一项优先收口了使用面极小、但明显停留在旧 React 包装生态的媒体依赖，为后续把注意力集中到 `react-quill` 与 `reveal.js` 这类更重的升级专题腾出空间。

- 富文本
  - 当前状态：`react-quill 1.3.5`，仓库内 `react-quill` / `Quill` 使用面约 `110` 处。
  - 路线修正：`react-quill 2.0.0` 官方包本身仍依赖 `quill ^1.3.7`，它只是较新的 React 包装层，并不等于已经切到 Quill 2。
  - 目标方案：先评估是否以 `react-quill 2.x` 作为中间态，清理旧包装层 API；若目标是进入真正的 Quill 2 主线，则需要继续评估 Quill 2 的 React 封装，或基于官方 React playground 形态维护仓库内自有轻量适配层。
  - 执行策略：先抽出自定义 blot、颜色面板、只读适配层，再升级编辑器内核。
  - 2026-06-10 最新推进：`RichTextEditorHandle` 已继续收口 markdown 模块构造与 calc field 插入能力；仪表板富文本组件中的重复 `selection-change` 监听已移除，统一复用 `QuillPalette` 处理颜色同步，并补上 markdown 模块的销毁生命周期。随后又把 `getEditor()` / `getModule()` 从公开 handle 中回收，确认业务层不再直接依赖底层 Quill 实例细节。
  - 同日继续推进：`RichTextPluginLoader/index.ts` 已从历史编译态的 `//@ts-nocheck + Babel helper` 形式重写回项目可维护的 TypeScript 模块；`CalcFieldBlot` / `TagBlot` 也同步清理了 `innerHTML +=`、空 click 监听和宽松 dataset 写入等历史 DOM 写法。这样后续评估 `react-quill 2.x` / Quill 2 时，富文本扩展层的噪音会更小，兼容问题也更容易定位。
  - 同日继续推进：`imageDrop`、`tag`、`calcfield` 与字体/字号 attributor 的 `Quill.register(...)` 入口已经收敛到 `RichTextPluginLoader` 单点，`ChartRichTextAdapter`、`RichTextWidgetCore`、`CommonRichText` 不再各自重复注册。这一步继续压缩真正升级 Quill 2 时的改动面和注册时机分歧。
- 视频
  - 当前状态：`video-react` 使用面约 `3` 处。
  - 目标方案：优先迁到原生 `<video>` + 轻量控制层，避免继续绑定老 React 包装库。
- 故事播放
  - 当前状态：`reveal.js` 使用面约 `16` 处。
  - 目标方案：短中期先升级到较新稳定线；当前已从 `4.1.x` 提升到 `6.0.1`。长期若故事板形态继续重构，再评估 React 原生故事/演示方案。
- 完成定义
  - 富文本编辑、只读渲染、仪表板富文本组件、故事播放、视频播放均脱离明显陈旧或低活跃度依赖。
  - 相关功能有定向 smoke test 或人工检查表。

### Phase D.5：前端平台原生能力收口

目标：优先用浏览器和 React 18 已具备的原生能力，替换“小而旧、维护价值低”的包装依赖，减少升级噪音和兼容面。

- 当前阶段补充
  - `react-resize-detector` 已退出，统一切到仓库自有 `useResizeObserver`，底层直接使用浏览器原生 `ResizeObserver`，并保留 `window.resize` fallback。
  - `react-color` 已退出，颜色选择器统一收口为项目内自有轻量实现，继续保留现有弹层交互与颜色字符串输出语义。
  - 这一批不改调用方 API，只替换底层实现，确保布局测量、看板尺寸计算、分享页预览和资源树高度计算继续按原接口工作。
- 下一批优先候选
  - `react-window` 保留观望；若现有虚拟表格性能与 API 满足需求，可继续保留，不强制为“为了新而新”迁移
- 完成定义
  - 这类依赖要么退出，要么被明确标注为“活跃维护且没有更高价值的迁移收益”。
  - 自有 hook / 适配层优先收敛公共能力，避免再次把第三方 API 扩散到业务组件。

## 老旧技术栈审计与替代建议

这一节作为“全项目 review 结果”持续维护。每一项都明确当前状态、现代替代方向和迁移优先级，避免只凭印象推进升级。

| 类别 | 当前栈 | 当前状态 | 现代化替代方案 | 优先级 |
| --- | --- | --- | --- | --- |
| 前端 UI 基座 | `antd 5.26.x` | 已进入当前稳定主线，仍需做页面级回归与 compat 壳清理 | 保持 5.x 稳定线，暂不追 6.x | 已完成主升级，持续稳定化 |
| 时间体系 | `moment` | 生产代码直连调用已清零，项目直接依赖已退出；任务页产物也已切到 `dayjs`，剩余仅是上游可选 peer 声明与少量值链回归 | 全面迁到 `dayjs`，仅在控件值类型阶段做局部适配 | 高 |
| 富文本 | `react-quill 1.3.5` + 本地 markdown / image-drop 模块 | `react-quill` 本体仍偏旧，但 markdown 与图片拖拽已从低活跃度外部插件收回到仓库内维护；且 `react-quill 2.x` 仍基于 Quill 1.3.x | 先评估 `react-quill 2.x` 是否值得作为中间态，再决定是否切到真正的 Quill 2 React 封装或自有适配层 | 高 |
| 代码编辑器 | `react-monaco-editor 0.59.0` | 已退出；改为仓库自有 Monaco React 适配层 | 直接基于 `monaco-editor` 维护自有适配组件 | 已完成 |
| 故事播放 | `reveal.js 6.0.1` | 已在较新稳定线，短期无需继续追大版本 | 保持当前主线，长期再评估 React 原生故事方案 | 低 |
| 颜色选择器 | 自有轻量实现 | 已退出第三方重包装依赖 | 保持自有实现 | 已完成 |
| 尺寸监听 | `react-resize-detector` | 已退出 | 原生 `ResizeObserver` + 自有 hook | 已完成 |
| 视频播放 | `video-react` | 已退出 | 原生 `<video>` | 已完成 |
| 看板历史记录 | `redux-undo` | 仍在使用，但功能面集中 | 可保留；若继续现代化，可评估 RTK reducer 历史层 | 中低 |
| 网格布局 | `react-grid-layout` + `flexlayout-react` | `react-grid-layout` 是核心依赖；`flexlayout-react` 只在图表工作台局部使用 | 暂保留；出现 React 18/AntD5 兼容问题再专项治理 | 中低 |
| 虚拟列表 | `react-window` | 使用面仅约 `2` 处，运行风险可控 | 若性能与 API 满足需求可保留 | 低 |
| 测试栈 | `Vitest 4 + jsdom 29` | 已完成单栈主链收口；当前问题主要是历史组件 warning 与 Vite peer 提示，不再是 Jest 运行模型 | 保持 `Vitest` 单栈，后续只做 warning 治理与 `Vite 6+` 对齐评估 | 已完成主链收口 |
| task 独立打包链 | `Vite 5 library mode` | 已退出独立 `Rollup 2` 主工作流；`build:task` 直接产出 UMD `build/task/index.js`，并同步回写 `public/task/index.js` 供前端静态资源与后端 parser 重命名链复用 | 保持 Vite 单栈；后续只清理残留 Rollup 依赖声明 | 已完成主链切换，持续清理 |
| 代码规范链 | `ESLint 9` + `stylelint 17` + `Prettier 3` | 已进入 Node 24 LTS 可运行的较新稳定主线，并补 Node 当前线兼容验证；当前剩余主要是 Prettier 历史格式差异与无效 lint 注释清理 | 已完成 ESLint 9 flat config、stylelint 17、Prettier 3、提交流程工具链与 Vitest 单栈收口；下一步按触达文件渐进消化格式差异 | 中 |
| 国际化 | `i18next 19` + `react-i18next 11` | 可运行，但版本偏旧 | 评估进入当前稳定主线，并结合 React 18 Suspense/类型签名复核 | 中低 |
| 后端安全 | `Shiro 2` | 明显历史架构包袱，仓库使用面约 `71` 处 | `Spring Security` | 高，但单独专项 |
| 后端连接池 | `Druid` | 历史包袱，且偏监控导向，仓库使用面约 `21` 处 | `HikariCP` | 高，但单独专项 |
| 后端脚本引擎 | `Nashorn` | 已退场技术，当前直接使用面约 `8` 处 | `GraalJS` | 中高，单独专项 |
| 浏览器自动化 | `PhantomJS` | 已退出主链 | `Selenium 4` / `Playwright` | 已完成主链退出 |
| SQL 解析内核 | `Apache Calcite 1.26.0` | 使用面极深，仓库命中约 `1000+` 处 | 升级到较新稳定线，但必须单独治理 parser / JDBC provider | 高，但单独专项 |
| 样式系统 | `styled-components 6.1.19` | 已进入 v6 主线 | 保持当前主线；关注 `shouldForwardProp` / iframe target / SSR 行为 | 已完成主升级，持续稳定化 |

### Phase E：后端架构专项现代化

目标：把仍能运行、但已经属于历史架构包袱的后端基础设施列为专项退出对象。

- 安全专项：`Shiro -> Spring Security`
  - 当前证据：仓库内 `shiro` 使用面约 `71` 处，真实改动面已从权限检查、Realm、Subject 获取、缓存到服务层权限字符串拼装。
  - 目标方案：统一认证、鉴权、remember-me、OAuth2 登录和过滤器链到 Spring Security 体系。
  - 2026-06-11 最新推进：新增中立权限字符串编解码层 `datart.security.manager.PermissionStringCodec`，把角色字符串、权限位展开和权限字符串拼装从 `ShiroSecurityManager` 静态工具方法中抽离；`server` 服务层不再直接依赖 `security.manager.shiro.*`，`DatartRealm` 也已改为复用中立编解码层。
  - 2026-06-11 第二轮推进：`PermissionDataCache`、`RequestScopePermissionDataCache`、`ThreadScopePermissionDataCache` 不再以 `SimpleAuthorizationInfo` / `SimpleAuthenticationInfo` 作为通用缓存接口，而是改为中立的 `AuthorizationCache` / `AuthenticationCache`；Shiro 认证对象现在只在 `DatartRealm` 内部做适配转换。
  - 2026-06-11 第三轮推进：新增 `SecuritySubjectFacade` 抽象和 `ShiroSubjectFacade` 适配实现，把 `SecurityUtils.getSubject()`、`ThreadContext.unbindSubject()`、登录 token 构造、权限校验和当前主体获取等 Shiro 运行时 API 从 `ShiroSecurityManager` 主流程里收口出去。
  - 2026-06-11 第四轮推进：新增 `AuthenticationTokenAdapter` 抽象和 `ShiroAuthenticationTokenAdapter` 实现，把“从认证 token 解析用户名”和“密码/JWT 有效性校验”从 `DatartRealm` 与 `PasswordCredentialsMatcher` 中抽离；realm 和 matcher 现在只承担 Shiro 桥接职责。
  - 2026-06-11 第五轮推进：新增 `AuthenticationAssembler` / `AuthorizationAssembler` 及其 Shiro 适配实现，把用户查询、组织 owner 隐式权限和资源权限装配从 `DatartRealm` 中抽离；realm 现在只保留 principal 提取、缓存协同与 Shiro 对象转换。
  - 本轮收益：后续即使继续保留 Shiro 运行时，也可以把 `server -> shiro implementation` 这条静态依赖先拆掉，缩小未来迁移到 Spring Security 时需要一起改动的横切面。
  - 前提：JWT、OAuth 客户端、分享页认证链已经稳定。
- 数据源专项：`Druid -> HikariCP`
  - 当前证据：原始使用面主要集中在 server 主数据源和 JDBC provider 工厂，属于“使用面不大、耦合度不低”的典型专项。
  - 目标方案：服务端主链统一池化到 HikariCP，仅在确有必要的 provider 子域保留局部适配层。
  - 2026-06-10 最新推进：`server/pom.xml` 已移除 `druid-spring-boot-3-starter`，`application-demo.yml` 与 `config/profiles/application-config.yml` 均不再显式指定 `com.alibaba.druid.pool.DruidDataSource`；`datart-jdbc-data-provider` 已把 `DataSourceFactoryDruidImpl` 替换为 `DataSourceFactoryHikariImpl`，并切换到 `HikariCP` 依赖。
  - 最新验收证据：
    - `mvn -pl data-providers/jdbc-data-provider -am -DskipTests compile`
    - `mvn -pl server -am -DskipTests compile`
    - `mvn -pl server -am -DskipTests package`
    - `bash ./scripts/check-demo-health.sh`
    编译、打包和基于安装包的 demo 健康检查均已通过，证明当前迁移没有打断 JDBC provider、server 主运行时构建和 demo 启动链。
  - 下一步重点：继续补充多数据源参数语义、连接池监控诉求和 provider 级行为回归，而不是停留在主链可启动性验证。
- 脚本专项：`Nashorn -> GraalJS`
  - 当前证据：脚本引擎相关使用面约 `8` 处，当前核心实现集中在 [JavascriptUtils.java](/Users/chencongyu/WorkHome/VSProjects/open-project/datart/core/src/main/java/datart/core/common/JavascriptUtils.java:1)。
  - 目标方案：先建立自有脚本执行边界，再替换底层引擎实现。
  - 前提：明确脚本执行的输入输出模型、性能约束和安全边界。
- 代码生成专项：`mybatis-generator-core`
  - 当前证据：生成链相关使用面约 `14` 处。
  - 目标方案：升级到较新生成器版本，或迁出主运行时模块到独立 profile / 工具模块。
- Calcite 专项
  - 当前证据：仓库内 `calcite` / `SqlParser` / `SqlNode` 相关命中约 `1008` 处，主要集中在 `data-providers/data-provider-base` 的 parser、自定义 SQL 方言和 JDBC provider。
  - 目标方案：升级到较新稳定线，但必须以 SQL 解析、函数、JDBC provider 兼容回归为前提。
- 完成定义
  - 上述架构专题至少完成专项设计、目标选型、迁移顺序与验收策略，且不再作为“未知技术债”悬空。

## 最终收官标准

只有同时满足下面条件，才能宣称“整个技术栈清单已经现代化替代完成”：

1. 当前基线项
   - JDK 21、Spring Boot 3、Spring Cloud 2025、Node 24 LTS 默认基线与 Node 26 兼容验证、Vite 6 稳定线、React 18、Router 6、RTK 2、React Redux 9 均保持通过构建和运行验证。
2. 前端主栈项
   - Ant Design 5 完成主升级并稳定运行。
   - `moment` 退出主生产代码。
   - 富文本、视频、故事播放三类老插件链完成升级或替代。
3. 后端主运行时项
   - Jackson、JWT、HttpClient、POI、H2、Commons/Guava 主链均已收口到现代替代方案。
   - 不再存在生产代码对 `fastjson`、`commons-lang 2`、`commons-io 1.x`、旧 JWT 双栈等历史依赖的直接使用。
4. 架构专项项
   - `Shiro`、`Druid`、`Nashorn`、`mybatis-generator-core`、`Calcite` 至少要么完成迁移，要么有经验证可接受的保留结论与明确退出计划。
5. 工程化项
   - CI、Docker、发布包、本地开发使用同一套构建产物链。
   - 关键接口、关键页面、分享入口、导出链路具备稳定验收证据。

如果第 4 项仍未完成，则项目应表述为“主技术栈现代化已完成，后端架构专项仍在进行中”，不能直接宣称“整个技术栈已经全部现代化”。

## 项目总控路线图

这一节定义整个大型迁移项目的控制方式，确保后续每个 checkpoint 都在收敛到同一个终态，而不是零散升级。

### 里程碑状态

- `M0 已完成`：JDK 21、Spring Boot 3、Spring Cloud 2025、Node 默认基线收口到 24 LTS 并补 26 兼容验证、Vite 6 稳定线、React 18、Router 6 依赖切换、RTK 2、React Redux 9、Selenium 4、后端生产代码 `fastjson` 清零。
- `M1 已完成`：前端 Router 兼容层收口、Ant Design 5 主升级、测试工具链去 CRA 化、CI/Docker 与当前基线对齐。
- `M2 已完成`：JWT 升级、HttpClient 5、POI/Guava/Commons 系列老基础库清理。
- `M3 进行中`：`moment -> dayjs` 收尾、富文本现代化，以及 H2 2.x 之后的连接池、安全框架、脚本引擎与 Calcite 长期演进策略落地。

## 下一批建议执行顺序

结合当前真实基线、依赖使用面和回归成本，下一批升级不建议继续追 Router 7 或 AntD 6，而应按下面顺序推进：

1. `moment -> dayjs`
   - 原因：源码、直接依赖和任务页产物都已经收口，剩余工作主要是继续压缩值对象链与回归边界。
   - 收益：可以把时间体系迁移从“主要链路完成”推进到“专题级收尾”。
   - 风险：DatePicker / RangePicker、控制器配置、分享页和变量页仍需要持续定向回归。

2. 富文本内核现代化
   - 对象：`react-quill 1.3.5` 与当前本地富文本扩展层
   - 原因：使用面约 `110` 处，属于前端剩余最深的旧生态依赖之一。
   - 策略：先拆自定义 blot / toolbar / 只读渲染适配层，并把 markdown、图片拖拽等外部旧插件替换为仓库内模块，再评估 `react-quill 2.x` 或 Quill 2 原生封装。

3. `styled-components` 稳定化复核
   - 当前已在 `6.1.19`，不需要再做版本升级。
   - 但需要检查：
     - 是否存在自定义 props 透传到 DOM 的告警；
     - iframe / popup / embed 场景是否需要统一 `StyleSheetManager target`；
     - 是否需要显式 `shouldForwardProp` 策略。

4. 后端专项预研与分拆
   - 先不要直接同时动 `Shiro`、`Druid`、`Nashorn`、`Calcite`。
   - 应拆成四个独立专题，并先补设计和验证计划：
     - `Shiro -> Spring Security`
     - `Druid -> HikariCP`
     - `Nashorn -> GraalJS`
     - `Calcite 1.26 -> 较新稳定线`

5. Router 7 和 AntD 6 暂缓
   - `React Router 6.30.x` 当前已经在可接受主线。
   - `AntD 5.26.x` 刚进入稳定化阶段。
   - 两者现在继续追大版本的收益都低于时间体系和富文本现代化。

### 每个 checkpoint 的强制约束

- 一个 checkpoint 只解决一个主题，禁止把前端大版本、后端基础设施和构建链变更混在同一提交。
- 每个 checkpoint 必须同时更新本文档中的“当前状态”“风险”和“验收证据”。
- 每个 checkpoint 都要保留可回退点，优先通过兼容层、局部替换和双写期收口，而不是一次硬切。
- 所有 checkpoint 都在独立专题分支上串行推进，并保留中文 commit 作为阶段锚点；`main` 只保留 merge 结果。

### 统一验收与回退策略

- 前端 checkpoint 至少验证：`npm run checkTs`、`npm run build`；涉及 task 解析链时额外跑 `npm run build:task`。
- 后端 checkpoint 至少验证：`mvn -pl server -am -DskipTests compile`；涉及 JDBC/SQL 解析链时额外跑 `mvn test -pl data-providers/jdbc-data-provider -am`。
- 集成 checkpoint 至少验证：
  - `mvn -DskipTests -Dexec.skip=false package -pl server -am`
  - `GET /api/v1/sys/info` 返回 200
  - 首页和至少一个 share 入口可访问
- 回退策略统一遵循：
  - 先回退当前 checkpoint 提交
  - 再恢复上一个已验证 tag/commit 的构建路径
  - 不通过“保留半迁移状态”作为长期运行方案

## 工程化差距与收敛计划

这一节专门处理“本机已跑通，但工程化链路还没跟上”的问题。目标不是额外扩展范围，而是确保最终现代化结论在 CI、Docker 和发布包上都成立。

### 当前差距

1. CI 与安装包健康检查已基本对齐真实基线
   - 当前 `.github/workflows/dev-ut-stage.js.yml` 已统一到 `node-version: [24.x, 26.x]`，并显式声明 Temurin JDK 21。
   - workflow 已覆盖前端 `checkTs`、`build:task`、`build`、`test:ci`、stylelint，后端 `mvn -pl server -am -DskipTests package`、JDBC provider 定向测试，以及启动后的 `/api/v1/sys/info` 健康检查。

2. JDBC provider 全量上游联动测试仍待专项收口
   - `mvn test -pl data-providers/jdbc-data-provider -am` 当前会先触发 `core` 的 `POIUtilsTest`，并在本机 JDK 21 环境下出现 surefire fork crash。
   - 当前已先用定向测试保证 JDBC / SQL 主链验证，但这还不是最终形态。

3. Docker 与发布包已切到同一套产物链，但镜像级实测还缺一环
   - `Dockerfile` 已改为直接消费 `mvn package` 生成的安装包，而不是额外手工准备目录。
   - 运行参数也已与当前 JDK 21 真实启动要求对齐。
   - 但当前环境没有 Docker CLI，尚未完成镜像 build/run 的最终实测。

### 收敛策略

1. CI 先对齐当前真实基线
   - Node 版本统一到 26。
   - 增加 `npm run checkTs`、`npm run build:task`、`npm run build`。
   - 增加后端 compile / test / package 最小主链验证。

2. 再补集成启动验证
   - 以 demo 或最小配置启动 server。
   - 验证 `/api/v1/sys/info` 返回 200。
   - 必要时增加一个 share 入口或静态资源检查。

3. 最后闭环 Docker / 发布包一致性
   - 已明确发布包由 `mvn -pl server -am -DskipTests package` 生成。
   - 已让 Docker 镜像消费同一套安装包产物。
   - 剩余动作是补镜像级 build/run 实测，验证容器启动参数与安装包启动参数完全一致。

## 升级依赖矩阵

这一节定义哪些升级可以并行，哪些必须按顺序推进，避免后续把多个高风险改动混在一个阶段里。

- React Router 6/7 依赖于：
  - `useHistory`、`Redirect`、`Switch`、`Route component/render` 基本清空
  - 主路由容器先迁到兼容层
- Ant Design 5 依赖于：
  - React 18 已完成
  - Dropdown/Menu/Modal/Popover 等旧 API 基本收口
  - 主题链已切到 CSS variables / token 思路
- 测试栈从 CRA/Enzyme 迁出依赖于：
  - React Router 与 AntD 大面积结构改动先稳定
  - Vite 主构建链可长期作为唯一入口
- 后端 Jackson 单栈化依赖于：
  - 先识别 `fastjson` 在 DTO、配置校验、HTTP message converter、数据导入导出中的边界
  - 先把自动化/截图链与 JSON 栈解耦，避免多种基础设施同时变动
- JWT 单栈化依赖于：
  - 先确认现有 token 生成、刷新、校验和第三方登录链路
  - 最好在 JSON 栈趋稳之后推进
- PhantomJS / Selenium 3 迁移依赖于：
  - 明确当前截图、导出、分享缩略图等所有实际调用点
  - 明确服务器运行环境是否允许安装 Chromium 及其依赖
- H2 2.x 或 Testcontainers 依赖于：
  - demo 数据、初始化 SQL、兼容模式和文档同步调整
  - 最好放在后端核心库升级后期

## 执行原则

- 每次提交只解决一个明确升级主题。
- 每个阶段先做“兼容层收口”，再做真正的大版本切换。
- 尽量先消除重复实现，再做库替换。
- 优先替换已经停止维护、明显过时或高安全风险的依赖。
- 高风险基础设施变更必须带验证路径，不能只升级版本号。
- 文档中的“最终技术栈清单”是完成定义，阶段成果只是通往终态的中间站。

## 官方依据

- React 官方已宣布 Create React App 退场，新项目应使用框架或现代构建工具；本项目需要从 CRA 迁出。
- Vite 是现代前端构建工具，适合作为 CRA/CRACO 的替代方向。
- Ant Design 6 进入新主线，要求 React 18+，因此 UI 大版本升级必须排在 React 升级之后。
- Selenium 官方提供 Selenium 4 迁移路径；当前 Selenium 3 和 PhantomJS driver 属于老旧浏览器自动化组合。
- PhantomJS 官方项目已长期停止活跃开发，不应作为现代化目标的一部分。

## 分阶段升级路线

### 阶段 0：基线稳定与版本治理

目标：让项目有明确、可重复的构建和验收入口。

已完成：
- JDK 21 迁移。
- Spring Boot 3.5.12 迁移与运行期兼容修复。
- 第一批低风险依赖和插件升级。
- Node 26 下前端开发启动兼容。
- `.npmrc` registry 配置已兼容 npm 11。
- Maven Enforcer 已声明 Java 21+ 与 Maven 3.9+。
- 前端构建产物复制已后移到 `package` 阶段，后端 Java 编译不再被 `frontend/build/task/index.js` 缺失阻断。

待完成：
- 将前端完整构建纳入发布包验收，确保 `package` 阶段生成静态资源和 parser task 产物。

验收门槛：
- `mvn -DskipTests -Dexec.skip=true -Dmaven.resources.skip=true compile -pl server -am` 的 Java 编译阶段通过。
- `npm run checkTs` 通过。
- 前后端本地服务均可访问。

### 阶段 1：前端构建链短期稳定

目标：在不改业务代码的前提下消除 CRA4/Webpack4 对 Node 26 的明显摩擦。

已完成：
- `react-scripts 4.0.3 -> 5.0.1`。
- `@craco/craco 6 -> 7.1.0`。
- Webpack 4 间接升级到 Webpack 5。
- `monaco-editor-webpack-plugin 4 -> 7.1.1`，`monaco-editor 0.28 -> 0.52`，`react-monaco-editor 0.46 -> 0.59`。
- `webpackbar 5 -> 7`，Webpack 类型包升级到 5.x。
- 移除前端 `start`/`build` 中的 `NODE_OPTIONS=--openssl-legacy-provider`。
- 适配 webpack-dev-server 4 的 `setupMiddlewares`。
- 删除 `uuid` 私有导入路径，改用 `uuid` 公开 API，兼容 Webpack 5 package exports。
- 显式固定 `ajv@8`、`ajv-keywords@5`，避免 npm 11 hoist 到 Webpack5 不兼容组合。
- 显式固定 `@types/babel__traverse@7.18.5`，避免 TypeScript 4.5 解析新版类型语法失败。

遗留风险：
- CRA5 自身仍会打印 webpack-dev-server `onBeforeSetupMiddleware`/`onAfterSetupMiddleware` 弃用警告，长期应通过阶段 2 迁出 CRA 解决。
- Monaco 开发构建会打印缺失 `marked.umd.js.map` 的 source map 警告，不影响构建和运行。
- `npm audit` 仍有历史漏洞，需随后续前后端依赖升级分批处理。

验收门槛：
- `npm start` 成功。
- `npm run checkTs` 成功。
- `npm run build:task` 成功。
- `npm run build` 成功。
- `build/index.html`、`build/shareChart.html`、`build/shareDashboard.html`、`build/shareStoryPlayer.html` 均生成。
- 前端开发服务返回 200，自定义插件接口 `/api/v1/plugins/custom/charts` 返回成功。

### 阶段 2：迁出 CRA 到 Vite

目标：以 Vite 替代 CRA/CRACO，形成长期可维护的前端构建基础。

已完成：
- 新增并行 Vite 5 构建链：`dev:vite`、`build:vite`。
- 默认 `start`、`build` 已切换到 Vite，并已删除 `start:cra`、`build:cra` 这类 CRA5 回退脚本。
- 新增 Vite 多页面 HTML 入口：`index.html`、`shareChart.html`、`shareDashboard.html`、`shareStoryPlayer.html`。
- 迁移 Vite dev proxy 和 custom chart plugins middleware。
- 适配 CRA 兼容层：`process.env.NODE_ENV`、`process.env.PUBLIC_URL`、`module.hot`/`import.meta.hot`、SVG `ReactComponent` 导入、Ant Design Less 的 `~` import。
- 消除 Vite/Rollup 对 `app/components/index.tsx` barrel 循环 re-export 的分包警告。
- Vite 配置已改为 `.mts`，消除 CJS Node API 弃用警告。
- Vite 默认输出目录已切换为 `frontend/build`，兼容后端 Maven `package` 阶段的静态资源复制路径。
- 已删除失去运行职责的 `@craco/craco` 与 `frontend/craco.config.js`，前端运行/构建主链不再保留 CRACO 外壳。

风险：
- `process.env.PUBLIC_URL`、`BrowserRouter basename`、资源路径可能需要统一处理。
- Jest 当前仍暂时借用 `react-scripts` 提供的 transform 与样式/file mock，后续测试栈迁移时需要替换为独立方案。
- Vite 产物仍有大 chunk 提示，替换默认构建前需要继续优化分包和动态加载策略。

验收门槛：
- `npm start` 成功，默认启动 Vite dev server。
- `npm run build` 生成可被后端托管的 `frontend/build`。
- `npm run build:task` 生成 `frontend/build/task/index.js`，并同步 `frontend/public/task/index.js`。
- 端到端访问后端托管静态资源成功。

当前并行验收：
- `npm run build:all` 成功，产出 `build/index.html`、三个 share HTML 和 `build/task/index.js`。
- `npm start` 成功，首页 200，`/api/v1/plugins/custom/charts` 返回成功，`/shareChart/test-token` 返回 share chart 入口。
- `mvn -DskipTests -Dexec.skip=false package -pl server -am` 成功，`copy-resource` 从 `frontend/build` 复制 57 个资源到 `static`，并将 `frontend/build/task/index.js` 重命名为 `server/src/main/resources/javascript/parser.js`。

### 阶段 3：React 17 升级到 React 18

目标：进入 React 18 稳定生态，为 Ant Design 6、React Router 新版本和测试库升级铺路。

升级项：
- `react`、`react-dom`、`react-test-renderer` 升到 18。
- 应用入口从 `ReactDOM.render` 迁到 `createRoot`。
- 检查 StrictMode 下副作用和组件生命周期问题。
- 更新 `@types/react`、`@types/react-dom`。

已完成：
- `react 17.0.2 -> 18.3.1`。
- `react-dom 17.0.2 -> 18.3.1`。
- `react-test-renderer 17.0.2 -> 18.3.1`。
- 应用入口已改用 `react-dom/client` 的 `createRoot`。
- HMR 判断已兼容 Vite ESM 产物，避免 production preview 中访问未定义的 `module`。
- Testing Library 已升级到 React 18 兼容线：`@testing-library/react 14.3.1`、`@testing-library/user-event 14.6.1`。
- Enzyme 短期改用社区 React 18 adapter：`@cfaester/enzyme-adapter-react-18 0.8.0`，移除 React 17 adapter。
- React 类型定义已升级到 React 18：`@types/react 17.0.38 -> 18.3.12`。
- 显式补齐 React 18 不再隐式提供的 `children` 类型，覆盖 Provider、Wrapper、FormGenerator、ListTitle、ChartDrill、Dashboard Widget 等调用点。
- 适配 AntD Tree/Icon 回调、i18next 与 AntD message 内容类型、Chart iframe 容器返回类型等 React 18 类型收紧暴露的问题。

风险：
- 旧组件库可能依赖 React 17 行为。
- Enzyme 对 React 18 支持弱，当前 adapter 只是过渡方案，测试栈仍需迁移到 Testing Library。

验收门槛：
- `npm run checkTs` 通过。
- `npm run build` 通过。
- `npm run build:task` 通过。
- Vite production preview 登录页可渲染。
- 登录、组织/资源列表、图表或仪表板基础流程可操作。

### 阶段 4：UI 与路由现代化（历史阶段记录，当前已切到稳定化）

目标：升级用户界面和路由主线，同时控制视觉和交互回归。

升级项：
- Ant Design 5 主升级已完成，6 暂不作为当前优先级。
- `@ant-design/icons`、`@ant-design/pro-table` 同步升级。
- React Router 5 升级到 6/7。
- 替换或适配不再维护的 UI 依赖。

已完成：
- `react-helmet-async 1.2.2 -> 3.0.0`，先消除 React 18 类型迁移中 Helmet/HelmetProvider `children` 类型不完整的问题。
- `antd 4.16.13 -> 4.24.16`，先升级到 Ant Design 4 最后稳定线，避免一次性跨到 5 带来大面积 API 和主题回归。
- React 18 类型定义已落地，UI 组件中依赖 React 17 隐式 `children` 的写法已清理到可编译状态。
- 移除 `antd-theme-generator`、`frontend/scripts/extractAntdTheme.js` 和浏览器端 `less.min.js` 动态编译链。
- AntD 4.24 样式入口切换为 `antd.variable.min.css`，运行时主题切换改用 `ConfigProvider.config({ theme })` 写入 CSS variables，为 Ant Design 5 token 主题迁移铺路。
- 修复 AntD 4.24 暴露的类型收紧问题，包括 `InputRef`、`InputNumber` 的 `number | null`、Tree/Cascader 节点类型和 FormList `fieldKey` 可选值。
- 修复 Vite 生产构建中 CRA HMR 兼容分支被折叠成裸 `module.hot` 后导致预览页空白的问题。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理 AntD 5 的浮层打开态 API：直接 `Modal`、`Popover` 以及项目 `Popup` 封装已从 `visible`/`onVisibleChange` 迁到 `open`/`onOpenChange`，`ModalForm`、`Confirm`、`DeleteConfirm` 封装保留旧入参并向 AntD 传递 `open`。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理 AntD 5 的 Dropdown 菜单 API：第一批静态菜单已从 `overlay={<Menu />}` 和 `Menu.Item` JSX 改为 `menu={{ items, onClick }}`，覆盖 ListTitle、视图/图表 Tab 右键菜单、仪表板工具栏添加入口、快捷键、设备切换、SQL 预览 Limit、引用资源排序和 Join 类型选择。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理 AntD 5 的自定义 Dropdown 渲染 API：Header/Widget/ChartDraggable 等自定义 `overlay` 入口已切换为 `dropdownRender`，复杂菜单内部 JSX 暂保留。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 继续预处理 AntD 5 的自定义 Dropdown 渲染 API：交互规则关系配置、字段类型选择、富文本引用字段、图表钻取右键菜单和仪表板控制器添加入口已从 `overlay` 切换到 `dropdownRender`。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理图表工作台字段菜单：数据字段更多操作菜单已从 `Menu.Item` JSX 切换到 `Dropdown menu.items`，并移除 `antd/lib/menu/SubMenu` 深路径导入。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 继续预处理 AntD 5 菜单 API：故事板操作菜单和仪表板全屏 widget 列表已从 JSX `Menu.Item` 切换到 `Menu items` 配置，保留原有 Popconfirm 和点击行为。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理视图字段类型菜单：字段类型、日期格式和字段分类菜单已从 JSX `Menu.Item`/`Menu.SubMenu` 切换到 `Menu items` 配置，保留原有 `keyPath` 回调语义。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理仪表板控制器添加菜单：分组控制器菜单已从 `dropdownRender` + JSX `Menu.ItemGroup` 切换到 `Dropdown menu.items` 配置，保留禁用态和添加动作。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理图表字段替换菜单：递归字段替换菜单已从 JSX `Menu.Item`/`Menu.SubMenu` 切换到 `Menu items` 配置，保留叶子节点替换动作。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理富文本引用字段菜单：引用字段下拉菜单已从 `Menu.Item` JSX 切换到 `Menu items` 配置，保留字段插入动作和空数据提示。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理仪表板组件操作菜单：Widget 更多操作菜单已从 `dropdownRender` + JSX `Menu.Item` 切换到 `Dropdown menu.items`，保留分割线、危险态、禁用态和点击动作。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理仪表板更多操作菜单：`BoardDropdownList` 已从 JSX `Menu.Item`/`Menu.Divider` 切换到 `Menu items` 配置，保留分享、下载确认、发布、另存、添加故事板和归档动作。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理可视化资源更多操作菜单：`VizOperationMenu` 已从 JSX `Menu.Item`/`Menu.Divider` 切换到 `Menu items` 配置，保留刷新、另存、加入仪表板、分享、下载确认、发布和归档动作。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理日期层级菜单片段：`DateLevelMenuItems` 已从 JSX `Menu.Item` 切换到 `Menu items` 配置，保留默认值和日期层级计算字段切换逻辑。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理图表钻取右键菜单：`ChartDrillContextMenu` 已从 JSX `Menu.Item`/`Menu.SubMenu` 切换到 `Menu items` 配置，日期层级子菜单复用共享的 `buildDateLevelMenuItems`，保留钻取、联动、查看数据和日期层级切换逻辑。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。

预研结果：
- Ant Design 相关调用点约 358 个文件，`visible`/`onVisibleChange`/`overlay`/`Menu.Item` 等 AntD 5 迁移热点分布广，不能直接大版本替换。
- 动态主题链已从浏览器端 Less 编译迁到 AntD CSS variables；后续迁移 Ant Design 5 时需要把 `ConfigProvider.config` 迁到组件级 `ConfigProvider` token 配置。

风险：
- Ant Design 5/6 token、Less 变量和组件 API 变化较大。
- 当前仍有 AntD 4 API 调用点，包括复杂 `Menu.Item`/`Menu.SubMenu` JSX 菜单、Tooltip/Popover 的 `overlay` 内容、少量项目封装层的 legacy `visible` 入参；这是迁移 Ant Design 5/6 的主要阻塞。
- React Router 6/7 的路由声明和导航 API 有破坏性变化。

继续推进：
- React Router 预迁移第一批：分享页 token 路由和成员页 sidebar 选择态已从 `useRouteMatch` 的 `params/url` 读取迁到 `useParams` / `useLocation`，先减少 `useRouteMatch` 依赖，为后续 `Switch`、`Redirect`、`useHistory` 迁移铺路。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第二批：登录入口和三个分享页 Router 已从 `Route component=` 切换到 children element 写法，先减少 v5 旧版路由声明 API，为后续 `render`、`Redirect`、`Switch` 迁移铺路。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第三批：`AppRouter`、`MainPage` 以及成员、来源、调度、权限子页中的 `Route component=` / 简单 `render=` 已切换到 children element 写法；图表编辑路由改为局部组件内使用 `useLocation` 读取 query，保留原有导航与权限包装逻辑。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第四批：`VizPage/Main` 的看板编辑路由已移除 `Route render=`，并将 `vizId` 读取从 `useRouteMatch` 切到 `useParams`；`frontend/src/app` 下 React Router 旧路由声明热点已收敛到 `Redirect` 与 `useHistory` 为主。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第五批：成员页详情与侧边栏列表中的 `memberId` / `roleId` 读取已从 `useRouteMatch` 迁到 `useParams`，并结合 `useLocation().pathname` 保留成员与角色列表的选中态判断逻辑。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第六批：`Navbar` 与 `SourcePage` 中的模块名、`sourceId` 读取已从 `useRouteMatch` 迁到 `useLocation().pathname` / `useParams`，保留设置区子导航显隐和数据源详情页打开逻辑不变。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第七批：`ViewPage`、`VizPage Sidebar`、`SchedulePage`、`PermissionPage` 中剩余的 `useRouteMatch` 已全部迁到 `useParams` / `useLocation().pathname`，前端主应用内的路由参数读取已不再依赖 `useRouteMatch`。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第八批：新增 `useCompatNavigate` 兼容 hook，认证、登录、注册、激活和重置密码等固定跳转页面已不再直接依赖 `useHistory`，为后续切换到 `useNavigate` 预留统一替换点。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第九批：`OrganizationForm`、组织删除确认、邀请确认页和 `SchedulePage` 的详情跳转 hook 已切到 `useCompatNavigate`，同时补齐兼容 hook 的稳定引用，避免作为 hook 依赖时触发重复副作用。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第十批：`ViewPage` 和 `SchedulePage` 中的字符串路径跳转已切到 `useCompatNavigate`，覆盖视图 Tab/树/回收站以及调度列表/回收站，继续压缩主应用内对 `useHistory` 的直接依赖面。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第十一批：扩展 `useCompatNavigate` 以暴露 `location`，并将 `ViewPage` 的启动分析与编辑器工具栏迁到兼容层，覆盖带 `search` 参数和 `location.state` 读取的复杂导航场景。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第十二批：`VizPage` 与成员页侧边栏中的纯路径跳转已切到 `useCompatNavigate`，覆盖可视化回收站、文件夹树、故事板列表以及成员/角色列表与切换导航，继续缩小主应用侧边栏层对 `useHistory` 的直接依赖。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第十三批：`useDrillThrough`、`useRecycleViz`、顶部导航和权限页侧边栏中的字符串跳转已切到 `useCompatNavigate`，覆盖品牌首页跳转、组织切换、登出回跳和权限页视角详情导航，继续收敛应用级公共入口对 `useHistory` 的直接依赖。
- React Router 预迁移第十四批：`SourcePage` 详情页、资源树、回收站和侧边栏新增入口中的导航已切到 `useCompatNavigate`，覆盖数据源创建后回跳、归档/恢复后的列表回退、资源树详情跳转以及带 `location.state` 的新建视图入口。
- React Router 预迁移第十五批：成员与角色详情页、图表工作台数据视图入口，以及故事板预览/编辑页中的导航已切到 `useCompatNavigate`，覆盖删除后的 replace 回退、故事板预览切编辑、编辑器关闭回跳和基于 `location.state` 的故事板加页事务清理。
- React Router 预迁移第十六批：`MainPage` 主入口和 `VizPage/Main` 标签页导航已切到 `useCompatNavigate`，覆盖图表编辑器关闭回退、保存后跳转、标签切换、关闭当前/其他/全部标签后的回跳，以及保留 `search` 参数的可视化页签导航。
- React Router 预迁移第十七批：`ChartEditor`、`ChartPreviewBoard` 和 Dashboard 头部/动作入口中的对象导航已切到 `useCompatNavigate`，覆盖保存后跳转、加入仪表板时携带 `location.state` 的跳转、可视化预览页回退，以及 Dashboard 编辑入口、关闭编辑器和加入故事板事务传参。
- React Router 预迁移第十八批：`VizPage` 文件夹侧边栏、`BoardEditor` 初始化流程和 Dashboard widget action 中透传的导航对象已切到 `useCompatNavigate`，覆盖新建分析入口、仪表板编辑态从 `location.state` 读取挂件信息，以及图表跳转动作对兼容导航对象的依赖收口。
- React Router 预迁移第十九批：前端主应用内剩余 `Redirect` 已统一替换为本地 `CompatRedirect` 组件，覆盖登录鉴权跳转、模块访问拒绝跳转以及 `MainPage` 根路径、组织首页和权限首页的默认跳转，先消除 `Redirect` 对 Router v5 的直接依赖。
- React Router 预迁移第二十批：`AppRouter`、三个分享页 Router、成员页和 `VizPage/Main` 中的 `Switch` 已统一替换为本地 `CompatSwitch` 组件，先把 Router 容器组件的升级入口收敛到单点，降低后续切换到 Router 6 `Routes` 时的改动面。
- React Router 预迁移第二十一批：新增 `CompatRoute` / `CompatRoutes`，并将 `AppRouter`、三个分享页 Router、成员页和 `VizPage/Main` 改写为 `element` 风格声明，先把低风险路由容器从 v5 的 children 形态迁到更接近 Router 6 `Routes + Route element` 的结构。
- React Router 预迁移第二十二批：`MainPage` 主路由容器和 `LoginAuthRoute` 已切到 `CompatRoutes + CompatRoute + element` 形态，主应用入口不再直接依赖 `Switch` 和 `Route` children 声明，为后续真正切换到 Router 6/7 的 `Routes` 打通主干。
- React Router 预迁移第二十三批：`SourcePage`、`SchedulePage`、`PermissionPage` 内部详情路由已统一切到 `CompatRoute`，`MainPage` 里最后一个 `useRouteMatch` 也已移除，继续把页面容器层对 Router v5 `Route` / `useRouteMatch` 的直接依赖压缩到兼容层本身。
- React Router 预迁移第二十四批：新增本地 `routerCompat` 出口，并将 `AppRouter`、分享页 Router、`Compat*` 组件、`useCompatNavigate`、`useRouteQuery` 以及部分主入口/测试页的路由导入统一收口到项目内部模块，为后续真正替换到底层 Router 6/7 实现先缩小 import 改动面。
- React Router 预迁移第二十五批：登录/找回密码页面、主导航、成员/来源/调度/Viz 侧边栏，以及三个分享页主页面的 `useLocation` / `useParams` / `Link` / `NavLink` 导入已继续切到本地 `routerCompat`，进一步扩大统一出口覆盖面，为下一步替换兼容层底座减少散点改动。
- React Router 预迁移第二十六批：成员/来源/调度详情页与故事板编辑/播放页的 `useParams` 导入已全部切到本地 `routerCompat`，当前 `frontend/src/app` 内对 `react-router-dom` / `react-router` 的直接依赖已经收口到 `routerCompat.ts` 单点，后续可以开始真正替换兼容层底座实现。
- React Router 预迁移第二十七批：`useCompatNavigate` 已改为项目内显式包装的导航 API，继续保留字符串和对象导航、`location.state` 与 `go/goBack` 兼容行为；`CompatRedirect` 同时移除了 `Route render` 旧写法，把后续迁移焦点进一步收敛到兼容层底座本身。
- React Router 预迁移第二十八批：`AuthorizedRoute` 已切到 `CompatRoute`，兼容层外部最后一个直接渲染 `Route` 的授权封装也已收回到本地路由组件；当前应用内的路由声明旧实现进一步集中到 `CompatRoute` 与 `routerCompat.ts` 两个底座点。
- React Router 预迁移第二十九批：删除仅做空转发的 `CompatSwitch`，让 `CompatRoutes` 直接承接 `Switch`；兼容路由容器层级继续压缩，为后续把 `CompatRoutes` 直接替换到 Router 6/7 `Routes` 减少一层过渡包装。
- React Router 预迁移第三十批：`CompatRoute` 与 `CompatRedirect` 已脱离 `RouteProps` 类型耦合，改为项目内部只暴露 `path` / `exact` / `element` / `to` 的窄接口；`routerCompat.ts` 也不再向外导出 `RouteProps`，进一步压缩 Router v5 类型面对业务层的渗透。
- React Router 预迁移第三十一批：新增 `CompatNavLink`，先承接 Navbar 中唯一仍依赖 `activeClassName` / `isActive` 的 v5 `NavLink` 历史 API；主导航和设置子导航已切到本地兼容组件，为后续把底层 `NavLink` 切到 Router 6 写法继续缩小业务改动面。
- React Router 预迁移第三十二批：`useRouteMatch` 已从 `routerCompat.ts` 出口移除，确认应用层与兼容层公开接口都不再依赖该 v5 历史 API；`CompatNavLink` 的自定义激活判断也改为只消费当前业务实际需要的 `pathname`。
- React Router 预迁移第三十三批：新增内部 `routerCompatLegacy.ts`，把 `Route` / `Switch` / `NavLink` / `useHistory` 全部关回兼容层内部；公开的 `routerCompat.ts` 只再暴露 `BrowserRouter`、`Link`、`MemoryRouter`、`useLocation`、`useParams` 这些 Router 6 仍稳定存在的能力，为真正升级依赖时缩小公开 API 差异。
- React Router 预迁移第三十四批：`CompatNavLink` 已彻底脱离底层 `NavLink`，改为 `Link + useLocation` 自行计算激活态；`routerCompatLegacy.ts` 因此不再需要保留 `NavLink`，Router 5 的链接级历史 API 已进一步从兼容层底座中清除。
- React Router 预迁移第三十五批：成员、来源、调度、权限四类详情容器已从 `CompatRoute` 切到 `useParams` / `useLocation` 驱动的条件渲染；这些页面不再把详情区显示逻辑委托给 v5 `Route` 匹配，兼容层底座对 `Route` 的真实依赖面继续缩小。
- React Router 预迁移第三十六批：`AppRouter` 中的 `LoginAuthRoute` 已回收为普通 `CompatRoute` 的 `element`，分享页三个 Router 和 Viz 看板编辑器入口也已去掉只承载单一路由的 `CompatRoutes` 包装；这些场景不再占用底层 `Switch`，兼容层对 `CompatRoutes` 的真实依赖进一步收敛到少数多分支主容器。
- React Router 预迁移第三十七批：`CompatRoute` / `CompatRoutes` 已改为使用 `useLocation + matchPath` 自行匹配，不再依赖底层 `Route` / `Switch` 运行时；`routerCompatLegacy.ts` 因此只剩 `useHistory`，兼容层内部的 Router 5 运行时依赖已压缩到最后一个导航能力点。
- React Router 预迁移第三十八批：新增 `routerCompatRuntime.tsx`，由兼容层自持 `BrowserRouter` / `MemoryRouter` 与 history 上下文；`useCompatNavigate` 已改为直接消费内部 `useCompatHistory`，不再依赖 `useHistory` hook，至此兼容层内部对 Router 5 运行时 hook 的依赖也已清空。

验收门槛：
- 全部路由可访问。
- 表单、弹窗、表格、菜单、布局无明显视觉破坏。
- 关键工作流人工验收通过。

### 阶段 5：状态管理、测试与前端依赖整理

目标：减少老旧库和重复状态逻辑，提高长期维护性。

升级项：
- Redux Toolkit 1 -> 2。
- React Redux 7 -> 9。
- Testing Library 升级，逐步移除 Enzyme。
- Prettier 2 -> 3、ESLint/stylelint 规则升级。
- Axios 0.x -> 1.x，`js-cookie` 2 -> 3，`uuid` 8 -> 当前主线。

风险：
- RTK 2 和 React Redux 9 对类型推断和 store 配置有变化。
- Axios 1 的取消请求、拦截器类型和错误对象可能影响请求封装。

已完成：
- `js-cookie 2.2.1 -> 3.0.8`，`@types/js-cookie 2.2.6 -> 3.0.6`，先完成低风险认证 cookie 库升级，保持现有 token 读写封装不变。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- `axios 0.21.1 -> 1.17.0`，完成请求库主线升级；按 `AxiosError<APIResponse<any>>` 补齐错误响应类型，保持现有拦截器和错误提示行为不变。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- `react-beautiful-dnd` 已从数据模型树场景迁出到维护中的 `@hello-pangea/dnd 18.0.1`，保留现有拖拽排序、组合建层级和层级内移动逻辑；补齐 `Draggable index` 的 number 约束。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- `typescript 4.5.4 -> 5.9.3`，`@reduxjs/toolkit 1.8.0 -> 2.12.0`，`react-redux 7.2.6 -> 9.3.0`；同步移除 `@types/react-redux`，按 RTK 2 的 `enhancers` 新签名调整 store 配置，并修复 TS5 暴露的公共树型 helper、图表样式和迁移测试类型问题。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。

验收门槛：
- `npm run checkTs` 通过。
- 主要单测通过或迁移完成。
- 请求封装和认证流程正常。

### 阶段 6：后端库现代化

目标：删除老旧、高风险或重复的后端基础库。

当前 review 结论：
- `Selenium 3 + PhantomJSDriver` 是当前后端最老旧、风险最高的自动化组合。PhantomJS 已停止活跃维护，现代替代应优先考虑 `Playwright`，如必须保留 JVM 内集成则退而求其次迁到 `Selenium 4 + Chromium WebDriver`。
- `fastjson 1.2.x` 仍在多个模块承担 JSON 解析、HTTP message converter 和配置反序列化，属于历史包袱。对 Spring Boot 3 项目，更现代且收敛的方向是统一到 `Jackson`；如果必须保留阿里系 API，则至少迁到 `fastjson2`，但不建议继续双栈长期共存。
- JWT 主链当前运行时仍基于 `jjwt 0.7.0`，历史上还残留过未使用的 `java-jwt 3.7.0` 依赖。建议继续统一到单一现代库，优先评估升级到 `jjwt 0.12+`，也可评估是否完全交给 Spring Security OAuth2/JOSE 能力。
- `Apache HttpClient 4.5.x` 与 `OkHttp` 并存，HTTP 客户端重复。JDK 21 下更现代的方向是统一到 `java.net.http.HttpClient` 或只保留一套活跃客户端；若短期兼容成本最低，可先升级到 `HttpClient 5` 并逐步收口。
- `commons-lang 2.6`、`commons-io 1.3.1`、`guava 21.0`、`poi-ooxml 5.0.0`、`commons-csv 1.8` 都明显偏旧，其中 `commons-lang 2` 和 `commons-io 1.x` 优先级更高，因为现代代码大多已可用 `commons-lang3`、`commons-io 2.x` 或 JDK 标准库替换。
- `H2 1.4.200` 仍适合作为兼容 demo 库，但不应视作长期现代化终态。若继续维护 demo / 测试数据，应补一套可在 `H2 2.x` 下运行的数据脚本或改为容器化测试数据库。
- `Shiro 2` 现在已经能在 Boot 3 上运行，但从 Spring 生态一致性看，长期仍弱于 `Spring Security` 原生体系。这个迁移收益很高，但改动面极大，不应排在 Router、测试栈和 JSON/JWT 清理之前。

建议升级顺序：
1. 浏览器自动化：`PhantomJS` / `Selenium 3` -> `Playwright` 或 `Selenium 4`。
2. 后端 JSON 栈：`fastjson 1.x` -> `Jackson` 单栈。
3. JWT 栈统一：移除 `jjwt 0.7` 与 `java-jwt 3.7` 双实现。
4. 基础工具库清理：`commons-lang 2`、`commons-io 1.x`、`guava 21`、`httpclient 4`。
5. 数据与 demo 兼容：`H2 1.4` 升级路线。
6. 安全框架长期演进：评估 `Shiro -> Spring Security`。

升级项：
- `fastjson 1.x` 迁移到 `fastjson2` 或统一到 Jackson。
- `jjwt 0.7`、`java-jwt 3.7` 统一为一个现代 JWT 库。
- Apache HttpClient 4 迁移到 HttpClient 5，或统一 OkHttp/JDK HttpClient。
- Selenium 3/PhantomJS 迁移到 Selenium 4 + Chrome/Edge WebDriver，或 Playwright。
- `commons-io 1.3.1`、`commons-lang 2.6`、Guava 21、POI 5.0、Calcite 1.26、MyBatis Generator 1.4 等逐项升级。
- H2 1.4.200 仅作为 demo 库兼容保留；长期应生成 H2 2.x 可读的新 demo 数据库或迁移到 Testcontainers。

风险：
- JSON 序列化行为变化可能影响 API、配置、导入导出。
- JWT 库统一会影响 token 签发、校验和兼容性。
- Selenium/PhantomJS 替换会影响截图和导出能力。
- Calcite/POI/Guava 大版本升级可能影响数据处理逻辑。

验收门槛：
- 后端模块编译通过。
- `mvn test -pl data-providers/jdbc-data-provider -am` 通过。
- demo profile 启动成功。
- `/api/v1/sys/info` 返回 200。
- 截图/导出能力有浏览器级验证。

### 阶段 7：构建、CI 与发布现代化

目标：让本地、CI、Docker 和发布包使用一致的现代工具链。

升级项：
- Maven Enforcer 约束 Java、Maven、Node、npm 版本。
- Dockerfile 固化 JDK 21 runtime，并加入必要的 `--add-opens` 或替换反射实现。
- CI 拆分后端测试、前端类型检查、前端构建、集成启动验证。
- 清理 Maven 里过时的淘宝 registry 参数，统一使用 `.npmrc` 或 CI secret。
- 修复前端产物和 parser task 的构建顺序。

验收门槛：
- 干净环境可以一键构建。
- Docker 镜像可启动。
- 发布包包含正确静态资源和 parser.js。

## 阶段交付物与完成定义

这一节用于把升级路线变成可执行项目计划。每个阶段只有在“交付物”和“完成定义”都满足时，才算真正完成。

### 阶段 0 交付物

- 根 `pom.xml`、前端 `package.json`、`Dockerfile` 中的基础运行时版本声明完成统一。
- 文档中明确最低 Java / Maven / Node / npm 要求。
- 本地与发布包的启动路径可复现。

完成定义：
- 新同事按文档准备环境后，能在不额外猜测版本的情况下完成构建和启动。

### 阶段 1-2 交付物

- Vite 成为默认开发与生产构建链。
- CRA / CRACO 退出前端开发、构建与测试主链。
- 多入口 HTML、静态资源复制、parser task 生成链稳定。

完成定义：
- 后端 Maven `package` 产物不再依赖 CRA 链。
- 前端开发、生产构建和后端托管结果一致。

### 阶段 3 交付物

- React 18 主升级完成。
- React 18 类型收口完成。
- React 18 下主流程可运行。

完成定义：
- 不再保留任何为了兼容 React 17 而存在的临时实现。

### 阶段 4 交付物

- React Router 6/7 升级完成。
- Ant Design 5 升级完成。
- 主题、弹层、菜单和复杂工作区页面人工验收完成。

完成定义：
- 前端主应用不再直接依赖 Router 5 API。
- AntD 4 专属 API 和样式补丁收敛到可删除状态。

### 阶段 5 交付物

- Enzyme 完全移除。
- CRA 测试链完全退出。
- Testing Library 与独立测试运行器稳定。
- Lint / Format / Type Check 工具链与 Node 26 稳定兼容。

完成定义：
- `frontend/package.json` 中不再保留 `enzyme`、`@cfaester/enzyme-adapter-react-18`、`react-scripts`、`@craco/craco`。

### 阶段 6 交付物

- PhantomJS / Selenium 3 替换完成。
- `fastjson 1.x` 清理完成。
- JWT 双栈统一完成。
- 旧基础库分批替换完成。
- demo / 内置样例数据库策略明确。

完成定义：
- `pom.xml` 各模块中不再出现 `phantomjsdriver`、`selenium-java 3.x`、`fastjson 1.x`、`jjwt 0.7.0`、`java-jwt 3.7.0`、`commons-lang 2.x`、`commons-io 1.x`。

### 阶段 7 交付物

- CI 流水线覆盖后端测试、前端检查、前端构建、集成启动验证。
- Docker 镜像和发布包都能在干净环境运行。
- 关键业务验收有稳定脚本或检查表。

完成定义：
- 同一提交在本地、CI、Docker 和发布包中的构建行为一致。

## 专题模板与决策表

这一节用于把“长期专项怎么开、怎么推进、怎么验收”固定成统一模板。后续每个现代化专题都应回填到这里，而不是继续只在过程记录里增长。

### 专题模板

每个专项都必须补齐以下 8 项：

1. 当前栈与当前问题
2. 目标替代方案
3. 选择该方案的理由
4. 迁移前提
5. 当前已完成的前置拆障
6. 主改动面
7. 专项验收方式
8. 回退方式与完成定义

### 当前长期专题决策表

| 专题 | 当前栈 | 目标替代方案 | 当前阶段 | 进入条件 | 完成定义 |
| --- | --- | --- | --- | --- | --- |
| 时间体系 | `moment` 收尾中 | `dayjs` 单栈 | 已进入收尾阶段 | AntD 5 主升级稳定 | 生产代码、task 产物、控件值链和页面回归全部闭环 |
| 富文本 | `react-quill 2.0.0` / Quill 旧类型耦合 | 先定真正的 Quill 2 React 路线或继续压缩兼容层 | 兼容层已收口，待路线定稿 | 时间体系波动降低 | 编辑、只读、邮件、仪表板富文本统一落在现代路线 |
| 测试栈 | `Vitest 4 + jsdom 29` | `Vitest` 单栈 | 主链已完成，进入稳定化阶段 | 已完成主要 Jest 退出与全量回归 | 测试入口、环境、mock、CI 全部维持单栈稳定 |
| 工具链规范 | `ESLint 9` + `stylelint 17` + `Prettier 3` | 进入当前稳定主线 | 已完成 ESLint 9 flat config，进入历史 warning 渐进治理 | 测试栈路线明确 | lint/format/type check 与 Node 24 LTS 一致，并补 Node 当前线验证；历史格式差异进入可控存量 |
| 脚本引擎 | `nashorn-core 15.4` | `GraalJS` | 已做 JSR-223 收口 | 前端主栈稳定，脚本边界明确 | 运行时不再依赖 Nashorn 兜底 |
| SQL 解析内核 | `Calcite 1.26.0` | 较新稳定线 | 待专项预研 | 脚本专题独立 | parser、函数、JDBC provider 回归通过 |
| 安全体系 | `Shiro 2.0.5` | `Spring Security` | 已做前置拆障 | JWT/OAuth/脚本链稳定 | 登录、分享页、权限、remember-me、OAuth2 全链路接管 |
| 代码生成链 | `mybatis-generator-core 1.4.0` | 独立 profile / 工具模块 | 主运行时已退出 | 主运行时稳定 | 默认运行时完全不依赖生成器链 |

### 专题优先级判断规则

当多个栈都还可以继续升级时，按以下顺序判断优先级：

1. 是否仍在主运行时链上
2. 是否已进入 maintenance mode 或已经退场
3. 是否能显著缩小后续改动面
4. 是否已有兼容层或中立边界
5. 是否有可控的专项验收路径

按这套规则，当前最值得继续推进的顺序仍然是：

1. 时间体系收尾
2. 富文本专题
3. 测试/工具链路线定稿
4. `Nashorn -> GraalJS`
5. `Shiro -> Spring Security`

### 现代替代方案依据

这部分只记录路线依据，不展开长引文。

| 领域 | 当前建议方案 | 依据类型 | 当前结论 |
| --- | --- | --- | --- |
| 安全框架 | `Spring Security` | Spring 官方 reference | 与 Boot 3 / OAuth2 / JOSE 同生态，是当前 Spring 主线默认安全体系 |
| 脚本引擎 | `GraalJS` | GraalVM 官方 JS / ScriptEngine 文档 | 是 Nashorn 退出后的现代替代方向，且支持标准 JSR-223 接入 |
| 测试栈 | `Jest 30` 或 `Vitest` | 官方迁移文档与当前 Vite 生态 | 需要二选一，不宜长期维持“构建链现代、测试链半历史化”的状态 |
| 富文本 | Quill 2 路线 | Quill 官方升级文档与 `react-quill` 包状态 | `react-quill 2` 不等于 Quill 2，必须单独评估底层编辑器路线 |
| task 打包链 | `Vite library mode` | Vite 官方 build 文档 | 当前已是更现代、与主构建链一致的替代方案 |

### 允许暂时保留但不能视为终态的栈

只有满足下面全部条件，某项技术才允许暂时保留：

- 当前仍能在 JDK 21 / Node 24 LTS 默认基线、Node 26 兼容验证 / React 18 / Boot 3 基线上稳定运行
- 替换它会显著放大当前回归面
- 当前已经有边界收口或适配层
- 文档里明确写了退出条件

按这个规则，目前允许暂时保留、但不能宣称已经完成现代替代的主要只有：

- `Shiro 2`
- `nashorn-core`
- `calcite-core 1.26.0`
- `react-quill 1.3.5`
- `Jest 29`

## 建议执行顺序

这一节保留的是较早阶段的执行顺序记录。当前真正生效的总控顺序，以“项目总控蓝图”和“专题决策表”为准。

基于当前阶段记录，后续执行曾按下面顺序推进：

1. 完成 React Router 预迁移收口并切换到 Router 6/7。
2. 继续清理 AntD 4 历史 API，完成 AntD 5 升级。
3. 清理 CRA / Enzyme 测试链，迁到现代测试方案。
4. 替换 PhantomJS / Selenium 3。
5. 推进后端 JSON 栈单栈化。
6. 统一 JWT 实现。
7. 分批替换旧基础库。
8. 处理 H2/demo 与长期安全框架演进。

## 当前 backlog 量化视图

这一节用于把计划落到当前代码面，避免后续升级只凭感觉推进。

### A. React Router 主升级 backlog

当前扫描结论：
- 主应用业务代码里的 `useRouteMatch`、`Route component=`、`Route render=`、业务层 `Redirect` 已基本清空。
- 剩余 Router 5 依赖主要集中在兼容层自身：
  - `frontend/src/app/hooks/useCompatNavigate.ts`
  - `frontend/src/app/components/CompatRoute.tsx`
  - `frontend/src/app/components/CompatRoutes.tsx`
  - `frontend/src/app/components/CompatRedirect.tsx`
- 剩余页面容器层直接 `Route` 已基本清空，主应用内容页容器主要通过 `CompatRoute` / `CompatRoutes` 承接。
- `AuthorizedRoute` 已收口到 `CompatRoute`，兼容层外部最后一个直接渲染 `Route` 的授权封装已移除。
- 路由能力的 import 源已开始从 `react-router-dom` / `react-router` 向本地 `routerCompat` 收口，后续可以按批次继续把剩余页面切到同一出口。
- 当前剩余直接依赖已经集中到少量详情页、故事板编辑/播放页和兼容层本身，后续更适合继续按模块批次清理，而不是全局撒网式替换。
- 当前应用层对外部路由包的直接依赖已经清空，只剩 `routerCompat.ts` 作为单点出口；下一阶段重点应转向 `useCompatNavigate`、`CompatRoutes` 和 `CompatRedirect` 的回归验证与最终简化。
- `useCompatNavigate` 已改为包装 `useNavigate`，`CompatRedirect` 已不再透传 v5 `Route render` 语义；当前重点不再是继续“去 v5 hook”，而是确认默认跳转、replace、对象导航和 `location.state` 的行为一致性。
- `CompatRoute` / `CompatRedirect` 已不再依赖 `RouteProps`，兼容层对外只暴露当前业务实际使用到的最小路由声明能力。
- `NavLink` 的 v5 历史 API 已收口到 `CompatNavLink`，当前业务层不再直接依赖 `activeClassName` / `isActive`。
- `useRouteMatch` 已从兼容出口移除，当前公开路由能力里已不存在该历史 API。
- 公开 `routerCompat.ts` 已直接回到 Router 6 稳定能力：`BrowserRouter`、`MemoryRouter`、`Link`、`useLocation`、`useNavigate`、`useParams`。
- `CompatNavLink` 已不再依赖底层 `NavLink`，当前兼容层内部也不再保留 `routerCompatLegacy.ts` 这类 Router 5 runtime 过渡层。
- 成员、来源、调度、权限这些“详情区条件显示”页面已不再依赖 `CompatRoute`，当前 `Route` 的主要残留压力已经收敛到主入口和少数真正需要互斥匹配的容器。
- 剩余需要继续处理的重点不是“全局搜索更多旧 API”，而是：
  1. 评估 `CompatRoute` / `CompatRoutes` 是否还有长期保留价值。
  2. 补齐嵌套路由、默认跳转、参数路由和分享页的回归验证。
  3. 逐步把兼容组件继续缩小到纯薄封装，避免长期背负自定义路由层。

工作包拆分：
1. 主入口验收：`AppRouter`、`LoginAuthRoute`、`MainPage`、share routers。
2. 复杂页面回归：`VizPage`、`ViewPage`、成员页、权限页、故事板。
3. 兼容层瘦身：评估 `CompatRoute`、`CompatRoutes`、`CompatRedirect` 是否可继续向 Router 6 原生组件收缩。

### B. Ant Design 5 backlog（当前已转为稳定化 backlog）

当前扫描结论：
- 主业务弹窗和分享页的 `Modal` / `Drawer` / `Popover` 已基本收口到 `open` / `onOpenChange`，`visible` 的搜索结果里目前有相当一部分只是内部状态字段或 styled-components 私有属性，并非 AntD 4 旧 API。
- JSX `Menu.Item` / `Menu.SubMenu` / `Menu.Divider` 在业务代码里已基本清零；当前更真实的稳定化重点变成：
  - 若干 `dropdownRender` 自定义菜单入口仍直接拼 `Menu` 结构；
  - 少量封装层仍兼容 `visible ?? open` 双入参；
  - token 主题链、时间组件值链和 `TreeSelect` 旧 props 仍需继续收口。
- 自 2026-06-10 这轮持续清障之后，以下语义已基本完成主升级前的历史阻塞清理：
  - `Dropdown dropdownRender -> popupRender`
  - `Dropdown destroyPopupOnHide -> destroyOnHidden`
  - `Select dropdownMatchSelectWidth -> popupMatchSelectWidth`
  - 成员、变量、分享页等高频弹层的 `open` / `onOpenChange` 语义
- 当前剩余 backlog 需要分层看待：
  - 可以继续直接清理的：
    - 少量 `Popup` / `Popover` / `Dropdown` 的样式挂载点命名；
    - 个别菜单或子菜单配置上的 `popupClassName`；
    - 仍只为过渡存在的 compat props 壳。
  - 需要和相关专题联动处理的：
    - `TreeSelect dropdownRender/dropdownStyle/dropdownMatchSelectWidth`；
    - token / `ConfigProvider` 主题链；
    - DatePicker / RangePicker 与 `dayjs` 值对象链联动；
    - 第三方依赖与 AntD 5 的类型兼容细节。

工作包拆分：
1. 清理内部组件与样式层把 `visible` 当作私有属性的历史命名，降低对 AntD 旧 API 的搜索噪音。
2. 收口少量仍能在当前 `antd 5` 基线上继续清理的 `Popup/Popover/Dropdown` 样式与菜单细节。
3. 结合 `moment -> dayjs` 专题集中处理 `TreeSelect`、token 主题链和时间组件值链。
4. 继续做页面级回归并删除过渡 compat 壳。

### C. 前端测试栈 backlog

当前扫描结论：
- `frontend/package.json` 已不再保留：
  - `react-scripts`
  - `@craco/craco`
  - `enzyme`
  - `@cfaester/enzyme-adapter-react-18`
  - 包级 `eslintConfig`
- 当前残留主要是：
  - Jest 主链已升级到 `29.7.0`，`babel-jest`、`jest-environment-jsdom`、`@types/jest` 与 `@types/node` 也已同步到现代稳定线
  - 测试入口已经脱离 `react-app` / CRA transform，当前仍保留的是 Jest 运行模型本身，以及 `jest-watch-typeahead` 这类配套插件
  - ESLint 主链已切到项目自有显式依赖与 `eslint.config.mjs` flat config，但当前 lint 结果仍有一批存量 warning 需要后续分批清理

工作包拆分：
1. 评估是否继续保留 Jest 29，还是在覆盖面和运行速度都可接受时迁到 Vitest。
2. 继续清理测试 warning、`act(...)` 警告和 React Router future flag 噪音，让测试输出更干净。
3. 继续清理 lint 存量 warning，重点收口 Prettier 历史格式差异、无效 `eslint-disable` 注释和 Hooks 依赖提示。

### D. 后端现代化 backlog

当前扫描结论：
- 浏览器自动化：
  - `core/pom.xml`
  - `server/pom.xml`
  当前只保留 `selenium-java 4.31.0`，`phantomjsdriver` 已移除
- JSON 栈：
  - 后端生产代码中的 `fastjson` 使用点已清零
  - Web 层默认 message converter 已回到 Jackson
- JWT：
  - `core/pom.xml` 已切到 `jjwt-api` / `jjwt-impl` / `jjwt-jackson 0.12.7`
  - `security/pom.xml` 中 `java-jwt` 已删除
- 旧基础库：
  - `httpclient 4.x` 已退出，当前主链为 `httpclient5 5.5`
  - `h2`、`poi-ooxml`、`commons-csv` 已升级到现代稳定线
  - 剩余更值得继续关注的是 `guava`、`commons-text`、`aspectjweaver` 以及更长线的 `Druid / Shiro / Nashorn / Calcite`

工作包拆分：
1. 继续收口仍停留在旧生态的基础库：Guava、Commons Text、AspectJ。
2. 补齐 demo/H2 策略与样例数据迁移的验证闭环。
3. Druid / Shiro / Nashorn / Calcite / MyBatis Generator 的中长期专项设计。

### E. 推荐近期执行队列

基于当前风险和收益，建议接下来的 5 个 checkpoint 按这个顺序推进：

1. `moment -> dayjs`：继续沿“格式化出口 -> 类型残留 -> 值链专题”分批收口。
2. 富文本与故事播放专题：评估 `react-quill 1.x` 和当前 `reveal.js 6.x` 的后续现代化路径。
3. `styled-components 6` 稳定化复核：继续检查 `shouldForwardProp`、iframe/popup target 和测试告警。
4. 测试输出治理：继续清理 Jest warning、`act(...)` 警告和 React Router future flag 噪音。
5. 后端长期基础库专题：Guava / Commons Text / AspectJ，以及 Druid / Shiro / Nashorn 的专项设计。

## 最终完成定义

当且仅当以下条件同时成立，才可视为“整个技术栈清单都是现代化替代方案”已经完成：

- 文档中的“最终现代化技术栈清单”全部落地，而不是只完成一部分。
- 前端不再依赖 Router 5、CRA/CRACO、Enzyme、AntD 4 历史 API。
- 后端不再依赖 PhantomJS、Selenium 3、fastjson 1.x、双 JWT 老实现、`commons-lang 2`、`commons-io 1.x` 等已认定为老旧的核心依赖。
- 本地、CI、Docker、发布包都能使用同一套现代化构建链。
- 关键业务流程、分享页、登录鉴权、图表/仪表板、截图导出都完成验收。

## 不建议一次性升级的项目

- React、Ant Design、React Router、Vite 不应在同一个提交里同时完成。
- fastjson2、JWT 库统一、HttpClient 5 不应和 Spring Boot 主版本升级混做。
- Selenium 4/Playwright 迁移需要先明确截图、导出、浏览器驱动安装方式。
- H2 2.x demo 数据库迁移需要生成新数据或迁移脚本，不能只改版本号。

## 每阶段通用验收清单

- `git status --short` 只包含本阶段预期文件。
- `git diff --check` 通过。
- 后端相关阶段运行 Maven 编译或模块测试。
- 前端相关阶段运行 `npm run checkTs`，必要时运行构建。
- 启动后端 demo profile，验证 `/api/v1/sys/info`。
- 启动前端，验证首页和至少一个 API 代理请求。
- 每个阶段单独提交，提交信息用中文说明升级范围。

## 当前下一步

阶段 3 已完成，阶段 4 和阶段 5、6 已进入并行推进态。下一步建议按以下顺序执行：

1. 收口 AntD 5 升级前最后一批热点：复杂 `Menu.*` JSX、`visible`、深路径导入和主题 token 入口。
2. 把测试栈从 Jest 27 + `react-app` 系配置推进到较新稳定线。
3. 继续推进 `poi-ooxml 5.0.0 -> 较新 5.x` 专项，优先补导出与样式转换链回归。
4. 在已完成 H2 2.x 迁移后，继续评估连接池、安全框架与脚本引擎等中长期专项。

## 2026-06-10 老旧技术栈盘点

本轮全项目 review 后，仍然建议继续现代化的重点如下。这里已经按“是否仍在运行时主链”和“升级风险是否可控”重新排序，避免把已完成项继续误标为待办。

### 一类：运行时主链里仍偏旧，且后续收益明确

1. `jjwt 0.7.0`
   - 现状：当前 JWT 主链已在 `core/pom.xml` 升级到 `jjwt-api/jjwt-impl/jjwt-jackson 0.12.7`，生产代码调用仍集中在 `JwtUtils` 与 `JwkUtils`。
   - 更现代替代：`jjwt 0.12+`，或后续统一到 Spring Security 的 JOSE 能力。
   - 调研结论：新版 `jjwt` 对 HMAC 密钥长度校验更严格，当前默认 `datart.security.token.secret` 过短，不能直接硬升；因此已在工具层实现“新签发使用派生的 256-bit HMAC key，旧短 secret token 继续兼容解析”的双轨策略。
   - 风险判断：主链升级已完成，但若未来要彻底移除旧短 secret 兼容分支，需要先准备配置迁移公告和 token 失效窗口。

2. `org.apache.httpcomponents:httpclient:4.5.14`
   - 现状：仍处在 HTTP 数据源与第三方 OAuth 运行时主链。
   - 更现代替代：`HttpClient 5.x`，或进一步收敛到 JDK `HttpClient` / `OkHttp` 单栈。
   - 调研结论：Apache 官方提供了 5.x 迁移指南，经典阻塞式 API 已整体迁到 `org.apache.hc.httpclient5` 包名空间，属于明确的 API 级升级，不是只改版本号。
   - 风险判断：需要同步改造请求/响应模型、URI 构造、SSL 配置和实体读取逻辑，建议单列为下一批后端专项。

3. `poi-ooxml 5.0.0`
   - 现状：Excel 导入导出、样式转换链已从 `5.0.0` 升级到 `5.5.1`，`core` 模块补了最小 Excel 写入/读取回归测试。
   - 更现代替代：当前先稳定在 POI `5.5.1` 这条活跃 5.x 正式版主线，后续再按上游发布节奏做小版本跟进。
   - 调研结论：项目自有代码只用到了 `Workbook` / `Sheet` / `CellStyle` 等基础 API，升级本身风险可控；真正暴露出的兼容点不是 POI API 变更，而是 `server` 的 PDF 导出链一直直接使用 PDFBox，却没有显式声明依赖。
   - 风险判断：这一批已完成；后续若继续治理图像 / SVG / PDF 导出链，应把 `batik`、`pdfbox` 与截图链一起作为一个更明确的导出专项。

4. Guava 21.0
   - 现状：项目自有生产代码中的 `CaseFormat`、`Lists`、`Sets`、`ImmutableSet`、`Iterables` 使用点已全部替换为 JDK 集合工厂或本地 `NamingUtils`；`core/pom.xml` 中 `guava 21.0` 直连依赖已删除。
   - 更现代替代：当前策略是不再为自有代码保留 Guava API 依赖，后续若要进一步压缩传递依赖，再分别评估 Calcite 与 Selenium 升级路径。
   - 调研结论：当前依赖树中仍有 Guava，但来源已经收敛为上游组件传递依赖，其中 `calcite-core` 带入 `29.0-jre`，`selenium-remote-driver` 带入 `33.4.6-jre`。
   - 风险判断：这批迁移已经完成；剩余风险不在项目代码，而在上游组件版本联动。

5. `commons-csv 1.8` 与 `commons-text 1.9`
   - 现状：`commons-csv` 已从 `1.8` 升级到 `1.14.1`；`commons-text` 已确认在生产代码中无使用面，并已移除直接依赖。
   - 更现代替代：CSV 能力继续保留在 Apache Commons CSV；文本处理优先回归 JDK / `commons-lang3`，不再保留无效壳依赖。
   - 调研结论：这一批已完成，当前主链不再存在 `commons-text`，`commons-csv` 也已收口到现代稳定线。
   - 风险判断：后续只需在处理 `CSVParse` 时关注 1.14.x 的 API 废弃告警，无需再为版本滞后单独开专项。

6. `aspectjweaver 1.9.8.M1`
   - 现状：已从 `1.9.8.M1` 升级到 `1.9.25.1`，不再保留 milestone 版。
   - 更现代替代：当前先保持 Spring AOP + AspectJ weaver 组合，后续再评估是否还有必要继续显式声明。
   - 风险判断：升级已完成，当前仅剩 `AccessLogAdvice` 一处切面使用面，后续若继续收缩 AOP 能力可以再评估是否移除。

7. `H2 1.4.200`
   - 现状：父 POM 已升级到 `H2 2.4.240`；`LocalDB` 本地 SQL 执行链与仓库内 demo 数据库文件都已完成适配。
   - 更现代替代：当前已落到 `H2 2.x` 现代稳定线；更长期的测试隔离方案仍可评估迁到 Testcontainers。
   - 调研结论：这批升级的关键不在 API，而在数据库文件格式。旧 `1.4.200` demo 库不能被 `2.4.240` 直接打开，必须先用旧版导出 SQL，再在新版中重建导入。
   - 风险判断：当前版本迁移已完成；剩余风险主要在于后续若继续维护 demo 数据，必须沿用“脚本化重建”路径，而不能再回到直接提交旧版 H2 文件库。

### 二类：前端仍有历史包袱，但需要结合当前基线继续推进

1. Ant Design 5.26.x
   - 现状：主升级已完成，当前问题已从“大版本切换”转成页面回归、compat 壳删除和 token/时间值链稳定化。
   - 更现代替代：当前继续保持 5.x 稳定线，不追 6.x。
   - 当前阻塞：少量 `TreeSelect` 旧 props、主题链和时间组件值链还需要继续收口。

2. Jest 29 测试链
   - 现状：运行、构建与测试转译链都已脱离 CRA，当前已经落到 `jest 29.7.0` 稳定线。
   - 更现代替代：继续评估 Jest 30 或 Vitest，但都不是当前最高优先级。
   - 调研结论：这一项的“老旧性”已经显著下降，剩余重点是测试 warning、`act(...)` 告警和运行速度治理。
   - 风险判断：短期继续保持 Jest 29 更稳，等富文本和时间链路稳定后再评估是否迁到 Vitest。

3. `styled-components 5.3.3` + `styled-components/macro` 兼容壳
   - 现状：源码导入已经从 `styled-components/macro` 全量收口到 `styled-components`，并已继续升级到 `styled-components 6.1.19`；社区 `@types/styled-components` 也已移除。
   - 更现代替代：当前已落到 `styled-components` 6 原生 TypeScript 主线，后续是否继续迁到更轻的样式方案属于另一个方向性决策，不再是“版本老旧”问题。
   - 调研结论：v6 升级的主要适配点不是样式语法本身，而是移除 `@types` 后对本地 `media.ts` 辅助类型和少量 `css` prop 历史写法的收口。
   - 风险判断：这批升级已完成；当前剩余风险主要是测试输出里少量 React warning，与 `styled-components` 版本升级本身无关。

4. `moment`
   - 现状：前端生产代码中的直接 `moment` 调用已清零，项目直接依赖也已退出；`task` 独立打包链产物也已重新生成到 `dayjs`，剩余问题主要是少量局部值对象链继续回归。
   - 更现代替代：`dayjs`、`date-fns`，或结合 AntD 5 时间适配策略重构。
   - 调研结论：Moment 官方文档明确标注项目已进入 maintenance mode，属于典型“还能用，但不建议新项目继续扩展”的库。
   - 风险判断：当前已具备按专题继续收尾的条件；重点应继续放在 DatePicker / RangePicker、控制器配置、分享页和变量页。

5. `react-app-polyfill`
   - 现状：此前仅剩运行时与测试 3 个入口的 `stable` 引用。
   - 更现代替代：按当前 `browserslist` 与 Vite 产物能力运行，个别缺口改为按需 polyfill。
   - 当前动作：本批次已删除 `frontend/src/entryPointFactory.tsx`、`frontend/src/task.ts`、`frontend/src/setupTests.ts` 的显式引入，并移除前端依赖声明。
   - 风险判断：在当前 `browserslist` 不再包含 IE 的前提下，这一步属于低风险兼容层收缩。

### 三类：中长期专项，不适合当作随手升级

1. Shiro 2.0.5
   - 现状：已经兼容当前 Boot 3 主线，但仍是与 Spring Security 并行的一套安全体系。
   - 更现代替代：长期评估迁到 Spring Security 原生能力。
   - 风险判断：收益高，但改动面极大，必须晚于 JWT、HTTP、测试链和前端主栈收口。

2. Druid 1.2.28
   - 现状：服务端主数据源已经移除 `druid-spring-boot-3-starter`，JDBC 数据源工厂也已切到 `DataSourceFactoryHikariImpl`；当前剩余工作不再是“是否迁移”，而是继续做参数语义和专项回归。
   - 更现代替代：HikariCP 统一池化。
   - 风险判断：连接池本身已经开始退出主运行时链，但多数据源参数语义、监控诉求和 provider 行为仍需要后续专项验证。

3. `mybatis-generator-core 1.4.0`
   - 现状：仍在 `core` 中声明，但主要服务代码生成链，不是主运行时链。
   - 更现代替代：升级到更新版本，或把生成器能力迁到独立 profile / 独立工具模块。
   - 风险判断：应按“代码生成链专项治理”处理，不建议和运行时模块混改。

4. `nashorn-core 15.4`
   - 现状：脚本解析链仍依赖 Nashorn 兼容层。
   - 更现代替代：GraalJS，或进一步收缩脚本执行边界。
   - 风险判断：会影响脚本数据源与表达式执行，不适合在当前阶段贸然切换。

### React Router 预迁移第三十九批之外的并行治理：浏览器自动化依赖收口

- `core` 与 `server` 已删除未被实际代码使用的 `phantomjsdriver` 依赖，避免继续保留已退出主流维护链的历史驱动。
- `selenium-java` 已从两个模块各自声明的 3.x 版本统一到父 POM 的 `${selenium.version}`，当前收敛为 `4.31.0`。
- 现有截图代码 `WebUtils` 只使用 `ChromeDriver` / `RemoteWebDriver`，没有任何 PhantomJS 专属分支，因此这一步属于依赖树现代化，不改变当前截图运行语义。

### 并行治理：MySQL JDBC 驱动坐标收口

- `core/pom.xml` 已将旧坐标 `mysql:mysql-connector-java` 切换为当前官方 Maven 坐标 `com.mysql:mysql-connector-j`。
- 驱动类和 JDBC URL 仍保持 `com.mysql.cj.jdbc.Driver` 与 `jdbc:mysql://`，因此这一步是依赖声明现代化，不改变现有连接语义。
- 版本号交由 Spring Boot 父 POM 管理，减少 JDBC 驱动版本继续手写漂移的风险。

### 并行治理：收紧 CRA 与 IE11 残留类型壳

- 运行时与测试入口已删除 `react-app-polyfill/ie11` 显式引入，浏览器兼容策略进一步从 IE11 历史包袱收缩到现代浏览器基线。
- `frontend/src/react-app-env.d.ts` 已去掉 `react-scripts` 类型引用，改为保留项目真实需要的本地声明。
- 为 Vite 链路补齐了 `*.svg` / `ReactComponent` 的显式类型声明，避免继续依赖 CRA 类型包做隐式兜底。

### 并行治理：移除 `react-app-polyfill` 运行时残留

- `frontend/src/entryPointFactory.tsx`、`frontend/src/task.ts`、`frontend/src/setupTests.ts` 已删除 `react-app-polyfill/stable` 显式引入。
- `frontend/package.json` 与 `frontend/package-lock.json` 已移除 `react-app-polyfill` 依赖声明。
- 这一步保留了当前仍在使用的 `core-js/features/string/replace-all`，只删除已不再需要的整包浏览器兼容壳。

### 并行治理：移除无效的前端 JSDoc 与 `eslint-plugin-jsdoc` 壳

- `frontend/.eslintrc.js` 已删除未被任何规则实际使用的 `jsdoc` 插件声明，避免继续安装只支持 Node 12-17 的 `eslint-plugin-jsdoc`。
- `frontend/package.json` 与 `frontend/package-lock.json` 已移除 `eslint-plugin-jsdoc`、`jsdoc`、`docdash` 三个开发依赖，并删除无效的 `doc:html` 脚本。
- `frontend/jsdoc.config.json` 已删除；该配置原本指向不存在的 `types/chartHelper.js`，仓库内也没有生成后的 `types/docs` 产物，因此属于失效文档链而不是仍在使用的工程能力。
- 这一步不改变前端运行、构建、测试或类型检查主链，只是去掉 Node 26 下会产生 `engine` 告警的无效历史壳。

### 并行治理：收口 `package.json` 中重复的 Jest 与 ESLint 历史配置

- `frontend/package.json` 已删除包级 `eslintConfig` 与 `jest` 配置块。
- 当前前端真实生效的入口已经明确收口为：
  - ESLint 使用 `frontend/.eslintrc.js`
  - Jest 使用 `frontend/jest.config.js`
- 删除前已复核：`package.json` 里的 `jest` 配置内容只是对 `frontend/jest.config.js` 的重复镜像，并不提供额外行为。
- 这一步的目标不是升级 Jest 版本，而是先去掉 CRA 时代遗留的重复配置源，降低后续继续推进 Jest 30 或 Vitest 迁移时的配置歧义。

### 并行治理：让 Jest Babel 转译链脱离 `babel-preset-react-app`

- `frontend/jest/babelTransform.js` 已从 `babel-preset-react-app` 切到显式的 `@babel/preset-env`、`@babel/preset-react`、`@babel/preset-typescript` 组合。
- `frontend/package.json` 与 `frontend/package-lock.json` 已新增 `@babel/preset-react`、`@babel/preset-typescript` 直接依赖，并移除 `babel-preset-react-app`。
- 当前 Jest 转译策略已经明确收口为“项目自有 Babel 预设”，不再继续借用 CRA 打包时代的预设黑盒。
- 这一步仍然不改变 Jest 27 版本本身；目标是先把测试转译链与 CRA 历史预设解耦，降低后续升级到 Jest 30 或迁到 Vitest 时的联动复杂度。

### 并行治理：恢复前端 ESLint 主链的显式运行时

- `frontend/.eslintrc.js` 已不再依赖 `react-app` 扩展，改为项目自有的显式配置，当前最小收口包括：
  - `@typescript-eslint/parser`
  - `eslint-plugin-prettier`
  - `eslint-plugin-react`
  - `eslint-plugin-react-hooks`
  - `eslint-plugin-import`
- `frontend/package.json` 与 `frontend/package-lock.json` 已补齐 `eslint`、`@typescript-eslint/parser`、`@typescript-eslint/eslint-plugin`、`eslint-plugin-react`、`eslint-plugin-import` 等前端 lint 运行时依赖。
- 当前 `npm run lint` 已恢复为真实可执行状态，不再出现 `eslint: command not found` 或规则定义缺失错误。
- 在恢复主链后，已先用 `prettier --write src` 清掉全部 `prettier/prettier` 格式告警，并进一步修正两处 `react-hooks/exhaustive-deps` 依赖声明；当前 `npm run lint` 已达到 0 error、0 warning。
- 这意味着前端 lint 主链已经从“不可执行、依赖隐式、warning 大量堆积”收口到可执行、依赖显式、结果干净的稳定状态。

### React Router 预迁移第四十批：切到 Router 6 经典运行时底座

- `frontend/package.json` 已将 `react-router-dom` 切换到 `^6.30.1`，并删除只服务于 v5 的 `@types/react-router-dom`。
- `routerCompat.ts` 已直接回到 `react-router-dom` 的 `BrowserRouter`、`MemoryRouter`、`Link`、`useLocation`、`useNavigate`、`useParams`。
- 删除了只为 v5 `Router history` 桥接而存在的 `routerCompatRuntime.tsx`，兼容层不再依赖 `react-router-dom/node_modules/history/*` 这类脆弱内部路径。
- `useCompatNavigate` 已改为包装 `useNavigate + useLocation`，`location` 改为响应式来源，不再透传自持 history 的静态快照。
- `CompatRoute` 已切到 Router 6 的 `matchPath(pattern, pathname)` 新签名，并把原有 `exact` 语义映射到 v6 的 `end`。

### 并行治理：移除 Enzyme 测试基座

- `frontend/src/setupTests.ts` 已删除 Enzyme 初始化，前端测试基座不再依赖 React 18 的社区 Enzyme adapter。
- `frontend/package.json` 已移除 `enzyme` 与 `@cfaester/enzyme-adapter-react-18`。
- 当前测试文件已全部使用 Testing Library / Jest 风格断言，Enzyme 仅剩历史初始化残留，因此这一步不会改变现有测试写法。

### 并行治理：移除 `react-test-renderer` 历史残留

- `frontend/src/app/pages/NotFoundPage/__tests__/index.test.tsx` 已从 `react-test-renderer` 的跳过快照测试改为 Testing Library 断言。
- `frontend/src/app/pages/NotFoundPage/__tests__/__snapshots__/index.test.tsx.snap` 已删除，不再保留无效 snapshot 噪音。
- `frontend/package.json` 与 lockfile 已移除 `react-test-renderer`、`@types/react-test-renderer`，前端测试栈进一步收口到 `@testing-library/* + jest` 单一路径。

### 并行治理：更新 Stylelint 的 CSS-in-JS 语法栈

- `frontend/.stylelintrc.json` 已把 JS/TS 文件的 `customSyntax` 从 `@stylelint/postcss-css-in-js` 切到 `postcss-styled-syntax`，收口到 `styled-components` 的库专用语法解析。
- 同时把原先仅由 `stylelint-config-styled-components` 提供的少量规则直接内联进本地配置，不再依赖这个历史配置包。
- `frontend/package.json` 与 lockfile 已移除 `@stylelint/postcss-css-in-js`、`postcss-syntax`、`stylelint-config-prettier`、`stylelint-config-styled-components`，并新增 `postcss-styled-syntax`。

### 并行治理：测试执行入口脱离 `craco test`

- `frontend/package.json` 的 `test` 脚本已从 `craco test` 切到 `jest --config jest.config.js`。
- `frontend/jest.config.js` 已改为静态独立配置，测试执行入口已经不再依赖 CRACO 配置生成。
- `frontend/jest/` 已新增本地 `babelTransform.js`、`cssTransform.js`、`fileTransform.js`，测试链不再依赖 `react-scripts/config/jest/*`。
- `frontend/package.json` 与 lockfile 已显式声明 `jest`、`jest-environment-jsdom`、`babel-jest`、`@babel/preset-env`、`@babel/preset-react`、`@babel/preset-typescript`、`identity-obj-proxy`、`jest-watch-typeahead`，并移除 `react-scripts` 与 `babel-preset-react-app`。

### 并行治理：Jest 测试链升级到 29 稳定线

- `frontend/package.json` 与 lockfile 已将 `jest`、`babel-jest`、`jest-environment-jsdom`、`@types/jest` 升级到 29 稳定线，并同步把 `jest-watch-typeahead` 升级到兼容版本。
- 升级过程中顺带暴露出 `@types/node 14.14.31` 与当前 `vite 5.4.x` 的 peer 约束冲突，因此一并提升到现代稳定线，避免继续让前端工具链停留在 Node 14 类型时代。
- 这一步继续保持现有 `babel-jest + jsdom` 运行模型，不引入 `ts-jest` 或 Vitest，避免和现有大批 JSX/TSX 测试一起漂移。
- `frontend/jest.config.js` 新增 `snapshotFormat` 显式配置，用来维持既有 snapshot 输出格式，避免 Jest 29 默认格式变化造成无意义快照噪音。
- 适配过程中还补齐了 Jest 28+ 的自定义 transformer 返回协议，并为 `uuid` 与 `redux/*` 这类在新解析规则下更敏感的导入补了显式映射，避免把升级噪音误判成业务回归。
- 升级后的目标是让前端测试栈从“CRA 时代遗留的 Jest 27”迈到当前仍广泛使用的稳定线，为后续继续评估 Jest 30 或 Vitest 迁移先打稳基础。
- 在本机 `Node 26.0.0 / npm 11.15.0` 下，本批 `npm test -- --runInBand --watchAll=false` 与 `npm run build` 已通过；`npm run checkTs` 仍会暴露前端既有的 Redux thunk dispatch 类型债，这一批没有继续扩大该专项范围。

### 并行治理：收口 React Redux 9 下的 thunk dispatch 类型债

- `frontend/src/app/hooks/useRedux.ts` 已新增 `useAppDispatch` 与 `useAppStore` typed hooks，集中承接 `react-redux 9` 默认 `Dispatch<UnknownAction>` 带来的类型收紧，不再依赖组件侧各自手写 `any` 或宽松推断。
- 前端业务组件、页面和自定义 hooks 中原先直接调用 `useDispatch()` 的位置，已统一切换到 `useAppDispatch()`；这一步只改变类型入口，不改变现有 dispatch 行为。
- `frontend/src/utils/@reduxjs/injectReducer/index.tsx` 已改为使用 typed store hook，并把 HOC 场景下的 `context.store` 显式收口到 `AppStore`，让动态 reducer 注入链和当前 store 扩展类型保持一致。
- `frontend/src/app/pages/DashBoardPage/pages/Board/slice/thunk.ts` 中 `renderedWidgetAsync` 的 thunk state 泛型已从局部 `{ board: BoardState }` 收口到统一 `RootState`，避免 RTK 2 thunk 与全局 `AppDispatch` 的状态签名分叉。
- 这一批解决的是前一批 Jest/类型工具链升级后暴露出来的集中类型债：`dispatch(AsyncThunkAction | thunk function)` 不再被误判成 `UnknownAction`。
- 2026-06-10 验证：`npm run checkTs`、`npm test -- --runInBand --watchAll=false`、`npm run build` 均通过。

### 并行治理：升级 styled-components 到 6 原生 TypeScript 主线

- `frontend/package.json` 与 lockfile 已将 `styled-components` 从 `5.3.3` 升级到 `6.1.19`，并删除 `@types/styled-components`，不再依赖社区类型包补洞。
- `frontend/src/styles/media.ts` 已改用 v6 原生导出的 `RuleSet`、`StyleFunction`、`Interpolation` 等类型，保留项目当前媒体查询 helper 的调用方式不变。
- 本批还顺手收口了 3 处只在旧 `css` prop 类型壳下才能通过的历史写法：
  - `frontend/src/app/components/Configuration.tsx`
  - `frontend/src/app/pages/MainPage/pages/SourcePage/SourceDetailPage/ConfigComponent/FileUpload.tsx`
  - `frontend/src/app/pages/MainPage/pages/VariablePage/VariableForm.tsx`
- 适配策略保持保守：不改主题结构，不动运行时样式语义，只把类型入口和少量依赖 `css` prop 魔法的节点改成显式 styled/class 包装。
- 2026-06-10 验证：`npm run checkTs`、`npm test -- --runInBand --watchAll=false`、`npm run build` 均通过。

### 并行治理：收口 Ant Design 深路径导入

- 前端一批 `antd/lib/*` 深路径导入已切回公开导出路径，覆盖 `message`、`Dropdown`、`Modal.useModal`、`FormInstance`、`InputRef`、`TreeDataNode`、`TableColumnsType` 等运行时或类型入口。
- 这一步重点清理的是只依赖 AntD 内部目录结构的历史导入方式，降低后续升级到 Ant Design 5 时因内部文件路径调整带来的额外噪音。
- 对 Jest 仍敏感的 locale 资源保留在 `antd/lib/locale/*`，避免把前端测试链额外拉进 `node_modules` ESM 转译范围；其它类型和组件入口则优先回到公开导出。
- 本批不改变业务逻辑，也不处理 `visible`、`Menu.*` 或 Dropdown/Modal 的历史 API 语义，只收口 import 边界。
- 2026-06-10 验证：`npm run checkTs`、`npm test -- --runInBand --watchAll=false`、`npm run build` 均通过。

### 并行治理：收口 Node LTS 工程化基线

- `frontend/.nvmrc` 当前已收口到 `v24.0.0`，作为默认 Node LTS 开发基线。
- `frontend/package.json` 的 `engines.node` 当前已收口到 `>=24.0.0`，避免继续把未验证的旧 Node 主版本标成受支持范围。
- `.github/workflows/dev-ut-stage.js.yml` 当前使用 `24.x` 与 `26.x` 双版本矩阵，既覆盖默认 LTS 基线，也保留对 Node 当前线的兼容验证。
- 这一步不引入新的构建工具，也不改变前端运行语义，目标是把“默认用 LTS、同时验证当前线”的 Node 基线真正下沉到仓库级工程化约束里。

### 并行治理：收口图表工作台里的 Ant Design 旧菜单 API

- `frontend/src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction/SizeAction.tsx` 已将 `Slider.tooltipVisible` 切到 `tooltip={{ open: true }}`，对齐 AntD 4.24+ 推荐写法，也为后续 AntD 5 的 `open` 语义收口提前清障。
- 图表工作台字段操作菜单相关文件已从旧的 `Menu.Item` / `Menu.SubMenu` JSX 子节点模式切到 `Menu items` 数组模式，覆盖：
  - `ChartDataConfigSectionActionMenu.tsx`
  - `SortAction/SortAction.tsx`
  - `AggregationAction.tsx`
  - `AggregationLimitAction.tsx`
- 这一批只处理图表工作台内部、且已经由 `Dropdown dropdownRender` 承载的局部菜单，不混入通用 `Popup/MenuListItem` 或自定义组件的 `visible` prop 约定，避免扩大行为面。
- 适配策略保持保守：
  - 仍保留现有 `dropdownRender` 容器与工作台交互结构
  - 只是把菜单项构造收口到 `items` 数组
  - 原有 `onConfigChange` / `onOpenModal` 行为保持不变
- 2026-06-10 验证：
  - `npm run checkTs` 通过
  - `npm test -- --runInBand --watchAll=false` 通过
  - `npm run build` 通过

### 并行治理：收口主导航里的 Ant Design 旧菜单 API

- 主导航和组织切换菜单这一批已从旧的 `Menu.Item` / `Menu.SubMenu` / `Menu.Divider` JSX 子节点模式切到 `Menu items` 数组模式，覆盖：
  - `frontend/src/app/pages/MainPage/Navbar/index.tsx`
  - `frontend/src/app/pages/MainPage/Navbar/OrganizationList.tsx`
- `frontend/src/app/components/Popup/MenuListItem.tsx` 新增了可复用的 `MenuItemContent` 渲染组件，用于保留现有 prefix / suffix / 对齐样式，同时让新的 `items` 数据结构不必再依赖 `Menu.SubMenu` JSX 包装。
- 这一步仍然没有硬切整个 `Popup/MenuListItem` 生态，只先收口“主导航”这一块业务域，避免把所有树、列表、侧边栏右键菜单一次性卷入。
- 保留语义：
  - 语言切换和主题切换仍保持二级子菜单结构
  - 个人资料 / 修改密码 / 退出登录行为保持不变
  - 组织切换列表的当前项高亮与勾选图标保持不变
- 2026-06-10 验证：
  - `npm run checkTs` 通过
  - `npm test -- --runInBand --watchAll=false` 通过
  - `npm run build` 通过

### 并行治理：收口数据视图与通用标题里的 Ant Design 旧菜单 API

- `frontend/src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartDataViewPanel/ChartDataViewPanel.tsx`
  已将数据视图弹出菜单从 `MenuListItem` / `Menu.SubMenu` 组合收口到 `Menu items` 数组模式。
- 这一步保留了原有行为：
  - 搜索字段
  - 新建计算字段
  - 分组 / 取消分组
  - 按名称排序 / 保持原字段顺序
- `frontend/src/app/components/ListTitle/index.tsx`
  已把通用标题栏里的 “更多” 菜单切到 `MenuWrapper items` 模式，不再在该处继续拼 `MenuListItem` JSX 子节点。
- 这一批的意义不只是清一个页面，而是继续把通用菜单构造方式往 `items` 数据模型收敛，为后续剩余侧边栏和树节点菜单复用同一模式做准备。
- 2026-06-10 验证：
  - `npm run checkTs` 通过
  - `npm test -- --runInBand --watchAll=false` 通过
  - `npm run build` 通过

### 并行治理：收口侧边栏树节点里的 Ant Design 旧菜单 API

- `View`、`Source`、`Schedule`、`Viz` 四个主业务侧边栏中的树节点/列表更多菜单，已从 `MenuListItem` + `Menu.Item` JSX 子节点模式切到 `Menu items` 数组模式，覆盖：
  - `frontend/src/app/pages/MainPage/pages/ViewPage/Sidebar/FolderTree.tsx`
  - `frontend/src/app/pages/MainPage/pages/SourcePage/Sidebar/SourceList.tsx`
  - `frontend/src/app/pages/MainPage/pages/SchedulePage/Sidebar/ScheduleList.tsx`
  - `frontend/src/app/pages/MainPage/pages/VizPage/Sidebar/Folders/FolderTree.tsx`
- 这一批复用了前一批在 `frontend/src/app/components/Popup/MenuListItem.tsx` 中补出的 `MenuItemContent`，保留现有 prefix icon、loading icon 和文案布局，不再继续依赖旧的 `Menu.Item` JSX 包装。
- 保留语义：
  - `info`、`saveAs`、`startAnalysis`、`addNewView`、`start`、`stop`、`execute`、`delete/archive` 等原有菜单动作保持不变
  - `Popconfirm` 删除/归档确认逻辑保持不变
  - 调度启动、停止、立即执行以及来源/调度删除中的 loading icon 保持不变
- 这一步的价值是把主应用最常用的一组侧边栏更多菜单也收口到 `items` 数据模型，继续缩小 Ant Design 5 升级时 `Menu children -> items` 的散点存量。
- 2026-06-10 验证：
  - `npm run checkTs` 通过
  - `npm test -- --runInBand --watchAll=false` 通过
  - `npm run build` 通过

### 并行治理：收口侧边栏回收站与故事板里的 Ant Design 旧菜单 API

- 主应用侧边栏里同属导航域的回收站与故事板更多菜单，已继续从 `MenuListItem` / `Menu.Item` JSX 子节点模式切到 `Menu items` 数组模式，覆盖：
  - `frontend/src/app/pages/MainPage/pages/ViewPage/Sidebar/Recycle.tsx`
  - `frontend/src/app/pages/MainPage/pages/SourcePage/Sidebar/Recycle.tsx`
  - `frontend/src/app/pages/MainPage/pages/SchedulePage/Sidebar/Recycle.tsx`
  - `frontend/src/app/pages/MainPage/pages/VizPage/Sidebar/Recycle.tsx`
  - `frontend/src/app/pages/MainPage/pages/VizPage/Sidebar/Storyboards/List.tsx`
- 这一批沿用 `MenuItemContent` 作为统一 label 渲染容器，保留原有 icon、文案、`Popconfirm` 确认和点击事件传播控制，不改变回收站恢复/彻底删除以及故事板编辑/归档逻辑。
- 这一步的价值是把主导航域里剩余一组高频树菜单继续收口到 `items` 数据模型，让后续 Ant Design 5 升级时，侧边栏相关的 `Menu children -> items` 散点进一步减少。
- 2026-06-10 验证：
  - `npm run checkTs` 通过
  - `npm test -- --runInBand --watchAll=false` 通过
  - `npm run build` 通过

### 并行治理：收口数据模型与数据源选择里的 Ant Design 旧菜单 API

- `View` 领域里两处仍在使用 `MenuListItem` 的局部菜单已切到 `Menu items` 数组模式，覆盖：
  - `frontend/src/app/pages/MainPage/pages/ViewPage/Main/Properties/DataModelTree/DataModelComputerFieldNode.tsx`
  - `frontend/src/app/pages/MainPage/pages/ViewPage/Main/StructView/components/SelectDataSource.tsx`
- `DataModelComputerFieldNode` 继续保留原有“编辑/删除计算字段”行为，只把菜单 label 构造收口到 `MenuItemContent`。
- `SelectDataSource` 除了切换到 `items` 模式，还把数据源菜单项 key 从历史数组下标收口到 `source.id`，避免后续再依赖位置索引与 `Menu` 事件 key 的隐式耦合。
- 这一步把 `View` 主工作流内剩余一组局部旧菜单 API 再往前清掉，为后续真正进入 Ant Design 5 升级时减少散点适配。
- 2026-06-10 验证：
  - `npm run checkTs` 通过
  - `npm test -- --runInBand --watchAll=false` 通过
  - `npm run build` 通过

### 并行治理：清理 Popup 菜单兼容壳里的 Ant Design 旧菜单 API

- 在业务页面层的 `MenuListItem` 使用点基本清零后，`Popup` 通用基座里历史保留的 `Menu.Item` / `Menu.SubMenu` 兼容壳也已移除，覆盖：
  - `frontend/src/app/components/Popup/MenuListItem.tsx`
  - `frontend/src/app/components/Popup/MenuWrapper.tsx`
  - `frontend/src/app/components/Popup/index.tsx`
  - `frontend/src/app/components/index.tsx`
- `MenuListItem.tsx` 现在只保留 `MenuItemContent` 这个纯渲染容器，继续承接 prefix / suffix / icon 样式；不再通过兼容组件继续向外扩散旧 `Menu.Item` / `Menu.SubMenu` 写法。
- `MenuWrapper` 也同步收口为纯 `items` / `MenuProps` 包装层，只保留统一 `onClose` 关闭逻辑，不再承接 children 方式的历史菜单拼装。
- 这一步意味着当前前端仓库里，主业务代码和通用 `Popup` 基座都已经不再依赖 `MenuListItem` 这种旧菜单包装壳，后续进入 Ant Design 5 主升级时，`Menu children -> items` 这一类兼容负担会显著减小。
- 2026-06-10 验证：
  - `npm run checkTs` 通过
  - `npm test -- --runInBand --watchAll=false` 通过
  - `npm run build` 通过

### 并行治理：收口分享页认证弹层的 visible/open 兼容层

- 分享页认证链路中直接承接 Ant Design 弹层的两个组件已把对外控制属性从 `visible` 收口到 `open`，覆盖：
  - `frontend/src/app/pages/SharePage/components/PasswordModal.tsx`
  - `frontend/src/app/pages/SharePage/components/ShareLoginModal.tsx`
  - `frontend/src/app/pages/SharePage/Dashboard/ShareDashboardPage.tsx`
  - `frontend/src/app/pages/SharePage/Chart/ShareChartPage.tsx`
  - `frontend/src/app/pages/SharePage/StoryPlayer/ShareStoryPlayerPage.tsx`
- `PasswordModal` 本身已经在内部使用 AntD Modal 的 `open`，这一批进一步把组件外层调用面也统一到了 `open`，不再继续沿用历史 `visible` 命名。
- `ShareLoginModal` 虽然不是 AntD Modal，但它和 `PasswordModal` 共同组成分享认证入口，这里一并把外层开关语义统一到 `open`，减少后续分享页弹层 API 的双轨状态。
- 这一步不改变登录/口令校验逻辑，只是把分享页这组最贴近 AntD Modal 的兼容层提前收拢，为后续继续处理其它真实 `visible -> open` 存量打样。
- 2026-06-10 验证：
  - `npm run checkTs` 通过
  - `npm test -- --runInBand --watchAll=false` 通过
  - `npm run build` 通过

### 并行治理：收口主导航与组织管理弹层的 visible/open 兼容层

- 主导航与组织管理相关的三个 Modal 包装组件已把对外控制属性从 `visible` 收口到 `open`，覆盖：
  - `frontend/src/app/pages/MainPage/OrganizationForm.tsx`
  - `frontend/src/app/pages/MainPage/Navbar/Profile.tsx`
  - `frontend/src/app/pages/MainPage/Navbar/ModifyPassword.tsx`
- 对应调用点也已同步切换到 `open`，覆盖：
  - `frontend/src/app/pages/MainPage/Background.tsx`
  - `frontend/src/app/pages/MainPage/Navbar/OrganizationList.tsx`
  - `frontend/src/app/pages/MainPage/Navbar/index.tsx`
- 这三类弹层内部本来就已经把 AntD Modal 的真实属性写成 `open`，这一步主要是消除组件边界上仍然遗留的 `visible` 命名，让主导航和组织管理链路不再继续保留历史过渡语义。
- 这一步不改动新增组织、编辑资料、修改密码的业务逻辑，只收口控制属性命名，为后续继续推进其它 Modal/Popover/Dropdown 的 `visible -> open` 存量提供同一模式。
- 2026-06-10 验证：
  - `npm run checkTs` 通过
  - `npm test -- --runInBand --watchAll=false` 通过
  - `npm run build` 通过

### 并行治理：收口局部配置弹层的 visible/open 兼容层

- 三个局部配置弹层组件已把对外控制属性从 `visible` 收口到 `open`，覆盖：
  - `frontend/src/app/components/FormGenerator/Customize/ConditionalStyle/add.tsx`
  - `frontend/src/app/components/FormGenerator/Customize/ScorecardConditionalStyle/add.tsx`
  - `frontend/src/app/components/ChartGraph/BasicRichText/RichTextPluginLoader/CustomColor.tsx`
- 对应调用点也已同步切换到 `open`，覆盖：
  - `frontend/src/app/components/FormGenerator/Customize/ConditionalStyle/ConditionalStyle.tsx`
  - `frontend/src/app/components/FormGenerator/Customize/ScorecardConditionalStyle/ScorecardConditionalStyle.tsx`
  - `frontend/src/app/components/ChartGraph/BasicRichText/ChartRichTextAdapter.tsx`
  - `frontend/src/app/pages/DashBoardPage/components/Widgets/RichTextWidget/RichTextWidgetCore.tsx`
- 这三类弹层都属于局部配置与富文本编辑场景，内部本来就已经把 AntD Modal 的真实控制属性写成 `open`；本批只把组件边界上的历史 `visible` 命名清掉，不改条件样式、评分卡条件样式和自定义颜色选择逻辑。
- 这一步把 `visible -> open` 清障从全局入口继续推进到配置面板和富文本工具链，进一步缩小 AntD 5 主升级前需要继续兼容的弹层边界。
- 2026-06-10 验证：
  - `npm run checkTs` 通过
  - `npm test -- --runInBand --watchAll=false` 通过
  - `npm run build` 通过

### 并行治理：继续收口资源保存表单的 open 语义

- 四个资源保存表单上下文与容器已继续把对外控制属性从 `visible` 收口到 `open`，覆盖：
  - `frontend/src/app/pages/MainPage/pages/ViewPage/SaveFormContext.ts`
  - `frontend/src/app/pages/MainPage/pages/ViewPage/SaveForm.tsx`
  - `frontend/src/app/pages/MainPage/pages/SourcePage/SaveFormContext.ts`
  - `frontend/src/app/pages/MainPage/pages/SourcePage/SaveForm.tsx`
  - `frontend/src/app/pages/MainPage/pages/SchedulePage/SaveFormContext.ts`
  - `frontend/src/app/pages/MainPage/pages/SchedulePage/SaveForm.tsx`
  - `frontend/src/app/pages/MainPage/pages/VizPage/SaveFormContext.ts`
  - `frontend/src/app/pages/MainPage/pages/VizPage/SaveForm.tsx`
- 对应调用点也已同步切换到 `open`，覆盖 `View` / `Source` / `Schedule` / `Viz` 四个主业务域的保存、新建、另存、归档等入口。
- 这一步只收口弹层控制语义，不改资源保存、复制、移动等业务逻辑。
- 2026-06-10 验证：
  - `npm run checkTs` 通过
  - `npm test -- --runInBand --watchAll=false` 通过
  - `npm run build` 通过
  - `git diff --check` 通过

### 并行治理：收口头部更多菜单的 dropdownRender 用法

- 三组头部“更多”菜单已从 `dropdownRender={() => <Menu ... />}` 收口到 `Dropdown menu={{ items }}` 数据模式，覆盖：
  - `frontend/src/app/components/VizHeader/VizHeader.tsx`
  - `frontend/src/app/components/VizOperationMenu/VizOperationMenu.tsx`
  - `frontend/src/app/pages/StoryBoardPage/components/StoryHeader.tsx`
  - `frontend/src/app/pages/StoryBoardPage/components/StoryOverLay.tsx`
  - `frontend/src/app/pages/DashBoardPage/components/BoardHeader/TitleHeader.tsx`
  - `frontend/src/app/pages/DashBoardPage/components/BoardDropdownList/BoardDropdownList.tsx`
- 这一步复用了原有菜单项、`Popconfirm`、分享/发布/归档/下载等动作，只把 Dropdown 承载方式切到 AntD 5 主线接口。
- 当前剩余 `dropdownRender` 主要是交互规则关系面板、树选择器扩展面板和图表工作台动作面板，这些属于自定义内容容器，不在这一批强行改成 `menu.items`。
- 2026-06-10 验证：
  - `npm run checkTs` 通过
  - `npm test -- --runInBand --watchAll=false` 通过
  - `npm run build` 通过
  - `git diff --check` 通过

### 并行治理：继续收口局部弹层状态与 Popup 兼容壳

- 以下局部 UI 状态命名已从 `visible` 收口到 `open`，覆盖：
  - `frontend/src/app/components/ColorPicker/ColorPickerPopover.tsx`
  - `frontend/src/app/components/FormGenerator/Customize/ConditionalStyle/ConditionalStyle.tsx`
  - `frontend/src/app/components/FormGenerator/Customize/ScorecardConditionalStyle/ScorecardConditionalStyle.tsx`
  - `frontend/src/app/pages/DashBoardPage/pages/BoardEditor/components/ControllerWidgetPanel/index.tsx`
  - `frontend/src/app/pages/StoryBoardPage/Editor/StoryToolBar.tsx`
  - `frontend/src/app/components/ChartGraph/BasicRichText/ChartRichTextAdapter.tsx`
- `frontend/src/app/components/Popup/index.tsx` 已移除 `onVisibleChange` 兼容壳，只保留 `open` / `onOpenChange` 语义；仓库调用点检索已确认没有业务侧仍在传 `onVisibleChange`。
- 这一步不修改 Board 可见性、Widget 可见性等真实业务字段，只清理 UI 浮层控制态与 Popup 封装边界。
- 2026-06-10 验证：
  - `npm run checkTs` 通过
  - `npm test -- --runInBand --watchAll=false` 通过
  - `npm run build` 通过
  - `npm run lint:css` 通过
  - `npm run lint:style` 通过
  - `git diff --check` 通过

### 并行治理：收口看板选图与故事板加页弹层的 visible/open 兼容层

- 两个直接承接 AntD Modal 的选择弹层组件已把对外控制属性从 `visible` 收口到 `open`，覆盖：
  - `frontend/src/app/pages/StoryBoardPage/components/StoryPageAddModal.tsx`
  - `frontend/src/app/pages/DashBoardPage/pages/BoardEditor/components/ChartSelectModal.tsx`
- 对应调用点也已同步切换到 `open`，覆盖：
  - `frontend/src/app/pages/StoryBoardPage/Editor/StoryToolBar.tsx`
  - `frontend/src/app/pages/DashBoardPage/pages/BoardEditor/components/BoardToolBar/AddChart/AddChart.tsx`
- 这两个弹层内部本来就已经把 AntD Modal 的真实属性写成 `open`；本批只清理组件边界上的历史 `visible` 命名，不改变故事板加页、看板导入已有图表、树搜索和已选中图表禁用等业务行为。
- 这一步把 `visible -> open` 清障继续推进到故事板与看板编辑核心流程，进一步减少 AntD 5 主升级前仍保留的历史弹层兼容边界。

### 并行治理：收口资源保存与变量权限弹层的 visible/open 兼容层

- 四个资源保存弹层组件已把对外控制属性从 `visible` 收口到 `open`，覆盖：
  - `frontend/src/app/pages/MainPage/pages/ViewPage/SaveForm.tsx`
  - `frontend/src/app/pages/MainPage/pages/SchedulePage/SaveForm.tsx`
  - `frontend/src/app/pages/MainPage/pages/SourcePage/SaveForm.tsx`
  - `frontend/src/app/pages/MainPage/pages/VizPage/SaveForm.tsx`
- 两个变量相关弹层组件也已把对外控制属性从 `visible` 收口到 `open`，覆盖：
  - `frontend/src/app/pages/MainPage/pages/VariablePage/VariableForm.tsx`
  - `frontend/src/app/pages/MainPage/pages/VariablePage/SubjectForm/index.tsx`
- 对应调用点已同步切换到 `open`，覆盖：
  - `frontend/src/app/pages/MainPage/pages/ViewPage/Main/Properties/Variables.tsx`
  - `frontend/src/app/pages/MainPage/pages/VariablePage/index.tsx`
- 这一步只清理承接 `ModalForm` / `Modal` 的组件边界命名，不改资源保存、变量默认值、行权限编辑等业务逻辑，也不触碰 `ModalForm` 通用兼容壳里仍需继续兼容的更广泛历史调用面。
- 这一步把 `visible -> open` 清障继续推进到主资源管理与变量权限主链，为后续进一步缩小 `ModalForm` 通用兼容壳的使用面做准备。

### 并行治理：收口 Tabs.TabPane、TreeSelect.TreeNode 与局部成员配置弹层旧 API

- 以下使用 `Tabs.TabPane` 的界面已改为 `Tabs items` 配置，覆盖：
  - `frontend/src/app/pages/MainPage/pages/VariablePage/SubjectForm/index.tsx`
  - `frontend/src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartDataViewPanel/components/ChartComputedFieldSettingPanel.tsx`
  - `frontend/src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction/FilterControlPanel/CategoryConditionConfiguration.tsx`
  - `frontend/src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction/FilterControlPanel/DateConditionConfiguration.tsx`
- `frontend/src/app/pages/MainPage/pages/VizPage/ChartPreview/components/ControllerPanel/components/MultiDropdownListFilter.tsx` 已把 `TreeSelect.TreeNode` 子节点写法改为 `treeData`。
- 以下局部弹层调用边界已继续从 `visible` 收口到 `open`，覆盖：
  - `frontend/src/app/pages/MainPage/pages/SourcePage/SourceDetailPage/ConfigComponent/ArrayConfig.tsx`
  - `frontend/src/app/pages/MainPage/pages/MemberPage/pages/RoleDetailPage/index.tsx`
  - `frontend/src/app/pages/MainPage/pages/MemberPage/pages/RoleDetailPage/MemberForm.tsx`
  - `frontend/src/app/pages/MainPage/pages/MemberPage/Sidebar/MemberList.tsx`
- 这一步集中清理 AntD legacy 标签页与树选择器写法，同时继续缩小成员管理和数据源配置弹层上的历史 `visible` 控制面，为 Ant Design 5 稳定化继续清掉一批明确阻塞项。

### 2026-06-10 全项目老旧技术栈复核结论

这一轮不是只看版本号，而是按“是否仍在主维护线、是否已经被现代替代方案覆盖、是否值得继续在当前架构上扩展”三条标准重新复核。

#### A. 仍建议尽快推进的前端老旧栈

1. `moment`
   - 当前状态：前端生产代码中的直接调用已清零，项目直接依赖也已退出，`task` 独立打包链产物也已切到 `dayjs`。
   - 官方状态：Moment 官方已明确将项目定义为 maintenance mode，只保留稳定性与关键安全/时区数据维护，不再建议新项目继续扩展。
   - 更现代替代：`dayjs` 是最自然的低迁移成本替代；若后续要进一步走函数式与按需组合，也可以评估 `date-fns`。
   - 本仓库判断：当前已经从“大面积散落依赖”进入“专题收尾阶段”；下一步应继续压缩局部值对象链与边界回归。

2. `react-quill 1.3.5`
   - 当前状态：富文本编辑与展示仍依赖旧版 `react-quill` / Quill 1 生态。
   - 路线修正：`react-quill 2.0.0` 官方包仍直接依赖 `quill ^1.3.7`，因此“升 `react-quill 2`”只能算 React 包装层升级，不能当作“已经进入 Quill 2 主线”。
   - 补充证据：当前不仅有 `react-quill` 本体，还叠加了自定义 `TagBlot` / `CalcFieldBlot`、调色板和邮件/仪表板双编辑链路；原先低活跃度的 `quill-image-drop-module`、`quilljs-markdown` 已在项目内用本地模块替代并移除依赖。
   - 2026-06-10 最新推进：已新增本地兼容出口 `frontend/src/app/components/ChartGraph/BasicRichText/quillCompat.ts`，并把图表富文本、仪表板富文本、调度邮件富文本、自定义 blot 与 palette 的 `ReactQuill` / `Quill` / `DeltaStatic` 入口统一收口到这一层。
   - 2026-06-10 补充推进：已新增本地包装层 `frontend/src/app/components/ChartGraph/BasicRichText/RichTextEditor.tsx`，图表富文本、仪表板富文本、调度邮件富文本已不再直接依赖 `ReactQuill` 组件实例，而统一走仓库内 editor handle。
   - 2026-06-10 继续推进：`RichTextEditorHandle` 已补齐 `format`、`getContents`、`getModule`、`on`、`off` 等常用能力，图表富文本、仪表板富文本与自定义调色板不再散用 `getEditor()` 做基础操作。
   - 2026-06-10 再次推进：`RichTextEditorHandle` 已继续补齐 `createMarkdownModule` 与 `insertCalcFieldItem`，图表富文本和仪表板富文本已不再直接 `new MarkdownModule(quill.getEditor(), ...)` 或直接操作 `calcfield` module。
   - 2026-06-10 本轮补充推进：`MarkdownModule`、`ImageDropModule`、`RichTextEditor` 与 `RichTextPluginLoader` 里散落的 `quill` 类型入口已继续收口到 `quillCompat.ts`，后续若替换底层编辑器包，类型和实例出口也能维持单点调整。
   - 本轮收益：业务代码已不再直接依赖 `react-quill` / `quill` 包入口，同时 markdown、图片拖拽/粘贴模块都已切到仓库内维护实现；后续无论升级到 `react-quill 2.x`，还是改接 Quill 2 的其它 React 封装，都可以先在 compat 层、本地模块和 `RichTextEditor` 包装层上集中适配。
   - 更现代替代：先判断是否需要用 `react-quill 2.x` 完成旧 React 包装层收口；若目标是进入真正的 Quill 2 主线，则继续评估仍活跃维护的 Quill 2 React 封装，或基于官方 React playground 方案维护仓库内自有适配层。
   - 本仓库判断：它不一定要先于 AntD 5，但已经属于“仍能跑、后续应替换”的旧编辑器基座，尤其需要关注 React 18 严格模式与自定义 blot 扩展兼容性。

3. `styled-components 6.1.19` 稳定化
   - 当前状态：版本本身已经不老，但样式系统仍需要继续复核 `shouldForwardProp`、iframe target 和测试输出里的样式告警。
   - 更现代替代：当前不需要再换库，重点是把 v6 主线用稳。
   - 本仓库判断：这不是版本升级专题，而是现代化升级后的稳定化专题。

4. `reveal.js 6.0.1`
   - 当前状态：已经不再是明显过时版本，但故事板链路仍是历史插件型依赖。
   - 更现代替代：短期继续保持当前稳定线，长期再评估更贴近 React 的故事方案。
   - 本仓库判断：优先级低于 `moment` 和 `react-quill`，但仍值得在富文本/故事专题里一起复核。

#### B. 后端中长期专项，当前不宜零散混改

1. `Shiro 2.0.5`
   - 当前状态：已经能跑在 Spring Boot 3 / Jakarta 链上，但和 Spring Security 并行维护两套安全体系。
   - 更现代替代：长期方向仍是收口到 Spring Security 原生体系。
   - 2026-06-10 本轮前置清障：`datart-security` 中自定义 OAuth2 过滤器已把 Spring Security 即将移除的 `AntPathRequestMatcher` 替换为 `PathPatternRequestMatcher`，先清掉 Boot 3 / Security 6 主线上的一批已弃用 Web matcher 告警。
   - 2026-06-11 本轮前置清障：权限字符串相关的角色编码、权限位展开和 permission string 拼装已抽到 `PermissionStringCodec`，`server` 多个服务实现不再直接静态引用 `ShiroSecurityManager`；这一批改动不改变运行时鉴权语义，但把未来 `Shiro -> Spring Security` 的第一层服务端耦合面收窄了。
   - 2026-06-11 本轮继续推进：权限缓存层已从 Shiro 认证/授权对象改为中立缓存模型，`SimpleAuthorizationInfo` / `SimpleAuthenticationInfo` 只保留在 `DatartRealm` 内部适配，不再向 `security.manager` 通用层外溢。
   - 2026-06-11 本轮继续推进：`ShiroSecurityManager` 已不再直接持有 `SecurityUtils/Subject` 作为主流程实现细节，而是改通过 `SecuritySubjectFacade` 访问当前主体、登录、登出、角色检查和权限检查；Shiro API 现已进一步收缩到 `shiro` 子包适配层。
   - 2026-06-11 本轮继续推进：认证 token 语义已从 `DatartRealm` / `PasswordCredentialsMatcher` 中抽到 `AuthenticationTokenAdapter`，用户名解析和凭证匹配统一经由 `ShiroAuthenticationTokenAdapter` 完成，继续压缩了 realm 与 matcher 对 Datart 业务 token 语义的理解范围。
   - 2026-06-11 本轮继续推进：认证/授权数据装配已进一步抽到 `AuthenticationAssembler` / `AuthorizationAssembler`，`DatartRealm` 不再自己理解用户查找、组织 owner 隐式权限和资源权限拼装细节，Shiro realm 本身继续收缩为更薄的适配壳。
   - 验收证据：`mvn -o -pl security -am -DskipTests -Dmaven.compiler.showDeprecation=true compile` 与 `mvn -o -pl server -am -DskipTests -Dmaven.compiler.showDeprecation=true compile` 已通过。
   - 本仓库判断：Shiro 不是“坏掉了”，但它已经属于架构级历史包袱；适合单列安全专项，而不是在基础库升级时顺手搀带。

2. `Druid 1.2.28`
   - 当前状态：这一项已开始退出。服务端主数据源已回到 Spring Boot 默认连接池选择策略，JDBC provider 也已切到本地 `DataSourceFactoryHikariImpl`。
   - 更现代替代：主方向仍是 HikariCP 统一池化。
   - 2026-06-10 最新推进：`server` 已移除 `druid-spring-boot-3-starter`，`datart-jdbc-data-provider` 已移除 `druid` 依赖并改接 `HikariCP`，`config/profiles/application-config.yml` 的 Druid 类型残留也已清理。
   - 验收证据：`mvn -pl server -am -DskipTests package` 与 `bash ./scripts/check-demo-health.sh` 已通过，demo 安装包可在 `http://127.0.0.1:8080/api/v1/sys/info` 返回成功。
   - 本仓库判断：迁移已经从“前置设计”进入“第一批功能落地并完成启动级验证”；剩余工作主要是参数语义和专项回归，而不是再停留在专项评估。

3. `nashorn-core 15.4`
   - 当前状态：脚本执行与表达式能力仍依赖 Nashorn 兼容层。
   - 更现代替代：GraalJS。
   - 2026-06-10 本轮推进：`JavascriptUtils` 已从 `NashornScriptEngineFactory` 直绑改为 JSR-223 标准脚本引擎发现，默认按 `graal.js -> js -> JavaScript -> javascript -> nashorn` 顺序尝试，也支持通过 `-Ddatart.script.engine=...` 或环境变量 `DATART_SCRIPT_ENGINE` 显式指定引擎名。`nashorn-core` 同步降为运行期依赖，编译期不再直接耦合 Nashorn 专有 API。
   - 本轮收益：当前仍可继续使用 Nashorn 维持兼容，但后续切换到 GraalJS 时，主改动面可以收敛到依赖与启动参数，而不是继续改业务调用链。
   - 本仓库判断：这是明显老旧但高风险的运行时基座，必须放在单独专项中，且先把脚本执行边界盘清楚。

4. `mybatis-generator-core 1.4.0`
   - 当前状态：主要服务代码生成，不是主运行时链。
   - 更现代替代：升级生成器版本，或把生成链迁出主模块到独立 profile / 工具模块。
   - 2026-06-10 最新推进：`core/pom.xml` 已把 `mybatis-generator-core` 从默认运行时依赖降为 `provided`，并把生成器执行入口收口到 `mybatis-generator` Maven profile。这样主源码中的 `MybatisGeneratorPlugin` 仍可参与默认编译，但运行时产物不再携带该依赖。
   - 本仓库判断：这类依赖收口的优先级虽然低于前端主栈清障和后端运行时基础设施改造，但它属于低风险、可直接落地的现代化治理项，已经适合先完成。

5. `calcite-core 1.26.0`
   - 当前状态：仍是 SQL 解析与 provider 能力的关键底座，也带入部分旧传递依赖生态。
   - 更现代替代：先评估上游兼容性再升级到较新的稳定线。
   - 本仓库判断：它不是简单“提版本号”的库，升级会牵动 SQL 解析行为，必须配套 JDBC provider 回归验证。

6. HTTP 客户端双栈
   - 当前状态：生产代码中的实际 HTTP 请求已经统一落在 Apache HttpClient 5，主要使用点集中在 `datart-security` 和 `datart-http-data-provider`。
   - 2026-06-10 本轮推进：已确认 `core/pom.xml` 中遗留的 `okhttp` 没有任何生产代码使用面，并已从主运行时依赖中移除。
   - 2026-06-10 继续推进：`HttpDataFetcher` 与 `WeChartOauth2Client` 已把 HttpClient 5 的 `executeOpen(...)` 替换为标准 `execute(...)` 关闭语义，避免继续依赖已弃用的“返回开放响应对象”接口。
   - 本轮收益：HTTP 客户端主链继续朝单栈收敛，后续无需再为未使用的第二套客户端维护额外依赖和传递风险。

#### C. 已经收口到现代主线，可从“老旧清单”里降级处理的项

- JDK `21`
- Spring Boot `3.5.12`
- Spring Cloud `2025.0.1`
- Selenium `4.31.0`
- JWT `0.12.7`
- Apache HttpClient `5.5`
- `okhttp` 已退出主运行时依赖
- Apache POI `5.5.1`
- H2 `2.4.240`
- React `18.3.x`
- React Router `6.30.x`
- Redux Toolkit `2.x`
- React Redux `9.x`
- Ant Design `5.26.2`
- `styled-components 6.1.19`
- Jest `29.7.0`
- Vite `5.x`
- `video-react` 已退出

#### D. 推荐的后续推进顺序

1. `moment -> dayjs`
   - 继续收口局部值对象链，并保持 `build:task` / `build` / `checkTs` 同步回归。
   - 当前复核后的重点仍包括：
     - `frontend/src/app/pages/MainPage/pages/SchedulePage/types.ts`、`frontend/src/app/pages/MainPage/pages/SchedulePage/utils.ts`
     - `frontend/src/app/pages/DashBoardPage/pages/BoardEditor/components/ControllerWidgetPanel/types.ts`
     - `frontend/src/app/pages/MainPage/pages/VizPage/ChartPreview/components/ControllerPanel/components/TimeFilter.tsx`
     - `frontend/src/app/pages/MainPage/pages/VizPage/ChartPreview/components/ControllerPanel/components/RangeTimePickerFilter.tsx`
       这些文件和相关链路仍需要结合当前 AntD 5 时间组件行为继续回归，而不是只看 import 是否已清零。
2. 富文本专题
   - `react-quill` 继续评估升级或替代。
   - 目前外部 `quill-image-drop-module`、`quilljs-markdown` 都已退出，业务层也已切到本地 `RichTextEditor` 包装层；下一步重点改为核验本地 `MarkdownModule`、`RichTextEditor`、自定义 `TagBlot` / `CalcFieldBlot` 对 Quill 2 的兼容性，再决定是先升 `react-quill 2.x` 作为中间态，还是直接切到真正的 Quill 2 React 封装。
   - 在真正切 Quill 2 之前，继续把少量仍需直接透传底层实例的链路压缩到 `RichTextEditorHandle`，避免再次出现业务页散改；当前 `getEditor()` / `getModule()` 已确认从业务层调用面清零。
   - 本地 `calcfield` 扩展模块已经从历史编译产物形态恢复成 TypeScript 源码形态，后续应继续对照 Quill 2 的模块与 blot API 做行为级核验，而不是在 `//@ts-nocheck` 状态下盲升依赖。
   - 插件注册入口也已收敛到 `RichTextPluginLoader` 单点；类型与实例出口则继续收敛在 `quillCompat.ts`。后续若切换到 `react-quill 2.x` 或直接切 Quill 2 React 封装，应优先只在 compat 层和这个 bootstrap 点上调整，而不是回到页面组件里散改 `Quill.register(...)`。
   - 每次尝试升级前后，都至少回归 `npm run checkTs`、`npm run build`、`npm run build:task`。
3. 前端工具链老旧主版本治理
   - 当前还偏旧但不应先于业务主链升级的项包括：
     - `Rollup 2`
     - `stylelint 17`
     - `Prettier 3`
   - `ESLint 9` flat config 主链已经完成，本节后续重点不再是迁移配置模型，而是继续治理 warning 存量与局部辅助链。
   - 这些栈都有更现代的替代或更高主线，但它们对业务功能的直接收益低于时间体系和富文本专题；更合理的顺序是先保持当前稳定，再分批做“工具链升级 + 存量 warning 清理 + 配置收口”。
4. `styled-components 6` 稳定化复核
   - 补查 `shouldForwardProp`、iframe / popup target 和测试输出告警。
5. 故事播放链路复核
   - `reveal.js 6.x` 保持当前版本，但继续确认编辑、预览、分享页行为。

### 2026-06-10 编译层弃用 API 清障补充

- `datart-http-data-provider` 已把 `Class.newInstance()` 与 HttpClient 5 的 `executeOpen(...)` 替换为 `getDeclaredConstructor().newInstance()` 和标准 `execute(...)`。
- `datart-security` 的 `WeChartOauth2Client` 已同步切到 HttpClient 5 的标准 `execute(...)` 调用。
- `server` 里的 `APPLICATION_JSON_UTF8_VALUE` 已收口为 `APPLICATION_JSON_VALUE`。
- `core/src/main/resources/mybatis-generator/generatorConfig.xml` 已把旧驱动类名 `com.mysql.jdbc.Driver` 替换为 `com.mysql.cj.jdbc.Driver`。
6. 后端长期专项
   - `Shiro -> Spring Security`
   - `Nashorn -> GraalJS`
7. 代码生成链专项
   - 继续把 `mybatis-generator-core` 维持在独立 profile / 工具链内，默认运行时不再直接携带。
   - 后续若仍需保留生成能力，再单独评估是否升级生成器版本和驱动适配。
8. 数据源迁移收尾
   - 在当前已切到 Hikari 且 demo 启动链已验证通过的基础上，继续补齐数据库迁移链、连接参数映射和 provider 行为专项回归。

### 并行治理：删除 CRACO 回退外壳

- `frontend/package.json` 已删除 `start:cra`、`build:cra`、`eject` 等只服务 CRA/CRACO 的历史脚本。
- `frontend/package.json` 与 lockfile 已移除 `@craco/craco`，并同步删除只服务旧 webpack/CRACO 外壳的 `cross-env`、`monaco-editor-webpack-plugin`、`webpackbar`、`webpack-cli`、`@types/webpack`、`@types/webpack-env`。
- `frontend/craco.config.js` 已删除，前端运行与构建主链只保留 Vite 配置作为单一入口。

### 并行治理：收口 BoardEditor 的 Node events 残留

- `frontend/src/app/pages/DashBoardPage/pages/BoardEditor/slice/events.ts` 已从 Node `events` 切换为浏览器友好的本地事件总线实现。
- 新增 `frontend/src/app/pages/DashBoardPage/pages/BoardEditor/slice/__tests__/events.test.ts`，覆盖 `widgetMove`、`widgetMoveEnd`、`boardScroll` 的订阅、取消订阅和按 boardId 隔离行为。
- 重新验证 `npm run build` 后，Vite 构建日志中不再出现 `events` 模块被浏览器外置化的告警。

### 并行治理：fastjson 低风险使用面第一批收口

- `server/src/main/java/datart/server/common/JsParserUtils.java` 已改为使用 Jackson 反序列化 `DownloadCreateParam`。
- `security/src/main/java/datart/security/util/AESUtil.java` 已改为使用 Jackson 进行加密前序列化和解密后反序列化。
- `server/src/main/java/datart/server/service/impl/ShareServiceImpl.java`、`server/src/main/java/datart/server/service/impl/VariableServiceImpl.java` 已将字符串数组/集合这类低风险 JSON 路径切到 Jackson。
- `core/src/main/java/datart/core/data/provider/ExecuteParam.java`、`core/src/main/java/datart/core/data/provider/QueryScript.java` 已改为复用 `DataProvider.MAPPER` 生成 JSON 字符串和查询 key。
- 这一步之后，`fastjson` 在后端仍主要残留于高风险的 `JSONObject` / `JSONArray` 动态树模型处理路径，后续需要分批继续替换。

### 并行治理：Web 层默认 JSON 输出回收至 Jackson

- `server/src/main/java/datart/server/config/WebMvcConfig.java` 已删除 `FastJsonHttpMessageConverter` 注册逻辑，Web 层默认 JSON 输出重新回到 Spring Boot 3 的 Jackson 主链。
- 为了保持原先 `SerializerFeature.WriteEnumUsingToString` 的关键语义，`WebMvcConfig` 中新增了 `Jackson2ObjectMapperBuilderCustomizer`，显式开启 `SerializationFeature.WRITE_ENUMS_USING_TO_STRING`。
- `spring.jackson.date-format`、`time-zone` 和 `FAIL_ON_EMPTY_BEANS` 等现有配置继续由 `server/src/main/resources/application.yml` 生效。

### 并行治理：移除 @JSONField 配置绑定路径

- `server/src/main/java/datart/server/config/CustomConfigValidateBean.java` 已删除 `@JSONField` 注解，改为显式声明配置 key 常量。
- `server/src/main/java/datart/server/config/CustomPropertiesValidate.java` 已不再依赖 `fastjson` 将 `Properties` 映射到校验 bean，而是改为本地显式映射。
- 校验失败时返回给用户的配置项名称仍保持 `datasource.ip`、`datasource.port`、`datasource.database`、`datasource.username`、`datasource.password` 这组外部配置 key，不改变原有提示语义。

### 并行治理：导出表头与数字格式树模型切到 Jackson

- `server/src/main/java/datart/server/base/dto/chart/ChartColumn.java` 已将 `format` 字段从 `fastjson JSONObject` 切到 Jackson `JsonNode`，数字、货币、百分比、科学计数法导出格式改为使用 `ObjectMapper.convertValue` 解析。
- `server/src/main/java/datart/server/common/PoiConvertUtils.java` 已移除 `JSONValidator`、`JSON.parseObject`、`JSON.parseArray`，图表导出配置改为使用 Jackson 反序列化。
- `tableHeaders` 分组表头配置现在同时兼容运行期已经反序列化成 `List<?>` 的值，以及历史字符串 JSON 值；解析失败时继续回退到无分组表头导出，保持原有兜底语义。
- 2026-06-09 验证：`mvn -pl server -am -DskipTests compile` 通过。

### 并行治理：视图模型 schema 与数据源配置加密切到 Jackson

- `server/src/main/java/datart/server/service/impl/DataProviderServiceImpl.java` 的 `parseSchema` 已移除 `fastjson JSONObject/JSONArray` 与 `Feature.OrderedField` 依赖，改为用 Jackson `JsonNode` 解析 `columns`、`hierarchy` 和历史 beta 版本的 schema 结构。
- `name` 字段的历史兼容逻辑仍保留：既支持数组值，也支持单元素数组里嵌套 JSON 数组字符串的旧数据形态；解析失败时继续按普通列名处理。
- `server/src/main/java/datart/server/service/impl/SourceServiceImpl.java` 的 `encryptConfig` 已改为基于 Jackson `ObjectNode` 遍历和回写配置项，加密字段仍保持原有 `ENC(...)` 语义，不改变外部配置格式。
- 2026-06-09 验证：`mvn -pl server -am -DskipTests compile` 通过。

### fastjson 后续残留重点

- 2026-06-09 当前状态：后端生产代码中的 `fastjson` 使用点已经清零。
- 目前剩余的是测试代码中的 `org.json` 使用，例如：
  - `security/src/test/java/datart/security/test/jwt/JwkSetCreator.java`
- 这部分不再阻塞后端生产主链从 `fastjson` 退出，但后续仍建议和 JWT 技术栈统一时一起清理。

### 并行治理：移除 fastjson 直接依赖

- `core/pom.xml` 已删除 `com.alibaba:fastjson:1.2.83` 直接依赖。
- 2026-06-09 复核：后端生产代码检索 `fastjson` 结果为空，当前仅剩测试代码中的 `org.json` 使用，不再有生产主链的 `fastjson` 依赖入口。

### 并行治理：收口 JWT 双栈中的未使用依赖

- `security/pom.xml` 已删除未使用的 `com.auth0:java-jwt:3.7.0` 依赖。
- 2026-06-10 当前状态：JWT 主链已从 `io.jsonwebtoken:jjwt:0.7.0` 升级到 `0.12.7` 三件套（`jjwt-api`、`jjwt-impl`、`jjwt-jackson`）。
- `security/src/main/java/datart/security/util/JwtUtils.java` 已切到 `Jwts.builder().claims(...).subject(...).expiration(...).signWith(key)` 与 `Jwts.parser().verifyWith(key).build().parseSignedClaims(...)` 新 API。
- 为兼容默认短字符串 `datart.security.token.secret`，当前策略是：
  - 新签发 token 使用 `SHA-256(secret + 固定 salt)` 派生出的 256-bit HMAC key；
  - 历史短 secret token 解析失败后，回退到 legacy `SecretKeySpec(secret, "HmacSHA256")` 再验一次，保证已有激活链接、邀请链接和登录 token 不被一次性打断。
- `security/src/main/java/datart/security/util/JwkUtils.java` 保留了历史 JWK 验签能力，并把弱 HMAC key 与 `secp256k1` 这类 legacy 测试资源的兼容收敛在本地验签实现里，不把这些历史约束继续反向污染生产登录主链。
- 2026-06-10 验证：
  - `mvn -pl security -am -Dtest=datart.security.test.jwt.TestJwkParse -Dsurefire.failIfNoSpecifiedTests=false test` 通过。
  - `mvn -pl server -am -DskipTests compile` 通过。

### 并行治理：升级 commons-csv、移除 commons-text 并收口 AspectJ 版本

- `core/pom.xml` 已将 `org.apache.commons:commons-csv` 从 `1.8` 升级到 `1.14.1`。
- `core/pom.xml` 已删除未使用的 `org.apache.commons:commons-text:1.9` 直接依赖；当前主代码检索 `commons-text` 结果为空。
- `core/pom.xml` 已将 `org.aspectj:aspectjweaver` 从 `1.9.8.M1` 升级到 `1.9.25.1`，去掉 milestone 版本残留。
- 当前 `aspectj` 生产使用面已确认只剩 [server/src/main/java/datart/server/config/AccessLogAdvice.java](/Users/chencongyu/WorkHome/VSProjects/open-project/datart/server/src/main/java/datart/server/config/AccessLogAdvice.java)，升级后切面编译和主链行为保持不变。
- 2026-06-10 依赖树复核：
  - `commons-csv` 已统一到 `1.14.1`
  - `commons-text` 已不再出现在 `server` 联动依赖树中
  - `aspectjweaver` 已统一到 `1.9.25.1`
- 2026-06-10 验证：`mvn -pl server -am -DskipTests compile` 与 `mvn -pl server -am dependency:tree -Dincludes=org.apache.commons:commons-csv,org.apache.commons:commons-text,org.aspectj:aspectjweaver -DskipTests` 通过。

### 并行治理：清理生产代码中的 Guava 依赖

- `core/src/main/java/datart/core/common/NamingUtils.java` 已新增本地命名转换工具，用于替代原来的 `CaseFormat`。
- 以下生产代码使用点已替换为 JDK 21 原生实现：
  - `core/src/main/java/datart/core/mappers/ext/CRUDMapper.java`
  - `server/src/main/java/datart/server/service/BaseCRUDService.java`
  - `server/src/main/java/datart/server/job/ScheduleJob.java`
  - `server/src/main/java/datart/server/common/PoiConvertUtils.java`
  - `server/src/main/java/datart/server/service/impl/DataProviderServiceImpl.java`
  - `server/src/main/java/datart/server/service/impl/ShareServiceImpl.java`
  - `security/src/main/java/datart/security/util/JwkUtils.java`
  - `security/src/main/java/datart/security/oauth2/CustomOauth2Client.java`
  - `data-providers/data-provider-base/src/main/java/datart/data/provider/calcite/SqlValidateUtils.java`
  - `data-providers/data-provider-base/src/main/java/datart/data/provider/calcite/dialect/SqlStdOperatorSupport.java`
  - `data-providers/data-provider-base/src/main/java/datart/data/provider/script/SqlStringUtils.java`
- `core/pom.xml` 已删除 `com.google.guava:guava:21.0` 直连依赖。
- 迁移过程中确认：`core` 模块截图链上的 Selenium 编译面仍需要 Guava 类型签名，因此不能继续把 `selenium-java` 的 Guava 传递依赖手工排除；当前保留 Selenium 自带的现代 Guava 传递依赖，不再用旧版直连 Guava 兜底。
- 2026-06-10 复核：
  - 生产代码检索 `com.google.common` / `CaseFormat` / `Lists` / `Sets` / `ImmutableSet` / `Iterables` 结果已清零，仅剩 `jdbc-data-provider` 测试代码使用点。
  - `mvn -pl server -am dependency:tree -Dincludes=com.google.guava:guava -DskipTests` 显示当前剩余来源只有：
    - `org.apache.calcite:calcite-core -> guava:29.0-jre`
    - `org.seleniumhq.selenium:selenium-remote-driver -> guava:33.4.6-jre`
  - `mvn -pl server -am -DskipTests compile` 通过；该命令联动触发的 `npm run build:task` 与 `vite build` 也通过。

### 并行治理：升级 Apache POI 并补齐 PDF 导出显式依赖

- `core/pom.xml` 已将 `org.apache.poi:poi-ooxml` 从 `5.0.0` 升级到 `5.5.1`。
- `core/src/test/java/datart/core/common/POIUtilsTest.java` 已新增定向回归测试，覆盖 `POIUtils.createEmpty -> withSheet -> save -> loadExcel` 这条项目自有 Excel 生成/读取主链。
- 升级过程中确认：`server/src/main/java/datart/server/service/impl/AttachmentPdfServiceImpl.java` 一直直接使用 PDFBox API 生成 PDF，但项目此前没有显式声明 `org.apache.pdfbox:pdfbox`，只是依赖旧传递依赖“侥幸可编译”。
- `server/pom.xml` 现已补充 `org.apache.pdfbox:pdfbox:3.0.7` 直接依赖，保证 PDF 导出链不再依赖 POI / Batik 的偶然传递行为。
- 2026-06-10 依赖树复核：
  - `mvn -pl server -am dependency:tree -Dincludes=org.apache.poi:poi-ooxml,org.apache.pdfbox:pdfbox -DskipTests` 显示：
    - `datart-core -> org.apache.poi:poi-ooxml:5.5.1`
    - `datart-server -> org.apache.pdfbox:pdfbox:3.0.7`
- 2026-06-10 验证：
  - `mvn -pl core -am -Dtest=datart.core.common.POIUtilsTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过。
  - `mvn -pl server -am -DskipTests compile` 通过；该命令联动触发的 `npm run build:task` 与 `vite build` 也通过。

### 并行治理：升级 H2 到 2.4 并迁移 demo 数据库

- 父 `pom.xml` 已将 `com.h2database:h2` 从 `1.4.200` 升级到 `2.4.240`，并已通过 `mvn -pl server -am dependency:tree -Dincludes=com.h2database:h2 -DskipTests` 复核多模块统一收口到同一版本。
- `data-providers/data-provider-base/src/main/java/datart/data/provider/local/LocalDB.java` 已去掉 H2 2.x 不再支持的连接参数 `LOG=0` 与 `UNDO_LOG=0`，保留项目实际仍可用的 `MODE=MySQL`、`DATABASE_TO_UPPER=false`、`CASE_INSENSITIVE_IDENTIFIERS=TRUE`、`CACHE_SIZE=65536`、`LOCK_MODE=0`。
- `LocalDB.init()` 增加了 `Application.getContext() == null` 的保护，避免纯单测场景在静态初始化阶段因为 Spring 上下文尚未就绪而抛空指针。
- `data-providers/data-provider-base/src/test/java/datart/data/provider/local/LocalDBTest.java` 已新增最小回归测试，覆盖 dataframe 注册到内存 H2 后执行 `SELECT name, score ORDER BY score DESC` 的本地 SQL 主链。
- `bin/h2/datart.demo.mv.db` 已按 H2 官方兼容路径完成重建：先用 `1.4.200` 导出旧 demo 库 SQL，再用 `2.4.240` 回灌生成新的仓库内 demo 数据库文件。
- 本批关键结论：
  - 旧 demo 库无法被 H2 2.4.240 直接打开，报错为文件格式过旧，不能只改依赖版本号。
  - demo profile 使用的是仓库内现成的 H2 文件库，而不是启动时自动跑 migration SQL，所以必须同步迁移仓库中的样例数据库文件。
- 2026-06-10 验证：
  - `mvn -pl data-providers/data-provider-base -am -Dtest=datart.data.provider.local.LocalDBTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过。
  - `mvn -pl server -am -DskipTests compile` 通过。
  - 新版 `bin/h2/datart.demo.mv.db` 已在 H2 2.4.240 下验证可读，结果为 `tables=39`、`users=1`。

### 并行治理：移除未使用的 cglib 直接依赖

- `core/pom.xml` 已删除未使用的 `cglib:cglib:3.3.0` 直接依赖。
- 2026-06-09 复核：主代码检索 `cglib` / `Enhancer` / `MethodInterceptor` 结果为空，当前项目没有实际运行时代码依赖该库。

### 并行治理：移除 asciitable 旧展示依赖

- `core/src/main/java/datart/core/migration/DatabaseMigration.java` 已去除 `de.vandermeer.asciitable`，数据库迁移版本输出改为本地 ASCII 表格渲染逻辑。
- `core/pom.xml` 已删除 `de.vandermeer:asciitable:0.3.2` 直接依赖。
- 这一步不改变迁移流程和成功/失败判定，只替换控制台展示实现。

### 剩余老基础库评估：mybatis-generator-core 与 httpclient

- `org.mybatis.generator:mybatis-generator-core:1.4.0`
  - 当前判断：属于生成期依赖，不是主运行时链。
  - 证据：
    - `core/src/main/java/datart/core/common/MybatisGeneratorPlugin.java`
    - `core/src/main/resources/mybatis-generator/generatorConfig.xml`
  - 2026-06-10 最新状态：`core/pom.xml` 已把它从默认 dependencies 移到 `mybatis-generator` profile 下的 `mybatis-generator-maven-plugin` 依赖中。
  - 处理策略：默认运行时继续不携带该依赖；后续结合驱动类名、JDK 21 与实体生成策略继续治理独立生成链。

- `org.apache.httpcomponents:httpclient:4.5.14`
  - 当前判断：属于明确的运行时主链依赖，分布在 HTTP 数据源拉取和第三方 OAuth 登录流程中。
  - 主要使用点：
    - `data-providers/http-data-provider/src/main/java/datart/data/provider/HttpDataFetcher.java`
    - `data-providers/http-data-provider/src/main/java/datart/data/provider/ResponseJsonParser.java`
    - `security/src/main/java/datart/security/oauth2/WeChartOauth2Client.java`
    - `security/src/main/java/datart/security/oauth2/DingTalkOauth2Client.java`
  - 风险判断：若升级到 `httpclient5`，需要同步改造请求/响应模型、URI 构造、SSL 配置与实体读取 API，不适合作为“顺手清理”批次，需要单独专项推进。

### 并行治理：运行时 HTTP 客户端迁到 HttpClient 5

- `core/pom.xml` 已将 `org.apache.httpcomponents:httpclient:4.5.14` 替换为 `org.apache.httpcomponents.client5:httpclient5:5.5`。
- `data-providers/http-data-provider/src/main/java/datart/data/provider/HttpDataFetcher.java` 已切到 HttpClient 5 classic API：
  - 请求基类从 `HttpRequestBase` 切到 `HttpUriRequestBase`
  - `RequestConfig` 超时改为 `Timeout.ofMilliseconds(...)`
  - `URIBuilder` 切到 `org.apache.hc.core5.net.URIBuilder`
  - 响应读取改为 `ClassicHttpResponse`
- `data-providers/http-data-provider/src/main/java/datart/data/provider/HttpResponseParser.java` 与 `ResponseJsonParser.java` 已同步切到 HttpClient 5 的 `ClassicHttpResponse` / `EntityUtils`。
- `security/src/main/java/datart/security/oauth2/WeChartOauth2Client.java` 与 `security/src/main/java/datart/security/oauth2/DingTalkOauth2Client.java` 已完成 5.x 包名迁移，OAuth 授权 URI 拼装改为使用新的 `URIBuilder`。
- 2026-06-10 验证：`mvn -pl server -am -DskipTests compile` 通过。
- 2026-06-10 依赖树复核：项目自有主运行时代码已经切到 `httpclient5`，但 `data-provider-base -> calcite-core -> avatica-core` 仍会传递带入 `org.apache.httpcomponents:httpclient:4.5.9:runtime`；这属于上游分析栈残留，不能视为“仓库已经完全清零 HttpClient 4”。
- 当前兼容取舍：
  - 本批先保证主运行时链完成 5.x 升级并保持构建通过。
  - 旧实现里对“自签名证书 + 忽略 hostname 校验”的放宽 SSL 策略，当前未继续沿用到新实现；若 HTTP 数据源或第三方 OAuth 需要保留这一行为，应在后续单独做一批“HttpClient 5 TLS 策略补齐”并补回归验证。

### 并行治理：清理 commons-lang 2 与直连 commons-io 1.x 使用点

- `core/pom.xml` 已删除 `commons-lang:commons-lang:2.6` 与 `commons-io:commons-io:1.3.1` 两个直接依赖。
- `core/src/main/java/datart/core/common/CSVParse.java`、`data-providers/data-provider-base/src/main/java/datart/data/provider/DefaultDataProvider.java` 已切到 `org.apache.commons.lang3.math.NumberUtils`。
- `core/src/main/java/datart/core/entity/poi/format/PoiNumFormat.java` 已将 `NumberUtils.isNumber(...)` 替换为 `NumberUtils.isCreatable(...)`，保持原有数值判定语义。
- `core/src/main/java/datart/core/data/provider/sql/ColumnOperator.java` 已去除 `commons-lang` 的 `ArrayUtils` 依赖，改为直接使用 JDK `System.arraycopy(...)`。
- `data-providers/file-data-provider/src/main/java/datart/data/provider/FileDataProvider.java` 已去除 `commons-io` 的 `FilenameUtils` 依赖，改为使用 `java.nio.file.Path` 处理文件名和父目录。
- `data-providers/data-provider-base/pom.xml` 已对 `calcite-core` 排除 `aggdesigner-algorithm`，切断其传递引入的 `commons-lang:commons-lang:2.4` 运行时依赖；当前项目代码中未发现对该聚合设计器算法包的直接使用。
- 2026-06-09 验证：`mvn -pl server -am -DskipTests compile` 通过；`mvn -pl server -am dependency:tree -Dincludes=commons-lang:commons-lang -DskipTests` 不再出现 `commons-lang` 依赖链。
### 并行治理：统一 commons-io 传递依赖版本

- 父 `pom.xml` 的 `dependencyManagement` 已新增 `commons-io:commons-io:2.22.0`，用于把多模块中的旧 `commons-io` 传递依赖统一收口到现代稳定版本。
- 这一步优先保持 `poi-ooxml:5.0.0` 与 `calcite-core:1.26.0` 现有功能链不变，只做版本对齐，不直接切断其 SVG/图像和 SQL 解析相关依赖路径。
- 2026-06-09 验证：
  - `org.apache.calcite:calcite-core:1.26.0 -> commons-io:commons-io:2.22.0`
  - `org.apache.poi:poi-ooxml:5.0.0 -> org.apache.xmlgraphics:xmlgraphics-commons:2.4 -> commons-io:commons-io:2.22.0`
  - `mvn -pl server -am dependency:tree -Dincludes=commons-io:commons-io -DskipTests` 已不再出现 `commons-io:1.3.1` 或 `commons-io:2.4`
  - `mvn -pl server -am -DskipTests compile` 通过

### 并行治理：HTTP JSON 解析与 widget 配置链切到 Jackson

- `data-providers/http-data-provider/src/main/java/datart/data/provider/ResponseJsonParser.java` 已移除 `fastjson`，改为使用 Jackson `JsonNode` 解析 HTTP 响应 JSON。
- 现有行为保持不变：支持顶层数组响应、支持按 `a.b.c` 点路径定位目标数组、嵌套对象/数组值仍序列化为 JSON 字符串写入结果集、未显式给列定义时仍按首行对象自动推断 schema。
- `server/src/main/java/datart/server/base/dto/chart/WidgetConfig.java` 已将 `content` 从 `fastjson JSONObject` 切到 Jackson `JsonNode`。
- `server/src/main/java/datart/server/service/impl/VizServiceImpl.java` 读取 widget 图表配置时，已改为使用 Jackson 反序列化 `WidgetConfig`。
- 2026-06-09 验证：`mvn -pl server -am -DskipTests compile` 通过。

### 并行治理：OAuth 返回解析与脚本模型切到 Jackson

- `security/src/main/java/datart/security/oauth2/WeChartOauth2Client.java` 已移除 `fastjson`，微信 OAuth 的 access token 和用户信息返回解析改为使用 Jackson `JsonNode`。
- `data-providers/data-provider-base/src/main/java/datart/data/provider/script/StructScript.java` 已将脚本 JSON 反序列化入口统一到 Jackson。
- `data-providers/data-provider-base/src/main/java/datart/data/provider/calcite/StructScriptProcessor.java` 已改为复用 `StructScript.ofScript()`，脚本模型解析入口收敛为单点。
- `data-providers/jdbc-data-provider/src/main/java/datart/data/provider/jdbc/adapters/JdbcDataProviderAdapter.java` 生成 query key 时，`includeColumns` 与 `pageInfo` 的 JSON 序列化已从 `fastjson` 切到 Jackson。
- 2026-06-09 验证：`mvn -pl server -am -DskipTests compile` 通过。

### 并行治理：清理服务端最后两处 fastjson 低风险残留

- `server/src/main/java/datart/server/service/impl/UserServiceImpl.java` 与 `server/src/main/java/datart/server/service/impl/ExternalRegisterServiceImpl.java` 已不再把 `OAuth2User.getAttributes()` 包成 `fastjson JSONObject`，而是直接对原始属性 `Map` 使用 `JsonPath.read(...)`。
- 这一步完成后，后端生产代码中的 `fastjson` 使用面已经收口为 0，后续只剩测试代码中的 `org.json` 残留需要按优先级处理。
- 2026-06-09 验证：`mvn -pl server -am -DskipTests compile` 通过；对 `security/src/main/java`、`server/src/main/java`、`data-providers/*/src/main/java`、`core/src/main/java` 的 `fastjson` 检索结果为空。

## 2026-06-11 项目级技术栈审计结论

这一节补的是“review 整个项目之后，哪些技术栈仍偏老、现代替代方案是什么、当前已经做到哪一步”。它和上面的迁移记录不是重复关系，而是当前阶段的可执行审计结论。

### 已完成主链现代化、但仍需稳定化回归的栈

| 领域 | 旧栈 / 历史问题 | 当前状态 | 现代化结论 |
| --- | --- | --- | --- |
| Java 运行时 | JDK 8/11 时代遗留假设 | 已统一到 `JDK 21` | 主线已完成，后续只做兼容性回归 |
| Spring 主框架 | Boot 2.x / Spring 5 时代接口 | 已统一到 `Spring Boot 3.5.12`、`Spring Cloud 2025.0.1` | 主线已完成 |
| 前端构建 | `CRA / CRACO` | 已切到 `Vite 5`，CRA/CRACO 退出主工作流 | 主线已完成 |
| React 主栈 | React 16/17 时期兼容思路 | 已统一到 `React 18.3.x`、`React Router 6.30.x`、`RTK 2.x`、`React Redux 9.x` | 主线已完成 |
| UI 主栈 | `Ant Design 4` | 已进入 `Ant Design 5.26.x` 稳定线 | 主线已完成，仍需页面回归 |
| 连接池 | `Druid` | 运行链已切到 `HikariCP` | 主链已完成，后续只剩行为/监控补验 |
| HTTP 客户端 | `HttpClient 4 + okhttp` 并存 | 生产代码主运行链已统一到 `HttpClient 5.5`，`okhttp` 已退出主运行时依赖；5.5 的 `responseHandler` / JWK 远程加载兼容入口也已同步收口 | 主链已完成，后续只做 TLS/行为回归 |

### 仍偏老、需要继续专项升级或替代的栈

| 领域 | 当前栈 | 老旧点判断 | 更现代替代 | 当前建议优先级 |
| --- | --- | --- | --- | --- |
| 安全框架 | `Shiro 2.0.5` | 能跑，但与当前 Spring 生态割裂，属于长期架构包袱 | `Spring Security` 原生体系 | 高 |
| 富文本 | `react-quill 2.0.0` / Quill 旧类型耦合 | React 包装层已升，但仍保留旧类型路径和深度定制插件负担 | 继续评估真正的 Quill 2 React 封装或自有适配层 | 高 |
| 时间体系尾部 | `dayjs` 已落地主链，但仍有值链回归专题 | 主逻辑已现代化，剩余是控件值对象与页面回归 | `dayjs` 单栈收口 | 高 |
| 脚本引擎 | `nashorn-core 15.4` 仍保留运行期兜底 | JDK 主线已不再内建 Nashorn | `GraalJS` / 标准 JSR-223 发现链 | 中高 |
| SQL 解析/方言 | `calcite-core 1.26.0` | 明显偏老，且带入历史依赖与弃用 API | 保守升级到较新稳定线，或继续封装隔离 | 中高 |
| 代码生成链 | `mybatis-generator-core 1.4.0` | 工具链偏旧，但已退出主运行时 | 独立 profile / 独立生成链治理 | 中 |
| 浏览器自动化 | 当前主链已是 Selenium 4，但历史思维仍偏导出脚本式 | 依赖层已经不老，产品能力层仍可继续现代化 | `Playwright` 或继续巩固 `Selenium 4 + Chromium` | 中 |
| 测试主栈 | `Vitest 4 + jsdom 29` | 主链已统一，Jest 运行时与 transform 已退出 | 保持 `Vitest` 单栈，并继续治理 warning / peer 提示 | 已完成主链收口 |

### 本轮继续收口后的剩余后端重点

截至 `2026-06-11`，本轮又清掉了一批低风险弃用入口，包括：

- `Class.newInstance()` -> `getDeclaredConstructor().newInstance()`
- `Jackson PropertyNamingStrategy.KEBAB_CASE` -> `PropertyNamingStrategies.KEBAB_CASE`
- 多处 `commons-lang3` 旧字符串工具入口，改回 JDK 基础字符串处理或本地等价实现
- `RandomStringUtils.randomNumeric/randomAscii` -> `RandomStringUtils.secure().nextNumeric/nextAscii`
- `JsonNode.fields()` -> `JsonNode.properties().iterator()`

本轮离线编译后，剩余 warning 已进一步收敛到以下几类：

1. `javacc` 生成代码提示
   - `data-providers/data-provider-base/target/generated-sources/javacc/.../SimpleCharStream.java`
   - 现状：属于生成物 warning
   - 下一步：先不手改生成代码，后续若要清零，优先从生成链版本或模板入手

2. Lombok `equals/hashCode` 提示
   - `server/src/main/java/datart/server/base/transfer/model/*`
   - `server/src/main/java/datart/server/base/dto/UserProfile.java`
   - 2026-06-11 最新状态：已批量补 `@EqualsAndHashCode(callSuper = false)`，server 编译阶段这批 Lombok 提示已退出
   - 结论：不是主链阻塞项，已完成本轮低风险收口

3. 常规 unchecked 提示
   - 现状：主要是历史泛型收紧不足
   - 下一步：按模块风险分批治理，不作为当前现代化主线阻塞项

### 当前验收口径

截至本节更新时，以下链路已再次验证通过：

- `mvn -o -pl data-providers/jdbc-data-provider -am -DskipTests -Dmaven.compiler.showDeprecation=true compile`
- `mvn -o -pl security -am -DskipTests -Dmaven.compiler.showDeprecation=true compile`
- `mvn -o -pl server -am -DskipTests -Dmaven.compiler.showDeprecation=true compile`

其中 `server` 链会联动执行：

- `frontend npm install --legacy-peer-deps`
- `npm run build:task`
- `vite build`

因此这次不是“只改文档”或“只做静态替换”，而是已经在完整联动构建下确认当前修改未破坏主链运行与打包。

### 2026-06-11 本轮继续推进：前端低风险工具链与测试辅助依赖收口

- 本轮实际落地：
  - `frontend/package.json` / `frontend/package-lock.json`
  - 升级并验证以下低风险工具链依赖：
    - `@babel/preset-env` -> `7.29.7`
    - `@babel/preset-react` -> `7.29.7`
    - `@babel/preset-typescript` -> `7.29.7`
    - `@testing-library/jest-dom` -> `6.9.1`
    - `@types/lodash` -> `4.17.24`
    - `babel-plugin-styled-components` -> `2.3.0`
    - `jest-canvas-mock` -> `2.5.2`
    - `jest-styled-components` -> `7.4.0`
    - `less` -> `4.6.4`
    - `source-map-explorer` -> `2.5.3`
  - 同时移除了已被 `@testing-library/jest-dom` 内置类型覆盖的 `@types/testing-library__jest-dom`

- 本轮收益：
  - 前端测试辅助链和 Babel 转译预设不再停留在明显更早的 `7.15.x` / `jest-dom 5.x` 状态。
  - `jest-dom` 类型来源回到官方包自身，减少一层历史 DefinitelyTyped 兼容壳。
  - 这批改动没有引入业务侧 API 迁移，适合作为“工具链现代化”的低风险第一批收口。

- 本轮 review 结论：
  - `ESLint 9` 已完成本轮单列专题收口；后续代码规范链的重点转为 warning 治理、stylelint/Prettier 稳定化和按触达文件渐进收口。
  - `stylelint 17`、`Prettier 3` 仍值得继续收口，但它们已经进入“需要单列专题或按触达文件渐进治理”的范围，不能和当前主业务专题随手混升。
  - `React 19`、`React Router 7`、`Ant Design 6`、`Vite 8` 也都存在更高的新主线，但对当前仓库来说改动面和回归面都明显更大，现阶段不应作为低风险顺手升级项。
  - 因此当前更合理的策略是：
    1. 继续把测试链、富文本、安全体系这些高耦合专题拆开做；
    2. 对工具链先做同主线或低风险依赖收口；
    3. 等测试和富文本链路更稳定后，再继续推进 stylelint / Prettier 存量治理与其余低风险工具链收口。

- 本轮验证结果：
  - `npm run checkTs` 通过。
  - `npm run build` 通过。
  - `npm run test:ci -- src/__tests__/task.test.ts src/styles/theme/__tests__/ThemeProvider.test.tsx src/app/models/__tests__/ChartSelectionManager.test.ts` 通过。

- 当前仍未完成项：
- 代码规范链已推进到 `ESLint 9 / stylelint 17 / Prettier 3`，其中 ESLint 9 flat config 主链已完成，Prettier 3 历史格式差异与无效 lint 注释按触达文件渐进收口。

### 2026-06-11 本轮继续推进：收口 Vitest setup 历史兼容壳

- 本轮实际落地：
  - `frontend/vitest.setup.ts`
  - `frontend/src/setupTests.ts`
  - `frontend/src/__tests__/MockMatchMedia.ts`
  - 若干已迁到 Vitest 的测试文件
  - `frontend/tsconfig.json`

- 本轮收口内容：
  - `vitest.setup.ts` 不再人为注入 `globalThis.jest = vi` 这一层兼容壳。
  - `jest-dom` 的 Vitest 入口已切到官方推荐的 `@testing-library/jest-dom/vitest`。
  - `MockMatchMedia` 不再兜底读取 `jest.fn`，统一直接使用 `vi.fn`。
  - 已迁到 Vitest 的测试文件不再重复局部 `import '@testing-library/jest-dom'`，由统一 setup 负责注入。
  - `tsconfig.json` 已显式补入 `vitest/globals` 与 `@testing-library/jest-dom` 类型，保证 `checkTs` 与 Vitest 运行时对齐。

- 本轮收益：
  - 仓库对“Vitest 运行时里伪装一个 Jest 全局对象”的依赖继续缩小，后续迁移剩余 Jest 存量测试时边界会更清晰。
  - `jest-dom` 的 matcher 注册入口从“Jest 默认入口 + 本地兼容理解”收口到官方 Vitest 入口，减少 setup 歧义。
  - 这一步不改变生产代码行为，只收测试链运行时和类型边界。

- 本轮验证结果：
  - `npm run checkTs` 通过。
  - `npm run test:ci -- src/__tests__/task.test.ts src/styles/theme/__tests__/ThemeProvider.test.tsx src/app/models/__tests__/ChartSelectionManager.test.ts src/app/components/ChartIFrameContainer/__tests__/ChartIFrameContainer.test.jsx src/app/components/FormGenerator/__tests__/BasicFont.test.tsx src/app/components/FormGenerator/__tests__/BasicColorSelector.test.jsx src/app/components/FormGenerator/__tests__/BasicCheckbox.test.jsx` 通过。

- 当前仍未完成项：
  - 本轮测试输出中的 React warning 主要来自旧组件自身实现和历史测试写法，不是这次 Vitest setup 收口新引入的问题，后续应按组件专题继续治理。

### 2026-06-11 本轮继续推进：Vite 6 稳定线对齐，消除 Vitest 主链 peer 错位

- 本轮实际落地：
  - `frontend/package.json`
  - `frontend/package-lock.json`
  - `frontend/src/app/components/FormGenerator/__tests__/BasicCheckbox.test.jsx`
  - `frontend/src/app/components/FormGenerator/__tests__/BasicColorSelector.test.jsx`
  - `frontend/src/app/components/ChartIFrameContainer/__tests__/ChartIFrameContainer.test.jsx`

- 本轮收口内容：
  - 前端主构建链已从 `Vite 5.4.21` 收口到 `Vite 6.4.3` 稳定线。
  - 这次升级不追逐更高主版本，只解决当前 `Vitest 4.1.8` 与 `Vite 5` 之间的主链 peer 错位。
  - 为兼容 `Vite 6` 下测试文件不再依赖隐式 React 注入的行为，补了 3 个历史 `.jsx` 测试文件的显式 `React` 导入；未修改对应生产代码逻辑。

- 本轮收益：
  - `Vitest 4` 与主构建链的版本关系恢复一致，不再维持“测试栈要求 `Vite 6+`、仓库却停在 `Vite 5`”的错位状态。
  - `Node 26` 下的 `checkTs`、测试和构建链均继续通过，说明这次升级符合“较新的稳定版”而非盲目追新。
  - 这一步继续保持 Vite 单栈方向，不引入新的构建工具或新的测试主链。

- 本轮验证结果：
  - 在本机 `Node 26.0.0 / npm 11.15.0` 下：
    - `npm run checkTs` 通过。
    - `npm run build:task` 通过。
    - `npm run build` 通过。
    - `npm run test:ci` 通过，结果为 `87 passed`, `665 passed | 4 skipped`。

- 当前仍未完成项：
  - 全量测试输出中仍存在一批历史 React warning 与上游 sourcemap warning，这些不是本轮 `Vite 6` 升级新引入的问题，后续应在组件和依赖治理专题中继续消化。

### 2026-06-11 本轮继续推进：Jest 运行链退出，Vitest 单栈完成主链收口

- 本轮实际落地：
  - 删除：
    - `frontend/jest.config.js`
    - `frontend/jest/babelTransform.js`
    - `frontend/jest/cssTransform.js`
    - `frontend/jest/fileTransform.js`
  - `frontend/package.json` 已移除：
    - `test:jest`
    - `jest`
    - `babel-jest`
    - `@types/jest`
    - `jest-environment-jsdom`
    - `jest-styled-components`
    - `jest-watch-typeahead`
    - `jest-canvas-mock`
    - `identity-obj-proxy`
  - 为保证 `Vitest` 的 `jsdom` 环境继续稳定运行，新增直接依赖：
    - `jsdom 29.1.1`

- 本轮收益：
  - 前端测试主链不再维持“Vitest 运行、Jest 配置和依赖还在”的双栈假象。
  - 测试入口、环境与锁文件都已收口到 `Vitest + jsdom`，Node 26 下的运行模型更直接，也更符合当前 Vite 主构建链。
  - `npm install` 后锁文件中已移除整批 Jest 运行时依赖，仓库测试工具链负担明显收缩。

- 本轮验证结果：
  - `npm run checkTs` 通过。
  - `npm run lint:css` 通过。
  - `npm run lint:style` 通过。
  - `npm run test:ci` 通过，结果为 `87 passed`, `665 passed | 4 skipped`。
  - 额外检索确认：源码与配置层已不存在 `test:jest`、`jest.config.js`、`frontend/jest/`、`babel-jest`、`@types/jest` 等主链入口。

- 本轮结论：
  - “测试栈”这一项可以从“`Vitest 4` 主链 + `Jest 29` 存量，进行中”更新为“`Vitest 4 + jsdom 29`，主链已完成收口”。
  - 后续测试专题的重点已从“是否退出 Jest”切换为“如何治理历史 warning、是否升级到与未来 `Vite 6+` 更一致的版本组合”。

### 2026-06-11 本轮继续推进：ESLint 9 flat config 主链收口

- 本轮实际落地：
  - `frontend/package.json`
  - `frontend/package-lock.json`
  - `frontend/eslint.config.mjs`
  - 删除 `frontend/.eslintrc.js`

- 本轮收口内容：
  - 前端代码规范主链已从 `ESLint 8` 升级到 `ESLint 9` 稳定线。
  - 配置模型已从历史 `.eslintrc.js` 迁移到 `eslint.config.mjs` flat config。
  - `eslint-plugin-react-hooks` 已升级到支持 ESLint 9 的稳定线，并保留仓库原本实际启用的两条 Hooks 规则：`rules-of-hooks` 与 `exhaustive-deps`。
  - 这次迁移没有顺手启用 React Compiler 相关的新规则，也没有把旧仓库一次性拉进大规模类型规则治理；目标是先完成配置模型和主版本收口。
  - `package.json` 里的 `eslint` 脚本已从旧的 `eslint --ext ...` 收口到 flat config 模式下的 `eslint`，避免继续依赖旧 CLI 习惯。

- 本轮收益：
  - 代码规范链与当前维护主线对齐，不再停留在 ESLint 8 的旧配置模型。
  - Node 26 下的 lint 主链保持可运行，符合“兼容优先、稳定优先”的现代化改造原则。
  - 这一步把风险控制在工具链边界，没有扩大到生产代码行为改造。

- 本轮验证结果：
  - 在本机 `Node 26.0.0 / npm 11.15.0` 下：
    - `npm run lint` 通过。
    - `npm run lint:style` 通过。
    - `npm run test:ci` 通过，结果为 `87 passed`, `665 passed | 4 skipped`。
  - 当前 `npm run lint` 输出仍存在历史 `prettier/prettier` warning、`react-hooks/exhaustive-deps` warning 和无效 `eslint-disable` 注释 warning，但已经没有新的 error 级阻塞。

- 当前仍未完成项：
  - 代码规范链已完成主版本和配置模型收口，后续重点转为按触达文件渐进清理历史 warning，而不是继续做大范围规则扩张。

### 2026-06-11 本轮继续推进：清理无效 eslint-disable 历史抑制

- 本轮实际落地：
  - `frontend/src/utils/persistence.ts`
  - `frontend/src/utils/object.ts`
  - `frontend/src/app/components/FormGenerator/Customize/PivotSheetTheme/theme.ts`
  - `frontend/src/app/components/ReactFrameComponent/Frame.jsx`
  - `frontend/src/app/components/Split.tsx`
  - `frontend/src/app/components/SplitPane/index.tsx`
  - `frontend/src/app/models/PluginChartLoader.ts`
  - `frontend/src/app/pages/DashBoardPage/components/WidgetComponents/WidgetDndHandleMask.tsx`
  - `frontend/src/app/pages/MainPage/slice/utils.ts`
  - `frontend/src/utils/@reduxjs/injectReducer/reducerInjectors.ts`
  - `frontend/src/utils/utils.ts`

- 本轮收口内容：
  - 只处理 `ESLint 9` 主链下已经失效的 `eslint-disable` / `eslint-disable-next-line` 注释，不扩展到业务逻辑调整。
  - 删除的都是 `lint` 已明确判定为 `Unused eslint-disable directive` 的历史抑制，包括：
    - 匿名默认导出、`eqeqeq`、`no-param-reassign`、`no-new-func`
    - `no-underscore-dangle`、`react/destructuring-assignment`
    - `react/no-unused-prop-types`、`no-bitwise`、`no-empty`
    - 若干无目标规则名的块级 `eslint-disable`
  - 这一步不改变运行时行为，只是让代码规范链和当前真实规则集保持一致。

- 本轮验证结果：
  - 在本机 `Node 26.0.0 / npm 11.15.0` 下：
    - `npm run lint` 通过。
  - `lint` 总 warning 从 `422` 降到 `407`，且没有新增 error。

- 当前仍未完成项：
  - 代码规范链后续仍以低风险 warning 渐进治理为主，优先顺序保持为：无效抑制清理 -> Prettier 历史差异 -> Hooks 依赖提示。

### 2026-06-11 本轮继续推进：收口 BasicRichText 目录 Prettier 历史格式差异

- 本轮实际落地：
  - `frontend/src/app/components/ChartGraph/BasicRichText/ChartRichTextAdapter.tsx`
  - `frontend/src/app/components/ChartGraph/BasicRichText/RichTextEditor.tsx`
  - `frontend/src/app/components/ChartGraph/BasicRichText/RichTextPluginLoader/CustomColor.tsx`
  - `frontend/src/app/components/ChartGraph/BasicRichText/RichTextPluginLoader/index.ts`
  - `frontend/src/app/components/ChartGraph/BasicRichText/modules/ImageDropModule.ts`
  - `frontend/src/app/components/ChartGraph/BasicRichText/modules/MarkdownModule.ts`

- 本轮收口内容：
  - 只处理 `BasicRichText` 文件簇内的 `prettier/prettier` 历史格式差异，不改业务逻辑、不改富文本 API、不调整 Quill 兼容策略。
  - 处理方式为使用前端本地已安装的 `prettier` 对目标文件做机械格式化，避免依赖网络拉包，也避免手工改出额外噪音。
  - 这一步属于代码规范链的低风险收口，不改变运行时行为。

- 本轮验证结果：
  - 在本机 `Node 26.0.0 / npm 11.15.0` 下：
    - `npm run lint` 通过。
  - `lint` 总 warning 从 `403` 降到 `374`，且 `BasicRichText` 目录相关的本轮目标 `prettier` warning 已退出。

- 当前仍未完成项：
  - 代码规范链后续继续按文件簇处理 Prettier 历史格式差异，优先选择同模块、纯机械、可单独回归的小批次。

### 2026-06-11 本轮继续推进：收口 BasicTableChart / PivotSheetChart 历史格式差异

- 本轮实际落地：
  - `frontend/src/app/components/ChartGraph/BasicTableChart/BasicTableChart.tsx`
  - `frontend/src/app/components/ChartGraph/BasicTableChart/TableComponents.tsx`
  - `frontend/src/app/components/ChartGraph/PivotSheetChart/PivotSheetChart.tsx`

- 本轮收口内容：
  - 只处理 `BasicTableChart / PivotSheetChart` 文件簇内的 `prettier/prettier` 历史格式差异，不改表格渲染逻辑、不改透视表配置行为、不碰交互逻辑。
  - 处理方式仍然是使用前端本地已安装的 `prettier` 对目标文件做机械格式化，保证这一步没有网络依赖，也没有手工改动噪音。
  - 这一步属于代码规范链的低风险收口，不改变运行时行为。

- 本轮验证结果：
  - 在本机 `Node 26.0.0 / npm 11.15.0` 下：
    - `npm run lint` 通过。
  - `lint` 总 warning 从 `374` 降到 `350`，且本轮目标文件相关的 `prettier` warning 已退出。

- 当前仍未完成项：
  - 代码规范链后续继续按文件簇收口 Prettier 历史格式差异，优先处理同模块、小范围、可单独回归的纯机械改动。

### 2026-06-11 本轮继续推进：收口一组通用前端小文件历史格式差异

- 本轮实际落地：
  - `frontend/src/app/components/ColorPicker/utils.ts`
  - `frontend/src/app/components/Configuration.tsx`
  - `frontend/src/app/components/Confirm.tsx`
  - `frontend/src/app/components/Popup/MenuWrapper.tsx`
  - `frontend/src/app/components/ToolbarButton.tsx`
  - `frontend/src/app/hooks/useResizeObserver.ts`

- 本轮收口内容：
  - 只处理一组体量较小、边界清晰的通用前端文件中的 `prettier/prettier` 历史格式差异，不改业务逻辑、不调整组件 API、不改 hooks 行为。
  - 处理方式仍然是使用前端本地已安装的 `prettier` 对目标文件做机械格式化，确保不依赖网络，也不引入手工修改噪音。
  - 这一步依然属于代码规范链的低风险收口，不改变运行时行为。

- 本轮验证结果：
  - 在本机 `Node 26.0.0 / npm 11.15.0` 下：
    - `npm run lint` 通过。
  - `lint` 总 warning 从 `350` 降到 `342`，且本轮目标文件相关的 `prettier` warning 已退出。

- 当前仍未完成项：
  - 代码规范链后续继续优先处理同类“小文件、小范围、纯机械”的 Prettier 存量，再视情况回到中等体量文件簇。

### 2026-06-11 本轮继续推进：收口 StoryBoardPage 文件簇历史格式差异

- 本轮实际落地：
  - `frontend/src/app/pages/StoryBoardPage/components/StoryOverLay.tsx`
  - `frontend/src/app/pages/StoryBoardPage/components/StoryPageAddModal.tsx`
  - `frontend/src/app/pages/StoryBoardPage/slice/types.ts`
  - `frontend/src/app/pages/StoryBoardPage/utils.ts`

- 本轮收口内容：
  - 只处理 `StoryBoardPage` 文件簇内的 `prettier/prettier` 历史格式差异，不改故事板交互逻辑、不改类型设计、不碰页面行为。
  - 处理方式仍然是使用前端本地已安装的 `prettier` 对目标文件做机械格式化，保证这一步没有网络依赖，也没有手工改动噪音。
  - 这一步依然属于代码规范链的低风险收口，不改变运行时行为。

- 本轮验证结果：
  - 在本机 `Node 26.0.0 / npm 11.15.0` 下：
    - `npm run lint` 通过。
  - `lint` 总 warning 从 `342` 降到 `326`，且本轮目标文件相关的 `prettier` warning 已退出。

- 当前仍未完成项：
  - 代码规范链后续继续按文件簇推进 Prettier 存量清理，优先保留“纯机械、易回归、低行为风险”的节奏。

### 2026-06-11 本轮继续推进：收口 DashBoardPage 类型与小组件历史格式差异

- 本轮实际落地：
  - `frontend/src/app/pages/DashBoardPage/components/WidgetComponents/DropHolder.tsx`
  - `frontend/src/app/pages/DashBoardPage/components/Widgets/ControllerWidget/Controller/CheckboxGroupController.tsx`
  - `frontend/src/app/pages/DashBoardPage/constants.ts`
  - `frontend/src/app/pages/DashBoardPage/pages/Board/slice/asyncActions.ts`
  - `frontend/src/app/pages/DashBoardPage/pages/Board/slice/types.ts`
  - `frontend/src/app/pages/DashBoardPage/types/widgetTypes.ts`

- 本轮收口内容：
  - 只处理 `DashBoardPage` 小组件、常量和类型文件中的 `prettier/prettier` 历史格式差异，不改仪表板交互逻辑、不改数据结构语义、不碰运行时行为。
  - 处理方式仍然是使用前端本地已安装的 `prettier` 对目标文件做机械格式化，避免网络依赖，也避免掺入手工逻辑改动。
  - 本轮改动全部停留在前端代码规范链，不涉及 Java 服务端实现，因此不会引入额外的 `JDK 21` 兼容面变化。

- 本轮验证结果：
  - 在本机 `Node 26.0.0 / npm 11.15.0` 下：
    - `npm run lint` 通过。
  - `lint` 总 warning 从 `326` 降到 `308`，且本轮目标文件相关的 `prettier` warning 已退出。

- 当前仍未完成项：
  - 代码规范链后续继续优先处理同类“小文件、小范围、纯机械”的 Prettier 存量。
  - 现阶段仍暂不进入高风险内部命名、数据迁移常量和后端稳定标识调整。

### 2026-06-11 本轮继续推进：收口 DashBoardPage 下拉菜单与配置面板历史格式差异

- 本轮实际落地：
  - `frontend/src/app/pages/DashBoardPage/components/BoardDropdownList/BoardDropdownList.tsx`
  - `frontend/src/app/pages/DashBoardPage/pages/BoardEditor/components/ControllerWidgetPanel/ControllerConfig/ValuesSetter/ValuesOptionsSetter/CustomOptions.tsx`
  - `frontend/src/app/pages/DashBoardPage/pages/BoardEditor/components/SlideSetting/WidgetConfigPanel.tsx`

- 本轮收口内容：
  - 只处理 `DashBoardPage` 下拉菜单与配置面板文件中的 `prettier/prettier` 历史格式差异，不改菜单项逻辑、不改控制器配置语义、不改面板运行行为。
  - 处理方式仍然是使用前端本地已安装的 `prettier` 对目标文件做机械格式化，避免网络依赖，也避免引入与业务无关的手工变更。
  - 本轮改动仍然只落在前端代码规范链，不涉及后端实现和内部稳定标识，因此不会扩大 `JDK 21` 兼容面风险。

- 本轮验证结果：
  - 在本机 `Node 26.0.0 / npm 11.15.0` 下：
    - `npm run lint` 通过。
  - `lint` 总 warning 从 `308` 降到 `302`，且本轮目标文件相关的 `prettier` warning 已退出。

- 当前仍未完成项：
  - 代码规范链后续继续优先处理 `DashBoardPage` 和相邻页面中同类“小文件、小范围、纯机械”的 Prettier 存量。
  - 现阶段仍暂不进入 hooks 依赖修复、业务逻辑调整和高风险内部命名重构。

### 2026-06-11 本轮继续推进：收口 DashBoardPage widget 工具链大文件历史格式差异

- 本轮实际落地：
  - `frontend/src/app/pages/DashBoardPage/utils/widget.ts`

- 本轮收口内容：
  - 只处理 `DashBoardPage` 的 `widget.ts` 中 `prettier/prettier` 历史格式差异，不改 widget 映射逻辑、不改 computed field 合并语义、不改容器复制和序列化行为。
  - 虽然目标文件体量较大，但本轮问题全部是 `reduce`、长条件表达式和长对象字面量的机械换行与缩进，不涉及类型重构或运行时逻辑修复。
  - 处理方式仍然是使用前端本地已安装的 `prettier` 进行单文件机械格式化，保持无网络依赖，也不引入手工逻辑调整。

- 本轮验证结果：
  - 在本机 `Node 26.0.0 / npm 11.15.0` 下：
    - `npm run lint` 通过。
  - `lint` 总 warning 从 `302` 降到 `255`，且 `frontend/src/app/pages/DashBoardPage/utils/widget.ts` 中原有的 `47` 条 `prettier` warning 已全部退出。

- 当前仍未完成项：
  - 代码规范链后续可继续按“单文件 warning 高但仍为纯格式问题”的方式收口剩余大文件。
  - 现阶段仍暂不进入 hooks 依赖修复、业务逻辑调整和高风险内部命名重构。

### 2026-06-11 本轮继续推进：收口 MainPage 权限表单与侧边栏列表历史格式差异

- 本轮实际落地：
  - `frontend/src/app/pages/MainPage/pages/PermissionPage/Main/PermissionForm/VizPermissionForm.tsx`
  - `frontend/src/app/pages/MainPage/pages/PermissionPage/Main/PermissionForm/index.tsx`
  - `frontend/src/app/pages/MainPage/pages/SchedulePage/Sidebar/ScheduleList.tsx`
  - `frontend/src/app/pages/MainPage/pages/SourcePage/Sidebar/SourceList.tsx`
  - `frontend/src/app/pages/MainPage/pages/VariablePage/VariableForm.tsx`

- 本轮收口内容：
  - 只处理 MainPage 这 5 个文件中的 `prettier/prettier` 历史格式差异，不改权限计算逻辑、不改侧边栏菜单行为、不改变量权限表单语义。
  - 本轮优先挑选了单文件扫描结果中仅包含 `prettier/prettier` 的目标文件，暂未纳入同样 warning 较多但体量更大的 `ViewPage/Sidebar/FolderTree.tsx`，以保证提交边界和回归验证更清晰。
  - 处理方式仍然是使用前端本地已安装的 `prettier` 对目标文件做机械格式化，保持无网络依赖，也不引入手工逻辑修改。

- 本轮验证结果：
  - 在本机 `Node 26.0.0 / npm 11.15.0` 下：
    - `npm run lint` 通过。
  - `lint` 总 warning 从 `255` 降到 `196`，且本轮目标文件相关的 `59` 条 `prettier` warning 已退出。

- 当前仍未完成项：
  - 代码规范链后续可继续处理 `FolderTree.tsx`、`chartHelper.ts` 等剩余纯格式大文件。
  - 现阶段仍暂不进入 hooks 依赖修复、业务逻辑调整和高风险内部命名重构。

### 2026-06-11 本轮继续推进：收口 HttpClient 5.5 / JWT-JWK / Calcite 局部弃用入口

- `data-providers/http-data-provider/src/main/java/datart/data/provider/HttpDataFetcher.java`
  - 已从 `CloseableHttpClient.execute(request)` 切到 `execute(request, responseHandler)` 风格
  - 已把连接超时默认配置收口到 `ConnectionConfig + PoolingHttpClientConnectionManagerBuilder`
  - 请求级只保留响应超时配置，避免继续使用 5.5 的 `RequestConfig#setConnectTimeout(...)` 弃用入口

- `security/src/main/java/datart/security/oauth2/WeChartOauth2Client.java`
  - access token 和 userinfo 拉取都已切到 `responseHandler` 风格
  - 微信 OAuth 的 `HttpClient 5.5` 弃用执行接口 warning 已清零

- `security/src/main/java/datart/security/util/JwkUtils.java`
  - `Jwts.claims(Map)` 已改为 `Jwts.claims().add(...).build()`
  - `RemoteJWKSet` 已替换为 `JWKSourceBuilder.create(...).build()` 生成的远端 JWK source
  - URL 构造已改为 `URI.create(...).toURL()`，避免继续使用 `new URL(String)` 弃用入口

- `data-providers/data-provider-base/src/main/java/datart/data/provider/jdbc/SqlScriptRender.java`
  - `sqlDialect.getDatabaseProduct()` 已改为更保守的 `sqlDialect.getClass().getSimpleName()`
  - `StringUtils.replaceIgnoreCase(...)` 已改为本地大小写无关替换实现

- `2026-06-11` 定向验证结果：
  - `mvn -o -pl data-providers/http-data-provider -am -DskipTests -Dmaven.compiler.showDeprecation=true compile` 通过
  - `mvn -o -pl security -am -DskipTests -Dmaven.compiler.showDeprecation=true compile` 通过
  - 当前这两条链中的 `HttpClient 5.5`、`JWK/JWT`、`SqlScriptRender` 相关 deprecated warning 已退出
