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

import { CheckOutlined } from '@ant-design/icons';
import { Menu, MenuProps, Radio, Space } from 'antd';
import {
  AggregateFieldActionType,
  AggregateFieldSubAggregateType,
  ChartDataSectionFieldActionType,
} from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { ChartDataSectionField } from 'app/types/ChartConfig';
import { updateBy } from 'app/utils/mutation';
import { FC, useState } from 'react';

interface AggregationActionMenuItemsArgs {
  actionType: typeof ChartDataSectionFieldActionType.Aggregate;
  aggregate?: AggregateFieldActionType;
  onChange: (selectedValue: AggregateFieldActionType) => void;
  t: (key: string) => string;
}

export const buildAggregationMenuItems = ({
  actionType,
  aggregate,
  onChange,
  t,
}: AggregationActionMenuItemsArgs): MenuProps['items'] => {
  return AggregateFieldSubAggregateType[actionType]?.map(agg => ({
    key: agg,
    icon: aggregate === agg ? <CheckOutlined /> : undefined,
    label: t(agg),
    onClick: () => onChange(agg),
  }));
};

const AggregationAction: FC<{
  config: ChartDataSectionField;
  onConfigChange: (
    config: ChartDataSectionField,
    needRefresh?: boolean,
  ) => void;
  mode?: 'menu';
}> = ({ config, onConfigChange, mode }) => {
  const t = useI18NPrefix(`viz.common.enum.aggregateTypes`);
  const actionNeedNewRequest = true;
  const [aggregate, setAggregate] = useState(config?.aggregate);

  const onChange = (selectedValue: AggregateFieldActionType) => {
    const newConfig = updateBy(config, draft => {
      draft.aggregate = selectedValue;
    });
    setAggregate(selectedValue);
    onConfigChange?.(newConfig, actionNeedNewRequest);
  };

  const renderOptions = (mode?: 'menu') => {
    if (mode === 'menu') {
      return (
        <Menu
          selectable={false}
          items={buildAggregationMenuItems({
            actionType: ChartDataSectionFieldActionType.Aggregate,
            aggregate,
            onChange,
            t,
          })}
        />
      );
    }

    return (
      <Radio.Group onChange={e => onChange(e.target?.value)} value={aggregate}>
        <Space direction="vertical">
          {AggregateFieldSubAggregateType[
            ChartDataSectionFieldActionType.Aggregate
          ]?.map(agg => {
            return (
              <Radio key={agg} value={agg}>
                {t(agg)}
              </Radio>
            );
          })}
        </Space>
      </Radio.Group>
    );
  };

  return renderOptions(mode);
};

export default AggregationAction;
