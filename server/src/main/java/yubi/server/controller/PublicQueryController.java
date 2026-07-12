package yubi.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yubi.server.base.dto.ResponseData;
import yubi.server.query.web.PublicQueryExecutor;
import yubi.server.query.web.QueryExecuteRequest;
import yubi.server.query.web.QueryResponse;
import yubi.server.query.web.QueryWebMapper;

@Tag(name = "PublicQueryController")
@RestController
@RequestMapping("/public/queries")
public class PublicQueryController extends BaseController {

    public static final String SHARE_TOKEN_HEADER = "X-YuBi-Share-Token";

    private final PublicQueryExecutor publicQueryExecutor;
    private final QueryWebMapper mapper;

    public PublicQueryController(PublicQueryExecutor publicQueryExecutor, QueryWebMapper mapper) {
        this.publicQueryExecutor = publicQueryExecutor;
        this.mapper = mapper;
    }

    @Operation(summary = "Execute a shared view query")
    @PostMapping("/execute")
    public ResponseData<QueryResponse> execute(
            @RequestHeader(value = SHARE_TOKEN_HEADER, required = false) String token,
            @RequestBody(required = false) QueryExecuteRequest request) {
        return ResponseData.success(mapper.toResponse(publicQueryExecutor.execute(token, mapper.toCommand(request))));
    }
}
