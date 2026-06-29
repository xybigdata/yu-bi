/**
 * Bug Condition Exploration Test: minSize Array Referential Equality Regression
 *
 * **Validates: Requirements 1.1, 2.1**
 *
 * Bug condition from design:
 *   isBugCondition(input) where prevMinSize !== nextMinSize AND arrayValuesEqual(prevMinSize, nextMinSize)
 *
 * This test encodes the EXPECTED (correct) behavior:
 *   When a parent re-renders passing a new minSize array reference with identical values,
 *   the Split.js instance should NOT be destroyed/recreated.
 *
 * On UNFIXED code, this test is expected to FAIL because the useEffect dependency array
 * includes `minSize` with referential equality — a new [100, 200] array reference triggers
 * the effect, causing destroy(true, true) → loadSplit() → new instance.
 *
 * Counterexample: Re-render with new `[100, 200]` array reference triggers unnecessary
 * `destroy(true, true)` → `loadSplit()` cycle.
 */
import { render, waitFor } from '@testing-library/react';
import React from 'react';
import { afterEach, describe, expect, test, vi } from 'vitest';
import Split from '../Split';

const runtimeMock = vi.hoisted(() => ({
  loadSplit: vi.fn(),
}));

vi.mock('../splitRuntime', () => ({
  loadSplit: runtimeMock.loadSplit,
}));

describe('Split minSize referential equality bug condition', () => {
  afterEach(() => {
    runtimeMock.loadSplit.mockReset();
    vi.restoreAllMocks();
  });

  /**
   * Property 1: Bug Condition - minSize Array Referential Equality Regression
   *
   * GIVEN a Split component rendered with minSize=[100, 200]
   * WHEN the parent re-renders passing a NEW array reference [100, 200] (same values)
   * THEN the Split.js instance should NOT be destroyed and recreated
   *
   * Bug condition: prevMinSize !== nextMinSize AND arrayValuesEqual(prevMinSize, nextMinSize)
   * Expected: destroy() is NOT called (no unnecessary recreation)
   * Actual on unfixed code: destroy(true, true) IS called (confirming the bug)
   */
  test('should NOT destroy Split.js instance when re-rendered with new minSize array reference containing identical values', async () => {
    const splitInstance = {
      collapse: vi.fn(),
      destroy: vi.fn(),
      getSizes: vi.fn(() => [50, 50]),
      setSizes: vi.fn(),
    };
    const splitFactory = vi.fn(() => splitInstance);
    runtimeMock.loadSplit.mockResolvedValue(splitFactory);

    // Initial render with minSize=[100, 200]
    const { rerender } = render(
      <Split sizes={[50, 50]} minSize={[100, 200]}>
        <div>left</div>
        <div>right</div>
      </Split>,
    );

    // Wait for initial Split.js instance creation
    await waitFor(() => {
      expect(splitFactory).toHaveBeenCalledTimes(1);
    });

    // Clear tracking after initial mount
    splitInstance.destroy.mockClear();
    splitFactory.mockClear();

    // Re-render with a NEW array reference [100, 200] — same values, different object
    // This simulates the common pattern: parent re-renders with <Split minSize={[100, 200]}>
    // where the array literal creates a new reference on each render
    const newMinSizeRef = [100, 200];
    rerender(
      <Split sizes={[50, 50]} minSize={newMinSizeRef}>
        <div>left</div>
        <div>right</div>
      </Split>,
    );

    // Allow any async effects to settle
    await new Promise(resolve => setTimeout(resolve, 50));

    // EXPECTED BEHAVIOR (post-fix): destroy should NOT be called because values are identical
    // BUG BEHAVIOR (pre-fix): destroy IS called because React sees different reference in deps
    expect(splitInstance.destroy).not.toHaveBeenCalled();
    expect(splitFactory).not.toHaveBeenCalled();
  });
});
