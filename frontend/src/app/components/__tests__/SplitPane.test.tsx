import { fireEvent, render } from '@testing-library/react';
import React from 'react';
import { describe, expect, test, vi } from 'vitest';
import { SplitPane } from '../SplitPane/index';

describe('SplitPane', () => {
  test('renders two panes with default size', () => {
    const { container } = render(
      <SplitPane defaultSize={200} split="vertical">
        <div>pane1</div>
        <div>pane2</div>
      </SplitPane>,
    );

    const pane1 = container.querySelector('.Pane1');
    expect(pane1).not.toBeNull();
    expect(pane1!.getAttribute('style')).toContain('width: 200px');
  });

  test('controlled size prop updates pane', () => {
    const { container, rerender } = render(
      <SplitPane size={200} split="vertical">
        <div>pane1</div>
        <div>pane2</div>
      </SplitPane>,
    );

    const pane1 = container.querySelector('.Pane1');
    expect(pane1!.getAttribute('style')).toContain('width: 200px');

    rerender(
      <SplitPane size={300} split="vertical">
        <div>pane1</div>
        <div>pane2</div>
      </SplitPane>,
    );

    expect(pane1!.getAttribute('style')).toContain('width: 300px');
  });

  test('primary="second" assigns size to pane2', () => {
    const { container } = render(
      <SplitPane defaultSize={250} split="vertical" primary="second">
        <div>pane1</div>
        <div>pane2</div>
      </SplitPane>,
    );

    const pane1 = container.querySelector('.Pane1');
    const pane2 = container.querySelector('.Pane2');
    // pane1 should not have a fixed width when primary is second
    expect(pane1!.getAttribute('style')).not.toContain('width: 250px');
    // pane2 should have the size
    expect(pane2!.getAttribute('style')).toContain('width: 250px');
  });

  test('respects minSize constraint during drag', () => {
    const { container } = render(
      <SplitPane defaultSize={200} split="vertical" minSize={100}>
        <div>pane1</div>
        <div>pane2</div>
      </SplitPane>,
    );

    const resizer = container.querySelector('.Resizer');
    expect(resizer).not.toBeNull();

    // Mock getBoundingClientRect on pane1 and pane2
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

    // Simulate drag: mousedown on resizer, then mousemove far left to try to go below minSize
    fireEvent.mouseDown(resizer!, { clientX: 200, clientY: 100 });
    // Move mouse far to the right (which makes pane1 smaller due to positionDelta = start - current)
    // positionDelta = 200 - 50 = 150 (positive), sizeDelta for primary=first = 150
    // newSize = 200 - 150 = 50, but minSize=100 so it should clamp to 100
    fireEvent.mouseMove(document, { clientX: 50, clientY: 100 });

    // pane1 size should be clamped at minSize (100), not 50
    expect(pane1.getAttribute('style')).toContain('width: 100px');
  });

  test('calls onChange during drag', () => {
    const onChange = vi.fn();
    const { container } = render(
      <SplitPane defaultSize={200} split="vertical" onChange={onChange}>
        <div>pane1</div>
        <div>pane2</div>
      </SplitPane>,
    );

    const resizer = container.querySelector('.Resizer');
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

    // Start drag
    fireEvent.mouseDown(resizer!, { clientX: 200, clientY: 100 });
    // Move mouse to the left by 50px (positionDelta = 200 - 150 = 50, sizeDelta = 50)
    // newSize = 200 - 50 = 150
    fireEvent.mouseMove(document, { clientX: 150, clientY: 100 });

    expect(onChange).toHaveBeenCalledWith(150);
  });
});
