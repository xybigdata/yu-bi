# yu-bi 现代化改造执行板

本文档是后续改造的工作板，只保留决策、执行顺序和验证策略。

“现代化”不是追最新版本，而是在兼容、正确、可回归的前提下，把前后端核心技术栈和关键运行链路收口到较新的稳定状态。

最后复盘时间：2026-06-18

## 1. 目标

### 1.1 长期目标

- 后端稳定兼容 `JDK 21`
- 前端工程链稳定兼容 `Node 24`
- 前端 CI 与本地开发基线统一使用 `Node 24.x`
- 前后端核心技术栈保持在较新的稳定版，而不是盲目追最新
- 中高风险项必须先收窄边界、补足验证证据，再渐进替换或升级
- 全过程以本文档作为执行记录和下一步依据

### 1.2 当前短期目标

当前专题分支继续推进“前端兼容边界收口”：

- 优先处理 FormGenerator / 交互配置 / Dashboard 配置链路中的弱类型边界
- 顺手处理同一调用链内可验证的中风险边界
- 不扩散到无关模块，不做内部命名大迁移
- 累计到一批值得回归的改造后再合回 `main`

### 1.3 固定边界

- 工作目录：`/Users/chencongyu/WorkHome/VSProjects/open-project/yu-bi`
- 主线分支：`main`
- 当前专题分支：`codex/modernization-compatible-boundaries`
- `main` 不直接开发，只允许通过 merge 接收专题分支
- 同一专题尽量在一条长期分支累计，不频繁创建分支或合并 `main`
- 默认自动 `git add`、`git commit --no-verify`，必要时 `git push origin <branch>`
- `.tmp/`、`logs/` 已加入 `.gitignore`

禁止贸然重构：

- Java 包名 `datart.*`
- 配置前缀 `datart.*`
- `DATART_*` 等内部技术符号
- 数据迁移相关稳定常量、后缀、内部标识

## 2. 当前项目现状

### 2.1 Git 状态

- 当前分支：`codex/modernization-compatible-boundaries`
- 当前分支基点：`b519a24cd chore: 合入 Dashboard widget 内容协议边界批次`
- 复盘时当前分支相对 `origin/main`：领先 1 个提交，未落后
- 最近业务改造提交：`64d62d771 chore: 收口 FormGenerator 交互配置边界`
- 最近一次复盘前工作区干净

后续恢复工作时先执行：

```bash
git status --short --branch
git log --oneline --decorate -8
git rev-list --left-right --count origin/main...HEAD
```

### 2.2 后端基线

| 项目                | 当前基线     | 判断                     |
| ------------------- | ------------ | ------------------------ |
| Java                | `21`         | 已达硬性目标             |
| Maven               | `>=3.9`      | 已由 Enforcer 约束       |
| Spring Boot         | `3.5.12`     | 当前主链稳定             |
| Spring Cloud        | `2025.0.1`   | 与 Boot 3.5 配套         |
| MyBatis Spring Boot | `3.0.4`      | 已适配 Boot 3            |
| GraalJS             | `25.0.1`     | 已替代 Nashorn 主链      |
| Springdoc           | `2.8.17`     | 已适配 Boot 3            |
| H2                  | `2.4.240`    | 已升级                   |
| Selenium            | `4.31.0`     | 已升级                   |
| Shiro               | `2.0.5`      | 高风险，只做可验证子问题 |
| Druid               | `1.2.28`     | 暂不优先动               |
| Calcite             | 现网主链依赖 | 高风险，先审计和补用例   |

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

### 2.4 已完成的阶段成果

- 项目已从 datart 独立为 `yu-bi`
- README、NOTICE、SECURITY、ROADMAP、CHANGELOG、issue template 等治理文档已完成独立项目表述
- Maven 对外坐标、安装包、Docker 运行目录、部署文档已开始向 `yu-bi` 收口
- 保留 `datart.*` Java 包名、配置前缀和稳定内部标识不动
- 后端已建立 `JDK 21 + Spring Boot 3.5.x` 主链
- 前端已建立 `React 18 + Ant Design 5 + Vite 6 + Vitest 4 + Node 24` 主链
- CRA / CRACO、IE11 主兼容链、Nashorn、PhantomJS 等历史主链已退出
- GitHub Actions 主线门禁已切到 `main`
- 安装包闭环验证已打通，`yu-bi-server-*.zip` 可解压，demo 健康检查脚本可通过真实端口环境验证

## 3. 已完成专题复盘

### 3.1 依赖与工程链

- 清理未使用依赖，例如 `@antv/g2`、`html2canvas`
- `react-resizable`、`flexlayout-react`、`react-grid-layout` 等声明与锁文件已收口
- `react-dev-inspector` 已从开发态接入中移除
- Ant Design / rc 历史深路径类型入口已清零
- Monaco 静态类型导入切到包根入口，运行时懒加载路径保留

### 3.2 运行时加载与兜底

- ECharts、WordCloud、Split、React Window、Reveal、Monaco、SQL formatter 等懒加载失败后清空缓存 Promise，支持重试
- 运行时加载调用层补齐失败兜底，减少未处理 Promise
- 富文本、Reveal、Monaco 等真实运行时依赖已建立局部 runtime 封装

### 3.3 时间体系

- 当前时间入口统一到 `getDatartNow()` / `getDatartNowMillis()` / `formatCurrentDatartDate()`
- 图表筛选默认时间、分享过期时间、区间默认值、迁移链路等已逐步收口
- 日期工具补齐 `Dayjs` / `Date` 输入的稳定转换语义

### 3.4 富文本

- `react-quill 2` 已在用
- 内容解析、Delta 归一化、运行时就绪态、字段引用、图片插入、模块销毁、运行时重试均已补强
- 富文本仍属中风险链路，后续只做可验证稳定化，不整体替换

### 3.5 Dashboard / 图表 / 查询链路

- Dashboard widget 内容协议已完成第一阶段收口
- `WidgetConf.content` 从裸 `any` 收口为兼容结构，`WidgetCreateProps.content` 改为 `unknown`
- 新增 chart / controller / tab 内容守卫，消费点改为先守卫再读取
- Dashboard permissions、WidgetInfo parameters 等弱类型边界已收紧
- 图表事件行数据、ECharts 实例、BasicTable、地图、分享链路、请求参数、变量参数等多个运行时边界已收口
- View config / model / detail / columnPermission 消费链补齐安全解析
- 数据源配置坏 JSON 回退到默认图标或空配置
- QueryResult rows 已从 `any[][]` 收口为查询结果标量二维数组

### 3.6 当前分支已完成批次

提交：`64d62d771 chore: 收口 FormGenerator 交互配置边界`

已完成：

- `ItemLayoutProps` / `FormGeneratorLayoutProps` 从全局 `any` 改为显式 `FormGeneratorValue` 和泛型 `context`
- `useDebouncedFormValue` 补齐泛型返回 tuple
- 交互面板、条件样式面板、指标卡条件样式面板补齐局部 `context` 类型
- `ChartStyleConfigPanel`、`BoardConfigPanel`、`WidgetConfigPanel` 增加配置行守卫
- `CrossFilteringRuleList.onRuleChange` 改为按 `CrossFilteringInteractionRule` 键值映射约束
- CrossFiltering 自定义关系写回时把空输入归一为空数组
- `RuleList` 按 `JumpToChart` / `JumpToDashboard` / `JumpToUrl` 三类规则分别写回
- `JumpToChart` / `JumpToDashboard` 的规则更新改为显式对象构造，去掉 `as JumpTo...Rule` 桥接

验证已通过：

```bash
npm run checkTs
npm run test:ci -- src/app/components/FormGenerator/__tests__/utils.test.tsx src/app/components/FormGenerator/__tests__/BasicCheckbox.test.jsx src/app/components/FormGenerator/__tests__/BasicColorSelector.test.jsx src/app/components/FormGenerator/__tests__/BasicFont.test.tsx src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/index.test.ts
npm run test:ci -- src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/index.test.ts src/app/pages/DashBoardPage/pages/BoardEditor/slice/__tests__/events.test.ts src/app/pages/DashBoardPage/utils/__tests__/index.test.tsx
npm run test:ci -- src/app/components/FormGenerator/__tests__/utils.test.tsx src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/index.test.ts src/app/pages/DashBoardPage/pages/BoardEditor/slice/__tests__/events.test.ts src/app/pages/DashBoardPage/utils/__tests__/index.test.tsx
```

## 4. 风险分层

### 4.1 可持续推进

| 专题                         | 下一步                                                       |
| ---------------------------- | ------------------------------------------------------------ |
| FormGenerator / 交互配置边界 | 继续按 CrossFiltering / DrillThrough / ViewDetail 子链路收口 |
| 前端局部类型边界             | 按调用链推进，优先处理 `unknown` 入口和 `any` 消费点         |
| 时间体系剩余调用点           | 复扫零散原生时间调用                                         |
| 前端公开类型入口             | 防止 Ant Design / rc 深路径类型入口回退                      |
| 安装健康度                   | 防止锁文件与 package 声明漂移                                |

### 4.2 中风险，可同步处理但必须有验证

| 专题                                          | 风险                       | 策略                                           |
| --------------------------------------------- | -------------------------- | ---------------------------------------------- |
| 富文本兼容层                                  | 编辑态、只读态、分享态回归 | 只做小步稳定化，每步配富文本定向测试           |
| 图表运行时类型边界                            | 渲染、事件、分页排序回归   | 先收口 helper 与事件 payload，再补图表定向测试 |
| Dashboard widget 内容协议                     | 多类 widget 与迁移链路耦合 | 按 widget 类型分批，避免一次性改协议           |
| react-window / flexlayout / react-grid-layout | 真实运行时链路             | 不盲目替换，优先包兼容边界和异常兜底           |
| Docker / 安装包闭环                           | 交付结构与启动链           | 涉及时跑安装包验证                             |

### 4.3 高风险，只做可控子问题

| 专题                  | 暂不做                                  | 可做                               |
| --------------------- | --------------------------------------- | ---------------------------------- |
| Shiro                 | 不整体迁移认证授权                      | 依赖健康度、兼容验证、最小认证用例 |
| Calcite               | 不整体升级或替换 SQL 解析链             | 版本约束、SQL 解析用例、边界审计   |
| 数据源 / 脚本深层架构 | 不整体重构 provider / 方言 / 脚本运行时 | 配置解析、异常兜底、测试覆盖       |
| 内部命名与稳定标识    | 不做包名、配置前缀、迁移标识重构        | 仅继续收口对外品牌元数据           |

## 5. 下一步执行队列

### 5.1 当前分支继续推进

继续在 `codex/modernization-compatible-boundaries` 上累计，不切新分支，不合 `main`。

P0：

1. 收口 FormGenerator 交互规则内部 `value / Customize` 子链路
2. 处理 `CrossFilteringRuleList` 默认规则构造里的残留强转
3. 处理 `ViewDetailPanel` 自定义字段编辑边界
4. 处理 `JumpToUrl` URL 参数关系写回边界
5. 复扫 `ControllerList` / `UrlParamList` / `ChartRelationList` / `BoardRelationList` 的重复关系编辑结构

P1：

1. 复扫当前专题改动范围内的残留 `as any`、`@ts-ignore`、宽泛 `unknown` 消费点
2. 只处理同一协议边界内的问题，不扩散到图表运行时或 View/Source 其他模块
3. 当前批次达到提交点后提交并推送专题分支

P2：

1. 梳理 `ChartFilterCondition.value` 公共协议
2. 梳理 `ChartDataSetDTO.rows` 泛型边界
3. 选择已有测试覆盖的图表 helper 做下一批收口

### 5.2 进入下一专题的条件

满足以下条件后，再考虑合回 `main` 或切新专题：

- 当前 FormGenerator / 交互配置边界 P0 已完成
- 当前分支没有明显散落的同链路强转
- `npm run checkTs` 通过
- 相关 FormGenerator / Dashboard 定向测试通过
- 文档记录本批改动和验证

合回 `main` 前必须再跑完整门禁。

## 6. 执行方法

### 6.1 每轮开始检查

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

### 6.2 开发期验证

日常前端改造默认轻量验证：

```bash
npm run checkTs
npm run test:ci -- <related test files>
```

触发更强验证的条件：

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

### 6.3 提交与推送节奏

- 当前专题内可以多改一点再提交
- 不因单个小文件改动立即提交
- 每次提交前更新本文档的阶段记录和验证记录
- 专题分支可以推送远端
- 合回 `main` 使用 `git merge --no-ff <branch>`
- 推送 `main` 前不跳过完整门禁

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
