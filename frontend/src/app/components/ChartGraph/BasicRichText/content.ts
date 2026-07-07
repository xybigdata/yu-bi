import DOMPurify from 'dompurify';
import type { DeltaStatic } from './quillCompat';

type DeltaLike = {
  ops?: unknown;
};

const isDeltaStatic = (value: unknown): value is DeltaStatic => {
  return (
    !!value &&
    typeof value === 'object' &&
    Array.isArray((value as DeltaLike).ops)
  );
};

const looksLikeStructuredRichText = (value: string) => {
  const trimmed = value.trim();
  return trimmed.startsWith('{') || trimmed.startsWith('[');
};

export const normalizeRichTextValue = (
  value: unknown,
): DeltaStatic | string => {
  if (typeof value === 'string') {
    return sanitizeRichTextHtml(value);
  }

  if (isDeltaStatic(value)) {
    return value;
  }

  return '';
};

export const sanitizeRichTextHtml = (value: string): string => {
  if (!value) {
    return value;
  }

  return DOMPurify.sanitize(value, {
    USE_PROFILES: { html: true },
  });
};

export const parseRichTextContent = (
  value?: string | null,
): DeltaStatic | string => {
  if (!value) {
    return '';
  }

  if (!looksLikeStructuredRichText(value)) {
    return sanitizeRichTextHtml(value);
  }

  try {
    return normalizeRichTextValue(JSON.parse(value));
  } catch {
    return sanitizeRichTextHtml(value);
  }
};
