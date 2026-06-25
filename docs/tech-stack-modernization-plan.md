# yu-bi 现代化改造执行计划

复盘时间：2026-06-25

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
| 当前专题分支               | `codex/modernization-frontend-security-deps`               |
| 当前分支相对 `origin/main` | 以恢复命令输出为准；本轮复盘前为 `0 122`                   |
| 最近专题提交               | 以 `git log -1 --oneline` 输出为准                         |
| 当前工作区                 | 干净                                                       |

分支纪律：

- 不直接在 `main` 开发
- 当前阶段继续在 `codex/modernization-frontend-security-deps` 上累计改造
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
- 构建体积报告已具备文本、JSON、文件输出、观察基线、稳定 id、分类汇总和分类过滤能力

### 3.2 当前专题成果

当前专题：P2-E 前端安全依赖与运行时治理。

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
- 构建基线治理：baseline 校验已覆盖 raw 超限稳定 id 和 raw 超限分类计数，能发现分包分类漂移

### 3.3 已验证但未收口的问题

- JS raw 超限仍存在 7 个稳定 id：`monacoEditor.js`、`antdDesign.js`、`echarts.js`、`monacoBase.js`、`antvG2.js`、`antvS2.js`、`antvG.js`
- asset raw 超限仍存在 2 个稳定 id：`geo-china-city.map.json`、`geo-china.map.json`
- gzip 维度 JS 当前已无超限；asset gzip 仍主要来自 `geo-china-city.map.json`
- AntD 6、ESLint 10、Monaco 最新线、Quill 最新线仍有明确 peer 或 audit 阻塞
- Calcite、Shiro、Druid、数据源方言、调度实例名等属于中高风险链路，后续可以改造，但必须先补专项基线

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
| Calcite             | `1.26.0`      | 高风险，已有 SQL parser / render 基线，后续可专项推进评估 |

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
| P1     | 构建体积 raw 超限治理      | 中   | 用 `build:report` 的 `category`、`idFilter` 和 JSON baseline 聚焦 `monaco`、`antd`、地图 |
| P1     | 前端动态运行时入口补强     | 中   | 继续补真实入口 smoke，优先覆盖分享页、图表 iframe、编辑器、地图和看板只读链路            |
| P1     | 后端方言 / SQL 基线扩展    | 中高 | 先补更多 SQL render 和 driver metadata 合同，再评估 Calcite 或方言实现升级               |
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
| Calcite 新版本          | 可专项评估     | SQL parser、render、变量替换、多方言、driver metadata 基线足够后再试         |
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
- `npm run build:report:check`：按观察基线校验稳定 id 清单

`vendor` 分类应覆盖 `vite.shared.mts#createVendorManualChunks` 中当前所有手工第三方分包，避免构建报告和实际分包规则漂移。

`build:report:check` 会校验 raw / gzip 超限稳定 id，并校验 raw 超限分类计数。当前分类基线为 JS `vendor=7`、asset `geo=2`。

当前观察对象：

| 类型      | raw 超限稳定 id                                                                                         | gzip 超限稳定 id          |
| --------- | ------------------------------------------------------------------------------------------------------- | ------------------------- |
| JS / task | `monacoEditor.js`、`antdDesign.js`、`echarts.js`、`monacoBase.js`、`antvG2.js`、`antvS2.js`、`antvG.js` | 无                        |
| asset     | `geo-china-city.map.json`、`geo-china.map.json`                                                         | `geo-china-city.map.json` |

后续治理顺序：

1. 先看 gzip 维度，避免把 raw 拆分后的统计变化误判为用户传输体积回退。
2. 对 raw 超限项只做有收益的拆分；无收益的按需加载试验不保留。
3. 地图资源继续走 asset 方向治理，优先评估按层级、按省份、服务端资源或压缩策略。
4. 每次构建体积治理后同步更新基线文件或记录不更新原因。
5. 生成 `build/build-report.json` 后再顺序执行 `build:report:check`，不要并行执行，避免校验脚本读取旧报告文件。

## 9. 提交与推送

当前执行偏好：

- `git add`、`git commit --no-verify -m "..."`、`git push origin codex/modernization-frontend-security-deps` 可以自动执行
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
cd frontend
npm run verify:toolchain
npm audit --json
npm outdated --json --registry=https://registry.npmmirror.com
npm ci --dry-run --ignore-scripts --no-audit --no-fund
npm run checkTs
```

构建体积聚焦：

```bash
cd frontend
npm run build
npm run build:task
YU_BI_CHUNK_REPORT_ONLY_OVERSIZED=1 npm run build:report
YU_BI_CHUNK_REPORT_CATEGORY_FILTER=vendor YU_BI_CHUNK_REPORT_ONLY_OVERSIZED=1 npm run build:report
YU_BI_CHUNK_REPORT_FORMAT=json YU_BI_CHUNK_REPORT_ONLY_OVERSIZED=1 YU_BI_CHUNK_REPORT_OUTPUT=build/build-report.json npm run build:report
YU_BI_CHUNK_REPORT_BASELINE_REPORT=build/build-report.json npm run build:report:check
```

后端专项恢复：

```bash
mvn -pl security -am test
mvn -pl data-providers/jdbc-data-provider -am test
mvn -pl server -am -DskipTests package
```
