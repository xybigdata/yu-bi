import { InteractionRelationType } from '../constants';
import {
  addDefaultRelation,
  deleteRelationById,
  normalizeRelations,
  updateRelationType,
  updateRelationValue,
} from '../Customize/Interaction/relationUtils';

describe('interaction relation utils', () => {
  test('normalizeRelations should always return an array copy', () => {
    const relations = [
      { id: 'relation-1', type: InteractionRelationType.Field },
    ];

    expect(normalizeRelations()).toEqual([]);
    expect(normalizeRelations(relations)).toEqual(relations);
    expect(normalizeRelations(relations)).not.toBe(relations);
  });

  test('addDefaultRelation should append a field relation from empty input', () => {
    const nextRelations = addDefaultRelation();

    expect(nextRelations).toHaveLength(1);
    expect(nextRelations[0].id).toBeTruthy();
    expect(nextRelations[0].type).toBe(InteractionRelationType.Field);
  });

  test('deleteRelationById should remove matched relation and keep original input immutable', () => {
    const relations = [
      { id: 'relation-1', type: InteractionRelationType.Field },
      { id: 'relation-2', type: InteractionRelationType.Variable },
    ];

    expect(deleteRelationById(relations, 'relation-1')).toEqual([
      { id: 'relation-2', type: InteractionRelationType.Variable },
    ]);
    expect(relations).toHaveLength(2);
  });

  test('updateRelationValue should only update matched relation side', () => {
    const relations = [
      { id: 'relation-1', type: InteractionRelationType.Field },
      { id: 'relation-2', type: InteractionRelationType.Variable },
    ];

    expect(
      updateRelationValue(relations, 'relation-2', 'target', 'target-value'),
    ).toEqual([
      { id: 'relation-1', type: InteractionRelationType.Field },
      {
        id: 'relation-2',
        type: InteractionRelationType.Variable,
        target: 'target-value',
      },
    ]);
  });

  test('updateRelationType should reset matched relation payload', () => {
    const relations = [
      {
        id: 'relation-1',
        type: InteractionRelationType.Field,
        source: 'source-value',
        target: 'target-value',
      },
    ];

    const nextRelations = updateRelationType(
      relations,
      'relation-1',
      InteractionRelationType.Variable,
    );

    expect(nextRelations).toHaveLength(1);
    expect(nextRelations[0].id).toBeTruthy();
    expect(nextRelations[0].id).not.toBe('relation-1');
    expect(nextRelations[0]).toEqual({
      id: nextRelations[0].id,
      type: InteractionRelationType.Variable,
    });
  });
});
