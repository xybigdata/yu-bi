import { render, screen } from '@testing-library/react';
import { forwardRef, useImperativeHandle } from 'react';
import { describe, expect, test, vi } from 'vitest';

import ChartRichTextAdapter from '../ChartRichTextAdapter';
import type {
  RichTextEditorHandle,
  RichTextEditorProps,
} from '../RichTextEditor';
import type { DeltaStatic } from '../quillCompat';

const editorProps = vi.hoisted(() => ({
  calls: [] as RichTextEditorProps[],
}));

vi.mock('../RichTextEditor', () => ({
  default: forwardRef<RichTextEditorHandle, RichTextEditorProps>(
    (props, ref) => {
      editorProps.calls.push(props);
      useImperativeHandle(
        ref,
        () => ({
          blur: vi.fn(),
          createMarkdownModule: vi.fn() as never,
          focus: vi.fn(),
          format: vi.fn() as never,
          getContents: () => props.value as DeltaStatic,
          insertCalcFieldItem: vi.fn() as never,
          isReady: () => true,
          off: vi.fn() as never,
          on: vi.fn() as never,
        }),
        [props.value],
      );

      return (
        <div
          className={props.className}
          data-readonly={String(props.readOnly)}
          data-testid={props.readOnly ? 'rich-text-view' : 'rich-text-editor'}
        >
          {typeof props.value === 'string'
            ? props.value
            : JSON.stringify(props.value)}
        </div>
      );
    },
  ),
}));

const serializedDelta = JSON.stringify({
  ops: [
    { insert: '城市：' },
    { insert: { calcfield: { id: 'city-field', name: 'city' } } },
    { insert: '，金额：' },
    { insert: { calcfield: { id: 'amount-field', name: 'SUM(amount)' } } },
    { insert: '\n' },
  ],
});

describe('ChartRichTextAdapter readonly runtime', () => {
  beforeEach(() => {
    editorProps.calls = [];
  });

  test('should render translated calcfield values in readonly view', async () => {
    render(
      <ChartRichTextAdapter
        dataList={[
          { id: 'city-field', name: 'city', value: '杭州' },
          { id: 'amount-field', name: 'SUM(amount)', value: '128' },
        ]}
        id="share-rich-text"
        initContent={serializedDelta}
        onChange={vi.fn()}
        t={key => key}
      />,
    );

    const view = await screen.findByTestId('rich-text-view');

    expect(view).toHaveAttribute('data-readonly', 'true');
    expect(view).toHaveTextContent('城市：');
    expect(view).toHaveTextContent('杭州');
    expect(view).toHaveTextContent('金额：');
    expect(view).toHaveTextContent('128');
    expect(screen.queryByTestId('rich-text-editor')).not.toBeInTheDocument();
    expect(editorProps.calls.at(-1)).toMatchObject({
      className: 'react-quill-view',
      formats: expect.any(Array),
      modules: { toolbar: null },
      readOnly: true,
    });
  });

  test('should keep plain readonly content without parsing as delta', async () => {
    render(
      <ChartRichTextAdapter
        dataList={[]}
        id="plain-rich-text"
        initContent="<p>plain content</p>"
        onChange={vi.fn()}
        t={key => key}
      />,
    );

    expect(await screen.findByTestId('rich-text-view')).toHaveTextContent(
      '<p>plain content</p>',
    );
  });

  test('should not render readonly preview before edit preview is opened', () => {
    render(
      <ChartRichTextAdapter
        dataList={[]}
        id="edit-rich-text"
        initContent={serializedDelta}
        isEditing
        onChange={vi.fn()}
        t={key => key}
      />,
    );

    expect(screen.getByTestId('rich-text-editor')).toHaveAttribute(
      'data-readonly',
      'false',
    );
    expect(screen.queryByTestId('rich-text-view')).not.toBeInTheDocument();
  });
});
