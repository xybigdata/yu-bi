import { PlusOutlined } from '@ant-design/icons';
import { Dropdown, Menu, Tooltip } from 'antd';
import React, { ReactElement } from 'react';
import { ToolbarButton } from '../ToolbarButton';

interface AddButtonProps {
  dataSource: {
    items: Array<{ key: string; text: string }>;
    icon?: ReactElement;
    callback: (menuClickHandler?: any) => void | false;
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
        onClick={() => callback()}
      />
    </Tooltip>
  ) : (
    <Dropdown
      trigger={['click']}
      overlay={
        <Menu onClick={callback}>
          {items.map(({ key, text }) => (
            <Menu.Item key={key}>{text}</Menu.Item>
          ))}
        </Menu>
      }
    >
      <ToolbarButton size="small" icon={icon || <PlusOutlined />} />
    </Dropdown>
  );
}
