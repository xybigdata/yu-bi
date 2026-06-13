# yu-bi 技术栈现代化改造计划

本文档只保留当前有效的信息，作为现代化改造执行板使用。

目标是：在兼容、正确、可回归的前提下，把 yu-bi 收口到较新的稳定技术栈，而不是盲目追最新。

## 1. 改造目标

本轮现代化改造的固定目标：

- 后端兼容 `JDK 21`
- 前端工程链兼容 `Node 26`
- 默认开发基线保持在稳定 LTS 主线
- 只做分批、可验证、可回退的升级
- 不把多个高耦合专题混在同一批提交里

本轮明确不直接触碰：

- Java 包名 `datart.*`
- 配置前缀 `datart.*`
- `DATART_*` 等内部技术符号
- 数据迁移相关稳定常量、后缀、内部标识

## 2. 当前约束

### 2.1 仓库与分支

- 工作目录：`/Users/chencongyu/WorkHome/VSProjects/open-project/yu-bi`
- `main` 只允许 merge，不直接开发
- 当前工作分支：`codex/modernization-vite-vitest`
- 默认自动 `git add`、`git commit --no-verify`

### 2.2 工作区边界

- 不动未跟踪目录：
  - `.tmp/`
  - `logs/`
- 每次只提交当前专题相关文件
- 除当前专题外，工作区应尽量保持干净

### 2.3 当前工作区状态

当前仍有一批未提交的低风险前端改动，属于上轮收尾项：

- `frontend/src/app/pages/StoryBoardPage/components/StoryPageAddModal.tsx`
- `frontend/src/app/pages/DashBoardPage/pages/BoardEditor/components/BoardToolBar/context/BoardToolBarContext.ts`

两处都属于 Ant Design 历史类型导入收口，尚未完成最终验证与提交。

## 3. 当前技术栈基线

### 3.1 后端基线

| 项目 | 当前基线 | 说明 |
| --- | --- | --- |
| Java | `21` | 根 POM 已锁定 |
| Spring Boot | `3.5.12` | 当前稳定主线 |
| Spring Cloud | `2025.0.1` | 当前兼容主线 |
| Maven | `3.9+` | Enforcer 已限制最低版本 |
| MyBatis Spring Boot | `3.0.4` | 已适配 Boot 3 |
| Shiro | `2.0.5` | 仍在主认证链 |
| H2 | `2.4.240` | demo / test 基线 |
| Selenium | `4.31.0` | 已退出 PhantomJS 主链 |
| GraalJS | `25.0.1` | 已替换 Nashorn 主链 |
| Commons Lang3 | `3.20.0` | 当前根依赖版本 |
| Commons IO | `2.22.0` | 由 dependencyManagement 管理 |
| Springdoc | `2.8.17` | 已适配 Boot 3 |
| Druid | `1.2.28` | 仍在使用 |

补充说明：

- `jjwt 0.12.7`、`httpclient5 5.5`、`poi-ooxml 5.5.1`、`commons-csv 1.14.1` 已进入较新稳定线
- `calcite-core` 仍保留在 `data-providers/data-provider-base` 主链，是高风险遗留项

### 3.2 前端基线

| 项目 | 当前基线 | 说明 |
| --- | --- | --- |
| Node | `>=24.0.0` | 默认本地基线；需兼容 Node 26 |
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
| react-quill | `2.0.0` | 已升级，但仍属中风险专题 |

### 3.3 工程化基线

| 项目 | 当前基线 | 说明 |
| --- | --- | --- |
| Husky | `9.1.7` | Git hooks 已升级 |
| lint-staged | `17.0.7` | staged 校验已升级 |
| commitlint | `21.0.2` | 提交规范链已升级 |
| CI Node 矩阵 | `24.x` + `26.x` | GitHub Actions 已覆盖 |
| CI Java | `21` | GitHub Actions 已覆盖 |
| Docker 运行时 | `eclipse-temurin:21-jre` | 与 JDK 21 对齐 |
| frontend/.nvmrc | `v24.0.0` | 默认本地 Node 基线 |

## 4. 已完成里程碑

这里只保留对后续执行仍有参考价值的成果。

### 4.1 项目独立化与品牌收口

已完成：

- 仓库已从 datart 独立为 `yu-bi`
- `README`、`README_zh`、`ROADMAP`、`MAINTAINERS`、`SECURITY`、`CHANGELOG`、`NOTICE` 已完成项目表述收口
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
- Selenium 4 主链建立
- GraalJS 主链建立
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
- 多轮页面与图表运行时按需加载已完成，覆盖 Monaco、StoryBoard、富文本运行时、PivotSheet / S2、WordCloud、ECharts 注册链

### 4.4 前端依赖与接入方式收口

已完成：

- `@antv/g2` 前端直接依赖已移除
- `html2canvas` 前端直接依赖已移除
- 一批 `antd/locale/*` 与历史深路径类型导入已完成收口
- 未接入当前构建链的 Ant Design 历史 Less 变量残留文件已删除

最近已完成并提交的里程碑：

- `c00573a69 docs: 重整现代化改造计划文档`
- `29b46197c chore: 移除前端未使用的 @antv/g2 直接依赖`
- `e9a42dfae chore: 移除前端未使用的 html2canvas 直接依赖`
- `2e6c1d8a5 chore: 收口一批 Ant Design 深路径导入`
- `9de952f11 chore: 继续收口 Ant Design 深路径类型导入`
- `3c395c940 chore: 删除未使用的 Ant Design Less 变量残留`
- `2477307d6 chore: 收口 RangeNumber 的 Ant Design 内部类型依赖`
- `00861e059 chore: 继续收口 Ant Design 类型推导入口`
- `1a21e7893 chore: 收口一批 Ant Design 历史类型入口`
- `492193acc chore: 收口 SelectController 的类型入口`
- `5f538da70 chore: 移除前端安装脚本的 legacy-peer-deps`

## 5. 当前遗留项分层

### 5.1 低风险，继续推进

这类任务可以继续分批做，小步提交。

| 项目 | 当前情况 | 建议动作 |
| --- | --- | --- |
| 前端剩余深路径类型导入 | 仅剩 `SliderFilter` 中一处 `antd/es/slider` | 保留精确 range 类型，避免退化为联合类型 |
| 前端直接依赖收口 | 仍需持续确认是否存在“声明但未直接消费”的依赖 | 逐项检索、删除、构建验证 |
| 本地安装脚本 | `bootstrap` 已验证可去掉 `--legacy-peer-deps` | 收口为普通 `npm install`，继续观察安装稳定性 |
| 时间值标准化 | 仍有少量 `as any` / `toDate()` / 分散格式化入口 | 继续收口到统一日期工具层 |
| 文档状态同步 | 当前文档已收口，但需要保持与仓库进度同步 | 每轮只更新状态和下一步 |

当前已知剩余 Ant Design 历史类型入口：

- `antd/es/slider`

### 5.2 中风险，需要专项验证

| 项目 | 当前情况 | 风险点 | 建议顺序 |
| --- | --- | --- | --- |
| 时间体系最终收口 | 主链已切到 `dayjs`，但仍需确认页面值链完整性 | 日期控件、格式化、时区回归 | 优先级中 |
| 富文本专题 | `react-quill 2.0.0` 已在用，但兼容层仍较重 | 自定义 blot、只读态、编辑态、分享态回归 | 优先级中 |
| Node 24 / 26 双线稳定性 | CI 已覆盖，但仍需持续观察本地和 hooks 行为 | lint / test / build 在不同 Node 版本下的差异 | 持续回归 |
| Docker / 安装包闭环 | 配置已收口，但镜像级与安装包级实测证据不足 | 安装包结构、静态资源复制、启动链 | 优先级中 |
| Ant Design 5 稳定化 | 主升级已完成，但仍有历史 API 和样式兼容边角 | 弹层、表单、菜单、主题回归 | 结合触达渐进处理 |

### 5.3 高风险，暂不直接动

| 项目 | 当前情况 | 原因 |
| --- | --- | --- |
| Shiro 2 -> Spring Security 全量接管 | 仍存在完整 Shiro 运行链 | 牵涉登录、权限、分享、remember-me、OAuth2 |
| Calcite 升级或替换 | `calcite-core` 仍深度耦合 SQL 解析与 provider 体系 | 影响 SQL parser、函数、脚本渲染 |
| 数据源 / 脚本深层架构收口 | Provider、脚本解析、方言处理耦合深 | 需要独立设计与回归矩阵 |
| 内部命名与稳定标识重构 | 当前明确禁止贸然调整 | 影响面不可控 |

## 6. 当前批次

当前批次主题：

- 收口一批时间值标准化入口

当前已改未提：

1. `frontend/src/app/utils/date.ts`
   - 新增 `formatDatartDate`，统一日期值格式化入口
2. 日期调用点收口
   - `Variables.tsx`
   - `SchedulePage/utils.ts`
   - `ControllerWidgetPanel/utils.ts`
   - `DashBoardPage/utils/widget.ts`
   - `ShareLinkModal.tsx`
3. 收口目标
   - 减少 `datartDayjs(x as any)`、`current.toDate()` 和分散格式化逻辑

当前批次门禁：

- `npm run checkTs`
- `npm run build:all`
- `npm run test:ci -- --silent`

当前批次完成标准：

- 上述三项验证通过
- 仅提交当前专题相关文件
- 不引入新的深路径回退

## 7. 下一批执行顺序

按以下顺序推进，避免主题扩散。

### Wave A：收尾当前批次

目标：

- 提交时间值标准化的这一小批低风险收口
- 保持时间体系回归在小步、可验证范围内

### Wave B：继续收口前端历史入口

目标：

- 保留 `SliderFilter` 里唯一剩余的 `antd/es/slider` 精确 range 类型
- 转向前端直接依赖与安装健康度的持续收口

候选项：

- 清理安装目录中的历史残留包
- 持续确认是否还有“声明但未直接消费”的直接依赖

### Wave C：中风险稳定化专题

按顺序建议：

1. 时间体系回归收尾
2. 富文本专题稳定化
3. Docker / 安装包闭环补证据

### Wave D：高风险专题仅做预研

按顺序建议：

1. Shiro -> Spring Security 迁移设计
2. Calcite 升级 / 替代预研

在没有完整迁移设计和专项回归矩阵前，不进入实质替换。

## 8. 每轮固定门禁

### 8.1 范围规则

- 一个提交只处理一个专题
- 低风险与高风险不混提
- 文档只记录当前有效结论，不恢复成长流水账

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

## 9. 当前判断：哪些栈值得继续关注

优先关注但不等于立即替换：

- `Shiro 2.0.5`
- `calcite-core`
- `react-quill` 兼容层
- Ant Design 历史深路径导入与历史接入方式
- `bootstrap` 脚本的安装稳定性

当前已在较新稳定线、暂不追更高版本：

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
- `jjwt 0.12.7`
- `httpclient5 5.5`
- `h2 2.4.x`
- `poi 5.5.x`
- `commons-csv 1.14.x`

结论：

这些栈当前重点应放在稳定化和使用面收口，而不是继续为了“更高版本号”做无收益升级。

## 10. 文档维护方式

后续只按下面方式更新本文档：

- 基线变化了，更新“当前技术栈基线”
- 某个专题完成了，更新“已完成里程碑”
- 当前批次变了，更新“当前批次”和“下一批执行顺序”
- 风险判断变了，更新“遗留项分层”

不再新增长篇过程记录。
