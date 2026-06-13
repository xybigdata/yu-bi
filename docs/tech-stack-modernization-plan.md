# yu-bi 技术栈现代化改造执行板

本文档只保留当前有效信息，用来指导后续逐批改造。

这里的“现代化”指的是：在兼容、正确、可回归前提下，收口到较新的稳定版本，而不是盲目追最新。

## 1. 改造目标

- 后端稳定兼容 `JDK 21`
- 前端工程链稳定兼容 `Node 24`
- CI 同时覆盖 `Node 24.x` 与 `Node 26.x`
- 改造按专题拆分，做到可验证、可回退、可持续推进

## 2. 固定边界

### 2.1 仓库与分支

- 工作目录：`/Users/chencongyu/WorkHome/VSProjects/open-project/yu-bi`
- `main` 只允许 merge，不直接开发
- 当前执行分支：`codex/modernization-vite-vitest`
- 默认自动 `git add`、`git commit --no-verify`

### 2.2 不直接触碰的高风险标识

- Java 包名 `datart.*`
- 配置前缀 `datart.*`
- `DATART_*` 等内部技术符号
- 数据迁移相关稳定常量、后缀、内部标识

### 2.3 工作区边界

- 不动未跟踪目录：`.tmp/`、`logs/`
- 一个提交只处理一个专题
- 只提交与当前专题直接相关的文件

## 3. 当前基线

### 3.1 后端

| 项目 | 当前基线 | 判断 |
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
| Shiro | `2.0.5` | 高风险遗留 |
| Druid | `1.2.28` | 暂不优先动 |
| Calcite | 现网主链依赖 | 高风险遗留 |

补充：

- `jjwt`、`httpclient5`、`poi-ooxml`、`commons-csv` 已在较新稳定线
- 后端当前重点是兼容验证与依赖健康维护，不做结构性迁移

### 3.2 前端

| 项目 | 当前基线 | 判断 |
| --- | --- | --- |
| Node | `>=24.0.0` | 本地默认基线 |
| `frontend/.nvmrc` | `v24.0.0` | 与本地基线一致 |
| npm | `>=11.0.0` | 已写入 `engines` |
| React | `18.3.1` | 已完成主升级 |
| React Router | `6.30.1` | 已完成主升级 |
| Ant Design | `5.26.2` | 已完成主升级 |
| Redux Toolkit | `2.12.0` | 已完成主升级 |
| React Redux | `9.3.0` | 已完成主升级 |
| TypeScript | `5.9.3` | 当前稳定主线 |
| Vite | `6.4.3` | 已替代 CRA 主工作流 |
| Vitest | `4.1.8` | 已成为主测试栈 |
| styled-components | `6.1.19` | 已完成主升级 |
| react-quill | `2.0.0` | 已升级，仍需稳定化 |
| react-window | `1.8.6` | 仍在真实运行时链路 |
| flexlayout-react | `0.5.12` | 版本偏旧，但仍在主链 |
| react-grid-layout | `1.2.4` | 版本偏旧，但仍在主链 |
| monaco-editor | `0.52.2` | 真实运行时依赖 |
| reveal.js | `6.0.1` | 真实运行时依赖 |

补充：

- 当前前端主链已经是 `React 18 + Ant Design 5 + Vite 6 + Vitest 4`
- `react-dev-inspector` 已从开发态接入中移除

### 3.3 工程化

| 项目 | 当前基线 | 判断 |
| --- | --- | --- |
| Husky | `9.1.7` | 已升级 |
| lint-staged | `17.0.7` | 已升级 |
| commitlint | `21.0.2` | 已升级 |
| Docker 运行时 | `eclipse-temurin:21-jre` | 与 JDK 21 对齐 |
| 前端 CI | `Node 24.x` + `26.x` | 已覆盖 |
| Java CI | `JDK 21` | 已覆盖 |

## 4. 已完成的有效成果

这里只保留仍会影响后续判断的结果。

### 4.1 主链已完成

- 项目已从 datart 独立为 `yu-bi`
- 品牌、治理文档、基础工程信息已完成独立化收口
- 后端已建立 `JDK 21 + Spring Boot 3.5.x` 主链
- 前端已建立 `React 18 + Ant Design 5 + Vite 6 + Vitest 4` 主链
- CRA / CRACO、IE11 主兼容链、Nashorn、PhantomJS 等历史主链已退出

### 4.2 近期已完成专题

- 前端安装健康度审计与锁文件收口
- 未使用前端依赖清理：`@antv/g2`、`html2canvas`
- `react-resizable` 收口到 `3.0.4`
- Ant Design 深路径导入与历史类型入口收口
- 富文本兼容层第一批稳定化小修
- 富文本兼容层第二批稳定化小修：
  - `RichTextEditor` 的 `onChange` 类型边界收紧
  - `ChartRichTextAdapter` 的弱类型参数与运行时保护收口
  - `BasicRichText` 的 `getOnChange()` 返回类型收口
- 时间体系多批次收口：
  - 时间选择器回填链路
  - 展示与表单日期回填链路
  - 通用时间格式化入口
  - 图表时间值解析入口
  - 图表时间序列化入口
  - 区间时间值转换入口
  - 日期数组回填入口
- `react-window` 运行时包装边界收口
- `react-dev-inspector` 开发态接入移除

### 4.3 近期里程碑提交

- `d1fd4be02` `chore: 移除 react-dev-inspector 开发态接入`
- `9dc21fde6` `docs: 重整现代化改造执行板`
- `5e540c4d5` `chore: 继续收口图表时间值解析入口`
- `b65768725` `chore: 继续收口图表时间序列化入口`
- `10817c1fa` `chore: 收口区间时间值转换入口`
- `203db201d` `chore: 收口日期数组回填入口`
- `9a157b164` `chore: 收口 react-window 运行时包装边界`

## 5. 风险分层

### 5.1 低风险：优先持续推进

| 专题 | 当前判断 | 策略 |
| --- | --- | --- |
| 时间体系剩余调用点 | 已有统一工具入口，仍有零散调用 | 继续小批量收口 |
| 前端依赖收口 | 仍有少量历史依赖可继续审计 | 按证据逐个清理 |
| Ant Design 历史入口 | 只剩局部残留 | 按调用点逐步消化 |
| 安装健康度维护 | 当前已稳定，但需防止锁文件回退 | 持续关注 Node 24 / 26 安装表现 |

### 5.2 中风险：做专项稳定化，不做重写

| 专题 | 当前判断 | 主要风险 |
| --- | --- | --- |
| 富文本兼容层 | `react-quill 2` 已在用，但兼容层仍偏重 | 编辑态、只读态、分享态回归 |
| 时间体系最终收口 | 主链已切到 `dayjs` | 日期控件、格式化、时区回归 |
| `react-window` | 接入面窄，但处在真实运行时链路 | 虚拟表格渲染与滚动行为回归 |
| `flexlayout-react` | 版本偏旧，仍在工作台布局主链 | 布局拖拽、面板状态、样式回归 |
| `react-grid-layout` | 版本偏旧，仍在看板布局主链 | 拖拽缩放、响应式布局回归 |
| Docker / 安装包闭环 | 基线已收口 | 安装包结构、静态资源、启动链验证 |

### 5.3 高风险：暂不直接动

| 专题 | 当前判断 | 原因 |
| --- | --- | --- |
| Shiro 迁移 | 暂不进入实质迁移 | 认证授权影响面大 |
| Calcite 升级或替换 | 暂不进入实质替换 | SQL 解析链耦合深 |
| 数据源 / 脚本深层架构调整 | 暂不直接动 | provider、脚本、方言耦合深 |
| 内部命名与稳定标识重构 | 明确禁止 | 影响面不可控 |

## 6. 当前进行中专题

当前工作区正在推进的下一批仍是低到中风险交界处的小范围稳定化，不进入结构性替换。

当前最近完成的是“富文本兼容层第二批稳定化小修”，已通过：

- `npm run checkTs`
- `npm run build:all`
- `npm run test:ci -- --silent`

因此当前进行中专题应顺延到下一批，而不是继续停留在富文本第二批。

## 7. 下一阶段执行顺序

按这个顺序推进，避免专题扩散：

1. 时间体系剩余调用点继续收口
2. `react-window` 专项审计，确认是继续小修还是进入替代预研
3. `flexlayout-react` / `react-grid-layout` 使用面盘点与风险评估
4. Docker / 安装包闭环验证

## 8. 每轮固定门禁

### 8.1 范围规则

- 一个提交只处理一个专题
- 低风险与高风险不混提
- 当前专题之外的改动不顺手带上

### 8.2 验证规则

前端专题至少执行：

- `npm run checkTs`
- `npm run build:all`
- `npm run test:ci -- --silent`

后端专题至少执行：

- `mvn -pl server -am -DskipTests package`
- 必要时补模块级测试

工程化专题至少检查：

- `engines` / `.nvmrc` / Maven Enforcer 是否一致
- CI 是否覆盖目标运行时
- Dockerfile / 安装包链是否仍匹配当前基线

### 8.3 提交规则

- 默认直接在当前专题分支提交
- 不直接改 `main`
