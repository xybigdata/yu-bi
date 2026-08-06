import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import { Tooltip } from 'antd';
import styled from 'styled-components';
import { LEVEL_20 } from 'styles/StyleConstants';

interface SidebarCollapseButtonProps {
  collapsed: boolean;
  expandLabel: string;
  collapseLabel: string;
  onToggle: (collapsed: boolean) => void;
}

export function SidebarCollapseButton({
  collapsed,
  expandLabel,
  collapseLabel,
  onToggle,
}: SidebarCollapseButtonProps) {
  const label = collapsed ? expandLabel : collapseLabel;

  return (
    <Tooltip title={label} placement="right">
      <ToggleButton
        $collapsed={collapsed}
        type="button"
        aria-label={label}
        onClick={() => onToggle(!collapsed)}
      >
        {collapsed ? <RightOutlined /> : <LeftOutlined />}
      </ToggleButton>
    </Tooltip>
  );
}

const ToggleButton = styled.button<{ $collapsed: boolean }>`
  position: absolute;
  top: 50%;
  right: ${p => (p.$collapsed ? '-24px' : '0')};
  z-index: ${LEVEL_20};
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 48px;
  padding: 0;
  color: ${p => p.theme.textColorLight};
  cursor: pointer;
  background-color: ${p => p.theme.componentBackground};
  border: 1px solid ${p => p.theme.borderColorSplit};
  border-radius: 4px;
  box-shadow: ${p => p.theme.shadowBlock};
  transform: translateY(-50%);

  &:hover,
  &:focus-visible {
    color: ${p => p.theme.primary};
    outline: none;
    border-color: ${p => p.theme.primary};
  }
`;
