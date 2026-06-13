const clamp = (value: number, min: number, max: number) => {
  return Math.min(Math.max(value, min), max);
};

const normalizeHex = (value: string) => {
  const hex = value.trim().replace(/^#/, '');

  if (hex.length === 3 || hex.length === 4) {
    return hex
      .split('')
      .map(char => char + char)
      .join('');
  }

  return hex;
};

const parseHexColor = (value: string) => {
  const normalizedHex = normalizeHex(value);

  if (![6, 8].includes(normalizedHex.length)) {
    return null;
  }

  if (!/^[0-9a-fA-F]+$/.test(normalizedHex)) {
    return null;
  }

  const rgbHex = normalizedHex.slice(0, 6);
  const alphaHex = normalizedHex.slice(6, 8);

  return {
    r: parseInt(rgbHex.slice(0, 2), 16),
    g: parseInt(rgbHex.slice(2, 4), 16),
    b: parseInt(rgbHex.slice(4, 6), 16),
    a: alphaHex ? parseInt(alphaHex, 16) / 255 : 1,
  };
};

const parseRgbColor = (value: string) => {
  const match = value
    .trim()
    .match(
      /^rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)(?:\s*,\s*([\d.]+)\s*)?\)$/i,
    );

  if (!match) {
    return null;
  }

  const [, r, g, b, a] = match;

  return {
    r: clamp(Number(r), 0, 255),
    g: clamp(Number(g), 0, 255),
    b: clamp(Number(b), 0, 255),
    a: clamp(a === undefined ? 1 : Number(a), 0, 1),
  };
};

export const parseColorString = (value?: string) => {
  if (!value) {
    return {
      r: 0,
      g: 0,
      b: 0,
      a: 1,
    };
  }

  const normalizedValue = value.trim();

  if (!normalizedValue) {
    return {
      r: 0,
      g: 0,
      b: 0,
      a: 1,
    };
  }

  if (normalizedValue.toLowerCase() === 'transparent') {
    return {
      r: 0,
      g: 0,
      b: 0,
      a: 0,
    };
  }

  if (normalizedValue.startsWith('#')) {
    return parseHexColor(normalizedValue);
  }

  return parseRgbColor(normalizedValue);
};

export const toHexColor = (color: { r: number; g: number; b: number }) => {
  const toHex = (value: number) => {
    return clamp(Math.round(value), 0, 255).toString(16).padStart(2, '0');
  };

  return `#${toHex(color.r)}${toHex(color.g)}${toHex(color.b)}`.toUpperCase();
};

export const toRgbaColor = (color: {
  r: number;
  g: number;
  b: number;
  a: number;
}) => {
  const alpha = clamp(Number(color.a.toFixed(2)), 0, 1);

  return `rgba(${Math.round(color.r)}, ${Math.round(color.g)}, ${Math.round(
    color.b,
  )}, ${alpha})`;
};
