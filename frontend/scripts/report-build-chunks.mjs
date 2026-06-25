import {
  createBuildReport,
  createReportOptions,
} from './report-build-chunks-core.mjs';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

const options = createReportOptions();
const report = await createBuildReport(options);
const output =
  options.format === 'json'
    ? `${JSON.stringify(report, null, 2)}\n`
    : `${report.lines.join('\n')}\n`;

if (options.output) {
  const outputPath = path.resolve(options.appRoot, options.output);
  await mkdir(path.dirname(outputPath), { recursive: true });
  await writeFile(outputPath, output);
} else {
  process.stdout.write(output);
}

if (options.failOnOversized && report.oversizedCount > 0) {
  process.exitCode = 1;
}
