import {
  CheckOutlined,
  CloseOutlined,
  LinkOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Descriptions,
  Divider,
  Space,
  Tag,
  Typography,
} from 'antd';
import type {
  AgentApproval,
  AgentApprovalStatus,
  AgentClientFailure,
} from 'app/features/agent';
import { isAgentWritableTool } from 'app/features/agent';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import dayjs from 'dayjs';
import styled from 'styled-components';
import {
  BORDER_RADIUS,
  SPACE_MD,
  SPACE_SM,
  SPACE_XS,
} from 'styles/StyleConstants';

interface ApprovalCardProps {
  approval: AgentApproval;
  decision?: 'approve' | 'reject';
  decisionFailure?: AgentClientFailure;
  now: number;
  onApprove: (approvalId: string) => void;
  onOpenChange?: (resourceType: string, resourceId: string) => void;
  onReject: (approvalId: string) => void;
}

const statusColor: Record<AgentApprovalStatus, string> = {
  PENDING: 'processing',
  SUCCEEDED: 'success',
  REJECTED: 'default',
  EXPIRED: 'warning',
  FAILED: 'error',
};

export function ApprovalCard({
  approval,
  decision,
  decisionFailure,
  now,
  onApprove,
  onOpenChange,
  onReject,
}: ApprovalCardProps) {
  const t = useI18NPrefix('agent');
  const unsupported = !isAgentWritableTool(approval.toolName);
  const expired =
    approval.status === 'PENDING' && Date.parse(approval.expiresAt) <= now;
  const status: AgentApprovalStatus = expired ? 'EXPIRED' : approval.status;
  const pending = status === 'PENDING' && !unsupported;
  const statusLabel = t(`status.${status.toLowerCase()}`);

  return (
    <Card aria-label={approval.preview.title}>
      <CardHeader>
        <SafetyCertificateOutlined />
        <TitleBlock className="approval-title">
          <Typography.Title level={4} ellipsis={{ tooltip: true }}>
            {approval.preview.title}
          </Typography.Title>
          <Typography.Text type="secondary">
            {approval.preview.summary}
          </Typography.Text>
        </TitleBlock>
        <Space size={8} wrap>
          <Tag>{approval.toolName}</Tag>
          {approval.duplicate && <Tag color="gold">{t('duplicate')}</Tag>}
          <Tag color={statusColor[status]}>{statusLabel}</Tag>
        </Space>
      </CardHeader>

      {unsupported && (
        <Alert
          type="error"
          showIcon
          title={t('unsupportedTool')}
          description={approval.toolName}
        />
      )}

      <MetaRow>
        <Typography.Text type="secondary">
          {t('approval.createdAt')}{' '}
          {dayjs(approval.createdAt).format('YYYY-MM-DD HH:mm:ss')}
        </Typography.Text>
        <Typography.Text type="secondary">
          {t('approval.expiresAt')}{' '}
          {dayjs(approval.expiresAt).format('YYYY-MM-DD HH:mm:ss')}
        </Typography.Text>
      </MetaRow>

      <Divider />
      <Typography.Title level={5}>{t('approval.parameters')}</Typography.Title>
      {approval.preview.parameters.length > 0 ? (
        <Descriptions
          size="small"
          column={1}
          items={approval.preview.parameters.map(parameter => ({
            key: parameter.name,
            label: parameter.label,
            children: <ValueText>{parameter.value}</ValueText>,
          }))}
        />
      ) : (
        <Typography.Text type="secondary">{t('empty')}</Typography.Text>
      )}

      <Typography.Title level={5}>{t('approval.impacts')}</Typography.Title>
      {approval.preview.impacts.length > 0 ? (
        <ImpactList>
          {approval.preview.impacts.map((impact, index) => (
            <li key={`${impact.resourceType}-${impact.resourceId}-${index}`}>
              <Typography.Text strong>{impact.action}</Typography.Text>
              <Typography.Text>{impact.description}</Typography.Text>
              <Typography.Text type="secondary">
                {impact.resourceType} / {impact.resourceId}
              </Typography.Text>
            </li>
          ))}
        </ImpactList>
      ) : (
        <Typography.Text type="secondary">{t('empty')}</Typography.Text>
      )}

      {approval.failure && (
        <Alert
          type="error"
          showIcon
          title={approval.failure.code}
          description={approval.failure.message}
        />
      )}
      {decisionFailure && (
        <Alert
          type="error"
          showIcon
          title={decisionFailure.code}
          description={decisionFailure.message}
        />
      )}

      {approval.change && (
        <ChangeBlock>
          <Typography.Title level={5}>{t('approval.change')}</Typography.Title>
          <Descriptions
            size="small"
            column={1}
            items={[
              {
                key: 'changeId',
                label: t('approval.changeId'),
                children: approval.change.changeId,
              },
              {
                key: 'resource',
                label: t('approval.resource'),
                children: `${approval.change.resourceType} / ${approval.change.resourceId}`,
              },
              {
                key: 'action',
                label: t('approval.action'),
                children: approval.change.action,
              },
              {
                key: 'finalStatus',
                label: t('approval.finalStatus'),
                children: approval.change.finalStatus,
              },
            ]}
          />
          {onOpenChange && (
            <Button
              icon={<LinkOutlined />}
              onClick={() =>
                onOpenChange(
                  approval.change!.resourceType,
                  approval.change!.resourceId,
                )
              }
            >
              {t('approval.openResource')}
            </Button>
          )}
        </ChangeBlock>
      )}

      {pending && (
        <Actions wrap>
          <Button
            danger
            icon={<CloseOutlined />}
            loading={decision === 'reject'}
            disabled={Boolean(decision)}
            onClick={() => onReject(approval.approvalId)}
          >
            {t('approval.reject')}
          </Button>
          <Button
            type="primary"
            icon={<CheckOutlined />}
            loading={decision === 'approve'}
            disabled={Boolean(decision)}
            onClick={() => onApprove(approval.approvalId)}
          >
            {t('approval.approve')}
          </Button>
        </Actions>
      )}
    </Card>
  );
}

const Card = styled.article`
  display: flex;
  flex-direction: column;
  gap: ${SPACE_SM};
  padding: ${SPACE_MD};
  background: ${p => p.theme.componentBackground};
  border: 1px solid ${p => p.theme.borderColorSplit};
  border-radius: ${BORDER_RADIUS};

  .ant-descriptions-item-content {
    overflow-wrap: anywhere;
  }
`;

const CardHeader = styled.header`
  display: flex;
  gap: ${SPACE_SM};
  align-items: flex-start;

  > .anticon {
    flex-shrink: 0;
    margin-top: 3px;
    color: ${p => p.theme.primary};
  }

  @media (max-width: 480px) {
    flex-wrap: wrap;

    .approval-title {
      flex-basis: calc(100% - 32px);
    }

    > .ant-space {
      width: 100%;
      padding-left: 32px;
    }
  }
`;

const TitleBlock = styled.div`
  flex: 1;
  min-width: 0;

  .ant-typography {
    margin: 0;
  }
`;

const MetaRow = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${SPACE_XS} ${SPACE_MD};
`;

const ValueText = styled(Typography.Text)`
  overflow-wrap: anywhere;
`;

const ImpactList = styled.ul`
  display: grid;
  gap: ${SPACE_XS};
  padding: 0;
  margin: 0;
  list-style: none;

  li {
    display: grid;
    grid-template-columns: minmax(80px, auto) minmax(0, 1fr);
    gap: ${SPACE_XS};
    align-items: baseline;
    padding: ${SPACE_XS} 0;
    border-bottom: 1px solid ${p => p.theme.borderColorSplit};

    > * {
      min-width: 0;
      overflow-wrap: anywhere;
    }

    > :last-child {
      grid-column: 1 / -1;
    }
  }

  @media (max-width: 767px) {
    li {
      grid-template-columns: minmax(0, 1fr);
    }
  }
`;

const ChangeBlock = styled.section`
  display: flex;
  flex-direction: column;
  gap: ${SPACE_XS};
  padding-top: ${SPACE_XS};
  border-top: 1px solid ${p => p.theme.borderColorSplit};

  .ant-typography {
    margin: 0;
  }

  .ant-btn {
    align-self: flex-start;
  }
`;

const Actions = styled(Space)`
  justify-content: flex-end;
  padding-top: ${SPACE_XS};

  @media (max-width: 480px) {
    .ant-btn {
      flex: 1;
      min-width: 0;
    }
  }
`;
