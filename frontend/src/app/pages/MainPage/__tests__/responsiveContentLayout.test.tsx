import { render, screen } from '@testing-library/react';
import {
  PERMISSION_FORM_LAYOUT,
  PermissionFormContent,
} from '../pages/PermissionPage/Main/PermissionForm/layout';
import { ScheduleFormContent } from '../pages/SchedulePage/EditorPage/layout';

describe('responsive main page content', () => {
  test('permission forms reserve a stable label track and let content shrink', () => {
    expect(PERMISSION_FORM_LAYOUT).toEqual({
      labelCol: { flex: '104px' },
      wrapperCol: { flex: '1 1 0' },
    });
  });

  test.each([
    [
      'permission form',
      <PermissionFormContent data-testid="responsive-content" />,
      '960px',
    ],
    [
      'schedule form',
      <ScheduleFormContent data-testid="responsive-content" />,
      '860px',
    ],
  ])(
    '%s uses its maximum width without forcing its parent wider',
    (_, component, maxWidth) => {
      render(component);

      expect(screen.getByTestId('responsive-content')).toHaveStyle({
        width: '100%',
        maxWidth,
        minWidth: '0',
      });
    },
  );
});
