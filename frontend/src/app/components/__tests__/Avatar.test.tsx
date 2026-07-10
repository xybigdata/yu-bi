import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, test, vi } from 'vitest';
import { ThemeProvider } from 'styled-components';
import { themes } from 'styles/theme/themes';
import { Avatar, normalizeAvatarSrc } from '../Avatar';

function renderAvatar(element: React.ReactElement) {
  return render(<ThemeProvider theme={themes.light}>{element}</ThemeProvider>);
}

describe('Avatar', () => {
  test('normalizes empty or invalid avatar src', () => {
    expect(normalizeAvatarSrc('')).toBeUndefined();
    expect(normalizeAvatarSrc('/null')).toBeUndefined();
    expect(normalizeAvatarSrc('/undefined')).toBeUndefined();
    expect(normalizeAvatarSrc('/resources/user.png')).toBe(
      '/resources/user.png',
    );
  });

  test('renders fallback children when avatar path contains null', () => {
    renderAvatar(<Avatar src="/null">D</Avatar>);

    expect(screen.getByText('D')).toBeInTheDocument();
    expect(document.querySelector('img')).toBeNull();
  });

  test('renders fallback children when avatar path contains undefined', () => {
    renderAvatar(<Avatar src="/undefined">Y</Avatar>);

    expect(screen.getByText('Y')).toBeInTheDocument();
    expect(document.querySelector('img')).toBeNull();
  });

  test('falls back to children after image load failure', () => {
    const onError = vi.fn();
    renderAvatar(
      <Avatar src="/resources/missing-avatar.png" onError={onError}>
        A
      </Avatar>,
    );

    fireEvent.error(document.querySelector('img')!);

    expect(onError).toHaveBeenCalled();
    expect(screen.getByText('A')).toBeInTheDocument();
  });

  test('keeps image mode when custom onError requests it', () => {
    const onError = vi.fn(() => false);
    renderAvatar(
      <Avatar src="/resources/missing-avatar.png" onError={onError}>
        A
      </Avatar>,
    );

    const image = document.querySelector('img')!;
    fireEvent.error(image);

    expect(onError).toHaveBeenCalled();
    expect(document.querySelector('img')).toBe(image);
  });
});
