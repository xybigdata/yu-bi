import { render } from '@testing-library/react';
import { ThemeProvider } from 'styled-components';
import { describe, expect, test, vi } from 'vitest';

import { themes } from 'styles/theme/themes';
import ConditionalStyle from '../ConditionalStyle';

const translations = {
  'conditionalStyleTable.btn.add': '新增',
  'conditionalStyleTable.btn.confirm': '确认',
  'conditionalStyleTable.btn.edit': '编辑',
  'conditionalStyleTable.btn.remove': '删除',
  'conditionalStyleTable.header.action': '操作',
  'conditionalStyleTable.header.color.background': '背景',
  'conditionalStyleTable.header.color.text': '文字',
  'conditionalStyleTable.header.color.title': '颜色',
  'conditionalStyleTable.header.operator': '关系',
  'conditionalStyleTable.header.range.cell': '单元格',
  'conditionalStyleTable.header.range.title': '应用范围',
  'conditionalStyleTable.header.value': '值',
} as Record<string, string>;

describe('ConditionalStyle panel', () => {
  test('should render condition color tags with white text and 8px right margin', () => {
    const { container } = render(
      <ThemeProvider theme={themes.light}>
        <ConditionalStyle
          ancestors={[]}
          translate={key => translations[key] ?? key}
          data={
            {
              value: [
                {
                  uid: 'condition-1',
                  range: 'cell',
                  operator: '=',
                  value: '123',
                  color: {
                    background: '#1677ff',
                    textColor: '#91caff',
                  },
                },
              ],
            } as never
          }
          onChange={vi.fn()}
        />
      </ThemeProvider>,
    );

    const tags = container.querySelectorAll('.conditional-style-color-tag');

    expect(tags).toHaveLength(2);
    tags.forEach(tag => {
      expect(tag).toHaveStyle('color: #ffffff');
      expect(tag).toHaveStyle('margin-right: 8px');
    });
  });
});
