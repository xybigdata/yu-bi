/**
 * Datart
 *
 * Copyright 2021
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

import { SelectOutlined } from '@ant-design/icons';
import { Button, Dropdown, MenuProps, Modal, Row } from 'antd';
import debounce from 'lodash/debounce';
import {
  FC,
  memo,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import styled from 'styled-components';
import { BLUE } from 'styles/StyleConstants';
import { normalizeRichTextValue, parseRichTextContent } from './content';
import RichTextEditor, { RichTextEditorHandle } from './RichTextEditor';
import type { DeltaStatic } from './quillCompat';
import { CustomColor, QuillPalette } from './RichTextPluginLoader/CustomColor';
import { Formats } from './RichTextPluginLoader/RichTextConfig';
import {
  createRichTextColorHandlers,
  createRichTextModules,
  getRichTextContainerId,
  translateRichTextCalcFields,
  useRichTextMarkdownModule,
  useRichTextPalette,
  type RichTextColorType,
  type RichTextFieldReference,
  type RichTextModules,
} from './runtimeHelpers';

type RichTextField = RichTextFieldReference & { id?: string };

const ChartRichTextAdapter: FC<{
  dataList: RichTextField[];
  id: string;
  isEditing?: boolean;
  initContent: string | undefined;
  onChange: (delta: string | undefined) => void;
  openQuillMarkdown?: boolean;
  t?: (
    key: string,
    disablePrefix?: boolean,
    options?: Record<string, unknown>,
  ) => string | undefined;
}> = memo(
  ({
    dataList,
    id,
    isEditing = false,
    initContent,
    onChange,
    openQuillMarkdown = false,
    t,
  }) => {
    const [open, setOpen] = useState<boolean>(false);
    const [quillValue, setQuillValue] = useState<DeltaStatic | string>('');
    const [translate, setTranslate] = useState<DeltaStatic | string>('');

    const quillRef = useRef<RichTextEditorHandle>(null);
    const quillEditRef = useRef<RichTextEditorHandle>(null);

    const [customColorVisible, setCustomColorVisible] =
      useState<boolean>(false);
    const [customColor, setCustomColor] = useState<{
      background: string;
      color: string;
    }>({ ...QuillPalette.RICH_TEXT_CUSTOM_COLOR_INIT });
    const [customColorType, setCustomColorType] =
      useState<RichTextColorType>('color');

    const containerId = useMemo(() => getRichTextContainerId(id), [id]);
    const quillColorHandlers = useMemo(
      () =>
        createRichTextColorHandlers({
          editorRef: quillEditRef,
          onOpenCustomColor: type => {
            setCustomColorType(type);
            setCustomColorVisible(true);
          },
        }),
      [],
    );
    const quillModules = useMemo<RichTextModules>(
      () =>
        createRichTextModules({
          containerId,
          handlers: quillColorHandlers,
          editable: isEditing,
          includeCalcField: true,
        }),
      [containerId, isEditing, quillColorHandlers],
    );

    useEffect(() => {
      setQuillValue(parseRichTextContent(initContent));
    }, [initContent]);

    const debouncedDataChange = useMemo(
      () =>
        debounce(value => {
          onChange?.(value);
        }, 300),
      [onChange],
    );

    useEffect(() => {
      return () => {
        debouncedDataChange.cancel();
      };
    }, [debouncedDataChange]);

    const quillChange = useCallback(() => {
      if (quillEditRef.current) {
        const contents = normalizeRichTextValue(
          quillEditRef.current.getContents(),
        );
        setQuillValue(contents);
        contents && debouncedDataChange(JSON.stringify(contents));
      }
    }, [debouncedDataChange]);

    useEffect(() => {
      setTranslate(translateRichTextCalcFields(quillValue, dataList));
    }, [quillValue, dataList]);

    useRichTextPalette({
      editorRef: quillEditRef,
      containerId,
      onChange: setCustomColor,
    });
    useRichTextMarkdownModule({
      editorRef: quillEditRef,
      enabled: openQuillMarkdown,
    });

    const customColorChange = (color: string | boolean) => {
      if (typeof color === 'string') {
        quillEditRef.current?.format(customColorType, color);
      }
      setCustomColorVisible(false);
    };

    const reactQuillView = useMemo(
      () =>
        (!isEditing || open) && (
          <RichTextEditor
            ref={quillRef}
            className="react-quill-view"
            placeholder=""
            value={translate}
            modules={{ toolbar: null }}
            formats={Formats}
            readOnly={true}
          />
        ),
      [translate, quillRef, isEditing, open],
    );

    const selectField = useCallback(
      (field: RichTextField | undefined) => () => {
        if (!field || !quillEditRef.current?.isReady()) {
          return;
        }

        const text = `[${field.name}]`;
        quillEditRef.current.insertCalcFieldItem(
          {
            denotationChar: '',
            id: field.id,
            name: field.name,
            value: field.value,
            text,
          },
          true,
        );

        queueMicrotask(() => {
          if (!quillEditRef.current?.isReady()) {
            return;
          }
          setQuillValue(
            normalizeRichTextValue(quillEditRef.current.getContents()),
          );
        });
      },
      [],
    );

    const fieldItems = useMemo<MenuProps['items']>(() => {
      return dataList?.length
        ? dataList.map(fieldName => ({
            key: fieldName.name,
            label: fieldName.name,
          }))
        : [
            {
              key: 'nodata',
              label: t?.('common.noData'),
              disabled: true,
            },
          ];
    }, [dataList, t]);

    const handleFieldMenuClick = useCallback<NonNullable<MenuProps['onClick']>>(
      ({ key, domEvent }) => {
        domEvent.preventDefault();
        const field = dataList?.find(item => item.name === key);
        field && selectField(field)();
      },
      [dataList, selectField],
    );

    const toolbar = useMemo(
      () =>
        QuillPalette.getToolbar({
          id: containerId,
          extendNodes: {
            4: (
              <Dropdown
                menu={{
                  items: fieldItems,
                  onClick: handleFieldMenuClick,
                }}
                trigger={['click']}
                key="ql-selectLink"
              >
                <a
                  className="selectLink"
                  href="#javascript;"
                  title={t?.('common.referenceFields')}
                >
                  <SelectOutlined />
                </a>
              </Dropdown>
            ),
          },
          t,
        }),
      [containerId, fieldItems, handleFieldMenuClick, t],
    );

    const reactQuillEdit = useMemo(
      () =>
        isEditing && (
          <>
            {toolbar}
            <RichTextEditor
              ref={quillEditRef}
              className="react-quill"
              placeholder={t?.('viz.board.setting.enterHere')}
              defaultValue={quillValue}
              onChange={quillChange}
              modules={quillModules}
              formats={Formats}
              readOnly={false}
              bounds={`#quill-box-${id}`}
            />
            <Row align="middle" justify="end" style={{ paddingTop: 16 }}>
              <Button
                onClick={() => {
                  setOpen(true);
                }}
                type="primary"
              >
                {t?.('common.preview')}
              </Button>
            </Row>
          </>
        ),
      [quillModules, quillValue, isEditing, toolbar, quillChange, id, t],
    );

    const ssp = (e: React.MouseEvent<HTMLDivElement>) => {
      e.stopPropagation();
    };

    return (
      <TextWrap onClick={ssp}>
        <QuillBox id={`quill-box-${id}`}>
          {reactQuillEdit}
          {!isEditing && reactQuillView}
        </QuillBox>
        <Modal
          title={t?.('common.richTextPreview')}
          open={open}
          footer={null}
          width="80%"
          getContainer={false}
          onCancel={() => {
            setOpen(false);
          }}
        >
          {isEditing && reactQuillView}
        </Modal>
        <CustomColor
          open={customColorVisible}
          onCancel={() => setCustomColorVisible(false)}
          color={customColor?.[customColorType]}
          colorChange={customColorChange}
        />
      </TextWrap>
    );
  },
);
export default ChartRichTextAdapter;

const QuillBox = styled.div`
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  .react-quill {
    flex: 1;
    overflow-y: auto;
  }
  .react-quill-view {
    flex: 1;
    overflow-y: auto;
  }
`;
const TextWrap = styled.div`
  width: 100%;
  height: 100%;

  & .ql-snow {
    border: none;
  }

  & .ql-container.ql-snow {
    border: none;
  }

  & .selectLink {
    display: inline-block;
    width: 28px;
    height: 24px;
    padding: 0 5px;

    color: black;

    i {
      font-size: 16px;
    }
  }

  & .selectLink:hover {
    color: ${BLUE};
  }
`;
