package io.opentracing.contrib.spring.cloud.async;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;

import io.opentracing.ActiveSpan;
import io.opentracing.ActiveSpan.Continuation;
import io.opentracing.NoopActiveSpanSource.NoopContinuation;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

/**
 * @author kameshsampath
 */
@Aspect
public class TraceAsyncAspect {
    private static final String ASYNC_COMPONENT = "async";
    private static final String TAG_CLASS = "class";
    private static final String TAG_METHOD = "method";

    @Autowired
    private Tracer tracer;

    private Continuation continuation;

    public TraceAsyncAspect(Tracer tracer) {
        this.tracer = tracer;
        this.continuation = tracer.activeSpan() != null ? tracer.activeSpan().capture() : NoopContinuation.INSTANCE;
    }

    @Around("execution (@org.springframework.scheduling.annotation.Async * *.*(..))")
    public Object traceBackgroundThread(final ProceedingJoinPoint pjp) throws Throwable {
        Span span = null;
        try (ActiveSpan activeSpan = this.continuation.activate()) {
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
}
