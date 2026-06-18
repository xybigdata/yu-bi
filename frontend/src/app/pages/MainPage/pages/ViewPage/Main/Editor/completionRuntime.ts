import { loadMonaco } from 'app/components/MonacoEditor/runtime';
import type * as Monaco from 'monaco-editor';
import type { MutableRefObject } from 'react';
import { AppDispatch } from 'redux/configureStore';
import { getEditorProvideCompletionItems } from '../../slice/thunks';

type CompletionProviderRef = MutableRefObject<Monaco.IDisposable | undefined>;

export function registerSqlCompletionProvider({
  dispatch,
  providerRef,
  sourceId,
  isCancelled,
  errorMessage,
}: {
  dispatch: AppDispatch;
  providerRef: CompletionProviderRef;
  sourceId?: string;
  isCancelled: () => boolean;
  errorMessage: string;
}) {
  providerRef.current?.dispose();

  void loadMonaco()
    .then(monaco => {
      if (isCancelled()) {
        return;
      }
      dispatch(
        getEditorProvideCompletionItems({
          sourceId,
          resolve: getItem => {
            if (isCancelled()) {
              return;
            }
            providerRef.current =
              monaco.languages.registerCompletionItemProvider('sql', {
                provideCompletionItems: getItem,
              });
          },
        }),
      );
    })
    .catch(error => {
      console.error(errorMessage, error);
    });
}
