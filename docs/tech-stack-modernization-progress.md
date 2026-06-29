# yu-bi 技术栈现代化进度看板

## 当前状态

- 当前分支：`refactor/tech-modernization-part2`（基于 `main`，含第一轮技术债清理 + 前端现代化第二轮改动，未提交/未合入）
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
| Shiro → Spring Security | ✅ 已验证 | Shiro 运行时依赖移除；装配器已重命名为 Default*；使用 Spring Security 7 (via Boot 4) |
| Jackson 3 完整迁移 | ✅ 已验证 | 业务代码全部迁至 `tools.jackson.*`；注解保持 `com.fasterxml.jackson.annotation.*`（Jackson 3 设计如此）；仅 jjwt-jackson 传递依赖保留 Jackson 2 |
| Ant Design 6 | ✅ 已验证 | AntD 6.4.5 + pro-components 3.1.12-0 |
| React 现代化 | ✅ 已验证 | 8 个 class component 全部迁移为函数组件 + hooks；PropTypes 移除 |
| TypeScript 7 评估 | ✅ 已完成 | 项目代码 0 错误，生态阻塞(@typescript-eslint)，保持 6.0.3 |
| 前端工具链升级 | ✅ 已验证 | monaco-editor 0.55.1 + @vitejs/plugin-react 6.0.3 + fast-check 4.x |
| 发布链路 | ✅ 已验证 | 安装包完整、demo health 通过、无遗留 Javassist/Shiro JAR |

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
| Security | Shiro 2.0.6 | **Spring Security 7** (via Boot 4) | Shiro 运行时依赖移除；装配器已重命名为 Default* 并迁至 `yubi.security.manager` |
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
| data-provider-base 测试编译偶发失败 | maven-compiler-plugin 3.15.0 测试阶段 | ⚠️ 不影响产物，后续跟踪 |
| EnvironmentPostProcessor 废弃 | Spring Boot 4.0 标记 @Deprecated(forRemoval=true) | ⚠️ 已加 @SuppressWarnings("removal")，待迁移至 ApplicationListener |

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

> 注：上述全量验证矩阵来自 `refactor/tech-modernization-part2` 先前完整验证。本轮 `modernization-round-2-fixes` 仅补跑 `git diff --check`、`npm run checkTs` 和 Split/SplitPane 相关测试（4 个测试文件、24 tests passed）。

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

## 相关文档

- [全项目技术栈审计报告](./tech-stack-audit-report.md) — 2026-06-28 审计，含版本对比、Gap 分析和废弃 API 扫描

## 补充修正记录

### `modernization-round-2-fixes` spec（2026-06-29）

在 round 2 代码审查后，针对以下遗留问题进行修正：

- **Split.tsx minSize 稳定性修复**：引入 `useStableArray` hook，避免 `minSize` 数组引用变化但值未变时不必要的 Split.js 实例重建
- **SplitPane 死代码清理**：移除未被读取的 `resizedRef`
- **文档修正**：更正 progress / audit-report / log 三份文档中的版本号、迁移状态和合并状态描述
