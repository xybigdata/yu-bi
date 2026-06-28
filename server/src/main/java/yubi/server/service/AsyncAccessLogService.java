package yubi.server.service;

import yubi.core.entity.AccessLog;
import yubi.core.log.AccessType;
import yubi.security.base.ResourceType;

import java.util.Date;

public interface AsyncAccessLogService {

    void start();

    AccessLog log(AccessLog log);

    AccessLog log(AccessType accessType, ResourceType resourceType, String resourceId);

    AccessLog log(AccessType accessType, ResourceType resourceType, String resourceId, Date accessTime);

    AccessLog log(AccessType accessType, ResourceType resourceType, String resourceId, Date accessTime, Integer duration);

    void stop();

}