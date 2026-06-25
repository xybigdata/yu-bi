import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

import {
  readFrontendToolchainFiles,
  verifyFrontendToolchain,
} from './verify-toolchain-core.mjs';

const execFileAsync = promisify(execFile);
const appRoot = process.cwd();

const [{ stdout: npmActual }, { stdout: engineStrict }] = await Promise.all([
  execFileAsync('npm', ['-v'], { cwd: appRoot }),
  execFileAsync('npm', ['config', 'get', 'engine-strict'], { cwd: appRoot }),
]);

const result = verifyFrontendToolchain({
  engineStrict,
  nodeActual: process.version,
  npmActual,
  toolchain: await readFrontendToolchainFiles(appRoot),
});

console.log(
  `yu-bi frontend toolchain verified: node=${result.nodeVersion}, npm=${result.npmVersion}, packageManager=${result.packageManager}, engine-strict=true`,
);
