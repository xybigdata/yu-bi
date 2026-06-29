# 全项目技术栈审计报告

> 审计日期：2026-06-28（版本矩阵已更新至最新升级结果）  
> 审计范围：后端 POM 配置、前端 package.json、Dockerfile、启动脚本  
> 参考基准：`docs/tech-stack-modernization-progress.md`

---

## 1. 版本对比结果

### 1.1 后端核心（一致项 ✅）

| 组件 | progress.md 声明 | 实际值（pom.xml） | 状态 |
|------|-----------------|-------------------|------|
| Spring Boot parent | 4.0.7 | 4.0.7 | ✅ 一致 |
| Spring Cloud | 2025.1.2 | 2025.1.2 | ✅ 一致 |
| MyBatis Spring Boot | 4.0.0 | 4.0.0 | ✅ 一致 |
| Springdoc | 3.0.3 | 3.0.3 | ✅ 一致 |
| Calcite | 1.42.0 | 1.42.0 | ✅ 一致 |
| JDK | 21 | 21 | ✅ 一致 |

### 1.2 前端核心（一致项 ✅）

| 组件 | progress.md 声明 | 实际值（package.json） | 状态 |
|------|-----------------|------------------------|------|
| React | 19.2.7 | 19.2.7 | ✅ 一致 |
| Vite | 8.1.0 | 8.1.0 | ✅ 一致 |
| TypeScript | 6.0.3 | 6.0.3 | ✅ 一致 |
| Pro Components | 3.1.12-0 | 3.1.12-0 | ✅ 一致 |
| react-router-dom | 7.18.0 | 7.18.0 | ✅ 一致 |
| ECharts | 6.1.0 | 6.1.0 | ✅ 一致 |
| monaco-editor | 0.55.1 | 0.55.1 | ✅ 一致 |
| ESLint | 9.39.4 | 9.39.4 | ✅ 一致 |
| Prettier | 3.9.1 | 3.9.1 | ✅ 一致 |
| Stylelint | 17.14.0 | 17.14.0 | ✅ 一致 |
| Vitest | 4.1.9 | 4.1.9 | ✅ 一致 |

### 1.3 不一致项（⚠️）

| 组件 | progress.md 声明 | 实际值 | 差异说明 |
|------|-----------------|--------|----------|
| Ant Design | 6.4.5 | `^6.0.0`（package.json） | package.json 使用 semver 范围声明，lockfile 解析为 6.4.5。progress.md 记录的是 lockfile 解析后的实际安装版本，而非 package.json 中的声明值。**功能上无影响**，但存在文档精度问题。 |

### 1.4 容器 / 部署配置

| 检查项 | 预期 | 实际 | 状态 |
|--------|------|------|------|
| Dockerfile 基础镜像 | `eclipse-temurin:21-jre` | `eclipse-temurin:21-jre` | ✅ 一致 |
| Dockerfile 无 `--add-opens` | 不包含 | 确认不包含 | ✅ 已验证 |
| Dockerfile ENTRYPOINT 主类 | `yubi.YuBiServerApplication` | `yubi.YuBiServerApplication` | ✅ 一致 |
| `bin/yu-bi-server.sh` 无 `--add-opens` | 不包含 | 确认不包含 | ✅ 已验证 |

---

## 2. 技术债验证

### 2.1 已清理项 ✅

| 技术债 | Phase 1 前状态 | 当前状态 | 验证方式 |
|--------|--------------|----------|----------|
| Shiro 命名装配器重命名 | `ShiroAuthenticationAssembler` / `ShiroAuthorizationAssembler` 在 `yubi.security.manager.shiro` 包下 | 已重命名为 `DefaultAuthenticationAssembler` / `DefaultAuthorizationAssembler`，迁至 `yubi.security.manager` 包；`shiro` 子目录已删除 | 全项目 grep 确认无 Shiro 类名残留（仅 `SpringAuthenticationTokenAdapter.java` 注释中有历史引用） |
| Jackson 2 API 残留（业务代码） | 14+ 文件使用 `com.fasterxml.jackson.*` | 业务代码和数据提供器已全部迁移至 `tools.jackson.*`；仅 `jjwt-jackson` 传递依赖保留 Jackson 2 | `grep -r "^import com.fasterxml.jackson"` 仅发现 `ResponseData.java` 的 annotation 引用（见下方说明） |
| Jackson 2 POM 显式依赖 | `core/pom.xml` 显式依赖 `jackson-annotations` | 已移除；仅通过 `jjwt-jackson` 传递依赖引入 | `mvn dependency:tree` 确认 |

### 2.2 补充说明：Jackson 注解命名空间

`ResponseData.java` 中仍保留 `import com.fasterxml.jackson.annotation.JsonInclude`。

**这是预期行为，非遗留问题。** Jackson 3 的注解模块（`jackson-annotations`）在设计上保持了 `com.fasterxml.jackson.annotation.*` 命名空间，与 Jackson 3 的 databind/core 模块（`tools.jackson.*`）不同。这是 Jackson 3 的官方设计决策，确保注解在 Jackson 2 和 3 之间保持源码级兼容。

**progress.md 文档修正建议：** Jackson 行项备注应从"Boot 主栈 Jackson 3；业务代码/数据提供器仍有 Jackson 2 API 共存"修正为"Boot 主栈 Jackson 3；注解仍使用 `com.fasterxml.jackson.annotation.*`（Jackson 3 设计如此）；仅 jjwt-jackson 传递依赖保留完整 Jackson 2 运行时"。

---

## 3. 风险清单状态验证

| 风险 | progress.md 状态 | 当前验证状态 | 建议 |
|------|-----------------|-------------|------|
| Spring Boot 3.5 EOL | ✅ 已解决：迁移到 Boot 4.0.7 | ✅ 确认已解决（pom.xml parent = 4.0.7） | 无需操作 |
| Javassist + `--add-opens` | ✅ 已解决：子类覆盖替换 | ✅ 确认已解决（Dockerfile 和 bin 脚本均无 `--add-opens`） | 无需操作 |
| Shiro 社区活跃度 | ✅ 已解决：运行时依赖移除 | ✅ 确认已解决（无 Shiro 运行时 JAR） | 无需操作 |
| AntD 5 EOL | ✅ 已解决：迁移到 AntD 6.4.5 | ✅ 确认已解决（lockfile 解析为 6.4.5） | 无需操作 |
| pro-components 3.x beta | 🔍 监控中 | 🔍 仍为 beta（npm latest tag 仍为 2.8.10） | 持续监控 |
| @typescript-eslint TS 7 兼容 | 🔍 监控中 | 🔍 仍未兼容 TS 7（当前使用 8.62.0） | 持续监控 |
| Jackson 2/3 共存 | 🔍 监控中 | ⬇️ 风险降低：业务代码已迁移至 Jackson 3，仅 jjwt-jackson 传递依赖保留 Jackson 2 | 更新状态描述 |
| Shiro 命名装配器遗留 | ⚠️ 不影响运行，待重命名 | ✅ 已解决：Phase 1 完成重命名 | 标记为已解决 |
| data-provider-base 测试编译偶发失败 | ⚠️ 不影响产物 | ⚠️ 待验证（maven-compiler-plugin 3.15.0） | 持续跟踪 |

---

## 4. 文档准确性复核

| # | 复核项 | 当前结论 | 后续处理 |
|---|--------|----------|----------|
| 1 | `antd` 版本记录 | package.json 声明为 `^6.0.0`，lockfile 解析为 `6.4.5`；progress.md 记录的是实际安装版本 | 保持现状，必要时在后续 npm update 后同步 lockfile 版本 |
| 2 | Jackson 注解命名空间 | `com.fasterxml.jackson.annotation.*` 是 Jackson 3 注解模块设计，非业务代码 Jackson 2 残留 | 无需处理 |
| 3 | Shiro 命名装配器 | 已重命名为 `DefaultAuthenticationAssembler` / `DefaultAuthorizationAssembler`，旧 `shiro` 包已删除 | 无需处理 |
| 4 | Jackson 2 API 残留 | 业务代码和数据提供器已迁至 `tools.jackson.*`，仅 `jjwt-jackson` 传递依赖保留 Jackson 2 | 持续关注 jjwt 后续 Jackson 3 支持 |

---

## 5. 审计总结

| 指标 | 结果 |
|------|------|
| 后端版本一致性 | 6/6 ✅（100%） |
| 前端版本一致性 | 11/12 ✅（91.7%），1 项 semver 范围精度差异 |
| 容器配置一致性 | 4/4 ✅（100%） |
| 技术债清理完成率 | 主要项已完成；`EnvironmentPostProcessor` 废弃迁移待后续阶段处理 |
| 风险清单准确性 | 已解决项、监控项和后续迁移项均已回写 progress.md |
| 文档待更新项 | 无阻断项；仅保留 AntD semver 声明与 lockfile 实际版本的精度差异 |

**结论：** 当前代码库与现代化文档的主结论一致。Spring Boot 4.0.7、Jackson 3 业务代码迁移、Shiro 装配器收敛、前端依赖升级和 React class component 迁移均已反映在文档中；后续仅需持续监控 pro-components 3.x stable、TypeScript 7 生态兼容，以及迁移 `EnvironmentPostProcessor`。


---

## 6. Gap 分析——后端依赖版本（Task 10）

> 分析日期：2026-06-28  
> 数据来源：Maven Central、Spring 官方博客

### 6.1 有更新可用的依赖

> **注：** 以下 3 项已在 tech-debt-cleanup spec 中升级完成（2026-06-28）。此处保留原始分析记录供参考。

| 依赖 | 升级前版本 | 已升级至 | 状态 |
|------|---------|-----------|------|
| Spring Boot | 4.0.0 | **4.0.7** | ✅ 已完成 |
| Spring Cloud | 2025.1.0 | **2025.1.2** | ✅ 已完成 |
| Springdoc | 3.0.0 | **3.0.3** | ✅ 已完成 |

### 6.2 已为最新版本的依赖

| 依赖 | 当前版本 | 最新稳定版 | 状态 |
|------|---------|-----------|------|
| MyBatis Spring Boot | 4.0.0 | 4.0.0 | ✅ 最新 |
| Lombok | 1.18.46 | 1.18.46 | ✅ 最新 |
| Commons Lang3 | 3.20.0 | 3.20.0 | ✅ 最新 |
| Commons CSV | 1.14.1 | 1.14.1 | ✅ 最新 |
| GraalVM JS | 25.0.3 | 25.0.3 | ✅ 最新 |
| Bouncy Castle | 1.84 | 1.84 | ✅ 最新 |
| Druid | 1.2.28 | 1.2.28 | ✅ 最新 |
| H2 | 2.4.240 | 2.4.240 | ✅ 最新 |
| Hibernate Validator | 8.0.4.Final | 8.0.4.Final | ✅ 最新 |
| MySQL Connector | 9.7.0 | 9.7.0 | ✅ 最新 |
| HikariCP | 7.1.0 | 7.1.0 | ✅ 最新 |
| Selenium | 4.45.0 | 4.45.0 | ✅ 最新 |
| json-path | 3.0.0 | 3.0.0 | ✅ 最新 |
| JJWT | 0.13.0 | 0.13.0 | ✅ 最新 |
| jose4j | 0.9.6 | 0.9.6 | ✅ 最新 |
| HttpClient5 | 5.6.1 | 5.6.1 | ✅ 最新 |
| POI | 5.5.1 | 5.5.1 | ✅ 最新 |
| PDFBox | 3.0.7 | 3.0.7 | ✅ 最新 |
| Thumbnailator | 0.4.21 | 0.4.21 | ✅ 最新 |
| AspectJ Weaver | 1.9.25.1 | 1.9.25.1 | ✅ 最新 |
| Calcite | 1.42.0 | 1.42.0 | ✅ 最新 |
| SQL Formatter | 2.0.5 | 2.0.5 | ✅ 最新 |

### 6.3 可升级选项（Spring Boot 4.1.0）

Spring Boot 4.1.0 已于 2026-06-10 发布（GA）。这是一个 minor version bump，可能包含新特性和依赖升级。

| 升级路径 | 风险等级 | 建议 |
|---------|---------|------|
| 4.0.0 → 4.0.7（patch） | **低** | 建议立即升级，纯安全和 bug 修复 |
| 4.0.0 → 4.1.0（minor） | **中** | 建议在 4.0.7 稳定后评估，需要测试兼容性 |

### 6.4 后端依赖整体评估

- **22/25 依赖已是最新**（88%）
- **3 项仅需 patch 升级**（Spring Boot、Spring Cloud、Springdoc）
- **无依赖落后超过 1 个 minor 版本**
- 后端依赖健康度：**优秀**

---

## 7. Gap 分析——前端依赖版本（Task 11）

> 分析日期：2026-06-28  
> 数据来源：`npm outdated` 实际输出、npm registry

### 7.1 可安全升级的依赖（Low Risk）

> **注：** 以下 7 项已在 modernization-round-2 spec 中升级完成（2026-06-28）。仅 `antd` 仍待 npm update。

| 包名 | 当前版本 | 最新版本 | 落后程度 | 风险 | 状态 |
|------|---------|---------|----------|------|------|
| antd | 6.4.5 | 6.5.0 | 1 minor | **低** — semver 范围 `^6.0.0` 已覆盖 | 待升级 |
| @ant-design/icons | 6.3.2 | 6.3.2 | — | — | ✅ 已完成 |
| i18next | 26.3.3 | 26.3.3 | — | — | ✅ 已完成 |
| prettier | 3.9.1 | 3.9.1 | — | — | ✅ 已完成 |
| quill | 2.0.3 | 2.0.3 | — | — | ✅ 已完成 |
| react-hotkeys-hook | 5.3.3 | 5.3.3 | — | — | ✅ 已完成 |
| react-quill-new | 3.8.3 | 3.8.3 | — | — | ✅ 已完成 |
| stylelint | 17.14.0 | 17.14.0 | — | — | ✅ 已完成 |

### 7.2 需评估后升级的依赖（Medium Risk）

> **注：** 以下 3 项已在 modernization-round-2 spec 中升级完成（2026-06-28）。此处保留原始分析记录供参考。

| 包名 | 升级前版本 | 已升级至 | 状态 | 原始说明 |
|------|---------|---------|------|----------|
| monaco-editor | 0.52.2 | 0.55.1 | ✅ 已完成 | 编辑器核心组件，已验证语法高亮和功能兼容性 |
| @vitejs/plugin-react | 5.2.0 | 6.0.3 | ✅ 已完成 | 构建插件 major 版本变更，已完成配置迁移 |
| fast-check (dev) | 3.23.2 | 4.8.0 | ✅ 已完成 | PBT 框架 major 升级，已更新现有属性测试 |

### 7.3 暂不建议升级的依赖（High Risk）

| 包名 | 当前版本 | 最新版本 | 落后程度 | 风险 | 说明 |
|------|---------|---------|----------|------|------|
| eslint | 9.39.4 | 10.6.0 | 1 major | **高** | ESLint 10 可能对配置格式和插件生态有较大变更，整个 lint 生态需联动升级 |
| @types/node | 24.13.2 | 26.0.1 | 2 major | **低** | 仅类型定义，但需确认 Node 版本对齐（项目锁定 Node 24） |

### 7.4 特殊状态评估

#### pro-components 3.x 稳定性

```
npm dist-tags 实际输出：
  latest: 2.8.10
  beta: 3.1.12-0
```

**结论：** pro-components 3.x 仍为 beta 状态。npm `latest` tag 仍指向 2.8.x 系列。项目当前使用 `3.1.12-0`（beta tag）。

**建议：** 持续监控。在 pro-components 3.x 正式发布 latest tag 前，维持当前 beta 版本。

#### @typescript-eslint TypeScript 兼容性

```
peerDependencies.typescript: '>=4.8.4 <6.1.0'
```

**结论：** @typescript-eslint 8.62.0 声明兼容 TypeScript `<6.1.0`。项目使用 TypeScript 6.0.3，仍在兼容范围内。但不支持未来的 TypeScript 7。

**建议：** 当前配置兼容，无需操作。当 TypeScript 7 发布时需关注 @typescript-eslint 的兼容性更新。

### 7.5 前端依赖整体评估

- **7 项 low-risk 已升级完成**（仅 `antd` 待 npm update）
- **3 项 medium-risk 已升级完成**（monaco-editor、@vitejs/plugin-react、fast-check）
- **1 项暂不建议升级**（ESLint 10 生态联动成本高）
- 前端依赖健康度：**优秀**

---

## 8. Gap 分析——废弃 API 扫描（Task 12）

> 分析日期：2026-06-28  
> 扫描范围：项目源码（排除 target/ 和 node_modules/）

### 8.1 后端废弃 API 扫描

#### @Deprecated 注解使用

| 位置 | 说明 | 严重度 |
|------|------|--------|
| `data-providers/.../target/generated-sources/.../SimpleCharStream.java:210` | JavaCC 自动生成代码中的 @Deprecated | **无需处理** — 生成代码 |

**结论：** 项目源码中无 @Deprecated 标注。仅在 JavaCC 生成的解析器代码中发现（非手动编写），属于正常现象。

#### @SuppressWarnings("deprecation") 使用

**结论：** 项目中未发现任何 `@SuppressWarnings("deprecation")` 标注。代码库无已知废弃 API 调用的压制。

#### Spring Boot 4 已移除/废弃 API 检查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| `WebMvcConfigurerAdapter` | ✅ 未使用 | 已在 Boot 3 移除，项目未引用 |
| `WebSecurityConfigurerAdapter` | ✅ 未使用 | 已在 Boot 3 移除，项目使用组件化配置 |
| `SecurityConfigurerAdapter` | ✅ 未使用 | 无遗留安全配置适配器 |
| `EnvironmentPostProcessor` | ⚠️ 仍在使用 | `CustomPropertiesValidate.java` 实现此接口，Boot 4.0 标记 `@Deprecated(forRemoval=true)`；已加 `@SuppressWarnings("removal")` 压制，待迁移至 `ApplicationListener<ApplicationEnvironmentPreparedEvent>` |

**结论：** 后端代码存在 1 处 Spring Boot 4 废弃 API 使用（`EnvironmentPostProcessor`），已通过 `@SuppressWarnings("removal")` 临时压制并记入风险清单，待后续迁移。其余已检查的废弃 API 均未使用。

### 8.2 前端废弃模式扫描

#### React Class Components（React 19 遗留模式）

> **注：** 以下 8 个 class component 已在 modernization-round-2 spec 中全部迁移为函数组件（2026-06-28）。

| 文件 | 原类名 | 当前状态 |
|------|--------|----------|
| `src/app/components/ReactFrameComponent/Frame.tsx` | `Frame` | ✅ 已迁移为函数组件 |
| `src/app/components/ReactFrameComponent/Content.tsx` | `Content` | ✅ 已迁移为函数组件 |
| `src/app/components/SplitPane/index.tsx` | `SplitPane` | ✅ 已迁移为函数组件 |
| `src/app/components/SplitPane/Resizer.tsx` | `Resizer` | ✅ 已迁移为函数组件 |
| `src/app/components/SplitPane/Pane.tsx` | `Pane` | ✅ 已迁移为 React.memo 函数组件 |
| `src/app/components/Split.tsx` | `SplitWrapper` | ✅ 已迁移为函数组件 |
| `src/app/components/ChartGraph/ReactVizXYPlotChart/ReactVizXYPlot.tsx` | `ReactXYPlot` | ✅ 已迁移为函数组件 |
| `src/utils/@reduxjs/injectReducer/index.tsx` | `ReducerInjector` | ✅ 已迁移为函数组件 HOC |

**总计：8 个 React class component → 全部已迁移为函数组件**

#### PropTypes 使用

**结论：** PropTypes 已全部移除。Frame.jsx 和 Content.jsx 已迁移为 TypeScript 函数组件（Frame.tsx、Content.tsx），使用 TypeScript 接口替代 PropTypes。`prop-types` 包已不在 package.json 依赖中。

#### Legacy Context API

**结论：** 未发现 `contextTypes`、`getChildContext`、`childContextTypes` 等旧版 Context API 使用。

#### UNSAFE Lifecycle Methods

**结论：** 未发现 `componentWillMount`、`componentWillReceiveProps`、`componentWillUpdate`、`UNSAFE_componentWillMount` 等废弃生命周期方法。

#### Deprecated React Router 模式

**结论：** 未发现 `<Switch>`（已被 `<Routes>` 替代）或 `useHistory()`（已被 `useNavigate()` 替代）。项目已完全使用 react-router-dom v7 API。

#### AntD 6 废弃 API

**结论：** 未发现 `antd/lib` 或 `antd/es` 深路径导入（AntD 5+ 已废弃此模式）。项目使用标准 `antd` 顶层导入。

#### 其他观察

| 发现 | 说明 | 严重度 |
|------|------|--------|
| `react-dnd` 16.0.1 与 `@hello-pangea/dnd` 18.0.1 共存 | 项目同时依赖两个 DnD 库，存在冗余 | **低** — 功能不冲突但增加 bundle 体积 |

### 8.3 废弃 API 扫描总结

| 类别 | 发现数 | 严重度 | 建议优先级 |
|------|--------|--------|-----------|
| 后端废弃 API | 1 | 中 | P2 — `EnvironmentPostProcessor` 待迁移至 `ApplicationListener` |
| React Class Components | 0（8 文件已迁移） | — | ✅ 已完成 |
| PropTypes / defaultProps | 0 | — | ✅ 已完成 |
| Legacy Context / UNSAFE lifecycle | 0 | — | 无需操作 |
| Deprecated Router/AntD patterns | 0 | — | 无需操作 |
| DnD 库冗余 | 1 | 低 | P3 — 统一为 @hello-pangea/dnd |

---

## 9. Gap 分析行动建议

### 9.1 立即可执行（低风险 Patch 升级）

> **注：** 后端 3 项升级和前端 7 项 low-risk 升级已在 tech-debt-cleanup / modernization-round-2 中完成。

**前端（仅剩余未完成项）：**
- `antd`: ^6.0.0（npm update 即可拉取 6.5.0）

### 9.2 后续计划（需评估和测试）

| 项目 | 优先级 | 预计工作量 | 阻塞因素 |
|------|--------|-----------|----------|
| react-dnd 清理 | P3 | 低 | 确认所有 DnD 场景可由 @hello-pangea/dnd 覆盖 |
| ESLint 9 → 10 | P4 | 高 | 等待插件生态跟进 |
| Spring Boot 4.0 → 4.1 | P4 | 中~高 | 需验证全栈兼容性 |

### 9.3 持续监控项

| 项目 | 当前状态 | 关注点 |
|------|---------|--------|
| pro-components 3.x | beta（latest tag = 2.8.10） | 等待 3.x 发布 GA |
| @typescript-eslint TS 7 | 当前支持 <6.1.0 | TS 7 发布后关注兼容更新 |
| jjwt-jackson Jackson 2 传递依赖 | 0.13.0 仍依赖 Jackson 2 | 关注 jjwt 后续版本是否迁移至 Jackson 3 |
