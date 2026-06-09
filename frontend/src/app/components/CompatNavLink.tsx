import classnames from 'classnames';
import React, { FC, ReactNode, useMemo } from 'react';
import { NavLink, useLocation } from 'app/routerCompat';

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
  const overrideActive = useMemo(() => {
    if (!isActive) {
      return false;
    }
    return isActive(undefined, { pathname: location.pathname });
  }, [isActive, location]);

  return (
    <NavLink
      className={classnames(className, {
        [activeClassName || 'active']: overrideActive,
      })}
      isActive={isActive}
      to={to}
    >
      {children}
    </NavLink>
  );
};
