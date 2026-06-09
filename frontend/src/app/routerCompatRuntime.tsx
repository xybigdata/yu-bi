import React, {
  createContext,
  FC,
  PropsWithChildren,
  useContext,
  useMemo,
} from 'react';
import { Router } from 'react-router-dom';

// react-router-dom@5 运行时实际依赖的是它自己携带的 history@4。
// 这里继续复用同一实现，但把 history 实例控制权收回到项目兼容层。
// eslint-disable-next-line @typescript-eslint/no-var-requires
const createBrowserHistory = require('react-router-dom/node_modules/history/createBrowserHistory');
// eslint-disable-next-line @typescript-eslint/no-var-requires
const createMemoryHistory = require('react-router-dom/node_modules/history/createMemoryHistory');

interface CompatHistory {
  location: {
    pathname: string;
    search: string;
    hash: string;
    state?: unknown;
    key?: string;
  };
  push: (
    to:
      | string
      | {
          pathname?: string;
          search?: string;
          hash?: string;
          state?: unknown;
          key?: string;
        },
    state?: unknown,
  ) => void;
  replace: (
    to:
      | string
      | {
          pathname?: string;
          search?: string;
          hash?: string;
          state?: unknown;
          key?: string;
        },
    state?: unknown,
  ) => void;
  go: (delta: number) => void;
  goBack: () => void;
}

interface BrowserRouterProps extends PropsWithChildren<{}> {
  basename?: string;
}

interface MemoryRouterProps extends PropsWithChildren<{}> {
  initialEntries?: Array<string | Record<string, unknown>>;
  initialIndex?: number;
  keyLength?: number;
}

const CompatHistoryContext = createContext<CompatHistory | null>(null);

const toCompatHistory = (history: any): CompatHistory => ({
  location: history.location,
  push: history.push.bind(history),
  replace: history.replace.bind(history),
  go: history.go.bind(history),
  goBack: history.goBack
    ? history.goBack.bind(history)
    : () => history.go(-1),
});

export const BrowserRouter: FC<BrowserRouterProps> = ({
  basename,
  children,
}) => {
  const history = useMemo(
    () => createBrowserHistory({ basename }),
    [basename],
  );
  const compatHistory = useMemo(() => toCompatHistory(history), [history]);

  return (
    <CompatHistoryContext.Provider value={compatHistory}>
      <Router history={history}>{children}</Router>
    </CompatHistoryContext.Provider>
  );
};

export const MemoryRouter: FC<MemoryRouterProps> = ({
  children,
  initialEntries,
  initialIndex,
  keyLength,
}) => {
  const history = useMemo(
    () =>
      createMemoryHistory({
        initialEntries,
        initialIndex,
        keyLength,
      }),
    [initialEntries, initialIndex, keyLength],
  );
  const compatHistory = useMemo(() => toCompatHistory(history), [history]);

  return (
    <CompatHistoryContext.Provider value={compatHistory}>
      <Router history={history}>{children}</Router>
    </CompatHistoryContext.Provider>
  );
};

export const useCompatHistory = () => {
  const history = useContext(CompatHistoryContext);

  if (!history) {
    throw new Error('useCompatHistory 必须在兼容 Router 上下文内使用');
  }

  return history;
};
