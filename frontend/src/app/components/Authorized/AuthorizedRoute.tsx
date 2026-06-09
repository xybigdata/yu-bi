import React from 'react';
import { Route, type RouteProps } from 'app/routerCompat';
import { CompatRedirect } from '../CompatRedirect';
import { Authorized } from './Authorized';

interface AuthorizedRouteProps {
  authority: boolean;
  routeProps: RouteProps;
  redirectProps: { to: string };
}

export function AuthorizedRoute({
  authority,
  routeProps,
  redirectProps,
}: AuthorizedRouteProps) {
  return (
    <Authorized
      authority={authority}
      denied={<CompatRedirect to={redirectProps.to} />}
    >
      <Route {...routeProps} />
    </Authorized>
  );
}
