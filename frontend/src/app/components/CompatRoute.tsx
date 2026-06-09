import { FC, ReactNode } from 'react';
import { Route, type RouteProps } from 'app/routerCompat';

export interface CompatRouteProps
  extends Omit<RouteProps, 'children' | 'component'> {
  element: ReactNode;
}

export const CompatRoute: FC<CompatRouteProps> = ({ element, ...routeProps }) => {
  return <Route {...routeProps}>{element}</Route>;
};
