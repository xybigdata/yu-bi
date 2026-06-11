import { useLocation } from 'app/routerCompat';
import { matchPath } from 'react-router-dom';

type ShareRouteKey = 'chart' | 'dashboard' | 'storyPlayer';

const SHARE_ROUTE_PATTERNS: Record<ShareRouteKey, string> = {
  chart: '/shareChart/:token',
  dashboard: '/shareDashboard/:token',
  storyPlayer: '/shareStoryPlayer/:token',
};

export const useShareRouteParams = (routeKey: ShareRouteKey) => {
  const location = useLocation();
  const match = matchPath(
    {
      path: SHARE_ROUTE_PATTERNS[routeKey],
      end: false,
    },
    location.pathname,
  );

  return {
    token: match?.params.token as string | undefined,
  };
};
