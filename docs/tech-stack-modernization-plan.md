# yu-bi 现代化改造执行板

本文档是 yu-bi 现代化改造的恢复入口、阶段复盘和后续执行清单。恢复工作时优先读本页，再按“当前短期目标”继续推进。

复盘时间：2026-06-22

## 1. 改造目标

在保证业务兼容、数据兼容、发布链稳定、结果正确的前提下，分阶段把前后端核心技术栈收口到较新的稳定版本。

“现代化”不等于追最新版本。版本选择遵循：

- 优先较新的稳定版
- 优先生态兼容明确的版本
- 优先能被当前测试和 smoke test 验证的升级
- 高风险链路先补验证，再小步替换

硬性兼容目标：

- 后端必须兼容 `JDK 21`
- 前端必须兼容 `Node 24`
- 前端 npm 基线按 `npm >= 11` 维护

固定禁止项：

- 不贸然改 Java 包名 `datart.*`
- 不贸然改配置前缀 `datart.*`
- 不贸然改 `DATART_*` 等内部技术符号
- 不贸然改数据迁移相关稳定常量、后缀和内部标识

## 2. 当前快照

恢复时先执行：

```bash
git status --short --branch
git rev-list --left-right --count origin/main...HEAD
git log --oneline --decorate -8
```

当前状态：

| 项目 | 状态 |
| --- | --- |
| 工作目录 | `/Users/chencongyu/WorkHome/VSProjects/open-project/yu-bi` |
| 远端 | `git@github.com:xybigdata/yu-bi.git` |
| 主线分支 | `main` |
| 当前专题分支 | `codex/modernization-frontend-security-deps` |
| 当前专题 | P2-E 前端安全依赖与运行时治理 |
| 当前分支相对 `origin/main` | 以恢复命令输出为准 |
| 最近专题提交 | 以 `git log --oneline --decorate -8` 为准 |
| 最近主线提交 | `f1739f621 chore: 合入 PRESTO driver 元数据治理` |

已确认的自动化权限和偏好：

- 可以自动执行 `git add`
- 可以自动执行 `git commit --no-verify -m "..."`
- 可以自动执行 `git push origin <branch>`
- `npm view ...` 查询已授权，后续不再单独询问
- `npm audit ...` 查询已授权，后续不再单独询问
- `mvn -pl security ...` 命令已授权，后续不再单独询问
- 同一专题内尽量累计一组相关改动后再提交，避免过频繁提交
- 当前专题继续在同一分支推进，不因为小批次改动立即合入 `main`
- 减少 `main` 分支合并频率；前端安全和运行时相关改造优先在 `codex/modernization-frontend-security-deps` 上连续推进，累计足够完整的一批后再统一合入 `main`
- 除非用户明确要求阶段合并，否则专题分支只推送远端，不主动 merge 回 `main`
- 目标模式中的“专题完成后 push 再 merge 到 main”按最新执行口径理解为：专题分支可持续 push 保存进度，但 `main` 合并要减少频率，只在一批可验收成果完成后统一处理

## 3. 分支与合并规则

固定规则：

- 不直接在 `main` 开发
- 按专题使用 `codex/*` 分支
- 专题分支可以推送远端
- 同一专题尽量长期保留一条分支连续推进，避免为小批次改动频繁创建新分支
- 专题累计到一批可验收成果后，再按用户确认节奏 `--no-ff` merge 回 `main`
- 推送 `main` 前必须完整门禁

当前专题分支：

```bash
codex/modernization-frontend-security-deps
```

当前专题收口前不要创建新分支。P2-E 聚焦前端安全依赖、前端工具链和运行时主链治理：优先处理补丁级升级、lockfile、npm overrides、可验证的富文本 Quill 2 迁移，以及已经具备验证证据的 Vite / TypeScript / React 主链升级；本专题继续累计在 `codex/modernization-frontend-security-deps`，暂不因为单个小批次合入 `main`。

## 4. 技术栈基线

### 4.1 后端

| 技术栈 | 当前基线 | 状态 |
| --- | --- | --- |
| Java | `21` | 已达硬性目标 |
| Maven | `>=3.9` | 已由 Enforcer 约束 |
| Spring Boot | `3.5.12` | 当前主链 |
| Spring Cloud | `2025.0.1` | 与 Boot 3.5 配套 |
| MyBatis Spring Boot | `3.0.4` | 已适配 Boot 3 |
| GraalJS | `25.0.1` | 已替代 Nashorn 主链 |
| BouncyCastle | `1.81.1` | 已统一到 `jdk18on` 组件线 |
| Springdoc | `2.8.17` | 已适配 Boot 3 |
| H2 | `2.4.240` | 已升级 |
| Selenium | `4.31.0` | 已升级 |
| Shiro | `2.0.5` | 高风险，只做认证授权边界审计和小步修复 |
| Druid | `1.2.28` | 中风险，暂不优先 |
| Calcite | 现网主链 | 高风险，先补 SQL 解析兼容样例 |

### 4.2 前端

| 技术栈 | 当前基线 | 状态 |
| --- | --- | --- |
| Node | `>=24.0.0 <25.0.0` | 硬性目标，当前验证基线 `24.16.0` |
| npm | `>=11.0.0 <12.0.0` | 与 Node 24 配套，当前验证基线 `11.13.0` |
| React | `19.2.7` | 已升级到 React 19 稳定线 |
| React Router | `6.30.4` | 已收口到 React Router 6 稳定线，并通过兼容层启用 v7 future flags |
| Ant Design | `5.29.3` | 已收口到 AntD 5 稳定线，并迁移到 v5 theme token API |
| Redux Toolkit | `2.12.0` | 已完成主升级 |
| React Redux | `9.3.0` | 已完成主升级 |
| TypeScript | `6.0.3` | 已升级到 6.0 稳定线，`baseUrl` 已迁移为显式 `paths` |
| Vite | `8.0.16` | 已升级到 Vite 8 主链，暂配 `@vitejs/plugin-react 5.2.0` |
| Vitest | `4.1.9` | 当前主测试栈 |
| Testing Library | `@testing-library/react 16.3.2 / dom 10.4.1` | 已对齐 React 19 测试栈 |
| Less | `4.6.7` | 已完成补丁升级 |
| vite-plugin-svgr | `5.2.0` | 已完成 Vite 8 下构建验证 |
| stylelint-order | `8.1.1` | 已完成 Stylelint 17 下 lint 验证 |
| styled-components | `6.4.2` | 已完成主升级并确认运行时依赖位置 |
| 富文本编辑器 | `react-quill-new 3.7.0 / quill 2.0.2` | 当前 P2-E 正在迁移验证 |
| monaco-editor | `0.52.2` | 已补真实运行时加载边界 |
| reveal.js | `6.0.1` | 已补真实运行时加载边界 |
| ECharts | `5.6.0` | 已升级到 ECharts 5 稳定线 |
| Axios | `1.18.1` | 已补 request wrapper 行为基线后小版本升级 |
| AntV S2 | `2.7.2 / 2.3.1` | 已确认当前稳定线 |
| i18next / react-i18next | `26.3.1 / 17.0.8` | 已确认国际化主链 |
| react-grid-layout | `2.2.3` | 已通过 legacy 入口升级 |
| flexlayout-react | `0.9.1` | 已升级并改用命名导出 |
| react-window | `1.8.11` | 保持 1.x 兼容线，2.x 独立评估 |
| react-draggable | `4.7.0` | 已升级 |
| react-resizable | `3.2.0` | 已升级 |
| @hello-pangea/dnd | `18.0.1` | 已确认当前稳定线 |
| react-dnd | `16.0.1` | 已确认当前稳定线 |
| react-dnd-html5-backend | `16.0.1` | 已确认当前稳定线 |

## 5. 阶段复盘

### 5.1 已完成主线成果

- yu-bi 已从 datart 独立，仓库、默认分支、远端已切换完成
- README、README_zh、NOTICE、SECURITY、ROADMAP、CHANGELOG、MAINTAINERS、issue template 已收口为独立开源项目表述
- 后端已建立 `JDK 21 + Spring Boot 3.5.x + Spring Cloud 2025.0.x` 主链
- 前端已建立 `Node 24 + React 19 + Ant Design 5 + Vite 8 + TypeScript 6 + Vitest 4` 主链
- CRA / CRACO、IE11 主兼容链、Nashorn、PhantomJS 等历史主链已退出
- `.tmp/`、`logs/` 已加入 `.gitignore`

### 5.2 已合入批次

| 批次 | 结果 |
| --- | --- |
| 独立开源治理与 yu-bi 基础品牌 | 已合入 `main` |
| 后端 JDK 21 / Spring Boot 3 主链 | 已合入 `main` |
| 前端 React 18 / AntD 5 / Vite 6 主链 | 已合入 `main` |
| Dashboard widget 内容协议边界 | 已合入 `main` |
| 图表运行时类型边界 | 已合入 `main` |
| 现代化兼容边界 | 已合入并推送 `origin/main` |
| 图表运行时现代化 | 已合入并推送 `origin/main` |
| 前端运行时现代化批次 | 已合入并推送 `origin/main`，主线提交 `77217676b` |
| 构建与安装包链路现代化 | 已合入并推送 `origin/main`，主线提交 `2c691916b` |
| Shiro 认证授权健康度审计 | 已合入并推送 `origin/main`，主线提交 `99336814e` |
| Calcite SQL 解析健康度审计 | 已合入并推送 `origin/main`，主线提交 `065e4d007` |
| SQL 变量替换行为修复 | 已合入并推送 `origin/main`，主线提交 `bf6eee2e2` |
| PRESTO JDBC driver 元数据治理 | 已合入并推送 `origin/main`，主线提交 `f1739f621` |

### 5.3 前端运行时专题复盘

分支：`codex/modernization-frontend-runtime-next`

已完成并合入：

- `react-grid-layout` `^1.3.4 -> ^2.2.3`，通过 `react-grid-layout/legacy` 保持旧布局 props 兼容
- `flexlayout-react` `^0.5.21 -> ^0.9.1`，入口改为命名导出，布局配置迁移到 `weight`
- `react-window` `^1.8.6 -> ^1.8.11`，暂不升级 2.x
- `react-draggable` `^4.4.3 -> ^4.7.0`
- `react-resizable` `^3.0.4 -> ^3.2.0`
- 移除过时的 `@types/react-grid-layout`
- Node 24 / React 18 类型边界对齐：`@types/node` `24.13.2`、`@types/react` `18.3.31`、`@types/react-dom` `18.3.7`
- 补丁线升级：`vitest` `4.1.9`、`less` `4.6.6`、`lint-staged` `17.0.8`
- Dashboard widget content 统一读取 helper 已落地，避免组件散落结构判断
- 旧依赖 `react-beautiful-dnd`、`react-sortable-hoc`、`react-virtualized` 等确认无源码和依赖树残留

已通过完整前端门禁：

```bash
npm run checkTs
npm run test:ci
npm run lint:css
npm run lint:style
```

## 6. 已完成短期目标：P2-A 构建与安装包链路

分支：`codex/modernization-build-package`

目标：把 Maven、Docker、安装包、启动脚本、部署文档收口到 yu-bi 当前发布形态，并确保 JDK 21 安装包启动链路可验证。

已完成：

- 清理 `server/pom.xml` 中已注释的旧 `frontend-maven-plugin` Node 14 / npm 6 配置块
- 将 Maven 前端 bootstrap registry 从废弃 `registry.npm.taobao.org` 切到 `registry.npmmirror.com`
- Linux / Windows 安装包启动脚本补齐 JDK 21 运行参数：`--add-opens=java.base/java.lang=ALL-UNNAMED`
- Linux / Windows 安装包启动脚本用户可见文案收口为 `yu-bi`
- Dockerfile 已将旧个人 label 收口为 OCI image labels
- Dockerfile 已增加基于 `/api/v1/sys/info` 的 `HEALTHCHECK`
- Deployment.md 安装包示例已从旧 beta 表述更新到当前 `1.0.0-rc.3`
- 保留内部启动类 `datart.DatartServerApplication`，不触碰 Java 包名和稳定内部标识

已通过验证：

```bash
bash -n bin/yu-bi-server.sh scripts/check-demo-health.sh
java -version
mvn -version
npm run checkTs
npm run test:ci
npm run lint:css
npm run lint:style
mvn package -DskipTests
scripts/check-demo-health.sh
git diff --check
```

验证说明：

- `mvn package -DskipTests` 在普通沙箱下因无法写入 `~/.m2` resolver 状态文件失败；提权后通过
- `scripts/check-demo-health.sh` 在普通沙箱下因本地端口绑定受限失败；提权后通过
- Maven 打包已重新生成 `yu-bi-server-1.0.0-rc.3-install.zip`，构建产物未产生需要提交的跟踪文件变更
- 本批次再次运行 `mvn package -DskipTests` 已通过，确认 Dockerfile 依赖的安装包可生成
- 合入 `main` 前完整前端门禁已通过：`checkTs`、`test:ci`、`lint:css`、`lint:style`
- `scripts/check-demo-health.sh` 普通沙箱下无法连上本地 8080；提权后健康检查通过
- 当前本机无 `docker` 命令，无法本地执行 Docker build；本批次只完成 Dockerfile 静态复扫

P2-A 本批次完成状态：

| 事项 | 风险 | 状态 |
| --- | --- | --- |
| Dockerfile 元数据 | 低 | 已将旧个人 label 改为 OCI labels |
| Docker 健康检查 | 中 | 已增加 `HEALTHCHECK`；最终镜像保留 `curl` 用于健康探测 |
| Deployment.md 安装包示例 | 低 | 已更新为当前 `1.0.0-rc.3` 示例 |
| Deployment.md 品牌文案 | 低 | 仅收口用户可见安装包示例，不改 `datart.conf` 和 `datart.*` 配置前缀 |
| Docker build 验证 | 受环境限制 | 当前本机无 `docker` 命令，记录为验证缺口 |

P2-A 后续缺口：

- 后续具备 Docker 环境后补 `docker build` 和容器健康检查验证

## 7. 已完成短期目标：P2-B Shiro 认证授权健康度审计

分支：`codex/modernization-shiro-health`

目标：不整体替换 Shiro，只补认证授权关键边界用例，并修复能被测试证明的授权正确性问题。

本批次已完成：

- 审计 `security` 模块 Shiro 适配层：`ShiroSecurityManager`、`ShiroSubjectFacade`、`ShiroAuthenticationTokenAdapter`
- 新增 `ShiroSecurityManagerTest`，覆盖 `requireAllPermissions` 的权限缓存边界
- 修复 `requireAllPermissions` 在已缓存允许权限时提前返回的问题；现在会继续检查后续权限
- 新增 `ShiroAuthenticationTokenAdapterTest`，覆盖密码 token、合法 bearer token、非法 bearer token 边界
- `ShiroAuthenticationTokenAdapter` 对非法 bearer token 返回稳定认证失败结果，不再向后传递空指针风险
- 排除钉钉 SDK 传入的旧 `bcpkix-jdk15on` / `bcprov-jdk15on` `1.65`
- 将 security 模块 BouncyCastle 组件统一到 `jdk18on` `1.81.1`
- 修复 EC PEM / JWK 测试里的 BouncyCastle `NoSuchMethodError`
- 确认本批次不触碰 Java 包名 `datart.*`、配置前缀 `datart.*`、`DATART_*` 和迁移稳定标识

已通过验证：

```bash
mvn -pl security -am -Dtest=datart.security.manager.shiro.ShiroSecurityManagerTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl security -am test
mvn -pl security -am dependency:tree '-Dincludes=org.bouncycastle:*' -Dscope=test
git diff --check
npm run checkTs
npm run test:ci
npm run lint:css
npm run lint:style
```

验证说明：

- 普通沙箱运行 Maven 仍会因写 `~/.m2` 被拦截失败；提权后定向 Shiro 测试通过
- `mvn -pl security -am test` 已通过，覆盖 core 3 个测试、security 12 个测试
- BouncyCastle 依赖树已确认只保留 `bcpkix-jdk18on`、`bcutil-jdk18on`、`bcprov-jdk18on` `1.81.1`
- 合入 `main` 前完整前端门禁已通过：`checkTs`、`test:ci`、`lint:css`、`lint:style`
- `npm run test:ci` 已通过 132 个测试文件、919 个测试，4 个跳过
- Maven 解析阿里云仓库 metadata 时出现过缺 checksum warning，但构建和测试通过

P2-B 完成状态：

- 已提交并推送专题分支
- 已使用 `--no-ff` 合入 `main` 并推送主线

## 8. 已完成短期目标：P2-C Calcite SQL 解析健康度审计

分支：`codex/modernization-calcite-health`

目标：不直接升级或替换 Calcite，先为 SQL 解析、变量解析和方言输出建立可重复的 JDK 21 健康度基线，为后续 Calcite 版本评估提供回归证据。

本批次已完成：

- 盘点 Calcite 主要使用点：自定义 JavaCC parser、`SqlParserUtils`、`SqlQueryScriptProcessor`、`SqlParserVariableResolver`、`SqlNodeUtils` 和各数据库方言 `unparse`
- 确认 `data-provider-base` 原先缺少启用状态的 Calcite parser 单元测试
- 新增 `SqlParserUtilsTest`，覆盖：
  - 常见 snippet 表达式解析
  - MySQL 反引号标识符解析和 SQL 输出
  - yu-bi 自定义 `AGG_DATE_MONTH` 在 MySQL 方言下的输出合约
  - 非法 snippet 的明确解析失败边界
  - `$status$` 查询变量通过 parser visitor 识别，并将多值等值条件替换为 `IN`
- 新增 `SqlQueryScriptProcessorTest`，覆盖：
  - 注释清理后的单查询脚本处理
  - 多查询脚本拒绝边界
  - parser 无法识别但字符串校验仍属于查询时的 fallback 包装行为
- 新增 `SqlScriptRenderTest`，覆盖：
  - SQL render 默认包装为 `SELECT * FROM (...) AS DATART_VTABLE`
  - render 阶段的查询变量多值等值条件替换为 `IN`
- 新增 `ProviderFactoryTest`，覆盖：
  - `init=false` 时不初始化数据源也能完成 adapter 和 dialect 发现
  - MySQL、H2、Oracle、ClickHouse 的 adapter / dialect 映射
  - 小写 `dbType` 自动归一为大写
  - 未知 `dbType` 的错误边界
- 新增 `SqlScriptRenderExamplesTest`，从历史禁用的 `SqlScriptRenderTest` 中拆出一批无需 Spring Boot 上下文、无需真实数据库连接的稳定样例，覆盖：
  - normal SQL、变量 SQL、fallback SQL 的代表性渲染路径
  - forbidden SQL 的拒绝边界
  - `enableSpecialSql=true` 时特殊 SQL 可放行的边界
- 调整测试侧 `TestSqlDialects`：
  - 复用 `init=false` adapter 创建路径获取 dialect，避免仅为测试初始化数据源
  - `PRESTO` 暂不纳入自动初始化集合；当前内置 driver 元数据缺少 `identifierQuote` / `literalQuote`，需要后续单独治理
- 修复 JDBC provider 默认加载边界：
  - `config/jdbc-driver-ext.yml` 不存在时回退为空扩展配置，不阻断内置驱动加载
  - `init=false` 创建 adapter 时同步设置 `JdbcProperties` 和 `JdbcDriverInfo`，让方言发现和分页能力判断可用
- 父 POM 为 Surefire 增加 `-Djava.awt.headless=true`，修复 JDK 21 / macOS 下 `POIUtilsTest` fork JVM `Abort trap: 6`
- 确认本批次不触碰 Calcite 版本、不改 Java 包名 `datart.*`、配置前缀 `datart.*`、`DATART_*` 和迁移稳定标识

已通过验证：

```bash
mvn -pl data-providers/data-provider-base -am -Dtest=datart.data.provider.calcite.SqlParserUtilsTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl data-providers/data-provider-base -am -Dtest=datart.data.provider.calcite.SqlParserUtilsTest,datart.data.provider.calcite.SqlQueryScriptProcessorTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl core -Dtest=datart.core.common.POIUtilsTest test
mvn -pl data-providers/data-provider-base -am test
mvn -pl data-providers/jdbc-data-provider -am -Dtest=datart.data.provider.jdbc.ProviderFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl data-providers/jdbc-data-provider -am -Dtest=datart.data.provider.sql.SqlScriptRenderExamplesTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl data-providers/jdbc-data-provider -am test
npm run checkTs
npm run test:ci
npm run lint:css
npm run lint:style
```

验证说明：

- 定向测试覆盖 core、data-provider 聚合模块和 data-provider-base，`SqlParserUtilsTest` 5 个用例通过
- P2-C 两个新增测试类共 8 个用例通过
- `SqlScriptRenderTest` 2 个轻量渲染用例通过
- `mvn -pl data-providers/data-provider-base -am test` 已通过，覆盖 core 3 个测试、data-provider-base 8 个测试，共 11 个测试
- `ProviderFactoryTest` 4 个用例通过
- `SqlScriptRenderExamplesTest` 6 个用例通过
- `mvn -pl data-providers/jdbc-data-provider -am test` 已通过，覆盖 core 3 个测试、data-provider-base 8 个测试、jdbc-data-provider 11 个启用测试，旧 `SqlScriptRenderTest` 仍跳过 6 个历史用例
- 合入主线前完整前端门禁已通过：
  - `npm run checkTs`
  - `npm run test:ci`：132 个测试文件通过，919 个用例通过，4 个跳过
  - `npm run lint:css`
  - `npm run lint:style`
- 历史 SQL render 样例中发现 `ORDER BY $部门$` 当前不会被变量替换，暂不纳入本批次绿色基线，后续作为 SQL 变量替换行为专项处理
- JavaCC 生成代码在 JDK 21 编译下仍有 deprecated annotation warning，暂不阻塞
- `POIUtilsTest` 在默认 fork JVM 下曾稳定出现 `Abort trap: 6`；加入 `-Djava.awt.headless=true` 后定向和完整 data-provider-base 门禁均通过
- 本批次只建立健康度基线，不做 Calcite 主版本升级

P2-C 合入状态：

- 已具备合入 `main` 条件
- 保留 `ORDER BY $变量$` 行为差异和 PRESTO driver 元数据缺口为后续独立专题，避免在本批次扩大行为变更面

## 9. 已完成短期目标：P2-G SQL 变量替换行为专项

分支：`codex/modernization-sql-variable-render`

目标：补齐 P2-C 中发现的 SQL 变量替换行为差异，优先恢复历史样例中 `ORDER BY $变量$` 等简单变量重复出现时的替换行为，并用 JDK 21 可重复测试证明修复。

本批次已完成：

- 复现并定位 `SELECT $部门$ ... ORDER BY $部门$` 只替换第一处变量的问题
- 确认根因：
  - parser visitor 能为普通位置变量生成 `SimpleVariablePlaceholder`
  - render 阶段对每个 placeholder 使用单次 `replaceIgnoreCase`
  - 同一个简单变量在 SQL 中重复出现时，只会替换第一处，后续同名变量残留
- 修复 `SimpleVariablePlaceholder` 替换路径：
  - 表达式级 placeholder 仍保持单次替换，避免误伤结构化 SQL 片段
  - 简单变量 placeholder 改为大小写无关的全量替换，覆盖同名变量多次出现
  - 使用 `Pattern.quote` 与 `Matcher.quoteReplacement`，避免变量名或替换值包含正则特殊字符时出现误替换
- 扩展 `SqlScriptRenderExamplesTest`，恢复历史样例中 `order by $部门$` 的断言
- 扩展 `SqlScriptRenderTest`，新增窄用例覆盖同一个简单变量在 SELECT 和 ORDER BY 中重复出现时都应被替换
- 补充大小写不一致变量占位和正则敏感替换值边界，确认全量替换不会受 `$`、`\` 等字符影响

已通过验证：

```bash
mvn -pl data-providers/data-provider-base -am -Dtest=datart.data.provider.jdbc.SqlScriptRenderTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl data-providers/jdbc-data-provider -am -Dtest=datart.data.provider.sql.SqlScriptRenderExamplesTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl data-providers/jdbc-data-provider -am test
git diff --check
npm run checkTs
npm run test:ci
npm run lint:css
npm run lint:style
```

验证说明：

- `SqlScriptRenderExamplesTest` 6 个用例通过，已恢复 `ORDER BY $变量$` 历史样例
- `SqlScriptRenderTest` 5 个用例通过，新增重复简单变量替换、大小写不一致和正则敏感替换值窄用例
- `mvn -pl data-providers/jdbc-data-provider -am test` 已通过，覆盖 core 3 个测试、data-provider-base 12 个测试、jdbc-data-provider 11 个启用测试，旧 `SqlScriptRenderTest` 仍跳过 6 个历史用例
- 曾并行执行两个 Maven 测试命令，定向 `data-provider-base` 测试出现一次 `SqlScriptRender$1` 类加载缺失；单独复跑 base 定向测试后通过，判断为并行 target 目录竞争
- 合入主线前完整前端门禁已通过：
  - `npm run checkTs`
  - `npm run test:ci`：132 个测试文件通过，919 个用例通过，4 个跳过
  - `npm run lint:css`
  - `npm run lint:style`

P2-G 合入状态：

- 已合入并推送 `origin/main`，主线提交 `bf6eee2e2`
- 本批次不处理 PRESTO driver 元数据缺口，保留为 P2-H 独立专题

## 10. 已完成短期目标：P2-H PRESTO JDBC driver 元数据治理

分支：`codex/modernization-presto-driver-metadata`

目标：补齐 PRESTO 内置 JDBC driver 的 quote 元数据，让 PRESTO 在无 Calcite 内置 `DatabaseProduct` 映射时仍可稳定走现有 `CustomSqlDialect` fallback，并恢复测试侧 `TestSqlDialects.PRESTO` 自动初始化。

本批次已完成：

- 为 `jdbc-driver.yml` 中的 `PRESTO` 增加：
  - `literal-quote: "'"`
  - `identifier-quote: "\""`
- 新增 `ProviderFactoryTest` 覆盖：
  - `PRESTO` 创建 `PrestoDataProviderAdapter`
  - `init=false` 下不初始化数据源
  - dialect fallback 为 `CustomSqlDialect`
  - quote 元数据正确加载
  - PRESTO 当前 `supportPaging=false` 行为保持不变
- 恢复 `TestSqlDialects.PRESTO` 自动初始化，不再在测试初始化集合中跳过 PRESTO
- 确认本批次不触碰 Java 包名 `datart.*`、配置前缀 `datart.*`、`DATART_*` 和迁移稳定标识

已通过验证：

```bash
mvn -pl data-providers/jdbc-data-provider -am -Dtest=datart.data.provider.jdbc.ProviderFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl data-providers/jdbc-data-provider -am -Dtest=datart.data.provider.sql.SqlScriptRenderExamplesTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl data-providers/jdbc-data-provider -am test
git diff --check
npm run checkTs
npm run test:ci
npm run lint:css
npm run lint:style
```

验证说明：

- `ProviderFactoryTest` 5 个用例通过
- `SqlScriptRenderExamplesTest` 6 个用例通过，PRESTO 已重新纳入历史 SQL render 样例 dialect 初始化
- `mvn -pl data-providers/jdbc-data-provider -am test` 已通过，覆盖 core 3 个测试、data-provider-base 14 个测试、jdbc-data-provider 12 个启用测试，旧 `SqlScriptRenderTest` 仍跳过 6 个历史用例
- 测试日志中 `DBType PRESTO mismatched, use custom sql dialect` 是当前预期 fallback 路径，不代表失败
- 合入主线前完整前端门禁已通过：
  - `npm run checkTs`
  - `npm run test:ci`：132 个测试文件通过，919 个用例通过，4 个跳过
  - `npm run lint:css`
  - `npm run lint:style`

P2-H 合入状态：

- 已合入并推送 `origin/main`，主线提交 `f1739f621`

## 11. 当前短期目标：P2-E 前端安全依赖与运行时治理

分支：`codex/modernization-frontend-security-deps`

目标：在同一专题分支上持续推进前端依赖安全、工具链和运行时主链现代化。优先消除补丁级、lockfile 级、overrides 可安全治理的前端依赖漏洞；完成富文本链路从 `react-quill -> quill@1.3.7` 到 Quill 2 兼容线的可验证迁移；对具备生态兼容证据和门禁结果的 Vite / TypeScript / React 主链升级，在本分支内连续推进。

本批次已完成：

- `npm audit --json` 初始结果：20 个 vulnerability 条目，包括 2 critical、10 high、6 moderate、2 low
- 直接依赖调整：
  - `lodash` `^4.17.21 -> 4.18.1`
  - `styled-components` `6.1.19 -> 6.4.2`
  - `styled-components` 从 `devDependencies` 移到 `dependencies`，匹配源码运行时真实使用方式
- 通过 npm `overrides` 收口传递依赖安全版本：
  - `lodash` / `lodash-es` `4.18.1`
  - `path-to-regexp` `8.4.2`
  - `form-data` `4.0.6`
  - `undici` `7.28.0`
  - `brace-expansion` `1.1.13`
  - `braces` `3.0.3`
  - `micromatch` `4.0.8`
  - `minimist` `1.2.8`
  - `tmp` `0.2.7`
  - `strip-ansi@4.0.0` 下的 `ansi-regex` `3.0.1`
- `tsconfig.json` 显式加入 `node` 类型，恢复 `process`、`global`、`NodeJS.Timeout` 等当前源码实际使用的 Node 24 类型边界
- 同步 `package-lock.json` 和本地 `node_modules`，确认新依赖树可解析
- 富文本编辑器安全迁移：
  - 移除直接依赖 `react-quill`
  - 新增 `react-quill-new 3.7.0`、`quill 2.0.2`、`quill-delta 5.1.0`
  - 富文本 CSS 入口切换到 `react-quill-new/dist/*`
  - Quill 2 的 `import`、`register`、Delta 和 Blot 类型差异集中在 `quillCompat.ts`
  - 自定义 `calcfield`、`tag`、`imageDrop` 注册逻辑改走兼容层
  - Vite 手工分包将 `react-quill-new`、`quill`、`quill-delta`、`parchment` 统一收口到 `quill` chunk，避免富文本运行时依赖分散
  - 清理 Quill 1 遗留的未注册 `bullet` / `mention` format；无序列表继续通过 Quill 标准 `list: 'bullet'` 值支持，引用字段继续通过 `calcfield` 支持

已通过验证：

```bash
npm audit --json
npm ls react-quill react-quill-new quill quill-delta --all
npm ci --dry-run --ignore-scripts --no-audit --no-fund
npm ls lodash lodash-es styled-components path-to-regexp form-data undici brace-expansion braces micromatch minimist tmp ansi-regex react-quill quill --all
npm run checkTs
npm run test -- src/styles/theme/__tests__/ThemeProvider.test.tsx src/app/components/ChartGraph/BasicRichText/__tests__/runtime.test.ts src/app/components/ChartGraph/BasicRichText/__tests__/RichTextEditorRuntime.test.ts src/app/components/ChartGraph/BasicRichText/__tests__/runtimeHelpers.test.ts
npm run test:ci
npm run lint:css
npm run lint:style
npm run build
git diff --check
```

验证说明：

- `npm audit --json` 已从 20 个 vulnerability 条目降到 2 个，剩余 2 个均来自 `react-quill -> quill@1.3.7`
- 迁移 Quill 2 后，`npm audit --json` 已清零
- `npm ls react-quill react-quill-new quill quill-delta --all` 已确认 `react-quill` 不在依赖树中，当前富文本链路为 `react-quill-new 3.7.0 / quill 2.0.2`
- 已评估 `react-quill-new 3.8.3 / quill 2.0.3`，该组合会触发 `GHSA-v3m3-f69x-jf25` 低危 XSS audit，暂不采用
- 已补 `utils/request` 行为基线测试，覆盖 token header、响应 token 刷新、API body 展开、header 返回和后端错误消息转换
- `axios` 已从 `1.17.0` 小版本升级到 `1.18.1`，依赖树确认 `form-data` 继续受 override 约束到 `4.0.6`
- `antd` 声明版本已从 `^5.26.2` 收口到当前 lockfile 实际解析并已验证的 `5.29.3`，避免恢复时误判 UI 基线；暂不切入 AntD 6
- 已将一组 lockfile 已实际解析并通过验证的直接依赖声明固定为当前稳定基线：`react-router-dom 6.30.4`、`i18next 26.3.1`、`classnames 2.5.1`、`split.js 1.6.5`、`postcss 8.5.15`、`eslint-plugin-prettier 5.5.6`、`stylelint-order 7.0.1`
- 已继续固定一组已由 lockfile 实际解析并验证的运行时依赖声明：`@ant-design/icons 5.6.1`、`@ant-design/pro-components 2.8.10`、`@antv/s2 2.7.2`、`@antv/s2-react 2.3.1`、`dayjs 1.11.21`、`immer 9.0.12`、`polished 4.1.4`、`react-dnd 16.0.1`、`react-dnd-html5-backend 16.0.1`、`react-hotkeys-hook 3.4.4`、`react-window 1.8.11`、`reveal.js 6.0.1`
- AntD 主题切换已从废弃的 `ConfigProvider.config({ theme })` 迁移到 `ConfigProvider theme={{ token }}`，移除测试期 `[antd: ConfigProvider] config` warning
- React Router 入口已集中通过 `app/routerCompat` 默认启用 `v7_startTransition` 和 `v7_relativeSplatPath`，移除测试期 React Router v7 future warning
- `ChartIFrameContainer` 测试已等待异步 lifecycle effect 收敛，移除 React 18 `act(...)` warning
- 已评估 `monaco-editor 0.55.1`，该版本引入的 `dompurify 3.2.7` 会让 `npm audit` 新增 2 个漏洞，暂不采用；当前继续保持 `monaco-editor 0.52.2`
- `@testing-library/react` 已从 `14.3.1` 升级到 `16.3.2`，并显式补齐 peer 依赖 `@testing-library/dom 10.4.1`
- `@testing-library/dom` 已归位到 `devDependencies`，避免测试工具链进入生产依赖声明
- 前端 lint 和 Husky 脚本已移除 `npx` 调用，统一走 npm scripts / `npm exec` 解析本地依赖，降低 Node 24 / npm 11 下隐式网络解析风险
- `npm ci --dry-run --ignore-scripts --no-audit --no-fund` 已通过，确认 `package.json` 与 `package-lock.json` 一致可安装
- `npm ls ... --all` 已通过，确认 override 后依赖树无 invalid / missing
- `npm run checkTs` 已通过
- 请求 wrapper 相关测试 3 个测试文件、15 个用例通过
- AntD / Router 兼容 warning 相关测试 3 个测试文件、5 个用例通过
- Chart iframe 容器测试 1 个测试文件、2 个用例通过
- Testing Library 16 升级后 `npm run test:ci` 已通过：135 个测试文件通过，928 个用例通过，4 个跳过
- 运行时依赖声明收口相关测试 6 个测试文件、36 个用例通过
- 相关运行时测试 4 个测试文件、13 个用例通过
- Quill 2 迁移后富文本相关测试 7 个测试文件、25 个用例通过
- 已补真实 `RichTextEditorRuntime` 挂载 smoke，覆盖 Quill 2 runtime 在 jsdom 下挂载、读取 Delta 内容，以及图表富文本预览 calcfield 渲染
- `npm run test:ci` 已通过：134 个测试文件通过，926 个用例通过，4 个跳过
- `npm run lint:css`、`npm run lint:style` 已通过
- `npm run build` 已验证 Quill 2 迁移后的生产构建链路
- `git diff --check` 已通过

验证缺口：

- 后续具备浏览器 smoke 条件时，补 Dashboard 富文本 widget、分享页富文本展示的端到端验证

P2-E 合入状态：

- 当前分支继续累计前端安全 / 运行时改造
- 当前分支继续领先 `origin/main` 多个专题提交，具体数量以恢复命令输出为准，继续在同一分支推进
- 暂不合入 `main`，减少主线合并和完整回归频率

最新批次：前端直接依赖声明固定化

- 已将 `frontend/package.json` 中剩余带 `^` 的直接依赖和开发工具依赖全部固定到当前 lockfile 已解析版本
- 本批次不拉取新版本、不改源码行为，只降低后续恢复安装时的漂移风险
- 固定范围包含 `echarts-wordcloud`、`react-draggable`、`react-grid-layout`、`react-i18next`、`react-resizable`、`redux-undo`、`eslint`、`vite`、`vitest`、`stylelint`、`prettier`、`husky`、`lint-staged`、`jsdom`、`less`、`@typescript-eslint/*`、`@testing-library/jest-dom` 等直接声明
- 直接依赖声明与 `package-lock.json` 根解析版本已对账一致，`rg -n "\^" frontend/package.json` 无剩余输出

本批次已通过验证：

```bash
npm ci --dry-run --ignore-scripts --no-audit --no-fund
npm audit --json
npm run checkTs
npm run test -- src/app/components/ChartGraph/PivotSheetChart/__tests__/runtime.test.ts src/app/pages/StoryBoardPage/__tests__/revealRuntime.test.ts src/app/components/__tests__/dndRuntime.test.ts src/app/components/__tests__/VirtualTable.test.tsx src/app/components/__tests__/Split.test.tsx src/app/pages/DashBoardPage/hooks/__tests__/useGridLayoutMap.test.ts src/app/utils/__tests__/date.test.ts src/app/pages/DashBoardPage/utils/__tests__/widget.test.ts src/app/__tests__/routerCompat.test.tsx
npm run lint:css
npm run lint:style
git diff --check
```

验证说明：

- 直接依赖声明对账无输出，说明 `package.json` 声明版本与 lockfile 根解析版本一致
- `npm audit --json` 仍为 0 vulnerabilities

最新批次：前端 npm 11 lockfile 格式收口

- 已使用当前基线 `Node 24.16.0 / npm 11.13.0` 将 `frontend/package-lock.json` 从 lockfile v2 转换到 v3
- v3 lockfile 保留 `packages` 作为唯一依赖树来源，移除 v2 兼容用的 legacy `dependencies` 镜像，降低锁文件冗余
- 本批次不升级依赖版本、不改 `package.json`，只收口 npm 11 原生 lockfile 格式

本批次验证命令：

```bash
npm install --package-lock-only --lockfile-version=3 --ignore-scripts --no-audit --no-fund
npm ci --dry-run --ignore-scripts --no-audit --no-fund
npm audit --json
npm run checkTs
node -e "const p=require('./package.json'); const lock=require('./package-lock.json'); let mismatches=[]; for (const section of ['dependencies','devDependencies']) { for (const [name, spec] of Object.entries(p[section]||{})) { const locked=lock.packages?.['node_modules/'+name]?.version; if (locked && spec !== locked) mismatches.push(section+'\t'+name+'\t'+spec+'\t'+locked); } } console.log(JSON.stringify({lockfileVersion:lock.lockfileVersion, hasDependencies:Boolean(lock.dependencies), mismatches}, null, 2));"
git diff --check
```

验证说明：

- `package-lock.json` 当前为 `lockfileVersion: 3`
- 直接依赖声明与 lockfile 根解析版本对账无差异
- `npm ci --dry-run` 已通过，`npm audit --json` 仍为 0 vulnerabilities

最新批次：前端 Node/npm 版本约束收口

- 已将 `frontend/.nvmrc` 从 `v24.0.0` 对齐到当前验证基线 `v24.16.0`
- 已在 `frontend/package.json` 增加 `packageManager: npm@11.13.0`
- 已在 `frontend/.npmrc` 增加 `engine-strict=true`，让非 Node 24 / npm 11 环境在安装期更早失败
- 本批次不升级依赖版本，只强化本地开发、CI 和 Maven bootstrap 使用同一 Node/npm 基线

本批次验证命令：

```bash
node -v
npm -v
npm config get engine-strict
npm ci --dry-run --ignore-scripts --no-audit --no-fund
npm run checkTs
npm audit --json
git diff --check
```

验证说明：

- 当前本机验证基线为 `Node v24.16.0 / npm 11.13.0`
- `npm config get engine-strict` 输出 `true`
- `npm ci --dry-run` 已通过，`npm audit --json` 仍为 0 vulnerabilities

最新批次：CI 前端工具链基线校验

- 已在 GitHub Actions 前端门禁中增加 `Verify frontend toolchain` 步骤
- CI 已从浮动 `node-version: 24.x` 改为读取 `frontend/.nvmrc`
- CI 在 `npm ci` 前显式安装并校验 `packageManager` 声明的 `npm@11.13.0`
- CI 在 `npm ci` 前输出并校验 Node/npm 基线、`engine-strict=true`、`packageManager=npm@11.13.0`
- 本批次不改变 CI 的构建、测试、lint、Maven 门禁，只让错误 Node/npm 基线更早失败

本批次验证命令：

```bash
node -e "const fs=require('fs'); const p=require('./package.json'); if (fs.readFileSync('.nvmrc','utf8').trim() !== 'v24.16.0') throw new Error('Unexpected .nvmrc'); if (p.packageManager !== 'npm@11.13.0') throw new Error('Unexpected packageManager: '+p.packageManager)"
node -v
npm -v
npm config get engine-strict
node -e "const p=require('./package.json'); if (p.packageManager !== 'npm@11.13.0') throw new Error('Unexpected packageManager: '+p.packageManager)"
npm ci --dry-run --ignore-scripts --no-audit --no-fund
npm run checkTs
git diff --check
```

验证说明：

- 当前本机验证基线为 `Node v24.16.0 / npm 11.13.0`
- `engine-strict` 和 `packageManager` 校验均通过

最新批次：前端未使用提交辅助工具链清理

- 已移除未被脚本和文档使用的 `cz-conventional-changelog`
- 已移除 `config.commitizen` 配置，保留现有 `commitlint` hook 作为提交信息校验入口
- lockfile 已清理 `commitizen`、`inquirer`、`external-editor`、`tmp`、旧 `strip-ansi@4` 等传递链路
- 已删除不再命中的 `tmp` 和 `strip-ansi@4.0.0 -> ansi-regex` overrides，保留仍被依赖树命中的 `minimist` override
- 本批次不改变运行时代码和提交校验规则，只减少旧开发工具链和过期 overrides

本批次验证命令：

```bash
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
npm ls cz-conventional-changelog commitizen inquirer external-editor tmp strip-ansi ansi-regex minimist --package-lock-only --all
npm audit --json
npm run checkTs
git diff --check
```

验证说明：

- lockfile 视角已无 `cz-conventional-changelog`、`commitizen`、`inquirer`、`external-editor`、`tmp`
- `npm audit --json` 仍为 0 vulnerabilities，依赖总数从 1002 降到 937

最新批次：前端未加载 Prettier 插件清理

- 已确认 `prettier-plugin-organize-imports` 只存在于 package 声明和 lockfile，未在 `.prettierrc` 或脚本中加载
- 已移除 `prettier-plugin-organize-imports`
- 本批次不改变 Prettier 配置和格式化行为，只减少未使用开发依赖

本批次验证命令：

```bash
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
npm ls prettier prettier-plugin-organize-imports --package-lock-only --all
npm run lint:css
npm run lint:style
npm audit --json
npm run checkTs
git diff --check
```

验证说明：

- lockfile 视角已无 `prettier-plugin-organize-imports`
- `npm audit --json` 仍为 0 vulnerabilities，依赖总数从 937 降到 936

最新批次：前端未直接使用 PostCSS 声明清理

- 已确认项目没有 `postcss.config.*`、`.postcssrc*` 或源码直接导入 `postcss`
- `postcss` 仍由 `postcss-styled-syntax`、`stylelint`、`stylelint-order`、`vite` 等工具链传递依赖提供，版本继续解析为 `8.5.15`
- 已移除根 `devDependencies.postcss`，避免把传递工具依赖声明为项目直依赖
- 本批次不改变 Stylelint、Vite 或 PostCSS 处理行为

本批次验证命令：

```bash
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
npm ls postcss postcss-styled-syntax stylelint vite --package-lock-only --all
npm run lint:css
npm run lint:style
npm ci --dry-run --ignore-scripts --no-audit --no-fund
npm run checkTs
npm audit --json
git diff --check
```

验证说明：

- lockfile 视角 `postcss` 仍解析为 `8.5.15`
- `npm run lint:css`、`npm run lint:style`、`npm run checkTs` 已通过
- `npm audit --json` 仍为 0 vulnerabilities，依赖总数保持 936
- 代表性前端测试 9 个文件、44 个用例通过
- 测试日志中仍有 AntV S2 sourcemap 和 jsdom pseudo-element 历史噪声，不影响结果

最新批次：前端 Node/npm 主线约束收口

- 已将 `frontend/package.json` 的 `engines.node` 从 `>=24.0.0` 收口为 `>=24.0.0 <25.0.0`
- 已将 `engines.npm` 从 `>=11.0.0` 收口为 `>=11.0.0 <12.0.0`
- 本批次明确当前前端只承诺 Node 24 / npm 11 主线兼容，避免未来 Node 25+ 或 npm 12+ 在安装期被 `engine-strict=true` 误放行
- `.nvmrc` 和 CI 仍固定到当前验证基线 `Node v24.16.0 / npm 11.13.0`
- 本批次不升级依赖版本、不改运行时代码

最新批次：前端品牌展示与外链默认值收口

- 已将 `Brand` 组件可见标题从 `datart` 改为 `yu-bi`，图片 alt 同步为 `yu-bi logo`
- 已将 SQL 解析异常说明中的用户可见品牌文案从 Datart 收口为 yu-bi
- 已将 SQL 解析异常一键提交 issue 的仓库地址从 `running-elephant/datart` 改为 `xybigdata/yu-bi`
- 已新增 `newIssueUrl` 单元测试，覆盖 GitHub / Gitee issue 链接都指向 yu-bi 仓库
- 已将邮件发件人默认展示名从 `Datart` 改为 `yu-bi`，保留配置键 `spring.mail.senderName` 不变
- 本批次不触碰 Java 包名 `datart.*`、配置前缀 `datart.*`、`DATART_*`、迁移常量和内部稳定标识

最新批次：前端编译目标现代化

- 已将 `frontend/tsconfig.json` 的 `compilerOptions.target` 从 `es5` 收口到 `es2020`
- 已移除仅服务 ES5 降级的 `downlevelIteration`
- 已在主 Vite 构建和 task bundle 构建中显式设置 `build.target: 'es2020'`
- 已清理 `src/task.ts` 中旧 polyfill 注释，明确 task bundle 与主 Vite 构建使用同一现代浏览器基线
- 本批次不改业务逻辑、不升级依赖版本、不触碰内部稳定标识

本批次验证命令：

```bash
npm exec -- tsc --showConfig
npm run checkTs
npm run test -- src/__tests__/task.test.ts src/utils/__tests__/utils.test.ts src/app/components/__tests__/splitRuntime.test.ts src/app/components/__tests__/virtualTableRuntime.test.ts src/app/components/__tests__/dndRuntime.test.ts
npm run build
npm run build:task
```

验证说明：

- `tsc --showConfig` 已确认有效 target 为 `es2020`
- 相关测试 5 个文件、22 个用例通过
- 主构建和 task bundle 构建均通过
- task bundle 体积从约 `432.50 kB` 降到约 `420.87 kB`，符合退出 ES5 降级后的预期

最新批次：前端格式化门禁稳定性收口

- 已将 Prettier `endOfLine` 从 `auto` 固定为 `lf`，避免不同本地系统和 CI 产生换行漂移
- 已将 lint-staged 的 Prettier 文件范围从 `*.{css,md,json}` 扩展为 `*.{css,md,json,mjs}`，覆盖当前 ESLint / commitlint 等 `.mjs` 配置文件
- 已用当前 Prettier 配置格式化 `eslint.config.mjs`
- 本批次不升级依赖版本、不改运行时代码

本批次验证命令：

```bash
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
npm exec -- prettier --check .prettierrc package.json package-lock.json eslint.config.mjs commitlint.config.mjs vite.config.mts vite.task.config.mts tsconfig.json src/task.ts
npm run eslint -- eslint.config.mjs
npm run checkTs
npm audit --json
npm ci --dry-run --ignore-scripts --no-audit --no-fund
git diff --check
```

最新批次：前端 TypeScript 模块解析收口

- 已将 `frontend/tsconfig.json` 的 `moduleResolution` 从旧 `node` 收口为 `bundler`
- `tsc --showConfig` 已确认有效解析策略从 `node10` 变为 `bundler`
- 已移除 `eslint.config.mjs` 中未使用的 `@eslint/js` 导入
- 本批次让 TypeScript 解析模型更贴近 Vite / ESM bundler 实际构建链路，不改业务逻辑、不升级依赖版本

本批次验证命令：

```bash
npm exec -- tsc --showConfig
npm run checkTs
npm run test -- src/__tests__/task.test.ts src/app/components/__tests__/splitRuntime.test.ts src/app/components/__tests__/virtualTableRuntime.test.ts src/app/components/__tests__/dndRuntime.test.ts src/app/components/ChartGraph/BasicRichText/__tests__/runtime.test.ts
npm run eslint -- eslint.config.mjs
npm exec -- prettier --check eslint.config.mjs tsconfig.json
npm run build
npm run build:task
```

验证说明：

- 相关测试 5 个文件、13 个用例通过
- 主构建和 task bundle 构建均通过

最新批次：前端测试依赖边界收口

- 已确认 `@testing-library/dom` 只服务测试工具链和 `@testing-library/react` peer 依赖，源码运行时无引用
- 已将 `@testing-library/dom 10.4.1` 从 `dependencies` 移到 `devDependencies`
- 已同步 `package-lock.json`，确认根声明只保留 dev 依赖，`node_modules/@testing-library/dom` 标记为 `dev: true`
- 本批次不升级版本、不改源码行为，只收口生产依赖边界

本批次验证命令：

```bash
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
npm ls @testing-library/dom @testing-library/react @testing-library/jest-dom @testing-library/user-event --all
npm audit --json
npm run checkTs
npm run test -- src/styles/theme/__tests__/ThemeProvider.test.tsx src/app/__tests__/routerCompat.test.tsx src/app/components/__tests__/VirtualTable.test.tsx src/app/components/FormGenerator/__tests__/BasicFont.test.tsx
git diff --check
```

验证说明：

- `npm audit --json` 仍为 0 vulnerabilities，生产依赖计数从 305 降到 293
- Testing Library 相关定向测试 4 个文件通过，13 个用例通过，1 个历史 skip
- 测试日志中的 jsdom pseudo-element `getComputedStyle` warning 是既有噪声，不影响结果

最新批次：前端本地工具链脚本收口

- 已将 `lint:css`、`lint:style`、`lint:style:staged` 从 `npx stylelint` 改为直接调用 npm scripts 可解析的本地 `stylelint`
- 已将 `prepare` 从 `npx --prefix frontend husky ...` 改为 `npm exec --prefix frontend -- husky ...`
- 已将 Husky `pre-commit` 从 `npx lint-staged` 改为 `npm exec -- lint-staged`
- 已将 Husky `commit-msg` 从 `npx --prefix frontend --no -- commitlint ...` 改为 `npm exec --prefix frontend -- commitlint ...`
- 仓库非依赖目录已无 `npx` 残留；本批次不升级依赖版本、不改变门禁语义

本批次验证命令：

```bash
rg -n "npx" . --glob '!frontend/node_modules/**' --glob '!node_modules/**' --glob '!**/target/**' --glob '!**/dist/**'
npm run lint:css
npm run lint:style
npm exec --prefix frontend -- commitlint --version
cd frontend && npm exec -- lint-staged --version
npm exec --prefix frontend -- husky frontend/.husky
git diff --check
```

验证说明：

- `npm run lint:css` 和 `npm run lint:style` 已通过，确认 stylelint 本地 bin 可解析
- `commitlint --version` 输出 `@commitlint/cli@21.0.2`，`lint-staged --version` 输出 `17.0.8`
- Husky 初始化命令普通沙箱因无法写 `.git/config` 失败，提权复跑通过

最新批次：前端 lockfile 驱动安装入口收口

- 已将前端 `bootstrap` 脚本从 `npm install` 改为 `npm ci`
- Maven `server` 模块仍调用 `npm run bootstrap`，因此打包链路会自动使用 lockfile 驱动安装
- README / README_zh 前端本地初始化命令同步改为 `npm ci`
- 本批次不升级依赖版本、不改构建产物，只减少安装时的版本漂移风险

本批次验证命令：

```bash
npm ci --dry-run --ignore-scripts --no-audit --no-fund
npm run bootstrap -- --dry-run --ignore-scripts --no-audit --no-fund
npm run checkTs
npm audit --json
git diff --check
```

验证说明：

- `npm ci --dry-run --ignore-scripts --no-audit --no-fund` 已通过，确认 lockfile 可驱动安装
- `npm run bootstrap -- --dry-run --ignore-scripts --no-audit --no-fund` 已通过，确认 Maven 调用的 bootstrap 脚本入口可用
- `npm audit --json` 仍为 0 vulnerabilities

最新批次：前端构建配置重复治理

- 已新增 `frontend/vite.shared.mts`，集中维护 Vite / Vitest 共用 alias 配置
- 主 Vite 构建、task bundle 构建和 Vitest 已统一调用 `createViteAliases`
- 主构建和 task bundle 中重复的 Less `~` import 兼容插件、CRA SVG `ReactComponent` 兼容插件已抽到共享模块
- ESLint flat config 已覆盖 `.mjs` / `.mts`，lint-staged 已为配置文件单独执行 ESLint + Prettier
- 兼容插件名称保持 `datart-*` 不变，仅代表内部历史兼容标识，不触碰 Java 包名、配置前缀和迁移稳定标识
- 本批次不升级依赖版本、不改业务逻辑，目标是降低后续 Vite / Vitest 工具链升级时的配置漂移风险
- Less preprocessor 选项暂不抽象，避免为去重引入不清晰的 Less 内部类型边界

本批次验证命令：

```bash
npm run checkTs
npm run test -- src/__tests__/task.test.ts src/app/components/__tests__/splitRuntime.test.ts src/app/components/__tests__/virtualTableRuntime.test.ts src/app/components/__tests__/dndRuntime.test.ts src/app/components/ChartGraph/BasicRichText/__tests__/runtime.test.ts
npm run build
npm run build:task
npm exec -- vitest run src/utils/__tests__/utils.test.ts
npm run eslint -- eslint.config.mjs vite.config.mts vite.task.config.mts vitest.config.mts vite.shared.mts
npm exec -- prettier --check package.json eslint.config.mjs vite.config.mts vite.task.config.mts vitest.config.mts vite.shared.mts
npm audit --json
npm ci --dry-run --ignore-scripts --no-audit --no-fund
git diff --check
```

验证说明：

- `.mjs` / `.mts` 配置文件已被 ESLint 实际检查，不再出现 no matching configuration warning
- 代表性测试 6 个文件、24 个用例通过
- 主构建和 task bundle 构建均通过
- `npm audit --json` 仍为 0 vulnerabilities
- 主构建仍有既有大 chunk warning，本批次未处理包体拆分

最新批次：前端 Vite 8 主链升级

- 已将 `vite` 从 `6.4.3` 升级到 `8.0.16`
- 已将 `@vitejs/plugin-react` 从 `4.7.0` 升级到 `5.2.0`
- 当前暂不采用 `@vitejs/plugin-react 6.0.2`：npm 11 解析其可选 `@rolldown/plugin-babel` / Babel 8 peer 链时出现 `ERESOLVE`
- 当前不采用 Vite 7 作为中间线：`7.2.7` 会触发 Vite high audit，`7.3.5` 会引入 `esbuild 0.27.7` low audit
- Vite 8 当前通过 `rolldown 1.0.3` / `lightningcss 1.32.0` 构建链路，`npm audit --json` 仍为 0 vulnerabilities
- AntD 6 暂不升级：稳定版 `@ant-design/pro-components 2.8.10` peer 只支持 AntD 4/5，支持 AntD 6 的 3.x 仍是预发布链路
- React 19、TypeScript 6 继续保留为独立高风险评估，不并入本批次

本批次验证命令：

```bash
npm exec -- vite --version
npm run checkTs
npm run test -- src/__tests__/task.test.ts src/app/components/__tests__/splitRuntime.test.ts src/app/components/__tests__/virtualTableRuntime.test.ts src/app/components/__tests__/dndRuntime.test.ts src/app/components/ChartGraph/BasicRichText/__tests__/runtime.test.ts
npm exec -- vitest run src/utils/__tests__/utils.test.ts src/app/components/MonacoEditor/__tests__/runtime.test.ts src/app/pages/StoryBoardPage/__tests__/revealRuntime.test.ts
npm run eslint -- eslint.config.mjs vite.config.mts vite.task.config.mts vitest.config.mts vite.shared.mts
npm run build
npm run build:task
npm audit --json
npm ci --dry-run --ignore-scripts --no-audit --no-fund
git diff --check
```

验证说明：

- 本地实际 Vite 版本已确认为 `vite/8.0.16 darwin-arm64 node-v24.16.0`
- 代表性测试 8 个文件、32 个用例通过
- 主构建和 task bundle 构建均通过，输出均显示 `vite v8.0.16`
- task bundle 从约 `420.87 kB` 增至约 `454.88 kB`，后续如需优化包体，单独进入构建产物治理
- `npm audit --json` 仍为 0 vulnerabilities

最新批次：前端 TypeScript 6 主链升级

- 已将 `typescript` 从 `5.9.3` 升级到 `6.0.3`
- 已将 `@typescript-eslint/parser`、`@typescript-eslint/eslint-plugin` 从 `8.61.1` 升级到 `8.62.0`
- `@typescript-eslint 8.62.0` peer 支持 `typescript >=4.8.4 <6.1.0`，与 TypeScript 6.0.3 兼容
- 已移除 TypeScript 6 标记废弃的 `compilerOptions.baseUrl`
- 已使用显式 `paths` 覆盖现有 `app/*`、`redux/*`、`styles/*`、`utils/*`、`types`、`globalConstants`、`entryPointFactory` 等绝对导入
- 本批次不使用 `ignoreDeprecations` 掩盖废弃项，直接完成 TS 解析配置迁移
- React 19、AntD 6 继续保留为独立高风险评估，不并入本批次

本批次验证命令：

```bash
npm exec -- tsc --version
npm run checkTs
npm exec -- tsc --showConfig
npm run test -- src/__tests__/task.test.ts src/app/components/__tests__/splitRuntime.test.ts src/app/components/__tests__/virtualTableRuntime.test.ts src/app/components/__tests__/dndRuntime.test.ts src/app/components/ChartGraph/BasicRichText/__tests__/runtime.test.ts src/app/models/__tests__/ChartDataRequestBuilder.test.ts src/utils/__tests__/utils.test.ts
npm run build
npm run build:task
npm audit --json
npm ci --dry-run --ignore-scripts --no-audit --no-fund
git diff --check
```

验证说明：

- 本地实际 TypeScript 版本已确认为 `Version 6.0.3`
- `npm run checkTs` 已通过
- `tsc --showConfig` 已确认不再包含 `baseUrl`，路径解析由显式 `paths` 承接
- 代表性测试 7 个文件、46 个用例通过
- 主构建和 task bundle 构建均通过，继续使用 Vite 8.0.16
- `npm audit --json` 仍为 0 vulnerabilities

最新批次：前端 React 19 运行时升级

- 已将 `react`、`react-dom` 从 `18.3.1` 升级到 `19.2.7`
- 已将 `@types/react` 从 `18.3.31` 升级到 `19.2.17`
- 已将 `@types/react-dom` 从 `18.3.7` 升级到 `19.2.3`
- 依赖树已确认 AntD 5.29.3、Pro Components 2.8.10、Testing Library 16.3.2、react-grid-layout 2.2.3、react-quill-new 3.7.0、react-helmet-async 3.0.0 等当前主链可解析到 React 19
- 已补 React 19 类型适配：
  - `useRef<T>()` 改为显式 `undefined` 或 `null` 初始化
  - 全局 `JSX.Element` 改为 `React.JSX.Element` 或显式导入 `JSX`
  - `React.ReactChild` 改为 `React.ReactNode`
  - `cloneElement` 注入 props 的旧写法集中通过 `utils/reactCompat.ts` 收窄
  - `react-dnd` connector 作为 `ref` 时改为 void-return callback 包装
  - 真实初始值可能为 `null` 的 DOM ref hook 签名改为 nullable
- 本批次只做 React 19 运行时和类型兼容，不升级 AntD 6，不触碰 Java 包名、配置前缀、`DATART_*` 或迁移稳定标识

本批次验证命令：

```bash
npm ls react react-dom @types/react @types/react-dom --all
npm run checkTs
npm run test -- src/__tests__/task.test.ts src/app/__tests__/routerCompat.test.tsx src/styles/theme/__tests__/ThemeProvider.test.tsx src/app/components/__tests__/VirtualTable.test.tsx src/app/components/__tests__/Split.test.tsx src/app/components/ChartGraph/BasicRichText/__tests__/runtime.test.ts
npm exec -- vitest run src/app/models/__tests__/ReactLifecycleAdapter.test.ts src/app/models/__tests__/ReactChart.test.ts src/app/components/ChartIFrameContainer/__tests__/ChartIFrameContainer.test.jsx
npm run build
npm run build:task
npm run eslint -- eslint.config.mjs vite.config.mts vite.task.config.mts vitest.config.mts vite.shared.mts
npm exec -- prettier --check package.json
npm audit --json
npm ci --dry-run --ignore-scripts --no-audit --no-fund
git diff --check
```

验证说明：

- `npm ls react react-dom @types/react @types/react-dom --all` 已确认依赖树统一解析到 React 19.2.7 / React DOM 19.2.7 / React types 19.2.x，无 invalid 依赖
- `npm run checkTs` 已通过
- 第一组代表性测试 6 个文件、14 个用例通过；日志中 jsdom pseudo-element `getComputedStyle` warning 为既有噪声
- React lifecycle / ReactChart / ChartIFrameContainer 相关测试 3 个文件、8 个用例通过
- 主构建和 task bundle 构建均通过，继续使用 Vite 8.0.16
- `npm audit --json` 仍为 0 vulnerabilities
- `npm ci --dry-run --ignore-scripts --no-audit --no-fund` 已通过

最新批次：前端工具链补丁线升级

- 已将 `less` 从 `4.6.6` 升级到 `4.6.7`
- 已将 `stylelint-order` 从 `7.0.1` 升级到 `8.1.1`
- 已将 `vite-plugin-svgr` 从 `4.5.0` 升级到 `5.2.0`
- `stylelint-order 8.1.1` peer 支持 `stylelint ^16.18.0 || ^17.0.0`，与当前 `stylelint 17.13.0` 兼容
- `vite-plugin-svgr 5.2.0` peer 支持 `vite >=3.0.0`，已在当前 Vite 8 构建链路验证通过
- 本批次只处理开发 / 构建工具链补丁线和小版本升级，不改业务代码，不升级 React Router 7、ECharts 6、AntD 6、react-window 2 等高风险主版本

本批次验证命令：

```bash
npm ls less stylelint-order vite-plugin-svgr --all
npm run lint:css
npm run lint:style
npm run checkTs
npm run build
npm run build:task
npm run eslint -- eslint.config.mjs vite.config.mts vite.task.config.mts vitest.config.mts vite.shared.mts
npm exec -- prettier --check package.json
npm audit --json
npm ci --dry-run --ignore-scripts --no-audit --no-fund
git diff --check
```

验证说明：

- 依赖树已确认 `less 4.6.7`、`stylelint-order 8.1.1`、`vite-plugin-svgr 5.2.0` 均解析到目标版本，无 invalid 依赖
- `npm run lint:css`、`npm run lint:style` 已通过
- `npm run checkTs` 已通过
- 主构建和 task bundle 构建均通过，继续使用 Vite 8.0.16
- 配置文件 ESLint、`package.json` Prettier 检查和 `git diff --check` 均通过
- `npm audit --json` 仍为 0 vulnerabilities
- `npm ci --dry-run --ignore-scripts --no-audit --no-fund` 已通过

本轮暂缓项：

- AntD 6 暂缓：`antd` 最新稳定为 `6.4.5`，但最新稳定 `@ant-design/pro-components 2.8.10` peer 仍只支持 `antd ^4.24.15 || ^5.11.2`
- `react-window` 2 暂缓：`react-window 2.2.7` 不再导出当前 `VirtualTable` 依赖的 `VariableSizeGrid`，新 API 改为 `Grid` / `cellComponent` / `gridRef`，需要独立组件适配专题
- `quill 2.0.3` / `react-quill-new 3.8.3` 暂缓：此前已确认会触发 `GHSA-v3m3-f69x-jf25` 低危 XSS audit
- `monaco-editor 0.55.1` 暂缓：此前已确认会通过 `dompurify 3.2.7` 引入新的 audit 风险

## 12. 后续队列

| 阶段 | 事项 | 风险 | 执行策略 |
| --- | --- | --- | --- |
| P2-D | `react-window` 2.x 适配 | 中高 | 独立专题，当前 `VariableSizeGrid` API 已被 2.x 移除，需要改造 `VirtualTable` 到新 `Grid` API |
| P2-F | AntD 6 主版本评估 | 高 | 稳定版 Pro Components 尚未支持 AntD 6，继续等待稳定生态链路 |
| P2-I | 数据源 provider / 方言依赖审计 | 高 | 先盘点依赖树和驱动兼容，不做大规模重构 |
| P2-J | 富文本编辑器运行时 smoke | 中 | P2-E 已迁移 Quill 2，后续补浏览器层编辑 / 预览 / 分享页验证 |

## 13. 门禁策略

开发期按风险分层验证，不为每个小改动跑完整门禁。提交前做本批次相关门禁；准备合入 `main` 或推送 `main` 前做完整门禁。

| 场景 | 最低门禁 |
| --- | --- |
| 文档或纯元数据 | `git diff --check` |
| 前端类型边界、小范围组件迁移 | `npm run checkTs` + 相关测试 |
| helper、模型、共享协议变化 | `npm run checkTs` + 相关模型 / helper 测试 |
| 依赖、构建配置、运行时加载变化 | `npm run checkTs` + 相关运行时测试；专题收尾补 `npm run test:ci` |
| Maven、Docker、安装包链路变化 | `mvn package -DskipTests`，必要时补 demo smoke |
| 准备 merge 回 `main` | 前端完整门禁，必要时补后端门禁 |
| 推送 `main` | 不跳过完整门禁 |

完整前端门禁：

```bash
npm run checkTs
npm run test:ci
npm run lint:css
npm run lint:style
```

构建与安装包门禁：

```bash
mvn package -DskipTests
scripts/check-demo-health.sh
```

依赖链路补充门禁：

```bash
npm install --package-lock-only --dry-run --ignore-scripts
npm ci --dry-run --ignore-scripts
```

测试缺口处理：

- 找不到现成相关测试时，必须记录缺口
- 优先补 helper、协议、运行时最小用例
- 不为了覆盖率硬造低价值快照测试
- 本机缺少外部工具时记录原因，例如当前无 `docker` 命令，不能本地验证 Docker build

## 14. 提交节奏

同一专题内累计一组相关改动后再提交，减少主线合并和完整回归次数。专题分支可以阶段性 push 远端保存进度，但不要因为单个小批次改动就合入 `main`。

建议粒度：

| 类型 | 粒度 |
| --- | --- |
| 低风险类型边界 | 累计 3 到 10 个相关文件后提交 |
| 中风险运行时链路 | 每条可验证链路独立提交 |
| 依赖和构建链路 | 独立提交，但尽量包含完整链路文档和验证记录 |
| 阶段复盘 | 跟随当前批次提交，必要时可单独文档提交 |

不要因为单个小文件改动立刻提交。当前 P2-E 应围绕前端安全依赖治理累计依赖升级、lockfile、门禁和文档记录后再提交。

## 15. 恢复命令

继续 P2-E：

```bash
git status --short --branch
git rev-list --left-right --count origin/main...HEAD
npm audit --json
npm ls react-quill react-quill-new quill quill-delta --all
npm ci --dry-run --ignore-scripts --no-audit --no-fund
npm run checkTs
npm run test:ci
```

追溯历史：

```bash
git log --oneline -- docs/tech-stack-modernization-plan.md
git log --oneline -- Dockerfile Deployment.md server/pom.xml bin/yu-bi-server.sh bin/yu-bi-server.cmd
git log --oneline -- security/src/main/java/datart/security/manager/shiro security/src/test/java/datart/security
git log --oneline -- frontend/package.json frontend/package-lock.json
```
