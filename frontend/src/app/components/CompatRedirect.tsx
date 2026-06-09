import { FC, useEffect } from 'react';
import { type RouteProps } from 'app/routerCompat';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';

interface CompatRedirectProps {
  to: string;
}

export const CompatRedirect: FC<CompatRedirectProps & RouteProps> = ({
  to,
  path,
  exact,
}) => {
  const navigate = useCompatNavigate();

  useEffect(() => {
    navigate.replace(to);
  }, [navigate, to]);

  if (path || exact) {
    return null;
  }

  return null;
};
