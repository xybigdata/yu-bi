export const MAIN_PAGE_ROUTE_PATHS = {
  root: '/',
  confirmInvite: '/confirminvite',
  organization: '/organizations/:orgId',
  agentWorkspace: '/organizations/:orgId/agent',
  vizChartEditor: '/organizations/:orgId/vizs/chartEditor',
  vizStoryPlayer: '/organizations/:orgId/vizs/storyPlayer/:storyId',
  vizStoryEditor: '/organizations/:orgId/vizs/storyEditor/:storyId',
  vizBoardEditor: '/organizations/:orgId/vizs/:vizId/boardEditor',
  vizList: '/organizations/:orgId/vizs',
  vizDetail: '/organizations/:orgId/vizs/:vizId',
  viewList: '/organizations/:orgId/views',
  viewDetail: '/organizations/:orgId/views/:viewId',
  sourceList: '/organizations/:orgId/sources',
  sourceDetail: '/organizations/:orgId/sources/:sourceId',
  scheduleList: '/organizations/:orgId/schedules',
  scheduleDetail: '/organizations/:orgId/schedules/:scheduleId',
  memberList: '/organizations/:orgId/members',
  memberDetail: '/organizations/:orgId/members/:memberId',
  roleList: '/organizations/:orgId/roles',
  roleDetail: '/organizations/:orgId/roles/:roleId',
  permissionList: '/organizations/:orgId/permissions',
  permissionViewpoint: '/organizations/:orgId/permissions/:viewpoint',
  permissionDetail: '/organizations/:orgId/permissions/:viewpoint/:type/:id',
  variables: '/organizations/:orgId/variables',
  orgSettings: '/organizations/:orgId/orgSettings',
  resourceMigration: '/organizations/:orgId/resourceMigration',
} as const;

export const MAIN_PAGE_ROUTE_PATTERNS = Object.values(MAIN_PAGE_ROUTE_PATHS);

export const getChartEditorClosePath = ({
  orgId,
  dataChartId,
  defaultViewId,
}: {
  orgId: string;
  dataChartId?: string;
  defaultViewId?: string;
}) => {
  if (dataChartId) {
    return `/organizations/${orgId}/vizs/${dataChartId}`;
  }
  if (defaultViewId) {
    return `/organizations/${orgId}/views/${defaultViewId}`;
  }
  return `/organizations/${orgId}/vizs`;
};
