package yubi.agent.port;

import yubi.agent.domain.ControlledWriteModels.PreparedWrite;
import yubi.agent.domain.ControlledWriteModels.TrustedWriteContext;
import yubi.agent.domain.ControlledWriteModels.WriteAction;
import yubi.agent.domain.ControlledWriteModels.WriteExecutionResult;
import yubi.agent.domain.ControlledWriteModels.WritePreview;

/** server 适配独立可视化写业务 Use Case 的窄端口。 */
public interface VisualizationWritePort {

    /** 只读取当前资源、授权和影响范围，不得写入业务数据。 */
    PreparedWrite prepare(WriteAction action,
                          WritePreview safePreview,
                          TrustedWriteContext context);

    /** 必须重新校验当前权限和目标状态，并在调用方事务内原子写入。 */
    WriteExecutionResult execute(PreparedWrite operation, TrustedWriteContext context);
}
