import { render, screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { describe, expect, test } from 'vitest';
import { MemoryRouter, useParams } from '../routerCompat';

const ProjectRoute = () => {
  const params = useParams<{ orgId: string; projectId: string }>();
  return (
    <div>
      {params.orgId}/{params.projectId}
    </div>
  );
};

describe('routerCompat', () => {
  test('should render routes through the compatibility memory router', () => {
    render(
      <MemoryRouter initialEntries={['/route']}>
        <Routes>
          <Route path="/route" element={<div>route</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('route')).toBeInTheDocument();
  });

  test('should keep typed route params helper', () => {
    render(
      <MemoryRouter initialEntries={['/org/o-1/project/p-1']}>
        <Routes>
          <Route
            path="/org/:orgId/project/:projectId"
            element={<ProjectRoute />}
          />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('o-1/p-1')).toBeInTheDocument();
  });
});
