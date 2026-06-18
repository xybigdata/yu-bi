# yu-bi 现代化改造执行板

本文档用于指导后续逐批改造，只保留当前决策需要的信息。

“现代化”不是追最新版本，而是在兼容、正确、可回归前提下，把前后端核心技术栈与关键运行链路收口到较新的稳定状态。

## 1. 长期目标

- 后端稳定兼容 `JDK 21`
- 前端工程链稳定兼容 `Node 24`
- 前端 CI 与本地开发基线统一使用 `Node 24.x`
- `main` 只允许 merge，不直接开发
- 同一改造专题优先在一条长期分支持续推进，累计到值得回归的批量后再 merge 回 `main`
- 低风险、中风险和可控高风险都纳入现代化改造；中高风险必须先收窄边界，并补足验证证据
- 不盲目替换业务协议，不做无证据的大范围重写

## 2. 固定边界

### 2.1 仓库与分支

- 工作目录：`/Users/chencongyu/WorkHome/VSProjects/open-project/yu-bi`
- 当前主线：`main`
- 当前累计分支：`codex/modernization-compatible-boundaries`
- 当前分支基点：`b519a24cd chore: 合入 Dashboard widget 内容协议边界批次`
- 默认自动 `git add`、`git commit --no-verify`、必要时 `git push origin <branch>`
- `main` 推送前必须跑完整门禁；专题分支日常推送只跑轻量门禁

### 2.2 禁止贸然重构的标识

- Java 包名 `datart.*`
- 配置前缀 `datart.*`
- `DATART_*` 等内部技术符号
- 数据迁移相关稳定常量、后缀、内部标识

### 2.3 工作区约束

- `.tmp/`、`logs/` 已加入 `.gitignore`
- 一个提交聚焦一个专题，不夹带无关重构
- 同一专题尽量多改一点再提交，避免频繁触发门禁
- 阶段性提交前在本文档记录本批验证证据

## 3. 当前技术栈基线

### 3.1 后端

| 项目 | 当前基线 | 状态 |
| --- | --- | --- |
| Java | `21` | 已达目标 |
| Spring Boot | `3.5.12` | 当前主链稳定 |
| Spring Cloud | `2025.0.1` | 与 Boot 3.5 配套 |
| Maven Enforcer | `Java >= 21`、`Maven >= 3.9` | 已落地 |
| MyBatis Spring Boot | `3.0.4` | 已适配 Boot 3 |
| GraalJS | `25.0.1` | 已替代 Nashorn 主链 |
| H2 | `2.4.240` | 已升级 |
| Selenium | `4.31.0` | 已升级 |
| Springdoc | `2.8.17` | 已适配 Boot 3 |
| Shiro | `2.0.5` | 高风险，只做可验证子问题 |
| Druid | `1.2.28` | 暂不优先动 |
| Calcite | 现网主链依赖 | 高风险，只做审计与用例准备 |

### 3.2 前端

| 项目 | 当前基线 | 状态 |
| --- | --- | --- |
| Node | `>=24.0.0` | 当前硬性目标 |
| `frontend/.nvmrc` | `v24.0.0` | 与目标一致 |
| npm | `>=11.0.0` | 已写入 `engines` |
| React | `18.3.1` | 已完成主升级 |
| React Router | `6.30.1` | 已完成主升级 |
| Ant Design | `5.26.2` | 已完成主升级 |
| Redux Toolkit | `2.12.0` | 已完成主升级 |
| React Redux | `9.3.0` | 已完成主升级 |
| TypeScript | `5.9.3` | 当前稳定主线 |
| Vite | `6.4.3` | 已替代 CRA 主工作流 |
| Vitest | `4.1.8` | 当前主测试栈 |
| styled-components | `6.1.19` | 已完成主升级 |
| react-quill | `2.0.0` | 已升级，兼容层继续稳定化 |
| monaco-editor | `0.52.2` | 真实运行时依赖 |
| reveal.js | `6.0.1` | 真实运行时依赖 |
| react-window | `1.8.6` | 使用面窄，继续稳定化 |
| flexlayout-react | `0.5.21` | 已收口到当前小版本 |
| react-grid-layout | `1.3.4` | 已收口到当前小版本 |

## 4. 阶段性复盘

### 4.1 已完成的主线成果

- 项目已从 datart 独立为 `yu-bi`
- README、NOTICE、SECURITY、ROADMAP、CHANGELOG、issue template 等治理文档已完成独立项目表述
- Maven 对外坐标、安装包、Docker 运行目录、部署文档已开始向 `yu-bi` 收口
- 保留 `datart.*` Java 包名、配置前缀和稳定内部标识不动
- 后端已建立 `JDK 21 + Spring Boot 3.5.x` 主链
- 前端已建立 `React 18 + Ant Design 5 + Vite 6 + Vitest 4 + Node 24` 主链
- CRA / CRACO、IE11 主兼容链、Nashorn、PhantomJS 等历史主链已退出
- GitHub Actions 主线门禁已切到 `main`
- 安装包闭环验证已打通，`yu-bi-server-*.zip` 可解压，demo 健康检查脚本可通过真实端口环境验证

### 4.2 已完成的现代化专题

- 依赖与安装健康度：
  - 清理未使用依赖，例如 `@antv/g2`、`html2canvas`
  - `react-resizable`、`flexlayout-react`、`react-grid-layout` 等声明与锁文件已收口
  - `react-dev-inspector` 已从开发态接入中移除

- 类型入口与运行时加载：
  - Ant Design / rc 历史深路径类型入口已清零
  - Monaco 静态类型导入切到包根入口，运行时懒加载路径保留
  - ECharts、WordCloud、Split、React Window、Reveal、Monaco、SQL formatter 等懒加载失败后清空缓存 Promise，支持重试
  - 运行时加载调用层补齐失败兜底，减少未处理 Promise

- 时间体系：
  - 当前时间入口统一到 `getDatartNow()` / `getDatartNowMillis()` / `formatCurrentDatartDate()`
  - 图表筛选默认时间、分享过期时间、区间默认值、迁移链路等已逐步收口
  - 日期工具补齐 `Dayjs` / `Date` 输入的稳定转换语义

- 富文本：
  - `react-quill 2` 已在用
  - 内容解析、Delta 归一化、运行时就绪态、字段引用、图片插入、模块销毁、运行时重试均已补强
  - 富文本仍属中风险链路，后续只做可验证稳定化，不做整体替换

- 看板与图表：
  - Dashboard widget 内容协议已完成第一阶段收口
  - `WidgetConf.content` 从裸 `any` 收口为兼容结构，`WidgetCreateProps.content` 改为 `unknown`
  - 新增 chart / controller / tab 内容守卫，消费点改为先守卫再读取
  - Dashboard permissions、WidgetInfo parameters 等弱类型边界已收紧
  - 图表事件行数据、ECharts 实例、BasicTable、地图、分享链路、请求参数、变量参数等多个运行时边界已收口

- View / Source / Query：
  - View config / model / detail / columnPermission 消费链补齐安全解析
  - 数据源配置坏 JSON 回退到默认图标或空配置
  - QueryResult rows 已从 `any[][]` 收口为查询结果标量二维数组
  - 查询结果消费入口补齐结构守卫

- 工具与测试：
  - `request2`、`requestWithHeader`、下载工具、错误处理、通用数组与树 helper 已完成多批类型边界收口
  - 迁移测试、工具测试、FormGenerator 测试等已大量减少散落 `as any`
  - 当前测试层真实宽泛强转主要集中在 legacy fixture 兼容场景

### 4.3 当前分支已完成但尚未提交的批次

当前工作区已有一批前端兼容边界改造，属于 `codex/modernization-compatible-boundaries`：

- FormGenerator 配置透传兼容边界第一阶段：
  - `ItemLayoutProps` / `FormGeneratorLayoutProps` 从全局 `any` 改为显式 `FormGeneratorValue` 和泛型 `context`
  - 交互面板、条件样式面板、指标卡条件样式面板补齐局部 `context` 类型
  - `ChartStyleConfigPanel`、`BoardConfigPanel`、`WidgetConfigPanel` 增加配置行守卫，只有完整 `ChartStyleConfig` 会提交到上层 reducer
  - `useDebouncedFormValue` 补齐泛型返回 tuple

- CrossFiltering 规则回调边界：
  - `CrossFilteringRuleList.onRuleChange` 改为按 `CrossFilteringInteractionRule` 键值映射约束
  - 自定义关联关系写回时把空输入归一为空数组，避免 `undefined` 穿透到必填 `Customize` 字段

- DrillThrough / Jump 规则动态写回边界：
  - `RuleList` 按 `JumpToChart` / `JumpToDashboard` / `JumpToUrl` 三类规则分别写回
  - `JumpToChart` / `JumpToDashboard` 的规则更新改为显式对象构造，去掉 `as JumpTo...Rule` 桥接
  - 保持原交互规则配置结构不变，只收紧编辑态回调边界

已验证：

- `npm run checkTs`
- `npm run test:ci -- src/app/components/FormGenerator/__tests__/utils.test.tsx src/app/components/FormGenerator/__tests__/BasicCheckbox.test.jsx src/app/components/FormGenerator/__tests__/BasicColorSelector.test.jsx src/app/components/FormGenerator/__tests__/BasicFont.test.tsx src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/index.test.ts`
- `npm run test:ci -- src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/index.test.ts src/app/pages/DashBoardPage/pages/BoardEditor/slice/__tests__/events.test.ts src/app/pages/DashBoardPage/utils/__tests__/index.test.tsx`
- `npm run test:ci -- src/app/components/FormGenerator/__tests__/utils.test.tsx src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/index.test.ts src/app/pages/DashBoardPage/pages/BoardEditor/slice/__tests__/events.test.ts src/app/pages/DashBoardPage/utils/__tests__/index.test.tsx`

## 5. 当前风险分层

### 5.1 可持续推进

| 专题 | 判断 | 下一步 |
| --- | --- | --- |
| FormGenerator / 交互配置边界 | 当前分支正在推进 | 继续按 DrillThrough / CrossFiltering / ViewDetail 子链路收口 |
| 前端局部类型边界 | 多数是可验证小步 | 继续按文件和调用链推进 |
| 时间体系剩余调用点 | 已有统一工具入口 | 复扫零散原生时间调用 |
| 前端公开类型入口 | 当前已清零主要深路径 | 防止回退，顺手审计新增代码 |
| 安装健康度 | 当前稳定 | 防止锁文件与 package 声明漂移 |

### 5.2 中风险专项

| 专题 | 风险 | 策略 |
| --- | --- | --- |
| 富文本兼容层 | 编辑态、只读态、分享态回归 | 只做小步稳定化，每步配富文本定向测试 |
| 图表运行时类型边界 | 图表渲染、事件、分页排序回归 | 先收口 helper 与事件 payload，再补图表定向测试 |
| Dashboard widget 内容协议 | 多类 widget 与迁移链路耦合 | 当前已完成第一阶段，后续按 widget 类型分批 |
| react-window / flexlayout / react-grid-layout | 真实运行时链路 | 不盲目替换，优先包一层兼容边界和异常兜底 |
| Docker / 安装包闭环 | 交付结构与启动链 | 涉及时跑安装包验证 |

### 5.3 高风险只做可控子问题

| 专题 | 暂不做 | 可做 |
| --- | --- | --- |
| Shiro | 不整体迁移认证授权 | 依赖健康度、兼容验证、最小认证用例 |
| Calcite | 不整体升级或替换 SQL 解析链 | 版本约束、SQL 解析用例、边界审计 |
| 数据源 / 脚本深层架构 | 不整体重构 provider / 方言 / 脚本运行时 | 配置解析、异常兜底、测试覆盖 |
| 内部命名与稳定标识 | 不做包名、配置前缀、数据迁移标识重构 | 仅对外品牌元数据继续收口 |

## 6. 下一步执行队列

### 6.1 当前分支继续推进

当前分支继续围绕“前端兼容边界收口”，暂不切新分支，暂不合 `main`。

优先队列：

1. 继续收口 FormGenerator 交互规则内部 `value / Customize` 子链路
   - `ViewDetailPanel` 自定义字段
   - `JumpToUrl` URL 参数关系
   - `ControllerList` / `UrlParamList` / `ChartRelationList` / `BoardRelationList` 的重复关系编辑结构
2. 复扫当前分支改动范围内的残留局部强转
   - 只处理同一协议边界内的强转
   - 不顺手扩散到图表运行时或 View/Source 其他模块
3. 当前批次达到提交点后提交并推送专题分支
   - 建议 commit message：`chore: 收口 FormGenerator 交互配置边界`
4. 专题分支继续累计后，再决定是否跑完整门禁并合回 `main`

### 6.2 下一批候选

| 优先级 | 专题 | 进入条件 |
| --- | --- | --- |
| P0 | FormGenerator 交互配置剩余子链路 | 当前分支继续 |
| P1 | `ChartFilterCondition.value` 公共协议 | 先梳理运行时真实值形态和 UI 调用面 |
| P1 | `ChartDataSetDTO.rows` 泛型边界 | 先确认后端 Dataframe 标量集合与前端图表消费边界 |
| P2 | 富文本兼容层剩余稳定化 | 每步必须有富文本定向测试 |
| P2 | 图表运行时 helper 宽口 | 优先已有测试覆盖的 helper |
| P3 | Shiro / Calcite 健康度审计 | 只做审计与用例，不做迁移 |

## 7. 执行方法

### 7.1 每轮开始

先核对：

- 当前分支不是 `main`
- `main` 与 `origin/main` 是否同步
- 当前改动是否仍属于当前专题
- 是否触碰禁止重构标识
- 是否已有足够验证入口

建议命令：

```bash
git status --short --branch
git log --oneline --decorate -8
git rev-list --left-right --count origin/main...main
```

### 7.2 开发期验证

前端日常改造默认：

```bash
npm run checkTs
npm run test:ci -- <related test files>
```

只在以下情况补更强验证：

- 依赖、构建配置、路由入口、运行时加载、共享模型、迁移链路变化
- 定向测试覆盖不足
- 一个专题已累计较多小步修改
- 准备 merge 回 `main`

### 7.3 合并前完整门禁

准备合回 `main` 前必须执行：

```bash
npm run test:ci
npm run lint:css
npm run lint:style
```

推送 `main` 时 pre-push 会自动执行完整前端门禁。专题分支默认只执行轻量 `npm run checkTs`。

### 7.4 提交策略

- 当前专题内可以多改一点再提交
- 提交前确保文档记录本批改动和验证
- 不直接在 `main` 开发
- 合回 `main` 使用 `git merge --no-ff <branch>`
- 推送 `main` 前不跳过完整门禁

## 8. 阶段性验证记录

只保留最近需要影响判断的验证记录。

- Dashboard widget 内容协议批次合并前完整门禁：
  - `npm run test:ci`，127 个测试文件通过，891 个测试通过，4 个跳过
  - `npm run lint:css`
  - `npm run lint:style`

- 当前 FormGenerator / 交互配置边界批次：
  - `npm run checkTs`
  - `npm run test:ci -- src/app/components/FormGenerator/__tests__/utils.test.tsx src/app/components/FormGenerator/__tests__/BasicCheckbox.test.jsx src/app/components/FormGenerator/__tests__/BasicColorSelector.test.jsx src/app/components/FormGenerator/__tests__/BasicFont.test.tsx src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/index.test.ts`，5 个测试文件通过，23 个测试通过，1 个跳过
  - `npm run test:ci -- src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/index.test.ts src/app/pages/DashBoardPage/pages/BoardEditor/slice/__tests__/events.test.ts src/app/pages/DashBoardPage/utils/__tests__/index.test.tsx`，3 个测试文件通过，29 个测试通过，3 个跳过
  - `npm run test:ci -- src/app/components/FormGenerator/__tests__/utils.test.tsx src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/index.test.ts src/app/pages/DashBoardPage/pages/BoardEditor/slice/__tests__/events.test.ts src/app/pages/DashBoardPage/utils/__tests__/index.test.tsx`，4 个测试文件通过，40 个测试通过，3 个跳过

## 9. 历史检索提示

此前大量细粒度流水已压缩为专题摘要。后续如需追溯具体实现，用 git 历史检索：

```bash
git log --oneline -- docs/tech-stack-modernization-plan.md
git log --oneline -- frontend/src/app/components/FormGenerator
git log --oneline -- frontend/src/app/pages/DashBoardPage
git log --oneline -- frontend/src/app/components/ChartGraph
```

关键合并点：

- `b519a24cd chore: 合入 Dashboard widget 内容协议边界批次`
- `484c44fd9 chore: 合入图表运行时类型边界批次`
