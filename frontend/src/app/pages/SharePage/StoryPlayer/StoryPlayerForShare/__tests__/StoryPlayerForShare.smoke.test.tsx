import { act, render, screen, waitFor } from '@testing-library/react';
import { useSelector } from 'react-redux';
import { describe, expect, test, vi } from 'vitest';

import { useAppDispatch } from 'app/hooks/useRedux';
import { createRevealPlayerRuntime } from 'app/pages/StoryBoardPage/playerRuntime';
import { storyActions } from 'app/pages/StoryBoardPage/slice';
import { getPageContentDetail } from 'app/pages/StoryBoardPage/slice/thunks';
import type {
  StoryBoard,
  StoryBoardState,
} from 'app/pages/StoryBoardPage/slice/types';

import { StoryPlayerForShare } from '../StoryPlayerForShare';

vi.mock('react-redux', async importOriginal => ({
  ...(await importOriginal<typeof import('react-redux')>()),
  useSelector: vi.fn(),
}));

vi.mock('app/hooks/useRedux', () => ({
  useAppDispatch: vi.fn(),
}));

vi.mock('app/components/DndProviderCompat', () => ({
  default: ({ children }) => <div data-testid="dnd-provider">{children}</div>,
}));

vi.mock('react-dnd-html5-backend', () => ({
  HTML5Backend: vi.fn(),
}));

const revealRuntime = vi.hoisted(() => ({
  cancel: vi.fn(),
  destroy: vi.fn(),
}));

vi.mock('app/pages/StoryBoardPage/playerRuntime', () => ({
  createRevealPlayerRuntime: vi.fn(() => revealRuntime),
}));

vi.mock('app/pages/StoryBoardPage/slice/thunks', () => ({
  getPageContentDetail: vi.fn(payload => ({
    payload,
    type: 'storyBoard/getPageContentDetail',
  })),
}));

vi.mock('../StoryPageItem', () => ({
  default: ({ page }) => (
    <section
      data-index={page.config.index}
      data-testid="story-page-item"
      data-transition={`${page.config.transitionEffect.in} ${page.config.transitionEffect.out}`}
      data-transition-speed={page.config.transitionEffect.speed}
    >
      {page.id}
    </section>
  ),
}));

const storyBoard: StoryBoard = {
  config: {
    autoPlay: {
      auto: true,
      delay: 3,
    },
    version: '1.0.0',
  },
  id: 'story-1',
  name: '分享故事板',
  permission: null,
  status: 1,
  thumbnail: '',
};

const storyBoardState: StoryBoardState = {
  storyMap: {
    [storyBoard.id]: storyBoard,
  },
  storyPageInfoMap: {
    [storyBoard.id]: {
      'page-a': { id: 'page-a', selected: false },
      'page-b': { id: 'page-b', selected: false },
    },
  },
  storyPageMap: {
    [storyBoard.id]: {
      'page-b': {
        config: {
          index: 1,
          transitionEffect: {
            in: 'fade-in',
            out: 'fade-out',
            speed: 'fast',
          },
          version: '1.0.0',
        },
        id: 'page-b',
        relId: 'dashboard-b',
        relType: 'DASHBOARD',
        storyboardId: storyBoard.id,
      },
      'page-a': {
        config: {
          index: 0,
          transitionEffect: {
            in: 'slide-in',
            out: 'slide-out',
            speed: 'slow',
          },
          version: '1.0.0',
        },
        id: 'page-a',
        relId: 'dashboard-a',
        relType: 'DASHBOARD',
        storyboardId: storyBoard.id,
      },
    },
  },
};

const createState = () => ({
  share: {
    subVizTokenMap: {
      'page-a': 'viz-token-a',
      'page-b': 'viz-token-b',
    },
  },
  storyBoard: storyBoardState,
});

describe('StoryPlayerForShare smoke', () => {
  beforeEach(() => {
    revealRuntime.cancel.mockClear();
    revealRuntime.destroy.mockClear();
    vi.mocked(createRevealPlayerRuntime).mockClear();
    vi.mocked(getPageContentDetail).mockClear();
    vi.mocked(useAppDispatch).mockReturnValue(vi.fn());
    vi.mocked(useSelector).mockImplementation(selector =>
      selector(createState()),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test('should render sorted pages, initialize reveal runtime and load first shared page', async () => {
    const dispatch = vi.fn();
    vi.mocked(useAppDispatch).mockReturnValue(dispatch);

    const { unmount } = render(
      <StoryPlayerForShare storyBoard={storyBoard} shareToken="share-token" />,
    );

    const pages = screen.getAllByTestId('story-page-item');
    expect(pages.map(page => page.textContent)).toEqual(['page-a', 'page-b']);
    expect(pages[0]).toHaveAttribute('data-transition', 'slide-in slide-out');
    expect(pages[0]).toHaveAttribute('data-transition-speed', 'slow');

    expect(createRevealPlayerRuntime).toHaveBeenCalledWith(
      expect.objectContaining({
        config: expect.objectContaining({
          autoSlide: 3000,
          controls: true,
          hash: false,
          history: false,
          keyboard: expect.objectContaining({
            27: expect.any(Function),
          }),
        }),
        domId: expect.any(String),
        onSlideChanged: expect.any(Function),
      }),
    );
    expect(dispatch).toHaveBeenCalledWith(
      storyActions.changePageSelected({
        multiple: false,
        pageId: 'page-a',
        storyId: storyBoard.id,
      }),
    );
    expect(getPageContentDetail).toHaveBeenCalledWith({
      relId: 'dashboard-a',
      relType: 'DASHBOARD',
      shareToken: 'share-token',
      vizToken: 'viz-token-a',
    });
    expect(dispatch).toHaveBeenCalledWith({
      payload: {
        relId: 'dashboard-a',
        relType: 'DASHBOARD',
        shareToken: 'share-token',
        vizToken: 'viz-token-a',
      },
      type: 'storyBoard/getPageContentDetail',
    });

    unmount();

    expect(revealRuntime.cancel).toHaveBeenCalledTimes(1);
    expect(revealRuntime.destroy).toHaveBeenCalledTimes(1);
  });

  test('should switch page and load shared content when reveal slide changes', async () => {
    const dispatch = vi.fn();
    vi.mocked(useAppDispatch).mockReturnValue(dispatch);

    render(
      <StoryPlayerForShare storyBoard={storyBoard} shareToken="share-token" />,
    );

    const onSlideChanged = vi.mocked(createRevealPlayerRuntime).mock
      .calls[0]?.[0].onSlideChanged as unknown as (event: {
      indexh: number;
    }) => void;
    act(() => {
      onSlideChanged({ indexh: 1 });
    });

    expect(dispatch).toHaveBeenCalledWith(
      storyActions.changePageSelected({
        multiple: false,
        pageId: 'page-b',
        storyId: storyBoard.id,
      }),
    );
    await waitFor(() => {
      expect(getPageContentDetail).toHaveBeenLastCalledWith({
        relId: 'dashboard-b',
        relType: 'DASHBOARD',
        shareToken: 'share-token',
        vizToken: 'viz-token-b',
      });
    });
  });
});
