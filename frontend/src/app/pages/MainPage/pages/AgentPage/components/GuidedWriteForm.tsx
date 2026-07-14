import {
  EyeOutlined,
  PlusCircleOutlined,
  TagsOutlined,
} from '@ant-design/icons';
import { Button, Form, Input, Select, Space, Typography } from 'antd';
import type {
  AgentWritableTool,
  AgentWritePreviewRequest,
} from 'app/features/agent';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useEffect } from 'react';
import styled from 'styled-components';
import { SPACE_MD, SPACE_SM } from 'styles/StyleConstants';
import { uuidv4 } from 'utils/utils';
import type { AgentPreviewIntent } from '../slice/types';

interface GuidedWriteFormValues {
  toolName: AgentWritableTool;
  name: string;
  viewId?: string;
  dashboardId?: string;
}

interface GuidedWriteFormProps {
  availableTools: AgentWritableTool[];
  disabled: boolean;
  loading: boolean;
  previousIntent?: AgentPreviewIntent;
  onSubmit: (request: AgentWritePreviewRequest, idempotencyKey: string) => void;
}

const sameRequest = (
  left: AgentWritePreviewRequest,
  right: AgentWritePreviewRequest,
) => {
  if (left.toolName !== right.toolName) {
    return false;
  }
  if (left.toolName === 'create_chart' && right.toolName === 'create_chart') {
    return (
      left.arguments.name === right.arguments.name &&
      left.arguments.viewId === right.arguments.viewId
    );
  }
  return (
    left.toolName === 'rename_dashboard' &&
    right.toolName === 'rename_dashboard' &&
    left.arguments.newName === right.arguments.newName &&
    left.arguments.dashboardId === right.arguments.dashboardId
  );
};

export function GuidedWriteForm({
  availableTools,
  disabled,
  loading,
  previousIntent,
  onSubmit,
}: GuidedWriteFormProps) {
  const t = useI18NPrefix('agent');
  const [form] = Form.useForm<GuidedWriteFormValues>();
  const toolName = Form.useWatch('toolName', form);

  useEffect(() => {
    const current = form.getFieldValue('toolName');
    if (!current || !availableTools.includes(current)) {
      form.setFieldValue('toolName', availableTools[0]);
    }
  }, [availableTools, form]);

  const submit = (values: GuidedWriteFormValues) => {
    const request: AgentWritePreviewRequest =
      values.toolName === 'create_chart'
        ? {
            toolName: 'create_chart',
            arguments: {
              name: values.name.trim(),
              viewId: values.viewId!.trim(),
            },
          }
        : {
            toolName: 'rename_dashboard',
            arguments: {
              dashboardId: values.dashboardId!.trim(),
              newName: values.name.trim(),
            },
          };
    const idempotencyKey =
      previousIntent && sameRequest(previousIntent.request, request)
        ? previousIntent.idempotencyKey
        : uuidv4();
    onSubmit(request, idempotencyKey);
  };

  return (
    <Form
      form={form}
      layout="vertical"
      initialValues={{ toolName: availableTools[0] }}
      onFinish={submit}
      requiredMark={false}
    >
      <Form.Item
        label={t('form.tool')}
        name="toolName"
        rules={[{ required: true, message: t('form.toolRequired') }]}
      >
        <Select
          disabled={disabled || loading}
          options={availableTools.map(tool => ({
            value: tool,
            label:
              tool === 'create_chart'
                ? t('tools.createChart')
                : t('tools.renameDashboard'),
          }))}
        />
      </Form.Item>

      {toolName === 'create_chart' && (
        <Form.Item
          label={t('form.viewId')}
          name="viewId"
          rules={[
            { required: true, message: t('form.viewIdRequired') },
            { max: 128 },
          ]}
        >
          <Input
            prefix={<PlusCircleOutlined />}
            autoComplete="off"
            disabled={disabled || loading}
          />
        </Form.Item>
      )}

      {toolName === 'rename_dashboard' && (
        <Form.Item
          label={t('form.dashboardId')}
          name="dashboardId"
          rules={[
            { required: true, message: t('form.dashboardIdRequired') },
            { max: 128 },
          ]}
        >
          <Input
            prefix={<TagsOutlined />}
            autoComplete="off"
            disabled={disabled || loading}
          />
        </Form.Item>
      )}

      <Form.Item
        label={t('form.name')}
        name="name"
        rules={[
          { required: true, whitespace: true, message: t('form.nameRequired') },
          { max: 128 },
        ]}
      >
        <Input autoComplete="off" disabled={disabled || loading} />
      </Form.Item>

      <FormActions align="center" wrap>
        <Button
          htmlType="submit"
          type="primary"
          icon={<EyeOutlined />}
          loading={loading}
          disabled={disabled || availableTools.length === 0}
        >
          {t('form.preview')}
        </Button>
        <Typography.Text type="secondary">
          {t('form.previewHint')}
        </Typography.Text>
      </FormActions>
    </Form>
  );
}

const FormActions = styled(Space)`
  width: 100%;
  padding-top: ${SPACE_SM};

  .ant-typography {
    flex: 1;
    min-width: ${SPACE_MD};
  }
`;
