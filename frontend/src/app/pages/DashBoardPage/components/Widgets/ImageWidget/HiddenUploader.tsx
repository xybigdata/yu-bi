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

import {
  ChangeEvent,
  CSSProperties,
  forwardRef,
  useCallback,
  useContext,
  useImperativeHandle,
  useRef,
} from 'react';
import { uploadBoardImage } from 'app/pages/DashBoardPage/pages/BoardEditor/slice/thunk';
import { useAppDispatch } from 'app/hooks/useRedux';
import { BoardContext } from '../../BoardProvider/BoardProvider';

interface HiddenUploaderProps {
  onChange: (url: string) => void;
}

export interface HiddenUploaderRef {
  onClick: () => void;
}

export const HiddenUploader = forwardRef<
  HiddenUploaderRef | undefined,
  HiddenUploaderProps
>(({ onChange }: HiddenUploaderProps, ref) => {
  const dispatch = useAppDispatch();
  const inputRef = useRef<HTMLInputElement>(null);
  const { boardId } = useContext(BoardContext);

  useImperativeHandle(ref, () => ({
    onClick: () => {
      inputRef.current?.click();
    },
  }));

  const uploadImage = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      const file = event.target.files?.[0];
      if (!file) {
        return;
      }

      const formData = new FormData();
      formData.append('file', file);
      dispatch(
        uploadBoardImage({
          boardId,
          fileName: file.name,
          formData: formData,
          resolve: onChange,
        }),
      );
      event.target.value = '';
    },
    [boardId, dispatch, onChange],
  );

  return (
    <input
      accept=".jpg,.jpeg,.png,.gif,.svg"
      onChange={uploadImage}
      ref={inputRef}
      style={hiddenInputStyle}
      tabIndex={-1}
      type="file"
    />
  );
});

const hiddenInputStyle: CSSProperties = {
  position: 'fixed',
  top: 0,
  left: -9999,
  width: 1,
  height: 1,
  opacity: 0,
  pointerEvents: 'none',
};
