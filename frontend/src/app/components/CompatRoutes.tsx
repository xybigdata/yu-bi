import { FC, PropsWithChildren } from 'react';
import { CompatSwitch } from './CompatSwitch';

export const CompatRoutes: FC<PropsWithChildren<{}>> = ({ children }) => {
  return <CompatSwitch>{children}</CompatSwitch>;
};
