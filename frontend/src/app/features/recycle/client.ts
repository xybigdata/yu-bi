import { request2 } from 'utils/request';
import {
  RecycleBatch,
  RecycleEntry,
  RecyclePolicy,
  RecyclePreflight,
  RecycleResourceType,
} from './types';

const basePath = (orgId: string, resourceType: RecycleResourceType) =>
  `/organizations/${orgId}/recycle/${resourceType}`;

export const recycleClient = {
  async preflight(
    orgId: string,
    resourceType: RecycleResourceType,
    rootIds: string[],
  ) {
    const { data } = await request2<RecyclePreflight>({
      url: `${basePath(orgId, resourceType)}/preflight`,
      method: 'POST',
      data: { rootIds },
    });
    return data;
  },

  async move(
    orgId: string,
    resourceType: RecycleResourceType,
    operationToken: string,
    clientRequestId: string,
  ) {
    const { data } = await request2<RecycleBatch>({
      url: basePath(orgId, resourceType),
      method: 'POST',
      data: { operationToken, clientRequestId },
    });
    return data;
  },

  async getBatch(
    orgId: string,
    resourceType: RecycleResourceType,
    batchId: string,
  ) {
    const { data } = await request2<RecycleBatch>({
      url: `${basePath(orgId, resourceType)}/batches/${batchId}`,
      method: 'GET',
    });
    return data;
  },

  async undo(
    orgId: string,
    resourceType: RecycleResourceType,
    batchId: string,
    undoToken: string,
  ) {
    const { data } = await request2<RecycleBatch>({
      url: `${basePath(orgId, resourceType)}/batches/${batchId}/undo`,
      method: 'POST',
      data: { undoToken },
    });
    return data;
  },

  async list(orgId: string, resourceType: RecycleResourceType) {
    const { data } = await request2<RecycleEntry[]>({
      url: basePath(orgId, resourceType),
      method: 'GET',
    });
    return data;
  },

  async restore(
    orgId: string,
    resourceType: RecycleResourceType,
    recordIds: string[],
    clientRequestId: string,
  ) {
    const { data } = await request2<RecycleBatch>({
      url: `${basePath(orgId, resourceType)}/restore`,
      method: 'POST',
      data: { recordIds, clientRequestId },
    });
    return data;
  },

  async permanentlyDelete(
    orgId: string,
    resourceType: RecycleResourceType,
    recordIds: string[],
    clientRequestId: string,
  ) {
    const { data } = await request2<RecycleBatch>({
      url: basePath(orgId, resourceType),
      method: 'DELETE',
      data: { recordIds, clientRequestId },
    });
    return data;
  },

  async getPolicy(orgId: string, resourceType: RecycleResourceType) {
    const { data } = await request2<RecyclePolicy>({
      url: `${basePath(orgId, resourceType)}/policy`,
      method: 'GET',
    });
    return data;
  },

  async updatePolicy(
    orgId: string,
    resourceType: RecycleResourceType,
    policy: RecyclePolicy,
  ) {
    const { data } = await request2<RecyclePolicy>({
      url: `${basePath(orgId, resourceType)}/policy`,
      method: 'PUT',
      data: policy,
    });
    return data;
  },
};

export const createClientRequestId = () =>
  globalThis.crypto?.randomUUID?.() ||
  `${Date.now()}-${Math.random().toString(16).slice(2)}`;
