import { render, screen, waitFor } from '@testing-library/react';
import { ReactNode } from 'react';
import { useSelector } from 'react-redux';
import { ThemeProvider } from 'styled-components';
import { describe, expect, test, vi } from 'vitest';

import {
  ChartInteractionEvent,
  ChartStyleSectionComponentType,
} from 'app/constants';
import useResizeObserver from 'app/hooks/useResizeObserver';
import type { IChart } from 'app/types/Chart';
import type ChartDataSetDTO from 'app/types/ChartDataSet';
import { themes } from 'styles/theme/themes';

import type { ChartPreview } from '../../../MainPage/pages/VizPage/slice/types';
import ChartPreviewBoardForShare from '../ChartPreviewBoardForShare';

const dispatchMock = vi.fn();
const registerMouseEventsMock = vi.fn();
const iframePropsMock = vi.fn();
const getChartByIdMock = vi.fn(
  (chartGraphId?: string) =>
    ({
      meta: {
        id: chartGraphId,
        name: chartGraphId || '',
      },
      registerMouseEvents: registerMouseEventsMock,
    }) as Partial<IChart>,
);

vi.mock('react-redux', () => ({
  useSelector: vi.fn(),
}));

vi.mock('app/hooks/useRedux', () => ({
  useAppDispatch: () => dispatchMock,
}));

vi.mock('app/hooks/useResizeObserver', () => ({
  default: vi.fn(),
}));

vi.mock('app/hooks/useDebouncedLoadingStatus', () => ({
  default: ({ isLoading }: { isLoading?: boolean }) => Boolean(isLoading),
}));

vi.mock('app/models/ChartManager', () => ({
  default: {
    instance: () => ({
      getById: getChartByIdMock,
    }),
  },
}));

vi.mock('app/components/ChartIFrameContainer', () => ({
  ChartIFrameContainer: props => {
    iframePropsMock(props);
    return (
      <div
        data-container-id={props.containerId}
        data-dataset-id={props.dataset?.id}
        data-height={props.height}
        data-loading={String(props.isLoadingData)}
        data-testid="chart-iframe-container"
        data-width={props.width}
      />
    );
  },
}));

vi.mock('app/components/ChartDrill/ChartDrillContextMenu', () => ({
  default: ({ children }: { children: ReactNode }) => (
    <div data-testid="chart-drill-menu">{children}</div>
  ),
}));

vi.mock('app/components/ChartDrill/ChartDrillPaths', () => ({
  default: () => <div data-testid="chart-drill-paths" />,
}));

vi.mock(
  'app/pages/MainPage/pages/VizPage/ChartPreview/components/ControllerPanel',
  () => ({
    default: ({ viewId }: { viewId?: string }) => (
      <div data-testid="controller-panel" data-view-id={viewId || ''} />
    ),
  }),
);

vi.mock('app/pages/MainPage/pages/VizPage/hooks/useDisplayViewDetail', () => ({
  default: () => [vi.fn(), <div key="view-detail" />],
}));

vi.mock(
  'app/pages/MainPage/pages/VizPage/hooks/useDisplayJumpVizDialog',
  () => ({
    default: () => [vi.fn(), <div key="jump-viz-dialog" />],
  }),
);

vi.mock('app/hooks/useChartInteractions', () => ({
  default: () => ({
    getDrillThroughSetting: vi.fn(),
    getViewDetailSetting: vi.fn(),
    handleDrillThroughEvent: vi.fn(),
    handleViewDataEvent: vi.fn(),
  }),
}));

vi.mock('app/pages/SharePage/components/HeadlessBrowserIdentifier', () => ({
  HeadlessBrowserIdentifier: ({
    height,
    renderSign,
    width,
  }: {
    height: number;
    renderSign: boolean;
    width: number;
  }) => (
    <div
      data-height={height}
      data-render-sign={String(renderSign)}
      data-testid="headless-browser-identifier"
      data-width={width}
    />
  ),
}));

const sampleData: ChartDataSetDTO = {
  id: 'sample-dataset',
  columns: [{ name: 'name' }],
  rows: [['sample']],
};

const fetchedDataset: ChartDataSetDTO = {
  id: 'fetched-dataset',
  columns: [{ name: 'name' }],
  rows: [['fetched']],
};

const createChartPreview = ({
  chartConfig,
  chartGraphId = 'basic-line-chart',
  viewId,
}: {
  chartConfig?: ChartPreview['chartConfig'];
  chartGraphId?: string;
  viewId?: string;
} = {}): ChartPreview =>
  ({
    backendChart: {
      config: {
        aggregation: true,
        chartConfig: {},
        chartGraphId,
        computedFields: [],
        sampleData,
      },
      id: 'chart-id',
      name: 'Share Chart',
      orgId: 'org-id',
      status: 1,
      view: {
        config: {},
        id: viewId || '',
        meta: [],
      },
      viewId: viewId || '',
    },
    chartConfig: {
      datas: [],
      interactions: [],
      styles: [],
      ...chartConfig,
    },
    dataset: fetchedDataset,
    isLoadingData: false,
  }) as unknown as ChartPreview;

const renderBoard = (chartPreview: ChartPreview) =>
  render(
    <ThemeProvider theme={themes.light}>
      <ChartPreviewBoardForShare chartPreview={chartPreview} orgId="org-id" />
    </ThemeProvider>,
  );

describe('ChartPreviewBoardForShare smoke', () => {
  beforeEach(() => {
    dispatchMock.mockClear();
    registerMouseEventsMock.mockClear();
    iframePropsMock.mockClear();
    getChartByIdMock.mockClear();
    vi.mocked(useSelector).mockImplementation(selector =>
      selector({
        share: {
          executeTokenMap: {},
          headlessBrowserRenderSign: true,
          selectedItems: [],
        },
      }),
    );
    vi.mocked(useResizeObserver).mockReturnValue({
      height: 240,
      ref: { current: null },
      width: 320,
    });
  });

  test('should render share chart with sample data when chart has no bound view', async () => {
    renderBoard(createChartPreview());

    const container = await screen.findByTestId('chart-iframe-container');

    expect(container).toHaveAttribute('data-dataset-id', 'sample-dataset');
    expect(container).toHaveAttribute('data-container-id', 'chart-id');
    expect(container).toHaveAttribute('data-width', '320');
    expect(container).toHaveAttribute('data-height', '240');
    expect(screen.getByTestId('headless-browser-identifier')).toHaveAttribute(
      'data-render-sign',
      'true',
    );
    expect(registerMouseEventsMock).toHaveBeenCalledWith([
      expect.objectContaining({
        name: 'click',
        callback: expect.any(Function),
      }),
    ]);
    await waitFor(() => {
      expect(dispatchMock).toHaveBeenCalledWith(expect.any(Function));
    });
  });

  test('should render share chart with fetched dataset when chart has bound view', async () => {
    renderBoard(createChartPreview({ viewId: 'view-id' }));

    const container = await screen.findByTestId('chart-iframe-container');

    expect(container).toHaveAttribute('data-dataset-id', 'fetched-dataset');
    expect(screen.getByTestId('controller-panel')).toHaveAttribute(
      'data-view-id',
      'view-id',
    );
  });

  test('should dispatch selected items when chart selection event fires', async () => {
    renderBoard(createChartPreview());

    await waitFor(() => {
      expect(registerMouseEventsMock).toHaveBeenCalled();
    });

    const clickHandler = registerMouseEventsMock.mock.calls[0][0][0].callback;
    clickHandler({
      interactionType: ChartInteractionEvent.Select,
      selectedItems: [{ index: '0,0', data: { name: 'sample' } }],
    });

    expect(dispatchMock).toHaveBeenCalledWith(
      expect.objectContaining({
        payload: [{ index: '0,0', data: { name: 'sample' } }],
        type: 'share/changeSelectedItems',
      }),
    );
  });

  test('should pass rich text chart model and content to share container', async () => {
    const serializedDelta = JSON.stringify({
      ops: [{ insert: 'share rich text' }, { insert: '\n' }],
    });
    const richTextPreview = createChartPreview({
      chartConfig: {
        datas: [],
        interactions: [],
        styles: [
          {
            comType: ChartStyleSectionComponentType.GROUP,
            key: 'delta',
            label: 'delta',
            rows: [
              {
                comType: ChartStyleSectionComponentType.INPUT,
                key: 'richText',
                label: 'richText',
                value: serializedDelta,
              },
            ],
          },
        ],
      } as ChartPreview['chartConfig'],
      chartGraphId: 'react-rich-text',
    });

    renderBoard(richTextPreview);

    await screen.findByTestId('chart-iframe-container');

    expect(getChartByIdMock).toHaveBeenCalledWith('react-rich-text');
    expect(iframePropsMock).toHaveBeenCalledWith(
      expect.objectContaining({
        chart: expect.objectContaining({
          meta: expect.objectContaining({
            id: 'react-rich-text',
          }),
        }),
        config: expect.objectContaining({
          styles: expect.arrayContaining([
            expect.objectContaining({
              key: 'delta',
              rows: expect.arrayContaining([
                expect.objectContaining({
                  key: 'richText',
                  value: serializedDelta,
                }),
              ]),
            }),
          ]),
        }),
      }),
    );
  });
});
