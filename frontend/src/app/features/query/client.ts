import type { AxiosResponse } from 'axios';
import type { APIResponse } from 'types';
import { request2 } from 'utils/request';
import type {
  ChartDataRequest,
  PreviewQueryRequest,
  QueryResult,
} from './types';

export const SHARE_TOKEN_HEADER = 'X-YuBi-Share-Token';

export type QueryClientOptions<T> = {
  onFulfilled?: (value: AxiosResponse<unknown>) => APIResponse<T>;
  onRejected?: (error: unknown) => unknown;
};

export async function executeQuery<T = QueryResult>(
  request: ChartDataRequest,
  options?: QueryClientOptions<T>,
): Promise<T> {
  const response = await request2<T>(
    { method: 'POST', url: 'queries/execute', data: request },
    {},
    options,
  );
  return response?.data;
}

export async function executePublicQuery<T = QueryResult>(
  request: ChartDataRequest,
  shareToken: string,
  options?: QueryClientOptions<T>,
): Promise<T> {
  const response = await request2<T>(
    {
      method: 'POST',
      url: 'public/queries/execute',
      headers: { [SHARE_TOKEN_HEADER]: shareToken },
      data: request,
    },
    {},
    options,
  );
  return response?.data;
}

export async function previewQuery<T = QueryResult>(
  request: PreviewQueryRequest,
  options?: QueryClientOptions<T>,
): Promise<APIResponse<T>> {
  return request2<T>(
    { method: 'POST', url: 'queries/preview', data: request },
    {},
    options,
  );
}
