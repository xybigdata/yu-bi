import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  PlayCircleOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Empty,
  Input,
  Spin,
  Steps,
  Tag,
  Typography,
} from 'antd';
import type {
  AgentClientFailure,
  AgentRunPlanStep,
  AgentRunStep,
  AgentRunToolName,
  AgentWorkspaceRun,
} from 'app/features/agent';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useState } from 'react';
import styled from 'styled-components';
import {
  FONT_WEIGHT_MEDIUM,
  SPACE_MD,
  SPACE_SM,
  SPACE_XS,
} from 'styles/StyleConstants';
import type { AgentRunLoadStatus } from '../slice/types';

interface ReadOnlyRunPanelProps {
  runtimeAvailable: boolean;
  sessionReady: boolean;
  status: AgentRunLoadStatus;
  run?: AgentWorkspaceRun;
  failure?: AgentClientFailure;
  onRun: (message: string) => void;
}

const toolExecutions = (run?: AgentWorkspaceRun) =>
  run?.steps.filter(step => step.kind === 'TOOL_CALL') || [];

const isEmptyResult = (step: AgentRunStep) => {
  const data = step.result?.data;
  if (!data || Object.keys(data).length === 0) {
    return true;
  }
  if (step.toolName === 'search_data_assets') {
    return Array.isArray(data.assets) && data.assets.length === 0;
  }
  if (step.toolName === 'execute_view') {
    return Array.isArray(data.rows) && data.rows.length === 0;
  }
  return false;
};

export function ReadOnlyRunPanel({
  runtimeAvailable,
  sessionReady,
  status,
  run,
  failure,
  onRun,
}: ReadOnlyRunPanelProps) {
  const t = useI18NPrefix('agent.readonly');
  const [message, setMessage] = useState('');
  const running = status === 'running';
  const disabled = !runtimeAvailable || !sessionReady;
  const executions = toolExecutions(run);

  const submit = () => {
    const normalized = message.trim();
    if (normalized && !disabled && !running) {
      onRun(normalized);
    }
  };

  return (
    <Panel>
      <PromptArea>
        <Input.TextArea
          aria-label={t('prompt')}
          value={message}
          rows={3}
          maxLength={8000}
          disabled={disabled}
          placeholder={t('placeholder')}
          onChange={event => setMessage(event.target.value)}
        />
        <Button
          type="primary"
          icon={<PlayCircleOutlined />}
          loading={running}
          disabled={disabled || !message.trim()}
          onClick={submit}
        >
          {t('run')}
        </Button>
      </PromptArea>

      {!runtimeAvailable && (
        <Typography.Text type="secondary">{t('disabled')}</Typography.Text>
      )}

      {status === 'failed' && failure && (
        <Alert
          type="error"
          showIcon
          title={failure.code}
          description={failure.message}
        />
      )}

      <Spin spinning={running}>
        {run && (
          <RunSurface aria-live="polite">
            <RunHeader>
              <Tag color={run.status === 'COMPLETED' ? 'success' : 'error'}>
                {t(`status.${run.status.toLowerCase()}`)}
              </Tag>
              <Typography.Text type="secondary" copyable>
                {run.runId}
              </Typography.Text>
            </RunHeader>

            {run.resultSize.truncated && (
              <Alert type="warning" showIcon title={t('truncated')} />
            )}

            <RunSection>
              <h3>{t('plan')}</h3>
              {run.plan.length === 0 ? (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={t('planEmpty')}
                />
              ) : (
                <Steps
                  size="small"
                  orientation="vertical"
                  items={run.plan.map(step => ({
                    status: step.status === 'SUCCEEDED' ? 'finish' : 'error',
                    title: planTitle(step, t),
                    content: t(`stepStatus.${step.status.toLowerCase()}`),
                  }))}
                />
              )}
            </RunSection>

            <RunSection>
              <h3>{t('tools')}</h3>
              {executions.length === 0 ? (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={t('toolsEmpty')}
                />
              ) : (
                <ExecutionList>
                  {executions.map(step => (
                    <Execution key={step.index}>
                      <ExecutionHeader>
                        <span>
                          {step.status === 'SUCCEEDED' ? (
                            <CheckCircleOutlined />
                          ) : (
                            <CloseCircleOutlined />
                          )}
                          <strong>{toolLabel(step.toolName, t)}</strong>
                        </span>
                        <span>
                          {step.result?.size.truncated && (
                            <Tag color="warning">{t('truncatedShort')}</Tag>
                          )}
                          <Typography.Text type="secondary">
                            {step.durationMillis} ms
                          </Typography.Text>
                        </span>
                      </ExecutionHeader>
                      {step.failure && (
                        <Alert
                          type="error"
                          showIcon
                          title={step.failure.code}
                          description={step.failure.message}
                        />
                      )}
                      {step.result &&
                        (isEmptyResult(step) ? (
                          <Empty
                            image={Empty.PRESENTED_IMAGE_SIMPLE}
                            description={t('dataEmpty')}
                          />
                        ) : (
                          <DataResult>
                            {JSON.stringify(step.result.data, null, 2)}
                          </DataResult>
                        ))}
                    </Execution>
                  ))}
                </ExecutionList>
              )}
            </RunSection>

            {run.finalAnswer && (
              <RunSection>
                <h3>{t('answer')}</h3>
                <Answer>{run.finalAnswer}</Answer>
              </RunSection>
            )}

            {run.failure && (
              <Alert
                type="error"
                showIcon
                title={run.failure.code}
                description={run.failure.message}
              />
            )}
          </RunSurface>
        )}
      </Spin>
    </Panel>
  );
}

const planTitle = (step: AgentRunPlanStep, t: (key: string) => string) =>
  step.kind === 'TOOL_CALL'
    ? toolLabel(step.toolName, t)
    : t(`stepKind.${step.kind.toLowerCase()}`);

const toolLabel = (
  toolName: AgentRunToolName | null | undefined,
  t: (key: string) => string,
) => (toolName ? t(`tool.${toolName}`) : t('tool.unknown'));

const Panel = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${SPACE_MD};
  min-width: 0;
`;

const PromptArea = styled.div`
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: ${SPACE_SM};
  align-items: end;

  @media (max-width: 480px) {
    grid-template-columns: minmax(0, 1fr);

    .ant-btn {
      width: 100%;
    }
  }
`;

const RunSurface = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${SPACE_MD};
  min-width: 0;
  padding-top: ${SPACE_SM};
`;

const RunHeader = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${SPACE_SM};
  align-items: center;
  justify-content: space-between;
`;

const RunSection = styled.section`
  min-width: 0;

  h3 {
    margin: 0 0 ${SPACE_SM};
    font-size: 14px;
    font-weight: ${FONT_WEIGHT_MEDIUM};
  }

  .ant-empty {
    margin-block: ${SPACE_SM};
  }
`;

const ExecutionList = styled.div`
  display: flex;
  flex-direction: column;
`;

const Execution = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${SPACE_SM};
  min-width: 0;
  padding: ${SPACE_SM} 0;
  border-top: 1px solid ${p => p.theme.borderColorSplit};

  &:last-child {
    border-bottom: 1px solid ${p => p.theme.borderColorSplit};
  }
`;

const ExecutionHeader = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${SPACE_SM};
  align-items: center;
  justify-content: space-between;

  > span {
    display: inline-flex;
    gap: ${SPACE_XS};
    align-items: center;
  }

  > span:first-child .anticon-check-circle {
    color: ${p => p.theme.success};
  }

  > span:first-child .anticon-close-circle {
    color: ${p => p.theme.error};
  }
`;

const DataResult = styled.pre`
  max-width: 100%;
  max-height: 360px;
  padding: ${SPACE_SM};
  margin: 0;
  overflow: auto;
  font-family: SFMono-Regular, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  line-height: 1.6;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  background: ${p => p.theme.bodyBackground};
  border: 1px solid ${p => p.theme.borderColorSplit};
  border-radius: 4px;
`;

const Answer = styled(Typography.Paragraph)`
  margin-bottom: 0 !important;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
`;
