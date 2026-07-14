package yubi.server.service;

import yubi.core.base.consts.ShareAuthenticationMode;

public record ShareDownloadSession(
        String shareId,
        String sessionDigest,
        String securityFingerprint,
        ShareAuthenticationMode authenticationMode,
        String authenticatedSubjectId
) {
}
