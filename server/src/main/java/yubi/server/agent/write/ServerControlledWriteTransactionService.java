package yubi.server.agent.write;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import yubi.agent.api.AgentExecutionContext;
import yubi.agent.api.ControlledWriteException;
import yubi.agent.api.WriteApprovalUseCase;
import yubi.agent.api.WriteProposalUseCase;
import yubi.agent.domain.ControlledWriteModels.WriteFailureCategory;
import yubi.agent.domain.ControlledWriteModels.WriteOperationView;
import yubi.agent.domain.ControlledWriteModels.WriteProposalCommand;
import yubi.visualization.write.api.VisualizationWriteException;
import yubi.visualization.write.api.VisualizationWriteFailureCategory;

import java.util.List;

@Service
public final class ServerControlledWriteTransactionService {

    private final WriteProposalUseCase proposals;
    private final WriteApprovalUseCase approvals;
    private final TransactionTemplate required;
    private final TransactionTemplate approvalExecution;
    private final TransactionTemplate failureRecording;

    public ServerControlledWriteTransactionService(WriteProposalUseCase proposals,
                                                   WriteApprovalUseCase approvals,
                                                   PlatformTransactionManager transactionManager) {
        this.proposals = proposals;
        this.approvals = approvals;
        this.required = new TransactionTemplate(transactionManager);
        this.required.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.approvalExecution = requiresNew(transactionManager,
                TransactionDefinition.ISOLATION_REPEATABLE_READ);
        this.failureRecording = requiresNew(transactionManager,
                TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    public WriteOperationView propose(WriteProposalCommand command, AgentExecutionContext context) {
        return required.execute(status -> proposals.propose(command, context));
    }

    public List<WriteOperationView> listOperations(AgentExecutionContext context) {
        return required.execute(status -> approvals.listOperations(context));
    }

    public WriteOperationView reject(String approvalId, AgentExecutionContext context) {
        return required.execute(status -> approvals.reject(approvalId, context));
    }

    public WriteOperationView approve(String approvalId, AgentExecutionContext context) {
        try {
            return approvalExecution.execute(status -> approvals.approve(approvalId, context));
        } catch (VisualizationWriteException exception) {
            WriteFailureCategory failure = category(exception.category());
            markFailed(approvalId, context, failure);
            throw new ServerControlledWriteExecutionException(exception.code().name(),
                    exception.code().safeMessage(), failure);
        } catch (ControlledWriteException exception) {
            if (exception.code() != ControlledWriteException.Code.INVALID_BUSINESS_RESULT) {
                throw exception;
            }
            markFailed(approvalId, context, WriteFailureCategory.EXECUTION);
            throw new ServerControlledWriteExecutionException("INVALID_BUSINESS_RESULT",
                    "受控业务写入结果无效", WriteFailureCategory.EXECUTION);
        } catch (RuntimeException exception) {
            markFailed(approvalId, context, WriteFailureCategory.INTERNAL);
            throw new ServerControlledWriteExecutionException("CONTROLLED_WRITE_EXECUTION_FAILED",
                    "受控业务写入失败", WriteFailureCategory.INTERNAL);
        } catch (Error error) {
            try {
                markFailed(approvalId, context, WriteFailureCategory.INTERNAL);
            } catch (RuntimeException reportingFailure) {
                error.addSuppressed(reportingFailure);
            }
            throw error;
        }
    }

    private void markFailed(String approvalId,
                            AgentExecutionContext context,
                            WriteFailureCategory failure) {
        try {
            failureRecording.executeWithoutResult(status -> approvals.markFailed(approvalId, failure, context));
        } catch (RuntimeException ignored) {
            // 原业务事务已经回滚；失败账本不可用时仍只向 Web 边界返回有限错误。
        }
    }

    private WriteFailureCategory category(VisualizationWriteFailureCategory category) {
        return switch (category) {
            case ACCESS_DENIED -> WriteFailureCategory.ACCESS_DENIED;
            case VALIDATION -> WriteFailureCategory.VALIDATION;
            case CONFLICT, STALE -> WriteFailureCategory.CONFLICT;
            case EXECUTION -> WriteFailureCategory.EXECUTION;
        };
    }

    private TransactionTemplate requiresNew(PlatformTransactionManager transactionManager, int isolationLevel) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setIsolationLevel(isolationLevel);
        return transaction;
    }
}
