# yu-bi 技术栈现代化改造执行板

本文档只保留当前有效信息，用来指导后续逐批改造。

这里的“现代化”指的是：在兼容、正确、可回归前提下，收口到较新的稳定版本，而不是盲目追最新。

## 1. 改造目标

- 后端稳定兼容 `JDK 21`
- 前端工程链稳定兼容 `Node 24`
- 前端 CI 与本地开发基线统一使用 `Node 24.x`
- 改造按专题拆分，做到可验证、可回退、可持续推进

## 2. 固定边界

### 2.1 仓库与分支

- 工作目录：`/Users/chencongyu/WorkHome/VSProjects/open-project/yu-bi`
- `main` 只允许 merge，不直接开发
- 低风险改造阶段改为在单一长期分支持续推进，累计到足够批量后再 merge 回 `main`
- 高风险或跨域专题仍单独建分支，不在文档中写死分支名
- 默认自动 `git add`、`git commit --no-verify`

### 2.2 不直接触碰的高风险标识

- Java 包名 `datart.*`
- 配置前缀 `datart.*`
- `DATART_*` 等内部技术符号
- 数据迁移相关稳定常量、后缀、内部标识

### 2.3 工作区边界

- 不动未跟踪目录：`.tmp/`、`logs/`
- 一个提交只处理一个专题
- 只提交与当前专题直接相关的文件
- 尽量按专题攒成一批再提交，避免高频提交带来重复回归成本

## 3. 当前基线

### 3.1 后端

| 项目 | 当前基线 | 判断 |
| --- | --- | --- |
| Java | `21` | 已达目标 |
| Spring Boot | `3.5.12` | 当前主链稳定 |
| Spring Cloud | `2025.0.1` | 与 Boot 3.5 配套 |
| Maven Enforcer | `Java >= 21`、`Maven >= 3.9` | 已落地 |
| MyBatis Spring Boot | `3.0.4` | 已适配 Boot 3 |
| GraalJS | `25.0.1` | 已替代 Nashorn 主链 |
| H2 | `2.4.240` | 已升级 |
| Selenium | `4.31.0` | 已升级 |
| Springdoc | `2.8.17` | 已适配 Boot 3 |
| Shiro | `2.0.5` | 高风险遗留 |
| Druid | `1.2.28` | 暂不优先动 |
| Calcite | 现网主链依赖 | 高风险遗留 |

补充：

- `jjwt`、`httpclient5`、`poi-ooxml`、`commons-csv` 已在较新稳定线
- 后端当前重点是兼容验证与依赖健康维护，不做结构性迁移

### 3.2 前端

| 项目 | 当前基线 | 判断 |
| --- | --- | --- |
| Node | `>=24.0.0` | 本地默认基线 |
| `frontend/.nvmrc` | `v24.0.0` | 与本地基线一致 |
| npm | `>=11.0.0` | 已写入 `engines` |
| React | `18.3.1` | 已完成主升级 |
| React Router | `6.30.1` | 已完成主升级 |
| Ant Design | `5.26.2` | 已完成主升级 |
| Redux Toolkit | `2.12.0` | 已完成主升级 |
| React Redux | `9.3.0` | 已完成主升级 |
| TypeScript | `5.9.3` | 当前稳定主线 |
| Vite | `6.4.3` | 已替代 CRA 主工作流 |
| Vitest | `4.1.8` | 已成为主测试栈 |
| styled-components | `6.1.19` | 已完成主升级 |
| react-quill | `2.0.0` | 已升级，仍需稳定化 |
| react-window | `1.8.6` | 仍在真实运行时链路 |
| flexlayout-react | `0.5.21` | 已收口到当前稳定小版本 |
| react-grid-layout | `1.3.4` | 已收口到当前稳定小版本 |
| monaco-editor | `0.52.2` | 真实运行时依赖 |
| reveal.js | `6.0.1` | 真实运行时依赖 |

补充：

- 当前前端主链已经是 `React 18 + Ant Design 5 + Vite 6 + Vitest 4`
- `react-dev-inspector` 已从开发态接入中移除

### 3.3 工程化

| 项目 | 当前基线 | 判断 |
| --- | --- | --- |
| Husky | `9.1.7` | 已升级 |
| lint-staged | `17.0.7` | 已升级 |
| commitlint | `21.0.2` | 已升级 |
| Docker 运行时 | `eclipse-temurin:21-jre` | 与 JDK 21 对齐 |
| 前端 CI | `Node 24.x` | 与当前基线一致 |
| Java CI | `JDK 21` | 已覆盖 |

## 4. 已完成的有效成果

这里只保留仍会影响后续判断的结果。

### 4.1 主链已完成

- 项目已从 datart 独立为 `yu-bi`
- 品牌、治理文档、基础工程信息已完成独立化收口
- 后端已建立 `JDK 21 + Spring Boot 3.5.x` 主链
- 前端已建立 `React 18 + Ant Design 5 + Vite 6 + Vitest 4` 主链
- CRA / CRACO、IE11 主兼容链、Nashorn、PhantomJS 等历史主链已退出

### 4.2 近期已完成专题

- 前端安装健康度审计与锁文件收口
- 未使用前端依赖清理：`@antv/g2`、`html2canvas`
- `react-resizable` 收口到 `3.0.4`
- Ant Design 深路径导入与历史类型入口收口
  - `rc-field-form/lib/interface` 与 `antd/es/slider` 等历史类型深路径继续收口到公开导出入口，降低对内部目录结构的耦合
- `rc-table` 历史类型入口继续收口
  - `VirtualTable` 的 `CustomizeScrollBody`、`DataIndex`、`ScrollConfig` 改为基于 `antd` 公开表格 props 与 `rc-table` 包根公开类型反推，去掉 `rc-table/es/interface` 依赖
- 前端展示与控制器弱类型边界继续收口
  - 控制器筛选组件的 `options`、选中值和多选回调不再使用宽泛 `any`
  - `translations.ts` 的语言 JSON 递归转换补齐对象分支类型
  - 运行态控制器组件的 `value` / `onChange` / 本地状态继续按真实数值或字符串边界收紧，避免 `Slider / Number / RangeNumber / Text` 继续透传宽泛 `any`
  - 运行态单选与时间控制器的值边界继续收紧，`RadioGroup / Time / RangeTime` 改为按真实单值、区间值和日期回调语义透传，避免继续使用宽泛 `any`
  - 运行态选择类控制器的回调值边界继续收紧，`Select / MultiSelect / CheckboxGroup / Tree` 改为按真实单值、多值和树选择值语义透传，避免继续保留未声明的 `values` 形态
  - 配置态基础 setter 的值边界开始收口，`SingleTimeSet / TextSetForm / RadioStyleSet / SqlOperatorSet` 改为按真实日期、文本和选择值语义透传，减少 `FormItemProps<any>` 与宽泛 `onChange` 的扩散
  - 配置态数值 setter 的值边界继续收口，`NumberSetForm / RangeNumberSet / MaxAndMinSetter` 改为按真实数值与区间数值语义透传，减少 `FormItemProps<any>` 与宽泛数组值
  - 配置态滑块 setter 的值边界继续收口，`SliderSet / RangeSliderSet` 改为按 antd 公开滑块单值与区间值语义透传，避免继续保留 `useState<any>` 与宽泛数组值
  - 配置态相对时间 setter 的值边界继续收口，`RelativeTimeSetter` 改为按真实方向、数量和时间单位语义透传，避免继续保留宽泛 `value` / `onChange` 与未声明的数值输入类型
  - 配置态树默认值选择链路的值边界继续收口，`ValuesOptionsSetter` 改为按树勾选返回的真实键值数组透传，避免继续保留 `onCheck={(checkedObj: any) => ...}` 这类未声明对象形态
  - 控制器关联字段值类型过滤链继续收口，`filterValueTypeByControl` 改为按字段类型和变量值类型联合约束，避免继续保留 `valueType: any`
  - 控制器配置默认值数组边界开始收口，`controllerValues` 改为显式 `string | number` 数组，先覆盖当前真实使用的单值、多值与区间数值场景
  - 运行态控制器提交值边界继续收口，`ControllerWidgetCore.onFinish` 改为按单值、多值和区间值的真实表单输入形态归一化，避免继续保留未声明的 `value.value`
- 富文本兼容层第一批稳定化小修
- 富文本兼容层第二批稳定化小修：
  - `RichTextEditor` 的 `onChange` 类型边界收紧
  - `ChartRichTextAdapter` 的弱类型参数与运行时保护收口
  - `BasicRichText` 的 `getOnChange()` 返回类型收口
  - 富文本内容安全解析与最小 Delta 结构归一化补齐，避免脏 JSON 或异常对象直接打断编辑态/只读态
  - 富文本运行时补齐显式就绪态防护，避免异步运行时尚未挂载时提前执行 Markdown/调色板调用
  - 富文本内容解析补齐“仅对对象/数组形态 JSON 尝试解析”的保护，避免 `123` / `true` / `null` 这类纯文本被误判后清空
- 富文本看板组件内容持久化边界继续收口：
  - `RichTextWidgetCore` 改为按显式 `richText.content` 结构读取与写回内容
  - 去掉保存链路里的裸 `JSON.parse` 与局部 `any`，保持写回结构不变
  - 富文本模块配置与自定义颜色回调按真实值类型声明
- 富文本适配器编辑链路边界继续收口：
  - `ChartRichTextAdapter` 的模块配置与字段引用值按真实结构声明
  - calcfield 引用字段读取改为显式对象形态收口，减少 `Record<string, any>`
  - 插入引用字段后的异步回写改为显式微任务调度，避免继续依赖 `setImmediate`
- 时间体系多批次收口：
  - 时间选择器回填链路
  - 展示与表单日期回填链路
  - 通用时间格式化入口
  - 图表时间值解析入口
  - 图表时间序列化入口
  - 区间时间值转换入口
  - 日期数组回填入口
  - 图表预览时间筛选器的单值序列化与范围值回填边界
  - 当前时间获取入口补齐为 `getDatartNow()` / `getDatartNowMillis()` / `formatCurrentDatartDate()`
  - 图表筛选默认时间、计时器组件、同步时间提示与秒表计时链路继续收口
  - 认证 token 过期时间计算收口到统一日期工具，避免局部直接叠加原生时间戳
  - 分享过期时间禁用判断、图表预览区间时间默认值与相对时间计算入口继续统一走 `getDatartNow()`
  - 时间范围计算与区间时间默认值补齐单次 `now` 快照，避免同一轮计算里多次取当前时间产生边界漂移
  - 日期工具内部补齐 `Dayjs` / `Date` 输入的稳定转换语义，避免已有时间对象先字符串化再重解析导致时间语义漂移
  - 看板标签容器默认标签项索引的时间来源统一改走 `getDatartNowMillis()`，避免前端编辑态继续混用原生 `Date.now()`
  - 看板复制图表生成新图表 ID 的毫秒时间来源统一改走 `getDatartNowMillis()`，保持 ID 结构不变，只统一当前时间入口
  - 看板 beta4 迁移里 tab 项默认索引的时间来源统一改走 `getDatartNowMillis()`，避免迁移链路继续保留原生 `Date.now()`
- 看板 beta4 迁移链路局部弱类型继续收口：
  - `beta4utils.ts` 中 `nameConfig`、`padding`、tab `itemMap` 的访问改为按局部显式结构读取
  - 去掉局部 `@ts-ignore`、`as any`、`as any[]`
  - 保持迁移输出结构与转换语义不变
- 看板迁移主流程继续补强：
  - `migrateWidgets.ts` 中 beta4 / beta4_2 / RC0 的局部弱类型入口继续收口，去掉迁移主链上的宽泛 `any` 中转
  - `migrateWidgetConfig.ts` 的 RC1 事件分发改为直接走 `Widget` 类型，不再保留局部 `widget as any`
  - 修正 `migrateWidgets` 主流程未接回 `beta4_2` 结果的问题，并补齐回归测试，确保图表交互配置迁移真正生效
- 看板配置链路继续补强：
  - `migrateBoardConfig.ts` 补齐显式迁移目标类型与 `jsonConfig` 识别，避免看板配置入口继续依赖宽泛对象分支
  - 修正 beta0 阶段 `hasResetControl` 误跟随 `hasQueryControl` 的历史逻辑，保持查询与重置开关各自独立
  - `BoardConfigProvider` 读取移动端间距时改为真实使用 `mSpace` 配置组，并补齐回归测试，确保移动端间距配置真正生效
- widget chart 配置迁移入口继续补强：
  - `migrateWidgetChartConfig.ts` 把历史字符串配置解析收口到显式兼容函数，不再在迁移主链里直接裸 `JSON.parse`
  - RC2 事件分发改为走局部显式迁移目标类型，减少 widget chart 配置入口的宽泛对象透传
  - 补齐字符串配置输入的回归测试，确保旧版字符串配置在迁移后仍能正确得到日期层级字段改写结果
- widget 服务端解析入口继续补强：
  - `migrateWidgets.ts` 中 widget 配置与 relation 配置的字符串解析收口到局部兼容函数，不再在主链里重复裸 `JSON.parse`
  - `parseServerWidget` 与 `convertWidgetRelationsToObj` 的异常路径补齐显式回退语义，保持坏数据不扩散到迁移主流程
  - 补齐无效 JSON 与正常解析的回归测试，确保服务端 widget 解析入口在兼容场景下行为稳定
- StoryConfig 迁移解析入口继续补强：
  - `migrateStoryConfig.ts` 与 `migrateStoryPageConfig.ts` 的字符串解析统一收口到局部兼容函数，去掉入口里的裸 `JSON.parse + console.log`
  - 补齐空字符串、无效 JSON、beta2 版本升级和最新版本归一化测试，确保 StoryConfig 迁移入口行为稳定
- View 详情配置迁移入口继续补强：
  - `migrationViewDetailConfig.ts` 的字符串解析收口到局部兼容函数，补齐无效 JSON 输入的显式回退语义
  - beta2 事件分发改为走局部显式迁移目标类型，减少 view detail 配置入口的宽泛对象透传
  - 补齐无效 JSON 与 `null` 配置回归测试，确保 view detail 配置迁移入口行为稳定
- View model 配置迁移入口继续补强：
  - `migrationViewModelConfig.ts` 的字符串解析收口到局部兼容函数，补齐无效 JSON 与 `null` 输入的显式回退语义
  - beta2 / beta4 事件分发改为走局部显式迁移目标类型与视图类型闭包，减少 view model 配置入口的宽泛对象透传
  - 补齐无效 JSON、`null` 配置与最新版本稳定性回归测试，确保 view model 配置迁移入口行为稳定
- View 基础配置迁移入口继续补强：
  - `migrationViewConfig.ts` 补齐局部显式迁移目标类型与 beta4 任务函数，减少 view 基础配置入口的宽泛对象透传
  - 保持 `type` 缺省时回填 `SQL` 的既有语义不变，继续兼容 `null` / `undefined` 输入
  - 新增 `migrationViewConfig` 回归测试，覆盖空输入、默认类型回填与已有类型稳定性
  - 新增统一 `migrateView` 入口，收口 `view/type + detail config + model` 的重复迁移串联逻辑
  - `ChartDtoHelper`、View 详情加载、Workbench 详情加载、看板 view 转换和控制器取 view 等核心调用点改为复用统一入口，降低迁移顺序分叉风险
  - `transformModelToViewModel` 补齐 `config` / `model` / `columnPermission` 的局部兼容解析，避免同链路里继续裸 `JSON.parse`
- `react-window` 专项审计与运行时包装边界收口：
  - 实际使用面确认仅剩 `VirtualTable -> SchemaTable`
  - 虚拟表格 reset 时机修正为依赖变化即时触发
  - 列宽分摊逻辑补齐除零保护
- `flexlayout-react` / `react-grid-layout` 使用面盘点与布局映射收口：
  - `flexlayout-react` 实际使用面确认集中在图表工作台 `ChartOperationPanel`
  - `ChartOperationPanel` 补齐布局节点尺寸读取兜底，避免布局节点未就绪时直接取空
  - `ChartOperationPanel` 尺寸适配链继续补齐有限数值、非负整数与预留空间扣减保护，避免布局瞬态下向图表容器透传负值或异常尺寸
- `react-grid-layout` 实际使用面确认集中在看板编辑态 `AutoBoardEditor` 与查看态 `AutoBoardCore`
  - 看板布局映射入口补齐 `pRect / mRect` 缺省值归一化，避免不完整布局数据直接传入 RGL
  - 布局归一化继续补齐非法值兜底与断点列数约束，避免异常宽高或负值直接进入 RGL
  - 布局归一化继续补齐整数化与 `x + width` 越界保护，避免浮点布局值或超界坐标直接进入 RGL
  - `package.json` 依赖声明已与当前锁文件和真实运行版本同步到 `flexlayout-react 0.5.21`、`react-grid-layout 1.3.4`
- 前端锁文件根元数据继续收口：
  - `package-lock.json` 根依赖声明重新与 `package.json` 对齐
  - 重点修正 `flexlayout-react`、`react-grid-layout` 与 `@types/react-grid-layout` 的历史声明残留
  - 保持实际安装解析结果不扩散到无关依赖升级
- `react-dev-inspector` 开发态接入移除
- Node 运行时基线明确统一回 `Node 24`
- Maven 对外品牌命名开始从 `datart` 向 `yu-bi` 收口：
  - Maven 模块坐标改为 `yubi` / `yu-bi-*`
  - 安装包、脚本名、Docker 打包输入改为 `yu-bi-server`
  - 保持 `datart.*` Java 包名、配置前缀与稳定内部标识不变
- Maven 对外品牌元数据继续收口：
  - 根 POM 与各服务端模块补齐 `name`、`description` 等对外元数据，统一以 `yu-bi` 对外呈现
  - SCM 与许可证信息指向 `yu-bi` 新仓库
  - 明确保留 `datart.DatartServerApplication` 这类内部稳定入口不动
- 构建与交付层品牌残留继续收口：
  - Docker 镜像运行目录从 `/datart` 调整为 `/yu-bi`
  - 部署文档中的 Docker 挂载路径示例同步更新为 `/yu-bi`
  - Maven 聚合模块中的失效 `datart` 注释依赖片段已清理
- GitHub Actions 主线门禁收口：
  - Node 24 / JDK 21 复合工作流从旧 `dev` 分支触发切换到 `main`
  - 避免默认主线已迁移到 `main` 后，远端 CI 仍未覆盖真实合并入口
- 安装包闭环验证已打通：
  - `yu-bi-server-*.zip` 可正常解压
  - `scripts/check-demo-health.sh` 在真实端口绑定环境中可通过健康检查
  - 当前沙箱内的端口绑定失败属于环境限制，不是安装包链缺陷

### 4.3 近期里程碑提交

- `d1fd4be02` `chore: 移除 react-dev-inspector 开发态接入`
- `9dc21fde6` `docs: 重整现代化改造执行板`
- `5e540c4d5` `chore: 继续收口图表时间值解析入口`
- `b65768725` `chore: 继续收口图表时间序列化入口`
- `10817c1fa` `chore: 收口区间时间值转换入口`
- `203db201d` `chore: 收口日期数组回填入口`
- `9a157b164` `chore: 收口 react-window 运行时包装边界`

## 5. 风险分层

### 5.1 低风险：优先持续推进

| 专题 | 当前判断 | 策略 |
| --- | --- | --- |
| 前端测试层弱类型收口 | 迁移测试和工具测试已完成多批收口，剩余集中在少数大测试文件 | 继续按文件分段推进，优先小范围、可定向验证的用例 |
| 前端交互/弹窗类型收口 | 仍有局部 `Function`、宽泛回调与最小视图结构未声明 | 继续按调用链小批量收口 |
| 时间体系剩余调用点 | 已有统一工具入口，仍有零散调用 | 继续小批量收口 |
| 前端依赖收口 | 仍有少量历史依赖可继续审计 | 按证据逐个清理 |
| Ant Design 历史入口 | 只剩局部残留 | 按调用点逐步消化 |
| 安装健康度维护 | 当前已稳定，但需防止锁文件回退 | 持续关注 Node 24 安装表现 |

### 5.2 中风险：做专项稳定化，不做重写

| 专题 | 当前判断 | 主要风险 |
| --- | --- | --- |
| 富文本兼容层 | `react-quill 2` 已在用，但兼容层仍偏重 | 编辑态、只读态、分享态回归 |
| 时间体系最终收口 | 主链已切到 `dayjs` | 日期控件、格式化、时区回归 |
| `react-window` | 接入面窄，但处在真实运行时链路 | 虚拟表格渲染与滚动行为回归 |
| `flexlayout-react` | 版本偏旧，仍在工作台布局主链 | 布局拖拽、面板状态、样式回归 |
| `react-grid-layout` | 版本偏旧，仍在看板布局主链 | 拖拽缩放、响应式布局回归 |
| Docker / 安装包闭环 | 基线已收口 | 安装包结构、静态资源、启动链验证 |

### 5.3 高风险：暂不直接动

| 专题 | 当前判断 | 原因 |
| --- | --- | --- |
| Shiro 迁移 | 暂不进入实质迁移 | 认证授权影响面大 |
| Calcite 升级或替换 | 暂不进入实质替换 | SQL 解析链耦合深 |
| 数据源 / 脚本深层架构调整 | 暂不直接动 | provider、脚本、方言耦合深 |
| 内部命名与稳定标识重构 | 明确禁止 | 影响面不可控 |

## 6. 当前进行中专题

当前工作区只推进低风险、小范围、可回归的专题，不进入结构性替换。

### 6.1 正在推进

当前累计专题：`前端生产代码低风险类型边界收口（长期低风险分支持续推进）`

本批目标：

- 继续清理生产代码中能用现有 DTO / APIResponse / Axios 类型表达的 `any`
- 优先处理请求工具、下载工具、错误处理和“不消费响应体”的 API 调用
- 保持接口路径、请求参数、错误提示和下载行为不变
- 返回数据会进入业务拼装的链路先标记评估，不硬改

当前累计清单：

- 已完成：迁移测试层局部弱类型收口
- 已完成：工具测试层第一批局部弱类型收口
- 已完成：`overflowFuncs.test.ts`、`internalChartHelper.test.ts`、`FormGenerator` 测试与 `chartHelper.test.ts` 的局部弱类型收口
- 已完成：工具测试层真实 `as any` / 宽泛 `any` 复扫，当前只剩 `expect.any(Function)` 这类断言 matcher
- 已完成：`requestWithHeader<T>` 返回 `[data, headers]` 的显式类型边界，下载文件调用方不再通过 `as any` 解构
- 已完成：`request2` 扩展点、默认错误处理和未授权处理的错误入参从宽泛 `any` 收口到 `unknown` + 局部最小结构
- 已完成：下载任务、模板导出、资源导入导出、看板更新等“不消费响应体”的请求泛型从 `any` / `{}` 收口到 `null`
- 已完成：数据源 schema 同步 thunk 修正为真实返回 `null`，View schema 拉取改为复用 `DatabaseSchema` 显式 payload
- 已完成：`utils/utils.ts` 的 Axios 错误响应体改为 `APIResponse<unknown>`，`fastDeleteArrayElement` 改为泛型数组 helper
- 正在推进：生产工具函数中确认低风险的单点类型债复扫
- 暂缓评估：`useSaveAsViz` 的复制保存链路仍保留 `request2<any>`，因为返回数据会按 `DATACHART / DASHBOARD` 进入不同业务拼装
- 下一批候选：`utils/chartHelper.ts`、`utils/internalChartHelper.ts` 中可隔离、已有测试覆盖的 helper 局部类型收口

当前已落地范围：

- `VariablePage/utils.ts` 继续沉淀变量权限日期值序列化 helper
- `VariablePage/index.tsx` 与 `ViewPage/Main/Properties/Variables.tsx` 改为复用统一 relation 序列化入口
- `date.ts` 继续补齐范围时间格式化与“今天结束前禁用” helper
- `date.ts` 新增标准时间串 helper，继续承接 `TIME_FORMATTER` 的零散重复调用
- `date.ts` 继续补齐标准时间串的可选格式化 helper，减少过滤器与当前时间默认值链路的直接模板耦合
- `SchedulePage/utils.ts`、`ShareLinkModal.tsx`、`DashBoardPage/utils/*`、`ChartTimeSelector/utils.ts` 改为复用统一日期工具
- `SourceDetailPage`、`DateConditionConfiguration`、`timeFilterUtils`、`ChartDataRequestBuilder`、`time.ts` 继续改为复用统一标准时间 helper
- `ShareLinkModal`、`ShareManageModal`、`SubjectForm`、`VariableForm` 的 `destroyOnHidden` 兼容 props 改为直接透传，去掉对象展开配合 `as any`
- `BasicFont`、`JumpToChart`、`JumpToDashboard`、`JumpToUrl`、`ControllerList`、`UrlParamList`、`ChartRelationList`、`BoardRelationList`、`ChartDrillContextMenu`、`CrossFilteringRuleList`、`MemberDetailPage` 的下拉/弹层兼容 props 继续改为直接透传，减少 `popupMatchSelectWidth`、`destroyOnHidden`、`popupRender`、`onOpenChange` 的局部 `as any`
- `ConditionalStyle/add` 与 `ScorecardConditionalStyle/add` 的 `destroyOnHidden` 兼容 props 改为直接透传，继续压缩 modal 兼容层里的对象展开与局部 `as any`
- `SourceDetailPage` 的编辑态配置字符串解析补齐局部安全入口，`ArrayConfig` 的 `columns` 读取改为显式结构访问，开始收口 SourcePage 的局部弱类型与裸解析
- `useGetSourceDbTypeIcon` 的数据源配置字符串解析补齐局部安全入口，继续减少 Source 侧直接裸 `JSON.parse`
- `SourcePage/Sidebar/Recycle` 的树节点标题渲染改为走局部显式节点类型，继续压缩 Source 侧 `as any`
- `VariablePage/utils.ts` 新增变量关系值解析 helper，`ViewPage/Main/Properties/Variables.tsx` 改为复用统一入口，继续减少变量权限链路的局部裸 `JSON.parse`
- `ViewPage/Main/Properties/Resource.tsx` 的资源树图标渲染改为走局部显式节点类型，继续压缩资源树节点上的 `as any`
- `VariablePage/utils.ts` 新增变量默认值解析 helper，`VariableForm.tsx` 与 `VariablePage/index.tsx` 改为复用统一入口，继续减少变量链路的局部裸 `JSON.parse`
- `ViewPage/Main/StructView/components/SelectDataSource.tsx` 的树节点与已选表结构改为走局部显式类型，继续压缩 StructView 数据源选择链路上的 `as any`
- `ViewPage/Main/Editor/SQLEditor.tsx` 的只读提示贡献对象改为走局部显式接口，继续压缩 Monaco 编辑器链路上的单点 `as any`
- `ViewPage/Main/Editor/Toolbar.tsx` 的导航 state 改为走局部显式类型，继续压缩编辑链路中的单点 `as any`
- `useChartInteractions.ts` 与图表预览/分享/看板调用方改为走显式弹窗与详情参数类型，继续压缩交互链路上的 `Function` 与局部 `as any`
- `VizPage/hooks/useDisplayJumpVizDialog.tsx`、`useDisplayViewDetail.tsx` 改为直接复用显式参数类型与 `openStateModal` 返回值，继续收口展示层局部 `Function`
- `ViewPage/Main/Properties/DataModelTree.tsx` 的计算字段校验异常提示改为显式字符串化，继续压缩属性链路里的单点 `as any`
- `useDisplayViewDetail.tsx` 的数据集行、列与授权令牌类型改为走显式最小结构，避免详情表格继续透传宽泛 `any`
- `useChartInteractions.ts` 的 drill/view detail/cross filtering 入参改为复用 `IChartDrillOption`、`ChartConfig`、`SelectedItem`、最小 view 结构与真实交互枚举，避免预览/分享/看板继续混用未声明对象
- `useStateModal.tsx` 的公共弹窗 API 补齐 `content`、`maskClosable`、`cancelButtonProps`、缓存回调等真实调用面，减少各处 `as Function` 绕过
- `useFieldActionModal.tsx`、`CheckboxModal.tsx`、`GroupLayout.tsx`、`FilterTypeSection.tsx`、`ChartDataViewPanel.tsx`、`ChartDraggableTargetContainer.tsx`、`DataModelTree.tsx` 改为直接复用显式弹窗返回值，继续压缩工作台与属性链路的局部 `Function`
- `ChartDataViewPanel.tsx` 与 `DataModelTree.tsx` 的计算字段树数据改为走显式 `TreeDataNode` 结构，避免树节点 key/children 继续保留宽泛对象
- `useToggle.ts` 与 `useComputedState.ts` 的通用 hook 返回值与依赖类型改为显式声明，保持现有“切换 / 显式设值 / 延迟计算”语义不变，减少公共 hook 的 `Function` 与宽泛返回值
- `ChartDrillContext.ts`、`ChartWorkbench.tsx` 与 `ChartEditor.tsx` 的 `onDateLevelChange` 改为复用显式 `ChartConfigPayloadType`，继续收口时间层级切换链路中的宽泛回调
- `ChartPreviewBoard.tsx`、`ChartPreviewBoardForShare.tsx`、`DataChartWidgetCore.tsx` 的时间层级切换与图表鼠标事件入口改为复用显式 `ChartConfigPayloadType`、`ChartMouseEventParams`，继续压缩预览/分享/看板链路中的局部 `any`
- `ChartEditor.tsx`、`ChartPreviewBoard.tsx`、`ChartPreviewBoardForShare.tsx` 的 `registerMouseEvents` 回调入口改为走显式 `ChartMouseEventParams`，继续压缩工作台/预览/分享链路里的未声明事件对象
- `ChartSelectionManager.ts` 的点击回调数组与事件透传结构改为走显式 `ChartMouseEventParams` 兼容形态，并保留既有 `componentIndex / dataIndex / selectedItems` 语义不变
- `ChartIFrameEventBroker.ts` 与 `useVisibiltyStyle.ts` 的通用 runtime 容器回调改为显式函数签名，继续压缩 iframe 生命周期代理与可见性样式 helper 中的 `Function`
- `WaterfallChart.tsx` 的选择事件数据补齐最小 `name / value / rowData` 结构，避免运行时图表选择链路继续依赖未声明的裸对象拼装
- `ChartIFrameContainerDispatcher.tsx` 的容器渲染缓存与 metadata 元组改为显式 `ReactNode` / `ChartDataSetDTO` / `ChartConfig` 结构，继续压缩 iframe 容器分发链路中的 `Function` 与未声明元组
- `ChartDraggableElement.tsx`、`ChartDraggableElementField.tsx`、`ChartDraggableElementHierarchy.tsx`、`ChartDraggableSourceContainer.tsx`、`ChartDraggableSourceGroupContainer.tsx` 的拖拽内容渲染、日期层级 children 与操作菜单回调改为显式类型，继续收口工作台拖拽与字段替换链路中的局部 `Function` / `any`
- `ChartIFrameContainer.tsx`、`ChartIFrameLifecycleAdapter.tsx`、`ReactLifecycleAdapter.ts` 的 dataset / widget 配置 / 容器尺寸 / React root 渲染入口改为显式兼容类型，继续压缩 iframe 生命周期容器里的局部 `any`
- `ChartIFrameResourceLoader.ts`、`ChartIFrameEventBroker.ts`、`ReactChart.ts`、`ChartDataConfigSectionReplaceMenu.tsx` 的文档对象、事件 broker 入参、React 图表挂载入口和字段替换配置改为显式兼容类型，继续收口 runtime helper 与工作台替换菜单中的局部 `any`
- `ChartDataRequest.ts`、`ChartDrillOption.ts`、`Chart.ts` 与对应实现/测试的 `limit`、drill filter 数据结构和图表初始化入参改为显式兼容类型，继续收口请求构建与 drill 运行链路中的局部 `any`
- `useI18NPrefix.ts` 与 `ChartLifecycleBroker.ts` 的运行时翻译函数签名改为显式 `I18NTranslate` 类型，继续收口 iframe 生命周期上下文中的局部 `any`
- `ChartDataSet.ts` 与 `ChartDataRequestBuilder.ts` 的列索引兜底、数组构造和过滤值归一化改为显式兼容类型，并同步收口对应测试中的局部 `any`
- `PluginChartLoader.ts` 与 `ChartManager`、`Chart` 的模型层测试继续收口插件加载返回值、私有加载入口访问与生命周期入参中的局部 `any`
- `ChartDataRequestBuilder.test.ts` 继续按测试 helper 收口 dataView、字段、section、过滤器与排序场景中的局部 `any`，补齐测试数据与真实类型约束的一致性
- `migrateWidgetChartConfig.test.ts`、`alpha3.test.ts` 与 `migrateWidgetConfig.test.ts` 继续按最小测试结构收口 widget 配置、图表迁移配置与 tab 自定义配置中的局部 `any`，保持迁移逻辑与输出语义不变
- `MigrationEventDispatcher.test.ts` 继续按显式迁移模型收口事件输入、回退值和任务回调中的局部 `any` / `undefined as any`，保持事件分发与异常回退语义不变
- `migrateChartConfig.test.ts`、`migrateBoardConfig.test.ts`、`index.test.ts` 与 `migrateStoryPageConfig.test.ts` 继续按正式 DTO / 配置类型收口迁移测试中的局部 `any`，保持版本归一化、默认值补齐与异常回退语义不变
- `migrateWidgets.test.ts` 继续按 `WidgetBeta3` / `Widget` / `ServerWidget` helper 收口 widget 迁移主流程测试中的局部 `any`，仅保留旧 `filter` 兼容输入场景所需的单点显式兼容强转
- `time.test.ts`、`chartDtoHelper.test.ts` 与 `ChartEventListenerHelper.test.ts` 继续按请求过滤器、图表 DTO 与鼠标事件最小类型收口工具测试中的局部 `any`，保持空输入保护、DTO 合并与事件分发语义不变
- `number.test.ts` 与 `BasicDoubleYChart/utils.test.ts` 继续按数值输入、图表字段与数据集 helper 收口测试中的局部 `any`，保持精度计算、安全数值转换和双 Y 轴区间计算语义不变
- `overflowFuncs.test.ts` 通过 ECharts options、漏斗图配置 helper 和集中 legacy helper 收口散落 `as any`，保持轴标签溢出判断与漏斗图 top 位置兼容语义不变
- `FormGenerator/__tests__/utils.test.tsx` 与 `BasicFont.test.tsx` 改为按真实 translator 与字体 options 类型表达测试数据，去掉测试内宽泛 translator / fontFamilies 强转
- `ChartConfig.ts` 补齐 `FontFamilyOption`，让 `BasicFont` 既有 `{ name, value }` 字体项形态进入正式类型表达，保持运行时行为不变
- `internalChartHelper.test.ts` 改用 `ChartDataViewFieldCategory.Field`、表驱动类型和集中 legacy config helper，收口迁移/合并测试中的字段类别与历史配置强转
- `chartHelper.test.ts` 补充 style config、data field、legacy data config、runtime date level 测试 helper，收口 requirement、dataset、tooltip、drill、style value 与 runtime date level 配置用例中的局部弱类型
- `number.ts` 的数值格式化入口改为 `unknown` 宽输入加显式数值转换 helper，保留非数值原样返回语义，减少生产工具函数里的宽泛 `any`
- `mutation.ts` 的 Immer draft 更新桥接改为 `Draft<T>`，去掉泛型数组与递归更新链路中的局部 `as any`
- `time.ts` 的 Dayjs 单位兼容桥接从 `any` 收窄到官方单位类型断言，保持 quarter 插件运行时语义不变
- `request.ts` 的请求扩展点和 `requestWithHeader<T>` 响应头返回链路改为显式类型，减少请求工具层结构伪装与 `any` 中转
- `fetch.ts` 的下载任务、分享链接、文件保存与名称校验入口补齐最小类型，保持 Blob 下载行为不变
- `ChartWorkbenchPage`、`Board`、`BoardEditor`、`VizPage`、`ResourceMigrationPage`、`SourcePage`、`ViewPage` 中可确认响应体不消费或已有 DTO 的请求泛型完成局部收口

当前验证计划：

- `npm run checkTs`
- `npm run test:ci -- src/app/utils/__tests__/fetch.test.ts src/utils/__tests__/utils.test.ts`
- `rg -n "request2<any>|request2<any\\[\\]>|request2<\\{\\}>" frontend/src/app frontend/src/utils -g '*.ts' -g '*.tsx'`

### 6.2 最近已完成

- View 消费链解析边界收口
- 前端配置字符串解析入口收口
- 前端历史类型入口收口补强
- 控制器配置态滑块 setter 值类型边界收口
- 控制器配置态相对时间 setter 值类型边界收口
- 控制器配置态树默认值选择链路类型收口
- 控制器关联字段值类型过滤链收口
- 控制器配置默认值数组边界收口
- 富文本纯文本 JSON 误判保护补强
- 时间体系剩余调用点跟进收口
- 迁移测试层局部弱类型继续收口
- 工具与表单测试层局部弱类型继续收口
- `chartHelper.test.ts` 局部弱类型收口
- `number.ts` 数值工具输入边界收口
- `mutation.ts` Immer draft 更新边界收口
- `time.ts` Dayjs 单位兼容桥接类型收口
- 请求工具与下载工具响应边界收口
- API thunk 中“不消费响应体”调用的泛型收口
- 通用错误处理与数组 helper 类型边界收口

## 7. 下一阶段执行顺序

按这个顺序推进，避免专题扩散：

1. 继续复扫生产工具函数中的低风险单点类型债，只处理不改变运行时语义的局部收口
2. 优先从 `chartHelper.ts` / `internalChartHelper.ts` 中挑已有测试覆盖、可隔离的 helper 继续收口
3. `useSaveAsViz`、schema/复制保存这类返回数据会进入业务拼装的链路单独评估，不混入低风险批次
4. 时间体系剩余调用点继续收口
5. 前端依赖收口与历史入口审计继续逐个推进
6. 安装健康度与锁文件一致性持续检查

## 8. 每轮固定门禁

### 8.1 范围规则

- 一个提交只处理一个专题
- 低风险与高风险不混提
- 当前专题之外的改动不顺手带上

### 8.2 验证规则

前端专题至少执行：

- `npm run checkTs`
- `npm run build:all`
- `npm run test:ci -- --silent`

后端专题至少执行：

- `mvn -pl server -am -DskipTests package`
- 必要时补模块级测试

工程化专题至少检查：

- `engines` / `.nvmrc` / CI / Maven Enforcer 是否一致
- CI 是否覆盖目标运行时
- Dockerfile / 安装包链是否仍匹配当前基线

### 8.3 提交规则

- 默认直接在当前专题分支提交
- 不直接改 `main`
- 同一专题尽量攒成更完整的一批再提交，避免高频触发整套回归
