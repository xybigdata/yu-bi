# yu-bi 技术栈现代化改造执行板

本文档只保留当前有效结论，用来指导后续逐批改造。

现代化的含义是：在兼容、正确、可回归前提下，收口到较新的稳定版，而不是盲目追最新。

## 1. 固定目标

- 后端兼容 `JDK 21`
- 前端工程链兼容 `Node 26`
- 默认开发基线保持稳定 LTS 主线
- 只做分批、可验证、可回退的升级
- 一个提交只处理一个专题，不把高低风险内容混在一起

## 2. 固定约束

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
- 文档只记录当前有效状态，不恢复成长流水账

## 3. 当前技术栈基线

### 3.1 后端

| 项目 | 当前基线 | 判断 |
| --- | --- | --- |
| Java | `21` | 已达目标基线 |
| Spring Boot | `3.5.12` | 稳定主线，暂不追更 |
| Spring Cloud | `2025.0.1` | 与 Boot 3.5 配套 |
| Maven | `3.9+` | 已由 Enforcer 限制 |
| MyBatis Spring Boot | `3.0.4` | 已适配 Boot 3 |
| Shiro | `2.0.5` | 仍是高风险遗留 |
| Druid | `1.2.28` | 可继续观察，但不优先动 |
| H2 | `2.4.240` | 已进入新主线 |
| Selenium | `4.31.0` | 已完成主升级 |
| GraalJS | `25.0.1` | 已替代 Nashorn 主链 |
| Springdoc | `2.8.17` | 已适配 Boot 3 |
| Commons Lang3 | `3.20.0` | 稳定 |
| Commons IO | `2.22.0` | 稳定 |

补充：

- `jjwt 0.12.7`、`httpclient5 5.5`、`poi-ooxml 5.5.1`、`commons-csv 1.14.1` 已收口到较新稳定线
- `calcite-core` 仍在 `data-providers/data-provider-base` 主链，是明确高风险项

### 3.2 前端

| 项目 | 当前基线 | 判断 |
| --- | --- | --- |
| Node | `>=24.0.0` | 默认本地基线；必须保持对 `Node 26` 兼容 |
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
| i18next | `26.0.2` | 已升级 |
| react-i18next | `17.0.8` | 已升级 |
| react-quill | `2.0.0` | 已升级，但仍属中风险稳定化专题 |

### 3.3 工程化

| 项目 | 当前基线 | 判断 |
| --- | --- | --- |
| Husky | `9.1.7` | 已升级 |
| lint-staged | `17.0.7` | 已升级 |
| commitlint | `21.0.2` | 已升级 |
| Docker 运行时 | `eclipse-temurin:21-jre` | 与 JDK 21 对齐 |
| CI Java | `21` | 已覆盖 |
| CI Node | `24.x` + `26.x` | 已覆盖 |
| `frontend/.nvmrc` | `v24.0.0` | 本地默认基线仍是 24 |

## 4. 当前进度总览

### 4.1 已完成的大项

- 项目已从 datart 独立为 `yu-bi`
- 核心开源治理文档已完成独立项目收口
- 前端品牌基础露出已切到 `yu-bi`
- 后端已建立 `JDK 21 + Spring Boot 3.5.x` 主链
- 前端已建立 `React 18 + Ant Design 5 + Vite 6 + Vitest 4` 主链
- CRA / CRACO、IE11 主兼容链、Nashorn、PhantomJS 等历史主链已退出

### 4.2 已完成的近期现代化里程碑

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
- `502c3a61f docs: 补充前端安装健康度审计结论`

## 5. 当前结论

### 5.1 已明确不再作为重点的方向

这些栈已经在较新稳定线，当前重点应是稳定化和使用面收口，而不是继续追版本号：

- Spring Boot 3.5.x
- Spring Cloud 2025.0.x
- React 18
- React Router 6
- Ant Design 5
- Redux Toolkit 2
- React Redux 9
- TypeScript 5.9
- Vite 6
- Vitest 4
- styled-components 6

### 5.2 当前仍需关注的遗留点

#### 低风险，可持续分批推进

| 项目 | 当前判断 | 处理策略 |
| --- | --- | --- |
| 时间值标准化 | 已建立统一工具入口，但仍有零散调用点 | 继续小批量收口 |
| 前端安装健康度 | `bootstrap` 已去掉 `legacy-peer-deps`，但安装生态还需观察 | 继续审计 peer 兼容与历史残留 |
| 前端直接依赖收口 | 已清掉部分未使用依赖 | 按证据继续删减 |
| Ant Design 历史深路径 | 前端源码仅剩 `SliderFilter.tsx` 一处 `antd/es/slider` | 先保留，避免类型退化 |

#### 中风险，需要专项验证

| 项目 | 当前判断 | 主要风险 |
| --- | --- | --- |
| 时间体系最终收口 | 主链已切到 `dayjs` | 日期控件、格式化、时区回归 |
| 富文本专题 | `react-quill 2.0.0` 已在用，但兼容层仍重 | 编辑态、只读态、分享态回归 |
| Node 24 / 26 双线稳定性 | CI 已覆盖 | 本地安装、hooks、构建脚本差异 |
| Docker / 安装包闭环 | 版本基线已收口 | 安装包结构、静态资源、启动链验证 |
| Ant Design 5 稳定化 | 主升级已完成 | 表单、弹层、菜单、主题边角回归 |

#### 高风险，暂不直接动

| 项目 | 当前判断 | 原因 |
| --- | --- | --- |
| Shiro 2 -> Spring Security | 暂不进入实质迁移 | 登录、权限、分享、remember-me、OAuth2 影响面大 |
| Calcite 升级或替换 | 暂不进入实质替换 | SQL 解析与 provider 体系深耦合 |
| 数据源 / 脚本深层架构收口 | 暂不直接动 | 方言、脚本、解析链牵一发动全身 |
| 内部命名与稳定标识重构 | 明确禁止 | 影响面不可控 |

## 6. 当前执行面

### 6.1 当前工作区状态

截至本次整理：

- 当前分支：`codex/modernization-vite-vitest`
- 工作区干净
- 仅保留未跟踪目录 `.tmp/`、`logs/`

### 6.2 当前批次已完成

当前批次已完成并提交：

- 时间值标准化入口第一轮收口
- 新增统一日期格式化入口 `formatDatartDate`
- 几处分散的 `datartDayjs(... as any)`、`toDate()`、旧式格式化调用已回收到工具层

### 6.3 下一批优先顺序

按这个顺序继续，避免主题扩散：

1. 时间体系剩余调用点继续收口
2. 富文本兼容层稳定化审计
3. `react-window` / `react-dev-inspector` 后续处置评估

## 7. Wave A 审计结论

### 7.1 安装健康度结论

本轮已确认：

- `npm prune` 可以清掉当前 `frontend/node_modules` 中的历史残留包
- 清理后，`npm ls --all --omit=optional` 不再出现大批 `extraneous`
- 当前安装树的主要关注点，已回落为少量“源码仍在使用，但 peer 声明偏旧”的依赖

结论：

- 现阶段前端安装健康度的主要问题，不是 `package.json` 失真
- 主要是历史安装目录残留，以及少数旧包对 React 18 的 peer 声明滞后

### 7.2 候选依赖处置策略

| 依赖 | 当前判断 | 处置策略 |
| --- | --- | --- |
| `flexlayout-react` | 源码仍直接使用；锁文件当前解析到 `0.5.21`；仍属旧代布局库 | 先保留，后续做兼容与替代预研，不在本轮贸然更换 |
| `react-resizable` | 当前源码用法与 `3.0.4` 兼容，现有前端门禁已验证通过 | 已完成从 `1.11.1` 到 `3.0.4` 的依赖收口 |
| `react-window` | 仅在虚拟表格运行时使用，接入面较窄，但 React peer 声明老旧 | 先保留，后续单独评估升级或替代，不与当前批次混做 |
| `react-dev-inspector` | 仅开发态按需加载；当前库自身仍带明显 webpack / umi 历史包袱 | 倾向后续替换或移除，不作为生产运行时问题处理 |

## 8. 下一批执行清单

### Wave A：前端安装健康度

目标：

- 识别当前 `node_modules` 健康问题的主要来源
- 判断哪些是历史残留，哪些是仍需保留但版本偏旧的依赖

优先核查：

- `flexlayout-react`
- `react-resizable`
- `react-window`
- `react-dev-inspector`

完成标准：

- 给出每个包的现状判断：保留、升级、替换、暂缓
- 不做无证据的大改

当前状态：

- `react-resizable` 已完成从 `1.11.1` 到 `3.0.4` 的依赖收口
- 运行证据：
  - `npm run checkTs`
  - `npm run build:all`
  - `npm run test:ci -- --silent`
  均已通过

### Wave B：时间体系继续收口

目标：

- 继续清理零散的日期值转换和格式化入口
- 不改业务语义，只收口调用方式

重点检索：

- `datartDayjs(... as any)`
- `toDate()`
- 分散的日期字符串回转

完成标准：

- 每批改动都能独立通过前端门禁

当前状态：

- 已收口一批时间选择器与控制器中的重复转换链路
- 重点去掉了“先格式化字符串再回转 dayjs”的重复路径

### Wave C：富文本专题稳定化

目标：

- 审计 `react-quill 2.0.0` 当前兼容层是否还有明显历史负担
- 优先做证据充分的小修，不做重写

重点关注：

- `quillCompat.ts`
- 编辑态 / 只读态 / 分享态是否仍有分叉兼容逻辑

## 9. 每轮固定门禁

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

## 10. 文档维护规则

后续只在四种情况下更新本文档：

- 技术栈基线变化
- 某个专题完成
- 当前批次切换
- 风险判断发生变化

除此之外，不记录过程流水账。
