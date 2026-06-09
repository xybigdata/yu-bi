/// <reference types="styled-components/cssprop" />

declare namespace NodeJS {
  interface ProcessEnv {
    NODE_ENV: 'development' | 'production' | 'test';
    PUBLIC_URL: string;
  }
}

declare module '*.css';
declare module '*.svg' {
  import type { FC, SVGProps } from 'react';

  const src: string;
  export default src;
  export const ReactComponent: FC<SVGProps<SVGSVGElement>>;
}

declare module 'react-draggable' {
  interface DraggableCoreProps {
    children?: React.ReactNode;
  }
}
