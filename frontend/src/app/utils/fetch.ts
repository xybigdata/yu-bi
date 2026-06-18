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
import { message } from 'antd';
import type { AxiosResponse } from 'axios';
import type {
  ShareLinkCreateRequest,
  ShareLinkCreateResult,
} from 'app/components/VizOperationMenu/components/slice/type';
import { DownloadFileType } from 'app/constants';
import {
  DownloadTask,
  DownloadTaskState,
} from 'app/pages/MainPage/slice/types';
import { ExecuteToken } from 'app/pages/SharePage/slice/types';
import { ChartDataRequest } from 'app/types/ChartDataRequest';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import { ChartDTO } from 'app/types/ChartDTO';
import {
  filterSqlOperatorName,
  transformToViewConfig,
} from 'app/utils/internalChartHelper';
import { BASE_RESOURCE_URL } from 'globalConstants';
import i18next from 'i18next';
import { stringifyQuery } from 'utils/queryString';
import { request2, requestWithHeader } from 'utils/request';
import { convertToChartDto } from './ChartDtoHelper';
import { getAllColumnInMeta } from './chartHelper';

type ShareTaskParams = {
  clientId?: string;
  password?: string | null;
  shareToken?: string;
};

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
    const { data } = await request2<ChartDataSetDTO>({
      method: 'POST',
      url: `shares/execute`,
      params: {
        executeToken: executeToken[viewId].authorizedToken,
      },
      data: requestParams,
    });
    return filterSqlOperatorName(requestParams, data);
  } else {
    const response = await request2<ChartDataSetDTO>({
      method: 'POST',
      url: `data-provider/execute`,
      data: requestParams,
    });
    return filterSqlOperatorName(requestParams, response?.data);
  }
};

export const makeDownloadDataTask =
  (params: {
    downloadParams: ChartDataRequest[];
    fileName: string;
    downloadType: DownloadFileType;
    imageWidth?: number;
    resolve: () => void;
  }) =>
  async () => {
    const { downloadParams, fileName, resolve, downloadType, imageWidth } =
      params;
    const res = await request2<null>({
      url: `download/submit/task`,
      method: 'POST',
      data: {
        downloadParams: downloadParams,
        fileName: fileName,
        downloadType,
        imageWidth,
      },
    });
    if (res?.success) {
      message.success(String(i18next.t('viz.action.downloadTaskSuccess')));
    }
    resolve();
  };
// TODO
export const makeShareDownloadDataTask =
  (params: {
    resolve: () => void;
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
      resolve,
      executeToken,
      clientId,
      password,
      shareToken,
    } = params;
    const { success } = await request2<null>({
      url: `shares/download`,
      method: 'POST',
      data: {
        downloadParams,
        fileName: fileName,
        executeToken,
        shareToken,
      },
      params: {
        password,
        clientId,
      },
    });
    if (success) {
      message.success(String(i18next.t('viz.action.downloadTaskSuccess')));
    }
    resolve();
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

function getDownloadFileName(contentDisposition: string | undefined) {
  if (!contentDisposition) {
    return 'unknown.xlsx';
  }

  const encodedMatch = /filename\*\s*=\s*([^;]+)/i.exec(contentDisposition);

  if (encodedMatch?.[1]) {
    const encodedFileName = encodedMatch[1].trim().replace(/^UTF-8''/i, '');
    return decodeURIComponent(encodedFileName).split('"').join('');
  }

  const normalMatch = /filename\s*=\s*((['"]).*?\2|[^;\n]*)/i.exec(
    contentDisposition,
  );

  if (normalMatch?.[1]) {
    return decodeURIComponent(normalMatch[1]).split('"').join('');
  }

  return 'unknown.xlsx';
}

export const dealFileSave = (
  data: BlobPart,
  headers: AxiosResponse['headers'],
) => {
  const fileName =
    getDownloadFileName(String(headers?.['content-disposition'] || '')) ||
    'unknown.xlsx';
  const blob = new Blob([data], { type: '**application/octet-stream**' });
  const downloadUrl = URL.createObjectURL(blob);
  const anchor = document.createElement('a');

  anchor.href = downloadUrl;
  anchor.download = fileName;
  anchor.style.display = 'none';

  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(downloadUrl);
};

export async function downloadFile(id: string): Promise<void> {
  const [data, headers] = await requestWithHeader<BlobPart>({
    url: `download/files/${id}`,
    method: 'GET',
    responseType: 'blob',
  });
  dealFileSave(data, headers);
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

export async function loadShareTask(params: ShareTaskParams): Promise<{
  isNeedStopPolling: boolean;
  data: DownloadTask[];
}> {
  const { data } = await request2<DownloadTask[]>({
    url: `/shares/download/task`,
    method: 'GET',
    params,
  });
  const isNeedStopPolling = !(data || []).some(
    v => v.status === DownloadTaskState.CREATED,
  );
  return {
    isNeedStopPolling,
    data: data || [],
  };
}
interface DownloadShareDashChartFileParams {
  downloadId: string;
  shareToken: string;
  password?: string | null;
}
export async function downloadShareDataChartFile(
  params: DownloadShareDashChartFileParams,
) {
  const [data, headers] = await requestWithHeader<BlobPart>({
    url: `shares/download`,
    method: 'GET',
    responseType: 'blob',
    params,
  });
  dealFileSave(data, headers);
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
    const { data } = await request2<ChartDataSetDTO>({
      method: 'POST',
      url: `shares/execute`,
      params: {
        executeToken: authorizedToken,
      },
      data: requestParams,
    });
    return data;
  }

  const { data } = await request2<ChartDataSetDTO>({
    method: 'POST',
    url: `data-provider/execute`,
    data: requestParams,
  });
  return data;
}

export async function fetchDashboardDetail(boardId: string) {
  const { data } = await request2(`/viz/dashboards/${boardId}`);
  return data;
}
