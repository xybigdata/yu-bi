import { describe, expect, test } from 'vitest';

const expectComponentExport = (component: unknown) => {
  const isComponentFunction = typeof component === 'function';
  const isMemoComponent =
    typeof component === 'object' &&
    component !== null &&
    '$$typeof' in component;

  expect(isComponentFunction || isMemoComponent).toBe(true);
};

describe('drag and drop runtime packages', () => {
  test('should load actual react-dnd runtime exports', async () => {
    const runtimeModule = await import('react-dnd');

    expectComponentExport(runtimeModule.DndProvider);
    expect(runtimeModule.useDrag).toEqual(expect.any(Function));
    expect(runtimeModule.useDrop).toEqual(expect.any(Function));
    expect(runtimeModule.useDragLayer).toEqual(expect.any(Function));
  });

  test('should load actual react-dnd HTML5 backend export', async () => {
    const runtimeModule = await import('react-dnd-html5-backend');

    expect(runtimeModule.HTML5Backend).toEqual(expect.any(Function));
  });

  test('should load actual hello-pangea dnd runtime exports', async () => {
    const runtimeModule = await import('@hello-pangea/dnd');

    expect(runtimeModule.DragDropContext).toEqual(expect.any(Function));
    expectComponentExport(runtimeModule.Droppable);
    expectComponentExport(runtimeModule.Draggable);
  });
});
