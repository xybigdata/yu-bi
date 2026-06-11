import { prefixI18N } from 'app/hooks/useI18NPrefix';
import RichTextEditor from 'app/components/ChartGraph/BasicRichText/RichTextEditor';
import { FC, ReactNode } from 'react';
import styled from 'styled-components';

export const Formats = [
  'header',
  'bold',
  'italic',
  'underline',
  'strike',
  'blockquote',
  'list',
  'bullet',
  'indent',
  'link',
  'color',
  'tag',
  'calcfield',
  'mention',
  'image',
];
interface CommonRichTextProps {
  children?: ReactNode;
  value?: string;
  onChange?: (v?: string) => void;
  placeholder?: string;
}
export const CommonRichText: FC<CommonRichTextProps> = ({
  children,
  placeholder = prefixI18N(
    'schedule.editor.emailSettingForm.commonRichText.pleaseEnter',
  ),
  ...restProps
}) => {
  return (
    <ReactQuillWrapper>
      <RichTextEditor
        theme="snow"
        placeholder={placeholder}
        formats={Formats}
        modules={{
          toolbar: [
            [{ header: [1, 2, false] }],
            ['bold', 'italic', 'underline', 'strike', 'blockquote'],
            [
              { list: 'ordered' },
              { list: 'bullet' },
              { indent: '-1' },
              { indent: '+1' },
            ],
            ['link', 'image'],
            ['clean'],
          ],
        }}
        {...restProps}
      />
    </ReactQuillWrapper>
  );
};

const ReactQuillWrapper = styled.div`
  .ql-container {
    min-height: 260px;
  }
`;
