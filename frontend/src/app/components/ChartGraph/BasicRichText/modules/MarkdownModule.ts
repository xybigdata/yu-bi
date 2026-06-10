import type {
  DeltaStatic,
  QuillInstance,
  RangeStatic,
} from '../quillCompat';

export interface MarkdownModuleOptions {
  ignoreTags?: string[];
  tags?: Record<string, { pattern?: RegExp | ((value: string) => RegExp | string | null) }>;
}

type InlineAction = {
  name: string;
  match: (text: string) => RegExpMatchArray | null;
  run: (ctx: MarkdownActionContext) => boolean;
};

type LineAction = {
  name: string;
  run: (ctx: MarkdownLineContext) => boolean;
};

type MarkdownActionContext = {
  annotatedText: string;
  lineStart: number;
  lineText: string;
  match: RegExpMatchArray;
  quill: QuillInstance;
  selection: RangeStatic;
};

type MarkdownLineContext = {
  line: any;
  lineIndex: number;
  lineOffset: number;
  quill: QuillInstance;
  selection: RangeStatic;
  text: string;
};

const defaultPatterns = {
  header: /^(#){1,6}\s/g,
  bold: /(\*|_){2}(.+?)(?:\1){2}/g,
  italic:
    /(?:^|\s)(?:(\*|_)\s*([^*_]+)\s*?\1|(\*|_){3}\s*([^*_]*)\s*\1{3})(?:$|(?=\s))/g,
  strikethrough: /(?:~|_){2}(.+?)(?:~|_){2}/g,
  inlineCode: /(`){1}(.+)(`){1}/g,
  blockquote: /^\s{0,99}(>)\s/g,
  orderedList: /^\s{0,9}(\d)+\.\s/g,
  bulletList: /^\s{0,9}(-|\*){1}\s/,
  checkboxChecked: /^(\[x\])+\s/g,
  checkboxUnchecked: /^(\[\s?\])+\s/g,
  link: /(?:\[(.+?)\])(?:\((.+?)\))/g,
  codeBlock: /^(```).*/g,
};

function execPattern(
  pattern: RegExp | ((value: string) => RegExp | string | null),
  value: string,
) {
  if (typeof pattern === 'function') {
    const result = pattern(value);
    if (!result || typeof result === 'string') {
      return null;
    }
    result.lastIndex = 0;
    return result.exec(value);
  }

  pattern.lastIndex = 0;
  return pattern.exec(value);
}

function isOnlyFormattingText(text: string, pattern: RegExp) {
  pattern.lastIndex = 0;
  return pattern.test(text);
}

export class MarkdownModule {
  private quill: QuillInstance;
  private options: MarkdownModuleOptions;
  private onTextChangeBound: (
    delta: DeltaStatic,
    oldContents: DeltaStatic,
    source: string,
  ) => void;

  constructor(quill: QuillInstance, options: MarkdownModuleOptions = {}) {
    this.quill = quill;
    this.options = options;
    this.onTextChangeBound = this.onTextChange.bind(this);
    this.quill.on('text-change', this.onTextChangeBound);
  }

  destroy() {
    this.quill.off('text-change', this.onTextChangeBound);
  }

  private getPattern(
    key: keyof typeof defaultPatterns,
    fallback = defaultPatterns[key],
  ) {
    return this.options.tags?.[key]?.pattern || fallback;
  }

  private onTextChange(
    delta: DeltaStatic,
    _oldContents: DeltaStatic,
    source: string,
  ) {
    if (source !== 'user') return;

    const insertOp = delta.ops?.find(op => Object.prototype.hasOwnProperty.call(op, 'insert'));
    const insertText =
      typeof insertOp?.insert === 'string' ? insertOp.insert : undefined;

    const retain = (delta.ops?.[0] as any)?.retain || 0;

    if (insertText && insertText.length > 1) {
      setTimeout(() => {
        this.processCurrentLine(retain);
      }, 0);
      return;
    }

    if (insertText && [' ', '\n', ')', '*', '`', '~', '_'].includes(insertText)) {
      setTimeout(() => {
        this.processCurrentLine();
      }, 0);
    }

    const deleteOp = delta.ops?.find(op => Object.prototype.hasOwnProperty.call(op, 'delete'));
    if (deleteOp && (deleteOp as any).delete === 1) {
      setTimeout(() => {
        this.releaseEmptyBlockFormats();
      }, 0);
    }
  }

  private processCurrentLine(selectionIndex?: number) {
    const selection = this.quill.getSelection();
    const index = selectionIndex ?? selection?.index;
    if (index === undefined || index === null) return;

    const [line, lineOffset] = this.quill.getLine(index);
    if (!line) return;

    const lineIndex = this.quill.getIndex(line);
    const lineText = line.domNode?.textContent || '';
    const normalizedText = `${lineText} `;
    const range = selection || ({ index, length: 0 } as RangeStatic);

    if (this.tryLineActions({
      line,
      lineIndex,
      lineOffset,
      quill: this.quill,
      selection: range,
      text: normalizedText,
    })) {
      return;
    }

    this.tryInlineActions({
      lineText,
      lineStart: lineIndex,
      quill: this.quill,
      selection: range,
    });
  }

  private tryInlineActions(base: {
    lineText: string;
    lineStart: number;
    quill: QuillInstance;
    selection: RangeStatic;
  }) {
    const inlineActions: InlineAction[] = [
      {
        name: 'bold',
        match: text => execPattern(this.getPattern('bold'), text),
        run: ({ annotatedText, lineStart, match, quill }) => {
          if (isOnlyFormattingText(base.lineText, /^([*_ \n]+)$/g)) return false;
          const startIndex = lineStart + (match.index || 0);
          const matchedText = match[2];
          quill.deleteText(startIndex, annotatedText.length);
          quill.insertText(startIndex, matchedText, { bold: true });
          quill.format('bold', false);
          return true;
        },
      },
      {
        name: 'italic',
        match: text => execPattern(this.getPattern('italic'), text),
        run: ({ annotatedText, lineStart, match, quill }) => {
          const matchedToken = match[1] || match[3];
          const matchedText = match[2] || match[4];
          const firstToken = quill.getText()[lineStart + (match.index || 0)];
          const secondToken = quill.getText()[lineStart + (match.index || 0) + 1];
          if (matchedToken === firstToken && firstToken === secondToken) {
            return false;
          }
          const startIndex = lineStart + (match.index || 0);
          const isFirstLine = !(match.index || 0);
          const adjustPosition = isFirstLine ? startIndex : startIndex + 1;
          const deleteEndOffset = isFirstLine
            ? annotatedText.length
            : annotatedText.length - 1;
          quill.deleteText(adjustPosition, deleteEndOffset);
          quill.insertText(adjustPosition, matchedText, { italic: true });
          quill.format('italic', false);
          return true;
        },
      },
      {
        name: 'strikethrough',
        match: text => execPattern(this.getPattern('strikethrough'), text),
        run: ({ annotatedText, lineStart, match, quill }) => {
          if (isOnlyFormattingText(base.lineText, /^([~_ \n]+)$/g)) return false;
          const startIndex = lineStart + (match.index || 0);
          const matchedText = match[1];
          quill.deleteText(startIndex, annotatedText.length);
          quill.insertText(startIndex, matchedText, { strike: true });
          quill.format('strike', false);
          return true;
        },
      },
      {
        name: 'inlineCode',
        match: text => execPattern(this.getPattern('inlineCode'), text),
        run: ({ annotatedText, lineStart, match, quill }) => {
          const startIndex = lineStart + (match.index || 0);
          const message = annotatedText.replace(/`/g, '');
          quill.deleteText(startIndex, annotatedText.length);
          quill.insertText(startIndex, message, { code: true });
          quill.insertText(startIndex + message.length, ' ', { code: false });
          return true;
        },
      },
    ];

    for (const action of inlineActions) {
      const match = action.match(base.lineText);
      if (!match) continue;
      const formatted = action.run({
        annotatedText: match[0],
        lineStart: base.lineStart,
        lineText: base.lineText,
        match,
        quill: base.quill,
        selection: base.selection,
      });
      if (formatted) {
        return true;
      }
    }

    return false;
  }

  private tryLineActions(ctx: MarkdownLineContext) {
    const actions: LineAction[] = [
      {
        name: 'header',
        run: ({ lineIndex, quill, text }) => {
          const match = execPattern(this.getPattern('header'), text);
          if (!match) return false;
          const level = match[0].length - 1;
          quill.formatLine(lineIndex, 0, 'header', level);
          quill.deleteText(lineIndex, match[0].length);
          return true;
        },
      },
      {
        name: 'blockquote',
        run: ({ lineIndex, quill, text }) => {
          const match = execPattern(this.getPattern('blockquote'), text);
          if (!match) return false;
          quill.deleteText(lineIndex, match[0].length);
          quill.formatLine(lineIndex, 0, 'blockquote', true);
          return true;
        },
      },
      {
        name: 'orderedList',
        run: ({ lineIndex, quill, text }) => {
          const match = execPattern(this.getPattern('orderedList'), text);
          if (!match) return false;
          const depth = text
            .split('. ')[0]
            .split('')
            .filter(ch => /\s/.test(ch)).length;
          const replaceText = text.split('. ').slice(1).join('. ').trimEnd();
          quill.deleteText(lineIndex, text.length - 1);
          quill.insertText(lineIndex, replaceText);
          quill.formatLine(lineIndex, 0, { list: 'ordered', indent: depth });
          return true;
        },
      },
      {
        name: 'bulletList',
        run: ({ lineIndex, quill, text }) => {
          const match = execPattern(this.getPattern('bulletList'), text);
          if (!match) return false;
          const normalized = /^\s{0,9}\*/.test(text) ? text.replace('*', '-') : text;
          const depth = normalized
            .split('- ')[0]
            .split('')
            .filter(ch => /\s/.test(ch)).length;
          const replaceText = normalized.split('- ').slice(1).join('- ').trimEnd();
          quill.deleteText(lineIndex, text.length - 1);
          quill.insertText(lineIndex, replaceText);
          quill.formatLine(lineIndex, 0, { list: 'bullet', indent: depth });
          return true;
        },
      },
      {
        name: 'checkboxChecked',
        run: ({ lineIndex, quill, text }) => {
          const match = execPattern(this.getPattern('checkboxChecked'), text);
          if (!match) return false;
          const replaceText = text.split('[x] ').slice(1).join('[x] ').trimEnd();
          quill.deleteText(lineIndex, text.length - 1);
          quill.insertText(lineIndex, replaceText);
          quill.formatLine(lineIndex, 0, 'list', 'checked');
          return true;
        },
      },
      {
        name: 'checkboxUnchecked',
        run: ({ lineIndex, quill, text }) => {
          const match = execPattern(this.getPattern('checkboxUnchecked'), text);
          if (!match) return false;
          let replaceText = text;
          replaceText = replaceText.includes('[ ] ')
            ? replaceText.split('[ ] ').slice(1).join('[ ] ')
            : replaceText;
          replaceText = replaceText.includes('[] ')
            ? replaceText.split('[] ').slice(1).join('[] ')
            : replaceText;
          replaceText = replaceText.trimEnd();
          quill.deleteText(lineIndex, text.length - 1);
          quill.insertText(lineIndex, replaceText);
          quill.formatLine(lineIndex, 0, 'list', 'unchecked');
          return true;
        },
      },
      {
        name: 'link',
        run: ({ lineIndex, quill, text }) => {
          const match = execPattern(this.getPattern('link'), text);
          if (!match) return false;
          const matchedText = match[0];
          const hrefText = match[1];
          const hrefLink = match[2];
          const startIndex = lineIndex + (match.index || 0);
          quill.deleteText(startIndex, matchedText.length);
          quill.insertText(startIndex, hrefText, 'link', hrefLink);
          return true;
        },
      },
      {
        name: 'codeBlock',
        run: ({ lineIndex, quill, text }) => {
          const match = execPattern(this.getPattern('codeBlock'), text);
          if (!match) return false;
          quill.deleteText(lineIndex, match[0].length);
          quill.insertText(lineIndex, '\n');
          quill.insertText(lineIndex + 1, '\n');
          quill.formatLine(lineIndex + 1, 1, 'code-block', true);
          return true;
        },
      },
    ];

    for (const action of actions) {
      if (action.run(ctx)) {
        return true;
      }
    }

    return false;
  }

  private releaseEmptyBlockFormats() {
    const selection = this.quill.getSelection();
    if (!selection) return;

    const [line, length] = this.quill.getLine(selection.index);
    if (!line) return;

    if (length === 0) {
      this.quill.format('blockquote', false);
      this.quill.format('code-block', false);
    }
  }
}

export default MarkdownModule;
