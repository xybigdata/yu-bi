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
  ClearOutlined,
  DeleteOutlined,
  SelectOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { message } from 'antd';
import {
  ListNav,
  ListPane,
  ListTitle,
  SIDEBAR_TITLE_MORE_MENU_ITEM_CLASS,
  SIDEBAR_TITLE_MORE_MENU_POPUP_CLASS,
} from 'app/components';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useGetSourceDbTypeIcon from 'app/hooks/useGetSourceDbTypeIcon';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { SidebarCollapseButton } from 'app/pages/MainPage/components/SidebarCollapseButton';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { useParams } from 'app/routerCompat';
import { CommonFormTypes } from 'globalConstants';
import {
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
import { LEVEL_5, SPACE_XS } from 'styles/StyleConstants';
import { getInsertedNodeIndex } from 'utils/utils';
import { SaveFormContext } from '../SaveFormContext';
import { makeSelectSourceTree, selectSources } from '../slice/selectors';
import { addSource, getSources } from '../slice/thunks';
import { SourceSimpleViewModel } from '../slice/types';
import { SourceList } from './SourceList';

interface SidebarProps {
  isDragging: boolean;
  sliderVisible: boolean;
  handleSliderVisible: (status: boolean) => void;
}

export const Sidebar = memo(
  ({ isDragging, sliderVisible, handleSliderVisible }: SidebarProps) => {
    const dispatch = useAppDispatch();
    const navigate = useCompatNavigate();
    const orgId = useSelector(selectOrgId);
    const sourceData = useSelector(selectSources);
    const { sourceId } = useParams<{ sourceId?: string }>();
    const t = useI18NPrefix('source');
    const selectSourceTree = useMemo(makeSelectSourceTree, []);
    const { showSaveForm } = useContext(SaveFormContext);
    const [batchMode, setBatchMode] = useState(false);
    const recycleRef = useRef<RecycleBinHandle>(null);

    const handleBatchCompleted = useCallback(() => {
      setBatchMode(false);
      dispatch(getSources(orgId));
    }, [dispatch, orgId]);

    const getIcon = useGetSourceDbTypeIcon();
    const getDisabled = useCallback(
      ({ deleteLoading }: SourceSimpleViewModel) => deleteLoading,
      [],
    );

    const treeData = useSelector(state =>
      selectSourceTree(state, { getIcon, getDisabled }),
    );
    const { filteredData: sourceList, debouncedSearch: listSearch } =
      useDebouncedSearch(treeData, (keywords, d) =>
        d.title.toLowerCase().includes(keywords.toLowerCase()),
      );
    const toAdd = useCallback(
      ({ key }) => {
        switch (key) {
          case 'add':
            navigate.push(`/organizations/${orgId}/sources/add`);
            break;
          case 'folder':
            showSaveForm({
              sourceType: 'folder',
              type: CommonFormTypes.Add,
              open: true,
              simple: false,
              parentIdLabel: t('sidebar.parent'),
              onSave: (values, onClose) => {
                let index = getInsertedNodeIndex(values, sourceData);
                dispatch(
                  addSource({
                    source: {
                      ...values,
                      config: JSON.stringify(values.config),
                      parentId: values.parentId || null,
                      index,
                      orgId,
                      isFolder: true,
                    },
                    resolve: () => {
                      onClose();
                      message.success(t('sidebar.addSuccess'));
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
      [dispatch, navigate, orgId, showSaveForm, sourceData, t],
    );

    const moreMenuClick = useCallback((key, _, onNext) => {
      switch (key) {
        case 'batch':
          setBatchMode(value => !value);
          break;
        case 'recycle':
          setBatchMode(false);
          onNext();
          break;
      }
    }, []);

    const recycleMenuClick = useCallback(key => {
      if (key === 'policy') void recycleRef.current?.openPolicy();
      if (key === 'empty') void recycleRef.current?.empty();
    }, []);

    const titles = useMemo(
      () => [
        {
          key: 'list',
          title: t('sidebar.title'),
          search: true,
          onSearch: listSearch,
          add: {
            items: [
              { key: 'add', text: t('sidebar.add') },
              { key: 'folder', text: t('sidebar.addFolder') },
            ],
            callback: toAdd,
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
                text: t('sidebar.recycle'),
                prefix: <DeleteOutlined className="icon" />,
              },
            ],
            callback: moreMenuClick,
          },
        },
        {
          key: 'recycle',
          title: t('sidebar.recycle'),
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
      [t, listSearch, toAdd, moreMenuClick, batchMode, recycleMenuClick],
    );

    return (
      <Wrapper
        sliderVisible={sliderVisible}
        className={sliderVisible ? 'close' : ''}
        isDragging={isDragging}
      >
        <SidebarCollapseButton
          collapsed={sliderVisible}
          expandLabel={t('sidebar.open')}
          collapseLabel={t('sidebar.close')}
          onToggle={handleSliderVisible}
        />
        <ListNavWrapper defaultActiveKey="list">
          <ListPane key="list">
            <ListTitle {...titles[0]} />
            {batchMode ? (
              <RecycleBatchManager
                orgId={orgId}
                resourceType="SOURCE"
                treeData={sourceList || []}
                onCompleted={handleBatchCompleted}
                onExit={() => setBatchMode(false)}
              />
            ) : (
              <SourceList sourceId={sourceId} list={sourceList} />
            )}
          </ListPane>
          <ListPane key="recycle">
            <ListTitle {...titles[1]} />
            <RecycleBinManager
              ref={recycleRef}
              orgId={orgId}
              resourceType="SOURCE"
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
  z-index: ${LEVEL_5};
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  min-height: 0;
  background-color: ${p => p.theme.componentBackground};
  box-shadow: ${p => p.theme.shadowSider};
  transition: ${p => (!p.isDragging ? 'width 0.3s ease' : 'none')};
  .hidden {
    display: none;
  }
  > ul {
    display: ${p => (p.sliderVisible ? 'none' : 'block')};
  }
  > div {
    display: ${p => (p.sliderVisible ? 'none' : 'flex')};
  }
  &.close {
    position: absolute;
    width: 0 !important;
    height: 100%;
    box-shadow: none;
  }
`;

const ListNavWrapper = styled(ListNav)`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: ${SPACE_XS} 0;
  background-color: ${p => p.theme.componentBackground};
`;
