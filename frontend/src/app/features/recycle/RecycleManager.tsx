import {
  CloseOutlined,
  DeleteOutlined,
  RollbackOutlined,
  SelectOutlined,
} from '@ant-design/icons';
import {
  Button,
  Checkbox,
  Empty,
  List,
  message,
  Modal,
  Select,
  Space,
  Switch,
  Tooltip,
  Tree,
  TreeDataNode,
  Typography,
} from 'antd';
import dayjs from 'dayjs';
import {
  forwardRef,
  ReactNode,
  useCallback,
  useEffect,
  useImperativeHandle,
  useMemo,
  useState,
} from 'react';
import styled from 'styled-components';
import { recycleClient, createClientRequestId } from './client';
import { flattenTreeKeys, normalizeSelectedRoots } from './selection';
import { RecycleEntry, RecyclePolicy, RecycleResourceType } from './types';
import { useMoveToRecycle } from './useMoveToRecycle';

interface BatchManagerProps {
  orgId: string;
  resourceType: RecycleResourceType;
  treeData: TreeDataNode[];
  onCompleted?: () => void;
  onExit: () => void;
}

export function RecycleBatchManager({
  orgId,
  resourceType,
  treeData,
  onCompleted,
  onExit,
}: BatchManagerProps) {
  const [checkedKeys, setCheckedKeys] = useState<React.Key[]>([]);
  const allKeys = useMemo(() => flattenTreeKeys(treeData), [treeData]);
  const selectedRoots = useMemo(
    () => normalizeSelectedRoots(checkedKeys, treeData),
    [checkedKeys, treeData],
  );

  const handleCompleted = useCallback(() => {
    setCheckedKeys([]);
    onCompleted?.();
  }, [onCompleted]);
  const { loading, moveToRecycle } = useMoveToRecycle({
    orgId,
    resourceType,
    onCompleted: handleCompleted,
  });

  return (
    <ManagerWrapper>
      <Toolbar>
        <Checkbox
          checked={allKeys.length > 0 && checkedKeys.length === allKeys.length}
          indeterminate={
            checkedKeys.length > 0 && checkedKeys.length < allKeys.length
          }
          onChange={event =>
            setCheckedKeys(event.target.checked ? allKeys : [])
          }
        >
          全选
        </Checkbox>
        <Typography.Text type="secondary">
          已选 {selectedRoots.length} 项
        </Typography.Text>
        <Button
          danger
          type="primary"
          size="small"
          icon={<DeleteOutlined />}
          disabled={!selectedRoots.length}
          loading={loading}
          onClick={() => void moveToRecycle(selectedRoots)}
        >
          移入回收站
        </Button>
        <Tooltip title="退出批量管理">
          <Button
            type="text"
            size="small"
            icon={<CloseOutlined />}
            aria-label="退出批量管理"
            onClick={onExit}
          />
        </Tooltip>
      </Toolbar>
      {treeData.length ? (
        <Tree
          blockNode
          checkable
          checkedKeys={checkedKeys}
          treeData={treeData}
          onCheck={keys =>
            setCheckedKeys(
              Array.isArray(keys) ? keys : (keys.checked as React.Key[]),
            )
          }
        />
      ) : (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />
      )}
    </ManagerWrapper>
  );
}

export interface RecycleBinHandle {
  openPolicy: () => Promise<void>;
  empty: () => Promise<void>;
  refresh: () => Promise<void>;
}

interface BinManagerProps {
  orgId: string;
  resourceType: RecycleResourceType;
  emptyText?: ReactNode;
}

export const RecycleBinManager = forwardRef<RecycleBinHandle, BinManagerProps>(
  ({ orgId, resourceType, emptyText }, ref) => {
    const [entries, setEntries] = useState<RecycleEntry[]>([]);
    const [selected, setSelected] = useState<string[]>([]);
    const [loading, setLoading] = useState(false);

    const load = useCallback(async () => {
      setLoading(true);
      try {
        setEntries(await recycleClient.list(orgId, resourceType));
        setSelected([]);
      } finally {
        setLoading(false);
      }
    }, [orgId, resourceType]);

    useEffect(() => {
      void load();
    }, [load]);

    const run = useCallback(
      async (operation: 'restore' | 'delete', recordIds = selected) => {
        if (!recordIds.length) return;
        const execute = async () => {
          const batch =
            operation === 'restore'
              ? await recycleClient.restore(
                  orgId,
                  resourceType,
                  recordIds,
                  createClientRequestId(),
                )
              : await recycleClient.permanentlyDelete(
                  orgId,
                  resourceType,
                  recordIds,
                  createClientRequestId(),
                );
          const succeeded = batch.items.filter(
            item => item.status === 'SUCCESS',
          ).length;
          const failed = batch.items.filter(
            item => item.status === 'FAILED',
          ).length;
          const skipped = batch.items.length - succeeded - failed;
          message.success(
            `${operation === 'restore' ? '恢复' : '永久删除'}完成：成功 ${succeeded} 项，跳过 ${skipped} 项，失败 ${failed} 项`,
          );
          await load();
        };
        if (operation === 'delete') {
          Modal.confirm({
            title: `永久删除 ${recordIds.length} 项内容？`,
            content: '删除后无法恢复。',
            okText: '永久删除',
            okButtonProps: { danger: true },
            cancelText: '取消',
            onOk: execute,
          });
        } else {
          await execute();
        }
      },
      [load, orgId, resourceType, selected],
    );

    const openPolicy = useCallback(async () => {
      const current = await recycleClient.getPolicy(orgId, resourceType);
      let next = current;
      Modal.confirm({
        title: '自动清理设置',
        content: (
          <PolicyEditor value={current} onChange={value => (next = value)} />
        ),
        okText: '保存',
        cancelText: '取消',
        onOk: async () => {
          await recycleClient.updatePolicy(orgId, resourceType, next);
          message.success('自动清理设置已更新');
          await load();
        },
      });
    }, [load, orgId, resourceType]);

    const empty = useCallback(async () => {
      const current = await recycleClient.list(orgId, resourceType);
      if (!current.length) {
        message.info('当前模块回收站为空');
        return;
      }
      await run(
        'delete',
        current.map(entry => entry.id),
      );
    }, [orgId, resourceType, run]);

    useImperativeHandle(ref, () => ({ openPolicy, empty, refresh: load }), [
      empty,
      load,
      openPolicy,
    ]);

    return (
      <ManagerWrapper>
        <Toolbar>
          <Checkbox
            checked={entries.length > 0 && selected.length === entries.length}
            indeterminate={
              selected.length > 0 && selected.length < entries.length
            }
            onChange={event =>
              setSelected(
                event.target.checked ? entries.map(entry => entry.id) : [],
              )
            }
          >
            全选
          </Checkbox>
          <Typography.Text type="secondary">
            已选 {selected.length} 项
          </Typography.Text>
          <Button
            size="small"
            icon={<RollbackOutlined />}
            disabled={!selected.length}
            onClick={() => void run('restore')}
          >
            恢复
          </Button>
          <Button
            danger
            size="small"
            icon={<DeleteOutlined />}
            disabled={!selected.length}
            onClick={() => void run('delete')}
          >
            永久删除
          </Button>
        </Toolbar>
        <List
          loading={loading}
          dataSource={entries}
          locale={{ emptyText: emptyText || '回收站暂无内容' }}
          renderItem={entry => (
            <List.Item>
              <Checkbox
                checked={selected.includes(entry.id)}
                onChange={event =>
                  setSelected(keys =>
                    event.target.checked
                      ? [...keys, entry.id]
                      : keys.filter(key => key !== entry.id),
                  )
                }
              >
                <EntryTitle>{entry.name}</EntryTitle>
                <EntryMeta>
                  {entry.folder
                    ? `目录 · ${entry.expandedItemCount} 项`
                    : '资源'}
                  {entry.originalParentId
                    ? ` · 原目录 ${entry.originalParentId}`
                    : ' · 根目录'}
                  {` · 删除人 ${entry.deletedByName || entry.deletedBy}`}
                  {' · '}
                  {dayjs(entry.deletedAt).format('YYYY-MM-DD HH:mm')}
                  {entry.expiresAt
                    ? ` · ${dayjs(entry.expiresAt).diff(dayjs(), 'day')} 天后清理`
                    : ' · 不自动清理'}
                </EntryMeta>
              </Checkbox>
            </List.Item>
          )}
        />
      </ManagerWrapper>
    );
  },
);

function PolicyEditor({
  value,
  onChange,
}: {
  value: RecyclePolicy;
  onChange: (value: RecyclePolicy) => void;
}) {
  const [policy, setPolicy] = useState(value);
  const update = (next: RecyclePolicy) => {
    setPolicy(next);
    onChange(next);
  };
  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Space>
        <Switch
          checked={policy.enabled}
          onChange={enabled => update({ ...policy, enabled })}
        />
        <span>启用自动清理</span>
      </Space>
      <Select
        value={policy.retentionDays}
        disabled={!policy.enabled}
        style={{ width: '100%' }}
        options={[7, 30, 60, 90].map(days => ({
          value: days,
          label: `保留 ${days} 天`,
        }))}
        onChange={retentionDays =>
          update({ ...policy, retentionDays } as RecyclePolicy)
        }
      />
    </Space>
  );
}

const ManagerWrapper = styled.div`
  min-height: 0;
  padding: 0 8px 8px;
  overflow: auto;
`;

const Toolbar = styled.div`
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  gap: 8px;
  align-items: center;
  min-height: 40px;
  background: ${p => p.theme.componentBackground};
  border-bottom: 1px solid ${p => p.theme.borderColorSplit};
`;

const EntryTitle = styled.div`
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
  white-space: nowrap;
`;

const EntryMeta = styled.div`
  font-size: 12px;
  color: ${p => p.theme.textColorLight};
`;
