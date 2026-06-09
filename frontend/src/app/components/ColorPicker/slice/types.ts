import { ReactNode } from 'react';

export interface colorSelectionPropTypes {
  color?: string;
  onChange?: (color?: string) => void;
}
export interface themeColorPropTypes {
  children: ReactNode;
  callbackFn: (colors: string[]) => void;
}
