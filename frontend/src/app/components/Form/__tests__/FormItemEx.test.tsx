import { render, screen } from '@testing-library/react';
import { ThemeProvider } from 'styled-components';
import { describe, expect, test } from 'vitest';

import { themes } from 'styles/theme/themes';
import FormItemEx from '../FormItemEx';

const renderFormItem = () =>
  render(
    <ThemeProvider theme={themes.light}>
      <FormItemEx
        label="字段名称"
        labelAlign="right"
        labelCol={{ span: 8 }}
        wrapperCol={{ span: 8 }}
      >
        country_code
      </FormItemEx>
    </ThemeProvider>,
  );

describe('FormItemEx', () => {
  test('should render label with secondary text color under labelCol layout', () => {
    renderFormItem();

    expect(screen.getByText('字段名称')).toHaveStyle({
      color: themes.light.textColorLight,
    });
  });
});
