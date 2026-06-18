import type { ForwardRefExoticComponent, RefAttributes } from 'react';
import type {
  RichTextEditorHandle,
  RichTextEditorProps,
} from './RichTextEditor';

type RichTextEditorRuntimeModule = {
  default: ForwardRefExoticComponent<
    RichTextEditorProps & RefAttributes<RichTextEditorHandle>
  >;
};

let richTextEditorRuntimePromise: Promise<RichTextEditorRuntimeModule> | null =
  null;
let richTextEditorRuntimeLoader: () => Promise<RichTextEditorRuntimeModule> =
  () => import('./RichTextEditorRuntime');

export function loadRichTextEditorRuntime() {
  if (!richTextEditorRuntimePromise) {
    richTextEditorRuntimePromise = richTextEditorRuntimeLoader().catch(
      error => {
        richTextEditorRuntimePromise = null;
        throw error;
      },
    );
  }

  return richTextEditorRuntimePromise;
}

export function __setRichTextEditorRuntimeLoaderForTest(
  loader: () => Promise<RichTextEditorRuntimeModule>,
) {
  richTextEditorRuntimeLoader = loader;
  richTextEditorRuntimePromise = null;
}

export function __resetRichTextEditorRuntimeLoaderForTest() {
  richTextEditorRuntimeLoader = () => import('./RichTextEditorRuntime');
  richTextEditorRuntimePromise = null;
}
