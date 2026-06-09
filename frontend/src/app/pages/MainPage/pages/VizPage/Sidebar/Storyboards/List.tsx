import { DeleteOutlined, EditOutlined, MoreOutlined } from '@ant-design/icons';
import { Menu, MenuProps, message, Popconfirm } from 'antd';
import { Popup, Tree, TreeTitle } from 'app/components';
import { MenuItemContent } from 'app/components/Popup/MenuListItem';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { CascadeAccess } from 'app/pages/MainPage/Access';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { CommonFormTypes } from 'globalConstants';
import { memo, useCallback, useContext, useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { useAppDispatch } from 'app/hooks/useRedux';
import { onDropTreeFn, stopPPG } from 'utils/utils';
import { LocalTreeDataNode } from '../../../../slice/types';
import {
  PermissionLevels,
  ResourceTypes,
} from '../../../PermissionPage/constants';
import { SaveFormContext } from '../../SaveFormContext';
import { selectStoryboardListLoading } from '../../slice/selectors';
import {
  deleteViz,
  editStoryboard,
  getStoryboards,
  removeTab,
} from '../../slice/thunks';

interface StoryboardListProps {
  selectedId?: string;
  list?: LocalTreeDataNode[];
}

export const List = memo(({ list, selectedId }: StoryboardListProps) => {
  const dispatch = useAppDispatch();
  const navigate = useCompatNavigate();
  const loading = useSelector(selectStoryboardListLoading);
  const orgId = useSelector(selectOrgId);
  const { showSaveForm } = useContext(SaveFormContext);
  const tg = useI18NPrefix('global');
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);

  useEffect(() => {
    dispatch(getStoryboards(orgId));
  }, [dispatch, orgId]);

  const redirect = useCallback(
    tabKey => {
      if (tabKey) {
        navigate.push(`/organizations/${orgId}/vizs/${tabKey}`);
      } else {
        navigate.push(`/organizations/${orgId}/vizs`);
      }
    },
    [navigate, orgId],
  );

  const archiveStoryboard = useCallback(
    (isFolder, id) => () => {
      dispatch(
        deleteViz({
          params: { id, archive: !isFolder },
          type: 'STORYBOARD',
          resolve: () => {
            message.success(
              isFolder
                ? tg('operation.deleteSuccess')
                : tg('operation.deleteSuccess'),
            );
            dispatch(removeTab({ id, resolve: redirect }));
          },
        }),
      );
    },
    [dispatch, redirect, tg],
  );

  const moreMenuClick = useCallback(
    storyboard =>
      ({ key, domEvent }) => {
        domEvent.stopPropagation();
        switch (key) {
          case 'info':
            showSaveForm({
              vizType: 'STORYBOARD',
              type: CommonFormTypes.Edit,
              open: true,
              initialValues: {
                ...storyboard,
                parentId: storyboard.parentId || void 0,
              },
              onSave: (values, onClose) => {
                dispatch(
                  editStoryboard({
                    storyboard: { ...storyboard, ...values },
                    resolve: onClose,
                  }),
                );
              },
            });
            break;
          case 'delete':
            break;
          default:
            break;
        }
      },
    [dispatch, showSaveForm],
  );

  const renderTreeTitle = useCallback(
    node => {
      const { isFolder, title, path, id } = node;
      const items: MenuProps['items'] = [
        {
          key: 'info',
          label: (
            <MenuItemContent prefix={<EditOutlined className="icon" />}>
              {tg('button.info')}
            </MenuItemContent>
          ),
        },
        {
          key: 'delete',
          label: (
            <MenuItemContent prefix={<DeleteOutlined className="icon" />}>
              <Popconfirm
                title={`${
                  isFolder
                    ? tg('operation.deleteConfirm')
                    : tg('operation.archiveConfirm')
                }`}
                onConfirm={archiveStoryboard(isFolder, id)}
              >
                {isFolder ? tg('button.delete') : tg('button.archive')}
              </Popconfirm>
            </MenuItemContent>
          ),
        },
      ];

      return (
        <TreeTitle>
          <h4>{`${title}`}</h4>
          <CascadeAccess
            module={ResourceTypes.Viz}
            path={path}
            level={PermissionLevels.Manage}
          >
            <Popup
              trigger={['click']}
              placement="bottom"
              content={
                <Menu
                  prefixCls="ant-dropdown-menu"
                  selectable={false}
                  onClick={moreMenuClick(node)}
                  items={items}
                />
              }
            >
              <span className="action" onClick={stopPPG}>
                <MoreOutlined />
              </span>
            </Popup>
          </CascadeAccess>
        </TreeTitle>
      );
    },
    [moreMenuClick, archiveStoryboard, tg],
  );

  const menuSelect = useCallback(
    (_, { node }) => {
      if (node.isFolder) {
        if (expandedKeys?.includes(node.key)) {
          setExpandedKeys(expandedKeys.filter(k => k !== node.key));
        } else {
          setExpandedKeys([node.key].concat(expandedKeys));
        }
      } else {
        navigate.push(`/organizations/${orgId}/vizs/${node.id}`);
      }
    },
    [expandedKeys, navigate, orgId],
  );

  const onDrop = info => {
    onDropTreeFn({
      info,
      treeData: list,
      callback: (id, parentId, index) => {
        dispatch(
          editStoryboard({
            storyboard: {
              id,
              parentId,
              index: index,
              name: info.dragNode.name,
            },
            resolve: () => {},
          }),
        );
      },
    });
  };

  const handleExpandTreeNode = expandedKeys => {
    setExpandedKeys(expandedKeys);
  };

  return (
    <Tree
      loading={loading}
      treeData={list}
      expandedKeys={expandedKeys}
      titleRender={renderTreeTitle}
      onExpand={handleExpandTreeNode}
      onSelect={menuSelect}
      onDrop={onDrop}
      {...(selectedId && { selectedKeys: [selectedId] })}
      draggable
    />
  );
});
