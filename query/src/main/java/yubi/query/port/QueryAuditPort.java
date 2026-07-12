package yubi.query.port;

import yubi.query.domain.QueryModels.AuditEvent;

public interface QueryAuditPort {
    void record(AuditEvent event);
}
