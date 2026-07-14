/*
 * YuBi
 * <p>
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package yubi.server.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import yubi.server.base.dto.ResponseData;
import yubi.server.controller.DownloadController;
import yubi.server.controller.ShareDownloadController;
import yubi.server.controller.VizDownloadController;

@RestControllerAdvice(assignableTypes = {
        DownloadController.class,
        ShareDownloadController.class,
        VizDownloadController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DownloadTaskWebExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseData<Object> unreadableRequest(HttpMessageNotReadableException ignored) {
        return ResponseData.failure("下载任务请求参数无效");
    }
}
