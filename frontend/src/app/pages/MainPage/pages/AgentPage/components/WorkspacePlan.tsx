import { Steps } from 'antd';
import type { AgentApproval } from 'app/features/agent';
import useI18NPrefix from 'app/hooks/useI18NPrefix';

interface WorkspacePlanProps {
  approvals: AgentApproval[];
  previewLoading: boolean;
  sessionReady: boolean;
}

export function WorkspacePlan({
  approvals,
  previewLoading,
  sessionReady,
}: WorkspacePlanProps) {
  const t = useI18NPrefix('agent');
  const hasApproval = approvals.length > 0;
  const hasPending = approvals.some(item => item.status === 'PENDING');
  const hasChange = approvals.some(item => item.change);
  const current = !sessionReady
    ? 0
    : previewLoading || !hasApproval
      ? 1
      : hasPending || !hasChange
        ? 2
        : 3;

  return (
    <Steps
      current={current}
      orientation="vertical"
      size="small"
      items={[
        {
          title: t('plan.session'),
          content: t('plan.sessionDescription'),
        },
        {
          title: t('plan.preview'),
          content: t('plan.previewDescription'),
        },
        {
          title: t('plan.approval'),
          content: t('plan.approvalDescription'),
        },
        {
          title: t('plan.change'),
          content: t('plan.changeDescription'),
        },
      ]}
    />
  );
}
