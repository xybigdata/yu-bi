# yu-bi 现代化改造执行板

本文档是 yu-bi 现代化改造的恢复入口、阶段复盘和后续执行清单。恢复工作时先读“恢复必读”和“后续队列”，再继续当前专题分支，不从历史批次流水里反推执行策略。

复盘时间：2026-06-25

## 恢复必读

当前目标不是追最新，而是在兼容、正确和可验证的前提下，把技术栈收口到较新的稳定版。硬性基线保持：

- `JDK 21`
- `Node 24`
- `npm 11`

当前长期专题分支：

```bash
codex/modernization-frontend-security-deps
```

当前执行规则：

- 继续在这一条分支上推进，不因为单个小批次完成就新建分支
- 不主动合并 `main`，也不因为 `main` 落后较多就自动合并
- 专题分支可以阶段性 push 保存进度
- 只有当前专题累计到一批可验收成果，并且用户明确要求阶段合并时，才准备合入 `main`
- 推送 `main` 前必须完整门禁；日常开发按风险分层执行相关门禁
- 提交节奏按“一组相关改动”提交，避免单文件小改动频繁提交

当前优先队列：

| 优先级 | 事项                             | 当前策略                                                                |
| ------ | -------------------------------- | ----------------------------------------------------------------------- |
| P0     | 分支纪律和执行文档同步           | 持续保持本文档与最新用户指令一致，避免恢复时误用旧目标文本              |
| P1     | 分享页 / 图表运行时 smoke        | 补最小可维护测试，优先覆盖分享页只读图表和富文本展示链路                |
| P1     | 构建产物治理                     | JS gzip 超限已清零；继续观察 raw 维度的 `monacoEditor`、`antdDesign` 等 |
| P2     | 前端剩余 outdated 复核           | 暂缓重复试装，只在 peer、audit 或生态条件变化时复核                     |
| P2     | 后端补丁线滚动复核               | 只处理同生态线、可验证的补丁升级；不触碰高风险内部稳定标识              |
| 暂缓   | AntD 6 / ESLint 10 / Quill 2.0.3 | 等待生态兼容或 audit 风险解除后再评估                                   |

## 0. 当前执行口径

当前阶段优先在同一条专题分支上连续推进，减少 `main` 分支合并和推送频率。恢复工作时先看本节和“后续队列”，不要从历史批次流水里反推执行策略。

- 当前默认工作分支：`codex/modernization-frontend-security-deps`
- 不直接在 `main` 开发
- 不因为小批次改动创建新分支
- 不因为小批次改动合入 `main`
- 当前专题没有明确收口前，不新建下一条专题分支
- 专题分支可以阶段性 push 到远端保存进度
- 只有当前专题累计到一批可验收成果，并且用户明确要求阶段合并时，才准备 `main` 合并
- 准备合入 `main` 或推送 `main` 前必须执行完整门禁
- 日常开发按风险分层跑相关门禁，不为每个小改动执行完整门禁
- 同一专题内尽量累计一组相关改动后再提交；提交后可 push 当前专题分支，但不自动 merge `main`

当前短期执行指令：

1. 继续在 `codex/modernization-frontend-security-deps` 上推进。
2. 先处理能被现有门禁证明正确的前端安全、构建、运行时、工具链改造。
3. 中高风险项可以同步评估和补基线；只有证据充分时才升级，不满足条件就记录暂缓原因。
4. 不主动合并 `main`，即使 `main` 落后较多，也等用户明确要求阶段合并。

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

| 项目                       | 状态                                                       |
| -------------------------- | ---------------------------------------------------------- |
| 工作目录                   | `/Users/chencongyu/WorkHome/VSProjects/open-project/yu-bi` |
| 远端                       | `git@github.com:xybigdata/yu-bi.git`                       |
| 主线分支                   | `main`                                                     |
| 当前专题分支               | `codex/modernization-frontend-security-deps`               |
| 当前专题                   | P2-E 前端安全依赖与运行时治理                              |
| 当前分支相对 `origin/main` | `0 95`，以恢复时重新执行命令为准                           |
| 最近专题提交               | `bddd37cdb test: 补强 Monaco 业务入口 smoke`               |
| 最近主线提交               | `f1739f621 chore: 合入 PRESTO driver 元数据治理`           |

已确认的自动化权限和偏好：

- 可以自动执行 `git add`
- 可以自动执行 `git commit --no-verify -m "..."`
- 可以自动执行 `git push origin <branch>`
- `npm view ...` 查询已授权，后续不再单独询问
- `npm audit ...` 查询已授权，后续不再单独询问
- `mvn -pl security ...` 命令已授权，后续不再单独询问
- 同一专题内尽量累计一组相关改动后再提交，避免过频繁提交
- 当前专题继续在同一分支推进，不因为小批次改动立即合入 `main`
- 减少 `main` 分支合并频率；前端安全和运行时相关改造优先在 `codex/modernization-frontend-security-deps` 上连续推进
- 除非用户明确要求阶段合并，否则专题分支只推送远端，不主动 merge 回 `main`
- 目标模式中的“专题完成后 push 再 merge 到 main”按最新执行口径理解为：专题分支可持续 push 保存进度，但 `main` 合并要尽量少，只在一批可验收成果完成且用户明确要求时统一处理
- 最新用户指令优先级高于目标旧文本：不要因为分支领先 `main` 很多就主动合并；当前阶段默认继续在一个分支上干活
- 当前分支已经累计较多现代化提交，后续仍优先继续在本分支补齐前端安全、运行时、构建产物治理和必要的后端补丁线复核；不要为了“分支领先太多”主动切分支或合并主线

## 3. 分支与合并规则

固定规则：

- 不直接在 `main` 开发
- 按专题使用 `codex/*` 分支
- 专题分支可以推送远端
- 同一专题尽量长期保留一条分支连续推进，避免为小批次改动频繁创建新分支
- 当前专题未收口前，默认不创建新分支
- 当前专题未收口前，默认不合入 `main`
- 专题累计到一批可验收成果，并且用户明确要求阶段合并后，再按 `--no-ff` merge 回 `main`
- 推送 `main` 前必须完整门禁
- 不把“阶段性 push 专题分支”理解为“阶段性 merge main”
- 不把“main 落后较多”作为自动合并理由

当前专题分支：

```bash
codex/modernization-frontend-security-deps
```

当前专题收口前不要创建新分支。P2-E 聚焦前端安全依赖、前端工具链和运行时主链治理：优先处理补丁级升级、lockfile、npm overrides、可验证的富文本 Quill 2 迁移，以及已经具备验证证据的 Vite / TypeScript / React 主链升级；本专题继续累计在 `codex/modernization-frontend-security-deps`，暂不因为单个小批次合入 `main`。

## 4. 技术栈基线

### 4.1 后端

| 技术栈              | 当前基线                             | 状态                                                           |
| ------------------- | ------------------------------------ | -------------------------------------------------------------- |
| Java                | `21`                                 | 已达硬性目标                                                   |
| Maven               | `>=3.9`                              | 已由 Enforcer 约束                                             |
| JavaCC Maven Plugin | `3.2.0`                              | 已验证 SQL parser clean 重生成链路                             |
| Spring Boot         | `3.5.15`                             | Boot 3.5 补丁线，继续保持 Spring Framework 6 / Security 6 主链 |
| Spring Cloud        | `2025.0.1`                           | 与 Boot 3.5 配套                                               |
| Spring Security     | `6.5.11`                             | 由 Boot 3.5.15 管理，暂不跳 Spring Security 7                  |
| MyBatis Spring Boot | `3.0.5`                              | 已适配 Boot 3                                                  |
| MyBatis Generator   | `1.4.2`                              | 继续保留 1.x 线，插件 profile 依赖版本已补齐                   |
| GraalJS             | `25.0.3`                             | 已替代 Nashorn 主链，当前保持 25.0.x 补丁线                    |
| BouncyCastle        | `1.84`                               | 已统一到 `jdk18on` 组件线                                      |
| Springdoc           | `2.8.17`                             | 已适配 Boot 3                                                  |
| H2                  | `2.4.240`                            | 已升级                                                         |
| Hibernate Validator | `8.0.4.Final`                        | 继续保持 Jakarta Validation 3 线，HV 9 暂缓                    |
| MySQL Connector/J   | `9.7.0`                              | 通过 Boot BOM 属性覆盖完成补丁升级，JDBC provider 基线已验证   |
| HikariCP            | `7.1.0`                              | 已补 JDBC 配置映射测试后通过 Boot BOM 属性覆盖升级             |
| Selenium            | `4.45.0`                             | 已升级到 Selenium 4 稳定线较新补丁版本                         |
| JsonPath            | `3.0.0`                              | 已补 OAuth 属性映射行为测试后升级                              |
| DingTalk SDK        | `1.1.100`                            | 继续保持 1.x 线补丁升级，暂不跳 DingTalk 2                     |
| Maven 仓库          | Maven Central / Spring Boot 默认仓库 | 已移除不再需要的 Spring milestone 仓库声明                     |
| Shiro               | `2.0.6`                              | 高风险，只做认证授权边界审计和小步修复；Shiro 3 alpha 暂缓     |
| Druid               | `1.2.28`                             | 中风险，暂不优先                                               |
| Calcite             | 现网主链                             | 高风险，先补 SQL 解析兼容样例                                  |

### 4.2 前端

| 技术栈                  | 当前基线                                     | 状态                                                                                                          |
| ----------------------- | -------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| Node                    | `>=24.0.0 <25.0.0`                           | 硬性目标，当前验证基线 `24.16.0`                                                                              |
| npm                     | `>=11.0.0 <12.0.0`                           | 与 Node 24 配套，当前验证基线 `11.13.0`                                                                       |
| React                   | `19.2.7`                                     | 已升级到 React 19 稳定线                                                                                      |
| React Router            | `7.18.0`                                     | 已升级到 React Router 7 稳定线                                                                                |
| Ant Design              | `5.29.3`                                     | 已收口到 AntD 5 稳定线，并迁移到 v5 theme token API                                                           |
| @ant-design/icons       | `6.2.5`                                      | 直接依赖已升级；AntD / Pro Components 内部仍保留 5.x                                                          |
| Redux Toolkit           | `2.12.0`                                     | 已完成主升级                                                                                                  |
| React Redux             | `9.3.0`                                      | 已完成主升级                                                                                                  |
| Immer                   | `11.1.8`                                     | 已与 Redux Toolkit 依赖线去重                                                                                 |
| TypeScript              | `6.0.3`                                      | 已升级到 6.0 稳定线，`baseUrl` 已迁移为显式 `paths`                                                           |
| Vite                    | `8.1.0`                                      | 已升级到 Vite 8 主链，暂配 `@vitejs/plugin-react 5.2.0`                                                       |
| Vitest                  | `4.1.9`                                      | 当前主测试栈                                                                                                  |
| Testing Library         | `@testing-library/react 16.3.2 / dom 10.4.1` | 已对齐 React 19 测试栈                                                                                        |
| Less                    | `4.6.7`                                      | 已完成补丁升级；保留用于满足 Vite / AntV S2 peer，源码构建侧已退出 `~` import 兼容                            |
| vite-plugin-svgr        | `5.2.0`                                      | 已完成 Vite 8 下构建验证                                                                                      |
| stylelint-order         | `8.1.1`                                      | 已完成 Stylelint 17 下 lint 验证                                                                              |
| styled-components       | `6.4.3`                                      | 已完成主升级并继续保持 6.x 补丁线                                                                             |
| polished                | `4.3.1`                                      | 已完成样式工具小版本升级                                                                                      |
| 富文本编辑器            | `react-quill-new 3.7.0 / quill 2.0.2`        | 已完成 Quill 2 迁移；暂不升 `react-quill-new 3.8.x / quill 2.0.3`，因为会重新引入 npm audit 低危 XSS advisory |
| monaco-editor           | `0.52.2`                                     | 已补真实运行时加载边界                                                                                        |
| reveal.js               | `6.0.1`                                      | 已补真实运行时加载边界                                                                                        |
| ECharts                 | `6.1.0`                                      | 已升级到 ECharts 6 稳定线，词云改用 `@echarts-x/custom-word-cloud`                                            |
| Axios                   | `1.18.1`                                     | 已补 request wrapper 行为基线后小版本升级                                                                     |
| sql-formatter           | `15.8.2`                                     | 已补真实运行时 smoke 后升级                                                                                   |
| AntV S2                 | `2.7.2 / 2.3.1`                              | 已确认当前稳定线                                                                                              |
| i18next / react-i18next | `26.3.2 / 17.0.8`                            | 已确认国际化主链                                                                                              |
| react-grid-layout       | `2.2.3`                                      | 已通过 legacy 入口升级                                                                                        |
| react-hotkeys-hook      | `5.3.2`                                      | 已完成快捷键 hook 主版本升级验证                                                                              |
| flexlayout-react        | `0.9.1`                                      | 已升级并改用命名导出                                                                                          |
| react-window            | `2.2.7`                                      | 已适配 `Grid / cellComponent / gridRef` 新 API                                                                |
| redux-undo              | `1.1.0`                                      | 已补撤销 / 重做历史栈测试后升级                                                                               |
| react-draggable         | `4.7.0`                                      | 已升级                                                                                                        |
| react-resizable         | `4.0.2`                                      | 已改用内置类型声明，移除旧 `@types` stub                                                                      |
| @hello-pangea/dnd       | `18.0.1`                                     | 已确认当前稳定线                                                                                              |
| react-dnd               | `16.0.1`                                     | 已确认当前稳定线                                                                                              |
| react-dnd-html5-backend | `16.0.1`                                     | 已确认当前稳定线                                                                                              |

## 5. 阶段复盘

### 5.1 已完成主线成果

- yu-bi 已从 datart 独立，仓库、默认分支、远端已切换完成
- README、README_zh、NOTICE、SECURITY、ROADMAP、CHANGELOG、MAINTAINERS、issue template 已收口为独立开源项目表述
- 后端已建立 `JDK 21 + Spring Boot 3.5.x + Spring Cloud 2025.0.x` 主链
- 前端已建立 `Node 24 + React 19 + Ant Design 5 + Vite 8 + TypeScript 6 + Vitest 4` 主链
- CRA / CRACO、IE11 主兼容链、Nashorn、PhantomJS 等历史主链已退出
- `.tmp/`、`logs/` 已加入 `.gitignore`

### 5.2 已合入批次

| 批次                                 | 结果                                             |
| ------------------------------------ | ------------------------------------------------ |
| 独立开源治理与 yu-bi 基础品牌        | 已合入 `main`                                    |
| 后端 JDK 21 / Spring Boot 3 主链     | 已合入 `main`                                    |
| 前端 React 18 / AntD 5 / Vite 6 主链 | 已合入 `main`                                    |
| Dashboard widget 内容协议边界        | 已合入 `main`                                    |
| 图表运行时类型边界                   | 已合入 `main`                                    |
| 现代化兼容边界                       | 已合入并推送 `origin/main`                       |
| 图表运行时现代化                     | 已合入并推送 `origin/main`                       |
| 前端运行时现代化批次                 | 已合入并推送 `origin/main`，主线提交 `77217676b` |
| 构建与安装包链路现代化               | 已合入并推送 `origin/main`，主线提交 `2c691916b` |
| Shiro 认证授权健康度审计             | 已合入并推送 `origin/main`，主线提交 `99336814e` |
| Calcite SQL 解析健康度审计           | 已合入并推送 `origin/main`，主线提交 `065e4d007` |
| SQL 变量替换行为修复                 | 已合入并推送 `origin/main`，主线提交 `bf6eee2e2` |
| PRESTO JDBC driver 元数据治理        | 已合入并推送 `origin/main`，主线提交 `f1739f621` |

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

| 事项                     | 风险       | 状态                                                                |
| ------------------------ | ---------- | ------------------------------------------------------------------- |
| Dockerfile 元数据        | 低         | 已将旧个人 label 改为 OCI labels                                    |
| Docker 健康检查          | 中         | 已增加 `HEALTHCHECK`；最终镜像保留 `curl` 用于健康探测              |
| Deployment.md 安装包示例 | 低         | 已更新为当前 `1.0.0-rc.3` 示例                                      |
| Deployment.md 品牌文案   | 低         | 仅收口用户可见安装包示例，不改 `datart.conf` 和 `datart.*` 配置前缀 |
| Docker build 验证        | 受环境限制 | 当前本机无 `docker` 命令，记录为验证缺口                            |

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
- React Router 入口已集中通过 `app/routerCompat` 管理；后续已升级到 React Router 7 并移除 v6 future flags
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
- `react-window` 2 后续已完成适配：`VirtualTable` 已从旧 `VariableSizeGrid` 迁移到 `Grid / cellComponent / gridRef` 新 API
- `quill 2.0.3` / `react-quill-new 3.8.3` 暂缓：此前已确认会触发 `GHSA-v3m3-f69x-jf25` 低危 XSS audit
- `monaco-editor 0.55.1` 暂缓：此前已确认会通过 `dompurify 3.2.7` 引入新的 audit 风险

最新批次：前端样式工具小版本升级

- 已将 `polished` 从 `4.1.4` 升级到 `4.3.1`
- `polished` 当前主要用于主题色、图表色、拖拽占位色和按钮 hover 色计算；本批次不改样式逻辑，只升级同一主版本内的小版本
- `redux-undo 1.1.0` 当时未并入本批次：它直接影响 Dashboard 编辑器 `undoable` 历史栈和撤销 / 重做语义，需要先补独立行为用例

本批次验证命令：

```bash
npm ls polished --all
npm run test -- src/styles/theme/__tests__/ThemeProvider.test.tsx src/app/components/ChartGraph/BasicTableChart/__tests__/runtime.test.ts src/app/pages/DashBoardPage/utils/__tests__/board.test.ts src/app/pages/DashBoardPage/utils/__tests__/widget.test.ts src/app/pages/DashBoardPage/hooks/__tests__/useGridLayoutMap.test.ts
npm run checkTs
npm run build
npm run build:task
npm audit --json
npm ci --dry-run --ignore-scripts --no-audit --no-fund
git diff --check
```

验证说明：

- 依赖树已确认 `polished 4.3.1` 解析到目标版本
- 样式 / 图表 / Dashboard 相关测试 4 个文件、19 个用例通过
- `npm run checkTs` 已通过
- 主构建和 task bundle 构建均通过，继续使用 Vite 8.0.16
- `npm audit --json` 仍为 0 vulnerabilities
- `npm ci --dry-run --ignore-scripts --no-audit --no-fund` 已通过

最新批次：Dashboard 撤销历史依赖升级

- 已新增 `BoardEditor/slice/__tests__/undoHistory.test.ts`，覆盖编辑器历史栈关键契约：
  - 初始化看板进入编辑栈时不写入 undo history
  - 白名单 action 会进入 past，可通过 `BOARD_UNDO.undo` / `BOARD_UNDO.redo` 恢复
  - 非白名单 stack action 不进入 undo history
- 已显式导出 `editBoardStackReducer` 供 reducer 层行为测试使用
- 已将 `redux-undo` 从 `1.0.1` 升级到 `1.1.0`
- 本批次不改 Dashboard 业务逻辑，只先补行为基线，再升级同一主版本内的小版本

本批次验证命令：

```bash
npm run test -- src/app/pages/DashBoardPage/pages/BoardEditor/slice/__tests__/undoHistory.test.ts
npm ls redux-undo --all
npm run test -- src/app/pages/DashBoardPage/pages/BoardEditor/slice/__tests__/undoHistory.test.ts src/app/pages/DashBoardPage/pages/BoardEditor/slice/__tests__/events.test.ts src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/index.test.ts src/app/pages/DashBoardPage/utils/__tests__/board.test.ts src/app/pages/DashBoardPage/utils/__tests__/widget.test.ts
npm run checkTs
npm run build
npm run build:task
npm audit --json
npm ci --dry-run --ignore-scripts --no-audit --no-fund
git diff --check
```

验证说明：

- 升级前新增撤销历史基线测试已通过，确认测试能覆盖当前行为
- 升级后依赖树已确认 `redux-undo 1.1.0` 解析到目标版本
- Dashboard / 撤销历史相关测试 5 个文件、18 个用例通过
- `npm run checkTs` 已通过
- 主构建和 task bundle 构建均通过，继续使用 Vite 8.0.16
- `npm audit --json` 仍为 0 vulnerabilities
- `npm ci --dry-run --ignore-scripts --no-audit --no-fund` 已通过

最新批次：前端快捷键 hook 升级

- 已将 `react-hotkeys-hook` 从 `3.4.4` 升级到 `5.3.2`
- `react-hotkeys-hook 5.3.2` peer 支持 `react >=16.8.0` 和 `react-dom >=16.8.0`，与当前 React 19 主链兼容
- 已检查 v5 类型签名：`useHotkeys(keys, callback, options?, dependencies?)` 仍可兼容当前依赖数组调用方式
- 当前使用点集中在 Dashboard 编辑器快捷键和 SQL 编辑器执行 / 保存快捷键；本批次不改快捷键行为，只升级 hook 运行时

本批次验证命令：

```bash
npm ls react-hotkeys-hook --all
npm run checkTs
npm run test -- src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/index.test.ts src/app/pages/DashBoardPage/pages/BoardEditor/slice/__tests__/undoHistory.test.ts src/app/pages/MainPage/pages/ViewPage/Main/Editor/__tests__/completionRuntime.test.ts src/app/pages/MainPage/pages/ViewPage/Main/Editor/__tests__/sqlFormatterRuntime.test.ts
npm run build
npm run build:task
npm audit --json
npm ci --dry-run --ignore-scripts --no-audit --no-fund
git diff --check
```

验证说明：

- 依赖树已确认 `react-hotkeys-hook 5.3.2` 解析到目标版本
- `npm run checkTs` 已通过，确认当前 `useHotkeys` 调用签名兼容 v5
- Dashboard 编辑器和 SQL 编辑器相关测试 4 个文件、9 个用例通过
- 主构建和 task bundle 构建均通过，继续使用 Vite 8.0.16
- `npm audit --json` 仍为 0 vulnerabilities
- `npm ci --dry-run --ignore-scripts --no-audit --no-fund` 已通过

最新批次：前端 SQL 格式化依赖升级

- 已将 `sql-formatter` 从 `9.2.0` 升级到 `15.8.2`
- 已在 `sqlFormatterRuntime.test.ts` 增加真实运行时 smoke，覆盖动态加载后的基础 SQL 格式化行为
- 当前业务使用点通过 `loadSqlFormatter()` 延迟加载格式化运行时；本批次不改变 SQL 编辑器交互和格式化入口
- 主构建中 `sql-formatter` 独立 chunk 由约 `240.89 kB / gzip 58.26 kB` 增加到约 `262.41 kB / gzip 74.55 kB`，仍保持动态拆包

本批次验证命令：

```bash
npm ls sql-formatter --all
npm run test -- src/app/pages/MainPage/pages/ViewPage/Main/Editor/__tests__/sqlFormatterRuntime.test.ts src/app/pages/MainPage/pages/ViewPage/Main/Editor/__tests__/completionRuntime.test.ts
npm run checkTs
npm audit --json
npm run build
npm run build:task
npm ci --dry-run --ignore-scripts --no-audit --no-fund
git diff --check
```

验证说明：

- 依赖树已确认 `sql-formatter 15.8.2` 解析到目标版本
- SQL 编辑器格式化运行时和 completion runtime 测试 2 个文件、5 个用例通过
- `npm run checkTs` 已通过，确认升级后类型边界兼容
- 主构建和 task bundle 构建均通过，继续使用 Vite 8.0.16
- `npm audit --json` 仍为 0 vulnerabilities
- `npm ci --dry-run --ignore-scripts --no-audit --no-fund` 已通过

最新批次：前端交互基础依赖升级

- 已将直接依赖 `@ant-design/icons` 从 `5.6.1` 升级到 `6.2.5`
- 已将 `react-resizable` 从 `3.2.0` 升级到 `4.0.2`，并移除废弃的 `@types/react-resizable` stub
- `react-resizable 4` 不再导出旧 `ResizableProps` 类型，本批次将表格列宽相关类型切到内置 `Props['onResize']`
- 已将直接依赖 `immer` 从 `9.0.12` 升级到 `11.1.8`，与 `@reduxjs/toolkit 2.12.0` 带入版本去重
- Immer 默认导入已统一改为命名导入 `{ produce }`，保持当前状态更新逻辑不变
- `@ant-design/icons 6.2.5` peer 仍支持 `react >=16.0.0 / react-dom >=16.0.0`；当前 `antd 5.29.3` 和 `@ant-design/pro-components 2.8.10` 的内部依赖仍解析到 `@ant-design/icons 5.6.1`，本批次不做强制 override 去重

本批次验证命令：

```bash
npm ls @ant-design/icons react-resizable @types/react-resizable --all
npm ls immer --all
npm run checkTs
npm run test -- src/app/components/ChartGraph/BasicTableChart/__tests__ src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/index.test.ts src/app/pages/DashBoardPage/utils/__tests__/widget.test.ts
npm run test -- src/app/utils/__tests__/mutation.test.ts src/app/pages/DashBoardPage/utils/__tests__/widget.test.ts src/app/pages/DashBoardPage/utils/__tests__/index.test.tsx src/app/pages/DashBoardPage/pages/BoardEditor/components/ControllerWidgetPanel/__tests__/utils.test.ts src/app/pages/DashBoardPage/pages/BoardEditor/slice/__tests__/undoHistory.test.ts src/app/pages/DashBoardPage/pages/BoardEditor/slice/__tests__/events.test.ts src/app/migration/__tests__/migrateWidgets.test.ts src/app/migration/__tests__/migrateWidgetConfig.test.ts src/app/migration/__tests__/migrateWidgetChartConfig.test.ts src/app/migration/__tests__/migrateBoardConfig.test.ts
npm audit --json
npm run build
npm run build:task
npm ci --dry-run --ignore-scripts --no-audit --no-fund
git diff --check
```

验证说明：

- 直接依赖已确认解析到 `@ant-design/icons 6.2.5`、`react-resizable 4.0.2`、`immer 11.1.8`
- `@types/react-resizable` 已从直接依赖树移除；`react-grid-layout 2.2.3` 仍自带其内部 `react-resizable 3.2.0`
- `npm run checkTs` 已通过
- BasicTable / Dashboard 相关测试 3 个文件、10 个用例通过
- Immer 相关测试 9 个文件、79 个用例通过、3 个既有 skipped
- `npm audit --json` 仍为 0 vulnerabilities
- 主构建和 task bundle 构建均通过；既有大 chunk warning 仍存在

最新批次：前端路由主版本升级

- 已将 `react-router-dom` 从 `6.30.4` 升级到 `7.18.0`，同步解析 `react-router 7.18.0`
- React Router 7 peer 要求 `react >=18 / react-dom >=18`，engine 要求 `node >=20.0.0`，与当前 React 19 / Node 24 基线兼容
- `app/routerCompat` 已移除 React Router 6 专用的 `v7_startTransition` 和 `v7_relativeSplatPath` future flags，保留 `BrowserRouter`、`MemoryRouter`、`Link`、`useLocation`、`useNavigate` 和类型化 `useParams` 入口
- 已新增 `useShareRouteParams` hook 测试，覆盖 `shareChart`、`shareDashboard`、`shareStoryPlayer` 三类分享路径 token 解析，验证 React Router 7 下 `matchPath` 行为
- 主构建中 `entryPointFactory` chunk 由约 `124.64 kB / gzip 44.95 kB` 增加到约 `143.38 kB / gzip 51.11 kB`，仍保持现有拆包结构

本批次验证命令：

```bash
npm ls react-router-dom react-router --all
npm run checkTs
npm run test -- src/app/__tests__/routerCompat.test.tsx src/app/pages/SharePage/hooks/__tests__/useShareRouteParams.test.tsx src/app/pages/NotFoundPage/__tests__/index.test.tsx src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/index.test.ts
npm audit --json
npm run build
npm run build:task
npm ci --dry-run --ignore-scripts --no-audit --no-fund
git diff --check
```

验证说明：

- 依赖树已确认 `react-router-dom 7.18.0` 和 `react-router 7.18.0` 解析到目标版本
- `npm run checkTs` 已通过
- 路由兼容层、分享路由参数、404 页面和 Dashboard 编辑器路由相关测试 4 个文件、9 个用例通过
- `npm audit --json` 仍为 0 vulnerabilities
- 主构建和 task bundle 构建均通过；既有大 chunk warning 仍存在

最新批次：前端虚拟表格运行时升级

- 已将 `react-window` 从 `1.8.11` 升级到 `2.2.7`
- `react-window 2` peer 支持 `react ^18.0.0 || ^19.0.0`，与当前 React 19 主链兼容
- `VirtualTable` 已从旧 `VariableSizeGrid` render-prop API 迁移到 v2 `Grid / cellComponent / cellProps / gridRef` API
- `virtualTableRuntime` 延迟加载入口已从 `VariableSizeGrid` 切换到 `Grid`，继续保持运行时动态拆包
- 保留 AntD Table 自定义 body 的横向滚动同步：`scrollLeft` setter 改为通过 v2 `gridRef.current.element.scrollTo({ left })` 同步
- 已移除 v1 专用的 `resetAfterIndices` 调用；v2 根据 `Grid` props 和 `cellProps` 重新计算渲染
- 主构建中 `react-window` 独立 chunk 由约 `12.76 kB / gzip 4.05 kB` 降到约 `9.51 kB / gzip 3.44 kB`

本批次验证命令：

```bash
npm ls react-window --all
npm run checkTs
npm run test -- src/app/components/__tests__/virtualTableRuntime.test.ts src/app/components/__tests__/VirtualTable.test.tsx
npm audit --json
npm run build
npm run build:task
npm ci --dry-run --ignore-scripts --no-audit --no-fund
git diff --check
```

验证说明：

- 依赖树已确认 `react-window 2.2.7` 解析到目标版本
- `npm run checkTs` 已通过
- 虚拟表格运行时和组件测试 2 个文件、7 个用例通过
- 当前仓库没有 `SchemaTable` 专用单测；业务使用面由 `VirtualTable` 组件测试、类型检查和生产构建覆盖
- `npm audit --json` 仍为 0 vulnerabilities
- 主构建和 task bundle 构建均通过；既有大 chunk warning 仍存在

最新批次：前端剩余 outdated 复核

- 当前剩余 outdated 已收敛到 `@types/node`、`@vitejs/plugin-react`、`antd`、`echarts`、`eslint`、`monaco-editor`、`quill`、`react-quill-new`
- `@types/node 26.0.0` 暂不升级：当前硬性目标是 Node 24，继续保持 `@types/node 24.13.2`
- `@vitejs/plugin-react 6.0.2` 复核后继续暂缓：虽然 peer 中 `@rolldown/plugin-babel` / `babel-plugin-react-compiler` 标记为 optional，但 npm 11 安装仍会解析到 Babel 8 peer 链并触发 `ERESOLVE`
- `antd 6.4.5` 暂不升级：当前稳定 `@ant-design/pro-components 2.8.10` peer 仍只支持 AntD 4/5
- `echarts 6.1.0` 暂不升级：当前 `echarts-wordcloud 2.1.0` peer 只声明支持 `echarts ^5.0.1`，直接升级会产生明确 peer 违约
- `monaco-editor 0.55.1` 暂不升级：仍直接依赖 `dompurify 3.2.7`，会引入已知 audit 风险
- `quill 2.0.3` / `react-quill-new 3.8.3` 暂不升级：`react-quill-new 3.8.3` 依赖 `quill ~2.0.3`，此前已确认会触发 `GHSA-v3m3-f69x-jf25` 低危 XSS audit
- `eslint 10.5.0` 暂不并入当前批次：属于 lint 工具链主版本升级，需要独立检查 flat config、插件兼容和完整 lint 门禁

本批次验证命令：

```bash
npm outdated --json
npm view @vitejs/plugin-react@6.0.2 peerDependencies dependencies optionalDependencies peerDependenciesMeta engines --json
npm install --ignore-scripts --no-audit --no-fund
npm view echarts@6.1.0 version dependencies peerDependencies engines exports main module types --json
npm view echarts-wordcloud@2.1.0 peerDependencies dependencies version --json
npm view monaco-editor@0.55.1 version dependencies engines --json
npm view react-quill-new@3.8.3 version peerDependencies dependencies engines --json
git diff --check
```

验证说明：

- `@vitejs/plugin-react 6.0.2` 试装失败，错误为 npm `ERESOLVE`，冲突来自可选 `@rolldown/plugin-babel` / Babel 8 peer 链；已回退到 `5.2.0` 且工作区恢复干净
- ECharts 6 的阻塞来自 `echarts-wordcloud` peer，不是 ECharts 包本身；后续需要先找到兼容 ECharts 6 的词云扩展或替换方案
- 本批次不提交依赖版本变更，只沉淀剩余项决策依据，避免后续重复试错

最新批次：富文本 Widget Quill 2 业务入口 smoke

- 已新增 `RichTextWidgetCore` smoke，覆盖 Dashboard 富文本 Widget 展示层渲染 Delta 内容
- 已覆盖编辑弹窗挂载真实 Quill 2 运行时，并通过 Quill API 触发内容变更后保存到 `changeMediaWidgetConfig`
- 修复编辑态初始挂载期间 Quill 2 触发 `onChange` 时读取未就绪 ref 的边界；现在 `quillChange` 只在运行时 ready 后读取内容
- 保存逻辑改为使用组件已维护的 `quillValue` 状态，避免确认保存时再依赖 Quill ref 即时读取，降低弹窗关闭和运行时未就绪边界风险
- jsdom 层已补 Dashboard 富文本 Widget 展示 / 编辑保存 smoke；真实浏览器 E2E 仍留作后续端到端验证，不阻塞当前 P2-E 继续推进

本批次验证命令：

```bash
npm run test -- src/app/pages/DashBoardPage/components/Widgets/RichTextWidget/__tests__/RichTextWidgetCore.smoke.test.tsx
npm run checkTs
npm run test -- src/app/components/ChartGraph/BasicRichText/__tests__/RichTextEditorRuntime.smoke.test.tsx src/app/pages/DashBoardPage/components/Widgets/RichTextWidget/__tests__/RichTextWidgetCore.smoke.test.tsx
git diff --check
```

验证说明：

- 富文本相关 smoke 2 个测试文件、4 个用例通过
- `npm run checkTs` 已通过
- 测试日志中的 jsdom pseudo-element `getComputedStyle` warning 是 AntD / Quill 测试环境噪声，不影响结果
- 本批次不升级依赖版本，不触碰高风险内部命名

最新批次：ESLint 10 生态兼容复核

- 已复核 `eslint 10.5.0`：Node engine 为 `^20.19.0 || ^22.13.0 || >=24`，满足 Node 24 目标
- 已复核 `@typescript-eslint/parser`、`@typescript-eslint/eslint-plugin`、`typescript-eslint` 最新稳定线仍为 `8.62.0`，peer 已声明支持 `eslint ^8.57.0 || ^9.0.0 || ^10.0.0` 和 `typescript >=4.8.4 <6.1.0`
- 当前阻塞来自 ESLint 插件生态：
  - `eslint-plugin-react 7.37.5` 最新稳定版 peer 仅声明 `eslint ^3 || ^4 || ^5 || ^6 || ^7 || ^8 || ^9.7`
  - `eslint-plugin-import 2.32.0` 最新稳定版 peer 仅声明 `eslint ^2 || ^3 || ^4 || ^5 || ^6 || ^7.2.0 || ^8 || ^9`
- 当前不硬升 ESLint 10，避免在 lint 工具链上引入 peer 违约和隐性误报 / 漏报风险
- 后续等待 `eslint-plugin-react`、`eslint-plugin-import` 发布支持 ESLint 10 的稳定版本后，再进入实际升级和完整 lint 门禁

本批次验证命令：

```bash
npm view eslint@10.5.0 version engines peerDependencies dependencies --json
npm view @typescript-eslint/parser@latest version peerDependencies engines --json
npm view @typescript-eslint/eslint-plugin@latest version peerDependencies engines --json
npm view typescript-eslint@latest version peerDependencies engines --json
npm view eslint-plugin-react@latest version peerDependencies engines --json
npm view eslint-plugin-import@latest version peerDependencies engines --json
npm view eslint-plugin-react versions --json
npm view eslint-plugin-import versions --json
```

验证说明：

- ESLint 10 本体满足 Node 24，但当前 React / import 插件最新稳定版本尚未声明支持 ESLint 10
- 本批次只记录兼容性结论，不改依赖版本，不触碰运行时代码

最新批次：ECharts 6 与词云扩展迁移

- 已将 `echarts` 从 `5.6.0` 升级到 `6.1.0`
- 已移除只声明支持 `echarts ^5.0.1` 的 `echarts-wordcloud 2.1.0`
- 已引入 `@echarts-x/custom-word-cloud 1.0.1`，其 peer 明确要求 `echarts ^6.0.0`，来源为 Apache ECharts custom series 词云扩展
- `WordCloudChart` 运行时改为动态加载 `echarts` 和 `@echarts-x/custom-word-cloud`，并通过 `echarts.use(...)` 注册 custom series installer
- 词云 option 已从旧扩展协议 `series.type = 'wordCloud'` 迁移为 ECharts 6 custom series 协议：
  - `series.type = 'custom'`
  - `series.renderItem = 'wordCloud'`
  - 样式配置放入 `series.itemPayload`
  - 数据使用 `[name, value, rowData, textStyle]` 数组，兼容新扩展 API
- 为保持 yu-bi 现有联动 / 跳转 / 钻取依赖的 `params.data.rowData`，`WordCloudChart` 在点击事件进入 `ChartSelectionManager` 前将数组数据归一为原对象形态
- 已补 `WordCloudChart` option smoke，覆盖 ECharts 6 custom word cloud 协议和 `rowData` 保留

本批次验证命令：

```bash
npm view @echarts-x/custom-word-cloud@latest version peerDependencies dependencies engines exports main module types files --json
npm view @echarts-x/custom-word-cloud@latest readme --json
npm view echarts@6.1.0 version dependencies peerDependencies engines exports main module types --json
npm pack @echarts-x/custom-word-cloud@1.0.1 --dry-run --json
npm install echarts@6.1.0 @echarts-x/custom-word-cloud@1.0.1 --save --ignore-scripts --no-audit --no-fund
npm run checkTs
npm run test -- src/app/components/ChartGraph/WordCloudChart/__tests__/WordCloudChart.test.jsx src/app/components/ChartGraph/WordCloudChart/__tests__/runtime.test.ts src/app/components/ChartGraph/__tests__/echartsRuntime.test.ts src/app/utils/__tests__/echartsThemeRuntime.test.ts
npm ls echarts @echarts-x/custom-word-cloud echarts-wordcloud --all
npm audit --json
npm ci --dry-run --ignore-scripts --no-audit --no-fund
npm run build
npm run build:task
npm outdated --json
git diff --check
```

验证说明：

- `npm ls` 已确认当前词云链路为 `echarts 6.1.0` + `@echarts-x/custom-word-cloud 1.0.1`，旧 `echarts-wordcloud` 已不在依赖树中
- `npm audit --json` 仍为 0 vulnerabilities
- 词云和 ECharts runtime 相关测试 4 个文件、10 个用例通过
- `npm run checkTs` 已通过
- 主构建和 task bundle 构建均通过；主构建出现独立 `word-cloud.esm` chunk，既有大 chunk warning 仍存在
- `npm outdated --json` 中已无 `echarts`，P2-G 阻塞项解除

最新批次：ECharts 6 词云生命周期 smoke

- 已补 `WordCloudChart` 生命周期 smoke，覆盖 `onMount` + `onUpdated` 后真实 ECharts 6 runtime 初始化并写入 custom word cloud option
- smoke 发现 ECharts 6 custom series 在无坐标系词云场景下需要显式 `coordinateSystem: 'none'`，否则会按默认坐标系解析并报 `xAxis "0" not found`
- 已为词云 option 增加 `coordinateSystem: 'none'`，并在 option smoke 和生命周期 smoke 中同时断言
- 生命周期 smoke 继续覆盖点击事件数据归一化，确认 `[name, value, rowData, textStyle]` 数组能恢复为现有联动 / 跳转 / 钻取依赖的 `{ name, value, rowData, textStyle }` 形态
- jsdom 层已补 ECharts 6 词云生命周期验证；后续可继续补浏览器层图表 smoke，但当前不再阻塞 P2-G

本批次验证命令：

```bash
npm run test -- src/app/components/ChartGraph/WordCloudChart/__tests__/WordCloudChart.test.jsx src/app/components/ChartGraph/WordCloudChart/__tests__/runtime.test.ts
npm run test -- src/app/components/ChartGraph/WordCloudChart/__tests__/WordCloudChart.test.jsx src/app/components/ChartGraph/WordCloudChart/__tests__/runtime.test.ts src/app/components/ChartGraph/__tests__/echartsRuntime.test.ts src/app/utils/__tests__/echartsThemeRuntime.test.ts
npm run checkTs
npm audit --json
npm run build
npm run build:task
git diff --check
```

验证说明：

- 词云和 ECharts runtime 相关测试 4 个文件、11 个用例通过
- `npm run checkTs` 已通过
- `npm audit --json` 仍为 0 vulnerabilities
- 主构建和 task bundle 构建均通过；既有大 chunk warning 仍存在

最新批次：ECharts 6 词云残留清理

- 已确认源码运行时只引用 `@echarts-x/custom-word-cloud`，不再引用旧 `echarts-wordcloud`
- 已删除 `WordCloudChart/__mocks__/echarts-wordcloud.js`，避免旧扩展 mock 残留误导后续测试维护
- 本批次不改运行时代码，只清理 ECharts 6 迁移后的无引用测试残留

本批次验证命令：

```bash
rg -n "echarts-wordcloud|@echarts-x/custom-word-cloud|word-cloud" frontend/src docs/tech-stack-modernization-plan.md frontend/package.json
npm run test -- src/app/components/ChartGraph/WordCloudChart/__tests__/WordCloudChart.test.jsx src/app/components/ChartGraph/WordCloudChart/__tests__/runtime.test.ts
npm run checkTs
npm audit --json
git diff --check
```

最新批次：Vite 8 小版本升级

- 已将 `vite` 从 `8.0.16` 升级到 `8.1.0`
- `vite 8.1.0` engine 为 `^20.19.0 || >=22.12.0`，满足 Node 24 目标
- 当前继续保留 `@vitejs/plugin-react 5.2.0`：该版本 peer 已支持 Vite 8，`@vitejs/plugin-react 6.0.3` 仍有可选 `@rolldown/plugin-babel` / `babel-plugin-react-compiler` peer 链风险，暂不并入本批次
- `vitest 4.1.9` peer 已支持 `vite ^6.0.0 || ^7.0.0 || ^8.0.0`
- 本批次只升级构建工具小版本，不改业务代码和运行时协议

本批次验证命令：

```bash
npm view vite@8.1.0 version engines dependencies optionalDependencies peerDependencies peerDependenciesMeta dist --json
npm view @vitejs/plugin-react@5.2.0 version peerDependencies engines dependencies optionalDependencies peerDependenciesMeta --json
npm view @vitejs/plugin-react@6.0.3 version peerDependencies engines dependencies optionalDependencies peerDependenciesMeta --json
npm view vitest@4.1.9 version peerDependencies engines dependencies optionalDependencies --json
npm install vite@8.1.0 --save-dev --ignore-scripts --no-audit --no-fund
npm exec -- vite --version
npm ls vite @vitejs/plugin-react vitest --all
npm run checkTs
npm run test -- src/__tests__/task.test.ts src/app/components/__tests__/splitRuntime.test.ts src/app/components/__tests__/virtualTableRuntime.test.ts src/app/components/__tests__/dndRuntime.test.ts src/app/components/ChartGraph/BasicRichText/__tests__/runtime.test.ts src/app/components/ChartGraph/WordCloudChart/__tests__/runtime.test.ts src/app/components/ChartGraph/WordCloudChart/__tests__/WordCloudChart.test.jsx
npm run eslint -- eslint.config.mjs vite.config.mts vite.task.config.mts vitest.config.mts vite.shared.mts
npm audit --json
npm run build
npm run build:task
npm ci --dry-run --ignore-scripts --no-audit --no-fund
git diff --check
```

验证说明：

- 本地实际 Vite 版本已确认为 `vite/8.1.0 darwin-arm64 node-v24.16.0`
- `npm ls` 已确认 `@vitejs/plugin-react 5.2.0`、`vite-plugin-svgr 5.2.0`、`vitest 4.1.9` 均解析到 `vite 8.1.0`
- `npm run checkTs` 已通过
- 代表性测试 7 个文件、18 个用例通过
- Vite / Vitest 配置文件 ESLint 已通过
- `npm audit --json` 仍为 0 vulnerabilities
- 主构建和 task bundle 构建均通过，输出均显示 `vite v8.1.0`

最新批次：前端剩余阻塞项再复核

- 当前 `npm outdated --json` 剩余项为 `@types/node`、`@vitejs/plugin-react`、`antd`、`eslint`、`monaco-editor`、`quill`、`react-quill-new`
- `@types/node 26.0.0` 继续暂缓：当前硬性目标是 Node 24，继续保持 `@types/node 24.13.2`
- `antd 6.4.5` 本体 peer 支持 React 18+，但最新稳定 `@ant-design/pro-components 2.8.10` peer 仍只支持 `antd ^4.24.15 || ^5.11.2`
- `@ant-design/pro-components` 的 3.x 目前只有 `3.0.0-beta.*` 和 `3.x.x-0/-1` 预发布版本，尚无稳定 `3.0.0`；当前不以预发布链路升级 AntD 6
- `monaco-editor 0.55.1` 仍直接依赖 `dompurify 3.2.7`；临时 lockfile 试装后 `npm audit --json` 会新增 1 low + 1 moderate，继续暂缓
- `quill 2.0.3` / `react-quill-new 3.8.3` 临时 lockfile 试装后仍触发 `GHSA-v3m3-f69x-jf25` 低危 XSS audit，npm 修复建议为回退到当前 `quill 2.0.2` / `react-quill-new 3.7.0`
- 本批次只更新阻塞项证据，不提交风险依赖版本变更

本批次验证命令：

```bash
npm outdated --json
npm view antd@latest version peerDependencies dependencies engines --json
npm view @ant-design/pro-components@latest version peerDependencies dependencies engines --json
npm view @ant-design/pro-components versions --json
npm view @ant-design/pro-components@3.0.0 version peerDependencies dependencies engines --json
npm view monaco-editor@0.55.1 version dependencies peerDependencies engines --json
npm view react-quill-new@3.8.3 version peerDependencies dependencies engines --json
npm view quill@2.0.3 version dependencies peerDependencies engines --json
npm view dompurify@3.2.7 version dependencies peerDependencies --json
npm install monaco-editor@0.55.1 --save --package-lock-only --ignore-scripts --no-audit --no-fund
npm audit --json
git checkout -- frontend/package.json frontend/package-lock.json
npm install react-quill-new@3.8.3 quill@2.0.3 --save --package-lock-only --ignore-scripts --no-audit --no-fund
npm audit --json
git checkout -- frontend/package.json frontend/package-lock.json
```

验证说明：

- AntD 6 仍被 Pro Components 稳定版 peer 阻塞
- Monaco / Quill 最新组合仍会破坏当前 `npm audit` 清零状态
- 临时试装改动均已恢复，工作区不保留风险依赖

最新批次：前端 Less 构建配置重复治理

- 已先通过 `npm ci --ignore-scripts --no-audit --no-fund` 恢复本地 `node_modules` 与 lockfile 一致，清理此前临时试装 `quill 2.0.3` / `react-quill-new 3.8.3` 导致的本地依赖树漂移
- 已复核当前本机基线为 `Node v24.16.0 / npm 11.13.0`
- 已确认 `npm audit --json` 仍为 0 vulnerabilities
- 已确认当前剩余 outdated 仍为 `@types/node`、`@vitejs/plugin-react`、`antd`、`eslint`、`monaco-editor`、`quill`、`react-quill-new`
- 已复核 `@types/node` 的 24.x 最新版本仍是当前 `24.13.2`，不升级到 Node 26 类型线
- 已复核安装期废弃提示：
  - `intersection-observer` 来自 `@antv/s2-react -> ahooks`，ahooks 最新稳定版运行时代码仍在 `useInViewport` 中导入该 polyfill，暂不强行 override 删除
  - `lodash.isequal` 来自 `quill-delta 5.1.0`，上游最新稳定版仍依赖它；后续已通过本地兼容包转发到
    `lodash/isEqual`，不再使用 registry 上 deprecated 的独立包
- 已将主 Vite 构建和 task bundle 构建中重复的 Less `preprocessorOptions` 抽到 `vite.shared.mts#createLessPreprocessorOptions`
- 共享 Less 配置继续保留现有行为：
  - `javascriptEnabled: true`
  - `rewriteUrls: 'all'`
  - Less `~` import 文件解析兼容
- 本批次不升级依赖版本、不改业务代码，只降低后续 Vite / Less 构建配置继续演进时的漂移风险

本批次验证命令：

```bash
npm ci --ignore-scripts --no-audit --no-fund
npm ls quill react-quill-new vite @vitejs/plugin-react antd eslint monaco-editor --all
npm audit --json
npm outdated --json
npm view @types/node@24 version dist-tags engines --json
npm view @antv/s2-react@latest version peerDependencies dependencies engines --json
npm view ahooks@latest version dependencies peerDependencies engines --json
npm view quill-delta@latest version dependencies peerDependencies engines --json
npm run eslint -- vite.config.mts vite.task.config.mts vite.shared.mts
npm exec -- prettier --check vite.config.mts vite.task.config.mts vite.shared.mts
npm run checkTs
npm run build
npm run build:task
git diff --check
```

验证说明：

- `npm ls` 已确认本地 `quill 2.0.2` / `react-quill-new 3.7.0` 与 package 声明一致，无 invalid 依赖
- Vite 配置文件 ESLint 和 Prettier 检查通过
- `npm run checkTs` 已通过
- 主构建和 task bundle 构建均通过，继续使用 `vite v8.1.0`
- 主构建仍有既有大 chunk warning，本批次不处理包体拆分

最新批次：前端 React 构建插件配置重复治理

- 已将主 Vite 构建和 task bundle 构建中重复的 `@vitejs/plugin-react` 配置抽到 `vite.shared.mts#createReactPlugin`
- 共享 React 插件配置继续保留现有 `babel-plugin-styled-components` 行为
- 主构建和 task bundle 构建不再分别直接导入 `@vitejs/plugin-react`，后续如果调整 React plugin 或 styled-components 编译策略，只需维护共享入口
- 本批次不升级依赖版本、不改业务代码，只降低 Vite / React 构建配置继续演进时的分叉风险

本批次验证命令：

```bash
npm run eslint -- vite.config.mts vite.task.config.mts vite.shared.mts
npm exec -- prettier --check vite.config.mts vite.task.config.mts vite.shared.mts
npm run checkTs
npm run build
npm run build:task
git diff --check
```

验证说明：

- Vite 配置文件 ESLint 和 Prettier 检查通过
- `npm run checkTs` 已通过
- 主构建和 task bundle 构建均通过，继续使用 `vite v8.1.0`
- 主构建仍有既有大 chunk warning，本批次不处理包体拆分

最新批次：前端共享 Vite 配置测试基线

- 已新增 `vite.shared.test.mts`，为当前共享 Vite helper 建立轻量行为基线
- 覆盖 `createViteAliases` 的源码 alias 输出，避免后续继续整理构建配置时破坏绝对导入路径
- 覆盖 `createReactPlugin` / `createReactBabelOptions`，确认继续加载 `babel-plugin-styled-components`
- 覆盖 `lessTildeImportCompat`，确认只对 Less 文件重写 `~` import
- 覆盖 `craSvgReactComponentCompat`，确认保留 CRA 风格 `ReactComponent as Xxx` SVG 导入兼容
- 覆盖 `createLessPreprocessorOptions`，确认 Less `javascriptEnabled`、`rewriteUrls` 和 `~` 文件解析兼容仍有效
- 本批次不改业务代码、不升级依赖版本，重点是为后续继续去兼容化或升级 Vite 插件提供回归证据

本批次验证命令：

```bash
npm run test -- vite.shared.test.mts
npm run checkTs
npm run eslint -- vite.shared.mts vite.shared.test.mts
npm exec -- prettier --check vite.shared.mts vite.shared.test.mts
npm run build
npm run build:task
git diff --check
```

验证说明：

- `vite.shared.test.mts` 5 个用例通过
- `npm run checkTs` 已通过
- Vite 共享配置和测试文件 ESLint / Prettier 检查通过
- 主构建和 task bundle 构建均通过，继续使用 `vite v8.1.0`
- 主构建仍有既有大 chunk warning，本批次不处理包体拆分

最新批次：前端 CRA SVG 兼容退出

- 已确认源码中只剩 `ChartIFrameContainer` 使用 CRA 风格 `ReactComponent as Loading` SVG 导入
- 已将该使用点迁移为 Vite / SVGR 原生 `*.svg?react` 导入
- 已移除 `vite.shared.mts` 中的 CRA SVG `ReactComponent` 导入转换插件
- 主 Vite 构建和 task bundle 构建不再加载该历史兼容插件
- `react-app-env.d.ts` 已收紧 SVG 类型声明：普通 `*.svg` 只作为 URL 导入，`*.svg?react` 才作为 React 组件导入
- `vite.shared.test.mts` 已删除旧 CRA SVG 兼容断言，避免继续为已退出的迁移兼容层建立回归责任
- 本批次不升级依赖版本、不改运行时协议，只减少从 CRA 迁移遗留的构建时字符串重写逻辑

本批次验证命令：

```bash
rg "ReactComponent as .*\\.svg|craSvgReactComponentCompat|datart-cra-svg-react-component-compat" frontend/src frontend/vite*.mts
npm run test -- vite.shared.test.mts src/app/components/ChartIFrameContainer/__tests__/ChartIFrameContainer.test.jsx
npm run checkTs
npm run eslint -- vite.config.mts vite.task.config.mts vite.shared.mts vite.shared.test.mts src/app/components/ChartIFrameContainer/ChartIFrameContainer.tsx
npm exec -- prettier --check vite.config.mts vite.task.config.mts vite.shared.mts vite.shared.test.mts src/app/components/ChartIFrameContainer/ChartIFrameContainer.tsx src/react-app-env.d.ts
npm run build
npm run build:task
git diff --check
```

最新批次：前端 Less `~` import 兼容退出

- 已确认 `frontend/src` 源码中没有 `.less` / `.scss` 入口，实际样式入口均为 CSS
- 已确认源码和配置中没有真实 `@import "~..."` Less 写法，之前命中项只来自兼容插件测试
- 已移除主 Vite 构建和 task bundle 构建中的 `lessTildeImportCompat`
- 已移除 `vite.shared.mts#createLessPreprocessorOptions` 中的 Less `~` 文件解析兼容和 `preprocessorOptions`
- `vite.shared.test.mts` 已删除 Less `~` 兼容层测试，保留 alias 和 React plugin 共享配置基线
- 保留 `less 4.6.7` 依赖声明：`@antv/s2-react 2.3.1` 仍声明 `less >=4.0.0` peer，Vite 8 也将 `less` 作为可选 peer；本批不制造 peer 缺口
- 保留 `frontend/public/antd/theme.less` 静态资源，不纳入 Vite 源码构建兼容清理，避免影响外部仍可能引用的历史主题资产
- 本批次不升级依赖版本、不改业务代码，只减少源码构建侧已无输入的历史 Less 兼容配置

本批次验证命令：

```bash
rg -n "@import\\s+['\\\"]~|~[^'\\\")]+\\.(less|css)" frontend/src frontend -g '*.less' -g '*.css' -g '*.scss' -g '*.tsx' -g '*.ts' -g '*.mts' -g '*.mjs' --glob '!node_modules/**' --glob '!build/**'
npm ls less --all
npm run test -- vite.shared.test.mts
npm run checkTs
npm run eslint -- vite.config.mts vite.task.config.mts vite.shared.mts vite.shared.test.mts
npm exec -- prettier --check vite.config.mts vite.task.config.mts vite.shared.mts vite.shared.test.mts docs/tech-stack-modernization-plan.md
npm run build
npm run build:task
git diff --check
```

最新批次：前端 core-js alias 兼容退出

- 已确认 `frontend/src` 源码没有 `core-js` / `regenerator-runtime` / `babel-polyfill` 导入
- 已确认当前依赖树中不存在 `core-js`，`npm why core-js` 返回无匹配依赖
- 已移除 `vite.shared.mts#createViteAliases` 中的 `core-js -> node_modules/core-js` alias
- `vite.shared.test.mts` 已同步删除该 alias 期望，避免继续为不存在的旧 polyfill 包建立构建兼容
- 本批次不升级依赖版本、不改运行时代码，只减少 CRA / 旧 polyfill 迁移遗留的无效 alias

本批次验证命令：

```bash
rg -n "core-js|regenerator-runtime|babel-polyfill" frontend/src frontend/vite*.mts frontend/package.json frontend/package-lock.json docs/tech-stack-modernization-plan.md --glob '!frontend/node_modules/**' --glob '!frontend/build/**'
npm ls core-js regenerator-runtime @babel/runtime --all
npm why core-js
npm run test -- vite.shared.test.mts
npm run checkTs
npm run eslint -- vite.shared.mts vite.shared.test.mts vite.config.mts vite.task.config.mts
npm exec -- prettier --check vite.shared.mts vite.shared.test.mts vite.config.mts vite.task.config.mts
npm run build
npm run build:task
git diff --check
```

最新批次：前端未使用 CRA HTML 模板清理

- 已确认真实 Vite 入口为 `frontend/index.html`、`shareChart.html`、`shareDashboard.html`、`shareStoryPlayer.html`
- 已确认 `frontend/public/index.html` 没有被源码、配置或文档引用
- 已删除 `frontend/public/index.html`，该文件仍保留 CRA 模板注释、`%PUBLIC_URL%` 占位和旧 `logo192.png` 说明，当前 Vite 构建链路不会使用
- 本批次不改变运行时 HTML 入口、不改 public 目录其他静态资源，只清理已退出的 CRA 模板残留

本批次验证命令：

```bash
rg -n "public/index\\.html|%PUBLIC_URL%|logo192|Create React App|frontend/public/index\\.html" . --glob '!frontend/node_modules/**' --glob '!frontend/build/**' --glob '!**/target/**'
npm run checkTs
npm run build
npm run build:task
git diff --check
```

最新批次：前端 HTML / Helmet 元数据品牌收口

- 已将 4 个 Vite HTML 入口的 `<meta name="description">` 从旧 `Data Art` 描述收口为 yu-bi 描述
- 已将主应用 Helmet 默认标题从 `Datart` 收口为 `yu-bi`，标题模板从 `%s - Datart` 收口为 `%s - yu-bi`
- 已将分享页 Helmet 描述收口为 `yu-bi shared analytics content`
- 已将钻取弹窗 iframe 的可访问标题从 `Datart Iframe Window` 收口为 `yu-bi Iframe Window`
- 本批次只处理前端用户可见元数据和无障碍标题，不改 Java 包名、配置前缀、迁移稳定标识或内部技术常量

本批次验证命令：

```bash
rg -n "Data Art|titleTemplate=\"%s - Datart\"|defaultTitle=\"Datart\"|Datart Iframe Window" frontend/index.html frontend/shareChart.html frontend/shareDashboard.html frontend/shareStoryPlayer.html frontend/src/app --glob '!frontend/node_modules/**'
npm run checkTs
npm run build
npm run build:task
git diff --check
```

最新批次：前端 Vite 插件内部名称品牌收口

- 已将 Vite dev server 自定义图表插件中间件名称从 `datart-custom-chart-plugins` 收口为 `yu-bi-custom-chart-plugins`
- 已将分享页 HTML fallback 插件名称从 `datart-share-html-fallback` 收口为 `yu-bi-share-html-fallback`
- 已将 task bundle 同步插件名称从 `datart-sync-task-bundle` 收口为 `yu-bi-sync-task-bundle`
- 本批次只改变 Vite 插件调试标识，不改变插件逻辑、构建输入输出、HTTP 路径或前端运行时协议

本批次验证命令：

```bash
rg -n "datart-custom-chart-plugins|datart-share-html-fallback|datart-sync-task-bundle" frontend/vite*.mts
npm run eslint -- vite.config.mts vite.task.config.mts
npm exec -- prettier --check vite.config.mts vite.task.config.mts
npm run checkTs
npm run build
npm run build:task
git diff --check
```

最新批次：后端低风险依赖和 Maven 插件补丁线收口

- 已将 GraalJS 组件线从 `25.0.1` 补丁升级到 `25.0.3`，继续保持同一 GraalVM 25.0.x 稳定线
- 已将 `thumbnailator` 从 `0.4.14` 升级到 `0.4.21`，用于图片缩略图生成链路
- 已将 `javassist` 从 `3.28.0-GA` 升级到 `3.32.0-GA`，用于现有 ClassTransformer / JDBC adapter 动态类处理链路
- 已将 `maven-compiler-plugin` 从 `3.14.1` 升级到 `3.15.0`
- 已将 `maven-enforcer-plugin` 从 `3.6.2` 升级到 `3.6.3`
- 暂不升级 Spring Boot 4、Spring Security 7、Calcite 1.42、MyBatis Generator 2、DingTalk 2、Shiro 3 alpha、AntD 6、ESLint 10、`@vitejs/plugin-react` 6、Monaco 最新线和 Quill 最新线；这些都需要单独兼容验证或当前已知会破坏 audit / peer 状态
- 本批次验证时，非提权 Maven 首次执行因沙箱不能写 `~/.m2` 下载跟踪文件失败；提权重跑后通过

本批次验证命令：

```bash
mvn versions:display-dependency-updates -Dincludes=net.coobird:thumbnailator,org.javassist:javassist,org.mybatis.generator:mybatis-generator-core,com.jayway.jsonpath:json-path,org.bitbucket.b_c:jose4j,com.github.vertical-blank:sql-formatter -DprocessDependencyManagement=false -DgenerateBackupPoms=false
mvn versions:display-plugin-updates -DgenerateBackupPoms=false
mvn -pl core -Dtest=datart.core.common.POIUtilsTest test
mvn -pl data-providers/jdbc-data-provider -am -Dtest=datart.data.provider.jdbc.ProviderFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl server -am -DskipTests package
git diff --check
```

最新批次：后端 JWT / HTTP / 浏览器截图依赖补丁线收口

- 已将 `jjwt-api`、`jjwt-impl`、`jjwt-jackson` 从 `0.12.7` 升级到 `0.13.0`
- 已将 `jose4j` 从 `0.7.12` 升级到 `0.9.6`
- 已将 BouncyCastle `bcpkix-jdk18on`、`bcutil-jdk18on` 从 `1.81.1` 升级到 `1.84`
- 已将 `httpclient5` 从 `5.5` 升级到 `5.6.1`
- 已将 Selenium 组件线从 `4.31.0` 升级到 `4.45.0`
- 本批次不升级 Spring Boot 4、Spring Security 7、Shiro 3 alpha、Calcite 1.42、MyBatis Generator 2、DingTalk 2、Springdoc 3、JsonPath 3、HikariCP 7；这些仍按中高风险或主版本跳跃单独评估

本批次验证命令：

```bash
mvn versions:display-dependency-updates -Dincludes=org.apache.httpcomponents.client5:httpclient5,com.mysql:mysql-connector-j,io.jsonwebtoken:jjwt-api,io.jsonwebtoken:jjwt-impl,io.jsonwebtoken:jjwt-jackson,org.bouncycastle:bcpkix-jdk18on,org.bouncycastle:bcutil-jdk18on,org.seleniumhq.selenium:selenium-java,org.springdoc:springdoc-openapi-starter-webmvc-ui,org.apache.pdfbox:pdfbox -DprocessDependencyManagement=false -DgenerateBackupPoms=false
mvn -pl security -am -Dtest=datart.security.test.jwt.TestJwkParse,datart.security.manager.shiro.ShiroAuthenticationTokenAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl data-providers/http-data-provider -am -DskipTests package
mvn -pl server -am -DskipTests package
git diff --check
```

最新批次：前端安全依赖小版本收口

- 已将 `i18next` 从 `26.3.1` 升级到 `26.3.2`
- 已将 `postcss-styled-syntax` 从 `0.7.1` 升级到 `0.7.2`
- 已将 `@commitlint/cli`、`@commitlint/config-conventional` 从 `21.0.2` 升级到 `21.1.0`
- 已复核 `react-quill-new 3.8.3 / quill 2.0.3`，该组合会重新引入 `GHSA-v3m3-f69x-jf25` 低危 XSS advisory；当前继续保留 `react-quill-new 3.7.0 / quill 2.0.2`，保持 `npm audit` 0 漏洞
- 本批次不升级 `@types/node 26`，继续保持 Node 24 类型基线

本批次验证命令：

```bash
npm view i18next@26.3.2 version peerDependencies dependencies engines
npm view postcss-styled-syntax@0.7.2 version peerDependencies dependencies engines
npm view quill@2.0.3 version peerDependencies dependencies engines
npm view react-quill-new@3.8.3 version peerDependencies dependencies engines
npm audit --json
npm run checkTs
npm run test:ci -- src/__tests__/task.test.ts src/app/components/ChartGraph/BasicRichText/__tests__/runtime.test.ts src/app/components/ChartGraph/BasicRichText/__tests__/RichTextEditorRuntime.smoke.test.tsx src/app/pages/DashBoardPage/components/Widgets/RichTextWidget/__tests__/RichTextWidgetCore.smoke.test.tsx
npm run lint:style
```

最新批次：前端现代浏览器 polyfill 依赖清理

- 已确认 `intersection-observer 0.12.2` 来自 `@antv/s2-react -> ahooks -> useInViewport` 的传递依赖，`ahooks` 通过 side-effect import 加载该 polyfill
- 当前前端基线已经收口到 `Node 24`、现代浏览器和 `build.target: es2020`，Intersection Observer 已是浏览器 Baseline 能力，不再需要该旧 polyfill
- 已新增本地 no-op 兼容包 `frontend/npm-overrides/intersection-observer`，并通过根依赖 `file:./npm-overrides/intersection-observer` 满足 `ahooks` 的模块解析
- `npm ls intersection-observer` 已确认 `ahooks` 解析到本地 no-op 包，不再使用 registry 上 deprecated 的 polyfill 包
- 本批次不升级 AntV S2、不改 PivotSheet 业务代码、不改变浏览器运行时协议

本批次验证命令：

```bash
npm ci --dry-run --ignore-scripts --no-audit --no-fund
npm ls intersection-observer lodash.isequal --all
npm audit --json
npm run checkTs
npm run test -- src/app/components/ChartGraph/PivotSheetChart/__tests__/runtime.test.ts src/app/components/ChartGraph/PivotSheetChart/__tests__/LazyAntVS2Wrapper.test.tsx src/app/components/__tests__/Split.test.tsx src/app/components/__tests__/VirtualTable.test.tsx
npm exec -- prettier --check package.json package-lock.json npm-overrides/intersection-observer/package.json npm-overrides/intersection-observer/index.js npm-overrides/intersection-observer/index.d.ts
npm run build
npm run build:task
git diff --check
```

最新批次：Quill Delta 深比较依赖收口

- 已确认 `lodash.isequal 4.5.0` 只来自 `quill-delta 5.1.0`，用于富文本 Delta attributes / ops
  深比较
- 已新增本地兼容包 `frontend/npm-overrides/lodash.isequal`，通过 CommonJS 转发到当前项目已使用的
  `lodash/isEqual`
- 已通过根依赖 `file:./npm-overrides/lodash.isequal` 满足 `quill-delta` 的 `require('lodash.isequal')`
  解析，不再下载 registry 上 deprecated 的独立包
- 本批次不升级 `react-quill-new`、`quill` 或 `quill-delta`，不改变富文本 Delta 语义，只清理废弃传递依赖来源

本批次验证命令：

```bash
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
npm install --ignore-scripts --no-audit --no-fund
npm ls lodash.isequal lodash quill-delta --all
node -e "const Delta=require('quill-delta'); const isEqual=require('lodash.isequal'); if(!isEqual({a:[1,{b:2}]},{a:[1,{b:2}]})) throw new Error('isEqual failed'); const a=new Delta().insert('x',{bold:true}); const b=new Delta().insert('x',{bold:true}); if(!isEqual(a.ops,b.ops)) throw new Error('Delta ops equality failed'); console.log('ok')"
npm audit --json
npm run checkTs
```

最新批次：前端 Node 24 本地工具版本文件补强

- 已新增 `frontend/.node-version`，内容与 `frontend/.nvmrc` 对齐到当前验证基线 `24.16.0`
- CI 的 `Verify frontend toolchain` 步骤已校验 `.nvmrc`、`.node-version` 和实际 `node -v` 三者一致
- README / README_zh 已同步当前前端基线为 `npm 11`、`Vite 8`、`React 19`，并提示使用 `.nvmrc` 或 `.node-version` 选择 Node 运行时
- 本批次不升级依赖版本、不改业务代码，只补齐 nodenv / asdf / mise 等不读取 `.nvmrc` 的本地工具入口，降低 Node 版本漂移风险
- 本轮复核 `@vitejs/plugin-react 6.0.3` 仍不并入当前批次：npm 11 试装会解析到可选 `@rolldown/plugin-babel` / Babel 8 peer 链并触发 `ERESOLVE`
- 本轮复核 `@types/node 26.0.0` 继续暂缓：当前硬性目标是 Node 24，继续保持 `@types/node 24.13.2`
- 本轮复核 AntD 6、ESLint 10、Monaco 0.55.1、Quill 2.0.3 / `react-quill-new 3.8.3` 均仍不满足当前稳定升级条件

本批次验证命令：

```bash
node -e "const fs=require('fs'); const nvm=fs.readFileSync('.nvmrc','utf8').trim().replace(/^v/,''); const nodeVersion=fs.readFileSync('.node-version','utf8').trim().replace(/^v/,''); const actual=process.version.replace(/^v/,''); if (nvm !== nodeVersion) throw new Error('Node version files mismatch: '+nvm+' !== '+nodeVersion); if (actual !== nodeVersion) throw new Error('Unexpected node version: '+actual+' !== '+nodeVersion)"
npm outdated --json
npm view @vitejs/plugin-react@6.0.3 version peerDependencies dependencies engines peerDependenciesMeta --json
npm install @vitejs/plugin-react@6.0.3 --save-dev --package-lock-only --ignore-scripts --no-audit --no-fund
npm view @ant-design/pro-components@latest version peerDependencies dependencies engines --json
npm view eslint-plugin-react@latest version peerDependencies engines --json
npm view eslint-plugin-import@latest version peerDependencies engines --json
npm exec -- prettier --check ../.github/workflows/dev-ut-stage.js.yml ../README.md ../README_zh.md
git diff --check
```

最新批次：后端构建仓库治理收口

- 已确认当前 POM 中没有 `SNAPSHOT`、milestone、RC、alpha、beta 版本依赖
- 已移除父 POM 中不再需要的 `spring-milestones` 仓库声明，避免稳定版构建链路继续暴露预发布仓库解析入口
- 本批次只调整 Maven 仓库声明，不改变依赖版本、业务代码、Java 包名、配置前缀、`DATART_*` 或迁移稳定标识
- 已试评估 `monaco-editor 0.55.1`，该版本新增的 `dompurify 3.2.7` 链路会引入 1 个 moderate 和 1 个 low audit advisory；当前继续保留 `monaco-editor 0.52.2`，保持 `npm audit` 0 漏洞

本批次验证命令：

```bash
rg -n "spring-milestones|repo.spring.io/milestone|SNAPSHOT|M[0-9]|RC[0-9]|alpha|beta" pom.xml core/pom.xml security/pom.xml server/pom.xml data-providers -g 'pom.xml'
mvn -q help:effective-pom -Doutput=/tmp/yu-bi-effective-pom-after-repo-cleanup.xml
mvn -pl server -am -DskipTests package
npm view monaco-editor@0.55.1 version dependencies peerDependencies optionalDependencies engines
npm audit --json
git diff --check
```

最新批次：JsonPath 主版本升级与 OAuth 属性映射边界收口

- 已将 `com.jayway.jsonpath:json-path` 从 `2.7.0` 升级到 `3.0.0`
- 已将 `json-path` 版本抽到父 POM 属性 `json-path.version`，避免 server 模块继续硬编码版本
- 已新增 `OAuth2AttributeMapping` helper，集中 `UserServiceImpl` 和 `ExternalRegisterServiceImpl` 中 OAuth2 用户属性 JSONPath 读取逻辑
- 已新增 `OAuth2AttributeMappingTest`，覆盖嵌套字符串属性、可选映射为空、非字符串属性保持原有强制字符串转换异常行为
- 已确认 reactor 依赖树中 `json-path` 统一到 `3.0.0`，包括 server 直接依赖、Spring Boot test 传递链路和 Calcite 传递链路
- 本批次不改变 OAuth2 配置键、用户注册语义、Java 包名、配置前缀、`DATART_*` 或迁移稳定标识

本批次验证命令：

```bash
rg -n "json-path|JsonPath|com\\.jayway\\.jsonpath|DocumentContext|Configuration\\.defaultConfiguration|Option\\." server core security data-providers -g '*.java' -g 'pom.xml'
mvn -pl server -am -Dtest=datart.server.common.OAuth2AttributeMappingTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl server -am dependency:tree -Dincludes=com.jayway.jsonpath:json-path
mvn -pl server -am -DskipTests package
git diff --check
```

最新批次：JavaCC 插件升级与 SQL parser 生成链路验证

- 已将 `org.codehaus.mojo:javacc-maven-plugin` 从 `2.4` 升级到 `3.2.0`
- 已通过 `clean test` 强制删除旧 generated-sources 后重新生成 SQL parser，确认 JavaCC 7.0.13 生成链路可在 JDK 21 下编译
- 已确认 SQL parser / query processor / SQL render 相关 13 个用例通过
- 新插件生成日志仍有 4 个语法 choice warning，属于 `Parser.jj` 既有语法结构提示；旧 generated source 的 deprecated annotation warning 已消失
- 本批次不升级 Calcite，不修改 `Parser.jj` 语法，不改变 SQL 方言、Java 包名、配置前缀、`DATART_*` 或迁移稳定标识

本批次验证命令：

```bash
mvn versions:display-plugin-updates -DgenerateBackupPoms=false -Dincludes=org.codehaus.mojo:javacc-maven-plugin
mvn -pl data-providers/data-provider-base -am -Dtest=datart.data.provider.calcite.SqlParserUtilsTest,datart.data.provider.calcite.SqlQueryScriptProcessorTest,datart.data.provider.jdbc.SqlScriptRenderTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl data-providers/data-provider-base -am -Dtest=datart.data.provider.calcite.SqlParserUtilsTest,datart.data.provider.calcite.SqlQueryScriptProcessorTest,datart.data.provider.jdbc.SqlScriptRenderTest -Dsurefire.failIfNoSpecifiedTests=false clean test
mvn -pl server -am -DskipTests package
git diff --check
```

最新批次：MySQL Connector/J 补丁线收口

- 已通过父 POM 覆盖 Spring Boot BOM 的 `mysql.version`，将 `com.mysql:mysql-connector-j` 从 `9.6.0` 升级到 `9.7.0`
- `core` 模块直接依赖和 `mybatis-generator` profile 中的 MySQL driver 继续共用同一个 BOM 属性版本，避免双轨版本
- 已确认当前源码和配置继续使用稳定驱动类 `com.mysql.cj.jdbc.Driver`
- 本批次只做 JDBC driver 补丁升级，不修改数据库配置键、MyBatis Generator 主版本、Java 包名、配置前缀、`DATART_*` 或迁移稳定标识
- Spring Boot 4、Spring Security 7、Shiro 3 alpha、Calcite 1.42、MyBatis Generator 2、DingTalk 2、Springdoc 3、HikariCP 7 仍按中高风险或主版本跳跃暂缓

本批次验证命令：

```bash
mvn versions:display-dependency-updates -DprocessDependencyManagement=false -DgenerateBackupPoms=false
mvn dependency:tree -Dincludes=com.mysql:mysql-connector-j
mvn -pl data-providers/jdbc-data-provider -am -Dtest=datart.data.provider.jdbc.ProviderFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl core -Dtest=datart.core.common.POIUtilsTest test
mvn -pl server -am -DskipTests package
git diff --check
```

最新批次：HikariCP 主版本升级与配置映射基线

- 已通过 Spring Boot BOM 属性 `hikaricp.version` 将 `com.zaxxer:HikariCP` 从 `6.3.3` 升级到 `7.1.0`
- 已将 `DataSourceFactoryHikariImpl` 中的 Hikari 配置构建拆为包内可测试方法，`createDataSource` 的外部行为保持不变
- 新增 `DataSourceFactoryHikariImplTest`，不创建真实连接池，覆盖 JDBC driver、url、用户名、密码、连接超时、初始化失败超时和自定义 data source properties 映射
- 已确认 `core` 的 Spring Boot JDBC 传递链路和 `jdbc-data-provider` 直接依赖均解析到 `HikariCP 7.1.0`
- 本批次不改变数据库配置键、不改 Java 包名、配置前缀、`DATART_*` 或迁移稳定标识

本批次验证命令：

```bash
mvn help:evaluate -Dexpression=hikaricp.version -q -DforceStdout
mvn -pl data-providers/jdbc-data-provider -am dependency:tree -Dincludes=com.zaxxer:HikariCP
mvn -pl data-providers/jdbc-data-provider -am -Dtest=datart.data.provider.jdbc.DataSourceFactoryHikariImplTest,datart.data.provider.jdbc.ProviderFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl server -am -DskipTests package
git diff --check
```

验证说明：

- 普通沙箱首次解析 `HikariCP 7.1.0` 时因无法写 `~/.m2` tracking 文件失败；提权复跑依赖树和定向测试通过
- `DataSourceFactoryHikariImplTest` 2 个用例、`ProviderFactoryTest` 5 个用例通过
- `mvn -pl server -am -DskipTests package` 已通过，覆盖 JDK 21 编译、前端 build、task build 和安装包 assembly
- 主构建仍有既有前端大 chunk warning，本批次未处理包体拆分

最新批次：后端 Maven 版本属性集中管理

- 已将一批后端直依赖和插件版本统一抽到父 POM 属性，包含 `jjwt`、`jose4j`、`dingtalk`、`httpclient5`、`poi`、`pdfbox`、`thumbnailator`、`javassist`、`aspectjweaver`、`calcite`、`sql-formatter`、`mybatis-generator`、`javacc-maven-plugin` 等
- 已删除父 POM 中长期注释掉且无实际使用的 Spring Cloud Alibaba BOM 声明块，减少构建治理噪声
- 本批次不改变任何依赖版本，不改变依赖 scope、exclusion、业务代码、Java 包名、配置前缀、`DATART_*` 或迁移稳定标识
- 依赖树已确认属性化后解析版本保持不变：`jjwt 0.13.0`、`jose4j 0.9.6`、`dingtalk 1.1.86`、`calcite-core 1.26.0`、`sql-formatter 2.0.5`、`pdfbox 3.0.7` 等

本批次验证命令：

```bash
mvn dependency:tree -Dincludes=io.jsonwebtoken:jjwt-api,io.jsonwebtoken:jjwt-impl,io.jsonwebtoken:jjwt-jackson,org.bitbucket.b_c:jose4j,com.aliyun:dingtalk,org.apache.calcite:calcite-core,com.github.vertical-blank:sql-formatter,org.apache.pdfbox:pdfbox,org.apache.poi:poi-ooxml,org.apache.httpcomponents.client5:httpclient5,org.javassist:javassist,net.coobird:thumbnailator,org.mybatis.generator:mybatis-generator-core
mvn -pl security -am -Dtest=datart.security.test.jwt.TestJwkParse,datart.security.manager.shiro.ShiroAuthenticationTokenAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl data-providers/jdbc-data-provider -am -Dtest=datart.data.provider.calcite.SqlParserUtilsTest,datart.data.provider.calcite.SqlQueryScriptProcessorTest,datart.data.provider.jdbc.ProviderFactoryTest,datart.data.provider.sql.SqlScriptRenderExamplesTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl server -am -DskipTests package
git diff --check
```

最新批次：后端补丁线与前端布局类型边界收口

- 已将 Spring Boot parent 从 `3.5.12` 升级到 `3.5.15`，继续保持 Boot 3.5 稳定线，不跳 Boot 4
- 已确认 Spring Security 随 Boot 3.5.15 解析到 `6.5.11`，暂不升级 Spring Security 7
- 已将 `mybatis-spring-boot-starter` 从 `3.0.4` 升级到 `3.0.5`
- 已将 `mybatis-generator-core` / `mybatis-generator-maven-plugin` 从 `1.4.0` 升级到 `1.4.2`
- 已为 `core` 的 `mybatis-generator` profile 中插件依赖 `com.mysql:mysql-connector-j` 补齐显式 `${mysql.version}`，避免插件依赖不继承项目 `dependencyManagement` 导致 profile 模型解析失败
- 已将 Shiro 从 `2.0.5` 升级到 `2.0.6`，继续停留在 Shiro 2 稳定线
- 已将 DingTalk SDK 从 `1.1.86` 升级到 `1.1.100`，继续停留在 1.x 线
- 已显式覆盖 Hibernate Validator 到 `8.0.4.Final`，继续保持 Jakarta Validation 3 线
- 已新增 `BeanUtilsTest`，覆盖 `BeanUtils.requireNotNull` 和 Jakarta Validation `@NotBlank` / `@Pattern` 行为，约束 message 显式固定，避免受系统 Locale 影响
- 已移除 `react-grid-layout/legacy` 的本地类型 shim，改用 `react-grid-layout 2.2.3` 官方 legacy 入口类型
- 已将 Dashboard 自动布局回调类型收口到官方只读 `Layout`，进入 Redux 前浅拷贝为项目内部可变 `LayoutItem[]`
- 本批次不改 Java 包名 `datart.*`、配置前缀 `datart.*`、`DATART_*`、迁移常量和内部稳定标识
- Hibernate Validator 9.1.1 暂缓：它目标 Jakarta Validation 3.1 / Jakarta EL 6，而当前 Boot 3.5 主线仍管理 Jakarta Validation API 3.0.2；不作为当前稳定基线
- Springdoc 3 暂缓：当前稳定 3.x 已进入 Boot 4 / Spring 7 生态，不并入 Boot 3.5 主线
- MyBatis Generator `generate` 未执行：真实生成依赖本地 MySQL `127.0.0.1:3306/datart` schema；本批次只验证 profile 模型和插件依赖可解析

本批次验证命令：

```bash
mvn help:evaluate -Dexpression=spring-security.version -q -DforceStdout
mvn help:evaluate -Dexpression=hibernate-validator.version -q -DforceStdout
mvn -pl security -am dependency:tree -Dincludes=org.apache.shiro:*,org.springframework.security:*,com.aliyun:dingtalk,org.hibernate.validator:hibernate-validator,jakarta.validation:jakarta.validation-api,org.bouncycastle:*
mvn -pl core dependency:tree -Dincludes=org.mybatis.generator:mybatis-generator-core,org.mybatis.spring.boot:mybatis-spring-boot-starter,org.hibernate.validator:hibernate-validator,jakarta.validation:jakarta.validation-api
mvn -pl core -Dtest=datart.core.common.BeanUtilsTest test
mvn -pl security -am test
mvn -pl core -DskipTests install
mvn -pl core -Pmybatis-generator mybatis-generator:help -Ddetail=false
npm run checkTs
npm run test -- src/app/pages/DashBoardPage/hooks/__tests__/useGridLayoutMap.test.ts src/app/pages/DashBoardPage/utils/__tests__/widget.test.ts src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/__tests__/layoutRuntime.test.ts
npm exec -- prettier --check src/react-app-env.d.ts src/app/pages/DashBoardPage/pages/BoardEditor/AutoEditor/AutoBoardEditor.tsx src/app/pages/DashBoardPage/pages/Board/AutoDashboard/AutoBoardCore.tsx
mvn -pl server -am -DskipTests package
git diff --check
```

最新批次：用户可见旧品牌与 Nashorn 运行时提示清理

- 已将服务端 i18n 中注册激活、邀请、找回密码邮件标题 / 主题 / 正文提示里的用户可见品牌从 `Datart` 收口为 `yu-bi`
- 已将 JDBC 配置模板中 `enableSpecialSQL` 和 `enableSyncSchemas` 的用户可见描述从 `Datart` 收口为 `yu-bi`
- 已将英文、中文和默认 i18n 文件同步处理，保持同一批用户可见文案一致
- 已从 `JavascriptUtils` 默认脚本引擎候选中移除 `nashorn`，默认主链只保留 GraalJS / 通用 JavaScript 引擎名称
- 已将 JavaScript 引擎缺失错误提示从“安装 Nashorn 或 GraalJS”收口为“安装 GraalJS”
- 已补充 `JavascriptUtilsTest`，确认默认候选不再包含 `nashorn` 且仍包含 `graal.js`
- 本批次不改 i18n key 名、文件名、Java 包名 `datart.*`、配置前缀 `datart.*`、`DATART_SCRIPT_ENGINE`、Quartz `DatartScheduleCluster` 等内部稳定标识
- Quartz scheduler name 暂不改：`application.yml` 中的 `DatartScheduleCluster` 可能影响持久化调度实例标识，应作为调度迁移专题单独评估

本批次验证命令：

```bash
rg -n "Datart|DATART|Nashorn" server/src/main/resources/i18n core/src/main/java/datart/core/common/JavascriptUtils.java core/src/test/java/datart/core/common/JavascriptUtilsTest.java
mvn -pl core -Dtest=datart.core.common.JavascriptUtilsTest test
mvn -pl server -am -DskipTests package
git diff --check
```

最新批次：JDBC provider 内置驱动元数据与 CustomSqlDialect 回退基线

- 已扩展 `ProviderFactoryTest`，通过公开 `createDataProvider(..., false)` 路径覆盖 30 个内置 JDBC driver 元数据
- 新增动态测试，确认每个内置 `db-type` 都能在不初始化真实连接池、不连接数据库的情况下创建 adapter 并解析 SQL dialect
- 已确认内置 driver 元数据继续保持大写 `dbType`、默认 adapter 和默认 `quoteIdentifiers` 兜底
- 测试暴露出一类既有问题：部分 JDBC 类型不在 Calcite 默认 `DatabaseProduct` 中，会回退到 `CustomSqlDialect`，但内置元数据没有 `literalQuote` / `identifierQuote` 时会在 Jakarta Validation 阶段失败
- 已在 `CustomSqlDialect` 中为回退方言补齐默认 literal quote `'` 和 identifier quote `"`，并在 Bean Validation 前应用默认值
- 本批次不连接真实数据库、不升级 Calcite、不改 `jdbc-driver.yml` 中各数据库类型标识、不改 Java 包名、配置前缀、`DATART_*` 或迁移稳定标识
- 该基线为后续 Calcite 1.42、JDBC driver 元数据和方言适配升级提供快速回归入口

本批次验证命令：

```bash
mvn -pl data-providers/jdbc-data-provider -am -Dtest=datart.data.provider.jdbc.ProviderFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl data-providers/jdbc-data-provider -am -Dtest=datart.data.provider.calcite.SqlParserUtilsTest,datart.data.provider.calcite.SqlQueryScriptProcessorTest,datart.data.provider.jdbc.ProviderFactoryTest,datart.data.provider.sql.SqlScriptRenderExamplesTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl server -am -DskipTests package
git diff --check
```

最新批次：Monaco 编辑器组件运行时 smoke 补强

- 已补强 `MonacoEditor` 组件测试，从仅覆盖 runtime 加载失败态扩展到成功挂载主路径
- 新增可控 Monaco runtime mock，验证组件会创建 model、创建 editor、合并 `editorWillMount` 返回选项、调用 `editorDidMount`
- 已覆盖用户编辑触发 `onChange`、受控 `value` 变更通过 `pushEditOperations` 同步、`language` 变更调用 `setModelLanguage`、`theme` 变更调用 `setTheme`
- 已覆盖组件卸载时调用 `editorWillUnmount` 和 `editor.dispose`
- 本批次不升级 `monaco-editor` 版本；`0.55.1` 仍因 `dompurify 3.2.7` audit 风险暂缓
- 该 smoke 为后续 Monaco 版本升级、Vite chunk 拆分和 SQL / JS 编辑器入口调整提供组件级回归入口

本批次验证命令：

```bash
npm run test -- src/app/components/MonacoEditor/__tests__/index.test.tsx
npm run test -- src/app/components/MonacoEditor/__tests__/runtime.test.ts src/app/components/MonacoEditor/__tests__/index.test.tsx src/app/pages/MainPage/pages/ViewPage/Main/Editor/__tests__/completionRuntime.test.ts
npm run checkTs
npm exec -- prettier --check src/app/components/MonacoEditor/__tests__/index.test.tsx
git diff --check
```

最新批次：前端 Vite 分包配置基线补强

- 已将主 Vite 构建中的 `manualChunks` 规则抽到 `vite.shared.mts#createVendorManualChunks`
- 已为关键运行时依赖分包建立测试基线，覆盖 AntD、ECharts / ZRender、Quill / react-quill-new、React、react-grid-layout、reveal.js 和 flexlayout-react
- 已确认应用源码和未归类依赖继续交给 Rollup / Rolldown 默认策略处理
- 本批次不改变现有分包策略、不升级依赖版本、不处理既有大 chunk warning，只为后续包体治理和运行时依赖升级提供回归入口

本批次验证命令：

```bash
npm run test -- vite.shared.test.mts
npm run eslint -- vite.config.mts vite.shared.mts vite.shared.test.mts
npm exec -- prettier --check vite.config.mts vite.shared.mts vite.shared.test.mts ../docs/tech-stack-modernization-plan.md
npm run checkTs
npm run build
npm run build:task
git diff --check
```

验证说明：

- `vite.shared.test.mts` 17 个用例通过
- Vite 配置文件 ESLint 和 Prettier 检查通过
- `npm run checkTs` 已通过
- 主构建和 task bundle 构建均通过，继续使用 `vite v8.1.0`
- 主构建仍有既有大 chunk warning，本批次只建立分包规则回归基线，不做包体拆分

最新批次：前端 vendor 分包边界细化

- 已在 `createVendorManualChunks` 中细化 Ant Design 生态分包：
  - `antd` 保持进入 `antdDesign`
  - `@ant-design/pro-*` 进入 `antdPro`
  - `@ant-design/icons` / `@ant-design/icons-svg` 进入 `antdIcons`
- 已将 AntV 生态包 `@antv/*` 统一拆到 `antv` vendor chunk，降低 PivotSheet 业务 wrapper chunk 体积
- 已扩展 `vite.shared.test.mts`，覆盖 Pro Components、Pro Table、icons、icons-svg、AntV S2、AntV G2 等新分包规则
- 本批次不改变源码加载方式、不升级依赖版本、不调高 `chunkSizeWarningLimit`，只细化现有 vendor 边界，让后续包体治理可定位性更强

本批次验证命令：

```bash
npm run test -- vite.shared.test.mts
npm run eslint -- vite.shared.mts vite.shared.test.mts vite.config.mts
npm exec -- prettier --check vite.shared.mts vite.shared.test.mts vite.config.mts
npm run checkTs
npm run build
npm run build:task
npm audit --json
git diff --check
```

验证说明：

- `vite.shared.test.mts` 22 个用例通过
- Vite 配置文件 ESLint 和 Prettier 检查通过
- `npm run checkTs` 已通过
- 主构建和 task bundle 构建均通过，继续使用 `vite v8.1.0`
- `npm audit --json` 仍为 0 vulnerabilities
- 主构建中 `antdDesign` 从约 `2,149.72 kB / gzip 655.82 kB` 降到约 `1,321.55 kB / gzip 409.69 kB`
- 新增 `antdIcons` 约 `356.99 kB / gzip 100.85 kB`、`antdPro` 约 `469.16 kB / gzip 141.62 kB`、`antv` 约 `1,716.88 kB / gzip 504.54 kB`
- 原 `AntVS2Wrapper` 大 chunk 收缩为约 `1.42 kB / gzip 0.79 kB` 的业务 wrapper；AntV S2 生态体积已归入 `antv`
- `editor.api`、`echarts`、地图 JSON 和 `antv` 仍超过 500 kB，保留为后续构建产物治理对象

最新批次：ECharts 默认主题运行时入口收口

- 已将 `ensureEChartsDefaultTheme` 从独立 `import('echarts')` loader 收口为复用 `loadEChartsRuntime()`
- ECharts 默认主题注册和图表运行时加载现在共享同一个 ECharts runtime promise，后续如评估 ECharts core/components 按需迁移，只需优先收口 `echartsRuntime`
- 已保留默认主题注册自己的 promise 缓存和失败重试语义，避免重复注册主题或吞掉加载失败后的重试
- 已扩展 `echartsThemeRuntime` 测试，确认默认主题注册后再调用 `loadEChartsRuntime()` 不会重复触发 runtime loader
- 本批次不改变页面加载方式、不升级 ECharts、不拆 ECharts 主包，只减少重复动态加载入口

本批次验证命令：

```bash
npm run test -- src/app/utils/__tests__/echartsThemeRuntime.test.ts src/app/components/ChartGraph/__tests__/echartsRuntime.test.ts src/app/components/ChartGraph/WordCloudChart/__tests__/runtime.test.ts
npm run checkTs
npm exec -- prettier --check src/app/utils/echartsThemeRuntime.ts src/app/utils/__tests__/echartsThemeRuntime.test.ts
npm run build
npm run build:task
git diff --check
```

验证说明：

- ECharts 主题 / runtime / 词云 runtime 相关 3 个测试文件、9 个用例通过
- `npm run checkTs` 已通过
- 主构建和 task bundle 构建均通过，继续使用 `vite v8.1.0`
- 构建产物体积基本不变，本批次是运行时入口收口，不是 ECharts 拆包

最新批次：前端构建产物体积报告基线

- 已新增 `frontend/scripts/report-build-chunks.mjs`，读取 `build/static/js` 和 `build/task/index.js`，输出最大 chunk 和超过阈值的 chunk
- 已新增 npm script `build:report`，默认阈值为 `500 KiB`，可通过 `YU_BI_CHUNK_REPORT_THRESHOLD_KIB` 和 `YU_BI_CHUNK_REPORT_LIMIT` 调整
- 已新增 `scripts/__tests__/report-build-chunks.test.mts`，覆盖报告脚本输出结构，避免后续误改报告入口
- 当前脚本只报告、不失败，不调高 Vite `chunkSizeWarningLimit`，先用于构建产物治理的可重复基线

本批次验证命令：

```bash
npm run build
npm run build:task
npm run build:report
npm run test -- scripts/__tests__/report-build-chunks.test.mts
npm run eslint -- scripts/report-build-chunks.mjs scripts/__tests__/report-build-chunks.test.mts
npm exec -- prettier --check package.json package-lock.json scripts/report-build-chunks.mjs scripts/__tests__/report-build-chunks.test.mts
npm run checkTs
npm audit --json
git diff --check
```

验证说明：

- `build:report` 已输出当前 100 个 JS / task 文件，超过 `500 KiB` 的 chunk 为 6 个
- 当前超过阈值的对象为 `editor.api`、`antv`、`antdDesign`、`geo-china-city.map`、`echarts`、`geo-china.map`
- `scripts/__tests__/report-build-chunks.test.mts` 1 个用例通过
- `npm run checkTs` 已通过
- `npm audit --json` 仍为 0 vulnerabilities

最新批次：前端构建产物体积报告增强

- `build:report` 已新增 gzip 体积列，同时输出 raw / gzip 两种体积，避免只按未压缩体积判断治理优先级
- 已新增可选强门禁开关 `YU_BI_CHUNK_REPORT_FAIL_ON_OVERSIZED=1`；默认不启用，CI 当前仍只报告不失败
- 已扩展 `report-build-chunks` 测试，覆盖 gzip 输出和可选失败模式
- 本批次不改变构建产物、不调整 Vite 阈值、不增加默认 CI 失败条件，只增强后续治理工具能力

本批次验证命令：

```bash
npm run build:report
npm run test -- scripts/__tests__/report-build-chunks.test.mts
npm run eslint -- scripts/report-build-chunks.mjs scripts/__tests__/report-build-chunks.test.mts
npm exec -- prettier --check ../docs/tech-stack-modernization-plan.md scripts/report-build-chunks.mjs scripts/__tests__/report-build-chunks.test.mts
npm run checkTs
npm audit --json
git diff --check
```

验证说明：

- `build:report` 已输出 raw / gzip 两列
- `scripts/__tests__/report-build-chunks.test.mts` 2 个用例通过
- `npm run checkTs` 已通过
- `npm audit --json` 仍为 0 vulnerabilities

最新批次：前端构建体积 gzip 阈值报告

- `build:report` 已支持独立 gzip 阈值 `YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB`
- 报告头现在分别输出 `rawOversized`、`gzipOversized` 和去重后的 `oversized`
- 每个 chunk 行增加 `flags=raw,gzip` 标记，便于区分 raw 超限、gzip 超限或两者都超限
- 默认 gzip 阈值为 `off`，CI 当前仍只按 raw `500 KiB` 输出报告，不新增失败条件
- 当前以 `YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB=500` 复核，gzip 超限对象为 `editor.api` 和 `geo-china-city.map`

本批次验证命令：

```bash
npm run build:report
YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB=500 npm run build:report
npm run test -- scripts/__tests__/report-build-chunks.test.mts
npm run eslint -- scripts/report-build-chunks.mjs scripts/__tests__/report-build-chunks.test.mts
npm exec -- prettier --check ../docs/tech-stack-modernization-plan.md scripts/report-build-chunks.mjs scripts/__tests__/report-build-chunks.test.mts
npm run checkTs
npm audit --json
git diff --check
```

验证说明：

- 默认报告为 `rawOversized=6`、`gzipOversized=0`、`oversized=6`
- 设置 gzip `500 KiB` 阈值后为 `rawOversized=6`、`gzipOversized=2`、`oversized=6`
- `scripts/__tests__/report-build-chunks.test.mts` 3 个用例通过
- `npm run checkTs` 已通过
- `npm audit --json` 仍为 0 vulnerabilities

最新批次：前端 CI 构建体积报告接入

- 已在 `.github/workflows/dev-ut-stage.js.yml` 中将 `npm run build:report` 接到前端 `build:task` 和 `build` 之后
- CI 现在会在主线 push / PR 门禁中输出当前超阈值 chunk 清单，方便持续观察构建产物治理进度
- `build:report` 仍是报告型步骤，不设置失败阈值，不通过调高 `chunkSizeWarningLimit` 掩盖 Vite warning
- 本批次只调整 CI 可见性，不改业务代码、不升级依赖、不增加本地 push hook 耗时

本批次验证命令：

```bash
npm run build
npm run build:task
npm run build:report
npm exec -- prettier --check ../.github/workflows/dev-ut-stage.js.yml ../docs/tech-stack-modernization-plan.md
git diff --check
```

最新批次：地图资源静态化与构建报告分层

- 已将 `BasicOutlineMapChart` 的 `geo-china.map.json` / `geo-china-city.map.json` 从动态 JSON JS chunk 改为 `?url` 静态资源加载
- 新增 `geoMapRuntime`，统一处理地图资源 URL、`fetch`、缓存、并发复用和失败重试；图表侧继续保持原有 ECharts `registerMap` 和渲染重放逻辑
- 已补 `geoMapRuntime` 测试，覆盖同层级并发复用、加载后缓存、失败后可重试
- `build:report` 已拆成 `chunk report` 和 `asset report` 两段输出：JS / task bundle 与 `static/media` 资源分开统计 raw / gzip 阈值，避免地图资源迁出 JS 后失去体积可见性
- 主构建后地图 JSON 已进入 `build/static/media`，不再作为 `build/static/js/geo-china*.map.*.js` chunk 参与 JS 包预算
- JS chunk raw 超限对象从 6 个降到 4 个：`editor.api`、`antv`、`antdDesign`、`echarts`
- 设置 `YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB=500` 后，JS gzip 超限对象从 2 个降到 1 个，仅剩 `editor.api`
- 地图资源本体仍在 asset report 中可见：`geo-china-city.map` raw / gzip 均超限，`geo-china.map` raw 超限；后续如继续治理，应面向地图数据压缩、层级拆分或服务端资源策略，而不是再把它们塞回 JS chunk

本批次验证命令：

```bash
npm run test -- src/app/components/ChartGraph/BasicOutlineMapChart/__tests__/geoMapRuntime.test.ts src/app/components/ChartGraph/BasicOutlineMapChart/__tests__/BasicOutlineMapChart.test.jsx
npm run eslint -- src/app/components/ChartGraph/BasicOutlineMapChart/BasicOutlineMapChart.tsx src/app/components/ChartGraph/BasicOutlineMapChart/geoMapRuntime.ts src/app/components/ChartGraph/BasicOutlineMapChart/__tests__/geoMapRuntime.test.ts
npm exec -- prettier --check ../docs/tech-stack-modernization-plan.md src/app/components/ChartGraph/BasicOutlineMapChart/BasicOutlineMapChart.tsx src/app/components/ChartGraph/BasicOutlineMapChart/geoMapRuntime.ts src/app/components/ChartGraph/BasicOutlineMapChart/__tests__/geoMapRuntime.test.ts
npm run checkTs
npm run build
npm run build:task
npm run build:report
YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB=500 npm run build:report
npm run test -- scripts/__tests__/report-build-chunks.test.mts src/app/components/ChartGraph/BasicOutlineMapChart/__tests__/geoMapRuntime.test.ts src/app/components/ChartGraph/BasicOutlineMapChart/__tests__/BasicOutlineMapChart.test.jsx
npm run eslint -- scripts/report-build-chunks.mjs scripts/__tests__/report-build-chunks.test.mts src/app/components/ChartGraph/BasicOutlineMapChart/BasicOutlineMapChart.tsx src/app/components/ChartGraph/BasicOutlineMapChart/geoMapRuntime.ts src/app/components/ChartGraph/BasicOutlineMapChart/__tests__/geoMapRuntime.test.ts
npm audit --json
git diff --check
```

验证说明：

- 地图 runtime 和构建报告相关 3 个测试文件、7 个用例通过
- 相关源码和脚本 ESLint 通过
- `npm run checkTs` 已通过
- 主构建和 task bundle 构建通过
- `npm audit --json` 仍为 0 vulnerabilities

最新批次：styled-components 补丁线收口

- 2026-06-25 复核 `npm outdated --json` 时新增 `styled-components 6.4.2 -> 6.4.3`
- 已复核 `styled-components 6.4.3` 的 peer、dependencies、engines 与 `6.4.2` 一致，Node engine 仍为 `>=16`，与 Node 24 / React 19 基线兼容
- 已将 `styled-components` 升级到 `6.4.3`，并保持 `package.json` 精确版本声明，不恢复 `^` 范围
- 依赖树已确认 `babel-plugin-styled-components` 和项目运行时均复用 `styled-components 6.4.3`
- 升级后 `npm outdated --json` 又回到 7 个暂缓项：`@types/node`、`@vitejs/plugin-react`、AntD、ESLint、Monaco、Quill、`react-quill-new`
- 本批次不改变样式代码，只做同一主版本补丁线升级

本批次验证命令：

```bash
npm view styled-components@6.4.3 version peerDependencies dependencies engines dist-tags --json
npm view styled-components@6.4.2 version peerDependencies dependencies engines --json
npm install styled-components@6.4.3 --save --ignore-scripts --no-audit --no-fund
npm install --package-lock-only --ignore-scripts --no-audit --no-fund
npm ls styled-components babel-plugin-styled-components --all
npm run test -- src/styles/theme/__tests__/ThemeProvider.test.tsx src/styles/__tests__/media.test.ts vite.shared.test.mts
npm run checkTs
npm audit --json
npm outdated --json
npm run build:report
git diff --check
```

验证说明：

- 样式主题、媒体查询和 Vite shared 配置测试 3 个文件、26 个用例通过
- `npm run checkTs` 已通过
- `npm audit --json` 仍为 0 vulnerabilities
- `build:report` 基线保持不变：JS chunk raw 超限 4 个，asset raw 超限 2 个

最新批次：富文本图表只读协议 smoke 补强

- 已新增 `BasicRichText` 模型层 smoke，覆盖分享页 / 图表预览只读环境下的富文本配置协议
- 测试模拟富文本图表的数据集、分组字段、聚合字段和 Delta 配置，确认 `getOptions` 能为 `ChartRichTextAdapter` 产出：
  - `isEditing: false`
  - 稳定的 `initContent`
  - 可替换计算字段的 `dataList`
  - `openQuillMarkdown` 配置透传
- 已补编辑环境边界断言：仅当 `widgetSpecialConfig.env` 存在时，富文本图表才进入编辑模式
- 现有 `RichTextEditorRuntime.smoke.test.tsx` 已覆盖 Quill 2 实际运行时和 `ChartRichTextAdapter` 只读展示，本批次补的是图表模型到适配器之间的协议边界
- 本批次不改业务代码、不改富文本内容格式，只补分享页相关只读链路回归证据

本批次验证命令：

```bash
npm run test -- src/app/components/ChartGraph/BasicRichText/__tests__/BasicRichText.test.ts src/app/components/ChartGraph/BasicRichText/__tests__/RichTextEditorRuntime.smoke.test.tsx src/app/components/ChartGraph/BasicRichText/__tests__/runtimeHelpers.test.ts src/app/components/ChartGraph/BasicRichText/__tests__/content.test.ts
npm run eslint -- src/app/components/ChartGraph/BasicRichText/__tests__/BasicRichText.test.ts
npm exec -- prettier --check src/app/components/ChartGraph/BasicRichText/__tests__/BasicRichText.test.ts
npm run checkTs
npm audit --json
git diff --check
```

验证说明：

- 富文本相关 4 个测试文件、20 个用例通过
- 新增测试文件 ESLint 和 Prettier 检查通过
- `npm run checkTs` 已通过
- `npm audit --json` 仍为 0 vulnerabilities

最新批次：地图 JSON 静态资源无损压缩

- 已对 `geo-china.map.json` 和 `geo-china-city.map.json` 做 JSON parse 后再 `JSON.stringify` 的无损 minify，只移除格式化空白，不改变地图结构和坐标数据
- 源文件大小变化：
  - `geo-china.map.json`：`1,003,241 bytes -> 581,646 bytes`
  - `geo-china-city.map.json`：`1,311,291 bytes -> 1,207,908 bytes`
- JSON 结构复核通过：`geo-china.map.json` 仍为 `FeatureCollection` 且 `features.length = 35`，`geo-china-city.map.json` 仍为 `FeatureCollection` 且 `features.length = 367`
- 主构建后 asset report 中地图资源 raw 体积下降：
  - `geo-china.map`：约 `979.73 KiB -> 568.01 KiB`，gzip 约 `224.64 KiB -> 194.23 KiB`
  - `geo-china-city.map`：约 `1280.56 KiB -> 1179.60 KiB`，gzip 约 `698.68 KiB -> 693.80 KiB`
- JS chunk 报告保持不变：raw 超限仍为 4 个，gzip `500 KiB` 阈值下 JS 超限仍仅 `editor.api`
- 地图静态资源仍在 asset report 中可见；后续若继续治理地市地图 gzip 超限，需要评估地图数据拆分、按省懒加载或服务端资源策略

本批次验证命令：

```bash
node -e "const fs=require('fs'); for (const f of ['frontend/src/app/components/ChartGraph/BasicOutlineMapChart/geo-china.map.json','frontend/src/app/components/ChartGraph/BasicOutlineMapChart/geo-china-city.map.json']) { const data=JSON.parse(fs.readFileSync(f,'utf8')); console.log(f, data.type, Array.isArray(data.features), data.features.length); }"
npm run test -- src/app/components/ChartGraph/BasicOutlineMapChart/__tests__/geoMapRuntime.test.ts src/app/components/ChartGraph/BasicOutlineMapChart/__tests__/BasicOutlineMapChart.test.jsx
npm run checkTs
npm run build
npm run build:task
npm run build:report
YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB=500 npm run build:report
npm run test -- scripts/__tests__/report-build-chunks.test.mts src/app/components/ChartGraph/BasicOutlineMapChart/__tests__/geoMapRuntime.test.ts
npm audit --json
git diff --check
```

验证说明：

- 地图 runtime 与构建报告相关 2 个测试文件、6 个用例通过
- `npm run checkTs` 已通过
- 主构建和 task bundle 构建通过
- `npm audit --json` 仍为 0 vulnerabilities

最新批次：AntV vendor 分包细化

- 已将原统一的 `antv` vendor chunk 继续细分为 `antvG2`、`antvS2`、`antvG` 和 fallback `antv`
- `@antv/s2`、`@antv/s2-react` 进入 `antvS2`
- `@antv/g`、`@antv/g-lite`、`@antv/g-canvas`、`@antv/g-math`、`@antv/g-plugin-dragndrop` 进入 `antvG`
- `@antv/g2`、`@antv/component`、`@antv/coord`、`@antv/scale` 进入 `antvG2`
- 其他 `@antv/*` 继续进入 fallback `antv`，例如 `@antv/util`
- 原 `antv` 单包约 `1,676.65 KiB / gzip 485.72 KiB`
- 拆分后主要 AntV chunk：
  - `antvG2` 约 `598.40 KiB / gzip 177.87 KiB`
  - `antvS2` 约 `517.45 KiB / gzip 152.29 KiB`
  - `antvG` 约 `508.72 KiB / gzip 134.40 KiB`
  - fallback `antv` 约 `52.57 KiB / gzip 20.70 KiB`
- JS 文件数从 `98` 增加到 `101`，JS raw 超限从 `4` 增加到 `6`，这是 AntV 包拆细后的统计口径变化
- 设置 gzip `500 KiB` 阈值后，JS gzip 超限仍仅 `editor.api`，AntV 拆分后的每个 gzip 体积均低于 `500 KiB`
- 本批次不升级 AntV 依赖版本、不改 PivotSheet 业务加载入口、不改变浏览器运行时协议

本批次验证命令：

```bash
npm run build:report
YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB=500 npm run build:report
npm run test -- scripts/__tests__/report-build-chunks.test.mts vite.shared.test.mts src/app/components/ChartGraph/PivotSheetChart/__tests__/runtime.test.ts src/app/components/ChartGraph/PivotSheetChart/__tests__/PivotSheetChart.test.jsx
npm run eslint -- vite.shared.mts vite.shared.test.mts scripts/__tests__/report-build-chunks.test.mts
npm exec -- prettier --check vite.shared.mts vite.shared.test.mts scripts/__tests__/report-build-chunks.test.mts ../docs/tech-stack-modernization-plan.md
npm run checkTs
npm audit --json
git diff --check
```

验证说明：

- `build:report` 当前输出 JS chunk `files=101`、`rawOversized=6`
- 设置 gzip `500 KiB` 阈值后，JS chunk `gzipOversized=1`
- asset report 保持 `files=6`、`rawOversized=2`，设置 gzip `500 KiB` 阈值后 `gzipOversized=1`
- 后续构建产物治理优先继续观察 `editor.api`、`antdDesign`、`echarts` 和地图资源，不把 AntV 拆分后的 raw 统计增加误判为压缩传输体积回退

最新批次：Monaco editor vendor 分包细化

- 已将 `monaco-editor/esm/vs` 内部模块按稳定目录分成 `monacoBase`、`monacoPlatform`、`monacoEditor`
- `MonacoEditor/runtime.ts#loadMonaco()` 仍保持原有动态加载入口，不改变业务组件调用协议
- SQL 编辑器、计算字段编辑器和 Mock Data 编辑器继续复用同一份 Monaco runtime promise
- 原 `editor.api` chunk 约 `2,207.29 KiB / gzip 559.72 KiB`
- 拆分后主要 Monaco chunk：
  - `monacoEditor` 约 `1,552.74 KiB / gzip 379.31 KiB`
  - `monacoBase` 约 `659.98 KiB / gzip 183.22 KiB`
  - `monacoPlatform` 体积较小，未进入默认 Top 20 报告
- JS gzip `500 KiB` 阈值下超限对象从 `editor.api` 1 个降到 0 个
- JS raw 超限从 `6` 增加到 `7`，这是 Monaco 拆细后的统计口径变化；传输体积维度已经解除 JS 超限
- 曾进一步尝试按 `base/browser`、`base/common`、`editor/contrib`、`editor/standalone`、`editor/browser`、`editor/common` 细拆，但未继续降低最大 `monacoEditor` raw 体积，最终保留更克制的三段分包
- 本批次不升级 `monaco-editor` 版本，不引入 `monaco-editor 0.55.x`，避免重新触发此前 `dompurify` audit 风险

本批次验证命令：

```bash
npm run test -- vite.shared.test.mts src/app/components/MonacoEditor/__tests__/runtime.test.ts src/app/components/MonacoEditor/__tests__/index.test.tsx src/app/pages/MainPage/pages/ViewPage/Main/Editor/__tests__/completionRuntime.test.ts
npm run build
npm run build:task
npm run build:report
YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB=500 npm run build:report
npm run test -- scripts/__tests__/report-build-chunks.test.mts
npm run eslint -- vite.shared.mts vite.shared.test.mts scripts/__tests__/report-build-chunks.test.mts
npm exec -- prettier --check vite.shared.mts vite.shared.test.mts scripts/__tests__/report-build-chunks.test.mts ../docs/tech-stack-modernization-plan.md
npm run checkTs
npm audit --json
git diff --check
```

验证说明：

- Monaco runtime / Vite shared 相关 4 个测试文件、45 个用例通过
- `report-build-chunks` 3 个用例通过
- 主构建和 task bundle 构建通过
- `build:report` 当前输出 JS chunk `files=102`、`rawOversized=7`
- 设置 gzip `500 KiB` 阈值后，JS chunk `gzipOversized=0`
- asset report 保持 `files=6`、`rawOversized=2`，设置 gzip `500 KiB` 阈值后 `gzipOversized=1`
- 后续构建产物治理优先继续观察 raw 维度的 `monacoEditor`、`antdDesign`、`echarts`、地图资源，以及是否有可验证的 Monaco feature 级懒加载边界

最新批次：ECharts runtime 入口复核与词云复用收口

- 已复核当前 ECharts 图表类型，源码实际使用的主要 series 为 `line`、`bar`、`pie`、`scatter`、`map`、`funnel`、`gauge`、`radar`、`custom` 和词云扩展
- 已尝试用 Vite `manualChunks` 将 `zrender`、`echarts/charts`、`echarts/components` 从统一 `echarts` chunk 拆出；构建结果未生成有意义的独立 chunk，未保留该规则
- 已尝试将 `loadEChartsRuntime()` 从 `import('echarts')` 改为 `echarts/core` + 当前图表和组件显式注册；无论通过 `echarts/charts` barrel named imports，还是通过 `echarts/lib/*/install.js` direct imports，构建后 `echarts` chunk 基本仍保持约 `1,128 KiB / gzip 374 KiB`
- 上述按需拆分试验没有达到当前构建治理收益标准，因此不保留 ECharts 主 runtime 行为变更，避免扩大图表注册风险
- 本批次保留低风险运行时入口收口：`WordCloudChart/runtime.ts` 不再单独 `import('echarts')`，改为复用 `loadEChartsRuntime()`，确保词云扩展和普通图表共享同一份 ECharts runtime promise
- 已新增词云 runtime 测试注入点，证明默认词云加载路径复用基础 ECharts runtime loader，并对并发请求只注册一次词云扩展
- 构建报告维持 Monaco 分包后的基线：JS chunk `files=102`、`rawOversized=7`，设置 gzip `500 KiB` 阈值后 JS `gzipOversized=0`
- 本批次不升级 ECharts，不改变图表组件调用方式，不改变 `loadEChartsRuntime()` 对外协议

本批次验证命令：

```bash
npm run test -- src/app/components/ChartGraph/WordCloudChart/__tests__/runtime.test.ts src/app/components/ChartGraph/__tests__/echartsRuntime.test.ts src/app/utils/__tests__/echartsThemeRuntime.test.ts
npm run checkTs
npm run build
npm run build:task
npm run build:report
YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB=500 npm run build:report
npm run eslint -- src/app/components/ChartGraph/WordCloudChart/runtime.ts src/app/components/ChartGraph/WordCloudChart/__tests__/runtime.test.ts
npm exec -- prettier --check src/app/components/ChartGraph/WordCloudChart/runtime.ts src/app/components/ChartGraph/WordCloudChart/__tests__/runtime.test.ts ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

验证说明：

- ECharts / 词云 runtime 相关 3 个测试文件、10 个用例通过
- `npm run checkTs` 已通过
- 主构建和 task bundle 构建通过
- `build:report` 当前输出 JS chunk `files=102`、`rawOversized=7`
- 设置 gzip `500 KiB` 阈值后，JS chunk `gzipOversized=0`
- asset report 保持 `files=6`、`rawOversized=2`，设置 gzip `500 KiB` 阈值后 `gzipOversized=1`
- 测试日志中的 jsdom canvas `getContext` warning 是当前词云 smoke 的测试环境噪声，不影响断言结果

最新批次：图表预览数据入口 smoke 补强

- 已将图表工作台、主工作台图表预览和分享页图表展示前的 dataset 选择逻辑收口到 `app/utils/chartPreviewDataset`
- `selectDatasetByViewBinding` 负责“无视图绑定时优先使用模板 `sampleData`，否则使用查询 `dataset`”的通用规则
- `selectChartPreviewDataset` 负责适配 `ChartPreview` 数据结构
- 已新增单元测试覆盖：
  - 无 `viewId` 且存在 `sampleData` 时使用模板样例数据
  - 有 `viewId` 时使用后端查询返回的 `dataset`
  - `sampleData` 缺失时回退到 `dataset`
- `ChartPreviewBoard` 和 `ChartPreviewBoardForShare` 继续保持原展示协议，只通过共享 helper 获取传入 `ChartIFrameContainer` 的数据集
- `datasetsSelector` 复用同一 helper，避免图表工作台和预览 / 分享链路规则漂移
- 本批次为图表预览和分享页只读图表 smoke 建立最小可维护基线，暂不硬挂载整页组件，避免 Redux、Modal、resize observer 和 iframe 组合导致脆弱测试

本批次验证命令：

```bash
npm run test -- src/app/utils/__tests__/chartPreviewDataset.test.ts
npm run checkTs
npm run eslint -- src/app/utils/chartPreviewDataset.ts src/app/utils/__tests__/chartPreviewDataset.test.ts src/app/pages/ChartWorkbenchPage/slice/selectors.ts src/app/pages/MainPage/pages/VizPage/ChartPreview/ChartPreviewBoard.tsx src/app/pages/SharePage/Chart/ChartPreviewBoardForShare.tsx
npm exec -- prettier --check src/app/utils/chartPreviewDataset.ts src/app/utils/__tests__/chartPreviewDataset.test.ts src/app/pages/ChartWorkbenchPage/slice/selectors.ts src/app/pages/MainPage/pages/VizPage/ChartPreview/ChartPreviewBoard.tsx src/app/pages/SharePage/Chart/ChartPreviewBoardForShare.tsx ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

最新批次：图表 iframe 生命周期 smoke 补强

- 已新增 `ChartIFrameLifecycleAdapter` 组件级 smoke，覆盖 mounted、updated、resize、unmount 生命周期从 React adapter 到 chart lifecycle 的真实调用路径
- 测试通过 mock 资源加载器避免引入外部脚本，保留真实 adapter、event broker、React effect 路径
- 测试发现并修复 unmount 生命周期使用旧 `dataset` / context 的问题：
  - mount effect 依赖保持不变，避免数据更新时重新挂载图表
  - cleanup 改用 ref 保存的最新 `BrokerOption` 和 `BrokerContext`
  - 图表卸载清理时可以拿到最近一次 dataset、尺寸和上下文
- 本批次不改变 `ChartIFrameContainer` 对外协议，不改 iframe / 非 iframe 分支结构

本批次验证命令：

```bash
npm run test -- src/app/components/ChartIFrameContainer/__tests__/ChartIFrameLifecycleAdapter.smoke.test.tsx src/app/components/ChartIFrameContainer/__tests__/ChartIFrameEventBroker.test.jsx
npm run checkTs
npm run eslint -- src/app/components/ChartIFrameContainer/ChartIFrameLifecycleAdapter.tsx src/app/components/ChartIFrameContainer/__tests__/ChartIFrameLifecycleAdapter.smoke.test.tsx
npm exec -- prettier --check src/app/components/ChartIFrameContainer/ChartIFrameLifecycleAdapter.tsx src/app/components/ChartIFrameContainer/__tests__/ChartIFrameLifecycleAdapter.smoke.test.tsx ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

最新批次：Reveal 播放器 runtime 收口

- 已新增 `createRevealPlayerRuntime`，统一 Story 编辑器、Story 播放器和分享页 Story 播放器的 Reveal 初始化、zoom 插件注册、`slidechanged` 监听和销毁逻辑
- 三处页面继续保留各自原有配置差异：
  - 普通播放和分享播放保留 controls、自动播放和 ESC 禁用
  - 编辑器保留 controls=false、autoSlide=false 和 F 键占位
  - 编辑器继续通过 `onReady` 维护 `revealRef`，支持缩略图点击跳转 slide
- 已新增 runtime 单元测试覆盖：
  - Reveal 构造参数包含原配置和 Zoom 插件
  - 初始化后绑定 `slidechanged`
  - destroy 时解绑事件并销毁实例
  - 加载完成前 cancel 不创建 Reveal 实例
- 本批次不改变 Story 页面 Redux 数据流、页面排序和内容加载逻辑

本批次验证命令：

```bash
npm run test -- src/app/pages/StoryBoardPage/__tests__/playerRuntime.test.ts src/app/pages/StoryBoardPage/__tests__/revealRuntime.test.ts
npm run checkTs
npm run eslint -- src/app/pages/StoryBoardPage/playerRuntime.ts src/app/pages/StoryBoardPage/__tests__/playerRuntime.test.ts src/app/pages/StoryBoardPage/Player/index.tsx src/app/pages/StoryBoardPage/Editor/index.tsx src/app/pages/SharePage/StoryPlayer/StoryPlayerForShare/StoryPlayerForShare.tsx
npm exec -- prettier --check src/app/pages/StoryBoardPage/playerRuntime.ts src/app/pages/StoryBoardPage/__tests__/playerRuntime.test.ts src/app/pages/StoryBoardPage/Player/index.tsx src/app/pages/StoryBoardPage/Editor/index.tsx src/app/pages/SharePage/StoryPlayer/StoryPlayerForShare/StoryPlayerForShare.tsx ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

最新批次：VirtualTable 异步 runtime 渲染边界补强

- 已修复 `VirtualTable` 的 `renderVirtualList` memo 依赖，补入异步加载后的 `Grid`
- 该修复确保 `react-window` runtime 加载完成后，body renderer 能从 placeholder 切换到真实虚拟表格 Grid，不继续使用旧闭包
- 已新增组件测试覆盖 runtime resolve 后 mock Grid 渲染单元格内容
- 相关测试仍覆盖 runtime 加载失败时保持 placeholder、以及列宽不向 Grid 传入负数
- 本批次不升级 `react-window`，不改变 `VirtualTable` 对外 props 协议

本批次验证命令：

```bash
npm run test -- src/app/components/__tests__/VirtualTable.test.tsx src/app/components/__tests__/virtualTableRuntime.test.ts
npm run checkTs
npm run eslint -- src/app/components/VirtualTable.tsx src/app/components/__tests__/VirtualTable.test.tsx
npm exec -- prettier --check src/app/components/VirtualTable.tsx src/app/components/__tests__/VirtualTable.test.tsx ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

最新批次：Split 异步 runtime smoke 补强

- 已补强 `Split` 组件级 smoke，覆盖 `split.js` runtime 加载成功后创建实例并只传入非 gutter 子节点
- 已覆盖自定义 gutter 会被标记为内部 gutter，避免后续重建时误作为普通 pane 传回 `split.js`
- 已覆盖初次挂载和更新重建两条异步加载路径：组件在 runtime resolve 前卸载时，不再创建 split 实例
- 现有失败路径测试继续覆盖 runtime 加载失败时页面内容保持挂载
- 本批次不升级 `split.js`，不改变 `Split` 对外 props 协议

本批次验证命令：

```bash
npm run test -- src/app/components/__tests__/Split.test.tsx src/app/components/__tests__/splitRuntime.test.ts
npm run checkTs
npm run eslint -- src/app/components/Split.tsx src/app/components/__tests__/Split.test.tsx
npm exec -- prettier --check src/app/components/Split.tsx src/app/components/__tests__/Split.test.tsx ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

最新批次：DnD Provider runtime smoke 补强

- 已新增 `DndProviderCompat` 组件级 smoke，覆盖真实 `react-dnd-html5-backend` 下 Provider 可以正常渲染子节点
- 已补 `DragSortEditTable` 挂载 smoke，确认基于 `DndProviderCompat`、`useDrag`、`useDrop` 的表格行拖拽容器在 React 19 测试环境下可挂载
- 继续保留 `dndRuntime` 包导出测试，覆盖 `react-dnd`、`react-dnd-html5-backend`、`@hello-pangea/dnd` 的运行时导出
- 本批次不升级拖拽库，不改变 Dashboard / Story / ChartWorkbench 的拖拽行为

本批次验证命令：

```bash
npm run test -- src/app/components/__tests__/DndProviderCompat.test.tsx src/app/components/__tests__/dndRuntime.test.ts
npm run checkTs
npm run eslint -- src/app/components/DndProviderCompat.tsx src/app/components/DragSortEditTable.tsx src/app/components/__tests__/DndProviderCompat.test.tsx
npm exec -- prettier --check src/app/components/DndProviderCompat.tsx src/app/components/DragSortEditTable.tsx src/app/components/__tests__/DndProviderCompat.test.tsx ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

最新批次：SQL formatter 调用边界收口

- 已新增 `formatSqlScript` helper，集中维护 SQL 编辑器格式化调用和 `sql-formatter` 选项
- `Toolbar` 的格式化按钮继续保持原有异步加载和失败 console 行为，只改为调用共享 helper
- 已新增单元测试覆盖：
  - 格式化时固定透传 `denseOperators=true`、`logicalOperatorNewline='before'`
  - formatter runtime 加载失败时错误向调用方传播，由 Toolbar 保持原 catch 行为
- 本批次不升级 `sql-formatter`，不改变 SQL 编辑器对外交互

本批次验证命令：

```bash
npm run test -- src/app/pages/MainPage/pages/ViewPage/Main/Editor/__tests__/formatSqlScript.test.ts src/app/pages/MainPage/pages/ViewPage/Main/Editor/__tests__/sqlFormatterRuntime.test.ts
npm run checkTs
npm run eslint -- src/app/pages/MainPage/pages/ViewPage/Main/Editor/formatSqlScript.ts src/app/pages/MainPage/pages/ViewPage/Main/Editor/__tests__/formatSqlScript.test.ts src/app/pages/MainPage/pages/ViewPage/Main/Editor/Toolbar.tsx
npm exec -- prettier --check src/app/pages/MainPage/pages/ViewPage/Main/Editor/formatSqlScript.ts src/app/pages/MainPage/pages/ViewPage/Main/Editor/__tests__/formatSqlScript.test.ts src/app/pages/MainPage/pages/ViewPage/Main/Editor/Toolbar.tsx ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

最新批次：基础及派生 ECharts 图表生命周期 smoke 补强

- 已新增基础及派生 ECharts 图表生命周期 smoke，覆盖折线、面积、柱状、堆叠柱、百分比堆叠、饼图、环图、南丁格尔玫瑰图等 15 条类组件链路
- 测试验证异步 `loadEChartsRuntime` resolve 后会 replay 最新 render，并调用 `setOption`
- 测试验证组件在 runtime resolve 前 `onUnMount` 后，旧 promise 不再初始化图表，也不会调用 `setOption`
- 测试保留真实 chart helper 数据转换路径，仅 mock ECharts 实例最小接口
- 本批次不升级 `echarts`，不改变图表配置协议和渲染行为

本批次验证命令：

```bash
npm run test -- src/app/components/ChartGraph/__tests__/basicEChartsLifecycle.test.ts
npm run checkTs
npm run eslint -- src/app/components/ChartGraph/__tests__/basicEChartsLifecycle.test.ts
npm exec -- prettier --check src/app/components/ChartGraph/__tests__/basicEChartsLifecycle.test.ts ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

最新批次：分享页图表预览入口 smoke 补强

- 已新增 `ChartPreviewBoardForShare` 组件级 smoke，覆盖分享页图表预览入口的只读渲染边界
- 测试验证未绑定 view 的分享图表优先使用 `sampleData`，绑定 view 的分享图表使用已拉取 `dataset`
- 测试验证 `ChartIFrameContainer` 能收到分享页的 `containerId`、尺寸、loading 和 dataset props
- 测试验证 chart click 选中事件会通过分享页入口 dispatch selectedItems 更新
- 本批次不改变分享页交互、不改数据请求协议，只补运行时入口回归证据

本批次验证命令：

```bash
npm run test -- src/app/pages/SharePage/Chart/__tests__/ChartPreviewBoardForShare.smoke.test.tsx
npm run checkTs
npm run eslint -- src/app/pages/SharePage/Chart/__tests__/ChartPreviewBoardForShare.smoke.test.tsx
npm exec -- prettier --check src/app/pages/SharePage/Chart/__tests__/ChartPreviewBoardForShare.smoke.test.tsx ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

最新批次：富文本编辑态字段插入 smoke 补强

- 已新增 `ChartRichTextAdapter` 编辑态组件 smoke，覆盖 Quill 2 迁移后的引用字段插入交互边界
- 测试通过 mock `RichTextEditor` runtime handle，验证字段菜单选择后会调用 `insertCalcFieldItem`
- 测试验证编辑态变更会归一化为 calcfield delta，并经 debounce 后透传 `onChange`
- 该测试与现有只读展示 smoke 互补：只读链路验证 calcfield 展示，编辑链路验证 calcfield 写入和回传
- 本批次不升级 `quill` / `react-quill-new`，不改变富文本内容协议

本批次验证命令：

```bash
npm run test -- src/app/components/ChartGraph/BasicRichText/__tests__/ChartRichTextAdapter.edit.test.tsx
npm run checkTs
npm run eslint -- src/app/components/ChartGraph/BasicRichText/__tests__/ChartRichTextAdapter.edit.test.tsx
npm exec -- prettier --check src/app/components/ChartGraph/BasicRichText/__tests__/ChartRichTextAdapter.edit.test.tsx ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

最新批次：构建体积报告脚本测试稳定性收口

- 已将 `report-build-chunks` 的文件收集、排序、阈值判断和输出行生成拆到 `report-build-chunks-core.mjs`
- CLI 入口 `report-build-chunks.mjs` 继续保留原命令和输出格式，只负责读取环境变量、打印报告和设置退出码
- 已将报告脚本测试改为临时 build fixture，不再依赖当前 `frontend/build` 的真实文件数量和 hash
- 新测试覆盖：
  - JS chunk / task bundle / media asset 收集与排序
  - raw / gzip 阈值独立判断
  - `FAIL_ON_OVERSIZED` CLI 退出码
  - 缺少 `build/static/js` 时的明确构建前置错误
- 本批次不改变 Vite 分包配置和构建产物，只降低后续构建治理测试的脆弱性

本批次验证命令：

```bash
npm run test -- scripts/__tests__/report-build-chunks.test.mts
npm run build:report
npm run checkTs
npm run eslint -- scripts/report-build-chunks.mjs scripts/report-build-chunks-core.mjs scripts/__tests__/report-build-chunks.test.mts
npm exec -- prettier --check scripts/report-build-chunks.mjs scripts/report-build-chunks-core.mjs scripts/__tests__/report-build-chunks.test.mts ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

最新批次：构建体积报告参数校验补强

- 已为 `report-build-chunks` 增加环境变量数值参数校验，避免阈值或 limit 传错时静默生成 `NaN` 报告
- 校验范围覆盖：
  - `YU_BI_CHUNK_REPORT_THRESHOLD_KIB`
  - `YU_BI_CHUNK_REPORT_GZIP_THRESHOLD_KIB`
  - `YU_BI_CHUNK_REPORT_LIMIT`
- 非数字、`0`、负数会直接抛出明确错误，提示变量名和当前值
- 正常默认参数和现有 `build:report` 输出保持不变

本批次验证命令：

```bash
npm run test -- scripts/__tests__/report-build-chunks.test.mts
npm run build:report
npm run checkTs
npm run eslint -- scripts/report-build-chunks-core.mjs scripts/__tests__/report-build-chunks.test.mts
npm exec -- prettier --check scripts/report-build-chunks-core.mjs scripts/__tests__/report-build-chunks.test.mts ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

最新批次：Dashboard mock 数据 Monaco 入口 smoke 补强

- 已新增 `MockDataEditor` 组件级 smoke，覆盖 Dashboard mock 数据编辑器的 Monaco 调用边界
- 测试验证组件会按当前主题传入 `vs-dark`/`vs-light` Monaco theme，并保留 javascript language、自动布局和可编辑选项
- 测试验证挂载前会调用 `ensureMonacoJavascriptLanguage`
- 测试验证编辑器挂载后会执行 format document、重置当前值并 focus
- 测试验证合法 JSON 数组经 debounce 后回传 `onDataChange`，对象或非法 JSON 不会误写入 mock 数据
- 本批次不升级 `monaco-editor`，不改变 mock 数据协议

本批次验证命令：

```bash
npm run test -- src/app/pages/DashBoardPage/components/MockDataPanel/__tests__/MockDataEditor.smoke.test.tsx
npm run checkTs
npm run eslint -- src/app/pages/DashBoardPage/components/MockDataPanel/__tests__/MockDataEditor.smoke.test.tsx
npm exec -- prettier --check src/app/pages/DashBoardPage/components/MockDataPanel/__tests__/MockDataEditor.smoke.test.tsx ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

最新批次：SQL 编辑器与计算字段 Monaco 入口 smoke 补强

- 已新增 `SQLEditor` 入口级 smoke，覆盖 SQL Monaco 配置、`ensureMonacoSqlLanguage` 调用、补全 provider 注册、只读提示、快捷键命令注册和卸载清理
- 已新增 `ChartComputedFieldEditor` 入口级 smoke，覆盖 `dql` 语言注册、自定义主题注册、内置函数注入、字段插入、debounce 后表达式回传和函数说明展示
- 本批次不升级 `monaco-editor`，不改变 SQL 编辑器、计算字段编辑器和 DQL 语法协议
- 目的仍是为后续 Monaco / TypeScript / React 运行时升级补齐业务入口回归基线

本批次验证命令：

```bash
npm run test -- src/app/pages/MainPage/pages/ViewPage/Main/Editor/__tests__/SQLEditor.monaco.smoke.test.tsx src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartDataViewPanel/components/ChartComputedFieldEditor/__tests__/ChartComputedFieldEditor.monaco.smoke.test.tsx
npm run checkTs
npm run eslint -- src/app/pages/MainPage/pages/ViewPage/Main/Editor/__tests__/SQLEditor.monaco.smoke.test.tsx src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartDataViewPanel/components/ChartComputedFieldEditor/__tests__/ChartComputedFieldEditor.monaco.smoke.test.tsx
npm exec -- prettier --check src/app/pages/MainPage/pages/ViewPage/Main/Editor/__tests__/SQLEditor.monaco.smoke.test.tsx src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartDataViewPanel/components/ChartComputedFieldEditor/__tests__/ChartComputedFieldEditor.monaco.smoke.test.tsx ../docs/tech-stack-modernization-plan.md
git diff --check
```

最新批次：分享 Story 播放器入口 smoke 补强

- 已新增 `StoryPlayerForShare` 组件级 smoke，覆盖分享故事页按页码排序渲染、Reveal runtime 初始化、自动播放间隔换算、初始页面选中和卸载清理
- 测试验证带 `subVizTokenMap` 的分享页会通过 `getPageContentDetail` 加载首屏 Dashboard 内容，并传递 `shareToken` 与对应子 viz token
- 测试验证 Reveal `slidechanged` 回调会切换选中页面，并加载新页面对应的 Dashboard 内容
- 本批次不升级 `reveal.js`，不改变故事页分享协议和 Dashboard 子页面加载协议

本批次验证命令：

```bash
npm run test -- src/app/pages/SharePage/StoryPlayer/StoryPlayerForShare/__tests__/StoryPlayerForShare.smoke.test.tsx
npm run checkTs
npm run eslint -- src/app/pages/SharePage/StoryPlayer/StoryPlayerForShare/__tests__/StoryPlayerForShare.smoke.test.tsx
npm exec -- prettier --check src/app/pages/SharePage/StoryPlayer/StoryPlayerForShare/__tests__/StoryPlayerForShare.smoke.test.tsx ../docs/tech-stack-modernization-plan.md
npm audit --json
git diff --check
```

## 12. 后续队列

当前队列按“继续在当前专题分支累计”的方式推进。状态为“评估”的事项可以先补测试和调查结论；状态为“可推进”的事项可以直接进入实现和相关门禁。不要因为队列中的单项完成就新建分支或合入 `main`。

### 12.1 当前 npm outdated 复核

复核时间：2026-06-24

当前 `npm outdated --json` 剩余 7 项，`npm audit --json` 仍为 0 vulnerabilities。处理原则是：能在 Node 24 / npm 11 / React 19 / Vite 8 / AntD 5 主链内安全升级的继续推进；会破坏 audit 清零、peer 合约或当前稳定生态的暂缓。

| 依赖                   | 当前版本  | 最新版本 | 当前决策 | 依据                                                                                                          |
| ---------------------- | --------- | -------- | -------- | ------------------------------------------------------------------------------------------------------------- |
| `@types/node`          | `24.13.2` | `26.0.0` | 暂缓     | 硬性目标是 Node 24，不切到 Node 26 类型线；24.x 当前已是最新                                                  |
| `@vitejs/plugin-react` | `5.2.0`   | `6.0.3`  | 暂缓     | 5.2.0 已支持 Vite 8；6.x 仍需继续复核 optional Babel / Rolldown peer 链，避免 npm 11 安装解析风险             |
| `antd`                 | `5.29.3`  | `6.4.5`  | 暂缓     | 当前稳定 `@ant-design/pro-components 2.8.10` peer 仍只支持 AntD 4/5；不采用 Pro Components 3.x 预发布链路     |
| `eslint`               | `9.39.4`  | `10.5.0` | 暂缓     | ESLint 10 本体满足 Node 24，但 `eslint-plugin-react`、`eslint-plugin-import` 最新稳定版尚未声明支持 ESLint 10 |
| `monaco-editor`        | `0.52.2`  | `0.55.1` | 暂缓     | 0.55.1 依赖 `dompurify 3.2.7`，此前临时试装会引入 npm audit 风险                                              |
| `quill`                | `2.0.2`   | `2.0.3`  | 暂缓     | 与 `react-quill-new 3.8.3` 组合会触发 `GHSA-v3m3-f69x-jf25` 低危 XSS advisory                                 |
| `react-quill-new`      | `3.7.0`   | `3.8.3`  | 暂缓     | 3.8.3 依赖 `quill ~2.0.3`，会破坏当前 audit 清零状态                                                          |

下一步不要重复做无结果试装。只有出现以下变化时再重新评估对应依赖：

- `@vitejs/plugin-react` 6.x peer 链可在 npm 11 下干净安装
- Pro Components 稳定版明确支持 AntD 6
- `eslint-plugin-react` 和 `eslint-plugin-import` 稳定版明确支持 ESLint 10
- Monaco 最新版本不再引入当前 audit 风险
- Quill / react-quill-new 最新组合不再触发 XSS advisory

### 12.2 当前可推进事项

| 阶段      | 事项                           | 风险 | 执行策略                                                                                                                  |
| --------- | ------------------------------ | ---- | ------------------------------------------------------------------------------------------------------------------------- |
| P2-E-Next | 前端剩余 outdated 复核         | 中   | 暂停重复试装；按 12.1 的触发条件定期复核，当前不为这 7 项强行升级                                                         |
| P2-E-Next | 前端运行时 smoke 补强          | 中   | 可推进；优先补 Monaco、Quill、ECharts、分享页等动态运行时入口的最小回归                                                   |
| P2-E-Next | 前端构建产物治理               | 中   | 可推进；JS gzip 超限已清零，ECharts 按需拆分试验暂不保留，后续优先分析 raw 维度的 `monacoEditor`、`antdDesign` 和地图资源 |
| P2-F      | AntD 6 主版本评估              | 高   | 暂缓；继续等待 Pro Components 稳定版支持 AntD 6，不采用预发布 3.x 链路                                                    |
| P2-G      | ECharts 6 主版本评估           | 高   | 已完成；后续可增强浏览器层图表 smoke                                                                                      |
| P2-H      | ESLint 10 主版本评估           | 中高 | 暂缓；当前被 `eslint-plugin-react` / `eslint-plugin-import` 最新稳定 peer 阻塞，等待生态支持后再升级                      |
| P2-I      | 数据源 provider / 方言依赖审计 | 高   | 评估；先盘点依赖树和驱动兼容，不做大规模重构                                                                              |
| P2-J      | 富文本编辑器运行时 smoke       | 中   | 可推进；已补 jsdom 层运行时和 Dashboard Widget smoke，后续补分享页展示和浏览器入口                                        |
| P2-K      | 后端依赖补丁线滚动收口         | 中   | 可推进；继续优先处理补丁级、同生态线、可被现有测试覆盖的升级；主版本跳跃单独评估                                          |

## 13. 门禁策略

开发期按风险分层验证，不为每个小改动跑完整门禁。提交前做本批次相关门禁；当前专题分支可阶段性 push 保存进度；准备合入 `main` 或推送 `main` 前做完整门禁。

| 场景                           | 最低门禁                                                         |
| ------------------------------ | ---------------------------------------------------------------- |
| 文档或纯元数据                 | `git diff --check`                                               |
| 前端类型边界、小范围组件迁移   | `npm run checkTs` + 相关测试                                     |
| helper、模型、共享协议变化     | `npm run checkTs` + 相关模型 / helper 测试                       |
| 依赖、构建配置、运行时加载变化 | `npm run checkTs` + 相关运行时测试；专题收尾补 `npm run test:ci` |
| Maven、Docker、安装包链路变化  | `mvn package -DskipTests`，必要时补 demo smoke                   |
| 准备 merge 回 `main`           | 前端完整门禁，必要时补后端门禁                                   |
| 推送 `main`                    | 不跳过完整门禁                                                   |

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

| 类型             | 粒度                                       |
| ---------------- | ------------------------------------------ |
| 低风险类型边界   | 累计 3 到 10 个相关文件后提交              |
| 中风险运行时链路 | 每条可验证链路独立提交                     |
| 依赖和构建链路   | 独立提交，但尽量包含完整链路文档和验证记录 |
| 阶段复盘         | 跟随当前批次提交，必要时可单独文档提交     |

不要因为单个小文件改动立刻提交。当前 P2-E 应围绕前端安全依赖治理累计依赖升级、lockfile、门禁和文档记录后再提交。

当前执行偏好：

- 优先在 `codex/modernization-frontend-security-deps` 上持续完成前端现代化改造
- 不因阶段性 push 或单个批次完成而切新分支
- 不因阶段性 push 或单个批次完成而 merge `main`
- 当 `main` 落后较多时，也先继续完成当前批次；只有用户明确要求合并时再执行主线合并门禁
- 若只有文档复盘或小范围流程修正，默认先累计到下一批实际改造提交中，不为单个小改动单独提交

## 15. 恢复命令

继续 P2-E：

```bash
git status --short --branch
git rev-list --left-right --count origin/main...HEAD
git log --oneline --decorate -8
npm audit --json
npm outdated --json
npm ls react-quill react-quill-new quill quill-delta --all
npm ci --dry-run --ignore-scripts --no-audit --no-fund
npm run checkTs
```

追溯历史：

```bash
git log --oneline -- docs/tech-stack-modernization-plan.md
git log --oneline -- Dockerfile Deployment.md server/pom.xml bin/yu-bi-server.sh bin/yu-bi-server.cmd
git log --oneline -- security/src/main/java/datart/security/manager/shiro security/src/test/java/datart/security
git log --oneline -- frontend/package.json frontend/package-lock.json
```
