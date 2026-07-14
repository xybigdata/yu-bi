package yubi.server.service;

import yubi.core.data.provider.StdSqlOperator;
import yubi.core.entity.Share;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.server.base.dto.DownloadTaskDTO;
import yubi.server.base.dto.ShareInfo;
import yubi.server.base.params.*;

import java.util.List;
import java.util.Set;

public interface ShareService extends BaseCRUDService<Share, ShareMapperExt> {

    ShareToken createShare(ShareCreateParam createParam);

    ShareToken createShare(String shareUser, ShareCreateParam createParam);

    ShareInfo updateShare(ShareUpdateParam updateParam);

    List<ShareInfo> listShare(String vizId);

    ShareVizAccess getShareViz(ShareToken shareToken, ShareDownloadSession existingSession);

    DownloadTaskDTO createDownload(
            String shareId,
            ShareDownloadSession session,
            ShareDownloadParam downloadCreateParams
    );

    List<DownloadTaskDTO> listDownloadTask(String shareId, ShareDownloadSession session);

    DownloadFileResource download(String shareId, ShareDownloadSession session, String downloadId);

    Set<StdSqlOperator> supportedStdFunctions(ShareToken shareToken, String sourceId);

}
