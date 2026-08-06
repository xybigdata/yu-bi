import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { chmod, mkdtemp, mkdir, rm, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";
import test from "node:test";

const execFileAsync = promisify(execFile);
const repoRoot = fileURLToPath(new URL("../..", import.meta.url));
const healthScript = path.join(repoRoot, "scripts/check-demo-health.sh");

const writeExecutable = async (file, content) => {
  await writeFile(file, content);
  await chmod(file, 0o755);
};

test("服务进程提前退出时健康检查立即输出日志并失败", async () => {
  const fixtureRoot = await mkdtemp(path.join(os.tmpdir(), "yu-bi-health-"));
  const fakeBin = path.join(fixtureRoot, "bin");
  const installZip = path.join(fixtureRoot, "yu-bi-server-v0.1.0-install.zip");

  try {
    await mkdir(fakeBin);
    await writeFile(installZip, "fixture");
    await writeExecutable(
      path.join(fakeBin, "unzip"),
      `#!/usr/bin/env bash
set -euo pipefail
mkdir -p "$4/lib"
`,
    );
    await writeExecutable(
      path.join(fakeBin, "java"),
      `#!/usr/bin/env bash
echo "模拟 Java 启动失败" >&2
exit 42
`,
    );
    await writeExecutable(
      path.join(fakeBin, "curl"),
      `#!/usr/bin/env bash
exit 7
`,
    );

    await assert.rejects(
      execFileAsync("bash", [healthScript], {
        cwd: repoRoot,
        env: {
          ...process.env,
          PATH: `${fakeBin}:${process.env.PATH}`,
          YUBI_DEMO_INSTALL_ZIP_GLOB: installZip,
          YUBI_DEMO_TIMEOUT_SECONDS: "1",
        },
        timeout: 5_000,
      }),
      (error) => {
        assert.equal(error.code, 1);
        assert.match(error.stderr, /服务进程提前退出/);
        assert.match(error.stderr, /模拟 Java 启动失败/);
        return true;
      },
    );
  } finally {
    await rm(fixtureRoot, { force: true, recursive: true });
  }
});
