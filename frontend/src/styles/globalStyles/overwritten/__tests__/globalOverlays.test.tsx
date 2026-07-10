import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, test } from 'vitest';

describe('Global overlay styles', () => {
  test('should keep shared modal chrome aligned with Datart', () => {
    const source = readFileSync(
      resolve(
        process.cwd(),
        'src/styles/globalStyles/overwritten/globalOverlays.ts',
      ),
      'utf8',
    );

    expect(source).toContain('DATART_MODAL_SHADOW');
    expect(source).toContain('.ant-modal.yubi-plain-modal');
    expect(source).toContain('.ant-modal.yubi-form-modal');
    expect(source).toContain('.ant-modal.yubi-state-modal');
    expect(source).toContain('.ant-modal-confirm .ant-modal-container');
    expect(source).toContain('border-radius: 2px');
    expect(source).toContain('SPACE_TIMES(8)');
    expect(source).toContain('SPACE_TIMES(6)');
    expect(source).toContain('theme.componentBackground');
  });

  test('should remove menu borders in shared popup overlays', () => {
    const source = readFileSync(
      resolve(
        process.cwd(),
        'src/styles/globalStyles/overwritten/globalOverlays.ts',
      ),
      'utf8',
    );

    expect(source).toMatch(
      /\.yubi-popup[\s\S]*\.ant-dropdown-menu,\s*\.ant-menu[\s\S]*border-inline-end: 0 !important;/,
    );
    expect(source).toMatch(
      /\.yubi-popup[\s\S]*\.ant-dropdown-menu,\s*\.ant-menu[\s\S]*border-right: 0 !important;/,
    );
  });
});
