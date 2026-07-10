import { render } from '@testing-library/react';
import { describe, expect, test } from 'vitest';

import { SPACE_XS } from 'styles/StyleConstants';
import { Group, WithColorPicker } from '../Basic/components/Group';

describe('FormGenerator Group', () => {
  test('should align horizontal config controls with an 8px gap', () => {
    const { getByTestId } = render(
      <Group data-testid="group">
        <span>颜色</span>
        <WithColorPicker data-testid="color-picker-wrap">
          <div className="color-picker" />
        </WithColorPicker>
      </Group>,
    );

    expect(getByTestId('group')).toHaveStyle({
      alignItems: 'center',
      columnGap: SPACE_XS,
    });
    expect(getByTestId('color-picker-wrap')).toHaveStyle({
      alignItems: 'center',
      columnGap: SPACE_XS,
    });
  });
});
