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
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { parseBoardConfig } from 'app/migration/BoardConfig/migrateBoardConfig';
import { parseChartConfig } from 'app/utils/ChartDtoHelper';
import { CommonFormTypes } from 'globalConstants';
import { useCallback, useContext } from 'react';
import { useSelector } from 'react-redux';
import { useAppDispatch } from 'app/hooks/useRedux';
import { APIResponse } from 'types';
import { request2 } from 'utils/request';
import { getInsertedNodeIndex } from 'utils/utils';
import { SaveFormContext, SaveFormModel } from '../SaveFormContext';
import { selectVizs } from '../slice/selectors';
import { addViz, saveAsDashboard } from '../slice/thunks';
import { AddVizParams, SaveAsDashboardParams, VizType } from '../slice/types';

type VizDetail = SaveFormModel & {
  id: string;
  config: string;
  orgId: string;
  permissions?: unknown;
};

const emptyVizDetailResponse: APIResponse<VizDetail> = {
  success: false,
  errCode: 0,
  message: '',
  exception: '',
  data: {} as VizDetail,
  warnings: [],
};

export function useSaveAsViz() {
  const { showSaveForm } = useContext(SaveFormContext);
  const vizsData = useSelector(selectVizs);
  const dispatch = useAppDispatch();
  const tg = useI18NPrefix('global');

  const getVizDetail = useCallback(
    async (backendChartId: string, type: string) => {
      const { data } = await request2<VizDetail>(
        {
          method: 'GET',
          url: `viz/${type.toLowerCase()}s/${backendChartId}`,
        },
        undefined,
        {
          onRejected(error) {
            return emptyVizDetailResponse;
          },
        },
      );
      return data;
    },
    [],
  );

  const saveAsViz = useCallback(
    async (vizId: string, type: VizType) => {
      let vizData = await getVizDetail(vizId, type).then(data => {
        return data;
      });
      const boardType = parseBoardConfig(vizData.config || '')?.type;
      const chartConfig = parseChartConfig(vizData.config);

      showSaveForm({
        vizType: type,
        type: CommonFormTypes.SaveAs,
        open: true,
        initialValues: {
          ...vizData,
          parentId: vizData.parentId || void 0,
          name: vizData.name + '_' + tg('copy'),
          boardType: boardType,
        },
        onSave: async (values: SaveFormModel, onClose) => {
          let index = getInsertedNodeIndex(values, vizsData);

          if (type === 'DATACHART') {
            const requestData: AddVizParams['viz'] = Object.assign({}, vizData, {
              ...values,
              parentId: values.parentId || null,
              index,
              avatar: chartConfig?.chartGraphId,
            });

            await dispatch(
              addViz({
                viz: requestData,
                type,
              }),
            );
            onClose?.();
          } else {
            const requestData: SaveAsDashboardParams['viz'] = {
              config: vizData.config,
              id: vizData.id,
              index,
              name: values.name,
              orgId: vizData.orgId,
              parentId: values.parentId || null,
              permissions: vizData.permissions,
              subType: boardType,
              boardType: boardType,
            };

            await dispatch(
              saveAsDashboard({
                viz: requestData,
                dashboardId: vizData.id,
              }),
            );
            onClose?.();
          }
        },
      });
    },
    [showSaveForm, tg, vizsData, dispatch, getVizDetail],
  );

  return saveAsViz;
}
