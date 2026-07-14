package yubi.agent.api;

import yubi.agent.domain.ControlledWriteModels.WriteFailureCategory;
import yubi.agent.domain.ControlledWriteModels.WriteOperationView;

import java.util.List;

public interface WriteApprovalUseCase {

    /** 仅列出当前可信 subject + organization + session 绑定的操作。 */
    List<WriteOperationView> listOperations(AgentExecutionContext context);

    /** 调用方必须在可信服务端事务中执行；该方法不接收或重传业务参数。 */
    WriteOperationView approve(String approvalId, AgentExecutionContext context);

    WriteOperationView reject(String approvalId, AgentExecutionContext context);

    /** 业务事务回滚后，由服务端在独立事务中记录有限失败终态。 */
    WriteOperationView markFailed(String approvalId,
                                  WriteFailureCategory failure,
                                  AgentExecutionContext context);
}
