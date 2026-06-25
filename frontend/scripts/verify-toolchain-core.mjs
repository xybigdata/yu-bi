import { readFile } from 'node:fs/promises';
import path from 'node:path';

const trimVersionPrefix = value => value.trim().replace(/^v/, '');

const assertEqual = (name, actual, expected) => {
  if (actual !== expected) {
    throw new Error(`${name} 不匹配: ${actual} !== ${expected}`);
  }
};

const parseVersion = value => {
  const [, major, minor, patch] =
    trimVersionPrefix(value).match(/^(\d+)\.(\d+)\.(\d+)$/) ?? [];

  if (!major) {
    throw new Error(`版本号格式不支持: ${value}`);
  }

  return [Number(major), Number(minor), Number(patch)];
};

const compareVersions = (left, right) => {
  const leftVersion = parseVersion(left);
  const rightVersion = parseVersion(right);

  for (let index = 0; index < leftVersion.length; index += 1) {
    const diff = leftVersion[index] - rightVersion[index];

    if (diff !== 0) {
      return diff;
    }
  }

  return 0;
};

const assertSatisfiesSimpleRange = (name, actual, range) => {
  const parts = range.split(/\s+/).filter(Boolean);

  for (const part of parts) {
    const [, operator, expected] = part.match(/^(>=|<)(\d+\.\d+\.\d+)$/) ?? [];

    if (!operator) {
      throw new Error(`${name} engines 范围格式不支持: ${range}`);
    }

    const comparison = compareVersions(actual, expected);

    if (operator === '>=' && comparison < 0) {
      throw new Error(`${name} 不满足 engines: ${actual} 不在 ${range}`);
    }

    if (operator === '<' && comparison >= 0) {
      throw new Error(`${name} 不满足 engines: ${actual} 不在 ${range}`);
    }
  }
};

export async function readFrontendToolchainFiles(appRoot) {
  const [nvmrc, nodeVersion, npmrc, packageJson] = await Promise.all([
    readFile(path.resolve(appRoot, '.nvmrc'), 'utf8'),
    readFile(path.resolve(appRoot, '.node-version'), 'utf8'),
    readFile(path.resolve(appRoot, '.npmrc'), 'utf8'),
    readFile(path.resolve(appRoot, 'package.json'), 'utf8'),
  ]);

  return {
    nodeVersion: trimVersionPrefix(nodeVersion),
    npmrc,
    nvmrc: trimVersionPrefix(nvmrc),
    packageJson: JSON.parse(packageJson),
  };
}

export function verifyFrontendToolchain({
  engineStrict,
  nodeActual,
  npmActual,
  toolchain,
}) {
  const { nodeVersion, npmrc, nvmrc, packageJson } = toolchain;
  const [, packageManagerVersion] =
    packageJson.packageManager?.match(/^npm@(.+)$/) ?? [];
  const actualNode = trimVersionPrefix(nodeActual);
  const actualNpm = npmActual.trim();

  if (!packageManagerVersion) {
    throw new Error(`packageManager 必须使用 npm: ${packageJson.packageManager}`);
  }

  assertEqual('.nvmrc 与 .node-version', nvmrc, nodeVersion);
  assertSatisfiesSimpleRange('Node 运行时版本', nodeVersion, packageJson.engines?.node);
  assertSatisfiesSimpleRange('Node 运行时版本', actualNode, packageJson.engines?.node);
  assertSatisfiesSimpleRange(
    'packageManager npm 版本',
    packageManagerVersion,
    packageJson.engines?.npm,
  );
  assertSatisfiesSimpleRange('npm 运行时版本', actualNpm, packageJson.engines?.npm);
  assertEqual('npm engine-strict', engineStrict.trim(), 'true');

  if (packageJson.engines?.node !== '>=24.0.0 <25.0.0') {
    throw new Error(`Node engines 不匹配: ${packageJson.engines?.node}`);
  }

  if (packageJson.engines?.npm !== '>=11.0.0 <12.0.0') {
    throw new Error(`npm engines 不匹配: ${packageJson.engines?.npm}`);
  }

  if (!/^engine-strict=true$/m.test(npmrc)) {
    throw new Error('.npmrc 必须显式启用 engine-strict=true');
  }

  return {
    nodeVersion: actualNode,
    npmVersion: actualNpm,
    packageManager: packageJson.packageManager,
  };
}
