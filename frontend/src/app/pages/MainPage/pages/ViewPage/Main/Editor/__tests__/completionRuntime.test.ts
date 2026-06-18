import { afterEach, describe, expect, test, vi } from 'vitest';
import type * as Monaco from 'monaco-editor';
import { __resetMonacoRuntimeLoadersForTest, __setMonacoLoaderForTest } from 'app/components/MonacoEditor/runtime';
import type { AppDispatch } from 'redux/configureStore';
import { registerSqlCompletionProvider } from '../completionRuntime';

vi.mock('../../../slice/thunks', () => ({
  getEditorProvideCompletionItems: vi.fn(payload => ({
    payload,
    type: 'view/getEditorProvideCompletionItems',
  })),
}));

const createMonacoModule = () => {
  const disposable = { dispose: vi.fn() };
  const registerCompletionItemProvider = vi.fn(() => disposable);
  return {
    disposable,
    monaco: {
      languages: {
        registerCompletionItemProvider,
      },
    } as unknown as typeof Monaco,
    registerCompletionItemProvider,
  };
};

describe('registerSqlCompletionProvider', () => {
  afterEach(() => {
    __resetMonacoRuntimeLoadersForTest();
    vi.restoreAllMocks();
  });

  test('should register sql completion provider after monaco loaded', async () => {
    const { disposable, monaco, registerCompletionItemProvider } =
      createMonacoModule();
    const existingDisposable = { dispose: vi.fn() };
    const providerRef = { current: existingDisposable };
    const dispatch = vi.fn(action => {
      action.payload.resolve(() => ({ suggestions: [] }));
    }) as unknown as AppDispatch;
    __setMonacoLoaderForTest(vi.fn().mockResolvedValue(monaco));

    registerSqlCompletionProvider({
      dispatch,
      providerRef,
      sourceId: 'source-1',
      isCancelled: () => false,
      errorMessage: 'load failed',
    });

    await vi.waitFor(() => {
      expect(vi.mocked(dispatch)).toHaveBeenCalledWith({
        payload: expect.objectContaining({ sourceId: 'source-1' }),
        type: 'view/getEditorProvideCompletionItems',
      });
    });
    expect(existingDisposable.dispose).toHaveBeenCalledTimes(1);
    expect(registerCompletionItemProvider).toHaveBeenCalledWith('sql', {
      provideCompletionItems: expect.any(Function),
    });
    expect(providerRef.current).toBe(disposable);
  });

  test('should keep provider unset when monaco loading failed', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const providerRef = { current: undefined };
    const dispatch = vi.fn() as unknown as AppDispatch;
    __setMonacoLoaderForTest(
      vi.fn().mockRejectedValue(new Error('load failed')),
    );

    registerSqlCompletionProvider({
      dispatch,
      providerRef,
      sourceId: 'source-1',
      isCancelled: () => false,
      errorMessage: 'Load completion runtime failed',
    });

    await vi.waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        'Load completion runtime failed',
        expect.any(Error),
      );
    });
    expect(vi.mocked(dispatch)).not.toHaveBeenCalled();
    expect(providerRef.current).toBeUndefined();
  });
});
