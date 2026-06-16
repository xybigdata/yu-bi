/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { RectConfig } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import {
  convertListToTree,
  convertToTree,
  handleRowDataForTree,
} from '../../../../utils/widget';
import { getParentRect } from '../utils';

describe('getParentRect Test', () => {
  test('should get a rect by args', () => {
    const widgetMap = {
      a1: {
        id: 'a1',
        config: { rect: { x: 1, y: 1, width: 11, height: 11 } as RectConfig },
      },
      b2: {
        id: 'b2',
        config: { rect: { x: 5, y: 5, width: 55, height: 55 } as RectConfig },
      },
    };
    const ids = ['a1', 'b2'];

    const res = getParentRect({
      childIds: ids,
      widgetMap: widgetMap as any,
      preRect: { x: 0, y: 0, width: 5, height: 6 },
    });
    expect(res).toEqual({
      x: 1,
      y: 1,
      height: 59,
      width: 59,
    } as RectConfig);
  });
});

describe('widget tree utils', () => {
  test('should convert row paths to flat tree nodes', () => {
    expect(
      handleRowDataForTree([
        ['浙江', '杭州', '西湖'],
        ['浙江', '宁波'],
      ]),
    ).toEqual([
      { id: '浙江', parentId: null },
      { id: '杭州', parentId: '浙江' },
      { id: '西湖', parentId: '杭州' },
      { id: '宁波', parentId: '浙江' },
    ]);
  });

  test('should convert list nodes to tree nodes', () => {
    expect(
      convertListToTree([
        { id: '浙江', parentId: null, label: '浙江省' },
        { id: '杭州', parentId: '浙江' },
        { id: '西湖', parentId: '杭州' },
      ]),
    ).toEqual([
      {
        id: '浙江',
        parentId: null,
        key: '浙江',
        title: '浙江省',
        children: [
          {
            id: '杭州',
            parentId: '浙江',
            key: '杭州',
            title: '杭州',
            children: [
              {
                id: '西湖',
                parentId: '杭州',
                key: '西湖',
                title: '西湖',
                isLeaf: true,
              },
            ],
          },
        ],
      },
    ]);
  });

  test('should convert source rows to tree by parent or path', () => {
    expect(
      convertToTree(
        [
          ['浙江', '浙江省', 'null'],
          ['杭州', '杭州市', '浙江'],
        ],
        'byParent',
      ),
    ).toEqual([
      {
        id: '浙江',
        parentId: 'null',
        key: '浙江',
        title: '浙江省',
        children: [
          {
            id: '杭州',
            parentId: '浙江',
            key: '杭州',
            title: '杭州市',
            isLeaf: true,
          },
        ],
      },
    ]);

    expect(convertToTree([['浙江', '杭州']], 'byPath')).toEqual([
      {
        id: '浙江',
        parentId: null,
        key: '浙江',
        title: '浙江',
        children: [
          {
            id: '杭州',
            parentId: '浙江',
            key: '杭州',
            title: '杭州',
            isLeaf: true,
          },
        ],
      },
    ]);
  });
});
