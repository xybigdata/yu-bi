import { CommonFormTypes } from 'globalConstants';
import { createContext, useCallback, useState } from 'react';
import { BoardType } from '../../../DashBoardPage/pages/Board/slice/types';
import { VizType } from './slice/types';

export type TemplateUploadValue = File | FormData;

export interface SaveFormModel {
  id?: string;
  name: string;
  boardType?: BoardType; //template
  config?: string;
  description?: string;
  parentId?: string | null;
  viewId?: string;
  file?: TemplateUploadValue; //template
  subType?: string; //board
  avatar?: string; //datachart
}

type SaveFormInitialValues = Partial<SaveFormModel>;

interface SaveFormState {
  vizType: VizType;
  type: CommonFormTypes;
  open: boolean;
  isSaveAs?: boolean;
  initialValues?: SaveFormInitialValues;
  onSave: (values: SaveFormModel, onClose: () => void) => void;
  onAfterClose?: () => void;
}

interface SaveFormContextValue extends SaveFormState {
  onCancel: () => void;
  showSaveForm: (formState: SaveFormState) => void;
}

const saveFormContextValue: SaveFormContextValue = {
  vizType: 'FOLDER',
  type: CommonFormTypes.Add,
  open: false,
  isSaveAs: false,
  onSave: () => {},
  onCancel: () => {},
  showSaveForm: () => {},
};
export const SaveFormContext = createContext(saveFormContextValue);
export const useSaveFormContext = (): SaveFormContextValue => {
  const [vizType, setVizType] = useState<VizType>('FOLDER');
  const [type, setType] = useState(CommonFormTypes.Add);
  const [open, setOpen] = useState(false);
  const [initialValues, setInitialValues] = useState<
    undefined | SaveFormInitialValues
  >();
  const [onSave, setOnSave] = useState(() => () => {});
  const [onAfterClose, setOnAfterClose] = useState(() => () => {});

  const onCancel = useCallback(() => {
    setOpen(false);
  }, [setOpen]);

  const showSaveForm = useCallback(
    ({
      vizType,
      type,
      open,
      initialValues,
      onSave,
      onAfterClose,
    }: SaveFormState) => {
      setVizType(vizType);
      setType(type);
      setOpen(open);
      setInitialValues(initialValues);
      setOnSave(() => onSave);
      setOnAfterClose(() => onAfterClose);
    },
    [],
  );

  return {
    vizType,
    type,
    open,
    initialValues,
    onSave,
    onCancel,
    onAfterClose,
    showSaveForm,
  };
};
