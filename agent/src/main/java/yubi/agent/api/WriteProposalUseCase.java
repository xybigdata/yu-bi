package yubi.agent.api;

import yubi.agent.domain.ControlledWriteModels.WriteOperationView;
import yubi.agent.domain.ControlledWriteModels.WriteProposalCommand;

public interface WriteProposalUseCase {

    /** 只创建待审批操作，禁止在该入口执行任何业务写入。 */
    WriteOperationView propose(WriteProposalCommand command, AgentExecutionContext context);
}
