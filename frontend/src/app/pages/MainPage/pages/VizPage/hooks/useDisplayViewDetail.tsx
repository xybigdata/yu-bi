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

import { Table, Tabs } from 'antd';
import { InteractionFieldMapper } from 'app/components/FormGenerator/constants';
import { ViewDetailSetting } from 'app/components/FormGenerator/Customize/Interaction/types';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import useMount from 'app/hooks/useMount';
import useStateModal, { StateModalSize } from 'app/hooks/useStateModal';
import { ExecuteToken } from 'app/pages/SharePage/slice/types';
import { ChartDataRequestBuilder } from 'app/models/ChartDataRequestBuilder';
import { ChartConfig, ChartDataConfig } from 'app/types/ChartConfig';
import {
  ChartDataRequest,
  PendingChartDataRequestFilter,
} from 'app/types/ChartDataRequest';
import ChartDataSetDTO, { ChartDatasetMeta } from 'app/types/ChartDataSet';
import ChartDataView from 'app/types/ChartDataView';
import { IChartDrillOption } from 'app/types/ChartDrillOption';
import { fetchChartDataSet } from 'app/utils/fetch';
import { FC, memo, useState } from 'react';
import styled from 'styled-components';
import { SPACE_XS } from 'styles/StyleConstants';
import { errorHandle } from 'utils/utils';

const { TabPane } = Tabs;

type ViewDetailDataView = Pick<
  ChartDataView,
  'id' | 'meta' | 'computedFields' | 'type'
> & {
  config: string | object;
};

const filterTableColumnsByViewDetailSetting = (
  datas?: ChartDataConfig[],
  viewDetailSetting?: ViewDetailSetting,
) => {
  if (viewDetailSetting?.mapper === InteractionFieldMapper.All) {
    return datas;
  }
  const enableColumns: string[] = viewDetailSetting?.customize || [];
  return datas?.map(section => {
    const rows = section?.rows?.filter(r => enableColumns?.includes(r.colName));
    return Object.assign({}, section, { rows });
  });
};

const TemplateTable: FC<{
  requestParams: ChartDataRequest;
  chartConfig?: ChartConfig;
  token?: ExecuteToken;
}> = memo(({ chartConfig, requestParams, token }) => {
  const [datas, setSDatas] = useState<ChartDataSetDTO['rows']>();
  const [columns, setColumns] = useState<Array<{ title: string; dataIndex: number }>>();

  useMount(async () => {
    try {
      const response = await fetchChartDataSet(requestParams, token);
      setSDatas(response?.rows);
      setColumns(getTableColumns(response?.columns));
    } catch (error) {
      errorHandle(error);
    }
  });

  const getTableColumns = (columns?: ChartDatasetMeta[]) => {
    const allConfigFields = chartConfig?.datas?.flatMap(d => d.rows || []);
    return (columns || []).map((col, index) => {
      const renderName = getRenderTitle(col);
      const currentConfig = allConfigFields?.find(
        f => f.colName === renderName,
      );
      return {
        title: currentConfig?.alias?.name || renderName || '',
        dataIndex: index,
      };
    });
  };

  const getRenderTitle = (column?: ChartDatasetMeta) =>
    Array.isArray(column?.name) ? column?.name?.join('.') : column?.name;

  return (
    <div>
      <Table
        loading={!Boolean(datas)}
        dataSource={datas}
        columns={columns}
        rowKey="id"
        size="small"
        pagination={{ hideOnSinglePage: true, pageSize: 10 }}
        scroll={{ x: 'max-content', y: 600 }}
      />
    </div>
  );
});

export type DisplayViewDetailProps = {
  currentDataView?: ViewDetailDataView;
  chartConfig?: ChartConfig;
  drillOption?: IChartDrillOption;
  viewDetailSetting?: ViewDetailSetting;
  clickFilters?: PendingChartDataRequestFilter[];
  authToken?: ExecuteToken;
};

const useDisplayViewDetail = () => {
  const t = useI18NPrefix('viz.palette.interaction.viewDetail');
  const [openStateModal, contextHolder] = useStateModal({});

  const getSummaryTableRequestParams = ({
    currentDataView,
    chartConfig,
    drillOption,
    viewDetailSetting,
    clickFilters,
  }: DisplayViewDetailProps) => {
    const builder = new ChartDataRequestBuilder(
      currentDataView!,
      filterTableColumnsByViewDetailSetting(
        chartConfig?.datas,
        viewDetailSetting,
      ),
    );
    return builder
      .addRuntimeFilters(clickFilters)
      .addDrillOption(drillOption)
      .build();
  };

  const getDetailsTableRequestParams = ({
    currentDataView,
    chartConfig,
    drillOption,
    viewDetailSetting,
    clickFilters,
  }: DisplayViewDetailProps) => {
    const builder = new ChartDataRequestBuilder(
      currentDataView!,
      filterTableColumnsByViewDetailSetting(
        chartConfig?.datas,
        viewDetailSetting,
      ),
    );
    return builder
      .addRuntimeFilters(clickFilters)
      .addDrillOption(drillOption)
      .buildDetails();
  };

  const openModal = (props: DisplayViewDetailProps) => {
    return openStateModal({
      modalSize: StateModalSize.MIDDLE,
      content: () => {
        return (
          <StyledTabs defaultActiveKey="summary">
            <TabPane tab={t('summary')} key="summary">
              <TemplateTable
                chartConfig={props?.chartConfig}
                token={props.authToken}
                requestParams={getSummaryTableRequestParams(props)}
              />
            </TabPane>
            <TabPane tab={t('details')} key="details">
              <TemplateTable
                chartConfig={props?.chartConfig}
                token={props.authToken}
                requestParams={getDetailsTableRequestParams(props)}
              />
            </TabPane>
          </StyledTabs>
        );
      },
    });
  };
  return [openModal, contextHolder] as const;
};

export default useDisplayViewDetail;

const StyledTabs = styled(Tabs)`
  .ant-tabs-nav {
    margin-bottom: ${SPACE_XS} !important;
  }
`;
