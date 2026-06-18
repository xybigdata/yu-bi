import { render } from '@testing-library/react';
import React from 'react';
import { vi } from 'vitest';

vi.mock('../../../WidgetProvider/WidgetProvider', () => ({
  WidgetContext: React.createContext({}),
}));

import { WidgetContext } from '../../../WidgetProvider/WidgetProvider';
import { VideoWidgetCore } from '../VideoWidgetCore';
import { videoWidgetToolKit } from '../videoConfig';

describe('VideoWidgetCore', () => {
  it('应使用原生 video 渲染视频源', () => {
    const widget = videoWidgetToolKit.create({});
    widget.config.customConfig.props![0].rows![0].value =
      'https://example.com/demo.mp4';

    const { container } = render(
      <WidgetContext.Provider value={widget}>
        <VideoWidgetCore />
      </WidgetContext.Provider>,
    );

    const video = container.querySelector('video');
    expect(video).not.toBeNull();
    if (!video) {
      throw new Error('video 元素未渲染');
    }
    expect(video.tagName).toBe('VIDEO');
    expect(video).toHaveAttribute('controls');

    const source = video.querySelector('source');
    expect(source).not.toBeNull();
    expect(source).toHaveAttribute('src', 'https://example.com/demo.mp4');
  });
});
