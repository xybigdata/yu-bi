import classnames from 'classnames';
import React, { FC, ReactNode, useMemo } from 'react';
import { Link, useLocation } from 'app/routerCompat';

interface CompatNavLinkProps {
  activeClassName?: string;
  children?: ReactNode;
  className?: string;
  isActive?: (match: unknown, location: { pathname: string }) => boolean;
  to: string;
}

export const CompatNavLink: FC<CompatNavLinkProps> = ({
  activeClassName,
  children,
  className,
  isActive,
  to,
}) => {
  const location = useLocation();
  const active = useMemo(() => {
    if (isActive) {
      return isActive(undefined, { pathname: location.pathname });
    }
    return location.pathname === to;
  }, [isActive, location.pathname, to]);

  return (
    <Link
      className={classnames(className, {
        [activeClassName || 'active']: active,
      })}
      to={to}
    >
      {children}
    </Link>
  );
};
