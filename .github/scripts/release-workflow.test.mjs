import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const workflowPath = new URL("../workflows/release.yml", import.meta.url);
const ciWorkflowPath = new URL(
  "../workflows/dev-ut-stage.js.yml",
  import.meta.url,
);
const releaseNotesPath = new URL("../release-notes/v0.1.0.md", import.meta.url);

const readWorkflow = () => readFile(workflowPath, "utf8");

test("主 CI 在打包前执行完整后端测试链", async () => {
  const workflow = await readFile(ciWorkflowPath, "utf8");
  const fullTest = "mvn -B -ntp -Dexec.skip=true clean test";
  const cleanPackage = "mvn -B -ntp -DskipTests clean package";

  assert.ok(workflow.includes(fullTest));
  assert.ok(workflow.includes(cleanPackage));
  assert.ok(workflow.indexOf(fullTest) < workflow.indexOf(cleanPackage));
  assert.doesNotMatch(workflow, /-Dtest=/);
  assert.match(workflow, /npm run build:report:check/);
  assert.match(workflow, /npm run build:report:gzip:check:current/);
  assert.match(workflow, /frontend\/build\/build-report-gzip\.json/);
  assert.match(workflow, /node --test \.github\/scripts\/\*\.test\.mjs/);
  assert.match(workflow, /scripts\/check-release-package\.sh/);
});

test("发布工作流支持标签推送与同标签手动重发", async () => {
  const workflow = await readWorkflow();

  assert.match(workflow, /tags:\s*\["v\*"\]/);
  assert.match(workflow, /workflow_dispatch:/);
  assert.match(workflow, /release_tag:/);
  assert.match(workflow, /refs\/tags\/\$\{RELEASE_TAG\}/);
  assert.doesNotMatch(workflow, /git\s+(tag|push\s+.*--force)/);
});

test("发布工作流执行完整验证与干净构建", async () => {
  const workflow = await readWorkflow();

  const requiredCommands = [
    "npm run verify:toolchain",
    "npm run checkTs",
    "npm run test:ci",
    "npm run lint:css",
    "npm run lint:style",
    "mvn -B -ntp -Dexec.skip=true clean test",
    "mvn -B -ntp -DskipTests clean package",
    "scripts/check-release-package.sh",
    "scripts/check-demo-health.sh",
  ];

  for (const command of requiredCommands) {
    assert.ok(workflow.includes(command), `缺少发布门禁命令: ${command}`);
  }
  assert.match(workflow, /node --test \.github\/scripts\/\*\.test\.mjs/);
});

test("发布工作流生成并证明全部发布资产", async () => {
  const workflow = await readWorkflow();

  assert.match(workflow, /anchore\/sbom-action@/);
  assert.match(
    workflow,
    /file: release\/\$\{\{ steps\.assets\.outputs\.package_name \}\}/,
  );
  assert.doesNotMatch(
    workflow,
    /^\s+path: release\/\$\{\{ steps\.assets\.outputs\.package_name \}\}/m,
  );
  assert.match(workflow, /SHA256SUMS/);
  assert.match(workflow, /actions\/attest-build-provenance@/);
  assert.match(workflow, /actions\/upload-artifact@/);
  assert.match(workflow, /gh release upload .*--clobber/);
  assert.match(workflow, /gh release create .*--verify-tag/);
  assert.match(workflow, /attestations:\s*write/);
  assert.match(workflow, /id-token:\s*write/);
  assert.match(workflow, /contents:\s*write/);
});

test("v0.1.0 重发会同步版本化 Release Notes", async () => {
  const workflow = await readWorkflow();
  const notes = await readFile(releaseNotesPath, "utf8");

  assert.match(workflow, /release-notes\/\$\{RELEASE_TAG\}\.md/);
  assert.match(workflow, /gh release edit .*--notes-file/);
  assert.match(workflow, /gh release create .*--notes-file/);
  assert.doesNotMatch(workflow, /--generate-notes/);
  assert.match(notes, /YUBI_SECURITY_TOKEN_SECRET/);
  assert.match(notes, /SHA256SUMS/);
  assert.match(notes, /SBOM/);
  assert.match(notes, /升级/);
  assert.match(notes, /回滚/);
});
