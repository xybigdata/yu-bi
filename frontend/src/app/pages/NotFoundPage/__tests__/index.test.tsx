import { HelmetProvider } from 'react-helmet-async';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'app/routerCompat';
import { NotFoundPage } from '..';

const renderPage = () =>
  render(
    <MemoryRouter>
      <HelmetProvider>
        <NotFoundPage />
      </HelmetProvider>
    </MemoryRouter>,
  );

describe('<NotFoundPage />', () => {
  it('should render 404 title', () => {
    renderPage();
    expect(
      screen.getByRole('heading', { level: 1, name: /404/i }),
    ).toBeInTheDocument();
    expect(screen.getByText('Page not found')).toBeInTheDocument();
  });
});
