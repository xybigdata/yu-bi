import {
  DeleteOutlined,
  MoreOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { Menu, MenuProps, message, Popconfirm, TreeDataNode } from 'antd';
import { Popup, Tree, TreeTitle } from 'app/components';
import { MenuItemContent } from 'app/components/Popup/MenuListItem';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import {
  selectIsOrgOwner,
  selectOrgId,
} from 'app/pages/MainPage/slice/selectors';
import { CommonFormTypes } from 'globalConstants';
import { memo, useCallback, useContext, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { useAppDispatch } from 'app/hooks/useRedux';
import { getInsertedNodeIndex, stopPPG } from 'utils/utils';
import { useToScheduleDetails } from '../hooks';
import { SaveFormContext } from '../SaveFormContext';
import { selectArchivedListLoading } from '../slice/selectors';
import {
  deleteSchedule,
  getArchivedSchedules,
  unarchiveSchedule,
} from '../slice/thunks';

interface ListData extends TreeDataNode {
  id: string;
  parentId: string | null;
}

interface RecycleProps {
  scheduleId?: string;
  list?: ListData[];
}

export const Recycle = memo(({ scheduleId, list }: RecycleProps) => {
  const dispatch = useAppDispatch();
  const navigate = useCompatNavigate();
  const loading = useSelector(selectArchivedListLoading);
  const orgId = useSelector(selectOrgId);
  const t = useI18NPrefix('schedule.editor.index');
  const { toDetails } = useToScheduleDetails();
  const isOwner = useSelector(selectIsOrgOwner);
  const { showSaveForm } = useContext(SaveFormContext);
  useEffect(() => {
    dispatch(getArchivedSchedules(orgId));
  }, [dispatch, orgId]);

  const moreMenuClick = useCallback(
    (id, name) =>
      ({ key, domEvent }) => {
        domEvent.stopPropagation();
        switch (key) {
          case 'reset':
            showSaveForm({
              scheduleType: 'folder',
              type: CommonFormTypes.Edit,
              visible: true,
              simple: false,
              initialValues: { id, name, parentId: null },
              parentIdLabel: t('parent'),
              onSave: (values, onClose) => {
                let index = getInsertedNodeIndex(values, list);
                dispatch(
                  unarchiveSchedule({
                    schedule: { ...values, id, index },
                    resolve: () => {
                      message.success(t('restoredSuccess'));
                      toDetails(orgId);
                      onClose();
                    },
                  }),
                );
              },
            });
            break;
          default:
            break;
        }
      },
    [showSaveForm, t, list, dispatch, toDetails, orgId],
  );

  const del = useCallback(
    id => () => {
      dispatch(
        deleteSchedule({
          id,
          archive: false,
          resolve: () => {
            message.success(`${t('success')}${t('delete')}`);
            toDetails(orgId);
          },
        }),
      );
    },
    [dispatch, orgId, t, toDetails],
  );

  const renderTreeTitle = useCallback(
    ({ key, title }) => {
      const items: MenuProps['items'] = [
        {
          key: 'reset',
          label: (
            <MenuItemContent prefix={<ReloadOutlined className="icon" />}>
              {t('restore')}
            </MenuItemContent>
          ),
        },
        {
          key: 'delete',
          label: (
            <MenuItemContent prefix={<DeleteOutlined className="icon" />}>
              <Popconfirm title={t('sureToDelete')} onConfirm={del(key)}>
                {t('delete')}
              </Popconfirm>
            </MenuItemContent>
          ),
        },
      ];

      return (
        <TreeTitle>
          <h4>{`${title}`}</h4>
          {isOwner && (
            <Popup
              trigger={['click']}
              placement="bottomRight"
              content={
                <Menu
                  prefixCls="ant-dropdown-menu"
                  selectable={false}
                  onClick={moreMenuClick(key, title)}
                  items={items}
                />
              }
            >
              <span className="action" onClick={stopPPG}>
                <MoreOutlined />
              </span>
            </Popup>
          )}
        </TreeTitle>
      );
    },
    [isOwner, moreMenuClick, t, del],
  );

  const treeSelect = useCallback(
    (_, { node }) => {
      if (!node.isFolder && node.id !== scheduleId) {
        navigate.push(`/organizations/${orgId}/schedules/${node.id}`);
      }
    },
    [navigate, orgId, scheduleId],
  );

  return (
    <Tree
      loading={loading}
      treeData={list}
      titleRender={node => renderTreeTitle(node as any)}
      onSelect={treeSelect}
    />
  );
});
