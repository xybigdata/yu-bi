# yu-bi

<p align="center">
  <img src="frontend/public/brand/yu-bi-logo.svg" alt="yu-bi logo" width="180" />
</p>

<p align="center">
  独立维护的开源商业智能与数据可视化平台。
</p>

<p align="center">
  <a href="./README.md">English</a> · 简体中文 ·
  <a href="https://github.com/xybigdata/yu-bi/releases">版本下载</a> ·
  <a href="./Deployment.md">部署文档</a> ·
  <a href="./CONTRIBUTING.md">参与贡献</a>
</p>

## 产品概览

yu-bi 帮助团队连接数据、创建可复用的数据视图和图表，并把分析结果组织成仪表板或故事板，在组织内协作与分享。当前主要能力包括：

- 关系型数据源、SQL 数据视图与可复用数据模型；
- 交互式图表、仪表板和演示型故事板；
- 组织、角色、成员和资源权限管理；
- 分享、定时任务、截图及导出相关工作流；
- 数据提供器和可视化扩展能力。

本仓库源自 [`running-elephant/datart`](https://github.com/running-elephant/datart)，现以 `yu-bi` 品牌独立开发。它不是原项目的官方发行版，也不再把原仓库作为持续维护来源。归属信息参见 [NOTICE](./NOTICE)。

## 项目状态

yu-bi 正处于首个独立发布系列，当前重点是建立可复现的发布流程、安全的部署默认值、现代工具链支持，以及尽量保持兼容的持续维护。可用版本、安装包和版本说明以 [GitHub Releases](https://github.com/xybigdata/yu-bi/releases) 页面为准。

目前没有发布官方容器镜像，请使用 Release 安装包或从源码构建。

## 快速开始

本地体验可先从 [Releases](https://github.com/xybigdata/yu-bi/releases) 下载最新安装包，然后执行：

```bash
unzip <yu-bi-install-package>.zip -d yu-bi-dist
cd yu-bi-dist
bash bin/yu-bi-server.sh start
```

访问 <http://127.0.0.1:8080>。未配置外部数据库时，应用会进入仅供本地体验的 demo 模式。不要把 demo 模式暴露到公网，也不要用它保存生产数据。

生产部署前必须配置外部数据库、至少 32 字节且仅属于当前部署的 `YUBI_SECURITY_TOKEN_SECRET`，并规划首个用户初始化流程。注册与自动提升管理员默认关闭，完整配置和备份要求参见 [Deployment.md](./Deployment.md)。

从源码构建安装包：

```bash
git clone https://github.com/xybigdata/yu-bi.git
cd yu-bi
mvn -pl server -am -DskipTests package
```

构建过程会先安装并构建前端，再把安装包输出到仓库根目录。正式 Release 产物从干净检出构建并完成验证；本地生成的安装包只属于开发产物。

## 开发环境

### 工具链基线

| 范围         | 支持版本 |
| ------------ | -------- |
| Java         | 21       |
| Maven        | 3.9+     |
| Spring Boot  | 4.0.7    |
| Spring Cloud | 2025.1.2 |
| Node.js      | 24.x     |
| npm          | 11.x     |
| React        | 19.2.7   |
| Vite         | 8.1.0    |

后端编译与测试：

```bash
mvn -pl server -am -DskipTests -Dexec.skip=true compile
mvn test
```

前端检查：

```bash
cd frontend
npm ci
npm run verify:toolchain
npm run checkTs
npm run test:ci
npm run build
```

## 文档入口

- [部署说明](./Deployment.md)
- [变更记录](./CHANGELOG.md)
- [路线图](./ROADMAP.md)
- [贡献指南](./CONTRIBUTING.md)
- [支持说明](./SUPPORT.md)
- [安全策略](./SECURITY.md)
- [维护者](./MAINTAINERS.md)

## 参与贡献与获取支持

欢迎提交缺陷报告、功能建议、文档、测试和范围清晰的维护改动。发起 Pull Request 前请先阅读 [CONTRIBUTING.md](./CONTRIBUTING.md)；使用问题和故障排查参见 [SUPPORT.md](./SUPPORT.md)。安全漏洞不得在公开 Issue 中披露，请使用 [SECURITY.md](./SECURITY.md) 规定的私密渠道。

## 许可证

yu-bi 使用 [Apache License 2.0](./LICENSE) 发布。上游归属与项目来源信息保留在 [NOTICE](./NOTICE) 中。
