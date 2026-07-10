import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, test } from 'vitest';

describe('Global form styles', () => {
  test('should override AntD 6 form label color through the shared theme token', () => {
    const source = readFileSync(
      resolve(process.cwd(), 'src/styles/globalStyles/overwritten/form.ts'),
      'utf8',
    );

    expect(source).toContain('--ant-form-label-color');
    expect(source).toContain('theme.textColorLight');
    expect(source).toContain('!important');
    expect(source).toContain('.ant-form-item-label > label');
  });
});
