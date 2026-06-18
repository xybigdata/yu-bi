import { AnyAction, Reducer } from '@reduxjs/toolkit';
import { useAppStore } from 'app/hooks/useRedux';
import hoistNonReactStatics from 'hoist-non-react-statics';
import React from 'react';
import { ReactReduxContext } from 'react-redux';
import type { AppStore } from 'redux/configureStore';
import getInjectors from './reducerInjectors';

interface InjectReducerParams {
  key: string;
  reducer: Reducer<any, AnyAction>;
}

/**
 * A higher-order component that dynamically injects a reducer when the
 * component is instantiated
 *
 * @param {Object} params
 * @param {string} params.key The key to inject the reducer under
 * @param {function} params.reducer The reducer that will be injected
 *
 * @example
 *
 * class BooksManager extends React.PureComponent {
 *   render() {
 *     return null;
 *   }
 * }
 *
 * export default injectReducer({ key: "books", reducer: booksReducer })(BooksManager)
 *
 * @public
 */
const injectReducer =
  ({ key, reducer }: InjectReducerParams) =>
  <Props extends object>(WrappedComponent: React.ComponentType<Props>) => {
    class ReducerInjector extends React.Component<Props> {
      static WrappedComponent = WrappedComponent;

      static contextType = ReactReduxContext;

      static displayName = `withReducer(${
        WrappedComponent.displayName || WrappedComponent.name || 'Component'
      })`;

      constructor(props: Props, context) {
        super(props, context);

        getInjectors(context.store as AppStore).injectReducer(key, reducer);
      }

      render() {
        return <WrappedComponent {...this.props} />;
      }
    }

    return hoistNonReactStatics(ReducerInjector, WrappedComponent);
  };

export default injectReducer;

/**
 * A react hook that dynamically injects a reducer when the hook is run
 *
 * @param {Object} params
 * @param {string} params.key The key to inject the reducer under
 * @param {function} params.reducer The reducer that will be injected
 *
 * @example
 *
 * function BooksManager() {
 *   useInjectReducer({ key: "books", reducer: booksReducer })
 *
 *   return null;
 * }
 *
 * @public
 */
export const useInjectReducer = ({
  key,
  reducer,
}: InjectReducerParams) => {
  const store = useAppStore();

  const isInjected = React.useRef(false);

  if (!isInjected.current) {
    getInjectors(store).injectReducer(key, reducer);
    isInjected.current = true;
  }
};
