import classnames from 'classnames';
import React, {
  ComponentPropsWithoutRef,
  forwardRef,
  ReactNode,
  useMemo,
} from 'react';
import { Link, useLocation } from 'app/routerCompat';

interface CompatNavLinkProps extends Omit<
  ComponentPropsWithoutRef<typeof Link>,
  'children' | 'to'
> {
  activeClassName?: string;
  children?: ReactNode;
  isActive?: (match: unknown, location: { pathname: string }) => boolean;
  to: string;
}

export const CompatNavLink = forwardRef<HTMLAnchorElement, CompatNavLinkProps>(
  (
    { activeClassName, children, className, isActive, to, ...linkProps },
    ref,
  ) => {
    const location = useLocation();
    const active = useMemo(() => {
      if (isActive) {
        return isActive(undefined, { pathname: location.pathname });
      }
      return location.pathname === to;
    }, [isActive, location.pathname, to]);

    return (
      <Link
        {...linkProps}
        ref={ref}
        className={classnames(className, {
          [activeClassName || 'active']: active,
        })}
        to={to}
      >
        {children}
      </Link>
    );
  },
);

CompatNavLink.displayName = 'CompatNavLink';
