import { Reducer, UnknownAction } from '@reduxjs/toolkit';
import { ThemeState } from 'styles/theme/slice/types';
import { InjectedReducersType } from 'utils/types/injector-typings';
import { createReducer } from '../reducers';

describe('reducer', () => {
  it('should inject reducers', () => {
    const themeState: ThemeState = { selected: 'system' };
    const dummyReducer: InjectedReducersType['theme'] = () => themeState;
    const reducer = createReducer({ theme: dummyReducer });
    const state = reducer({}, { type: 'TEST' });
    expect(state.theme).toBe(themeState);
  });

  it('should return identity reducers when empty', () => {
    const reducer = createReducer() as Reducer<
      Record<string, number>,
      UnknownAction
    >;
    const state = { a: 1 };
    const newState = reducer(state, { type: 'TEST' });
    expect(newState).toBe(state);
  });
});
