import { configureStore } from '@reduxjs/toolkit';
import { InjectedReducersType } from 'utils/types/injector-typings';
import rejectedErrorHandlerMiddleware from '../utils/@reduxjs/rejectedErrorHandlerMiddleware';
import { RootState } from '../types';
import { createReducer } from './reducers';

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
      process.env.NODE_ENV !== 'production' ||
      process.env.PUBLIC_URL.length > 0,
  });

  return Object.assign(store, {
    createReducer,
    injectedReducers: {} as InjectedReducersType,
  });
}

export type AppStore = ReturnType<typeof configureAppStore>;
export type AppDispatch = AppStore['dispatch'];
export type AppState = RootState;
