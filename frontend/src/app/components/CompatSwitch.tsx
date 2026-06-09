import { FC, PropsWithChildren } from 'react';
import { Switch } from 'react-router-dom';

export const CompatSwitch: FC<PropsWithChildren<{}>> = ({ children }) => {
  return <Switch>{children}</Switch>;
};
