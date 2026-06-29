/**
 * Preservation Property Tests: Split.js Recreation on Actual Value Changes
 * and SplitPane Drag Behavior
 *
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.5, 3.6**
 *
 * These tests capture EXISTING CORRECT behaviors that must NOT regress after
 * the minSize stabilization fix is applied. They must PASS on UNFIXED code.
 *
 * Property 2: Preservation
 * - Part A: Split minSize value change preservation (recreation on actual changes)
 * - Part B: SplitPane drag behavior preservation (state tracking without resizedRef)
 * - Part C: Collapse preservation (collapse called after recreation)
 */
import { fireEvent, render, waitFor } from '@testing-library/react';
import * as fc from 'fast-check';
import React from 'react';
import { afterEach, describe, expect, test, vi } from 'vitest';
import Split from '../Split';
import { SplitPane } from '../SplitPane/index';

const runtimeMock = vi.hoisted(() => ({
  loadSplit: vi.fn(),
}));

vi.mock('../splitRuntime', () => ({
  loadSplit: runtimeMock.loadSplit,
}));

/**
 * Part A - Split minSize value change preservation
 *
 * Observation on UNFIXED code:
 * - render `<Split minSize={[100, 200]}>` then re-render with `[150, 200]` → destroy() IS called (correct)
 * - render `<Split minSize={[100, 200]}>` then re-render with changed gutterSize → destroy() IS called (correct)
 */
describe('Part A - Split minSize value change preservation', () => {
  afterEach(() => {
    runtimeMock.loadSplit.mockReset();
    vi.restoreAllMocks();
  });

  /**
   * Property-based test: for all pairs of minSize arrays where at least one
   * element differs, assert Split.js instance IS recreated.
   *
   * **Validates: Requirements 3.1, 3.2**
   */
  test('Split.js instance IS recreated when minSize array values actually change (property-based)', async () => {
    await fc.assert(
      fc.asyncProperty(
        // Generate a pair of minSize arrays where at least one element differs
        fc
          .tuple(
            fc.array(fc.integer({ min: 0, max: 1000 }), {
              minLength: 1,
              maxLength: 5,
            }),
            fc.array(fc.integer({ min: 0, max: 1000 }), {
              minLength: 1,
              maxLength: 5,
            }),
          )
          .filter(([a, b]) => {
            // Ensure at least one element differs (different lengths or different values)
            if (a.length !== b.length) return true;
            return a.some((val, i) => val !== b[i]);
          }),
        async ([initialMinSize, newMinSize]) => {
          runtimeMock.loadSplit.mockReset();

          const splitInstance = {
            collapse: vi.fn(),
            destroy: vi.fn(),
            getSizes: vi.fn(() => [50, 50]),
            setSizes: vi.fn(),
          };
          const splitFactory = vi.fn(() => splitInstance);
          runtimeMock.loadSplit.mockResolvedValue(splitFactory);

          // Generate matching number of child elements
          const childCount = Math.max(initialMinSize.length, 2);
          const children = Array.from({ length: childCount }, (_, i) => (
            <div key={i}>pane{i}</div>
          ));

          const { rerender, unmount } = render(
            <Split sizes={[50, 50]} minSize={initialMinSize}>
              {children}
            </Split>,
          );

          await waitFor(() => {
            expect(splitFactory).toHaveBeenCalledTimes(1);
          });

          // Clear tracking after initial mount
          splitInstance.destroy.mockClear();
          splitFactory.mockClear();

          // Re-render with actually different minSize values
          rerender(
            <Split sizes={[50, 50]} minSize={newMinSize}>
              {children}
            </Split>,
          );

          // Wait for async recreation
          await waitFor(() => {
            expect(splitInstance.destroy).toHaveBeenCalled();
          });

          // Verify recreation occurred
          expect(splitFactory).toHaveBeenCalledTimes(1);

          unmount();
        },
      ),
      { numRuns: 20 },
    );
  });

  /**
   * Property-based test: for all changes to other effect dependencies
   * (maxSize, gutterSize, direction), assert Split.js instance IS recreated.
   *
   * **Validates: Requirements 3.2**
   */
  test('Split.js instance IS recreated when other effect dependencies change (property-based)', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc
          .oneof(
            // gutterSize change
            fc.record({
              prop: fc.constant('gutterSize' as const),
              initial: fc.integer({ min: 1, max: 20 }),
              next: fc.integer({ min: 1, max: 20 }),
            }),
            // direction change
            fc.record({
              prop: fc.constant('direction' as const),
              initial: fc.constant('horizontal' as const),
              next: fc.constant('vertical' as const),
            }),
            // maxSize change (number value)
            fc.record({
              prop: fc.constant('maxSize' as const),
              initial: fc.integer({ min: 100, max: 1000 }),
              next: fc.integer({ min: 100, max: 1000 }),
            }),
          )
          .filter(({ initial, next }) => initial !== next),
        async ({ prop, initial, next }) => {
          runtimeMock.loadSplit.mockReset();

          const splitInstance = {
            collapse: vi.fn(),
            destroy: vi.fn(),
            getSizes: vi.fn(() => [50, 50]),
            setSizes: vi.fn(),
          };
          const splitFactory = vi.fn(() => splitInstance);
          runtimeMock.loadSplit.mockResolvedValue(splitFactory);

          const initialProps = { [prop]: initial };
          const nextProps = { [prop]: next };

          const { rerender, unmount } = render(
            <Split sizes={[50, 50]} {...initialProps}>
              <div>left</div>
              <div>right</div>
            </Split>,
          );

          await waitFor(() => {
            expect(splitFactory).toHaveBeenCalledTimes(1);
          });

          // Clear tracking after initial mount
          splitInstance.destroy.mockClear();
          splitFactory.mockClear();

          // Re-render with changed dependency
          rerender(
            <Split sizes={[50, 50]} {...nextProps}>
              <div>left</div>
              <div>right</div>
            </Split>,
          );

          // Wait for async recreation
          await waitFor(() => {
            expect(splitInstance.destroy).toHaveBeenCalled();
          });

          // Verify recreation occurred
          expect(splitFactory).toHaveBeenCalledTimes(1);

          unmount();
        },
      ),
      { numRuns: 15 },
    );
  });

  /**
   * Concrete test: render with minSize=[100, 200] then re-render with [150, 200]
   * → destroy() IS called (verifies observation on unfixed code).
   *
   * **Validates: Requirements 3.1**
   */
  test('Split.js instance IS recreated when minSize changes from [100,200] to [150,200]', async () => {
    const splitInstance = {
      collapse: vi.fn(),
      destroy: vi.fn(),
      getSizes: vi.fn(() => [50, 50]),
      setSizes: vi.fn(),
    };
    const splitFactory = vi.fn(() => splitInstance);
    runtimeMock.loadSplit.mockResolvedValue(splitFactory);

    const { rerender } = render(
      <Split sizes={[50, 50]} minSize={[100, 200]}>
        <div>left</div>
        <div>right</div>
      </Split>,
    );

    await waitFor(() => {
      expect(splitFactory).toHaveBeenCalledTimes(1);
    });

    splitInstance.destroy.mockClear();
    splitFactory.mockClear();

    // Re-render with actually different values
    rerender(
      <Split sizes={[50, 50]} minSize={[150, 200]}>
        <div>left</div>
        <div>right</div>
      </Split>,
    );

    await waitFor(() => {
      expect(splitInstance.destroy).toHaveBeenCalledWith(true, true);
      expect(splitFactory).toHaveBeenCalledTimes(1);
    });
  });

  /**
   * Concrete test: render with gutterSize=8 then change to gutterSize=12
   * → destroy() IS called (verifies observation on unfixed code).
   *
   * **Validates: Requirements 3.2**
   */
  test('Split.js instance IS recreated when gutterSize changes', async () => {
    const splitInstance = {
      collapse: vi.fn(),
      destroy: vi.fn(),
      getSizes: vi.fn(() => [50, 50]),
      setSizes: vi.fn(),
    };
    const splitFactory = vi.fn(() => splitInstance);
    runtimeMock.loadSplit.mockResolvedValue(splitFactory);

    const { rerender } = render(
      <Split sizes={[50, 50]} minSize={[100, 200]} gutterSize={8}>
        <div>left</div>
        <div>right</div>
      </Split>,
    );

    await waitFor(() => {
      expect(splitFactory).toHaveBeenCalledTimes(1);
    });

    splitInstance.destroy.mockClear();
    splitFactory.mockClear();

    rerender(
      <Split sizes={[50, 50]} minSize={[100, 200]} gutterSize={12}>
        <div>left</div>
        <div>right</div>
      </Split>,
    );

    await waitFor(() => {
      expect(splitInstance.destroy).toHaveBeenCalledWith(true, true);
      expect(splitFactory).toHaveBeenCalledTimes(1);
    });
  });
});

/**
 * Part B - SplitPane drag behavior preservation
 *
 * Observation on UNFIXED code:
 * - drag operations correctly update activeRef, positionRef, and draggedSizeRef
 * - resizedRef is written but never read — removing it must not affect drag behavior
 */
describe('Part B - SplitPane drag behavior preservation', () => {
  /**
   * Test: SplitPane drag (mousedown → mousemove → mouseup) correctly tracks state
   * via activeRef, positionRef, draggedSizeRef without relying on resizedRef.
   *
   * **Validates: Requirements 3.5**
   */
  test('drag operation correctly updates pane size and calls onDragFinished with draggedSize', () => {
    const onDragStarted = vi.fn();
    const onDragFinished = vi.fn();
    const onChange = vi.fn();

    const { container } = render(
      <SplitPane
        defaultSize={200}
        split="vertical"
        onDragStarted={onDragStarted}
        onDragFinished={onDragFinished}
        onChange={onChange}
      >
        <div>pane1</div>
        <div>pane2</div>
      </SplitPane>,
    );

    const resizer = container.querySelector('.Resizer')!;
    const pane1 = container.querySelector('.Pane1') as HTMLElement;
    const pane2 = container.querySelector('.Pane2') as HTMLElement;

    vi.spyOn(pane1, 'getBoundingClientRect').mockReturnValue({
      width: 200,
      height: 400,
      top: 0,
      left: 0,
      right: 200,
      bottom: 400,
      x: 0,
      y: 0,
      toJSON: () => {},
    });
    vi.spyOn(pane2, 'getBoundingClientRect').mockReturnValue({
      width: 400,
      height: 400,
      top: 0,
      left: 200,
      right: 600,
      bottom: 400,
      x: 200,
      y: 0,
      toJSON: () => {},
    });

    // mousedown → activates drag (sets activeRef=true, positionRef=200)
    fireEvent.mouseDown(resizer, { clientX: 200, clientY: 100 });
    expect(onDragStarted).toHaveBeenCalledTimes(1);

    // mousemove → updates position, calculates new size, updates draggedSizeRef
    // positionDelta = 200 - 250 = -50, sizeDelta (primary=first) = -50
    // newSize = 200 - (-50) = 250
    fireEvent.mouseMove(document, { clientX: 250, clientY: 100 });
    expect(onChange).toHaveBeenCalledWith(250);
    expect(pane1.getAttribute('style')).toContain('width: 250px');

    // mouseup → deactivates drag, reports draggedSize
    fireEvent.mouseUp(document);
    expect(onDragFinished).toHaveBeenCalledWith(250);
  });

  /**
   * Test: multiple sequential drags work correctly (activeRef reset between drags).
   *
   * **Validates: Requirements 3.5**
   */
  test('multiple sequential drags work correctly with proper state reset', () => {
    const onDragFinished = vi.fn();
    const onChange = vi.fn();

    const { container } = render(
      <SplitPane
        defaultSize={200}
        split="vertical"
        onDragFinished={onDragFinished}
        onChange={onChange}
      >
        <div>pane1</div>
        <div>pane2</div>
      </SplitPane>,
    );

    const resizer = container.querySelector('.Resizer')!;
    const pane1 = container.querySelector('.Pane1') as HTMLElement;
    const pane2 = container.querySelector('.Pane2') as HTMLElement;

    vi.spyOn(pane1, 'getBoundingClientRect').mockReturnValue({
      width: 200,
      height: 400,
      top: 0,
      left: 0,
      right: 200,
      bottom: 400,
      x: 0,
      y: 0,
      toJSON: () => {},
    });
    vi.spyOn(pane2, 'getBoundingClientRect').mockReturnValue({
      width: 400,
      height: 400,
      top: 0,
      left: 200,
      right: 600,
      bottom: 400,
      x: 200,
      y: 0,
      toJSON: () => {},
    });

    // First drag
    fireEvent.mouseDown(resizer, { clientX: 200, clientY: 100 });
    fireEvent.mouseMove(document, { clientX: 250, clientY: 100 });
    fireEvent.mouseUp(document);
    expect(onDragFinished).toHaveBeenCalledWith(250);

    // After mouseup, moving mouse should NOT change pane size (activeRef=false)
    onChange.mockClear();
    fireEvent.mouseMove(document, { clientX: 300, clientY: 100 });
    // onChange should NOT be called when not actively dragging
    expect(onChange).not.toHaveBeenCalled();

    // Second drag
    // Reset mock for pane1 to reflect new size from first drag
    vi.spyOn(pane1, 'getBoundingClientRect').mockReturnValue({
      width: 250,
      height: 400,
      top: 0,
      left: 0,
      right: 250,
      bottom: 400,
      x: 0,
      y: 0,
      toJSON: () => {},
    });

    fireEvent.mouseDown(resizer, { clientX: 250, clientY: 100 });
    fireEvent.mouseMove(document, { clientX: 230, clientY: 100 });
    // positionDelta = 250 - 230 = 20, sizeDelta = 20
    // newSize = 250 - 20 = 230
    fireEvent.mouseUp(document);
    expect(onDragFinished).toHaveBeenCalledWith(230);
  });

  /**
   * Test: external `size` prop changes still sync pane sizes via existing useEffect.
   *
   * **Validates: Requirements 3.6**
   */
  test('external size prop changes sync pane sizes correctly', () => {
    const { container, rerender } = render(
      <SplitPane size={200} split="vertical">
        <div>pane1</div>
        <div>pane2</div>
      </SplitPane>,
    );

    const pane1 = container.querySelector('.Pane1') as HTMLElement;
    expect(pane1.getAttribute('style')).toContain('width: 200px');

    // External size change
    rerender(
      <SplitPane size={350} split="vertical">
        <div>pane1</div>
        <div>pane2</div>
      </SplitPane>,
    );

    expect(pane1.getAttribute('style')).toContain('width: 350px');

    // Another external change
    rerender(
      <SplitPane size={150} split="vertical">
        <div>pane1</div>
        <div>pane2</div>
      </SplitPane>,
    );

    expect(pane1.getAttribute('style')).toContain('width: 150px');
  });

  /**
   * Test: horizontal split drag behavior preserved.
   *
   * **Validates: Requirements 3.5**
   */
  test('horizontal split drag correctly tracks state', () => {
    const onChange = vi.fn();
    const onDragFinished = vi.fn();

    const { container } = render(
      <SplitPane
        defaultSize={200}
        split="horizontal"
        onChange={onChange}
        onDragFinished={onDragFinished}
      >
        <div>pane1</div>
        <div>pane2</div>
      </SplitPane>,
    );

    const resizer = container.querySelector('.Resizer')!;
    const pane1 = container.querySelector('.Pane1') as HTMLElement;
    const pane2 = container.querySelector('.Pane2') as HTMLElement;

    vi.spyOn(pane1, 'getBoundingClientRect').mockReturnValue({
      width: 400,
      height: 200,
      top: 0,
      left: 0,
      right: 400,
      bottom: 200,
      x: 0,
      y: 0,
      toJSON: () => {},
    });
    vi.spyOn(pane2, 'getBoundingClientRect').mockReturnValue({
      width: 400,
      height: 400,
      top: 200,
      left: 0,
      right: 400,
      bottom: 600,
      x: 0,
      y: 200,
      toJSON: () => {},
    });

    // For horizontal split, use clientY
    fireEvent.mouseDown(resizer, { clientX: 100, clientY: 200 });
    // Move down → positionDelta = 200 - 250 = -50, sizeDelta = -50
    // newSize = 200 - (-50) = 250
    fireEvent.mouseMove(document, { clientX: 100, clientY: 250 });
    expect(onChange).toHaveBeenCalledWith(250);

    fireEvent.mouseUp(document);
    expect(onDragFinished).toHaveBeenCalledWith(250);
  });
});

/**
 * Part C - Collapse preservation
 *
 * After Split.js recreation due to actual value change, collapse() is still
 * called when collapsed prop is set.
 */
describe('Part C - Collapse preservation', () => {
  afterEach(() => {
    runtimeMock.loadSplit.mockReset();
    vi.restoreAllMocks();
  });

  /**
   * Test: after Split.js recreation due to actual value change, collapse()
   * is still called when collapsed prop is set.
   *
   * **Validates: Requirements 3.3**
   */
  test('collapse() is called on new instance after recreation due to actual minSize change', async () => {
    const splitInstance = {
      collapse: vi.fn(),
      destroy: vi.fn(),
      getSizes: vi.fn(() => [50, 50]),
      setSizes: vi.fn(),
    };
    const splitFactory = vi.fn(() => splitInstance);
    runtimeMock.loadSplit.mockResolvedValue(splitFactory);

    const { rerender } = render(
      <Split sizes={[50, 50]} minSize={[100, 200]} collapsed={0}>
        <div>left</div>
        <div>right</div>
      </Split>,
    );

    await waitFor(() => {
      expect(splitFactory).toHaveBeenCalledTimes(1);
    });

    // Initial collapse called during mount
    expect(splitInstance.collapse).toHaveBeenCalledWith(0);
    splitInstance.collapse.mockClear();
    splitInstance.destroy.mockClear();
    splitFactory.mockClear();

    // Re-render with different minSize AND collapsed prop set
    // This triggers recreation → after recreation, collapse should be called
    rerender(
      <Split sizes={[50, 50]} minSize={[150, 200]} collapsed={1}>
        <div>left</div>
        <div>right</div>
      </Split>,
    );

    await waitFor(() => {
      expect(splitInstance.destroy).toHaveBeenCalledWith(true, true);
      expect(splitFactory).toHaveBeenCalledTimes(1);
    });

    // After recreation, collapse should be called with the collapsed value
    await waitFor(() => {
      expect(splitInstance.collapse).toHaveBeenCalledWith(1);
    });
  });

  /**
   * Test: after Split.js recreation due to gutterSize change, collapse()
   * is still called when collapsed prop is set.
   *
   * **Validates: Requirements 3.3**
   */
  test('collapse() is called on new instance after recreation due to gutterSize change', async () => {
    const splitInstance = {
      collapse: vi.fn(),
      destroy: vi.fn(),
      getSizes: vi.fn(() => [50, 50]),
      setSizes: vi.fn(),
    };
    const splitFactory = vi.fn(() => splitInstance);
    runtimeMock.loadSplit.mockResolvedValue(splitFactory);

    const { rerender } = render(
      <Split sizes={[50, 50]} gutterSize={8} collapsed={0}>
        <div>left</div>
        <div>right</div>
      </Split>,
    );

    await waitFor(() => {
      expect(splitFactory).toHaveBeenCalledTimes(1);
    });

    // Initial collapse
    expect(splitInstance.collapse).toHaveBeenCalledWith(0);
    splitInstance.collapse.mockClear();
    splitInstance.destroy.mockClear();
    splitFactory.mockClear();

    // Change gutterSize with collapsed still set
    rerender(
      <Split sizes={[50, 50]} gutterSize={12} collapsed={0}>
        <div>left</div>
        <div>right</div>
      </Split>,
    );

    await waitFor(() => {
      expect(splitInstance.destroy).toHaveBeenCalledWith(true, true);
      expect(splitFactory).toHaveBeenCalledTimes(1);
    });

    // Collapse should still be called after recreation
    await waitFor(() => {
      expect(splitInstance.collapse).toHaveBeenCalledWith(0);
    });
  });
});
