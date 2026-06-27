# yu-bi 现代化改造执行计划

复盘时间：2026-06-27

本文档是 yu-bi 现代化改造的恢复入口和执行看板。后续恢复工作时优先阅读本文档，不再从历史流水记录里反推当前策略。

## 1. 当前目标

现代化改造不是追逐所有技术栈的最新版本，而是在兼容、正确、可验证、可回滚的前提下，将前后端核心技术栈收口到较新的稳定版。

硬性基线：

- 后端兼容 `JDK 21`
- 前端兼容 `Node 24`
- 前端包管理兼容 `npm 11`
- 不采用预发布、alpha、beta、milestone 作为默认升级目标
- 中高风险任务可以推进，但必须先满足准入规则

固定禁止项：

- 不贸然改 Java 包名 `datart.*`
- 不贸然改配置前缀 `datart.*`
- 不贸然改 `DATART_*` 等内部技术符号
- 不贸然改数据迁移相关常量、后缀和内部稳定标识

固定改造原则：

- 遇到兼容性问题，必须优先改旧代码适配现代化目标版本，不通过降低目标版本规避适配
- 例如 Calcite 升级后的 parser、dialect、SQL unparse 差异，优先改 yu-bi 自定义 parser / dialect / SQL builder 代码并补回归基线

## 2. 当前快照

恢复时先执行：

```bash
git status --short --branch
git rev-list --left-right --count origin/main...HEAD
git log --oneline --decorate -8
```

当前状态：

| 项目                       | 状态                                                       |
| -------------------------- | ---------------------------------------------------------- |
| 工作目录                   | `/Users/chencongyu/WorkHome/VSProjects/open-project/yu-bi` |
| 远端                       | `git@github.com:xybigdata/yu-bi.git`                       |
| 主线分支                   | `main`                                                     |
| 当前专题分支               | `codex/modernization-datasource-metadata-baselines`        |
| 当前分支相对 `origin/main` | 以恢复命令输出为准                                         |
| 最近专题提交               | 以 `git log -1 --oneline` 输出为准                         |
| 当前工作区                 | 以 `git status --short --branch` 输出为准                 |

分支纪律：

- 不直接在 `main` 开发
- 当前阶段在 `codex/modernization-datasource-metadata-baselines` 上扩展后端 datasource metadata、SQL parser、query script 和变量替换合同基线
- 不因为单个小批次完成就新建分支
- 不因为分支领先较多就主动合并 `main`
- 专题分支可以阶段性 push 保存进度
- 只有用户明确要求阶段合并时，才准备 `main` 合并和完整门禁
- 尽量累计一组相关改动后提交，避免过频繁 commit

## 3. 阶段性复盘

### 3.1 已完成主线成果

- yu-bi 已从 datart 独立，仓库、默认分支、远端已切换完成
- README、README_zh、NOTICE、SECURITY、ROADMAP、CHANGELOG、MAINTAINERS、issue template 已收口为独立开源项目表述
- `.tmp/`、`logs/` 已加入 `.gitignore`
- Maven 坐标、POM 名称、描述、SCM 已收口到 yu-bi；内部启动类 `datart.DatartServerApplication` 保留
- 后端已建立 `JDK 21 + Spring Boot 3.5.x + Spring Cloud 2025.0.x` 主链
- 前端已建立 `Node 24 + npm 11 + React 19 + Ant Design 5 + Vite 8 + TypeScript 6 + Vitest 4` 主链
- CRA / CRACO、IE11 主兼容链、Nashorn 默认链路、PhantomJS 等历史主链已退出或不再作为默认路径
- 前端 `npm audit` 当前保持 0 漏洞
- 构建体积报告已具备文本、JSON、文件输出、观察基线、稳定 id、分类汇总、分类过滤和分类体积预算校验能力

### 3.2 已完成专题成果

上一专题：P2-E 前端安全依赖与运行时治理，已合入 `main`。

已完成的核心改造：

- Node / npm 基线：`.nvmrc`、`.node-version`、`packageManager`、`engine-strict`、`verify:toolchain`
- npm lockfile：收口到 npm 11 原生 lockfile v3
- 依赖声明：前端直接依赖固定到 lockfile 已验证版本，降低漂移风险
- 安全治理：通过直接升级、overrides 和本地兼容包清理安全风险与废弃传递依赖
- 富文本：从 `react-quill -> quill@1.3.7` 迁移到 `react-quill-new 3.7.0 / quill 2.0.2`
- React 主链：升级到 React 19，并补齐类型、ref、cloneElement、DnD connector 等兼容边界
- Vite / TypeScript：升级到 Vite 8、TypeScript 6，并将模块解析收口到 bundler 模型
- ECharts：升级到 ECharts 6，词云迁移到 `@echarts-x/custom-word-cloud`
- React Router：升级到 7.x，入口通过 `routerCompat` 管理
- react-window：升级到 2.x，虚拟表格迁移到新 `Grid` API
- Monaco、ECharts、Quill、Reveal、Split、DnD、SQL formatter 等动态运行时入口已补最小 smoke
- 构建体积治理：地图 JSON 已迁出 JS chunk，Monaco / AntV / AntD vendor 分包已细化，gzip 维度 JS 超限清零
- 构建报告治理：vendor 分类已覆盖当前所有手工 vendor chunk，分类过滤可用于完整观察第三方包体积上下文
- 构建基线治理：baseline 校验已覆盖 raw / gzip 超限稳定 id、分类计数、分类集合、分类体积和总体积，能发现分包分类和体积预算漂移
- 后端方言基线：内置 JDBC driver 已固化 CustomSqlDialect fallback 与显式 / 标准方言边界，支撑后续 Calcite 和 driver 元数据评估
- 后端 render 基线：CustomSqlDialect fallback driver 已覆盖基础 SQL 包装渲染合同，确认默认引号和 `DATART_VTABLE` 包装稳定

### 3.3 当前专题

当前专题：P1 后端 datasource metadata 与 SQL/query 基线扩展。

推进原则：

- 优先固化 JDBC datasource metadata 读取中的 catalog/schema、table、column、foreign key 边界
- 同步固化 SQL parser 多方言 quoting、query script 过滤与 fallback、struct script JOIN、SQL validate、SQL builder 生成、注释清理、片段变量和 snippet 变量替换边界
- 优先补测试，不改运行时行为；后续 Calcite、driver、数据源链路升级必须复用这些门禁作为回归证据
- 本专题不改变业务协议、路由协议和持久化数据结构
- 当前用户要求是同一专题分支多累计相关改动后再合并 `main`，不要小批量频繁合并主线

### 3.4 已验证但未收口的问题

- JS raw 超限仍存在 7 个稳定 id：`monacoEditor.js`、`antdDesign.js`、`echarts.js`、`monacoBase.js`、`antvG2.js`、`antvS2.js`、`antvG.js`
- asset raw 超限仍存在 2 个稳定 id：`geo-china-city.map.json`、`geo-china.map.json`
- gzip 维度 JS 当前已无超限；asset gzip 仍主要来自 `geo-china-city.map.json`
- 路由级页面 Loadable 已补集中 smoke，覆盖顶层页面、MainPage 子页、分享页和看板子页懒加载入口
- React 入口工厂已补 smoke，覆盖主应用和分享页共同使用的 `createRoot` 装配路径，以及生产环境 React DevTools hook 关闭逻辑
- 看板只读入口已补 smoke，覆盖 read 模式 BoardProvider 装配、auto/free 看板分支、数据拉取、可见性派发和卸载清理
- 地图图表真实入口已补 smoke，覆盖 `loadGeoMap`、ECharts `registerMap` 和最新渲染重放到 `setOption` 的主路径
- 图表 iframe 真实入口已补 smoke，覆盖 iframe / 非 iframe 双路径、非法尺寸归零、loading 遮罩、iframe runtime context、右键坐标按 scale 转发，以及 dispatcher 向 `ChartIFrameContainer` 传递 visibility、尺寸、loading、选中项、下钻项和 workbench 环境
- 图表 iframe loading 样式状态已改为 styled-components transient prop，避免 React 19 将 `isLoading` 透传到 DOM 并输出 unknown prop warning
- JDBC 方言基线继续扩展：`ProviderFactoryTest` 已覆盖全部 30 个内置 driver 的 adapter / dialect 装配、CustomSqlDialect fallback 分类、fallback quote 默认值、quoteIdentifiers 默认开启、显式 / 标准方言分页能力，以及 fallback 基础 render 合同
- JDBC datasource metadata 基线已补：`JdbcDataProviderAdapterMetadataTest` 覆盖 catalog 优先、schema fallback、按 catalog/schema 读取表、`TABLE/VIEW` 过滤参数、列类型映射、外键挂载，以及 driver 不支持 imported keys metadata 时的容错
- SQL parser / query 基线继续扩展：`SqlParserUtilsTest` 覆盖 MySQL backtick、Oracle double quote、MSSQL bracket quoting，以及 snippet bracket quoting 与运行时 dialect 解耦
- Query script 基线继续扩展：`SqlQueryScriptProcessorTest` 覆盖多语句拒绝、parser fallback、现代 SQL fallback、特殊 SQL 禁止/放行和 special SQL + query 混合时只选 query 的合同
- SQL render / 字符串基线继续扩展：`SqlScriptRenderTest` 覆盖 query/simple/fragment/snippet 变量替换；`SqlStringUtilsTest` 覆盖多方言注释清理、末尾分号清理、fragment 变量单值约束
- SQL builder 基线继续扩展：`SqlBuilderTest` 覆盖 select/group/aggregate/filter/order/page、function column、HAVING、关闭 quoteIdentifiers 的生成合同，并修复聚合过滤 HAVING 生成双 `DATART_VTABLE` 前缀的问题
- SQL 变量解析基线继续扩展：`SqlParserUtilsTest` 覆盖 query 变量多值 IN、范围变量 min/max 收敛、空 query 变量转 `IS NULL`、禁用 permission 变量转 `1=1`，以及 parser 失败后的 regex fallback
- Struct script / SQL validate 基线继续扩展：`StructScriptProcessorTest` 覆盖无 join、连续 join、多条件 join、非法 join 条件跳过和空 table 异常；`SqlScriptRenderTest` 覆盖 STRUCT 端到端渲染；`SqlValidateUtilsTest` 覆盖 SELECT/WITH、DDL/DML 禁止、special SQL 开关和 parsed DML 禁止
- Calcite 已从 `1.26.0` 升级到 `1.42.0`：已适配自定义 JavaCC parser 的 `SqlAbstractParserImpl#parseArray()`、`SqlBasicCall` 构造器、`SqlOperator#createCall` varargs 类型和已移除的 `CalciteResource` 方法；base SQL/parser 45 个基线和 JDBC provider 96 个基线已通过
- Calcite 后续方言回归已补第一批代表分页合同：`ProviderFactoryTest` 通过真实 `jdbc-driver.yml` 创建 MySQL、ClickHouse、PostgreSQL adapter/dialect，固化 `withPage=true` 后的分页 SQL 输出；JDBC 专项扩展到 99 个基线
- Calcite 后续方言回归继续扩展：`ProviderFactoryTest` 通过真实 adapter/dialect 固化 MySQL、Oracle、MSSQL 的 `AGG_DATE_MONTH` 输出，以及 Oracle `NOW()` 到 `SYSDATE` 的自定义函数合同；JDBC 专项扩展到 103 个基线
- Calcite 1.42 Oracle 方言适配已收窄到自定义标准函数操作数渲染：保留 Oracle 显式双引号标识符解析合同，同时恢复 `AGG_DATE_*` 输出不额外 quote 小写字段的旧合同；已通过 base SQL/parser 45 个基线和 JDBC provider 103 个基线
- AntD 6、ESLint 10、Monaco 最新线、Quill 最新线仍有明确 peer 或 audit 阻塞
- Shiro、Druid、数据源真实方言、调度实例名等属于中高风险链路，后续可以改造，但必须先补专项基线

## 4. 当前技术栈基线

### 4.1 后端

| 技术栈              | 当前基线      | 当前判断                                                  |
| ------------------- | ------------- | --------------------------------------------------------- |
| Java                | `21`          | 硬性目标已满足                                            |
| Maven               | `>=3.9`       | 已由 Enforcer 约束                                        |
| Spring Boot         | `3.5.15`      | 保持 Boot 3.5 稳定线，不跳 Boot 4                         |
| Spring Cloud        | `2025.0.3`    | 当前项目无实际 Spring Cloud 编译依赖，保留 BOM 补丁线     |
| Spring Security     | `6.5.11`      | 随 Boot 3.5 管理，暂不跳 Spring Security 7                |
| Shiro               | `2.0.6`       | 中高风险，继续补认证授权基线后小步推进                    |
| MyBatis Boot        | `3.0.5`       | 已适配 Boot 3                                             |
| MyBatis Generator   | `1.4.2`       | 保持 1.x，2.x 需单独评估生成差异                          |
| GraalJS             | `25.0.3`      | 默认 JavaScript 引擎链路已从 Nashorn 转向 GraalJS         |
| BouncyCastle        | `1.84`        | 已统一到 `jdk18on` 组件线                                 |
| Springdoc           | `2.8.17`      | 适配 Boot 3；3.x 属 Boot 4 / Spring 7 生态，暂不推进      |
| H2                  | `2.4.240`     | 已升级                                                    |
| Hibernate Validator | `8.0.4.Final` | 保持 Jakarta Validation 3 线                              |
| MySQL Connector/J   | `9.7.0`       | 已通过 BOM 属性覆盖                                       |
| HikariCP            | `7.1.0`       | 已补配置映射测试后升级                                    |
| Selenium            | `4.45.0`      | 已升级                                                    |
| JsonPath            | `3.0.0`       | 已补 OAuth 属性映射测试后升级                             |
| Druid               | `1.2.28`      | 中风险，需补连接池与监控链路验证                          |
| Calcite             | `1.42.0`      | 已完成 parser/API 和 Oracle 标准函数 quote 适配；已补代表分页和函数方言回归 |

### 4.2 前端

| 技术栈                  | 当前基线           | 当前判断                                            |
| ----------------------- | ------------------ | --------------------------------------------------- |
| Node                    | `>=24.0.0 <25.0.0` | 硬性目标                                            |
| npm                     | `>=11.0.0 <12.0.0` | 与 Node 24 配套                                     |
| React                   | `19.2.7`           | 已升级并补类型边界                                  |
| React Router            | `7.18.0`           | 已升级                                              |
| Ant Design              | `5.29.3`           | 稳定主链；AntD 6 被 Pro Components 稳定版 peer 阻塞 |
| Pro Components          | `2.8.10`           | 稳定版仍只支持 AntD 4/5                             |
| TypeScript              | `6.0.3`            | 已升级并移除 `baseUrl`                              |
| Vite                    | `8.1.0`            | 已升级                                              |
| Vitest                  | `4.1.9`            | 当前测试主链                                        |
| ECharts                 | `6.1.0`            | 已升级，词云扩展已替换                              |
| monaco-editor           | `0.52.2`           | 最新线仍有 audit 风险，暂不升级                     |
| react-quill-new / quill | `3.7.0 / 2.0.2`    | 保持 audit 清零组合                                 |
| AntV S2                 | `2.7.2 / 2.3.1`    | 当前稳定线                                          |
| styled-components       | `6.4.3`            | 已升级                                              |
| react-window            | `2.2.7`            | 已完成 API 迁移                                     |
| react-grid-layout       | `2.2.3`            | 继续使用官方 legacy 入口                            |
| react-hotkeys-hook      | `5.3.2`            | 已升级                                              |
| sql-formatter           | `15.8.2`           | 已补 runtime smoke                                  |
| @testing-library/react  | `16.3.2`           | 适配 React 19                                       |
| ESLint                  | `9.39.4`           | ESLint 10 被插件 peer 阻塞                          |

## 5. 中高风险任务准入规则

后续中高风险任务可以进入改造，但不能直接“大版本替换后再看”。每个中高风险项在动版本或行为前必须先写清：

| 准入项       | 要求                                                                         |
| ------------ | ---------------------------------------------------------------------------- |
| 当前阻塞点   | 明确是 peer、audit、API 变更、数据兼容、运行时协议还是测试缺口               |
| 生态兼容证据 | 优先用官方 release、npm peer、Maven dependency tree、实际 dry-run 或试装证明 |
| 最小验证门禁 | 先补一组能暴露风险的定向测试，再升级或替换                                   |
| 回滚策略     | 版本回退、配置开关、兼容 helper 或保持旧路径的方式必须明确                   |
| 影响面       | 列出涉及模块、用户可见行为、数据格式、部署链路或性能体积变化                 |

可以推进的中高风险任务类型：

- 已有专项 smoke 或可补专项 smoke 的运行时依赖升级
- 有明确 peer 支持、audit 不回退、构建可验证的主版本升级
- 先建立基线、再替换实现的后端方言、认证、数据源、调度链路治理
- 构建体积治理中有可观测收益且不改变业务协议的拆分或资源策略调整

暂不推进的情况：

- 只为追最新而引入预发布链路
- 试装会破坏当前 `npm audit` 清零状态
- peer 依赖明确不支持当前主链
- 会触碰 `datart.*`、`DATART_*`、迁移标识或持久化调度标识，但没有迁移设计和回滚方案

## 6. 后续队列

### 6.1 立即推进

| 优先级 | 事项                       | 风险 | 下一步                                                                                   |
| ------ | -------------------------- | ---- | ---------------------------------------------------------------------------------------- |
| P0     | 执行文档瘦身和恢复入口维护 | 低   | 本轮完成；后续只记录阶段结论，不再堆叠长流水                                             |
| P1     | React 19 DOM prop 兼容治理 | 低   | 已完成并合入 `main`：图表 iframe loading 样式状态改为 transient prop                              |
| P1     | 前端动态运行时入口补强     | 中   | 路由级 Loadable、入口工厂、看板只读、地图图表和图表 iframe smoke 已补；后续继续观察其他 runtime warning |
| P1     | 构建体积 raw 超限治理      | 中   | 已补分类体积预算校验；后续用 `build:report` 聚焦 `monaco`、`antd`、地图                  |
| P1     | 后端方言 / SQL 基线扩展    | 中高 | 已补内置 driver 方言 fallback、基础 render、driver metadata、datasource metadata、SQL parser、query script、struct script、SQL validate、SQL builder、变量替换、SQL 字符串工具、代表分页、函数方言和 Oracle quote 合同；Calcite 已升至 `1.42.0` 并通过专项基线 |
| P2     | Shiro / Security 边界治理  | 中高 | 不整体替换 Shiro；继续补认证、授权、token、异常边界测试后做小步修复                      |

### 6.2 条件满足后推进

| 事项                    | 当前状态       | 触发条件                                                                     |
| ----------------------- | -------------- | ---------------------------------------------------------------------------- |
| AntD 6                  | 暂缓           | Pro Components 稳定版明确支持 AntD 6，不采用 beta / prerelease               |
| ESLint 10               | 暂缓           | `eslint-plugin-react`、`eslint-plugin-import` 稳定版 peer 明确支持 ESLint 10 |
| Monaco 最新线           | 暂缓           | 最新版本不再引入当前 `dompurify` / `marked` audit 风险                       |
| Quill / react-quill-new | 暂缓           | 最新组合不再触发 `GHSA-v3m3-f69x-jf25`，并通过富文本 smoke                   |
| @vitejs/plugin-react 6  | 暂缓           | npm 11 下可干净安装，不再触发 Babel 8 peer 解析风险                          |
| @types/node 26          | 不推进         | 当前目标是 Node 24；除非目标基线调整，否则不切 Node 26 类型线                |
| Spring Boot 4           | 暂不作为本阶段 | 需要 Spring 7、Springdoc 3、Security 7 等整体生态评估                        |
| Calcite 后续方言回归    | 持续推进       | 已补 MySQL / ClickHouse / PostgreSQL 分页合同，以及 MySQL / Oracle / MSSQL 日期聚合和 Oracle `NOW()` 合同；后续按 quote、metadata 和更多真实数据库继续补回归 |
| Druid 新版本或替换      | 可专项评估     | 需要连接池配置、监控、连接生命周期和生产兼容验证                             |

### 6.3 当前 npm outdated 复核口径

当前剩余 outdated 不要重复无效试装。只有以下信息发生变化时再复核：

- `@vitejs/plugin-react` 6.x peer 链在 npm 11 下可干净安装
- Pro Components 稳定版支持 AntD 6
- ESLint React / import 插件稳定版支持 ESLint 10
- Monaco 最新线不再引入 audit 风险
- Quill 最新组合不再触发 XSS advisory

复核命令优先使用：

```bash
npm audit --json
npm outdated --json --registry=https://registry.npmmirror.com
npm ls @types/node @vitejs/plugin-react antd eslint monaco-editor quill react-quill-new --depth=0 --json
npm run verify:toolchain
```

## 7. 门禁策略

开发期按风险分层验证，不为每个小改动跑完整门禁。准备合入 `main` 或推送 `main` 前再执行完整门禁。

| 场景                           | 最低门禁                                                         |
| ------------------------------ | ---------------------------------------------------------------- |
| 文档或纯元数据                 | `git diff --check`，必要时 `prettier --check`                    |
| 前端类型边界、小范围组件迁移   | `npm run checkTs` + 相关测试                                     |
| helper、模型、共享协议变化     | `npm run checkTs` + 相关模型 / helper 测试                       |
| 依赖、构建配置、运行时加载变化 | `npm run checkTs` + 相关运行时测试；专题收尾补 `npm run test:ci` |
| 构建体积治理                   | `npm run build` + `npm run build:task` + `npm run build:report`  |
| Maven、Docker、安装包链路变化  | `mvn package -DskipTests`，必要时补 demo smoke                   |
| 准备 merge 回 `main`           | 前端完整门禁，必要时补后端门禁                                   |
| 推送 `main`                    | 不跳过完整门禁                                                   |

完整前端门禁：

```bash
npm run verify:toolchain
npm run checkTs
npm run test:ci
npm run lint:css
npm run lint:style
```

构建与安装包门禁：

```bash
npm run build
npm run build:task
npm run build:report
mvn package -DskipTests
scripts/check-demo-health.sh
```

依赖链路补充门禁：

```bash
npm ci --dry-run --ignore-scripts --no-audit --no-fund
npm audit --json
npm ls --all
```

## 8. 构建体积观察基线

当前构建报告能力：

- `npm run build:report`：输出文本报告
- `YU_BI_CHUNK_REPORT_FORMAT=json`：输出 JSON 报告
- `YU_BI_CHUNK_REPORT_OUTPUT=build/build-report.json`：写入文件
- `YU_BI_CHUNK_REPORT_ONLY_OVERSIZED=1`：只输出超限项
- `YU_BI_CHUNK_REPORT_ID_FILTER=antdDesign.js`：按稳定 id 过滤
- `YU_BI_CHUNK_REPORT_CATEGORY_FILTER=vendor`：按分类过滤
- 文本报告每项输出 `gzipRatio`
- JSON 报告 `summary.chunk.size` / `summary.asset.size` 输出 raw、gzip、压缩率和 gzip 节省字节
- 文本报告输出分类体积摘要；JSON 报告 `summary.*.categorySizes` 输出分类文件数、raw、gzip、压缩率和节省字节
- `npm run build:report:check`：按观察基线校验稳定 id 清单、分类计数、分类体积和总体积
- `npm run build:report:check:current`：先生成当前 JSON 报告，再校验默认观察基线
- `npm run build:report:gzip`：生成 gzip 预算报告 `build/build-report-gzip.json`
- `npm run build:report:gzip:check`：按 gzip 预算基线校验 raw / gzip 超限稳定 id、分类计数、分类体积和总体积
- `npm run build:report:gzip:check:current`：先生成当前 gzip 预算报告，再校验 gzip 预算基线
- baseline 校验同时检查 `summary.*.size.bytes` / `summary.*.size.gzipBytes` 与 `summary.*.categorySizes.*.bytes` / `summary.*.categorySizes.*.gzipBytes`，防止超限名单不变但总体积或单分类体积回退
- gzip 预算脚本使用 Node wrapper 设置报告参数，避免依赖 Unix shell 内联环境变量

`vendor` 分类应覆盖 `vite.shared.mts#createVendorManualChunks` 中当前所有手工第三方分包，避免构建报告和实际分包规则漂移。

`build:report:check` 会校验 raw / gzip 超限稳定 id、raw / gzip 超限分类计数、分类集合、分类 raw / gzip 体积预算，以及总 raw / gzip 体积预算。当前 raw 分类基线为 JS `vendor=7`、asset `geo=2`。gzip 预算基线为 gzip 阈值 `500 KiB`，当前只有 asset `geo=1` 超限。

当前观察对象：

| 类型      | raw 超限稳定 id                                                                                         | gzip 超限稳定 id          |
| --------- | ------------------------------------------------------------------------------------------------------- | ------------------------- |
| JS / task | `monacoEditor.js`、`antdDesign.js`、`echarts.js`、`monacoBase.js`、`antvG2.js`、`antvS2.js`、`antvG.js` | 无                        |
| asset     | `geo-china-city.map.json`、`geo-china.map.json`                                                         | `geo-china-city.map.json` |

本轮真实报告观察：

- JS vendor 超限项 gzip 压缩率约 `24.4%` 到 `32.9%`
- 当前分类体积：JS `vendor` gzip 约 `2170.60 KiB`，JS `runtime` gzip 约 `630.00 KiB`
- 当前分类体积：asset `geo` gzip 约 `888.03 KiB`，其他 asset gzip 约 `78.40 KiB`
- `geo-china.map.json` gzip 压缩率约 `34.2%`
- `geo-china-city.map.json` gzip 压缩率约 `58.8%`，gzip 后仍约 `693.80 KiB`
- 地图 city 资源后续优先评估资源粒度和加载策略，不优先做 JS 分包式治理

后续治理顺序：

1. 先看 gzip 维度，避免把 raw 拆分后的统计变化误判为用户传输体积回退。
2. 对 raw 超限项只做有收益的拆分；无收益的按需加载试验不保留。
3. 地图资源继续走 asset 方向治理，优先评估按层级、按省份、服务端资源或压缩策略。
4. 每次构建体积治理后同步更新基线文件或记录不更新原因。
5. 生成 `build/build-report.json` 后再顺序执行 `build:report:check`，不要并行执行，避免校验脚本读取旧报告文件。

## 9. 提交与推送

当前执行偏好：

- `git add`、`git commit --no-verify -m "..."`、`git push origin <当前专题分支>` 可以自动执行
- 同一专题内累计一组相关改动后再提交
- 文档复盘可以单独提交，但后续小型文档修正优先并入相关改造提交
- 专题分支可 push；不主动 merge `main`
- 用户明确要求阶段合并时，再准备完整门禁、`--no-ff` 合并和 `main` 推送

建议提交粒度：

| 类型             | 粒度                          |
| ---------------- | ----------------------------- |
| 低风险类型边界   | 累计 3 到 10 个相关文件后提交 |
| 中风险运行时链路 | 每条可验证链路独立提交        |
| 依赖和构建链路   | 独立提交，但包含验证记录      |
| 阶段复盘         | 可单独提交，作为后续恢复入口  |

## 10. 恢复命令

继续当前专题：

```bash
git status --short --branch
git rev-list --left-right --count origin/main...HEAD
git log --oneline --decorate -8
mvn -pl data-providers/jdbc-data-provider -am -Dtest=ProviderFactoryTest,JdbcDataProviderAdapterMetadataTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl data-providers/data-provider-base -am -Dtest=SqlBuilderTest,SqlParserUtilsTest,SqlQueryScriptProcessorTest,SqlScriptRenderTest,SqlStringUtilsTest,StructScriptProcessorTest,SqlValidateUtilsTest -Dsurefire.failIfNoSpecifiedTests=false test
```

构建体积聚焦：

```bash
cd frontend
npm run build
npm run build:task
YU_BI_CHUNK_REPORT_ONLY_OVERSIZED=1 npm run build:report
npm run build:report:check:current
npm run build:report:gzip:check:current
YU_BI_CHUNK_REPORT_CATEGORY_FILTER=vendor YU_BI_CHUNK_REPORT_ONLY_OVERSIZED=1 npm run build:report
```

后端专项恢复：

```bash
mvn -pl security -am test
mvn -pl data-providers/jdbc-data-provider -am test
mvn -pl server -am -DskipTests package
```
