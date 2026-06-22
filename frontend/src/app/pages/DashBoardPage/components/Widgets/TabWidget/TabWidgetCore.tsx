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
import type { TabsProps } from 'antd';
import { Tabs } from 'antd';
import { getTabWidgetContent } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { memo, useCallback, useContext, useEffect, useState } from 'react';
import { useAppDispatch } from 'app/hooks/useRedux';
import { scheduleMicrotask } from 'app/pages/DashBoardPage/utils/scheduleMicrotask';
import styled from 'styled-components';
import { PRIMARY } from 'styles/StyleConstants';
import { uuidv4 } from 'utils/utils';
import { editBoardStackActions } from '../../../pages/BoardEditor/slice';
import { WidgetActionContext } from '../../ActionProvider/WidgetActionProvider';
import { BoardContext } from '../../BoardProvider/BoardProvider';
import { DropHolder } from '../../WidgetComponents/DropHolder';
import { WidgetMapper } from '../../WidgetMapper/WidgetMapper';
import { WidgetInfoContext } from '../../WidgetProvider/WidgetInfoProvider';
import { WidgetContext } from '../../WidgetProvider/WidgetProvider';
import { WidgetWrapProvider } from '../../WidgetProvider/WidgetWrapProvider';
import tabProto, { TabToolkit } from './tabConfig';

const { TabPane } = Tabs;

export const TabWidgetCore: React.FC<{}> = memo(() => {
  const dispatch = useAppDispatch();
  const widget = useContext(WidgetContext);
  const { align, position } = (tabProto.toolkit as TabToolkit).getCustomConfig(
    widget.config.customConfig.props,
  );
  const { editing } = useContext(WidgetInfoContext);
  const { onEditSelectWidget } = useContext(WidgetActionContext);
  const {
    boardType,
    editing: boardEditing,
    boardId,
  } = useContext(BoardContext);
  const itemMap = getTabWidgetContent(widget)?.itemMap || {};
  const tabsCons = Object.values(itemMap).sort((a, b) => a.index - b.index);
  const [activeKey, SetActiveKey] = useState<string | number>(
    tabsCons[0]?.index || 0,
  );

  useEffect(() => {
    const tab = tabsCons?.find(t => String(t.index) === String(activeKey));
    if (tab && editing) {
      onEditSelectWidget({
        multipleKey: false,
        id: tab.childWidgetId,
        selected: true,
      });
    }
  }, [activeKey, editing, onEditSelectWidget, tabsCons]);

  const onTabClick: TabsProps['onTabClick'] = useCallback(activeKey => {
    SetActiveKey(activeKey);
  }, []);

  const tabAdd = useCallback(() => {
    const newTabId = `tab_${uuidv4()}`;
    const maxIndex = tabsCons[tabsCons.length - 1]?.index || 0;
    const nextIndex = maxIndex + 1;
    dispatch(
      editBoardStackActions.tabsWidgetAddTab({
        parentId: widget.id,
        tabItem: {
          index: nextIndex,
          name: 'tab',
          tabId: newTabId,
          childWidgetId: '',
        },
      }),
    );
    scheduleMicrotask(() => {
      SetActiveKey(nextIndex);
    });
  }, [dispatch, tabsCons, widget.id]);

  const tabRemove = useCallback(
    (targetKey: React.MouseEvent | React.KeyboardEvent | string) => {
      const tabId =
        typeof targetKey === 'string'
          ? tabsCons.find(tab => String(tab.index) === targetKey)?.tabId || ''
          : '';
      dispatch(
        editBoardStackActions.tabsWidgetRemoveTab({
          parentId: widget.id,
          sourceTabId: tabId,
          mode: boardType,
        }),
      );
      scheduleMicrotask(() => {
        SetActiveKey(tabsCons[0].index);
      });
    },

    [dispatch, widget.id, boardType, tabsCons],
  );

  const tabEdit: TabsProps['onEdit'] = useCallback(
    (targetKey, action: 'add' | 'remove') => {
      action === 'add' ? tabAdd() : tabRemove(targetKey);
    },
    [tabAdd, tabRemove],
  );

  return (
    <TabsBoxWrap className="TabsBoxWrap" tabsAlign={align}>
      <Tabs
        onTabClick={editing ? onTabClick : undefined}
        size="small"
        tabBarGutter={1}
        tabPosition={position}
        activeKey={editing ? String(activeKey) : undefined}
        tabBarStyle={{ fontSize: '16px' }}
        type={editing ? 'editable-card' : undefined}
        onEdit={editing ? tabEdit : undefined}
        destroyInactiveTabPane
      >
        {tabsCons.map(tab => (
          <TabPane
            tab={tab.name || 'tab'}
            key={tab.index}
            className="TabPane"
            forceRender
          >
            {tab.childWidgetId ? (
              <WidgetWrapProvider
                id={tab.childWidgetId}
                boardEditing={boardEditing}
                boardId={boardId}
              >
                <MapWrapper>
                  <WidgetMapper boardEditing={boardEditing} hideTitle={true} />
                </MapWrapper>
              </WidgetWrapProvider>
            ) : (
              boardEditing && (
                <DropHolder tabItem={tab} tabWidgetId={widget.id} />
              )
            )}
          </TabPane>
        ))}
      </Tabs>
    </TabsBoxWrap>
  );
});
const MapWrapper = styled.div`
  position: relative;
  box-sizing: border-box;
  display: flex;
  flex: 1;
  width: 100%;
  height: 100%;
`;
const TabsBoxWrap = styled.div<{ tabsAlign: string }>`
  width: 100%;
  height: 100%;

  & .ant-tabs {
    width: 100%;
    height: 100%;
    background: none;
  }

  & .ant-tabs-content {
    width: 100%;
    height: 100%;
  }

  .ant-tabs-nav {
    margin: 0;
  }

  .ant-tabs-tab {
    padding: 0 !important;
    margin-right: 30px;
  }
  & .ant-tabs.ant-tabs-card.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab {
    margin: 0 10px;
  }
  & .TabPane {
    width: 100%;
    height: 100%;
  }
  & .ant-tabs-tab-remove {
    background-color: ${PRIMARY};
  }

  & .ant-tabs > .ant-tabs-nav .ant-tabs-nav-add {
    padding: 0;
    /* color: ${PRIMARY}; */
    margin: 0 20px;
    background: none;
    border: none;
  }

  & .ant-tabs .ant-tabs-nav-wrap {
    justify-content: ${p => p.tabsAlign};

    & > .ant-tabs-nav-list {
      flex: none;
    }
  }
`;
