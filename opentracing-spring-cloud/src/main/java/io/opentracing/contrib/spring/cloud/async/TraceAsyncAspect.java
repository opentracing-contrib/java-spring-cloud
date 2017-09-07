package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.ActiveSpan;
import io.opentracing.ActiveSpan.Continuation;
import io.opentracing.NoopActiveSpanSource.NoopContinuation;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.async.utils.NameAndTagUtil;
import io.opentracing.tag.Tags;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

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

    protected Continuation continuation;

    public TraceAsyncAspect() {

    }
    @PostConstruct
    public void init() {
        this.continuation = tracer.activeSpan() != null ? tracer.activeSpan().capture() : NoopContinuation.INSTANCE;
    }

    @Around("execution (@org.springframework.scheduling.annotation.Async * *.*(..))")
    public Object traceBackgroundThread(final ProceedingJoinPoint pjp) throws Throwable {
        Span span = null;
        try (ActiveSpan activeSpan = this.continuation.activate()) {
            span = this.tracer.buildSpan(NameAndTagUtil.operationName(pjp))
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
}
