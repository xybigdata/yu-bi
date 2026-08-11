import {
  ClearOutlined,
  DeleteOutlined,
  SelectOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { Tabs, TreeDataNode } from 'antd';
import {
  ListNav,
  ListPane,
  ListTitle,
  SIDEBAR_TITLE_MORE_MENU_ITEM_CLASS,
  SIDEBAR_TITLE_MORE_MENU_POPUP_CLASS,
} from 'app/components';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useGetVizIcon from 'app/hooks/useGetVizIcon';
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
import { useAddViz } from '../../hooks/useAddViz';
import { SaveFormContext } from '../../SaveFormContext';
import { makeSelectVizTree } from '../../slice/selectors';
import { FolderViewModel } from '../../slice/types';
import { getFolders } from '../../slice/thunks';
import { FolderTree } from './FolderTree';

interface FoldersProps extends I18NComponentProps {
  selectedId?: string;
  className?: string;
}

export const Folders = memo(
  ({ selectedId, className, i18nPrefix }: FoldersProps) => {
    const dispatch = useAppDispatch();
    const orgId = useSelector(selectOrgId);
    const selectVizTree = useMemo(makeSelectVizTree, []);
    const t = useI18NPrefix(i18nPrefix);
    const navigate = useCompatNavigate();
    const { showSaveForm } = useContext(SaveFormContext);
    const addVizFn = useAddViz({ showSaveForm });
    const [batchType, setBatchType] = useState<
      'DATACHART' | 'DASHBOARD' | null
    >(null);
    const datachartRecycleRef = useRef<RecycleBinHandle>(null);
    const dashboardRecycleRef = useRef<RecycleBinHandle>(null);

    const handleBatchCompleted = useCallback(() => {
      setBatchType(null);
      dispatch(getFolders(orgId));
    }, [dispatch, orgId]);

    const getIcon = useGetVizIcon();

    const getDisabled = useCallback(
      ({ deleteLoading }: FolderViewModel) => deleteLoading,
      [],
    );

    const treeData = useSelector(state =>
      selectVizTree(state, { getIcon, getDisabled }),
    );

    const { filteredData: filteredTreeData, debouncedSearch: treeSearch } =
      useDebouncedSearch(treeData, (keywords, d) =>
        d.title.toLowerCase().includes(keywords.toLowerCase()),
      );

    const batchTree = useMemo(
      () =>
        filterVizTree(
          (filteredTreeData || []) as VizBatchNode[],
          batchType || 'DATACHART',
        ),
      [batchType, filteredTreeData],
    );

    const add = useCallback(
      ({ key }) => {
        if (key === 'DATACHART') {
          navigate.push({
            pathname: `/organizations/${orgId}/vizs/chartEditor`,
            search: `dataChartId=&chartType=dataChart&container=dataChart`,
          });
          return false;
        }

        addVizFn({
          vizType: key,
          type: CommonFormTypes.Add,
          open: true,
          initialValues: undefined,
        });
      },
      [orgId, navigate, addVizFn],
    );

    const recycleMenuClick = useCallback(key => {
      const target = key.startsWith('datachart')
        ? datachartRecycleRef.current
        : dashboardRecycleRef.current;
      if (key.endsWith('policy')) void target?.openPolicy();
      if (key.endsWith('empty')) void target?.empty();
    }, []);

    const titles = useMemo(
      () => [
        {
          subTitle: t('folders.folderTitle'),
          add: {
            items: [
              { key: 'DATACHART', text: t('folders.startAnalysis') },
              { key: 'DASHBOARD', text: t('folders.dashboard') },
              { key: 'FOLDER', text: t('folders.folder') },
              { key: 'TEMPLATE', text: t('folders.template') },
            ],
            callback: add,
          },
          more: {
            overlayClassName: SIDEBAR_TITLE_MORE_MENU_POPUP_CLASS,
            itemClassName: SIDEBAR_TITLE_MORE_MENU_ITEM_CLASS,
            items: [
              {
                key: 'batch-datachart',
                text: '批量管理图表',
                prefix: <SelectOutlined className="icon" />,
              },
              {
                key: 'batch-dashboard',
                text: '批量管理仪表盘',
                prefix: <SelectOutlined className="icon" />,
              },
              {
                key: 'recycle',
                text: t('folders.recycle'),
                prefix: <DeleteOutlined className="icon" />,
              },
            ],
            callback: (key, _, onNext) => {
              switch (key) {
                case 'batch-datachart':
                  setBatchType('DATACHART');
                  break;
                case 'batch-dashboard':
                  setBatchType('DASHBOARD');
                  break;
                case 'recycle':
                  setBatchType(null);
                  onNext();
                  break;
              }
            },
          },
          search: true,
          onSearch: treeSearch,
        },
        {
          key: 'recycle',
          subTitle: t('folders.recycle'),
          back: true,
          more: {
            overlayClassName: SIDEBAR_TITLE_MORE_MENU_POPUP_CLASS,
            itemClassName: SIDEBAR_TITLE_MORE_MENU_ITEM_CLASS,
            items: [
              {
                key: 'datachart-policy',
                text: '图表自动清理设置',
                prefix: <SettingOutlined className="icon" />,
              },
              {
                key: 'dashboard-policy',
                text: '仪表盘自动清理设置',
                prefix: <SettingOutlined className="icon" />,
              },
              {
                key: 'datachart-empty',
                text: '清空图表回收站',
                prefix: <ClearOutlined className="icon" />,
              },
              {
                key: 'dashboard-empty',
                text: '清空仪表盘回收站',
                prefix: <ClearOutlined className="icon" />,
              },
            ],
            callback: recycleMenuClick,
          },
        },
      ],
      [add, treeSearch, t, recycleMenuClick],
    );

    return (
      <Wrapper className={className} defaultActiveKey="list">
        <ListPane key="list">
          <ListTitle {...titles[0]} />
          {batchType ? (
            <RecycleBatchManager
              orgId={orgId}
              resourceType={batchType}
              treeData={batchTree}
              onCompleted={handleBatchCompleted}
              onExit={() => setBatchType(null)}
            />
          ) : (
            <FolderTree
              treeData={filteredTreeData}
              selectedId={selectedId}
              i18nPrefix={i18nPrefix}
            />
          )}
        </ListPane>
        <ListPane key="recycle">
          <ListTitle {...titles[1]} />
          <Tabs
            size="small"
            items={[
              {
                key: 'datachart',
                label: '图表',
                children: (
                  <RecycleBinManager
                    ref={datachartRecycleRef}
                    orgId={orgId}
                    resourceType="DATACHART"
                  />
                ),
              },
              {
                key: 'dashboard',
                label: '仪表盘',
                children: (
                  <RecycleBinManager
                    ref={dashboardRecycleRef}
                    orgId={orgId}
                    resourceType="DASHBOARD"
                  />
                ),
              },
            ]}
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

interface VizBatchNode extends TreeDataNode {
  relType?: string;
  relId?: string;
  submitKey?: string | null;
  children?: VizBatchNode[];
}

function filterVizTree(
  nodes: VizBatchNode[],
  resourceType: 'DATACHART' | 'DASHBOARD',
): VizBatchNode[] {
  const result: VizBatchNode[] = [];
  nodes.forEach(node => {
    const children = filterVizTree(node.children || [], resourceType);
    if (node.relType === resourceType && node.relId) {
      result.push({ ...node, key: node.relId, children });
      return;
    }
    if (node.relType === 'FOLDER' && children.length) {
      result.push({
        ...node,
        key: `viz-folder:${resourceType}:${node.key}`,
        submitKey: null,
        selectable: false,
        children,
      });
    }
  });
  return result;
}
