import React from 'react';
import { Navigate, Route, type RouteProps } from 'react-router-dom';
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
      denied={<Navigate to={redirectProps.to} replace />}
    >
      <Route {...routeProps} />
    </Authorized>
  );
}
