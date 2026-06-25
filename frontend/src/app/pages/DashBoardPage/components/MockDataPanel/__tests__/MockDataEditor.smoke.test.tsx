import { act, render } from '@testing-library/react';
import { useSelector } from 'react-redux';
import { describe, expect, test, vi } from 'vitest';

import { ensureMonacoJavascriptLanguage } from 'app/components/MonacoEditor/runtime';

import { MockDataEditor } from '../MockDataEditor';

const monacoEditorProps = vi.hoisted(() => ({
  current: null as null | Record<string, unknown>,
}));

vi.mock('react-redux', async importOriginal => ({
  ...(await importOriginal<typeof import('react-redux')>()),
  useSelector: vi.fn(),
}));

vi.mock('app/components/MonacoEditor/runtime', () => ({
  ensureMonacoJavascriptLanguage: vi.fn(),
}));

vi.mock('app/components/MonacoEditor', () => ({
  default: props => {
    monacoEditorProps.current = props;
    return <div data-testid="mock-monaco-editor" />;
  },
}));

const createEditor = (value = '[]') => ({
  focus: vi.fn(),
  getAction: vi.fn(() => ({
    run: vi.fn(),
  })),
  getValue: vi.fn(() => value),
  setValue: vi.fn(),
});

describe('MockDataEditor smoke', () => {
  beforeEach(() => {
    monacoEditorProps.current = null;
    vi.mocked(useSelector).mockImplementation(selector =>
      selector({ theme: { selected: 'dark' } }),
    );
    vi.mocked(ensureMonacoJavascriptLanguage).mockReset();
  });

  test('should configure monaco editor and ensure javascript runtime', async () => {
    const onDataChange = vi.fn();

    render(
      <MockDataEditor
        originalData={[['杭州', '128']]}
        onDataChange={onDataChange}
      />,
    );

    expect(monacoEditorProps.current).toMatchObject({
      language: 'javascript',
      theme: 'vs-dark',
      value: JSON.stringify([['杭州', '128']], null, 4),
      options: expect.objectContaining({
        automaticLayout: true,
        readOnly: false,
      }),
    });

    await (monacoEditorProps.current?.editorWillMount as () => Promise<void>)();

    expect(ensureMonacoJavascriptLanguage).toHaveBeenCalledTimes(1);
  });

  test('should format, reset and focus editor after mount', () => {
    render(<MockDataEditor originalData={[]} onDataChange={vi.fn()} />);

    const editor = createEditor('[["yu-bi"]]');
    (
      monacoEditorProps.current?.editorDidMount as (
        editor: ReturnType<typeof createEditor>,
      ) => void
    )(editor);

    expect(editor.getAction).toHaveBeenCalledWith(
      'editor.action.formatDocument',
    );
    expect(editor.getValue).toHaveBeenCalled();
    expect(editor.setValue).toHaveBeenCalledWith('[["yu-bi"]]');
    expect(editor.focus).toHaveBeenCalledTimes(1);
  });

  test('should debounce valid JSON array changes and ignore invalid JSON', async () => {
    vi.useFakeTimers();
    const onDataChange = vi.fn();
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

    try {
      render(<MockDataEditor originalData={[]} onDataChange={onDataChange} />);

      act(() => {
        (monacoEditorProps.current?.onChange as (value: string) => void)(
          '[["valid"]]',
        );
        vi.advanceTimersByTime(500);
      });

      expect(onDataChange).toHaveBeenCalledWith([['valid']]);

      act(() => {
        (monacoEditorProps.current?.onChange as (value: string) => void)(
          '{"not":"array"}',
        );
        vi.advanceTimersByTime(500);
      });
      expect(onDataChange).toHaveBeenCalledTimes(1);

      act(() => {
        (monacoEditorProps.current?.onChange as (value: string) => void)('{');
        vi.advanceTimersByTime(500);
      });
      expect(warnSpy).toHaveBeenCalledWith('error on', expect.any(Error));
    } finally {
      warnSpy.mockRestore();
      vi.useRealTimers();
    }
  });
});
