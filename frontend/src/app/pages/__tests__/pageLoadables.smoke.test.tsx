import { render, screen } from '@testing-library/react';
import React from 'react';
import { afterEach, describe, expect, test, vi } from 'vitest';

const lazyLoadMock = vi.hoisted(() => {
  const createMockComponent =
    (kind: string) =>
    (...args: unknown[]) => {
      const MockLoadable = () => (
        <div data-testid="mock-loadable">{`${kind}:${args.length}`}</div>
      );

      return MockLoadable;
    };

  return {
    defaultLazyLoad: vi.fn(createMockComponent('default')),
    lazyLoad: vi.fn(createMockComponent('custom')),
  };
});

vi.mock('utils/loadable', () => lazyLoadMock);

interface PageLoadableCase {
  argumentCount?: number;
  exportName: string;
  importModule: () => Promise<Record<string, unknown>>;
  kind: 'custom' | 'default';
}

const modules: PageLoadableCase[] = [
  {
    exportName: 'LazyActivationPage',
    importModule: () => import('../ActivationPage/Loadable'),
    kind: 'default',
  },
  {
    exportName: 'LazyAuthorizationPage',
    importModule: () => import('../AuthorizationPage/Loadable'),
    kind: 'default',
  },
  {
    exportName: 'LazyForgetPasswordPage',
    importModule: () => import('../ForgetPasswordPage/Loadable'),
    kind: 'default',
  },
  {
    exportName: 'LazyLoginPage',
    importModule: () => import('../LoginPage/Loadable'),
    kind: 'default',
  },
  {
    exportName: 'LazyMainPage',
    importModule: () => import('../MainPage/Loadable'),
    kind: 'default',
  },
  {
    argumentCount: 3,
    exportName: 'NotFoundPage',
    importModule: () => import('../NotFoundPage/Loadable'),
    kind: 'custom',
  },
  {
    exportName: 'LazyRegisterPage',
    importModule: () => import('../RegisterPage/Loadable'),
    kind: 'default',
  },
  {
    exportName: 'LazySetupPage',
    importModule: () => import('../SetupPage/Loadable'),
    kind: 'default',
  },
  {
    exportName: 'EditorPPT',
    importModule: () => import('../StoryBoardPage/Loadable'),
    kind: 'default',
  },
  {
    exportName: 'PPTPlayer',
    importModule: () => import('../StoryBoardPage/Loadable'),
    kind: 'default',
  },
  {
    exportName: 'LazyBoard',
    importModule: () => import('../DashBoardPage/pages/Board/Loadable'),
    kind: 'default',
  },
  {
    exportName: 'LazyConfirmInvitePage',
    importModule: () => import('../MainPage/PageLoadables'),
    kind: 'default',
  },
  {
    exportName: 'LazyMemberPage',
    importModule: () => import('../MainPage/PageLoadables'),
    kind: 'default',
  },
  {
    exportName: 'LazyOrgSettingPage',
    importModule: () => import('../MainPage/PageLoadables'),
    kind: 'default',
  },
  {
    exportName: 'LazyPermissionPage',
    importModule: () => import('../MainPage/PageLoadables'),
    kind: 'default',
  },
  {
    exportName: 'LazyResourceMigrationPage',
    importModule: () => import('../MainPage/PageLoadables'),
    kind: 'default',
  },
  {
    exportName: 'LazySchedulePage',
    importModule: () => import('../MainPage/PageLoadables'),
    kind: 'default',
  },
  {
    exportName: 'LazySourcePage',
    importModule: () => import('../MainPage/PageLoadables'),
    kind: 'default',
  },
  {
    exportName: 'LazyVariablePage',
    importModule: () => import('../MainPage/PageLoadables'),
    kind: 'default',
  },
  {
    exportName: 'LazyViewPage',
    importModule: () => import('../MainPage/PageLoadables'),
    kind: 'default',
  },
  {
    exportName: 'LazyVizPage',
    importModule: () => import('../MainPage/PageLoadables'),
    kind: 'default',
  },
  {
    exportName: 'LazyShareChart',
    importModule: () => import('../SharePage/Chart/Loadable'),
    kind: 'default',
  },
  {
    exportName: 'LazyShareDashboard',
    importModule: () => import('../SharePage/Dashboard/Loadable'),
    kind: 'default',
  },
  {
    exportName: 'LazyBoardPageItem',
    importModule: () =>
      import('../SharePage/StoryPlayer/StoryPlayerForShare/Loadable'),
    kind: 'default',
  },
  {
    exportName: 'LazyShareStoryPlayer',
    importModule: () => import('../SharePage/StoryPlayer/Loadable'),
    kind: 'default',
  },
];

afterEach(() => {
  vi.clearAllMocks();
});

describe('page loadables', () => {
  test.each(modules)(
    'should expose route loadable $exportName',
    async ({ argumentCount = 2, exportName, importModule, kind }) => {
      const module = await importModule();
      const Component = module[exportName] as React.ComponentType<
        Record<string, unknown>
      >;

      expect(Component).toEqual(expect.any(Function));

      render(<Component boardId="board-id" id="board-id" renderMode="read" />);

      expect(screen.getByTestId('mock-loadable')).toHaveTextContent(
        `${kind}:${argumentCount}`,
      );
    },
  );
});
