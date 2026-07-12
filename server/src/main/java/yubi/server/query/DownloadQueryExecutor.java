package yubi.server.query;

import org.springframework.stereotype.Service;
import yubi.core.data.provider.Dataframe;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.QueryExecutionException;
import yubi.server.base.params.DownloadQueryRequest;

@Service
public class DownloadQueryExecutor {

    private final ExecuteQueryUseCase executeQueryUseCase;
    private final DownloadQueryMapper mapper;
    private final ServerQueryExecutionContextFactory contextFactory;

    public DownloadQueryExecutor(ExecuteQueryUseCase executeQueryUseCase,
                                 DownloadQueryMapper mapper,
                                 ServerQueryExecutionContextFactory contextFactory) {
        this.executeQueryUseCase = executeQueryUseCase;
        this.mapper = mapper;
        this.contextFactory = contextFactory;
    }

    public Dataframe execute(DownloadQueryRequest request) throws Exception {
        if (request.isEmpty()) {
            return Dataframe.empty();
        }
        try {
            return mapper.toDataframe(executeQueryUseCase.execute(mapper.toCommand(request), contextFactory.forSystem()));
        } catch (QueryExecutionException exception) {
            if (exception.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw exception;
        }
    }
}
