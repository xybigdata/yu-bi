import { describe, expect, test } from 'vitest';
import { getMemberSidebarSelectedKey } from '..';

describe('getMemberSidebarSelectedKey', () => {
  test.each([
    ['/organizations/o-1/members', 'members'],
    ['/organizations/o-1/members/member-1', 'members'],
    ['/organizations/o-1/members/add', 'members'],
    ['/organizations/o-1/roles', 'roles'],
    ['/organizations/o-1/roles/role-1', 'roles'],
    ['/organizations/o-1/roles/add', 'roles'],
  ])('returns %s selected key', (pathname, expected) => {
    expect(getMemberSidebarSelectedKey(pathname)).toBe(expected);
  });

  test.each([
    '/',
    '/organizations/o-1',
    '/organizations/o-1/sources',
    '/organizations/o-1/permissions/subject/USER_ROLE/member-1',
  ])('returns empty selected key for %s', pathname => {
    expect(getMemberSidebarSelectedKey(pathname)).toBe('');
  });
});
