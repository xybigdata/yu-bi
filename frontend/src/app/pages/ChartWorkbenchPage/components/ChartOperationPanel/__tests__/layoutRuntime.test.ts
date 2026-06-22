import { describe, expect, test } from 'vitest';
import {
  getLayoutNodeRectSize,
  getStableContainerSize,
  normalizeLayoutComponentType,
} from '../layoutRuntime';
import { LayoutComponentType } from '../ChartOperationPanelLayout';

describe('getLayoutNodeRectSize', () => {
  test('should fallback to zero when layout node is missing', () => {
    expect(
      getLayoutNodeRectSize(
        {
          getNodeById: () => undefined,
        },
        'present-wrapper',
      ),
    ).toEqual({
      width: 0,
      height: 0,
    });
  });

  test('should return node rect size when node exists', () => {
    expect(
      getLayoutNodeRectSize(
        {
          getNodeById: () => ({
            getRect: () => ({
              width: 640,
              height: 480,
            }),
          }),
        },
        'present-wrapper',
      ),
    ).toEqual({
      width: 640,
      height: 480,
    });
  });

  test('should normalize invalid and negative rect sizes to zero', () => {
    expect(
      getLayoutNodeRectSize(
        {
          getNodeById: () => ({
            getRect: () => ({
              width: Number.NaN,
              height: -12.8,
            }),
          }),
        },
        'present-wrapper',
      ),
    ).toEqual({
      width: 0,
      height: 0,
    });
  });

  test('should floor float rect sizes into stable layout units', () => {
    expect(
      getLayoutNodeRectSize(
        {
          getNodeById: () => ({
            getRect: () => ({
              width: 640.9,
              height: 480.4,
            }),
          }),
        },
        'present-wrapper',
      ),
    ).toEqual({
      width: 640,
      height: 480,
    });
  });
});

describe('getStableContainerSize', () => {
  test('should subtract reserved size and keep non-negative result', () => {
    expect(getStableContainerSize(120, 24)).toBe(96);
    expect(getStableContainerSize(20, 24)).toBe(0);
  });

  test('should normalize invalid input before subtraction', () => {
    expect(getStableContainerSize(Number.NaN, 24)).toBe(0);
    expect(getStableContainerSize(100.9, 20.4)).toBe(80);
    expect(getStableContainerSize(Infinity, 10)).toBe(0);
  });
});

describe('normalizeLayoutComponentType', () => {
  test('should keep known layout component type', () => {
    expect(
      normalizeLayoutComponentType(LayoutComponentType.PRESENT),
    ).toBe(LayoutComponentType.PRESENT);
  });

  test('should reject unknown layout component type', () => {
    expect(normalizeLayoutComponentType('unknown-component')).toBeUndefined();
    expect(normalizeLayoutComponentType(undefined)).toBeUndefined();
  });
});

describe('layout package runtime', () => {
  test('should load actual flexlayout-react runtime exports', async () => {
    const runtimeModule = await import('flexlayout-react');

    expect(runtimeModule.Layout).toEqual(expect.any(Object));
    expect(runtimeModule.Model.fromJson).toEqual(expect.any(Function));
    expect(runtimeModule.TabNode).toEqual(expect.any(Function));
  });

  test('should load actual react-grid-layout legacy runtime exports', async () => {
    const runtimeModule = await import('react-grid-layout/legacy');

    expect(runtimeModule.default).toEqual(expect.any(Function));
    expect(runtimeModule.WidthProvider).toEqual(expect.any(Function));
  });
});
