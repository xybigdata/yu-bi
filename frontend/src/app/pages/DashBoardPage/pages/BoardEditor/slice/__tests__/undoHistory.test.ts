import { describe, expect, test } from 'vitest';
import { BOARD_UNDO } from 'app/pages/DashBoardPage/constants';
import { Dashboard } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { editBoardStackActions, editBoardStackReducer } from '..';
import { EditBoardStack } from '../types';

const createDashboard = (id: string): Dashboard =>
  ({
    id,
    name: id,
    orgId: 'org-1',
    queryVariables: [],
    status: 1,
    thumbnail: '',
    config: {
      jsonConfig: {
        props: [],
      },
    },
  }) as unknown as Dashboard;

const createStack = (id: string): EditBoardStack => ({
  dashBoard: createDashboard(id),
  widgetRecord: {},
});

const createInitialHistory = () =>
  editBoardStackReducer(undefined, { type: '@@INIT' });

describe('editBoardStack undo history', () => {
  test('should keep initial board setup out of undo history', () => {
    const state = editBoardStackReducer(
      createInitialHistory(),
      editBoardStackActions.setBoardToEditStack(createStack('initial')),
    );

    expect(state.present.dashBoard.id).toBe('initial');
    expect(state.past).toHaveLength(0);
    expect(state.future).toHaveLength(0);
  });

  test('should track whitelisted board updates and support undo redo', () => {
    const initialState = editBoardStackReducer(
      createInitialHistory(),
      editBoardStackActions.setBoardToEditStack(createStack('initial')),
    );

    const updatedState = editBoardStackReducer(
      initialState,
      editBoardStackActions.updateBoard(createDashboard('updated')),
    );

    expect(updatedState.present.dashBoard.id).toBe('updated');
    expect(updatedState.past).toHaveLength(1);

    const undoneState = editBoardStackReducer(updatedState, {
      type: BOARD_UNDO.undo,
    });

    expect(undoneState.present.dashBoard.id).toBe('initial');
    expect(undoneState.future).toHaveLength(1);

    const redoneState = editBoardStackReducer(undoneState, {
      type: BOARD_UNDO.redo,
    });

    expect(redoneState.present.dashBoard.id).toBe('updated');
    expect(redoneState.past).toHaveLength(1);
  });

  test('should ignore non-whitelisted stack actions in undo history', () => {
    const initialState = editBoardStackReducer(
      createInitialHistory(),
      editBoardStackActions.setBoardToEditStack(createStack('initial')),
    );

    const updatedState = editBoardStackReducer(
      initialState,
      editBoardStackActions.updateQueryVariables([]),
    );

    expect(updatedState.present.dashBoard.queryVariables).toEqual([]);
    expect(updatedState.past).toHaveLength(0);
    expect(updatedState.future).toHaveLength(0);
  });
});
