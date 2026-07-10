import { render } from '@testing-library/react';
import { ReactNode } from 'react';
import { describe, expect, test, vi } from 'vitest';

const layoutMocks = vi.hoisted(() => ({
  dispatchResize: vi.fn(),
  splitPaneProps: [] as Array<Record<string, unknown>>,
}));

vi.mock('app/components/SplitPane', () => ({
  SplitPane: ({
    children,
    ...props
  }: {
    children: ReactNode;
    [key: string]: unknown;
  }) => {
    layoutMocks.splitPaneProps.push(props);
    return <div data-testid="split-pane">{children}</div>;
  },
}));

vi.mock('app/utils/dispatchResize', () => ({
  dispatchResize: layoutMocks.dispatchResize,
}));

vi.mock(
  'app/pages/DashBoardPage/pages/BoardEditor/components/BoardToolBar/BoardToolBar',
  () => ({
    BoardToolBar: () => <div data-testid="board-toolbar" />,
  }),
);

vi.mock(
  'app/pages/DashBoardPage/pages/BoardEditor/components/LayerPanel/LayerTreePanel',
  () => ({
    LayerTreePanel: () => <div data-testid="layer-tree-panel" />,
  }),
);

vi.mock(
  'app/pages/DashBoardPage/pages/BoardEditor/components/SlideSetting/SlideSetting',
  () => ({
    default: () => <div data-testid="slide-setting" />,
  }),
);

vi.mock(
  'app/pages/DashBoardPage/pages/BoardEditor/AutoEditor/AutoBoardEditor',
  () => ({
    AutoBoardEditor: () => <div data-testid="auto-board-editor" />,
  }),
);

vi.mock(
  'app/pages/DashBoardPage/pages/BoardEditor/FreeEditor/FreeBoardEditor',
  () => ({
    FreeBoardEditor: () => <div data-testid="free-board-editor" />,
  }),
);

import { AutoEditor } from '../AutoEditor';
import { FreeEditor } from '../FreeEditor';

describe('BoardEditor split layout', () => {
  test('should dispatch resize while auto editor panes are dragging', () => {
    layoutMocks.splitPaneProps = [];

    render(<AutoEditor />);

    expect(layoutMocks.splitPaneProps).toHaveLength(2);
    expect(layoutMocks.splitPaneProps[0]).toMatchObject({
      onChange: layoutMocks.dispatchResize,
      onDragFinished: layoutMocks.dispatchResize,
      pane2Style: { minWidth: 0 },
    });
    expect(layoutMocks.splitPaneProps[1]).toMatchObject({
      onChange: layoutMocks.dispatchResize,
      onDragFinished: layoutMocks.dispatchResize,
      pane1Style: { display: 'flex', minWidth: 0 },
    });
  });

  test('should dispatch resize while free editor panes are dragging', () => {
    layoutMocks.splitPaneProps = [];

    render(<FreeEditor />);

    expect(layoutMocks.splitPaneProps).toHaveLength(2);
    expect(layoutMocks.splitPaneProps[0]).toMatchObject({
      onChange: layoutMocks.dispatchResize,
      onDragFinished: layoutMocks.dispatchResize,
      pane2Style: { minWidth: 0 },
    });
    expect(layoutMocks.splitPaneProps[1]).toMatchObject({
      onChange: layoutMocks.dispatchResize,
      onDragFinished: layoutMocks.dispatchResize,
      pane1Style: { display: 'flex', minWidth: 0 },
    });
  });
});
