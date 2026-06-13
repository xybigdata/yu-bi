# yu-bi 技术栈现代化改造执行板

本文档只保留当前有效结论，用于指导后续逐批改造。

现代化的含义是：在兼容、正确、可回归前提下，收口到较新的稳定版，而不是盲目追最新。

## 1. 改造目标

- 后端稳定兼容 `JDK 21`
- 前端工程链稳定兼容 `Node 26`
- 优先收口到较新的稳定版本，不追逐最新版本号
- 所有改造都必须分批、可验证、可回退
- 一个提交只处理一个专题，不混入无关改动

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

- 不动未跟踪目录：
  - `.tmp/`
  - `logs/`
- 仅提交当前专题相关文件
- 文档只记录当前有效状态，不写过程流水账

## 3. 当前基线

### 3.1 后端基线

| 项目 | 当前基线 | 结论 |
| --- | --- | --- |
| Java | `21` | 已达目标 |
| Spring Boot | `3.5.12` | 稳定主线，暂不追更 |
| Spring Cloud | `2025.0.1` | 与 Boot 3.5 配套 |
| Maven | `3.9+` | 已由 Enforcer 限制 |
| MyBatis Spring Boot | `3.0.4` | 已适配 Boot 3 |
| Shiro | `2.0.5` | 明确高风险遗留 |
| Druid | `1.2.28` | 暂不优先动 |
| H2 | `2.4.240` | 已进入新主线 |
| Selenium | `4.31.0` | 已完成主升级 |
| GraalJS | `25.0.1` | 已替代 Nashorn 主链 |
| Springdoc | `2.8.17` | 已适配 Boot 3 |

补充：

- `jjwt 0.12.7`、`httpclient5 5.5`、`poi-ooxml 5.5.1`、`commons-csv 1.14.1` 已收口到较新稳定线
- `calcite-core` 仍在主链，是明确高风险项

### 3.2 前端基线

| 项目 | 当前基线 | 结论 |
| --- | --- | --- |
| Node | `>=24.0.0` | 本地默认基线；必须兼容 `Node 26` |
| npm | `>=11.0.0` | 已写入 `engines` |
| React | `18.3.1` | 已完成主升级 |
| React Router | `6.30.1` | 已完成主升级 |
| Ant Design | `5.26.2` | 已完成主升级 |
| Redux Toolkit | `2.12.0` | 已完成主升级 |
| React Redux | `9.3.0` | 已完成主升级 |
| TypeScript | `5.9.3` | 稳定主线 |
| Vite | `6.4.3` | 已替代 CRA 主工作流 |
| Vitest | `4.1.8` | 已成为唯一主测试栈 |
| styled-components | `6.1.19` | 已完成主升级 |
| react-quill | `2.0.0` | 已升级，但仍需稳定化 |
| react-resizable | `3.0.4` | 已完成收口 |

### 3.3 工程化基线

| 项目 | 当前基线 | 结论 |
| --- | --- | --- |
| Husky | `9.1.7` | 已升级 |
| lint-staged | `17.0.7` | 已升级 |
| commitlint | `21.0.2` | 已升级 |
| Docker 运行时 | `eclipse-temurin:21-jre` | 与 JDK 21 对齐 |
| CI Java | `21` | 已覆盖 |
| CI Node | `24.x` + `26.x` | 已覆盖 |
| `frontend/.nvmrc` | `v24.0.0` | 本地默认基线仍是 24 |

## 4. 当前已完成

### 4.1 主线结论

- 项目已从 datart 独立为 `yu-bi`
- 核心开源治理文档已完成独立项目收口
- 前端品牌基础露出已切到 `yu-bi`
- 后端已建立 `JDK 21 + Spring Boot 3.5.x` 主链
- 前端已建立 `React 18 + Ant Design 5 + Vite 6 + Vitest 4` 主链
- CRA / CRACO、IE11 主兼容链、Nashorn、PhantomJS 等历史主链已退出

### 4.2 近期里程碑

- `7507ffe22 docs: 重整现代化改造执行板`
- `502c3a61f docs: 补充前端安装健康度审计结论`
- `2d840cadc chore: 收口 react-resizable 到 3.x`
- `05a398439 chore: 收口一批时间选择器日期链路`
- `e5758ff2f chore: 继续收口变量页日期格式化入口`
- `d78d2522c chore: 继续收口控制器日期格式化入口`

更早已完成：

- `29b46197c chore: 移除前端未使用的 @antv/g2 直接依赖`
- `e9a42dfae chore: 移除前端未使用的 html2canvas 直接依赖`
- `2e6c1d8a5 chore: 收口一批 Ant Design 深路径导入`
- `9de952f11 chore: 继续收口 Ant Design 深路径类型导入`
- `3c395c940 chore: 删除未使用的 Ant Design Less 变量残留`
- `2477307d6 chore: 收口 RangeNumber 的 Ant Design 内部类型依赖`
- `7aa70431a docs: 收口现代化改造执行计划`
- `00861e059 chore: 继续收口 Ant Design 类型推导入口`
- `1a21e7893 chore: 收口一批 Ant Design 历史类型入口`
- `492193acc chore: 收口 SelectController 的类型入口`
- `5f538da70 chore: 移除前端安装脚本的 legacy-peer-deps`
- `c9e022d4a chore: 收口一批时间值标准化入口`

## 5. 风险分层与处理策略

### 5.1 低风险：持续推进

| 专题 | 当前判断 | 策略 |
| --- | --- | --- |
| 时间值标准化 | 已有统一工具入口，仍有零散调用点 | 继续小批量收口 |
| 前端安装健康度 | 历史残留问题已识别，生态还需观察 | 继续审计 peer 与锁文件健康度 |
| 前端依赖收口 | 已清掉部分未使用依赖 | 按证据继续删减 |
| Ant Design 历史入口 | 仅剩少量深路径或历史类型入口 | 不追求一次性清空，逐点收口 |

### 5.2 中风险：做专项稳定化，不做重写

| 专题 | 当前判断 | 主要风险 |
| --- | --- | --- |
| 时间体系最终收口 | 主链已切到 `dayjs` | 日期控件、格式化、时区回归 |
| 富文本兼容层 | `react-quill 2.0.0` 已在用，但兼容层仍偏重 | 编辑态、只读态、分享态回归 |
| Node 24 / 26 双线稳定性 | CI 已覆盖 | 本地安装、hooks、构建脚本差异 |
| Docker / 安装包闭环 | 版本基线已收口 | 安装包结构、静态资源、启动链验证 |
| Ant Design 5 稳定化 | 主升级已完成 | 表单、弹层、菜单、主题回归 |

### 5.3 高风险：暂不直接动

| 专题 | 当前判断 | 原因 |
| --- | --- | --- |
| Shiro 2 -> Spring Security | 暂不进入实质迁移 | 登录、权限、分享、remember-me、OAuth2 影响面大 |
| Calcite 升级或替换 | 暂不进入实质替换 | SQL 解析与 provider 体系深耦合 |
| 数据源 / 脚本深层架构收口 | 暂不直接动 | 方言、脚本、解析链耦合深 |
| 内部命名与稳定标识重构 | 明确禁止 | 影响面不可控 |

## 6. 当前有效结论

### 6.1 前端安装健康度

已确认：

- `npm prune` 可以清掉 `frontend/node_modules` 中的历史残留包
- 大批 `extraneous` 主要来自历史安装目录残留，不是 `package.json` 失真
- 当前主要关注点已回落为少量“源码仍在使用，但 peer 声明偏旧”的依赖

当前依赖判断：

| 依赖 | 判断 | 策略 |
| --- | --- | --- |
| `flexlayout-react` | 源码仍直接使用，锁文件解析较旧 | 先保留，后续做替代预研 |
| `react-resizable` | 已验证与当前源码兼容 | 已完成收口到 `3.0.4` |
| `react-window` | 接入面较窄，但 peer 声明老旧 | 后续单独评估升级或替代 |
| `react-dev-inspector` | 仅开发态使用，历史包袱重 | 倾向后续替换或移除 |

### 6.2 时间体系收口

已完成三批收口，方向明确：

- 减少 `datartDayjs(...).format(...)` 的分散调用
- 减少 `formatTime(datartDayjs(...), ...)` 的重复链路
- 减少“先格式化字符串再回转 dayjs”的路径
- 优先收口到 `formatDatartDate(...)` 与 `toDatartDayjs(...)`

当前状态：

- 时间选择器、变量页、控制器相关链路已完成一批收口
- 控制器与时间选择器中的 `value` 回填已继续向 `toDatartDayjs(...)` 收口
- 一批 `DatePicker` / `RangePicker` 的时间值类型断言已缩小到局部范围
- 展示型时间格式化入口已继续向 `formatDatartDate(...)` 收口
- 变量默认值与权限值中的日期回填已继续向 `toDatartDayjs(...)` 收口，并过滤无效值
- 每批均已通过前端门禁

### 6.3 富文本专题

当前判断：

- `react-quill 2.0.0` 版本本身先不动
- 下一步不是重写富文本，而是做兼容层稳定化小修
- 优先减少显式 `any`、非空断言和过重的历史兼容写法
- 不改 Quill blot / calcfield 业务逻辑

当前状态：

- 已完成第一步兼容层稳定化小修
- `RichTextEditor` / `RichTextEditorRuntime` 已补齐运行时未就绪保护
- `quillCompat.ts` 已集中承接富文本事件、`calcfield` 模块与键盘扩展类型
- `RichTextPluginLoader` 已把 `container` / `keyboard` 相关 `any` 收口到局部扩展类型
- 当前改动已通过 `npm run checkTs`、`npm run build:all`、`npm run test:ci -- --silent`

重点文件：

- `frontend/src/app/components/ChartGraph/BasicRichText/quillCompat.ts`
- `frontend/src/app/components/ChartGraph/BasicRichText/RichTextEditor.tsx`
- `frontend/src/app/components/ChartGraph/BasicRichText/RichTextEditorRuntime.tsx`
- `frontend/src/app/components/ChartGraph/BasicRichText/RichTextPluginLoader/index.ts`
- `frontend/src/app/components/ChartGraph/BasicRichText/runtime.ts`
- `frontend/src/app/components/ChartGraph/BasicRichText/ChartRichTextAdapter.tsx`
- `frontend/src/app/pages/DashBoardPage/components/Widgets/RichTextWidget/RichTextWidgetCore.tsx`

## 7. 当前执行顺序

按这个顺序推进，避免专题扩散：

1. 富文本兼容层稳定化审计与小修
2. 时间体系剩余调用点继续收口
3. `react-window` / `react-dev-inspector` 处置评估

## 8. 每轮固定门禁

### 8.1 范围规则

- 一个提交只处理一个专题
- 低风险与高风险不混提
- 当前专题之外的改动不顺手带上

### 8.2 验证规则

前端专题至少执行：

- `npm run checkTs`
- `npm run build` 或 `npm run build:all`
- `npm run test:ci -- --silent`

后端专题至少执行：

- `mvn -pl server -am -DskipTests package`
- 必要时补模块级测试

工程化专题至少检查：

- `engines` / `.nvmrc` / Maven Enforcer 是否一致
- CI 配置是否覆盖目标版本
- Dockerfile / 安装包链是否仍匹配当前基线

### 8.3 提交规则

- 默认直接在当前专题分支提交
- 不直接改 `main`
- 到达里程碑再 push
- commit message 使用中文

## 9. 文档维护规则

后续只在以下情况更新本文档：

- 技术栈基线变化
- 某个专题完成
- 当前执行顺序变化
- 风险判断发生变化

除此之外，不记录过程流水账。
