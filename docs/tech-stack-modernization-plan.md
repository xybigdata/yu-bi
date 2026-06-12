# yu-bi 技术栈现代化改造计划

本文档是 yu-bi 技术栈现代化改造的执行底稿，只保留当前有效的信息：

- 改造目标是什么
- 当前基线是什么
- 哪些事情已经完成
- 哪些事情还要做
- 下一步按什么顺序推进

不再继续堆叠逐轮流水账。每一轮完成后，只更新状态、证据和下一步。

## 1. 改造目标

现代化改造不追求“全部升到最新”，而是在兼容、正确、可回归的前提下，收口到较新的稳定版本。

本项目当前目标：

- 后端必须兼容 `JDK 21`
- 前端工程链必须兼容 `Node 26`
- 默认开发和构建基线保持在稳定 LTS 主线
- 不做高风险内部命名重构
- 不把多个高风险专题混在同一批改动里

明确不在本轮贸然调整的内容：

- Java 包名 `datart.*`
- 配置前缀 `datart.*`
- `DATART_*` 等内部技术符号
- 数据迁移相关稳定常量、后缀、内部标识

## 2. 当前约束

### 2.1 分支与提交流程

- `main` 只允许 merge，不直接开发
- 当前工作分支：`codex/modernization-vite-vitest`
- 后续改造按专题分批提交
- 默认自动 `git add`、`git commit --no-verify`

### 2.2 工作区边界

- 工作目录：`/Users/chencongyu/WorkHome/VSProjects/open-project/yu-bi`
- 不动未跟踪目录：
  - `.tmp/`
  - `logs/`

### 2.3 当前工作区要求

- 除正在处理的单一专题外，工作区应尽量保持干净
- `.tmp/` 和 `logs/` 保持未跟踪，不纳入改造提交
- 每一轮改造只提交与当前专题直接相关的文件

## 3. 当前技术栈基线

### 3.1 后端基线

| 项目 | 当前基线 | 说明 |
| --- | --- | --- |
| Java | `21` | 根 POM 已锁定 |
| Spring Boot | `3.5.12` | 已进入稳定主线 |
| Spring Cloud | `2025.0.1` | 已进入当前兼容主线 |
| Maven | `3.9+` | Enforcer 已限制最低版本 |
| MyBatis Spring Boot | `3.0.4` | 已适配 Boot 3 |
| MySQL 驱动 | `com.mysql:mysql-connector-j` | 坐标已收口 |
| JWT | `jjwt 0.12.7` | 已替换旧版链路 |
| HTTP 客户端 | `httpclient5 5.5` | 已完成主链替换 |
| H2 | `2.4.240` | demo/test 基线已升级 |
| POI | `5.5.1` | 已进入较新稳定线 |
| Commons CSV | `1.14.1` | 已升级 |
| Selenium | `4.31.0` | 已退出 PhantomJS 主链 |
| 脚本引擎 | `GraalJS 25.0.1` | 已替换 Nashorn 主链 |

### 3.2 前端基线

| 项目 | 当前基线 | 说明 |
| --- | --- | --- |
| Node | `>=24.0.0` | 默认基线；需兼容 Node 26 |
| npm | `>=11.0.0` | 已写入 engines |
| React | `18.3.1` | 已完成主升级 |
| React Router | `6.30.1` | 已进入 Router 6 主线 |
| Ant Design | `5.26.2` | 已完成主升级 |
| Redux Toolkit | `2.12.0` | 已完成主升级 |
| React Redux | `9.3.0` | 已完成主升级 |
| TypeScript | `5.9.3` | 当前稳定线 |
| Vite | `6.4.3` | 已替代 CRA 主工作流 |
| Vitest | `4.1.8` | 已成为唯一主测试栈 |
| styled-components | `6.1.19` | 已完成主升级 |
| i18next | `26.0.2` | 已升级 |
| react-i18next | `17.0.8` | 已升级 |
| react-quill | `2.0.0` | 已升级，但仍属专题保留项 |

### 3.3 工程化基线

| 项目 | 当前基线 | 说明 |
| --- | --- | --- |
| Git hooks | `husky 9` | 已升级 |
| staged 校验 | `lint-staged 17` | 已升级 |
| 提交规范 | `commitlint 21` | 已升级 |
| CI Node 矩阵 | `24.x` + `26.x` | GitHub Actions 已覆盖 |
| CI Java | `21` | GitHub Actions 已覆盖 |
| Docker 运行时 | `eclipse-temurin:21-jre` | 与 JDK 21 对齐 |
| 前端 Node 版本文件 | `frontend/.nvmrc = v24.0.0` | 默认本地基线 |

## 4. 已完成的现代化改造成果

这里只保留对后续执行仍有意义的结果，不再按提交轮次展开流水账。

### 4.1 项目独立化与品牌收口

已完成：

- 仓库已从 datart 独立为 `yu-bi`
- `README`、`README_zh`、`ROADMAP`、`MAINTAINERS`、`SECURITY`、`CHANGELOG`、`NOTICE` 已收口
- 前端标题、manifest、分享页、部分国际化文案、issue template 已切到 yu-bi 品牌

### 4.2 后端主链现代化

已完成：

- JDK 21 基线建立
- Spring Boot 3.5.x / Spring Cloud 2025.0.x 收口
- MyBatis Spring Boot 3 主线适配
- MySQL 驱动切到 `com.mysql:mysql-connector-j`
- `jjwt` 升级到 `0.12.7`
- Apache HttpClient 升级到 `5.5`
- `fastjson` 主运行时退出，Web 层 JSON 主链回收至 Jackson
- H2 升级到 `2.x`
- Apache POI、Commons CSV、AspectJ 等基础库升级
- Selenium 4 主链建立，PhantomJS 已退出主运行时
- GraalJS 主链建立，Nashorn 已退出主运行时
- HikariCP 已成为主连接池路线

### 4.3 前端主链现代化

已完成：

- CRA / CRACO 已退出前端主工作流
- Vite 6 成为主构建链
- Vitest 成为唯一主测试栈
- React 18、React Router 6、Redux Toolkit 2、React Redux 9 已落地
- Ant Design 5 已落地
- styled-components 6 已落地
- IE11 主运行时兼容链已退出
- 多轮页面与图表运行时按需加载已完成，重点包括：
  - MonacoEditor
  - StoryBoard / reveal.js
  - 富文本运行时
  - PivotSheet / S2
  - WordCloud
  - ECharts 图表与 ChartManager 注册链

### 4.4 前端依赖清单收口

已完成：

- 多个“历史直接声明但已不再直接消费”的依赖已清理
- 多个大体积运行时已改为按需加载
- 当前前端依赖治理方向已经明确：只保留仓库源码真实直接消费的依赖
- `@antv/g2` 已从前端直接依赖中移除，继续由 `@antv/s2` / `@antv/s2-react` 作为传递依赖提供
- `html2canvas` 已从前端直接依赖中移除，继续由 `@antv/s2 -> @antv/g` 作为传递依赖提供

## 5. 当前遗留问题分层

这一节只保留仍影响后续改造顺序的内容。

### 5.1 低风险，可继续推进

这类任务可以继续分批做，小步提交。

| 项目 | 当前情况 | 建议动作 |
| --- | --- | --- |
| 前端直接依赖收口 | 仍有少量“未直接使用但仍声明”的依赖 | 继续逐个检索、删除、构建验证 |
| 前端深路径类型导入 | 仍存在 `antd/es/*`、`antd/lib/*` 等历史导入 | 改为公共导出入口或更稳定的类型入口 |
| AntD 历史 Less 变量链 | `frontend/src/styles/antd/variables.less` 仍使用 `~antd/lib/style/*` | 评估迁到 token / CSS variables，减少历史 Less 耦合 |
| 本地安装脚本 | `npm install --legacy-peer-deps` 仍保留在 `bootstrap` | 评估是否可以移除，避免掩盖依赖问题 |
| 文档与验证记录 | 之前过于流水账 | 改为按状态维护，避免继续膨胀 |

### 5.2 中风险，需要专项验证

这类任务可以做，但不能和别的高耦合专题混在一起。

| 项目 | 当前情况 | 风险点 | 建议顺序 |
| --- | --- | --- | --- |
| 时间体系最终收口 | 主链已切到 `dayjs`，但仍需确认页面值链完整性 | 日期控件、格式化、时区回归 | 优先级中 |
| 富文本专题 | `react-quill 2.0.0` 已在用，但自定义 compat 层仍较重 | 自定义 blot、只读态、编辑态、分享态回归 | 优先级中 |
| Node 24/26 双线稳定性 | CI 已覆盖，但本地与 hooks 行为仍需持续观察 | lint/test/build 与不同 Node 行为差异 | 持续回归 |
| Docker/安装包闭环 | 配置已收口，但镜像级实测证据不足 | 安装包结构、静态资源复制、启动链 | 优先级中 |
| Ant Design 5 稳定化 | 主升级已完成，但还留有历史 API 与样式兼容边角 | 弹层、表单、菜单、主题回归 | 结合触达渐进处理 |

### 5.3 高风险，暂不直接动

这些项必须单独立项，当前阶段只做调研、拆障、补证据。

| 项目 | 当前情况 | 原因 |
| --- | --- | --- |
| Shiro 2 -> Spring Security 全量接管 | 仍存在完整 Shiro 运行链 | 涉及登录、权限、分享、remember-me、OAuth2 |
| Calcite 升级或替换 | `calcite-core 1.26.0` 深度耦合 SQL 解析与 JDBC provider | 影响 SQL parser、函数、脚本渲染 |
| 数据源/脚本深层架构收口 | Provider、脚本解析、方言处理耦合深 | 需要独立设计和回归矩阵 |
| 内部命名与稳定标识重构 | 明确禁止贸然调整 | 影响面不可控 |

## 6. 当前判断：哪些栈还算“偏老”

这里的“偏老”不是简单看版本号，而是看维护状态、兼容性和长期维护成本。

### 6.1 优先关注

- `Shiro 2`
  - 不是马上替换，但已经是后端安全栈里最主要的长期保留项
- `Calcite 1.26.0`
  - 版本明显偏老，且强耦合，是后端侧最典型的高风险遗留栈
- `react-quill` 兼容层
  - 版本已不算旧，但局部实现仍偏历史化，维护成本高
- AntD 历史 Less / 深路径导入
  - 版本不旧，接入方式偏旧
- `bootstrap` 脚本里的 `--legacy-peer-deps`
  - 不是依赖版本问题，而是工程化健康度问题

### 6.2 已进入较新稳定线，可先不追更高版本

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
- jjwt 0.12.7
- HttpClient 5.5
- H2 2.4.x
- POI 5.5.x
- Commons CSV 1.14.x

结论：这些栈当前重点应放在稳定化和使用面收口，不应为了“看起来更新”继续盲目追版本。

## 7. 下一批建议执行顺序

后续按这个顺序推进，避免主题扩散。

### Wave A：继续做低风险前端依赖收口

目标：

- 清理未直接使用的前端直接依赖
- 保证 `checkTs`、`build`、`build:task`、`test:ci` 持续通过

当前第一项：

- 继续筛查剩余“未直接使用但仍声明”的前端依赖，并逐项独立提交

### Wave B：收口前端历史接入方式

目标：

- 压缩 `antd/es/*`、`antd/lib/*` 深路径导入
- 评估并缩减 AntD 历史 Less 变量链
- 检查 `styled-components/cssprop`、`react-app-env.d.ts` 等历史壳是否还能进一步收口

当前进展：

- 已完成一小批 Ant Design 深路径类型与 locale 导入收口，优先改为 `antd` 公共导出和 `antd/locale/*` 入口
- 已删除未接入当前构建链的 Ant Design 历史 Less 变量残留文件

### Wave C：做中风险稳定化专题

按顺序建议：

1. 时间体系回归收尾
2. 富文本专题稳定化
3. Docker / 安装包闭环补证据

### Wave D：高风险专题只做预研

按顺序建议：

1. Shiro -> Spring Security 迁移设计
2. Calcite 升级/替代预研

这两项在没有完整迁移设计和专项回归矩阵前，不进入实质替换。

## 8. 每一轮改造的固定门禁

每次提交都要满足以下规则：

### 8.1 范围规则

- 一个提交只处理一个专题
- 低风险与高风险不混提
- 文档更新只记录当前仍有效的结论

### 8.2 验证规则

前端专题至少执行：

- `npm run checkTs`
- `npm run build` 或 `npm run build:all`
- `npm run test:ci`

后端专题至少执行：

- `mvn -pl server -am -DskipTests package`
- 必要时补模块级测试

工程化或版本基线专题至少检查：

- CI 配置
- Dockerfile / 安装包链
- 本地脚本与 engines / Enforcer 是否一致

### 8.3 提交规则

- 默认直接在当前专题分支提交
- 不直接改 `main`
- 到达里程碑再 push
- commit message 使用中文

## 9. 当前里程碑状态

### 已完成

- 项目独立化与 yu-bi 品牌基础收口
- JDK 21 / Node 24+26 / Vite 6 / Vitest 4 / React 18 / AntD 5 等主链现代化
- 后端主要基础库和前端主工作流现代化

### 进行中

- 前端剩余直接依赖清单收口
- 前端历史接入方式收口
- 中风险专题的稳定化回归

### 暂不启动实做

- Shiro 全量迁移
- Calcite 升级或替代
- 高风险内部命名重构

## 10. 本文档的维护方式

后续只按下面方式更新本文档：

- 基线变化了，更新“当前技术栈基线”
- 某个专题完成了，更新“已完成的现代化改造成果”
- 优先级变化了，更新“遗留问题分层”和“下一批建议执行顺序”
- 不再新增长篇历史过程记录

这样文档才能持续作为改造执行板使用，而不是变成第二份提交日志。
