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
import { Modal } from 'antd';
import {
  CustomColor,
  QuillPalette,
} from 'app/components/ChartGraph/BasicRichText/RichTextPluginLoader/CustomColor';
import {
  normalizeRichTextValue,
} from 'app/components/ChartGraph/BasicRichText/content';
import RichTextEditor, {
  RichTextEditorHandle,
} from 'app/components/ChartGraph/BasicRichText/RichTextEditor';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { WidgetInfo } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { editBoardStackActions } from 'app/pages/DashBoardPage/pages/BoardEditor/slice';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { getDatartNowMillis } from 'app/utils/date';
import produce from 'immer';
import React, {
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { useAppDispatch } from 'app/hooks/useRedux';
import type { DeltaStatic } from 'app/components/ChartGraph/BasicRichText/quillCompat';
import styled from 'styled-components';
import { SPACE_TIMES } from 'styles/StyleConstants';
import { WidgetActionContext } from '../../ActionProvider/WidgetActionProvider';
import { Formats, MarkdownOptions } from './config';

type RichTextValue = DeltaStatic | string;

type RichTextWidgetProps = {
  widget: Widget;
  widgetInfo: WidgetInfo;
  boardEditing: boolean;
};
export const RichTextWidgetCore: React.FC<RichTextWidgetProps> = ({
  widget,
  widgetInfo,
  boardEditing,
}) => {
  const t = useI18NPrefix();
  const dispatch = useAppDispatch();

  const { onEditClearActiveWidgets } = useContext(WidgetActionContext);
  const initContent = useMemo(() => {
    return normalizeRichTextValue((widget.config.content as any).richText?.content);
  }, [widget.config.content]);
  const [quillValue, setQuillValue] = useState<RichTextValue>(
    initContent,
  );
  const [containerId, setContainerId] = useState<string>();
  const [quillModules, setQuillModules] = useState<any>(null);
  const markdownModuleRef = useRef<{ destroy: () => void } | null>(null);

  const [customColorVisible, setCustomColorVisible] = useState<boolean>(false);
  const [customColor, setCustomColor] = useState<{
    background: string;
    color: string;
  }>({ ...QuillPalette.RICH_TEXT_CUSTOM_COLOR_INIT });
  const [customColorType, setCustomColorType] = useState<
    'color' | 'background'
  >('color');
  const [contentSavable, setContentSavable] = useState(false);

  useEffect(() => {
    if (widgetInfo.editing) {
      setQuillValue(initContent);
    }
  }, [initContent, widgetInfo.editing]);

  useEffect(() => {
    if (widgetInfo.editing === false && contentSavable && boardEditing) {
      if (quillRef.current) {
        const contents = normalizeRichTextValue(quillRef.current.getContents());
        if (typeof contents === 'string') {
          setContentSavable(false);
          return;
        }
        const strContents = JSON.stringify(contents);
        if (strContents !== JSON.stringify(initContent)) {
          const nextMediaWidgetContent = produce(
            widget.config.content,
            draft => {
              (draft as any).richText = {
                content: JSON.parse(strContents || '{}'),
              };
            },
          ) as any;

          dispatch(
            editBoardStackActions.changeMediaWidgetConfig({
              id: widget.id,
              mediaWidgetContent: nextMediaWidgetContent,
            }),
          );
          setContentSavable(false);
        }
      }
    }
  }, [
    boardEditing,
    dispatch,
    initContent,
    contentSavable,
    widget.config.content,
    widget.id,
    widgetInfo.editing,
  ]);

  useEffect(() => {
    const newId = `rich-text-${widgetInfo.id + getDatartNowMillis()}`;
    setContainerId(newId);
    const modules = {
      toolbar: {
        container: `#${newId}`,
        handlers: {
          color: function (value) {
            if (value === QuillPalette.RICH_TEXT_CUSTOM_COLOR) {
              setCustomColorType('color');
              setCustomColorVisible(true);
            }
            quillRef.current!.format('color', value);
          },
          background: function (value) {
            if (value === QuillPalette.RICH_TEXT_CUSTOM_COLOR) {
              setCustomColorType('background');
              setCustomColorVisible(true);
            }
            quillRef.current!.format('background', value);
          },
        },
      },
      imageDrop: true,
    };
    setQuillModules(modules);
  }, [widgetInfo.id]);

  const quillRef = useRef<RichTextEditorHandle>(null);

  useLayoutEffect(() => {
    if (quillRef.current) {
      markdownModuleRef.current?.destroy();
      markdownModuleRef.current =
        quillRef.current.createMarkdownModule(MarkdownOptions);
    }

    return () => {
      markdownModuleRef.current?.destroy();
      markdownModuleRef.current = null;
    };
  }, [quillModules]);

  useEffect(() => {
    let palette: QuillPalette | null = null;
    if (quillRef.current && containerId) {
      palette = new QuillPalette(quillRef.current, {
        toolbarId: containerId,
        onChange: setCustomColor,
      });
    }

    return () => {
      palette?.destroy();
    };
  }, [containerId]);

  const ssp = e => {
    e.stopPropagation();
  };

  const quillChange = useCallback(() => {
    if (quillRef.current) {
      const contents = normalizeRichTextValue(quillRef.current.getContents());
      setQuillValue(contents);
    }
  }, []);

  const toolbar = useMemo(
    () => QuillPalette.getToolbar({ id: containerId as string, t }),
    [containerId, t],
  );

  const customColorChange = color => {
    if (color) {
      quillRef.current!.format(customColorType, color);
    }
    setCustomColorVisible(false);
  };

  const modalCancel = useCallback(() => {
    onEditClearActiveWidgets();
  }, [onEditClearActiveWidgets]);

  const modalOk = useCallback(() => {
    setContentSavable(true);
    modalCancel();
  }, [modalCancel]);

  return (
    <TextWrap onClick={ssp}>
      <RichTextEditor
        className="react-quill"
        value={initContent}
        modules={{ toolbar: null }}
        formats={Formats}
        readOnly={true}
      />
      <CustomColor
        open={customColorVisible}
        onCancel={() => setCustomColorVisible(false)}
        color={customColor?.[customColorType]}
        colorChange={customColorChange}
      />
      <Modal
        width={992}
        closable={false}
        maskClosable={false}
        keyboard={false}
        open={widgetInfo.editing}
        onOk={modalOk}
        onCancel={modalCancel}
      >
        {quillModules && (
          <ModalBody>
            {toolbar}
            <RichTextEditor
              ref={quillRef}
              className="react-quill"
              placeholder={t('viz.board.setting.enterHere')}
              value={quillValue}
              onChange={quillChange}
              modules={quillModules}
              formats={Formats}
              readOnly={false}
            />
          </ModalBody>
        )}
      </Modal>
    </TextWrap>
  );
};
export default RichTextWidgetCore;

const TextWrap = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;

  & .react-quill {
    width: 100%;
    height: 100%;
  }

  & .ql-snow {
    border: none;
  }

  & .ql-container.ql-snow {
    border: none;
  }

  & .ql-editor {
    position: absolute;
    top: 50%;
    left: 0;
    width: 100%;
    height: auto;
    max-height: 100%;
    transform: translate(0, -50%);
  }
`;

const ModalBody = styled.div`
  & .ql-editor {
    min-height: ${SPACE_TIMES(60)};
  }
`;
