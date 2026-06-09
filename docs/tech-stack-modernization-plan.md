# 技术栈现代化升级计划

本文档记录 Datart 技术栈现代化迁移的目标终态、阶段顺序、验收门槛和风险控制。它是后续升级工作的执行基线，避免把多个高风险迁移混在同一个提交里。

## 当前基线

- Java 运行时：JDK 21。
- 后端主框架：Spring Boot 3.5.12，Spring Cloud 2025.0.1。
- 构建工具：Maven，`maven-compiler-plugin` 3.14.1，`maven-assembly-plugin` 3.8.0，`exec-maven-plugin` 3.6.3。
- 前端运行时：本机 Node 26 可启动，CRA4/Webpack4 通过 `NODE_OPTIONS=--openssl-legacy-provider` 兼容。
- 前端主框架：React 17、React Router 5、Ant Design 4、CRACO 6、TypeScript 4.5。
- 已验证基线：
  - `npm run checkTs` 通过。
  - `mvn test -pl data-providers/jdbc-data-provider -am` 通过。
  - `GET /api/v1/sys/info` 返回 200。
  - `http://127.0.0.1:3000` 返回 200。

## 目标终态

- 后端保持 Java 21+ 与 Spring Boot 稳定主线，优先使用 Boot BOM 管理版本，减少手写版本漂移。
- 前端迁出已退场的 Create React App，使用 Vite 作为现代构建工具。
- React 升级到 18+，后续再评估 React 19。
- UI 栈升级到 Ant Design 5/6，其中 Ant Design 6 需要先完成 React 18+。
- 路由升级到 React Router 6/7。
- 状态管理升级到 Redux Toolkit 2 与 React Redux 9。
- 浏览器自动化迁出 PhantomJS，使用 Selenium 4 + Chrome/Edge WebDriver，或 Playwright。
- JSON/JWT/HTTP/工具库统一到仍维护的现代库，删除重复和过时依赖。
- 构建和 CI 明确 Node、npm、Maven 最低版本，避免依赖开发者本机偶然状态。

## 官方依据

- React 官方已宣布 Create React App 退场，新项目应使用框架或现代构建工具；本项目需要从 CRA 迁出。
- Vite 是现代前端构建工具，适合作为 CRA/CRACO 的替代方向。
- Ant Design 6 进入新主线，要求 React 18+，因此 UI 大版本升级必须排在 React 升级之后。
- Selenium 官方提供 Selenium 4 迁移路径；当前 Selenium 3 和 PhantomJS driver 属于老旧浏览器自动化组合。
- PhantomJS 官方项目已长期停止活跃开发，不应作为现代化目标的一部分。

## 分阶段升级路线

### 阶段 0：基线稳定与版本治理

目标：让项目有明确、可重复的构建和验收入口。

已完成：
- JDK 21 迁移。
- Spring Boot 3.5.12 迁移与运行期兼容修复。
- 第一批低风险依赖和插件升级。
- Node 26 下前端开发启动兼容。
- `.npmrc` registry 配置已兼容 npm 11。
- Maven Enforcer 已声明 Java 21+ 与 Maven 3.9+。
- 前端构建产物复制已后移到 `package` 阶段，后端 Java 编译不再被 `frontend/build/task/index.js` 缺失阻断。

待完成：
- 将前端完整构建纳入发布包验收，确保 `package` 阶段生成静态资源和 parser task 产物。

验收门槛：
- `mvn -DskipTests -Dexec.skip=true -Dmaven.resources.skip=true compile -pl server -am` 的 Java 编译阶段通过。
- `npm run checkTs` 通过。
- 前后端本地服务均可访问。

### 阶段 1：前端构建链短期稳定

目标：在不改业务代码的前提下消除 CRA4/Webpack4 对 Node 26 的明显摩擦。

升级项：
- `react-scripts 4 -> 5.0.1`。
- `@craco/craco 6 -> 7`。
- Webpack 4 间接升级到 Webpack 5。
- Babel、PostCSS、Browserslist 数据做兼容性更新。

风险：
- CRACO 配置对 Webpack 4 API 的假设可能失效。
- Ant Design Less 主题、Monaco 插件、多入口 share 页面可能需要适配。

验收门槛：
- `npm start` 成功。
- `npm run checkTs` 成功。
- `npm run build:task` 成功。
- `npm run build` 成功。
- 登录页、系统信息接口代理、至少一个核心页面可打开。

### 阶段 2：迁出 CRA 到 Vite

目标：以 Vite 替代 CRA/CRACO，形成长期可维护的前端构建基础。

升级项：
- 新增 Vite 配置，替代 CRACO 配置。
- 迁移入口文件、多页面 share 入口、静态资源路径和代理配置。
- 替换 Webpack 专属插件，尤其是 Monaco 和 Less 主题处理。
- 保留 Rollup task 构建，或统一到 Vite/Rollup 配置。

风险：
- `process.env.PUBLIC_URL`、`BrowserRouter basename`、资源路径可能需要统一处理。
- CRA 的 Jest 配置不能直接复用。

验收门槛：
- `npm run dev` 成功。
- `npm run build` 生成可被后端托管的 `frontend/build` 或等价产物。
- `npm run build:task` 生成 `frontend/build/task/index.js` 或更新 Maven 复制路径。
- 端到端访问后端托管静态资源成功。

### 阶段 3：React 17 升级到 React 18

目标：进入 React 18 稳定生态，为 Ant Design 6、React Router 新版本和测试库升级铺路。

升级项：
- `react`、`react-dom`、`react-test-renderer` 升到 18。
- 应用入口从 `ReactDOM.render` 迁到 `createRoot`。
- 检查 StrictMode 下副作用和组件生命周期问题。
- 更新 `@types/react`、`@types/react-dom`。

风险：
- 旧组件库可能依赖 React 17 行为。
- Enzyme 对 React 18 支持弱，测试栈需要同步迁移。

验收门槛：
- `npm run checkTs` 通过。
- 前端核心页面无明显运行时错误。
- 登录、组织/资源列表、图表或仪表板基础流程可操作。

### 阶段 4：UI 与路由现代化

目标：升级用户界面和路由主线，同时控制视觉和交互回归。

升级项：
- Ant Design 4 先评估升级到 5，再评估 6。
- `@ant-design/icons`、`@ant-design/pro-table` 同步升级。
- React Router 5 升级到 6/7。
- 替换或适配不再维护的 UI 依赖。

风险：
- Ant Design 5/6 token、Less 变量和组件 API 变化较大。
- React Router 6/7 的路由声明和导航 API 有破坏性变化。

验收门槛：
- 全部路由可访问。
- 表单、弹窗、表格、菜单、布局无明显视觉破坏。
- 关键工作流人工验收通过。

### 阶段 5：状态管理、测试与前端依赖整理

目标：减少老旧库和重复状态逻辑，提高长期维护性。

升级项：
- Redux Toolkit 1 -> 2。
- React Redux 7 -> 9。
- Testing Library 升级，逐步移除 Enzyme。
- Prettier 2 -> 3、ESLint/stylelint 规则升级。
- Axios 0.x -> 1.x，`js-cookie` 2 -> 3，`uuid` 8 -> 当前主线。

风险：
- RTK 2 和 React Redux 9 对类型推断和 store 配置有变化。
- Axios 1 的取消请求、拦截器类型和错误对象可能影响请求封装。

验收门槛：
- `npm run checkTs` 通过。
- 主要单测通过或迁移完成。
- 请求封装和认证流程正常。

### 阶段 6：后端库现代化

目标：删除老旧、高风险或重复的后端基础库。

升级项：
- `fastjson 1.x` 迁移到 `fastjson2` 或统一到 Jackson。
- `jjwt 0.7`、`java-jwt 3.7` 统一为一个现代 JWT 库。
- Apache HttpClient 4 迁移到 HttpClient 5，或统一 OkHttp/JDK HttpClient。
- Selenium 3/PhantomJS 迁移到 Selenium 4 + Chrome/Edge WebDriver，或 Playwright。
- `commons-io 1.3.1`、`commons-lang 2.6`、Guava 21、POI 5.0、Calcite 1.26、MyBatis Generator 1.4 等逐项升级。
- H2 1.4.200 仅作为 demo 库兼容保留；长期应生成 H2 2.x 可读的新 demo 数据库或迁移到 Testcontainers。

风险：
- JSON 序列化行为变化可能影响 API、配置、导入导出。
- JWT 库统一会影响 token 签发、校验和兼容性。
- Selenium/PhantomJS 替换会影响截图和导出能力。
- Calcite/POI/Guava 大版本升级可能影响数据处理逻辑。

验收门槛：
- 后端模块编译通过。
- `mvn test -pl data-providers/jdbc-data-provider -am` 通过。
- demo profile 启动成功。
- `/api/v1/sys/info` 返回 200。
- 截图/导出能力有浏览器级验证。

### 阶段 7：构建、CI 与发布现代化

目标：让本地、CI、Docker 和发布包使用一致的现代工具链。

升级项：
- Maven Enforcer 约束 Java、Maven、Node、npm 版本。
- Dockerfile 固化 JDK 21 runtime，并加入必要的 `--add-opens` 或替换反射实现。
- CI 拆分后端测试、前端类型检查、前端构建、集成启动验证。
- 清理 Maven 里过时的淘宝 registry 参数，统一使用 `.npmrc` 或 CI secret。
- 修复前端产物和 parser task 的构建顺序。

验收门槛：
- 干净环境可以一键构建。
- Docker 镜像可启动。
- 发布包包含正确静态资源和 parser.js。

## 不建议一次性升级的项目

- React、Ant Design、React Router、Vite 不应在同一个提交里同时完成。
- fastjson2、JWT 库统一、HttpClient 5 不应和 Spring Boot 主版本升级混做。
- Selenium 4/Playwright 迁移需要先明确截图、导出、浏览器驱动安装方式。
- H2 2.x demo 数据库迁移需要生成新数据或迁移脚本，不能只改版本号。

## 每阶段通用验收清单

- `git status --short` 只包含本阶段预期文件。
- `git diff --check` 通过。
- 后端相关阶段运行 Maven 编译或模块测试。
- 前端相关阶段运行 `npm run checkTs`，必要时运行构建。
- 启动后端 demo profile，验证 `/api/v1/sys/info`。
- 启动前端，验证首页和至少一个 API 代理请求。
- 每个阶段单独提交，提交信息用中文说明升级范围。

## 当前下一步

优先进入阶段 0 剩余项：

1. 修复 `.npmrc` 的 npm 11 registry 配置警告。
2. 增加 Maven Enforcer，声明 Maven 与 Java 最低版本。
3. 验证 Maven `package` 阶段的前端完整构建和发布包产物。

完成阶段 0 后，再进入阶段 1：CRA4/CRACO6 升级到 CRA5/CRACO7。
