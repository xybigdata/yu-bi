# 参与贡献

感谢你为 yu-bi 提交问题、文档、测试或代码。这个项目由独立维护者维护，清晰的范围和可复现的验证会显著加快评审。

参与社区即表示你同意遵守 [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md)。安全漏洞请使用 [SECURITY.md](./SECURITY.md) 中的私密渠道，不要创建公开 Issue。

## 提交 Issue

创建 Issue 前，请先搜索已有问题和 [SUPPORT.md](./SUPPORT.md)。

- 缺陷报告应包含版本、环境、最小复现步骤、实际结果和期望结果。
- 功能建议应先说明要解决的问题、目标使用者和可验收的结果。
- 大范围架构调整、兼容性破坏或数据迁移应先发起 Issue，确认方向后再实现。
- 请移除日志、截图和配置中的令牌、密码、个人数据及生产连接信息。

## 开发准备

支持的工具链以根 `pom.xml`、`frontend/package.json` 和锁文件为准，README 提供可读摘要。克隆仓库后安装前端依赖：

```bash
git clone https://github.com/xybigdata/yu-bi.git
cd yu-bi/frontend
npm ci
npm run verify:toolchain
```

建议从最新 `main` 创建范围单一的分支。不要把 Maven `target/`、前端 `node_modules/` 或 `build/`、日志、H2 数据库、本地配置、密钥和安装包提交到仓库。

## 实现原则

- 修复缺陷或新增可观察行为时，优先先写一个会失败的测试，再完成最小实现并整理代码。
- 沿用所在模块的既有设计和命名，不在功能改动中夹带无关重构或格式化。
- 保持兼容性；确需改变公开行为或配置时，在 Issue、PR 和变更记录中说明迁移方式。
- 新增依赖前说明必要性、许可证、维护状态和对产物体积的影响。
- 新增用户可见文字时，同步维护中文和英文翻译。

## 本地验证

根据影响范围运行最小充分的检查。常用后端命令：

```bash
# 编译后端模块及依赖，跳过绑定在 server 的前端构建
mvn -pl server -am -DskipTests -Dexec.skip=true compile

# 全部 Maven 测试
mvn test
```

常用前端命令：

```bash
cd frontend
npm run checkTs
npm run test:ci
npm run build
```

若只运行了相关测试，请在 PR 中写明测试范围。无法执行某项检查时，说明原因和残余风险。

## Pull Request

提交 PR 时请：

- 关联对应 Issue，说明问题、方案和明确不在范围内的内容；
- 填写验证命令及结果，必要时提供界面截图或迁移步骤；
- 更新受影响的 README、部署说明、配置示例和 `CHANGELOG.md`；
- 确认没有提交敏感信息、本地数据库、日志或生成产物；
- 保持提交可审阅。提交信息建议使用 Conventional Commits，例如 `fix: 修复分享页权限校验`。

维护者可能要求补充复现、测试、文档或缩小范围。PR 被接受前，作者负责处理评审意见和分支冲突。

## 许可证

提交贡献即表示你有权提供该内容，并同意按照仓库的 [Apache License 2.0](./LICENSE) 发布。源自第三方的代码、资源或设计必须保留必要归属并说明兼容许可证。
