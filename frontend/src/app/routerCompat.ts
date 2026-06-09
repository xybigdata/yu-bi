import {
  BrowserRouter,
  Link,
  MemoryRouter,
  useLocation,
  useNavigate,
  useParams as useReactRouterParams,
} from 'react-router-dom';

export { BrowserRouter, Link, MemoryRouter, useLocation, useNavigate };

export const useParams = <
  TParams extends Record<string, string> = Record<string, string>,
>() => useReactRouterParams() as TParams;
