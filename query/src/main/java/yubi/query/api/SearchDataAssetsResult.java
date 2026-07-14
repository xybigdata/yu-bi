package yubi.query.api;

import java.util.List;

public record SearchDataAssetsResult(List<DataAssetSummary> assets) {
    public SearchDataAssetsResult {
        assets = assets == null ? List.of() : List.copyOf(assets);
    }
}
