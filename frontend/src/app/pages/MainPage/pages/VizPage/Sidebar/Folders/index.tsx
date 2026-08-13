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
import {
  RecycleBatchManager,
  RecycleBinHandle,
  RecycleBinManager,
} from 'app/features/recycle';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useGetVizIcon from 'app/hooks/useGetVizIcon';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import { useAppDispatch } from 'app/hooks/useRedux';
import {
  selectIsOrgOwner,
  selectOrgId,
} from 'app/pages/MainPage/slice/selectors';
import { CommonFormTypes } from 'globalConstants';
import {
  memo,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { useSelector } from 'react-redux';
import styled from 'styled-components';
import { SPACE_XS } from 'styles/StyleConstants';
import { useAddViz } from '../../hooks/useAddViz';
import { SaveFormContext } from '../../SaveFormContext';
import { makeSelectVizTree } from '../../slice/selectors';
import { FolderViewModel } from '../../slice/types';
import { getFolders } from '../../slice/thunks';
import { FolderTree } from './FolderTree';

type VizResourceType = 'DATACHART' | 'DASHBOARD';

interface FoldersProps extends I18NComponentProps {
  selectedId?: string;
  className?: string;
}

export const Folders = memo(
  ({ selectedId, className, i18nPrefix }: FoldersProps) => {
    const dispatch = useAppDispatch();
    const orgId = useSelector(selectOrgId);
    const isOwner = useSelector(selectIsOrgOwner);
    const selectVizTree = useMemo(makeSelectVizTree, []);
    const t = useI18NPrefix(i18nPrefix);
    const navigate = useCompatNavigate();
    const { showSaveForm } = useContext(SaveFormContext);
    const addVizFn = useAddViz({ showSaveForm });
    const [activeType, setActiveType] = useState<VizResourceType>('DASHBOARD');
    const [batchMode, setBatchMode] = useState(false);
    const [recycleEmpty, setRecycleEmpty] = useState({
      DATACHART: true,
      DASHBOARD: true,
    });
    const datachartRecycleRef = useRef<RecycleBinHandle>(null);
    const dashboardRecycleRef = useRef<RecycleBinHandle>(null);

    const handleBatchCompleted = useCallback(() => {
      setBatchMode(false);
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

    const selectedResourceType = useMemo(
      () => findResourceType((treeData || []) as VizTreeNode[], selectedId),
      [selectedId, treeData],
    );

    useEffect(() => {
      if (selectedResourceType) {
        setActiveType(selectedResourceType);
      }
    }, [selectedId, selectedResourceType]);

    const scopedTreeData = useMemo(
      () => filterScopedVizTree((treeData || []) as VizTreeNode[], activeType),
      [activeType, treeData],
    );
    const { filteredData: filteredTreeData, debouncedSearch: treeSearch } =
      useDebouncedSearch(scopedTreeData, (keywords, node) =>
        String(node.title).toLowerCase().includes(keywords.toLowerCase()),
      );
    const batchTree = useMemo(
      () => toBatchTree(filteredTreeData || []),
      [filteredTreeData],
    );

    const switchResourceType = useCallback((key: string) => {
      setBatchMode(false);
      setActiveType(key as VizResourceType);
    }, []);

    const add = useCallback(
      ({ key }) => {
        if (key === 'DATACHART') {
          navigate.push({
            pathname: `/organizations/${orgId}/vizs/chartEditor`,
            search: 'dataChartId=&chartType=dataChart&container=dataChart',
          });
          return false;
        }
        addVizFn({
          vizType: key,
          type: CommonFormTypes.Add,
          open: true,
          initialValues:
            key === 'FOLDER' || key === 'TEMPLATE'
              ? { resourceType: activeType }
              : undefined,
        });
      },
      [activeType, addVizFn, navigate, orgId],
    );

    const activeRecycle = useCallback(
      () =>
        activeType === 'DASHBOARD'
          ? dashboardRecycleRef.current
          : datachartRecycleRef.current,
      [activeType],
    );
    const recycleMenuClick = useCallback(
      key => {
        const target = activeRecycle();
        if (key === 'policy') void target?.openPolicy();
        if (key === 'empty') void target?.empty();
      },
      [activeRecycle],
    );

    const addItems = useMemo(
      () =>
        activeType === 'DASHBOARD'
          ? [
              { key: 'DASHBOARD', text: t('folders.dashboard') },
              { key: 'FOLDER', text: t('folders.folder') },
              { key: 'TEMPLATE', text: t('folders.template') },
            ]
          : [
              { key: 'DATACHART', text: t('folders.startAnalysis') },
              { key: 'FOLDER', text: t('folders.folder') },
              { key: 'TEMPLATE', text: t('folders.template') },
            ],
      [activeType, t],
    );

    const permissionReason = !isOwner ? '仅组织所有者可操作' : undefined;
    const emptyReason = recycleEmpty[activeType] ? '当前回收站为空' : undefined;
    const titles = useMemo(
      () => [
        {
          subTitle: t('folders.folderTitle'),
          add: { items: addItems, callback: add },
          more: {
            overlayClassName: SIDEBAR_TITLE_MORE_MENU_POPUP_CLASS,
            itemClassName: SIDEBAR_TITLE_MORE_MENU_ITEM_CLASS,
            items: [
              {
                key: 'batch',
                text: '批量管理',
                prefix: <SelectOutlined className="icon" />,
                disabled: batchMode,
                tooltip: batchMode ? '当前已进入批量管理' : undefined,
              },
              {
                key: 'recycle',
                text: t('folders.recycle'),
                prefix: <DeleteOutlined className="icon" />,
              },
            ],
            callback: (key, _, onNext) => {
              if (key === 'batch') setBatchMode(true);
              if (key === 'recycle') {
                setBatchMode(false);
                onNext?.();
              }
            },
          },
          search: true,
          onSearch: treeSearch,
        },
        {
          subTitle: t('folders.recycle'),
          back: true,
          more: {
            overlayClassName: SIDEBAR_TITLE_MORE_MENU_POPUP_CLASS,
            itemClassName: SIDEBAR_TITLE_MORE_MENU_ITEM_CLASS,
            items: [
              {
                key: 'policy',
                text: '自动清理设置',
                prefix: <SettingOutlined className="icon" />,
                disabled: !isOwner,
                tooltip: permissionReason,
              },
              {
                key: 'empty',
                text: '清空回收站',
                prefix: <ClearOutlined className="icon" />,
                disabled: !isOwner || recycleEmpty[activeType],
                tooltip: permissionReason || emptyReason,
              },
            ],
            callback: recycleMenuClick,
          },
        },
      ],
      [
        activeType,
        add,
        addItems,
        batchMode,
        emptyReason,
        isOwner,
        permissionReason,
        recycleEmpty,
        recycleMenuClick,
        t,
        treeSearch,
      ],
    );

    const tabs = (
      <ResourceTabs
        size="small"
        activeKey={activeType}
        onChange={switchResourceType}
        items={[
          { key: 'DASHBOARD', label: '仪表板' },
          { key: 'DATACHART', label: '数据图表' },
        ]}
      />
    );

    return (
      <Wrapper className={className} defaultActiveKey="list">
        <ListPane key="list">
          <ListTitle {...titles[0]} />
          {tabs}
          {batchMode ? (
            <RecycleBatchManager
              orgId={orgId}
              resourceType={activeType}
              treeData={batchTree}
              onCompleted={handleBatchCompleted}
              onExit={() => setBatchMode(false)}
            />
          ) : (
            <FolderTree
              treeData={filteredTreeData}
              selectedId={selectedId}
              resourceType={activeType}
              i18nPrefix={i18nPrefix}
            />
          )}
        </ListPane>
        <ListPane key="recycle">
          <ListTitle {...titles[1]} />
          {tabs}
          {activeType === 'DASHBOARD' ? (
            <RecycleBinManager
              ref={dashboardRecycleRef}
              orgId={orgId}
              resourceType="DASHBOARD"
              onEntriesChange={count =>
                setRecycleEmpty(value => ({ ...value, DASHBOARD: count === 0 }))
              }
            />
          ) : (
            <RecycleBinManager
              ref={datachartRecycleRef}
              orgId={orgId}
              resourceType="DATACHART"
              onEntriesChange={count =>
                setRecycleEmpty(value => ({ ...value, DATACHART: count === 0 }))
              }
            />
          )}
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

const ResourceTabs = styled(Tabs)`
  flex-shrink: 0;
  padding: 0 8px;
`;

interface VizTreeNode extends TreeDataNode {
  id?: string;
  relType?: string;
  relId?: string;
  subType?: string;
  children?: VizTreeNode[];
}

function filterScopedVizTree(
  nodes: VizTreeNode[],
  resourceType: VizResourceType,
): VizTreeNode[] {
  return nodes.flatMap(node => {
    const children = filterScopedVizTree(node.children || [], resourceType);
    if (node.relType === resourceType) {
      return [{ ...node, children }];
    }
    if (node.relType === 'FOLDER' && node.subType === resourceType) {
      return [{ ...node, children }];
    }
    return [];
  });
}

function toBatchTree(nodes: VizTreeNode[]): VizTreeNode[] {
  return nodes.map(node => ({
    ...node,
    key: node.relType === 'FOLDER' ? node.key : node.relId || node.key,
    children: toBatchTree(node.children || []),
  }));
}

function findResourceType(
  nodes: VizTreeNode[],
  selectedId?: string,
): VizResourceType | undefined {
  if (!selectedId) return undefined;
  for (const node of nodes) {
    if (String(node.key) === selectedId) {
      if (node.relType === 'DATACHART' || node.relType === 'DASHBOARD') {
        return node.relType;
      }
    }
    const childType = findResourceType(node.children || [], selectedId);
    if (childType) return childType;
  }
  return undefined;
}
