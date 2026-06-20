# yu-bi 现代化改造执行板

本文档是“现代化改造”的恢复入口和执行工作板。它只记录目标、边界、阶段复盘、下一批任务和验证策略，历史细节通过 git 追溯。

“现代化”不是追最新版本，而是在兼容、正确、可回归的前提下，把前后端核心技术栈和关键运行链路收口到较新的稳定状态。

最后复盘时间：2026-06-20

## 1. 目标与边界

### 1.1 长期目标

- 后端稳定兼容 `JDK 21`
- 前端工程链稳定兼容 `Node 24`
- 前端 CI 与本地开发基线统一使用 `Node 24.x`
- 前后端核心技术栈保持在较新的稳定版，不盲目追最新
- 中高风险项先收窄边界、补足验证证据，再渐进替换或升级
- 同一专题尽量在一条长期分支累计，减少分支创建、主线合并和完整门禁次数

### 1.2 当前短期目标

当前专题分支继续推进“前端兼容边界收口”：

- 先完成 `ChartFilterCondition.value` 公共协议收口
- 梳理但暂不贸然改造 `ChartDataSetDTO.rows`，因为它影响全图表运行时
- 继续处理同一调用链内可验证的中风险边界
- 不扩散到无关模块，不做内部命名大迁移
- 累计到一批值得回归的改造后，再合回 `main`

### 1.3 固定边界

- 工作目录：`/Users/chencongyu/WorkHome/VSProjects/open-project/yu-bi`
- 主线分支：`main`
- 当前专题分支：`codex/modernization-compatible-boundaries`
- `main` 不直接开发，只允许通过 merge 接收专题分支
- 默认自动 `git add`、`git commit --no-verify`，必要时 `git push origin <branch>`
- `.tmp/`、`logs/` 已加入 `.gitignore`

禁止贸然重构：

- Java 包名 `datart.*`
- 配置前缀 `datart.*`
- `DATART_*` 等内部技术符号
- 数据迁移相关稳定常量、后缀、内部标识

## 2. 当前状态快照

### 2.1 Git 状态

- 当前分支：`codex/modernization-compatible-boundaries`
- 当前分支基点：`b519a24cd chore: 合入 Dashboard widget 内容协议边界批次`
- 复盘时当前分支相对 `origin/main`：领先 9 个提交，未落后
- 最近已推送提交：`deb647680 chore: 收口 ChartFilterCondition 值协议边界`
- 复盘时工作区为 `ChartDataSet` 单元格值协议试点批次，验证已通过，待提交

恢复工作时先执行：

```bash
git status --short --branch
git log --oneline --decorate -8
git rev-list --left-right --count origin/main...HEAD
```

判断：

- 当前分支不能是 `main`
- 当前改动必须属于当前专题
- 不能触碰禁止重构标识
- 如果 `main` 已前进，先判断是否需要同步；不为小改动频繁合并

### 2.2 后端基线

| 项目                | 当前基线   | 判断                     |
| ------------------- | ---------- | ------------------------ |
| Java                | `21`       | 已达硬性目标             |
| Maven               | `>=3.9`    | 已由 Enforcer 约束       |
| Spring Boot         | `3.5.12`   | 当前主链稳定             |
| Spring Cloud        | `2025.0.1` | 与 Boot 3.5 配套         |
| MyBatis Spring Boot | `3.0.4`    | 已适配 Boot 3            |
| GraalJS             | `25.0.1`   | 已替代 Nashorn 主链      |
| Springdoc           | `2.8.17`   | 已适配 Boot 3            |
| H2                  | `2.4.240`  | 已升级                   |
| Selenium            | `4.31.0`   | 已升级                   |
| Shiro               | `2.0.5`    | 高风险，只做可验证子问题 |
| Druid               | `1.2.28`   | 暂不优先动               |
| Calcite             | 现网主链   | 高风险，先审计和补用例   |

### 2.3 前端基线

| 项目              | 当前基线   | 判断                     |
| ----------------- | ---------- | ------------------------ |
| Node              | `>=24.0.0` | 硬性目标                 |
| `frontend/.nvmrc` | `v24.0.0`  | 与目标一致               |
| npm               | `>=11.0.0` | 已写入 `engines`         |
| React             | `18.3.1`   | 当前稳定主链             |
| React Router      | `6.30.1`   | 已完成主升级             |
| Ant Design        | `5.26.2`   | 已完成主升级             |
| Redux Toolkit     | `2.12.0`   | 已完成主升级             |
| React Redux       | `9.3.0`    | 已完成主升级             |
| TypeScript        | `5.9.3`    | 当前稳定主线             |
| Vite              | `6.4.3`    | 已替代 CRA 主工作流      |
| Vitest            | `4.1.8`    | 当前主测试栈             |
| styled-components | `6.1.19`   | 已完成主升级             |
| react-quill       | `2.0.0`    | 已升级，兼容层继续稳定化 |
| monaco-editor     | `0.52.2`   | 真实运行时依赖           |
| reveal.js         | `6.0.1`    | 真实运行时依赖           |
| react-window      | `1.8.6`    | 使用面窄，先稳定边界     |
| flexlayout-react  | `0.5.21`   | 当前小版本已收口         |
| react-grid-layout | `1.3.4`    | 当前小版本已收口         |

## 3. 阶段复盘

### 3.1 已建立的主链

- 项目已从 datart 独立为 `yu-bi`
- README、NOTICE、SECURITY、ROADMAP、CHANGELOG、issue template 等治理文档已完成独立项目表述
- Maven 对外坐标、安装包、Docker 运行目录、部署文档已开始向 `yu-bi` 收口
- 保留 `datart.*` Java 包名、配置前缀和稳定内部标识不动
- 后端已建立 `JDK 21 + Spring Boot 3.5.x` 主链
- 前端已建立 `React 18 + Ant Design 5 + Vite 6 + Vitest 4 + Node 24` 主链
- CRA / CRACO、IE11 主兼容链、Nashorn、PhantomJS 等历史主链已退出
- GitHub Actions 主线门禁已切到 `main`
- 安装包闭环验证已打通，`yu-bi-server-*.zip` 可解压，demo 健康检查脚本可通过真实端口环境验证

### 3.2 已完成的改造专题

| 专题                         | 状态     | 结果                                                     |
| ---------------------------- | -------- | -------------------------------------------------------- |
| 依赖与工程链                 | 已完成   | 清理未使用依赖，收口声明与锁文件，移除 CRA/CRACO 主链    |
| 运行时加载与兜底             | 已完成   | 懒加载失败清空缓存 Promise，调用层补齐失败兜底           |
| 时间体系                     | 已完成   | 当前时间入口统一到 `getDatartNow()` 等工具               |
| 富文本兼容层                 | 阶段完成 | `react-quill 2` 已在用，内容解析和运行时就绪态已补强     |
| Dashboard / Widget 内容协议  | 阶段完成 | `WidgetConf.content`、参数、权限、迁移链路已分批收口     |
| 图表 / 查询运行时边界        | 阶段完成 | 事件行数据、实例、请求参数、变量参数、查询 rows 已收口   |
| FormGenerator / 交互配置边界 | 阶段完成 | 关系编辑、规则构造、规则回调协议已收口，定向测试已补齐   |

### 3.3 当前分支已完成批次

| 提交       | 批次                         | 关键内容                                             | 验证摘要             |
| ---------- | ---------------------------- | ---------------------------------------------------- | -------------------- |
| `64d62d771` | FormGenerator 交互配置边界   | FormGenerator 泛型、context、配置行守卫、规则写回    | `checkTs` + 定向测试 |
| `bd633990a` | FormGenerator 关系编辑边界   | `relationUtils`、关系数组增删改、事件切换 bug 修正   | `checkTs` + 定向测试 |
| `9599cc59f` | FormGenerator 交互规则构造   | ViewDetail / Jump 规则构造函数与构造语义测试         | `checkTs` + 定向测试 |
| `7083dae3f` | FormGenerator 交互规则回调   | `InteractionRuleChange` 泛型回调协议，规则值映射收口 | `checkTs`            |
| `deb647680` | ChartFilterCondition 值协议  | 筛选值联合协议、关系值守卫、筛选 UI 消费点归一化     | `checkTs` + 定向测试 |

本阶段结论：

- FormGenerator P0 弱类型边界已基本清空
- 剩余 `unknown` 主要是默认泛型、翻译 options 和通用 value 入口，暂不作为同类弱类型问题继续消耗
- 下一阶段应转入图表筛选值协议，而不是继续在 FormGenerator 内做低收益扫尾

### 3.4 当前待提交批次

批次：`ChartDataSet` 单元格值协议试点

已完成：

- `frontend/src/app/types/ChartDataSet.ts`
- `frontend/src/app/utils/chartHelper.ts`
- `frontend/src/app/components/ChartGraph/BasicDoubleYChart/BasicDoubleYChart.tsx`
- `frontend/src/app/components/ChartGraph/BasicDoubleYChart/types.ts`
- `frontend/src/app/components/ChartGraph/BasicDoubleYChart/utils.ts`
- `frontend/src/app/components/ChartGraph/BasicBarChart/types.ts`
- `frontend/src/app/components/ChartGraph/BasicLineChart/types.ts`
- `frontend/src/app/pages/DashBoardPage/components/Widgets/ControllerWidget/ControllerWidgetCore.tsx`
- `frontend/src/app/pages/DashBoardPage/utils/widget.ts`

本批结果：

- 新增 `ChartDataSetCellValue` / `ChartDataSetRow` / `ChartDataSetRows`，显式记录图表数据单元真实支持 `string | number | null | undefined`
- `ChartDataSetDTO.rows` 暂不全局切换，仍保持 `string[][]`，避免一次性强迫所有图表运行时迁移
- 公共 `chartHelper` 中 min/max、mark line、mark area、tooltip rowData 等入口先接受 `ChartDataSetCellValue`
- `BasicDoubleYChart` 作为有测试覆盖的图表链路完成标量值协议试点
- Bar / Line series 类型补齐空值协议，匹配图表数据真实可能出现的 null/undefined
- Dashboard 控制器选项和树形数据边界将数据集值显式字符串化，避免非字符串单元格泄漏到 key/label 协议

验证已通过：

```bash
npm run checkTs
npm run test:ci -- src/app/models/__tests__/ChartDataSet.test.ts src/app/components/ChartGraph/BasicDoubleYChart/__tests__/utils.test.ts
npm run test:ci -- src/app/utils/__tests__/chartHelper.test.ts
```

## 4. 下一步执行队列

### 4.1 当前批次 P0

提交并推送 `ChartDataSet` 单元格值协议试点批次：

1. 复核暂存范围，只包含本批数据集单元格协议、公共 helper、双轴试点和执行文档
2. 提交信息使用：

建议提交信息：

```text
chore: 收口 ChartDataSet 单元格值协议边界
```

3. 推送当前专题分支

### 4.2 下一批 P1

继续在同一分支推进以下可控项，不立即切新分支：

| 专题                         | 下一步                                           | 风险 |
| ---------------------------- | ------------------------------------------------ | ---- |
| `ChartDataSetDTO.rows`       | 暂不全局切换，按图表链路逐批迁移 `IChartDataSet<string>` | 高   |
| 图表筛选值协议               | 复扫筛选 UI、下钻、联动参数写回的残留断言       | 中   |
| 图表 helper 类型边界         | 选择已有测试覆盖的 helper 做小批收口            | 中   |
| 时间体系剩余调用点           | 复扫零散原生时间调用                            | 低   |
| 前端公开类型入口             | 防止 Ant Design / rc 深路径类型入口回退         | 低   |
| 安装健康度                   | 防止锁文件与 package 声明漂移                   | 低   |

### 4.3 暂缓或只做审计

| 专题                  | 暂不做                                  | 可做                               |
| --------------------- | --------------------------------------- | ---------------------------------- |
| `ChartDataSetDTO.rows` | 不直接改全图表 rows 协议                | 记录调用面、补运行时用例、选子链路 |
| Shiro                 | 不整体迁移认证授权                      | 依赖健康度、兼容验证、最小认证用例 |
| Calcite               | 不整体升级或替换 SQL 解析链             | 版本约束、SQL 解析用例、边界审计   |
| 数据源 / 脚本深层架构 | 不整体重构 provider / 方言 / 脚本运行时 | 配置解析、异常兜底、测试覆盖       |
| 内部命名与稳定标识    | 不做包名、配置前缀、迁移标识重构        | 仅继续收口对外品牌元数据           |

## 5. 验证策略

### 5.1 开发期轻量验证

日常前端改造默认执行：

```bash
npm run checkTs
npm run test:ci -- <related test files>
```

当前 `ChartFilterCondition.value` 批次优先执行：

```bash
npm run checkTs
npm run test:ci -- src/app/models/__tests__/ChartFilterCondition.test.ts
```

如果改到筛选 UI 或时间筛选工具，再补跑：

```bash
npm run test:ci -- src/app/pages/MainPage/pages/VizPage/ChartPreview/components/ControllerPanel/components/__tests__/timeFilterUtils.test.ts src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartTimeSelector/__tests__/utils.test.ts
```

### 5.2 触发更强验证的条件

- 依赖、构建配置、路由入口、运行时加载、共享模型、迁移链路变化
- 定向测试覆盖不足
- 一个专题已累计较多小步修改
- 准备 merge 回 `main`

合并前完整门禁：

```bash
npm run test:ci
npm run lint:css
npm run lint:style
```

推送 `main` 前不跳过完整门禁。

## 6. 提交与合并节奏

- 当前专题内可以多改一点再提交，不因单个小文件改动立即提交
- 每次提交前更新本文档的阶段记录和验证记录
- 专题分支可以推送远端
- 合回 `main` 使用 `git merge --no-ff <branch>`
- 合回 `main` 后再推送主线

推荐提交粒度：

- 小批类型边界：累计 3 到 6 个相关文件后提交
- 中风险运行时链路：每条链路独立提交
- 依赖或构建链路：独立提交并跑更强验证
- 文档阶段复盘：可独立提交

## 7. 历史检索提示

此前大量细粒度流水已压缩为专题摘要。需要追溯具体实现时用 git 历史检索：

```bash
git log --oneline -- docs/tech-stack-modernization-plan.md
git log --oneline -- frontend/src/app/components/FormGenerator
git log --oneline -- frontend/src/app/pages/DashBoardPage
git log --oneline -- frontend/src/app/components/ChartGraph
```

关键合并点：

- `b519a24cd chore: 合入 Dashboard widget 内容协议边界批次`
- `484c44fd9 chore: 合入图表运行时类型边界批次`
