# yu-bi 现代化改造执行板

本文档是现代化改造的恢复入口和执行看板。它优先记录目标、边界、当前状态、下一步和验证策略；历史细节通过 git 追溯。

复盘时间：2026-06-20

## 1. 目标

现代化改造不追求所有技术栈都使用最新版本，而是在保证兼容、正确、可回归的前提下，把前后端核心技术栈和关键运行链路收口到较新的稳定状态。

硬性兼容目标：

- 后端兼容 `JDK 21`
- 前端兼容 `Node 24`

执行原则：

- 不直接在 `main` 开发；专题分支完成后 push，再 merge 回 `main`
- 当前专题分支持续累计前端兼容边界改造，减少分支创建和主线合并频率
- 尽量多改一点再提交，避免每个小改动都触发完整门禁
- 中高风险项可以推进，但必须拆成可验证子链路，不能无证据大迁移
- 每次阶段性提交前同步本文档的进度、风险和验证记录

固定禁止项：

- 不贸然改 Java 包名 `datart.*`
- 不贸然改配置前缀 `datart.*`
- 不贸然改 `DATART_*` 等内部技术符号
- 不贸然改数据迁移相关稳定常量、后缀、内部标识

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
| 当前专题分支 | `codex/modernization-chart-runtime` |
| 当前分支相对 `origin/main` | 新专题分支从 `origin/main` 最新合并点创建 |
| 最近主线合并 | `c67e2d2c7 chore: 合入现代化兼容边界批次` |
| 工作区 | 本文档提交后应恢复干净 |
| 远端 | `git@github.com:xybigdata/yu-bi.git` |

注意：

- `.tmp/`、`logs/` 已加入 `.gitignore`
- 专题分支可以推送远端
- 允许自动执行 `git add`、`git commit --no-verify`、`git push origin <branch>`
- 推送 `main` 前必须走完整门禁

## 3. 技术栈基线

### 3.1 后端

| 技术栈 | 当前基线 | 判断 |
| --- | --- | --- |
| Java | `21` | 已达硬性目标 |
| Maven | `>=3.9` | 已由 Enforcer 约束 |
| Spring Boot | `3.5.12` | 当前稳定主链 |
| Spring Cloud | `2025.0.1` | 与 Boot 3.5 配套 |
| MyBatis Spring Boot | `3.0.4` | 已适配 Boot 3 |
| GraalJS | `25.0.1` | 已替代 Nashorn 主链 |
| Springdoc | `2.8.17` | 已适配 Boot 3 |
| H2 | `2.4.240` | 已升级 |
| Selenium | `4.31.0` | 已升级 |
| Shiro | `2.0.5` | 高风险，只做可验证子问题 |
| Druid | `1.2.28` | 暂不优先动 |
| Calcite | 现网主链 | 高风险，先审计和补用例 |

### 3.2 前端

| 技术栈 | 当前基线 | 判断 |
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
| monaco-editor | `0.52.2` | 真实运行时依赖 |
| reveal.js | `6.0.1` | 真实运行时依赖 |
| ECharts | `5.3.1` | 后续中风险专题 |
| AntV S2 | `2.7.2 / 2.3.1` | 后续中风险专题 |
| react-window | `1.8.6` | 使用面窄，先稳定边界 |

## 4. 阶段复盘

### 4.1 主链已建立

- 项目已从 datart 独立为 `yu-bi`
- 治理文档、README、NOTICE、SECURITY、ROADMAP、CHANGELOG、issue template 已收口为独立开源项目表述
- GitHub 新仓库、默认分支、远端和主线门禁已切到 `yu-bi / main`
- 后端已建立 `JDK 21 + Spring Boot 3.5.x + Spring Cloud 2025.0.x` 主链
- 前端已建立 `Node 24 + React 18 + Ant Design 5 + Vite 6 + Vitest 4` 主链
- CRA / CRACO、IE11 主兼容链、Nashorn、PhantomJS 等历史主链已退出
- 安装包闭环验证已打通，`yu-bi-server-*.zip` 可解压并通过 demo 健康检查脚本验证

### 4.2 当前分支已完成

| 批次 | 结果 |
| --- | --- |
| FormGenerator 交互配置边界 | 泛型、context、配置行守卫、规则写回完成收口 |
| FormGenerator 关系编辑边界 | `relationUtils`、关系数组增删改、事件切换问题完成修正 |
| FormGenerator 交互规则构造 | ViewDetail / Jump 规则构造函数和语义测试完成 |
| FormGenerator 交互规则回调 | `InteractionRuleChange` 泛型回调协议完成收口 |
| `ChartFilterCondition.value` 协议 | 筛选值联合协议、关系值守卫、筛选 UI 消费点完成归一化 |
| `ChartDataSetCellValue` 协议 | 公共数据集单元格协议、helper、双轴图表试点完成 |
| 普通图表数据集标量协议 | 普通图表内部 `IChartDataSet<string>` 假设完成迁移 |
| 表格 / 透视表数据集协议 | 生产代码旧数据集泛型入口完成清理 |
| 数据集泛型残留清理 | helper 注释和测试旧泛型断言完成清理 |
| 图表筛选值协议残留清理 | ControllerPanel 和 ChartWorkbench 明显筛选值断言完成清理 |
| 前端时间调用残留清理 | 生产代码和相关测试裸时间调用残留完成清理 |

最近验证已通过：

```bash
npm run checkTs
npm run test:ci -- src/app/utils/__tests__/date.test.ts src/app/utils/__tests__/time.test.ts src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartTimeSelector/__tests__/utils.test.ts src/app/pages/MainPage/pages/VizPage/ChartPreview/components/ControllerPanel/components/__tests__/timeFilterUtils.test.ts
git diff --check
```

```bash
npm run checkTs
npm run test:ci -- src/app/models/__tests__/ChartFilterCondition.test.ts src/app/pages/MainPage/pages/VizPage/ChartPreview/components/ControllerPanel/components/__tests__/filterValueUtils.test.ts src/app/pages/MainPage/pages/VizPage/ChartPreview/components/ControllerPanel/components/__tests__/timeFilterUtils.test.ts
git diff --check
```

## 5. 下一步执行队列

### 5.1 P0：当前分支收尾审计

状态：已完成。

目标：判断 `codex/modernization-compatible-boundaries` 是否已经可以进入完整门禁和合并准备。

执行项：

- 已复扫 Ant Design / rc 生态深路径类型入口
- 已复扫前端公开类型入口，避免依赖包内部路径回退
- 已在 Node 24 / npm 11 下复扫安装健康度
- 已复扫 Maven 坐标和展示元数据中的 `datart` 残留，只处理品牌 / 发布元数据，不碰 Java 包名和配置前缀
- 已更新本文档的审计结果

已执行命令：

```bash
node -v
npm -v
npm install --package-lock-only --dry-run --ignore-scripts
npm ci --dry-run --ignore-scripts
rg -n "from ['\"](?:antd/es|antd/lib|rc-[^'\"]+/(?:es|lib)|@ant-design/[^'\"]+/(?:es|lib))|import\(['\"](?:antd/es|antd/lib|rc-[^'\"]+/(?:es|lib)|@ant-design/[^'\"]+/(?:es|lib))" frontend/src frontend -g '*.ts' -g '*.tsx'
rg -n "datart" pom.xml core security data-providers server -g 'pom.xml'
npm run checkTs
git diff --check
```

结论：

- 未发现明显 `antd/es`、`antd/lib`、`rc-*/es`、`rc-*/lib` 深路径类型入口
- 本机验证环境为 `node v24.16.0`、`npm 11.13.0`
- `frontend/.nvmrc` 已是 `v24.0.0`
- `frontend/package.json` 已约束 `node >=24.0.0`、`npm >=11.0.0`
- `frontend/package-lock.json` 仍是 `lockfileVersion: 2`，但 `npm install --package-lock-only --dry-run --ignore-scripts` 和 `npm ci --dry-run --ignore-scripts` 在 npm 11 下均通过，暂不无意义改写 lockfile
- 顶层和子模块 POM 的 `groupId`、`artifactId`、`name`、`description` 已收口到 `yu-bi`
- POM 中唯一 `datart` 命中为 `server/pom.xml` 的 `mainClass=datart.DatartServerApplication`，这是 Java 包名启动入口，按固定禁止项保留，不作为品牌残留处理
- `npm run checkTs` 已通过

### 5.2 P1：完整门禁与合并准备

状态：已完成。

上一专题分支已累计较多前端兼容边界改造。P0 收尾审计已完成，不再继续无限追加低价值小改动，应准备完整门禁和合并。

前端完整门禁：

```bash
npm run checkTs
npm run test:ci
npm run lint:css
npm run lint:style
```

验证结果：

- `npm run checkTs` 已通过
- `npm run test:ci` 已通过，结果为 130 个测试文件通过、907 个测试通过、4 个跳过
- `npm run lint:css` 已通过
- `npm run lint:style` 已通过

如果 P0 改到 Maven、后端依赖或安装包，再补：

```bash
mvn test
mvn package -DskipTests
```

合并流程：

```bash
git push origin codex/modernization-compatible-boundaries
git checkout main
git merge --no-ff codex/modernization-compatible-boundaries
git push origin main
```

合并结果：

- 已通过 `--no-ff` 合并到 `main`
- 合并提交：`c67e2d2c7 chore: 合入现代化兼容边界批次`
- `main` 已推送到 `origin/main`
- 推送前和 push hook 均已通过完整前端门禁

### 5.3 P2：当前专题

当前专题分支：`codex/modernization-chart-runtime`

专题目标：评估并渐进推进图表运行时依赖现代化，优先从 ECharts 运行时开始。先补审计和 smoke test，再决定是否升级依赖版本；不做无验证的大版本跳跃。

优先级如下：

| 优先级 | 专题 | 风险 | 处理策略 |
| --- | --- | --- | --- |
| P2-A | ECharts / 图表运行时依赖升级评估 | 中 | 先做版本审计、图表 smoke test、关键 helper 用例 |
| P2-B | AntV S2 / 透视表链路升级评估 | 中 | 先补透视表最小运行时用例，再升级 |
| P2-C | 前端安装健康度和依赖漂移治理 | 低 | Node 24 下复核 install、lockfile、engines、CI 版本 |
| P2-D | Maven / 发布元数据品牌残留 | 低到中 | 仅改 POM 坐标、描述、包名展示和文档，不改 Java 包名 |
| P2-E | 后端 Shiro / Calcite 健康度审计 | 高 | 只做用例和兼容边界，不直接整体替换 |

当前第一步：

1. 审计 `echarts`、`echarts-wordcloud`、图表组件和测试覆盖现状
2. 复核当前 `echarts` 版本在 Node 24 / Vite 6 / Vitest 4 下的运行时边界
3. 补足低成本 smoke test 后，再决定是否升级到更高的 ECharts 5.x 稳定版

## 6. 风险分层

### 6.1 可继续推进

- 前端公开类型入口清理
- 时间工具、筛选值、数据集值协议边界清理
- POM / README / 发布脚本中的 yu-bi 展示元数据收口
- Node 24 / npm 11 安装健康度复核
- `.gitignore`、CI、文档类低风险治理

### 6.2 需要先补验证

- `ChartDataSetDTO.rows` 全局协议切换
- ECharts 主版本或关键运行时升级
- AntV S2 / 透视表运行时升级
- react-window、react-grid-layout、flexlayout-react 等布局运行时升级
- Dashboard widget 内容协议继续收口
- Maven 打包、Docker、安装包和 demo 启动链路调整

### 6.3 暂不做整体重构

- Java 包名 `datart.*`
- 配置前缀 `datart.*`
- `DATART_*`
- 数据迁移稳定标识
- Shiro 认证授权整体替换
- Calcite SQL 解析主链整体替换
- 数据源 provider / 方言 / 脚本运行时的大规模重构

## 7. 门禁策略

开发期按风险分层验证，不为每个小改动跑完整门禁：

| 场景 | 最低门禁 |
| --- | --- |
| 文档或纯元数据 | `git diff --check` |
| 前端类型边界、小范围组件迁移 | `npm run checkTs` + 相关测试 |
| helper、模型、共享协议变化 | `npm run checkTs` + 相关模型 / helper 测试 |
| 依赖、构建配置、运行时加载变化 | `npm run checkTs` + `npm run test:ci` |
| 准备 merge 回 `main` | 前端完整门禁，必要时补后端门禁 |
| 推送 `main` | 不跳过完整门禁 |

测试缺口处理：

- 找不到现成相关测试时，必须记录缺口
- 优先补 helper / 协议 / 运行时最小用例
- 不为了覆盖率硬造低价值快照测试

## 8. 提交节奏

- 同一专题内累计一组相关改动后再提交
- 不因单个小文件改动立即提交
- 文档复盘可以随当前批次一起提交
- 中风险运行时链路按可验证子链路独立提交
- 依赖、构建、安装包链路独立提交，并跑更强验证

建议提交粒度：

| 类型 | 粒度 |
| --- | --- |
| 低风险类型边界 | 累计 3 到 10 个相关文件后提交 |
| 中风险运行时链路 | 每条可验证链路独立提交 |
| 依赖和构建链路 | 独立提交 |
| 阶段复盘 | 跟随当前批次提交或单独文档提交 |

## 9. 历史追溯

需要追溯具体实现时使用：

```bash
git log --oneline -- docs/tech-stack-modernization-plan.md
git log --oneline -- frontend/src/app/components/FormGenerator
git log --oneline -- frontend/src/app/components/ChartGraph
git log --oneline -- frontend/src/app/pages/DashBoardPage
git log --oneline -- frontend/src/app/pages/MainPage/pages/VizPage
```

关键合并点：

- `b519a24cd chore: 合入 Dashboard widget 内容协议边界批次`
- `484c44fd9 chore: 合入图表运行时类型边界批次`
