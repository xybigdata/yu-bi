import { describe, expect, test, vi } from 'vitest';
import { ImageDropModule } from '../modules/ImageDropModule';
import type { QuillInstance } from '../quillCompat';

const createQuill = (selection: { index: number; length: number } | null) =>
  ({
    root: {
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    },
    getLength: vi.fn(() => 12),
    getSelection: vi.fn(() => selection),
    insertEmbed: vi.fn(),
  }) as unknown as QuillInstance;

describe('ImageDropModule', () => {
  test('should unbind root event listeners on destroy', () => {
    const quill = createQuill(null);
    const module = new ImageDropModule(quill);

    module.destroy();

    expect(quill.root.removeEventListener).toHaveBeenCalledWith(
      'drop',
      expect.any(Function),
      false,
    );
    expect(quill.root.removeEventListener).toHaveBeenCalledWith(
      'paste',
      expect.any(Function),
      false,
    );
  });

  test('should insert image at cursor index 0', () => {
    const quill = createQuill({ index: 0, length: 0 });
    const module = new ImageDropModule(quill);

    module.insert('data:image/png;base64,demo');

    expect(quill.insertEmbed).toHaveBeenCalledWith(
      0,
      'image',
      'data:image/png;base64,demo',
      'user',
    );
  });

  test('should insert image at document end when selection is missing', () => {
    const quill = createQuill(null);
    const module = new ImageDropModule(quill);

    module.insert('data:image/png;base64,demo');

    expect(quill.insertEmbed).toHaveBeenCalledWith(
      12,
      'image',
      'data:image/png;base64,demo',
      'user',
    );
  });
});
