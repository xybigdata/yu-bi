import { defaultLazyLoad } from 'utils/loadable';

export const LazyConfirmInvitePage = defaultLazyLoad(
  () => import('./pages/ConfirmInvitePage'),
  module => module.ConfirmInvitePage,
);

export const LazyMemberPage = defaultLazyLoad(
  () => import('./pages/MemberPage'),
  module => module.MemberPage,
);

export const LazyOrgSettingPage = defaultLazyLoad(
  () => import('./pages/OrgSettingPage'),
  module => module.OrgSettingPage,
);

export const LazyPermissionPage = defaultLazyLoad(
  () => import('./pages/PermissionPage'),
  module => module.PermissionPage,
);

export const LazyResourceMigrationPage = defaultLazyLoad(
  () => import('./pages/ResourceMigrationPage'),
  module => module.ResourceMigrationPage,
);

export const LazySchedulePage = defaultLazyLoad(
  () => import('./pages/SchedulePage'),
  module => module.SchedulePage,
);

export const LazySourcePage = defaultLazyLoad(
  () => import('./pages/SourcePage'),
  module => module.SourcePage,
);

export const LazyVariablePage = defaultLazyLoad(
  () => import('./pages/VariablePage'),
  module => module.VariablePage,
);

export const LazyViewPage = defaultLazyLoad(
  () => import('./pages/ViewPage'),
  module => module.ViewPage,
);

export const LazyVizPage = defaultLazyLoad(
  () => import('./pages/VizPage'),
  module => module.VizPage,
);
