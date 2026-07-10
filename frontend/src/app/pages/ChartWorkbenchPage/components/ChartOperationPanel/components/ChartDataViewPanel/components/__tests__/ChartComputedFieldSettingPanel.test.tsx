import { render, screen } from '@testing-library/react';
import { ThemeProvider } from 'styled-components';
import { describe, expect, test, vi } from 'vitest';

import { DataViewFieldType } from 'app/constants';
import { themes } from 'styles/theme/themes';

import ChartComputedFieldSettingPanel, {
  CHART_COMPUTED_FIELD_MODAL_BODY_STYLE,
  CHART_COMPUTED_FIELD_MODAL_WIDTH,
  CHART_COMPUTED_FIELD_TAB_HORIZONTAL_PADDING,
} from '../ChartComputedFieldSettingPanel';

vi.mock('../ChartComputedFieldEditor/ChartComputedFieldEditor', () => ({
  default: () => <div data-testid="computed-field-editor" />,
}));

const renderPanel = () =>
  render(
    <ThemeProvider theme={themes.light}>
      <ChartComputedFieldSettingPanel
        viewType="SQL"
        computedField={{
          name: 'profit_amount',
          type: DataViewFieldType.NUMERIC,
          expression: 'SUM(profit)',
        }}
        fields={[
          {
            name: 'very_long_business_metric_field_name',
            type: DataViewFieldType.STRING,
          },
          {
            name: 'country_code',
            type: DataViewFieldType.STRING,
          },
        ]}
        variables={[
          {
            name: 'selected_country_region_variable',
            type: DataViewFieldType.STRING,
          },
        ]}
        onChange={vi.fn()}
      />
    </ThemeProvider>,
  );

describe('ChartComputedFieldSettingPanel', () => {
  test('should expose wider modal config without horizontal body scroll', () => {
    expect(CHART_COMPUTED_FIELD_MODAL_WIDTH).toBe(1180);
    expect(CHART_COMPUTED_FIELD_TAB_HORIZONTAL_PADDING).toBe(8);
    expect(CHART_COMPUTED_FIELD_MODAL_BODY_STYLE).toMatchObject({
      overflowX: 'hidden',
    });
  });

  test('should keep long field names readable in the side list', () => {
    const { container } = renderPanel();

    expect(screen.getByTestId('computed-field-editor')).toBeInTheDocument();
    expect(
      screen.getByTitle('very_long_business_metric_field_name'),
    ).toBeInTheDocument();
    expect(
      container.querySelector('.searchable-list-container'),
    ).not.toBeNull();
  });
});
