package com.institute.achievement.framework.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.institute.achievement.framework.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * AOP aspect that intercepts all write operations (POST/PUT/DELETE) on
 * REST controllers and records audit log entries (D-25).
 * <p>
 * Captures: operator identity (from SecurityContext), client IP, request
 * body, response data, operation type, and target object type.
 * <p>
 * Audit logging runs in a separate transaction so that the log entry
 * persists even if the main operation fails (recording the failure).
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    /**
     * Pointcut: match all write operation annotations on controller methods.
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void inRestController() {
    }

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)" +
            " || @annotation(org.springframework.web.bind.annotation.PutMapping)" +
            " || @annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void writeOperation() {
    }

    @Pointcut("inRestController() && writeOperation()")
    public void controllerWriteOperation() {
    }

    @Around("controllerWriteOperation()")
    public Object auditWriteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // Determine operation type from annotation
        String operationType = determineOperationType(method);
        if (operationType == null) {
            // Not a write operation, proceed without auditing
            return joinPoint.proceed();
        }

        // Get HTTP request info
        HttpServletRequest request = getCurrentRequest();

        // Extract target type from controller class and method
        String targetType = extractTargetType(joinPoint);

        // Extract request body (original content for updates, target for creates)
        Object requestBody = extractRequestBody(method, args);

        // Extract target ID from path variables or request body if possible
        String targetId = extractTargetId(request, requestBody);

        // Build operation name description
        String operationName = buildOperationName(operationType, targetType, requestBody);

        // Proceed with the original method call and capture response
        Object result = null;
        boolean success = true;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            success = false;
            throw e; // Re-throw after audit logging
        } finally {
            try {
                // Build audit DTO
                AuditLogDTO auditDto = new AuditLogDTO();
                auditDto.setOperatorId(SecurityUtils.getCurrentUserId());
                auditDto.setOperatorName(SecurityUtils.getCurrentUsername());
                auditDto.setOperationType(operationType);
                auditDto.setOperationName(operationName);
                auditDto.setTargetType(targetType);
                auditDto.setTargetId(targetId);
                auditDto.setIpAddress(SecurityUtils.getClientIp());
                auditDto.setUserAgent(request != null ? request.getHeader("User-Agent") : null);
                auditDto.setStatus(success ? 1 : 0);

                // For UPDATE, capture original content from request body
                if ("UPDATE".equals(operationType)) {
                    auditDto.setOriginalContent(requestBody);
                }
                // For CREATE and UPDATE, capture target content from method return
                if ("CREATE".equals(operationType) || "UPDATE".equals(operationType)) {
                    auditDto.setTargetContent(result);
                }

                auditLogService.record(auditDto);
            } catch (Exception e) {
                log.error("Failed to record audit log for operation: {}.{} - {}",
                        joinPoint.getTarget().getClass().getSimpleName(),
                        method.getName(), e.getMessage());
            }
        }

        return result;
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    /**
     * Determine the operation type based on the method annotation.
     */
    private String determineOperationType(Method method) {
        if (method.isAnnotationPresent(PostMapping.class)) {
            return "CREATE";
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            return "UPDATE";
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            return "DELETE";
        }
        return null;
    }

    /**
     * Extract the target type from the controller class.
     * Strips "Controller" suffix from the class name.
     * Examples: UserController -> User, RoleController -> Role
     */
    private String extractTargetType(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        // Remove Controller suffix
        if (className.endsWith("Controller")) {
            className = className.substring(0, className.length() - "Controller".length());
        }
        // Also handle CGLIB proxies
        if (className.contains("$$")) {
            className = className.substring(0, className.indexOf("$$"));
            if (className.endsWith("Controller")) {
                className = className.substring(0, className.length() - "Controller".length());
            }
        }
        return className;
    }

    /**
     * Extract the request body parameter from method arguments.
     */
    private Object extractRequestBody(Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(RequestBody.class)) {
                return args[i];
            }
        }
        return null;
    }

    /**
     * Extract target ID from request path variables or request body.
     */
    private String extractTargetId(HttpServletRequest request, Object requestBody) {
        // Try to get ID from path
        if (request != null) {
            String pathInfo = request.getRequestURI();
            // Extract last path segment as potential ID
            String[] segments = pathInfo.split("/");
            if (segments.length > 0) {
                String lastSegment = segments[segments.length - 1];
                if (lastSegment.matches("\\d+")) {
                    return lastSegment;
                }
            }
        }
        // Try to get ID from request body
        if (requestBody != null) {
            try {
                if (requestBody instanceof Map) {
                    Object id = ((Map<?, ?>) requestBody).get("id");
                    return id != null ? id.toString() : null;
                }
                // Use Jackson to extract id field
                String json = objectMapper.writeValueAsString(requestBody);
                Map<?, ?> map = objectMapper.readValue(json, Map.class);
                Object id = map.get("id");
                if (id == null) {
                    id = map.get("userId");
                }
                if (id == null) {
                    id = map.get("roleId");
                }
                if (id == null) {
                    id = map.get("deptId");
                }
                return id != null ? id.toString() : null;
            } catch (Exception e) {
                log.trace("Could not extract target ID from request body: {}", e.getMessage());
            }
        }
        return null;
    }

    /**
     * Build a human-readable operation description.
     */
    private String buildOperationName(String operationType, String targetType, Object requestBody) {
        String name = null;
        if (requestBody != null) {
            try {
                String json = objectMapper.writeValueAsString(requestBody);
                Map<?, ?> map = objectMapper.readValue(json, Map.class);
                // Try common name fields
                Object nameField = map.get("name");
                if (nameField == null) nameField = map.get("username");
                if (nameField == null) nameField = map.get("realName");
                if (nameField == null) nameField = map.get("roleName");
                if (nameField == null) nameField = map.get("deptName");
                if (nameField != null) {
                    name = nameField.toString();
                }
            } catch (Exception e) {
                log.trace("Could not extract name from request body: {}", e.getMessage());
            }
        }

        switch (operationType) {
            case "CREATE":
                return (name != null) ? "新增" + targetType + "-" + name : "新增" + targetType;
            case "UPDATE":
                return (name != null) ? "编辑" + targetType + "-" + name : "编辑" + targetType;
            case "DELETE":
                return (name != null) ? "删除" + targetType + "-" + name : "删除" + targetType;
            default:
                return operationType + "-" + targetType;
        }
    }

    /**
     * Get the current HTTP request from RequestContextHolder.
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest();
        }
        return null;
    }
}
