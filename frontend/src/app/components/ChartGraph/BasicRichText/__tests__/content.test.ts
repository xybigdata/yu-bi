import { describe, expect, test } from 'vitest';
import {
  normalizeRichTextValue,
  parseRichTextContent,
  sanitizeRichTextHtml,
} from '../content';

describe('normalizeRichTextValue', () => {
  test('should keep plain string values', () => {
    expect(normalizeRichTextValue('<p>demo</p>')).toBe('<p>demo</p>');
  });

  test('should sanitize unsafe plain html values', () => {
    const normalized = normalizeRichTextValue(
      '<img src=x onerror=alert(1)><p onclick=alert(2)>demo<script>alert(3)</script></p>',
    );

    expect(normalized).toContain('<img src="x">');
    expect(normalized).toContain('<p>demo</p>');
    expect(normalized).not.toContain('onerror');
    expect(normalized).not.toContain('onclick');
    expect(normalized).not.toContain('<script>');
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

  test('should sanitize invalid json html fallback', () => {
    expect(
      parseRichTextContent(
        '<p onclick=alert(1)>demo<script>alert(2)</script></p>',
      ),
    ).toBe('<p>demo</p>');
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

describe('sanitizeRichTextHtml', () => {
  test('should strip script and event handlers from legacy html', () => {
    const sanitized = sanitizeRichTextHtml(
      '<a href="javascript:alert(1)" onclick="alert(2)">link</a><script>alert(3)</script>',
    );

    expect(sanitized).toBe('<a>link</a>');
  });
});
