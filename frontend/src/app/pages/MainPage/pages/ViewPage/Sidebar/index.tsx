/**
 * YuBi
 *
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
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
  CodeFilled,
  ClearOutlined,
  DeleteOutlined,
  FolderFilled,
  FolderOpenFilled,
  SelectOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import {
  ListNav,
  ListPane,
  ListTitle,
  SIDEBAR_TITLE_MORE_MENU_ITEM_CLASS,
  SIDEBAR_TITLE_MORE_MENU_POPUP_CLASS,
} from 'app/components';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { SidebarCollapseButton } from 'app/pages/MainPage/components/SidebarCollapseButton';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { CommonFormTypes } from 'globalConstants';
import React, {
  memo,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
} from 'react';
import { useSelector } from 'react-redux';
import { useAppDispatch } from 'app/hooks/useRedux';
import {
  RecycleBatchManager,
  RecycleBinHandle,
  RecycleBinManager,
} from 'app/features/recycle';
import styled from 'styled-components';
import { LEVEL_10, SPACE_XS } from 'styles/StyleConstants';
import { getInsertedNodeIndex, uuidv4 } from 'utils/utils';
import { UNPERSISTED_ID_PREFIX } from '../constants';
import { SaveFormContext } from '../SaveFormContext';
import { makeSelectViewTree, selectViews } from '../slice/selectors';
import { getViews, saveFolder } from '../slice/thunks';
import { ViewSimpleViewModel } from '../slice/types';
import { FolderTree } from './FolderTree';

interface SidebarProps {
  isDragging: boolean;
  sliderVisible: boolean;
  handleSliderVisible: (status: boolean) => void;
}

export const Sidebar = memo(
  ({ isDragging, sliderVisible, handleSliderVisible }: SidebarProps) => {
    const navigate = useCompatNavigate();
    const dispatch = useAppDispatch();
    const { showSaveForm } = useContext(SaveFormContext);
    const orgId = useSelector(selectOrgId);
    const selectViewTree = useMemo(makeSelectViewTree, []);
    const viewsData = useSelector(selectViews);
    const t = useI18NPrefix('view.sidebar');
    const [batchMode, setBatchMode] = useState(false);
    const recycleRef = useRef<RecycleBinHandle>(null);

    const handleBatchCompleted = useCallback(() => {
      setBatchMode(false);
      dispatch(getViews(orgId));
    }, [dispatch, orgId]);

    const getIcon = useCallback(
      ({ isFolder }: ViewSimpleViewModel) =>
        isFolder ? (
          p => (p.expanded ? <FolderOpenFilled /> : <FolderFilled />)
        ) : (
          <CodeFilled />
        ),
      [],
    );
    const getDisabled = useCallback(
      ({ deleteLoading }: ViewSimpleViewModel) => deleteLoading,
      [],
    );

    const treeData = useSelector(state =>
      selectViewTree(state, { getIcon, getDisabled }),
    );

    const { filteredData: filteredTreeData, debouncedSearch: treeSearch } =
      useDebouncedSearch(treeData, (keywords, d) =>
        d.title.toLowerCase().includes(keywords.toLowerCase()),
      );
    const add = useCallback(
      ({ key }) => {
        switch (key) {
          case 'view':
            navigate.push(
              `/organizations/${orgId}/views/${`${UNPERSISTED_ID_PREFIX}${uuidv4()}`}`,
            );
            break;
          case 'folder':
            showSaveForm({
              type: CommonFormTypes.Add,
              open: true,
              simple: true,
              parentIdLabel: t('parent'),
              onSave: (values, onClose) => {
                let index = getInsertedNodeIndex(values, viewsData);

                dispatch(
                  saveFolder({
                    folder: {
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
          default:
            break;
        }
      },
      [dispatch, navigate, orgId, showSaveForm, viewsData, t],
    );

    const recycleMenuClick = useCallback(key => {
      if (key === 'policy') void recycleRef.current?.openPolicy();
      if (key === 'empty') void recycleRef.current?.empty();
    }, []);

    const titles = useMemo(
      () => [
        {
          key: 'list',
          title: t('title'),
          search: true,
          add: {
            items: [
              { key: 'view', text: t('addView') },
              { key: 'folder', text: t('addFolder') },
            ],
            callback: add,
          },
          more: {
            overlayClassName: SIDEBAR_TITLE_MORE_MENU_POPUP_CLASS,
            itemClassName: SIDEBAR_TITLE_MORE_MENU_ITEM_CLASS,
            items: [
              {
                key: 'batch',
                text: batchMode ? '退出批量管理' : '批量管理',
                prefix: <SelectOutlined className="icon" />,
              },
              {
                key: 'recycle',
                text: t('recycle'),
                prefix: <DeleteOutlined className="icon" />,
              },
            ],
            callback: (key, _, onNext) => {
              switch (key) {
                case 'batch':
                  setBatchMode(value => !value);
                  break;
                case 'recycle':
                  setBatchMode(false);
                  onNext();
                  break;
              }
            },
          },
          onSearch: treeSearch,
        },
        {
          key: 'recycle',
          title: t('recycle'),
          back: true,
          more: {
            overlayClassName: SIDEBAR_TITLE_MORE_MENU_POPUP_CLASS,
            itemClassName: SIDEBAR_TITLE_MORE_MENU_ITEM_CLASS,
            items: [
              {
                key: 'policy',
                text: '自动清理设置',
                prefix: <SettingOutlined className="icon" />,
              },
              {
                key: 'empty',
                text: '清空回收站',
                prefix: <ClearOutlined className="icon" />,
              },
            ],
            callback: recycleMenuClick,
          },
        },
      ],
      [add, treeSearch, t, batchMode, recycleMenuClick],
    );

    return (
      <Wrapper
        sliderVisible={sliderVisible}
        className={sliderVisible ? 'close' : ''}
        isDragging={isDragging}
      >
        <SidebarCollapseButton
          collapsed={sliderVisible}
          expandLabel={t('open')}
          collapseLabel={t('close')}
          onToggle={handleSliderVisible}
        />
        <ListNavWrapper defaultActiveKey="list">
          <ListPane key="list">
            <ListTitle {...titles[0]} />
            {batchMode ? (
              <RecycleBatchManager
                orgId={orgId}
                resourceType="VIEW"
                treeData={filteredTreeData || []}
                onCompleted={handleBatchCompleted}
                onExit={() => setBatchMode(false)}
              />
            ) : (
              <FolderTree treeData={filteredTreeData} />
            )}
          </ListPane>
          <ListPane key="recycle">
            <ListTitle {...titles[1]} />
            <RecycleBinManager
              ref={recycleRef}
              orgId={orgId}
              resourceType="VIEW"
            />
          </ListPane>
        </ListNavWrapper>
      </Wrapper>
    );
  },
);
const Wrapper = styled.div<{
  sliderVisible: boolean;
  isDragging: boolean;
}>`
  position: relative;
  height: 100%;
  transition: ${p => (!p.isDragging ? 'width 0.3s ease' : 'none')};
  &.close {
    position: absolute;
    width: 0 !important;
    height: 100%;
    background: transparent;
    border-right: 0;
    > div {
      display: ${p => (p.sliderVisible ? 'none' : 'flex')};
    }
  }
`;
const ListNavWrapper = styled(ListNav)`
  position: relative;
  z-index: ${LEVEL_10};
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  height: 100%;
  padding: ${SPACE_XS} 0;
  background-color: ${p => p.theme.componentBackground};
  border-right: 1px solid ${p => p.theme.borderColorSplit};
`;
