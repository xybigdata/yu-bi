# yu-bi 部署指南

本文面向 `v0.1.x`。正式可用的版本和安装包以 [GitHub Releases](https://github.com/xybigdata/yu-bi/releases) 为准。

当前没有官方容器镜像。请使用 Release 安装包或从源码构建，不要使用来源不明的同名镜像。

## 1. 本地体验

本地体验需要 JDK 21。下载安装包后执行：

```bash
unzip yu-bi-server-v0.1.0-install.zip -d yu-bi-dist
cd yu-bi-dist
bash bin/yu-bi-server.sh start
```

访问 <http://127.0.0.1:8080>。未配置外部数据库时，应用使用安装包内的受控 H2 种子进入 demo 模式。

demo 模式只用于本机体验：不要暴露到公网，不要存放生产数据，也不要把其中的固定演示配置复制到生产环境。停止服务可执行：

```bash
bash bin/yu-bi-server.sh stop
```

## 2. 生产准备

生产部署至少需要：

- JDK 21；
- 外部 MySQL；目标数据库版本必须先在隔离环境验证迁移与查询兼容性，当前项目尚未发布完整兼容矩阵；
- 独立且可备份的用户文件目录；
- 至少 32 字节、仅属于当前部署的令牌密钥；
- 反向代理、TLS 和数据库访问控制等基础安全措施。

创建数据库时使用 UTF-8 编码：

```sql
CREATE DATABASE `yubi` CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
```

首次启动和版本升级会执行数据库迁移。数据库账号需要具备完成迁移所需的 DDL/DML 权限；迁移稳定后可按实际访问需求收紧权限。

## 3. 基础配置

编辑安装目录下的 `config/yubi.conf`：

```properties
datasource.ip=127.0.0.1
datasource.port=3306
datasource.database=yubi
datasource.username=<database-user>
datasource.password=<database-password>

server.port=8080
server.address=127.0.0.1
yubi.address=https://bi.example.com
```

不要把包含真实密码的 `yubi.conf` 提交到 Git。限制配置文件的读取权限，并通过部署平台的密钥管理能力注入敏感值。

启动前必须提供稳定的令牌密钥：

```bash
export YUBI_SECURITY_TOKEN_SECRET='<at-least-32-byte-random-secret>'
bash bin/yu-bi-server.sh start
```

该密钥用于签发和校验登录令牌。同一集群内所有实例必须使用同一密钥；部署后不要随意更换，否则现有登录态会失效。不要使用文档占位符、demo 密钥或公开示例值。

## 4. 首个用户

生产配置默认关闭公开注册，也不会自动提升固定用户名。首次初始化时可在受控网络内临时开放注册：

```bash
export YUBI_USER_REGISTER=true
export YUBI_SECURITY_TOKEN_SECRET='<the-same-stable-secret>'
bash bin/yu-bi-server.sh start
```

创建并验证首个账号后停止服务，移除 `YUBI_USER_REGISTER`（或设置为 `false`），再重新启动。确认注册入口已关闭后，才可对外提供服务。

如需在启动时把已存在的指定用户提升为其组织的 `ORG_OWNER`，可临时设置 `YUBI_ADMIN_USERNAME`。完成后应移除该变量，避免后续启动产生意外授权。

## 5. 用户文件与可选服务

默认用户文件位于安装目录的 `files/`。生产环境应把该目录放在独立持久化存储上，并纳入备份、恢复和容量监控。

截图和 PDF 导出需要可访问的 Chrome WebDriver。可在 `config/yubi.conf` 中配置：

```properties
yubi.webdriver-path=http://127.0.0.1:4444/wd/hub
```

WebDriver 不可用时，截图或 PDF 导出会失败，但不应影响应用的其他核心功能。邮件、Redis、OAuth 等高级配置位于 `config/profiles/application-config.yml`，修改前先备份并使用测试环境验证。

## 6. 启停与验证

```bash
bash bin/yu-bi-server.sh start
bash bin/yu-bi-server.sh status
bash bin/yu-bi-server.sh restart
bash bin/yu-bi-server.sh stop
```

启动后至少验证：

- 登录页和健康检查可访问；
- 注册开关符合预期；
- 数据库迁移无报错；
- 用户文件目录可写且备份任务生效；
- 使用到截图或 PDF 导出时，WebDriver 链路可用。

## 7. 升级与回滚

升级前备份数据库、用户文件和外部配置，并在隔离环境验证迁移。不要覆盖旧安装目录，建议解压到新目录后复用外部配置和持久化目录。

令牌密钥在升级前后必须保持一致。若升级失败，先停止新版本，再按已验证的数据库恢复方案和旧安装目录回滚；不要在未确认迁移兼容性时让多个版本同时写入同一数据库。

历史 datart 安装迁移到 yu-bi 可能涉及配置名、资源扩展名和 SQL 兼容差异。`v0.1.x` 不承诺直接升级历史数据，必须先在隔离环境完成验证。
