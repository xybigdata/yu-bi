# 技术栈现代化升级计划

本文档记录 Datart 技术栈现代化迁移的目标终态、阶段顺序、验收门槛和风险控制。它是后续升级工作的执行基线，避免把多个高风险迁移混在同一个提交里。

## 当前基线

- Java 运行时：JDK 21。
- 后端主框架：Spring Boot 3.5.12，Spring Cloud 2025.0.1。
- 构建工具：Maven，`maven-compiler-plugin` 3.14.1，`maven-assembly-plugin` 3.8.0，`exec-maven-plugin` 3.6.3。
- 前端运行时：本机 Node 26 可启动，默认开发和生产构建已切换到 Vite 5。
- 前端主框架：React 18、React Router 6.30.1、Ant Design 4.24、TypeScript 5.9；CRA/CRACO 已退出前端主工作流。
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

## 最终现代化技术栈清单

这一节定义“项目最终完成现代化替代”时应达到的目标清单。后续每个阶段都要朝这个清单收敛，而不是只做局部兼容。

### 后端目标清单

- 运行时与框架
  - JDK 21 LTS
  - Spring Boot 3 稳定主线
  - Spring Framework 6 / Jakarta EE 10 兼容链
- 数据与服务基础设施
  - MyBatis Spring Boot 3 主线
  - MySQL 驱动使用当前坐标 `com.mysql:mysql-connector-j`
  - Quartz 保持 Spring Boot 当前稳定主线
  - Redis、LDAP、Mail、Thymeleaf 维持 Boot BOM 管理
- 安全与认证
  - JWT 收敛到单一现代实现
  - 长期评估从 Shiro 迁到 Spring Security 原生安全体系
- JSON 与 HTTP
  - Web 层统一使用 Jackson
  - 自定义 JSON 解析尽量统一到 Jackson tree/model API
  - HTTP 客户端收敛到单一现代实现：JDK HttpClient / HttpClient 5 / OkHttp 三选一
- 浏览器自动化与导出
  - 不再依赖 PhantomJS
  - 使用 Playwright 或 Selenium 4 + Chromium Headless
- 旧基础库治理
  - 淘汰 `commons-lang 2`
  - 淘汰 `commons-io 1.x`
  - 收敛或升级 Guava、POI、Commons CSV、Calcite、MyBatis Generator、Nashorn

### 前端目标清单

- 构建与运行
  - Node 26 可稳定开发、构建、测试
  - Vite 作为唯一主构建链
  - CRA / CRACO 完全退出主工作流
- 核心框架
  - React 18 稳定运行，后续再评估 React 19
  - React Router 升级到 6/7
  - Ant Design 升级到 5，之后再评估 6
- 状态与数据
  - Redux Toolkit 2
  - React Redux 9
  - Axios 1
- 样式与主题
  - 继续使用 `styled-components`，但升级到当前主线时需补齐 SSR、类型和 macro 兼容策略
  - AntD 主题能力统一到 token / CSS variables，不保留浏览器端 Less 动态编译链
- 测试与工具链
  - Testing Library 作为主测试方案
  - Enzyme 完全退出
  - Lint / Format / Type Check 与 Node 26 兼容
- 兼容与运行策略
  - 明确是否放弃 IE 11；若放弃，则移除 `react-app-polyfill` 和相关历史兼容代码

### 构建、发布与工程化目标清单

- Maven / npm / Node / Java 版本约束全部显式化
- 本地、CI、Docker、发布包使用同一套产物生成链
- 发布包内静态资源、parser.js、配置目录和启动参数都可复现
- 服务启动、前端代理、后端 API、截图导出、分享页、多入口 HTML 都有自动或半自动验收路径

## 升级依赖矩阵

这一节定义哪些升级可以并行，哪些必须按顺序推进，避免后续把多个高风险改动混在一个阶段里。

- React Router 6/7 依赖于：
  - `useHistory`、`Redirect`、`Switch`、`Route component/render` 基本清空
  - 主路由容器先迁到兼容层
- Ant Design 5 依赖于：
  - React 18 已完成
  - Dropdown/Menu/Modal/Popover 等旧 API 基本收口
  - 主题链已切到 CSS variables / token 思路
- 测试栈从 CRA/Enzyme 迁出依赖于：
  - React Router 与 AntD 大面积结构改动先稳定
  - Vite 主构建链可长期作为唯一入口
- 后端 Jackson 单栈化依赖于：
  - 先识别 `fastjson` 在 DTO、配置校验、HTTP message converter、数据导入导出中的边界
  - 先把自动化/截图链与 JSON 栈解耦，避免多种基础设施同时变动
- JWT 单栈化依赖于：
  - 先确认现有 token 生成、刷新、校验和第三方登录链路
  - 最好在 JSON 栈趋稳之后推进
- PhantomJS / Selenium 3 迁移依赖于：
  - 明确当前截图、导出、分享缩略图等所有实际调用点
  - 明确服务器运行环境是否允许安装 Chromium 及其依赖
- H2 2.x 或 Testcontainers 依赖于：
  - demo 数据、初始化 SQL、兼容模式和文档同步调整
  - 最好放在后端核心库升级后期

## 执行原则

- 每次提交只解决一个明确升级主题。
- 每个阶段先做“兼容层收口”，再做真正的大版本切换。
- 尽量先消除重复实现，再做库替换。
- 优先替换已经停止维护、明显过时或高安全风险的依赖。
- 高风险基础设施变更必须带验证路径，不能只升级版本号。
- 文档中的“最终技术栈清单”是完成定义，阶段成果只是通往终态的中间站。

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
- 默认 `start`、`build` 已切换到 Vite，并已删除 `start:cra`、`build:cra` 这类 CRA5 回退脚本。
- 新增 Vite 多页面 HTML 入口：`index.html`、`shareChart.html`、`shareDashboard.html`、`shareStoryPlayer.html`。
- 迁移 Vite dev proxy 和 custom chart plugins middleware。
- 适配 CRA 兼容层：`process.env.NODE_ENV`、`process.env.PUBLIC_URL`、`module.hot`/`import.meta.hot`、`styled-components/macro`、SVG `ReactComponent` 导入、Ant Design Less 的 `~` import。
- 消除 Vite/Rollup 对 `app/components/index.tsx` barrel 循环 re-export 的分包警告。
- Vite 配置已改为 `.mts`，消除 CJS Node API 弃用警告。
- Vite 默认输出目录已切换为 `frontend/build`，兼容后端 Maven `package` 阶段的静态资源复制路径。
- 已删除失去运行职责的 `@craco/craco` 与 `frontend/craco.config.js`，前端运行/构建主链不再保留 CRACO 外壳。

风险：
- `process.env.PUBLIC_URL`、`BrowserRouter basename`、资源路径可能需要统一处理。
- Jest 当前仍暂时借用 `react-scripts` 提供的 transform 与样式/file mock，后续测试栈迁移时需要替换为独立方案。
- Vite 产物仍有大 chunk 提示，替换默认构建前需要继续优化分包和动态加载策略。

验收门槛：
- `npm start` 成功，默认启动 Vite dev server。
- `npm run build` 生成可被后端托管的 `frontend/build`。
- `npm run build:task` 生成 `frontend/build/task/index.js`。
- 端到端访问后端托管静态资源成功。

当前并行验收：
- `npm run build:all` 成功，产出 `build/index.html`、三个 share HTML 和 `build/task/index.js`。
- `npm start` 成功，首页 200，`/api/v1/plugins/custom/charts` 返回成功，`/shareChart/test-token` 返回 share chart 入口。
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
- React 类型定义已升级到 React 18：`@types/react 17.0.38 -> 18.3.12`。
- 显式补齐 React 18 不再隐式提供的 `children` 类型，覆盖 Provider、Wrapper、FormGenerator、ListTitle、ChartDrill、Dashboard Widget 等调用点。
- 适配 AntD Tree/Icon 回调、i18next 与 AntD message 内容类型、Chart iframe 容器返回类型等 React 18 类型收紧暴露的问题。

风险：
- 旧组件库可能依赖 React 17 行为。
- Enzyme 对 React 18 支持弱，当前 adapter 只是过渡方案，测试栈仍需迁移到 Testing Library。

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
- `antd 4.16.13 -> 4.24.16`，先升级到 Ant Design 4 最后稳定线，避免一次性跨到 5 带来大面积 API 和主题回归。
- React 18 类型定义已落地，UI 组件中依赖 React 17 隐式 `children` 的写法已清理到可编译状态。
- 移除 `antd-theme-generator`、`frontend/scripts/extractAntdTheme.js` 和浏览器端 `less.min.js` 动态编译链。
- AntD 4.24 样式入口切换为 `antd.variable.min.css`，运行时主题切换改用 `ConfigProvider.config({ theme })` 写入 CSS variables，为 Ant Design 5 token 主题迁移铺路。
- 修复 AntD 4.24 暴露的类型收紧问题，包括 `InputRef`、`InputNumber` 的 `number | null`、Tree/Cascader 节点类型和 FormList `fieldKey` 可选值。
- 修复 Vite 生产构建中 CRA HMR 兼容分支被折叠成裸 `module.hot` 后导致预览页空白的问题。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理 AntD 5 的浮层打开态 API：直接 `Modal`、`Popover` 以及项目 `Popup` 封装已从 `visible`/`onVisibleChange` 迁到 `open`/`onOpenChange`，`ModalForm`、`Confirm`、`DeleteConfirm` 封装保留旧入参并向 AntD 传递 `open`。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理 AntD 5 的 Dropdown 菜单 API：第一批静态菜单已从 `overlay={<Menu />}` 和 `Menu.Item` JSX 改为 `menu={{ items, onClick }}`，覆盖 ListTitle、视图/图表 Tab 右键菜单、仪表板工具栏添加入口、快捷键、设备切换、SQL 预览 Limit、引用资源排序和 Join 类型选择。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理 AntD 5 的自定义 Dropdown 渲染 API：Header/Widget/ChartDraggable 等自定义 `overlay` 入口已切换为 `dropdownRender`，复杂菜单内部 JSX 暂保留。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 继续预处理 AntD 5 的自定义 Dropdown 渲染 API：交互规则关系配置、字段类型选择、富文本引用字段、图表钻取右键菜单和仪表板控制器添加入口已从 `overlay` 切换到 `dropdownRender`。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理图表工作台字段菜单：数据字段更多操作菜单已从 `Menu.Item` JSX 切换到 `Dropdown menu.items`，并移除 `antd/lib/menu/SubMenu` 深路径导入。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 继续预处理 AntD 5 菜单 API：故事板操作菜单和仪表板全屏 widget 列表已从 JSX `Menu.Item` 切换到 `Menu items` 配置，保留原有 Popconfirm 和点击行为。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理视图字段类型菜单：字段类型、日期格式和字段分类菜单已从 JSX `Menu.Item`/`Menu.SubMenu` 切换到 `Menu items` 配置，保留原有 `keyPath` 回调语义。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理仪表板控制器添加菜单：分组控制器菜单已从 `dropdownRender` + JSX `Menu.ItemGroup` 切换到 `Dropdown menu.items` 配置，保留禁用态和添加动作。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理图表字段替换菜单：递归字段替换菜单已从 JSX `Menu.Item`/`Menu.SubMenu` 切换到 `Menu items` 配置，保留叶子节点替换动作。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理富文本引用字段菜单：引用字段下拉菜单已从 `Menu.Item` JSX 切换到 `Menu items` 配置，保留字段插入动作和空数据提示。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理仪表板组件操作菜单：Widget 更多操作菜单已从 `dropdownRender` + JSX `Menu.Item` 切换到 `Dropdown menu.items`，保留分割线、危险态、禁用态和点击动作。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理仪表板更多操作菜单：`BoardDropdownList` 已从 JSX `Menu.Item`/`Menu.Divider` 切换到 `Menu items` 配置，保留分享、下载确认、发布、另存、添加故事板和归档动作。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理可视化资源更多操作菜单：`VizOperationMenu` 已从 JSX `Menu.Item`/`Menu.Divider` 切换到 `Menu items` 配置，保留刷新、另存、加入仪表板、分享、下载确认、发布和归档动作。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理日期层级菜单片段：`DateLevelMenuItems` 已从 JSX `Menu.Item` 切换到 `Menu items` 配置，保留默认值和日期层级计算字段切换逻辑。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。
- 预处理图表钻取右键菜单：`ChartDrillContextMenu` 已从 JSX `Menu.Item`/`Menu.SubMenu` 切换到 `Menu items` 配置，日期层级子菜单复用共享的 `buildDateLevelMenuItems`，保留钻取、联动、查看数据和日期层级切换逻辑。
- 2026-06-09 验证：`npm run checkTs`、`npm run build`、`npm run build:task` 均通过。

预研结果：
- Ant Design 相关调用点约 358 个文件，`visible`/`onVisibleChange`/`overlay`/`Menu.Item` 等 AntD 5 迁移热点分布广，不能直接大版本替换。
- 动态主题链已从浏览器端 Less 编译迁到 AntD CSS variables；后续迁移 Ant Design 5 时需要把 `ConfigProvider.config` 迁到组件级 `ConfigProvider` token 配置。

风险：
- Ant Design 5/6 token、Less 变量和组件 API 变化较大。
- 当前仍有 AntD 4 API 调用点，包括复杂 `Menu.Item`/`Menu.SubMenu` JSX 菜单、Tooltip/Popover 的 `overlay` 内容、少量项目封装层的 legacy `visible` 入参；这是迁移 Ant Design 5/6 的主要阻塞。
- React Router 6/7 的路由声明和导航 API 有破坏性变化。

继续推进：
- React Router 预迁移第一批：分享页 token 路由和成员页 sidebar 选择态已从 `useRouteMatch` 的 `params/url` 读取迁到 `useParams` / `useLocation`，先减少 `useRouteMatch` 依赖，为后续 `Switch`、`Redirect`、`useHistory` 迁移铺路。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第二批：登录入口和三个分享页 Router 已从 `Route component=` 切换到 children element 写法，先减少 v5 旧版路由声明 API，为后续 `render`、`Redirect`、`Switch` 迁移铺路。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第三批：`AppRouter`、`MainPage` 以及成员、来源、调度、权限子页中的 `Route component=` / 简单 `render=` 已切换到 children element 写法；图表编辑路由改为局部组件内使用 `useLocation` 读取 query，保留原有导航与权限包装逻辑。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第四批：`VizPage/Main` 的看板编辑路由已移除 `Route render=`，并将 `vizId` 读取从 `useRouteMatch` 切到 `useParams`；`frontend/src/app` 下 React Router 旧路由声明热点已收敛到 `Redirect` 与 `useHistory` 为主。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第五批：成员页详情与侧边栏列表中的 `memberId` / `roleId` 读取已从 `useRouteMatch` 迁到 `useParams`，并结合 `useLocation().pathname` 保留成员与角色列表的选中态判断逻辑。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第六批：`Navbar` 与 `SourcePage` 中的模块名、`sourceId` 读取已从 `useRouteMatch` 迁到 `useLocation().pathname` / `useParams`，保留设置区子导航显隐和数据源详情页打开逻辑不变。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第七批：`ViewPage`、`VizPage Sidebar`、`SchedulePage`、`PermissionPage` 中剩余的 `useRouteMatch` 已全部迁到 `useParams` / `useLocation().pathname`，前端主应用内的路由参数读取已不再依赖 `useRouteMatch`。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第八批：新增 `useCompatNavigate` 兼容 hook，认证、登录、注册、激活和重置密码等固定跳转页面已不再直接依赖 `useHistory`，为后续切换到 `useNavigate` 预留统一替换点。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第九批：`OrganizationForm`、组织删除确认、邀请确认页和 `SchedulePage` 的详情跳转 hook 已切到 `useCompatNavigate`，同时补齐兼容 hook 的稳定引用，避免作为 hook 依赖时触发重复副作用。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第十批：`ViewPage` 和 `SchedulePage` 中的字符串路径跳转已切到 `useCompatNavigate`，覆盖视图 Tab/树/回收站以及调度列表/回收站，继续压缩主应用内对 `useHistory` 的直接依赖面。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第十一批：扩展 `useCompatNavigate` 以暴露 `location`，并将 `ViewPage` 的启动分析与编辑器工具栏迁到兼容层，覆盖带 `search` 参数和 `location.state` 读取的复杂导航场景。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第十二批：`VizPage` 与成员页侧边栏中的纯路径跳转已切到 `useCompatNavigate`，覆盖可视化回收站、文件夹树、故事板列表以及成员/角色列表与切换导航，继续缩小主应用侧边栏层对 `useHistory` 的直接依赖。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- React Router 预迁移第十三批：`useDrillThrough`、`useRecycleViz`、顶部导航和权限页侧边栏中的字符串跳转已切到 `useCompatNavigate`，覆盖品牌首页跳转、组织切换、登出回跳和权限页视角详情导航，继续收敛应用级公共入口对 `useHistory` 的直接依赖。
- React Router 预迁移第十四批：`SourcePage` 详情页、资源树、回收站和侧边栏新增入口中的导航已切到 `useCompatNavigate`，覆盖数据源创建后回跳、归档/恢复后的列表回退、资源树详情跳转以及带 `location.state` 的新建视图入口。
- React Router 预迁移第十五批：成员与角色详情页、图表工作台数据视图入口，以及故事板预览/编辑页中的导航已切到 `useCompatNavigate`，覆盖删除后的 replace 回退、故事板预览切编辑、编辑器关闭回跳和基于 `location.state` 的故事板加页事务清理。
- React Router 预迁移第十六批：`MainPage` 主入口和 `VizPage/Main` 标签页导航已切到 `useCompatNavigate`，覆盖图表编辑器关闭回退、保存后跳转、标签切换、关闭当前/其他/全部标签后的回跳，以及保留 `search` 参数的可视化页签导航。
- React Router 预迁移第十七批：`ChartEditor`、`ChartPreviewBoard` 和 Dashboard 头部/动作入口中的对象导航已切到 `useCompatNavigate`，覆盖保存后跳转、加入仪表板时携带 `location.state` 的跳转、可视化预览页回退，以及 Dashboard 编辑入口、关闭编辑器和加入故事板事务传参。
- React Router 预迁移第十八批：`VizPage` 文件夹侧边栏、`BoardEditor` 初始化流程和 Dashboard widget action 中透传的导航对象已切到 `useCompatNavigate`，覆盖新建分析入口、仪表板编辑态从 `location.state` 读取挂件信息，以及图表跳转动作对兼容导航对象的依赖收口。
- React Router 预迁移第十九批：前端主应用内剩余 `Redirect` 已统一替换为本地 `CompatRedirect` 组件，覆盖登录鉴权跳转、模块访问拒绝跳转以及 `MainPage` 根路径、组织首页和权限首页的默认跳转，先消除 `Redirect` 对 Router v5 的直接依赖。
- React Router 预迁移第二十批：`AppRouter`、三个分享页 Router、成员页和 `VizPage/Main` 中的 `Switch` 已统一替换为本地 `CompatSwitch` 组件，先把 Router 容器组件的升级入口收敛到单点，降低后续切换到 Router 6 `Routes` 时的改动面。
- React Router 预迁移第二十一批：新增 `CompatRoute` / `CompatRoutes`，并将 `AppRouter`、三个分享页 Router、成员页和 `VizPage/Main` 改写为 `element` 风格声明，先把低风险路由容器从 v5 的 children 形态迁到更接近 Router 6 `Routes + Route element` 的结构。
- React Router 预迁移第二十二批：`MainPage` 主路由容器和 `LoginAuthRoute` 已切到 `CompatRoutes + CompatRoute + element` 形态，主应用入口不再直接依赖 `Switch` 和 `Route` children 声明，为后续真正切换到 Router 6/7 的 `Routes` 打通主干。
- React Router 预迁移第二十三批：`SourcePage`、`SchedulePage`、`PermissionPage` 内部详情路由已统一切到 `CompatRoute`，`MainPage` 里最后一个 `useRouteMatch` 也已移除，继续把页面容器层对 Router v5 `Route` / `useRouteMatch` 的直接依赖压缩到兼容层本身。
- React Router 预迁移第二十四批：新增本地 `routerCompat` 出口，并将 `AppRouter`、分享页 Router、`Compat*` 组件、`useCompatNavigate`、`useRouteQuery` 以及部分主入口/测试页的路由导入统一收口到项目内部模块，为后续真正替换到底层 Router 6/7 实现先缩小 import 改动面。
- React Router 预迁移第二十五批：登录/找回密码页面、主导航、成员/来源/调度/Viz 侧边栏，以及三个分享页主页面的 `useLocation` / `useParams` / `Link` / `NavLink` 导入已继续切到本地 `routerCompat`，进一步扩大统一出口覆盖面，为下一步替换兼容层底座减少散点改动。
- React Router 预迁移第二十六批：成员/来源/调度详情页与故事板编辑/播放页的 `useParams` 导入已全部切到本地 `routerCompat`，当前 `frontend/src/app` 内对 `react-router-dom` / `react-router` 的直接依赖已经收口到 `routerCompat.ts` 单点，后续可以开始真正替换兼容层底座实现。
- React Router 预迁移第二十七批：`useCompatNavigate` 已改为项目内显式包装的导航 API，继续保留字符串和对象导航、`location.state` 与 `go/goBack` 兼容行为；`CompatRedirect` 同时移除了 `Route render` 旧写法，把后续迁移焦点进一步收敛到兼容层底座本身。
- React Router 预迁移第二十八批：`AuthorizedRoute` 已切到 `CompatRoute`，兼容层外部最后一个直接渲染 `Route` 的授权封装也已收回到本地路由组件；当前应用内的路由声明旧实现进一步集中到 `CompatRoute` 与 `routerCompat.ts` 两个底座点。
- React Router 预迁移第二十九批：删除仅做空转发的 `CompatSwitch`，让 `CompatRoutes` 直接承接 `Switch`；兼容路由容器层级继续压缩，为后续把 `CompatRoutes` 直接替换到 Router 6/7 `Routes` 减少一层过渡包装。
- React Router 预迁移第三十批：`CompatRoute` 与 `CompatRedirect` 已脱离 `RouteProps` 类型耦合，改为项目内部只暴露 `path` / `exact` / `element` / `to` 的窄接口；`routerCompat.ts` 也不再向外导出 `RouteProps`，进一步压缩 Router v5 类型面对业务层的渗透。
- React Router 预迁移第三十一批：新增 `CompatNavLink`，先承接 Navbar 中唯一仍依赖 `activeClassName` / `isActive` 的 v5 `NavLink` 历史 API；主导航和设置子导航已切到本地兼容组件，为后续把底层 `NavLink` 切到 Router 6 写法继续缩小业务改动面。
- React Router 预迁移第三十二批：`useRouteMatch` 已从 `routerCompat.ts` 出口移除，确认应用层与兼容层公开接口都不再依赖该 v5 历史 API；`CompatNavLink` 的自定义激活判断也改为只消费当前业务实际需要的 `pathname`。
- React Router 预迁移第三十三批：新增内部 `routerCompatLegacy.ts`，把 `Route` / `Switch` / `NavLink` / `useHistory` 全部关回兼容层内部；公开的 `routerCompat.ts` 只再暴露 `BrowserRouter`、`Link`、`MemoryRouter`、`useLocation`、`useParams` 这些 Router 6 仍稳定存在的能力，为真正升级依赖时缩小公开 API 差异。
- React Router 预迁移第三十四批：`CompatNavLink` 已彻底脱离底层 `NavLink`，改为 `Link + useLocation` 自行计算激活态；`routerCompatLegacy.ts` 因此不再需要保留 `NavLink`，Router 5 的链接级历史 API 已进一步从兼容层底座中清除。
- React Router 预迁移第三十五批：成员、来源、调度、权限四类详情容器已从 `CompatRoute` 切到 `useParams` / `useLocation` 驱动的条件渲染；这些页面不再把详情区显示逻辑委托给 v5 `Route` 匹配，兼容层底座对 `Route` 的真实依赖面继续缩小。
- React Router 预迁移第三十六批：`AppRouter` 中的 `LoginAuthRoute` 已回收为普通 `CompatRoute` 的 `element`，分享页三个 Router 和 Viz 看板编辑器入口也已去掉只承载单一路由的 `CompatRoutes` 包装；这些场景不再占用底层 `Switch`，兼容层对 `CompatRoutes` 的真实依赖进一步收敛到少数多分支主容器。
- React Router 预迁移第三十七批：`CompatRoute` / `CompatRoutes` 已改为使用 `useLocation + matchPath` 自行匹配，不再依赖底层 `Route` / `Switch` 运行时；`routerCompatLegacy.ts` 因此只剩 `useHistory`，兼容层内部的 Router 5 运行时依赖已压缩到最后一个导航能力点。
- React Router 预迁移第三十八批：新增 `routerCompatRuntime.tsx`，由兼容层自持 `BrowserRouter` / `MemoryRouter` 与 history 上下文；`useCompatNavigate` 已改为直接消费内部 `useCompatHistory`，不再依赖 `useHistory` hook，至此兼容层内部对 Router 5 运行时 hook 的依赖也已清空。

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

已完成：
- `js-cookie 2.2.1 -> 3.0.8`，`@types/js-cookie 2.2.6 -> 3.0.6`，先完成低风险认证 cookie 库升级，保持现有 token 读写封装不变。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- `axios 0.21.1 -> 1.17.0`，完成请求库主线升级；按 `AxiosError<APIResponse<any>>` 补齐错误响应类型，保持现有拦截器和错误提示行为不变。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- `react-beautiful-dnd` 已从数据模型树场景迁出到维护中的 `@hello-pangea/dnd 18.0.1`，保留现有拖拽排序、组合建层级和层级内移动逻辑；补齐 `Draggable index` 的 number 约束。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。
- `typescript 4.5.4 -> 5.9.3`，`@reduxjs/toolkit 1.8.0 -> 2.12.0`，`react-redux 7.2.6 -> 9.3.0`；同步移除 `@types/react-redux`，按 RTK 2 的 `enhancers` 新签名调整 store 配置，并修复 TS5 暴露的公共树型 helper、图表样式和迁移测试类型问题。
- 2026-06-09 验证：`npm run checkTs`、`npm run build` 均通过。

验收门槛：
- `npm run checkTs` 通过。
- 主要单测通过或迁移完成。
- 请求封装和认证流程正常。

### 阶段 6：后端库现代化

目标：删除老旧、高风险或重复的后端基础库。

当前 review 结论：
- `Selenium 3 + PhantomJSDriver` 是当前后端最老旧、风险最高的自动化组合。PhantomJS 已停止活跃维护，现代替代应优先考虑 `Playwright`，如必须保留 JVM 内集成则退而求其次迁到 `Selenium 4 + Chromium WebDriver`。
- `fastjson 1.2.x` 仍在多个模块承担 JSON 解析、HTTP message converter 和配置反序列化，属于历史包袱。对 Spring Boot 3 项目，更现代且收敛的方向是统一到 `Jackson`；如果必须保留阿里系 API，则至少迁到 `fastjson2`，但不建议继续双栈长期共存。
- JWT 目前同时存在 `jjwt 0.7.0` 和 `java-jwt 3.7.0` 两套老版本实现，维护成本高且安全面分散。建议统一到单一现代库，优先 `jjwt 0.12+`，也可评估是否完全交给 Spring Security OAuth2/JOSE 能力。
- `Apache HttpClient 4.5.x` 与 `OkHttp` 并存，HTTP 客户端重复。JDK 21 下更现代的方向是统一到 `java.net.http.HttpClient` 或只保留一套活跃客户端；若短期兼容成本最低，可先升级到 `HttpClient 5` 并逐步收口。
- `commons-lang 2.6`、`commons-io 1.3.1`、`guava 21.0`、`poi-ooxml 5.0.0`、`commons-csv 1.8` 都明显偏旧，其中 `commons-lang 2` 和 `commons-io 1.x` 优先级更高，因为现代代码大多已可用 `commons-lang3`、`commons-io 2.x` 或 JDK 标准库替换。
- `H2 1.4.200` 仍适合作为兼容 demo 库，但不应视作长期现代化终态。若继续维护 demo / 测试数据，应补一套可在 `H2 2.x` 下运行的数据脚本或改为容器化测试数据库。
- `Shiro 2` 现在已经能在 Boot 3 上运行，但从 Spring 生态一致性看，长期仍弱于 `Spring Security` 原生体系。这个迁移收益很高，但改动面极大，不应排在 Router、测试栈和 JSON/JWT 清理之前。

建议升级顺序：
1. 浏览器自动化：`PhantomJS` / `Selenium 3` -> `Playwright` 或 `Selenium 4`。
2. 后端 JSON 栈：`fastjson 1.x` -> `Jackson` 单栈。
3. JWT 栈统一：移除 `jjwt 0.7` 与 `java-jwt 3.7` 双实现。
4. 基础工具库清理：`commons-lang 2`、`commons-io 1.x`、`guava 21`、`httpclient 4`。
5. 数据与 demo 兼容：`H2 1.4` 升级路线。
6. 安全框架长期演进：评估 `Shiro -> Spring Security`。

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

## 阶段交付物与完成定义

这一节用于把升级路线变成可执行项目计划。每个阶段只有在“交付物”和“完成定义”都满足时，才算真正完成。

### 阶段 0 交付物

- 根 `pom.xml`、前端 `package.json`、`Dockerfile` 中的基础运行时版本声明完成统一。
- 文档中明确最低 Java / Maven / Node / npm 要求。
- 本地与发布包的启动路径可复现。

完成定义：
- 新同事按文档准备环境后，能在不额外猜测版本的情况下完成构建和启动。

### 阶段 1-2 交付物

- Vite 成为默认开发与生产构建链。
- CRA / CRACO 退出前端开发、构建与测试主链。
- 多入口 HTML、静态资源复制、parser task 生成链稳定。

完成定义：
- 后端 Maven `package` 产物不再依赖 CRA 链。
- 前端开发、生产构建和后端托管结果一致。

### 阶段 3 交付物

- React 18 主升级完成。
- React 18 类型收口完成。
- React 18 下主流程可运行。

完成定义：
- 不再保留任何为了兼容 React 17 而存在的临时实现。

### 阶段 4 交付物

- React Router 6/7 升级完成。
- Ant Design 5 升级完成。
- 主题、弹层、菜单和复杂工作区页面人工验收完成。

完成定义：
- 前端主应用不再直接依赖 Router 5 API。
- AntD 4 专属 API 和样式补丁收敛到可删除状态。

### 阶段 5 交付物

- Enzyme 完全移除。
- CRA 测试链完全退出。
- Testing Library 与独立测试运行器稳定。
- Lint / Format / Type Check 工具链与 Node 26 稳定兼容。

完成定义：
- `frontend/package.json` 中不再保留 `enzyme`、`@cfaester/enzyme-adapter-react-18`、`react-scripts`、`@craco/craco`。

### 阶段 6 交付物

- PhantomJS / Selenium 3 替换完成。
- `fastjson 1.x` 清理完成。
- JWT 双栈统一完成。
- 旧基础库分批替换完成。
- demo / 内置样例数据库策略明确。

完成定义：
- `pom.xml` 各模块中不再出现 `phantomjsdriver`、`selenium-java 3.x`、`fastjson 1.x`、`jjwt 0.7.0`、`java-jwt 3.7.0`、`commons-lang 2.x`、`commons-io 1.x`。

### 阶段 7 交付物

- CI 流水线覆盖后端测试、前端检查、前端构建、集成启动验证。
- Docker 镜像和发布包都能在干净环境运行。
- 关键业务验收有稳定脚本或检查表。

完成定义：
- 同一提交在本地、CI、Docker 和发布包中的构建行为一致。

## 建议执行顺序

基于当前状态，后续执行建议按下面顺序推进：

1. 完成 React Router 预迁移收口并切换到 Router 6/7。
2. 继续清理 AntD 4 历史 API，完成 AntD 5 升级。
3. 清理 CRA / Enzyme 测试链，迁到现代测试方案。
4. 替换 PhantomJS / Selenium 3。
5. 推进后端 JSON 栈单栈化。
6. 统一 JWT 实现。
7. 分批替换旧基础库。
8. 处理 H2/demo 与长期安全框架演进。

## 当前 backlog 量化视图

这一节用于把计划落到当前代码面，避免后续升级只凭感觉推进。

### A. React Router 主升级 backlog

当前扫描结论：
- 主应用业务代码里的 `useRouteMatch`、`Route component=`、`Route render=`、业务层 `Redirect` 已基本清空。
- 剩余 Router 5 依赖主要集中在兼容层自身：
  - `frontend/src/app/hooks/useCompatNavigate.ts`
  - `frontend/src/app/components/CompatSwitch.tsx`
- 剩余页面容器层直接 `Route` 基本只剩少量授权/布局封装，主应用内容页容器已大多切到 `CompatRoute`。
- `AuthorizedRoute` 已收口到 `CompatRoute`，兼容层外部最后一个直接渲染 `Route` 的授权封装已移除。
- 路由能力的 import 源已开始从 `react-router-dom` / `react-router` 向本地 `routerCompat` 收口，后续可以按批次继续把剩余页面切到同一出口。
- 当前剩余直接依赖已经集中到少量详情页、故事板编辑/播放页和兼容层本身，后续更适合继续按模块批次清理，而不是全局撒网式替换。
- 当前应用层对外部路由包的直接依赖已经清空，只剩 `routerCompat.ts` 作为单点出口；下一阶段重点应转向 `useCompatNavigate`、`CompatSwitch`、`CompatRoutes` 和 `CompatRedirect` 的真实底座迁移。
- `useCompatNavigate` 和 `CompatRedirect` 已不再直接把 v5 `history.push/replace` 与 `Route render` 透传给业务层，下一步可以继续推进 `CompatSwitch` / `CompatRoutes` 的真实底座替换。
- `CompatSwitch` 已删除，`CompatRoutes` 不再经过额外空包装层，兼容路由容器已进一步收敛。
- `CompatRoute` / `CompatRedirect` 已不再依赖 `RouteProps`，兼容层对外只暴露当前业务实际使用到的最小路由声明能力。
- `NavLink` 的 v5 历史 API 已收口到 `CompatNavLink`，当前业务层不再直接依赖 `activeClassName` / `isActive`。
- `useRouteMatch` 已从兼容出口移除，当前公开路由能力里已不存在该历史 API。
- 公开 `routerCompat.ts` 已不再暴露 `Route` / `Switch` / `NavLink` / `useHistory`，这些 v5 旧能力只保留在兼容层内部的 `routerCompatLegacy.ts`。
- `CompatNavLink` 已不再依赖底层 `NavLink`，当前兼容层内部剩余的 Router 5 旧能力已进一步收缩到 `Route` / `Switch` / `useHistory`。
- 成员、来源、调度、权限这些“详情区条件显示”页面已不再依赖 `CompatRoute`，当前 `Route` 的主要残留压力已经收敛到主入口和少数真正需要互斥匹配的容器。
- `CompatRoutes` 已从分享页 Router、Viz 看板编辑器入口和 `LoginAuthRoute` 的特殊子项场景中退出，当前 `Switch` 的主要残留压力已经收敛到主应用和少量真正需要多分支互斥的容器。
- `CompatRoute` / `CompatRoutes` 已不再依赖底层 `Route` / `Switch`，当前兼容层内部剩余的 Router 5 运行时依赖只剩 `useHistory`。
- `useCompatNavigate` 已改为消费兼容层自持的 history 上下文，`useHistory` hook 依赖已移除；当前兼容层对 Router 5 的运行时依赖已经不再通过 v5 hooks 暴露。
- 当前仍保留旧 Router 5 运行时语义的核心点，已经压缩到：
  1. `frontend/src/app/routerCompatRuntime.tsx`
  2. `frontend/src/app/routerCompatLegacy.ts`
- 剩余需要继续处理的重点不是“全局搜索更多旧 API”，而是：
  1. 让 `CompatRoute` / `CompatRoutes` 真实接管到 Router 6/7。
  2. 让 `useCompatNavigate` 从 `useHistory` 切到 `useNavigate`。
  3. 让 `CompatRedirect` 最终对齐到 Router 6 `Navigate` 语义。
  4. 补齐嵌套路由、默认跳转、参数路由和分享页的回归验证。

工作包拆分：
1. 兼容导航层主替换：`useCompatNavigate` 从 `useHistory` 切到 `useNavigate`。
2. 兼容重定向替换：`CompatRedirect` 对齐 `Navigate` 语义。
3. 兼容路由容器替换：`CompatRoutes` / `CompatRoute`。
4. 主入口验收：`AppRouter`、`LoginAuthRoute`、`MainPage`、share routers。
5. 复杂页面回归：`VizPage`、`ViewPage`、成员页、权限页、故事板。

### B. Ant Design 5 backlog

当前扫描结论：
- 仍有一批 `visible=` 历史 API，集中在分享页验证码弹窗、保存表单、Profile/Password、变量与成员管理、富文本颜色选择器等位置。
- 仍有一批 JSX `Menu.Item` / `Menu.SubMenu` / `Menu.Divider` 遗留，主要集中在：
  - 图表工作台字段动作菜单
  - Popup/MenuListItem 封装
  - Navbar/Profile 菜单
  - StoryHeader / VizHeader 等自定义 dropdownRender 入口

工作包拆分：
1. `visible` -> `open` / `onOpenChange` 收尾。
2. JSX Menu API -> `items` 配置收尾。
3. 自定义 `dropdownRender` / Popup 封装统一。
4. 主题 token / ConfigProvider 迁移准备。
5. AntD 5 真正升级与回归。

### C. 前端测试栈 backlog

当前扫描结论：
- `frontend/package.json` 已不再保留：
  - `react-scripts`
  - `@craco/craco`
  - `enzyme`
  - `@cfaester/enzyme-adapter-react-18`
- 当前残留主要是：
  - Jest 版本仍停留在 27.5.1
  - `react-app-polyfill` 在 `entryPointFactory.tsx`、`task.ts`、`setupTests.ts` 中仍有显式引入
  - ESLint 工具链仍依赖 `eslint-config-react-app`

工作包拆分：
1. Enzyme 测试盘点与替换优先级排序。
2. `setupTests.ts` 迁到纯 Testing Library。
3. Jest 27 升级到较新稳定线，或迁到 Vitest。
4. `react-app-polyfill` 的浏览器支持策略确认与移除。
5. 继续清理 `eslint-config-react-app` 等 CRA 历史工具链残留。

### D. 后端现代化 backlog

当前扫描结论：
- 浏览器自动化：
  - `core/pom.xml`
  - `server/pom.xml`
  仍声明 `phantomjsdriver` 和旧 `selenium-java`
- JSON 栈：
  - `security/src`
  - `core/src`
  - `server/src`
  仍广泛直接依赖 `fastjson`
  - `server/src/main/java/datart/server/config/WebMvcConfig.java` 仍直接注册 `FastJsonHttpMessageConverter`
- JWT：
  - `core/pom.xml` 中 `jjwt 0.7.0`
  - `security/pom.xml` 中 `java-jwt 3.7.0`
- 旧基础库：
  - `commons-lang 2.6`
  - `commons-io 1.3.1`
  - `guava 21.0`
  - `httpclient 4.5.14`
  - `h2 1.4.200`

工作包拆分：
1. PhantomJS / Selenium 3 调用点梳理与替换方案选型。
2. FastJson 使用面梳理：DTO、配置、message converter、导入导出。
3. JWT 双栈使用链路梳理。
4. 旧基础库逐项替换与回归验证。
5. demo/H2 策略与样例数据迁移。

### E. 推荐近期执行队列

基于当前风险和收益，建议接下来的 5 个 checkpoint 按这个顺序推进：

1. Router 兼容层主替换：`useCompatNavigate` / `CompatRoutes` 真正接管到 Router 6/7。
2. AntD 5 收尾清理：优先处理 `visible` 和剩余 JSX `Menu.*`。
3. 测试栈现代化：先升级 Jest 或迁到 Vitest，再处理 `react-app-polyfill` 与 ESLint 残留。
4. 浏览器自动化现状梳理，确定 `Playwright` 还是 `Selenium 4`。
5. FastJson 使用面梳理并设计 Jackson 迁移边界。

## 最终完成定义

当且仅当以下条件同时成立，才可视为“整个技术栈清单都是现代化替代方案”已经完成：

- 文档中的“最终现代化技术栈清单”全部落地，而不是只完成一部分。
- 前端不再依赖 Router 5、CRA/CRACO、Enzyme、AntD 4 历史 API。
- 后端不再依赖 PhantomJS、Selenium 3、fastjson 1.x、双 JWT 老实现、`commons-lang 2`、`commons-io 1.x` 等已认定为老旧的核心依赖。
- 本地、CI、Docker、发布包都能使用同一套现代化构建链。
- 关键业务流程、分享页、登录鉴权、图表/仪表板、截图导出都完成验收。

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

1. 继续预处理 AntD 5 API 迁移热点：复杂 `Menu.Item`/`Menu.SubMenu` JSX 菜单、Tooltip/Popover 的 `overlay` 内容和项目封装层 legacy `visible` 入参。
2. 继续 React Router 5 -> 6/7 预迁移，优先清理剩余 `Route component=` / `Route render=`、`Redirect`、`useHistory` 和嵌套路由旧写法。
3. 继续升级前端测试栈到较新稳定线，并处理 `react-app-polyfill` 与 `eslint-config-react-app` 残留。

## 2026-06-09 老旧技术栈盘点

本轮全项目 review 后，仍然建议继续现代化的重点如下，按优先级由高到低排列：

1. React Router 5
   - 现状：主应用仍残留一批 `useHistory`、`Redirect` 和 v5 风格路由声明。
   - 更现代替代：React Router 6/7。
   - 当前动作：先继续用 `useCompatNavigate` 收口导航调用点，再统一升级路由声明与重定向。

2. CRA 测试链残留
   - 现状：运行、构建、测试链已全部脱离 CRA/CRACO，但 Jest 仍停留在 27.5.1，ESLint 仍借用 `eslint-config-react-app` 这类 CRA 时代工具配置。
   - 更现代替代：Vitest，或独立 Jest 29/30 配置。
   - 升级前提：先完成 React Router 与 AntD 主迁移，避免多个高风险面同时变化。

3. Enzyme 过渡适配层
   - 现状：`frontend/src/setupTests.ts` 仍依赖 `enzyme` 和社区 React 18 adapter。
   - 更现代替代：React Testing Library。
   - 风险判断：Enzyme 目前只是勉强兼容 React 18，不适合作为长期测试基线。

4. Ant Design 4.24
   - 现状：已经在 AntD 4 最后稳定线，但仍不是当前主线。
   - 更现代替代：Ant Design 5。
   - 当前阻塞：复杂 Dropdown/Menu/Modal/Popover 历史 API 仍需继续清理。

5. `react-app-polyfill`
   - 现状：`frontend/src/setupTests.ts` 和运行时仍显式引入 `ie11`/`stable` polyfill。
   - 更现代替代：按浏览器支持矩阵精简到 Vite + `browserslist`/按需 polyfill。
   - 风险判断：需要先确认产品是否还要求兼容 IE 11。

6. `fastjson 1.x`
   - 现状：后端多个模块和 Web MVC message converter 仍直接依赖 `fastjson`。
   - 更现代替代：优先统一到 Jackson；如必须保留阿里生态特性，再评估 `fastjson2`。
   - 风险判断：这是后端最敏感的基础设施替换之一，需要按 DTO/配置/导入导出链路分批验证。

7. `jjwt 0.7.0`
   - 现状：JWT 依赖明显偏老。
   - 更现代替代：JJWT 0.12+，或统一到单一现代 JWT 库。
   - 风险判断：会影响 token 签发、解析和兼容性验证。

8. Selenium 3 + PhantomJS Driver
   - 现状：`core` 与 `server` 仍保留 `selenium-java 3.x` 和 `phantomjsdriver`。
   - 更现代替代：Selenium 4 + Headless Chrome/Edge，或 Playwright。
   - 风险判断：会影响截图、导出和服务器浏览器依赖安装方式。

9. 后端通用基础库老版本
   - 现状：仍有 `commons-lang 2.6`、`commons-io 1.3.1`、`guava 21.0`、`httpclient 4.5.x`、`poi-ooxml 5.0.0`、`commons-csv 1.8`、`aspectjweaver 1.9.8.M1`、`mysql-connector-java 8.0.29` 旧坐标等。
   - 更现代替代：优先迁到 Boot 3 生态兼容的较新稳定线，并尽量使用 BOM 或父 POM 管理。
   - 风险判断：这类升级适合拆成多个小批次，不应与 JSON/JWT/浏览器自动化迁移混做。

10. H2 1.4.200 demo 库
   - 现状：仅用于 demo/内置样例，但版本过旧。
   - 更现代替代：H2 2.x，或测试环境改用 Testcontainers。
   - 风险判断：需要同步准备可读的新 demo 数据或迁移脚本，不能只升版本号。

### React Router 预迁移第三十九批之外的并行治理：浏览器自动化依赖收口

- `core` 与 `server` 已删除未被实际代码使用的 `phantomjsdriver` 依赖，避免继续保留已退出主流维护链的历史驱动。
- `selenium-java` 已从两个模块各自声明的 3.x 版本统一到父 POM 的 `${selenium.version}`，当前收敛为 `4.31.0`。
- 现有截图代码 `WebUtils` 只使用 `ChromeDriver` / `RemoteWebDriver`，没有任何 PhantomJS 专属分支，因此这一步属于依赖树现代化，不改变当前截图运行语义。

### 并行治理：MySQL JDBC 驱动坐标收口

- `core/pom.xml` 已将旧坐标 `mysql:mysql-connector-java` 切换为当前官方 Maven 坐标 `com.mysql:mysql-connector-j`。
- 驱动类和 JDBC URL 仍保持 `com.mysql.cj.jdbc.Driver` 与 `jdbc:mysql://`，因此这一步是依赖声明现代化，不改变现有连接语义。
- 版本号交由 Spring Boot 父 POM 管理，减少 JDBC 驱动版本继续手写漂移的风险。

### 并行治理：收紧 CRA 与 IE11 残留类型壳

- 运行时与测试入口已删除 `react-app-polyfill/ie11` 显式引入，浏览器兼容策略进一步从 IE11 历史包袱收缩到现代浏览器基线。
- `frontend/src/react-app-env.d.ts` 已去掉 `react-scripts` 类型引用，改为保留项目真实需要的本地声明。
- 为 Vite 链路补齐了 `*.svg` / `ReactComponent` 的显式类型声明，避免继续依赖 CRA 类型包做隐式兜底。

### React Router 预迁移第四十批：切到 Router 6 经典运行时底座

- `frontend/package.json` 已将 `react-router-dom` 切换到 `^6.30.1`，并删除只服务于 v5 的 `@types/react-router-dom`。
- `routerCompat.ts` 已直接回到 `react-router-dom` 的 `BrowserRouter`、`MemoryRouter`、`Link`、`useLocation`、`useNavigate`、`useParams`。
- 删除了只为 v5 `Router history` 桥接而存在的 `routerCompatRuntime.tsx`，兼容层不再依赖 `react-router-dom/node_modules/history/*` 这类脆弱内部路径。
- `useCompatNavigate` 已改为包装 `useNavigate + useLocation`，`location` 改为响应式来源，不再透传自持 history 的静态快照。
- `CompatRoute` 已切到 Router 6 的 `matchPath(pattern, pathname)` 新签名，并把原有 `exact` 语义映射到 v6 的 `end`。

### 并行治理：移除 Enzyme 测试基座

- `frontend/src/setupTests.ts` 已删除 Enzyme 初始化，前端测试基座不再依赖 React 18 的社区 Enzyme adapter。
- `frontend/package.json` 已移除 `enzyme` 与 `@cfaester/enzyme-adapter-react-18`。
- 当前测试文件已全部使用 Testing Library / Jest 风格断言，Enzyme 仅剩历史初始化残留，因此这一步不会改变现有测试写法。

### 并行治理：测试执行入口脱离 `craco test`

- `frontend/package.json` 的 `test` 脚本已从 `craco test` 切到 `jest --config jest.config.js`。
- `frontend/jest.config.js` 已改为静态独立配置，测试执行入口已经不再依赖 CRACO 配置生成。
- `frontend/jest/` 已新增本地 `babelTransform.js`、`cssTransform.js`、`fileTransform.js`，测试链不再依赖 `react-scripts/config/jest/*`。
- `frontend/package.json` 与 lockfile 已显式声明 `jest`、`jest-environment-jsdom`、`babel-jest`、`babel-preset-react-app`、`identity-obj-proxy`、`jest-watch-typeahead`，并移除 `react-scripts`。

### 并行治理：删除 CRACO 回退外壳

- `frontend/package.json` 已删除 `start:cra`、`build:cra`、`eject` 等只服务 CRA/CRACO 的历史脚本。
- `frontend/package.json` 与 lockfile 已移除 `@craco/craco`，并同步删除只服务旧 webpack/CRACO 外壳的 `cross-env`、`monaco-editor-webpack-plugin`、`webpackbar`、`webpack-cli`、`@types/webpack`、`@types/webpack-env`。
- `frontend/craco.config.js` 已删除，前端运行与构建主链只保留 Vite 配置作为单一入口。

### 并行治理：Vite 浏览器兼容告警暴露

- 在删除 `react-scripts` 后重新验证 Vite 构建时，`src/app/pages/DashBoardPage/pages/BoardEditor/slice/events.ts` 引入的 Node `events` 模块触发了浏览器外置化告警。
- 当前构建仍然成功，这说明它不是本批的阻断项，但需要后续单独评估是改为浏览器友好的事件实现，还是在 Vite 侧显式补兼容策略。
