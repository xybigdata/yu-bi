import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { getToken, setToken } from '../auth';
import { instance, request2, requestWithHeader } from '../request';

const originalAdapter = instance.defaults.adapter;

function createAdapter(
  handler: (config: InternalAxiosRequestConfig) => AxiosResponse | Promise<AxiosResponse>,
): AxiosAdapter {
  return async config => handler(config as InternalAxiosRequestConfig);
}

function createResponse(
  config: InternalAxiosRequestConfig,
  response: Partial<AxiosResponse>,
): AxiosResponse {
  return {
    config,
    data: undefined,
    headers: {},
    status: 200,
    statusText: 'OK',
    ...response,
  };
}

describe('request utils', () => {
  beforeEach(() => {
    document.cookie =
      'AUTHORIZATION_TOKEN=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
  });

  afterEach(() => {
    instance.defaults.adapter = originalAdapter;
    vi.restoreAllMocks();
  });

  test('should attach authorization token in request interceptor', async () => {
    let authorization: unknown;
    setToken('token-1');
    instance.defaults.adapter = createAdapter(config => {
      authorization = config.headers.Authorization;
      return createResponse(config, {
        data: { data: { id: 1 }, errCode: 0, success: true },
      });
    });

    await request2('/demo');

    expect(authorization).toBe('token-1');
  });

  test('should update authorization token from response interceptor', async () => {
    instance.defaults.adapter = createAdapter(config =>
      createResponse(config, {
        data: { data: null, errCode: 0, success: true },
        headers: { authorization: 'token-2' },
      }),
    );

    await request2('/demo');

    expect(getToken()).toBe('token-2');
  });

  test('should unwrap default api response body', async () => {
    const body = { data: { id: 1 }, errCode: 0, success: true };
    instance.defaults.adapter = createAdapter(config =>
      createResponse(config, { data: body }),
    );

    await expect(request2('/demo')).resolves.toBe(body);
  });

  test('should return response data with headers', async () => {
    const body = { id: 1 };
    const headers = { trace: 'trace-1' };
    instance.defaults.adapter = createAdapter(config =>
      createResponse(config, { data: body, headers }),
    );

    const [data, responseHeaders] = await requestWithHeader('/demo');

    expect(data).toBe(body);
    expect(responseHeaders.trace).toBe(headers.trace);
  });

  test('should transform backend error message', async () => {
    vi.spyOn(console, 'log').mockImplementation(() => undefined);
    instance.defaults.adapter = createAdapter(() =>
      Promise.reject({
        response: {
          data: {
            message: 'backend failed',
          },
        },
      }),
    );

    await expect(request2('/demo')).rejects.toBe('backend failed');
  });
});
