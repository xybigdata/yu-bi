import { afterEach, describe, expect, test, vi } from 'vitest';
import type * as Monaco from 'monaco-editor';
import {
  __resetMonacoRuntimeLoadersForTest,
  __setMonacoJavascriptLanguageLoaderForTest,
  __setMonacoLoaderForTest,
  __setMonacoSqlLanguageLoaderForTest,
  ensureMonacoJavascriptLanguage,
  ensureMonacoSqlLanguage,
  loadMonaco,
} from '../runtime';

type MonacoModule = typeof Monaco;

const createMonacoModule = () =>
  ({
    languages: {
      register: vi.fn(),
      setMonarchTokensProvider: vi.fn(),
    },
  }) as unknown as MonacoModule;

describe('loadMonaco', () => {
  afterEach(() => {
    __resetMonacoRuntimeLoadersForTest();
  });

  test('should reuse pending runtime promise', async () => {
    const monacoModule = createMonacoModule();
    const loader = vi.fn().mockResolvedValue(monacoModule);
    __setMonacoLoaderForTest(loader);

    const first = loadMonaco();
    const second = loadMonaco();

    await expect(first).resolves.toBe(monacoModule);
    await expect(second).resolves.toBe(monacoModule);
    expect(loader).toHaveBeenCalledTimes(1);
  });

  test('should allow retry after runtime loading failed', async () => {
    const monacoModule = createMonacoModule();
    const loader = vi
      .fn()
      .mockRejectedValueOnce(new Error('load failed'))
      .mockResolvedValueOnce(monacoModule);
    __setMonacoLoaderForTest(loader);

    await expect(loadMonaco()).rejects.toThrow('load failed');
    await expect(loadMonaco()).resolves.toBe(monacoModule);
    expect(loader).toHaveBeenCalledTimes(2);
  });
});

describe('ensureMonacoSqlLanguage', () => {
  afterEach(() => {
    __resetMonacoRuntimeLoadersForTest();
  });

  test('should reuse pending language promise', async () => {
    const monacoModule = createMonacoModule();
    const language = {};
    const loader = vi.fn().mockResolvedValue({ language });
    __setMonacoSqlLanguageLoaderForTest(loader);

    const first = ensureMonacoSqlLanguage(monacoModule);
    const second = ensureMonacoSqlLanguage(monacoModule);

    await expect(first).resolves.toBeUndefined();
    await expect(second).resolves.toBeUndefined();
    expect(loader).toHaveBeenCalledTimes(1);
    expect(monacoModule.languages.register).toHaveBeenCalledWith({ id: 'sql' });
    expect(
      monacoModule.languages.setMonarchTokensProvider,
    ).toHaveBeenCalledWith('sql', language);
  });

  test('should allow retry after language loading failed', async () => {
    const monacoModule = createMonacoModule();
    const loader = vi
      .fn()
      .mockRejectedValueOnce(new Error('load failed'))
      .mockResolvedValueOnce({ language: {} });
    __setMonacoSqlLanguageLoaderForTest(loader);

    await expect(ensureMonacoSqlLanguage(monacoModule)).rejects.toThrow(
      'load failed',
    );
    await expect(ensureMonacoSqlLanguage(monacoModule)).resolves.toBeUndefined();
    expect(loader).toHaveBeenCalledTimes(2);
  });
});

describe('ensureMonacoJavascriptLanguage', () => {
  afterEach(() => {
    __resetMonacoRuntimeLoadersForTest();
  });

  test('should reuse pending language promise', async () => {
    const loader = vi.fn().mockResolvedValue({});
    __setMonacoJavascriptLanguageLoaderForTest(loader);

    const first = ensureMonacoJavascriptLanguage();
    const second = ensureMonacoJavascriptLanguage();

    await expect(first).resolves.toBeUndefined();
    await expect(second).resolves.toBeUndefined();
    expect(loader).toHaveBeenCalledTimes(1);
  });

  test('should allow retry after language loading failed', async () => {
    const loader = vi
      .fn()
      .mockRejectedValueOnce(new Error('load failed'))
      .mockResolvedValueOnce({});
    __setMonacoJavascriptLanguageLoaderForTest(loader);

    await expect(ensureMonacoJavascriptLanguage()).rejects.toThrow(
      'load failed',
    );
    await expect(ensureMonacoJavascriptLanguage()).resolves.toBeUndefined();
    expect(loader).toHaveBeenCalledTimes(2);
  });
});
