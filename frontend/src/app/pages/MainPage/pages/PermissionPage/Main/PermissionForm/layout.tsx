import { Form } from 'antd';
import styled from 'styled-components';

export const PERMISSION_FORM_LAYOUT = {
  labelCol: { flex: '104px' },
  wrapperCol: { flex: '1 1 0' },
} as const;

export const PermissionFormContent = styled(Form)`
  width: 100%;
  min-width: 0;
  max-width: 960px;

  .ant-form-item-row,
  .ant-form-item-control,
  .ant-form-item-control-input,
  .ant-form-item-control-input-content {
    min-width: 0;
  }
`;
