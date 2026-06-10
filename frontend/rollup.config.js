/* eslint-disable import/no-anonymous-default-export */
import { babel } from '@rollup/plugin-babel';
import commonjs from '@rollup/plugin-commonjs';
import json from '@rollup/plugin-json';
import { nodeResolve } from '@rollup/plugin-node-resolve';
import replace from '@rollup/plugin-replace';
import path from 'path';

export default {
  input: 'src/task.ts', // 打包入口
  output: {
    // 打包出口
    name: 'getQueryData', // namespace
    file: path.resolve(__dirname, 'public/task/index.js'), // 最终打包出来的文件路径和文件名
    format: 'umd', // umd/amd/cjs/iife
  },
  plugins: [
    json(),
    nodeResolve({
      extensions: ['.js', '.ts', '.tsx'],
      moduleDirectories: ['node_modules', 'src'],
    }),
    // 将 CommonJS 转换成 ES2015 模块供 Rollup 处理
    commonjs(),
    // es6--> es5
    babel({
      babelHelpers: 'bundled',
      exclude: 'node_modules/**',
      extensions: ['.js', '.ts', '.tsx'],
      presets: [
        ['@babel/preset-env', { modules: false }],
        '@babel/preset-typescript',
      ],
      comments: false,
    }),
    replace({
      preventAssignment: true,
      'console.log': '//console.log',
      'process.env.PUBLIC_URL': JSON.stringify(process.env.PUBLIC_URL),
    }),
  ],
};
