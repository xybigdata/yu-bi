# yu-bi 技术栈现代化进度看板

## 当前状态

- 当前分支：`fix/local-package-spa-entry`（本地安装包验收修复，未提交/未合入）
- 基线状态：第一轮现代化（`refactor/tech-modernization`）已合入远程 `main`
- 阶段状态：**阶段 A + B + C 全部完成**
- 任务进度：39/39 必需任务已完成；5 个 optional property tests 待补充
- 硬目标：兼容 JDK 21、Node.js 24，不以降低运行时版本作为兼容旧代码的默认方案
- 本机验证环境：Temurin JDK `21.0.11`，Maven `3.9.16`，Node.js `24.18.0`，npm `11.16.0`

## 改造成果总览

| 改造项 | 状态 | 说明 |
|--------|------|------|
| Javassist 消除 | ✅ 已验证 | `YuBiSqlPrettyWriter` 子类覆盖，移除 `--add-opens` |
| Spring Boot 4.0.7 | ✅ 已验证 | 含 Spring Cloud 2025.1.2 + MyBatis 4.0.0 + Springdoc 3.0.3 |
| Shiro → Spring Security | ✅ 已验证 | Shiro 运行时依赖移除；装配器已重命名为 Default*；Spring Security 7 请求级授权已恢复 Shiro 通配权限语义 |
| Jackson 3 完整迁移 | ✅ 已验证 | 业务代码全部迁至 `tools.jackson.*`；注解保持 `com.fasterxml.jackson.annotation.*`（Jackson 3 设计如此）；仅 jjwt-jackson 传递依赖保留 Jackson 2 |
| Ant Design 6 | ✅ 已验证 | AntD 6.4.5 + pro-components 3.1.12-0 |
| React 现代化 | ✅ 已验证 | 8 个 class component 全部迁移为函数组件 + hooks；PropTypes 移除 |
| TypeScript 7 评估 | ✅ 已完成 | 项目代码 0 错误，生态阻塞(@typescript-eslint)，保持 6.0.3 |
| 前端工具链升级 | ✅ 已验证 | monaco-editor 0.55.1 + @vitejs/plugin-react 6.0.3 + fast-check 4.x |
| 发布链路 | ✅ 已验证 | 安装包完整、demo health 通过、无遗留 Javassist/Shiro JAR |
| 本地安装包验收修复 | 🔄 进行中 | SPA 深链、语言切换、侧栏 Split、账号菜单、退出登录跳转、demo 账号成员权限、主应用深链路由矩阵、成员侧栏状态、权限页视觉间距、权限表列宽、数据视图 SQL 请求、Monaco 失败态与计算字段 DQL 降级、右侧属性面板间距、BasicTable 默认列宽、角色添加成员弹窗、demo H2 成员角色保存、FILE 上传路径、数据视图结果表可调列宽、字段操作弹窗取消关闭、筛选弹窗布局已完成修复，当前补充验证中 |
| Dependabot 漏洞修复 | ✅ 已验证 | DOMPurify 3.4.11 直接依赖 + override 已落地；Quill 2.0.3 当前无不降级修复版，已对历史 HTML 富文本增加 DOMPurify 清理并作为上游跟踪风险记录 |

## 当前组件版本矩阵（改造后）

### 后端核心

| 组件 | 改造前版本 | 改造后版本 | 备注 |
|------|-----------|-----------|------|
| JDK | 21 | **21** | Temurin 21.0.11，未变 |
| Maven | 3.9.x | **3.9.16** | enforcer 要求 ≥3.9，未变 |
| Spring Boot | 3.5.15 | **4.0.7** | 预计于 2026-06-30 EOL → 已升级至 4.0.7 patch |
| Spring Cloud | 2025.0.3 | **2025.1.2** | 与 Boot 4 兼容线 |
| MyBatis Spring Boot | 3.0.5 | **4.0.0** | 随 Boot 4 升级 |
| Springdoc | 2.8.17 | **3.0.3** | OpenAPI spec 不变 |
| Jackson | 2.x (`com.fasterxml.jackson`) | **3.x** (`tools.jackson`) | Boot 主栈 Jackson 3；注解保持 `com.fasterxml.jackson.annotation.*`（Jackson 3 设计如此）；仅 jjwt-jackson 传递依赖保留 Jackson 2 |
| Security | Shiro 2.0.6 | **Spring Security 7** (via Boot 4) | Shiro 运行时依赖移除；装配器已重命名为 Default* 并迁至 `yubi.security.manager`；权限检查懒加载 `AuthorizationAssembler` 并支持 owner 通配权限 |
| Javassist | 3.32.0-GA | **deleted** | `--add-opens` 同步移除 |
| Calcite | 1.42.0 | **1.42.0** | 方言修复，版本未变 |

### 前端核心

| 组件 | 改造前版本 | 改造后版本 | 备注 |
|------|-----------|-----------|------|
| Node.js | 24.x | **24.18.0** | engines 要求 ≥24.0.0，未变 |
| npm | 11.x | **11.16.0** | engines 要求 ≥11.0.0，未变 |
| React | 19.x | **19.2.7** | 未变 |
| Vite | 8.x | **8.1.0** | 未变 |
| TypeScript | 6.0.3 | **6.0.3** | TS 7 已评估，等待 @typescript-eslint 生态 |
| Ant Design | 5.29.3 | **6.4.5** | CSS Variable 模式 |
| Pro Components | 2.8.10 | **3.1.12-0** | beta 阶段，需监控 stable |
| react-router-dom | 7.x | **7.18.0** | 未变 |
| @reduxjs/toolkit | 2.x | **2.12.0** | 未变 |
| ECharts | 6.x | **6.1.0** | 未变 |
| monaco-editor | 0.52.x | **0.55.1** | 0.52.2 → 0.55.1 升级 |
| DOMPurify | 3.2.7 | **3.4.11** | 通过直接依赖 + overrides 收敛 Monaco 间接依赖安全告警 |
| Quill | 2.0.3 | **2.0.3** | 当前无不降级修复版；保留版本并通过历史 HTML sanitize 缓解 |

### Lint / Format / Test

| 组件 | 改造前版本 | 改造后版本 | 备注 |
|------|-----------|-----------|------|
| ESLint | 9.x | **9.39.4** | flat config，未变 |
| Prettier | 3.x | **3.9.1** | 未变 |
| Stylelint | 17.x | **17.14.0** | 未变 |
| Vitest | 4.x | **4.1.9** | 未变 |

### 容器 / 部署

| 组件 | 改造前版本 | 改造后版本 | 备注 |
|------|-----------|-----------|------|
| Docker base image | eclipse-temurin:21-jre | **eclipse-temurin:21-jre** | 未变 |
| Tomcat (embedded) | 11.x | **11.0.14** | 随 Boot 4 升级 |

## 分阶段改造路线

### ✅ 阶段 0：全仓 Review 与目标基线设计

- [x] 后端/前端/构建/部署/测试全面盘点
- [x] 目标架构和版本矩阵设计
- [x] 分阶段路线和验证标准制定

### ✅ 阶段 1：验证闭环与构建链路收敛

- [x] 构建体积 baseline 修正（task bundle 纳入统计）
- [x] README 技术基线漂移修正
- [x] 安装包 assembly 生命周期顺序修复
- [x] 静态资源目录隔离（避免历史 hash 残留）
- [x] Java-only / Frontend-only / Full package / Demo health 验证闭环

### ✅ 阶段 A：后端现代化

- [x] Javassist 消除：`YuBiSqlPrettyWriter` 子类覆盖 `keyword()` 方法
- [x] `--add-opens` 移除：启动脚本、CMD、Dockerfile、health check 脚本
- [x] Spring Boot 4.0.7 升级 + Spring Cloud 2025.1.2 + MyBatis 4.0.0 + Springdoc 3.0.3
- [x] Jackson 3 完整迁移：业务代码全部迁至 `tools.jackson.*`；注解保持 `com.fasterxml.jackson.annotation.*`（Jackson 3 设计如此）
- [x] OAuth2 class 重定位适配
- [x] Shiro 运行时依赖移除（shiro-spring-boot-web-starter + SecurityConfiguration + 5 个 Shiro 实现类删除）
- [x] Spring Security 实现：`SpringSecurityManager` + `SpringSecuritySubjectFacade` + `YuBiAuthenticationProvider`
- [x] SecurityFilterChain 配置：stateless + permitAll/authenticated 分层
- [x] JWT Filter 重写：直接 SecurityContextHolder
- [x] LoginInterceptor 删除 + `@SkipLogin` 注解清理
- [x] Calcite 方言修复：避免内建方言覆盖

### ✅ 阶段 B：前端现代化

- [x] Ant Design 5 → 6.4.5 迁移（14 文件 codemod）
- [x] Pro Components 2.8.10 → 3.1.12-0 升级
- [x] CSS 选择器修复（`.ant-card-body` → `.ant-pro-card-body` 等）
- [x] `rc-table` → `@rc-component/table` 适配
- [x] TypeScript 7 评估：零项目代码错误 → 生态阻塞 → 保持 6.0.3

### ✅ 阶段 C：发布链路和容器稳定化

- [x] 安装包 `yu-bi-server-1.0.0-rc.3-install.zip` 生成
- [x] 包内容校验：static/index.html、task/index.js、parser.js
- [x] 无 Javassist JAR、无 Shiro JAR 确认
- [x] 无 `--add-opens` 确认
- [x] Demo health：Tomcat 11.0.14 启动，API 返回 success

## 遗留 Optional 任务

以下 5 个 optional property tests 不阻塞合并，可后续补充：

1. Spring Security filter chain property test
2. JWT authentication flow property test
3. AntD 6 prop migration property test
4. Build artifact integrity property test
5. Jackson 3 serialization compatibility property test

## 遗留技术债

| 项目 | 说明 | 优先级 | 状态 |
|------|------|--------|------|
| ~~Shiro 命名装配器重命名~~ | `ShiroAuthenticationAssembler`/`ShiroAuthorizationAssembler` 仍在 `yubi.security.manager.shiro` 包下且为 @Component，功能已由 Spring Security 接管但命名未收敛 | 中 | ✅ 已完成 |
| ~~Jackson 2 API 残留~~ | 14+ 业务/数据提供器文件仍使用 `com.fasterxml.jackson.*`，core/pom.xml 仍有 Jackson 2 annotations 直接依赖 | 中 | ✅ 已完成 |

## 风险清单

| 风险 | 影响 | 状态 |
|------|------|------|
| ~~Spring Boot 3.5 预计于 2026-06-30 EOL~~ | ~~不再收到安全补丁~~ | ✅ 已解决：迁移到 Boot 4.0.7 |
| ~~Javassist patch + `--add-opens`~~ | ~~JDK 21 强封装兼容~~ | ✅ 已解决：子类覆盖替换 |
| ~~Shiro 社区活跃度下降~~ | ~~长期安全维护风险~~ | ✅ 已解决：运行时依赖移除，迁移到 Spring Security |
| ~~AntD 5 接近 EOL~~ | ~~组件库不再收到新功能~~ | ✅ 已解决：迁移到 AntD 6.4.5 |
| pro-components 3.x 仍为 beta | npm latest tag 仍为 2.8.10 | 🔍 监控中：持续关注 stable 发布 |
| `@typescript-eslint` 尚未支持 TS 7 | 阻塞 TypeScript 7 升级 | 🔍 监控中：等待兼容版本发布 |
| Jackson 2/3 共存 | 仅 jjwt-jackson 传递依赖保留 Jackson 2 运行时 | ✅ 已解决（注解命名空间为 Jackson 3 设计如此） |
| Shiro 命名装配器遗留 | 运行时无 Shiro 依赖但源码保留 Shiro 命名 | ✅ 已解决 |
| Spring Security 授权上下文缺失 | JWT 认证后 authorities 为空，导致 ORG_OWNER 仍被业务权限校验拒绝 | ✅ 已解决：`SpringSecuritySubjectFacade` 按当前 org 懒加载授权并支持通配权限匹配 |
| data-provider-base 测试编译偶发失败 | maven-compiler-plugin 3.15.0 测试阶段 | ⚠️ 不影响产物，后续跟踪 |
| EnvironmentPostProcessor 废弃 | Spring Boot 4.0 标记 @Deprecated(forRemoval=true) | ⚠️ 已加 @SuppressWarnings("removal")，待迁移至 ApplicationListener |
| Quill 2.0.3 HTML export XSS advisory | 上游当前无高于 2.0.3 的修复版本；npm audit 的自动方案为降级，不符合“只升级不降级”策略 | ⚠️ 已缓解：不使用 Quill HTML export 作为可信存储，历史字符串 HTML 进入富文本渲染前统一经 DOMPurify 3.4.11 清理；等待上游 2.0.4+ 或等效新版 |

## 验证矩阵

| 验证项 | 结果 |
|--------|------|
| `mvn compile` (all modules) | ✅ BUILD SUCCESS |
| `mvn test` | ✅ 194 tests pass |
| `npm run checkTs` | ✅ 0 errors |
| `npm run build` + `build:task` | ✅ 4 entry points + task UMD |
| `npm run test:ci` | ✅ 1156 tests pass (165 files) |
| `npm run lint:css` | ✅ 0 errors |
| Build size delta (gzip) | ✅ +9.6% (monaco-editor 0.55.1 升级导致，baseline 已更新) |
| Install zip | ✅ Complete structure |
| Demo health | ✅ Response within 4s |

> 注：上述全量验证矩阵来自 `refactor/tech-modernization-part2` 先前完整验证。本轮 `modernization-round-2-fixes` 仅补跑 `git diff --check`、`npm run checkTs` 和 Split/SplitPane 相关测试（4 个测试文件、24 tests passed）。本轮 `fix/local-package-spa-entry` 本地验收修复的验证记录见下方补充修正记录，当前最新本地安装包已包含主应用深链路由矩阵、成员侧栏状态、权限页视觉间距和权限表列宽修复。

## 下一步计划

1. ~~**合并到 main（part1）**~~：✅ 已完成（`refactor/tech-modernization` 分支已合入 `main`）；part2（`refactor/tech-modernization-part2`）待合入
2. ~~**Shiro 装配器重命名/收敛**~~：✅ 已完成
3. ~~**Jackson 2 API 统一迁移**~~：✅ 已完成
4. ~~**依赖 patch 升级**~~：✅ 已完成（Boot 4.0.7 + Cloud 2025.1.2 + Springdoc 3.0.3 + 7 前端依赖）
5. **Optional property tests**：补充 5 个 optional PBT
6. ~~**React Class Component 迁移**~~：✅ 已完成（8 个文件重构为函数组件 + hooks）
7. ~~**monaco-editor 升级**~~：✅ 已完成（0.52.2 → 0.55.1）
8. **@vitejs/plugin-react 6.0.3 升级**：✅ 已完成
9. **fast-check 4.x 升级**：✅ 已完成
10. **react-dnd 评估完成**：与 @hello-pangea/dnd 各覆盖不同场景，保留共存
11. **监控 pro-components stable**：关注 npm latest tag 从 2.8.10 更新到 3.x
12. **TypeScript 7 升级**：等待 `@typescript-eslint` 兼容版本发布后执行
13. **本地安装包修复验收**：当前 8080 验收服务已更新到最新安装包，等待人工确认后再决定是否提交/合并

## 相关文档

- [全项目技术栈审计报告](./tech-stack-audit-report.md) — 2026-06-28 审计，含版本对比、Gap 分析和废弃 API 扫描

## 补充修正记录

### `modernization-round-2-fixes` spec（2026-06-29）

在 round 2 代码审查后，针对以下遗留问题进行修正：

- **Split.tsx minSize 稳定性修复**：引入 `useStableArray` hook，避免 `minSize` 数组引用变化但值未变时不必要的 Split.js 实例重建
- **SplitPane 死代码清理**：移除未被读取的 `resizedRef`
- **文档修正**：更正 progress / audit-report / log 三份文档中的版本号、迁移状态和合并状态描述

### 本地安装包 SPA 入口与验收体验修复（2026-06-29）

针对本地安装包验收中发现的页面入口和交互问题进行修正：

- **SPA 深链入口修复**：`/organizations/**` 与 `/confirminvite` 由服务端转发到 `/index.html`，并在 Spring Security 与 `BasicValidRequestInterceptor` 中放行，避免刷新主应用深链返回 `{"success":false,"errCode":"login.not-login"}`。
- **语言切换修复**：`changeLang` 改为更新 locale、请求语言头和 dayjs/i18next 语言，不再执行 `window.location.reload()`，避免在主应用深链触发整页刷新。
- **侧边栏 Split 修复**：`useSplitSizes` 初始值从真实 min/max 约束计算，主应用侧栏 Split 增加 `onDrag` 同步和 8px gutter 命中区，恢复左右伸缩能力。
- **头像与账号菜单展示修复**：Avatar 过滤空值、`/null`、`/undefined` 并支持加载失败回退；账号菜单语言/主题项恢复文字展示。
- **本地安装包资源修复**：静态资源路径支持安装包运行目录 `static/`，登录接口返回当前登录用户信息，避免登录成功响应异常。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/components/__tests__/Split.test.tsx src/app/components/__tests__/Split.minSize.test.tsx src/app/components/__tests__/Split.preservation.test.tsx src/app/components/__tests__/SplitPane.test.tsx src/app/components/__tests__/Avatar.test.tsx`：✅ 5 files / 29 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 验收：✅ `/api/v1/sys/info` 返回 success；`/organizations/.../vizs` 与 `/organizations/.../sources/add` 返回 `text/html` SPA 入口；浏览器复核侧栏初始宽度约 256px、gutter 8px、拖拽后宽度变化正常

### 退出登录与 demo 权限修复（2026-06-29）

针对本地安装包验收中继续发现的账号与权限问题进行修正：

- **退出登录跳转修复**：账号菜单点击“退出登录”后不再跳转 `/`，改为明确 `navigate.replace('/login')`，避免主应用默认路由再次回到组织页。
- **密码登录校验恢复**：`SpringSecurityManager.login(PasswordToken)` 在写入 Spring Security 上下文前恢复密码校验，兼容 BCrypt 与历史明文密码数据，错误密码返回登录失败。
- **Spring Security 授权适配修复**：`SpringSecuritySubjectFacade` 使用 `AuthorizationAssembler` 按当前 org 懒加载角色和资源权限，并按 Shiro 迁移前语义支持 `org:*:*:*` 这类 owner 通配权限匹配具体权限字符串。
- **程序化登录主体修复**：`loginWithPassword` / `loginWithBearer` 写入 `User` principal，保持后台任务和服务层 `getCurrentUser()` 行为兼容。
- **测试补充**：新增/扩展安全测试覆盖错误密码拒绝、正确密码通过、User principal 读取、owner 通配权限通过和无权限拒绝。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `mvn -pl security -am test`：✅ core 22 tests + security 18 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 API 验收：✅ 错误密码返回 400 登录失败；正确密码登录成功；demo 组织列表返回 `demo's Organization`；`/api/v1/orgs/f8435e0a3323459aaef679ab63fbd01a/members` 返回 success 且 demo `orgOwner=true`
- 本地 8080 浏览器验收：✅ 成员页不再出现“权限不足”；点击“退出登录”后 URL 为 `http://127.0.0.1:8080/login` 且显示登录表单

### 组织内详情子路由修复（2026-06-29）

针对本地安装包验收中发现的前端 404 进行修正：

- **数据源子路由修复**：`/organizations/:orgId/sources` 改为支持可选 `:sourceId`，覆盖新建数据源 `/sources/add` 和已有数据源详情 `/sources/:sourceId`。
- **成员子路由修复**：`/organizations/:orgId/members` 改为支持可选 `:memberId`，覆盖成员详情 `/members/:memberId`。
- **角色子路由修复**：`/organizations/:orgId/roles` 改为支持可选 `:roleId`，覆盖新建角色 `/roles/add` 和角色详情 `/roles/:roleId`。
- **兼容性范围**：只修正 React Router 顶层匹配规则，不修改接口、数据结构、权限逻辑或页面内部行为。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 API 验收：✅ `/api/v1/sys/info` 返回 success；`/organizations/.../sources/add` 与 `/organizations/.../roles/add` 返回 `text/html` SPA 入口
- 本地 8080 浏览器验收：✅ demo 登录后直接访问 `/sources/add`、`/members/089de1b3693c4a49ab91d2dbebfda0c5`、`/roles/add` 均未出现应用内 404 或未登录提示，页面分别显示新建数据源、成员详情、新建角色内容

### 主应用路由矩阵与成员侧栏状态修复（2026-06-29）

针对本地安装包验收中继续发现的前端路由和侧栏状态问题进行修正：

- **权限详情路由修复**：补齐 `/organizations/:orgId/permissions/:viewpoint/:type/:id`，使权限页成员、角色、资源详情不再落入应用内 404。
- **成员侧栏状态修复**：新增 `getMemberSidebarSelectedKey(pathname)`，按 `/members` 或 `/roles` 模块段判断当前 tab，避免成员/角色详情页因 URL 最后一段为 ID 而隐藏左侧列表。
- **看板编辑深链修复**：补齐 `/organizations/:orgId/vizs/:vizId/boardEditor`，覆盖现有跳转和 `VizPage` 内部 `boardEditor` 渲染判断。
- **路由矩阵显式化**：将 `vizs/views/sources/schedules/members/roles` 根路径与详情路径拆成显式路由，减少可选参数兼容差异导致的漏配。
- **兼容性范围**：只修正 React Router 顶层匹配和侧栏 tab 状态推导，不修改后端接口、权限模型、数据库结构或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/pages/MainPage/__tests__/routes.test.ts src/app/pages/MainPage/pages/MemberPage/Sidebar/__tests__/index.test.ts`：✅ 2 files / 31 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 API 验收：✅ `/api/v1/sys/info` 返回 success；权限详情与 `boardEditor` 深链返回 `text/html` SPA 入口
- 本地 8080 浏览器验收：✅ demo 登录后直接访问成员列表、成员详情、角色列表、角色新增、权限成员详情、权限角色详情、权限数据源详情、`boardEditor` 深链均未出现应用内 404 或未登录提示；成员/角色详情页左侧列表保留；权限详情页显示权限配置主面板

### 权限页视觉与侧栏拖拽稳定性修复（2026-06-30）

针对本地安装包验收中继续发现的 UI 细节问题进行修正：

- **侧栏拖拽边界稳定性**：`useSplitSizes` 抽出 `normalizeSplitSizes` / `areSplitSizesEqual`，拖到 min/max 边界且归一化结果未变化时复用当前 sizes 引用，避免边界处反复回写导致抖动。
- **成员/角色列表留白修复**：成员模块成员/角色列表、权限页角色/成员列表改为局部 16px 横向留白，并保留文本溢出能力，避免名称贴边。
- **权限资源树间距修复**：权限资源视图树增加 `resource-tree` 局部样式，缩小展开/收起按钮占位并补充内容区间距，使“所有资源”等文案与按钮间距更合理。
- **权限表列宽修复**：权限详情表资源名称列增加最小宽度、`ellipsis`、固定布局和横向滚动宽度，避免窄容器下“资源名称/所有资源”逐字换行。
- **兼容性范围**：只修改前端 hook、局部样式和表格布局，不修改后端接口、权限模型、数据库结构、历史数据或发布产物结构。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/hooks/__tests__/useSplitSizes.test.ts src/app/pages/MainPage/pages/PermissionPage/Main/PermissionForm/__tests__/PermissionTable.test.ts src/app/pages/MainPage/pages/MemberPage/Sidebar/__tests__/index.test.ts`：✅ 3 files / 14 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 浏览器验收：✅ demo 登录态下复核通过；权限成员列表与成员模块角色列表左右留白均为 16px；资源树展开按钮宽 16px、按钮到“所有资源”文字间距 12px；权限表资源名称列 220px 且“所有资源”不再逐字换行；可视化侧栏拖到右侧边界后宽度稳定在 716px、800ms 后无回跳

### FILE 上传状态与权限表可调列宽修复（2026-06-30）

针对本地安装包验收中继续发现的 FILE 数据源上传与权限表列宽问题进行修正：

- **当前状态**：已完成
- **FILE 数据源上传状态修复**：收敛 Ant Design Upload 状态流，确保上传成功、上传失败、移除、接口业务失败和缺失 `sourceId` 等路径都能退出 loading，避免“选择文件”按钮长时间锁死。
- **权限表默认列宽修复**：权限详情表默认资源列宽调整为 340px，并在未手动拖拽前交给 AntD Table 按容器自动适配，避免可视化 tab 因 1px 边框误差出现默认横向滚动；数据视图/可视化默认表格宽度保持同一对齐基线。
- **权限表手动调宽**：复用项目已有 `react-resizable` 能力，为资源名称列提供拖拽调宽，权限列继续按权限项数量计算，保持权限保存逻辑不变。
- **兼容性范围**：只修改前端上传状态、权限表展示布局和相关测试，不修改后端接口、权限模型、数据库结构、历史数据或发布包目录结构。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/pages/MainPage/pages/SourcePage/SourceDetailPage/ConfigComponent/__tests__/FileUpload.test.tsx src/app/pages/MainPage/pages/PermissionPage/Main/PermissionForm/__tests__/PermissionTable.test.ts`：✅ 2 files / 7 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 浏览器验收：✅ demo 登录态下复核通过；可视化权限表默认列宽约 349px / 370px，`scrollWidth == clientWidth`，无默认横向滚动；资源名称列存在拖拽手柄，拖拽后资源列从约 349px 增至约 380px 并进入可控横向滚动；已有 FILE 数据源“新增配置”弹窗中“选择文件”按钮未 disabled、无 loading

### 数据视图补充问题修复（2026-06-30）

针对本地安装包验收和全库 review 中继续发现的数据视图补充问题进行修正：

- **当前状态**：已完成
- **SQL 测试请求修复**：SQL 类型执行不再发送字符串 `columns`，避免后端 `List<SelectColumn>` 反序列化失败；STRUCT 类型仍发送结构化列数组。
- **Monaco 失败态修复**：通用 Monaco 编辑器加载失败时不再永久 spinner，避免计算字段弹窗无法正常取消。
- **右侧属性面板间距修复**：变量配置和列权限列表补齐横向留白。
- **BasicTable 默认列宽修复**：对齐数据视图结果表的默认宽度口径，同时保留手动拖拽列宽能力。
- **兼容性范围**：只修改前端展示、请求 payload 和相关测试，不修改后端接口、权限模型、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/components/MonacoEditor/__tests__/index.test.tsx src/app/pages/MainPage/pages/ViewPage/__tests__/utils.test.ts src/app/components/ChartGraph/BasicTableChart/__tests__/BasicTableChart.test.jsx src/app/components/ChartGraph/BasicTableChart/__tests__/columnWidth.test.ts src/app/pages/MainPage/pages/ViewPage/slice/__tests__/thunks.test.ts`：✅ 5 files / 49 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`

未执行项：

- Maven 全量测试、前端全量测试、体积门禁、本地 8080 浏览器人工验收和 Docker build 本轮未执行；当前范围为本地安装包验收的数据视图补充修复。Docker build 未执行，原因：本机环境未确认可用。

### 本地验收二次补充修复（2026-06-30）

针对本地安装包验收中继续发现的权限表、角色成员弹窗、成员角色保存和 FILE 上传问题进行修正：

- **当前状态**：已完成
- **权限表列宽交互修复**：表格总宽精确等于资源名称列宽 + 权限列宽；拖拽第一列时按拖拽 delta 同步调整资源名称列宽和外层表格宽度，权限列宽保持不变；不再通过 AntD Table 内部横向滚动承载宽度变化。
- **角色添加成员弹窗比例修复**：移除角色详情页传入的 992px 宽弹窗，`MemberForm` 恢复 520px 宽度和固定 Transfer 列表尺寸，避免两列比例异常。
- **demo H2 成员角色保存修复**：demo H2 JDBC URL 增加 `NON_KEYWORDS=USER`，兼容 H2 2.4 对 `USER` 保留字的处理，避免给成员赋角色时 `SELECT COUNT(*) FROM user` 报 SQL grammar。
- **FILE 上传路径修复**：上传接口 action 从 `/files/datasource/` 调整为 `/files/datasource`，避免 Spring Boot 4/Tomcat 11 对尾斜杠路径映射差异导致 404。
- **兼容性范围**：不修改后端接口、权限模型、数据库 schema、历史数据或发布包目录结构；demo H2 仅补充兼容参数，前端上传仍调用现有 `/files/datasource` 接口。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/pages/MainPage/pages/PermissionPage/Main/PermissionForm/__tests__/PermissionTable.test.ts src/app/pages/MainPage/pages/SourcePage/SourceDetailPage/ConfigComponent/__tests__/FileUpload.test.tsx src/app/pages/MainPage/pages/MemberPage/Sidebar/__tests__/index.test.ts`：✅ 3 files / 20 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 API/页面入口验收：✅ 15:12 最终安装包以 demo profile 启动；`/api/v1/sys/info` 返回 success；权限详情深链返回 `text/html` SPA 入口；权限页最终静态资源 `/static/js/PermissionPage.CnwMrNoK.js` 返回 200；`PUT /api/v1/roles/{userId}/roles?orgId=...` 返回 success，不再出现 H2 `FROM user` SQL grammar；无尾斜杠上传路径 `/api/v1/files/datasource?sourceId=...` 已命中后端映射并返回业务错误“数据源 不存在”，不再落到静态资源 404

未执行项：

- Maven 全量测试、前端全量测试、体积门禁、权限表拖拽/弹窗比例浏览器人工操作和 Docker build 本轮暂未执行；当前范围为本地安装包验收补充修复。Docker build 未执行，原因：本机环境未确认可用。

### Dependabot DOMPurify / Quill 漏洞修复（2026-06-30）

针对 GitHub Dependabot 当前检测到的 DOMPurify 与 Quill 告警进行“只升级不降级”修复：

- **当前状态**：已完成
- **DOMPurify 修复**：新增 `dompurify@3.4.11` 直接依赖，并通过 `overrides.dompurify = "3.4.11"` 收敛 `monaco-editor@0.55.1` 的间接依赖，避免回退 Monaco。
- **Quill 缓解决策**：保留 `quill@2.0.3` 与 `react-quill-new@3.8.3`。当前上游没有高于 2.0.3 的修复版，npm audit 推荐方案为降级到 2.0.2，不符合本轮“不降级”约束。
- **富文本历史 HTML 加固**：新增统一 DOMPurify sanitize 路径，历史字符串 HTML 进入富文本只读/编辑渲染前先清理；Delta 内容继续作为主存储路径，不把 Quill HTML export 作为可信存储。
- **兼容性范围**：不修改富文本数据结构、后端接口、数据库 schema、历史数据或发布包目录结构；只收敛前端依赖安全版本和历史 HTML 渲染入口。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/components/ChartGraph/BasicRichText/__tests__/content.test.ts src/app/components/ChartGraph/BasicRichText/__tests__/ChartRichTextAdapter.readonly.test.tsx src/app/components/ChartGraph/BasicRichText/__tests__/ChartRichTextAdapter.edit.test.tsx src/app/pages/DashBoardPage/components/Widgets/RichTextWidget/__tests__/RichTextWidgetCore.smoke.test.tsx src/app/components/MonacoEditor/__tests__/index.test.tsx`：✅ 5 files / 19 tests passed
- `npm audit --json`：⚠️ DOMPurify / Monaco 链路已清除；剩余 `quill` / `react-quill-new` 2 个 low severity，npm 自动修复方案为降级，不符合本轮约束，保留跟踪
- `npm run build`：✅ 通过
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`

遗留问题：

- Quill 低危告警可能继续存在；原因是上游当前无不降级修复版。本轮记录为已缓解并跟踪，等待上游发布 `2.0.4+` 或 `react-quill-new` 等效新版后再升级关闭。
- Docker build 未执行，原因：本轮范围为前端依赖安全修复与安装包构建验证，且本机 Docker 环境未确认可用。

### 权限页与查询结果展示补充修复（2026-06-30）

针对本地安装包验收中继续发现的展示问题进行修正：

- **当前状态**：已完成
- **权限资源树角标居中**：`resource-tree` 的展开/收起 switcher 改为 flex 居中，避免小方框内箭头偏上/偏左。
- **权限表默认资源列加宽**：资源名称列默认宽度从 340px 增加到 510px，约为当前基础加宽 1/2；最大手动调宽上限同步提高到 760px。
- **查询结果单元格不换行**：`VirtualTable` 单元格统一 `white-space: nowrap`、`text-overflow: ellipsis`，覆盖数据源文件预览和数据视图查询结果，长字符串通过横向滚动查看。
- **兼容性范围**：只修改前端展示样式和布局常量，不修改后端接口、权限模型、数据库 schema、历史数据或发布包目录结构。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/pages/MainPage/pages/PermissionPage/Main/PermissionForm/__tests__/PermissionTable.test.ts src/app/components/__tests__/VirtualTable.test.tsx`：✅ 2 files / 11 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`

未执行项：

- Maven 全量测试、前端全量测试、体积门禁、Docker build 和浏览器人工复核本轮未执行；当前范围为本地安装包验收展示补充修复。Docker build 未执行，原因：本机 Docker 环境未确认可用。

### 查询结果列宽二次修复（2026-06-30）

针对本地安装包验收中继续发现的“查询结果已不换行，但时间/长字符串被省略导致无法完整查看”问题进行补充修正：

- **当前状态**：已完成
- **数据视图/FILE 预览列宽修复**：`getColumnWidthMap` 的默认列宽计算同步纳入表头文字、类型图标、列权限图标、内容文字宽度和单元格 padding；时间戳等常见长字符串在单行模式下默认完整展示。
- **极端长文本兜底**：`VirtualTable` 单元格保留 `title`，超过列宽上限时仍可悬停查看完整值。
- **兼容性范围**：只修改前端表格展示宽度和单元格可读性，不修改后端接口、查询 payload、权限模型、数据库 schema、历史数据或发布包目录结构。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/pages/MainPage/pages/ViewPage/__tests__/utils.test.ts src/app/components/__tests__/VirtualTable.test.tsx`：✅ 2 files / 44 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 安装包重启：✅ 已启动 `/private/tmp/yu-bi-demo-run-20260630-1650`，PID `96161`，`/api/v1/sys/info` 返回 `success: true`

待执行项：

- 浏览器人工复核

### 角色添加成员弹窗宽版修复（2026-06-30）

针对本地安装包验收中继续发现的“添加成员弹窗太小，成员名称和邮箱展示不全”问题进行修正：

- **当前状态**：已完成
- **弹窗尺寸修复**：角色详情页 `MemberForm` 弹窗宽度调整为 800px，左右 Transfer 列表通过 Ant Design 6 推荐的 `styles.section` 固定为 360px x 320px，恢复接近旧版宽版比例。
- **成员名称可读性**：成员项保持单行省略，完整的姓名、用户名和邮箱写入 `title`，极端长名称仍可悬停查看。
- **兼容性范围**：只修改角色添加成员弹窗展示尺寸和测试，不修改成员/角色保存接口、权限模型、数据库 schema、历史数据或发布包目录结构。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/pages/MainPage/pages/MemberPage/pages/RoleDetailPage/__tests__/MemberForm.test.tsx`：✅ 1 file / 1 test passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 安装包重启：✅ 已启动 `/private/tmp/yu-bi-demo-run-20260630-1706`，PID `17633`，`/api/v1/sys/info` 返回 `success: true`

待执行项：

- 浏览器人工复核

### 数据视图与 FILE 预览长数据完整展示修复（2026-06-30）

针对本地安装包验收中继续发现的“长数据仍被省略，无法在表格内完整查看”问题进行修正：

- **当前状态**：已完成，等待浏览器人工复核
- **共享列宽算法修复**：移除 `getColumnWidthMap` 中 360px 硬上限，数据视图查询结果和 FILE 数据源预览按真实内容宽度撑开列，依赖已有横向滚动查看完整数据。
- **单行展示保留**：`VirtualTable` 继续保持 `white-space: nowrap`，避免长文本撑高行高；完整值继续保留在 `title` 中作为极端长内容兜底。
- **兼容性范围**：只修改前端共享表格列宽算法和相关测试，不修改后端接口、查询 payload、权限模型、数据库 schema、历史数据或发布包目录结构。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/pages/MainPage/pages/ViewPage/__tests__/utils.test.ts src/app/components/__tests__/VirtualTable.test.tsx`：✅ 2 files / 44 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 安装包重启：✅ 已启动 `/private/tmp/yu-bi-demo-run-20260630-1848`，PID `34242`，`/api/v1/sys/info` 返回 `success: true`

待执行项：

- 浏览器人工复核数据视图查询结果和 FILE 数据源预览长字段展示

### 分析看板配置布局回归修复（2026-06-30）

针对本地安装包验收中发现的“分析看板配置界面被排成右侧窄栏，而不是左字段、中配置、上工具栏、右画布的旧版布局”问题进行修正：

- **当前状态**：已完成，等待浏览器人工复核
- **布局根因修复**：`flexlayout-react` 的 `weight` 是相对比例而不是像素宽度；原布局把 256/360 当固定宽度写入，导致数据字段列和配置列被异常放大，图表预览被挤到右侧窄栏。
- **目标布局恢复**：分析看板配置区权重调整为字段列 12、配置列 16、画布 72，并给字段列、配置列、画布设置最小宽度，保证宽屏下画布为主区域，小屏下基础面板不被压扁。
- **回归测试**：新增布局契约测试，确保画布区域权重大于字段列和配置列权重之和，避免再次把像素宽度误写成相对权重。
- **兼容性范围**：只修改前端分析看板配置布局常量和测试，不修改图表配置结构、数据请求、后端接口、权限模型、数据库 schema、历史数据或发布包目录结构。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/__tests__/layoutRuntime.test.ts`：✅ 1 file / 11 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 安装包重启：✅ 已启动 `/private/tmp/yu-bi-demo-run-20260630-1859`，使用本地 MySQL `127.0.0.1:3306/yubi`
- MySQL 初始化：✅ 已创建/使用 `yubi` 数据库，自动迁移完成，`/api/v1/sys/info` 返回 `initialized: true`
- 验收账号：✅ 已初始化管理员用户 `demo`，密码 `123456`

待执行项：

- 浏览器人工复核分析看板配置布局

### JSON 空值反序列化同类问题全代码修复（2026-06-30）

针对本地 MySQL 验收中分析看板配置页触发的 `JSON parse error: Cannot map null into type long` 问题进行全代码同类盘点和修正：

- **当前状态**：已完成，等待浏览器人工复核
- **根因范围**：后端 `@RequestBody` 入参 DTO 中存在 primitive 字段，当前命中路径为 `ViewExecuteParam.pageInfo.pageNo`；同类风险还包括 `pageInfo.pageSize/total/countTotal`、`ViewExecuteParam.cache/cacheExpires/concurrencyControl/script/analytics`、`TestExecuteParam.size`、`DownloadCreateParam.imageWidth`、`DashboardCreateParam.index`、变量 `expression`、权限 `permission`，以及请求链路中嵌套的 `ScriptVariable.expression/disabled`、`QueryScript.test` 等字段。
- **前端请求契约修复**：图表请求构造、表格分页事件、看板表格点击、数据视图 SQL 测试、下载任务、变量保存、计划任务图片宽度回显、新增可视化资源 index 均补齐非空默认值，避免向后端 primitive 字段发送 `null/undefined`。
- **后端防御性兜底**：`PageInfo` 和相关请求参数类增加 null-safe setter；`ScriptVariable`、`QueryScript`、数据源配置模板属性等嵌套 JSON 对象补齐 null-safe 反序列化；`DataProviderServiceImpl` 在执行前 normalize `pageInfo`，缺失或非法页码/页大小回落到 `pageNo=1`、`pageSize=1000`。
- **兼容性范围**：不修改后端接口路径、数据库 schema、权限模型、历史数据结构或发布包目录结构；保留业务默认行为，仅修正请求契约和反序列化防御。
- **回归测试**：新增/扩展前端请求构造测试和后端 Jackson 反序列化测试，覆盖显式 `null`、缺失字段、嵌套请求对象和分页事件缺省页码。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/models/__tests__/ChartDataRequestBuilder.test.ts src/app/utils/__tests__/ChartEventListenerHelper.test.ts src/app/utils/__tests__/internalChartHelper.test.ts src/app/pages/MainPage/pages/ViewPage/slice/__tests__/thunks.test.ts`：✅ 4 files / 182 tests passed
- `mvn -pl server -am -Dtest=ViewExecuteParamDeserializationTest -Dsurefire.failIfNoSpecifiedTests=false test`：✅ BUILD SUCCESS，`ViewExecuteParamDeserializationTest` 5 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`

待执行项：

- 本地 8080 MySQL 环境浏览器复核分析看板配置页
- Docker build 未执行，原因：本轮范围为请求契约修复和本地安装包验证，且本机 Docker 环境未确认可用

### 数据视图列宽与弹窗回归修复（2026-06-30）

针对本地安装包验收中继续发现的“数据视图查询结果列宽不可调、字段操作弹窗取消无反应、筛选弹窗布局偏离旧版”问题进行修正：

- **当前状态**：已完成，等待浏览器人工复核
- **数据视图结果表可调列宽**：`SchemaTable` 在默认 `getColumnWidthMap` 基础上维护手动列宽状态，业务列表头支持拖拽调宽；拖拽单列只改变该列和表格总横向宽度，其它列宽保持不变。
- **虚拟表格兼容性**：保留 `VirtualTable` 单行展示、横向滚动和完整 `title` 兜底，避免长文本修复回退。
- **弹窗取消关闭修复**：`useStateModal` 的取消路径清理缓存和表单后兜底关闭，覆盖别名、格式、聚合、过滤等字段操作弹窗；业务 `onCancel(close)` 扩展点保留。
- **筛选弹窗布局修复**：过滤字段操作弹窗固定为 800px 宽，过滤控制面板改为稳定 label 宽度和局部 Transfer 尺寸，恢复旧版紧凑排版，不改全局 Modal 样式。
- **兼容性范围**：只修改前端表格交互、弹窗关闭和弹窗布局；不修改后端接口、查询 payload、权限模型、数据库 schema、历史数据或发布包目录结构。

本轮验证：

- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/hooks/__tests__/useStateModal.test.tsx src/app/hooks/__tests__/useFieldActionModal.test.tsx src/app/pages/MainPage/pages/ViewPage/components/__tests__/SchemaTable.test.tsx src/app/components/__tests__/VirtualTable.test.tsx src/app/pages/MainPage/pages/ViewPage/__tests__/utils.test.ts`：✅ 5 files / 48 tests passed
- `git diff --check`：✅ 通过
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 安装包重启：✅ 已启动 `/private/tmp/yu-bi-demo-run-20260630-2146`，PID `63939`，使用本地 MySQL `127.0.0.1:3306/yubi`，`/api/v1/sys/info` 返回 `success: true`

待执行项：

- 浏览器人工复核数据视图结果表拖拽列宽、字段操作弹窗取消按钮、筛选弹窗布局
- Docker build 未执行，原因：本轮范围为前端本地验收修复，且本机 Docker 环境未确认可用

### 配置面板 Tab 与筛选弹窗布局二次修复（2026-06-30）

针对本地安装包验收中继续发现的“配置面板一级 tab 太紧凑、筛选弹窗 Transfer 区域仍未恢复旧版排版”问题进行补充修正：

- **当前状态**：已完成，等待浏览器人工复核
- **配置面板 Tab 间距修复**：分析看板配置面板一级 tab 在配置面板宽度内等分展开，增大点击区域并保持图标/文案居中，避免数据/样式/分析/交互入口挤在一起。
- **筛选弹窗排版修复**：过滤控制面板取消居中压缩布局，恢复左起表单布局；Transfer 源列表、操作按钮、默认列表同一水平线顶部对齐，列表宽度恢复接近旧版示例。
- **筛选弹窗上下间距修复**：过滤字段操作弹窗顶部表单行距从紧凑模式调整为更舒展的 9px 上下 padding，并给筛选方式 Tab 导航增加顶部/底部留白，避免名称、聚合方式、筛选方式三行贴在一起。
- **AntD 6 Transfer 适配**：AntD 6 的 Transfer 内部类名由旧 `.ant-transfer-list/.ant-transfer-operation` 调整为 `.ant-transfer-section/.ant-transfer-actions`；本轮局部样式同时覆盖新旧类名，确保左右列表宽高、分页、搜索框和操作按钮对齐规则真正生效。
- **兼容性范围**：只修改前端局部样式，不修改图表配置结构、后端接口、查询 payload、权限模型、数据库 schema、历史数据或发布包目录结构。

本轮验证：

- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/hooks/__tests__/useFieldActionModal.test.tsx src/app/hooks/__tests__/useStateModal.test.tsx src/app/pages/MainPage/pages/ViewPage/components/__tests__/SchemaTable.test.tsx`：✅ 3 files / 4 tests passed
- `git diff --check`：✅ 通过
- 筛选弹窗上下间距补充修复后复跑 `npm run checkTs`：✅ 0 errors；`git diff --check`：✅ 通过；`npm run test:ci -- src/app/hooks/__tests__/useFieldActionModal.test.tsx src/app/hooks/__tests__/useStateModal.test.tsx src/app/pages/MainPage/pages/ViewPage/components/__tests__/SchemaTable.test.tsx`：✅ 3 files / 4 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 安装包重启：✅ 已通过 `screen` 会话 `yubi-demo` 启动 `/private/tmp/yu-bi-demo-run-20260630-2246.qD7zU0`，PID `96904`，使用本地 MySQL `127.0.0.1:3306/yubi`，`/api/v1/sys/info` 返回 `success: true`

待执行项：

- 浏览器人工复核配置面板一级 tab 间距和筛选弹窗排版
- Docker build 未执行，原因：本轮范围为前端本地验收修复，且本机 Docker 环境未确认可用

### FILE 预览列宽与筛选弹窗行距补充修复（2026-07-01）

针对本地安装包验收中继续发现的“FILE 数据源上传后预览表格拖拽列宽错乱”和“筛选弹窗顶部两个下拉框/表单项上下紧挨着”问题进行补充修正：

- **当前状态**：已完成，等待浏览器人工复核
- **FILE 预览列宽根因**：`SchemaTable -> VirtualTable` 链路中，`VirtualTable` 在容器宽度大于 `scroll.x` 时会自动把剩余宽度平摊到业务列；FILE 预览弹窗宽度固定且列宽支持手动拖拽后，这个二次放大逻辑容易导致表头、虚拟表 body 和横向滚动宽度状态不一致。
- **列宽修复方案**：`VirtualTable` 新增 `fillColumnWidth` 可选开关，默认保持旧行为；`SchemaTable` 显式关闭自动填满，让数据视图查询结果和 FILE 预览的列宽只由默认列宽算法与用户手动拖拽值决定。
- **筛选弹窗行距修复**：`FilterControlPanel` 局部覆盖 `.ant-form-item` 与 `.ant-form-item-row`，补充顶部表单项真实 `margin-bottom` 和行内 padding，避免继续受 `FormItemEx` 全局 `margin: 0` 影响。
- **兼容性范围**：只修改前端共享表格列宽控制和筛选弹窗局部样式；不修改后端接口、查询 payload、权限模型、数据库 schema、历史数据或发布包目录结构。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/components/__tests__/VirtualTable.test.tsx src/app/pages/MainPage/pages/ViewPage/components/__tests__/SchemaTable.test.tsx src/app/hooks/__tests__/useFieldActionModal.test.tsx`：✅ 3 files / 8 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 安装包重启：✅ 已通过 `screen` 会话 `yubi-demo` 启动 `/private/tmp/yu-bi-demo-run-20260701-1943.m8XPFo`，使用本地 MySQL `127.0.0.1:3306/yubi`，`/api/v1/sys/info` 返回 `success: true`

待执行项：

- 浏览器人工复核 FILE 数据源预览列宽拖拽和筛选弹窗顶部表单项上下间距
- Docker build 未执行，原因：本轮范围为前端本地验收修复，且本机 Docker 环境未确认可用

### FILE 预览虚拟表格列宽缓存刷新修复（2026-07-01）

针对本地安装包验收中继续发现的“FILE 数据源预览拖拽列宽后只有表头变化，下面的数据行仍保持旧列宽，导致数据遮挡且难以恢复”问题进行补充修正：

- **当前状态**：已完成，等待浏览器人工复核
- **问题根因**：`SchemaTable` 的手动列宽状态和表头已经正确更新，但 `VirtualTable` 的 body 使用 `react-window` Grid；Grid 会缓存列宽边界，列宽变化后如果不重建虚拟布局，body 单元格仍按旧列宽定位和裁剪。
- **处理方案**：`VirtualTable` 根据容器宽度、滚动宽高、列 dataIndex 和列宽生成 `gridLayoutKey`；当列宽签名变化时强制虚拟 Grid 重新挂载，并把该签名纳入 `cellProps`，确保 body 单元格、横向滚动宽度和表头同步。
- **兼容性范围**：只修改前端虚拟表格布局刷新机制和测试；不修改列宽算法、后端接口、查询 payload、权限模型、数据库 schema、历史数据或发布包目录结构。

本轮验证：

- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/components/__tests__/VirtualTable.test.tsx src/app/pages/MainPage/pages/ViewPage/components/__tests__/SchemaTable.test.tsx`：✅ 2 files / 8 tests passed
- `git diff --check`：✅ 通过
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 安装包重启：✅ 已通过 `screen` 会话 `yubi-demo` 启动 `/private/tmp/yu-bi-demo-run-20260701-2020.0cTDGh`，使用本地 MySQL `127.0.0.1:3306/yubi`，`/api/v1/sys/info` 返回 `success: true`

待执行项：

- 浏览器人工复核 FILE 数据源预览拖拽列宽后表头和数据行同步变化
- Docker build 未执行，原因：本轮范围为前端本地验收修复，且本机 Docker 环境未确认可用

### 筛选弹窗对齐与列表宽度补充修复（2026-07-01）

针对本地验收中继续发现的“筛选方式与常规 tab 不在同一行、源列表/默认列表偏窄、是否可见与隐藏单选项不对齐”问题进行补充修正：

- **当前状态**：已完成，等待浏览器人工复核
- **筛选方式行对齐**：`FilterControlPanel` 为筛选方式表单项增加专用 class，局部调整 label padding、Tabs nav margin 和 tab 高度，使“筛选方式”和“常规/自定义/条件”入口在同一水平线上。
- **Transfer 列表宽度**：`CategoryConditionConfiguration` 将左右 Transfer 列表从 176px 扩大到 236px，约为当前基础上增加 1/3；同时将筛选弹窗默认宽度调整为 980px，避免列表变宽后继续挤压。
- **是否可见行对齐**：`FilterControlPanel` 为可见性表单项增加专用 class，并让 `FilterVisibilityConfiguration` 的单选项容器以 32px 行高居中，确保“是否可见”和“隐藏/显示”在同一水平线上。
- **兼容性范围**：只修改筛选字段操作弹窗局部样式和弹窗宽度；不修改图表配置结构、请求 payload、后端接口、权限模型、数据库 schema、历史数据或发布包目录结构。

本轮验证：

- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/hooks/__tests__/useFieldActionModal.test.tsx src/app/hooks/__tests__/useStateModal.test.tsx`：✅ 2 files / 3 tests passed
- `git diff --check`：✅ 通过
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 安装包重启：✅ 已通过 `screen` 会话 `yubi-demo` 启动 `/private/tmp/yu-bi-demo-run-20260701-2028.IuQ6sx`，使用本地 MySQL `127.0.0.1:3306/yubi`，`/api/v1/sys/info` 返回 `success: true`

待执行项：

- 浏览器人工复核筛选弹窗三处对齐和左右列表宽度
- Docker build 未执行，原因：本轮范围为前端本地验收修复，且本机 Docker 环境未确认可用

### FILE 预览列宽拖拽横向滚动保持修复（2026-07-01）

针对本地安装包验收中继续发现的“FILE 数据源预览表格拖拽列宽后，下方横向滚动条自动回到最左侧”问题进行补充修正：

- **当前状态**：已完成，等待浏览器人工复核
- **问题根因**：前一轮为解决 `react-window` Grid body 列宽缓存，`VirtualTable` 在列宽签名变化时通过 `key` 强制重建 Grid；重建能刷新 body 列宽，但新滚动容器默认 `scrollLeft=0`，导致横向滚动位置丢失。
- **修复方案**：`VirtualTable` 缓存最近一次横向滚动位置；列宽变化触发 Grid 重建后，将缓存的 `scrollLeft` 按新滚动范围 clamp 并恢复到新 Grid 容器，避免列宽拖拽时滚动条跳回最左。
- **兼容性范围**：只修改前端虚拟表格滚动位置保持逻辑和测试；不修改列宽算法、后端接口、查询 payload、权限模型、数据库 schema、历史数据或发布包目录结构。

本轮验证：

- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/components/__tests__/VirtualTable.test.tsx src/app/pages/MainPage/pages/ViewPage/components/__tests__/SchemaTable.test.tsx`：✅ 2 files / 9 tests passed
- `git diff --check`：✅ 通过
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 安装包重启：✅ 已通过 `screen` 会话 `yubi-demo` 启动 `/private/tmp/yu-bi-demo-run-20260701-2040.KtjVnf`，使用本地 MySQL `127.0.0.1:3306/yubi`，`/api/v1/sys/info` 返回 `success: true`

待执行项：

- 浏览器人工复核 FILE 数据源预览拖拽列宽后，横向滚动条不再自动回到最左侧
- Docker build 未执行，原因：本轮范围为前端本地验收修复，且本机 Docker 环境未确认可用

### 筛选过滤弹窗二次布局修复（2026-07-01）

针对本地安装包验收中继续发现的“筛选过滤弹窗 label 颜色/间距不一致、条件 tab 控件宽度偏窄、自定义 tab 表格列宽不稳定、是否可见=显示后控制器下拉框坍缩到不可用宽度”问题进行补充修正：

- **当前状态**：已完成，等待浏览器人工复核
- **整体表单布局**：计划把筛选弹窗 label、控件区和校验提示区收敛为固定宽度布局，避免错误提示或 `Row/Space` 组合挤压控件。
- **显示态控制器修复**：计划将控制器下拉、单选类型下拉和 Slider 数值输入改为固定宽度 flex 布局，禁止下拉框坍缩。
- **三类筛选内容区修复**：计划保留常规 tab Transfer 宽版布局；条件 tab 固定为 560px；自定义 tab 表格固定为 640px 且列宽稳定。
- **弹窗盘点**：本轮同步静态检查 `useStateModal`、`ModalForm`、直接 `<Modal>`、`Transfer`、`Table/DragSortEditTable` 组合的同类风险，只做局部明确问题修复。
- **兼容性范围**：只修改前端筛选字段操作弹窗局部布局和测试；不修改筛选配置结构、请求 payload、后端接口、权限模型、数据库 schema、历史数据或发布包目录结构。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/hooks/__tests__/useFieldActionModal.test.tsx src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction/FilterControlPanel/__tests__/layout.test.ts`：✅ 2 files / 5 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 安装包重启：✅ 已通过 `screen` 会话 `yubi-demo` 启动 `/private/tmp/yu-bi-demo-run-20260701-2101.G4ORsV`，使用本地 MySQL `127.0.0.1:3306/yubi`，`/api/v1/sys/info` 返回 `success: true`

待执行项：

- 浏览器人工复核日期字段过滤弹窗：推荐/手动、是否可见=显示、控制器、宽度、自定义、条件五处布局
- Docker build 未执行，原因：本轮范围为前端本地验收修复，且本机 Docker 环境未确认可用

### FILE 预览列宽拖拽实时同步修复（2026-07-01）

针对用户录屏反馈的“数据源列表 -> FILE 类型数据源 -> 新增配置 -> 上传 csv 文件，预览表格拖拽列宽时，期望整个表格列宽跟随鼠标选中的列变化”问题进行补充修正：

- **当前状态**：已完成代码修复和前端定向验证，待重新打包、重启本地 8080 后浏览器人工复核。
- **问题根因**：上一轮通过 `key` 强制重建 `react-window` Grid 可以刷新 body 列宽，但拖拽过程中 Grid DOM 重建会让表头、虚拟 body 和横向滚动同步变得不稳定；录屏中表现为手柄/表头在动，数据行未稳定跟随选中列实时变化。
- **修复方案**：`VirtualTable` 取消列宽变化时强制重建 Grid，改为将列宽签名传入 `cellProps`，让 `react-window@2` 在同一个 Grid 实例内基于新的 `columnWidth` 和 props 重新计算列边界；继续保留横向滚动位置缓存，避免拖拽时滚动条回到最左。
- **覆盖范围**：FILE 数据源配置预览和数据视图查询结果共用 `SchemaTable -> VirtualTable` 链路，本轮修复同时覆盖两处共享表格列宽拖拽行为。
- **兼容性范围**：只修改前端虚拟表格列宽同步机制和测试；不修改列宽算法、后端接口、查询 payload、权限模型、数据库 schema、历史数据或发布包目录结构。

本轮验证：

- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/components/__tests__/VirtualTable.test.tsx src/app/pages/MainPage/pages/ViewPage/components/__tests__/SchemaTable.test.tsx`：✅ 2 files / 9 tests passed
- `git diff --check`：✅ 通过
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 安装包重启：⏸️ 未执行，原因：用户要求暂时不用重启服务；当前 8080 仍为旧 `screen` 会话 `34215.yubi-demo` / Java PID `34235`

待执行项：

- 用户确认后重启本地 8080 最新安装包并检查 `/api/v1/sys/info`
- 浏览器人工复核 FILE 数据源预览拖拽列宽时，表头和数据行随选中列同步变化
- Docker build 未执行，原因：本轮范围为前端本地验收修复，且本机 Docker 环境未确认可用

### 筛选条件操作符选中态居中修复（2026-07-01）

针对本地验收截图中“过滤弹窗 -> 条件 tab，操作符下拉框选中内容展示需要居中”的问题进行局部修正：

- **当前状态**：已完成代码修复和前端定向验证，未重启本地 8080。
- **问题根因**：条件 tab 使用 `Input addonBefore + Select` 组合，选中值沿用 AntD Select 默认左对齐，视觉上贴近左侧边框。
- **修复方案**：为条件操作符 Select 增加 `filter-condition-operator-select` 专用 class，并在 `CategoryConditionRelationSelector` 局部样式中将 `.ant-select-selector` 和 `.ant-select-selection-item` 设置为居中显示；不影响其它 Select 或下拉菜单选项布局。
- **兼容性范围**：只修改筛选字段操作弹窗条件 tab 的局部展示样式和布局测试；不修改筛选配置结构、请求 payload、后端接口、权限模型、数据库 schema、历史数据或发布包目录结构。
- **后续风险**：定向测试暴露既有 AntD 警告：`Input addonBefore` 已废弃，后续可评估迁移为 `Space.Compact`；本轮不扩大重构范围。

本轮验证：

- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction/FilterControlPanel/__tests__/layout.test.ts`：✅ 1 file / 5 tests passed
- 初次新增测试时因 `.ts` 文件内使用 JSX 导致解析失败，已改为 `React.createElement` 后复跑通过
- `git diff --check`：✅ 通过
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 固定目录重启：✅ 已清理历史 `/private/tmp/yu-bi-demo-run-*` 随机目录，仅保留 `/private/tmp/yu-bi-demo-run-current`；已通过 `screen` 会话 `68014.yubi-demo` 启动，Java PID `68069`，使用本地 MySQL `127.0.0.1:3306/yubi`，`/api/v1/sys/info` 返回 `success: true`

待执行项：

- 浏览器人工复核条件操作符选中内容居中
- Docker build 未执行，原因：本轮范围为前端局部样式修复，且本机 Docker 环境未确认可用

### 筛选自定义表格空态滚动条修复（2026-07-01）

针对本地验收截图中“过滤弹窗 -> 自定义 tab，右侧空白过宽、空表出现左右/上下滚动条、表头高度偏高”的问题进行局部修正：

- **当前状态**：已完成代码修复和前端定向验证，未重启本地 8080。
- **问题根因**：自定义 tab 表格宽度仍使用旧的 640px，未充分利用右侧可用空间；空表场景也沿用固定滚动区域，导致空状态下出现横向和纵向滚动条。
- **修复方案**：将筛选弹窗宽版控件宽度扩为 680px，自定义表格列宽调整为“值 260px / 标题 260px / 操作 160px”；空表时不设置 Table `scroll`，局部隐藏 `.ant-table-content` 溢出，并压缩表头和空状态高度。
- **兼容性范围**：只修改筛选字段操作弹窗自定义 tab 的局部展示样式和布局常量；不修改筛选配置结构、请求 payload、后端接口、权限模型、数据库 schema、历史数据或发布包目录结构。

本轮验证：

- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction/FilterControlPanel/__tests__/layout.test.ts`：✅ 1 file / 5 tests passed
- `git diff --check`：✅ 通过
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 固定目录重启：✅ 已清理历史 `/private/tmp/yu-bi-demo-run-*` 随机目录，仅保留 `/private/tmp/yu-bi-demo-run-current`；已通过 `screen` 会话 `68014.yubi-demo` 启动，Java PID `68069`，使用本地 MySQL `127.0.0.1:3306/yubi`，`/api/v1/sys/info` 返回 `success: true`

待执行项：

- 浏览器人工复核自定义 tab 空表不再出现横纵滚动条，且表头高度更紧凑
- Docker build 未执行，原因：本轮范围为前端局部样式修复，且本机 Docker 环境未确认可用

### 本地验收运行目录治理（2026-07-01）

为减少本地验收重启产生的临时目录堆积，本轮调整 8080 服务运行目录策略：

- **当前状态**：已完成。
- **目录策略**：后续本地 8080 验收服务固定使用 `/private/tmp/yu-bi-demo-run-current`，重启时覆盖该目录，不再创建带时间戳/随机后缀的新目录。
- **清理结果**：已删除历史 `/private/tmp/yu-bi-demo-run-*` 随机目录；当前仅保留 `/private/tmp/yu-bi-demo-run-current`。
- **运行状态**：`screen` 会话 `68014.yubi-demo`，Java PID `68069`，健康接口 `http://127.0.0.1:8080/api/v1/sys/info` 返回 `success: true`。
- **配置**：固定目录的 `config/yubi.conf` 已写入本地 MySQL 配置：`127.0.0.1:3306/yubi`、用户名 `root`、密码 `123456`。

### 计算字段弹窗布局与 Monaco 初始化修复（2026-07-01）

针对用户反馈的“数据视图 -> 数据模型 -> 新建计算字段”弹窗中字段列表左右间距拥挤、字段超过 4 个单词展示不全、弹窗底部出现横向滚动条、中间编辑区域加载失败的问题进行局部修复：

- **当前状态**：已完成代码修复、前端定向验证、安装包打包和本地 8080 固定目录覆盖重启。
- **问题根因**：
  - 计算字段弹窗仍使用 `StateModalSize.MIDDLE = 1000` 和 `Row/Col span=4/16/4` 三栏布局，左右栏实际宽度过窄，长字段和函数列表容易贴边或截断；弹窗 body 默认允许横向滚动，内部溢出会直接暴露为底部滑动条。
  - 通用 `MonacoEditor` 在 `editorWillMount` 之前先执行初始 `setTheme(theme)`，而计算字段编辑器的 `dqlTheme` 是在 `editorWillMount` 中注册的；主题未注册时设置主题会触发加载失败态。
- **修复方案**：
  - 计算字段弹窗宽度统一调整为 `1180px`，body 禁止横向滚动，最大高度保持可控。
  - `ChartComputedFieldSettingPanel` 从 AntD 栅格比例布局改为固定左右栏和弹性中间编辑区：字段/变量栏 `240px`，函数栏 `180px`，中间编辑器自适应。
  - 字段列表和变量列表增加水平 padding、单行省略和 `title` 兜底；结构树标题同样按单行省略处理，避免长字段挤坏布局。
  - `MonacoEditor` 初始化顺序调整为先执行 `editorWillMount` 注册语言/主题，再设置初始主题，保证 `dqlTheme` 已定义后再使用。
- **兼容性范围**：只修改前端计算字段弹窗布局、共享可搜索列表局部样式和 Monaco 初始化顺序；不修改计算字段数据结构、请求 payload、后端接口、权限模型、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`：✅ 0 errors
- `npm run test:ci -- src/app/components/MonacoEditor/__tests__/index.test.tsx src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartDataViewPanel/components/__tests__/ChartComputedFieldSettingPanel.test.tsx src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartDataViewPanel/components/ChartComputedFieldEditor/__tests__/ChartComputedFieldEditor.monaco.smoke.test.tsx`：✅ 3 files / 7 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 固定目录覆盖重启：✅ 已覆盖 `/private/tmp/yu-bi-demo-run-current`，保留 `files/` 上传文件目录；使用本地 MySQL `127.0.0.1:3306/yubi`；通过 `screen` 会话 `707.yubi-demo` 启动；健康接口 `/api/v1/sys/info` 返回 `success: true`；当前 Java PID `727`

待执行项：

- 浏览器人工复核计算字段弹窗字段列表展示、无底部横向滚动条、Monaco 编辑区正常加载
- Docker build 未执行，原因：本轮范围为前端局部弹窗和编辑器修复，且本机 Docker 环境未确认可用

### 计算字段 Monaco 二次降级修复（2026-07-02）

针对用户复核截图中“数据视图 -> 数据模型 -> 新建计算字段”中间编辑区域仍显示“编辑器加载失败”的问题进行补充修复：

- **当前状态**：已完成代码修复、前端定向验证、安装包打包和本地 8080 固定目录覆盖重启。
- **问题根因**：
  - 前一轮只修正了 `dqlTheme` 注册前后顺序，但 `MonacoEditor` 仍会把 `editorWillMount`、`editorDidMount`、主题设置、语言设置中的任意异常统一视为运行时加载失败。
  - 计算字段编辑器的 DQL 自定义语言、Monarch token provider、主题注册属于增强能力，不应阻断基础编辑器加载；重复语言注册、Monarch 规则类型变化、主题注册异常都应降级而不是显示失败态。
- **修复方案**：
  - `MonacoEditor` 将运行时加载失败和调用方扩展失败拆开处理：只有 `loadMonaco()` 真失败才展示“编辑器加载失败”；`editorWillMount`、`editorDidMount`、`setTheme`、`setModelLanguage` 异常会记录日志并使用基础编辑器继续加载。
  - 自定义主题失败时按主题名降级到内置 `vs-dark` / `vs`；语言设置或模型创建失败时降级到 `plaintext`，保证输入区域可用。
  - `ChartComputedFieldEditor` 的 DQL 注册改为防御式：已存在语言时跳过重复注册，token provider 和主题注册失败不向上传播；光标函数提示异常也不影响编辑器主体。
  - 新增回归测试覆盖扩展初始化失败、主题失败、挂载回调失败、DQL 重复注册和 DQL 注册失败等场景。
- **兼容性范围**：只修改通用 Monaco 包装层和计算字段 DQL 初始化防御；不修改计算字段表达式语法、保存格式、请求 payload、后端接口、权限模型、数据库 schema 或历史数据。
- **补充启动兜底**：本轮覆盖重启时发现安装包缺少外部 `server.port` 配置会触发 Spring Boot 4 循环占位符错误，已将 `config/profiles/application-config.yml` 中的默认端口改为字面量 `8080`，避免本地安装包因端口占位符自引用启动失败。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/components/MonacoEditor/__tests__/index.test.tsx src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartDataViewPanel/components/ChartComputedFieldEditor/__tests__/ChartComputedFieldEditor.monaco.smoke.test.tsx`（`frontend/`）：✅ 2 files / 10 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 配置兜底后复跑 `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，安装包内 `application-config.yml` 已包含 `server.port: 8080`
- 本地 8080 固定目录覆盖重启：✅ 已覆盖 `/private/tmp/yu-bi-demo-run-current`，保留 `files/` 上传目录；`screen` 会话 `33118.yubi-demo`，Java PID `33136`，健康接口 `/api/v1/sys/info` 返回 `success: true`
- 运行目录静态资源确认：✅ 当前 `/private/tmp/yu-bi-demo-run-current/static/static/js/useResizeObserver.D6nlYL3w.js` 已包含 Monaco 扩展失败降级、主题兜底、语言 `plaintext` 兜底和错误态保留逻辑
- 初次在仓库根目录执行 `npm run checkTs` / `npm run test:ci` 失败，原因：根目录没有 `package.json`；已切换到 `frontend/` 后复跑通过

待执行项：

- 浏览器人工复核计算字段弹窗中间编辑器可正常输入；如 DQL 高亮初始化失败，应降级为基础编辑器而不是失败态
- Docker build 未执行，原因：本轮范围为前端局部编辑器修复，且本机 Docker 环境未确认可用

### 列权限弹层与数据模型菜单细节修复（2026-07-02）

针对浏览器标注反馈的“列权限弹层 checkbox 和字段文案高度未对齐、数据模型计算字段操作菜单过宽、删除项与编辑项样式不一致”进行局部修复：

- **当前状态**：已完成代码修复、前端定向验证、安装包打包和本地 8080 固定目录覆盖重启。
- **问题范围**：
  - 列权限右侧弹层使用 `ColumnPermissions -> Popup -> Tree.check-list.medium`，checkbox 与树节点内容未在同一高度基线上对齐。
  - 数据模型中计算字段的“编辑/删除”操作菜单使用 `DataModelComputerFieldNode -> Popup -> Menu`，外层 Popover 默认内边距和菜单默认宽度导致整体过宽。
  - 删除项将 `Popconfirm` 放在 `MenuItemContent` 内部，触发布局结构与编辑项不同，导致 hover/选中时视觉样式不一致。
- **处理方案**：
  - 给列权限弹层增加专用 overlay class，仅在该弹层内修正 Tree checkbox、内容 wrapper、title 的 flex 对齐。
  - 给计算字段操作菜单增加专用 overlay class，按标注收敛为 95px 宽、上下 10px/左右 0 的弹层内边距，并稳定每个菜单项高度。
  - 调整删除项结构为 `Popconfirm` 包裹完整 `MenuItemContent`，保证编辑/删除两项使用同一内容布局。
- **兼容性范围**：只修改前端局部样式和菜单 label 结构；不修改字段模型、计算字段数据结构、权限配置 payload、后端接口、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/pages/MainPage/pages/ViewPage/Main/Properties/DataModelTree/__tests__/DataModelComputerFieldNode.test.tsx`（`frontend/`）：✅ 1 file / 1 test passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 固定目录覆盖重启：✅ 已覆盖 `/private/tmp/yu-bi-demo-run-current`，保留 `files/` 上传目录；`screen` 会话 `91138.yubi-demo`，Java PID `91147`；健康接口 `/api/v1/sys/info` 返回 `success: true`
- 运行目录静态资源确认：✅ 当前安装包静态资源已包含 `yubi-column-permission-tree-popup`、`column-permission-tree`、`yubi-data-model-computed-field-menu-popup`

待执行项：

- 浏览器人工复核列权限字段对齐、计算字段操作菜单尺寸、编辑/删除项样式一致性
- Docker build 未执行，原因：本轮范围为前端局部样式修复，且本机 Docker 环境未确认可用

### 数据源列表标题菜单尺寸修复（2026-07-02）

针对浏览器标注反馈的“数据源列表标题栏更多菜单中回收站/收起弹层过宽、上下间距偏大”进行局部修复：

- **当前状态**：已完成代码修复、前端定向验证、安装包打包和本地 8080 固定目录覆盖重启。
- **问题范围**：
  - 数据源列表标题栏使用通用 `ListTitle -> Popup -> MenuWrapper`，更多菜单当前继承 AntD/通用 Popup 默认内边距，视觉尺寸约 129px x 116px。
  - 用户标注期望该菜单收敛到约 110px x 105px，Popover 内边距调整为上下 10px、左右 0px。
- **处理方案**：
  - 扩展 `ListTitle.more` 的可选 `overlayClassName` 和 `itemClassName`，默认不传时保持所有其它列表标题菜单行为不变。
  - 初始修复为数据源列表标题栏增加专用 class；后续“同类侧栏标题菜单统一修复”已将其替换为共享 `SIDEBAR_TITLE_MORE_MENU_POPUP_CLASS` / `SIDEBAR_TITLE_MORE_MENU_ITEM_CLASS`。
  - 当前实现统一使用 `.yubi-sidebar-title-more-menu-popup` 收敛 Popover 宽度、内边距、Menu 宽度和菜单项高度。
- **兼容性范围**：只修改前端数据源列表标题栏更多菜单的局部展示样式；不修改数据源模型、保存 payload、后端接口、权限模型、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/components/__tests__/ListTitle.test.tsx`（`frontend/`）：✅ 1 file / 1 test passed；存在 AntD `Input.bordered` deprecation warning，非本轮新增问题
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 固定目录覆盖重启：✅ 已覆盖 `/private/tmp/yu-bi-demo-run-current`，保留 `files/` 上传目录；`screen` 会话 `5295.yubi-demo`，Java PID `5300`；健康接口 `/api/v1/sys/info` 返回 `success: true`
- 运行目录静态资源确认：✅ 初始安装包曾包含数据源专用 class；当前最新安装包已由“同类侧栏标题菜单统一修复”替换为 `yubi-sidebar-title-more-menu-popup`、`sidebar-title-more-menu-item`

待执行项：

- 浏览器人工复核已并入“同类侧栏标题菜单统一修复”的 5 个侧栏统一验收
- Docker build 未执行，原因：本轮范围为前端局部样式修复，且本机 Docker 环境未确认可用

### 同类侧栏标题菜单统一修复（2026-07-02）

针对用户要求“排查其它地方的这种侧栏，改成同样样式”，对全仓同类 `ListTitle.more` 侧栏菜单进行统一：

- **当前状态**：已完成代码修复、前端定向验证、安装包打包和本地 8080 固定目录覆盖重启。
- **盘点范围**：
  - 数据源侧栏：`SourcePage/Sidebar`
  - 数据视图侧栏：`ViewPage/Sidebar`
  - 定时任务侧栏：`SchedulePage/Sidebar`
  - 可视化文件夹侧栏：`VizPage/Sidebar/Folders`
  - 故事板侧栏：`VizPage/Sidebar/Storyboards`
- **处理方案**：
  - 在 `ListTitle` 中保留可选 `overlayClassName` / `itemClassName` 扩展点，并新增共享常量 `SIDEBAR_TITLE_MORE_MENU_POPUP_CLASS` / `SIDEBAR_TITLE_MORE_MENU_ITEM_CLASS`。
  - 5 个同类侧栏统一接入共享侧栏菜单 class，不再使用数据源/数据视图各自独立的菜单样式。
  - 在 `GlobalOverlays` 中使用单一 `.yubi-sidebar-title-more-menu-popup` 规则，统一 Popover 宽度 `105px`、padding `10px 0`、菜单项高度 `45px`。
- **兼容性范围**：只修改前端侧栏标题更多菜单的局部展示样式；不修改列表数据结构、路由、后端接口、权限模型、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/components/__tests__/ListTitle.test.tsx`（`frontend/`）：✅ 1 file / 1 test passed；存在 AntD `Input.bordered` deprecation warning，非本轮新增问题
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 固定目录覆盖重启：✅ 已覆盖 `/private/tmp/yu-bi-demo-run-current`，保留 `files/` 上传目录；`screen` 会话 `31830.yubi-demo`，Java PID `31835`；健康接口 `/api/v1/sys/info` 返回 `success: true`
- 运行目录静态资源确认：✅ 当前安装包静态资源已包含 `yubi-sidebar-title-more-menu-popup`、`sidebar-title-more-menu-item`

待执行项：

- 浏览器人工复核数据源、数据视图、定时任务、可视化文件夹、故事板 5 个侧栏的“回收站/收起”菜单统一样式
- Docker build 未执行，原因：本轮范围为前端局部样式修复，且本机 Docker 环境未确认可用

### 同类侧栏标题菜单二次尺寸收敛（2026-07-02）

针对最新浏览器标注反馈，将已统一的 5 个侧栏标题栏“回收站/收起”菜单继续按当前标注尺寸收敛：

- **当前状态**：已完成代码修复、前端定向验证、安装包打包和本地 8080 固定目录覆盖重启。
- **覆盖范围**：继续复用“同类侧栏标题菜单统一修复”的 5 个侧栏：数据源、数据视图、定时任务、可视化文件夹、故事板。
- **处理方案**：
  - 保持 5 个侧栏继续使用共享 `.yubi-sidebar-title-more-menu-popup`，不新增页面级分裂样式。
  - 将共享 Popover 宽度保持为 `105px`，高度固定为 `110px`。
  - 将共享 Popover padding 从 `10px 0` 调整为 `1px 0`。
  - 两个菜单项高度调整为 `54px`，与 `1px + 54px + 54px + 1px = 110px` 对齐。
- **兼容性范围**：只修改前端侧栏标题更多菜单的局部展示样式；不修改列表数据结构、路由、后端接口、权限模型、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/components/__tests__/ListTitle.test.tsx`（`frontend/`）：✅ 1 file / 1 test passed；存在 AntD `Input.bordered` deprecation warning，非本轮新增问题
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 固定目录覆盖重启：✅ 已覆盖 `/private/tmp/yu-bi-demo-run-current`，保留 `files/` 上传目录；`screen` 会话 `67846.yubi-demo`，Java PID `67849`；健康接口 `/api/v1/sys/info` 返回 `success: true`
- 运行目录静态资源确认：✅ 当前安装包静态资源已包含 `yubi-sidebar-title-more-menu-popup`、`sidebar-title-more-menu-item`，且源码中旧的 `yubi-source-list-more-menu-popup` / `source-list-more-menu-item` / `yubi-view-list-more-menu-popup` / `view-list-more-menu-item` 已无残留

待执行项：

- 浏览器人工复核数据源、数据视图、定时任务、可视化文件夹、故事板 5 个侧栏的“回收站/收起”菜单最新尺寸
- Docker build 未执行，原因：本轮范围为前端局部样式修复，且本机 Docker 环境未确认可用

### 同类侧栏标题菜单右侧边界线修复（2026-07-03）

针对浏览器标注反馈的“回收站/收起菜单右侧有一条竖线”进行补充修复：

- **当前状态**：已完成代码修复、前端定向验证、安装包打包和本地 8080 固定目录覆盖重启。
- **问题根因**：
  - 共享侧栏菜单项使用 `width: 100%` 后又叠加水平 padding，默认 content-box 盒模型会让实际宽度超过 `105px` 容器。
  - 在 Popover 固定宽高且菜单项高度固定后，超出的内容可能在右侧被裁切成一条边界线。
- **覆盖范围**：
  - 数据源、数据视图、定时任务、可视化文件夹、故事板 5 个共享 `.yubi-sidebar-title-more-menu-popup` 的侧栏菜单。
  - 同步给数据模型计算字段编辑/删除菜单补同类 `box-sizing` 和 overflow 约束，避免同类窄菜单再次出现右侧溢出边界。
- **处理方案**：
  - `.yubi-sidebar-title-more-menu-popup .ant-popover-inner` 增加 `overflow: hidden` 和 `border: 0`。
  - `.yubi-sidebar-title-more-menu-popup .ant-dropdown-menu` 增加 `overflow: hidden`。
  - `.sidebar-title-more-menu-item` 增加 `box-sizing: border-box` 和 `overflow: hidden`，保证 `105px` 容器内的水平 padding 不再撑出右侧边界。
  - `.yubi-data-model-computed-field-menu-popup` 使用同类收敛方式，保持计算字段菜单宽度不被 padding 撑出。
- **兼容性范围**：只修改前端局部 overlay 样式；不修改侧栏数据结构、路由、回收站/收起行为、后端接口、权限模型、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/components/__tests__/ListTitle.test.tsx`（`frontend/`）：✅ 1 file / 1 test passed；存在 AntD `Input.bordered` deprecation warning，非本轮新增问题
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 固定目录覆盖重启：✅ 已覆盖 `/private/tmp/yu-bi-demo-run-current`，保留 `files/` 上传目录；`screen` 会话 `96666.yubi-demo`，Java PID `96669`；健康接口 `/api/v1/sys/info` 返回 `success: true`
- 运行目录静态资源确认：✅ 当前安装包静态资源已包含共享侧栏菜单 class 以及 `box-sizing:border-box` / `overflow:hidden` 修复

待执行项：

- 浏览器人工复核数据视图列表侧栏“回收站/收起”菜单右侧不再出现竖线
- Docker build 未执行，原因：本轮范围为前端局部样式修复，且本机 Docker 环境未确认可用

### BoardEditor 画布 SplitPane 拖拽联动修复（2026-07-03）

针对浏览器标注反馈的“看板编辑器画布右侧无法根据右侧面板分割线左拉而左移”进行局部修复：

- **当前状态**：已完成代码修复、前端定向验证、安装包打包和本地 8080 固定目录覆盖重启。
- **问题根因**：
  - `AutoBoardEditor` desktop 模式外层保留 `min-width: 769px`，当右侧“面板”向左拖拽导致中间画布 Pane 小于 769px 时，SplitPane 已移动但画布内部仍按 769px 最小宽度溢出。
  - BoardEditor 仅在 `onDragFinished` 触发 `dispatchResize`，自动布局画布内的 `react-grid-layout` 在拖拽过程中无法实时重新测量容器宽度。
- **覆盖范围**：
  - 自动布局看板编辑器：`AutoEditor` / `AutoBoardEditor`
  - 自由布局看板编辑器：`FreeEditor` / `FreeBoardEditor` 的同类 SplitPane resize 联动兜底
- **处理方案**：
  - `AutoBoardEditor` 外层从 `width: 100px`、desktop `min-width: 769px` 改为 `width: 100%`、`min-width: 0`，允许画布随 SplitPane Pane 宽度收缩。
  - 自动布局 `.grid-wrap` 增加 `min-width: 0`，避免内部滚动容器撑破父 Pane。
  - `AutoEditor` 左侧组件列表 SplitPane 和右侧面板 SplitPane 均在 `onChange` 与 `onDragFinished` 中触发 `dispatchResize`，让 `react-grid-layout` 在拖拽过程中实时测量。
  - `FreeEditor` 和 `FreeBoardEditor` 同步增加 SplitPane resize 触发与 `min-width: 0` 收缩兜底，避免自由布局画布出现同类问题。
  - 新增 `BoardEditor split layout` 定向测试，锁定 Auto/Free 编辑器 SplitPane 的 `onChange` / `onDragFinished` resize 契约。
- **兼容性范围**：只修改前端看板编辑器布局和 resize 触发；不修改看板配置数据结构、组件布局数据、保存 payload、后端接口、权限模型、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/layout.test.tsx src/app/components/__tests__/SplitPane.test.tsx`（`frontend/`）：✅ 2 files / 7 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 固定目录覆盖重启：✅ 已覆盖 `/private/tmp/yu-bi-demo-run-current`，保留 `files/` 上传目录；`screen` 会话 `13626.yubi-demo`，Java PID `13629`；健康接口 `/api/v1/sys/info` 返回 `success: true`
- 运行目录静态资源确认：✅ 当前安装包 `VizPage` chunk 已包含 BoardEditor SplitPane `dispatchResize` 和画布 `min-width: 0` 修复

待执行项：

- 浏览器人工复核看板编辑器右侧“面板”分割线左拉时，画布右边缘跟随左移且表格组件不再压到面板下方
- Docker build 未执行，原因：本轮范围为前端局部布局修复，且本机 Docker 环境未确认可用

### 图表工作台数据视图选择器留白修复（2026-07-03）

针对浏览器标注反馈的“看板编辑器左侧数据视图选择器树项左边留白太多”进行局部修复：

- **当前状态**：已完成代码修复、前端类型检查、安装包打包和本地 8080 固定目录覆盖重启。
- **问题根因**：
  - 被标注的 `test` 节点实际来自图表工作台数据视图 `TreeSelect` 下拉树，而不是字段拖拽列表。
  - AntD 6 的 `TreeSelect` 叶子节点默认仍保留 `24px` 的 `switcher-noop` 占位，并叠加内容区左右 padding，顶层叶子节点文字起始位置偏右。
- **处理方案**：
  - 给图表工作台数据视图选择器增加专用 `popupClassName="yubi-chart-dataview-selector-popup"`。
  - 在 `GlobalOverlays` 中只针对该 popup 收敛树项布局：叶子 `switcher-noop` 宽度改为 `0`，展开图标和缩进单位保留较窄宽度，文字内容区 padding 收敛为 `4px 8px`。
  - 不修改全局 `TreeSelect` 或通用 `Tree` 组件，避免影响权限树、数据模型树、控制器树等其它已修复区域。
- **兼容性范围**：只修改前端图表工作台数据视图选择器下拉树的局部展示样式；不修改数据视图选择逻辑、字段拖拽逻辑、看板配置数据、后端接口、权限模型、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 构建产物静态资源确认：✅ `frontend/build` / `server/target/frontend-static` 已包含 `yubi-chart-dataview-selector-popup` 和 `ant-select-tree-switcher-noop` 局部收敛规则
- 本地 8080 固定目录覆盖重启：✅ 已覆盖 `/private/tmp/yu-bi-demo-run-current`，保留 `files/` 上传目录；`screen` 会话 `62117.yubi-demo`，Java PID `62122`；健康接口 `/api/v1/sys/info` 返回 `success: true`
- 浏览器运行时 CSSOM 确认：✅ 当前页面已注入 `.yubi-chart-dataview-selector-popup`，最终规则为 `switcher-noop width: 0px`、`margin-right: 4px`、`node-content-wrapper padding-inline: 4px 8px`

待执行项：

- 浏览器人工复核数据视图选择器下拉树顶层叶子节点文字不再明显偏右
- Docker build 未执行，原因：本轮范围为前端局部样式修复，且本机 Docker 环境未确认可用

### Popover 内嵌 Menu 右侧竖线修复（2026-07-04）

针对浏览器标注反馈的“数据视图侧栏回收站/收起菜单右侧仍有竖线”进行运行时复核和同类盘点：

- **当前状态**：已完成代码修复、前端定向验证、安装包打包和本地 8080 固定目录覆盖重启。
- **运行时根因**：
  - 浏览器 CSSOM / computed style 确认，当前竖线来自 AntD 6 垂直 `Menu` 的默认右边框：`.ant-dropdown-menu { border-right: 1px solid rgba(5,5,5,.06) }`。
  - 该菜单是 Popover 弹层内嵌菜单，不是页面侧栏导航菜单；作为浮层菜单时不应保留垂直 Menu 的右边框。
  - 同时确认 AntD 6 当前真实 Popover 容器为 `.ant-popover-container`，旧的 `.ant-popover-inner` 兼容规则未完全命中，导致侧栏标题菜单仍保留默认 `12px` 容器 padding。
- **同类盘点范围**：
  - 5 个侧栏标题“回收站/收起”菜单：数据源、数据视图、定时任务、可视化文件夹、故事板。
  - 数据模型计算字段“编辑/删除”窄菜单。
  - 经 `Popup` 包装的 `.yubi-popup` 内嵌菜单。
  - Schema 表头菜单 `.yubi-schema-table-header-menu` 和图表数据配置菜单 `.yubi-data-section-dropdown`。
- **处理方案**：
  - 在 `.yubi-popup .ant-dropdown-menu` 统一去掉 `border-inline-end` / `border-right`，覆盖通用 Popover 内嵌菜单的右侧竖线。
  - 在 `.yubi-sidebar-title-more-menu-popup` 和 `.yubi-data-model-computed-field-menu-popup` 同步去掉 `border-inline-end` / `border-right`，并兼容 `.ant-popover-container` / `.ant-popover-content`。
  - 在 `.yubi-schema-table-header-menu` 和 `.yubi-data-section-dropdown` 去掉同类菜单右边框。
  - 侧栏标题菜单额外去掉 AntD 默认 `Menu.Item` margin，确保固定 `105px x 110px` 尺寸不再被默认 margin/padding 撑开。
- **兼容性范围**：只修改前端浮层菜单展示样式；不修改页面级导航菜单、侧栏数据结构、回收站/收起行为、字段菜单行为、后端接口、权限模型、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/components/__tests__/ListTitle.test.tsx`（`frontend/`）：✅ 1 file / 1 test passed；存在 AntD `Input.bordered` deprecation warning，非本轮新增问题
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 固定目录覆盖重启：✅ 已覆盖 `/private/tmp/yu-bi-demo-run-current`，保留 `files/` 上传目录；`screen` 会话 `67982.yubi-demo`，Java PID `67985`；健康接口 `/api/v1/sys/info` 返回 `success: true`
- 浏览器运行时 CSSOM 确认：✅ 数据视图详情页“回收站/收起”菜单 `.ant-dropdown-menu`、`.ant-popover-container`、`.ant-dropdown-menu-item` 的 `borderRightWidth` / `borderInlineEndWidth` 均为 `0px`

待执行项：

- 浏览器人工复核数据视图详情页“回收站/收起”菜单右侧竖线消失
- Docker build 未执行，原因：本轮范围为前端局部样式修复，且本机 Docker 环境未确认可用

### 筛选过滤弹窗对齐补充修复（2026-07-04）

针对浏览器标注反馈的图表编辑器“过滤”字段操作弹窗继续做局部布局修复：

- **当前状态**：已完成代码修复、前端类型检查、定向测试和安装包打包验证；本轮未重启本地 8080 服务。
- **问题根因**：
  - `筛选方式` 表单项右侧内容是 `Tabs`，但 `FormItemEx` 的控制区默认按整块内容高度居中，导致切换到“常规 / 自定义 / 条件”不同内容高度时，label 与 tabs 导航行的视觉对齐不稳定。
  - Ant Design 6 的 `Select` 当前实际渲染结构为 `.ant-select-content + .ant-select-suffix`，前序兼容旧结构的 `.ant-select-selector / .ant-select-selection-item` 居中规则没有完全命中当前条件操作符选择框。
  - 全局 `.ant-form-item-label > label` 已使用 `textColorLight`，对应 `rgb(126, 130, 153)`；过滤弹窗自身又把 label 覆盖成 `textColorSnd`，导致“名称 / 聚合方式 / 筛选方式 / 是否可见”等左侧选项文本颜色偏深。
  - `请输入筛选方式` 是用户需要保留的校验/提示文案，本轮不移除该文案，也不放宽筛选条件校验。
- **处理方案**：
  - 取消前序固定 `16px` label 偏移方案，改为让 `.filter-option-form-item` 的控制区从顶部开始布局，使 `筛选方式` label 自然与右侧 tabs 导航行对齐，不再依赖当前选中的 tab 内容高度。
  - 在条件筛选操作符 `Select` 上补充 AntD 6 `.ant-select-content`、`.ant-select-input`、`.ant-select-suffix` 局部样式，让已选值在可见选择框内居中展示，并保留旧版选择器兼容规则。
  - 将过滤弹窗内表单 label 的局部颜色覆盖改为 `textColorLight`，与其它 AntD 表单弹窗的左侧选项文本保持一致。
  - 同步更新筛选弹窗 layout 契约测试和字段弹窗宽度测试，保持当前宽版布局目标 `680px`。
- **兼容性范围**：只修改前端筛选字段操作弹窗局部展示样式和测试断言；不修改筛选配置数据结构、请求 payload、后端接口、权限模型、数据库 schema、历史数据或校验语义。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction/FilterControlPanel/__tests__/layout.test.ts src/app/hooks/__tests__/useFieldActionModal.test.tsx`（`frontend/`）：✅ 2 files / 6 tests passed；存在 AntD `Input.addonBefore` deprecation warning，非本轮新增阻断问题
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`；前端 `vite build` 和 `build:task` 均通过

待执行项：

- 浏览器人工复核图表编辑器过滤弹窗：左侧选项文本颜色为 `rgb(126, 130, 153)`、条件操作符已选值居中、`筛选方式` label 在常规/自定义/条件间切换时均与右侧 tabs 行对齐、`请输入筛选方式` 提示文案保留
- 如需要本地 8080 验收，使用最新安装包覆盖固定目录后重启；本轮按用户最新反馈未主动重启
- Docker build 未执行，原因：本轮范围为前端局部样式修复，且本机 Docker 环境未确认可用

### 筛选自定义表格滚动条修复（2026-07-04）

针对浏览器标注反馈的过滤弹窗“自定义”tab 表格出现下方横向滚动条和右侧纵向滚动条进行补充修复：

- **当前状态**：已完成代码修复、前端类型检查、定向测试和安装包打包验证；本轮未重启本地 8080 服务。
- **问题根因**：
  - 自定义表格宽度沿用宽版布局 `680px`，但过滤弹窗当前表单控件可见宽度为 `560px`，导致表格横向溢出并出现底部滚动条。
  - `CategoryConditionConfiguration` 的 `.ant-tabs-content-holder` 设置了 `max-height: 600px` 和 `overflow-y: auto`，在自定义 tab 中即使内容不需要独立滚动，也会出现右侧滚动条。
  - `DragSortEditTable` 在有数据行时传入 `scroll.x`，会让表格内部优先生成横向滚动容器。
- **处理方案**：
  - 将自定义表格宽度收敛到 `FILTER_FORM_CONTROL_WIDTH = 560px`，列宽调整为“值 220px / 标题 220px / 操作 120px”，总宽严格等于控件宽度。
  - 移除自定义表格的 `scroll.x`，并显式关闭表格内容区内部滚动，避免空数据和新增行后产生底部横向滚动条。
  - 将 tabs 内容区从 `overflow-y: auto` 改为 `overflow: visible`，让当前 tab 内容完整展示，不再出现右侧独立纵向滚动条。
- **兼容性范围**：只修改过滤弹窗自定义 tab 的局部展示尺寸和滚动行为；不修改筛选配置数据结构、请求 payload、保存逻辑、后端接口、权限模型、数据库 schema 或历史数据。常规 Transfer 宽版布局和条件筛选布局保持不变。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction/FilterControlPanel/__tests__/layout.test.ts src/app/hooks/__tests__/useFieldActionModal.test.tsx`（`frontend/`）：✅ 2 files / 6 tests passed；存在 AntD `Input.addonBefore` deprecation warning，非本轮新增阻断问题
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`；前端 `vite build` 和 `build:task` 均通过

待执行项：

- 浏览器人工复核过滤弹窗自定义 tab：默认无底部横向滚动条、无右侧独立纵向滚动条；空数据和新增行后表格内容可完整展示
- 如需要本地 8080 验收，使用最新安装包覆盖固定目录后重启
- Docker build 未执行，原因：本轮范围为前端局部样式修复，且本机 Docker 环境未确认可用

### 筛选错误提示垂直位置修复（2026-07-04）

针对浏览器标注反馈的过滤弹窗错误提示位置进行补充修复：

- **当前状态**：已完成代码修复、前端类型检查、定向测试和安装包打包验证；准备重启本地 8080 服务。
- **问题根因**：`filterOption` 表单项右侧内容区较高，错误提示沿用通用 explain 顶部位置，导致“请输入筛选方式”贴近 tabs 行，未处于 `筛选方式` 与 `是否可见` 之间的中部位置。
- **处理方案**：保留默认 required 校验和原文案“请输入筛选方式”不变；仅在 `.filter-option-form-item` 内将 `.ant-form-item-explain` 设置为垂直居中。
- **兼容性范围**：只修改过滤弹窗内 `filterOption` 错误提示的局部对齐；不修改校验文案、筛选配置数据结构、请求 payload、保存逻辑、后端接口、权限模型、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartFieldAction/FilterControlPanel/__tests__/layout.test.ts src/app/hooks/__tests__/useFieldActionModal.test.tsx`（`frontend/`）：✅ 2 files / 6 tests passed；存在 AntD `Input.addonBefore` deprecation warning，非本轮新增阻断问题
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`；前端 `vite build` 和 `build:task` 均通过

待执行项：

- 浏览器人工复核过滤弹窗：错误提示仍显示“请输入筛选方式”，并在筛选方式内容块中垂直居中
- Docker build 未执行，原因：本轮范围为前端局部样式修复，且本机 Docker 环境未确认可用

### 计算字段弹窗 tab 内边距修复（2026-07-04）

针对浏览器标注反馈的数据视图“新建计算字段”弹窗左侧 `字段 / 变量` tab 文字贴边问题进行局部样式修复：

- **当前状态**：已完成代码修复、契约测试补充、前端类型检查、定向测试和安装包打包验证；本轮未重启本地 8080 服务。
- **问题根因**：计算字段弹窗左侧 `SidePanel` 里的 AntD Tabs 在现代化布局收敛时使用了 `padding: 8px 0`，左右内边距为 0，选中态下文字和角标过于贴边。
- **处理方案**：新增 `CHART_COMPUTED_FIELD_TAB_HORIZONTAL_PADDING = 8`，将 `.ant-tabs-tab` 调整为 `padding: 8px 8px`，只作用于计算字段弹窗左侧 tab。
- **兼容性范围**：只修改前端局部展示样式；不修改计算字段表达式、字段/变量列表数据结构、保存 payload、后端接口、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/components/Popup/__tests__/MenuListItem.test.tsx src/app/components/MonacoEditor/__tests__/index.test.tsx src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartDataViewPanel/components/__tests__/ChartComputedFieldSettingPanel.test.tsx`（`frontend/`）：✅ 3 files / 9 tests passed；存在 AntD `Space.direction`、`List` deprecation 和测试 FormContext warning，非本轮新增阻断问题
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`；前端 `vite build` 和 `build:task` 均通过

待执行项：

- 浏览器人工复核新建计算字段弹窗左侧 `字段 / 变量` tab 左右内边距为 8px，角标和文字不再贴边
- Docker build 未执行，原因：本轮范围为前端局部样式修复，且本机 Docker 环境未确认可用

### Monaco 弹窗主题泄漏修复（2026-07-04）

针对浏览器标注反馈的“点击新建计算字段后，背后的 SQL 编辑器变黑，刷新后恢复”问题进行通用 Monaco 包装层修复：

- **当前状态**：已完成代码修复、单元测试补充、前端类型检查、定向测试和安装包打包验证；本轮未重启本地 8080 服务。
- **问题根因**：Monaco 的 `editor.setTheme(...)` 是全局主题切换。计算字段弹窗内编辑器使用 `dqlTheme`，挂载时会把外层数据视图 SQL 编辑器也切成深色主题；弹窗关闭后外层 SQL 编辑器的 `theme` prop 未变化，不会自动重新设置回 `vs-light`。
- **处理方案**：在通用 `MonacoEditor` 包装层维护实例主题栈：编辑器挂载/主题变化时注册当前主题，卸载时移除当前实例并恢复前一个仍然存在的编辑器主题。
- **兼容性范围**：只修改前端 Monaco 主题生命周期管理；不修改 SQL 内容、计算字段表达式、查询 payload、后端接口、权限模型、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/components/Popup/__tests__/MenuListItem.test.tsx src/app/components/MonacoEditor/__tests__/index.test.tsx src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/components/ChartDataViewPanel/components/__tests__/ChartComputedFieldSettingPanel.test.tsx`（`frontend/`）：✅ 3 files / 9 tests passed；存在 AntD `Space.direction`、`List` deprecation 和测试 FormContext warning，非本轮新增阻断问题
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`；前端 `vite build` 和 `build:task` 均通过

待执行项：

- 浏览器人工复核数据视图打开并关闭新建计算字段弹窗后，外层 SQL 编辑器保持原主题，不再残留深色主题
- Docker build 未执行，原因：本轮范围为前端局部生命周期和样式修复，且本机 Docker 环境未确认可用

### 菜单项 Popconfirm 对齐修复（2026-07-04）

针对浏览器标注反馈的可视化侧栏菜单中“移至回收站”与“基本信息 / 另存为”样式不一致问题进行共享组件修复：

- **当前状态**：已完成代码修复、单元测试补充、前端类型检查、定向测试和安装包打包验证；本轮未重启本地 8080 服务。
- **问题根因**：全仓盘点发现 `ViewPage / SourcePage / SchedulePage / VizPage` 多处菜单项使用 `MenuItemContent` 包裹 `Popconfirm`。`Popconfirm` 会把文字触发器渲染成额外 `span`，实际 DOM 中该 `span` 为 `display: block; height: 40px; line-height: 40px`，而普通菜单项是直接文本节点，导致同一菜单内的图标和文案垂直对齐不一致。
- **处理方案**：保持 `MenuItemContent` 的整体 div 结构不变，不再增加统一 `.content` 包裹层；仅对菜单项直接子 `span:not(.prefix):not(.suffix)` 和 `p` 设置 `inline-flex`、居中、溢出省略和 `line-height: 1`，让 `Popconfirm` 文字触发器像普通文本一样与图标对齐。
- **兼容性范围**：只修改前端菜单项展示结构和样式；不修改确认弹窗行为、删除/回收业务逻辑、接口、权限模型、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/components/Popup/__tests__/MenuListItem.test.tsx`（`frontend/`）：✅ 1 file / 1 test passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`；前端 `vite build` 和 `build:task` 均通过

待执行项：

- 浏览器人工复核可视化、数据视图、数据源、定时任务等侧栏菜单中带 Popconfirm 的删除/回收项，与普通菜单项图标和文本对齐一致
- Docker build 未执行，原因：本轮范围为前端局部展示和生命周期修复，且本机 Docker 环境未确认可用

### 侧栏树节点更多菜单尺寸收敛（2026-07-05）

针对浏览器标注反馈的可视化树节点菜单“移至回收站”图标/文案间距仍需与“基本信息”一致，以及整个浮层方框偏大的问题进行同类修复：

- **当前状态**：已完成代码修复、前端类型检查、定向测试和安装包打包验证；本轮未重启本地 8080 服务。
- **问题根因**：该菜单不是页标题“回收站 / 收起”二项菜单，而是侧栏树节点 `TreeTitle + Popup + Menu` 更多菜单，之前未挂载专用紧凑类名，仍受通用 Popover 内层留白影响；同时 `Popconfirm` 菜单项需要继续复用 `MenuItemContent` 的直接子 `span` 对齐规则。
- **处理方案**：新增 `TREE_MORE_MENU_POPUP_CLASS` / `TREE_MORE_MENU_ITEM_CLASS`，将数据视图、数据源、定时任务、可视化、故事板及各自回收站中的树节点更多菜单统一挂载到 `yubi-tree-more-menu-popup`；该浮层使用自然高度、最小宽度和 36px 菜单项高度，保留图标到文案 8px 间距，避免固定大框套住三项菜单。
- **兼容性范围**：只修改前端侧栏树节点菜单展示样式和 class 挂载；不修改菜单项数据、确认弹窗行为、删除/回收/恢复/另存为业务逻辑、后端接口、权限模型、数据库 schema 或历史数据。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/components/Popup/__tests__/MenuListItem.test.tsx src/app/components/__tests__/ListTitle.test.tsx`（`frontend/`）：✅ 2 files / 2 tests passed；存在 AntD `Input.bordered` deprecation warning，非本轮新增阻断问题
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`；前端 `vite build` 和 `build:task` 均通过

待执行项：

- 浏览器人工复核可视化详情侧栏节点菜单：`基本信息 / 另存为 / 移至回收站` 三项的图标与文案间距一致，浮层外框不再明显偏大
- 如需本地 8080 验收，使用最新安装包覆盖固定目录后重启服务
- Docker build 未执行，原因：本轮范围为前端局部展示修复，且本机 Docker 环境未确认可用

### 看板编辑视图与取消报错修复（2026-07-05）

针对用户录屏反馈的“编辑视图”以及“取消”报错进行排查和修复：

- **当前状态**：已完成代码修复、路由契约测试补充、前端类型检查、定向测试和安装包打包验证；本轮未重启本地 8080 服务。
- **问题根因**：
  - 看板编辑页初始化时，只要历史状态中存在 `widgetInfo` 就无条件打印 `console.error('if you see the error on board editor...')`，该日志不是运行异常，但会误导为编辑页报错。
  - 编辑态图表/控制器组件请求使用 `request2(..., { onRejected })` 后继续解构返回值；`onRejected` 没有返回标准 `APIResponse` 时会触发 thunk rejected，控制台显示 `Redux Rejection Error`。
  - 路由级图表编辑器的取消动作使用 `navigate.go(-1)`，在直接打开或历史栈被“编辑数据视图”确认跳转改变后，可能回到不稳定位置。
- **处理方案**：
  - 删除 BoardEditor 初始化中的误导性 `console.error`。
  - 将 `syncEditBoardWidgetChartDataAsync`、`getEditChartWidgetDataAsync`、`getEditControllerOptions` 改为显式 `try/catch`：请求失败时写入组件错误态并清空数据，但 thunk 自身正常返回，避免 Redux rejected 冒泡。
  - 新增 `getChartEditorClosePath`，路由级图表编辑器取消时优先回到图表详情；新建图表且带 `defaultViewId` 时回到数据视图详情；无上下文时回到可视化列表。
- **兼容性范围**：不修改图表配置、看板配置、查询 payload、后端接口、权限模型、数据库 schema 或历史数据；只收敛前端错误处理和路由级关闭目标。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/pages/MainPage/__tests__/routes.test.ts src/app/pages/DashBoardPage/pages/BoardEditor/__tests__/index.test.ts src/app/pages/DashBoardPage/pages/BoardEditor/slice/__tests__/events.test.ts`（`frontend/`）：✅ 3 files / 29 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`；前端 `vite build` 和 `build:task` 均通过

待执行项：

- 浏览器人工复核看板详情页点击编辑进入 `/boardEditor` 后，不再出现误导性 BoardEditor error 日志；编辑态 widget 请求失败时只在组件内显示错误，不再触发 Redux rejected。
- 人工复核图表编辑页“取消”：已有图表返回图表详情，新建图表返回原数据视图详情或可视化列表。
- 如需本地 8080 验收，使用最新安装包覆盖固定目录后重启服务。
- Docker build 未执行，原因：本轮范围为前端错误处理和路由回退修复，且本机 Docker 环境未确认可用。

### 图表编辑冻结配置与菜单文字对齐修复（2026-07-05）

针对用户继续反馈的两个问题进行补充修复：

- **当前状态**：已完成代码修复、前端类型检查、定向测试、安装包打包验证和本地 8080 重启；浏览器复核顶部“编辑”进入图表编辑器后未再出现 `Cannot assign to read only property 'datas'`。
- **问题根因**：
  - `mergeToChartConfig(currentChart.config, ...)` 会直接改写图表插件默认配置对象。该对象被放入 Redux state 后在开发态被冻结，后续再次进入图表编辑/预览时执行 `target.datas = ...` 会抛出 `Cannot assign to read only property 'datas'`，并冒泡为 rejected。
  - 树节点更多菜单中不同图标的实际宽度略有差异；带 `Popconfirm` 的“移至回收站”文本触发器和普通文本菜单项虽然同属 `MenuItemContent`，但文字起点仍会受图标宽度影响。
- **处理方案**：
  - `mergeToChartConfig` 先 `CloneValueDeep(target)`，后续 `datas/styles/settings/interactions` 合并只写入克隆对象，不再污染 `ChartManager` 中的共享默认配置。
  - 新增冻结对象回归测试，覆盖默认图表配置被冻结时仍可合并，并断言原配置未被写入 `rows`。
  - `MenuItemContent` 的 `.prefix` 图标槽位固定为 16px、居中显示，保留 8px 图标到文字间距，让“另存为”和“移至回收站”文字起点一致。
- **兼容性范围**：不修改图表配置数据结构、保存 payload、接口、权限模型、数据库 schema 或历史数据；菜单修复仅调整共享菜单项展示样式，不修改确认弹窗行为。

本轮验证：

- `git diff --check`：✅ 通过
- `npm run checkTs`（`frontend/`）：✅ 0 errors
- `npm run test:ci -- src/app/utils/__tests__/chartDtoHelper.test.ts src/app/components/Popup/__tests__/MenuListItem.test.tsx`（`frontend/`）：✅ 2 files / 11 tests passed
- `mvn -pl server -am -DskipTests package`：✅ BUILD SUCCESS，重新生成 `yu-bi-server-2.0.0-install.zip`
- 本地 8080 固定目录覆盖重启：✅ `/api/v1/sys/info` 返回 `success: true`；`screen` 会话 `34813.yubi-demo`，Java PID `34852`
- 浏览器复核：✅ 点击数据图表详情页顶部“编辑”后进入 `/vizs/chartEditor?...`，本次未新增 error 级控制台日志

待执行项：

- 浏览器人工复核侧栏树节点菜单：“基本信息 / 另存为 / 移至回收站”文字起点一致。
- Docker build 未执行，原因：本轮范围为前端运行时配置合并和局部菜单样式修复，且本机 Docker 环境未确认可用。
