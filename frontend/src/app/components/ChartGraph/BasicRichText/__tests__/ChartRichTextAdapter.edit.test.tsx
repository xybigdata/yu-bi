import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { forwardRef, useImperativeHandle } from 'react';
import { describe, expect, test, vi } from 'vitest';

import ChartRichTextAdapter from '../ChartRichTextAdapter';
import type {
  RichTextEditorHandle,
  RichTextEditorProps,
} from '../RichTextEditor';
import type { DeltaStatic } from '../quillCompat';

const editorContents = vi.hoisted(() => ({
  current: { ops: [{ insert: '\n' }] } as DeltaStatic,
}));

vi.mock('../RichTextEditor', () => ({
  default: forwardRef<RichTextEditorHandle, RichTextEditorProps>(
    ({ className, onChange, readOnly, value }, ref) => {
      useImperativeHandle(
        ref,
        () => ({
          blur: vi.fn(),
          createMarkdownModule: vi.fn() as never,
          focus: vi.fn(),
          format: vi.fn() as never,
          getContents: () => editorContents.current,
          insertCalcFieldItem: item => {
            editorContents.current = {
              ops: [
                {
                  insert: {
                    calcfield: {
                      id: item.id,
                      name: item.name,
                    },
                  },
                },
              ],
            };
            onChange?.('', editorContents.current, 'user', {
              getBounds: vi.fn(),
              getContents: () => editorContents.current,
              getHTML: vi.fn(),
              getLength: vi.fn(),
              getSelection: vi.fn(),
              getText: vi.fn(),
            });
          },
          isReady: () => true,
          off: vi.fn() as never,
          on: vi.fn() as never,
        }),
        [onChange],
      );

      return (
        <div
          className={className}
          data-readonly={String(readOnly)}
          data-testid={readOnly ? 'rich-text-view' : 'rich-text-editor'}
        >
          {typeof value === 'string' ? value : JSON.stringify(value)}
        </div>
      );
    },
  ),
}));

describe('ChartRichTextAdapter edit runtime', () => {
  test('should insert selected field and emit normalized calcfield delta', async () => {
    const onChange = vi.fn();

    render(
      <ChartRichTextAdapter
        dataList={[{ id: 'amount-field', name: 'SUM(amount)', value: '128' }]}
        id="rich-text-edit"
        initContent={JSON.stringify({ ops: [{ insert: '\n' }] })}
        isEditing
        onChange={onChange}
        t={key => key}
      />,
    );

    fireEvent.click(screen.getByTitle('common.referenceFields'));
    fireEvent.click(await screen.findByText('SUM(amount)'));

    await waitFor(
      () => {
        expect(onChange).toHaveBeenCalledWith(
          JSON.stringify({
            ops: [
              {
                insert: {
                  calcfield: {
                    id: 'amount-field',
                    name: 'SUM(amount)',
                  },
                },
              },
            ],
          }),
        );
      },
      { timeout: 1000 },
    );
  });
});
