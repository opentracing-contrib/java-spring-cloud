package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.ActiveSpan;
import io.opentracing.ActiveSpan.Continuation;
import io.opentracing.NoopActiveSpanSource.NoopContinuation;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * @author kamesh
 */
@Aspect
public class TraceAsyncAspect {

    private static final String ASYNC_COMPONENT = "async";
    private static final String TAG_CLASS = "class";
    private static final String TAG_METHOD = "method";

    Tracer tracer;

    protected final Continuation continuation;

    @Autowired
    public TraceAsyncAspect(Tracer tracer) {
        this.tracer = tracer;
        this.continuation =  tracer.activeSpan() != null ?  tracer.activeSpan().capture() : NoopContinuation.INSTANCE;
    }

    @Around("execution (@org.springframework.scheduling.annotation.Async  * *.*(..))")
    public Object traceBackgroundThread(final ProceedingJoinPoint pjp) throws Throwable {
        ActiveSpan activeSpan = this.continuation.activate();
        Span span = null;
        try {
            span = this.tracer.buildSpan(operationName(pjp))
                    .withTag(Tags.COMPONENT.getKey(), ASYNC_COMPONENT)
                    .withTag(TAG_CLASS, pjp.getTarget().getClass().getSimpleName())
                    .withTag(TAG_METHOD, pjp.getSignature().getName())
                    .startManual();
            return pjp.proceed();
        } finally {
            if (span != null) {
                span.finish();
            }
            activeSpan.close();
        }
    }

    private String operationName(ProceedingJoinPoint pjp) {
        return getMethod(pjp, pjp.getTarget()).getName();
    }

    private Method getMethod(ProceedingJoinPoint pjp, Object object) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        return ReflectionUtils
                .findMethod(object.getClass(), method.getName(), method.getParameterTypes());
    }
}
