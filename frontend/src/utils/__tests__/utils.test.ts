import {
  filterListOrTree,
  getInsertedNodeIndex,
  listToTree,
  universalUUID,
  uuidv4,
} from 'utils/utils';

const originalCrypto = globalThis.crypto;

describe('utils uuid', () => {
  afterEach(() => {
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      value: originalCrypto,
    });
  });

  it('should prefer crypto.randomUUID when available', () => {
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      value: {
        randomUUID: () => '11111111-1111-4111-8111-111111111111',
      },
    });

    expect(uuidv4()).toBe('11111111-1111-4111-8111-111111111111');
  });

  it('should build a v4 uuid from crypto.getRandomValues', () => {
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      value: {
        getRandomValues: (array: Uint8Array) => {
          array.set([
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88, 0x99, 0xaa,
            0xbb, 0xcc, 0xdd, 0xee, 0xff,
          ]);
          return array;
        },
      },
    });

    expect(uuidv4()).toBe('00112233-4455-4677-8899-aabbccddeeff');
  });

  it('should fall back when crypto is unavailable', () => {
    const randomSpy = vi
      .spyOn(Math, 'random')
      .mockReturnValueOnce(0)
      .mockReturnValueOnce(1 / 256)
      .mockReturnValueOnce(2 / 256)
      .mockReturnValueOnce(3 / 256)
      .mockReturnValueOnce(4 / 256)
      .mockReturnValueOnce(5 / 256)
      .mockReturnValueOnce(6 / 256)
      .mockReturnValueOnce(7 / 256)
      .mockReturnValueOnce(8 / 256)
      .mockReturnValueOnce(9 / 256)
      .mockReturnValueOnce(10 / 256)
      .mockReturnValueOnce(11 / 256)
      .mockReturnValueOnce(12 / 256)
      .mockReturnValueOnce(13 / 256)
      .mockReturnValueOnce(14 / 256)
      .mockReturnValueOnce(15 / 256);

    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      value: undefined,
    });

    expect(universalUUID()).toBe('00010203-0405-4607-8809-0a0b0c0d0e0f');

    randomSpy.mockRestore();
  });
});

describe('utils tree helpers', () => {
  type FlatNode = {
    id: string;
    name: string;
    parentId: string | null;
    isFolder: boolean;
    index: number | null;
  };

  const nodes: FlatNode[] = [
    {
      id: 'child-b',
      name: 'Child B',
      parentId: 'root',
      isFolder: false,
      index: 2,
    },
    { id: 'root', name: 'Root', parentId: null, isFolder: true, index: 1 },
    {
      id: 'child-a',
      name: 'Child A',
      parentId: 'root',
      isFolder: false,
      index: 1,
    },
  ];

  it('should convert flat list to sorted tree nodes', () => {
    expect(listToTree(nodes)).toEqual([
      {
        id: 'root',
        name: 'Root',
        parentId: null,
        isFolder: true,
        index: 1,
        key: 'root',
        title: 'Root',
        value: 'root',
        path: ['root'],
        children: [
          {
            id: 'child-a',
            name: 'Child A',
            parentId: 'root',
            isFolder: false,
            index: 1,
            key: 'child-a',
            title: 'Child A',
            value: 'child-a',
            path: ['root', 'child-a'],
            isLeaf: true,
          },
          {
            id: 'child-b',
            name: 'Child B',
            parentId: 'root',
            isFolder: false,
            index: 2,
            key: 'child-b',
            title: 'Child B',
            value: 'child-b',
            path: ['root', 'child-b'],
            isLeaf: true,
          },
        ],
      },
    ]);
  });

  it('should preserve leaf children when filterLeaf matches parent', () => {
    const tree = listToTree(nodes) || [];

    expect(
      filterListOrTree(
        tree,
        'Root',
        (_, node) => node.name.includes('Root'),
        true,
      ),
    ).toEqual(tree);
  });

  it('should get next inserted node index in same parent', () => {
    expect(
      getInsertedNodeIndex({ name: 'new node', parentId: 'root' }, [
        { parentId: 'root', index: 1 },
        { parentId: 'root', index: 3 },
        { parentId: null, index: 10 },
      ]),
    ).toBe(4);

    expect(
      getInsertedNodeIndex({ name: 'new node', parentId: 'missing' }, [
        { parentId: 'root', index: 1 },
      ]),
    ).toBe(0);
  });
});
