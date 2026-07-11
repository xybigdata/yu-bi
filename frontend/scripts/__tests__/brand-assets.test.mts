import { readFile } from 'node:fs/promises';
import path from 'node:path';

import { describe, expect, it } from 'vitest';

const appRoot = process.cwd();
const htmlEntries = [
  'index.html',
  'shareChart.html',
  'shareDashboard.html',
  'shareStoryPlayer.html',
];

const readPngDimensions = async (relativePath: string) => {
  const image = await readFile(path.resolve(appRoot, relativePath));

  expect(image.subarray(0, 8)).toEqual(
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
  );

  return {
    width: image.readUInt32BE(16),
    height: image.readUInt32BE(20),
  };
};

const readIcoSizes = async (relativePath: string) => {
  const icon = await readFile(path.resolve(appRoot, relativePath));
  const count = icon.readUInt16LE(4);

  return Array.from({ length: count }, (_, index) => {
    const offset = 6 + index * 16;
    const width = icon[offset] || 256;
    const height = icon[offset + 1] || 256;
    return `${width}x${height}`;
  });
};

describe('brand assets', () => {
  it('connects every HTML entry to the shared browser assets', async () => {
    const entries = await Promise.all(
      htmlEntries.map(entry => readFile(path.resolve(appRoot, entry), 'utf8')),
    );

    entries.forEach(html => {
      expect(html).toContain('href="/favicon.ico" sizes="any"');
      expect(html).toContain(
        'rel="apple-touch-icon" href="/icons/apple-touch-icon.png"',
      );
      expect(html).toContain('content="#fff3ee"');
      expect(html).toContain('href="/manifest.json"');
    });
  });

  it('declares the complete PWA icon set', async () => {
    const manifest = JSON.parse(
      await readFile(
        path.resolve(appRoot, 'public/manifest.json'),
        'utf8',
      ),
    );

    expect(manifest.icons).toEqual([
      {
        src: '/icons/icon-192.png',
        sizes: '192x192',
        type: 'image/png',
        purpose: 'any',
      },
      {
        src: '/icons/icon-512.png',
        sizes: '512x512',
        type: 'image/png',
        purpose: 'any',
      },
      {
        src: '/icons/icon-maskable-512.png',
        sizes: '512x512',
        type: 'image/png',
        purpose: 'maskable',
      },
    ]);
    expect(manifest.theme_color).toBe('#fff3ee');
    expect(manifest.background_color).toBe('#fffaf7');
  });

  it('keeps raster assets at their declared dimensions', async () => {
    await expect(
      readPngDimensions('public/icons/apple-touch-icon.png'),
    ).resolves.toEqual({ width: 180, height: 180 });
    await expect(
      readPngDimensions('public/icons/icon-192.png'),
    ).resolves.toEqual({ width: 192, height: 192 });
    await expect(
      readPngDimensions('public/icons/icon-512.png'),
    ).resolves.toEqual({ width: 512, height: 512 });
    await expect(
      readPngDimensions('public/icons/icon-maskable-512.png'),
    ).resolves.toEqual({ width: 512, height: 512 });
  });

  it('contains every supported favicon size', async () => {
    await expect(readIcoSizes('public/favicon.ico')).resolves.toEqual([
      '16x16',
      '32x32',
      '48x48',
      '64x64',
      '128x128',
      '256x256',
    ]);
  });

  it('uses the transparent brand mark in every product logo surface', async () => {
    const [logo, brand, navbar] = await Promise.all([
      readFile(
        path.resolve(appRoot, 'src/app/assets/images/logo.svg'),
        'utf8',
      ),
      readFile(
        path.resolve(appRoot, 'src/app/components/Brand/Brand.tsx'),
        'utf8',
      ),
      readFile(
        path.resolve(appRoot, 'src/app/pages/MainPage/Navbar/index.tsx'),
        'utf8',
      ),
    ]);

    expect(logo).toMatch(/viewBox=['"]0 0 1024 1024['"]/);
    expect(logo).toMatch(/aria-label=['"]yu-bi brand mark['"]/);
    expect(logo).not.toContain('<rect');
    expect(brand).toContain("app/assets/images/logo.svg");
    expect(navbar).toContain("app/assets/images/logo.svg");
  });
});
