package yubi.server.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import yubi.query.domain.QueryModels.AuditEvent;
import yubi.query.port.QueryAuditPort;

@Component
@Slf4j
public class ServerQueryAuditAdapter implements QueryAuditPort {

    @Override
    public void record(AuditEvent event) {
        log.info("query_audit channel={} subject={} organization={} correlation={} resource={} durationMs={} rows={} success={} failure={}",
                event.channel(), event.subjectId(), event.organizationId(), event.correlationId(), event.resourceId(),
                event.durationMillis(), event.rowCount(), event.success(), event.failureCategory());
    }
}
