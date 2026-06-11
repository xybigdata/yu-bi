# datart Community Maintained Fork

> A community-maintained fork of `datart`, focused on keeping the project runnable, modernized, and open.

## Project Status

This repository is no longer treated as an upstream mirror. It is a maintained open source fork based on the original `datart` project.

Current maintenance goals:

- keep the project buildable on modern toolchains
- preserve core datart capabilities and compatibility where practical
- continue security, dependency, and runtime maintenance
- keep all ongoing maintenance work open and reviewable

This fork is maintained independently. It is derived from the original `datart` codebase, but it is not presented as the original official project.

## Upstream Origin

- Original project: `running-elephant/datart`
- License: `Apache-2.0`
- This repository retains the original license and attribution notices required by Apache License 2.0.

## What This Fork Maintains

The current maintenance line is focused on:

- JDK 21 compatibility
- Spring Boot 3 / Spring Cloud modern baseline
- Node 26 local development compatibility
- Vite-based frontend build pipeline
- dependency upgrades that do not unnecessarily break existing behavior
- gradual migration away from aging technical stacks such as legacy test setup, Shiro coupling, and old editor/runtime integrations

## Current Baseline

- Java: `21`
- Spring Boot: `3.5.12`
- Spring Cloud: `2025.0.1`
- Node.js: `26.x`
- Frontend build: `Vite 5`
- React: `18.3.x`

## Local Development

### Backend

See [Deployment.md](./Deployment.md) for deployment notes.

Common commands:

```bash
mvn -pl server -am -DskipTests compile
mvn -pl server -am -DskipTests package
```

### Frontend

```bash
cd frontend
npm install --legacy-peer-deps
npm run checkTs
npm run build
npm run test:ci
```

## Documentation

- Deployment guide: [Deployment.md](./Deployment.md)
- Modernization plan: [docs/tech-stack-modernization-plan.md](./docs/tech-stack-modernization-plan.md)
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

Original project attribution remains in place. New maintenance work in this fork is also released under Apache 2.0 unless explicitly stated otherwise in a specific file.
