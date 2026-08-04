/**
 * YuBi
 *
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
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
import { executePublicQuery, executeQuery } from 'app/features/query';
import type {
  ShareLinkCreateRequest,
  ShareLinkCreateResult,
} from 'app/components/VizOperationMenu/components/slice/type';
import { DownloadFileType } from 'app/constants';
import {
  submitAuthenticatedArtifactTask,
  sharedArtifactAccess,
  submitSharedArtifactTask,
  type ArtifactTaskWebResponse,
} from 'app/features/artifact';
import { ExecuteToken } from 'app/pages/SharePage/slice/types';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { ChartDataRequest } from 'app/features/query';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import { ChartDTO } from 'app/types/ChartDTO';
import {
  filterSqlOperatorName,
  transformToViewConfig,
} from 'app/utils/internalChartHelper';
import { BASE_RESOURCE_URL } from 'globalConstants';
import { stringifyQuery } from 'utils/queryString';
import { request2 } from 'utils/request';
import { convertToChartDto } from './ChartDtoHelper';
import { getAllColumnInMeta } from './chartHelper';

export const getDistinctFields = async (
  viewId: string,
  columns: string[],
  view: ChartDTO['view'] | undefined,
  executeToken: Record<string, ExecuteToken> | undefined,
) => {
  const viewConfigs = transformToViewConfig(view?.config);
  const _columns = [...new Set(columns)];
  const requestParams: ChartDataRequest = {
    aggregators: [],
    filters: [],
    groups: [],
    functionColumns:
      view?.computedFields
        ?.filter(v => _columns.includes(v.name))
        ?.map(field => {
          return {
            alias: field?.name || '',
            snippet: field?.expression || '',
          };
        }) || [],
    columns: _columns.map(columnName => {
      const row = getAllColumnInMeta(view?.meta)?.find(
        v => v.name === columnName,
      );
      return {
        alias: columnName,
        column: row?.path || [columnName],
      };
    }),
    pageInfo: {
      pageNo: 1,
      pageSize: 99999999,
      total: 99999999,
    },
    orders: [],
    keywords: ['DISTINCT'],
    viewId,
    ...viewConfigs,
  };
  if (executeToken) {
    const data = await executePublicQuery<ChartDataSetDTO>(
      requestParams,
      executeToken[viewId].authorizedToken,
    );
    return filterSqlOperatorName(requestParams, data);
  } else {
    const data = await executeQuery<ChartDataSetDTO>(requestParams);
    return filterSqlOperatorName(requestParams, data);
  }
};

export const makeDownloadDataTask =
  (params: {
    downloadParams: ChartDataRequest[];
    fileName: string;
    downloadType: DownloadFileType;
    imageWidth?: number;
  }) =>
  async (_dispatch, getState) => {
    const { downloadParams, fileName, downloadType, imageWidth } = params;
    const orgId = selectOrgId(getState());
    const normalizedImageWidth =
      typeof imageWidth === 'number' && imageWidth > 0 ? imageWidth : 1920;
    await submitAuthenticatedArtifactTask(async () => {
      const response = await request2<ArtifactTaskWebResponse>({
        url: `download/submit/task`,
        method: 'POST',
        data: {
          downloadParams,
          fileName,
          downloadType,
          orgId,
          imageWidth: normalizedImageWidth,
        },
      });
      return response.data;
    }, orgId);
  };
export const makeShareDownloadDataTask =
  (params: {
    clientId: string;
    fileName: string;
    downloadParams: ChartDataRequest[];
    shareToken: string;
    executeToken?: Record<string, ExecuteToken>;
    password?: string | null;
  }) =>
  async () => {
    const {
      downloadParams,
      fileName,
      executeToken,
      clientId,
      password,
      shareToken,
    } = params;
    const access = sharedArtifactAccess(
      shareToken,
      clientId,
      password ?? undefined,
    );
    await submitSharedArtifactTask(async () => {
      const response = await request2<ArtifactTaskWebResponse>({
        url: `shares/download`,
        method: 'POST',
        data: {
          downloadParams,
          fileName,
          executeToken,
          shareToken,
        },
        params: {
          password,
          clientId,
        },
      });
      return response.data;
    }, access);
  };

export async function checkComputedFieldAsync(
  sourceId: string | undefined,
  expression: string | undefined,
): Promise<boolean> {
  const response = await request2<boolean>({
    method: 'POST',
    url: `data-provider/function/validate`,
    params: {
      sourceId,
      snippet: expression,
    },
    paramsSerializer: function (params) {
      return stringifyQuery(params, { arrayFormat: 'brackets' });
    },
  });
  return !!response;
}

export async function fetchAvailableSourceFunctionsAsync(
  sourceId: string,
): Promise<string[]> {
  const response = await request2<string[]>({
    method: 'POST',
    url: `data-provider/function/support/${sourceId}`,
  });
  return response?.data;
}

export async function fetchAvailableSourceFunctionsAsyncForShare(
  sourceId: string,
  executeToken: string,
): Promise<string[]> {
  const response = await request2<string[]>({
    method: 'POST',
    url: `shares/function/support/${sourceId}`,
    data: {
      authorizedToken: executeToken,
    },
  });
  return response?.data;
}

export async function generateShareLinkAsync({
  expiryDate,
  vizId,
  vizType,
  authenticationMode,
  roles,
  users,
  rowPermissionBy,
}: ShareLinkCreateRequest): Promise<ShareLinkCreateResult> {
  const response = await request2<ShareLinkCreateResult>({
    method: 'POST',
    url: `shares`,
    data: {
      expiryDate: expiryDate,
      authenticationMode,
      roles,
      users,
      rowPermissionBy,
      vizId: vizId,
      vizType,
    },
  });
  return response?.data;
}

export async function fetchPluginChart(path: string): Promise<string> {
  const result = await request2<string>(path, {
    baseURL: BASE_RESOURCE_URL,
    headers: { Accept: 'application/javascript' },
  }).catch(error => {
    console.error(error);
  });
  return result?.data || '';
}

export async function getChartPluginPaths() {
  const response = await request2<string[]>({
    method: 'GET',
    url: `plugins/custom/charts`,
  });
  return response?.data || [];
}

export async function fetchCheckName(url, data: unknown) {
  return await request2({
    url: `/${url}/check/name`,
    method: 'POST',
    data: data,
  });
}

export async function fetchDataChart(id: string) {
  const response = await request2<ChartDTO>(`/viz/datacharts/${id}`);
  return convertToChartDto(response?.data);
}

export async function fetchChartDataSet(
  requestParams: ChartDataRequest,
  authorizedToken?: ExecuteToken,
): Promise<ChartDataSetDTO> {
  if (authorizedToken) {
    return executePublicQuery<ChartDataSetDTO>(
      requestParams,
      authorizedToken.authorizedToken,
    );
  }

  return executeQuery<ChartDataSetDTO>(requestParams);
}

export async function fetchDashboardDetail(boardId: string) {
  const { data } = await request2(`/viz/dashboards/${boardId}`);
  return data;
}
