import { defaultLazyLoad } from 'utils/loadable';

export const LazyBoard = defaultLazyLoad(
  () => import('./index'),
  module => module.Board,
);
