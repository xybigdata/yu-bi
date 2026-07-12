package yubi.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.PreviewQueryUseCase;
import yubi.server.base.dto.ResponseData;
import yubi.server.query.ServerQueryExecutionContextFactory;
import yubi.server.query.web.QueryExecuteRequest;
import yubi.server.query.web.QueryPreviewRequest;
import yubi.server.query.web.QueryResponse;
import yubi.server.query.web.QueryWebMapper;

@Tag(name = "QueryController")
@RestController
@RequestMapping("/queries")
public class QueryController extends BaseController {

    private final ExecuteQueryUseCase executeQueryUseCase;
    private final PreviewQueryUseCase previewQueryUseCase;
    private final QueryWebMapper mapper;
    private final ServerQueryExecutionContextFactory contextFactory;

    public QueryController(ExecuteQueryUseCase executeQueryUseCase,
                           PreviewQueryUseCase previewQueryUseCase,
                           QueryWebMapper mapper,
                           ServerQueryExecutionContextFactory contextFactory) {
        this.executeQueryUseCase = executeQueryUseCase;
        this.previewQueryUseCase = previewQueryUseCase;
        this.mapper = mapper;
        this.contextFactory = contextFactory;
    }

    @Operation(summary = "Execute a view query")
    @PostMapping("/execute")
    public ResponseData<QueryResponse> execute(@RequestBody(required = false) QueryExecuteRequest request) {
        return ResponseData.success(mapper.toResponse(executeQueryUseCase.execute(mapper.toCommand(request),
                contextFactory.forView(true))));
    }

    @Operation(summary = "Preview a query")
    @PostMapping("/preview")
    public ResponseData<QueryResponse> preview(@RequestBody(required = false) QueryPreviewRequest request) {
        return ResponseData.success(mapper.toResponse(previewQueryUseCase.preview(mapper.toCommand(request),
                contextFactory.forSource())));
    }
}
