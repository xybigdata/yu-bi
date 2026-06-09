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
import { EllipsisOutlined } from '@ant-design/icons';
import { Button, ButtonProps, Dropdown, MenuProps } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import widgetManager from 'app/pages/DashBoardPage/components/WidgetManager';
import useWidgetAction from 'app/pages/DashBoardPage/hooks/useWidgetAction';
import { useWidgetDropdownList } from 'app/pages/DashBoardPage/hooks/useWidgetDropdownList';
import {
  Widget,
  WidgetActionListItem,
  widgetActionType,
} from 'app/pages/DashBoardPage/types/widgetTypes';
import React, { memo, useCallback, useContext, useMemo } from 'react';
import { BoardContext } from '../BoardProvider/BoardProvider';

export const WidgetDropdownList: React.FC<{
  widget: Widget;
  buttonProps?: ButtonProps;
}> = memo(({ widget, buttonProps }) => {
  const { renderMode } = useContext(BoardContext);
  const actions: WidgetActionListItem<widgetActionType>[] =
    widgetManager
      .toolkit(widget.config.originalType)
      .getDropDownList(widget.config) || [];
  const widgetAction = useWidgetAction();
  const actionList = useWidgetDropdownList(renderMode, actions);
  const t = useI18NPrefix(`viz.widget.action`);
  const menuClick = useCallback(
    ({ key }) => {
      widgetAction(key, widget);
    },
    [widgetAction, widget],
  );

  const dropdownItems = useMemo<MenuProps['items']>(() => {
    return actionList
      .filter(item => {
        if (item.key === 'lock') {
          return !widget?.config?.lock;
        } else if (item.key === 'unlock') {
          return widget?.config?.lock;
        }
        return true;
      })
      .flatMap<NonNullable<MenuProps['items']>[number]>(item => {
        const menuItem: NonNullable<MenuProps['items']>[number] = {
          key: item.key,
          danger: item.danger,
          icon: item.icon,
          disabled: item.disabled,
          label: t(item.label || ''),
        };
        return item.divider
          ? [{ key: `${item.key}Divider`, type: 'divider' }, menuItem]
          : [menuItem];
      });
  }, [actionList, widget?.config?.lock, t]);

  if (actionList.length === 0) {
    return null;
  }
  return (
    <Dropdown
      className="widget-tool-dropdown"
      menu={{ items: dropdownItems, onClick: menuClick }}
      placement="bottomCenter"
      trigger={['click']}
      arrow
    >
      <Button icon={<EllipsisOutlined />} type="link" {...buttonProps} />
    </Dropdown>
  );
});
