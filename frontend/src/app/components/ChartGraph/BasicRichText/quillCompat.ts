import ReactQuill, { Quill } from 'react-quill';
import type QuillType from 'react-quill/node_modules/@types/quill';
import type {
  DeltaStatic as QuillDeltaStatic,
  RangeStatic as QuillRangeStatic,
  Sources as QuillSources,
} from 'react-quill/node_modules/@types/quill';

export { Quill };
export type DeltaStatic = QuillDeltaStatic;
export type QuillInstance = QuillType;
export type RangeStatic = QuillRangeStatic;
export type Sources = QuillSources;
export default ReactQuill;
