import { renderHook } from '@testing-library/react';
import { ReactNode } from 'react';
import { MemoryRouter } from 'app/routerCompat';
import { useShareRouteParams } from '../useShareRouteParams';

const renderShareRouteParams = (
  routeKey: Parameters<typeof useShareRouteParams>[0],
  path: string,
) =>
  renderHook(() => useShareRouteParams(routeKey), {
    wrapper: ({ children }: { children: ReactNode }) => (
      <MemoryRouter initialEntries={[path]}>{children}</MemoryRouter>
    ),
  });

describe('useShareRouteParams', () => {
  test.each([
    ['chart', '/shareChart/chart-token'],
    ['dashboard', '/shareDashboard/dashboard-token'],
    ['storyPlayer', '/shareStoryPlayer/story-token'],
  ] as const)('should parse %s share token', (routeKey, path) => {
    const { result } = renderShareRouteParams(routeKey, path);

    expect(result.current.token).toBe(path.split('/').at(-1));
  });

  test('should return undefined token when path does not match route key', () => {
    const { result } = renderShareRouteParams('chart', '/shareDashboard/token');

    expect(result.current.token).toBeUndefined();
  });
});
