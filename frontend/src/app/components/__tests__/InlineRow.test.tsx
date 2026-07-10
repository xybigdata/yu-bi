import { render, screen } from '@testing-library/react';
import { describe, expect, test } from 'vitest';

import { InlineRow, InlineRowAction, InlineRowText } from '../InlineRow';

describe('InlineRow', () => {
  test('should keep icon text and action aligned in one row', () => {
    const { container } = render(
      <InlineRow data-testid="inline-row">
        <span data-testid="icon">icon</span>
        <InlineRowText data-testid="text">country_name_en</InlineRowText>
        <InlineRowAction data-testid="action">...</InlineRowAction>
      </InlineRow>,
    );

    expect(screen.getByTestId('inline-row')).toHaveStyle({
      alignItems: 'center',
      display: 'flex',
      minWidth: '0',
      width: '100%',
    });
    expect(screen.getByTestId('text')).toHaveStyle({
      flex: '1',
      margin: '0',
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap',
    });
    expect(screen.getByTestId('action')).toHaveStyle({
      alignItems: 'center',
      display: 'flex',
      flex: '0 0 auto',
    });
    expect(container.querySelector('p')).toBeNull();
  });
});
