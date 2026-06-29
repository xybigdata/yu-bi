import React, {
  type IframeHTMLAttributes,
  type ReactNode,
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';
import ReactDOM from 'react-dom';
import Content from './Content';
import { FrameContextProvider } from './Context';

interface FrameProps extends Omit<
  IframeHTMLAttributes<HTMLIFrameElement>,
  'children' | 'srcDoc'
> {
  head?: ReactNode;
  mountTarget?: string;
  contentDidMount?: () => void;
  contentDidUpdate?: () => void;
  children?: ReactNode;
}

export function Frame({
  style = {},
  head = null,
  mountTarget,
  contentDidMount = () => {},
  contentDidUpdate = () => {},
  children,
  ...rest
}: FrameProps) {
  const nodeRef = useRef<HTMLIFrameElement | null>(null);
  const [iframeLoaded, setIframeLoaded] = useState(false);

  const getDoc = useCallback(() => {
    return nodeRef.current ? nodeRef.current.contentDocument : null;
  }, []);

  const getMountTarget = useCallback(() => {
    const doc = getDoc();
    if (!doc) return null;
    if (mountTarget) {
      return doc.querySelector(mountTarget);
    }
    return doc.body;
  }, [getDoc, mountTarget]);

  const handleLoad = useCallback(() => {
    setIframeLoaded(true);
  }, []);

  useEffect(() => {
    const iframe = nodeRef.current;
    if (!iframe) return;

    const doc = iframe.contentDocument;
    if (doc && doc.readyState === 'complete') {
      setIframeLoaded(true);
    } else {
      iframe.addEventListener('load', handleLoad);
    }

    return () => {
      iframe.removeEventListener('load', handleLoad);
    };
  }, [handleLoad]);

  const setRef = useCallback((node: HTMLIFrameElement | null) => {
    nodeRef.current = node;
  }, []);

  const renderFrameContents = () => {
    const doc = getDoc();
    if (!doc) return null;

    const target = getMountTarget();
    if (!target) return null;

    const win = doc.defaultView || (doc as any).parentView;
    const contents = (
      <Content
        contentDidMount={contentDidMount}
        contentDidUpdate={contentDidUpdate}
      >
        <FrameContextProvider value={{ document: doc, window: win }}>
          <div className="frame-content">{children}</div>
        </FrameContextProvider>
      </Content>
    );

    return [
      ReactDOM.createPortal(head, doc.head),
      ReactDOM.createPortal(contents, target),
    ];
  };

  // Remove props that shouldn't be passed to the iframe element
  const iframeProps = { ...rest, style };
  delete (iframeProps as any).srcDoc;

  return (
    <iframe title={rest.id} {...iframeProps} ref={setRef} onLoad={handleLoad}>
      {renderFrameContents()}
    </iframe>
  );
}
