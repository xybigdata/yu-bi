import type { ComponentProps } from 'react';
import ReactQuill, { Quill } from 'react-quill';

type ReactQuillProps = ComponentProps<typeof ReactQuill>;
type ReactQuillInstance = InstanceType<typeof ReactQuill>;
type ReactQuillOnChange = NonNullable<ReactQuillProps['onChange']>;
type ReactQuillOnChangeSelection = NonNullable<
  ReactQuillProps['onChangeSelection']
>;

export { Quill };
export type DeltaStatic = Parameters<ReactQuillOnChange>[1];
export type QuillInstance = ReturnType<ReactQuillInstance['getEditor']>;
export type RangeStatic = NonNullable<
  Parameters<ReactQuillOnChangeSelection>[0]
>;
export type Sources = Parameters<ReactQuillOnChange>[2];
export default ReactQuill;
