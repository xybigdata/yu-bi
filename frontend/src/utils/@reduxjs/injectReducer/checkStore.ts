import conformsTo from 'lodash/conformsTo';
import isFunction from 'lodash/isFunction';
import isObject from 'lodash/isObject';
import assertInvariant from 'utils/assertInvariant';

/**
 * Validates the redux store is set up properly to work with this library.
 */
export default function checkStore(store: unknown) {
  const shape = {
    dispatch: isFunction,
    subscribe: isFunction,
    getState: isFunction,
    replaceReducer: isFunction,
    createReducer: isFunction,
    injectedReducers: isObject,
  };
  assertInvariant(
    conformsTo(store as object, shape),
    '(redux-injectors...) checkStore: Expected a redux store that has been configured for use with redux-injectors.',
  );
}
