import {
  dealFileSave,
  loadShareTask,
  makeShareDownloadDataTask,
} from 'app/utils/fetch';
import { request2 } from 'utils/request';

vi.mock('utils/request', () => ({
  request2: vi.fn(),
  requestWithHeader: vi.fn(),
}));

beforeEach(() => {
  vi.mocked(request2).mockReset();
});

describe('dealFileSave', () => {
  it('should save blob with decoded filename and cleanup object url', () => {
    const createObjectURLSpy = vi
      .spyOn(URL, 'createObjectURL')
      .mockReturnValue('blob:test-url');
    const revokeObjectURLSpy = vi
      .spyOn(URL, 'revokeObjectURL')
      .mockImplementation(() => undefined);
    const appendChildSpy = vi.spyOn(document.body, 'appendChild');
    const removeSpy = vi.spyOn(HTMLAnchorElement.prototype, 'remove');
    const clickSpy = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);

    dealFileSave(new Uint8Array([1, 2, 3]), {
      'content-disposition': "attachment; filename*=UTF-8''report%20name.xlsx",
    });

    expect(createObjectURLSpy).toHaveBeenCalledTimes(1);
    expect(appendChildSpy).toHaveBeenCalledTimes(1);

    const anchor = appendChildSpy.mock.calls[0][0] as HTMLAnchorElement;
    expect(anchor.href).toContain('blob:test-url');
    expect(anchor.download).toBe('report name.xlsx');

    expect(clickSpy).toHaveBeenCalledTimes(1);
    expect(removeSpy).toHaveBeenCalledTimes(1);
    expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:test-url');

    createObjectURLSpy.mockRestore();
    revokeObjectURLSpy.mockRestore();
    appendChildSpy.mockRestore();
    removeSpy.mockRestore();
    clickSpy.mockRestore();
  });
});

describe('share download requests', () => {
  const expectTrustedShareTaskRequest = () => {
    expect(request2).toHaveBeenCalledTimes(1);
    expect(request2).toHaveBeenCalledWith({
      url: 'shares/share-1/download',
      method: 'POST',
      data: {
        downloadParams: [],
        fileName: 'orders',
        executeToken: {},
      },
    });

    const requestConfig = vi.mocked(request2).mock.calls[0]?.[0];
    expect(requestConfig).not.toHaveProperty('params');
    expect(requestConfig).not.toHaveProperty('data.clientId');
    expect(requestConfig).not.toHaveProperty('data.password');
    expect(requestConfig).not.toHaveProperty('data.shareToken');
  };

  it('should resolve after a successful trusted share task request', async () => {
    vi.mocked(request2).mockResolvedValue({ success: true } as never);
    const resolve = vi.fn();

    await makeShareDownloadDataTask({
      shareId: 'share-1',
      fileName: 'orders',
      downloadParams: [],
      executeToken: {},
      resolve,
    })();

    expectTrustedShareTaskRequest();
    expect(resolve).toHaveBeenCalledTimes(1);
  });

  it('should not resolve when the trusted share task request fails', async () => {
    const requestError = new Error('share task request failed');
    vi.mocked(request2).mockRejectedValue(requestError);
    const resolve = vi.fn();

    await expect(
      makeShareDownloadDataTask({
        shareId: 'share-1',
        fileName: 'orders',
        downloadParams: [],
        executeToken: {},
        resolve,
      })(),
    ).rejects.toBe(requestError);

    expectTrustedShareTaskRequest();
    expect(resolve).not.toHaveBeenCalled();
  });

  it('should list tasks from the same top-level share route', async () => {
    vi.mocked(request2).mockResolvedValue({ data: [] } as never);

    await loadShareTask('share-1');

    expect(request2).toHaveBeenCalledWith({
      url: '/shares/share-1/download/tasks',
      method: 'GET',
    });
  });
});
