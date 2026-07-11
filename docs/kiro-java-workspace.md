# Kiro Java 工作区

YuBi 使用 Maven Reactor 管理后端模块。请从仓库根目录打开项目，并让 Kiro 使用 JDK 21 和 Maven 3.9 或更高版本。

仓库级 `.vscode/settings.json` 同时适用于 VS Code 与 Kiro：Maven 项目会自动更新，JavaCC 在工作区配置阶段生成解析器，npm 构建不会在 Java 增量编译时执行。项目尚未建立统一的 JSpecify 空值契约，因此关闭了注解式 null analysis；JDT 自身的潜在空指针控制流检查仍然启用。

如果 Problems 面板出现 `SqlTestEntity`、`ParamFactory` 等测试类未解析，请依次执行：

1. `Maven: Reload Projects`
2. `Java: Clean Java Language Server Workspace`
3. 重启 Kiro，并重新导入根目录的 `pom.xml`

使用以下命令确认 Maven 已加载数据提供器模块及其测试源码：

```bash
mvn -B -pl data-providers/jdbc-data-provider -am test-compile
```

`data-providers/data-provider-base/target/generated-sources/javacc` 是由 `Parser.jj` 自动生成的目录，不能直接修改。解析器相关修复必须提交到 `Parser.jj` 并通过 Maven 重新生成。

Maven 触发前端构建时，Vite 产物写入 `server/target/frontend-build`，避免 server 模块读取项目目录之外的 `frontend/build`。直接在 `frontend` 目录执行 `npm run build:all` 时仍使用 `frontend/build`，两种构建方式互不影响。

## 已知第三方诊断

- `@echarts-x/custom-word-cloud@1.0.1` 在 ECharts 6 自定义图表运行时调用已弃用的 `api.style`。测试夹具已消除项目自身的 Canvas 和尺寸错误；该提示来自依赖产物，未修改 `node_modules`。
- `@antv/s2@2.7.2` 发布的 ESM source map 引用了未随包发布的源码。Vitest 会报告缺失 source map，但不会影响编译、运行或项目源码诊断；升级依赖后应重新验证。
