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

import {
  InteractionAction,
  InteractionCategory,
  InteractionMouseEvent,
} from 'app/components/FormGenerator/constants';
import {
  CrossFilteringSetting,
  DrillThroughSetting,
  ViewDetailSetting,
} from 'app/components/FormGenerator/Customize/Interaction/types';
import { ChartInteractionEvent } from 'app/constants';
import useDrillThrough from 'app/hooks/useDrillThrough';
import type { DisplayViewDetailProps } from 'app/pages/MainPage/pages/VizPage/hooks/useDisplayViewDetail';
import type { ExecuteToken } from 'app/pages/SharePage/slice/types';
import type { VizType } from 'app/pages/MainPage/pages/VizPage/slice/types';
import type { ModalFuncProps } from 'antd';
import { ChartDataRequestBuilder } from 'app/models/ChartDataRequestBuilder';
import type { IChartDrillOption } from 'app/types/ChartDrillOption';
import type {
  ChartConfig,
  ChartStyleConfig,
  SelectedItem,
} from 'app/types/ChartConfig';
import type { ChartMouseEventParams } from 'app/types/Chart';
import type { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import type ChartDataView from 'app/types/ChartDataView';
import { getStyles, getValue } from 'app/utils/chartHelper';
import {
  buildClickEventBaseFilters,
  getJumpFiltersByInteractionRule,
  getJumpOperationFiltersByInteractionRule,
  getLinkFiltersByInteractionRule,
  getVariablesByInteractionRule,
  variableToFilter,
} from 'app/utils/internalChartHelper';
import { useCallback } from 'react';
import { isEmpty, isEmptyArray } from 'utils/object';
import { stringifyQuery } from 'utils/queryString';
import { urlSearchTransfer } from 'utils/urlSearchTransfer';

type ChartInteractionClickEventParams = Pick<
  ChartMouseEventParams,
  'interactionType'
> & {
  selectedItems?: SelectedItem[];
};

type ChartInteractionConfigs = ChartStyleConfig[];
type ChartInteractionView = Pick<
  ChartDataView,
  'id' | 'meta' | 'computedFields' | 'type'
> & {
  config: string | object;
};

type CrossFilteringLinkParam = {
  rule: NonNullable<CrossFilteringSetting['rules']>[number];
  isUnSelectedAll: boolean;
  filters: ReturnType<typeof getLinkFiltersByInteractionRule>;
  variables: ReturnType<typeof getVariablesByInteractionRule>;
};

interface DrillThroughEventParams {
  drillOption?: IChartDrillOption;
  drillThroughSetting?: DrillThroughSetting | null;
  clickEventParams?: ChartInteractionClickEventParams;
  targetEvent?: InteractionMouseEvent | ChartInteractionEvent;
  ruleId?: string;
  orgId?: string;
  view?: ChartInteractionView;
  queryVariables?: ChartDataView['variables'];
  computedFields?: ChartDataViewMeta[];
  aggregation?: boolean;
  chartConfig?: ChartConfig;
  isJumpUrlOnly?: boolean;
}

interface ViewDataEventParams {
  drillOption?: IChartDrillOption;
  clickEventParams?: ChartInteractionClickEventParams;
  targetEvent?: InteractionMouseEvent | ChartInteractionEvent;
  viewDetailSetting?: ViewDetailSetting | null;
  chartConfig?: ChartConfig;
  view?: ChartInteractionView;
  authToken?: ExecuteToken;
}

interface CrossFilteringEventParams {
  drillOption?: IChartDrillOption;
  crossFilteringSetting?: CrossFilteringSetting | null;
  clickEventParams?: ChartInteractionClickEventParams;
  targetEvent?: InteractionMouseEvent | ChartInteractionEvent;
  view?: ChartInteractionView;
  queryVariables?: ChartDataView['variables'];
  computedFields?: ChartDataViewMeta[];
  aggregation?: boolean;
  chartConfig?: ChartConfig;
}

type JumpVizDialogParams = {
  orgId: string;
  vizId: string;
  vizType: VizType;
  params?: string;
};

type JumpUrlDialogParams = Pick<
  ModalFuncProps,
  'width' | 'bodyStyle' | 'content'
>;

const useChartInteractions = (props: {
  openViewDetailPanel?: (props: DisplayViewDetailProps) => void;
  openJumpVizDialogModal?: (props: JumpVizDialogParams) => void;
  openJumpUrlDialogModal?: (props: JumpUrlDialogParams) => void;
}) => {
  const {
    openNewTab,
    openBrowserTab,
    getDialogContent,
    redirectByUrl,
    openNewByUrl,
    getDialogContentByUrl,
  } = useDrillThrough();

  const getDrillThroughSetting = (
    chartInteractions: ChartInteractionConfigs = [],
    boardInteractions: ChartInteractionConfigs = [],
  ): DrillThroughSetting | null => {
    const enableBoardDrillThrough = getValue(boardInteractions, [
      'drillThrough',
    ]);
    if (enableBoardDrillThrough) {
      return getStyles(boardInteractions, ['drillThrough'], ['setting'])?.[0];
    }
    const enableChartDrillThrough = getValue(chartInteractions, [
      'drillThrough',
    ]);
    if (enableChartDrillThrough) {
      return getStyles(chartInteractions, ['drillThrough'], ['setting'])?.[0];
    } else {
      return null;
    }
  };

  const getCrossFilteringSetting = (
    chartInteractions: ChartInteractionConfigs = [],
    boardInteractions: ChartInteractionConfigs = [],
  ): CrossFilteringSetting | null => {
    const enableBoardCrossFiltering = getValue(boardInteractions, [
      'crossFiltering',
    ]);
    if (enableBoardCrossFiltering) {
      return getStyles(boardInteractions, ['crossFiltering'], ['setting'])?.[0];
    }
    const enableChartCrossFiltering = getValue(chartInteractions, [
      'crossFiltering',
    ]);
    if (enableChartCrossFiltering) {
      return getStyles(chartInteractions, ['crossFiltering'], ['setting'])?.[0];
    } else {
      return null;
    }
  };

  const getViewDetailSetting = (
    chartInteractions: ChartInteractionConfigs = [],
    boardInteractions: ChartInteractionConfigs = [],
  ): ViewDetailSetting | null => {
    const enableBoardViewDetail = getValue(boardInteractions, ['viewDetail']);
    if (enableBoardViewDetail) {
      return getStyles(boardInteractions, ['viewDetail'], ['setting'])?.[0];
    }
    const enableChartViewDetail = getValue(chartInteractions, ['viewDetail']);
    if (enableChartViewDetail) {
      return getStyles(chartInteractions, ['viewDetail'], ['setting'])?.[0];
    } else {
      return null;
    }
  };

  const handleDrillThroughEvent = useCallback(
    ({
      drillOption,
      drillThroughSetting,
      clickEventParams,
      targetEvent,
      ruleId,
      orgId,
      view,
      queryVariables,
      computedFields,
      aggregation,
      chartConfig,
      isJumpUrlOnly,
    }: DrillThroughEventParams) => {
      if (drillThroughSetting) {
        const sourceChartFilters = new ChartDataRequestBuilder(
          {
            id: view?.id || '',
            config: view?.config || {},
            meta: view?.meta,
            computedFields: computedFields || [],
            type: view?.type,
          },
          chartConfig?.datas,
          chartConfig?.settings,
          {},
          false,
          aggregation,
        )
          .addDrillOption(drillOption)
          .getColNameStringFilter();
        const sourceChartNonAggFilters = (sourceChartFilters || []).filter(
          f => !Boolean(f.aggOperator),
        );
        const hasNoSelectedItems = isEmptyArray(
          clickEventParams?.selectedItems,
        );

        if (hasNoSelectedItems) {
          return;
        }

        (drillThroughSetting?.rules || [])
          .filter(rule => rule.event === targetEvent)
          .filter(rule => isEmpty(ruleId) || rule.id === ruleId)
          .filter(
            rule =>
              !isJumpUrlOnly ||
              rule?.category === InteractionCategory.JumpToUrl,
          )
          .forEach(rule => {
            const clickFilters = buildClickEventBaseFilters(
              clickEventParams?.selectedItems?.map(item => item?.data?.rowData),
              rule,
              drillOption,
              chartConfig?.datas,
            );
            const relId = rule?.[rule.category!]?.relId;
            if (rule.category === InteractionCategory.JumpToChart) {
              if (!orgId || !relId) {
                return;
              }
              const urlFilters = getJumpOperationFiltersByInteractionRule(
                clickFilters,
                sourceChartFilters,
                rule,
              );
              const clickVariables = getVariablesByInteractionRule(
                queryVariables,
                rule,
              );
              const urlFiltersStr: string = stringifyQuery({
                filters: urlFilters || [],
                variables: clickVariables,
              });
              if (rule?.action === InteractionAction.Redirect) {
                openNewTab(orgId, relId, urlFiltersStr);
              }
              if (rule?.action === InteractionAction.Window) {
                openBrowserTab(orgId, relId, urlFiltersStr);
              }
              if (rule?.action === InteractionAction.Dialog) {
                const modalContent = getDialogContent(
                  orgId,
                  relId,
                  'DATACHART',
                  urlFiltersStr,
                );
                props?.openJumpVizDialogModal?.(modalContent);
              }
            } else if (rule.category === InteractionCategory.JumpToDashboard) {
              if (!orgId || !relId) {
                return;
              }
              const variableFilters = variableToFilter(
                getVariablesByInteractionRule(queryVariables, rule),
              );
              const urlFilters = getJumpFiltersByInteractionRule(
                clickFilters,
                sourceChartNonAggFilters,
                variableFilters,
                rule,
              );
              Object.assign(urlFilters, { isMatchByName: true });
              const urlFiltersStr: string =
                urlSearchTransfer.toUrlString(urlFilters);
              if (rule?.action === InteractionAction.Redirect) {
                openNewTab(orgId, relId, urlFiltersStr);
              }
              if (rule?.action === InteractionAction.Window) {
                openBrowserTab(orgId, relId, urlFiltersStr);
              }
              if (rule?.action === InteractionAction.Dialog) {
                const modalContent = getDialogContent(
                  orgId,
                  relId,
                  'DASHBOARD',
                  urlFiltersStr,
                );
                props?.openJumpVizDialogModal?.(modalContent);
              }
            } else if (rule.category === InteractionCategory.JumpToUrl) {
              const variableFilters = variableToFilter(
                getVariablesByInteractionRule(queryVariables, rule),
              );
              const urlFilters = getJumpFiltersByInteractionRule(
                clickFilters,
                sourceChartNonAggFilters,
                variableFilters,
                rule,
              );
              Object.assign(urlFilters, { isMatchByName: true });
              const urlFiltersStr: string =
                urlSearchTransfer.toUrlString(urlFilters);
              const url = rule?.[rule.category!]?.url;
              if (!url) {
                return;
              }
              if (rule?.action === InteractionAction.Redirect) {
                redirectByUrl(url, urlFiltersStr);
              }
              if (rule?.action === InteractionAction.Window) {
                openNewByUrl(url, urlFiltersStr);
              }
              if (rule?.action === InteractionAction.Dialog) {
                const modalContent = getDialogContentByUrl(url, urlFiltersStr);
                props?.openJumpUrlDialogModal?.(modalContent);
              }
            }
          });
      }
    },
    [
      openNewTab,
      openBrowserTab,
      getDialogContent,
      props,
      redirectByUrl,
      openNewByUrl,
      getDialogContentByUrl,
    ],
  );

  const handleCrossFilteringEvent = useCallback(
    (
      {
        drillOption,
        crossFilteringSetting,
        clickEventParams,
        targetEvent,
        view,
        queryVariables,
        computedFields,
        aggregation,
        chartConfig,
      }: CrossFilteringEventParams,
      callback?: (linkParams: CrossFilteringLinkParam[]) => void,
    ) => {
      if (
        !crossFilteringSetting ||
        crossFilteringSetting?.event !== targetEvent
      ) {
        return null;
      }
      const nonAggChartFilters = new ChartDataRequestBuilder(
        {
          id: view?.id || '',
          config: view?.config || {},
          meta: view?.meta,
          computedFields: computedFields || [],
        },
        chartConfig?.datas,
        chartConfig?.settings,
        {},
        false,
        aggregation,
      )
        .addDrillOption(drillOption)
        .getColNameStringFilter()
        ?.filter(f => !Boolean(f.aggOperator));

      const linkParams: CrossFilteringLinkParam[] = (
        crossFilteringSetting?.rules || []
      ).map(rule => {
        const variableFilters = variableToFilter(
          getVariablesByInteractionRule(queryVariables, rule),
        );
        const clickFilters = buildClickEventBaseFilters(
          clickEventParams?.selectedItems?.map(item => item?.data?.rowData),
          rule,
          drillOption,
          chartConfig?.datas,
        );
        const filters = getLinkFiltersByInteractionRule(
          clickFilters,
          nonAggChartFilters,
          variableFilters,
          rule,
        );
        const variables = getVariablesByInteractionRule(queryVariables, rule);
        const isUnSelectedAll =
          clickEventParams?.interactionType === ChartInteractionEvent.UnSelect;
        return {
          rule,
          isUnSelectedAll,
          filters,
          variables,
        };
      });
      callback?.(linkParams);
    },
    [],
  );

  const handleViewDataEvent = useCallback(
    ({
      drillOption,
      clickEventParams,
      targetEvent,
      viewDetailSetting,
      chartConfig,
      view,
      authToken,
    }: ViewDataEventParams) => {
      if (viewDetailSetting?.event === targetEvent) {
        const clickFilters = buildClickEventBaseFilters(
          clickEventParams?.selectedItems?.map(item => item?.data?.rowData),
          undefined,
          drillOption,
          chartConfig?.datas,
        );
        const hasNoSelectedItems = isEmptyArray(
          clickEventParams?.selectedItems,
        );
        if (hasNoSelectedItems) {
          return;
        }
        props?.openViewDetailPanel?.({
          currentDataView: view,
          chartConfig: chartConfig,
          drillOption: drillOption,
          viewDetailSetting: viewDetailSetting || undefined,
          clickFilters: clickFilters,
          authToken,
        });
      }
    },
    [props?.openViewDetailPanel],
  );

  return {
    getDrillThroughSetting,
    getCrossFilteringSetting,
    getViewDetailSetting,
    handleDrillThroughEvent,
    handleCrossFilteringEvent,
    handleViewDataEvent,
  };
};

export default useChartInteractions;
