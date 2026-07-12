import { configureStore, type Action } from '@reduxjs/toolkit';
import { InjectedReducersType } from 'utils/types/injector-typings';
import rejectedErrorHandlerMiddleware from '../utils/@reduxjs/rejectedErrorHandlerMiddleware';
import { RootState } from '../types';
import { createReducer } from './reducers';

const AGENT_WORKSPACE_ACTION_PREFIX = 'agentWorkspace/';
export const AGENT_WORKSPACE_DEVTOOLS_REDACTED = '[Agent 工作区内容已脱敏]';

export const sanitizeReduxDevToolsAction = <A extends Action>(action: A): A => {
  if (!action.type.startsWith(AGENT_WORKSPACE_ACTION_PREFIX)) {
    return action;
  }
  return { type: action.type } as unknown as A;
};

export const sanitizeReduxDevToolsState = <S>(state: S): S => {
  if (!state || typeof state !== 'object' || Array.isArray(state)) {
    return state;
  }
  const rootState = state as Record<string, unknown>;
  if (!Object.prototype.hasOwnProperty.call(rootState, 'agentWorkspace')) {
    return state;
  }
  return {
    ...rootState,
    agentWorkspace: AGENT_WORKSPACE_DEVTOOLS_REDACTED,
  } as S;
};

export const reduxDevToolsOptions = {
  actionSanitizer: sanitizeReduxDevToolsAction,
  stateSanitizer: sanitizeReduxDevToolsState,
};

export function configureAppStore() {
  const store = configureStore({
    reducer: createReducer(),
    middleware: getDefaultMiddleware =>
      getDefaultMiddleware({
        serializableCheck: false,
        // immutableCheck: false,
      }).prepend(rejectedErrorHandlerMiddleware.middleware),
    devTools:
      /* istanbul ignore next line */
      process.env.NODE_ENV !== 'production' || process.env.PUBLIC_URL.length > 0
        ? reduxDevToolsOptions
        : false,
  });

  return Object.assign(store, {
    createReducer,
    injectedReducers: {} as InjectedReducersType,
  });
}

export type AppStore = ReturnType<typeof configureAppStore>;
export type AppDispatch = AppStore['dispatch'];
export type AppState = RootState;
