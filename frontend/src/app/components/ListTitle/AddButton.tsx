import { PlusOutlined } from '@ant-design/icons';
import { Dropdown, MenuProps, Tooltip } from 'antd';
import React, { ReactElement } from 'react';
import { ToolbarButton } from '../ToolbarButton';

type MenuClickInfo = Parameters<NonNullable<MenuProps['onClick']>>[0];
type AddButtonCallback =
  | (() => void | false)
  | ((menuClickHandler: MenuClickInfo) => void | false);

interface AddButtonProps {
  dataSource: {
    items: Array<{ key: string; text: string }>;
    icon?: ReactElement;
    callback: AddButtonCallback;
  };
}

export function AddButton({
  dataSource: { items, icon, callback },
}: AddButtonProps) {
  return items.length < 2 ? (
    <Tooltip title={items[0].text} placement="bottom">
      <ToolbarButton
        size="small"
        icon={icon || <PlusOutlined />}
        onClick={() => (callback as () => void | false)()}
      />
    </Tooltip>
  ) : (
    <Dropdown
      trigger={['click']}
      menu={{
        items: items.map(({ key, text }) => ({ key, label: text })),
        onClick: callback as MenuProps['onClick'],
      }}
    >
      <ToolbarButton size="small" icon={icon || <PlusOutlined />} />
    </Dropdown>
  );
}
