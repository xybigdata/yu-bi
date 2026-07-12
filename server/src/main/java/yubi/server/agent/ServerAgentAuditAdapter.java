package yubi.server.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import yubi.agent.domain.AgentModels.AgentAuditEvent;
import yubi.agent.port.AgentAuditPort;

@Component
@Slf4j
public final class ServerAgentAuditAdapter implements AgentAuditPort {

    @Override
    public void record(AgentAuditEvent event) {
        log.info("agent_trace event={} session={} request={} subject={} organization={} correlation={} step={} tool={} fields={} rejectedFields={} scalars={} collections={} depth={} durationMs={} observedItems={} returnedItems={} observedBytes={} returnedBytes={} truncated={} status={} failure={}",
                event.eventType(), event.sessionId(), event.requestId(), event.subjectId(), event.organizationId(),
                event.correlationId(), event.stepIndex(), event.toolName(), event.arguments().recognizedFields(),
                event.arguments().rejectedFieldCount(), event.arguments().scalarCount(),
                event.arguments().collectionCount(), event.arguments().maximumDepth(), event.durationMillis(),
                event.resultSize().observedItems(), event.resultSize().returnedItems(),
                event.resultSize().observedBytes(), event.resultSize().returnedBytes(),
                event.resultSize().truncated(), event.status(), event.failureCategory());
    }
}
