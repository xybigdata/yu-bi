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
  CopyFilled,
  DeleteOutlined,
  EditOutlined,
  MonitorOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { Menu, MenuProps, message, Popconfirm, TreeDataNode } from 'antd';
import { Popup, Tree, TreeTitle } from 'app/components';
import { MenuItemContent } from 'app/components/Popup/MenuListItem';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { getCascadeAccess, useAccess } from 'app/pages/MainPage/Access';
import {
  selectIsOrgOwner,
  selectOrgId,
  selectPermissionMap,
} from 'app/pages/MainPage/slice/selectors';
import { CommonFormTypes } from 'globalConstants';
import React, {
  memo,
  useCallback,
  useContext,
  useEffect,
  useState,
} from 'react';
import { useSelector } from 'react-redux';
import { useAppDispatch } from 'app/hooks/useRedux';
import { getInsertedNodeIndex, onDropTreeFn, stopPPG } from 'utils/utils';
import { isParentIdEqual } from '../../../slice/utils';
import {
  PermissionLevels,
  ResourceTypes,
} from '../../PermissionPage/constants';
import { useSaveAsView } from '../hooks/useSaveAsView';
import { useStartAnalysis } from '../hooks/useStartAnalysis';
import { SaveFormContext } from '../SaveFormContext';
import {
  selectCurrentEditingViewKey,
  selectViewListLoading,
  selectViews,
} from '../slice/selectors';
import {
  deleteView,
  getViews,
  removeEditingView,
  updateViewBase,
} from '../slice/thunks';

interface FolderTreeProps {
  treeData?: TreeDataNode[];
}

export const FolderTree = memo(({ treeData }: FolderTreeProps) => {
  const dispatch = useAppDispatch();
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);
  const navigate = useCompatNavigate();
  const { showSaveForm } = useContext(SaveFormContext);
  const loading = useSelector(selectViewListLoading);
  const currentEditingViewKey = useSelector(selectCurrentEditingViewKey);
  const orgId = useSelector(selectOrgId);
  const viewsData = useSelector(selectViews);
  const isOwner = useSelector(selectIsOrgOwner);
  const permissionMap = useSelector(selectPermissionMap);
  const t = useI18NPrefix('view');
  const tg = useI18NPrefix('global');
  const saveAsView = useSaveAsView();
  const startAnalysis = useStartAnalysis();
  const allowEnableViz = useAccess({
    type: 'module',
    module: ResourceTypes.Viz,
    id: '',
    level: PermissionLevels.Enable,
  })(true);

  useEffect(() => {
    dispatch(getViews(orgId));
  }, [dispatch, orgId]);

  const redirect = useCallback(
    currentEditingViewKey => {
      if (currentEditingViewKey) {
        navigate.push(`/organizations/${orgId}/views/${currentEditingViewKey}`);
      } else {
        navigate.push(`/organizations/${orgId}/views`);
      }
    },
    [navigate, orgId],
  );

  const archive = useCallback(
    (id, isFolder) => e => {
      e.stopPropagation();
      dispatch(
        deleteView({
          id,
          archive: !isFolder,
          resolve: () => {
            dispatch(removeEditingView({ id, resolve: redirect }));
            message.success(
              isFolder
                ? tg('operation.deleteSuccess')
                : tg('operation.archiveSuccess'),
            );
          },
        }),
      );
    },
    [dispatch, redirect, tg],
  );

  const moreMenuClick = useCallback(
    ({ id, name, parentId, index, isFolder }) =>
      ({ key, domEvent }) => {
        domEvent.stopPropagation();
        switch (key) {
          case 'info':
            showSaveForm({
              type: CommonFormTypes.Edit,
              open: true,
              simple: isFolder,
              initialValues: {
                id,
                name,
                parentId,
              },
              parentIdLabel: t('saveForm.folder'),
              onSave: (values, onClose) => {
                if (isParentIdEqual(parentId, values.parentId)) {
                  index = getInsertedNodeIndex(values, viewsData);
                }

                dispatch(
                  updateViewBase({
                    view: {
                      id,
                      ...values,
                      parentId: values.parentId || null,
                      index,
                    },
                    resolve: onClose,
                  }),
                );
              },
            });
            break;
          case 'delete':
            break;
          case 'saveAs':
            saveAsView(id);
            break;
          case 'startAnalysis':
            startAnalysis(id);
            break;
          default:
            break;
        }
      },
    [dispatch, showSaveForm, viewsData, t, saveAsView, startAnalysis],
  );

  const renderTreeTitle = useCallback(
    node => {
      const { title, path, isFolder, id } = node;
      const isAuthorized = getCascadeAccess(
        isOwner,
        permissionMap,
        ResourceTypes.View,
        path,
        PermissionLevels.Manage,
      );
      return (
        <TreeTitle>
          <h4>{`${title}`}</h4>
          {isAuthorized || allowEnableViz
            ? (() => {
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
                  ...(isAuthorized && !isFolder
                    ? [
                        {
                          key: 'saveAs',
                          label: (
                            <MenuItemContent
                              prefix={<CopyFilled className="icon" />}
                            >
                              {tg('button.saveAs')}
                            </MenuItemContent>
                          ),
                        },
                      ]
                    : []),
                  ...(allowEnableViz && !isFolder
                    ? [
                        {
                          key: 'startAnalysis',
                          label: (
                            <MenuItemContent
                              prefix={<MonitorOutlined className="icon" />}
                            >
                              {t('editor.startAnalysis')}
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
                              prefix={<DeleteOutlined className="icon" />}
                            >
                              <Popconfirm
                                title={
                                  isFolder
                                    ? tg('operation.deleteConfirm')
                                    : tg('operation.archiveConfirm')
                                }
                                onConfirm={archive(id, isFolder)}
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
              })()
            : ''}
        </TreeTitle>
      );
    },
    [archive, moreMenuClick, tg, allowEnableViz, t, isOwner, permissionMap],
  );

  const treeSelect = useCallback(
    (_, { node }) => {
      if (node.isFolder) {
        if (expandedKeys?.includes(node.key)) {
          setExpandedKeys(expandedKeys.filter(k => k !== node.key));
        } else {
          setExpandedKeys([node.key].concat(expandedKeys));
        }
      }
      if (!node.isFolder && node.id !== currentEditingViewKey) {
        navigate.push(`/organizations/${orgId}/views/${node.id}`);
      }
    },
    [navigate, orgId, currentEditingViewKey, expandedKeys],
  );

  const onDrop = info => {
    onDropTreeFn({
      info,
      treeData,
      callback: (id, parentId, index) => {
        dispatch(
          updateViewBase({
            view: {
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
      treeData={treeData}
      titleRender={renderTreeTitle}
      selectedKeys={[currentEditingViewKey]}
      onSelect={treeSelect}
      onDrop={onDrop}
      expandedKeys={expandedKeys}
      onExpand={handleExpandTreeNode}
      draggable
    />
  );
});
