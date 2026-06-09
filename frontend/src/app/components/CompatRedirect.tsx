import { FC } from 'react';
import { Route, type RouteProps } from 'app/routerCompat';

interface CompatRedirectProps {
  to: string;
}

export const CompatRedirect: FC<CompatRedirectProps & RouteProps> = ({
  to,
  ...routeProps
}) => {
  return (
    <Route
      {...routeProps}
      render={({ history }) => {
        history.replace(to);
        return null;
      }}
    />
  );
};
