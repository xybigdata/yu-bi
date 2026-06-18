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

import { AggregateFieldActionType, ChartInteractionEvent, SortActionType } from 'app/constants';
import { ChartConfigReducerActionType } from 'app/pages/ChartWorkbenchPage/slice/constant';
import { ChartMouseEventParams } from 'app/types/Chart';
import {
  ChartConfig,
  ChartStyleSectionRow,
  SelectedItem,
} from 'app/types/ChartConfig';
import { ChartDatasetPageInfo } from 'app/types/ChartDataSet';
import { ChartDataRequest } from 'app/types/ChartDataRequest';
import { IChartDrillOption } from 'app/types/ChartDrillOption';

type RequestSorter = NonNullable<ChartDataRequest['orders']>[number];

type TablePagingAndSortParams = {
  sorter?: RequestSorter;
  pageInfo: ChartDatasetPageInfo;
};

type ChartEventListenerCallback<T> = (newParams: T) => void;

type RichTextContextParams = {
  type: string;
  payload: {
    ancestors: number[];
    value: ChartStyleSectionRow;
  };
  needRefresh: false;
  updateDrillOption: (config?: ChartConfig) => IChartDrillOption | undefined;
};

const toSortActionType = (direction: unknown): SortActionType | undefined => {
  return direction === SortActionType.ASC || direction === SortActionType.DESC
    ? direction
    : undefined;
};

export const tablePagingAndSortEventListener = (
  param?: ChartMouseEventParams,
  callback?: ChartEventListenerCallback<TablePagingAndSortParams>,
) => {
  if (
    param?.chartType === 'table' &&
    param?.interactionType === ChartInteractionEvent.PagingOrSort
  ) {
    const column = param.seriesName ? [param.seriesName] : undefined;
    const operator = toSortActionType(param.value?.direction);
    callback?.({
      sorter:
        column && operator
          ? {
              column,
              operator,
              aggOperator: param.value?.aggOperator as
                | AggregateFieldActionType
                | undefined,
            }
          : undefined,
      pageInfo: {
        pageNo: param?.value?.pageNo,
      },
    });
  }
};

export const drillDownEventListener = (
  drillOption?: IChartDrillOption,
  param?: ChartMouseEventParams,
  callback?: ChartEventListenerCallback<IChartDrillOption>,
) => {
  if (drillOption?.isSelectedDrill && !drillOption?.isBottomLevel) {
    drillOption?.drillDown(param?.data?.rowData);
    callback?.(drillOption);
  }
};

export const pivotTableDrillEventListener = (
  param?: ChartMouseEventParams,
  callback?: ChartEventListenerCallback<IChartDrillOption>,
) => {
  if (
    param?.chartType === 'pivotSheet' &&
    param?.interactionType === ChartInteractionEvent.Drilled
  ) {
    callback?.(param.drillOption);
  }
};

export const richTextContextEventListener = (
  row: ChartStyleSectionRow,
  param?: ChartMouseEventParams,
  callback?: ChartEventListenerCallback<RichTextContextParams>,
) => {
  if (
    param?.chartType === 'rich-text' &&
    param?.interactionType === ChartInteractionEvent.ChangeContext
  ) {
    callback?.({
      type: ChartConfigReducerActionType.STYLE,
      payload: {
        ancestors: [1, 0],
        value: {
          ...row,
          value: param.value,
        },
      },
      needRefresh: false,
      updateDrillOption: config => {
        return undefined;
      },
    });
  }
};

export const chartSelectionEventListener = (
  param?: ChartMouseEventParams,
  callback?: ChartEventListenerCallback<SelectedItem[]>,
) => {
  if (
    param?.interactionType === ChartInteractionEvent.Select ||
    param?.interactionType === ChartInteractionEvent.UnSelect
  ) {
    callback?.(param?.selectedItems || []);
  }
};
