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
import { FONT_FAMILIES, FONT_SIZES } from 'globalConstants';
import debounce from 'lodash/debounce';
import {
  FC,
  memo,
  useCallback,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import 'react-quill/dist/quill.bubble.css';
import 'react-quill/dist/quill.core.css';
import styled from 'styled-components';
import { BLUE } from 'styles/StyleConstants';
import './RichTextPluginLoader';
import RichTextEditor, { RichTextEditorHandle } from './RichTextEditor';
import { DeltaStatic, Quill } from './quillCompat';
import { ImageDropModule } from './modules/ImageDropModule';
import { CustomColor, QuillPalette } from './RichTextPluginLoader/CustomColor';
import {
  Formats,
  MarkdownOptions,
} from './RichTextPluginLoader/RichTextConfig';
import TagBlot from './RichTextPluginLoader/TagBlot';

Quill.register('modules/imageDrop', ImageDropModule);
Quill.register('formats/tag', TagBlot);
const size = Quill.import('attributors/style/size');
size.whitelist = FONT_SIZES.map(fontSize => `${fontSize}px`);
Quill.register(size, true);

const font = Quill.import('attributors/style/font');
font.whitelist = FONT_FAMILIES.map(font => font.value);
Quill.register(font, true);

const ChartRichTextAdapter: FC<{
  dataList: Array<{ id: string | undefined; name: string; value: string }>;
  id: string;
  isEditing?: boolean;
  initContent: string | undefined;
  onChange: (delta: string | undefined) => void;
  openQuillMarkdown?: boolean;
  t?: (key: string, disablePrefix?: boolean, options?: any) => any;
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
    const [containerId, setContainerId] = useState<string>();
    const [quillModules, setQuillModules] = useState<any>(null);
    const [open, setOpen] = useState<boolean>(false);
    const [quillValue, setQuillValue] = useState<DeltaStatic | string>('');
    const [translate, setTranslate] = useState<DeltaStatic | string>('');

    const quillMarkdownConfigRef = useRef<{ destroy: () => void } | null>(null);
    const quillRef = useRef<RichTextEditorHandle>(null);
    const quillEditRef = useRef<RichTextEditorHandle>(null);

    const [customColorVisible, setCustomColorVisible] =
      useState<boolean>(false);
    const [customColor, setCustomColor] = useState<{
      background: string;
      color: string;
    }>({ ...QuillPalette.RICH_TEXT_CUSTOM_COLOR_INIT });
    const [customColorType, setCustomColorType] = useState<
      'color' | 'background'
    >('color');

    useEffect(() => {
      const value = (initContent && JSON.parse(initContent || '{}')) || '';
      setQuillValue(value);
    }, [initContent]);

    useEffect(() => {
      if (id) {
        const newId = `rich-text-${id}`;
        setContainerId(newId);
        const modules = {
          toolbar: {
            container: isEditing ? `#${newId}` : null,
            handlers: {
              color: function (value) {
                if (value === QuillPalette.RICH_TEXT_CUSTOM_COLOR) {
                  setCustomColorType('color');
                  setCustomColorVisible(true);
                }
                quillEditRef.current!.format('color', value);
              },
              background: function (value) {
                if (value === QuillPalette.RICH_TEXT_CUSTOM_COLOR) {
                  setCustomColorType('background');
                  setCustomColorVisible(true);
                }
                quillEditRef.current!.format('background', value);
              },
            },
          },
          calcfield: {},
          imageDrop: true,
        };
        setQuillModules(modules);
      }
    }, [id, isEditing]);

    const debouncedDataChange = useMemo(
      () =>
        debounce(value => {
          onChange?.(value);
        }, 300),
      [onChange],
    );

    const quillChange = useCallback(() => {
      if (quillEditRef.current) {
        const contents = quillEditRef.current.getContents();
        setQuillValue(contents);
        contents && debouncedDataChange(JSON.stringify(contents));
      }
    }, [debouncedDataChange]);

    useEffect(() => {
      if (typeof quillValue !== 'string') {
        const quill = Object.assign({}, quillValue);
        const ops = quill.ops?.concat().map(item => {
          let insert = item.insert;
          if (typeof insert !== 'string') {
            if (insert.hasOwnProperty('calcfield')) {
              const name = insert.calcfield?.name;
              const config = name
                ? dataList.find(items => items.name === name)
                : null;
              insert = config?.value;
            }
          }
          return { ...item, insert };
        });
        setTranslate(ops?.length ? { ...quill, ops } : '');
      } else {
        setTranslate(quillValue);
      }
    }, [quillValue, dataList, setTranslate]);

    useEffect(() => {
      let palette: QuillPalette | null = null;
      if (quillEditRef.current && containerId) {
        palette = new QuillPalette(quillEditRef.current, {
          toolbarId: containerId,
          onChange: setCustomColor,
        });
      }

      return () => {
        palette?.destroy();
      };
    }, [containerId]);

    useLayoutEffect(() => {
      if (quillEditRef.current) {
        if (openQuillMarkdown) {
          quillMarkdownConfigRef.current?.destroy();
          quillMarkdownConfigRef.current =
            quillEditRef.current.createMarkdownModule(MarkdownOptions);
        } else {
          quillMarkdownConfigRef.current?.destroy();
          quillMarkdownConfigRef.current = null;
        }
      }

      return () => {
        quillMarkdownConfigRef.current?.destroy();
        quillMarkdownConfigRef.current = null;
      };
    }, [openQuillMarkdown, quillModules]);

    const customColorChange = color => {
      if (color) {
        quillEditRef.current!.format(customColorType, color);
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
      (field: any) => () => {
        if (quillEditRef.current) {
          if (field) {
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
            setImmediate(() => {
              setQuillValue(quillEditRef.current?.getContents() || '');
            });
          }
        }
      },
      [quillEditRef],
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

    const handleFieldMenuClick = useCallback<
      NonNullable<MenuProps['onClick']>
    >(
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
          id: containerId as string,
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

    const ssp = e => {
      e.stopPropagation();
    };

    return (
      <TextWrap onClick={ssp}>
        <QuillBox id={`quill-box-${id}`}>
          {quillModules && reactQuillEdit}
          {quillModules && !isEditing && reactQuillView}
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
