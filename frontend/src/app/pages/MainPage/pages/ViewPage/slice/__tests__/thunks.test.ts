import { describe, expect, test, vi, beforeEach } from 'vitest';
import { runSql } from '../thunks';
import { ViewViewModelStages } from '../../constants';
import { generateEditingView } from '../../utils';
import { RootState } from 'types';
import { ViewViewModel } from '../types';

const requestMock = vi.hoisted(() => ({
  request2: vi.fn(),
}));

vi.mock('utils/request', () => ({
  request2: requestMock.request2,
}));

vi.mock('i18next', () => ({
  default: {
    t: (key: string) => key,
  },
}));

const createState = (overrides: Partial<ViewViewModel> = {}): RootState => ({
  view: {
    views: undefined,
    archived: undefined,
    viewListLoading: false,
    archivedListLoading: false,
    currentEditingView: 'view-1',
    sourceDatabases: {},
    sourceDatabaseSchema: {
      source_1: [
        {
          dbName: 'default',
          tables: [
            {
              primaryKeys: [],
              tableName: 'orders',
              columns: [
                {
                  fmt: '',
                  foreignKeys: [],
                  name: 'id',
                  type: 'STRING',
                },
              ],
            },
          ],
        },
      ],
    },
    saveViewLoading: false,
    unarchiveLoading: false,
    databaseSchemaLoading: false,
    editingViews: [
      generateEditingView({
        id: 'view-1',
        name: 'view',
        sourceId: 'source_1',
        size: 100,
        fragment: '',
        variables: [],
        type: 'SQL',
        script: 'select 1',
        stage: ViewViewModelStages.Initialized,
        ...overrides,
      }),
    ],
  },
});

const dispatchRunSql = async (state: ReturnType<typeof createState>) => {
  const dispatch = vi.fn();
  const getState = vi.fn(() => state);

  await runSql({ id: 'view-1', isFragment: false })(
    dispatch,
    getState,
    undefined,
  );
};

describe('runSql', () => {
  beforeEach(() => {
    requestMock.request2.mockReset();
    requestMock.request2.mockResolvedValue({
      data: {
        columns: [],
        rows: [],
        pageInfo: { pageNo: 1, pageSize: 100, total: 0 },
      },
    });
  });

  test('does not send columns field for SQL script execution', async () => {
    await dispatchRunSql(createState());

    expect(requestMock.request2).toHaveBeenCalledWith(
      expect.objectContaining({
        url: 'queries/preview',
        data: expect.not.objectContaining({
          columns: expect.anything(),
        }),
      }),
      {},
      expect.any(Object),
    );
  });

  test('uses default preview size when editing view size is invalid', async () => {
    await dispatchRunSql(
      createState({
        size: undefined as unknown as number,
      }),
    );

    expect(requestMock.request2).toHaveBeenCalledWith(
      expect.objectContaining({
        data: expect.objectContaining({
          size: 1000,
        }),
      }),
      {},
      expect.any(Object),
    );
  });

  test('sends structured columns array for STRUCT execution', async () => {
    await dispatchRunSql(
      createState({
        type: 'STRUCT',
        script: {
          table: ['orders'],
          columns: ['id'],
          joins: [],
        },
      }),
    );

    expect(requestMock.request2).toHaveBeenCalledWith(
      expect.objectContaining({
        data: expect.objectContaining({
          columns: [{ alias: 'orders.id', column: ['orders', 'id'] }],
          scriptType: 'STRUCT',
        }),
      }),
      {},
      expect.any(Object),
    );
  });
});
