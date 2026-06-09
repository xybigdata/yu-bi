/// <reference types="react-scripts" />
/// <reference types="styled-components/cssprop" />

declare module '*.css';

declare module 'react-draggable' {
  interface DraggableCoreProps {
    children?: React.ReactNode;
  }
}
