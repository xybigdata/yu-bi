import React from 'react';
import {
  DndProvider as ReactDndProvider,
  type DndProviderProps,
} from 'react-dnd';

type DndProviderCompatProps = DndProviderProps<unknown, unknown> & {
  children?: React.ReactNode;
};

export const DndProviderCompat: React.FC<DndProviderCompatProps> = props => {
  return <ReactDndProvider {...props} />;
};

export default DndProviderCompat;
