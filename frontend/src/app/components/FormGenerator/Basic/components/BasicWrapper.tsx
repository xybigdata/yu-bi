import { Form, FormItemProps } from 'antd';
import { ReactNode } from 'react';
import styled from 'styled-components';

export function BW(props: FormItemProps & { children?: ReactNode }) {
  return <Wrapper {...props} colon={false} />;
}

const Wrapper = styled(Form.Item)`
  flex-direction: column;
  margin-bottom: 0;

  .ant-form-item-label {
    text-align: left;
  }
`;
