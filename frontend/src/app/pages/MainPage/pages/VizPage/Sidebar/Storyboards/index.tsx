import {
  ClearOutlined,
  DeleteOutlined,
  FolderFilled,
  FolderOpenFilled,
  FundProjectionScreenOutlined,
  PlusOutlined,
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
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
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
import { SPACE_XS } from 'styles/StyleConstants';
import { getInsertedNodeIndex } from 'utils/utils';
import { VizResourceSubTypes } from '../../../PermissionPage/constants';
import { SaveFormContext } from '../../SaveFormContext';
import {
  makeSelectStoryboradTree,
  selectStoryboards,
} from '../../slice/selectors';
import { addStoryboard, getStoryboards } from '../../slice/thunks';
import { StoryboardViewModel } from '../../slice/types';
import { List } from './List';

interface FoldersProps extends I18NComponentProps {
  selectedId?: string;
  className?: string;
}

export const Storyboards = memo(
  ({ selectedId, className, i18nPrefix }: FoldersProps) => {
    const dispatch = useAppDispatch();
    const orgId = useSelector(selectOrgId);
    const { showSaveForm } = useContext(SaveFormContext);
    const storyborads = useSelector(selectStoryboards);
    const selectStoryboradTree = useMemo(makeSelectStoryboradTree, []);
    const t = useI18NPrefix(i18nPrefix);
    const [batchMode, setBatchMode] = useState(false);
    const recycleRef = useRef<RecycleBinHandle>(null);

    const handleBatchCompleted = useCallback(() => {
      setBatchMode(false);
      dispatch(getStoryboards(orgId));
    }, [dispatch, orgId]);

    const getIcon = useCallback(
      ({ isFolder }: StoryboardViewModel) =>
        isFolder ? (
          p => (p.expanded ? <FolderOpenFilled /> : <FolderFilled />)
        ) : (
          <FundProjectionScreenOutlined />
        ),
      [],
    );

    const getDisabled = useCallback(
      ({ deleteLoading }: StoryboardViewModel) => deleteLoading,
      [],
    );

    const treeData = useSelector(state =>
      selectStoryboradTree(state, { getIcon, getDisabled }),
    );

    const { filteredData: filteredListData, debouncedSearch: listSearch } =
      useDebouncedSearch(treeData, (keywords, d) =>
        d.title.toLowerCase().includes(keywords.toLowerCase()),
      );

    const add = useCallback(
      ({ key }) => {
        switch (key) {
          case 'add':
            showSaveForm({
              vizType: VizResourceSubTypes.Storyboard,
              type: CommonFormTypes.Add,
              open: true,
              onSave: (values, onClose) => {
                const index = getInsertedNodeIndex(values, storyborads);
                dispatch(
                  addStoryboard({
                    storyboard: {
                      ...values,
                      parentId: values.parentId || null,
                      orgId,
                      isFolder: false,
                      index,
                    },
                    resolve: onClose,
                  }),
                );
              },
            });
            break;
          case 'folder':
            showSaveForm({
              vizType: VizResourceSubTypes.Storyboard,
              type: CommonFormTypes.Add,
              open: true,
              onSave: (values, onClose) => {
                const index = getInsertedNodeIndex(values, storyborads);
                dispatch(
                  addStoryboard({
                    storyboard: {
                      ...values,
                      parentId: values.parentId || null,
                      orgId,
                      isFolder: true,
                      index,
                    },
                    resolve: onClose,
                  }),
                );
              },
            });
            break;
          default:
        }
      },
      [showSaveForm, storyborads, dispatch, orgId],
    );

    const recycleMenuClick = useCallback(key => {
      if (key === 'policy') void recycleRef.current?.openPolicy();
      if (key === 'empty') void recycleRef.current?.empty();
    }, []);

    const titles = useMemo(
      () => [
        {
          subTitle: t('storyboards.title'),
          search: true,
          add: {
            items: [
              { key: 'add', text: t('storyboards.add') },
              { key: 'folder', text: t('storyboards.addFolder') },
            ],
            icon: <PlusOutlined />,
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
                text: t('storyboards.recycle'),
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
          onSearch: listSearch,
        },
        {
          key: 'recycle',
          subTitle: t('storyboards.recycle'),
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
      [add, listSearch, t, batchMode, recycleMenuClick],
    );

    return (
      <Wrapper className={className} defaultActiveKey="list">
        <ListPane key="list">
          <ListTitle {...titles[0]} />
          {batchMode ? (
            <RecycleBatchManager
              orgId={orgId}
              resourceType="STORYBOARD"
              treeData={filteredListData || []}
              onCompleted={handleBatchCompleted}
              onExit={() => setBatchMode(false)}
            />
          ) : (
            <List list={filteredListData} selectedId={selectedId} />
          )}
        </ListPane>
        <ListPane key="recycle">
          <ListTitle {...titles[1]} />
          <RecycleBinManager
            ref={recycleRef}
            orgId={orgId}
            resourceType="STORYBOARD"
          />
        </ListPane>
      </Wrapper>
    );
  },
);

const Wrapper = styled(ListNav)`
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: ${SPACE_XS} 0;
  background-color: ${p => p.theme.componentBackground};
`;
