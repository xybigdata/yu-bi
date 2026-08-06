# 变更记录

本文件记录 yu-bi 独立维护线的重要变化。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循[语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### 计划

- 根据首个公开版本的反馈完善安装、升级和兼容性说明。

## [0.1.0] - 2026-08-06

首个以 `yu-bi` 品牌独立维护的公开版本。

### 新增

- 建立独立项目身份、Apache 2.0 归属说明、维护者、安全、路线图和社区协作文件。
- 增加数据导出交付任务中心，以及受控的 Agent 写工具和工作区能力。
- 建立 Release 安装包内容检查、校验值和解压启动验证要求。

### 变更

- 后端基线升级到 JDK 21、Spring Boot 4.0.7 和 Spring Cloud 2025.1.2。
- 前端升级到 Node.js 24、npm 11、React 19.2.7 和 Vite 8.1.0，并以 Vitest 作为主要测试运行器。
- 更新主导航、侧栏和多个业务页面的响应式布局与交互。
- 继续推进 Spring Security 接管和旧运行时集成的渐进式改造。

### 安全

- 生产配置要求显式提供至少 32 字节的令牌密钥，不再使用公开固定密钥回退值。
- 生产环境默认关闭注册和自动管理员提升；本地 demo 与生产配置边界分离。
- 更新前端依赖并修复已识别的依赖安全问题。
- 正式安装包排除运行期数据库、跟踪文件、日志和本地配置，并保留许可证与 NOTICE。

### 已知限制

- 当前未发布官方容器镜像。
- demo 模式只用于本地体验，不保证数据持久化、迁移和生产安全性。
- 这是新的独立维护线；从历史 datart 版本升级前应先在隔离环境完成备份和兼容性验证。

[Unreleased]: https://github.com/xybigdata/yu-bi/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/xybigdata/yu-bi/releases/tag/v0.1.0
