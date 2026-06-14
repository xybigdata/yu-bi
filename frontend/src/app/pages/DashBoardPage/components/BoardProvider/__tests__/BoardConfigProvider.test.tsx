import { render } from '@testing-library/react';
import { useContext } from 'react';
import { describe, expect, test } from 'vitest';
import { initAutoBoardConfig } from 'app/pages/DashBoardPage/utils/autoBoard';
import {
  BoardConfigProvider,
  BoardConfigValContext,
} from '../BoardConfigProvider';

const ContextReader = () => {
  const value = useContext(BoardConfigValContext);
  return <pre data-testid="board-config-value">{JSON.stringify(value)}</pre>;
};

describe('BoardConfigProvider', () => {
  test('should read mobile spacing from mSpace group', () => {
    const config = initAutoBoardConfig();
    const mSpace = config.jsonConfig.props.find(item => item.key === 'mSpace');

    mSpace?.rows?.find(row => row.key === 'paddingTB') && (mSpace.rows.find(row => row.key === 'paddingTB')!.value = 101);
    mSpace?.rows?.find(row => row.key === 'paddingLR') && (mSpace.rows.find(row => row.key === 'paddingLR')!.value = 102);
    mSpace?.rows?.find(row => row.key === 'marginTB') && (mSpace.rows.find(row => row.key === 'marginTB')!.value = 103);
    mSpace?.rows?.find(row => row.key === 'marginLR') && (mSpace.rows.find(row => row.key === 'marginLR')!.value = 104);

    const { getByTestId } = render(
      <BoardConfigProvider config={config} boardId="board-1">
        <ContextReader />
      </BoardConfigProvider>,
    );

    expect(JSON.parse(getByTestId('board-config-value').textContent || '{}'))
      .toMatchObject({
        mPadding: [102, 101],
        mMargin: [104, 103],
      });
  });
});
