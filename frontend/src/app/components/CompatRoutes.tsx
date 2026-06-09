import { Children, FC, isValidElement, PropsWithChildren } from 'react';
import { useLocation } from 'app/routerCompat';
import {
  CompatRouteProps,
  isCompatRouteMatched,
} from './CompatRoute';

export const CompatRoutes: FC<PropsWithChildren<{}>> = ({ children }) => {
  const location = useLocation();
  const routes = Children.toArray(children).filter(isValidElement) as Array<
    React.ReactElement<CompatRouteProps>
  >;

  const matchedRoute = routes.find(route =>
    isCompatRouteMatched(
      location.pathname,
      route.props.path,
      route.props.exact,
    ),
  );

  if (!matchedRoute) {
    return null;
  }

  return <>{matchedRoute.props.element}</>;
};
