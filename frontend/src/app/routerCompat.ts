import React from 'react';
import {
  BrowserRouter as ReactRouterBrowserRouter,
  type BrowserRouterProps,
  Link,
  MemoryRouter as ReactRouterMemoryRouter,
  type MemoryRouterProps,
  useLocation,
  useNavigate,
  useParams as useReactRouterParams,
} from 'react-router-dom';

const routerFuture = {
  v7_startTransition: true,
  v7_relativeSplatPath: true,
};

export function BrowserRouter(props: BrowserRouterProps) {
  return React.createElement(ReactRouterBrowserRouter, {
    ...props,
    future: { ...routerFuture, ...props.future },
  });
}

export function MemoryRouter(props: MemoryRouterProps) {
  return React.createElement(ReactRouterMemoryRouter, {
    ...props,
    future: { ...routerFuture, ...props.future },
  });
}

export { Link, useLocation, useNavigate };

export const useParams = <
  TParams extends Record<string, string> = Record<string, string>,
>() => useReactRouterParams() as TParams;
