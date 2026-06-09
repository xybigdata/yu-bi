import { FC, ReactNode } from 'react';
import { Route } from 'app/routerCompat';

type CompatRoutePath = string | readonly string[];

export interface CompatRouteProps {
  element: ReactNode;
  path?: CompatRoutePath;
  exact?: boolean;
}

export const CompatRoute: FC<CompatRouteProps> = ({ element, ...routeProps }) => {
  return <Route {...routeProps}>{element}</Route>;
};
