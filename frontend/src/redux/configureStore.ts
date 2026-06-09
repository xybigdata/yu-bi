import { configureStore } from '@reduxjs/toolkit';
import injectReducerEnhancer from 'utils/@reduxjs/injectReducer/enhancer';
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
      }).prepend(rejectedErrorHandlerMiddleware.middleware as any),
    devTools:
      /* istanbul ignore next line */
      process.env.NODE_ENV !== 'production' ||
      process.env.PUBLIC_URL.length > 0,
    enhancers: getDefaultEnhancers =>
      getDefaultEnhancers().concat(injectReducerEnhancer(createReducer)),
  });

  return store;
}

export type AppStore = ReturnType<typeof configureAppStore>;
export type AppDispatch = AppStore['dispatch'];
export type AppState = RootState;
