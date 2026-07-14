import { beforeEach, describe, expect, test, vi } from 'vitest';
import {
  executePublicQuery,
  executeQuery,
  previewQuery,
  SHARE_TOKEN_HEADER,
} from '../client';
import type { ChartDataRequest, PreviewQueryRequest } from '../types';

const requestMock = vi.hoisted(() => ({
  request2: vi.fn(),
}));

vi.mock('utils/request', () => ({
  request2: requestMock.request2,
}));

const executeRequest: ChartDataRequest = {
  viewId: 'view-1',
  columns: [{ alias: 'amount', column: ['orders', 'amount'] }],
  aggregators: [],
  filters: [],
  groups: [],
  orders: [],
  concurrencyControlMode: 'DIRTYREAD',
};

describe('query client', () => {
  beforeEach(() => {
    requestMock.request2.mockReset();
    requestMock.request2.mockResolvedValue({
      success: true,
      errCode: 0,
      data: { id: 'df-1', columns: [], rows: [] },
    });
  });

  test('executes authenticated queries through the new contract', async () => {
    await executeQuery(executeRequest);

    expect(requestMock.request2).toHaveBeenCalledWith(
      {
        method: 'POST',
        url: 'queries/execute',
        data: executeRequest,
      },
      {},
      undefined,
    );
  });

  test('sends public token only in the dedicated header', async () => {
    const shareToken = 'secret-execute-token';

    await executePublicQuery(executeRequest, shareToken);

    const [config] = requestMock.request2.mock.calls[0];
    expect(config).toEqual({
      method: 'POST',
      url: 'public/queries/execute',
      headers: { [SHARE_TOKEN_HEADER]: shareToken },
      data: executeRequest,
    });
    expect(config.url).not.toContain(shareToken);
    expect(config.params).toBeUndefined();
    expect(JSON.stringify(config.data)).not.toContain(shareToken);
  });

  test('previews through the feature client and preserves response metadata', async () => {
    const request: PreviewQueryRequest = {
      sourceId: 'source-1',
      script: 'select 1',
      scriptType: 'SQL',
      variables: [],
    };
    requestMock.request2.mockResolvedValue({
      success: true,
      errCode: 0,
      warnings: ['warning'],
      data: { columns: [], rows: [] },
    });

    const response = await previewQuery(request);

    expect(response.warnings).toEqual(['warning']);
    expect(requestMock.request2).toHaveBeenCalledWith(
      { method: 'POST', url: 'queries/preview', data: request },
      {},
      undefined,
    );
  });
});
