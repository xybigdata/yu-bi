# yu-bi 现代化改造执行板

本文档是现代化改造的恢复入口和阶段看板。恢复工作时先看本页，再按“当前短期目标”和“下一步队列”执行；历史细节通过 git 追溯。

复盘时间：2026-06-20

## 1. 目标边界

长期目标：在保证兼容、正确、可回归的前提下，把 yu-bi 前后端核心技术栈和关键运行链路收口到较新的稳定状态。现代化不等于盲目追最新版，优先选择兼容性明确、生态稳定、可验证的版本。

硬性兼容：

- 后端必须兼容 `JDK 21`
- 前端必须兼容 `Node 24`

固定禁止项：

- 不贸然改 Java 包名 `datart.*`
- 不贸然改配置前缀 `datart.*`
- 不贸然改 `DATART_*` 等内部技术符号
- 不贸然改数据迁移相关稳定常量、后缀和内部标识

执行原则：

- 不直接在 `main` 开发；按专题创建 `codex/*` 分支，专题完成后 push，再 `--no-ff` merge 回 `main`
- 减少分支和主线合并频率；同一专题内持续累计相关改造，阶段性提交
- 不为每个小改动执行完整门禁；按风险分层验证，合并和推送 `main` 前再跑完整门禁
- 中高风险项可以推进，但必须拆成可验证子链路，先补测试或 smoke test，再升级或替换
- 阶段性提交前同步本文档，记录目标、进度、风险和验证结果

## 2. 当前快照

恢复工作时先执行：

```bash
git status --short --branch
git rev-list --left-right --count origin/main...HEAD
git log --oneline --decorate -8
```

当前已知状态：

| 项目 | 状态 |
| --- | --- |
| 工作目录 | `/Users/chencongyu/WorkHome/VSProjects/open-project/yu-bi` |
| 主线分支 | `main` |
| 远端 | `git@github.com:xybigdata/yu-bi.git` |
| 当前专题分支 | `codex/modernization-frontend-runtime-next` |
| 当前阶段 | 前端布局运行时现代化专题进行中 |
| 本地 `main` | 已与 `origin/main` 对齐 |
| 最近主线提交 | `a208481b0 docs: 复盘现代化改造阶段进度` |
| 当前专题进展 | 布局运行时依赖已升级并通过分层验证，待继续累计同专题改造 |

注意：

- `.tmp/`、`logs/` 已加入 `.gitignore`
- 允许自动执行 `git add`、`git commit --no-verify`、`git push origin <branch>`
- 推送 `main` 前必须走完整门禁

## 3. 技术栈基线

### 3.1 后端

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
| Shiro | `2.0.5` | 高风险，先审计认证授权边界 |
| Druid | `1.2.28` | 暂不优先动 |
| Calcite | 现网主链 | 高风险，先补 SQL 解析兼容验证 |

### 3.2 前端

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
| Vitest | `4.1.8` | 当前主测试栈 |
| styled-components | `6.1.19` | 已完成主升级 |
| react-quill | `2.0.0` | 已升级，保留兼容层 |
| monaco-editor | `0.52.2` | 已补真实运行时加载边界 |
| reveal.js | `6.0.1` | 已补真实运行时加载边界 |
| ECharts | `5.6.0` | 已升级到 ECharts 5 稳定线 |
| AntV S2 | `2.7.2 / 2.3.1` | 已确认是当前稳定线 |
| react-window | `1.8.11` | 已升级到 1.x 兼容补丁线 |
| react-grid-layout | `2.2.3` | 已通过 legacy 兼容入口升级 |
| flexlayout-react | `0.9.1` | 已升级并改用命名导出 |
| react-draggable | `4.7.0` | 已随布局链路同步升级 |
| react-resizable | `3.0.5` | 已随布局链路同步升级 |
| @hello-pangea/dnd | `18.0.1` | 已确认是当前稳定线 |
| react-dnd | `16.0.1` | 已确认是当前稳定线 |
| react-dnd-html5-backend | `16.0.1` | 已确认是当前稳定线 |

## 4. 阶段复盘

### 4.1 已完成主链

- 项目已从 datart 独立为 `yu-bi`，GitHub 仓库、默认分支和远端已切换完成
- README、README_zh、NOTICE、SECURITY、ROADMAP、CHANGELOG、MAINTAINERS、issue template 已收口为独立开源项目表述
- 后端已建立 `JDK 21 + Spring Boot 3.5.x + Spring Cloud 2025.0.x` 主链
- 前端已建立 `Node 24 + React 18 + Ant Design 5 + Vite 6 + Vitest 4` 主链
- CRA / CRACO、IE11 主兼容链、Nashorn、PhantomJS 等历史主链已退出
- 安装包闭环验证已打通，`yu-bi-server-*.zip` 可解压并通过 demo 健康检查脚本验证

### 4.2 已完成批次

| 批次 | 合并点 | 结果 |
| --- | --- | --- |
| Dashboard widget 内容协议边界 | `b519a24cd` | 已合入 |
| 图表运行时类型边界 | `484c44fd9` | 已合入 |
| 现代化兼容边界 | `c67e2d2c7` | 已合入并推送 `origin/main` |
| 图表运行时现代化 | `e00ca3d87` | 已合入并推送 `origin/main` |

### 4.3 图表运行时专题结果

- 已联网确认 `echarts` 最新主线为 `6.1.0`
- 已确认 `echarts-wordcloud@2.1.0` peer 约束为 `echarts ^5.0.1`
- 因词云扩展仍约束 ECharts 5，本专题不升级到 ECharts 6
- 已将 `echarts` 从 `5.3.1` 升级到 `5.6.0`
- 已补充 ECharts 主运行时和词云运行时真实动态导入 smoke test
- 已确认 `@antv/s2@2.7.2`、`@antv/s2-react@2.3.1` 是当前稳定线，本专题不做无意义版本变更
- 已补充 `PivotSheetChart` 真实 `AntVS2Wrapper` 动态导入 smoke test
- `@antv/s2` 测试会输出 source map 指向缺失源文件 warning，当前不影响测试结果

已通过的专题验证：

```bash
npm run checkTs
npm run test:ci -- src/app/components/ChartGraph
npm install --package-lock-only --dry-run --ignore-scripts
npm ci --dry-run --ignore-scripts
```

合入本地 `main` 前完整门禁曾通过：

```bash
npm run checkTs
npm run test:ci
npm run lint:css
npm run lint:style
```

合入后发现 `PivotSheetChart` 真实运行时加载测试在完整 `test:ci` 中偶发超过默认 5s，已放宽到 15s。该收尾修复已随 `a208481b0` 推送到 `origin/main`。

## 5. 当前短期目标

P0：继续推进前端布局运行时专题。

专题分支：

```bash
codex/modernization-frontend-runtime-next
```

已完成：

- `react-grid-layout` 从 `^1.3.4` 升级到 `^2.2.3`
- 使用 `react-grid-layout/legacy` 保持 v1 平铺 props 兼容，不切换到 2.x hooks API
- 将旧 `Layout` 单项类型迁移为 `LayoutItem`，避免和 2.x readonly layout 数组语义冲突
- `flexlayout-react` 从 `^0.5.21` 升级到 `^0.9.1`
- FlexLayout 入口改为命名导出，布局配置中的旧 `width` 字段迁移为 `weight`
- `react-window` 从 `^1.8.6` 升级到 `^1.8.11`
- 暂不升级到 `react-window` 2.x；2.x API 已转向 `Grid/List`，当前项目依赖 `VariableSizeGrid`，需要独立迁移专题
- `react-draggable` 升级到 `^4.7.0`
- `react-resizable` 升级到 `^3.0.5`
- 补充 `react-window`、`react-grid-layout/legacy`、`flexlayout-react` 真实运行时导入 smoke test
- 已确认 `@hello-pangea/dnd@18.0.1`、`react-dnd@16.0.1`、`react-dnd-html5-backend@16.0.1` 是当前稳定线，本专题不做无意义版本变更
- 补充 `@hello-pangea/dnd`、`react-dnd`、`react-dnd-html5-backend` 真实运行时导入 smoke test
- 已移除过时的 `@types/react-grid-layout`；`react-grid-layout@2.2.3` 已自带类型，npm 也将该 `@types` 包标记为 stub/deprecated
- 已复扫前端依赖深路径入口；当前仅保留 `react-grid-layout/css/styles.css` 这类公开样式入口

已执行验证：

```bash
npm run checkTs
npm run test:ci -- src/app/components/__tests__/dndRuntime.test.ts src/app/components/__tests__/virtualTableRuntime.test.ts src/app/components/__tests__/VirtualTable.test.tsx src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/__tests__/layoutRuntime.test.ts src/app/pages/DashBoardPage/hooks/__tests__/useGridLayoutMap.test.ts src/app/components/ChartGraph/BasicTableChart/__tests__/BasicTableChart.test.jsx
npm install --package-lock-only --dry-run --ignore-scripts
npm ci --dry-run --ignore-scripts
rg --pcre2 -n "@types/react-grid-layout|ReactGridLayout\\.|react-grid-layout/(?!legacy)|from ['\\\"](?:antd/es|antd/lib|rc-[^'\\\"]+/(?:es|lib)|@ant-design/[^'\\\"]+/(?:es|lib)|react-grid-layout/(?!legacy)|flexlayout-react/|react-window/|react-dnd/|@hello-pangea/dnd/)" frontend/package.json frontend/src -g '*.json' -g '*.ts' -g '*.tsx' -g '*.js' -g '*.jsx'
```

下一步：

- 继续在同一分支累计前端运行时相关改造，不急于合并 `main`
- 继续评估其它前端运行时依赖是否已有低中风险稳定补丁可升级
- 继续推进 Dashboard widget 内容协议收口，先从 helper 和类型边界切入
- 若继续改动依赖，仍按“先版本审计，再补 smoke test，再分层验证”执行

## 6. 下一步队列

| 优先级 | 事项 | 风险 | 执行策略 |
| --- | --- | --- | --- |
| P1-A | 前端布局运行时审计：`react-window`、`react-grid-layout`、`flexlayout-react` | 中 | 已完成第一批升级；`react-window` 2.x 独立迁移专题暂缓 |
| P1-B | 拖拽运行时审计：`@hello-pangea/dnd`、`react-dnd`、`react-dnd-html5-backend` | 中 | 已确认当前稳定线并补真实运行时 smoke test |
| P1-C | Dashboard widget 内容协议继续收口 | 中 | 先收敛 helper 和类型边界，不做大规模数据结构替换 |
| P1-D | 前端公开类型入口和深路径 import 复扫 | 低 | 已移除 `@types/react-grid-layout`，仅保留公开样式入口 |
| P1-E | Node 24 / npm 11 安装健康度复核 | 低 | 保持 `npm ci --dry-run --ignore-scripts` 和 lockfile 可解析 |
| P2-A | Maven、Docker、安装包链路复核 | 中 | 改构建链路前补 `mvn package -DskipTests` 和 demo smoke |
| P2-B | Shiro 认证授权健康度审计 | 高 | 只做边界用例和小修，不整体替换 |
| P2-C | Calcite SQL 解析健康度审计 | 高 | 先补 SQL 解析兼容样例，不整体替换 |

## 7. 风险分层

可直接推进：

- 文档、`.gitignore`、CI 说明、展示元数据
- 前端公开类型入口清理
- 时间工具、筛选值、数据集值协议边界清理
- Node 24 / npm 11 安装健康度复核

需要先补验证：

- 依赖升级、构建配置、运行时加载
- `ChartDataSetDTO.rows` 全局协议切换
- 布局运行时、图表运行时、富文本、编辑器、故事板运行时升级
- Dashboard widget 内容协议继续收口
- Maven 打包、Docker、安装包和 demo 启动链路调整

暂不做整体重构：

- Java 包名 `datart.*`
- 配置前缀 `datart.*`
- `DATART_*`
- 数据迁移稳定标识
- Shiro 认证授权整体替换
- Calcite SQL 解析主链整体替换
- 数据源 provider / 方言 / 脚本运行时的大规模重构

## 8. 门禁策略

开发期按风险分层验证，不为每个小改动跑完整门禁：

| 场景 | 最低门禁 |
| --- | --- |
| 文档或纯元数据 | `git diff --check` |
| 前端类型边界、小范围组件迁移 | `npm run checkTs` + 相关测试 |
| helper、模型、共享协议变化 | `npm run checkTs` + 相关模型 / helper 测试 |
| 依赖、构建配置、运行时加载变化 | `npm run checkTs` + 相关运行时测试；专题收尾补 `npm run test:ci` |
| Maven、Docker、安装包链路变化 | `mvn test` 或 `mvn package -DskipTests`，必要时补 demo smoke |
| 准备 merge 回 `main` | 前端完整门禁，必要时补后端门禁 |
| 推送 `main` | 不跳过完整门禁 |

完整前端门禁：

```bash
npm run checkTs
npm run test:ci
npm run lint:css
npm run lint:style
```

测试缺口处理：

- 找不到现成相关测试时，必须记录缺口
- 优先补 helper、协议、运行时最小用例
- 不为了覆盖率硬造低价值快照测试

## 9. 提交节奏

- 同一专题内累计一组相关改动后再提交
- 不因单个小文件改动立即提交
- 文档复盘跟随当前批次提交，除非只是纯计划更新
- 中风险运行时链路按可验证子链路独立提交
- 依赖、构建、安装包链路独立提交，并跑更强验证
- 合并 `main` 和推送 `main` 尽量少做，但每次都必须可回归

建议提交粒度：

| 类型 | 粒度 |
| --- | --- |
| 低风险类型边界 | 累计 3 到 10 个相关文件后提交 |
| 中风险运行时链路 | 每条可验证链路独立提交 |
| 依赖和构建链路 | 独立提交 |
| 阶段复盘 | 跟随当前批次提交或单独文档提交 |

## 10. 历史追溯

需要追溯具体实现时使用：

```bash
git log --oneline -- docs/tech-stack-modernization-plan.md
git log --oneline -- frontend/src/app/components/FormGenerator
git log --oneline -- frontend/src/app/components/ChartGraph
git log --oneline -- frontend/src/app/pages/DashBoardPage
git log --oneline -- frontend/src/app/pages/MainPage/pages/VizPage
```
