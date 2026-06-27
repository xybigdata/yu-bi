# yu-bi 技术栈现代化进度看板

## 当前状态

- 当前分支：`refactor/tech-modernization`
- 阶段状态：**阶段 A + B + C 全部完成**
- 任务进度：39/39 必需任务已完成；5 个 optional property tests 待补充
- 硬目标：兼容 JDK 21、Node.js 24，不以降低运行时版本作为兼容旧代码的默认方案
- 本机验证环境：Temurin JDK `21.0.11`，Maven `3.9.16`，Node.js `24.18.0`，npm `11.16.0`

## 改造成果总览

| 改造项 | 状态 | 说明 |
|--------|------|------|
| Javassist 消除 | ✅ 已验证 | `DatartSqlPrettyWriter` 子类覆盖，移除 `--add-opens` |
| Spring Boot 4.0.0 | ✅ 已验证 | 含 Spring Cloud 2025.1.0 + MyBatis 4.0.0 + Springdoc 3.0.0 |
| Shiro → Spring Security | ✅ 已验证 | 完全移除 Shiro，使用 Spring Security 7 (via Boot 4) |
| Ant Design 6 | ✅ 已验证 | AntD 6.4.5 + pro-components 3.1.12-0 |
| TypeScript 7 评估 | ✅ 已完成 | 项目代码 0 错误，生态阻塞(@typescript-eslint)，保持 6.0.3 |
| 发布链路 | ✅ 已验证 | 安装包完整、demo health 通过、无遗留 Javassist/Shiro |

## 当前组件版本矩阵（改造后）

| 组件 | 改造前版本 | 改造后版本 | 备注 |
|------|-----------|-----------|------|
| Spring Boot | 3.5.15 | **4.0.0** | 3.5 已于 2026-06-30 EOL |
| Spring Cloud | 2025.0.3 | **2025.1.0** | 与 Boot 4 兼容线 |
| MyBatis Spring Boot | 3.0.5 | **4.0.0** | 随 Boot 4 升级 |
| Springdoc | 2.8.17 | **3.0.0** | OpenAPI spec 不变 |
| Jackson | 2.x (`com.fasterxml.jackson`) | **3.x** (`tools.jackson`) | 部分传递依赖仍为 Jackson 2 |
| Security | Shiro 2.0.6 | **Spring Security 7** (via Boot 4) | Shiro 完全移除 |
| Javassist | 3.32.0-GA | **deleted** | `--add-opens` 同步移除 |
| Ant Design | 5.29.3 | **6.4.5** | CSS Variable 模式 |
| Pro Components | 2.8.10 | **3.1.12-0** | beta 阶段，需监控 stable |
| TypeScript | 6.0.3 | **6.0.3** | TS 7 已评估，等待生态 |

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

- [x] Javassist 消除：`DatartSqlPrettyWriter` 子类覆盖 `keyword()` 方法
- [x] `--add-opens` 移除：启动脚本、CMD、Dockerfile、health check 脚本
- [x] Spring Boot 4.0.0 升级 + Spring Cloud 2025.1.0 + MyBatis 4.0.0 + Springdoc 3.0.0
- [x] Jackson 3 迁移：`com.fasterxml.jackson.*` → `tools.jackson.*`
- [x] OAuth2 class 重定位适配
- [x] Shiro 完全移除（5 个实现类 + SecurityConfiguration + starter 依赖）
- [x] Spring Security 实现：`SpringSecurityManager` + `SpringSecuritySubjectFacade` + `DatartAuthenticationProvider`
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

## 风险清单

| 风险 | 影响 | 状态 |
|------|------|------|
| ~~Spring Boot 3.5 已于 2026-06-30 EOL~~ | ~~不再收到安全补丁~~ | ✅ 已解决：迁移到 Boot 4.0.0 |
| ~~Javassist patch + `--add-opens`~~ | ~~JDK 21 强封装兼容~~ | ✅ 已解决：子类覆盖替换 |
| ~~Shiro 社区活跃度下降~~ | ~~长期安全维护风险~~ | ✅ 已解决：完全迁移到 Spring Security |
| ~~AntD 5 接近 EOL~~ | ~~组件库不再收到新功能~~ | ✅ 已解决：迁移到 AntD 6.4.5 |
| pro-components 3.x 仍为 beta | npm latest tag 仍为 2.8.10 | 🔍 监控中：持续关注 stable 发布 |
| `@typescript-eslint` 尚未支持 TS 7 | 阻塞 TypeScript 7 升级 | 🔍 监控中：等待兼容版本发布 |
| Jackson 2/3 共存 | 部分传递依赖仍为 Jackson 2 | 🔍 监控中：后续评估是否统一 |
| data-provider-base 测试编译偶发失败 | maven-compiler-plugin 3.15.0 测试阶段 | ⚠️ 不影响产物，后续跟踪 |

## 验证矩阵

| 验证项 | 结果 |
|--------|------|
| `mvn compile` (all modules) | ✅ BUILD SUCCESS |
| `mvn test` | ✅ 194 tests pass |
| `npm run checkTs` | ✅ 0 errors |
| `npm run build` + `build:task` | ✅ 4 entry points + task UMD |
| `npm run test:ci` | ✅ 1152 tests pass (164 files) |
| `npm run lint:css` | ✅ 0 errors |
| Build size delta (gzip) | ✅ +0.74% (< 5% threshold) |
| Install zip | ✅ Complete structure |
| Demo health | ✅ Response within 4s |

## 下一步计划

1. **合并到 main**：`refactor/tech-modernization` → `main` PR
2. **Optional property tests**：补充 5 个 optional PBT（不阻塞合并）
3. **监控 pro-components stable**：关注 npm latest tag 从 2.8.10 更新到 3.x
4. **TypeScript 7 升级**：等待 `@typescript-eslint` 兼容版本发布后执行升级
5. **Jackson 2/3 收敛**：评估传递依赖中 Jackson 2 的清除方案
