import { describe, expect, test, vi } from 'vitest';
import { destroyRichTextModules } from '../RichTextEditorRuntime';
import type { QuillWithCalcFieldModule } from '../quillCompat';

describe('destroyRichTextModules', () => {
  test('should destroy known destroyable rich text modules', () => {
    const imageDrop = { destroy: vi.fn() };
    const calcfield = { destroy: vi.fn() };
    const editor = {
      getModule: vi.fn((name: string) => {
        if (name === 'imageDrop') {
          return imageDrop;
        }
        if (name === 'calcfield') {
          return calcfield;
        }
        return undefined;
      }),
    } as unknown as QuillWithCalcFieldModule;

    destroyRichTextModules(editor);

    expect(editor.getModule).toHaveBeenCalledWith('imageDrop');
    expect(editor.getModule).toHaveBeenCalledWith('calcfield');
    expect(imageDrop.destroy).toHaveBeenCalledTimes(1);
    expect(calcfield.destroy).toHaveBeenCalledTimes(1);
  });

  test('should skip modules without destroy method', () => {
    const editor = {
      getModule: vi.fn(() => ({})),
    } as unknown as QuillWithCalcFieldModule;

    expect(() => destroyRichTextModules(editor)).not.toThrow();
  });
});
