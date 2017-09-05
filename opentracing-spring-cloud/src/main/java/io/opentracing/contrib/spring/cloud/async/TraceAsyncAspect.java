package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.ActiveSpan;
import io.opentracing.NoopActiveSpanSource;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.jms.common.TracingMessageListener;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.lang.reflect.Method;

/**
 * @author kamesh
 */
@Aspect
public class TraceAsyncAspect {

    private static final String ASYNC_COMPONENT = "async";
    public static final String TAG_LC = "localcomponent";
    public static final String TAG_CLASS = "class";
    public static final String TAG_METHOD = "method";

    Tracer tracer;
    protected final ActiveSpan.Continuation continuation;
    protected final ActiveSpan activeSpan;

    @Autowired
    public TraceAsyncAspect(Tracer tracer) {
        this.tracer = tracer;
        this.activeSpan = tracer.activeSpan();
        this.continuation = activeSpan != null ? activeSpan.capture() : NoopActiveSpanSource.NoopContinuation.INSTANCE;
    }

    @Around("execution (@org.springframework.scheduling.annotation.Async  * *.*(..))")
    public Object traceBackgroundThread(final ProceedingJoinPoint pjp) throws Throwable {
        this.continuation.activate();

        Span span = this.tracer.buildSpan(operationName(pjp))
            .asChildOf(this.activeSpan.context())
            .startManual();

        this.activeSpan.setTag(TAG_LC, ASYNC_COMPONENT);
        this.activeSpan.setTag(TAG_CLASS, pjp.getTarget().getClass().getSimpleName());
        this.activeSpan.setTag(TAG_METHOD, pjp.getSignature().getName());
        try {
            return pjp.proceed();
        } finally {
            span.finish();
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
