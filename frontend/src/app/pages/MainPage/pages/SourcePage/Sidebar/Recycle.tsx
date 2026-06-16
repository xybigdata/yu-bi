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
import {
  Key,
  ReactNode,
  memo,
  useCallback,
  useContext,
  useEffect,
} from 'react';
import { useSelector } from 'react-redux';
import { useAppDispatch } from 'app/hooks/useRedux';
import { getInsertedNodeIndex, stopPPG } from 'utils/utils';
import { SaveFormContext } from '../SaveFormContext';
import { selectArchivedListLoading } from '../slice/selectors';
import {
  deleteSource,
  getArchivedSources,
  unarchiveSource,
} from '../slice/thunks';

interface ListData extends TreeDataNode {
  id: string;
  parentId: string | null;
}

interface SourceRecycleNode extends ListData {
  key: Key;
  title?: ReactNode;
  isFolder?: boolean;
}

interface RecycleProps {
  sourceId?: string;
  list?: ListData[];
}

export const Recycle = memo(({ sourceId, list }: RecycleProps) => {
  const dispatch = useAppDispatch();
  const navigate = useCompatNavigate();
  const orgId = useSelector(selectOrgId);
  const loading = useSelector(selectArchivedListLoading);
  const t = useI18NPrefix('source.sidebar');
  const { showSaveForm } = useContext(SaveFormContext);
  const isOwner = useSelector(selectIsOrgOwner);

  useEffect(() => {
    dispatch(getArchivedSources(orgId));
  }, [dispatch, orgId]);

  const toDetail = useCallback(
    id => () => {
      navigate.push(`/organizations/${orgId}/sources/${id}`);
    },
    [navigate, orgId],
  );

  const moreMenuClick = useCallback(
    (id, name) =>
      ({ key, domEvent }) => {
        domEvent.stopPropagation();
        switch (key) {
          case 'reset':
            showSaveForm({
              sourceType: 'folder',
              type: CommonFormTypes.Edit,
              open: true,
              simple: false,
              initialValues: { id, name, parentId: null },
              parentIdLabel: t('parent'),
              onSave: (values, onClose) => {
                let index = getInsertedNodeIndex(values, list);
                dispatch(
                  unarchiveSource({
                    source: {
                      name: values.name,
                      parentId: values.parentId || null,
                      id,
                      index,
                    },
                    resolve: () => {
                      message.success(t('restoredSuccess'));
                      toDetail(orgId);
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
    [showSaveForm, t, list, dispatch, toDetail, orgId],
  );

  const treeSelect = useCallback(
    (_, { node }) => {
      if (!node.isFolder && node.id !== sourceId) {
        navigate.push(`/organizations/${orgId}/sources/${node.id}`);
      }
    },
    [navigate, orgId, sourceId],
  );

  const del = useCallback(
    id => () => {
      dispatch(
        deleteSource({
          id,
          archive: false,
          resolve: () => {
            message.success(`${t('success')}${t('delete')}`);
            toDetail(orgId);
          },
        }),
      );
    },
    [dispatch, orgId, t, toDetail],
  );

  const renderTreeTitle = useCallback(
    ({ key, title }: SourceRecycleNode) => {
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

  return (
    <Tree
      loading={loading}
      treeData={list}
      titleRender={node => renderTreeTitle(node as SourceRecycleNode)}
      onSelect={treeSelect}
    />
  );
});
