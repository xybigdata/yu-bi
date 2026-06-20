import { uuidv4 } from 'utils/utils';
import { InteractionRelationType } from '../../constants';
import { CustomizeRelation } from './types';

export const normalizeRelations = (
  relations?: CustomizeRelation[],
): CustomizeRelation[] => [...(relations || [])];

export const addDefaultRelation = (
  relations?: CustomizeRelation[],
): CustomizeRelation[] =>
  normalizeRelations(relations).concat({
    id: uuidv4(),
    type: InteractionRelationType.Field,
  });

export const deleteRelationById = (
  relations: CustomizeRelation[] | undefined,
  id?: string,
): CustomizeRelation[] => {
  if (!id) {
    return normalizeRelations(relations);
  }
  return normalizeRelations(relations).filter(relation => relation.id !== id);
};

export const updateRelationValue = (
  relations: CustomizeRelation[] | undefined,
  id: string | undefined,
  key: 'source' | 'target',
  value: string,
): CustomizeRelation[] => {
  if (!id) {
    return normalizeRelations(relations);
  }
  return normalizeRelations(relations).map(relation =>
    relation.id === id ? { ...relation, [key]: value } : relation,
  );
};

export const updateRelationType = (
  relations: CustomizeRelation[] | undefined,
  id: string | undefined,
  type: InteractionRelationType,
): CustomizeRelation[] => {
  if (!id) {
    return normalizeRelations(relations);
  }
  return normalizeRelations(relations).map(relation =>
    relation.id === id ? { id: uuidv4(), type } : relation,
  );
};

export const isFieldRelation = (relation: CustomizeRelation): boolean =>
  relation?.type === InteractionRelationType.Field;
