import { MenuProps } from 'antd';

type MenuItems = NonNullable<MenuProps['items']>;

interface MenuItemGroup {
  key: string;
  items: MenuItems;
}

export function joinMenuItemGroups(groups: MenuItemGroup[]): MenuItems {
  return groups
    .filter(group => group.items.length > 0)
    .flatMap((group, index) => [
      ...(index > 0
        ? [{ key: `${group.key}Line`, type: 'divider' as const }]
        : []),
      ...group.items,
    ]);
}
