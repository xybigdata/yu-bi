package yubi.server.config;

import yubi.core.entity.BaseEntity;
import yubi.core.log.AccessType;
import yubi.security.base.ResourceType;
import yubi.server.service.AsyncAccessLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AccessLogAdvice {

    private final AsyncAccessLogService logService;

    public AccessLogAdvice(AsyncAccessLogService logService) {
        this.logService = logService;
    }

    @Before(value = "execution(* yubi.core.mappers..*.selectByPrimaryKey(java.lang.String)) && args(id)")
    public void selectByPrimaryKey(JoinPoint jp, String id) {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        log(AccessType.READ, id, signature.getReturnType());
    }

    @Before(value = "execution(* yubi.core.mappers..*.deleteByPrimaryKey(java.lang.String)) && args(id)")
    public void deleteByPrimaryKey(JoinPoint jp, String id) {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        String typeName = signature.getDeclaringTypeName();
        log(AccessType.DELETE, id, typeName.replace("Mapper", "").replace("Ext", ""));
    }

    @Before(value = "execution(* yubi.core.mappers..*.insert(yubi.core.entity.BaseEntity)) && args(entity)")
    public void insert(JoinPoint jp, BaseEntity entity) {
        log(AccessType.CREATE, entity.getId(), entity.getClass());
    }

    @Before(value = "execution(* yubi.core.mappers..*.updateByPrimaryKey*(yubi.core.entity.BaseEntity)) && args(entity)")
    public void updateByPrimaryKey(JoinPoint jp, BaseEntity entity) {
        log(AccessType.UPDATE, entity.getId(), entity.getClass());
    }

    private void log(AccessType accessType, String id, Class<?> clz) {
        try {
            logService.log(accessType, getResourceType(clz), id);
        } catch (Exception ignored) {
        }
    }

    private void log(AccessType accessType, String id, String name) {
        try {
//            logService.log(accessType, ResourceType.valueOf(name.toUpperCase()), id);
        } catch (Exception ignored) {
        }
    }

    private ResourceType getResourceType(Class<?> clz) {
        try {
            return ResourceType.valueOf(clz.getSimpleName().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

}
