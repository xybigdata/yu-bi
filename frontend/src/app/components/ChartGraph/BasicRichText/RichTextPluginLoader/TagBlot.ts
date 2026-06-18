import { Quill } from '../quillCompat';

const Embed = Quill.import('blots/embed');

type TagBlotData = {
  name?: string | null;
  background?: string | null;
  color?: string | null;
};

class TagBlot extends Embed {
  static blotName = 'tag';
  static tagName = 'span';
  static className = 'tag-container';

  static create(data: TagBlotData): HTMLElement {
    const node = super.create(data);

    const { name, background, color } = data;

    // 将 delta 数据中的 insert 值存储在 dom 中，以便 static value 中能拿到
    node.setAttribute('data-name', name ?? '');
    node.setAttribute('data-background', background ?? '');
    node.setAttribute('data-color', color ?? '');
    node.setAttribute('contenteditable', 'false');

    const nodeSpan = document.createElement('span');

    nodeSpan.textContent = name ?? '';
    node.appendChild(nodeSpan);
    return node;
  }
  static value(node: HTMLElement): TagBlotData {
    return {
      name: node.getAttribute('data-name'),
      background: node.getAttribute('data-background'),
      color: node.getAttribute('data-color'),
    };
  }
}
export default TagBlot;
