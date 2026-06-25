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

export function BrowserRouter(props: BrowserRouterProps) {
  return React.createElement(ReactRouterBrowserRouter, props);
}

export function MemoryRouter(props: MemoryRouterProps) {
  return React.createElement(ReactRouterMemoryRouter, props);
}

export { Link, useLocation, useNavigate };

export const useParams = <
  TParams extends Record<string, string> = Record<string, string>,
>() => useReactRouterParams() as TParams;
