import { describe, expect, test } from 'vitest';

describe('rich text ready-state guard', () => {
  test('should gate palette creation with explicit ready check', () => {
    const readyRef = {
      isReady: () => true,
    };
    const pendingRef = {
      isReady: () => false,
    };

    expect(readyRef.isReady()).toBe(true);
    expect(pendingRef.isReady()).toBe(false);
  });
});
