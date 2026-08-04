import { StorageKeys } from 'globalConstants';
import { request2, requestWithHeader } from 'utils/request';
import {
  BrowserArtifactDownloadSink,
  BrowserArtifactTaskCoordination,
  HttpArtifactTaskTransport,
  LocalArtifactTaskStorage,
  type ArtifactTaskWebResponse,
} from './ArtifactTaskAdapters';
import {
  ArtifactTaskCoordinator,
  ArtifactTaskNotFoundError,
  type ArtifactTaskAccess,
} from './ArtifactTaskCoordinator';

let runtime: {
  coordinator: ArtifactTaskCoordinator;
  transport: HttpArtifactTaskTransport;
} | null = null;

export function authenticatedArtifactAccess(
  userId?: string,
  organizationId?: string,
): ArtifactTaskAccess {
  return {
    scopeKey: `${userId ? `user:${userId}` : 'user:current'}:org:${
      organizationId ?? 'current'
    }`,
    ...(organizationId ? { organizationId } : {}),
  };
}

export function sharedArtifactAccess(
  shareId: string,
  clientId: string,
  password?: string,
  principal = currentAuthenticatedPrincipal(),
): ArtifactTaskAccess {
  return {
    scopeKey: `share:${shareId}:${clientId}:${principal}`,
    shareId,
    clientId,
    ...(password ? { password } : {}),
  };
}

function currentAuthenticatedPrincipal() {
  try {
    const user = JSON.parse(
      localStorage.getItem(StorageKeys.LoggedInUser) ?? '{}',
    ) as { id?: unknown; username?: unknown };
    if (typeof user.id === 'string') {
      return `user:${user.id}`;
    }
    if (typeof user.username === 'string') {
      return `user:${user.username}`;
    }
  } catch {
    // 无有效登录信息时使用匿名 scope。
  }
  return 'anonymous';
}

export async function submitAuthenticatedArtifactTask(
  createTask: () => Promise<ArtifactTaskWebResponse>,
  organizationId: string,
) {
  const { coordinator, transport } = getRuntime();
  await coordinator.submitAndTrack(
    async () => transport.snapshot(await createTask()),
    currentAuthenticatedAccess(organizationId),
  );
}

export async function submitSharedArtifactTask(
  createTask: () => Promise<ArtifactTaskWebResponse>,
  access: ArtifactTaskAccess,
) {
  const { coordinator, transport } = getRuntime();
  await coordinator.submitAndTrack(
    async () => transport.snapshot(await createTask()),
    access,
  );
}

export async function resumeAuthenticatedArtifactTasks(
  userId: string | undefined,
  organizationId: string,
) {
  await resumeArtifactTasks(
    authenticatedArtifactAccess(userId, organizationId),
  );
}

export async function resumeSharedArtifactTasks(access: ArtifactTaskAccess) {
  await resumeArtifactTasks(access);
}

async function resumeArtifactTasks(access: ArtifactTaskAccess) {
  await getRuntime().coordinator.activate(access);
}

function currentAuthenticatedAccess(organizationId: string) {
  try {
    const user = JSON.parse(
      localStorage.getItem(StorageKeys.LoggedInUser) ?? '{}',
    ) as { id?: unknown };
    return authenticatedArtifactAccess(
      typeof user.id === 'string' ? user.id : undefined,
      organizationId,
    );
  } catch {
    return authenticatedArtifactAccess(undefined, organizationId);
  }
}

function getRuntime() {
  if (runtime) {
    return runtime;
  }
  const transport = new HttpArtifactTaskTransport(
    config =>
      request2<ArtifactTaskWebResponse>(config, undefined, {
        onRejected: error => {
          const response = error as {
            response?: { data?: { code?: unknown } };
          };
          if (response.response?.data?.code === 'ARTIFACT_NOT_FOUND') {
            throw new ArtifactTaskNotFoundError();
          }
          throw error;
        },
      }),
    config => requestWithHeader<Blob>(config),
  );
  const coordinator = new ArtifactTaskCoordinator({
    transport,
    storage: new LocalArtifactTaskStorage(localStorage),
    downloadSink: new BrowserArtifactDownloadSink(),
    coordination: new BrowserArtifactTaskCoordination(),
    notifier: { notify: () => undefined },
    clock: { now: () => Date.now() },
    timer: {
      setTimeout: (callback, delayMs) => window.setTimeout(callback, delayMs),
      clearTimeout: handle => window.clearTimeout(handle as number),
    },
  });
  runtime = { coordinator, transport };
  return runtime;
}

export function getArtifactTaskCoordinator() {
  return getRuntime().coordinator;
}
