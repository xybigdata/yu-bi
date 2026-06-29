import { Children, type ReactElement, useEffect, useRef } from 'react';

interface ContentProps {
  children: ReactElement;
  contentDidMount: () => void;
  contentDidUpdate: () => void;
}

export default function Content({
  children,
  contentDidMount,
  contentDidUpdate,
}: ContentProps) {
  const mounted = useRef(false);

  useEffect(() => {
    if (!mounted.current) {
      mounted.current = true;
      contentDidMount();
    } else {
      contentDidUpdate();
    }
  });

  return Children.only(children);
}
