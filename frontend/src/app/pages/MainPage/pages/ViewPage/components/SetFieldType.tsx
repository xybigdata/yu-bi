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

import { Dropdown, Menu, MenuProps, Tooltip } from 'antd';
import { DataViewFieldType, DateFormat } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { memo, ReactNode } from 'react';
import { ColumnCategories } from '../constants';
import { Column } from '../slice/types';

interface SetFieldTypeProps {
  onChange: (keyPath: string[], name: string) => void;
  field: Column;
  icon: ReactNode;
  hasCategory?: boolean;
  hasFormat?: boolean;
}

const SetFieldType = memo(
  ({
    onChange,
    field,
    hasCategory,
    icon,
    hasFormat = true,
  }: SetFieldTypeProps) => {
    const t = useI18NPrefix('view.schemaTable');
    const tg = useI18NPrefix('global');
    const menuItems: MenuProps['items'] = [
      ...Object.values(DataViewFieldType).map(type => {
        if (type === DataViewFieldType.DATE && hasFormat) {
          return {
            key: type,
            label: tg(`columnType.${type.toLowerCase()}`),
            popupClassName: 'datart-schema-table-header-menu',
            children: Object.values(DateFormat).map(format => ({
              key: format,
              label: format,
            })),
          };
        }
        return {
          key: type,
          label: tg(`columnType.${type.toLowerCase()}`),
        };
      }),
      ...(hasCategory
        ? [
            {
              type: 'divider' as const,
            },
            {
              key: 'categories',
              label: t('category'),
              popupClassName: 'datart-schema-table-header-menu',
              children: Object.values(ColumnCategories).map(category => ({
                key: `category-${category}`,
                label: tg(`columnCategory.${category.toLowerCase()}`),
              })),
            },
          ]
        : []),
    ];

    return (
      <Dropdown
        trigger={['click']}
        dropdownRender={() => (
          <Menu
            selectedKeys={[
              field.type,
              `category-${field.category}`,
              field.dateFormat || '',
            ]}
            className="datart-schema-table-header-menu"
            onClick={({ keyPath }) => onChange(keyPath, field?.name)}
            items={menuItems}
          />
        )}
      >
        <Tooltip
          title={hasCategory ? t('typeAndCategory') : t('category')}
          placement="left"
        >
          {icon}
        </Tooltip>
      </Dropdown>
    );
  },
);
export default SetFieldType;
