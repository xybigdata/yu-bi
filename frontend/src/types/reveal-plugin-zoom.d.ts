declare module 'reveal.js/plugin/zoom' {
  import type { RevealPlugin } from 'reveal.js';

  interface ZoomPlugin extends RevealPlugin {
    id: 'zoom';
  }

  const RevealZoom: () => ZoomPlugin;

  export default RevealZoom;
}
