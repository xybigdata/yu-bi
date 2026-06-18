import { useEffect, useLayoutEffect } from 'react';
import type { RefObject } from 'react';
import type { RichTextEditorHandle } from './RichTextEditor';
import {
  type RichTextCustomColorType,
  QuillPalette,
} from './RichTextPluginLoader/CustomColor';
import { MarkdownOptions } from './RichTextPluginLoader/RichTextConfig';
import type { DeltaStatic } from './quillCompat';

export type RichTextColorType = 'color' | 'background';
export type RichTextColorState = Pick<
  RichTextCustomColorType,
  'background' | 'color'
>;
export type RichTextFieldReference = {
  id?: string;
  name: string;
  value: string;
};
export type RichTextToolbarHandlers = {
  color: (value: string) => void;
  background: (value: string) => void;
};
export type RichTextModules = {
  toolbar: {
    container: string | null;
    handlers: RichTextToolbarHandlers;
  };
  imageDrop: boolean;
  calcfield?: Record<string, never>;
};

type RichTextEditorRef = RefObject<RichTextEditorHandle | null>;
type CalcFieldInsert = {
  calcfield?: {
    id?: string;
    name?: string;
  };
};

export const getRichTextContainerId = (id: string | number) => {
  return `rich-text-${id}`;
};

export const createRichTextColorHandlers = ({
  editorRef,
  onOpenCustomColor,
}: {
  editorRef: RichTextEditorRef;
  onOpenCustomColor: (type: RichTextColorType) => void;
}): RichTextToolbarHandlers => ({
  color: value => {
    if (value === QuillPalette.RICH_TEXT_CUSTOM_COLOR) {
      onOpenCustomColor('color');
    }
    editorRef.current?.format('color', value);
  },
  background: value => {
    if (value === QuillPalette.RICH_TEXT_CUSTOM_COLOR) {
      onOpenCustomColor('background');
    }
    editorRef.current?.format('background', value);
  },
});

export const createRichTextModules = ({
  containerId,
  handlers,
  editable,
  includeCalcField = false,
}: {
  containerId: string;
  handlers: RichTextToolbarHandlers;
  editable: boolean;
  includeCalcField?: boolean;
}): RichTextModules => ({
  toolbar: {
    container: editable ? `#${containerId}` : null,
    handlers,
  },
  imageDrop: true,
  ...(includeCalcField ? { calcfield: {} } : {}),
});

export const translateRichTextCalcFields = (
  value: DeltaStatic | string,
  fields: RichTextFieldReference[],
): DeltaStatic | string => {
  if (typeof value === 'string') {
    return value;
  }

  const ops = value.ops?.map(item => {
    const insert = item.insert;
    if (!insert || typeof insert === 'string') {
      return item;
    }

    const calcfield = (insert as CalcFieldInsert).calcfield;
    if (!calcfield?.name) {
      return item;
    }

    const field = fields.find(current =>
      calcfield.id
        ? current.id === calcfield.id
        : current.name === calcfield.name,
    );
    if (!field) {
      return item;
    }

    return {
      ...item,
      insert: field.value,
    };
  });

  return ops?.length ? { ...value, ops } : '';
};

export const getRichTextFieldKey = (
  field: RichTextFieldReference,
  index: number,
) => field.id || `${field.name}-${index}`;

export const findRichTextFieldByKey = (
  fields: RichTextFieldReference[],
  key: string,
) => fields.find((field, index) => getRichTextFieldKey(field, index) === key);

export const useRichTextMarkdownModule = ({
  editorRef,
  enabled,
}: {
  editorRef: RichTextEditorRef;
  enabled: boolean;
}) => {
  useLayoutEffect(() => {
    if (!enabled) {
      return;
    }

    let destroyed = false;
    let module: { destroy: () => void } | null = null;
    let frameId = 0;

    const bindMarkdownModule = () => {
      if (destroyed) {
        return;
      }
      if (!editorRef.current?.isReady()) {
        frameId = window.requestAnimationFrame(bindMarkdownModule);
        return;
      }

      module = editorRef.current.createMarkdownModule(MarkdownOptions);
    };

    bindMarkdownModule();

    return () => {
      destroyed = true;
      if (frameId) {
        window.cancelAnimationFrame(frameId);
      }
      module?.destroy();
    };
  }, [editorRef, enabled]);
};

export const useRichTextPalette = ({
  editorRef,
  containerId,
  onChange,
}: {
  editorRef: RichTextEditorRef;
  containerId?: string;
  onChange: (data: RichTextColorState) => void;
}) => {
  useEffect(() => {
    if (!containerId) {
      return;
    }

    let destroyed = false;
    let palette: QuillPalette | null = null;
    let frameId = 0;

    const bindPalette = () => {
      if (destroyed) {
        return;
      }
      if (!editorRef.current?.isReady()) {
        frameId = window.requestAnimationFrame(bindPalette);
        return;
      }

      palette = new QuillPalette(editorRef.current, {
        toolbarId: containerId,
        onChange,
      });
    };

    bindPalette();

    return () => {
      destroyed = true;
      if (frameId) {
        window.cancelAnimationFrame(frameId);
      }
      palette?.destroy();
    };
  }, [containerId, editorRef, onChange]);
};
