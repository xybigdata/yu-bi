import { afterEach, describe, expect, test, vi } from 'vitest';
import {
  __resetSqlFormatterLoaderForTest,
  __setSqlFormatterLoaderForTest,
  type SqlFormatterModule,
} from '../sqlFormatterRuntime';
import { formatSqlScript } from '../formatSqlScript';

describe('formatSqlScript', () => {
  afterEach(() => {
    __resetSqlFormatterLoaderForTest();
  });

  test('should format sql script with shared toolbar options', async () => {
    const format = vi.fn((sql: string) => `formatted:${sql}`);
    __setSqlFormatterLoaderForTest(
      vi.fn().mockResolvedValue({
        format,
      } as unknown as SqlFormatterModule),
    );

    await expect(formatSqlScript('select id from users')).resolves.toBe(
      'formatted:select id from users',
    );
    expect(format).toHaveBeenCalledWith('select id from users', {
      denseOperators: true,
      logicalOperatorNewline: 'before',
    });
  });

  test('should propagate formatter loading errors', async () => {
    __setSqlFormatterLoaderForTest(
      vi.fn().mockRejectedValue(new Error('load failed')),
    );

    await expect(formatSqlScript('select 1')).rejects.toThrow('load failed');
  });
});
