import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider } from 'styled-components';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { themes } from 'styles/theme/themes';
import { Folders } from '..';

const mocks = vi.hoisted(() => ({
  dispatch: vi.fn(),
  returnFreshTree: false,
  treeData: [
    {
      key: 'dashboard-folder',
      id: 'dashboard-folder',
      title: '仪表板目录',
      relType: 'FOLDER',
      subType: 'DASHBOARD',
      children: [
        {
          key: 'dashboard-nav',
          id: 'dashboard-nav',
          relId: 'dashboard-1',
          relType: 'DASHBOARD',
          title: '经营驾驶舱',
        },
      ],
    },
    {
      key: 'chart-folder',
      id: 'chart-folder',
      title: '数据图表目录',
      relType: 'FOLDER',
      subType: 'DATACHART',
      children: [
        {
          key: 'chart-nav',
          id: 'chart-nav',
          relId: 'chart-1',
          relType: 'DATACHART',
          title: '订单趋势',
        },
      ],
    },
  ],
}));

vi.mock('react-redux', () => ({
  useSelector: selector => selector({}),
}));

vi.mock('app/hooks/useRedux', () => ({
  useAppDispatch: () => mocks.dispatch,
}));

vi.mock('app/pages/MainPage/slice/selectors', () => ({
  selectOrgId: () => 'org-1',
  selectIsOrgOwner: () => true,
}));

vi.mock('../../../slice/selectors', () => ({
  makeSelectVizTree: () => () =>
    mocks.returnFreshTree
      ? mocks.treeData.map(node => ({
          ...node,
          children: node.children.map(child => ({ ...child })),
        }))
      : mocks.treeData,
}));

vi.mock('app/hooks/useDebouncedSearch', () => ({
  useDebouncedSearch: data => ({
    filteredData: data,
    debouncedSearch: vi.fn(),
  }),
}));

vi.mock('app/hooks/useGetVizIcon', () => ({
  default: () => vi.fn(),
}));

vi.mock('app/hooks/useCompatNavigate', () => ({
  useCompatNavigate: () => ({ push: vi.fn() }),
}));

vi.mock('../../../hooks/useAddViz', () => ({
  useAddViz: () => vi.fn(),
}));

vi.mock('../FolderTree', () => ({
  FolderTree: ({ treeData }) => (
    <div>
      {treeData?.flatMap(node => [
        <span key={node.key}>{node.title}</span>,
        ...(node.children || []).map(child => (
          <span key={child.key}>{child.title}</span>
        )),
      ])}
    </div>
  ),
}));

vi.mock('app/features/recycle', () => ({
  RecycleBatchManager: ({ resourceType }) => <div>批量管理 {resourceType}</div>,
  RecycleBinManager: ({ resourceType }) => <div>回收站 {resourceType}</div>,
}));

describe('仪表板和数据图表独立管理', () => {
  beforeEach(() => {
    mocks.returnFreshTree = false;
  });

  it('默认打开仪表板并按 Tab 切换独立目录树', async () => {
    const user = userEvent.setup();
    render(
      <ThemeProvider theme={themes.light}>
        <Folders i18nPrefix="viz" />
      </ThemeProvider>,
    );

    expect(screen.getByRole('tab', { name: '仪表板' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    expect(screen.getByText('经营驾驶舱')).toBeInTheDocument();
    expect(screen.queryByText('订单趋势')).not.toBeInTheDocument();

    await user.click(screen.getByRole('tab', { name: '数据图表' }));

    expect(screen.getByText('订单趋势')).toBeInTheDocument();
    expect(screen.queryByText('经营驾驶舱')).not.toBeInTheDocument();
  });

  it('资源树刷新时保留用户主动切换的数据图表 Tab', async () => {
    mocks.returnFreshTree = true;
    const user = userEvent.setup();
    render(
      <ThemeProvider theme={themes.light}>
        <Folders i18nPrefix="viz" selectedId="dashboard-nav" />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('tab', { name: '数据图表' }));

    expect(screen.getByRole('tab', { name: '数据图表' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    expect(screen.getByText('订单趋势')).toBeInTheDocument();
    expect(screen.queryByText('经营驾驶舱')).not.toBeInTheDocument();
  });
});
