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
  CalendarOutlined,
  DatabaseOutlined,
  FieldStringOutlined,
  MoreOutlined,
  NumberOutlined,
  SearchOutlined,
  TableOutlined,
} from '@ant-design/icons';
import {
  Button,
  Col,
  Dropdown,
  Input,
  Row,
  Space,
  TreeDataNode,
  TreeProps,
} from 'antd';
import type { ReactNode } from 'react';
import { loadMonaco } from 'app/components/MonacoEditor/runtime';
import { Tree } from 'app/components';
import { DataViewFieldType } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import useResizeObserver from 'app/hooks/useResizeObserver';
import { useSearchAndExpand } from 'app/hooks/useSearchAndExpand';
import { updateBy } from 'app/utils/mutation';
import { DEFAULT_DEBOUNCE_WAIT } from 'globalConstants';
import {
  memo,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useSelector } from 'react-redux';
import { useAppDispatch } from 'app/hooks/useRedux';
import styled from 'styled-components';
import { SPACE_MD, SPACE_XS } from 'styles/StyleConstants';
import { RootState } from 'types';
import { EditorContext } from '../../EditorContext';
import {
  selectCurrentEditingViewAttr,
  selectDatabaseSchemaLoading,
  selectSourceDatabaseSchemas,
} from '../../slice/selectors';
import { getEditorProvideCompletionItems } from '../../slice/thunks';
import { DatabaseSchema } from '../../slice/types';
import { buildAntdTreeNodeModel } from '../../utils';
import Container from './Container';

type ResourceTreeIconProps = Parameters<
  Extract<TreeProps<TreeDataNode>['icon'], (props: any) => ReactNode>
>[0];

type ResourceTreeIconNode = ResourceTreeIconProps & {
  value?: string[];
  type?: DataViewFieldType | string;
};

const isResourceTreeIconNode = (
  value: ResourceTreeIconProps,
): value is ResourceTreeIconNode => {
  return 'value' in value || 'type' in value;
};

export const Resource = memo(() => {
  const t = useI18NPrefix('view.resource');
  const dispatch = useAppDispatch();
  const { editorCompletionItemProviderRef } = useContext(EditorContext);
  const isDatabaseSchemaLoading = useSelector(selectDatabaseSchemaLoading);
  const sourceId = useSelector<RootState>(state =>
    selectCurrentEditingViewAttr(state, { name: 'sourceId' }),
  ) as string;
  const databaseSchemas = useSelector(state =>
    selectSourceDatabaseSchemas(state, { id: sourceId }),
  );
  const [databaseTreeModel, setDatabaseTreeModel] = useState<TreeDataNode[]>(
    [],
  );

  const { height, ref: treeWrapperRef } = useResizeObserver({
    refreshMode: 'debounce',
    refreshRate: 200,
  });

  const buildTableNode = useCallback((database: DatabaseSchema) => {
    const children =
      database?.tables?.map(table => {
        return buildTableColumnNode([database.dbName], table);
      }) || [];

    return buildAntdTreeNodeModel([], database.dbName, children, false);
  }, []);

  const buildTableColumnNode = (ancestors: string[] = [], table) => {
    const children =
      table?.columns?.map(column => {
        return Object.assign(
          buildAntdTreeNodeModel(
            ancestors.concat(table.tableName),
            column?.name[0],
            [],
            true,
          ),
          { type: column?.type },
        );
      }) || [];
    return buildAntdTreeNodeModel(ancestors, table.tableName, children, false);
  };

  useEffect(() => {
    setDatabaseTreeModel((databaseSchemas || []).map(buildTableNode));
  }, [buildTableNode, databaseSchemas]);

  const { filteredData, onExpand, debouncedSearch, expandedRowKeys } =
    useSearchAndExpand(
      databaseTreeModel,
      (keywords, data) => (data.title as string).includes(keywords),
      DEFAULT_DEBOUNCE_WAIT,
      true,
    );

  useEffect(() => {
    let cancelled = false;
    if (databaseTreeModel && editorCompletionItemProviderRef) {
      editorCompletionItemProviderRef.current?.dispose();
      void loadMonaco().then(monaco => {
        if (cancelled) {
          return;
        }
        dispatch(
          getEditorProvideCompletionItems({
            sourceId,
            resolve: getItem => {
              if (cancelled) {
                return;
              }
              editorCompletionItemProviderRef.current =
                monaco.languages.registerCompletionItemProvider('sql', {
                  provideCompletionItems: getItem,
                });
            },
          }),
        );
      });
    }
    return () => {
      cancelled = true;
    };
  }, [dispatch, sourceId, databaseTreeModel, editorCompletionItemProviderRef]);

  const renderIcon = useCallback((node: ResourceTreeIconProps) => {
    if (!isResourceTreeIconNode(node)) {
      return;
    }
    const { value, type } = node;
    if (Array.isArray(value)) {
      switch (value.length) {
        case 1:
          return <DatabaseOutlined />;
        case 2:
          return <TableOutlined />;
        case 3:
          switch (type as DataViewFieldType) {
            case DataViewFieldType.STRING:
              return <FieldStringOutlined />;
            case DataViewFieldType.NUMERIC:
              return <NumberOutlined />;
            case DataViewFieldType.DATE:
              return <CalendarOutlined />;
          }
      }
    }
  }, []);

  const sortListOrTree = useCallback((list, sortType, sortKey) => {
    return updateBy(list, draft => {
      draft.sort((a, b) => {
        let aname = a[sortKey],
          bname = b[sortKey];
        return String(aname).localeCompare(String(bname));
      });
      draft.forEach(item => {
        if (item.children) {
          item.children = sortListOrTree(item.children, sortType, sortKey);
        }
      });
    });
  }, []);

  const handleMenuClick = useCallback(
    ({ key }) => {
      if (key === 'byNameSort') {
        setDatabaseTreeModel(sortListOrTree(databaseTreeModel, key, 'title'));
      } else {
        setDatabaseTreeModel((databaseSchemas || []).map(buildTableNode));
      }
    },
    [databaseTreeModel, sortListOrTree, databaseSchemas, buildTableNode],
  );

  const menuItems = useMemo(
    () => [
      {
        key: 'sort',
        label: t('sortType'),
        children: [
          {
            key: 'byNameSort',
            label: t('byName'),
          },
          {
            key: 'byOriginalFieldSort',
            label: t('byField'),
          },
        ],
      },
    ],
    [t],
  );

  return (
    <Container title="reference">
      <SearchBar>
        <Col span={24}>
          <StyleSpace>
            <Input
              prefix={<SearchOutlined className="icon" />}
              placeholder={t('search')}
              className="input"
              bordered={false}
              onChange={debouncedSearch}
            />
            <Dropdown
              key="more"
              trigger={['click']}
              menu={{ items: menuItems, onClick: handleMenuClick }}
            >
              <Button type="text" icon={<MoreOutlined />} />
            </Dropdown>
          </StyleSpace>
        </Col>
      </SearchBar>
      <TreeWrapper ref={treeWrapperRef}>
        <Tree
          className="medium"
          treeData={filteredData}
          loading={isDatabaseSchemaLoading}
          icon={renderIcon}
          selectable={false}
          defaultExpandedKeys={expandedRowKeys}
          height={height}
          onExpand={onExpand}
        />
      </TreeWrapper>
    </Container>
  );
});

const SearchBar = styled(Row)`
  .input {
    padding-bottom: ${SPACE_XS};
  }

  .icon {
    color: ${p => p.theme.textColorLight};
  }
`;

const TreeWrapper = styled.div`
  flex: 1;
  padding-bottom: ${SPACE_MD};
  overflow-y: auto;
`;

const StyleSpace = styled(Space)`
  width: 100%;
  > div:nth-child(1) {
    flex: 1;
  }
`;
