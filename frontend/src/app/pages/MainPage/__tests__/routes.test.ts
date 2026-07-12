import { matchPath } from 'react-router-dom';
import { describe, expect, test } from 'vitest';
import { getChartEditorClosePath, MAIN_PAGE_ROUTE_PATTERNS } from '../routes';

const matchesMainPageRoute = (pathname: string) =>
  MAIN_PAGE_ROUTE_PATTERNS.some(pattern =>
    matchPath({ path: pattern, end: true }, pathname),
  );

describe('MainPage routes', () => {
  test.each([
    '/organizations/o-1/agent',
    '/organizations/o-1/vizs',
    '/organizations/o-1/vizs/v-1',
    '/organizations/o-1/vizs/v-1/boardEditor',
    '/organizations/o-1/views',
    '/organizations/o-1/views/view-1',
    '/organizations/o-1/sources',
    '/organizations/o-1/sources/add',
    '/organizations/o-1/schedules',
    '/organizations/o-1/schedules/schedule-1',
    '/organizations/o-1/members',
    '/organizations/o-1/members/member-1',
    '/organizations/o-1/roles',
    '/organizations/o-1/roles/add',
    '/organizations/o-1/permissions',
    '/organizations/o-1/permissions/subject',
    '/organizations/o-1/permissions/subject/USER_ROLE/member-1',
    '/organizations/o-1/permissions/subject/ROLE/role-1',
    '/organizations/o-1/permissions/resource/SOURCE/source-1',
  ])('matches %s', pathname => {
    expect(matchesMainPageRoute(pathname)).toBe(true);
  });

  test.each([
    '/organizations/o-1/permissions/subject/USER_ROLE',
    '/organizations/o-1/vizs/v-1/unknown',
    '/organizations/o-1/members/member-1/extra',
  ])('does not over-match %s', pathname => {
    expect(matchesMainPageRoute(pathname)).toBe(false);
  });

  test.each([
    [
      {
        orgId: 'o-1',
        dataChartId: 'chart-1',
        defaultViewId: 'view-1',
      },
      '/organizations/o-1/vizs/chart-1',
    ],
    [
      {
        orgId: 'o-1',
        defaultViewId: 'view-1',
      },
      '/organizations/o-1/views/view-1',
    ],
    [
      {
        orgId: 'o-1',
      },
      '/organizations/o-1/vizs',
    ],
  ])('builds chart editor close path %#', (params, expectedPath) => {
    expect(getChartEditorClosePath(params)).toBe(expectedPath);
  });
});
