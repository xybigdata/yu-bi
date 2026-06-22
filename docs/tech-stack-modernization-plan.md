# yu-bi 现代化改造执行板

本文档是 yu-bi 现代化改造的恢复入口、阶段看板和执行约束。恢复工作时优先读本页，不再从历史提交或聊天记录中重新拼上下文。

复盘时间：2026-06-22

## 1. 总目标

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

## 2. 当前状态

恢复时先执行：

```bash
git status --short --branch
git rev-list --left-right --count origin/main...HEAD
git log --oneline --decorate -8
```

当前快照：

| 项目 | 状态 |
| --- | --- |
| 工作目录 | `/Users/chencongyu/WorkHome/VSProjects/open-project/yu-bi` |
| 远端 | `git@github.com:xybigdata/yu-bi.git` |
| 主线分支 | `main` |
| 当前专题分支 | `codex/modernization-frontend-runtime-next` |
| 当前专题状态 | 前端运行时现代化继续累计中 |
| 当前分支相对 `origin/main` | 以恢复命令输出为准，当前专题持续领先主线 |
| 最近主线提交 | `a208481b0 docs: 复盘现代化改造阶段进度` |
| 最近专题提交 | 以 `git log --oneline --decorate -8` 为准 |

已确认的自动化权限和偏好：

- 可以自动执行 `git add`
- 可以自动执行 `git commit --no-verify -m "..."`
- 可以自动执行 `git push origin <branch>`
- `npm view ...` 已授权，后续不再单独询问
- 同一专题内尽量累计一组相关改动后再提交，避免过频繁提交
- 当前专题继续在同一分支推进，暂不因为小批次改动立即合入 `main`

## 3. 分支与合并规则

固定规则：

- 不直接在 `main` 开发
- 按专题使用 `codex/*` 分支
- 专题分支可以推送远端
- 专题完成后再 `--no-ff` merge 回 `main`
- 推送 `main` 前必须完整门禁

当前专题分支：

```bash
codex/modernization-frontend-runtime-next
```

当前专题收尾前不要创建新分支。只有当前专题达到一个清晰收口点，并完成必要验证后，才考虑合并 `main` 并开启下一个专题。

## 4. 技术栈基线

### 4.1 后端

| 技术栈 | 当前基线 | 处理状态 |
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

| 技术栈 | 当前基线 | 处理状态 |
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
| AntV S2 | `2.7.2 / 2.3.1` | 已确认当前稳定线 |
| react-window | `1.8.11` | 已升级到 1.x 兼容补丁线 |
| react-grid-layout | `2.2.3` | 已通过 legacy 入口升级 |
| flexlayout-react | `0.9.1` | 已升级并改用命名导出 |
| react-draggable | `4.7.0` | 已升级 |
| react-resizable | `3.2.0` | 已升级到 3.x 稳定补丁线 |
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
- 安装包闭环验证已打通，`yu-bi-server-*.zip` 可解压并通过 demo 健康检查脚本验证
- `.tmp/`、`logs/` 已加入 `.gitignore`

### 5.2 已合入批次

| 批次 | 合并点 | 结果 |
| --- | --- | --- |
| Dashboard widget 内容协议边界 | `b519a24cd` | 已合入 |
| 图表运行时类型边界 | `484c44fd9` | 已合入 |
| 现代化兼容边界 | `c67e2d2c7` | 已合入并推送 `origin/main` |
| 图表运行时现代化 | `e00ca3d87` | 已合入并推送 `origin/main` |
| 阶段复盘文档 | `a208481b0` | 已推送 `origin/main` |

### 5.3 当前专题已完成内容

分支：`codex/modernization-frontend-runtime-next`

已完成：

- `react-grid-layout` 从 `^1.3.4` 升级到 `^2.2.3`
- 使用 `react-grid-layout/legacy` 保持 v1 平铺 props 兼容，不切换到 2.x hooks API
- 将旧单项 `Layout` 类型迁移为 `LayoutItem`
- 为 `react-grid-layout/legacy` 补 ambient declaration，适配当前 `moduleResolution: node`
- `flexlayout-react` 从 `^0.5.21` 升级到 `^0.9.1`
- FlexLayout 入口改为命名导出
- 布局配置中的旧 `width` 字段迁移为 `weight`
- 移除新类型不支持的旧 `tabEnableFloat` / `splitterSize`
- `react-window` 从 `^1.8.6` 升级到 `^1.8.11`
- 暂不升级 `react-window` 2.x；当前项目依赖 `VariableSizeGrid`，2.x 需要独立迁移专题
- `react-draggable` 从 `^4.4.3` 升级到 `^4.7.0`
- `react-resizable` 从 `^3.0.4` 升级到 `^3.0.5`
- 移除过时的 `@types/react-grid-layout`
- 确认 `@hello-pangea/dnd`、`react-dnd`、`react-dnd-html5-backend` 当前已在稳定线
- 补充前端布局、虚拟表格、拖拽运行时 smoke test
- 新增 Dashboard widget content 读取 helper：`getChartWidgetContent`、`getControllerWidgetContent`、`getTabWidgetContent`
- 将 Dashboard utils、TabWidgetCore、action/thunk 的 chart/controller/tab content 访问收口到统一 helper
- 补充 widget content helper 测试，覆盖有效协议和错配协议分支
- 前端补丁线升级：`react-resizable` `3.2.0`、`vitest` `4.1.9`、`less` `4.6.6`、`lint-staged` `17.0.8`
- Node 24 / React 18 类型边界对齐：`@types/node` `24.13.2`、`@types/react` `18.3.31`、`@types/react-dom` `18.3.7`、`@types/react-resizable` `3.0.8`
- ESLint TypeScript 插件补丁线对齐：`@typescript-eslint/eslint-plugin` / `parser` `8.61.1`

已通过验证：

```bash
npm run checkTs
npm run test:ci -- src/app/components/__tests__/dndRuntime.test.ts src/app/components/__tests__/virtualTableRuntime.test.ts src/app/components/__tests__/VirtualTable.test.tsx src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/__tests__/layoutRuntime.test.ts src/app/pages/DashBoardPage/hooks/__tests__/useGridLayoutMap.test.ts src/app/components/ChartGraph/BasicTableChart/__tests__/BasicTableChart.test.jsx
npm run test:ci -- src/app/pages/DashBoardPage/utils/__tests__/widget.test.ts
npm run test:ci -- src/app/pages/DashBoardPage/utils/__tests__/widget.test.ts src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/__tests__/layoutRuntime.test.ts src/app/pages/DashBoardPage/hooks/__tests__/useGridLayoutMap.test.ts
npm install --package-lock-only --dry-run --ignore-scripts
npm ci --dry-run --ignore-scripts
git diff --check
```

当前专题尚未合入 `main`。

## 6. 当前短期目标

短期目标：在 `codex/modernization-frontend-runtime-next` 上继续累计前端运行时和前端协议边界改造，完成一个更完整批次后再统一验证、提交、推送、合并。

优先推进顺序：

| 优先级 | 事项 | 风险 | 当前策略 |
| --- | --- | --- | --- |
| P1-C | Dashboard widget 内容协议继续收口 | 中 | 已新增统一读取 helper；下一步处理 action/thunk 剩余散点 |
| P1-F | 前端运行时依赖剩余项复扫 | 中 | 已完成一批补丁线升级；React 19 / AntD 6 / Vite 8 / TS 6 等主版本暂缓 |
| P1-G | 前端公开入口和深路径 import 复扫 | 低 | 保留公开样式入口，清理过时类型包和私有入口 |
| P1-H | Node 24 / npm 11 安装健康度复核 | 低 | 保持 lockfile 可解析，继续验证 `npm ci --dry-run --ignore-scripts` |

Dashboard widget 内容协议下一步切入点：

- 找出仍直接访问 `widget.config.content.xxx` 或未复用 content helper 的路径
- 优先复用 `getChartWidgetContent`、`getControllerWidgetContent`、`getTabWidgetContent`
- 必要时补充轻量 helper，避免在组件中散落结构判断
- 不改变保存协议、不做数据迁移、不重命名稳定字段
- 对 reducer、helper、核心组件补最小测试或 smoke test

建议恢复命令：

```bash
rg -n "widget\\.config\\.content\\.[A-Za-z]|content\\.[A-Za-z]" frontend/src/app/pages/DashBoardPage -g '*.ts' -g '*.tsx'
sed -n '230,380p' frontend/src/app/pages/DashBoardPage/pages/Board/slice/types.ts
sed -n '280,430p' frontend/src/app/pages/DashBoardPage/utils/widget.ts
sed -n '560,710p' frontend/src/app/pages/DashBoardPage/utils/widget.ts
sed -n '360,540p' frontend/src/app/pages/DashBoardPage/pages/BoardEditor/slice/childSlice/stackSlice.ts
```

## 7. 后续队列

| 阶段 | 事项 | 风险 | 执行策略 |
| --- | --- | --- | --- |
| P2-A | Maven、Docker、安装包链路复核 | 中 | 改构建链路前补 `mvn package -DskipTests` 和 demo smoke |
| P2-B | Shiro 认证授权健康度审计 | 高 | 只做边界用例和小修，不整体替换 |
| P2-C | Calcite SQL 解析健康度审计 | 高 | 先补 SQL 解析兼容样例，不整体替换 |
| P2-D | `react-window` 2.x 可行性评估 | 中高 | 独立专题，先验证 `VariableSizeGrid` 替换路径 |
| P2-E | 前端安全依赖治理 | 中高 | 单独专题处理 Dependabot 类问题，避免混入运行时改造 |
| P2-F | React 19、AntD 6、Vite 8、TypeScript 6 主版本评估 | 高 | 独立专题，先建立兼容矩阵和关键页面 smoke test |

暂不做整体重构：

- Java 包名 `datart.*`
- 配置前缀 `datart.*`
- `DATART_*`
- 数据迁移稳定标识
- Shiro 认证授权整体替换
- Calcite SQL 解析主链整体替换
- 数据源 provider / 方言 / 脚本运行时的大规模重构

## 8. 门禁策略

开发期按风险分层验证，不为每个小改动跑完整门禁。

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

依赖链路补充门禁：

```bash
npm install --package-lock-only --dry-run --ignore-scripts
npm ci --dry-run --ignore-scripts
```

测试缺口处理：

- 找不到现成相关测试时，必须记录缺口
- 优先补 helper、协议、运行时最小用例
- 不为了覆盖率硬造低价值快照测试

## 9. 提交节奏

同一专题内累计一组相关改动后再提交。

建议粒度：

| 类型 | 粒度 |
| --- | --- |
| 低风险类型边界 | 累计 3 到 10 个相关文件后提交 |
| 中风险运行时链路 | 每条可验证链路独立提交 |
| 依赖和构建链路 | 独立提交 |
| 阶段复盘 | 跟随当前批次提交，必要时可单独文档提交 |

当前专题下一次提交建议包含：

- Dashboard widget 内容协议边界收口
- 与该收口直接相关的测试
- 本文档同步记录

不要因为单个小文件改动立刻提交。

## 10. 历史追溯

需要追溯具体实现时使用：

```bash
git log --oneline -- docs/tech-stack-modernization-plan.md
git log --oneline -- frontend/src/app/components/FormGenerator
git log --oneline -- frontend/src/app/components/ChartGraph
git log --oneline -- frontend/src/app/pages/DashBoardPage
git log --oneline -- frontend/src/app/pages/MainPage/pages/VizPage
git log --oneline -- frontend/package.json frontend/package-lock.json
```
