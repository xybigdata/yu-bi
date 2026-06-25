import {
  createBuildReport,
  createReportOptions,
} from './report-build-chunks-core.mjs';

const options = createReportOptions();
const report = await createBuildReport(options);

if (options.format === 'json') {
  console.log(JSON.stringify(report, null, 2));
} else {
  report.lines.forEach(line => {
    console.log(line);
  });
}

if (options.failOnOversized && report.oversizedCount > 0) {
  process.exitCode = 1;
}
