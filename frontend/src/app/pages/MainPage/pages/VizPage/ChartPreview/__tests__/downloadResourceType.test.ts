import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, test } from 'vitest';

describe('chart download resource type', () => {
  test('should submit ResourceType enum values accepted by server', () => {
    const previewSource = readFileSync(
      resolve(
        process.cwd(),
        'src/app/pages/MainPage/pages/VizPage/ChartPreview/ChartPreviewBoard.tsx',
      ),
      'utf8',
    );
    const editorSource = readFileSync(
      resolve(process.cwd(), 'src/app/components/ChartEditor.tsx'),
      'utf8',
    );

    expect(previewSource).toContain("vizType: 'DATACHART'");
    expect(editorSource).toContain("isWidget ? 'WIDGET' : 'DATACHART'");
    expect(`${previewSource}\n${editorSource}`).not.toContain(
      "vizType: 'dataChart'",
    );
    expect(`${previewSource}\n${editorSource}`).not.toContain(
      "vizType: 'widget'",
    );
  });
});
