package io.opentracing.contrib.spring.cloud.async.web;

import io.opentracing.ActiveSpan;
import io.opentracing.NoopActiveSpanSource;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedCallable;
import io.opentracing.contrib.spring.cloud.async.utils.NameAndTagUtil;
import io.opentracing.tag.Tags;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.async.WebAsyncTask;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;

/**
 * this class adds tracing to all {@link org.springframework.stereotype.Controller}
 * or {@link org.springframework.web.bind.annotation.RestController} that has
 * <ul>
 * <li>public {@link java.util.concurrent.Callable} methods</li>
 * <li>public {@link org.springframework.web.context.request.async.WebAsyncTask} methods</li>
 * </ul>
 * All those methods which will eventually have {@link Callable#call()}
 * will be wrapped with {@link io.opentracing.contrib.concurrent.TracedCallable}
 * <p>
 * NOTE: This will not create TraceFilters as thats handled by &quot;opentracing-spring-webautoconfigure&quot;
 *
 * @author kameshsampath
 */
@Aspect
public class TracedAsyncWebAspect {

    private static final String WEB_ASYNC_TASK_COMPONENT = "web-async-task";
    private static final String TAG_CLASS = "class";
    private static final String TAG_METHOD = "method";

    @Autowired
    private Tracer tracer;

    protected ActiveSpan.Continuation continuation;

    public TracedAsyncWebAspect() {

    }

    @PostConstruct
    public void init() {
        this.continuation = tracer.activeSpan() != null ? tracer.activeSpan().capture() : NoopActiveSpanSource.NoopContinuation.INSTANCE;
    }

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    private void anyRestControllerAnnotated() {
    }

    @Pointcut("@within(org.springframework.stereotype.Controller)")
    private void anyControllerAnnotated() {
    }

    @Pointcut("execution(public java.util.concurrent.Callable *(..))")
    private void anyPublicMethodReturningCallable() {
    }

    @Pointcut("(anyRestControllerAnnotated() || anyControllerAnnotated()) && anyPublicMethodReturningCallable()")
    private void anyControllerOrRestControllerWithPublicAsyncMethod() {
    }

    @Pointcut("execution(public org.springframework.web.context.request.async.WebAsyncTask *(..))")
    private void anyPublicMethodReturningWebAsyncTask() {
    }

    @Pointcut("(anyRestControllerAnnotated() || anyControllerAnnotated()) && anyPublicMethodReturningWebAsyncTask()")
    private void anyControllerOrRestControllerWithPublicWebAsyncTaskMethod() {
    }

    //TODO do we need to handle errors/exceptions as well here ???


    @Around("anyControllerOrRestControllerWithPublicAsyncMethod()")
    public Object tracePublicAsyncMethods(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        ActiveSpan activeSpan = this.continuation.activate();
        final Callable<Object> callable = (Callable<Object>) proceedingJoinPoint.proceed();
        Span span = null;
        try {
            span = createNewSpan(proceedingJoinPoint);
            return callable;
        } finally {
            closeSpan(activeSpan, span);
        }
    }

       @Around("anyControllerOrRestControllerWithPublicWebAsyncTaskMethod()")
    public Object tracePublicWebAsyncTaskMethods(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        ActiveSpan activeSpan = this.continuation.activate();
        final WebAsyncTask<?> webAsyncTask = (WebAsyncTask<?>) proceedingJoinPoint.proceed();
        Field callableField = WebAsyncTask.class.getDeclaredField("callable");
        callableField.setAccessible(true);
        callableField.set(webAsyncTask, new TracedCallable<>(webAsyncTask.getCallable(), activeSpan));

        Span span = null;
        try {
            span = createNewSpan(proceedingJoinPoint);
            return webAsyncTask;
        } finally {
            closeSpan(activeSpan, span);
        }
    }

    private Span createNewSpan(ProceedingJoinPoint proceedingJoinPoint) {
        return this.tracer.buildSpan(NameAndTagUtil.operationName(proceedingJoinPoint))
                .withTag(Tags.COMPONENT.getKey(), WEB_ASYNC_TASK_COMPONENT)
                .withTag(TAG_CLASS, NameAndTagUtil.clazzName(proceedingJoinPoint))
                .withTag(TAG_METHOD, NameAndTagUtil.methodName(proceedingJoinPoint))
                .startManual();
    }

    private void closeSpan(ActiveSpan activeSpan, Span span) {
        if (span != null) {
            span.finish();
        }
        activeSpan.close();
    }

}
