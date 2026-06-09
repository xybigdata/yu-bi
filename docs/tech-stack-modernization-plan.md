# 技术栈现代化升级计划

本文档记录 Datart 技术栈现代化迁移的目标终态、阶段顺序、验收门槛和风险控制。它是后续升级工作的执行基线，避免把多个高风险迁移混在同一个提交里。

## 当前基线

- Java 运行时：JDK 21。
- 后端主框架：Spring Boot 3.5.12，Spring Cloud 2025.0.1。
- 构建工具：Maven，`maven-compiler-plugin` 3.14.1，`maven-assembly-plugin` 3.8.0，`exec-maven-plugin` 3.6.3。
- 前端运行时：本机 Node 26 可启动，默认开发和生产构建已切换到 Vite 5。
- 前端主框架：React 18、React Router 5、Ant Design 4.24、TypeScript 5.9；CRACO 7/CRA5 仅作为回退脚本保留。
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

1. 继续预处理 AntD 5 API 迁移热点：复杂 `Menu.Item`/`Menu.SubMenu` JSX 菜单、Tooltip/Popover 的 `overlay` 内容和项目封装层 legacy `visible` 入参。
2. 继续 React Router 5 -> 6/7 预迁移，优先清理剩余 `Route component=` / `Route render=`、`Redirect`、`useHistory` 和嵌套路由旧写法。
3. 保持 CRA5/CRACO 回退脚本，直到 Jest/测试栈迁出 CRA。

## 2026-06-09 老旧技术栈盘点

本轮全项目 review 后，仍然建议继续现代化的重点如下，按优先级由高到低排列：

1. React Router 5
   - 现状：主应用仍残留一批 `useHistory`、`Redirect` 和 v5 风格路由声明。
   - 更现代替代：React Router 6/7。
   - 当前动作：先继续用 `useCompatNavigate` 收口导航调用点，再统一升级路由声明与重定向。

2. CRA/CRACO 测试链
   - 现状：运行与构建已迁到 Vite，但 `react-scripts`、CRACO、Jest 27 仍作为测试与回退链保留。
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
