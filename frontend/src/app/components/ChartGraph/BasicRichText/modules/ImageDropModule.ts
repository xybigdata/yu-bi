import type { QuillInstance } from '../quillCompat';

/**
 * 本地维护的图片拖拽/粘贴模块，用来替代低活跃度的 quill-image-drop-module。
 * 当前行为与既有插件保持一致，方便后续切换到 Quill 2 时只在这里集中适配。
 */
export class ImageDropModule {
  private quill: QuillInstance;

  constructor(quill: QuillInstance) {
    this.quill = quill;
    this.handleDrop = this.handleDrop.bind(this);
    this.handlePaste = this.handlePaste.bind(this);

    this.quill.root.addEventListener('drop', this.handleDrop, false);
    this.quill.root.addEventListener('paste', this.handlePaste, false);
  }

  destroy() {
    this.quill.root.removeEventListener('drop', this.handleDrop, false);
    this.quill.root.removeEventListener('paste', this.handlePaste, false);
  }

  handleDrop(evt: DragEvent) {
    evt.preventDefault();
    if (evt.dataTransfer?.files?.length) {
      if (document.caretRangeFromPoint) {
        const selection = document.getSelection();
        const range = document.caretRangeFromPoint(evt.clientX, evt.clientY);
        if (selection && range) {
          selection.setBaseAndExtent(
            range.startContainer,
            range.startOffset,
            range.startContainer,
            range.startOffset,
          );
        }
      }
      this.readFiles(evt.dataTransfer.files, this.insert.bind(this));
    }
  }

  handlePaste(evt: ClipboardEvent) {
    if (evt.clipboardData?.items?.length) {
      this.readFiles(evt.clipboardData.items, dataUrl => {
        const selection = this.quill.getSelection();
        if (!selection) {
          setTimeout(() => this.insert(dataUrl), 0);
        }
      });
    }
  }

  insert(dataUrl: string) {
    const selection = this.quill.getSelection();
    const index = selection?.index ?? this.quill.getLength();
    this.quill.insertEmbed(index, 'image', dataUrl, 'user');
  }

  readFiles(
    files: ArrayLike<File | DataTransferItem>,
    callback: (dataUrl: string) => void,
  ) {
    Array.prototype.forEach.call(files, item => {
      const file = item as File | DataTransferItem;
      if (
        !file.type.match(
          /^image\/(gif|jpe?g|a?png|svg|webp|bmp|vnd\.microsoft\.icon)/i,
        )
      ) {
        return;
      }

      const reader = new FileReader();
      reader.onload = evt => {
        const result = evt.target?.result;
        if (typeof result === 'string') {
          callback(result);
        }
      };

      const blob = 'getAsFile' in file ? file.getAsFile() : file;
      if (blob instanceof Blob) {
        reader.readAsDataURL(blob);
      }
    });
  }
}

export default ImageDropModule;
