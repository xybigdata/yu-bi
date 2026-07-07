import { act, render, screen } from '@testing-library/react';
import { createRef } from 'react';
import { describe, expect, test, vi } from 'vitest';

import type { ChartComputedFieldHandle } from 'app/types/ComputedFieldEditor';

import ChartComputedFieldEditor from '../ChartComputedFieldEditor';

const monacoEditorProps = vi.hoisted(() => ({
  handle: null as null | {
    editor: {
      focus: ReturnType<typeof vi.fn>;
      getModel: ReturnType<typeof vi.fn>;
      trigger: ReturnType<typeof vi.fn>;
    };
  },
  current: null as null | Record<string, unknown>,
}));

vi.mock('app/components/MonacoEditor', async () => {
  const React = await import('react');

  return {
    default: React.forwardRef((props: Record<string, unknown>, ref) => {
      monacoEditorProps.current = props;
      monacoEditorProps.handle ??= {
        editor: {
          focus: vi.fn(),
          getModel: vi.fn(() => ({
            getEOL: vi.fn(() => '\n'),
          })),
          trigger: vi.fn(),
        },
      };
      if (typeof ref === 'function') {
        ref(monacoEditorProps.handle);
      } else if (ref) {
        ref.current = monacoEditorProps.handle;
      }
      return <div data-testid="mock-computed-field-monaco-editor" />;
    }),
  };
});

const createEditor = () => {
  const model = {
    getWordAtPosition: vi.fn(() => ({ word: 'SUM' })),
  };
  let cursorHandler: null | ((event: { position: unknown }) => void) = null;

  return {
    getModel: vi.fn(() => model),
    onDidChangeCursorPosition: vi.fn(handler => {
      cursorHandler = handler;
    }),
    emitCursorPosition: () =>
      cursorHandler?.({ position: { column: 1, lineNumber: 1 } }),
    model,
  };
};

const functionDescription = {
  description: '求和函数',
  name: 'SUM',
  syntax: 'SUM(field)',
  type: 'aggregate',
};

type DqlMonacoModule = {
  editor: {
    defineTheme: ReturnType<typeof vi.fn>;
  };
  languages: {
    getLanguages?: ReturnType<typeof vi.fn>;
    register: ReturnType<typeof vi.fn>;
    setMonarchTokensProvider: ReturnType<typeof vi.fn>;
  };
};

describe('ChartComputedFieldEditor Monaco smoke', () => {
  beforeEach(() => {
    monacoEditorProps.handle = null;
    monacoEditorProps.current = null;
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  test('should register dql language, theme and builtin functions', () => {
    render(
      <ChartComputedFieldEditor
        functionDescriptions={[functionDescription]}
        onChange={vi.fn()}
        value="SUM(price)"
      />,
    );

    const monaco = {
      editor: {
        defineTheme: vi.fn(),
      },
      languages: {
        getLanguages: vi.fn(() => []),
        register: vi.fn(),
        setMonarchTokensProvider: vi.fn(),
      },
    };

    (
      monacoEditorProps.current?.editorWillMount as (
        monaco: DqlMonacoModule,
      ) => void
    )(monaco);

    expect(monacoEditorProps.current).toMatchObject({
      defaultValue: 'SUM(price)',
      language: 'dql',
      options: { lineDecorationsWidth: 1 },
      theme: 'dqlTheme',
    });
    expect(monaco.languages.register).toHaveBeenCalledWith({ id: 'dql' });
    expect(monaco.languages.setMonarchTokensProvider).toHaveBeenCalledWith(
      'dql',
      expect.objectContaining({
        builtinFunctions: ['SUM'],
      }),
    );
    expect(monaco.editor.defineTheme).toHaveBeenCalledWith(
      'dqlTheme',
      expect.any(Object),
    );
  });

  test('should skip duplicate dql language register and keep theme/token setup', () => {
    render(
      <ChartComputedFieldEditor
        functionDescriptions={[functionDescription]}
        onChange={vi.fn()}
        value="SUM(price)"
      />,
    );

    const monaco = {
      editor: {
        defineTheme: vi.fn(),
      },
      languages: {
        getLanguages: vi.fn(() => [{ id: 'dql' }]),
        register: vi.fn(),
        setMonarchTokensProvider: vi.fn(),
      },
    };

    (
      monacoEditorProps.current?.editorWillMount as (
        monaco: DqlMonacoModule,
      ) => void
    )(monaco);

    expect(monaco.languages.register).not.toHaveBeenCalled();
    expect(monaco.languages.setMonarchTokensProvider).toHaveBeenCalledWith(
      'dql',
      expect.objectContaining({
        builtinFunctions: ['SUM'],
      }),
    );
    expect(monaco.editor.defineTheme).toHaveBeenCalledWith(
      'dqlTheme',
      expect.any(Object),
    );
  });

  test('should not throw when dql extension setup failed', () => {
    const consoleErrorSpy = vi
      .spyOn(console, 'error')
      .mockImplementation(() => {});

    render(
      <ChartComputedFieldEditor
        functionDescriptions={[functionDescription]}
        onChange={vi.fn()}
        value="SUM(price)"
      />,
    );

    const monaco = {
      editor: {
        defineTheme: vi.fn(() => {
          throw new Error('theme failed');
        }),
      },
      languages: {
        getLanguages: vi.fn(() => []),
        register: vi.fn(() => {
          throw new Error('register failed');
        }),
        setMonarchTokensProvider: vi.fn(() => {
          throw new Error('tokens failed');
        }),
      },
    };

    expect(() =>
      (
        monacoEditorProps.current?.editorWillMount as (
          monaco: DqlMonacoModule,
        ) => void
      )(monaco),
    ).not.toThrow();

    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'DQL 语言注册失败，计算字段编辑器将降级继续加载',
      expect.any(Error),
    );
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'DQL 语法高亮注册失败，计算字段编辑器将降级继续加载',
      expect.any(Error),
    );
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'DQL 主题注册失败，计算字段编辑器将使用默认主题',
      expect.any(Error),
    );
  });

  test('should insert field, debounce change and show function description', async () => {
    vi.useFakeTimers();
    const onChange = vi.fn();
    const ref = createRef<ChartComputedFieldHandle>();

    render(
      <ChartComputedFieldEditor
        ref={ref}
        functionDescriptions={[functionDescription]}
        onChange={onChange}
        value=""
      />,
    );

    act(() => {
      ref.current?.insertField('SUM(price)', functionDescription);
    });

    expect(monacoEditorProps.handle?.editor.trigger).toHaveBeenCalledWith(
      'keyboard',
      'type',
      { text: 'SUM(price)' },
    );
    expect(monacoEditorProps.handle?.editor.focus).toHaveBeenCalledTimes(1);
    expect(screen.getByText('求和函数: SUM(field)')).toBeInTheDocument();

    act(() => {
      (monacoEditorProps.current?.onChange as (value: string) => void)(
        'SUM(price)\n+1',
      );
      vi.advanceTimersByTime(200);
    });

    expect(onChange).toHaveBeenCalledWith('SUM(price) +1');
  });

  test('should update function description from cursor word', () => {
    vi.useFakeTimers();

    render(
      <ChartComputedFieldEditor
        functionDescriptions={[functionDescription]}
        onChange={vi.fn()}
        value=""
      />,
    );

    const editor = createEditor();
    (
      monacoEditorProps.current?.editorDidMount as (
        editor: ReturnType<typeof createEditor>,
      ) => void
    )(editor);

    act(() => {
      editor.emitCursorPosition();
      vi.advanceTimersByTime(200);
    });

    expect(editor.model.getWordAtPosition).toHaveBeenCalledWith({
      column: 1,
      lineNumber: 1,
    });
    expect(screen.getByText('求和函数: SUM(field)')).toBeInTheDocument();
  });
});
