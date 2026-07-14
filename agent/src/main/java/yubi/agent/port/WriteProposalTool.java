package yubi.agent.port;

import yubi.agent.api.WriteToolSchema;
import yubi.agent.domain.ControlledWriteModels.NormalizedWriteProposal;
import yubi.agent.domain.StructuredValue.ObjectValue;

public interface WriteProposalTool {

    WriteToolSchema schema();

    /** 该方法只做严格映射和安全预览，不能产生业务副作用。 */
    NormalizedWriteProposal normalize(ObjectValue arguments);
}
