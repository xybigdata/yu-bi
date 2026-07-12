package yubi.query.port;

import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryMetadataModels.AssetReference;

import java.util.List;

public interface QueryMetadataCatalogPort {

    /** 返回可信上下文中具有 READ 权限的安全 View 投影。 */
    List<AssetReference> listReadable(QueryExecutionContext context);

    /** 按 ID 返回不含脚本、模型和 Source 配置的安全 View 投影。 */
    AssetReference find(String assetId);
}
