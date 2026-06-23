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
| 当前专题 | P2-E 前端安全依赖治理 |
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

## 3. 分支与合并规则

固定规则：

- 不直接在 `main` 开发
- 按专题使用 `codex/*` 分支
- 专题分支可以推送远端
- 专题完成后再 `--no-ff` merge 回 `main`
- 推送 `main` 前必须完整门禁

当前专题分支：

```bash
codex/modernization-frontend-security-deps
```

当前专题收口前不要创建新分支。P2-E 聚焦前端安全依赖治理：优先处理补丁级升级、lockfile、npm overrides 和可验证的富文本 Quill 2 迁移；本专题继续累计在 `codex/modernization-frontend-security-deps`，暂不因为单个小批次合入 `main`。

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
| Node | `>=24.0.0` | 硬性目标 |
| npm | `>=11.0.0` | 与 Node 24 配套 |
| React | `18.3.1` | 当前稳定主链 |
| React Router | `6.30.4` | 已收口到 React Router 6 稳定线，并通过兼容层启用 v7 future flags |
| Ant Design | `5.29.3` | 已收口到 AntD 5 稳定线，并迁移到 v5 theme token API |
| Redux Toolkit | `2.12.0` | 已完成主升级 |
| React Redux | `9.3.0` | 已完成主升级 |
| TypeScript | `5.9.3` | 当前稳定主链 |
| Vite | `6.4.3` | 已替代 CRA 主工作流 |
| Vitest | `4.1.9` | 当前主测试栈 |
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
- 前端已建立 `Node 24 + React 18 + Ant Design 5 + Vite 6 + Vitest 4` 主链
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

## 11. 当前短期目标：P2-E 前端安全依赖治理

分支：`codex/modernization-frontend-security-deps`

目标：在不升级 React/AntD/Vite 主版本的前提下，优先消除补丁级、lockfile 级、overrides 可安全治理的前端依赖漏洞，并完成富文本链路从 `react-quill -> quill@1.3.7` 到 Quill 2 兼容线的可验证迁移。

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
- AntD 主题切换已从废弃的 `ConfigProvider.config({ theme })` 迁移到 `ConfigProvider theme={{ token }}`，移除测试期 `[antd: ConfigProvider] config` warning
- React Router 入口已集中通过 `app/routerCompat` 默认启用 `v7_startTransition` 和 `v7_relativeSplatPath`，移除测试期 React Router v7 future warning
- `ChartIFrameContainer` 测试已等待异步 lifecycle effect 收敛，移除 React 18 `act(...)` warning
- `npm ci --dry-run --ignore-scripts --no-audit --no-fund` 已通过，确认 `package.json` 与 `package-lock.json` 一致可安装
- `npm ls ... --all` 已通过，确认 override 后依赖树无 invalid / missing
- `npm run checkTs` 已通过
- 请求 wrapper 相关测试 3 个测试文件、15 个用例通过
- AntD / Router 兼容 warning 相关测试 3 个测试文件、5 个用例通过
- Chart iframe 容器测试 1 个测试文件、2 个用例通过
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
- 暂不合入 `main`，减少主线合并和完整回归频率

## 12. 后续队列

| 阶段 | 事项 | 风险 | 执行策略 |
| --- | --- | --- | --- |
| P2-D | `react-window` 2.x 可行性评估 | 中高 | 独立专题，先验证 `VariableSizeGrid` 替换路径 |
| P2-F | React 19、AntD 6、Vite 8、TypeScript 6 主版本评估 | 高 | 独立专题，先建立兼容矩阵和关键页面 smoke test |
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

同一专题内累计一组相关改动后再提交，减少主线合并和完整回归次数。

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
