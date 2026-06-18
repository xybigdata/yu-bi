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
- 前端低风险改造优先复用当前累计分支，不因单个小专题频繁创建新分支
- `main` 推送需要完整门禁，只有累计到值得回归的批量后才执行完整门禁、`--no-ff` 合并并推送主线
- 高风险或跨域专题仍单独建分支，不在文档中写死分支名
- 默认自动 `git add`、`git commit --no-verify`

### 2.2 不直接触碰的高风险标识

- Java 包名 `datart.*`
- 配置前缀 `datart.*`
- `DATART_*` 等内部技术符号
- 数据迁移相关稳定常量、后缀、内部标识

### 2.3 工作区边界

- 本地运行目录 `.tmp/`、`logs/` 已加入 `.gitignore`
- 一个提交只处理一个专题
- 只提交与当前专题直接相关的文件
- 尽量按专题攒成一批再提交，避免高频提交带来重复回归成本

### 2.4 目标核对节奏

- 每轮开始先核对长期目标：`JDK 21`、`Node 24`、兼容优先、稳定版优先、`main` 不直接开发
- 每批提交前核对短期目标：当前改动是否仍属于当前专题，是否低风险、可验证、可回退
- 如果候选点开始牵涉共享协议、运行时行为或跨域依赖，拆成中高风险专项推进，并补足定向测试或完整门禁证据
- 每次阶段性提交或推送后，在本文档中保留对后续判断有用的执行记录
- 前端低风险日常推进使用轻量门禁：优先 `npm run checkTs`，必要时补相关单测；中风险运行时链路必须补定向测试或更强门禁；只有准备阶段性合入 `main` 前才执行完整前端门禁 `npm run test:ci`、`npm run lint:css`、`npm run lint:style`

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
- 富文本运行时加载链路继续稳定化：
  - `RichTextEditor` 运行时懒加载失败时补齐显式错误记录
  - `loadRichTextEditorRuntime` 失败后清空缓存 Promise，避免一次加载失败后永久无法重试
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
  - 虚拟表格运行时加载失败补齐显式兜底，列宽扣减滚动条时补齐非负保护，并补充 `VirtualTable` 定向测试
- `flexlayout-react` / `react-grid-layout` 使用面盘点与布局映射收口：
  - `flexlayout-react` 实际使用面确认集中在图表工作台 `ChartOperationPanel`
  - `ChartOperationPanel` 补齐布局节点尺寸读取兜底，避免布局节点未就绪时直接取空
  - `ChartOperationPanel` 尺寸适配链继续补齐有限数值、非负整数与预留空间扣减保护，避免布局瞬态下向图表容器透传负值或异常尺寸
  - `ChartOperationPanel` 布局工厂补齐组件类型守卫，未知 FlexLayout component 不再进入已知渲染分支
- `react-grid-layout` 实际使用面确认集中在看板编辑态 `AutoBoardEditor` 与查看态 `AutoBoardCore`
  - 看板布局映射入口补齐 `pRect / mRect` 缺省值归一化，避免不完整布局数据直接传入 RGL
  - 布局归一化继续补齐非法值兜底与断点列数约束，避免异常宽高或负值直接进入 RGL
  - 布局归一化继续补齐整数化与 `x + width` 越界保护，避免浮点布局值或超界坐标直接进入 RGL
  - 布局归一化补齐 `Infinity` 与非法列数兜底，避免异常布局值穿透到 RGL
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
- 前端图表与看板局部类型边界继续收口：
  - 透视表 S2 语言设置改为显式 `LangType` 映射，去掉局部 `as any`
  - 透视表 hover / selected 回调对齐 S2 包根公开类型，保持选中读取逻辑不变
  - Dashboard widget action 的兼容导航与联动参数改为真实类型，避免继续用宽泛 `any`
  - `DataChartConfig.computedFields` 与相关 reducer / provider 参数改为 `ChartDataViewMeta[]`
  - `WidgetMeta.icon` 收口为字符串，匹配所有 widget 注册入口
  - 基础柱状图 legend series 与基础表格列宽计算补齐局部结构类型，减少无约束 `any`
- 前端交互配置表单局部类型边界继续收口：
  - DrillThrough / CrossFiltering 面板的规则更新函数改为按规则字段类型约束，不再透传宽泛 `value: any`
  - JumpToChart / JumpToDashboard / JumpToUrl 的编辑态规则与自定义关系数组补齐真实类型
  - ViewDetail 自定义字段从 `any[]` 收口为 `string[]`
  - `internalChartHelper` 读取自定义关系时补齐空数组兜底，保持未配置场景的既有空结果语义
  - 本批不改 FormGenerator 全局 `ItemLayoutProps` 和配置 JSON 协议宽口
- 前端图表 option 局部类型边界继续收口：
  - 透视表缓存的 `updateOptions` 改为复用已有 `AndvS2Config`
  - 散点图 metric / size series 的混合值数组补齐真实 `string | number | undefined` 边界，去掉局部 `as any`
  - 轮廓地图 option 的 `series` 改为地图 series 与散点 series 联合类型，去掉 series 拼接处的局部 `as any`
  - 本批不迁移 ECharts 实例、地图注册入参、tooltip params 和 rowData 宽口
- 前端历史弱类型注释清理：
  - 删除看板 widget 工具、控制器核心、容器内容类型和查询/重置按钮配置里的失效注释代码
  - 清理注释中的历史 `as any` / `any[]` 噪音，降低后续低风险扫描误判
  - 本批不改变运行时代码，不改 widget content 协议宽口
- 前端翻译函数局部类型继续收口：
  - FormGenerator 的 `translate` 参数复用统一 `I18NTranslateOptions`
  - Waterfall 图表内部翻译参数复用统一 `I18NTranslate`，并补齐可选翻译函数缺省回退
  - `ChartFilterCondition.value` 经 `checkTs` 验证仍属于运行时宽口，继续暂缓不纳入低风险批次
- 前端图表局部类型边界继续收口：
  - 折线图与柱状图 `dataZoom` 局部缓存补齐显式配置类型
  - Scorecard 的 React 点击事件回调补齐真实事件类型
  - 漏斗图、散点图、词云图、基础饼图与轮廓地图的样式扩展字段从 `any` 收口为 `Record<string, unknown>`
  - 本批继续暂缓 ECharts 实例、tooltip params、地图注册入参、rowData 与图表公共协议宽口
- 前端局部 UI 与适配层类型边界继续收口：
  - 基础饼图 value 数组补齐真实值类型边界
  - ColorPicker 弹层回调保持上层字符串颜色 API，底层取消信号不再向上扩散
  - ChartTypeSelector 的类型切换回调收口到 `ChartPresentType`
  - Split 适配层补齐拖拽尺寸与 gutter 回调参数类型，并补齐重建分支的 HTMLElement 兜底
  - 本批不触碰 widget content、Dashboard view/sampleData、表格交互事件和第三方实例宽口
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
| 前端公开类型入口 | Ant Design / rc 历史类型入口已清零，Monaco 静态类型导入已切到包根入口 | 后续防止回退到组件内部目录或运行时深路径类型入口 |
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

当前工作区按“可验证、可回退、兼容优先”推进；低风险继续累计，中风险拆专项稳定化，高风险只在边界清晰且有验证证据时进入。

### 6.1 正在推进

当前累计专题：`前端局部类型边界收口`

本批目标：

- 优先处理图表组件内部缓存、局部事件回调、样式透传对象等低风险类型边界
- 同步处理调用链清楚的局部 UI 组件、工具函数和第三方适配层类型边界
- 只收口“声明比真实用法更宽”的局部类型，不改图表 option 输出结构
- 继续暂缓 ECharts 实例、tooltip params、地图注册入参、rowData 与图表公共协议宽口
- 不调整公共协议、内部稳定标识和业务配置结构

当前累计清单：

- 已完成：`ChartSelectModal` 去掉 `rc-tree/lib/interface` 的 `Key` 深路径导入，改用 React 公开 `Key` 类型
- 已完成：`UnControlledTableHeaderPanel` 去掉 `antd/es/table/interface` 的 `TableRowSelection` 深路径导入，改为从 `TableProps<T>['rowSelection']` 反推公开类型
- 已完成：`HiddenUploader` 去掉 `antd/es/upload/Upload` 的 `UploadRef` 深路径导入，改为从包根 `Upload` 公开组件反推 ref 类型
- 已完成：Monaco 编辑器封装、SQL 编辑器、MockData 编辑器和 View 编辑上下文的静态类型导入从 `monaco-editor/esm/vs/editor/editor.api` 切到 `monaco-editor` 包根入口
- 已完成：前端源码复扫已无 `antd/es`、`antd/lib`、`rc-*/es`、`rc-*/lib` 这类历史深路径类型入口；Monaco 剩余 `esm` 路径仅保留在运行时懒加载与语言贡献加载入口
- 已完成：看板与图表 MockData 面板的样例数据状态、编辑器回调和导出参数补齐 `ChartDataSetDTO` / 行数组 / 模板导出 payload 局部类型，去掉面板链路里的宽泛 `any` 与原地修改
- 已完成：Workbench、图表预览、分享预览和看板 widget 数据请求的排序、分页、执行 token 参数补齐 `ChartDataRequest` / `ChartDatasetPageInfo` / `ExecuteToken` 类型边界，去掉请求构造链路里的局部 `as any` 与 `getState() as any`
- 已完成：Share/Viz 预览数据集 fulfilled payload 补齐 `ChartDataSetDTO` 边界，computed fields 更新 payload 对齐 `ChartDataViewMeta[]`，去掉分享预览 dataset 的局部 `as any`
- 已完成：ChartOperationPanel 的配置更新回调复用 `ChartConfigPayloadType`，FlexLayout factory 节点和 DnD backend props 补齐公开类型
- 已完成：散点 symbol size 工具函数与轮廓地图散点 series 的 value 参数对齐真实数组边界，避免继续保留未声明回调参数
- 已完成：保存到看板链路的 `dashboardId / dashboardType` 回调补齐 `BoardType` 边界，`SaveToDashboard` 的 Tree 选择事件改为 antd 公开事件类型加本地节点守卫，保持路由 state 与保存行为不变
- 已完成：`ChartEventListenerHelper` 的分页排序、钻取、富文本上下文和选中事件回调补齐显式 payload 类型；表格排序列对齐请求层 `string[]` 边界，选中事件默认回传空数组，保持事件触发条件不变
- 已完成：字段 Action 菜单链路补齐 action、字段类型、排序方向、聚合值和颜色开关回调的局部类型边界，保留菜单过滤、排序默认值、颜色启停和配置更新语义不变
- 已完成：交互跳转 Hook、拖拽预览、搜索列表、聚合开关、通用 ModalForm 与 Gauge formatter 补齐局部参数边界；无效跳转规则显式跳过，避免 undefined 目标继续进入跳转链路
- 已完成：Workbench 数据集 thunk 与 reducer 补齐 `ChartDataSetDTO` 和 reject payload 最小结构，去掉 dataset fulfilled / rejected 链路里的局部 `as any`；配置 payload 的动态 `value` 仍按协议宽口暂缓
- 已完成：BasicRichText MarkdownModule 的 Delta op 与 Quill line 补齐最小局部类型，RichTextPluginLoader 的 calcfield/tag/custom color 数据入口去掉局部 `any`，保持 Quill DOM 数据写读行为不变
- 已完成：BasicFunnelChart 的数据行与指标排序比较改为局部数值读取 helper，去掉 `getCell(...) as any` 的数值比较中转，保持原 Number 转换语义不变
- 已完成：`media.ts` 的 styled-components 媒体查询 helper 补齐显式模板桥类型，去掉模板生成链路里的直接 `any`，并用 media 单测确认 CSS 输出不变
- 已完成：`Avatar`、`ListTitle` 与 `AddButton` 的局部 UI 回调和状态类型收口，复用 antd 公开 props 与菜单点击参数类型，保留按钮点击无参和菜单点击带参两种调用方式
- 已完成：`ThemeProvider` 测试里的主题变量与 store state 断言去掉局部 `any`，改用 `DefaultTheme` 与 `RootState`
- 已完成：`Split` 组件的 split.js 子节点收集改为显式 `HTMLElement[]` 过滤，内部 gutter 标记用局部 DOM 扩展类型承接，去掉 ref 和 gutter 判断里的宽泛强转
- 已完成：`SplitPane` 的尺寸 state patch、鼠标转触摸事件和默认尺寸计算补齐局部显式类型，去掉 `setState<any>`，保留 pane size 允许字符串、数字或空值的既有语义
- 已完成：Redux 动态 reducer 注入工厂参数改为 `unknown` 入口，继续由 `checkStore` 做运行时 shape 校验，非法 store 测试不再需要 `as any`
- 已完成：`DragSortEditTable` 的表格 props、编辑表单上下文和拖拽行 props 改为基于 `RelationFilterValue` 的显式类型，保留现有可编辑拖拽行为不变
- 已完成：`BasicSelector` 的动态选项结果收口为局部显式选项类型，保留标量选项与 `label/value` 对象选项两种兼容形态
- 已完成：`frontend/.husky/pre-commit` 改为 staged 范围检查，TS / JS 文件不再每次触发全量 `lint:style`
- 已完成：`ChartDtoHelper.parseChartConfig` 只接受对象形态配置，非对象 JSON 按空配置处理，并避免非对象配置先进入图表配置迁移器
- 已完成：`internalChartHelper.transformToHierarchyModel` 的 View model 解析只接受对象形态，非法 JSON 或非对象 JSON 按空模型处理，并补齐回归测试
- 已完成：`useGetSourceDbTypeIcon` 的 JDBC 数据源配置解析补齐异常兜底，坏 JSON 或非对象 JSON 回退到默认图标，不再打断 Source 树图标渲染
- 已完成：变量默认值与行权限值解析只接受数组形态，非法 JSON 或非数组 JSON 按空值处理，避免坏数据打断变量表单与权限页
- 已完成：View 详情消费链的 `config` / `model` 解析只接受对象形态，列权限解析只接受数组形态，非对象或非数组 JSON 按空配置处理
- 已完成：STRUCT 视图字段路径解析补齐安全入口，坏 JSON 字段名不再打断层级模型路径补齐
- 已完成：STRUCT 视图脚本列配置解析收口到 `all` 或数组形态，避免重复裸 `JSON.parse` 和异常列配置扩散
- 已完成：STRUCT 视图请求列构造补齐显式 `alias / column` 类型，去掉请求列链路里的宽泛数组中转
- 已完成：查询结果转模型入口补齐 STRUCT 列路径映射类型，去掉字段名中转里的局部 `any`
- 已完成：View tree 节点构造补齐显式 `value: string[]` 节点类型，去掉节点构造返回里的局部 `as any`
- 已完成：Source 编辑态配置字符串解析补齐异常兜底，坏 JSON 或非对象配置按空配置回退，不再打断详情页回填
- 已完成：SQL warning 提 issue 参数改为区分 GitHub / Gitee 的显式结构，去掉 issue 参数拼装里的局部 `any`
- 已完成：View tab 右键菜单事件改为复用 antd 公开 `MenuProps['onClick']` 参数类型，去掉菜单事件入口的局部 `any`
- 已完成：View 资源树图标、排序菜单和回收站标题渲染改为显式 Tree / Menu 类型边界，去掉相关局部 `any`
- 已完成：StructView 数据源选择链路的树图标、表选择、列选择、弹层开关和回调数据补齐显式类型，减少结构视图配置链路的宽泛入参
- 已完成：StructView 主表与 join 表变更入口补齐局部数据结构守卫，错误提示捕获改为 `unknown` + 最小消息读取
- 已完成：StructView join 条件列选择链路补齐列路径、左右条件、树节点和 TreeSelect 兼容桥类型，去掉 `SelectJoinColumns` 内部局部 `any`
- 已完成：View 运行 SQL thunk 的早退返回、变量默认值解析、STRUCT 请求列和 Monaco completion provider 回调补齐显式边界，去掉对应 `as any` 与裸 `JSON.parse`
- 已完成：`useStateModal` 的缓存回调参数从 `any[]` 收口到 `unknown[]`，OK 回调改为不规定具体入参形态，保留各调用方现有参数兼容面
- 已完成：`QueryResult.rows` 从 `any[][]` 收口为查询结果标量二维数组，和后端 `Dataframe.rows: List<List<Object>>` 的 JSON 标量返回边界对齐，并补齐预览数据源转换测试
- 已完成：查询结果预览 `dataSource` 从 `object[]` 收口为 `QueryResultDataSourceRow[]`，View 预览表与数据源 schema 预览链路统一使用查询结果标量行，避免预览结果继续扩散宽泛对象边界
- 已完成：查询结果消费入口补齐 `isQueryResult` 结构守卫，View SQL 执行与数据源 schema 预览只在 `columns / rows` 结构合法时进入转换链路，并补齐空结果集仍保留列模型的回归测试
- 已完成：迁移测试层局部弱类型收口
- 已完成：工具测试层第一批局部弱类型收口
- 已完成：`overflowFuncs.test.ts`、`internalChartHelper.test.ts`、`FormGenerator` 测试与 `chartHelper.test.ts` 的局部弱类型收口
- 已完成：工具测试层真实 `as any` / 宽泛 `any` 复扫，当前只剩 `expect.any(Function)` 这类断言 matcher
- 已完成：`requestWithHeader<T>` 返回 `[data, headers]` 的显式类型边界，下载文件调用方不再通过 `as any` 解构
- 已完成：`request2` 扩展点、默认错误处理和未授权处理的错误入参从宽泛 `any` 收口到 `unknown` + 局部最小结构
- 已完成：下载任务、模板导出、资源导入导出、看板更新等“不消费响应体”的请求泛型从 `any` / `{}` 收口到 `null`
- 已完成：数据源 schema 同步 thunk 修正为真实返回 `null`，View schema 拉取改为复用 `DatabaseSchema` 显式 payload
- 已完成：`utils/utils.ts` 的 Axios 错误响应体改为 `APIResponse<unknown>`，`fastDeleteArrayElement` 改为泛型数组 helper
- 已完成：`chartHelper.ts` 中对象数组转换、runtime date level 临时字段和 computed fields 合并链路的局部类型收口
- 已完成：`ChartDataRequestBuilder` 的 function columns 构造补齐显式返回类型，保留空字符串 snippet 的既有兼容语义
- 已完成：`internalChartHelper.ts` 的样式值合并入口从宽泛 `any` 收口到 `unknown`，空目标 rows 兼容分支用局部类型承接
- 已完成：`utils.ts` 的 `listToTree` 返回结构、`filterListOrTree` 叶子判断和 `getInsertedNodeIndex` 输入边界完成局部类型收口，并补齐对应工具测试
- 已完成：`modelListFormsTreeByTableName` 改为基于 `ChartDataViewMeta` 的显式输入输出结构，保留 `analysisPage / viewPage` 差异字段，并补齐分组排序测试
- 已完成：`ChartVariableParams` 统一图表请求变量参数类型，看板查看态、编辑态、图表预览态和请求构造器统一使用 `Record<string, string[]>`
- 已完成：图表交互变量默认值解析补齐兼容保护，仅数组默认值进入变量参数，坏 JSON 或非数组输入不再打断交互链路
- 已完成：看板过滤器去重 helper 改为泛型最小结构，保留同列后者覆盖前者的既有语义
- 已完成：看板表格分页/排序事件入口补齐分页值与排序值的独立窄化，`getDataOption.sorters` 与 `ChartDataRequest['orders']` 对齐
- 已完成：看板联动点击过滤器值转换改为数组保护与字符串化，去掉局部 `(v as any)?.map` 中转
- 已完成：控制器区间数值默认值校验改为显式区间值类型，补齐字符串数值转换和空值保护，并增加 validator 回归测试
- 已完成：看板 widget 树选项转换 helper 补齐列表节点与树节点类型，去掉树构造链路里的 `any[]` / 宽泛中转，并增加路径转树测试
- 已完成：看板编辑态图表数据请求错误处理改为复用 `getErrorMessage`，去掉局部 `error as any`
- 已完成：看板编辑页导航 state 补齐最小结构类型，去掉 `navigate.location.state as any`
- 已完成：看板配置面板与 widget 配置面板的 `Collapse.items` 改为复用 antd 公开 `CollapseProps`，去掉兼容层 `as any`
- 已完成：TabWidget 的标签位置配置改为复用 antd `TabsProps['tabPosition']`，并对历史异常位置值补齐默认回退
- 已完成：看板 widget 工具、图表选择弹窗、自定义选项表格、添加控制器菜单与图层树节点的局部类型边界收口，保留运行时行为不变
- 已完成：Chart Workbench 样式配置面板、图表图标渲染、分类条件拖拽表格、排序拖拽项、筛选配置交互与控制器文本/数值 setter 的局部类型边界收口，保留交互与配置语义不变
- 已完成：根目录 `.gitignore` 忽略 `.tmp/` 与 `logs/` 本地运行目录，避免后续误提交
- 已完成：`chartHelper.ts` 的样式取值、tooltip 行数据、额外 series 行数据、轴标签溢出 option 访问和选中样式对象补齐局部显式类型，去掉对应 `any` / `@ts-ignore`
- 已完成：`internalChartHelper.ts` 的跳转与联动过滤器返回值收口为显式字段值数组映射，保留调用方追加 URL 标记与联动过滤器语义不变
- 已完成：`urlSearchTransfer.ts` 的 URL 参数解析与序列化补齐显式参数值类型，并新增单测锁住重复参数、数组参数和标量参数行为
- 已完成：`internalChartHelper.ts` 的点击行数据输入收口为最小交互行数据类型，过滤器 values 显式字符串化后进入 `PendingChartDataRequestFilter`，并补齐点击过滤器回归测试
- 已完成：`useStateModal.tsx` 的内部缓存状态从 `any[]` 收口为 `unknown[]`，保留公开回调参数兼容面不变
- 已完成：`useGetVizIcon.tsx` 的可视化图标参数与渲染组件 props 补齐显式局部类型，去掉未使用的宽泛索引签名
- 已完成：`rejectedErrorHandlerMiddleware.ts` 的 rejected action 消息读取补齐 `unknown` 入口与局部类型守卫，去掉错误处理链路里的 `any`
- 已完成：Redux 动态 reducer 注入器补齐 HOC props、key 和 action 类型边界，保留不同 slice reducer 动态注入的运行时兼容面
- 已完成：`loadable.tsx` 改为通过重载保留懒加载组件 props 类型，默认导出和 selector 导出两类调用保持现有行为不变
- 已完成：`object.ts` 的二元数组、空值判断、浅合并、集合复制、对象取值、字符串转换、函数/Promise/树模型判断等 helper 改为显式 `unknown` / 泛型边界，保留原判断语义不变
- 已完成：`debugger.ts` 的性能测量入口改为泛型回调，保留同步和异步返回值语义不变
- 已完成：`utils.ts` 的通用错误处理入口改为 `unknown` + 最小错误结构守卫，并补齐错误消息提取与 reject 回归测试
- 已完成：Redux reducer 测试改为使用真实 `theme` 注入 key 与 `UnknownAction`，去掉测试里的宽泛 reducer 强转
- 已完成：`checkStore.ts` 的 Redux 动态注入 store 校验入口改为 `unknown`，继续由运行时 shape 校验兜底
- 已完成：`utils.ts` 的 className 合并与阻止冒泡 helper 补齐显式入参类型，保持 className 输出和事件行为不变
- 已完成：`internalChartHelper.ts` 的 view config 解析入口补齐对象守卫，非对象 JSON 输入按空配置处理，并补齐回归测试
- 已完成：`chartHelper.ts` 的轴标签 interval 判断和 meta 路径查找 helper 补齐显式入参与返回类型，保持旧筛选值兼容面不变
- 已完成：`fetch.ts` 的计算字段校验、可用函数列表、下载、插件加载、分享任务与图表数据请求 helper 补齐参数和返回类型，保持请求结构不变
- 正在推进：FormGenerator 控件配置透传兼容边界收口
- 已完成：`frontend/.husky/pre-push` 分层门禁，专题分支默认只跑轻量 TypeScript 门禁，`main` 保持完整前端门禁
- 已完成：`BasicCheckbox` 不再把 `hideLabel`、`needRefresh` 透传给 antd Checkbox，保留刷新回调语义不变
- 已完成：`BasicFont`、`BasicColorSelector`、`BasicLine`、`Background`、`WidgetBorder` 的 `ColorPickerPopover` 透传改为白名单提取，避免 `key` 与父级配置对象继续进入 Popover 链路
- 已完成：`BasicSwitch`、`BasicRadio`、`BasicInput`、`BasicInputNumber` 改为剥离 FormGenerator 内部 options 后再下传给 antd 控件，保留控件可识别配置透传
- 已完成：`BasicFontWeight`、`BasicFontStyle`、`BasicFontFamilySelector`、`BasicFontSizeSelector`、`BasicInputPercentage`、`BasicSlider`、`BasicSelector` 继续显式化 antd 控件 props，避免行级 `key / label / default / options` 元数据继续随 `rest` 下传
- 已完成：`BasicCheckbox`、`BasicSwitch`、`BasicRadio`、`BasicInput`、`BasicInputNumber` 的行级 `rest` 下传继续收口为显式 `value / checked / disabled / defaultValue` 等真实控件属性
- 已完成：`TimerFormat` 去掉未使用父级参数，Select ref 改为 antd 公开类型，避免继续保留局部 `any` ref
- 已完成：普通表格与指标卡条件样式弹窗的 target、颜色回调、数值区间回调和指标选项输入补齐局部显式类型，保持保存后的条件样式结构与匹配语义不变
- 已完成：`ListTemplatePanel` 的选项状态、当前选中项、模板行创建和子组件更新入口补齐局部类型，去掉面板内部 `any[]` / `as any`
- 已完成：`UnControlledTableHeaderPanel` 的表格行选择入口改为复用 antd 公开 `TableRowSelection`，内部继续按字符串 uid 状态处理，保持选择与合并行为不变
- 已完成：`ControllerList` 的看板详情返回、控制器名称解析和关系变更入口补齐局部类型，去掉控制器列表里的响应体 `any`
- 已完成：`DataReferencePanel` 的动态字段组装函数补齐返回类型，指标字段列表按局部最小结构收口，保持参考线/参考区配置组装语义不变
- 已完成：控制器默认值滑块 setter 的 Form.Item props 与 Slider props 拆分透传，避免 `maxValue / minValue / step / showMarks` 这类控件配置继续进入 Form.Item
- 已完成：控制器文本默认值 setter 去掉未使用的 `value` 解构，保持 Form 注入行为不变
- 已完成：`useSaveAsViz` 的复制保存链路补齐最小详情 DTO 与保存 payload 类型，去掉局部 `request2<any>` / `requestData: any`
- 已完成：看板图片上传、故事板分享、保存模板、标签页和故事板选择等组件局部类型边界收口，去掉局部 `@ts-ignore` 与上传私有 ref 依赖
- 已完成：故事板编辑态历史 state、故事板权限、VirtualTable 滚动体回调和视频/分组 widget 测试 fixture 的局部类型边界收口，去掉对应局部 `any` / `as any`
- 已完成：Dashboard 工具函数请求参数复用现有 `getDataOption` 类型，移动端间距配置类型对齐运行时缺省语义，去掉相关测试局部 `as any`
- 已完成：数据源配置模板的 `defaultValue` 与 `options` 从宽泛 `any` 收口为 JSON 兼容值和明确 option 联合类型，配置表单按 `dbType` / 字符串选项做局部类型守卫
- 已完成：看板查询/重置按钮点击事件补齐 React 鼠标事件类型，`DataChart.status` 收口为数值状态，DataChartWidget meta 入参图标类型收口为字符串
- 暂缓评估：`ChartFilterCondition` 的 `value` 运行时兼容面大于当前公共 `FilterCondition['value']` 类型，直接收口会牵涉多个筛选 UI 调用链，需要单独评估公共类型与运行时协议
- 暂缓评估：FormGenerator 全局 `ItemLayoutProps`、交互规则面板的动态 `value` / `Customize` 映射仍是协议宽口，需单独评估交互配置结构后再收口
- 暂缓评估：`ChartDataSetDTO.rows` 仍按历史 `string[][]` 表达图表运行时数据集，大量图表组件和 helper 以 `IChartDataSet<string>` 消费；后续如要对齐后端 `Dataframe.rows: List<List<Object>>`，需单独处理图表运行时的数据集泛型与字符串化边界
- 暂缓评估：`WidgetConf.content` / `WidgetCreateProps.content` 是多类 widget 的共享内容协议宽口，直接改成 `unknown` 会牵涉图表、控制器、标签页、富文本和迁移链路，需要按 widget 类型分批拆分
- 暂缓评估：Dashboard 工具测试中被跳过的历史大 fixture 仍有 `obj as any`，直接补全为完整 `Widget` 会引入大量非测试重点字段，后续如恢复这些测试应先抽通用 widget fixture helper
- 下一批候选：前端低风险继续在当前长期分支累计，优先处理局部组件、工具函数和有测试覆盖的类型边界；暂不因单个小批次合入 `main`

当前已落地范围：

- `VariablePage/utils.ts` 继续沉淀变量权限日期值序列化 helper
- `VariablePage/index.tsx` 与 `ViewPage/Main/Properties/Variables.tsx` 改为复用统一 relation 序列化入口
- `date.ts` 继续补齐范围时间格式化与“今天结束前禁用” helper
- `chartHelper.ts` 继续收口 tooltip、axis label overflow 与 selected item style 相关 helper 的局部类型边界
- `internalChartHelper.ts` 继续收口图表交互过滤器值映射的返回边界
- `urlSearchTransfer.ts` 补齐 URL 参数工具的显式类型与回归测试
- `useStateModal.tsx` 与 `useGetVizIcon.tsx` 补齐内部 hook 局部类型边界
- `rejectedErrorHandlerMiddleware.ts` 补齐 Redux rejected action 错误消息读取边界
- `injectReducer/index.tsx` 与 `reducerInjectors.ts` 补齐动态 reducer 注入器类型边界
- `loadable.tsx` 补齐懒加载组件 props 推断边界
- `object.ts` 补齐通用数组/空值/合并/复制/判断 helper 的低风险类型边界
- `debugger.ts` 补齐性能测量工具的泛型返回值边界
- `utils.ts` 补齐通用错误处理和 reject helper 的错误对象边界
- `redux/__tests__/reducer.test.ts` 补齐 reducer 测试的真实注入 key 与 action 类型边界
- `checkStore.ts` 补齐 Redux 动态注入 store 校验入口边界
- `utils.ts` 补齐 className 合并与事件阻止冒泡 helper 的入参边界
- `internalChartHelper.ts` 补齐 view config 解析对象守卫
- `chartHelper.ts` 补齐 interval 判断和 meta 路径查找 helper 类型边界
- `fetch.ts` 补齐计算字段校验、可用函数列表、下载、插件加载、分享任务与图表数据请求 helper 类型边界
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
- `chartHelper.ts` 的 `transformToObjectArray` 改为显式对象数组返回，runtime date level helper 通过重载表达 `ChartDataSectionField` / `ChartDataViewMeta` 两类真实输入
- `ChartDataRequestBuilder` 继续收口 computed function column 的返回结构，避免请求构造链路继续依赖隐式 `any`
- `internalChartHelper.ts` 的样式值合并比较入口改为 `unknown`，保持布尔、数值、字符串、数组、对象和空值兼容规则不变
- `utils.ts` 的树构造 helper 增加 `ListTreeNode<T>` 返回结构，`getInsertedNodeIndex` 改为只依赖 `parentId / index` 的最小输入结构，保持原索引计算语义不变
- `utils.ts` 的模型字段按表分组 helper 去掉 `columnNameObj`、`columnTreeData` 和输出节点的宽泛 `any`，缺失 `path` 的字段显式跳过，避免字段树构建时异常扩散
- `ChartDataRequest.ts` 新增 `ChartVariableParams`，`ChartDataRequestBuilder`、看板查看态/编辑态同步复用该类型，避免变量参数继续以 `Record<string, any[]>` 透传
- `internalChartHelper.ts` 的变量交互 helper 复用 `ChartVariableParams`，并对变量默认值解析补齐数组判断、字符串化和异常回退
- `DashBoardPage/utils/index.ts` 的过滤器去重 helper 改为基于 `column` 的泛型最小结构，去掉局部 `Record<string, any>` 中转
- `DashBoardPage/actions/widgetAction.ts` 的表格分页/排序事件入口补齐局部事件值类型守卫，保持分页页码独立透传，排序仅在 `ASC/DESC` 合法时进入请求
- `Board/slice/types.ts` 的 `getDataOption.sorters` 改为复用 `ChartDataRequest['orders']`，看板查看态、编辑态和图表请求 helper 不再需要 `as any[]` 强转
- `DashBoardPage/actions/widgetAction.ts` 的联动点击过滤器转换补齐数组判断和字符串化，非数组输入按空值处理，避免未知值直接 `.map`
- `ControllerWidgetPanel/utils.ts` 的 `rangeNumberValidator` 改为复用 `RangeNumberValue`，删除空分支，数值比较前显式处理 number/string 输入
- `DashBoardPage/utils/widget.ts` 的 `handleRowDataForTree`、`convertListToTree` 补齐局部节点类型，保持原树节点字段结构不变
- `BoardEditor/slice/thunk.ts` 的请求失败回调统一走 `getErrorMessage(error)`，避免错误对象继续通过 `any.message` 读取
- `BoardEditor/index.tsx` 的历史导航 state 改为显式 `BoardEditorLocationState`，旧 `widgetInfo` 字符串解析结果只声明当前使用的最小字段
- `SlideSetting/BoardConfigPanel.tsx` 与 `SlideSetting/WidgetConfigPanel.tsx` 的折叠面板 items 透传改为 `Pick<CollapseProps, 'items'>`
- `TabWidget/tabConfig.ts` 与 `TabWidgetCore.tsx` 的标签位置从字符串强转改为显式合法值收口，保持旧配置异常值回退到 `top`
- `DashBoardPage/utils/widget.ts` 的时间控制器 URL 参数回填与自有图表内容读取改为复用 `ControllerDate`、`ChartWidgetContent`，避免继续通过局部 `as any` 穿透
- `ChartSelectModal.tsx` 的默认勾选 id 与树选中事件补齐显式节点类型，保留“过滤文件夹和已存在图表”的既有逻辑
- `CustomOptions.tsx` 的拖拽行参数、`AddControler.tsx` 的菜单事件与按钮类型、`controlActions.ts` 的新增控制器 action 入参继续收口到显式边界
- `LayerTreeItem.tsx` 的树节点内容改为复用 `WidgetConf['content']`，只表达既有 widget 配置结构
- `ChartStyleConfigPanel.tsx` 的分组折叠面板改为直接复用 antd `CollapseProps['items']`，继续压缩 Workbench 样式配置链路中的兼容层强转
- `ChartGraphIcon.tsx` 的图标渲染入参与 base64 图片节点改为显式结构，避免图标组件继续依赖未声明的 `iconStr/src/alt`
- `CategoryConditionEditableTable.tsx` 与 `SortAction/DraggableItem.tsx` 的拖拽行/拖拽项参数改为显式索引与 HTML 属性结构，继续压缩 Workbench 拖拽链路里的局部 `any`
- `CategoryConditionConfiguration.tsx`、`FilterFacadeConfiguration.tsx`、`FilterVisibilityConfiguration.tsx` 的列表/树选中、facade 切换、slider 区间与可见性条件参数改为显式兼容类型，继续压缩筛选配置链路里的隐式 `any` 与宽泛状态值
- `TextSetter/index.tsx` 与 `BasicSet/NumberSet.tsx` 的表单 props / 数值回调改为复用 antd 公开类型，减少控制器与看板配置链路里的宽泛 `FormItemProps<any>`、`onChange: any`
- `WidgetConfigPanel.tsx`、`ImageUpload.tsx`、`AggregationColorizeAction.tsx`、`ChartComputedFieldSettingPanel.tsx` 的 context / 上传回调 / 颜色事件 / 树选择值改为显式兼容类型，继续压缩 Workbench 与看板配置链路中的局部 `any`
- `.gitignore` 增加 `.tmp/`、`logs/`，减少低风险改造过程中本地运行产物干扰
- `ControllerWidgetPanel/types.ts`、`TimeSetter.tsx`、`ChartDataConfigSection/utils.ts`、`DateLevelMenuItems.tsx` 的共享模型与工具函数边界继续收口，开始把低风险改造从 UI 透传扩展到 Workbench 公共配置层
- `BoardEditor/slice/thunk.ts` 的保存看板请求 thunk 返回值改为显式 `null`，继续压缩 Workbench 编辑链路中“不消费响应体”调用的宽泛泛型
- `DashBoardPage/utils/index.ts` 的 `getWidgetControlValues` 返回结构改为显式控制器值类型，继续压缩看板过滤器参数构造链路里的 `value: any`
- `chartHelper.ts` 的数值单位格式化、未使用表头行、tooltip 配置拼接和最小最大值计算入口继续收口局部 `any[]` / 宽泛入参，保持格式化与 tooltip 输出语义不变
- `internalChartHelper.ts` 的视图配置字段映射和拖拽 item 构造补齐显式局部类型，继续压缩 Workbench 拖拽源与 view config helper 中的宽泛输入边界
- `BasicCheckbox`、`BasicSwitch`、`BasicRadio`、`BasicInput`、`BasicInputNumber` 剥离 FormGenerator 内部 options 后再透传给 antd 控件，避免内部字段继续进入 DOM / antd 链路
- `BasicFont`、`BasicColorSelector`、`BasicLine`、`Background`、`WidgetBorder` 统一通过颜色选择器 options 白名单透传 ColorPicker 配置，避免 `key` spread 和父级 props 继续进入 Popover 链路
- `BasicFontWeight`、`BasicFontStyle`、`BasicFontFamilySelector`、`BasicFontSizeSelector`、`BasicInputPercentage`、`BasicSlider`、`BasicSelector` 的 antd 控件透传继续改为显式控件属性，避免 FormGenerator 行元数据继续进入三方组件
- `TimerFormat` 与条件样式 add 弹窗的局部 ref、context、颜色和区间值回调类型完成收口，减少 FormGenerator 自定义控件中的 `any` 边界
- `ListTemplatePanel` 与 `UnControlledTableHeaderPanel` 的选项状态、模板行和表格选择入口继续补齐局部显式类型，减少自定义控件中的 `any` 边界
- `ControllerList` 与 `DataReferencePanel` 的看板详情、关系变更和动态参考线配置组装入口继续补齐局部显式类型，减少自定义配置面板中的 `any` 边界
- 控制器默认值滑块与文本 setter 继续收口 Form.Item / 控件 props 边界，减少配置项误传

当前验证计划：

- `npm run checkTs`
- `npm run test:ci -- src/app/utils/__tests__/chartDtoHelper.test.ts src/app/utils/__tests__/internalChartHelper.test.ts src/app/hooks/__tests__/useGetSourceDbTypeIcon.test.tsx`
- `npm run test:ci -- src/app/pages/MainPage/pages/VariablePage/__tests__/utils.test.ts src/app/pages/MainPage/pages/ViewPage/__tests__/utils.test.ts`
- `npm run test:ci -- src/app/pages/MainPage/pages/ViewPage/__tests__/utils.test.ts`

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
- 图表 helper 中 runtime date level 与样式值合并类型边界收口
- 通用树 helper 与插入索引 helper 类型边界收口
- 模型字段树分组 helper 类型边界收口
- 主页面局部类型边界收口
  - `Profile` 与 `MemberForm` 的关闭回调去掉 `null as any`，按真实无参关闭语义传递
  - Schedule 配置解析补齐 `JobConfig` 安全入口，附件类型按 `FileTypes` 过滤
  - Schedule / Viz 回收站树标题节点补齐局部显式类型，去掉 `node as any`
  - Viz 页签菜单事件改用 Ant Design 公开事件类型
  - Viz 新增、另存、表单初始值与保存请求 payload 类型边界收口，保留现有保存流程不变
  - Viz 运行态树筛选、下拉筛选值转换与下载任务回调泛型继续去掉局部宽泛 `any`
- 工具函数与 Workbench 配置更新边界收口
  - `curry` 参数缓存从 `any[]` 收口为 `unknown[]`
  - `ChartConfigPayloadType.value` 改为图表配置节点联合类型，避免 Workbench 配置更新继续透传宽泛 `any`
  - `updateCollectionByAction` / `updateByAction` 从只支持样式配置泛化为支持数据、样式、i18n 与子行配置节点，匹配当前真实调用面
- 资源迁移与变量默认值表单边界收口
  - 资源导入 `FileUpload` 改用 Ant Design 上传回调类型，去掉 `value` / `onChange` 的宽泛 `any`
  - 变量默认值输入状态按字符串、数字、日期默认值联合类型收口，文本、数字和日期控件按各自真实值边界传递
- ViewPage 测试层 legacy 输入边界收口
  - `diffMergeHierarchyModel` / `addPathToHierarchyStructureAndChangeName` 测试改为集中 legacy hierarchy helper，去掉断言点散落 `as any`
  - `DataModelTree.toModel` 返回值改为列名映射结构，测试不再需要强转读取节点
  - 图表预览时间筛选测试改用 `FilterConditionType` 与集中 legacy condition helper，保留旧异常值兼容测试
- 普通表格与指标卡条件样式边界收口
  - 条件样式匹配入口从 `any` / 过窄 `string | number` 改为 `unknown` 输入加局部可比较值类型，保留 null、空字符串和 undefined 判断语义
  - 普通表格行记录改为 `Record<string, unknown>`，避免行条件样式继续以宽泛对象透传
  - 普通表格 cell、可调整表头和列标题组件补齐 HTML / react-resizable 公开 props 类型，去掉局部 `any` 和 styled td 宽泛 props
  - 仪表盘图 tooltip 数据和 data itemStyle 补齐局部显式结构，去掉 tooltip formatter 里的 `data: any` 与 itemStyle 宽泛类型

## 7. 下一阶段执行顺序

按这个顺序推进，避免专题扩散：

1. 继续复扫生产工具函数中的低风险单点类型债，只处理不改变运行时语义的局部收口
2. 优先从 `chartHelper.ts` / `internalChartHelper.ts` 中挑已有测试覆盖、可隔离的 helper 继续收口
3. schema/复制保存这类返回数据会进入业务拼装的链路继续单独评估，不混入普通低风险批次
4. 时间体系剩余调用点继续收口
5. 前端依赖收口与历史入口审计继续逐个推进
6. 安装健康度与锁文件一致性持续检查

## 8. 分层门禁

### 8.1 范围规则

- 一个提交只处理一个专题
- 低风险与高风险不混提
- 当前专题之外的改动不顺手带上
- 同一低风险专题尽量累计到一批再提交和推送，避免每个小点都触发完整回归

### 8.2 验证规则

当前门禁按三层执行，避免每个小改动都触发完整回归：

1. 开发期：只跑与改动直接相关的轻量验证。
2. 提交前：只检查本次暂存文件，避免全量样式扫描。
3. 合并前：再跑完整门禁，保证 `main` 质量。

当前执行策略：

- 专题分支日常开发不因每个小点触发完整门禁，优先执行 `npm run checkTs` 与相关定向测试
- 同一专题累计到一批后再提交和推送，降低重复回归成本
- 准备 merge 回 `main` 前必须补完整门禁，不能用轻量验证替代主线验收

当前 `frontend/.husky/pre-commit` 已改为 staged 范围：

- TS / JS 文件：`eslint --fix`、`stylelint`、`prettier` 只作用于本次暂存文件
- CSS / MD / JSON 文件：`prettier` 只作用于本次暂存文件

当前 `frontend/.husky/pre-push` 已改为分支分层：

- 推送 `main`：自动执行完整前端门禁
- 推送专题分支：默认只执行轻量门禁 `npm run checkTs`
- 专题分支需要主动完整回归时：使用 `YU_BI_FULL_GATE=1 git push origin <branch>`

前端低风险小步改造开发中默认执行：

- `npm run checkTs`
- 与改动文件直接相关的定向测试，例如 `npm run test:ci -- <test files>`

前端阶段性批次提交或推送前再执行：

- `npm run checkTs`
- 必要时 `npm run build:all`
- 必要时 `npm run test:ci -- --silent`

当前低风险批次合并前门禁记录：

- 通过：富文本运行时加载稳定化专项轻量门禁，`npm run checkTs`
- 通过：富文本运行时加载稳定化专项定向测试，`npm run test:ci -- src/app/components/ChartGraph/BasicRichText/__tests__/runtime.test.ts src/app/components/ChartGraph/BasicRichText/__tests__/readyState.test.ts`
- 通过：`flexlayout-react` 工作台布局工厂稳定化专项轻量门禁，`npm run checkTs`
- 通过：`flexlayout-react` 工作台布局工厂稳定化专项定向测试，`npm run test:ci -- src/app/pages/ChartWorkbenchPage/components/ChartOperationPanel/__tests__/layoutRuntime.test.ts`
- 通过：`react-grid-layout` 布局归一化稳定化专项轻量门禁，`npm run checkTs`
- 通过：`react-grid-layout` 布局归一化稳定化专项定向测试，`npm run test:ci -- src/app/pages/DashBoardPage/hooks/__tests__/useGridLayoutMap.test.ts`
- 通过：`react-window` 虚拟表格稳定化专项轻量门禁，`npm run checkTs`
- 通过：`react-window` 虚拟表格稳定化专项定向测试，`npm run test:ci -- src/app/components/__tests__/VirtualTable.test.tsx`
- 通过：ChartEventListenerHelper 回调类型边界批次轻量门禁，`npm run checkTs`
- 通过：ChartEventListenerHelper 回调类型边界批次定向测试，`npm run test:ci -- src/app/utils/__tests__/ChartEventListenerHelper.test.ts`
- 通过：字段 Action 菜单类型边界批次轻量门禁，`npm run checkTs`
- 通过：交互跳转与小组件参数边界批次轻量门禁，`npm run checkTs`
- 通过：表格与指标卡条件样式边界批次轻量门禁，`npm run checkTs`
- 通过：ViewPage 测试层边界批次定向测试，`npm run test:ci -- src/app/pages/MainPage/pages/ViewPage/__tests__/utils.test.ts src/app/pages/MainPage/pages/ViewPage/Main/Properties/DataModelTree/__tests__/utils.test.ts src/app/pages/MainPage/pages/VizPage/ChartPreview/components/ControllerPanel/components/__tests__/timeFilterUtils.test.ts`
- 通过：工具函数与表单边界批次定向测试，`npm run test:ci -- src/app/pages/MainPage/pages/VariablePage/__tests__/utils.test.ts`
- 通过：工具函数与 Workbench 配置更新边界批次轻量门禁，`npm run checkTs`
- 通过：主页面局部类型边界批次轻量门禁，`npm run checkTs`
- 通过：组件局部类型边界批次轻量门禁，`npm run checkTs`
- 通过：故事板与虚拟表格局部类型边界批次轻量门禁，`npm run checkTs`
- 通过：widget 测试 fixture 类型边界批次定向测试，`npm run test:ci -- src/app/pages/DashBoardPage/components/Widgets/VideoWidget/__tests__/VideoWidgetCore.test.tsx src/app/pages/DashBoardPage/components/Widgets/GroupWidget/__tests__/utils.test.ts`
- 通过：Dashboard 工具函数参数边界批次轻量门禁，`npm run checkTs`
- 通过：Dashboard 工具函数参数边界批次定向测试，`npm run test:ci -- src/app/pages/DashBoardPage/utils/__tests__/index.test.tsx src/app/pages/DashBoardPage/utils/__tests__/board.test.ts`
- 通过：数据源配置模板类型边界批次轻量门禁，`npm run checkTs`
- 通过：看板按钮与 DataChart 局部类型边界批次轻量门禁，`npm run checkTs`
- 通过：看板按钮与 DataChart 局部类型边界批次定向测试，`npm run test:ci -- src/app/pages/DashBoardPage/utils/__tests__/index.test.tsx`
- 通过：通用 UI 与工具类型边界专题合并前完整门禁，`npm run test:ci` 109 个测试文件通过，840 个测试通过，4 个跳过
- 通过：通用 UI 与工具类型边界专题合并前完整门禁，`npm run lint:css`
- 通过：通用 UI 与工具类型边界专题合并前完整门禁，`npm run lint:style`
- 通过：查询结果边界专题合并前完整门禁，`npm run test:ci` 109 个测试文件通过，840 个测试通过，4 个跳过
- 通过：查询结果边界专题合并前完整门禁，`npm run lint:css`
- 通过：查询结果边界专题合并前完整门禁，`npm run lint:style`
- 通过：`npm run test:ci`，109 个测试文件通过，838 个测试通过，4 个跳过
- 通过：`npm run lint:css`
- 通过：`npm run lint:style`

触发完整前端门禁的条件：

- 依赖、构建配置、路由入口、运行时加载、共享模型、迁移链路发生变化
- 一个批次准备 merge 回 `main`
- 定向测试不足以覆盖改动影响面
- 之前同专题已累计多次小步修改，需要阶段性回归

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
- 小步改造可先只做定向验证，阶段性提交或推送时再补完整门禁
