package io.opentracing.contrib.spring.cloud.async;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.web.context.request.async.WebAsyncTask;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedCallable;

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
 */
@Aspect
public class TracedAsyncWebAspect {

    private Tracer tracer;

    public TracedAsyncWebAspect(Tracer tracer) {
        this.tracer = tracer;
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

    @Around("anyControllerOrRestControllerWithPublicAsyncMethod()")
    public Object tracePublicAsyncMethods(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        final Callable<Object> delegate = (Callable<Object>) proceedingJoinPoint.proceed();
        return new TracedCallable<>(delegate, tracer.activeSpan());
    }

       @Around("anyControllerOrRestControllerWithPublicWebAsyncTaskMethod()")
    public Object tracePublicWebAsyncTaskMethods(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        final WebAsyncTask<?> webAsyncTask = (WebAsyncTask<?>) proceedingJoinPoint.proceed();
        Field callableField = WebAsyncTask.class.getDeclaredField("callable");
        callableField.setAccessible(true);
        // do not create span (there is always server span) just pass it to new thread.
        callableField.set(webAsyncTask, new TracedCallable<>(webAsyncTask.getCallable(), tracer.activeSpan()));
        return webAsyncTask;
    }
}
