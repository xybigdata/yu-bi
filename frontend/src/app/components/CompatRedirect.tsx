import { FC, useEffect } from 'react';
import { useCompatNavigate } from 'app/hooks/useCompatNavigate';

interface CompatRedirectProps {
  to: string;
  path?: string | readonly string[];
  exact?: boolean;
}

export const CompatRedirect: FC<CompatRedirectProps> = ({
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
