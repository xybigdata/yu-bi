import React from 'react';
import { CompatRedirect } from '../CompatRedirect';
import { CompatRoute, type CompatRouteProps } from '../CompatRoute';
import { Authorized } from './Authorized';

interface AuthorizedRouteProps {
  authority: boolean;
  routeProps: CompatRouteProps;
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
      <CompatRoute {...routeProps} />
    </Authorized>
  );
}
