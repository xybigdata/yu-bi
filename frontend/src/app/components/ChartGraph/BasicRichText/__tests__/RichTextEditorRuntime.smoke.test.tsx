import { render, screen, waitFor } from '@testing-library/react';
import { createRef } from 'react';
import { afterEach, describe, expect, test } from 'vitest';
import ChartRichTextAdapter from '../ChartRichTextAdapter';
import RichTextEditorRuntime from '../RichTextEditorRuntime';
import type { RichTextEditorHandle } from '../RichTextEditor';

describe('RichTextEditorRuntime Quill 2 smoke', () => {
  afterEach(() => {
    document.body.innerHTML = '';
  });

  test('should mount actual Quill 2 runtime and expose editor contents', async () => {
    const editorRef = createRef<RichTextEditorHandle>();

    render(
      <RichTextEditorRuntime
        ref={editorRef}
        defaultValue={{ ops: [{ insert: 'yu-bi rich text\n' }] }}
        modules={{ toolbar: false }}
        theme="snow"
      />,
    );

    await waitFor(() => expect(editorRef.current?.isReady()).toBe(true));

    expect(editorRef.current?.getContents().ops).toEqual([
      { insert: 'yu-bi rich text\n' },
    ]);
  });

  test('should render chart rich text preview with calcfield content', async () => {
    render(
      <ChartRichTextAdapter
        dataList={[{ name: '订单数', value: '128' }]}
        id="chart-rich-text-smoke"
        initContent={JSON.stringify({
          ops: [
            { insert: '指标:' },
            { insert: { calcfield: { name: '订单数' } } },
            { insert: '\n' },
          ],
        })}
        onChange={() => undefined}
        t={key => key}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText(/指标:128/)).toBeInTheDocument();
    });
  });
});
