/**
 * YuBi
 *
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { importQuillBlot } from '../quillCompat';

const Embed = importQuillBlot('blots/embed');

type CalcFieldEvent = Event & {
  value?: CalcFieldData;
  event?: MouseEvent;
};

type CalcFieldData = Record<string, unknown> & {
  denotationChar?: string;
  text?: string;
};

class CalcFieldBlot extends Embed {
  static blotName = 'calcfield';
  static tagName = 'span';
  static className = 'calcfield';

  static create(value?: unknown): HTMLElement {
    const data = (value ?? {}) as CalcFieldData;
    const node = super.create();
    node.addEventListener(
      'click',
      e => {
        const event = new Event('mention-clicked', {
          bubbles: true,
          cancelable: true,
        }) as CalcFieldEvent;
        event.value = data;
        event.event = e;
        window.dispatchEvent(event);
        e.preventDefault();
      },
      false,
    );
    const denotationChar = document.createElement('span');
    denotationChar.className = 'ql-calcfield-denotation-char';
    denotationChar.textContent = data.denotationChar ?? '';
    node.appendChild(denotationChar);
    node.appendChild(document.createTextNode(data.text || ''));
    return CalcFieldBlot.setDataValues(node, data);
  }

  static setDataValues(element: HTMLElement, data: CalcFieldData) {
    const domNode = element;
    Object.keys(data).forEach(key => {
      domNode.dataset[key] = String(data[key] ?? '');
    });
    return domNode;
  }

  static value(domNode: HTMLElement): DOMStringMap {
    return domNode.dataset;
  }
}
export default CalcFieldBlot;
