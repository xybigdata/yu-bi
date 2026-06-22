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
| 当前专题分支 | `codex/modernization-build-package` |
| 当前专题 | P2-A Maven、Docker、安装包链路复核 |
| 当前分支相对 `origin/main` | 以恢复命令输出为准，当前专题持续领先主线 |
| 最近专题提交 | 以 `git log --oneline --decorate -8` 为准 |
| 最近主线提交 | `77217676b chore: 合入前端运行时现代化批次` |

已确认的自动化权限和偏好：

- 可以自动执行 `git add`
- 可以自动执行 `git commit --no-verify -m "..."`
- 可以自动执行 `git push origin <branch>`
- `npm view ...` 查询已授权，后续不再单独询问
- 同一专题内尽量累计一组相关改动后再提交，避免过频繁提交
- 当前专题继续在同一分支推进，不因为小批次改动立即合入 `main`

## 3. 分支与合并规则

固定规则：

- 不直接在 `main` 开发
- 按专题使用 `codex/*` 分支
- 专题分支可以推送远端
- 专题完成后再 `--no-ff` merge 回 `main`
- 推送 `main` 前必须完整门禁

当前专题分支：

```bash
codex/modernization-build-package
```

当前专题收口前不要创建新分支。P2-A 完成 Docker、部署文档、安装包 smoke 后，再统一验证、提交、推送；是否合入 `main` 取决于本批次完整门禁结果和主线节奏。

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
| React Router | `6.30.1` | 已完成主升级 |
| Ant Design | `5.26.2` | 已完成主升级 |
| Redux Toolkit | `2.12.0` | 已完成主升级 |
| React Redux | `9.3.0` | 已完成主升级 |
| TypeScript | `5.9.3` | 当前稳定主链 |
| Vite | `6.4.3` | 已替代 CRA 主工作流 |
| Vitest | `4.1.9` | 当前主测试栈 |
| styled-components | `6.1.19` | 已完成主升级 |
| react-quill | `2.0.0` | 已升级，保留兼容层 |
| monaco-editor | `0.52.2` | 已补真实运行时加载边界 |
| reveal.js | `6.0.1` | 已补真实运行时加载边界 |
| ECharts | `5.6.0` | 已升级到 ECharts 5 稳定线 |
| AntV S2 | `2.7.2 / 2.3.1` | 已确认当前稳定线 |
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

## 6. 当前短期目标：P2-A 构建与安装包链路

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

P2-A 本批次下一步：

- 提交门禁记录后按 `--no-ff` 合并回 `main`
- 后续具备 Docker 环境后补 `docker build` 和容器健康检查验证

## 7. 后续队列

| 阶段 | 事项 | 风险 | 执行策略 |
| --- | --- | --- | --- |
| P2-B | Shiro 认证授权健康度审计 | 高 | 只补边界用例和小修，不整体替换 |
| P2-C | Calcite SQL 解析健康度审计 | 高 | 先补 SQL 解析兼容样例，不整体替换 |
| P2-D | `react-window` 2.x 可行性评估 | 中高 | 独立专题，先验证 `VariableSizeGrid` 替换路径 |
| P2-E | 前端安全依赖治理 | 中高 | 单独专题处理 Dependabot 类问题，避免混入运行时改造 |
| P2-F | React 19、AntD 6、Vite 8、TypeScript 6 主版本评估 | 高 | 独立专题，先建立兼容矩阵和关键页面 smoke test |
| P2-G | 数据源 provider / 方言依赖审计 | 高 | 先盘点依赖树和驱动兼容，不做大规模重构 |

## 8. 门禁策略

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

## 9. 提交节奏

同一专题内累计一组相关改动后再提交，减少主线合并和完整回归次数。

建议粒度：

| 类型 | 粒度 |
| --- | --- |
| 低风险类型边界 | 累计 3 到 10 个相关文件后提交 |
| 中风险运行时链路 | 每条可验证链路独立提交 |
| 依赖和构建链路 | 独立提交，但尽量包含完整链路文档和验证记录 |
| 阶段复盘 | 跟随当前批次提交，必要时可单独文档提交 |

不要因为单个小文件改动立刻提交。当前 P2-A 应完成 Dockerfile、Deployment.md 和执行文档后再提交。

## 10. 恢复命令

继续 P2-A：

```bash
git status --short --branch
git rev-list --left-right --count origin/main...HEAD
sed -n '1,120p' Dockerfile
sed -n '1,140p' Deployment.md
sed -n '180,320p' server/pom.xml
```

追溯历史：

```bash
git log --oneline -- docs/tech-stack-modernization-plan.md
git log --oneline -- Dockerfile Deployment.md server/pom.xml bin/yu-bi-server.sh bin/yu-bi-server.cmd
git log --oneline -- frontend/package.json frontend/package-lock.json
```
