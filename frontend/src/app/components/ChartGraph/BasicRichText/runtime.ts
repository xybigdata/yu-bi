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

export function loadRichTextEditorRuntime() {
  if (!richTextEditorRuntimePromise) {
    richTextEditorRuntimePromise = import('./RichTextEditorRuntime');
  }

  return richTextEditorRuntimePromise;
}
