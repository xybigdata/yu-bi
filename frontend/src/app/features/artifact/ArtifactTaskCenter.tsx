import {
  CopyOutlined,
  DeleteOutlined,
  DownloadOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import {
  Badge,
  Button,
  Drawer,
  Empty,
  Grid,
  Modal,
  Popover,
  Tag,
  Tooltip,
} from 'antd';
import dayjs from 'dayjs';
import {
  useEffect,
  useMemo,
  useRef,
  useState,
  useSyncExternalStore,
} from 'react';
import styled, { keyframes } from 'styled-components';
import type {
  ArtifactTaskCenterState,
  ArtifactTaskCoordinator,
  ArtifactTaskSnapshot,
} from './ArtifactTaskCoordinator';
import { ArtifactRetryCleanupError } from './ArtifactTaskCoordinator';
import { getArtifactTaskCoordinator } from './ArtifactTaskRuntime';

export interface ArtifactTaskCenterProps {
  coordinator?: ArtifactTaskCoordinator;
  floating?: boolean;
}

export function ArtifactTaskCenter({
  coordinator = getArtifactTaskCoordinator(),
  floating = false,
}: ArtifactTaskCenterProps) {
  const state = useSyncExternalStore(
    coordinator.subscribe,
    coordinator.getState,
    coordinator.getState,
  );
  const screens = Grid.useBreakpoint();
  const compact = screens.md === false;
  const [open, setOpen] = useState(false);
  const [failureHint, setFailureHint] = useState(false);
  const [operationErrors, setOperationErrors] = useState<
    Record<string, string>
  >({});
  const seenFailures = useRef(new Set<string>());
  const failureHintDeadline = useRef<number | undefined>(undefined);
  const failedTasks = state.tasks.filter(task => isFailed(task.status));
  const failedTaskSignature = JSON.stringify(failedTasks.map(task => task.id));
  const running = state.tasks.some(task => !isTerminal(task.status));

  useEffect(() => {
    const failedTaskIds = JSON.parse(failedTaskSignature) as string[];
    if (!failedTaskIds.length) {
      failureHintDeadline.current = undefined;
      setFailureHint(false);
      return;
    }
    const newFailures = failedTaskIds.filter(
      taskId => !seenFailures.current.has(taskId),
    );
    failedTaskIds.forEach(taskId => seenFailures.current.add(taskId));
    if (newFailures.length) {
      failureHintDeadline.current = Date.now() + 5000;
      setFailureHint(true);
    }
    const deadline = failureHintDeadline.current;
    if (deadline === undefined) {
      return;
    }
    const remaining = deadline - Date.now();
    if (remaining <= 0) {
      failureHintDeadline.current = undefined;
      setFailureHint(false);
      return;
    }
    const timer = window.setTimeout(() => {
      failureHintDeadline.current = undefined;
      setFailureHint(false);
    }, remaining);
    return () => window.clearTimeout(timer);
  }, [failedTaskSignature]);

  const panel = useMemo(
    () => (
      <TaskPanel
        state={state}
        coordinator={coordinator}
        operationErrors={operationErrors}
        onOperationError={(taskId, message) =>
          setOperationErrors(current => ({ ...current, [taskId]: message }))
        }
      />
    ),
    [coordinator, operationErrors, state],
  );

  const trigger = (
    <TriggerWrap $floating={floating}>
      {failureHint && <FailureHint>导出失败，点击查看详情</FailureHint>}
      <Badge dot={failedTasks.length > 0} color="#d9363e" offset={[-2, 3]}>
        <Tooltip title="下载" placement="right">
          <DownloadButton
            aria-label="下载"
            type="text"
            $running={running}
            icon={<DownloadOutlined />}
            onClick={() => setOpen(value => !value)}
          />
        </Tooltip>
      </Badge>
    </TriggerWrap>
  );

  if (compact) {
    return (
      <>
        {trigger}
        <Drawer
          title="下载详情"
          placement="bottom"
          open={open}
          onClose={() => setOpen(false)}
          styles={{ wrapper: { height: '60vh' } }}
        >
          {panel}
        </Drawer>
      </>
    );
  }

  return (
    <Popover
      content={panel}
      trigger="click"
      placement="rightBottom"
      open={open}
      onOpenChange={setOpen}
      styles={{ container: { width: 360, maxWidth: 'calc(100vw - 80px)' } }}
    >
      {trigger}
    </Popover>
  );
}

function TaskPanel({
  state,
  coordinator,
  operationErrors,
  onOperationError,
}: {
  state: ArtifactTaskCenterState;
  coordinator: ArtifactTaskCoordinator;
  operationErrors: Record<string, string>;
  onOperationError: (taskId: string, message: string) => void;
}) {
  const finished = state.tasks.filter(task => isTerminal(task.status));

  const clearFinished = () => {
    Modal.confirm({
      title: '清除已结束任务？',
      content: '任务记录和服务端文件将直接删除，且无法恢复。',
      okText: '清除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        for (const task of finished) {
          try {
            await coordinator.deleteTask(task.id);
          } catch (error) {
            onOperationError(
              task.id,
              operationErrorMessage(
                error,
                'ARTIFACT_DELETE_FAILED',
                '清除失败',
              ),
            );
          }
        }
      },
    });
  };

  return (
    <Panel>
      <PanelHeader>
        <strong>下载详情</strong>
        <Button
          size="small"
          type="text"
          danger
          disabled={!finished.length}
          icon={<DeleteOutlined />}
          onClick={clearFinished}
        >
          清除已结束任务
        </Button>
      </PanelHeader>
      <TaskList>
        {!state.tasks.length && !state.loading ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="暂无下载任务"
          />
        ) : (
          state.tasks.map(task => (
            <TaskRow
              key={task.id}
              task={task}
              coordinator={coordinator}
              operationError={operationErrors[task.id]}
              onOperationError={message => onOperationError(task.id, message)}
            />
          ))
        )}
      </TaskList>
      {state.nextOffset !== undefined && (
        <LoadMore
          block
          type="text"
          loading={state.loading}
          onClick={() => void coordinator.loadMore()}
        >
          加载更多
        </LoadMore>
      )}
    </Panel>
  );
}

function TaskRow({
  task,
  coordinator,
  operationError,
  onOperationError,
}: {
  task: ArtifactTaskSnapshot;
  coordinator: ArtifactTaskCoordinator;
  operationError?: string;
  onOperationError: (message: string) => void;
}) {
  const remove = () => {
    Modal.confirm({
      title: '清除该任务？',
      content: '任务记录和服务端文件将直接删除，且无法恢复。',
      okText: '清除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: () =>
        coordinator.deleteTask(task.id).catch(error => {
          onOperationError(
            operationErrorMessage(error, 'ARTIFACT_DELETE_FAILED', '清除失败'),
          );
        }),
    });
  };
  const copyError = () => {
    if (!task.error) {
      return;
    }
    const details = [`错误码：${task.error.code}`];
    if (task.error.message) {
      details.push(`错误原因：${task.error.message}`);
    }
    details.push(`追踪 ID：${task.error.traceId}`);
    void navigator.clipboard.writeText(details.join('\n'));
  };

  return (
    <TaskItem>
      <TaskSummary>
        <FileName title={task.fileName}>
          {task.fileName ?? '未命名文件'}
        </FileName>
        <Tag color={statusColor(task.status)}>{statusLabel(task.status)}</Tag>
      </TaskSummary>
      <Meta>
        <span>{sourceLabel(task.source)}</span>
        <span>{formatLabel(task.fileName)}</span>
        <span>{dayjs(task.createdAt).format('YYYY-MM-DD HH:mm:ss')}</span>
      </Meta>
      {task.completedAt && (
        <Meta>
          完成于 {dayjs(task.completedAt).format('YYYY-MM-DD HH:mm:ss')}
        </Meta>
      )}
      {task.error && (
        <ErrorDetails>
          <ErrorLine>
            <span>错误码</span>
            <code>{task.error.code}</code>
          </ErrorLine>
          {task.error.message && (
            <ErrorLine>
              <span>错误原因</span>
              <span>{task.error.message}</span>
            </ErrorLine>
          )}
          <ErrorLine>
            <span>追踪 ID</span>
            <code>{task.error.traceId}</code>
          </ErrorLine>
        </ErrorDetails>
      )}
      {operationError && <OperationError>{operationError}</OperationError>}
      <Actions>
        {task.status === 'SUCCEEDED' && (
          <Tooltip title="重新下载">
            <Button
              aria-label="重新下载"
              size="small"
              type="text"
              icon={<DownloadOutlined />}
              onClick={() =>
                void coordinator.downloadNow(task.id).catch(error => {
                  onOperationError(
                    operationErrorMessage(
                      error,
                      'ARTIFACT_DOWNLOAD_FAILED',
                      '下载失败',
                    ),
                  );
                })
              }
            />
          </Tooltip>
        )}
        {isFailed(task.status) && coordinator.canRetryGeneration(task.id) && (
          <Button
            size="small"
            type="link"
            icon={<ReloadOutlined />}
            onClick={() =>
              void coordinator.retryGeneration(task.id).catch(error => {
                const cleanupFailed =
                  error instanceof ArtifactRetryCleanupError;
                onOperationError(
                  operationErrorMessage(
                    cleanupFailed ? error.cause : error,
                    cleanupFailed
                      ? 'ARTIFACT_RETRY_CLEANUP_FAILED'
                      : 'ARTIFACT_RETRY_FAILED',
                    cleanupFailed ? '重试已开始，但旧任务清理失败' : '重试失败',
                  ),
                );
              })
            }
          >
            重试生成
          </Button>
        )}
        {task.error && (
          <Tooltip title="复制错误信息">
            <Button
              aria-label="复制错误信息"
              size="small"
              type="text"
              icon={<CopyOutlined />}
              onClick={copyError}
            />
          </Tooltip>
        )}
        {isTerminal(task.status) && (
          <Tooltip title="清除">
            <Button
              aria-label="清除"
              size="small"
              type="text"
              danger
              icon={<DeleteOutlined />}
              onClick={remove}
            />
          </Tooltip>
        )}
      </Actions>
    </TaskItem>
  );
}

const isTerminal = (status: ArtifactTaskSnapshot['status']) =>
  status === 'SUCCEEDED' || status === 'FAILED' || status === 'TIMED_OUT';
const isFailed = (status: ArtifactTaskSnapshot['status']) =>
  status === 'FAILED' || status === 'TIMED_OUT';

function statusLabel(status: ArtifactTaskSnapshot['status']) {
  return {
    ACCEPTED: '等待中',
    RUNNING: '生成中',
    SUCCEEDED: '已完成',
    FAILED: '失败',
    TIMED_OUT: '超时',
  }[status];
}

function statusColor(status: ArtifactTaskSnapshot['status']) {
  if (status === 'SUCCEEDED') return 'success';
  if (isFailed(status)) return 'error';
  return 'processing';
}

function sourceLabel(source?: string) {
  return (
    {
      VISUALIZATION: '可视化',
      TEMPLATE: '模版导出',
      RESOURCE_MIGRATION: '资源迁移',
      AGENT: 'Agent',
    }[source ?? ''] ?? '其它'
  );
}

function formatLabel(fileName?: string) {
  const suffix = fileName?.split('.').at(-1);
  return suffix ? suffix.toUpperCase() : '文件';
}

function operationErrorMessage(
  error: unknown,
  fallbackCode: string,
  label: string,
) {
  const response = error as {
    response?: { data?: { code?: unknown; traceId?: unknown } };
  };
  const code = String(response.response?.data?.code ?? fallbackCode);
  const traceId = String(response.response?.data?.traceId ?? 'unknown');
  return `${label}（错误码：${code}，追踪 ID：${traceId}）`;
}

const spin = keyframes`
  to { transform: rotate(360deg); }
`;

const TriggerWrap = styled.span<{ $floating: boolean }>`
  position: ${p => (p.$floating ? 'fixed' : 'relative')};
  right: auto;
  bottom: ${p => (p.$floating ? '16px' : 'auto')};
  left: ${p => (p.$floating ? '16px' : 'auto')};
  z-index: 1001;
  display: inline-flex;
`;

const DownloadButton = styled(Button)<{ $running: boolean }>`
  && {
    position: relative;
    width: 36px;
    min-width: 36px;
    height: 36px;
    color: ${p => p.theme.textColor ?? '#222'};
    background: ${p => p.theme.componentBackground ?? '#fff'};
    border: 1px solid ${p => p.theme.borderColorBase};
    border-radius: 50%;
  }

  &::after {
    position: absolute;
    inset: -3px;
    display: ${p => (p.$running ? 'block' : 'none')};
    content: '';
    border: 2px solid transparent;
    border-top-color: #1677ff;
    border-radius: 50%;
    animation: ${spin} 0.9s linear infinite;
  }
`;

const FailureHint = styled.span`
  position: absolute;
  bottom: 42px;
  left: 0;
  width: max-content;
  max-width: 220px;
  padding: 5px 8px;
  font-size: 12px;
  color: #fff;
  background: #c9363e;
  border-radius: 4px;
`;

const Panel = styled.div`
  display: flex;
  flex-direction: column;
  max-height: 50vh;
`;

const PanelHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 40px;
  padding-bottom: 8px;
  border-bottom: 1px solid ${p => p.theme.borderColorSplit};
`;

const TaskList = styled.div`
  min-height: 96px;
  overflow-y: auto;
`;

const TaskItem = styled.div`
  padding: 12px 0;
  border-bottom: 1px solid ${p => p.theme.borderColorSplit};
`;

const TaskSummary = styled.div`
  display: flex;
  gap: 8px;
  align-items: center;
`;

const FileName = styled.strong`
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const Meta = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
`;

const ErrorDetails = styled.div`
  padding-left: 8px;
  margin-top: 8px;
  border-left: 2px solid #d9363e;
`;

const ErrorLine = styled.div`
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 6px;
  margin-top: 3px;
  font-size: 12px;

  code {
    overflow-wrap: anywhere;
  }
`;

const OperationError = styled.div`
  margin-top: 6px;
  font-size: 12px;
  color: #c9363e;
`;

const Actions = styled.div`
  display: flex;
  justify-content: flex-end;
  min-height: 24px;
  margin-top: 6px;
`;

const LoadMore = styled(Button)`
  margin-top: 8px;
`;
