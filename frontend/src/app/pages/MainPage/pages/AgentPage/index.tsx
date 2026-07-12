import { PlusOutlined, ReloadOutlined, RobotOutlined } from '@ant-design/icons';
import {
  Alert,
  Button,
  Empty,
  Result,
  Skeleton,
  Spin,
  Tooltip,
  Typography,
} from 'antd';
import type { AgentWritePreviewRequest } from 'app/features/agent';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useAppDispatch } from 'app/hooks/useRedux';
import { useParams } from 'app/routerCompat';
import {
  selectOrganizations,
  selectOrgId,
  selectUserSettings,
} from 'app/pages/MainPage/slice/selectors';
import { switchOrganization } from 'app/pages/MainPage/slice/thunks';
import { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import styled from 'styled-components';
import {
  FONT_SIZE_HEADING,
  FONT_WEIGHT_MEDIUM,
  SPACE_LG,
  SPACE_MD,
  SPACE_SM,
  SPACE_XL,
  SPACE_XS,
} from 'styles/StyleConstants';
import { ApprovalCard } from './components/ApprovalCard';
import { GuidedWriteForm } from './components/GuidedWriteForm';
import { ReadOnlyRunPanel } from './components/ReadOnlyRunPanel';
import { WorkspacePlan } from './components/WorkspacePlan';
import { agentWorkspaceActions, useAgentWorkspaceSlice } from './slice';
import {
  selectAgentApprovals,
  selectAgentPreviewIntent,
  selectAgentSession,
  selectAgentWorkspace,
} from './slice/selectors';
import {
  decideAgentApproval,
  fetchAgentApprovals,
  runAgentWorkspaceReadOnly,
  startAgentWorkspace,
  submitAgentWritePreview,
} from './slice/thunks';

export function AgentPage() {
  useAgentWorkspaceSlice();
  const t = useI18NPrefix('agent');
  const dispatch = useAppDispatch();
  const navigate = useCompatNavigate();
  const { orgId: routeOrgId = '' } = useParams<{ orgId?: string }>();
  const selectedOrgId = useSelector(selectOrgId);
  const organizations = useSelector(selectOrganizations);
  const userSettings = useSelector(selectUserSettings);
  const workspace = useSelector(selectAgentWorkspace);
  const session = useSelector(selectAgentSession);
  const approvals = useSelector(selectAgentApprovals);
  const previewIntent = useSelector(selectAgentPreviewIntent);
  const [now, setNow] = useState(() => Date.now());
  const membershipKnown = userSettings !== undefined;
  const routeOrganization = organizations.find(
    organization => organization.id === routeOrgId,
  );
  const requestOrgId =
    routeOrganization && selectedOrgId === routeOrgId ? routeOrgId : undefined;

  useEffect(() => {
    if (membershipKnown && routeOrganization && selectedOrgId !== routeOrgId) {
      dispatch(switchOrganization(routeOrgId));
    }
  }, [dispatch, membershipKnown, routeOrgId, routeOrganization, selectedOrgId]);

  useEffect(() => {
    if (!requestOrgId) {
      dispatch(agentWorkspaceActions.clearWorkspace());
      return;
    }
    dispatch(startAgentWorkspace(requestOrgId));
    return () => {
      dispatch(agentWorkspaceActions.clearWorkspace());
    };
  }, [dispatch, requestOrgId]);

  useEffect(() => {
    if (requestOrgId && session?.sessionId) {
      dispatch(fetchAgentApprovals(requestOrgId));
    }
  }, [dispatch, requestOrgId, session?.sessionId]);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  if (!membershipKnown) {
    return (
      <PageContainer>
        <LoadingContent>
          <Skeleton active paragraph={{ rows: 8 }} />
        </LoadingContent>
      </PageContainer>
    );
  }

  if (!routeOrganization) {
    return (
      <PageContainer>
        <Result
          status="403"
          title={t('scope.denied')}
          subTitle={t('scope.deniedDescription')}
        />
      </PageContainer>
    );
  }

  if (!requestOrgId) {
    return (
      <PageContainer>
        <LoadingContent>
          <Skeleton active paragraph={{ rows: 8 }} />
        </LoadingContent>
      </PageContainer>
    );
  }

  const orgId = requestOrgId;
  const createSession = () => dispatch(startAgentWorkspace(orgId));
  const sessionExpired = Boolean(
    session && Date.parse(session.expiresAt) <= now,
  );
  const sessionReady =
    workspace.sessionStatus === 'ready' && Boolean(session) && !sessionExpired;

  const preview = (
    request: AgentWritePreviewRequest,
    idempotencyKey: string,
  ) => {
    dispatch(submitAgentWritePreview({ orgId, request, idempotencyKey }));
  };

  const decide = (approvalId: string, decision: 'approve' | 'reject') => {
    dispatch(decideAgentApproval({ orgId, approvalId, decision }));
  };

  const openResource = (resourceType: string, resourceId: string) => {
    if (['CHART', 'DATACHART', 'DASHBOARD'].includes(resourceType)) {
      navigate.push(`/organizations/${orgId}/vizs/${resourceId}`);
    }
  };

  if (workspace.sessionStatus === 'loading') {
    return (
      <PageContainer>
        <LoadingContent>
          <Skeleton active paragraph={{ rows: 8 }} />
        </LoadingContent>
      </PageContainer>
    );
  }

  if (workspace.sessionStatus === 'failed') {
    return (
      <PageContainer>
        <Result
          status="error"
          title={t('session.failed')}
          subTitle={workspace.sessionFailure?.message}
          extra={
            <Button
              type="primary"
              icon={<ReloadOutlined />}
              onClick={createSession}
            >
              {t('retry')}
            </Button>
          }
        />
      </PageContainer>
    );
  }

  return (
    <PageContainer>
      <PageHeader>
        <HeaderIdentity>
          <RobotOutlined />
          <div>
            <h1>{t('title')}</h1>
            <Typography.Text type="secondary">{t('subtitle')}</Typography.Text>
          </div>
        </HeaderIdentity>
        <Tooltip title={t('session.new')}>
          <Button icon={<PlusOutlined />} onClick={createSession}>
            {t('session.new')}
          </Button>
        </Tooltip>
      </PageHeader>

      <WorkspaceGrid>
        <ControlPane>
          {!session?.modelRuntimeAvailable && (
            <Alert
              type="info"
              showIcon
              title={t('runtime.unavailable')}
              description={t('runtime.guidedAvailable')}
            />
          )}
          {sessionExpired && (
            <Alert
              type="warning"
              showIcon
              title={t('session.expired')}
              action={
                <Button size="small" onClick={createSession}>
                  {t('session.new')}
                </Button>
              }
            />
          )}

          <Section>
            <SectionHeader>
              <div>
                <h2>{t('readonly.title')}</h2>
                <Typography.Text type="secondary">
                  {t('readonly.description')}
                </Typography.Text>
              </div>
            </SectionHeader>
            <ReadOnlyRunPanel
              key={session?.sessionId || 'no-session'}
              runtimeAvailable={Boolean(session?.modelRuntimeAvailable)}
              sessionReady={sessionReady}
              status={workspace.runStatus}
              run={workspace.run}
              failure={workspace.runFailure}
              onRun={message =>
                dispatch(runAgentWorkspaceReadOnly({ orgId, message }))
              }
            />
          </Section>

          <Section>
            <SectionHeader>
              <div>
                <h2>{t('plan.title')}</h2>
                <Typography.Text type="secondary">
                  {t('plan.description')}
                </Typography.Text>
              </div>
            </SectionHeader>
            <WorkspacePlan
              approvals={approvals}
              previewLoading={workspace.previewLoading}
              sessionReady={sessionReady}
            />
          </Section>

          <Section>
            <SectionHeader>
              <div>
                <h2>{t('form.title')}</h2>
                <Typography.Text type="secondary">
                  {t('form.description')}
                </Typography.Text>
              </div>
            </SectionHeader>
            {session?.writableTools.length === 0 ? (
              <Alert type="warning" showIcon title={t('noWritableTools')} />
            ) : (
              <GuidedWriteForm
                availableTools={session?.writableTools || []}
                disabled={!sessionReady}
                loading={workspace.previewLoading}
                previousIntent={previewIntent}
                onSubmit={preview}
              />
            )}
            {workspace.previewFailure && (
              <Alert
                type="error"
                showIcon
                closable
                title={workspace.previewFailure.code}
                description={workspace.previewFailure.message}
                onClose={() =>
                  dispatch(agentWorkspaceActions.clearPreviewFailure())
                }
              />
            )}
          </Section>
        </ControlPane>

        <ActivityPane>
          <ActivityHeader>
            <div>
              <h2>{t('activity.title')}</h2>
              <Typography.Text type="secondary">
                {t('activity.description')}
              </Typography.Text>
            </div>
            <Tooltip title={t('refresh')}>
              <Button
                aria-label={t('refresh')}
                icon={<ReloadOutlined />}
                loading={workspace.approvalsLoading}
                disabled={!sessionReady}
                onClick={() => dispatch(fetchAgentApprovals(orgId))}
              />
            </Tooltip>
          </ActivityHeader>

          {workspace.approvalsFailure && (
            <Alert
              type="error"
              showIcon
              title={workspace.approvalsFailure.code}
              description={workspace.approvalsFailure.message}
            />
          )}

          <Spin spinning={workspace.approvalsLoading}>
            <ApprovalList aria-live="polite">
              {approvals.length === 0 && !workspace.approvalsLoading ? (
                <Empty description={t('activity.empty')} />
              ) : (
                approvals.map(approval => (
                  <ApprovalCard
                    key={approval.approvalId}
                    approval={approval}
                    decision={workspace.decisions[approval.approvalId]}
                    decisionFailure={
                      workspace.decisionFailures[approval.approvalId]
                    }
                    now={now}
                    onApprove={approvalId => decide(approvalId, 'approve')}
                    onReject={approvalId => decide(approvalId, 'reject')}
                    onOpenChange={openResource}
                  />
                ))
              )}
            </ApprovalList>
          </Spin>
        </ActivityPane>
      </WorkspaceGrid>
    </PageContainer>
  );
}

const PageContainer = styled.div`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: ${p => p.theme.bodyBackground};
`;

const LoadingContent = styled.div`
  width: min(960px, 100%);
  padding: ${SPACE_XL};
  margin: auto;
`;

const PageHeader = styled.header`
  display: flex;
  flex-shrink: 0;
  gap: ${SPACE_MD};
  align-items: center;
  justify-content: space-between;
  padding: ${SPACE_MD} ${SPACE_LG};
  background: ${p => p.theme.componentBackground};
  border-bottom: 1px solid ${p => p.theme.borderColorSplit};

  @media (max-width: 480px) {
    align-items: flex-start;
    padding: ${SPACE_SM};

    .ant-btn > span:not(.anticon) {
      display: none;
    }
  }
`;

const HeaderIdentity = styled.div`
  display: flex;
  gap: ${SPACE_SM};
  align-items: center;
  min-width: 0;

  > div {
    min-width: 0;
  }

  > .anticon {
    flex-shrink: 0;
    font-size: 28px;
    color: ${p => p.theme.primary};
  }

  h1 {
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: ${FONT_SIZE_HEADING};
    font-weight: ${FONT_WEIGHT_MEDIUM};
    white-space: nowrap;
  }
`;

const WorkspaceGrid = styled.div`
  display: grid;
  flex: 1;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 420px);
  min-width: 0;
  min-height: 0;
  overflow: hidden;

  @media (max-width: 1023px) {
    display: block;
    overflow: auto;
  }
`;

const Pane = styled.div`
  min-width: 0;
  min-height: 0;
  padding: ${SPACE_LG};
  overflow: auto;

  @media (max-width: 767px) {
    padding: ${SPACE_SM};
    overflow: visible;
  }
`;

const ControlPane = styled(Pane)`
  display: flex;
  flex-direction: column;
  gap: ${SPACE_LG};
`;

const ActivityPane = styled(Pane)`
  display: flex;
  flex-direction: column;
  gap: ${SPACE_MD};
  background: ${p => p.theme.componentBackground};
  border-left: 1px solid ${p => p.theme.borderColorSplit};

  @media (max-width: 1023px) {
    border-top: 1px solid ${p => p.theme.borderColorSplit};
    border-left: 0;
  }
`;

const Section = styled.section`
  display: flex;
  flex-direction: column;
  gap: ${SPACE_MD};
  padding-bottom: ${SPACE_LG};
  border-bottom: 1px solid ${p => p.theme.borderColorSplit};

  &:last-child {
    border-bottom: 0;
  }
`;

const SectionHeader = styled.header`
  display: flex;
  gap: ${SPACE_SM};
  align-items: flex-start;
  justify-content: space-between;

  h2 {
    margin: 0 0 ${SPACE_XS};
    font-size: ${FONT_SIZE_HEADING};
    font-weight: ${FONT_WEIGHT_MEDIUM};
  }
`;

const ActivityHeader = styled(SectionHeader)`
  flex-shrink: 0;
`;

const ApprovalList = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${SPACE_MD};
  min-height: 160px;

  > .ant-empty {
    margin: auto;
  }
`;
