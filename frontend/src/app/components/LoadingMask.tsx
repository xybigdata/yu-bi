import { LoadingOutlined } from '@ant-design/icons';
import { ReactElement } from 'react';
import styled, { createGlobalStyle } from 'styled-components';
import { LEVEL_1 } from 'styles/StyleConstants';
import { cloneElementWithClassName } from 'utils/reactCompat';

interface LoadingMaskProps {
  loading: boolean;
  children: ReactElement;
}

export function LoadingMask({ loading, children }: LoadingMaskProps) {
  return (
    <>
      {loading && (
        <SpinWrapper>
          <LoadingOutlined />
        </SpinWrapper>
      )}
      {loading ? cloneElementWithClassName(children, 'blur') : children}
      <LoadingMaskStyle />
    </>
  );
}

const SpinWrapper = styled.div`
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: ${LEVEL_1};
  display: flex;
  align-items: center;
  justify-content: center;
`;

export const LoadingMaskStyle = createGlobalStyle`
  .blur {
    filter: blur(2px);
  }
`;
