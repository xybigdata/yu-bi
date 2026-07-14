# yu-bi Independent Open Source Project

<p align="center">
  <img src="frontend/public/brand/yu-bi-logo.svg" alt="yu-bi logo" width="180" />
</p>

> An independently maintained open source analytics project, focused on keeping the codebase runnable, modernized, and openly maintained.

## Project Status

This repository is maintained as an independent open source project.

Current maintenance goals:

- keep the project buildable on modern toolchains
- preserve core product capabilities and compatibility where practical
- continue security, dependency, and runtime maintenance
- keep all ongoing maintenance work open and reviewable

This project is independently maintained under the `yu-bi` brand. It originated from the original `datart` codebase, but it is not presented as the original official project and it does not follow the original upstream as a maintenance source.

## Project Origin

- Original project: `running-elephant/datart`
- License: `Apache-2.0`
- This repository retains the original license and attribution notices required by Apache License 2.0 and continues development as an independent project.

## What This Project Maintains

The current maintenance line is focused on:

- JDK 21 compatibility
- Spring Boot 3 / Spring Cloud modern baseline
- Node 24 local development compatibility
- Vite-based frontend build pipeline
- dependency upgrades that do not unnecessarily break existing behavior
- gradual migration away from aging technical stacks such as legacy test setup, Shiro coupling, and old editor/runtime integrations

## Current Baseline

- Java: `21`
- Spring Boot: `3.5.15`
- Spring Cloud: `2025.0.3`
- Node.js: `24.x`
- npm: `11.x`
- Frontend build: `Vite 8.1.0`
- React: `19.2.7`

## Local Development

### Backend

See [Deployment.md](./Deployment.md) for deployment notes.

Common commands:

```bash
# Verify backend Java compilation only and skip frontend npm builds bound to the server module.
mvn -pl server -am -DskipTests -Dexec.skip=true compile

# Build the full release package. This runs frontend npm install, frontend build, and assembly.
mvn -pl server -am -DskipTests package
```

### Frontend

```bash
cd frontend
# Use frontend/.nvmrc or frontend/.node-version to select the supported Node runtime.
npm ci
npm run verify:toolchain
npm run checkTs
npm run build:task
npm run build
npm run build:report:check:current
npm run build:report:gzip:check:current
npm run test:ci
```

## Documentation

- Deployment guide: [Deployment.md](./Deployment.md)
- Security policy: [SECURITY.md](./SECURITY.md)
- Maintainers: [MAINTAINERS.md](./MAINTAINERS.md)
- Roadmap: [ROADMAP.md](./ROADMAP.md)
- Changelog: [CHANGELOG.md](./CHANGELOG.md)

## Maintenance Policy

This repository accepts ongoing maintenance changes in these areas:

- build/runtime compatibility
- dependency upgrades
- test stabilization
- documentation and release process improvements
- targeted refactors that reduce modernization risk

Large behavioral or architectural changes should be documented before broad rollout.

## Contributing

Contributions are welcome, especially for:

- failing tests on modern environments
- dependency refreshes
- JDK / Spring / Node compatibility issues
- security fixes
- migration work that reduces legacy stack coupling

Before large changes, open an issue or draft PR to make the migration path explicit.

## Security

Please use the process documented in [SECURITY.md](./SECURITY.md).

## License

This repository continues to be distributed under the [Apache License 2.0](./LICENSE).

Original project attribution remains in place. Ongoing development in this repository is released under Apache 2.0 unless explicitly stated otherwise in a specific file. See [NOTICE](./NOTICE) for project origin and attribution context.
