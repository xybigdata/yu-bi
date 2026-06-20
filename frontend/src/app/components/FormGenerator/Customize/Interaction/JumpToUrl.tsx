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

import { Button, Dropdown, Input, Space } from 'antd';
import ChartDataView from 'app/types/ChartDataView';
import { getAllColumnInMeta } from 'app/utils/chartHelper';
import { FC, memo, useCallback, useState } from 'react';
import { InteractionFieldRelation } from '../../constants';
import { normalizeRelations } from './relationUtils';
import {
  CustomizeRelation,
  I18nTranslator,
  JumpToUrlRule,
  VizType,
} from './types';
import UrlParamList from './UrlParamList';

export const buildJumpToUrlRule = (
  value: JumpToUrlRule | undefined,
  url: string | undefined,
  relations: CustomizeRelation[],
): JumpToUrlRule => ({
  ...value,
  url,
  [InteractionFieldRelation.Customize]: relations,
});

const JumpToUrl: FC<
  {
    vizs?: VizType[];
    dataview?: ChartDataView;
    value?: JumpToUrlRule;
    onValueChange: (value: JumpToUrlRule) => void;
  } & I18nTranslator
> = memo(({ vizs, dataview, value, onValueChange, translate: t }) => {
  const [relations, setRelations] = useState<CustomizeRelation[]>(
    value?.[InteractionFieldRelation.Customize] || [],
  );
  const [url, setUrl] = useState(value?.url);

  const handleUpdateRelations = (relations?: CustomizeRelation[]) => {
    const newRelations = normalizeRelations(relations);
    setRelations(newRelations);
    onValueChange(buildJumpToUrlRule(value, url, newRelations));
  };

  const handleUpdateUrl = useCallback(
    (nextUrl: string) => {
      setUrl(nextUrl);
      onValueChange(buildJumpToUrlRule(value, nextUrl, relations));
    },
    [onValueChange, relations, value],
  );
  return (
    <Space>
      <Input
        style={{ width: 200 }}
        value={url}
        placeholder={t('drillThrough.rule.inputUrl')}
        onChange={e => handleUpdateUrl(e.target.value)}
      />
      <Dropdown
        destroyOnHidden
        overlayStyle={{ margin: 4 }}
        popupRender={() => (
          <UrlParamList
            translate={t}
            targetRelId={value?.relId}
            sourceFields={
              getAllColumnInMeta(dataview?.meta)?.concat(
                dataview?.computedFields || [],
              ) || []
            }
            sourceVariables={dataview?.variables || []}
            relations={relations}
            onRelationChange={handleUpdateRelations}
          />
        )}
        placement="bottomLeft"
        trigger={['click']}
        arrow
      >
        <Button type="link">{t('drillThrough.rule.relation.setting')}</Button>
      </Dropdown>
    </Space>
  );
});

export default JumpToUrl;
