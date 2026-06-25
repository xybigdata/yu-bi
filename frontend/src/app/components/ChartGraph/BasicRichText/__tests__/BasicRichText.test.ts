import {
  AggregateFieldActionType,
  ChartDataSectionType,
  DataViewFieldType,
} from 'app/constants';
import type { ChartConfig } from 'app/types/ChartConfig';
import type ChartDataSetDTO from 'app/types/ChartDataSet';
import BasicRichText from '../BasicRichText';

const createDataset = (): ChartDataSetDTO => ({
  columns: [{ name: 'city' }, { name: 'SUM(amount)' }],
  rows: [['杭州', '128']],
});

const createConfig = (): ChartConfig => ({
  datas: [
    {
      key: 'group',
      type: ChartDataSectionType.Group,
      rows: [
        {
          uid: 'city-field',
          colName: 'city',
          category: 'field',
          type: DataViewFieldType.STRING,
        },
      ],
    },
    {
      key: 'aggregate',
      type: ChartDataSectionType.Aggregate,
      rows: [
        {
          uid: 'amount-field',
          colName: 'amount',
          category: 'field',
          type: DataViewFieldType.NUMERIC,
          aggregate: AggregateFieldActionType.Sum,
        },
      ],
    },
  ],
  styles: [
    {
      label: 'delta',
      key: 'delta',
      comType: 'group',
      rows: [
        {
          label: 'richText',
          key: 'richText',
          comType: 'input',
          value: JSON.stringify({
            ops: [
              { insert: '城市:' },
              { insert: { calcfield: { id: 'city-field', name: 'city' } } },
              { insert: ', 金额:' },
              {
                insert: {
                  calcfield: { id: 'amount-field', name: 'SUM(amount)' },
                },
              },
              { insert: '\n' },
            ],
          }),
        },
      ],
    },
    {
      label: 'richTextMarkdown',
      key: 'richTextMarkdown',
      comType: 'group',
      rows: [
        {
          label: 'openQuillMarkdown',
          key: 'openQuillMarkdown',
          comType: 'checkbox',
          value: true,
        },
      ],
    },
  ],
});

describe('<BasicRichText />', () => {
  test('should mount', () => {
    const chart = new BasicRichText();

    expect(chart.config).toBeDefined();
    expect(chart.meta.requirements).toEqual([
      {
        group: [0, 999],
        aggregate: [0, 999],
      },
    ]);
  });

  test('should build readonly share options with dataset field values', () => {
    const chart = new BasicRichText();
    const dataset = createDataset();
    const config = createConfig();
    const translator = (key: string) => key;

    chart.onMount(
      {
        containerId: 'share-rich-text',
        dataset,
        config,
        widgetSpecialConfig: {},
      },
      { document, window },
    );

    const options = chart.getOptions({ translator }, dataset, config);

    expect(options).toMatchObject({
      id: 'share-rich-text',
      isEditing: false,
      openQuillMarkdown: true,
      initContent: expect.stringContaining('city-field'),
      t: translator,
    });
    expect(options.dataList).toEqual([
      {
        id: 'city-field',
        name: 'city',
        value: '杭州',
      },
      {
        id: 'amount-field',
        name: 'SUM(amount)',
        value: '128',
      },
    ]);
  });

  test('should mark rich text as editing only when widget env is present', () => {
    const chart = new BasicRichText();

    chart.onMount(
      {
        containerId: 'edit-rich-text',
        dataset: createDataset(),
        config: createConfig(),
        widgetSpecialConfig: { env: 'edit' },
      },
      { document, window },
    );

    expect(
      chart.getOptions({}, createDataset(), createConfig()).isEditing,
    ).toBe(true);
  });
});
