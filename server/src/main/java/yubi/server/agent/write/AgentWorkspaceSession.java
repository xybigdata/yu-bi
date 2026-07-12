package yubi.server.agent.write;

import java.time.Instant;

public record AgentWorkspaceSession(String sessionId,
                                    String subjectId,
                                    String organizationId,
                                    Instant expiresAt) {
}
