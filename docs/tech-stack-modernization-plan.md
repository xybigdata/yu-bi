# 技术栈现代化升级计划

本文档记录 Datart 技术栈现代化迁移的目标终态、阶段顺序、验收门槛和风险控制。它是后续升级工作的执行基线，避免把多个高风险迁移混在同一个提交里。

## 当前基线

- Java 运行时：JDK 21。
- 后端主框架：Spring Boot 3.5.12，Spring Cloud 2025.0.1。
- 构建工具：Maven，`maven-compiler-plugin` 3.14.1，`maven-assembly-plugin` 3.8.0，`exec-maven-plugin` 3.6.3。
- 前端运行时：本机 Node 26 可启动，默认开发和生产构建已切换到 Vite 5。
- 前端主框架：React 18、React Router 5、Ant Design 4、TypeScript 4.5；CRACO 7/CRA5 仅作为回退脚本保留。
- 已验证基线：
  - `npm run checkTs` 通过。
  - `npm run build:task` 通过。
  - `npm run build` 通过，默认使用 Vite 输出 `frontend/build`。
  - `mvn -DskipTests -Dexec.skip=false package -pl server -am` 通过，Maven `package` 阶段可复制 Vite 静态资源并生成 parser.js。
  - `mvn test -pl data-providers/jdbc-data-provider -am` 通过。
  - `GET /api/v1/sys/info` 返回 200。
  - `http://127.0.0.1:3001` 返回 200，`/api/v1/plugins/custom/charts` 返回成功。
  - Vite production preview 可渲染登录页。

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

已完成：
- `react-scripts 4.0.3 -> 5.0.1`。
- `@craco/craco 6 -> 7.1.0`。
- Webpack 4 间接升级到 Webpack 5。
- `monaco-editor-webpack-plugin 4 -> 7.1.1`，`monaco-editor 0.28 -> 0.52`，`react-monaco-editor 0.46 -> 0.59`。
- `webpackbar 5 -> 7`，Webpack 类型包升级到 5.x。
- 移除前端 `start`/`build` 中的 `NODE_OPTIONS=--openssl-legacy-provider`。
- 适配 webpack-dev-server 4 的 `setupMiddlewares`。
- 删除 `uuid` 私有导入路径，改用 `uuid` 公开 API，兼容 Webpack 5 package exports。
- 显式固定 `ajv@8`、`ajv-keywords@5`，避免 npm 11 hoist 到 Webpack5 不兼容组合。
- 显式固定 `@types/babel__traverse@7.18.5`，避免 TypeScript 4.5 解析新版类型语法失败。

遗留风险：
- CRA5 自身仍会打印 webpack-dev-server `onBeforeSetupMiddleware`/`onAfterSetupMiddleware` 弃用警告，长期应通过阶段 2 迁出 CRA 解决。
- Monaco 开发构建会打印缺失 `marked.umd.js.map` 的 source map 警告，不影响构建和运行。
- `eslint-plugin-jsdoc` 仍声明只支持 Node 12-17，安装时在 Node 26 下有 engine warning；后续放入阶段 5 的 lint 工具链升级。
- `npm audit` 仍有历史漏洞，需随后续前后端依赖升级分批处理。

验收门槛：
- `npm start` 成功。
- `npm run checkTs` 成功。
- `npm run build:task` 成功。
- `npm run build` 成功。
- `build/index.html`、`build/shareChart.html`、`build/shareDashboard.html`、`build/shareStoryPlayer.html` 均生成。
- 前端开发服务返回 200，自定义插件接口 `/api/v1/plugins/custom/charts` 返回成功。

### 阶段 2：迁出 CRA 到 Vite

目标：以 Vite 替代 CRA/CRACO，形成长期可维护的前端构建基础。

已完成：
- 新增并行 Vite 5 构建链：`dev:vite`、`build:vite`。
- 默认 `start`、`build` 已切换到 Vite；保留 `start:cra`、`build:cra` 作为 CRA5 回退脚本。
- 新增 Vite 多页面 HTML 入口：`index.html`、`shareChart.html`、`shareDashboard.html`、`shareStoryPlayer.html`。
- 迁移 Vite dev proxy 和 custom chart plugins middleware。
- 适配 CRA 兼容层：`process.env.NODE_ENV`、`process.env.PUBLIC_URL`、`module.hot`/`import.meta.hot`、`styled-components/macro`、SVG `ReactComponent` 导入、Ant Design Less 的 `~` import。
- 消除 Vite/Rollup 对 `app/components/index.tsx` barrel 循环 re-export 的分包警告。
- Vite 配置已改为 `.mts`，消除 CJS Node API 弃用警告。
- Vite 默认输出目录已切换为 `frontend/build`，兼容后端 Maven `package` 阶段的静态资源复制路径。

风险：
- `process.env.PUBLIC_URL`、`BrowserRouter basename`、资源路径可能需要统一处理。
- CRA 的 Jest 配置仍暂时通过 CRACO 保留，后续测试栈迁移时需要替换。
- Vite 产物仍有大 chunk 提示，替换默认构建前需要继续优化分包和动态加载策略。

验收门槛：
- `npm start` 成功，默认启动 Vite dev server。
- `npm run build` 生成可被后端托管的 `frontend/build`。
- `npm run build:task` 生成 `frontend/build/task/index.js`。
- 端到端访问后端托管静态资源成功。

当前并行验收：
- `npm run build:all` 成功，产出 `build/index.html`、三个 share HTML 和 `build/task/index.js`。
- `npm start` 成功，首页 200，`/api/v1/plugins/custom/charts` 返回成功，`/shareChart/test-token` 返回 share chart 入口。
- `npm run build:cra` 仍可成功，用于短期回退和差异对比。
- `mvn -DskipTests -Dexec.skip=false package -pl server -am` 成功，`copy-resource` 从 `frontend/build` 复制 57 个资源到 `static`，并将 `frontend/build/task/index.js` 重命名为 `server/src/main/resources/javascript/parser.js`。

### 阶段 3：React 17 升级到 React 18

目标：进入 React 18 稳定生态，为 Ant Design 6、React Router 新版本和测试库升级铺路。

升级项：
- `react`、`react-dom`、`react-test-renderer` 升到 18。
- 应用入口从 `ReactDOM.render` 迁到 `createRoot`。
- 检查 StrictMode 下副作用和组件生命周期问题。
- 更新 `@types/react`、`@types/react-dom`。

已完成：
- `react 17.0.2 -> 18.3.1`。
- `react-dom 17.0.2 -> 18.3.1`。
- `react-test-renderer 17.0.2 -> 18.3.1`。
- 应用入口已改用 `react-dom/client` 的 `createRoot`。
- HMR 判断已兼容 Vite ESM 产物，避免 production preview 中访问未定义的 `module`。
- Testing Library 已升级到 React 18 兼容线：`@testing-library/react 14.3.1`、`@testing-library/user-event 14.6.1`。
- Enzyme 短期改用社区 React 18 adapter：`@cfaester/enzyme-adapter-react-18 0.8.0`，移除 React 17 adapter。

当前过渡策略：
- 运行时已进入 React 18，但 `@types/react` 暂保留在 17.0.38，`@types/react-dom` 升级到 18.0.11 以提供 `createRoot` 类型。
- 原因：当前 Ant Design 4、react-helmet-async 1.x、styled-components 5 以及大量 `React.FC` 写法依赖 React 17 的隐式 `children` 类型。直接切到 `@types/react@18` 会触发大范围类型错误，适合与 Ant Design 5/6、测试栈迁移一起分阶段收口。

风险：
- 旧组件库可能依赖 React 17 行为。
- Enzyme 对 React 18 支持弱，当前 adapter 只是过渡方案，测试栈仍需迁移到 Testing Library。
- `@types/react` 仍是过渡状态，后续 UI/路由/测试栈现代化阶段需要升级到 React 18 类型并显式补齐 `children`。

验收门槛：
- `npm run checkTs` 通过。
- `npm run build` 通过。
- `npm run build:task` 通过。
- Vite production preview 登录页可渲染。
- 登录、组织/资源列表、图表或仪表板基础流程可操作。

### 阶段 4：UI 与路由现代化

目标：升级用户界面和路由主线，同时控制视觉和交互回归。

升级项：
- Ant Design 4 先评估升级到 5，再评估 6。
- `@ant-design/icons`、`@ant-design/pro-table` 同步升级。
- React Router 5 升级到 6/7。
- 替换或适配不再维护的 UI 依赖。

已完成：
- `react-helmet-async 1.2.2 -> 3.0.0`，先消除 React 18 类型迁移中 Helmet/HelmetProvider `children` 类型不完整的问题。

预研结果：
- Ant Design 相关调用点约 358 个文件，`visible`/`onVisibleChange`/`overlay`/`Menu.Item` 等 AntD 5 迁移热点分布广，不能直接大版本替换。
- `antd 4.16.13 -> 4.24.16` 预演会触发若干类型变化，包括 `Input` 类型引用、`InputNumber` 的 `number | null`、Tree/Cascader 类型收紧等。
- `antd 4.24.16` 还会阻断当前 `antd-theme-generator 1.2.11`：主题生成尝试读取 `antd/lib/style/themes/@{root-entry-name}.less`，导致 `npm run build:theme` 失败。
- 因此 UI 阶段的实际顺序应先替换或移除 `antd-theme-generator` 动态 Less 主题链，再把 Ant Design 升到 4.24.x，最后迁移到 Ant Design 5 token 主题。

风险：
- Ant Design 5/6 token、Less 变量和组件 API 变化较大。
- 当前动态主题依赖 Ant Design 4 Less 变量和浏览器端 `less.min.js`，这是迁移 Ant Design 5/6 的主要阻塞。
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

阶段 3 已完成，下一步进入阶段 4 的 UI 与路由现代化准备：

1. 替换 `antd-theme-generator` 和浏览器端 Less 动态主题链，为 Ant Design 4.24.x/5 铺路。
2. 在主题链替换后，先把 Ant Design 升到 4.24.x，并修复 Tree/Cascader/InputNumber 等类型收紧问题。
3. 梳理 `@types/react@18` 的阻塞点，优先补齐公共组件和上下文组件的 `children` 类型。
4. 评估 React Router 5 -> 6/7 的路由声明、`useHistory`、`Redirect` 和嵌套路由替换方案。
5. 保持 CRA5/CRACO 回退脚本，直到 Jest/测试栈迁出 CRA。
