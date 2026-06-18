import { LoadingOutlined } from '@ant-design/icons';
import React, { lazy, Suspense } from 'react';

interface Opts {
  fallback: React.ReactNode;
}
type LoadableComponent = React.ComponentType<any>;
type LoadableFactory<C extends LoadableComponent> = () => Promise<{
  default: C;
}>;

export function lazyLoad<
  TModule extends { default: LoadableComponent },
>(
  importFunc: () => Promise<TModule>,
  selectorFunc?: undefined,
  opts?: Opts,
): (props: React.ComponentProps<TModule['default']>) => JSX.Element;
export function lazyLoad<TModule, C extends LoadableComponent>(
  importFunc: () => Promise<TModule>,
  selectorFunc: (s: TModule) => C,
  opts?: Opts,
): (props: React.ComponentProps<C>) => JSX.Element;
export function lazyLoad<TModule, C extends LoadableComponent>(
  importFunc: () => Promise<TModule>,
  selectorFunc?: (s: TModule) => C,
  opts: Opts = { fallback: null },
) {
  let lazyFactory: LoadableFactory<C>;

  if (selectorFunc) {
    lazyFactory = () =>
      importFunc().then(module => ({ default: selectorFunc(module) }));
  } else {
    lazyFactory = importFunc as unknown as LoadableFactory<C>;
  }

  const LazyComponent = lazy(lazyFactory);

  return (props: React.ComponentProps<C>): JSX.Element => (
    <Suspense fallback={opts.fallback!}>
      <LazyComponent {...props} />
    </Suspense>
  );
}

export function defaultLazyLoad<
  TModule extends { default: LoadableComponent },
>(
  importFunc: () => Promise<TModule>,
  selectorFunc?: undefined,
  opts?: Opts,
): (props: React.ComponentProps<TModule['default']>) => JSX.Element;
export function defaultLazyLoad<TModule, C extends LoadableComponent>(
  importFunc: () => Promise<TModule>,
  selectorFunc: (s: TModule) => C,
  opts?: Opts,
): (props: React.ComponentProps<C>) => JSX.Element;
export function defaultLazyLoad<TModule, C extends LoadableComponent>(
  importFunc: () => Promise<TModule>,
  selectorFunc?: (s: TModule) => C,
  opts: Opts = { fallback: <LoadingOutlined /> },
) {
  if (selectorFunc) {
    return lazyLoad(importFunc, selectorFunc, opts);
  }

  return lazyLoad(
    importFunc as () => Promise<{ default: LoadableComponent }>,
    undefined,
    opts,
  );
}
