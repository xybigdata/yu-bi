import { render, screen, waitFor } from '@testing-library/react';
import React from 'react';
import { describe, expect, test, vi } from 'vitest';
import { User } from 'app/slice/types';
import { MemberForm } from '../MemberForm';

const members: User[] = [
  {
    id: 'member-1',
    username: 'chcy',
    name: 'chcy',
    email: 'chcy_188@allsaintsmusic.com',
    avatar: '',
    description: '',
  },
];

vi.mock('react-redux', () => ({
  useSelector: selector =>
    selector({
      main: { orgId: 'org-1' },
      member: {
        members,
        memberListLoading: false,
      },
    }),
}));

vi.mock('app/hooks/useRedux', () => ({
  useAppDispatch: () => vi.fn(),
}));

vi.mock('../../slice/thunks', () => ({
  getMembers: vi.fn(() => ({ type: 'member/getMembers' })),
}));

describe('MemberForm', () => {
  test('should render a wide transfer modal and keep full member title', async () => {
    render(
      <MemberForm
        title="添加成员"
        open
        initialValues={[]}
        onChange={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveStyle({ width: '800px' });

    await waitFor(() => {
      expect(document.querySelectorAll('.ant-transfer-section')[0]).toHaveStyle(
        {
          width: '360px',
          height: '320px',
        },
      );
    });

    expect(
      screen.getByTitle('chcy (chcy) chcy_188@allsaintsmusic.com'),
    ).toBeInTheDocument();
  });
});
