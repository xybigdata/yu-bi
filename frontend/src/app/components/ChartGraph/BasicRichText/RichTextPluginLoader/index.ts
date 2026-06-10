import type { Quill as QuillInstance, RangeStatic } from 'quill';
import { Quill } from '../quillCompat';
import CalcFieldBlot from './CalcFieldBlot';

Quill.register(CalcFieldBlot);

const Keys = {
  TAB: 9,
  ENTER: 13,
  ESCAPE: 27,
  UP: 38,
  DOWN: 40,
} as const;

type CalcFieldData = Record<string, any>;
const defaultDataAttributes = [
  'id',
  'value',
  'denotationChar',
  'link',
  'target',
  'disabled',
  'viewId',
  'model',
  'text',
  'agg',
  'size',
  'font-size',
];

type SourceExecutionToken = {
  abandoned: boolean;
};

type AllowedCharsResolver =
  | RegExp
  | ((denotationChar: string) => RegExp);

type CalcFieldSource = (
  searchTerm: string,
  renderList: (data: CalcFieldData[], searchTerm?: string) => void,
  denotationChar: string,
) => void;

type CalcFieldOptions = {
  source: CalcFieldSource | null;
  numberFieldDenotationChars: string[];
  showDenotationChar: boolean;
  allowedChars: AllowedCharsResolver;
  minChars: number;
  maxChars: number;
  offsetTop: number;
  offsetLeft: number;
  isolateCharacter: boolean;
  fixFieldsToQuill: boolean;
  positioningStrategy: 'normal' | string;
  defaultMenuOrientation: 'bottom' | 'top' | string;
  blotName: string;
  dataAttributes: string[];
  linkTarget: string;
  spaceAfterInsert: boolean;
  selectKeys: number[];
};

function getFieldCharIndex(
  text: string,
  numberFieldDenotationChars: string[],
) {
  return numberFieldDenotationChars.reduce(
    (prev, numberFieldChar) => {
      const numberFieldCharIndex = text.lastIndexOf(numberFieldChar);

      if (numberFieldCharIndex > prev.numberFieldCharIndex) {
        return {
          numberFieldChar,
          numberFieldCharIndex,
        };
      }

      return prev;
    },
    {
      numberFieldChar: null as string | null,
      numberFieldCharIndex: -1,
    },
  );
}

function hasValidChars(text: string, allowedChars: RegExp) {
  return allowedChars.test(text);
}

function hasValidFieldCharIndex(
  numberFieldCharIndex: number,
  text: string,
  isolateChar: boolean,
) {
  if (numberFieldCharIndex <= -1) {
    return false;
  }

  if (
    isolateChar &&
    !(
      numberFieldCharIndex === 0 ||
      Boolean(text[numberFieldCharIndex - 1]?.match(/\s/g))
    )
  ) {
    return false;
  }

  return true;
}

class CalcField {
  private existingSourceExecutionToken: SourceExecutionToken | null = null;
  private numberFieldCharPos: number | null = null;
  private cursorPos: number | null = null;
  private readonly options: CalcFieldOptions;
  private readonly quill: QuillInstance;

  constructor(quill: QuillInstance, options: Partial<CalcFieldOptions> = {}) {
    this.quill = quill;
    const dataAttributes = Array.isArray(options.dataAttributes)
      ? [...defaultDataAttributes, ...options.dataAttributes]
      : defaultDataAttributes;
    const mergedOptions = {
      ...options,
      dataAttributes,
    };

    this.options = {
      source: null,
      numberFieldDenotationChars: ['@'],
      showDenotationChar: true,
      allowedChars: /^[a-zA-Z0-9_]*$/,
      minChars: 0,
      maxChars: 31,
      offsetTop: 2,
      offsetLeft: 0,
      isolateCharacter: false,
      fixFieldsToQuill: false,
      positioningStrategy: 'normal',
      defaultMenuOrientation: 'bottom',
      blotName: 'calcfield',
      linkTarget: '_blank',
      spaceAfterInsert: true,
      selectKeys: [Keys.ENTER],
      ...mergedOptions,
    };

    quill.on('text-change', this.onTextChange);
    quill.on('selection-change', this.onSelectionChange);
    (quill as any).container.addEventListener('paste', this.handlePaste);

    this.options.selectKeys.forEach(selectKey => {
      (quill.keyboard as any).addBinding({
        key: selectKey,
      });
    });
  }

  insertItem(data: CalcFieldData | null, programmaticInsert?: boolean) {
    if (data === null || this.cursorPos === null) {
      return;
    }

    const render = { ...data };

    if (!this.options.showDenotationChar) {
      render.denotationChar = '';
    }

    let insertAtPos: number;
    if (!programmaticInsert && this.numberFieldCharPos !== null) {
      insertAtPos = this.numberFieldCharPos;
      this.quill.deleteText(
        this.numberFieldCharPos,
        this.cursorPos - this.numberFieldCharPos,
        'user',
      );
    } else {
      insertAtPos = this.cursorPos;
    }

    this.quill.insertEmbed(
      insertAtPos,
      this.options.blotName,
      render,
      'user',
    );

    if (this.options.spaceAfterInsert) {
      this.quill.insertText(insertAtPos + 1, ' ', 'user');
      this.quill.setSelection(insertAtPos + 2, 0, 'user');
    } else {
      this.quill.setSelection(insertAtPos + 1, 0, 'user');
    }
  }

  private getTextBeforeCursor() {
    const cursorPos = this.cursorPos ?? 0;
    const startPos = Math.max(0, cursorPos - this.options.maxChars);
    return this.quill.getText(startPos, cursorPos - startPos);
  }

  private readonly handlePaste = () => {
    setTimeout(() => {
      this.onSelectionChange(this.quill.getSelection());
    });
  };

  private readonly onSomethingChange = () => {
    const range = this.quill.getSelection();
    if (!range) {
      return;
    }

    this.cursorPos = range.index;
    const textBeforeCursor = this.getTextBeforeCursor();
    const { numberFieldChar, numberFieldCharIndex } = getFieldCharIndex(
      textBeforeCursor,
      this.options.numberFieldDenotationChars,
    );

    if (
      !numberFieldChar ||
      !hasValidFieldCharIndex(
        numberFieldCharIndex,
        textBeforeCursor,
        this.options.isolateCharacter,
      )
    ) {
      return;
    }

    const numberFieldCharPos =
      this.cursorPos - (textBeforeCursor.length - numberFieldCharIndex);
    this.numberFieldCharPos = numberFieldCharPos;

    const textAfter = textBeforeCursor.substring(
      numberFieldCharIndex + numberFieldChar.length,
    );

    if (
      textAfter.length < this.options.minChars ||
      !hasValidChars(textAfter, this.getAllowedCharsRegex(numberFieldChar))
    ) {
      return;
    }

    if (!this.options.source) {
      return;
    }

    if (this.existingSourceExecutionToken) {
      this.existingSourceExecutionToken.abandoned = true;
    }

    const sourceRequestToken: SourceExecutionToken = {
      abandoned: false,
    };
    this.existingSourceExecutionToken = sourceRequestToken;

    this.options.source(
      textAfter,
      (_data, _searchTerm) => {
        if (sourceRequestToken.abandoned) {
          return;
        }

        this.existingSourceExecutionToken = null;
      },
      numberFieldChar,
    );
  };

  private getAllowedCharsRegex(denotationChar: string) {
    if (this.options.allowedChars instanceof RegExp) {
      return this.options.allowedChars;
    }

    return this.options.allowedChars(denotationChar);
  }

  private readonly onTextChange = (
    _delta: unknown,
    _oldDelta: unknown,
    source: string,
  ) => {
    if (source === 'user') {
      this.onSomethingChange();
    }
  };

  private readonly onSelectionChange = (range: RangeStatic | null) => {
    if (range && range.length === 0) {
      this.onSomethingChange();
    }
  };
}

Quill.register('modules/calcfield', CalcField);
export default CalcField;
