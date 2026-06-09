import { Reducer, StoreEnhancer } from '@reduxjs/toolkit';
import { InjectedReducersType } from 'utils/types/injector-typings';

type CreateReducer = (injectedReducers?: InjectedReducersType) => Reducer;

const injectReducerEnhancer = (createReducer: CreateReducer): StoreEnhancer => {
  return createStore =>
    (...args) => {
      const store = createStore(...args);

      return {
        ...store,
        createReducer,
        injectedReducers: {},
      };
    };
};

export default injectReducerEnhancer;
