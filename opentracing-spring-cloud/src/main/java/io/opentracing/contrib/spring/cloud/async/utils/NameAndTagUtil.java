package io.opentracing.contrib.spring.cloud.async.utils;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 *
 * @author kameshsampath
 */
public class NameAndTagUtil {

    public static String operationName(ProceedingJoinPoint pjp) {
        return getMethod(pjp, pjp.getTarget()).getName();
    }

    public static Method getMethod(ProceedingJoinPoint pjp, Object object) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        return ReflectionUtils
                .findMethod(object.getClass(), method.getName(), method.getParameterTypes());
    }

    public static String clazzName(ProceedingJoinPoint pjp) {
        return pjp.getTarget().getClass().getSimpleName();
    }

    public static String methodName(ProceedingJoinPoint pjp) {
        return pjp.getSignature().getName();
    }
}
