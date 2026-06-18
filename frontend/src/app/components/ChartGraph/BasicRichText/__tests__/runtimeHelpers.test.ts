import { describe, expect, test } from 'vitest';
import type { DeltaStatic } from '../quillCompat';
import {
  createRichTextModules,
  findRichTextFieldByKey,
  getRichTextFieldKey,
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

  test('should translate calcfield insert by id before name', () => {
    expect(
      translateRichTextCalcFields(
        {
          ops: [
            { insert: { calcfield: { id: 'field-2', name: '订单数' } } },
          ],
        } as DeltaStatic,
        [
          { id: 'field-1', name: '订单数', value: '128' },
          { id: 'field-2', name: '订单数', value: '256' },
        ],
      ),
    ).toEqual({
      ops: [{ insert: '256' }],
    });
  });

  test('should keep calcfield insert when referenced field is missing', () => {
    const calcfield = { calcfield: { id: 'missing-field', name: '订单数' } };

    expect(
      translateRichTextCalcFields(
        {
          ops: [{ insert: calcfield }],
        } as DeltaStatic,
        [],
      ),
    ).toEqual({
      ops: [{ insert: calcfield }],
    });
  });

  test('should build stable field keys and find duplicate field names by key', () => {
    const fields = [
      { id: 'field-1', name: '订单数', value: '128' },
      { name: '订单数', value: '256' },
    ];

    expect(getRichTextFieldKey(fields[0], 0)).toBe('field-1');
    expect(getRichTextFieldKey(fields[1], 1)).toBe('订单数-1');
    expect(findRichTextFieldByKey(fields, 'field-1')).toBe(fields[0]);
    expect(findRichTextFieldByKey(fields, '订单数-1')).toBe(fields[1]);
  });

  test('should keep plain string value unchanged', () => {
    expect(translateRichTextCalcFields('<p>demo</p>', [])).toBe(
      '<p>demo</p>',
    );
  });
});
