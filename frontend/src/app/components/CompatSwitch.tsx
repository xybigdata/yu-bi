import { FC, PropsWithChildren } from 'react';
import { Switch } from 'app/routerCompat';

export const CompatSwitch: FC<PropsWithChildren<{}>> = ({ children }) => {
  return <Switch>{children}</Switch>;
};
