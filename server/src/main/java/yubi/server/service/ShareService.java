package yubi.server.service;

import yubi.core.data.provider.Dataframe;
import yubi.core.data.provider.StdSqlOperator;
import yubi.core.entity.Download;
import yubi.core.entity.Share;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.server.base.dto.ShareInfo;
import yubi.server.base.params.*;

import java.util.List;
import java.util.Set;

public interface ShareService extends BaseCRUDService<Share, ShareMapperExt> {

    ShareToken createShare(ShareCreateParam createParam);

    ShareToken createShare(String shareUser, ShareCreateParam createParam);

    ShareInfo updateShare(ShareUpdateParam updateParam);

    List<ShareInfo> listShare(String vizId);

    ShareVizDetail getShareViz(ShareToken shareToken);

    Download createDownload(String clientId, ShareDownloadParam downloadCreateParams);

    List<Download> listDownloadTask(ShareToken shareToken, String clientId);

    Download download(ShareToken shareToken, String downloadId);

    Set<StdSqlOperator> supportedStdFunctions(ShareToken shareToken, String sourceId);

}
