package yubi.agent.port;

import yubi.agent.domain.ControlledWriteModels.WriteAuditEvent;

/** 与写操作账本同事务提交的强一致审计端口。 */
public interface WriteAuditPort {

    void record(WriteAuditEvent event);
}
