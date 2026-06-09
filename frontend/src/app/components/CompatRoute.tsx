import { FC, ReactNode } from 'react';
import { Route, RouteProps } from 'react-router-dom';

interface CompatRouteProps extends Omit<RouteProps, 'children' | 'component'> {
  element: ReactNode;
}

export const CompatRoute: FC<CompatRouteProps> = ({ element, ...routeProps }) => {
  return <Route {...routeProps}>{element}</Route>;
};
