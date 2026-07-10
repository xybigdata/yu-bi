import { Tag } from 'antd';
import { memo, ReactNode } from 'react';

interface ConditionalColorTagProps {
  color?: string;
  children: ReactNode;
}

const CONDITIONAL_COLOR_TAG_STYLE = {
  color: '#ffffff',
  marginRight: 8,
} as const;

const ConditionalColorTag = memo(
  ({ color, children }: ConditionalColorTagProps) => (
    <Tag
      className="conditional-style-color-tag"
      color={color}
      style={CONDITIONAL_COLOR_TAG_STYLE}
    >
      {children}
    </Tag>
  ),
);

export default ConditionalColorTag;
