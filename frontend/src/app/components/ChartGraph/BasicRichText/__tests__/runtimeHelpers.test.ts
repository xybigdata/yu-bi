import { describe, expect, test } from 'vitest';
import type { DeltaStatic } from '../quillCompat';
import {
  createRichTextModules,
  getRichTextContainerId,
  translateRichTextCalcFields,
} from '../runtimeHelpers';

describe('runtimeHelpers', () => {
  test('should create stable rich text container id', () => {
    expect(getRichTextContainerId('widget-1')).toBe('rich-text-widget-1');
  });

  test('should create readonly toolbar config with null container', () => {
    const modules = createRichTextModules({
      containerId: 'rich-text-widget-1',
      handlers: {
        color: () => undefined,
        background: () => undefined,
      },
      editable: false,
      includeCalcField: true,
    });

    expect(modules.toolbar.container).toBeNull();
    expect(modules.imageDrop).toBe(true);
    expect(modules.calcfield).toEqual({});
  });

  test('should translate calcfield insert to field value', () => {
    expect(
      translateRichTextCalcFields(
        {
          ops: [
            { insert: '前缀:' },
            { insert: { calcfield: { name: '订单数' } } },
          ],
        } as DeltaStatic,
        [{ name: '订单数', value: '128' }],
      ),
    ).toEqual({
      ops: [{ insert: '前缀:' }, { insert: '128' }],
    });
  });

  test('should keep plain string value unchanged', () => {
    expect(translateRichTextCalcFields('<p>demo</p>', [])).toBe(
      '<p>demo</p>',
    );
  });
});
