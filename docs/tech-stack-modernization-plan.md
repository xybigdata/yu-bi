# yu-bi 现代化改造执行板

本文档是“现代化改造”的恢复入口和执行工作板。它只记录当前目标、边界、状态、下一步和验证策略；历史细节通过 git 追溯。

“现代化”不是追最新版本，而是在兼容、正确、可回归的前提下，把前后端核心技术栈和关键运行链路收口到较新的稳定状态。

最后复盘时间：2026-06-20

## 1. 目标与边界

### 1.1 长期目标

- 后端稳定兼容 `JDK 21`
- 前端工程链稳定兼容 `Node 24`
- 前后端核心技术栈保持在较新的稳定版，不盲目追最新
- 中高风险项先收窄协议边界、补足验证证据，再渐进替换或升级
- 同一改造专题尽量在一条长期分支累计，减少分支创建、主线合并和完整门禁次数

### 1.2 当前短期目标

当前专题分支继续推进“前端兼容边界收口”：

- 已完成 `FormGenerator` 交互配置边界阶段性收口
- 已完成 `ChartFilterCondition.value` 公共协议收口
- 正在推进图表数据集标量协议迁移
- `ChartDataSetDTO.rows` 暂不全局切换，避免一次性影响所有图表运行时
- 剩余中高风险链路按可验证子链路推进，不做无验证的大迁移

### 1.3 固定边界

- 工作目录：`/Users/chencongyu/WorkHome/VSProjects/open-project/yu-bi`
- 主线分支：`main`
- 当前专题分支：`codex/modernization-compatible-boundaries`
- `main` 不直接开发，只允许通过 merge 接收专题分支
- 专题分支可以推送远端
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
- 当前分支相对 `origin/main`：领先 10 个提交，未落后
- 最近已推送提交：`26bf8a959 chore: 收口 ChartDataSet 单元格值协议边界`
- 当前工作区：图表数据集标量协议迁移批次，尚未提交
- 当前轻量验证：`npm run checkTs` 已通过

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

| 项目                | 当前基线   | 状态                     |
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

| 项目              | 当前基线   | 状态                     |
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

### 3.2 当前分支已完成批次

| 提交        | 批次                         | 结果                                                     |
| ----------- | ---------------------------- | -------------------------------------------------------- |
| `64d62d771` | FormGenerator 交互配置边界   | 泛型、context、配置行守卫、规则写回完成收口              |
| `bd633990a` | FormGenerator 关系编辑边界   | `relationUtils`、关系数组增删改、事件切换 bug 修正       |
| `9599cc59f` | FormGenerator 交互规则构造   | ViewDetail / Jump 规则构造函数与构造语义测试完成         |
| `7083dae3f` | FormGenerator 交互规则回调   | `InteractionRuleChange` 泛型回调协议和规则值映射完成收口 |
| `deb647680` | ChartFilterCondition 值协议  | 筛选值联合协议、关系值守卫、筛选 UI 消费点归一化完成     |
| `26bf8a959` | ChartDataSet 单元格值协议    | 公共数据集单元格协议、helper、双轴图表试点完成           |

### 3.3 当前进行中批次

批次：图表数据集标量协议迁移

目标：

- 将普通图表内部 `IChartDataSet<string>` 假设迁移为 `ChartDataSetCellValue`
- 对 key、name、category 等字符串协议入口显式 `String(value ?? '')`
- 对 ECharts 数值协议入口显式使用 `toSafeNumber` 或保留 `undefined`
- 不全局修改 `ChartDataSetDTO.rows`

当前已覆盖文件：

- `BasicBarChart`
- `BasicLineChart`
- `BasicGaugeChart`
- `BasicRichText`
- `WordCloudChart`
- `WaterfallChart`
- `Scorecard`
- `BasicPieChart`
- `BasicFunnelChart`
- `BasicScatterChart`
- `BasicOutlineMapChart`

当前验证：

```bash
npm run checkTs
npm run test:ci -- src/app/utils/__tests__/chartHelper.test.ts src/app/components/ChartGraph/BasicDoubleYChart/__tests__/utils.test.ts
```

结果：已通过。

测试缺口：

- 未找到上述具体图表组件的现成 `*.test.ts` / `*.test.tsx`
- 本批暂以类型门禁和协议边界复核为主
- 后续迁移 `BasicTableChart` / `PivotSheetChart` 时需要补更有价值的运行时用例

### 3.4 当前残留弱类型入口

当前复扫命令：

```bash
rg -n "IChartDataSet<string>|IChartDataSetRow<string>" frontend/src/app/components/ChartGraph frontend/src/app/utils frontend/src/app/pages -g '*.ts' -g '*.tsx'
```

剩余重点：

| 模块              | 状态   | 处理策略                                         |
| ----------------- | ------ | ------------------------------------------------ |
| `BasicTableChart` | 未迁移 | 表格渲染链路复杂，作为下一批独立收口             |
| `PivotSheetChart` | 未迁移 | 透视表依赖 AntV S2，作为下一批独立收口           |
| 测试断言          | 可保留 | 仅在生产代码迁移后同步调整                       |
| 注释              | 可保留 | 顺手修正，不单独作为改造目标                     |

## 4. 下一步执行队列

### 4.1 当前批次 P0

完成并提交“图表数据集标量协议迁移”：

1. 复核 diff，只包含当前普通图表数据集标量协议迁移和本文档
2. 运行：

```bash
npm run checkTs
npm run test:ci -- src/app/utils/__tests__/chartHelper.test.ts src/app/components/ChartGraph/BasicDoubleYChart/__tests__/utils.test.ts
git diff --check
```

3. 提交信息：

```text
chore: 迁移图表数据集标量值边界
```

4. 推送当前专题分支：

```bash
git push origin codex/modernization-compatible-boundaries
```

### 4.2 下一批 P1

继续在同一分支推进，不立即切新分支：

| 优先级 | 专题                         | 下一步                                           | 风险 |
| ------ | ---------------------------- | ------------------------------------------------ | ---- |
| P1     | `BasicTableChart` 数据集协议 | 迁移表格行和单元格值协议，补必要用例             | 中   |
| P1     | `PivotSheetChart` 数据集协议 | 迁移 S2 输入边界，重点验证 null/number/string    | 中   |
| P1     | 图表 helper 类型边界         | 清理剩余注释、测试断言和 helper 入参假设         | 低   |
| P2     | 图表筛选值协议残留          | 复扫筛选 UI、下钻、联动参数写回的残留断言        | 中   |
| P2     | 时间体系剩余调用点          | 复扫零散原生时间调用                             | 低   |
| P2     | 前端公开类型入口            | 防止 Ant Design / rc 深路径类型入口回退          | 低   |
| P2     | 安装健康度                  | 防止锁文件与 package 声明漂移                    | 低   |

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

日常前端类型边界改造默认执行：

```bash
npm run checkTs
npm run test:ci -- <related test files>
```

如果没有相关测试文件：

- 必须记录测试缺口
- 必须说明本批验证依赖类型门禁还是手工运行时复核
- 不为了覆盖率硬造低价值快照测试

### 5.2 分层门禁

| 场景                             | 门禁                                             |
| -------------------------------- | ------------------------------------------------ |
| 纯类型边界、小范围组件迁移       | `npm run checkTs` + 相关测试                     |
| helper、模型、共享协议变化       | `npm run checkTs` + 相关模型 / helper 测试       |
| 依赖、构建配置、运行时加载变化   | `npm run checkTs` + `npm run test:ci`            |
| 准备 merge 回 `main`             | 前端完整门禁 + 后端必要门禁                      |
| 推送 `main`                      | 不跳过完整门禁                                   |

前端完整门禁：

```bash
npm run checkTs
npm run test:ci
npm run lint:css
npm run lint:style
```

后端必要门禁按改动范围选择：

```bash
mvn test
mvn package -DskipTests
```

## 6. 提交与合并节奏

- 当前专题内可以多改一点再提交，不因单个小文件改动立即提交
- 每次提交前更新本文档的阶段记录和验证记录
- 专题分支可以推送远端
- 合回 `main` 使用 `git merge --no-ff <branch>`
- 合回 `main` 后再推送主线

推荐提交粒度：

- 小批类型边界：累计 3 到 10 个相关文件后提交
- 中风险运行时链路：每条链路独立提交
- 依赖或构建链路：独立提交并跑更强验证
- 文档阶段复盘：可以随同当前批次提交

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
