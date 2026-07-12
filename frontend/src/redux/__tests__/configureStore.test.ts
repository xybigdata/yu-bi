import {
  AGENT_WORKSPACE_DEVTOOLS_REDACTED,
  configureAppStore,
  reduxDevToolsOptions,
  sanitizeReduxDevToolsAction,
  sanitizeReduxDevToolsState,
} from '../configureStore';
import { vi } from 'vitest';

describe('configureStore', () => {
  it('should return a store with injected enhancers', () => {
    const store = configureAppStore();
    expect(store).toEqual(
      expect.objectContaining({
        injectedReducers: expect.any(Object),
      }),
    );
  });

  it('should return an empty store', () => {
    const store = configureAppStore();
    expect(store.getState()).toBeUndefined();
  });

  it('应在 DevTools 中完整脱敏 Agent action 参数与结果', () => {
    const sensitiveValues = [
      'prompt-secret-7c7b',
      'session-secret-0d6a',
      'approval-secret-3eb1',
      'idempotency-secret-9a12',
      'chart-name-secret-b6d9',
      'dashboard-name-secret-1b73',
    ];
    const actions = [
      {
        type: 'agentWorkspace/runReadOnly/pending',
        meta: {
          arg: { orgId: 'org-1', message: sensitiveValues[0] },
          sessionId: sensitiveValues[1],
        },
      },
      {
        type: 'agentWorkspace/previewWrite/pending',
        meta: {
          arg: {
            orgId: 'org-1',
            idempotencyKey: sensitiveValues[3],
            request: {
              toolName: 'create_chart',
              arguments: {
                name: sensitiveValues[4],
                viewId: 'view-secret',
              },
            },
          },
        },
      },
      {
        type: 'agentWorkspace/decideApproval/fulfilled',
        meta: {
          arg: {
            orgId: 'org-1',
            approvalId: sensitiveValues[2],
            decision: 'approve',
          },
        },
        payload: {
          data: {
            sessionId: sensitiveValues[1],
            approvalId: sensitiveValues[2],
            preview: {
              parameters: [{ value: sensitiveValues[5] }],
            },
          },
        },
      },
    ];

    const projection = actions.map(action =>
      sanitizeReduxDevToolsAction(action),
    );
    expect(projection).toEqual(actions.map(action => ({ type: action.type })));
    sensitiveValues.forEach(value => {
      expect(JSON.stringify(projection)).not.toContain(value);
    });
    expect(actions[0].meta.arg.message).toBe(sensitiveValues[0]);
    expect(reduxDevToolsOptions.actionSanitizer).toBe(
      sanitizeReduxDevToolsAction,
    );
  });

  it('应在 DevTools 中隐藏整个 Agent 状态且保留其他状态投影', () => {
    const sensitiveValues = [
      'session-state-secret-f129',
      'approval-state-secret-51a6',
      'idempotency-state-secret-aaf2',
      'write-argument-state-secret-4c4f',
    ];
    const state = {
      agentWorkspace: {
        session: { sessionId: sensitiveValues[0] },
        approvals: [{ approvalId: sensitiveValues[1] }],
        previewIntent: {
          idempotencyKey: sensitiveValues[2],
          request: {
            toolName: 'rename_dashboard',
            arguments: { newName: sensitiveValues[3] },
          },
        },
      },
      theme: { mode: 'dark' },
    };

    const projection = sanitizeReduxDevToolsState(state);
    expect(projection).toEqual({
      agentWorkspace: AGENT_WORKSPACE_DEVTOOLS_REDACTED,
      theme: state.theme,
    });
    sensitiveValues.forEach(value => {
      expect(JSON.stringify(projection)).not.toContain(value);
    });
    expect(state.agentWorkspace.session.sessionId).toBe(sensitiveValues[0]);
    expect(reduxDevToolsOptions.stateSanitizer).toBe(
      sanitizeReduxDevToolsState,
    );
  });

  it('非 Agent action 和不含 Agent 的状态保持原投影', () => {
    const action = { type: 'theme/change', payload: { mode: 'dark' } };
    const state = { theme: { mode: 'dark' } };

    expect(sanitizeReduxDevToolsAction(action)).toBe(action);
    expect(sanitizeReduxDevToolsState(state)).toBe(state);
  });

  it('全局拒绝日志只记录 action 类型且不泄露 Agent 参数', async () => {
    const sensitiveValues = [
      'prompt-log-secret-3a7f',
      'session-log-secret-c821',
      'approval-log-secret-912d',
      'idempotency-log-secret-637a',
      'write-argument-log-secret-f152',
    ];
    const consoleError = vi
      .spyOn(console, 'error')
      .mockImplementation(() => undefined);

    try {
      const store = configureAppStore();
      store.dispatch({
        type: 'agentWorkspace/previewWrite/rejected',
        error: {},
        meta: {
          requestId: 'rejected-request-1',
          requestStatus: 'rejected',
          arg: {
            message: sensitiveValues[0],
            sessionId: sensitiveValues[1],
            approvalId: sensitiveValues[2],
            idempotencyKey: sensitiveValues[3],
            request: { arguments: { name: sensitiveValues[4] } },
          },
        },
      });

      await vi.waitFor(() => expect(consoleError).toHaveBeenCalled());
      expect(consoleError).toHaveBeenCalledWith(
        'Redux 异步操作失败',
        'agentWorkspace/previewWrite/rejected',
      );
      sensitiveValues.forEach(value => {
        expect(JSON.stringify(consoleError.mock.calls)).not.toContain(value);
      });
    } finally {
      consoleError.mockRestore();
    }
  });
});
