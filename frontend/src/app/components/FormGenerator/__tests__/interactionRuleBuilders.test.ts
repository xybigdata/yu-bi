import {
  InteractionFieldMapper,
  InteractionFieldRelation,
  InteractionMouseEvent,
  InteractionRelationType,
} from '../constants';
import { buildJumpToChartRule } from '../Customize/Interaction/JumpToChart';
import { buildJumpToDashboardRule } from '../Customize/Interaction/JumpToDashboard';
import { buildJumpToUrlRule } from '../Customize/Interaction/JumpToUrl';
import { buildViewDetailSetting } from '../Customize/Interaction/ViewDetailPanel';

describe('interaction rule builders', () => {
  test('buildViewDetailSetting should preserve custom fields only in customize mapper', () => {
    expect(
      buildViewDetailSetting(InteractionMouseEvent.Left, undefined, [
        'field-a',
      ]),
    ).toEqual({
      event: InteractionMouseEvent.Left,
      mapper: undefined,
      [InteractionFieldMapper.Customize]: [],
    });

    expect(
      buildViewDetailSetting(
        InteractionMouseEvent.Right,
        InteractionFieldMapper.Customize,
        [],
      ),
    ).toEqual({
      event: InteractionMouseEvent.Right,
      mapper: InteractionFieldMapper.Customize,
      [InteractionFieldMapper.Customize]: [],
    });

    expect(
      buildViewDetailSetting(
        InteractionMouseEvent.Right,
        InteractionFieldMapper.Customize,
        ['field-a'],
      ),
    ).toEqual({
      event: InteractionMouseEvent.Right,
      mapper: InteractionFieldMapper.Customize,
      [InteractionFieldMapper.Customize]: ['field-a'],
    });
  });

  test('jump rule builders should explicitly write normalized customize relations', () => {
    const relations = [
      {
        id: 'relation-1',
        type: InteractionRelationType.Field,
        source: 'source-a',
        target: 'target-a',
      },
    ];

    expect(
      buildJumpToChartRule(
        {
          relId: 'chart-1',
          relation: InteractionFieldRelation.Auto,
        },
        relations,
      ),
    ).toEqual({
      relId: 'chart-1',
      relation: InteractionFieldRelation.Auto,
      [InteractionFieldRelation.Customize]: relations,
    });

    expect(
      buildJumpToDashboardRule(
        {
          relId: 'dashboard-1',
        },
        relations,
      ),
    ).toEqual({
      relId: 'dashboard-1',
      [InteractionFieldRelation.Customize]: relations,
    });

    expect(
      buildJumpToUrlRule(
        {
          relId: 'url-1',
          url: 'https://old.example.com',
        },
        'https://new.example.com',
        relations,
      ),
    ).toEqual({
      relId: 'url-1',
      url: 'https://new.example.com',
      [InteractionFieldRelation.Customize]: relations,
    });
  });
});
