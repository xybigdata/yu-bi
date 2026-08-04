package yubi.server.service.impl;

import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.remote.NoSuchDriverException;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.remote.http.ConnectionFailedException;
import yubi.core.base.consts.AttachmentType;
import yubi.server.artifact.ArtifactGenerationException;

import java.net.ConnectException;

final class ArtifactExportFailures {

    private static final String PDF_RENDERER_UNAVAILABLE = "ARTIFACT_PDF_RENDERER_UNAVAILABLE";

    private ArtifactExportFailures() {
    }

    static Exception classify(AttachmentType type, Exception failure) {
        if (type != AttachmentType.PDF || !rendererUnavailable(failure)) {
            return failure;
        }
        return new ArtifactGenerationException(
                PDF_RENDERER_UNAVAILABLE,
                "PDF 渲染服务暂时不可用，请稍后重试",
                failure
        );
    }

    private static boolean rendererUnavailable(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SessionNotCreatedException
                    || current instanceof NoSuchSessionException
                    || current instanceof UnreachableBrowserException
                    || current instanceof NoSuchDriverException
                    || current instanceof ConnectionFailedException
                    || current instanceof ConnectException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
