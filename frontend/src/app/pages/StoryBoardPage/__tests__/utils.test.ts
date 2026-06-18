import { describe, expect, test } from 'vitest';
import { getStoryPageMapForm } from '../utils';
import type { StoryPageOfServer } from '../slice/types';

const createServerStoryPage = (config: string): StoryPageOfServer => ({
  id: 'story-page-1',
  relId: 'dashboard-1',
  storyboardId: 'story-1',
  relType: 'DASHBOARD',
  config,
});

describe('StoryBoardPage utils', () => {
  test('should migrate story page config from server string', () => {
    const pages = [
      createServerStoryPage(
        JSON.stringify({
          version: '',
          name: '第一页',
          thumbnail: 'thumb.png',
          index: 2,
          transitionEffect: {
            in: 'fade-in',
            out: 'fade-out',
            speed: 'slow',
          },
        }),
      ),
    ];

    getStoryPageMapForm(pages);

    expect(pages[0].config).toMatchObject({
      name: '第一页',
      index: 2,
      transitionEffect: {
        in: 'fade-in',
        out: 'fade-out',
        speed: 'slow',
      },
    });
  });

  test('should fallback to initial story page config when server config is invalid', () => {
    const pages = [createServerStoryPage('{bad json')];

    expect(() => getStoryPageMapForm(pages)).not.toThrow();
    expect(pages[0].config).toMatchObject({
      name: '',
      index: 0,
      transitionEffect: {
        in: 'fade-in',
        out: 'fade-out',
        speed: 'slow',
      },
    });
  });
});
