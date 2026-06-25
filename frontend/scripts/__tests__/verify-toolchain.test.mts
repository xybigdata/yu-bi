import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

import { afterEach, describe, expect, it } from 'vitest';

import {
  readFrontendToolchainFiles,
  verifyFrontendToolchain,
} from '../verify-toolchain-core.mjs';

const tempRoots: string[] = [];

const createToolchainFixture = async () => {
  const appRoot = await mkdtemp(path.join(os.tmpdir(), 'yu-bi-toolchain-'));
  tempRoots.push(appRoot);

  await writeFile(path.join(appRoot, '.nvmrc'), 'v24.16.0\n');
  await writeFile(path.join(appRoot, '.node-version'), '24.16.0\n');
  await writeFile(path.join(appRoot, '.npmrc'), 'engine-strict=true\n');
  await writeFile(
    path.join(appRoot, 'package.json'),
    JSON.stringify(
      {
        engines: {
          node: '>=24.0.0 <25.0.0',
          npm: '>=11.0.0 <12.0.0',
        },
        packageManager: 'npm@11.13.0',
      },
      null,
      2,
    ),
  );

  return appRoot;
};

afterEach(async () => {
  await Promise.all(
    tempRoots.splice(0).map(root => rm(root, { recursive: true, force: true })),
  );
});

describe('verify-toolchain', () => {
  it('verifies the Node 24 and npm 11 baseline from local config files', async () => {
    const toolchain = await readFrontendToolchainFiles(
      await createToolchainFixture(),
    );

    expect(
      verifyFrontendToolchain({
        engineStrict: 'true\n',
        nodeActual: 'v24.18.0',
        npmActual: '11.16.0\n',
        toolchain,
      }),
    ).toEqual({
      nodeVersion: '24.18.0',
      npmVersion: '11.16.0',
      packageManager: 'npm@11.13.0',
    });
  });

  it('rejects mismatched runtime versions', async () => {
    const toolchain = await readFrontendToolchainFiles(
      await createToolchainFixture(),
    );

    expect(() =>
      verifyFrontendToolchain({
        engineStrict: 'true',
        nodeActual: 'v26.0.0',
        npmActual: '11.13.0',
        toolchain,
      }),
    ).toThrow('Node 运行时版本 不满足 engines');
  });

  it('requires engine-strict in npm config and .npmrc', async () => {
    const toolchain = await readFrontendToolchainFiles(
      await createToolchainFixture(),
    );

    expect(() =>
      verifyFrontendToolchain({
        engineStrict: 'false',
        nodeActual: 'v24.16.0',
        npmActual: '11.13.0',
        toolchain,
      }),
    ).toThrow('npm engine-strict 不匹配');

    expect(() =>
      verifyFrontendToolchain({
        engineStrict: 'true',
        nodeActual: 'v24.16.0',
        npmActual: '11.13.0',
        toolchain: {
          ...toolchain,
          npmrc: 'registry=https://registry.npmjs.org/\n',
        },
      }),
    ).toThrow('.npmrc 必须显式启用 engine-strict=true');
  });
});
