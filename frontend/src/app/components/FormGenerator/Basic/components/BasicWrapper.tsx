import { Form, FormItemProps } from 'antd';
import { ReactNode } from 'react';
import styled from 'styled-components';

export function BW(props: FormItemProps & { children?: ReactNode }) {
  const { children, ...rest } = props;
  const isInline = Boolean(rest.labelCol || rest.wrapperCol);

  return (
    <Wrapper {...rest} $inline={isInline} colon={false}>
      {children}
    </Wrapper>
  );
}

const Wrapper = styled(Form.Item)<{ $inline: boolean }>`
  display: flex;
  width: 100%;
  margin-bottom: 0;

  .ant-form-item-row {
    flex-flow: ${p => (p.$inline ? 'row nowrap' : 'column nowrap')};
    align-items: ${p => (p.$inline ? 'center' : 'stretch')};
    width: 100%;
  }

  .ant-form-item-label {
    text-align: left;

    > label {
      color: ${p => p.theme.textColorLight};
    }
  }

  .ant-form-item-row > .ant-col > label {
    color: ${p => p.theme.textColorLight};
  }

  .ant-form-item-control-input-content {
    width: 100%;
  }

  .ant-form-item-control-input-content > .ant-input,
  .ant-form-item-control-input-content > .ant-input-number,
  .ant-form-item-control-input-content > .ant-select {
    width: 100%;
  }

  ${p =>
    !p.$inline &&
    `
      .ant-form-item-label,
      .ant-form-item-control {
        flex: none;
        width: 100%;
        max-width: 100%;
      }

      .ant-form-item-control-input {
        min-height: 32px;
      }
    `}
`;
