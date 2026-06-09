/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  DeleteOutlined,
  EditOutlined,
  LoadingOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { Menu, MenuProps, message, Popconfirm, TreeDataNode } from 'antd';
import { Popup, Tree, TreeTitle } from 'app/components';
import { MenuItemContent } from 'app/components/Popup/MenuListItem';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import {
  selectIsOrgOwner,
  selectOrgId,
  selectPermissionMap,
} from 'app/pages/MainPage/slice/selectors';
import { CommonFormTypes } from 'globalConstants';
import { memo, useCallback, useContext, useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { useAppDispatch } from 'app/hooks/useRedux';
import { onDropTreeFn, stopPPG, uuidv4 } from 'utils/utils';
import { CascadeAccess, getCascadeAccess } from '../../../Access';
import {
  PermissionLevels,
  ResourceTypes,
} from '../../PermissionPage/constants';
import { UNPERSISTED_ID_PREFIX } from '../../ViewPage/constants';
import { SaveFormContext } from '../SaveFormContext';
import {
  selectDeleteSourceLoading,
  selectSourceListLoading,
} from '../slice/selectors';
import { deleteSource, getSources, updateSourceBase } from '../slice/thunks';

interface SourceListProps {
  sourceId?: string;
  list?: TreeDataNode[];
}

export const SourceList = memo(({ sourceId, list }: SourceListProps) => {
  const dispatch = useAppDispatch();
  const navigate = useCompatNavigate();
  const orgId = useSelector(selectOrgId);
  const deleteLoading = useSelector(selectDeleteSourceLoading);
  const loading = useSelector(selectSourceListLoading);
  const t = useI18NPrefix('source');
  const tg = useI18NPrefix('global');
  const { showSaveForm } = useContext(SaveFormContext);
  const isOwner = useSelector(selectIsOrgOwner);
  const permissionMap = useSelector(selectPermissionMap);
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);

  useEffect(() => {
    dispatch(getSources(orgId));
  }, [dispatch, orgId]);

  const toDetails = useCallback(
    id => () => {
      navigate.push(`/organizations/${orgId}/sources/${id}`);
    },
    [navigate, orgId],
  );

  const moreMenuClick = useCallback(
    ({ id, name, parentId, index }) =>
      ({ key, domEvent }) => {
        domEvent.stopPropagation();
        switch (key) {
          case 'info':
            showSaveForm({
              sourceType: 'folder',
              type: CommonFormTypes.Edit,
              open: true,
              simple: false,
              initialValues: {
                id,
                name,
                parentId,
              },
              parentIdLabel: t('sidebar.parent'),
              onSave: (values, onClose) => {
                dispatch(
                  updateSourceBase({
                    source: {
                      id,
                      index,
                      name: values.name,
                      parentId: values.parentId,
                    },
                    resolve: () => {
                      toDetails(orgId);
                      onClose();
                    },
                  }),
                );
              },
            });
            break;
          case 'addNewView':
            navigate.push({
              pathname: `/organizations/${orgId}/views/${`${UNPERSISTED_ID_PREFIX}${uuidv4()}`}`,
              state: {
                sourcesId: id,
              },
            });
            break;
          case 'delete':
            break;
          default:
            break;
        }
      },
    [dispatch, navigate, orgId, showSaveForm, t, toDetails],
  );

  const del = useCallback(
    (id, isFolder) => () => {
      if (deleteLoading) return;
      dispatch(
        deleteSource({
          id,
          archive: !isFolder,
          resolve: () => {
            message.success(
              isFolder
                ? tg('operation.archiveSuccess')
                : tg('operation.deleteSuccess'),
            );
            navigate.replace(`/organizations/${orgId}/sources`);
          },
        }),
      );
    },
    [deleteLoading, dispatch, tg, navigate, orgId],
  );

  const renderTreeTitle = useCallback(
    node => {
      const { title, path, isFolder, id, type } = node;
      const isAuthorized = getCascadeAccess(
        isOwner,
        permissionMap,
        ResourceTypes.Source,
        path,
        PermissionLevels.Manage,
      );

      return (
        <TreeTitle>
          <h4>{title}</h4>
          <CascadeAccess
            module={ResourceTypes.Source}
            path={path}
            level={PermissionLevels.Manage}
          >
            {(() => {
              const items: MenuProps['items'] = [
                ...(isAuthorized
                  ? [
                      {
                        key: 'info',
                        label: (
                          <MenuItemContent
                            prefix={<EditOutlined className="icon" />}
                          >
                            {tg('button.info')}
                          </MenuItemContent>
                        ),
                      },
                    ]
                  : []),
                ...(isAuthorized && type !== 'FOLDER'
                  ? [
                      {
                        key: 'addNewView',
                        label: (
                          <MenuItemContent
                            prefix={<EditOutlined className="icon" />}
                          >
                            {t('creatView')}
                          </MenuItemContent>
                        ),
                      },
                    ]
                  : []),
                ...(isAuthorized
                  ? [
                      {
                        key: 'delete',
                        label: (
                          <MenuItemContent
                            prefix={
                              deleteLoading ? (
                                <LoadingOutlined className="icon" />
                              ) : (
                                <DeleteOutlined className="icon" />
                              )
                            }
                          >
                            <Popconfirm
                              title={
                                isFolder
                                  ? tg('operation.deleteConfirm')
                                  : tg('operation.archiveConfirm')
                              }
                              onConfirm={del(id, isFolder)}
                            >
                              {isFolder
                                ? tg('button.delete')
                                : tg('button.archive')}
                            </Popconfirm>
                          </MenuItemContent>
                        ),
                      },
                    ]
                  : []),
              ];

              return (
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
              );
            })()}
          </CascadeAccess>
        </TreeTitle>
      );
    },
    [isOwner, permissionMap, moreMenuClick, tg, t, deleteLoading, del],
  );

  const onDrop = info => {
    onDropTreeFn({
      info,
      treeData: list,
      callback: (id, parentId, index) => {
        dispatch(
          updateSourceBase({
            source: {
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

  const menuSelect = useCallback(
    (_, { node }) => {
      if (node.type === 'FOLDER') {
        if (expandedKeys?.includes(node.key)) {
          setExpandedKeys(expandedKeys.filter(k => k !== node.key));
        } else {
          setExpandedKeys([node.key].concat(expandedKeys));
        }
      } else {
        navigate.push(`/organizations/${orgId}/sources/${node.id}`);
      }
    },
    [expandedKeys, navigate, orgId],
  );

  const handleExpandTreeNode = expandedKeys => {
    setExpandedKeys(expandedKeys);
  };

  return (
    <Tree
      loading={loading}
      treeData={list}
      titleRender={renderTreeTitle}
      onSelect={menuSelect}
      onDrop={onDrop}
      expandedKeys={expandedKeys}
      onExpand={handleExpandTreeNode}
      {...(sourceId && { selectedKeys: [sourceId] })}
      draggable
    />
  );
});
