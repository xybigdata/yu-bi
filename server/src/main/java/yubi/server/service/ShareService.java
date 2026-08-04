package yubi.server.service;

import yubi.core.data.provider.StdSqlOperator;
import yubi.core.entity.Share;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.server.artifact.ArtifactContent;
import yubi.server.artifact.TaskHandle;
import yubi.server.artifact.TaskView;
import yubi.server.artifact.TaskPage;
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

    TaskHandle createDownload(String clientId, String password, ShareDownloadParam downloadCreateParams);

    TaskView getArtifactTask(String shareId, String clientId, String password, String taskId);

    TaskPage listArtifactTasks(String shareId, String clientId, String password,
                               int offset, int limit);

    TaskHandle retryArtifactTask(String shareId, String clientId, String password, String taskId);

    ArtifactContent openArtifact(String shareId, String clientId, String password, String taskId);

    void confirmArtifactDelivery(String shareId, String clientId, String password, String taskId);

    void deleteArtifactTask(String shareId, String clientId, String password, String taskId);

    Set<StdSqlOperator> supportedStdFunctions(ShareToken shareToken, String sourceId);

}
