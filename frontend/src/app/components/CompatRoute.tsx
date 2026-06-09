import { FC, ReactNode } from 'react';
import { matchPath } from 'react-router-dom';
import { useLocation } from 'app/routerCompat';

export type CompatRoutePath = string | readonly string[];

export interface CompatRouteProps {
  element: ReactNode;
  path?: CompatRoutePath;
  exact?: boolean;
}

export const isCompatRouteMatched = (
  pathname: string,
  path?: CompatRoutePath,
  exact?: boolean,
) => {
  if (!path || path === '*') {
    return true;
  }

  return !!matchPath(pathname, {
    path: path as string | string[],
    exact,
  });
};

export const CompatRoute: FC<CompatRouteProps> = ({ element, path, exact }) => {
  const location = useLocation();

  if (!isCompatRouteMatched(location.pathname, path, exact)) {
    return null;
  }

  return <>{element}</>;
};
