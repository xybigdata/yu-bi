import { ReactElement, cloneElement } from 'react';
import { mergeClassNames } from './utils';

type ClassNameProps = {
  className?: string;
};

export function cloneElementWithClassName<P extends ClassNameProps>(
  element: ReactElement<P> | ReactElement,
  className: string,
): ReactElement<P> | ReactElement {
  const classNameElement = element as ReactElement<ClassNameProps>;
  const props = classNameElement.props;
  return cloneElement(classNameElement, {
    className: mergeClassNames(props.className, className),
  });
}

export function cloneElementWithProps<P>(
  element: ReactElement<P>,
  props: Partial<P>,
): ReactElement<P> {
  return cloneElement(element, props);
}
