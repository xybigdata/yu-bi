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

import { Upload } from 'antd';
import type { UploadProps } from 'antd';
import type { UploadRef } from 'antd/es/upload/Upload';
import { uploadBoardImage } from 'app/pages/DashBoardPage/pages/BoardEditor/slice/thunk';
import {
  forwardRef,
  useCallback,
  useContext,
  useImperativeHandle,
  useRef,
} from 'react';
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
>(
  ({ onChange }: HiddenUploaderProps, ref) => {
    const dispatch = useAppDispatch();
    const uploadRef = useRef<UploadRef>(null);
    const { boardId } = useContext(BoardContext);

    useImperativeHandle(ref, () => ({
      onClick: () => {
        uploadRef.current?.nativeElement?.click();
      },
    }));

    const beforeUpload: UploadProps['beforeUpload'] = useCallback(
      async info => {
        const formData = new FormData();
        formData.append('file', info);
        dispatch(
          uploadBoardImage({
            boardId,
            fileName: info.name,
            formData: formData,
            resolve: onChange,
          }),
        );
        return false;
      },
      [boardId, dispatch, onChange],
    );

    return (
      <Upload
        accept=".jpg,.jpeg,.png,.gif,.svg"
        beforeUpload={beforeUpload}
        multiple={false}
        showUploadList={false}
        style={{ display: 'none' }}
        ref={uploadRef}
      />
    );
  },
);
