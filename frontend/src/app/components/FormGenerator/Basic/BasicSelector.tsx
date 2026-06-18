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

import { Select } from 'antd';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo, useMemo } from 'react';
import { isEmpty, isFunc } from 'utils/object';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';
import { BW } from './components/BasicWrapper';
import { omitFormGeneratorOptions } from './controlOptions';

type SelectorOptionValue = string | number | boolean;
type SelectorOption =
  | SelectorOptionValue
  | {
      label?: SelectorOptionValue;
      value?: SelectorOptionValue;
    };

const isSelectorOptionObject = (
  option: SelectorOption,
): option is Exclude<SelectorOption, SelectorOptionValue> =>
  typeof option === 'object' && option !== null;

const BasicSelector: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({
    ancestors,
    translate: t = title => title,
    data: row,
    dataConfigs,
    onChange,
  }) => {
    const { options } = row;
    const { translateItemLabel, hideLabel } = options || {};
    const selectOptions = omitFormGeneratorOptions(options);

    const handleSelectorValueChange = value => {
      onChange?.(ancestors, value, options?.needRefresh);
    };

    const cachedDataConfigs = useMemo(
      () => dataConfigs?.map(col => ({ ...col })),
      [dataConfigs],
    );

    const safeInvokeAction = () => {
      let results: SelectorOption[] = [];
      try {
        results = isFunc(row?.options?.getItems)
          ? row?.options?.getItems?.call(
              Object.create(null),
              cachedDataConfigs,
            ) || []
          : row?.options?.items || [];
      } catch (error) {
        console.error(`BasicSelector | invoke action error ---> `, error);
      } finally {
        return results;
      }
    };

    return (
      <BW label={!hideLabel ? t(row.label, true) : ''}>
        <Select
          className="datart-ant-select"
          dropdownMatchSelectWidth
          {...selectOptions}
          value={row.value}
          disabled={row.disabled}
          defaultValue={row.default}
          placeholder={t('select')}
          options={safeInvokeAction()?.map(o => {
            const label =
              isSelectorOptionObject(o) && !isEmpty(o.label) ? o.label : o;
            const value =
              isSelectorOptionObject(o) && !isEmpty(o.value) ? o.value : o;
            return {
              label: !!translateItemLabel ? t(`${label}`, true) : label,
              value,
            };
          })}
          onChange={handleSelectorValueChange}
        />
      </BW>
    );
  },
  itemLayoutComparer,
);

export default BasicSelector;
