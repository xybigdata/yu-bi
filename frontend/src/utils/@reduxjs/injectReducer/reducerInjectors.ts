import isEmpty from 'lodash/isEmpty';
import isFunction from 'lodash/isFunction';
import isString from 'lodash/isString';
import { AnyAction, Reducer } from '@reduxjs/toolkit';
import assertInvariant from 'utils/assertInvariant';
import { InjectedReducersType } from 'utils/types/injector-typings';
import checkStore from './checkStore';

type InjectedReducer = Reducer<any, AnyAction>;

interface ReducerInjectStore {
  injectedReducers: InjectedReducersType;
  createReducer: (injectedReducers?: InjectedReducersType) => Reducer;
  replaceReducer: (nextReducer: Reducer) => void;
}

export function injectReducerFactory(
  store: ReducerInjectStore,
  isValid?: boolean,
) {
  return function injectReducer(
    key: string,
    reducer: InjectedReducer,
  ) {
    if (!isValid) checkStore(store);

    assertInvariant(
      isString(key) && !isEmpty(key) && isFunction(reducer),
      '(redux-injectors...) injectReducer: Expected `reducer` to be a reducer function',
    );

    // Check `store.injectedReducers[key] === reducer` for hot reloading when a key is the same but a reducer is different
    if (
      Reflect.has(store.injectedReducers, key) &&
      store.injectedReducers[key] === reducer
    )
      return;

    store.injectedReducers[key] =
      reducer as InjectedReducersType[keyof InjectedReducersType];
    store.replaceReducer(store.createReducer(store.injectedReducers));
  };
}

export default function getInjectors(store: ReducerInjectStore) {
  checkStore(store);

  return {
    injectReducer: injectReducerFactory(store, true),
  };
}
