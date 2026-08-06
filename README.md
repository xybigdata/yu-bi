# yu-bi

<p align="center">
  <img src="frontend/public/brand/yu-bi-logo.svg" alt="yu-bi logo" width="180" />
</p>

<p align="center">
  An independently maintained, open source business intelligence and data visualization platform.
</p>

<p align="center">
  English | <a href="./README_zh.md">简体中文</a> ·
  <a href="https://github.com/xybigdata/yu-bi/releases">Releases</a> ·
  <a href="./Deployment.md">Deployment</a> ·
  <a href="./CONTRIBUTING.md">Contributing</a>
</p>

## Overview

yu-bi helps teams connect data, create reusable views and charts, assemble dashboards and storyboards, and share analytics in an organization. The project focuses on a self-hosted workflow and currently provides:

- relational data sources, SQL-based views, and reusable data models;
- interactive charts, dashboards, and presentation-style storyboards;
- organization, role, member, and resource permission management;
- sharing, schedules, screenshots, and export-oriented workflows;
- extension points for data providers and visualizations.

The repository originated from [`running-elephant/datart`](https://github.com/running-elephant/datart) and is now developed independently under the `yu-bi` brand. It is not an official distribution of the original project and does not use that upstream repository as its ongoing maintenance source. See [NOTICE](./NOTICE) for attribution details.

## Project Status

yu-bi is in its first independent release line. The current focus is a reproducible release process, secure deployment defaults, modern toolchain support, and compatibility-preserving maintenance. Available downloads and their release notes are published on the [GitHub Releases page](https://github.com/xybigdata/yu-bi/releases).

No official container image is currently published. Use a release archive or build from source.

## Quick Start

For local evaluation, download the latest install archive from [Releases](https://github.com/xybigdata/yu-bi/releases), then run:

```bash
unzip <yu-bi-install-package>.zip -d yu-bi-dist
cd yu-bi-dist
bash bin/yu-bi-server.sh start
```

Open <http://127.0.0.1:8080>. With no external database configured, the application enters demo mode for local evaluation only. Do not expose demo mode to the public network or use it to store production data.

For production, configure an external database, a unique `YUBI_SECURITY_TOKEN_SECRET` of at least 32 bytes, and the initial user process before startup. Registration and automatic administrator promotion are disabled by default. Follow [Deployment.md](./Deployment.md) for the full configuration and backup requirements.

To build the install archive from source:

```bash
git clone https://github.com/xybigdata/yu-bi.git
cd yu-bi
mvn -pl server -am -DskipTests package
```

The build runs the frontend installation and build steps before assembling the archive in the repository root. Formal release artifacts are built and verified from a clean checkout; local packages are development artifacts.

## Development

### Toolchain

| Area         | Supported baseline |
| ------------ | ------------------ |
| Java         | 21                 |
| Maven        | 3.9+               |
| Spring Boot  | 4.0.7              |
| Spring Cloud | 2025.1.2           |
| Node.js      | 24.x               |
| npm          | 11.x               |
| React        | 19.2.7             |
| Vite         | 8.1.0              |

Backend compilation and tests:

```bash
mvn -pl server -am -DskipTests -Dexec.skip=true compile
mvn test
```

Frontend checks:

```bash
cd frontend
npm ci
npm run verify:toolchain
npm run checkTs
npm run test:ci
npm run build
```

## Documentation

- [Deployment guide](./Deployment.md)
- [Changelog](./CHANGELOG.md)
- [Roadmap](./ROADMAP.md)
- [Contributing guide](./CONTRIBUTING.md)
- [Support guide](./SUPPORT.md)
- [Security policy](./SECURITY.md)
- [Maintainers](./MAINTAINERS.md)

## Contributing and Support

Bug reports, feature proposals, documentation, tests, and focused maintenance changes are welcome. Read [CONTRIBUTING.md](./CONTRIBUTING.md) before opening a pull request. For usage questions and troubleshooting, see [SUPPORT.md](./SUPPORT.md). Do not report vulnerability details in a public issue; use the private process in [SECURITY.md](./SECURITY.md).

## License

yu-bi is distributed under the [Apache License 2.0](./LICENSE). Upstream attribution and project-origin information are retained in [NOTICE](./NOTICE).
