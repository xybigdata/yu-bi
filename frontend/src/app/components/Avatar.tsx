import { Avatar as AntdAvatar, AvatarProps } from 'antd';
import endsWith from 'lodash/endsWith';
import {
  CSSProperties,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from 'react';
import styled from 'styled-components';

export function normalizeAvatarSrc(src: AvatarProps['src']) {
  if (typeof src !== 'string') {
    return src;
  }

  const trimmedSrc = src.trim();
  const lowerSrc = trimmedSrc.toLowerCase();

  if (
    !trimmedSrc ||
    endsWith(lowerSrc, 'null') ||
    endsWith(lowerSrc, 'undefined')
  ) {
    return undefined;
  }

  return trimmedSrc;
}

export function Avatar(props: AvatarProps) {
  const { src, size, style: propStyle, onError, ...rest } = props;
  const normalizedSrc = useMemo(() => normalizeAvatarSrc(src), [src]);
  const [safeSrc, setSafeSrc] = useState<AvatarProps['src']>(normalizedSrc);

  const style: CSSProperties = { ...propStyle };
  if (typeof size === 'number') {
    style.fontSize = `${size * 0.375}px`;
  }

  useEffect(() => {
    setSafeSrc(normalizedSrc);
  }, [normalizedSrc]);

  const handleError = useCallback(() => {
    const result = onError?.();

    if (result !== false) {
      setSafeSrc(undefined);
    }

    return result === false ? false : true;
  }, [onError]);

  return (
    <StyledAvatar
      {...rest}
      src={safeSrc}
      size={size}
      style={style}
      onError={handleError}
    >
      {props.children}
    </StyledAvatar>
  );
}

const StyledAvatar = styled(AntdAvatar)`
  &.ant-avatar {
    color: ${p => p.theme.textColorLight};
    background-color: ${p => p.theme.emphasisBackground};
  }
`;
