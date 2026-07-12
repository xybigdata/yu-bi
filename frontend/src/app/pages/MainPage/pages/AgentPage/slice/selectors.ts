import { createSelector } from '@reduxjs/toolkit';
import type { RootState } from 'types';
import { initialState } from '.';

export const selectAgentWorkspace = (state: RootState) =>
  state.agentWorkspace || initialState;

export const selectAgentSession = createSelector(
  selectAgentWorkspace,
  state => state.session,
);

export const selectAgentApprovals = createSelector(
  selectAgentWorkspace,
  state => state.approvals,
);

export const selectAgentPreviewIntent = createSelector(
  selectAgentWorkspace,
  state => state.previewIntent,
);
