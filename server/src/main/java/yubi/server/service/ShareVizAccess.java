package yubi.server.service;

import yubi.core.base.consts.ShareAuthenticationMode;
import yubi.server.base.params.ShareVizDetail;

public record ShareVizAccess(
        ShareVizDetail detail,
        String shareId,
        ShareAuthenticationMode authenticationMode,
        String authenticatedSubjectId,
        String securityFingerprint
) {
}
