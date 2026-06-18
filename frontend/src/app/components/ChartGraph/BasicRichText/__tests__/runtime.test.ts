import { afterEach, describe, expect, test, vi } from 'vitest';
import {
  __resetRichTextEditorRuntimeLoaderForTest,
  __setRichTextEditorRuntimeLoaderForTest,
  loadRichTextEditorRuntime,
} from '../runtime';

const createRuntimeModule = () => ({
  default: vi.fn(),
});

describe('loadRichTextEditorRuntime', () => {
  afterEach(() => {
    __resetRichTextEditorRuntimeLoaderForTest();
  });

  test('should reuse pending runtime promise', async () => {
    const runtimeModule = createRuntimeModule();
    const loader = vi.fn().mockResolvedValue(runtimeModule);
    __setRichTextEditorRuntimeLoaderForTest(loader);

    const first = loadRichTextEditorRuntime();
    const second = loadRichTextEditorRuntime();

    await expect(first).resolves.toBe(runtimeModule);
    await expect(second).resolves.toBe(runtimeModule);
    expect(loader).toHaveBeenCalledTimes(1);
  });

  test('should allow retry after runtime loading failed', async () => {
    const runtimeModule = createRuntimeModule();
    const loader = vi
      .fn()
      .mockRejectedValueOnce(new Error('load failed'))
      .mockResolvedValueOnce(runtimeModule);
    __setRichTextEditorRuntimeLoaderForTest(loader);

    await expect(loadRichTextEditorRuntime()).rejects.toThrow('load failed');
    await expect(loadRichTextEditorRuntime()).resolves.toBe(runtimeModule);
    expect(loader).toHaveBeenCalledTimes(2);
  });
});
