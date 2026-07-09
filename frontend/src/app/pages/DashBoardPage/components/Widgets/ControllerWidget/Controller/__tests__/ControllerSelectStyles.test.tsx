import { Select } from 'antd';
import { render } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import styled from 'styled-components';

import { controllerSelectStyles } from '../ControllerSelectStyles';

const StyledSelect = styled(Select)`
  ${controllerSelectStyles}
`;

describe('controllerSelectStyles', () => {
  test('should keep AntD 6 select content and suffix in one control row', () => {
    render(
      <StyledSelect
        showSearch
        placeholder="请选择"
        options={[{ label: '选项', value: 'value' }]}
      />,
    );

    expect(document.head.textContent).toContain(
      'ant-select{display:flex;align-items:center;height:32px;',
    );
    expect(document.head.textContent).toContain(
      '.ant-select-content{display:flex;flex:1 1 auto;align-items:center;',
    );
    expect(document.head.textContent).toContain(
      '.ant-select-multiple .ant-select-content{flex-wrap:nowrap;overflow:hidden;',
    );
    expect(document.head.textContent).toContain(
      '.ant-select-suffix{display:inline-flex;flex:0 0 auto;align-items:center;justify-content:center;',
    );
  });
});
