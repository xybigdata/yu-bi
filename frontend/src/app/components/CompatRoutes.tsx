import { FC, PropsWithChildren } from 'react';
import { Switch } from 'app/routerCompatLegacy';

export const CompatRoutes: FC<PropsWithChildren<{}>> = ({ children }) => {
  return <Switch>{children}</Switch>;
};
