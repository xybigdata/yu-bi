/// <reference types="styled-components/cssprop" />

declare namespace NodeJS {
  interface ProcessEnv {
    NODE_ENV: 'development' | 'production' | 'test';
    PUBLIC_URL: string;
  }
}

declare module '*.css';
declare module '*.svg' {
  const src: string;
  export default src;
}

declare module '*.svg?react' {
  import type { FC, SVGProps } from 'react';

  const ReactComponent: FC<SVGProps<SVGSVGElement>>;
  export default ReactComponent;
}

declare module 'react-grid-layout/legacy' {
  export {
    default,
    Layout,
    LayoutItem,
    ResponsiveLayouts,
    WidthProvider,
  } from 'react-grid-layout';
}
