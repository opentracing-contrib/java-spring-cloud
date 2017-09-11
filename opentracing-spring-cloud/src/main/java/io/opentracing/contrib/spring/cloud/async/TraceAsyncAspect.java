package io.opentracing.contrib.spring.cloud.async;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

/**
 * @author kameshsampath
 */
@Aspect
public class TraceAsyncAspect {
    static final String TAG_COMPONENT = "async";
    static final String TAG_CLASS = "class";
    static final String TAG_METHOD = "method";

    @Autowired
    private Tracer tracer;

    public TraceAsyncAspect(Tracer tracer) {
        this.tracer = tracer;
    }

    @Around("execution (@org.springframework.scheduling.annotation.Async * *.*(..))")
    public Object traceBackgroundThread(final ProceedingJoinPoint pjp) throws Throwable {
        /**
         * We create a span because parent span might be missing. E.g. method is invoked
         */
        Span span = null;
        try {
            span = this.tracer.buildSpan(operationName(pjp))
                    .withTag(Tags.COMPONENT.getKey(), TAG_COMPONENT)
                    .withTag(TAG_CLASS, pjp.getTarget().getClass().getSimpleName())
                    .withTag(TAG_METHOD, pjp.getSignature().getName())
                    .startManual();
            return pjp.proceed();
        } catch (Exception ex) {
            Tags.ERROR.set(span, Boolean.TRUE);
            span.log(exceptionLogs(ex));
            throw ex;
        } finally {
            span.finish();
        }
    }

    private static String operationName(ProceedingJoinPoint pjp) {
        return getMethod(pjp, pjp.getTarget()).getName();
    }

    private static Method getMethod(ProceedingJoinPoint pjp, Object object) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        return ReflectionUtils
                .findMethod(object.getClass(), method.getName(), method.getParameterTypes());
    }

    private static Map<String, Object> exceptionLogs(Exception ex) {
        Map<String, Object> exceptionLogs = new LinkedHashMap<>(2);
        exceptionLogs.put("event", Tags.ERROR.getKey());
        exceptionLogs.put("error.object", ex);
        return exceptionLogs;
    }
}
