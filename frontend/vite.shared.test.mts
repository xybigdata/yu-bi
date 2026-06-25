import path from 'path';

import { describe, expect, it } from 'vitest';

import {
  createReactBabelOptions,
  createReactPlugin,
  createVendorManualChunks,
  createViteAliases,
} from './vite.shared.mts';

const appRoot = __dirname;

describe('vite shared config', () => {
  it('creates the expected source aliases', () => {
    expect(createViteAliases(appRoot)).toEqual([
      { find: 'app', replacement: path.resolve(appRoot, 'src/app') },
      {
        find: 'entryPointFactory',
        replacement: path.resolve(appRoot, 'src/entryPointFactory.tsx'),
      },
      {
        find: 'globalConstants',
        replacement: path.resolve(appRoot, 'src/globalConstants.ts'),
      },
      { find: 'locales', replacement: path.resolve(appRoot, 'src/locales') },
      {
        find: /^redux\/(.+)/,
        replacement: path.resolve(appRoot, 'src/redux/$1'),
      },
      { find: 'styles', replacement: path.resolve(appRoot, 'src/styles') },
      { find: 'types', replacement: path.resolve(appRoot, 'src/types') },
      { find: 'utils', replacement: path.resolve(appRoot, 'src/utils') },
    ]);
  });

  it('keeps the styled-components Babel plugin in the React plugin config', () => {
    const plugins = createReactPlugin();

    expect(plugins.map(plugin => plugin.name)).toContain('vite:react-babel');
    expect(createReactBabelOptions()?.plugins).toContain(
      'babel-plugin-styled-components',
    );
  });

  it.each([
    ['node_modules/antd/es/button/index.js', 'antdDesign'],
    ['node_modules/@ant-design/pro-components/es/index.js', 'antdPro'],
    ['node_modules/@ant-design/pro-table/es/index.js', 'antdPro'],
    ['node_modules/@ant-design/icons/es/index.js', 'antdIcons'],
    ['node_modules/@ant-design/icons-svg/es/asn/PlusOutlined.js', 'antdIcons'],
    ['node_modules/@antv/s2/esm/index.js', 'antvS2'],
    ['node_modules/@antv/s2-react/esm/index.js', 'antvS2'],
    ['node_modules/@antv/g/esm/index.js', 'antvG'],
    ['node_modules/@antv/g-lite/esm/index.js', 'antvG'],
    ['node_modules/@antv/g-canvas/esm/index.js', 'antvG'],
    ['node_modules/@antv/g-math/esm/index.js', 'antvG'],
    ['node_modules/@antv/g-plugin-dragndrop/esm/index.js', 'antvG'],
    ['node_modules/@antv/g2/esm/index.js', 'antvG2'],
    ['node_modules/@antv/component/esm/index.js', 'antvG2'],
    ['node_modules/@antv/coord/esm/index.js', 'antvG2'],
    ['node_modules/@antv/scale/esm/index.js', 'antvG2'],
    ['node_modules/@antv/util/esm/index.js', 'antv'],
    ['node_modules/echarts/core.js', 'echarts'],
    ['node_modules/zrender/lib/core/util.js', 'echarts'],
    ['node_modules/quill/quill.js', 'quill'],
    ['node_modules/quill-delta/dist/Delta.js', 'quill'],
    ['node_modules/parchment/dist/parchment.js', 'quill'],
    ['node_modules/react-quill-new/lib/index.js', 'quill'],
    ['node_modules/react/index.js', 'react'],
    ['node_modules/react-dom/client.js', 'react'],
    ['node_modules/scheduler/index.js', 'react'],
    ['node_modules/react-grid-layout/build/index.js', 'reactGridLayout'],
    ['node_modules/reveal.js/dist/reveal.esm.js', 'reveal'],
    ['node_modules/flexlayout-react/lib/index.js', 'flexlayout'],
  ])('maps %s to the %s vendor chunk', (modulePath, chunkName) => {
    expect(createVendorManualChunks(path.resolve(appRoot, modulePath))).toBe(
      chunkName,
    );
  });

  it('leaves app files and uncategorized dependencies to Rollup defaults', () => {
    expect(
      createVendorManualChunks(path.resolve(appRoot, 'src/index.tsx')),
    ).toBe(undefined);
    expect(
      createVendorManualChunks(
        path.resolve(appRoot, 'node_modules/dayjs/dayjs.min.js'),
      ),
    ).toBe(undefined);
  });
});
