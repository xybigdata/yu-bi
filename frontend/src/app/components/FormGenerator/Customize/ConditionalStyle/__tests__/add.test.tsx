import { render, screen } from '@testing-library/react';
import { DataViewFieldType } from 'app/constants';
import { ThemeProvider } from 'styled-components';
import { describe, expect, test, vi } from 'vitest';

import { themes } from 'styles/theme/themes';
import Add from '../add';
import type { ConditionalStyleFormValues } from '../types';

const translations = {
  'conditionalStyleTable.header.color.background': '背景',
  'conditionalStyleTable.header.color.text': '文字',
  'conditionalStyleTable.header.color.title': '颜色',
  'conditionalStyleTable.header.operator': '关系',
  'conditionalStyleTable.header.range.cell': '单元格',
  'conditionalStyleTable.header.range.row': '整行',
  'conditionalStyleTable.header.range.title': '应用范围',
  'conditionalStyleTable.header.value': '值',
  'conditionalStyleTable.modal.title': '条件格式',
} as Record<string, string>;

const renderModal = () =>
  render(
    <ThemeProvider theme={themes.light}>
      <Add
        context={{
          label: 'currency_code',
          type: DataViewFieldType.STRING,
        }}
        open
        translate={key => translations[key] ?? key}
        values={{} as ConditionalStyleFormValues}
        onCancel={vi.fn()}
        onOk={vi.fn()}
      />
    </ThemeProvider>,
  );

describe('ConditionalStyle AddModal', () => {
  test('should render visible color picker swatches for background and text', () => {
    renderModal();

    expect(screen.getByText('背景')).toBeInTheDocument();
    expect(screen.getByText('文字')).toBeInTheDocument();
    expect(
      document.querySelectorAll('.conditional-style-color-swatch'),
    ).toHaveLength(2);
  });

  test('should use shared Datart-aligned form modal chrome', () => {
    renderModal();

    expect(document.querySelector('.yubi-form-modal')).toBeInTheDocument();
    expect(
      document.querySelector('.yubi-conditional-style-modal'),
    ).toBeInTheDocument();
  });
});
