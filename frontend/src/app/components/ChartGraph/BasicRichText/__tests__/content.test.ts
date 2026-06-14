import { describe, expect, test } from 'vitest';
import { normalizeRichTextValue, parseRichTextContent } from '../content';

describe('normalizeRichTextValue', () => {
  test('should keep plain string values', () => {
    expect(normalizeRichTextValue('<p>demo</p>')).toBe('<p>demo</p>');
  });

  test('should keep delta-like values with ops list', () => {
    const delta = { ops: [{ insert: 'demo' }] };

    expect(normalizeRichTextValue(delta)).toEqual(delta);
  });

  test('should fallback invalid object values to empty string', () => {
    expect(normalizeRichTextValue({ foo: 'bar' })).toBe('');
    expect(normalizeRichTextValue(null)).toBe('');
  });
});

describe('parseRichTextContent', () => {
  test('should parse serialized delta values', () => {
    expect(parseRichTextContent('{"ops":[{"insert":"demo"}]}')).toEqual({
      ops: [{ insert: 'demo' }],
    });
  });

  test('should fallback invalid json to original string value', () => {
    expect(parseRichTextContent('<p>demo</p>')).toBe('<p>demo</p>');
  });

  test('should keep plain json-like primitive text values', () => {
    expect(parseRichTextContent('123')).toBe('123');
    expect(parseRichTextContent('true')).toBe('true');
    expect(parseRichTextContent('null')).toBe('null');
  });

  test('should keep non-delta json array text values', () => {
    expect(parseRichTextContent('["demo"]')).toBe('');
  });

  test('should fallback invalid parsed object to empty string', () => {
    expect(parseRichTextContent('{"foo":"bar"}')).toBe('');
  });
});
