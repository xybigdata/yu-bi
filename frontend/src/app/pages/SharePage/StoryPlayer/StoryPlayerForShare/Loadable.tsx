import { defaultLazyLoad } from 'utils/loadable';

export const LazyBoardPageItem = defaultLazyLoad(
  () => import('./BoardPageItem'),
  module => module.BoardPageItem,
);
