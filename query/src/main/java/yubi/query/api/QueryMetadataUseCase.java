package yubi.query.api;

public interface QueryMetadataUseCase {

    SearchDataAssetsResult search(SearchDataAssetsRequest request, QueryExecutionContext context);

    DataAssetDetail describe(DescribeDataAssetRequest request, QueryExecutionContext context);
}
