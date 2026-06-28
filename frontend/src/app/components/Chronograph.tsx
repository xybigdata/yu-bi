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

import { Badge, BadgeProps } from 'antd';
import { useCallback, useEffect, useRef, useState } from 'react';
import { yubiDayjs, getYuBiNowMillis } from 'app/utils/date';
import styled from 'styled-components';
import { FONT_SIZE_LABEL } from 'styles/StyleConstants';

interface ChronographProps {
  running: boolean;
  status: BadgeProps['status'];
}

export function Chronograph({ running, status }: ChronographProps) {
  const [label, setLabel] = useState('00:00:00.00');
  const intervalRef = useRef<ReturnType<typeof setInterval> | undefined>(
    undefined,
  );

  const formatElapsed = useCallback((elapsed: number) => {
    const centiseconds = String(Math.floor((elapsed % 1000) / 10)).padStart(
      2,
      '0',
    );
    return `${yubiDayjs.utc(elapsed).format('HH:mm:ss')}.${centiseconds}`;
  }, []);

  const clear = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = void 0;
    }
  }, []);

  useEffect(() => {
    if (running) {
      const start = getYuBiNowMillis();
      intervalRef.current = setInterval(() => {
        const current = getYuBiNowMillis();
        setLabel(formatElapsed(current - start));
      }, 10);
    } else {
      clear();
    }
    return clear;
  }, [running, clear, formatElapsed]);

  return <StyledBadge status={status} text={label} />;
}

const StyledBadge = styled(Badge)`
  .ant-badge-status-text {
    font-size: ${FONT_SIZE_LABEL};
    color: ${p => p.theme.textColorLight};
  }
`;
