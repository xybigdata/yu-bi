package yubi.server.service.impl;

import yubi.core.common.UUIDGenerator;
import yubi.core.entity.AccessLog;
import yubi.core.log.AccessType;
import yubi.security.base.ResourceType;
import yubi.server.service.AsyncAccessLogService;
import yubi.server.service.BaseService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@Slf4j
public class AccessLogServiceImpl extends BaseService implements AsyncAccessLogService {

    private final LinkedBlockingQueue<AccessLog> logQueue = new LinkedBlockingQueue<>();

    private final Thread logThread;

    private volatile boolean stop = false;

    public AccessLogServiceImpl() {
        logThread = new Thread(() -> {
            while (!stop) {
                try {
                    logQueue.take();
                } catch (Exception e) {
//                    log.error("access log insert error", e);
                }
            }
        });
        logThread.start();
    }

    @Override
    public void start() {
        if (!logThread.isAlive()) {
            logThread.start();
        }
    }

    @Override
    public void setAccessLogService(AsyncAccessLogService accessLogService) {
        // AccessLogService 自身不需要再注入访问日志服务，否则会形成自引用循环依赖。
    }

    @Override
    public AccessLog log(AccessLog log) {
        log.setId(UUIDGenerator.generate());
        logQueue.add(log);
        return log;
    }

    @Override
    public AccessLog log(AccessType accessType, ResourceType resourceType, String resourceId) {
        return log(accessType, resourceType, resourceId, new Date());
    }

    @Override
    public AccessLog log(AccessType accessType, ResourceType resourceType, String resourceId, Date accessTime) {
        return log(accessType, resourceType, resourceId, accessTime, null);
    }


    @Override
    public AccessLog log(AccessType accessType, ResourceType resourceType, String resourceId, Date accessTime, Integer duration) {
        AccessLog log = new AccessLog();
        log.setUser(getCurrentUser().getId());
        log.setAccessType(accessType.name());
        log.setResourceType(resourceType != null ? resourceType.name() : null);
        log.setResourceId(resourceId);
        log.setAccessTime(accessTime);
        log.setDuration(duration);
        return log(log);
    }

    @Override
    @PreDestroy
    public void stop() {
        stop = true;
        logThread.interrupt();
    }


}
