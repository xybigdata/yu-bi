import { RollbackOutlined } from '@ant-design/icons';
import { Button, message, Modal, Space, Typography } from 'antd';
import { useCallback, useState } from 'react';
import styled from 'styled-components';
import { createClientRequestId, recycleClient } from './client';
import { RecycleBatch, RecycleResourceType, RecycleDependency } from './types';

const ASYNC_POLL_INTERVAL_MS = 1_000;
const ASYNC_POLL_LIMIT = 600;

interface MoveToRecycleOptions {
  orgId: string;
  resourceType: RecycleResourceType;
  onCompleted?: (rootIds: string[]) => void | Promise<void>;
}

export function useMoveToRecycle({
  orgId,
  resourceType,
  onCompleted,
}: MoveToRecycleOptions) {
  const [loading, setLoading] = useState(false);

  const waitForCompletion = useCallback(
    async (initial: RecycleBatch) => {
      let current = initial;
      for (let attempt = 0; current.state === 'PROCESSING'; attempt += 1) {
        if (attempt >= ASYNC_POLL_LIMIT) {
          throw new Error('批量删除仍在处理中，请稍后进入回收站查看结果');
        }
        await new Promise(resolve =>
          window.setTimeout(resolve, ASYNC_POLL_INTERVAL_MS),
        );
        current = await recycleClient.getBatch(orgId, resourceType, current.id);
      }
      return current;
    },
    [orgId, resourceType],
  );

  const showUndo = useCallback(
    (batch: RecycleBatch, succeeded: number, summary: string) => {
      if (!batch.undoToken || succeeded === 0) return;
      const key = `recycle-undo-${batch.id}`;
      message.open({
        key,
        type: 'success',
        duration: 10,
        content: (
          <Space size={4}>
            <span>{summary}</span>
            <Button
              type="link"
              size="small"
              icon={<RollbackOutlined />}
              onClick={async () => {
                await recycleClient.undo(
                  orgId,
                  resourceType,
                  batch.id,
                  batch.undoToken!,
                );
                message.destroy(key);
                message.success('已撤销本次删除');
                await onCompleted?.(batch.items.map(item => item.rootId));
              }}
            >
              撤销
            </Button>
          </Space>
        ),
      });
    },
    [onCompleted, orgId, resourceType],
  );

  const moveToRecycle = useCallback(
    async (rootIds: string[]) => {
      if (!rootIds.length) return;
      setLoading(true);
      try {
        const preflight = await recycleClient.preflight(
          orgId,
          resourceType,
          rootIds,
        );
        const ready = preflight.items.filter(item => item.status === 'SUCCESS');
        const blocked = preflight.items.filter(
          item => item.status !== 'SUCCESS',
        );
        Modal.confirm({
          title: `移入回收站（${ready.length} 项可执行）`,
          content: (
            <div>
              {blocked.length > 0 && (
                <Typography.Text type="warning">
                  {blocked.length} 项被依赖、权限或运行状态阻断，将保留在原处。
                </Typography.Text>
              )}
              {blocked.map(item => (
                <DependencyResult key={item.rootId}>
                  <Typography.Text>
                    {item.message || item.status}
                  </Typography.Text>
                  {item.dependencies.map(dependency => (
                    <DependencyDetail
                      key={`${dependency.type}:${dependency.id}`}
                      dependency={dependency}
                    />
                  ))}
                </DependencyResult>
              ))}
            </div>
          ),
          okText: ready.length ? '仅删除可执行项' : '确定',
          okButtonProps: { danger: true, disabled: ready.length === 0 },
          cancelText: '取消',
          onOk: async () => {
            try {
              let batch = await recycleClient.move(
                orgId,
                resourceType,
                preflight.operationToken,
                createClientRequestId(),
              );
              const processingMessageKey = `recycle-processing-${batch.id}`;
              if (batch.state === 'PROCESSING') {
                message.open({
                  key: processingMessageKey,
                  type: 'loading',
                  duration: 0,
                  content: '内容较多，正在后台移入回收站',
                });
                try {
                  batch = await waitForCompletion(batch);
                } finally {
                  message.destroy(processingMessageKey);
                }
              }
              const succeeded = batch.items.filter(
                item => item.status === 'SUCCESS',
              ).length;
              const failed = batch.items.filter(
                item => item.status === 'FAILED',
              ).length;
              const skipped = batch.items.length - succeeded - failed;
              const summary = `处理完成：成功 ${succeeded} 项，跳过 ${skipped} 项，失败 ${failed} 项`;
              if (!batch.undoToken || succeeded === 0) {
                message.success(summary);
              }
              showUndo(batch, succeeded, summary);
              if (succeeded > 0) {
                await onCompleted?.(rootIds);
              }
            } catch (error) {
              message.error(formatRecycleError('移入回收站失败', error));
            }
          },
        });
      } catch (error) {
        message.error(formatRecycleError('移入回收站失败', error));
      } finally {
        setLoading(false);
      }
    },
    [onCompleted, orgId, resourceType, showUndo, waitForCompletion],
  );

  return { loading, moveToRecycle };
}

function DependencyDetail({ dependency }: { dependency: RecycleDependency }) {
  return (
    <Typography.Text type="secondary">
      {dependency.readable && dependency.name
        ? dependency.name
        : '受限依赖对象'}
      {' · '}
      {resourceTypeLabels[dependency.type]}
      {' · '}
      {dependency.depth === 'DIRECT' ? '直接依赖' : '间接依赖'}
      {dependency.location ? ` · 位置 ${dependency.location}` : ''}
      {dependency.ownerId ? ` · 所有者 ${dependency.ownerId}` : ''}
      {dependency.route ? (
        <>
          {' · '}
          <Typography.Link href={dependency.route}>前往查看</Typography.Link>
        </>
      ) : null}
    </Typography.Text>
  );
}

function formatRecycleError(fallback: string, error: unknown) {
  const detail =
    typeof error === 'string'
      ? error.trim()
      : error instanceof Error
        ? error.message.trim()
        : '';
  return detail ? `${fallback}：${detail}` : fallback;
}

const DependencyResult = styled.div`
  display: flex;
  flex-direction: column;
  margin-top: 8px;
`;

const resourceTypeLabels: Record<RecycleResourceType, string> = {
  SOURCE: '数据源',
  VIEW: '数据视图',
  SCHEDULE: '定时任务',
  DATACHART: '图表',
  DASHBOARD: '仪表盘',
  STORYBOARD: '故事板',
};
