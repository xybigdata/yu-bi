package yubi.agent.port;

import yubi.agent.domain.ControlledWriteModels.ControlledWriteOperation;
import yubi.agent.domain.ControlledWriteModels.WriteApprovalScope;
import yubi.agent.domain.ControlledWriteModels.WriteIdempotencyScope;

import java.util.List;
import java.util.Optional;

/** 持久实现必须与调用方事务一致；审批读取必须持有到事务结束的排他锁，执行后的 CAS 不能替代该锁。 */
public interface WriteOperationStorePort {

    Optional<ControlledWriteOperation> findByIdempotency(WriteIdempotencyScope scope);

    /** 实现必须把 subject、organization、session 三个条件全部下推到可信存储查询。 */
    List<ControlledWriteOperation> listByScope(WriteApprovalScope scope, int maximumItems);

    /** 并发插入时必须按幂等作用域原子地返回唯一既有记录。 */
    CreateResult createPending(ControlledWriteOperation operation);

    Optional<ControlledWriteOperation> lockByApprovalId(String approvalId);

    /** previous 必须是当前事务已锁定版本；持久实现应拒绝任何状态竞争。 */
    void saveLocked(ControlledWriteOperation previous, ControlledWriteOperation updated);

    record CreateResult(ControlledWriteOperation operation, boolean created) {
        public CreateResult {
            java.util.Objects.requireNonNull(operation, "operation");
        }
    }
}
