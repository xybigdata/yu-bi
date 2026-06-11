# datart 社区维护分支

> 这是基于原始 `datart` 项目持续维护的开源分支，目标是让项目继续可运行、可升级、可发布。

## 项目状态

这个仓库不再只是上游仓库的镜像，而是一个持续维护的开源分支。

当前维护目标：

- 保持项目能够在现代工具链上继续构建和运行
- 在可控范围内保持 datart 主要功能行为稳定
- 持续进行安全、依赖和运行时维护
- 所有维护工作继续保持开源、可审阅、可追溯

这个仓库基于原始 `datart` 代码演化，但并不冒充原项目官方维护仓库。

## 上游来源

- 原始项目：`running-elephant/datart`
- 原始许可证：`Apache-2.0`
- 本仓库继续保留 Apache 2.0 要求的许可证与归属信息

## 本分支重点维护内容

当前维护主线主要覆盖：

- JDK 21 兼容
- Spring Boot 3 / Spring Cloud 新基线
- Node 26 本地开发兼容
- 基于 Vite 的前端构建链
- 在尽量不破坏既有功能的前提下推进依赖升级
- 逐步降低测试链、Shiro、安全体系、富文本和旧运行时集成等历史技术包袱

## 当前技术基线

- Java：`21`
- Spring Boot：`3.5.12`
- Spring Cloud：`2025.0.1`
- Node.js：`26.x`
- 前端构建：`Vite 5`
- React：`18.3.x`

## 本地开发

### 后端

部署相关说明参见 [Deployment.md](./Deployment.md)。

常用命令：

```bash
mvn -pl server -am -DskipTests compile
mvn -pl server -am -DskipTests package
```

### 前端

```bash
cd frontend
npm install --legacy-peer-deps
npm run checkTs
npm run build
npm run test:ci
```

## 文档入口

- 部署说明：[Deployment.md](./Deployment.md)
- 技术栈现代化计划：[docs/tech-stack-modernization-plan.md](./docs/tech-stack-modernization-plan.md)
- 安全策略：[SECURITY.md](./SECURITY.md)
- 维护者说明：[MAINTAINERS.md](./MAINTAINERS.md)
- 路线图：[ROADMAP.md](./ROADMAP.md)
- 变更记录：[CHANGELOG.md](./CHANGELOG.md)

## 维护策略

这个仓库当前接受以下类型的持续维护：

- 构建和运行时兼容修复
- 依赖升级
- 测试稳定性治理
- 文档与发布流程完善
- 能显著降低现代化升级风险的定向重构

涉及较大行为变化或架构变化的改造，应先把迁移路径说明清楚。

## 参与贡献

欢迎贡献，尤其是以下方向：

- 现代环境下失败的构建或测试修复
- 依赖升级
- JDK / Spring / Node 兼容问题
- 安全修复
- 降低旧技术栈耦合的迁移工作

如果改动较大，建议先提 issue 或 draft PR，先把迁移边界说明清楚。

## 安全问题

安全问题提交流程参见 [SECURITY.md](./SECURITY.md)。

## 许可证

本仓库继续使用 [Apache License 2.0](./LICENSE)。

原项目归属信息继续保留；本维护分支新增代码默认也在 Apache 2.0 下发布，除非某个文件明确另行说明。
